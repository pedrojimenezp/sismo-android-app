package com.sismoplatform.sismoapp;

/**
 * Created by pedro on 14/09/15.
 */


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Hashtable;
import java.util.Vector;


public class MQTTService extends Service
{
    enum CONNECTION_STATE
    {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    public static final int REGISTER = 0;
    public static final int SUBSCRIBE = 1;
    public static final int PUBLISH = 2;
    public static final int CONNECT = 3;
    private CONNECTION_STATE connectionState = CONNECTION_STATE.DISCONNECTED;


    public static final String TOPIC = "topic";
    public static final String MESSAGE = "message";
    public static final String NOTIFICATION_ID = "notification_id";
    public static final String STATUS = "status";
    public static final String INTENT_ACTION = "intentaction";



    private static boolean serviceIsRunning = false;
    private static int notificationId = 0;
    private static MQTTConnection connection = null;
    private final Messenger clientMessenger = new Messenger(new MessageHandler());

    @Override
    public void onCreate()
    {
        super.onCreate();
        connection = new MQTTConnection();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (serviceIsRunning) {
            return START_STICKY;
        } else {
            serviceIsRunning = true;
            super.onStartCommand(intent, flags, startId);
            connection.start();
            return START_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        connection.end();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return clientMessenger.getBinder();
    }


    class MessageHandler extends Handler  {
        @Override
        public void handleMessage(Message message)
        {
            boolean status = false;

            switch (message.what)
            {
                case SUBSCRIBE:
                    connection.subscribe(message);
                case PUBLISH:
                    connection.publish(message);
                    break;
                case REGISTER: {
                    Bundle b = message.getData();
                    if (b != null) {
                        CharSequence cs = b.getCharSequence(INTENT_ACTION);
                        if (cs != null) {
                            String name = cs.toString().trim();
                            if (name.isEmpty() == false) {
                                connection.setIntentAction(name);
                                status = true;
                            }
                        }
                    }
                    ReplytoClient(message.replyTo, message.what, status);
                    break;
                }
                case CONNECT:
                    connection.connect();
                    break;
            }
        }
    }

    private void ReplytoClient(Messenger receiver, int type, boolean status) {
        if (receiver != null) {
            Bundle data = new Bundle();
            data.putBoolean(STATUS, status);
            Message reply = Message.obtain(null, type);
            reply.setData(data);
            try {
                receiver.send(reply);
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(MainActivity.LOG_TAG, e.getMessage());
            }
        }
    }

    private class MQTTConnection extends Thread  implements MqttCallback{
        //private Class<?> launchActivity = null;
        private String intentAction = null;

        private int timeout = 5000;
        private String host = "192.168.1.184";
        private int port = 1883;
        private String uri;
        private MqttClient client = null;
        private MqttConnectOptions options = new MqttConnectOptions();
        //private Vector<String> topics = new Vector<String>();
        private MessageHandler messageHandler = new MessageHandler();
        String clientId = MqttClient.generateClientId();
        private Hashtable topics = new Hashtable();

        MQTTConnection(){
            Log.i(MainActivity.LOG_TAG, "Reading shared preferences");
            SharedPreferences sharedPreferences = getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
            String h = sharedPreferences.getString("mqttHost", "");
            int p = sharedPreferences.getInt("mqttPort", -1);
            if (h != "") {
                host = h;
            }
            if (p != -1) {
                port = p;
            }
            uri = "tcp://" + host + ":" + port;
            options.setCleanSession(true);
            try {
                Log.i(MainActivity.LOG_TAG, "Creating mqttClient");

                client = new MqttClient(uri, clientId, null);
                client.setCallback(this);
                this.connect();
            } catch (MqttException e) {
                e.printStackTrace();
                Log.e(MainActivity.LOG_TAG, e.getMessage());
            }
        }

        public void setIntentAction(String action) {
            intentAction = action;
        }

        public void end(){
            client.setCallback(null);
            if (client.isConnected()) {
                try {
                    client.disconnect();
                    client.close();
                } catch (MqttException e) {
                    e.printStackTrace();
                    Log.e(MainActivity.LOG_TAG, e.getMessage());
                }
            }
        }

        public void connect(){
            if (connectionState != CONNECTION_STATE.CONNECTED) {
                try {
                    Log.i(MainActivity.LOG_TAG, "Trying to connect to "+uri);
                    client.connect(options);
                    connectionState = CONNECTION_STATE.CONNECTED;
                    Log.i(getClass().getCanonicalName(), "Connected");
                    subscribeToAllTopics();
                } catch (MqttException e) {
                    Log.d(MainActivity.LOG_TAG, "Connection attemp failed. " + e.getCause());
                    delayReconnect();
                }
            }
        }

        private void subscribeToAllTopics(){
            topics = new Hashtable();
            Log.i(MainActivity.LOG_TAG, "Subscribing to existing topics");
            subscribe(clientId);
            subscribeToMotosTopics();
        }

        private void subscribeToMotosTopics(){
            SharedPreferences sp = getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
            String motosTopics = sp.getString("motos", "");
            String[] vectorTopics = motosTopics.split(";");
            int length = vectorTopics.length;
            for(int i=0;i<length;i++){
                subscribe(vectorTopics[i]);
            }
        }

        private void delayReconnect(){
            messageHandler.sendEmptyMessageDelayed(CONNECT, timeout);
        }

        private void subscribe(Message message) {
            boolean subscribed = false;
            Bundle b = message.getData();
            if (b != null) {
                CharSequence cs = b.getCharSequence(TOPIC);
                if (cs != null) {
                    String topic = cs.toString().trim();
                    if (topic.isEmpty() == false) {
                        subscribed = subscribe(topic);
                    }
                }
            }
            ReplytoClient(message.replyTo, message.what, subscribed);
        }

        private boolean subscribe(String topic) {
            boolean subscribed = false;
            try {
                Log.i(MainActivity.LOG_TAG, "Trying to subscribe to " + topic);
                client.subscribe(topic);
                subscribed = true;
            } catch (MqttException e) {
                 Log.d(MainActivity.LOG_TAG, "Subscribe failed with reason code = " + e.getReasonCode());
                 subscribed = false;
            }
            if (subscribed) {
                topics.put(topic, notificationId);
                notificationId++;
            }
            return subscribed;
        }

        private void publish(Message message) {
            boolean published = false;
            Bundle b = message.getData();
            if (b != null) {
                CharSequence cs = b.getCharSequence(TOPIC);
                if (cs != null) {
                    String topic = cs.toString().trim();
                    if (!topic.isEmpty()) {
                        cs = b.getCharSequence(MESSAGE);
                        if (cs != null) {
                            String m = cs.toString().trim();
                            if (!m.isEmpty()) {
                                published = publish(topic,m);
                            }
                        }
                    }
                }
            }
            ReplytoClient(message.replyTo, message.what, published);
        }

        private boolean publish(String topic, String message) {
            boolean published = false;

            try {
                MqttMessage mqttMessage = new MqttMessage();
                mqttMessage.setPayload(message.getBytes());
                Log.i(MainActivity.LOG_TAG, "Trying to publish message to " + topic);
                client.publish(topic, mqttMessage);
                published = true;
            } catch (MqttException e) {
                Log.d(MainActivity.LOG_TAG, "Publish failed with reason code = " + e.getReasonCode());
                published = false;
            }
            return published;
        }

        @Override
        public void connectionLost(Throwable throwable) {
            Log.i(MainActivity.LOG_TAG, "Connection lost");
            connectionState = CONNECTION_STATE.DISCONNECTED;
            delayReconnect();
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.i(MainActivity.LOG_TAG, topic + ":" + message.toString());

            Context context = getBaseContext();

            Log.d(MainActivity.LOG_TAG, "Mostrando notificacion");

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
            notificationBuilder.setSmallIcon(R.drawable.ic_notification_white);
            notificationBuilder.setSmallIcon(R.drawable.ic_notification_white);
            notificationBuilder.setContentTitle(topic);
            notificationBuilder.setContentText(message.toString());
            notificationBuilder.setAutoCancel(true);
            Intent resultIntent = new Intent(context, MainActivity.class);

            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            context,
                            0,
                            resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            notificationBuilder.setContentIntent(resultPendingIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.notify((int) topics.get(topic), notificationBuilder.build());
            if (intentAction != null) {
                Intent intent = new Intent();
                intent.setAction(intentAction);
                intent.putExtra(TOPIC, topic);
                intent.putExtra(MESSAGE, message.toString());
                intent.putExtra(NOTIFICATION_ID, (int)topics.get(topic));
                sendBroadcast(intent);
            }
            Log.d(MainActivity.LOG_TAG, "Notificacion mostrada");
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }

    }

}
