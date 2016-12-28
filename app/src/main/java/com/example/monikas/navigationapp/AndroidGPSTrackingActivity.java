package com.example.monikas.navigationapp;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import java.math.BigDecimal;


public class AndroidGPSTrackingActivity extends Activity {

    SQLiteDatabase sb;
    DatabaseOpenHelper databaseHelper;

    // GPSTracker class
    GPSTracker gps;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String query="";
        if (getIntent().getAction() != null && getIntent().getAction().equals("com.google.android.gms.actions.SEARCH_ACTION")) {
            query = getIntent().getStringExtra(SearchManager.QUERY);
            Log.e("Query:",query);   //query is the search word
        }


        // show location button click event

        // create class object
        initialization_database();
        gps = new GPSTracker(AndroidGPSTrackingActivity.this);

        // check if GPS enabled
        if(gps.canGetLocation()){

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();

            // \n is for new line


            Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
            BigDecimal bd = new BigDecimal(Float.toString(6));
            bd = bd.setScale(6, BigDecimal.ROUND_HALF_UP);

            ContentValues contentValues = new ContentValues();
            contentValues.put(Provider.new_bumps.LATITUDE, latitude);
            contentValues.put(Provider.new_bumps.LONGTITUDE, longitude);
            contentValues.put(Provider.new_bumps.MANUAL, 1);
            contentValues.put(Provider.new_bumps.INTENSITY, String.valueOf(bd));
            sb.insert(Provider.new_bumps.TABLE_NAME_NEW_BUMPS, null, contentValues);



        }else{
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }
        finish();



    }

    public void initialization_database(){
        // inicializacia databazy
        databaseHelper = new DatabaseOpenHelper(this);
        sb = databaseHelper.getWritableDatabase();
    }


}