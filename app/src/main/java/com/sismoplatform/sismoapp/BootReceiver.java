package com.sismoplatform.sismoapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by pedro on 15/09/15.
 */
public class BootReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d(SISMO.LOG_TAG, intent.getAction());
        Log.d(getClass().getCanonicalName(), "onReceive");
        context.startService(new Intent(context, MQTTService.class));
    }
}
