package com.sismoplatform.sismoapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.ConnectException;
import java.util.ArrayList;


public class HomeActivity extends AppCompatActivity {
    //public static boolean IsActive;

    public Messenger mqttServiceMessenger = null;
    public Messenger messageHandler = new Messenger(new MessageHandler());

    private IntentFilter intentFilter = null;
    private PushReceiver pushReceiver;

    public Context applicationContext;
    public Activity activity;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;

    private boolean register;

    MqttClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(SISMO.LOG_TAG, "HomeActivity.onCreate");

        applicationContext = getApplicationContext();
        activity = this;

        setContentView(R.layout.activity_home);

        Toolbar toolbar = (Toolbar) findViewById(R.id.HomeActivity_toolbar);
        toolbar.setTitle("SisMo App");
        setSupportActionBar(toolbar);

        recyclerView = (RecyclerView)findViewById(R.id.HomeActivity_ReciclerView_MotosList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        startService(new Intent(this, MQTTService.class));

        intentFilter = new IntentFilter();
        intentFilter.addAction(SISMO.MQTT.INTENT_ACTION_WARNING);
        intentFilter.addAction(SISMO.MQTT.INTENT_ACTION_RESPONSE);

        pushReceiver = new PushReceiver();
        registerReceiver(pushReceiver, intentFilter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(SISMO.LOG_TAG, "HomeActivity.onStart");
        if(SISMO.MotoList != null && SISMO.MotoList.size() > 0){
            adapter = new MotosAdapter(R.layout.moto_list_item, getApplicationContext(), this);
            recyclerView.setAdapter(adapter);

        }else {
            GetMotos getMotos = new GetMotos();
            getMotos.execute();
        }
        bindService(new Intent(this, MQTTService.class), serviceConnection, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(SISMO.LOG_TAG, "HomeActivity.onResumen");
        SISMO.IsAppRunning = true;
        registerReceiver(pushReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(SISMO.LOG_TAG, "HomeActivity.onPause");
        SISMO.IsAppRunning = false;
        unregisterReceiver(pushReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(SISMO.LOG_TAG, "HomeActivity.onStop");
        unbindService(serviceConnection);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        } else if(id == R.id.action_logout){
            SharedPreferences sp = getSharedPreferences(SISMO.SHARED_PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.remove("accessToken");
            editor.remove("refreshToken");
            editor.apply();
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }else if(id == R.id.action_add_moto){
            Intent intent = new Intent(HomeActivity.this, AddMotoActivity.class);
            startActivity(intent);
        }else if (id == R.id.action_load_motos){
            this.register = true;
            GetMotos getMotos = new GetMotos();
            getMotos.execute();
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    Methods to manage MQTT Service
     */

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder){
            Log.i(SISMO.LOG_TAG, "HomeActivity.ServiceConnection.onServiceConnected");
            mqttServiceMessenger = new Messenger(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(SISMO.LOG_TAG, "HomeActivity.ServiceConnection.onServiceDisconnected");
        }
    };

    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg)
        {
            Log.i(SISMO.LOG_TAG, "HomeActivity.MessageHandler.handleMessage");
            switch (msg.what)
            {
                case SISMO.MQTT.ACTIONS.SUBSCRIBE: break;
                case SISMO.MQTT.ACTIONS.PUBLISH: {
                    Bundle data = msg.getData();
                    Boolean status = data.getBoolean("published");
                    if(status){
                        Toast.makeText(getApplicationContext(), "Message sent to the moto.", Toast.LENGTH_SHORT).show();
                    }else{
                        String error = data.getString("error");
                        Toast.makeText(getApplicationContext(), "The message couldn't be sent cant because this error:"+ error, Toast.LENGTH_LONG).show();
                    }
                }

            }

            //Bundle b = msg.getData();
            //if (b != null) {
                //Boolean status = b.getBoolean(SISMO.MQTT.KEYS.STATUS);
                //Log.i(SISMO.LOG_TAG, "Status from MQTT mqttServiceMessenger on the case "+msg.what+": "+status.toString());
            //}
        }
    }

    public class GetMotos extends AsyncTask<Void, Void, String> {
        ProgressDialog progressDialog;

        public GetMotos() {
            Log.i(SISMO.LOG_TAG, "HomeActivity.GetMotos");
            progressDialog = new ProgressDialog(HomeActivity.this);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            progressDialog.setMessage("Getting motos, please wait...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                Log.i(SISMO.LOG_TAG, "Getting motos");
                String url = "http://192.168.1.184:4000/api/v1/users/"+ SISMO.Username+"/motos";
                HTTPClient httpClient = new HTTPClient(url);

                httpClient.setMethod("GET");
                httpClient.addHeader("access-token", SISMO.AccessToken);
                String response = httpClient.makeRequest();

                JSONObject jsonObj = new JSONObject(response);
                int responseCode = jsonObj.getInt("code");
                String responseStatus = jsonObj.getString("status");
                if(responseCode == 200){
                    JSONObject result = jsonObj.getJSONObject("result");
                    JSONArray motos = result.getJSONArray("motos");
                    int length = motos.length();
                    if(length > 0){
                        SISMO.MotoList = new ArrayList<>();
                        String motosTopics = "";
                        for(int i=0;i<length;i++){
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
                            if(imageEncodeType.equals("base64_url_safe")){
                                decodedString = Base64.decode(m.Image, Base64.URL_SAFE);
                            }else{
                                decodedString = Base64.decode(m.Image, Base64.DEFAULT);
                            }
                            m.BitmapImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            m.MonitorinStatus = monitoringStatus;
                            m.SafetyLockStaus = safetyLockStatus;
                            m.ElectricalFlowStatus = electricalFlowStatus;
                            SISMO.MotoList.add(m);
                            motosTopics += "/messages/from/"+m.Mac;
                        }
                        SharedPreferences sp = applicationContext.getSharedPreferences(SISMO.SHARED_PREFERENCES, Context.MODE_PRIVATE);
                        String mt = sp.getString("motosTopics", "");
                        if(!motosTopics.equals(mt)){
                            Log.i(SISMO.LOG_TAG, "The new topics are diferent from the existent topics, we are subscribing to the new topics");
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putString("motosTopics", motosTopics);
                            editor.apply();

                            if(register){
                                HomeActivity ha = (HomeActivity) activity;
                                Messenger messenger = ha.mqttServiceMessenger;
                                if(messenger != null){
                                    Message message = Message.obtain(null, SISMO.MQTT.ACTIONS.UNSUBSCRIBE_TO_MOTOS_TOPICS);
                                    message.replyTo = ha.messageHandler;
                                    Bundle data = new Bundle();
                                    message.setData(data);
                                    Log.i(SISMO.LOG_TAG, "Sending message to unsubscribe from topics in mqttService");
                                    try {
                                        messenger.send(message);
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                    message = Message.obtain(null, SISMO.MQTT.ACTIONS.SUBSCRIBE_TO_MOTOS_TOPICS);
                                    message.replyTo = ha.messageHandler;
                                    data = new Bundle();
                                    message.setData(data);
                                    Log.i(SISMO.LOG_TAG, "Sending message to subscribe from topics in mqttService");
                                    try {
                                        messenger.send(message);
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                }else{
                                    Log.i(SISMO.LOG_TAG, "HomeActivity.mqttServiceMessenger is null");
                                }
                                register = false;
                            }
                        }else{
                            Log.i(SISMO.LOG_TAG, "The new topic are the same that the existent topics");
                        }
                    }
                }
                return responseStatus;
            } catch (ConnectException e1) {
                System.out.println(e1.toString());
                return "Connection error";
            } catch (Exception e2){
                System.out.println(e2.toString());
                return "Another error";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            Toast toast;
            switch (result) {
                case "Ok" :
                    adapter = new MotosAdapter(R.layout.moto_list_item, HomeActivity.this, activity);
                    recyclerView.setAdapter(adapter);
                    break;
                case "Bad request" :
                    toast = Toast.makeText(applicationContext, "Bad request", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "Unauthorized" :
                    toast = Toast.makeText(applicationContext, "Invalid access token", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "Connection error" :
                    toast = Toast.makeText(applicationContext, "Error trying to connect to the server", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "Another error" :
                    toast = Toast.makeText(applicationContext, "Something was wrong", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
            }
        }
    }

    public class PushReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent i)
        {
            String topic = i.getStringExtra(SISMO.MQTT.KEYS.TOPIC);
            String message = i.getStringExtra(SISMO.MQTT.KEYS.MESSAGE);
            //int notificationId = i.getIntExtra(SISMO.MQTT.KEYS.NOTIFICATION_ID, -1);

            try {
                JSONObject json = new JSONObject(message);
                String type = json.getString("type");
                if(type.equals("response")){
                    String mac = json.getString("mac");
                    String action = json.getString("action");
                    if(action.equals("sm") ||  action.equals("em")){
                        JSONObject info = json.getJSONObject("info");
                        String monitoringStatus = info.getString("monitoringStatus");
                        String safetyLockStatus = info.getString("safetyLockStatus");
                        String electricalFlowStatus = info.getString("electricalFlowStatus");
                        int length = SISMO.MotoList.size();
                        for(int j=0; j<length; j++){
                            if(SISMO.MotoList.get(j).Mac.equals(mac)){
                                SISMO.MotoList.get(j).MonitorinStatus = monitoringStatus;
                                SISMO.MotoList.get(j).SafetyLockStaus = safetyLockStatus;
                                SISMO.MotoList.get(j).ElectricalFlowStatus = electricalFlowStatus;
                                adapter = new MotosAdapter(R.layout.moto_list_item, context, (Activity)context);
                                recyclerView.setAdapter(adapter);
                                break;
                            }
                        }
                    }
                }else if(type.equals("warning")){
                    String mac = json.getString("mac");
                    String subject = json.getString("subject");
                    String generalStatus = json.getString("generalStatus");
                    if(subject.equals("safetyLockUnlocked")){

                        int length = SISMO.MotoList.size();
                        for(int j=0; j<length; j++){
                            if(SISMO.MotoList.get(j).Mac.equals(mac)){
                                SISMO.MotoList.get(j).SafetyLockStaus = "unloked";
                                SISMO.MotoList.get(j).GeneralStatus = generalStatus;
                                adapter = new MotosAdapter(R.layout.moto_list_item, context, (Activity)context);
                                recyclerView.setAdapter(adapter);
                                break;
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //NotificationManager mNotifyMgr = (NotificationManager) getActivity().getSystemService(NOTIFICATION_SERVICE);
            //mNotifyMgr.cancel(notificationId);

            //Toast.makeText(context, "Push message received - " + topic + ":" + message, Toast.LENGTH_LONG).show();

        }
    }
}
