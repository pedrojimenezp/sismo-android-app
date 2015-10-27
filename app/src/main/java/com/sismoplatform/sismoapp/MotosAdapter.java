package com.sismoplatform.sismoapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

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
            viewHolder.Button_StartMonitoring.setText("End monitoring");
        }else{
            viewHolder.Button_StartMonitoring.setText("Start monitoring");
        }
    }

    @Override
    public int getItemCount() {
        return motos == null ? 0 : motos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public Context context;
        public Moto MotoObject;
        public TextView TextView_BrandAndLine;
        public TextView TextView_Plate;
        public TextView TextView_Color;
        public ImageView ImageView_MotoImage;
        public Button Button_SeeStatus;
        public Button Button_StartMonitoring;
        //public CardView CardViewMoto;

        public ViewHolder(View itemView) {
            super(itemView);
            TextView_BrandAndLine = (TextView) itemView.findViewById(R.id.MotoListItem_TextView_BrandAndLine);
            TextView_Plate = (TextView) itemView.findViewById(R.id.MotoListItem_TextView_Plate);
            TextView_Color = (TextView) itemView.findViewById(R.id.MotoListItem_TextView_Color);
            ImageView_MotoImage = (ImageView) itemView.findViewById(R.id.MotoListItem_ImageView_MotoImage);

            Button_SeeStatus = (Button)itemView.findViewById(R.id.MotoListItem_Button_SeeStatus);
            Button_SeeStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    Intent intent = new Intent(context, MotoStatusActivity.class);
                    intent.putExtra("listIndex", position);
                    context.startActivity(intent);
                }
            });

            Button_StartMonitoring = (Button)itemView.findViewById(R.id.MotoListItem_Button_StartMonitoring);
            Button_StartMonitoring.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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
                    HomeActivity ha = ((HomeActivity)activity);
                    Messenger messenger = ha.mqttServiceMessenger;
                    if(messenger != null){
                        Message message = Message.obtain(null,SISMO.MQTT.ACTIONS.PUBLISH);

                        String mqttTopic = "/messages/to/"+SISMO.Username+"/apps/motos/"+MotoObject.Mac;
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
                    //int position = getAdapterPosition();
                    //Intent intent = new Intent(context, MotoStatusActivity.class);
                    //intent.putExtra("listIndex", position);
                    //context.startActivity(intent);
                }
            });

            //CardViewMoto = (CardView) itemView.findViewById(R.id.MotoListItem_CardView_Moto);
            itemView.setOnClickListener(this);


        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            Intent intent = new Intent(context, MotoDetailsActivity.class);
            intent.putExtra("listIndex", position);
            context.startActivity(intent);
        }
    }
}
