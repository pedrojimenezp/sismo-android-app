package com.sismoplatform.sismoapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
    private GoogleMap map; // Might be null if Google Play services APK is not available.

    Marker parkingMotoPosition = null;
    Marker presentMotoPosition;
    Marker myPosition;

    String MAC = "";
    String brand = "";
    String line = "";
    String plate = "";

    LocationManager locationManager;

    boolean isCameraMoved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(SISMO.LOG_TAG, "MapsActivity.onCreate");
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        Bundle bundle = getIntent().getExtras();

        if(bundle != null){
            MAC = bundle.getString("mac", "");
            brand = bundle.getString("brand", "");
            line = bundle.getString("line", "");
            plate = bundle.getString("plate", "");
            double lat1 = bundle.getDouble("parkingLatitude", 0);
            double lon1 = bundle.getDouble("parkingLongitude", 0);
            if(lat1 != 0 && lon1 != 0) {
                setParkingMarker(lat1, lon1);
            }
            double lat2 = bundle.getDouble("presentLatitude", 0);
            double lon2 = bundle.getDouble("presentLongitude", 0);
            if(lat2 != 0 && lon2 != 0) {
                setMotoMaker(lat2, lon2);
                moveCamera(lat2, lon2);
            }
        }

        try{
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
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
        intentFilter.addAction(SISMO.MQTT.INTENT_ACTION_MESSAGE);
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
        if (map == null) {
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        }
    }

    @Override
    public void onLocationChanged(Location loc) {
        setMyMaker(loc.getLatitude(), loc.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    public class PushReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent i)
        {
            String topic = i.getStringExtra(SISMO.MQTT.KEYS.TOPIC);
            String message = i.getStringExtra(SISMO.MQTT.KEYS.MESSAGE);
            Log.i(SISMO.LOG_TAG, "Message onReceive "+message);

            try {
                JSONObject json = new JSONObject(message);
                String type = json.getString("type");
                if(type.equals("message")){
                    String mac = json.getString("mac");
                    if(mac.equals(MAC)){
                        brand = json.getString("brand");
                        line = json.getString("line");
                        plate = json.getString("plate");
                        String subject = json.getString("subject");
                        if(subject.equals("positionChanged")){
                            Log.i(SISMO.LOG_TAG, "Updating map");
                            JSONObject positions = json.getJSONObject("info");
                            JSONObject parkingPosition = positions.getJSONObject("parkingPosition");
                            double lat1 = parkingPosition.getDouble("latitude");
                            double lon1 = parkingPosition.getDouble("longitude");
                            if(parkingMotoPosition == null) {
                                setParkingMarker(lat1, lon1);
                            }
                            JSONObject presentPosition = positions.getJSONObject("presentPosition");
                            double lat2 = presentPosition.getDouble("latitude");
                            double lon2 = presentPosition.getDouble("longitude");
                            setMotoMaker(lat2, lon2);
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void moveCamera(double lat, double lon) {
        CameraUpdate center= CameraUpdateFactory.newLatLng(new LatLng(lat, lon));
        CameraUpdate zoom=CameraUpdateFactory.zoomTo(15);
        map.moveCamera(center);
        map.animateCamera(zoom);
    }

    public void setMotoMaker(double lat, double lon){
        Log.i(SISMO.LOG_TAG, "Setting moto marker");
        if(presentMotoPosition != null){
            presentMotoPosition.remove();
        }
        presentMotoPosition = map.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lon))
                .title("Posicion actual de la moto "+brand+" "+line+" "+plate)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        if(!isCameraMoved){
            moveCamera(lat,lon);
            isCameraMoved = true;
        }
    }

    public void setMyMaker(double lat, double lon){
        if(myPosition != null){
            myPosition.remove();
        }
        myPosition = map.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lon))
                .title("Mi posicion")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        if(!isCameraMoved){
            moveCamera(lat,lon);
            isCameraMoved = true;
        }
    }

    public void setParkingMarker(double lat, double lon){
        if(parkingMotoPosition != null) {
            parkingMotoPosition.remove();
        }
        parkingMotoPosition = map.addMarker(new MarkerOptions()
                    .position(new LatLng(lat, lon))
                    .title("Posicion en donde se parqueo la moto")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        if(!isCameraMoved){
            moveCamera(lat,lon);
            isCameraMoved = true;
        }
    }
}