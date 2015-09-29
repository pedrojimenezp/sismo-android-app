package com.sismoplatform.sismoapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;


public class MotosFragment extends Fragment {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private String[] motosArray = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private ArrayList<Moto> motoList;
    private List<String> motos;
    MqttClient client;

    public MotosFragment() {
        /* Required empty public constructor */
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //

        /*mAdapter = new MotosAdapter(motosArray, R.layout.moto_list_item, getContext());
        mRecyclerView.setAdapter(mAdapter);*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_motos, container, false);
        mRecyclerView = (RecyclerView)rootView.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        motoList = new ArrayList<Moto>();
        motoList.add(new Moto());
        motoList.add(new Moto());
        motoList.add(new Moto());
        motoList.add(new Moto());
        mAdapter = new MotosAdapter(motoList, R.layout.moto_list_item, getContext());
        mRecyclerView.setAdapter(mAdapter);
        return rootView;
    }


}
