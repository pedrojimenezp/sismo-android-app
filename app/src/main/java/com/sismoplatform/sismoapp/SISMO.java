package com.sismoplatform.sismoapp;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.logging.ConsoleHandler;

/**
 * Created by pedro on 14/10/15.
 */
public final class SISMO {
    public static String DeviceId = "";
    public static String AccessToken = "";
    public static String RefreshToken = "";
    public static String Username = "";
    public static ArrayList<Moto> MotoList = new ArrayList<Moto>();

    public static final String SHARED_PREFERENCES = "com.sismoplatform.sismoapp.sharedPreferences";

    public static final String LOG_TAG = "SisMo";

    public static final String SISMO_API_SERVER_HOST = "192.168.1.184";
    public static final String SISMO_API_SERVER_PORT = "4000";

    public static class MQTT{
        public static final String SERVER_HOST = "192.168.1.184";
        public static final String SERVER_PORT = "1883";

        public static final String INTENT_ACTION_WARNING = "com.sismoplatform.sismoapp.mqtt.motoWarning";
        public static final String INTENT_ACTION_RESPONSE = "com.sismoplatform.sismoapp.mqtt.motoResponse";


        public static class ACTIONS {
            public static final int CONNECT = 0;
            public static final int SUBSCRIBE = 1;
            public static final int UNSUBSCRIBE = 2;
            public static final int PUBLISH = 3;
            public static final int SUBSCRIBE_TO_MOTOS_TOPICS = 4;
            public static final int UNSUBSCRIBE_TO_MOTOS_TOPICS = 5;
        };

        public static enum CONNECTION_STATE {
            DISCONNECTED,
            CONNECTING,
            CONNECTED
        };

        public static class KEYS {
            public static final String TOPIC = "topic";
            public static final String MESSAGE = "message";
            public static final String NOTIFICATION_ID = "notificationId";
            public static final String STATUS = "status";
            public static final String DESCRIPTION = "description";

            public static final int RESPONSE_NOTIFICATION_ID = 0;
            public static final int WARNING_NOTIFICATION_ID = 1;
        };
    }

    public static void SendMessage(Messenger receiver, int type, Bundle data) {
        if (receiver != null) {
            Message reply = Message.obtain(null, type);
            reply.setData(data);
            try {
                Log.i(SISMO.LOG_TAG, "Sending message to receiver");
                receiver.send(reply);
            } catch (RemoteException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }else{
            Log.i(SISMO.LOG_TAG, "Receiver is null");
        }
    }
}
