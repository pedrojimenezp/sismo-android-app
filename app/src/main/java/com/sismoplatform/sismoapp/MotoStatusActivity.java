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
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.ConnectException;
import java.util.ArrayList;

public class MotoStatusActivity extends AppCompatActivity {
    public Messenger mqttServiceMessenger = null;
    public Messenger messageHandler = new Messenger(new MessageHandler());

    CollapsingToolbarLayout collapsingToolbarLayout;

    int vibrantColor = R.color.primary;

    Moto moto = null;

    ImageView ImageView_MotoImage;
    ImageView ImageView_Background;
    //TextView TextView_ConnectionStatus;
    TextView TextView_MotonitoringStatus;
    TextView TextView_SafetyLockStatus;
    TextView TextView_ElectricalFlowStatus;
    Button Button_StartMonitoring;
    Button Button_OpenMap;

    private IntentFilter intentFilter = null;
    private PushReceiver pushReceiver;

    Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(SISMO.LOG_TAG, "MotoStatusActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moto_status);

        activity = this;
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            int index = extras.getInt("listIndex", -1);
            if(index != -1){
                moto = SISMO.MotoList.get(index);
            }else{
                String mac = extras.getString("mac", "");
                if(!mac.isEmpty() && SISMO.MotoList != null){
                    int length = SISMO.MotoList.size();
                    for(int j=0; j<length; j++){
                        if(SISMO.MotoList.get(j).Mac.equals(mac)){
                            moto = SISMO.MotoList.get(j);
                            break;
                        }
                    }
                }
            }
        }


        final Toolbar toolbar = (Toolbar) findViewById(R.id.MotoStatusActivity_Toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.MotoStatusActivity_CollapsingToolbarLayout);


        collapsingToolbarLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        ImageView_MotoImage = (ImageView) findViewById(R.id.MotoStatusActivity_ImageView_MotoImage);
        ImageView_Background = (ImageView) findViewById(R.id.MotoStatusActivity_ImageView_Background);
        //TextView_ConnectionStatus = (TextView) findViewById(R.id.MotoStatusActivity_TextView_ConnectionStatus);
        TextView_MotonitoringStatus = (TextView) findViewById(R.id.MotoStatusActivity_TextView_MotonitoringStatus);
        TextView_SafetyLockStatus = (TextView) findViewById(R.id.MotoStatusActivity_TextView_SafetyLockStatus);
        TextView_ElectricalFlowStatus = (TextView) findViewById(R.id.MotoStatusActivity_TextView_ElectricalFlowStatus);
        Button_StartMonitoring = (Button) findViewById(R.id.MotoStatusActivity_Button_StartMonitoring);
        Button_OpenMap = (Button) findViewById(R.id.MotoStatusActivity_Button_OpenMap);

        ImageView_Background.setBackgroundColor(Color.argb(150, 255, 0, 0));
        if(moto!=null){
            this.setStatusFromMoto();
            collapsingToolbarLayout.setTitle(moto.getBrandAndLine());
            ImageView_MotoImage.setImageBitmap(moto.BitmapImage);
            Palette.from(moto.BitmapImage).generate(new Palette.PaletteAsyncListener() {
                @SuppressWarnings("ResourceType")
                @Override
                public void onGenerated(Palette palette) {
                    vibrantColor = palette.getVibrantColor(R.color.primary);
                    ImageView_Background.setBackgroundColor(Color.argb(100, Color.red(vibrantColor), Color.green(vibrantColor), Color.blue(vibrantColor)));
                    collapsingToolbarLayout.setContentScrimColor(vibrantColor);
                }
            });
            Button_StartMonitoring.setEnabled(true);
            Button_OpenMap.setEnabled(true);
        }else {
            collapsingToolbarLayout.setTitle("Moto");
            Button_StartMonitoring.setEnabled(false);
            Button_OpenMap.setEnabled(false);
        }

