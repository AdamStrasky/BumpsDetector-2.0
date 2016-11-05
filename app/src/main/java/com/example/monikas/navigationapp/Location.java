package com.example.monikas.navigationapp;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
    private final int lifo_size = 60;
    private boolean  first_start_estimation = false;
    private boolean  road = false;  // nastavi5 na tru ked kliknem  na navigate, ked kliknem na end tak false
    private boolean  lock_position = false,only_one_not_position = true ;
    private boolean lock_choise= false;
    private boolean  new_data = false;
    private boolean  lock_stop = false;
    private boolean start_bumps = true;

    private boolean  asd = false;




    Thread collision_thread = null ,estimation_thread = null;

    private ArrayList<Position> LIFO;

    ArrayList<LatLng> select_road = null,  choise_bump = null;

    LatLng position = null;


    public Location() {
       initialization_database();
        select_road = new  ArrayList<LatLng>();
        LIFO = new ArrayList<Position>();
       analyzePosition();
    }

    public void analyzePosition() {
        Thread analyze_thread = new Thread() {
            android.location.Location location = null;
            public void run() {
                while(true) {
                     if (!lock_position) {
                         lock_position = true;
                         if (global_gps != null && global_gps.getmCurrentLocation() != null) {
                             if (LIFO.size() == lifo_size)
                                 LIFO.remove(0);

                             location = global_gps.getmCurrentLocation();
                             Position new_position = new Position(location.getSpeed(), location.getLatitude(), location.getLongitude(),location.getTime());
                             LIFO.add(new_position);
                             lock_position = false;
                             if (road && select_road!= null) {


                                 Log.d("analyzePosition", "zadaná a nájdena cesta");

                                 if (asd && select_road!= null && !isLocationOnEdge(new LatLng(location.getLatitude(), location.getLongitude()), select_road, true, 4.0)) {
                                  /*  Log.d("analyzePosition", "moja poloha nie je na ceste");
                                     try {
                                         Thread.sleep(5000);
                                     } catch (InterruptedException e) {
                                     }*/


                                     if (collision_thread!=null && collision_thread.isAlive() ) {
                                        if (!lock_stop) {
                                            lock_stop = true;
                                            Log.d("analyzePosition", "beží thread na hľadanie koliznych miest");
                                            //   only_one_not_position= false; // vymazat
                                            collision_thread.interrupt();
                                            collision_thread = null;
                                            collision_places();
                                            first_start_estimation = false;
                                            collision_thread.start();

                                            if (estimation_thread != null && estimation_thread.isAlive()) {
                                                Log.d("analyzePosition", "beží thread na upozornovanie koliznych miest");
                                                estimation_thread.interrupt();
                                                estimation_thread = null;
                                            }
                                            lock_stop = false;
                                        }
                                     }
                                } else {
                                     Log.d("analyzePosition", String.valueOf(position.latitude));
                                     Log.d("analyzePosition", String.valueOf(position.longitude));
                                     Log.d("analyzePosition", String.valueOf(location.getLatitude()));
                                     Log.d("analyzePosition", String.valueOf(location.getLongitude())) ;
                                     if ( position!= null && getDistance((float) location.getLatitude(), (float) location.getLongitude(), (float) position.latitude, (float) position.longitude) < 10) {
                                     if (!lock_stop) {
                                         lock_stop = true;
                                         Log.d("analyzePosition", String.valueOf(getDistance((float) location.getLatitude(), (float) location.getLongitude(), (float) position.latitude, (float) position.longitude)));
                                         Log.d("analyzePosition", "som v cieli");
                                         stop_collision_thread();
                                         stop_estimation_thread();
                                         lock_stop = false;
                                     }
                                }}

                             }
                         }else
                            lock_position = false;
                     }
                     try {
                        Thread.sleep(1000);
                     } catch (InterruptedException e) {
                      }
                }
            }
        };
         analyze_thread.start();
    }

    public void stop_collison_navigate() {
        start_bumps = true;
        Thread stop_navigate = new Thread() {
            public void run() {
                  while (true) {
                      if (!lock_stop) {
                        lock_stop = true;
                         break;
                      }
                      try {
                        Thread.sleep(20);
                        } catch (InterruptedException e) {
                        }
                  }
                stop_collision_thread();
                stop_estimation_thread();
                start_bumps = false;;
                lock_stop = false;
            }
        };
        stop_navigate.start();
    }

    public void stop_collision_thread() {
        if (collision_thread!=null && collision_thread.isAlive()) {
            Log.d("stop_collision_thread", "vykonal sa stop");
            collision_thread.interrupt();
            collision_thread= null;
            first_start_estimation = false;
            position=null;

        }
    }

    public void stop_estimation_thread() {
        if (estimation_thread!=null && estimation_thread.isAlive()) {
            Log.d("stop_estimation_thread", "vykonal sa stop");
            estimation_thread.interrupt();
            estimation_thread= null;
        }
    }

    public void bumps_on_position(final LatLng to_position) {
        new Thread() {
            public void run() {
                while (true) {
                    if (start_bumps == false ) {
                        road = true;

                        Log.d("bumps_on_position", "start function");
                        position = to_position;
                        collision_places();
                        Log.d("bumps_on_position", "start thread");
                        collision_thread.start();
                        break;
                    }
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {

                    }
                    Log.d("bumps_on_position", "wait");
                }

            }
        }.start();


    }

    public void collision_places() {
        collision_thread = new Thread() {
            public void run() {
                Log.d("bumps_on_position", "wtf ");
                try {
                    if (this.isInterrupted()) {
                        Log.d("bumps_on_position", "omg ");
                    }
                    while(!this.isInterrupted() ) {
                        Log.d("bumps_on_position", "wtf 2 ");
                        Log.d("collision_places", "name of pid "+ String.valueOf(this.getId()));

                        if (Thread.currentThread().isInterrupted()) {
                            Log.d("collision_places", "throw exception on if ");
                            throw new InterruptedException();
                        }

                        double longitude = 0, latitude = 0;
                        if (global_gps != null && global_gps.getmCurrentLocation() != null) {
                            Log.d("collision_places", "get gps");
                            latitude = global_gps.getmCurrentLocation().getLatitude();
                            longitude = global_gps.getmCurrentLocation().getLongitude();

                            while (true) {
                                if (!updatesLock) {
                                    updatesLock = true;
                                    break;
                                }
                                try {
                                    Thread.sleep(20);
                                } catch (InterruptedException e) {
                                    Log.d("collision_places", "throw intr on update lock ");
                                    throw new InterruptedException("Stopped by ifInterruptedStop()");
                                }
                            }

                            if (this.isInterrupted() && updatesLock) {
                                updatesLock = false;
                                Log.d("collision_places", "throw intr after while on update lock ");
                                throw new InterruptedException("");
                            }
                            ArrayList<LatLng> all_bumps = new ArrayList<LatLng>();
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
                            if (position != null)
                                select_road = directionPoint(position, this);  // vratim cestu
                            if (select_road != null) {
                                while (true) {
                                    if (!lock_choise) {
                                        lock_choise = true;
                                        break;
                                    }
                                    try {
                                        Thread.sleep(20);
                                    } catch (InterruptedException e) {
                                        Log.d("collision_places", "throw intr after while on update lock_choise ");
                                        throw new InterruptedException();
                                    }
                                }
                                if (this.isInterrupted() && lock_choise) {
                                    lock_choise = false;
                                    Log.d("collision_places", "throw intr after while on update lock choise ");
                                    throw new InterruptedException();
                                }

                                choise_bump = new ArrayList<LatLng>();
                                for (int i = 0; i < all_bumps.size(); i++) {
                                    if (select_road!= null && isLocationOnEdge(all_bumps.get(i), select_road, true, 4.0)) {
                                        choise_bump.add(all_bumps.get(i));
                                    }
                                }

                                lock_choise = false;



                                if (this.isInterrupted()) {

                                    Log.d("collision_places", "pre spustanim estimation ");
                                    throw new InterruptedException();
                                } else {
                                    all_bumps = new ArrayList<LatLng>();
                                    new_data = true;
                                    if (!first_start_estimation && estimation_thread == null) {
                                        first_start_estimation = true;
                                        estimation();
                                        Log.d("collision_places", " estimation start ");
                                        estimation_thread.start();
                                    }
                                }

                            } else {
                                Log.d("collision_places", "no GPS signal");
                            }

                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                Log.d("collision_places", "throw sleep  for 10 seconds ");
                                throw new InterruptedException();
                            }
                        }
                    }
                } catch (InterruptedException t) {
                    Log.d("collision_places", "cath interuoption " + this.getId());
                }

             Log.d("bumps_on_position", " end pid  "+String.valueOf(this.getId()));

            }
        };
    }

    public void estimation() {
        Log.d("estimation"," function start");
        estimation_thread = new Thread() {
            public void run() {
                float speed = 0;
                float avarage_speed = 0;
                long time = 0;
                double distance= 0;
                float avarage_speed_second = 0;
                float best_cause = 0;
                double longitude = 0, latitude= 0;
                int time_stop = 1000;
                List<LatLng> directionPoint = null;
                ArrayList<LatLng> bump_actual = new ArrayList<LatLng>();
              try {
                while(!this.isInterrupted() ) {

                    while (true) {
                        if  (!lock_position) {
                            lock_position = true;
                            break;
                        }
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            Log.d("estimation_thread", "throw intr  while on update lock_position ");
                            throw new InterruptedException("");
                        }
                    }
                    Log.d("estimation_thread", "pid "+String.valueOf(this.getId()));

                    if (this.isInterrupted() && lock_position) {
                        lock_position = false;
                        Log.d("estimation_thread", "throw intr after if   on update lock_position ");
                        throw new InterruptedException("");
                    }

                    for (int i=0 ; i <   LIFO.size() ; i++ ) {
                        speed += LIFO.get(i).getSpeed();
                    }
                    avarage_speed = speed / LIFO.size();

                    Log.d("estimation","avarage_speed "+ String.valueOf(avarage_speed));
                    Log.d("estimation","getLatitude "+ String.valueOf(LIFO.get(0).getLatitude()));
                    Log.d("estimation","getLongitude "+ String.valueOf(LIFO.get(0).getLongitude()));
                    Log.d("estimation"," -1 getLatitude "+ String.valueOf(LIFO.get(LIFO.size()-1).getLatitude()));
                    Log.d("estimation","-1 getLongitude "+ String.valueOf(LIFO.get(LIFO.size()-1).getLongitude()));

                    distance = getDistance((float) LIFO.get(0).getLatitude(), (float) LIFO.get(0).getLongitude(), (float) LIFO.get(LIFO.size()-1).getLatitude(), (float) LIFO.get(LIFO.size()-1).getLongitude());
                    Log.d("estimation", "distance "+String.valueOf(distance));
                    time=   LIFO.get(LIFO.size()-1).getTime() - LIFO.get(0).getTime();
                    Log.d("estimation","time "+ String.valueOf(time));

                    lock_position= false;


                    long minutes = TimeUnit.MILLISECONDS.toMinutes((long) time);
                    time -= TimeUnit.MINUTES.toMillis(minutes);
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(time);
                    avarage_speed_second = (float) (distance/ (minutes+seconds));
                    Log.d("estimation", "Mins "+ String.valueOf(minutes));
                    Log.d("estimation", "seconds "+ String.valueOf(seconds));
                    Log.d("estimation", "avarage_speed_second "+ String.valueOf(avarage_speed_second));

                     if (avarage_speed > avarage_speed_second )
                         best_cause = avarage_speed_second;
                     else
                         best_cause = avarage_speed_second;

                    if (new_data && !lock_choise) {
                        lock_choise = true;
                        bump_actual =  new ArrayList<LatLng>();
                        if (choise_bump!= null && choise_bump.size()> 0)
                             bump_actual.addAll(choise_bump) ;
                        lock_choise = false;

                        if (global_gps != null && global_gps.getmCurrentLocation() != null) {
                            latitude = global_gps.getmCurrentLocation().getLatitude();
                            longitude = global_gps.getmCurrentLocation().getLongitude();
                            if (bump_actual!=null && bump_actual.size() > 0)
                            directionPoint  = sortLocations(bump_actual,latitude, longitude);
                            for (int i =0; i < directionPoint.size(); i++) {
                                Log.d("estimation", i + " "+ String.valueOf( directionPoint.get(i)));
                            }
                            new_data = false;
                        }
                    }
                    if (directionPoint!=null && directionPoint.size() > 0 && latitude!= 0 && longitude!=0) {
                        double google_distance = getDistance((float) latitude,(float) longitude,(float) directionPoint.get(0).latitude,(float) directionPoint.get(0).longitude);
                        Log.d("estimation", "distance google  " + String.valueOf(google_distance));
                        double times_to_sleep = google_distance / best_cause;
                        Log.d("estimation", "čas ku výtlku  " + String.valueOf(times_to_sleep));

                        //  - čas pred výtlkom

                        time_stop = 0;
                        int treshold = 1000;
                        if (times_to_sleep > treshold)
                            time_stop = treshold;
                        else if (times_to_sleep < 10) {
                            if (this.isInterrupted()) {
                               Log.d("estimation_thread", "throw intr pred upozornenim na výtlk ");
                                throw new InterruptedException("");
                            }
                            // pozor výtlk
                            directionPoint.remove(0);
                            time_stop = 0;
                        } else
                            time_stop = (int) times_to_sleep;
                    }

                    try {
                        Thread.sleep(time_stop);
                    } catch (InterruptedException e) {
                        Log.d("estimation_thread", "throw intr sleep  ");
                        throw new InterruptedException();
                    }
                }
              } catch (InterruptedException t) {
                  Log.d("estimation_thread", "cath interuption " + this.getId());
              }
            }
        };

    }

    public static List<LatLng> sortLocations(List<LatLng> locations, final double myLatitude,final double myLongitude) {
        Comparator comp = new Comparator<LatLng>() {
            @Override
            public int compare(LatLng o, LatLng o2) {
                float[] result1 = new float[3];
                android.location.Location.distanceBetween(myLatitude, myLongitude, o.latitude, o.longitude, result1);
                Float distance1 = result1[0];

                float[] result2 = new float[3];
                android.location.Location.distanceBetween(myLatitude, myLongitude,o2.latitude,  o2.longitude, result2);
                Float distance2 = result2[0];

                return distance1.compareTo(distance2);
            }
        };
        Collections.sort(locations, comp);
        return locations;
    }

    public void initialization_database(){
        // inicializacia databazy
        databaseHelper = new DatabaseOpenHelper(fragment_context);
        sb = databaseHelper.getWritableDatabase();
    }

    public  ArrayList<LatLng>  directionPoint (LatLng to_position, Thread thread) {
        Log.d("directionPoint", " get selected road");
        double latitude,longitude ;
        if (global_gps!=null  && global_gps.getmCurrentLocation()!= null) {
            latitude = global_gps.getmCurrentLocation().getLatitude();
            longitude = global_gps.getmCurrentLocation().getLongitude();
            Route md = new Route();
            LatLng myPosition = new LatLng(latitude, longitude);
            if (myPosition!=null && to_position!=null) {

                Document doc = md.getDocument(myPosition, to_position);
                if (doc  == null)
                    return null;
                ArrayList<LatLng> directionPoint = new  ArrayList<LatLng>();
                directionPoint = md.getDirection(doc);
                Log.d("choise_bump", " return  directionPoints");

                return directionPoint;

            }
        }
        return null;
    }

    public boolean isRoad() {
        return road;
    }

    public void setRoad(boolean road) {
        this.road = road;
    }

    public float getDistance(double lat1, double lon1, double lat2, double lon2) {
        String result_in_kms = "";
        String url = "http://maps.google.com/maps/api/directions/xml?origin=" + lat1 + "," + lon1 + "&destination=" + lat2 + "," + lon2 + "&sensor=false&units=metric";
        String tag[] = {"value"};
        HttpResponse response = null;
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            HttpPost httpPost = new HttpPost(url);
            response = httpClient.execute(httpPost, localContext);
            InputStream is = response.getEntity().getContent();
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            if (doc != null) {
                NodeList nl;
                ArrayList args = new ArrayList();
                for (String s : tag) {
                    nl = doc.getElementsByTagName(s);
                    if (nl.getLength() > 0) {
                        Node node = nl.item(nl.getLength() - 1);
                        args.add(node.getTextContent());
                    } else {
                        args.add(" - ");
                    }
                }
                result_in_kms =String.valueOf( args.get(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Float f = 0.f;

        try
        {
             f = Float.valueOf(result_in_kms.trim()).floatValue();
            System.out.println("float f = " + f);
        }
        catch (NumberFormatException nfe)
        {
            f = 99999999999.f;
        }

        return f*1000;
    }
}
