package com.example.monikas.navigationapp;


import java.util.Date;

import static android.R.attr.data;

/**
 * Created by Adam on 2.11.2016.
 */

public class Position {
    private float speed;
    private double latitude,longitude;
    private long time;


    public Position(float speed, double latitude, double longitude, long time) {

        this.speed = speed;
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
    }

    public float getSpeed() {
        return speed;
    }

    public double getLatitude()  {
        return latitude;
    }
    public double getLongitude()  {
        return longitude;
    }


    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}

