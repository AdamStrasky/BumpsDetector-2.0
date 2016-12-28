package com.example.monikas.navigationapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;


public class AndroidGPSTrackingActivity extends Activity {

    Button btnShowLocation;

    // GPSTracker class
    GPSTracker gps;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);




        // show location button click event

        // create class object
        gps = new GPSTracker(AndroidGPSTrackingActivity.this);

        // check if GPS enabled
        if(gps.canGetLocation()){

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();

            // \n is for new line
            Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
        }else{
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }
        finish();



}}