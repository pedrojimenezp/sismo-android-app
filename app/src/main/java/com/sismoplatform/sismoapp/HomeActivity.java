package com.sismoplatform.sismoapp;

import android.app.ActionBar;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttClient;


public class HomeActivity extends AppCompatActivity {
    public Messenger mqttServiceMessenger = null;
    public Messenger messageHandler = new Messenger(new MessageHandler());

    ViewPager viewPager;
    TabLayout tabLayout;
    private IntentFilter intentFilter = null;
    private PushReceiver pushReceiver;

    MqttClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(SISMO.LOG_TAG, "HomeActivity.onCreate");
        setContentView(R.layout.activity_home);

        startService(new Intent(this, MQTTService.class));

        intentFilter = new IntentFilter();
        intentFilter.addAction(SISMO.MQTT.INTENT_ACTION_WARNING);
        intentFilter.addAction(SISMO.MQTT.INTENT_ACTION_RESPONSE);
        pushReceiver = new PushReceiver();
        registerReceiver(pushReceiver, intentFilter);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("SisMo App");
        setSupportActionBar(toolbar);

        this.viewPager = (ViewPager) findViewById(R.id.view_pager);
        this.setupViewPager(this.viewPager);

        this.tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        this.tabLayout.setupWithViewPager(this.viewPager);

        this.tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int tabPosition = tab.getPosition();
                viewPager.setCurrentItem(tabPosition);
                //Log.i(SISMO.LOG_TAG, "New Tab selected");
                /*switch(tabPosition){
                    case 0:
                        getMenuInflater().inflate(R.menu.menu_motos, menu);
                    case 1:
                        getMenuInflater().inflate(R.menu.menu_motos, menu);
                    case 2:
                        getMenuInflater().inflate(R.menu.menu_profile, menu);
                }*/
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(SISMO.LOG_TAG, "HomeActivity.onStart");
        bindService(new Intent(this, MQTTService.class), serviceConnection, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(SISMO.LOG_TAG, "HomeActivity.onResumen");
        registerReceiver(pushReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(SISMO.LOG_TAG, "HomeActivity.onPause");
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

        //noinspection SimplifiableIfStatement
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupViewPager(ViewPager viewPager) {
        TabPagerAdapter adapter = new TabPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new MotosFragment(), "Motos");
        adapter.addFrag(new NotificationsFragment(), "Notifications");
        adapter.addFrag(new ProfileFragment(), "Profile");
        viewPager.setAdapter(adapter);
    }

    /*
    Methods to manage MQTT Service
     */

    public class PushReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent i)
        {
            String topic = i.getStringExtra(SISMO.MQTT.KEYS.TOPIC);
            String message = i.getStringExtra(SISMO.MQTT.KEYS.MESSAGE);
            int notificationId = i.getIntExtra(SISMO.MQTT.KEYS.NOTIFICATION_ID, -1);

            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.cancel(notificationId);

            //Toast.makeText(context, "Push message received - " + topic + ":" + message, Toast.LENGTH_LONG).show();

        }
    }

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
                        Toast.makeText(getApplicationContext(), "Published:"+ String.valueOf(status), Toast.LENGTH_SHORT).show();
                    }else{
                        String error = data.getString("error");
                        Toast.makeText(getApplicationContext(), "Error:"+ error, Toast.LENGTH_SHORT).show();
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
}
