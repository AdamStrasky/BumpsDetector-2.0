package com.example.monikas.navigationapp;



/**
 * Created by Adam on 2.11.2016.
 */

public class Position {
    private float speed;
    private double latitude,longitude;


    public Position(float speed, double latitude, double longitude) {

        this.speed = speed;
        this.latitude = latitude;
        this.longitude = longitude;
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


}

