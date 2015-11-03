package com.sismoplatform.sismoapp;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements LocationListener {
    private IntentFilter intentFilter = null;
    private PushReceiver pushReceiver;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    double parkingLatitude = 0;
    double parkingLongitude = 0;
    double presentLatitude = 0;
    double presentLongitude = 0;

    Marker presentMotoPosition;
    Marker myPosition;


    LocationManager mlocManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Bundle bundle = getIntent().getExtras();

        parkingLatitude = bundle.getDouble("latitude1");
        parkingLongitude = bundle.getDouble("longitude1");
        presentLatitude = bundle.getDouble("latitude2");
        presentLongitude = bundle.getDouble("longitude2");
        setUpMapIfNeeded();
        try{
            mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if(mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        Log.i(SISMO.LOG_TAG, "MapsActivity.onResumen");
        super.onResume();
        SISMO.IsAppRunning = true;
        setUpMapIfNeeded();
        intentFilter = new IntentFilter();
        intentFilter.addAction(SISMO.MQTT.INTENT_ACTION_UPDATE);
        pushReceiver = new PushReceiver();
        registerReceiver(pushReceiver, intentFilter);
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.i(SISMO.LOG_TAG, "MapsActivity.onPause");
        SISMO.IsAppRunning = false;
        unregisterReceiver(pushReceiver);
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        Log.i(SISMO.LOG_TAG, "Setup");
        presentMotoPosition = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(presentLatitude, presentLongitude))
                .title("Motorcycle present position")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        myPosition = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(8.9399733, -75.4404976))
                .title("My Position")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(parkingLatitude, parkingLongitude))
                .title("Motorcycle parking position")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        CameraUpdate center= CameraUpdateFactory.newLatLng(new LatLng(presentLatitude, presentLongitude));

        CameraUpdate zoom=CameraUpdateFactory.zoomTo(13);

        mMap.moveCamera(center);
        mMap.animateCamera(zoom);
    }

    @Override
    public void onLocationChanged(Location loc) {
        //Toast.makeText(MapsActivity.this, "Lat :" + loc.getLatitude() + ", lon: " + loc.getLongitude(), Toast.LENGTH_LONG).show();
        moveMyMaker(loc.getLatitude(), loc.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //Toast.makeText(MapsActivity.this, "Status changed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(MapsActivity.this, "Provider enable" + provider, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(MapsActivity.this, "Provider disabled" + provider, Toast.LENGTH_LONG).show();
    }

    public class PushReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent i)
        {
            String topic = i.getStringExtra(SISMO.MQTT.KEYS.TOPIC);
            String message = i.getStringExtra(SISMO.MQTT.KEYS.MESSAGE);
            Log.i(SISMO.LOG_TAG, message);
            //int notificationId = i.getIntExtra(SISMO.MQTT.KEYS.NOTIFICATION_ID, -1);

            try {
                JSONObject json = new JSONObject(message);
                String type = json.getString("type");
                if(type.equals("update")){
                    String mac = json.getString("mac");

                    JSONObject positions = json.getJSONObject("info");
                    JSONObject presentPosition = positions.getJSONObject("presentPosition");
                    double latitude2 = presentPosition.getDouble("latitude");
                    double longitude2 = presentPosition.getDouble("longitude");

                    moveMotoMaker(latitude2, longitude2);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void moveMotoMaker(double lat, double lon){
        if(presentMotoPosition != null){
            presentMotoPosition.remove();
        }
        presentMotoPosition = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lon))
                .title("Motorcycle present position")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        CameraUpdate center= CameraUpdateFactory.newLatLng(new LatLng(lat, lon));
        //mMap.moveCamera(center);
    }

    public void moveMyMaker(double lat, double lon){
        if(myPosition != null){
            myPosition.remove();
        }
        myPosition = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lon))
                .title("My Position")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
    }
}
