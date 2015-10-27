package com.sismoplatform.sismoapp;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class MotoStatusActivity extends AppCompatActivity {
    public Messenger mqttServiceMessenger = null;
    public Messenger messageHandler = new Messenger(new MessageHandler());

    CollapsingToolbarLayout collapsingToolbarLayout;

    int vibrantColor = R.color.primary;

    Moto moto;

    ImageView ImageView_MotoImage;
    ImageView ImageView_Background;
    TextView TextView_MotonitoringStatus;
    TextView TextView_SafetyLockStatus;
    TextView TextView_ElectricalFlowStatus;
    Button Button_StartMonitoring;

    private IntentFilter intentFilter = null;
    private PushReceiver pushReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(SISMO.LOG_TAG, "MotoStatusActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moto_status);

        intentFilter = new IntentFilter();
        intentFilter.addAction(SISMO.MQTT.INTENT_ACTION_WARNING);
        intentFilter.addAction(SISMO.MQTT.INTENT_ACTION_RESPONSE);
        pushReceiver = new PushReceiver();
        registerReceiver(pushReceiver, intentFilter);

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            int index = extras.getInt("listIndex");

            moto = SISMO.MotoList.get(index);
        }else{
            moto = null;
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
        TextView_MotonitoringStatus = (TextView) findViewById(R.id.MotoStatusActivity_TextView_MotonitoringStatus);
        TextView_SafetyLockStatus = (TextView) findViewById(R.id.MotoStatusActivity_TextView_SafetyLockStatus);
        TextView_ElectricalFlowStatus = (TextView) findViewById(R.id.MotoStatusActivity_TextView_ElectricalFlowStatus);
        Button_StartMonitoring = (Button) findViewById(R.id.MotoStatusActivity_Button_StartMonitoring);

        ImageView_Background.setBackgroundColor(Color.argb(150, 255, 0, 0));
        if(moto!=null){
            this.setNamesFromMoto();
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

        }else {
            collapsingToolbarLayout.setTitle("Moto");
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
        registerReceiver(pushReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        Log.i(SISMO.LOG_TAG, "MotoStatusActivity.onPause");
        super.onPause();
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
        getMenuInflater().inflate(R.menu.menu_moto_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClickGetLocation(View view) throws Exception {
        //MessageHandler messageHandler = new MQTTService.MessageHandler();
        Log.i(SISMO.LOG_TAG, "MotoStatusActivity.onClickGetLocation");
        Message message = Message.obtain(null,SISMO.MQTT.ACTIONS.PUBLISH);
        Bundle data = new Bundle();
        data.putString(SISMO.MQTT.KEYS.TOPIC, "/test");
        data.putString(SISMO.MQTT.KEYS.MESSAGE, "Hello world");
        message.setData(data);
        //message.replyTo = messageHandler;
        Log.i(SISMO.LOG_TAG, message.toString());
        try{
            mqttServiceMessenger.send(message);
        }catch (RemoteException e){
            e.printStackTrace();
        }
        //Intent i = new Intent(MotoStatusActivity.this, MapsActivity.class);

        //startActivity(i);

    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder){
            Log.i(SISMO.LOG_TAG, "MotoStatusActivity.ServiceConnection.onServiceConnected");
            mqttServiceMessenger = new Messenger(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(SISMO.LOG_TAG, "MotoStatusActivity.ServiceConnection.onServiceDisconnected");
        }
    };

    public void onCLickButtonStartMonitoring(View view) {
        Log.i(SISMO.LOG_TAG, "Starting monitoring");
        CharSequence cs = Button_StartMonitoring.getText();
        String action = "startMonitoring";
        if(cs != null){
            String text = cs.toString();
            Log.i(SISMO.LOG_TAG, text);
            if(!text.toLowerCase().equals("start monitoring")){
                action = "endMonitoring";
            }
        }
        Messenger messenger = mqttServiceMessenger;
        if(messenger != null){
            Message message = Message.obtain(null,SISMO.MQTT.ACTIONS.PUBLISH);

            String mqttTopic = "/messages/to/"+SISMO.Username+"/apps/motos/"+moto.Mac;
            String replyTo = "/messages/to/"+SISMO.Username+"/apps/android/"+SISMO.DeviceId;
            JSONObject json = new JSONObject();
            try {
                json.put("replyTo", replyTo);
                json.put("action", action);
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
                messenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }else{
            Log.i(SISMO.LOG_TAG, "HomeActivity.mqttServiceMessenger is null");
        }
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
                        Toast.makeText(getApplicationContext(), "Published:" + String.valueOf(status), Toast.LENGTH_SHORT).show();
                    }else{
                        String error = data.getString("error");
                        Toast.makeText(getApplicationContext(), "Error:"+ error, Toast.LENGTH_SHORT).show();
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
            int notificationId = i.getIntExtra(SISMO.MQTT.KEYS.NOTIFICATION_ID, -1);

            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.cancel(notificationId);

            try {
                JSONObject json = new JSONObject(message);
                String type = json.getString("type");
                if(type.equals("response")){
                    String mac = json.getString("mac");
                    String action = json.getString("action");
                    if(action.equals("startMonitoring") ||  action.equals("endMonitoring")){
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
                                moto = SISMO.MotoList.get(j);
                                setNamesFromMoto();
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

            Toast.makeText(context, "Push message received - " + topic + ":" + message, Toast.LENGTH_LONG).show();

        }
    }

    public void setNamesFromMoto(){
        TextView_MotonitoringStatus.setText(moto.MonitorinStatus);
        TextView_SafetyLockStatus.setText(moto.SafetyLockStaus);
        TextView_ElectricalFlowStatus.setText(moto.ElectricalFlowStatus);
        if(moto.MonitorinStatus.equals("on")){
            Button_StartMonitoring.setText("End monitoring");
        }else{
            Button_StartMonitoring.setText("Start monitoring");
        }
    }

}