        if(moto != null){
            setStatusFromMoto();
        }
    }

    @Override
    protected void onStart() {
        Log.i(SISMO.LOG_TAG, "MotoStatusActivity.onStart");
        super.onStart();
        bindService(new Intent(this, MQTTService.class), serviceConnection, 0);
    }

    @Override
    protected void onResume() {
        Log.i(SISMO.LOG_TAG, "MotoStatusActivity.onResumen");
        super.onResume();
        SISMO.IsAppRunning = true;
        intentFilter = new IntentFilter();
        intentFilter.addAction(SISMO.MQTT.INTENT_ACTION_MESSAGE);
        intentFilter.addAction(SISMO.MQTT.INTENT_ACTION_RESPONSE);
        pushReceiver = new PushReceiver();
        registerReceiver(pushReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        Log.i(SISMO.LOG_TAG, "MotoStatusActivity.onPause");
        super.onPause();
        SISMO.IsAppRunning = false;
        unregisterReceiver(pushReceiver);
    }

    @Override
    protected void onStop() {
        Log.i(SISMO.LOG_TAG, "MotoStatusActivity.onStop");
        super.onStop();
        unbindService(serviceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_moto_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }*/
        return super.onOptionsItemSelected(item);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder){
            Log.i(SISMO.LOG_TAG, "MotoStatusActivity.ServiceConnection.onServiceConnected");
            mqttServiceMessenger = new Messenger(binder);
            //requestMotoStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(SISMO.LOG_TAG, "MotoStatusActivity.ServiceConnection.onServiceDisconnected");
        }
    };

    public void onCLickButtonGetMotoStatus(View view) {
        if (mqttServiceMessenger != null) {
            String mqttTopic = "/messages/to/" + SISMO.Username + "/motos/" + moto.Mac;
            String replyTo = "/messages/to/" + SISMO.Username + "/android/" + SISMO.DeviceId;
            JSONObject json = new JSONObject();
            try {
                json.put("replyTo", replyTo);
                json.put("action", "gs");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Message message = Message.obtain(null, SISMO.MQTT.ACTIONS.PUBLISH);
            Bundle data = new Bundle();
            data.putString(SISMO.MQTT.KEYS.TOPIC, mqttTopic);
            data.putString(SISMO.MQTT.KEYS.MESSAGE, json.toString());
            message.setData(data);
            message.replyTo = messageHandler;;
            Log.i(SISMO.LOG_TAG, "Sending message to publish");
            try {
                mqttServiceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(SISMO.LOG_TAG, "MotoStatusActivity.mqttServiceMessenger is null");
        }
        Toast.makeText(getApplicationContext(), "Mensaje enviado  a la moto.", Toast.LENGTH_SHORT).show();
    }

    public void onCLickButtonStartMonitoring(View view) {
        String text = Button_StartMonitoring.getText().toString();
        if(text.toLowerCase().equals("encender monitoreo")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setIcon(R.drawable.ic_question);
            builder.setTitle("Confirmacion");
            builder.setMessage("¿Desea que SISMO comience a monitorear esta moto?");
            builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    if (mqttServiceMessenger != null) {
                        Message message = Message.obtain(null, SISMO.MQTT.ACTIONS.PUBLISH);

                        String mqttTopic = "/messages/to/" + SISMO.Username + "/motos/" + moto.Mac;
                        String replyTo = "/messages/to/" + SISMO.Username + "/android/" + SISMO.DeviceId;
                        JSONObject json = new JSONObject();
                        try {
                            json.put("replyTo", replyTo);
                            json.put("action", "sm");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Bundle data = new Bundle();

                        data.putString(SISMO.MQTT.KEYS.TOPIC, mqttTopic);
                        data.putString(SISMO.MQTT.KEYS.MESSAGE, json.toString());
                        message.setData(data);
                        message.replyTo = messageHandler;;
                        Log.i(SISMO.LOG_TAG, "Sending message to publish");
                        try {
                            mqttServiceMessenger.send(message);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.i(SISMO.LOG_TAG, "MotoStatusActivity.mqttServiceMessenger is null");
                    }
                }
            });
            builder.setNeutralButton("Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
        }else{
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setIcon(R.drawable.ic_question);
            builder.setTitle("Confirmacion");
            builder.setMessage("¿Desea que SISMO deje de monitorear esta moto?");
            builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    if (mqttServiceMessenger != null) {
                        Message message = Message.obtain(null, SISMO.MQTT.ACTIONS.PUBLISH);

                        String mqttTopic = "/messages/to/" + SISMO.Username + "/motos/" + moto.Mac;
                        String replyTo = "/messages/to/" + SISMO.Username + "/android/" + SISMO.DeviceId;
                        JSONObject json = new JSONObject();
                        try {
                            json.put("replyTo", replyTo);
                            json.put("action", "em");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Bundle data = new Bundle();

                        data.putString(SISMO.MQTT.KEYS.TOPIC, mqttTopic);
                        data.putString(SISMO.MQTT.KEYS.MESSAGE, json.toString());
                        message.setData(data);
                        message.replyTo = messageHandler;
                        Log.i(SISMO.LOG_TAG, "Sending message to publish");
                        try {
                            mqttServiceMessenger.send(message);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.i(SISMO.LOG_TAG, "MotoStatusActivity.mqttServiceMessenger is null");
                    }
                }
            });
            builder.setNeutralButton("Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
        }
    }

    public void onCLickButtonRegisterAsStolen(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setIcon(R.drawable.ic_question);
        builder.setTitle("Registrar moto como robada");
        builder.setMessage("Esta accion registrara los datos de la moto y la informacion asociada a la ultima posicion de parqueo a nuestro registro de motos robadas.\n¿Desea registrar en el sistema esta moto como robada?.");
        builder.setPositiveButton("Registrar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                new RegisterAsStolen().execute(moto.Mac);
            }
        });
        builder.setNeutralButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    public void onCLickButtonOpenMap(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setIcon(R.drawable.ic_question);
        builder.setTitle("¿Que dese hacer?");
        builder.setMessage("Seleccione una de las siguientes opciones.\n1) Abrir el mapa y pedir la posicion de la moto.\n2) Solo abrir el map para rastrear la moto.");
        builder.setNegativeButton("Opcion 1", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mqttServiceMessenger != null) {
                    String mqttTopic = "/messages/to/" + SISMO.Username + "/motos/" + moto.Mac;
                    String replyTo = "/messages/to/" + SISMO.Username + "/android/" + SISMO.DeviceId;
                    JSONObject json = new JSONObject();
                    try {
                        json.put("replyTo", replyTo);
                        json.put("action", "gp");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Message message = Message.obtain(null, SISMO.MQTT.ACTIONS.PUBLISH);
                    Bundle data = new Bundle();
                    data.putString(SISMO.MQTT.KEYS.TOPIC, mqttTopic);
                    data.putString(SISMO.MQTT.KEYS.MESSAGE, json.toString());
                    message.setData(data);
                    message.replyTo = messageHandler;
                    ;
                    Log.i(SISMO.LOG_TAG, "Sending message to publish");
                    try {
                        mqttServiceMessenger.send(message);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.i(SISMO.LOG_TAG, "MotoStatusActivity.mqttServiceMessenger is null");
                }
                dialog.dismiss();
                Toast.makeText(getApplicationContext(), "Mensaje enviado  a la moto.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setPositiveButton("Opcion 2", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent(MotoStatusActivity.this, MapsActivity.class);
                i.putExtra("mac", moto.Mac);
                startActivity(i);
                dialog.dismiss();
            }
        });
        builder.setNeutralButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }


    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg)
        {
            Log.i(SISMO.LOG_TAG, "MotoStatusActivity.MessageHandler.handleMessage");
            switch (msg.what)
            {
                case SISMO.MQTT.ACTIONS.SUBSCRIBE: break;
                case SISMO.MQTT.ACTIONS.PUBLISH: {
                    Bundle data = msg.getData();
                    Boolean status = data.getBoolean("published");
                    if(status){
                        //Toast.makeText(getApplicationContext(), "Mensaje enviado  la moto.", Toast.LENGTH_SHORT).show();
                    }else{
                        String error = data.getString("error");
                        Toast.makeText(getApplicationContext(), "El mensaje no pudo ser enviado.\nError:"+ error, Toast.LENGTH_LONG).show();
                    }
                }

            }
        }
    }

    public class PushReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent i)
        {
            String topic = i.getStringExtra(SISMO.MQTT.KEYS.TOPIC);
            String message = i.getStringExtra(SISMO.MQTT.KEYS.MESSAGE);
            Log.i(SISMO.LOG_TAG, message);
            try {
                JSONObject json = new JSONObject(message);
                String type = json.getString("type");
                String mac = json.getString("mac");
                String brand = json.getString("brand");
                String line = json.getString("line");
                String plate = json.getString("plate");
                int length = SISMO.MotoList.size();
                Moto m = null;
                for(int j=0; j<length; j++){
                    Log.i(SISMO.LOG_TAG, String.valueOf(j));
                    if(SISMO.MotoList.get(j).Mac.equals(mac)){
                        m = SISMO.MotoList.get(j);
                        break;
                    }
                }
                if(type.equals("response")){
                    String action = json.getString("action");
                    if(action.equals("sm") ||  action.equals("em") || action.equals("gs")){
                        JSONObject info = json.getJSONObject("info");
                        String monitoringStatus = info.getString("monitoringStatus");
                        String safetyLockStatus = info.getString("safetyLockStatus");
                        String electricalFlowStatus = info.getString("electricalFlowStatus");
                        if(m != null){
                            m.MonitorinStatus = monitoringStatus;
                            m.SafetyLockStaus = safetyLockStatus;
                            m.ElectricalFlowStatus = electricalFlowStatus;
                            if(m.Mac.equals(moto.Mac)){
                                moto = m;
                                setStatusFromMoto();
                            }
                        }
                    }else if(action.equals("gp")){
                        JSONObject info = json.getJSONObject("info");
                        double latitude = info.getDouble("latitude");
                        double longitude = info.getDouble("longitude");
                        Intent intent = new Intent(MotoStatusActivity.this, MapsActivity.class);
                        intent.putExtra("mac", mac);
                        intent.putExtra("brand", brand);
                        intent.putExtra("line", line);
                        intent.putExtra("plate", plate);
                        intent.putExtra("presentLatitude", latitude);
                        intent.putExtra("presentLongitude", longitude);
                        startActivity(intent);
                    }
                }else if(type.equals("message")){
                    String subject = json.getString("subject");
                    if(subject.equals("safetyLockStatusChanged")){
                        Log.i(SISMO.LOG_TAG, "safetyLockStatusChanged");
                        JSONObject info = json.getJSONObject("info");
                        String safetyLockStatus = info.getString("safetyLockStatus");
                        if(m.Mac.equals(moto.Mac)){
                            moto.SafetyLockStaus = safetyLockStatus;
                            setStatusFromMoto();
                        }else{
                            m.SafetyLockStaus = safetyLockStatus;
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
                    }else if(subject.equals("motoMoved")){
                        Log.i(SISMO.LOG_TAG, "Moto moved");

                        JSONObject info = json.getJSONObject("info");
                        int distance = info.getInt("distance");
                        String motoName = "esta motocicleta";
                        if(!m.Mac.equals(moto.Mac)){
                            motoName = "la motocicleta "+moto.getBrandAndLine()+" con placa "+moto.Plate;
                        }
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
                                    Intent i = new Intent(MotoStatusActivity.this, MapsActivity.class);
                                    i.putExtra("mac", motoMac);
                                    i.putExtra("parkingLatitude", lat1);
                                    i.putExtra("parkingLongitude", lon1);
                                    i.putExtra("presentLatitude", lat2);
                                    i.putExtra("presentLongitude", lon2);
                                    startActivity(i);
                                    dialog.dismiss();
                                }
                            });
                            builder.setNeutralButton("Cerrar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                        }
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }else if(subject.equals("connectionStatusChanged")){
                        JSONObject info = json.getJSONObject("info");
                        String connectionStatus = info.getString("connectionStatus");
                        String motoName;
                        if(m.Mac.equals(moto.Mac)){
                            motoName = "esta motocicleta";
                        }else {
                            motoName = "la motocicleta "+moto.getBrandAndLine()+" con placa "+moto.Plate;
                        }
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
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void setStatusFromMoto(){
        Log.i(SISMO.LOG_TAG, "Setting names");

        TextView_SafetyLockStatus.setText(moto.SafetyLockStaus + " ");
        if(moto.SafetyLockStaus.equals("locked")){
            TextView_SafetyLockStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_padlock_green, 0);
            TextView_SafetyLockStatus.setText("Bloqueado");
        }else if(moto.SafetyLockStaus.equals("unlocked")){
            TextView_SafetyLockStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_padlock_red, 0);
            TextView_SafetyLockStatus.setText("Desbloqueado");
        }else{
            TextView_SafetyLockStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_unknown, 0);
            TextView_SafetyLockStatus.setText("Desconocido");
        }

        TextView_ElectricalFlowStatus.setText(moto.ElectricalFlowStatus + " ");
        if(moto.ElectricalFlowStatus.equals("locked")){
            TextView_ElectricalFlowStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_padlock_green, 0);
            TextView_ElectricalFlowStatus.setText("Bloqueado");
        }else if(moto.ElectricalFlowStatus.equals("unlocked")){
            TextView_ElectricalFlowStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_padlock_red, 0);
            TextView_ElectricalFlowStatus.setText("Desbloqueado");
        }else{
            TextView_ElectricalFlowStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_unknown, 0);
            TextView_ElectricalFlowStatus.setText("Desconocido");
        }

        TextView_MotonitoringStatus.setText(moto.MonitorinStatus+" ");
        if(moto.MonitorinStatus.equals("on")){
            Button_StartMonitoring.setText("Apagar monitoreo");
            Button_StartMonitoring.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ex_red, 0, 0, 0);
            TextView_MotonitoringStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_green, 0);
            TextView_MotonitoringStatus.setText("Encendido");
        }else{
            Button_StartMonitoring.setText("Encender monitoreo");
            Button_StartMonitoring.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_green, 0, 0, 0);
            TextView_MotonitoringStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_ex_red, 0);
            TextView_MotonitoringStatus.setText("Apagado");
        }
    }

    public class GetMotoStatus extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        public GetMotoStatus() {
            Log.i(SISMO.LOG_TAG, "HomeActivity.GetMotos");
            progressDialog = new ProgressDialog(MotoStatusActivity.this);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            progressDialog.setMessage("Obteniendo datos de la moto, por favor espere...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                Log.i(SISMO.LOG_TAG, "Getting motos");
                String mac = params[0];
                String url = SISMO.SISMO_API_SERVER_HOST+"/api/v1/motos/"+mac+"/status";
                HTTPClient httpClient = new HTTPClient(url);

                httpClient.setMethod("GET");
                String response = httpClient.makeRequest();
                Log.i(SISMO.LOG_TAG, response);
                JSONObject jsonObj = new JSONObject(response);
                int responseCode = jsonObj.getInt("code");
                String responseStatus = jsonObj.getString("status");
                if(responseCode == 200) {
                    JSONObject result = jsonObj.getJSONObject("result");
                    JSONObject status = result.getJSONObject("status");
                    String monitoringStatus = status.getString("monitoring");
                    String safetyLockStatus = status.getString("safetyLock");
                    String electricalFlowStatus = status.getString("electricalFlow");
                    moto.MonitorinStatus = monitoringStatus;
                    moto.SafetyLockStaus = safetyLockStatus;
                    moto.ElectricalFlowStatus = electricalFlowStatus;
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
            switch (result) {
                case "Ok" :
                    setStatusFromMoto();
                    break;
                case "Bad request" :
                    Toast.makeText(MotoStatusActivity.this, "Bad request", Toast.LENGTH_SHORT).show();
                    break;
                case "Unauthorized" :
                    Toast.makeText(MotoStatusActivity.this, "Invalid access token", Toast.LENGTH_SHORT).show();
                    break;
                case "Connection error" :
                    Toast.makeText(MotoStatusActivity.this, "Error tratando de conectarse con el servidor", Toast.LENGTH_SHORT).show();
                    break;
                case "Another error" :
                    Toast.makeText(MotoStatusActivity.this, "Algo salio mal", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    public class RegisterAsStolen extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        public RegisterAsStolen() {
            Log.i(SISMO.LOG_TAG, "HomeActivity.GetMotos");
            progressDialog = new ProgressDialog(MotoStatusActivity.this);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            progressDialog.setMessage("Registrando moto como robada, por favor espere...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                Log.i(SISMO.LOG_TAG, "Getting motos");
                String mac = params[0];
                String url = SISMO.SISMO_API_SERVER_HOST+"/api/v1/thefts/"+mac;
                HTTPClient httpClient = new HTTPClient(url);

                httpClient.setMethod("POST");
                String response = httpClient.makeRequest();
                Log.i(SISMO.LOG_TAG, response);
                JSONObject jsonObj = new JSONObject(response);
                int responseCode = jsonObj.getInt("code");
                String responseStatus = jsonObj.getString("status");
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
            switch (result) {
                case "Created" :
                    Toast.makeText(MotoStatusActivity.this, "Moto registrada exitosamente", Toast.LENGTH_SHORT).show();
                    break;
                case "Bad request" :
                    Toast.makeText(MotoStatusActivity.this, "Bad request", Toast.LENGTH_SHORT).show();
                    break;
                case "Unauthorized" :
                    Toast.makeText(MotoStatusActivity.this, "Invalid access token", Toast.LENGTH_SHORT).show();
                    break;
                case "Connection error" :
                    Toast.makeText(MotoStatusActivity.this, "Error tratando de conectarse con el servidor", Toast.LENGTH_SHORT).show();
                    break;
                case "Another error" :
                    Toast.makeText(MotoStatusActivity.this, "Algo salio mal", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

}
