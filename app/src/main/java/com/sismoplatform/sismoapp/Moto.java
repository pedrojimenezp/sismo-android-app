package com.sismoplatform.sismoapp;

import android.graphics.Bitmap;

/**
 * Created by pedro on 31/08/15.
 */
public class Moto {
    public String Mac;
    public String Image;
    public String ImageEncodeType;
    public Bitmap BitmapImage;
    public String Brand;
    public String Line;
    public String Plate;
    public int Model;
    public String Color;
    public int CylinderCapacity;
    public String MonitorinStatus = "off";
    public String SafetyLockStaus = "unlocked";
    public String ElectricalFlowStatus = "unlocked";
    public String GeneralStatus = "";

    public Moto(){
        this.Image = "";
        this.ImageEncodeType = "";
        this.Mac = "";
        this.Brand = "";
        this.Line = "";
        this.Plate = "";
        this.Color = "";
        this.CylinderCapacity = 0;
        this.Model = 0;
        this.BitmapImage = null;
    }

    public String getBrandAndLine(){
        return this.Brand + " " + this.Line;
    }
}

