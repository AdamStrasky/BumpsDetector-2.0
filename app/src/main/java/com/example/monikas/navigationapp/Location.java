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

import static com.example.monikas.navigationapp.Accelerometer.getDistance;
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
    private boolean  road = false;
    private boolean  new_data = false;
    Thread t,estimation;
    ArrayList<LatLng> directionPoint = null;
    LatLng position;
    ArrayList<LatLng> choise_bump = null;

    public Location() {
       initialization_database();
       directionPoint = new  ArrayList<LatLng>();
       LIFO = new ArrayList<Position>();
       analyzePosition();
    }

    public void analyzePosition() {
        Thread v = new Thread() {
            public void run() {
                while(true )  {
                    android.location.Location location = null;
                    if (global_gps!= null && global_gps.getmCurrentLocation()!=null) {
                        if (LIFO.size() == 60)
                            LIFO.remove(0);
                        location = global_gps.getmCurrentLocation();
                        Position new_position = new Position(location.getSpeed(), location.getLatitude(), location.getLongitude());
                        LIFO.add(new_position);
                       // Log.d("choise_bump", "analyzePosition");
                        if (road && directionPoint != null) {
                            Log.d("choise_bump", "road and directionPoint");
                            if (!isLocationOnEdge(new LatLng(location.getLatitude(),location.getLongitude()), directionPoint, true, 2.0)) {
                                Log.d("choise_bump", "nie je na ceste");
                              //  t.stop();
                              //  t.start();
                              //  directionPoint= null;
                                // estimation.stop();
                                // wait
                                //estimation.start();


                            }else
                                if (getDistance((float) global_gps.getCurrentLatLng().latitude, (float) global_gps.getCurrentLatLng().longitude, (float) position.latitude, (float) position.longitude) < 10) {
                                    Log.d("choise_bump", "som v cieli");
                                  //  t.stop();
                                  //  t = null;
                                    // estimation.stop();
                                    directionPoint = null;
                                    position = null;
                                }
                        }
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
        Log.d("choise_bump", "bumps_on_position start");
        position =to_position;
      //  if (t!=null)
       //     t.stop();

          t = new Thread() {
            public void run() {
                ArrayList<LatLng> all_bumps = new ArrayList<LatLng>();
                choise_bump = new ArrayList<LatLng>();
                while(true ) {
                    Log.d("choise_bump", "thread");
                    double longitude, latitude;
                    if (global_gps != null && global_gps.getmCurrentLocation() != null) {
                        latitude = global_gps.getmCurrentLocation().getLatitude();
                        longitude = global_gps.getmCurrentLocation().getLongitude();
                        Log.d("choise_bump", "gps");


                        if (!updatesLock) {

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
                                        all_bumps.add(new LatLng(cursor.getDouble(0), cursor.getDouble(1)));
                                    }
                                    while (cursor.moveToNext());
                                }
                            } finally {
                                if (cursor != null)
                                    cursor.close();
                            }

                            updatesLock = false;

                            directionPoint = directionPoint(to_position);
                            for (int i = 0; i < all_bumps.size(); i++) {
                                if (isLocationOnEdge(all_bumps.get(i), directionPoint, true, 4.0)) {
                                    choise_bump.add(all_bumps.get(i));
                                    Log.d("choise_bump", String.valueOf(i));
                                }
                            }
                            new_data = true;

                            if (estimation == null) {
                                //   estimation.start();
                            }
                        } else  {
                            Log.d("choise_bump", "thread lock");

                        }

                        } else {
                            Log.d("choise_bump", "no gps");
                            //return;
                        }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                    }
                }


            }
        };
    t.start();
}

    public void initialization_database(){
        // inicializacia databazy
        databaseHelper = new DatabaseOpenHelper(fragment_context);
        sb = databaseHelper.getWritableDatabase();
    }

    public  ArrayList<LatLng>  directionPoint (LatLng to_position) {
        Log.d("choise_bump", " get directionPoints");
        double latitude,longitude ;
        if (global_gps!=null  && global_gps.getmCurrentLocation()!= null) {
            latitude = global_gps.getmCurrentLocation().getLatitude();
            longitude = global_gps.getmCurrentLocation().getLongitude();
            Route md = new Route();
            LatLng myPosition = new LatLng(latitude, longitude);
            Document doc = md.getDocument(myPosition, to_position);
            ArrayList<LatLng> directionPoint = md.getDirection(doc);
            Log.d("choise_bump", " return  directionPoints");
            return directionPoint;
        }
        return null;
    }

    public void estimation() {
         estimation = new Thread() {
            public void run() {
                while(true )  {



                }
            }

        };
        estimation.start();
    }

    public boolean isRoad() {
        return road;
    }

    public void setRoad(boolean road) {
        this.road = road;
    }
}
