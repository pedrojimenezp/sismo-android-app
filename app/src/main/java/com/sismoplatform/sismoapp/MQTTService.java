package com.sismoplatform.sismoapp;

/**
 * Created by pedro on 14/09/15.
 */


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;


public class MQTTService extends Service
{

    private int connectionState = SISMO.MQTT.CONNECTION_STATE.DISCONNECTED;

    private static boolean serviceIsRunning = false;

    private static MQTTConnection connection = null;

    private Messenger messageHandler = new Messenger(new MessageHandler());

    private String deviceId;
    private String username;

    @Override
    public void onCreate()
    {
        Log.i(SISMO.LOG_TAG, "MQTTService.onCreate");
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(SISMO.LOG_TAG, "MQTTService.onStartCommand");
        if (serviceIsRunning) {
            return START_STICKY;
        } else {
            super.onStartCommand(intent, flags, startId);
            SharedPreferences sp = getSharedPreferences(SISMO.SHARED_PREFERENCES, Context.MODE_PRIVATE);

            deviceId = sp.getString("deviceId", "");
            username = sp.getString("username", "");
            if(!deviceId.isEmpty() && !username.isEmpty()){
                connection = new MQTTConnection();
                GetMotos task = new GetMotos();
                task.execute();
            }else{
                Log.i(SISMO.LOG_TAG, "Empty credentials");
            }
            if(connection != null){
                connection.start();
                serviceIsRunning = true;
            }
            return START_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(SISMO.LOG_TAG, "MQTTService.onDestroy");

        if(connection!= null){
            connection.end();
        }
        SISMO.MotoList = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(SISMO.LOG_TAG, "MQTTService.onBind");
        //Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return messageHandler.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(SISMO.LOG_TAG, "MQTTService.onUnbind");
        //Toast.makeText(getApplicationContext(), "unbinding", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }

    class MessageHandler extends Handler  {
        @Override
        public void handleMessage(Message message) {
            Log.i(SISMO.LOG_TAG, "MQTTService.MessageHandler.handleMessage");
            if(connection != null){
                switch (message.what) {
                    case SISMO.MQTT.ACTIONS.CONNECT:
                        connection.connect();
                        break;
                    case SISMO.MQTT.ACTIONS.SUBSCRIBE:
                        connection.subscribe(message);
                        break;
                    case SISMO.MQTT.ACTIONS.UNSUBSCRIBE:
                        connection.unsubscribe(message);
                        break;
                    case SISMO.MQTT.ACTIONS.SUBSCRIBE_TO_MOTOS_TOPICS:
                        connection.subscribeToMotosTopics();
                        break;
                    case SISMO.MQTT.ACTIONS.UNSUBSCRIBE_TO_MOTOS_TOPICS:
                        connection.unsubscribeToMotosTopics();
                        break;
                    case SISMO.MQTT.ACTIONS.PUBLISH:
                        connection.publish(message);
                        break;
                }
            }else{
                if(message.replyTo != null){
                    Bundle data = new Bundle();
                    data.putString("error", "null_connection");
                    Message m = Message.obtain(null, message.what);
                    m.setData(data);
                    try {
                        message.replyTo.send(m);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class MQTTConnection extends Thread  implements MqttCallback{

        private int timeout = 5000;
        private String uri;
        private MqttClient client = null;
        private MqttConnectOptions options = new MqttConnectOptions();
        private MessageHandler MQTTMessageHandler = new MessageHandler();

        MQTTConnection(){
            uri = "tcp://" + SISMO.MQTT.SERVER_HOST + ":" + SISMO.MQTT.SERVER_PORT;
            options.setCleanSession(false);
            try {
                Log.i(SISMO.LOG_TAG, "Creating mqttClient");
                client = new MqttClient(uri, deviceId, null);
                client.setCallback(this);
                this.connect();
            } catch (MqttException e) {
                e.printStackTrace();
                Log.e(SISMO.LOG_TAG, e.getMessage());
            }
        }

        public void end(){
            client.setCallback(null);
            if (client.isConnected()) {
                try {
                    client.disconnect();
                    client.close();
                } catch (MqttException e) {
                    e.printStackTrace();
                    Log.e(SISMO.LOG_TAG, e.getMessage());
                }
            }
        }

        public void connect(){
            if (connectionState != SISMO.MQTT.CONNECTION_STATE.CONNECTED) {
                try {
                    Log.i(SISMO.LOG_TAG, "Trying to connect to " + uri);
                    client.connect(options);
                    connectionState = SISMO.MQTT.CONNECTION_STATE.CONNECTED;
                    Log.i(SISMO.LOG_TAG, "Connected to " + uri);
                    //subscribeToAllTopics();
                    subscribe("/messages/to/" + username + "/android/" + deviceId, 1);
                } catch (MqttException e) {
                    Log.d(SISMO.LOG_TAG, "Connection attemp failed. " + e.getCause());
                    delayReconnect();
                }
            }
        }

        private void subscribeToMotosTopics(){
            SharedPreferences sp = getSharedPreferences(SISMO.SHARED_PREFERENCES, Context.MODE_PRIVATE);
            String motosTopics = sp.getString("motosTopics", "");
            String[] vectorTopics = motosTopics.split(";");
            int[] vectorQOS = new int[vectorTopics.length];
            Arrays.fill(vectorQOS, 1);
            subscribe(vectorTopics, vectorQOS);
        }

        private void unsubscribeToMotosTopics(){
            SharedPreferences sp = getSharedPreferences(SISMO.SHARED_PREFERENCES, Context.MODE_PRIVATE);
            String motosTopics = sp.getString("motosTopics", "");
            String[] vectorTopics = motosTopics.split(";");
            unsubscribe(vectorTopics);
        }

        private void delayReconnect(){
            MQTTMessageHandler.sendEmptyMessageDelayed(SISMO.MQTT.ACTIONS.CONNECT, timeout);
        }

        private void subscribe(Message message) {
            Bundle response = new Bundle();
            Bundle data = message.getData();
            if (data != null) {
                String topic = data.getString(SISMO.MQTT.KEYS.TOPIC);
                int qos = data.getInt(SISMO.MQTT.KEYS.QOS);
                if (topic != null && !topic.isEmpty()) {
                    response = subscribe(topic, qos);
                }
            }
            if(message.replyTo != null){
                Message m = Message.obtain(null, message.what);
                message.replyTo = messageHandler;
                m.setData(response);
                try {
                    message.replyTo.send(m);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private Bundle subscribe(String topic, int qos) {
            Bundle response = new Bundle();
            try {
                Log.i(SISMO.LOG_TAG, "Trying to subscribe to " + topic);
                client.subscribe(topic, qos);
                Log.i(SISMO.LOG_TAG, "Subscribed to " + topic);
                response.putBoolean("subscribed", true);
            } catch (MqttException e) {
                Log.d(SISMO.LOG_TAG, "Subscribe failed with reason code = " + e.getReasonCode());
                response.putBoolean("subscribed", false);
                response.putString("error", e.getMessage());
            }
            return response;
        }

        private Bundle subscribe(String[] topics, int[] qos) {
            Bundle data = new Bundle();
            try {
                Log.i(SISMO.LOG_TAG, "Trying to subscribe to " + Arrays.toString(topics));
                client.subscribe(topics, qos);
                Log.i(SISMO.LOG_TAG, "Subscribed to " + Arrays.toString(topics));
                data.putBoolean("subscribed", true);
            } catch (MqttException e) {
                Log.d(SISMO.LOG_TAG, "Subscribe failed with reason code = " + e.getReasonCode());
                data.putBoolean("subscribed", false);
                data.putString("error", e.getMessage());
            }
            return data;
        }

        private void unsubscribe(Message message) {
            Bundle response = new Bundle();
            Bundle data = message.getData();
            if (data != null) {
                String topic = data.getString(SISMO.MQTT.KEYS.TOPIC);
                if (topic != null && !topic.isEmpty()) {
                    response = unsubscribe(topic);
                }
            }
            if(message.replyTo != null) {
                Message m = Message.obtain(null, message.what);
                message.replyTo = messageHandler;
                m.setData(response);
                try {
                    message.replyTo.send(m);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private Bundle unsubscribe(String topic) {
            Bundle response = new Bundle();
            try {
                Log.i(SISMO.LOG_TAG, "Trying to unsubscribe to " + topic);
                client.unsubscribe(topic);
                Log.i(SISMO.LOG_TAG, "Unsubscribed to " + topic);
                response.putBoolean("unsubscribed", true);
            } catch (MqttException e) {
                Log.d(SISMO.LOG_TAG, "Unsubscribe failed with reason code = " + e.getReasonCode());
                response.putBoolean("unsubscribed", false);
                response.putString("error", e.getMessage());
            }
            return response;
        }

        private Bundle unsubscribe(String[] topics) {
            Bundle response = new Bundle();
            try {
                Log.i(SISMO.LOG_TAG, "Trying to unsubscribe to " + Arrays.toString(topics));
                client.subscribe(topics);
                Log.i(SISMO.LOG_TAG, "Unsubscribed to " + Arrays.toString(topics));
                response.putBoolean("unsubscribed", true);
            } catch (MqttException e) {
                Log.d(SISMO.LOG_TAG, "Subscribe failed with reason code = " + e.getReasonCode());
                response.putBoolean("unsubscribed", false);
                response.putString("error", e.getMessage());
            }
            return response;
        }

        private void publish(Message message) {
            Bundle response = new Bundle();
            Bundle data = message.getData();
            if (data != null) {
                String topic = data.getString(SISMO.MQTT.KEYS.TOPIC);
                if (topic != null && !topic.isEmpty()) {
                    String m = data.getString(SISMO.MQTT.KEYS.MESSAGE);
                    int qos = data.getInt(SISMO.MQTT.KEYS.QOS);
                    if (m != null && !m.isEmpty()) {
                        response = publish(topic,m, qos);
                    }
                }
            }
            if(message.replyTo != null){
                Message m = Message.obtain(null, message.what);
                m.replyTo = messageHandler;
                m.setData(response);
                try {
                    message.replyTo.send(m);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private Bundle publish(String topic, String message, int qos) {
            Bundle response = new Bundle();
            try {
                MqttMessage mqttMessage = new MqttMessage();
                mqttMessage.setPayload(message.getBytes());
                Log.i(SISMO.LOG_TAG, "Trying to publish "+message+" to " + topic);
                //client.publish(topic, mqttMessage);
                client.publish(topic, mqttMessage.getPayload(), qos, false);
                Log.i(SISMO.LOG_TAG, "Message published");
                response.putBoolean("published", true);
            } catch (MqttException e) {
                Log.d(SISMO.LOG_TAG, "Publish failed with reason code = " + e.getReasonCode());
                response.putBoolean("published", false);
                response.putString("error", e.getMessage());
            }
            return response;
        }

        @Override
        public void connectionLost(Throwable throwable) {
            Log.i(SISMO.LOG_TAG, "Connection lost");
            Log.i(SISMO.LOG_TAG, throwable.getMessage());
            connectionState = SISMO.MQTT.CONNECTION_STATE.DISCONNECTED;
            delayReconnect();
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.i(SISMO.LOG_TAG, topic + ":" + message.toString());
            try{
                JSONObject jsonMessage = new JSONObject(message.toString());

                String type = jsonMessage.getString("type");

                if (SISMO.IsAppRunning){
                    Log.d(SISMO.LOG_TAG, "Sending intent");
                    Intent intent = new Intent();
                    if (type.equals("response")) {
                        intent.setAction(SISMO.MQTT.INTENT_ACTION_RESPONSE);
                    }else if (type.equals("message")) {
                        intent.setAction(SISMO.MQTT.INTENT_ACTION_MESSAGE);
                    }

                    intent.putExtra(SISMO.MQTT.KEYS.TOPIC, topic);
                    intent.putExtra(SISMO.MQTT.KEYS.MESSAGE, message.toString());
                    sendBroadcast(intent);
                }else{

                    String mac = jsonMessage.getString("mac");
                    String brand = jsonMessage.getString("brand");
                    String line = jsonMessage.getString("line");
                    String plate = jsonMessage.getString("plate");
                    String brandAndLine = brand + " " + line;

                    Log.i(SISMO.LOG_TAG,type);
                    int notificationId;
                    if (type.equals("response")) {
                        notificationId = SISMO.MQTT.KEYS.RESPONSE_NOTIFICATION_ID;
                    }else {
                        notificationId = SISMO.MQTT.KEYS.MESSAGE_NOTIFICATION_ID;
                    }

                    Context context = getBaseContext();
                    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
                    notificationBuilder.setDefaults(Notification.DEFAULT_ALL);
                    Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification_white);
                    int smallIcon = R.drawable.ic_notification_white;
                    String contentTitle = "Nueva Notificacion";
                    String contentText = "Text of notification";
                    Intent resultIntent = new Intent(context, MainActivity.class);

                    boolean showNotification = true;

                    if(type.equals("response")){
                        String action = jsonMessage.getString("action");
                        if(action.equals("sm")){
                            contentTitle = "Respuesta: Monitoreo activado";
                            contentText = "El sistema ahora esta monitoreando la moto "+brandAndLine+" con placa " + plate;
                            largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_check_green);
                            smallIcon = R.drawable.ic_check_green;
                            resultIntent = new Intent(context, MotoStatusActivity.class);
                        }else if(action.equals("em")){
                            contentTitle = "Respuesta: Monitoreo desactivado";
                            contentText = "El sistema ha dejado de monitorear la moto "+brandAndLine+" con placa " + plate;
                            largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_ex_red);
                            smallIcon = R.drawable.ic_ex_red;
                            resultIntent = new Intent(context, MotoStatusActivity.class);
                        }else if(action.equals("gp")){
                            JSONObject info = jsonMessage.getJSONObject("info");
                            double latitude = info.getDouble("latitude");
                            double longitude = info.getDouble("longitude");
                            contentTitle = "Respuesta: Posicion recivida";
                            contentText = "la motocicleta "+brandAndLine+" con placa " + plate + " ha enviado las siguientes coordendas\n"+
                                    "Latitud: "+ String.valueOf(latitude) + "\n" +
                                    "Longitud: "+String.valueOf(longitude);
                            largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_position);
                            smallIcon = R.drawable.ic_position;
                            resultIntent = new Intent(context, MapsActivity.class);
                            resultIntent.putExtra("presentLatitude", latitude);
                            resultIntent.putExtra("presentLongitude", longitude);
                        }else if(action.equals("gs")){
                            showNotification = false;
                        }
                    }else if(type.equals("message")){
                        String subject = jsonMessage.getString("subject");
                        if(subject.equals("safetyLockStatusChanged")){
                            Log.i(SISMO.LOG_TAG, "safetyLockStatusChanged");
                            JSONObject info = jsonMessage.getJSONObject("info");
                            String safetyLockStatus = info.getString("safetyLockStatus");
                            String motoName = "la motocicleta "+brandAndLine+" con placa "+plate;
                            if(safetyLockStatus.equals("locked")){
                                contentTitle = "Actualizacion: Seguro bloqueado";
                                contentText = "Hemos detectado que el seguro de " + motoName + " ha sido bloqueado.";
                                largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_check_green);
                                smallIcon = R.drawable.ic_check_green;
                            }else{
                                contentTitle = "Alerta: Seguro desbloqueado";
                                contentText = "Hemos detectado que el seguro de " + motoName + " ha sido desbloqueado.";
                                largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_ex_red);
                                smallIcon = R.drawable.ic_ex_red;
                            }
                            resultIntent.putExtra("safetyLockStatus", safetyLockStatus);
                            resultIntent = new Intent(context, MotoStatusActivity.class);
                        }else if(subject.equals("motoMoved")){
                            Log.i(SISMO.LOG_TAG, "Moto moved");
                            JSONObject info = jsonMessage.getJSONObject("info");
                            int distance = info.getInt("distance");
                            contentTitle = "Alerta: Moto movida";
                            resultIntent = new Intent(context, MotoStatusActivity.class);
                            if(distance == 5){
                                largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_warning_yellow);
                                smallIcon = R.drawable.ic_warning_yellow;
                            }else if(distance == 15){
                                largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_warning_red);
                                smallIcon = R.drawable.ic_warning_red;
                            }else if(distance == 25){
                                JSONObject parkingPosition = info.getJSONObject("parkingPosition");
                                double lat1 = parkingPosition.getDouble("latitude");
                                double lon1 = parkingPosition.getDouble("longitude");
                                JSONObject presentPosition = info.getJSONObject("presentPosition");
                                double lat2 = presentPosition.getDouble("latitude");
                                double lon2 = presentPosition.getDouble("longitude");
                                contentTitle = "Peligro: Moto posiblemente robada";
                                largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_danger_red);
                                smallIcon = R.drawable.ic_danger_red;
                                resultIntent = new Intent(context, MapsActivity.class);
                                resultIntent.putExtra("parkingLatitude", lat1);
                                resultIntent.putExtra("parkingLongitude", lon1);
                                resultIntent.putExtra("presentLatitude", lat2);
                                resultIntent.putExtra("presentLongitude", lon2);
                            }
                            contentText = "Hemos detectado que la motocicleta "+brandAndLine+" con placa " + plate + " ha sido movida "+String.valueOf(distance)+" metros desde su posicion de parqueo.";
                        }else if(subject.equals("connectionStatusChanged")){
                            /*JSONObject info = jsonMessage.getJSONObject("info");
                            String connectionStatus = info.getString("connectionStatus");
                            if(connectionStatus.equals("lost")){
                                contentTitle = "Alerta: Conexion perdida";
                                largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_connection_lost);
                                smallIcon = R.drawable.ic_connection_lost;
                                contentText = "Hemos perdido la conecxion con la motocicleta "+brandAndLine+" con placa " + plate;
                            }else{
                                contentTitle = "Actualizacion: Conexion establecida";
                                largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_connection_established);
                                smallIcon = R.drawable.ic_connection_established;
                                contentText = "Hemos establecido la conexion con la motocicleta "+brandAndLine+" con placas " + plate;
                            }
                            resultIntent = new Intent(context, MotoStatusActivity.class);*/
                            showNotification = false;
                        }else if(subject.equals("positionChanged")){
                            showNotification = false;
                        }

                    }

                    if(showNotification){
                        Log.d(SISMO.LOG_TAG, "Mostrando notificacion");

                        notificationBuilder.setSmallIcon(smallIcon);
                        notificationBuilder.setLargeIcon(largeIcon);
                        notificationBuilder.setContentTitle(contentTitle);
                        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(contentText));
                        notificationBuilder.setAutoCancel(true);
                        resultIntent.putExtra("mac", mac);
                        resultIntent.putExtra("type", type);

                        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                        notificationBuilder.setContentIntent(resultPendingIntent);
                        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        notificationManager.notify(notificationId, notificationBuilder.build());
                    }
                }
            }catch (Exception ex){
                System.out.println(ex.getMessage());
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        }
    }

    public class GetMotos extends AsyncTask<Void, Void, Void> {
        public GetMotos() {
            Log.i(SISMO.LOG_TAG, "MQTTService.GetMotos.GetMotos");
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.i(SISMO.LOG_TAG, "MQTTService.GetMotos.doInBackground");
            try {
                Log.i(SISMO.LOG_TAG, "Getting motos");
                String url = SISMO.SISMO_API_SERVER_HOST+"/api/v1/motos?userId=" + SISMO.UserId;
                HTTPClient httpClient = new HTTPClient(url);

                httpClient.setMethod("GET");
                String response = httpClient.makeRequest();

                JSONObject jsonObj = new JSONObject(response);
                int responseCode = jsonObj.getInt("code");
                if (responseCode == 200) {
                    JSONObject result = jsonObj.getJSONObject("result");
                    JSONArray motos = result.getJSONArray("motos");
                    int length = motos.length();
                    if (length > 0) {
                        SISMO.MotoList = new ArrayList<>();
                        String motosTopics = "";
                        for (int i = 0; i < length; i++) {
                            JSONObject moto = motos.getJSONObject(i);
                            String mac = moto.getString("mac");
                            String brand = moto.getString("brand");
                            String line = moto.getString("line");
                            String plate = moto.getString("plate");
                            String color = moto.getString("color");
                            String image = moto.getString("image");
                            String imageEncodeType = moto.getString("imageEncodeType");
                            int model = moto.getInt("model");
                            int cylinderCapacity = moto.getInt("cylinderCapacity");
                            JSONObject status = moto.getJSONObject("status");
                            String monitoringStatus = status.getString("monitoring");
                            String safetyLockStatus = status.getString("safetyLock");
                            String electricalFlowStatus = status.getString("electricalFlow");
                            Moto m = new Moto();
                            m.Mac = mac;
                            m.Brand = brand;
                            m.Line = line;
                            m.Color = color;
                            m.Model = model;
                            m.CylinderCapacity = cylinderCapacity;
                            m.Plate = plate;
                            m.Image = image;
                            m.ImageEncodeType = imageEncodeType;
                            byte[] decodedString;
                            if (imageEncodeType.equals("base64_url_safe")) {
                                decodedString = Base64.decode(m.Image, Base64.URL_SAFE);
                            } else {
                                decodedString = Base64.decode(m.Image, Base64.DEFAULT);
                            }
                            m.BitmapImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            m.MonitorinStatus = monitoringStatus;
                            m.SafetyLockStaus = safetyLockStatus;
                            m.ElectricalFlowStatus = electricalFlowStatus;
                            SISMO.MotoList.add(m);
                            motosTopics += "/messages/from/" + SISMO.Username + "/motos/" + m.Mac + ";";
                        }
                        SharedPreferences sp = getApplicationContext().getSharedPreferences(SISMO.SHARED_PREFERENCES, Context.MODE_PRIVATE);
                        String mt = sp.getString("motosTopics", "");
                        Log.i(SISMO.LOG_TAG, mt);
                        Log.i(SISMO.LOG_TAG, motosTopics);
                        if (!motosTopics.equals(mt)) {
                            Log.i(SISMO.LOG_TAG, "The new topics are diferent from the existent topics, we are subscribing to the new topics");
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putString("motosTopics", motosTopics);
                            editor.apply();


                            Message message = Message.obtain(null, SISMO.MQTT.ACTIONS.UNSUBSCRIBE_TO_MOTOS_TOPICS);
                            Log.i(SISMO.LOG_TAG, "Sending message to unsubscribe from topics in mqttService");
                            try {
                                messageHandler.send(message);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }

                        } else {
                            Log.i(SISMO.LOG_TAG, "The new topic are the same that the existent topics");
                        }
                        Message message = Message.obtain(null, SISMO.MQTT.ACTIONS.SUBSCRIBE_TO_MOTOS_TOPICS);
                        Log.i(SISMO.LOG_TAG, "Sending message to subscribe from topics in mqttService");
                        try {
                            messageHandler.send(message);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(SISMO.LOG_TAG, "Sending intent");
            Intent intent = new Intent();
            intent.setAction(SISMO.MQTT.INTENT_MOTOS_UPDATED);
            sendBroadcast(intent);
        }
    }
}
