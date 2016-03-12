package com.sismoplatform.sismoapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
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

    public RecyclerView recyclerView;
    public RecyclerView.Adapter adapter;

    private boolean register;

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(SISMO.LOG_TAG, "HomeActivity.onStart");
        bindService(new Intent(this, MQTTService.class), serviceConnection, 0);
        if(SISMO.MotoList != null && SISMO.MotoList.size() > 0){
            Log.i(SISMO.LOG_TAG, "Motos are loaded, render the adapter");
            Log.i(SISMO.LOG_TAG, String.valueOf(SISMO.MotoList.size()));
            adapter = new MotosAdapter(R.layout.moto_list_item, HomeActivity.this, this);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(SISMO.LOG_TAG, "HomeActivity.onResumen");
        SISMO.IsAppRunning = true;
        intentFilter = new IntentFilter();
        intentFilter.addAction(SISMO.MQTT.INTENT_ACTION_MESSAGE);
        intentFilter.addAction(SISMO.MQTT.INTENT_ACTION_RESPONSE);
        intentFilter.addAction(SISMO.MQTT.INTENT_MOTOS_UPDATED);

        pushReceiver = new PushReceiver();
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
            editor.remove("userId");
            editor.remove("username");
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

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder){
            Log.i(SISMO.LOG_TAG, "HomeActivity.ServiceConnection.onServiceConnected");
            mqttServiceMessenger = new Messenger(binder);
            if(SISMO.MotoList != null && SISMO.MotoList.size() > 0){
                adapter = new MotosAdapter(R.layout.moto_list_item, HomeActivity.this, activity);
                recyclerView.setAdapter(adapter);
            }
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
            switch (msg.what) {
                case SISMO.MQTT.ACTIONS.SUBSCRIBE: break;
                case SISMO.MQTT.ACTIONS.PUBLISH: {
                    Bundle data = msg.getData();
                    Boolean status = data.getBoolean("published");
                    if(status){
                        //Toast.makeText(getApplicationContext(), "Mensaje enviado a la moto.", Toast.LENGTH_SHORT).show();
                    }else{
                        String error = data.getString("error");
                        Toast.makeText(getApplicationContext(), "El mensaje no pudo ser enviado a la moto.\nError:"+ error, Toast.LENGTH_LONG).show();
                    }
                }
            }
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
            progressDialog.setMessage("Obteniendo motos por favor espere...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                Log.i(SISMO.LOG_TAG, "Getting motos");
                String url = SISMO.SISMO_API_SERVER_HOST+"/api/v1/motos?userId="+SISMO.UserId;
                HTTPClient httpClient = new HTTPClient(url);

                httpClient.setMethod("GET");
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
                            motosTopics += "/messages/from/"+SISMO.Username+"/motos/" +m.Mac+";";
                        }
                        SharedPreferences sp = applicationContext.getSharedPreferences(SISMO.SHARED_PREFERENCES, Context.MODE_PRIVATE);
                        String mt = sp.getString("motosTopics", "");
                        Log.i(SISMO.LOG_TAG, mt);
                        Log.i(SISMO.LOG_TAG, motosTopics);
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
                    toast = Toast.makeText(applicationContext, "Error tratando de conectarse con el servidor", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "Another error" :
                    toast = Toast.makeText(applicationContext, "Algo salio mal", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
            }
        }
    }

    public class PushReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent i) {
            if(i.getAction().equals(SISMO.MQTT.INTENT_MOTOS_UPDATED)){
                adapter = new MotosAdapter(R.layout.moto_list_item, HomeActivity.this, activity);
                recyclerView.setAdapter(adapter);
            }else{
                String topic = i.getStringExtra(SISMO.MQTT.KEYS.TOPIC);
                String message = i.getStringExtra(SISMO.MQTT.KEYS.MESSAGE);
                try {
                    JSONObject json = new JSONObject(message);
                    String type = json.getString("type");
                    String mac = json.getString("mac");
                    Moto moto = null;
                    int length = SISMO.MotoList.size();
                    for(int j=0; j<length; j++){
                        if(SISMO.MotoList.get(j).Mac.equals(mac)){
                            moto = SISMO.MotoList.get(j);
                            break;
                        }
                    }
                    if(type.equals("response")){
                        String action = json.getString("action");
                        if(action.equals("sm") ||  action.equals("em")){
                            JSONObject info = json.getJSONObject("info");
                            String monitoringStatus = info.getString("monitoringStatus");
                            String safetyLockStatus = info.getString("safetyLockStatus");
                            String electricalFlowStatus = info.getString("electricalFlowStatus");
                            if(moto != null){
                                moto.MonitorinStatus = monitoringStatus;
                                moto.SafetyLockStaus = safetyLockStatus;
                                moto.ElectricalFlowStatus = electricalFlowStatus;
                                adapter = new MotosAdapter(R.layout.moto_list_item, context, (Activity)context);
                                recyclerView.setAdapter(adapter);
                            }
                        }
                    }else if(type.equals("message")){
                        Log.i(SISMO.LOG_TAG, "message");
                        String subject = json.getString("subject");
                        if(subject.equals("motoMoved")){
                            Log.i(SISMO.LOG_TAG, "Moto moved");
                            if(moto != null){
                                JSONObject info = json.getJSONObject("info");
                                int distance = info.getInt("distance");
                                String motoName = "la motocicleta "+moto.getBrandAndLine()+" con placa "+moto.Plate;
                                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                                builder.setTitle("Alerta: Moto movida.");
                                builder.setMessage("Hemos detectado que " + motoName + " ha sido movida " + String.valueOf(distance) + " metros desde su posicion de parqueo.");
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                if(distance==5){
                                    builder.setIcon(R.drawable.ic_warning_yellow);
                                }else if(distance == 15){
                                    builder.setIcon(R.drawable.ic_warning_red);
                                }else if(distance == 25){
                                    final String motoMac = json.getString("mac");
                                    JSONObject parkingPosition = info.getJSONObject("parkingPosition");
                                    final double lat1 = parkingPosition.getDouble("latitude");
                                    final double lon1 = parkingPosition.getDouble("longitude");
                                    JSONObject presentPosition = info.getJSONObject("presentPosition");
                                    final double lat2 = presentPosition.getDouble("latitude");
                                    final double lon2 = presentPosition.getDouble("longitude");
                                    builder.setIcon(R.drawable.ic_danger_red);
                                    builder.setTitle("Peligro: Moto posiblemente robada.");
                                    builder.setPositiveButton("Abrir mapa", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent i = new Intent(HomeActivity.this, MapsActivity.class);
                                            i.putExtra("mac", motoMac);
                                            i.putExtra("parkingLatitude", lat1);
                                            i.putExtra("parkingLongitude", lon1);
                                            i.putExtra("presentLatitude", lat2);
                                            i.putExtra("presentLongitude", lon2);
                                            startActivity(i);
                                            dialog.dismiss();
                                        }
                                    });
                                    builder.setNegativeButton("Cerrar", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                                }
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                        }else if(subject.equals("connectionStatusChanged")){
                            Log.i(SISMO.LOG_TAG, "connectionStatusChanged");
                            JSONObject info = json.getJSONObject("info");
                            String connectionStatus = info.getString("connectionStatus");
                            if(moto != null){
                                String motoName = "la motocicleta "+moto.getBrandAndLine()+" con placa "+moto.Plate;
                                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                                if(connectionStatus.equals("lost")){
                                    builder.setTitle("Alerta: Conexion perdida.");
                                    builder.setMessage("Hemos perdido la conexion con " + motoName);
                                    builder.setIcon(R.drawable.ic_connection_lost);

                                }else{
                                    builder.setTitle("Actulizacion: Conexion establiesida.");
                                    builder.setMessage("Hemos establecido la conexion con " + motoName);
                                    builder.setIcon(R.drawable.ic_connection_established);
                                }
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                        }else if(subject.equals("safetyLockStatusChanged")){
                            Log.i(SISMO.LOG_TAG, "safetyLockStatusChanged");
                            JSONObject info = json.getJSONObject("info");
                            String safetyLockStatus = info.getString("safetyLockStatus");
                            if(moto != null){
                                moto.SafetyLockStaus = safetyLockStatus;
                                String motoName = "la motocicleta "+moto.getBrandAndLine()+" con placa "+moto.Plate;
                                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                                if(safetyLockStatus.equals("locked")){
                                    builder.setTitle("Actualizacion: Seguro bloqueado.");
                                    builder.setMessage("Hemos detectado que el seguro de " + motoName + " ha sido bloqueado.");
                                    builder.setIcon(R.drawable.ic_check_green);
                                }else{
                                    builder.setTitle("Alerta: Seguro desbloqueado.");
                                    builder.setMessage("Hemos detectado que el seguro de " + motoName + " ha sido desbloqueado.");
                                    builder.setIcon(R.drawable.ic_ex_red);
                                }
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        }
    }
}
