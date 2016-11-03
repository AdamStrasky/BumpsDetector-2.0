package com.example.monikas.navigationapp;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.w3c.dom.Document;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static com.example.monikas.navigationapp.FragmentActivity.fragment_context;
import static com.example.monikas.navigationapp.FragmentActivity.global_gps;
import static com.example.monikas.navigationapp.FragmentActivity.updatesLock;
import static com.google.maps.android.PolyUtil.isLocationOnEdge;

/**
 * Created by Adam on 2.11.2016.
 */

public class Location {
    SQLiteDatabase sb;
    DatabaseOpenHelper databaseHelper;
    private ArrayList<Position> LIFO;
    Thread t;
    public Location() {

       LIFO = new  ArrayList<Position>();
        analyzePosition();
      //  bumps_on_position();

    }

    public void analyzePosition() {
        Thread v = new Thread() {
            public void run() {
                while(true )  {
                    if (LIFO.size() > 60)
                        LIFO.remove(0);
                    if (global_gps!= null && global_gps.getmCurrentLocation()!=null ) {
                        Position aaa = new Position(global_gps.getmCurrentLocation().getSpeed(), global_gps.getmCurrentLocation().getLatitude(), global_gps.getmCurrentLocation().getLongitude());
                        LIFO.add(aaa);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {

                    }

                }
            }

        };
        v.start();
    }
    public void bumps_on_position(final LatLng to_position) {

          t = new Thread() {
            public void run() {
                ArrayList<LatLng> aa = new ArrayList<LatLng>();
                ArrayList<LatLng> choise = new ArrayList<LatLng>();
                while(true ) {
                    double longitude, latitude;
                    if (global_gps == null || global_gps.getmCurrentLocation() == null) {
                        latitude = global_gps.getmCurrentLocation().getLatitude();
                        longitude = global_gps.getmCurrentLocation().getLongitude();
                    } else
                        return;

                    if (updatesLock) {
                        return;
                    }
                    updatesLock = true;
                    SimpleDateFormat now, ago;
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new Date());
                    now = new SimpleDateFormat("yyyy-MM-dd");
                    String now_formated = now.format(cal.getTime());
                    // posun od dnesneho dna o 280 dni
                    cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE) - 280);
                    ago = new SimpleDateFormat("yyyy-MM-dd");
                    String ago_formated = ago.format(cal.getTime());
                    // ziskam sučasnu poziciu
                    // seleknutie vytlk z oblasti a starych 280 dni
                    String selectQuery = "SELECT latitude,longitude,count,manual FROM my_bumps WHERE " +
                            " ( last_modified BETWEEN '" + ago_formated + " 00:00:00' AND '" + now_formated + " 23:59:59') and  "
                            + " (ROUND(latitude,1)==ROUND(" + latitude + ",1) and ROUND(longitude,1)==ROUND(" + longitude + ",1)) ";
                    SQLiteDatabase database = databaseHelper.getWritableDatabase();
                    Cursor cursor = null;
                    try {
                        cursor = database.rawQuery(selectQuery, null);
                        if (cursor.moveToFirst()) {
                            do {
                                aa.add(new LatLng(cursor.getDouble(0), cursor.getDouble(1)));
                             }
                            while (cursor.moveToNext());
                        }
                    } finally {
                        if (cursor != null)
                            cursor.close();
                    }

                    updatesLock = false;


                    Route md = new Route();
                    LatLng myPosition = new LatLng(latitude,longitude);
                    Document doc = md.getDocument(myPosition, to_position);
                    ArrayList<LatLng> directionPoint = md.getDirection(doc);
                    for(int i=0; i < aa.size(); i++) {
                       if (isLocationOnEdge(aa.get(0), directionPoint, true, 2.0))
                           choise.add(aa.get(0));
                    }

                }}
                    };
                    t.start();
}

    public void initialization_database(){
        // inicializacia databazy
        databaseHelper = new DatabaseOpenHelper(fragment_context);
        sb = databaseHelper.getWritableDatabase();
    }

    public void estimation() {

    }
}
