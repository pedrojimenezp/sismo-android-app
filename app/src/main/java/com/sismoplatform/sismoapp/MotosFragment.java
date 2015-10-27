package com.sismoplatform.sismoapp;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.ConnectException;
import java.util.ArrayList;


public class MotosFragment extends Fragment {
    public Context applicationContext;
    public Activity activity;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;

    private IntentFilter intentFilter = null;
    private PushReceiver pushReceiver;

    private boolean register;

    public MotosFragment() {
        /* Required empty public constructor */
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(SISMO.LOG_TAG, "MotosFragment.onCreate");
        this.register = false;
        activity = this.getActivity();
        applicationContext = this.getActivity().getApplicationContext();
        setHasOptionsMenu(true);

        intentFilter = new IntentFilter();
        intentFilter.addAction(SISMO.MQTT.INTENT_ACTION_WARNING);
        intentFilter.addAction(SISMO.MQTT.INTENT_ACTION_RESPONSE);
        pushReceiver = new PushReceiver();

        getActivity().registerReceiver(pushReceiver, intentFilter);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(SISMO.LOG_TAG, "MotosFragment.onStart");
        if(SISMO.MotoList != null && SISMO.MotoList.size() > 0){
            adapter = new MotosAdapter(R.layout.moto_list_item, getContext(), getActivity());
            recyclerView.setAdapter(adapter);

        }else {
            GetMotos getMotos = new GetMotos();
            getMotos.execute();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(SISMO.LOG_TAG, "MotosFragment.onResume");
        getActivity().registerReceiver(pushReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(SISMO.LOG_TAG, "MotosFragment.onPause");
        getActivity().unregisterReceiver(pushReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(SISMO.LOG_TAG, "MotosFragment.onStop");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(SISMO.LOG_TAG, "MotosFragment.onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_motos, container, false);
        recyclerView = (RecyclerView)rootView.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());


        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_load_motos){
            this.register = true;
            GetMotos getMotos = new GetMotos();
            getMotos.execute();
        }
        return super.onOptionsItemSelected(item);
    }

    public class GetMotos extends AsyncTask<Void, Void, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            progressDialog = new ProgressDialog(activity);
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
                //System.out.println(response);

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
                                HomeActivity ha = ((HomeActivity) getActivity());
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
                    adapter = new MotosAdapter(R.layout.moto_list_item, getContext(), activity);
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
            int notificationId = i.getIntExtra(SISMO.MQTT.KEYS.NOTIFICATION_ID, -1);

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
                                adapter = new MotosAdapter(R.layout.moto_list_item, getContext(), activity);
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

            Toast.makeText(context, "Push message received - " + topic + ":" + message, Toast.LENGTH_LONG).show();

        }
    }
}
