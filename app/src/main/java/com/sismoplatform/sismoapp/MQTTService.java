package com.sismoplatform.sismoapp;

/**
 * Created by pedro on 14/09/15.
 */


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.Arrays;


public class MQTTService extends Service
{

    private SISMO.MQTT.CONNECTION_STATE connectionState = SISMO.MQTT.CONNECTION_STATE.DISCONNECTED;

    private static boolean serviceIsRunning = false;

    private static MQTTConnection connection = null;

    public Messenger messageHandler = new Messenger(new MessageHandler());

    public String deviceId;
    public String username;

    @Override
    public void onCreate()
    {
        Log.i(SISMO.LOG_TAG, "MQTTService.onCreate");
        super.onCreate();
        SharedPreferences sp = getSharedPreferences(SISMO.SHARED_PREFERENCES, Context.MODE_PRIVATE);

        deviceId = sp.getString("deviceId", "");
        username = sp.getString("username", "");
        if(!deviceId.isEmpty() && !username.isEmpty()){
            connection = new MQTTConnection();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (serviceIsRunning) {
            return START_STICKY;
        } else {
            super.onStartCommand(intent, flags, startId);
            if(connection != null){
                connection.start();
                serviceIsRunning = true;
            }
            return START_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        if(connection!= null){
            connection.end();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(SISMO.LOG_TAG, "MQTTService.onBind");
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return messageHandler.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(SISMO.LOG_TAG, "MQTTService.onUnbind");
        Toast.makeText(getApplicationContext(), "unbinding", Toast.LENGTH_SHORT).show();
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

        private int timeout = 10000;
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
                    Log.i(SISMO.LOG_TAG, "Trying to connect to "+uri);
                    client.connect(options);
                    connectionState = SISMO.MQTT.CONNECTION_STATE.CONNECTED;
                    Log.i(SISMO.LOG_TAG, "Connected to " + uri);
                    subscribeToAllTopics();
                } catch (MqttException e) {
                    Log.d(SISMO.LOG_TAG, "Connection attemp failed. " + e.getCause());
                    delayReconnect();
                }
            }
        }

        private void subscribeToAllTopics(){
            Log.i(SISMO.LOG_TAG, "Subscribing to existing topics");
            subscribe("/messages/to/" + username + "/apps/android/" + deviceId);
            subscribeToMotosTopics();
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
                if (topic != null && !topic.isEmpty()) {
                    response = subscribe(topic);
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

        private Bundle subscribe(String topic) {
            Bundle response = new Bundle();
            try {
                Log.i(SISMO.LOG_TAG, "Trying to subscribe to " + topic);
                client.subscribe(topic, 1);
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
                    if (m != null && !m.isEmpty()) {
                        response = publish(topic,m);
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

        private Bundle publish(String topic, String message) {
            Bundle response = new Bundle();
            try {
                MqttMessage mqttMessage = new MqttMessage();
                mqttMessage.setPayload(message.getBytes());
                Log.i(SISMO.LOG_TAG, "Trying to publish message to " + topic);
                //client.publish(topic, mqttMessage);
                client.publish(topic, mqttMessage.getPayload(), 1, false);
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
                String mac = jsonMessage.getString("mac");

                Moto moto = null;
                int length = SISMO.MotoList.size();
                for(int j=0; j<length; j++){
                    if(SISMO.MotoList.get(j).Mac.equals(mac)){
                        moto = SISMO.MotoList.get(j);
                        break;
                    }
                }

                Log.i(SISMO.LOG_TAG,type);
                int notificationId;
                if (type.equals("response")) {
                    notificationId = SISMO.MQTT.KEYS.RESPONSE_NOTIFICATION_ID;
                }else if (type.equals("warning")) {
                    notificationId = SISMO.MQTT.KEYS.WARNING_NOTIFICATION_ID;
                }else{
                    notificationId = SISMO.MQTT.KEYS.UPDATE_NOTIFICATION_ID;
                }

                if (SISMO.IsAppRunning){
                    Log.d(SISMO.LOG_TAG, "Sending intent");
                    Intent intent = new Intent();
                    if (type.equals("response")) {
                        intent.setAction(SISMO.MQTT.INTENT_ACTION_RESPONSE);
                    }else if (type.equals("warning")) {
                        intent.setAction(SISMO.MQTT.INTENT_ACTION_WARNING);
                    }else{
                        intent.setAction(SISMO.MQTT.INTENT_ACTION_UPDATE);
                    }

                    intent.putExtra(SISMO.MQTT.KEYS.TOPIC, topic);
                    intent.putExtra(SISMO.MQTT.KEYS.MESSAGE, message.toString());
                    sendBroadcast(intent);
                }else{
                    Context context = getBaseContext();
                    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
                    Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.ic_notification_white);;
                    int smallIcon = R.drawable.ic_notification_white;
                    String contentTitle = "New Notification";
                    String contentText = "Text of notification";
                    Intent resultIntent = new Intent(context, MainActivity.class);
                    if(type.equals("response")){
                        String action = jsonMessage.getString("action");


                        if(action.equals("sm")){
                            contentTitle = "Monitoring activated";
                            largeIcon = BitmapFactory.decodeResource(context.getResources(),
                                    R.drawable.ic_check);
                            smallIcon = R.drawable.ic_check;

                            if(moto != null){
                                contentText = "The device is now monitoring the motorcycle "+moto.getBrandAndLine()+" with plate " + moto.Plate;
                            }

                            resultIntent = new Intent(context, MotoStatusActivity.class);
                            resultIntent.putExtra("mac", mac);
                        }else if(action.equals("em")){
                            contentTitle = "Monitoring deactivated";
                            largeIcon = BitmapFactory.decodeResource(context.getResources(),
                                    R.drawable.ic_ex);
                            smallIcon = R.drawable.ic_ex;
                            if(moto != null){
                                contentText = "The device is no longer monitoring the motorcycle "+moto.getBrandAndLine()+" with plate " + moto.Plate;
                            }

                            resultIntent = new Intent(context, MotoStatusActivity.class);
                            resultIntent.putExtra("mac", mac);
                        }else if(action.equals("gp")){
                            JSONObject positions = jsonMessage.getJSONObject("info");
                            JSONObject parkingPosition = positions.getJSONObject("parkingPosition");
                            JSONObject presentPosition = positions.getJSONObject("presentPosition");
                            double latitude1 = parkingPosition.getDouble("latitude");
                            double longitude1 = parkingPosition.getDouble("longitude");
                            double latitude2 = presentPosition.getDouble("latitude");
                            double longitude2 = presentPosition.getDouble("longitude");
                            contentTitle = "Position received";
                            largeIcon = BitmapFactory.decodeResource(context.getResources(),
                                    R.drawable.ic_position);
                            smallIcon = R.drawable.ic_position;

                            if(moto != null){
                                contentText = "The motorcycle "+moto.getBrandAndLine()+" with plate " + moto.Plate + "has sent the next position\n"+
                                        "Latitude: "+ String.valueOf(latitude2) + "\n" +
                                        "Longitude: "+String.valueOf(longitude2);
                            }

                            resultIntent = new Intent(context, MapsActivity.class);
                            resultIntent.putExtra("mac", mac);
                            resultIntent.putExtra("latitude1", latitude1);
                            resultIntent.putExtra("longitude1", longitude1);
                            resultIntent.putExtra("latitude2", latitude2);
                            resultIntent.putExtra("longitude2", longitude2);
                        }

                    }else if(type.equals("warning")){

                    }else if (type.equals("update")) {
                        JSONObject positions = jsonMessage.getJSONObject("info");
                        JSONObject parkingPosition = positions.getJSONObject("parkingPosition");
                        JSONObject presentPosition = positions.getJSONObject("presentPosition");
                        double latitude1 = parkingPosition.getDouble("latitude");
                        double longitude1 = parkingPosition.getDouble("longitude");
                        double latitude2 = presentPosition.getDouble("latitude");
                        double longitude2 = presentPosition.getDouble("longitude");
                        contentTitle = "Position received";
                        largeIcon = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.ic_position);
                        smallIcon = R.drawable.ic_position;

                        if(moto != null){
                            contentText = "The motorcycle "+moto.getBrandAndLine()+" with plate " + moto.Plate + "has sent the next position\n"+
                                    "Latitude: "+ String.valueOf(latitude2) + "\n" +
                                    "Longitude: "+String.valueOf(longitude2);
                        }

                        resultIntent = new Intent(context, MapsActivity.class);
                        resultIntent.putExtra("mac", mac);
                        resultIntent.putExtra("latitude1", latitude1);
                        resultIntent.putExtra("longitude1", longitude1);
                        resultIntent.putExtra("latitude2", latitude2);
                        resultIntent.putExtra("longitude2", longitude2);
                    }

                    Log.d(SISMO.LOG_TAG, "Mostrando notificacion");

                    notificationBuilder.setSmallIcon(smallIcon);
                    notificationBuilder.setLargeIcon(largeIcon);
                    notificationBuilder.setContentTitle(contentTitle);
                    notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(contentText));
                    //notificationBuilder.setContentText(contentText);
                    notificationBuilder.setAutoCancel(true);

                    notificationBuilder.setDefaults(Notification.DEFAULT_ALL);


                    resultIntent.putExtra("mac", mac);
                    resultIntent.putExtra("type", type);

                    PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    notificationBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                    notificationManager.notify(notificationId, notificationBuilder.build());
                }
            }catch (Exception ex){
                System.out.println(ex.getMessage());
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }

    }

}
