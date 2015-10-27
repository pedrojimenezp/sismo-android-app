package com.sismoplatform.sismoapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Toast.makeText(this, deviceId, Toast.LENGTH_LONG).show();

        this.sharedPreferences = getSharedPreferences(SISMO.SHARED_PREFERENCES, Context.MODE_PRIVATE);

        SISMO.DeviceId = this.sharedPreferences.getString("deviceId", "");
        if(SISMO.DeviceId.isEmpty()){
            Log.i(SISMO.LOG_TAG, "Getting deviceId from settings");
            String deviceId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            SISMO.DeviceId = deviceId;
            SharedPreferences.Editor editor = this.sharedPreferences.edit();
            editor.putString("deviceId", deviceId);
            editor.apply();
        }

        SISMO.AccessToken = this.sharedPreferences.getString("accessToken", "");
        SISMO.RefreshToken = this.sharedPreferences.getString("refreshToken", "");
        SISMO.Username = this.sharedPreferences.getString("username", "");

        if(SISMO.AccessToken != ""){
            Handler mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                }
            }, 1000);
        }else{
            Handler mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }

            }, 1000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
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
}
