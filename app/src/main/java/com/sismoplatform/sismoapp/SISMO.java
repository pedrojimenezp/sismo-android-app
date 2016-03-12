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

    public static boolean IsAppRunning = false;

    public static String DeviceId = "";
    public static String UserId = "";
    public static String Username = "";
    public static ArrayList<Moto> MotoList = new ArrayList<Moto>();

    public static final String SHARED_PREFERENCES = "com.sismoplatform.sismoapp.sharedPreferences";

    public static final String LOG_TAG = "SisMo";

    public static final String SISMO_API_SERVER_HOST = "https://web-sismo.herokuapp.com";
    //public static final String SISMO_API_SERVER_HOST = "http://192.168.1.184:4000";

    public static class MQTT{
        public static final String SERVER_HOST = "test.mosquitto.org";
        public static final String SERVER_PORT = "1883";

        public static final String INTENT_ACTION_RESPONSE = "com.sismoplatform.sismoapp.mqtt.motoResponse";
        public static final String INTENT_ACTION_MESSAGE = "com.sismoplatform.sismoapp.mqtt.motoMessage";
        public static final String INTENT_MOTOS_UPDATED = "com.sismoplatform.sismoapp.mqtt.motosUpdated";


        public static class ACTIONS {
            public static final int CONNECT = 0;
            public static final int SUBSCRIBE = 1;
            public static final int UNSUBSCRIBE = 2;
            public static final int PUBLISH = 3;
            public static final int SUBSCRIBE_TO_MOTOS_TOPICS = 4;
            public static final int UNSUBSCRIBE_TO_MOTOS_TOPICS = 5;
        };

        public static class CONNECTION_STATE {
            public static final int DISCONNECTED = 0;
            public static final int CONNECTING = 1;
            public static final int CONNECTED = 2;
        };

        public static class KEYS {
            public static final String TOPIC = "topic";
            public static final String MESSAGE = "message";
            public static final String QOS = "qos";

            public static final int RESPONSE_NOTIFICATION_ID = 0;
            public static final int MESSAGE_NOTIFICATION_ID = 1;
        };
    }
}
