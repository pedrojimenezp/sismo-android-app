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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttClient;


public class HomeActivity extends AppCompatActivity {

    private Messenger messenger = null;
    private final Messenger messageHandler = new Messenger(new MessageHandler());


    ViewPager viewPager;
    TabPagerAdapter tabPagerAdapter;
    ActionBar actionBar;
    TabLayout tabLayout;
    Menu menu;
    int RESULT_GALERY;
    private IntentFilter intentFilter = null;
    private PushReceiver pushReceiver;

    MqttClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        startService(new Intent(this, MQTTService.class));

        intentFilter = new IntentFilter();
        intentFilter.addAction(MainActivity.MQTT_ACTION);
        pushReceiver = new PushReceiver();
        registerReceiver(pushReceiver, intentFilter);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("SisMo App");
        setSupportActionBar(toolbar);

        this.viewPager = (ViewPager) findViewById(R.id.view_pager);
        this.setupViewPager(this.viewPager);

        this.tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        //this.tabLayout.setTabMode(TabLayout.MODE_FIXED);
        //this.tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        this.tabLayout.setupWithViewPager(this.viewPager);
        /*tabLayout.addTab(tabLayout.newTab().setText("MotosList"));
        tabLayout.addTab(tabLayout.newTab().setText("Notifications"));
        tabLayout.addTab(tabLayout.newTab().setText("Profile"));
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_moto_white);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_notification_white);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_user_white);*/



        //this.tabPagerAdapter = new TabPagerAdapter(getSupportFragmentManager());
        //viewPager.setAdapter(tabPagerAdapter);
        //viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int tabPosition = tab.getPosition();
                viewPager.setCurrentItem(tabPosition);
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
    protected void onStart()
    {
        super.onStart();
        bindService(new Intent(this, MQTTService.class), serviceConnection, 0);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        unbindService(serviceConnection);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(pushReceiver, intentFilter);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(pushReceiver);
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
            SharedPreferences sp = getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
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

    public void onClickMotoItemList(View view)  {
        Intent intent = new Intent(HomeActivity.this, MotoDetailsActivity.class);
        startActivity(intent);

    }

    public void onClickLoadImage(View view) throws Exception {
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, 0);

    }

    public void onClickSeeStatus(View view) throws Exception {
        Intent i = new Intent(HomeActivity.this, MotoStatusActivity.class);

        startActivity(i);

    }


    /*
    Methods to manage MQTT Service
     */

    public class PushReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent i)
        {
            String topic = i.getStringExtra(MQTTService.TOPIC);
            String message = i.getStringExtra(MQTTService.MESSAGE);
            int notificationId = i.getIntExtra(MQTTService.NOTIFICATION_ID, -1);
            Toast.makeText(context, "Push message received - " + topic + ":" + message, Toast.LENGTH_LONG).show();
            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.cancel(notificationId);
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder)
        {
            messenger = new Messenger(binder);
            Bundle data = new Bundle();
            data.putCharSequence(MQTTService.INTENT_ACTION, MainActivity.MQTT_ACTION);
            Message msg = Message.obtain(null, MQTTService.REGISTER);
            msg.setData(data);
            msg.replyTo = messageHandler;
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
        }
    };

    class MessageHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MQTTService.SUBSCRIBE: 	break;
                case MQTTService.PUBLISH:		break;
                case MQTTService.REGISTER:		break;
                default:
                    super.handleMessage(msg);
                    return;
            }

            Bundle b = msg.getData();
            if (b != null) {
                Boolean status = b.getBoolean(MQTTService.STATUS);
                Log.i(MainActivity.LOG_TAG, "Status from MQTT service on the case "+msg.what+": "+status.toString());
            }
        }
    }
}
