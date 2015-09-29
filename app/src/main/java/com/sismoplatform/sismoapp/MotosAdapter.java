package com.sismoplatform.sismoapp;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pedro on 27/08/15.
 */
public class MotosAdapter extends RecyclerView.Adapter<MotosAdapter.ViewHolder>{

    private ArrayList<Moto> motos;
    private int rowLayout;
    private Context mContext;


    public MotosAdapter(ArrayList<Moto> motos, int rowLayout, Context context) {
        this.motos = motos;
        this.rowLayout = rowLayout;
        this.mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(rowLayout, viewGroup, false);
        return new ViewHolder(v);
    }


    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        //Integer inti  = integerList.get(i);
        //viewHolder.countryName.setText(country.name);
        //viewHolder.countryImage.setImageDrawable(mContext.getDrawable(country.getImageResourceId(mContext)));
    }

    @Override
    public int getItemCount() {
        return motos == null ? 0 : motos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView MotoBrand;
        public TextView MotoPlate;
        public TextView MotoColor;
        public ImageView MotoImage;

        public ViewHolder(View itemView) {
            super(itemView);
            MotoBrand = (TextView) itemView.findViewById(R.id.MotoBrand);
            MotoBrand.setText("Yamaha FZcd Git  ");
            MotoPlate.setText("AAA00");
            MotoColor.setText("Rojo");
            //countryImage = (ImageView)itemView.findViewById(R.id.countryImage);
        }

    }
}
