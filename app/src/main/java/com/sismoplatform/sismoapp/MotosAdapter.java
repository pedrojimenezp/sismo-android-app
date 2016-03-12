package com.sismoplatform.sismoapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.ConnectException;
import java.util.ArrayList;

/**
 * Created by pedro on 27/08/15.
 */
public class MotosAdapter extends RecyclerView.Adapter<MotosAdapter.ViewHolder> {

    private ArrayList<Moto> motos;
    private int idLayout;
    public Context context;
    public Activity activity;

    public MotosAdapter(int rowLayout, Context context, Activity activity) {
        Log.i(SISMO.LOG_TAG, "MotosAdapter.MotosAdapter");
        this.motos = SISMO.MotoList;
        this.idLayout = rowLayout;
        this.context = context;
        this.activity = activity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        Log.i(SISMO.LOG_TAG, "MotosAdapter.onCreateViewHolder");
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(idLayout, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {
        Log.i(SISMO.LOG_TAG, "MotosAdapter.onBindViewHolder");
        Moto moto = motos.get(i);
        viewHolder.context = context;
        viewHolder.MotoObject = moto;
        viewHolder.ImageView_MotoImage.setImageBitmap(moto.BitmapImage);
        viewHolder.TextView_Color.setText(moto.Color);
        
        viewHolder.TextView_BrandAndLine.setText(moto.getBrandAndLine());
        viewHolder.TextView_Plate.setText(moto.Plate);

        if(moto.MonitorinStatus.equals("on")){
            viewHolder.Button_StartMonitoring.setText("Apagar monitoreo");
            viewHolder.Button_StartMonitoring.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ex_red, 0, 0, 0);
        }else{
            viewHolder.Button_StartMonitoring.setText("Encender monitoreo");
            viewHolder.Button_StartMonitoring.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_green, 0, 0, 0);
        }

        if(moto.MonitorinStatus.equals("on")){
            viewHolder.RelativeLayout_HeaderTitle.setBackgroundResource(R.color.green);
        }else{
            viewHolder.RelativeLayout_HeaderTitle.setBackgroundResource(R.color.primary);
        }
    }

    @Override
    public int getItemCount() {
        return motos == null ? 0 : motos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public Context context;
        public Moto MotoObject;
        public RelativeLayout RelativeLayout_HeaderTitle;
        public TextView TextView_BrandAndLine;
        public TextView TextView_Plate;
        public TextView TextView_Color;
        public ImageView ImageView_MotoImage;
        public Button Button_UpdateData;
        public Button Button_StartMonitoring;
        public Button Button_DeleteMoto;

        public ViewHolder(View itemView) {
            super(itemView);
            RelativeLayout_HeaderTitle = (RelativeLayout) itemView.findViewById(R.id.MotoListItem_RelativeLayout_HeaderTitle);
            TextView_BrandAndLine = (TextView) itemView.findViewById(R.id.MotoListItem_TextView_BrandAndLine);
            TextView_Plate = (TextView) itemView.findViewById(R.id.MotoListItem_TextView_Plate);
            TextView_Color = (TextView) itemView.findViewById(R.id.MotoListItem_TextView_Color);
            ImageView_MotoImage = (ImageView) itemView.findViewById(R.id.MotoListItem_ImageView_MotoImage);

            Button_UpdateData = (Button)itemView.findViewById(R.id.MotoListItem_Button_UpdateData);

            Button_UpdateData.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    Intent intent = new Intent(context, MotoDetailsActivity.class);
                    intent.putExtra("listIndex", position);
                    context.startActivity(intent);
                }
            });

            Button_StartMonitoring = (Button)itemView.findViewById(R.id.MotoListItem_Button_StartMonitoring);

            Button_StartMonitoring.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
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
                                HomeActivity ha = ((HomeActivity) activity);
                                Messenger messenger = ha.mqttServiceMessenger;
                                if (messenger != null) {
                                    Message message = Message.obtain(null, SISMO.MQTT.ACTIONS.PUBLISH);

                                    String mqttTopic = "/messages/to/" + SISMO.Username + "/motos/" + MotoObject.Mac;
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
                                    message.replyTo = ha.messageHandler;
                                    Log.i(SISMO.LOG_TAG, "Sending message to publish");
                                    try {
                                        messenger.send(message);
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    Log.i(SISMO.LOG_TAG, "HomeActivity.mqttServiceMessenger is null");
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
                                HomeActivity ha = ((HomeActivity)activity);
                                Messenger messenger = ha.mqttServiceMessenger;
                                if(messenger != null){
                                    Message message = Message.obtain(null,SISMO.MQTT.ACTIONS.PUBLISH);

                                    String mqttTopic = "/messages/to/"+SISMO.Username+"/motos/"+MotoObject.Mac;
                                    String replyTo = "/messages/to/"+SISMO.Username+"/android/"+SISMO.DeviceId;
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
                                    message.replyTo = ha.messageHandler;
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
            });
            Button_DeleteMoto = (Button)itemView.findViewById(R.id.MotoListItem_Button_Delete);
            Button_DeleteMoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setIcon(R.drawable.ic_question);
                    builder.setTitle("Confirmacion");
                    builder.setMessage("¿Esta seguro que desea elliminar esta moto?");
                    builder.setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            new DeleteMoto().execute(MotoObject.Mac);
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
            });

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Log.i(SISMO.LOG_TAG, "Button_SeeStatus.OnClick");
            int position = getAdapterPosition();
            Intent intent = new Intent(context, MotoStatusActivity.class);
            intent.putExtra("listIndex", position);
            activity.startActivity(intent);
        }
    }

    public class DeleteMoto extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        public DeleteMoto() {
            Log.i(SISMO.LOG_TAG, "HomeActivity.GetMotos");
            progressDialog = new ProgressDialog(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Eliminado la moto, por favor espere...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                Log.i(SISMO.LOG_TAG, "Deleting moto");
                String mac = params[0];
                String url = SISMO.SISMO_API_SERVER_HOST+"/api/v1/motos/"+mac;
                HTTPClient httpClient = new HTTPClient(url);

                httpClient.setMethod("DELETE");
                String response = httpClient.makeRequest();

                JSONObject jsonObj = new JSONObject(response);
                int responseCode = jsonObj.getInt("code");
                if(responseCode == 200){
                    int length = motos.size();
                    if(length > 0) {
                        for(int i=0; i<length; i++){
                            if(motos.get(i).Mac.equals(mac)){
                                motos.remove(i);
                                break;
                            }
                        }
                    }
                }
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
                case "Ok" :
                    HomeActivity HA = (HomeActivity)activity;
                    HA.adapter = new MotosAdapter(R.layout.moto_list_item, context, activity);
                    HA.recyclerView.setAdapter(HA.adapter);
                    break;
                case "Bad request" :
                    Toast.makeText(context, "Bad request", Toast.LENGTH_SHORT).show();
                    break;
                case "Unauthorized" :
                    Toast.makeText(context, "Invalid access token", Toast.LENGTH_SHORT).show();
                    break;
                case "Connection error" :
                    Toast.makeText(context, "Error tratando de conectarse con el servidor", Toast.LENGTH_SHORT).show();
                    break;
                case "Another error" :
                    Toast.makeText(context, "Algo salio mal", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

}
