package com.example.monikas.navigationapp;

/**
 * Created by monikas on 23. 3. 2015.
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static com.example.monikas.navigationapp.FragmentActivity.fragment_context;
import static com.example.monikas.navigationapp.FragmentActivity.global_gps;


public class Accelerometer extends Service implements SensorEventListener, LocationListener {

    private boolean flag = false;
    private Context context;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float THRESHOLD = 4.5f;
    private ArrayList<HashMap<Location, Float>> possibleBumps;
    private float priorityX = 0.0f;
    private float priorityY = 0.0f;
    private float priorityZ = 0.0f;
    private float[] values = new float[3]; // values is array of X,Y,Z
    private ArrayList<AccData> LIFO;
    private int LIFOsize = 60;
    private float delta;
    private boolean unlock = true;

    public Accelerometer(){
        this.context = fragment_context;
        LIFO = new ArrayList<>();
        flag = false;
        mSensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
        mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        possibleBumps = new ArrayList<>();
    }

     public ArrayList<HashMap<Location, Float>> getPossibleBumps() {
        return possibleBumps;
    }

    @Override
    //pri zmene dat z akcelerometra nam metoda dava tieto data v premennej event.values[]
    public synchronized void onSensorChanged(SensorEvent event) {
            new SensorEventLoggerTask().execute(event);
     }

    private class SensorEventLoggerTask extends AsyncTask<SensorEvent, Void, String> {
        @Override
        protected String doInBackground(SensorEvent... events) {
            String result = null;
            SensorEvent event = events[0];
            float deltaZ = 0, deltaX = 0, deltaY = 0;
            boolean isBump = false;
            //objekt AccData obsahuje data z akcelerometra pre osi X,Y,Z
            final AccData currentData;
           Log.d("POP"," stav"+ MainActivity.isActivityVisible());
            values[0] = event.values[0];
            values[1] = event.values[1];
            values[2] = event.values[2];
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            if (!flag) {
                flag = true;
                currentData = new AccData(x,y,z);
                //premenna LIFO je pole velkosti 60, obsahuje objekty AccData
                LIFO.add(currentData);
            } else {
                currentData = new AccData(x,y,z);
                final Location location = global_gps.getmCurrentLocation();
                if (location != null) {
                  //  Log.d("GPS", " current position  " + location.getLongitude());
                 //   Log.d("GPS", " current position  " + location.getLatitude());
                }
                else
                    Log.d("GPS", " no GPS  " );


                //prechadza sa cele LIFO, kontroluje sa, ci zmena zrychlenia neprekrocila THRESHOLD
                for (AccData temp : LIFO) {
                    //pre kazdu os X,Y,Z sa vypocita zmena zrychlenia
                    deltaX = Math.abs(temp.getX() - currentData.getX());
                    deltaY = Math.abs(temp.getY() - currentData.getY());
                    deltaZ = Math.abs(temp.getZ() - currentData.getZ());
                    //na zaklade priorit jednotlivych osi sa vypocita celkova zmena zrychlenia
                    delta = priorityX*deltaX + priorityY*deltaY + priorityZ*deltaZ;
                    //ak je zmena vacsia ako THRESHOLD 4,5
                    if (delta > THRESHOLD) {
                        //staci ak zmena zrychlenia prekrocila THRESHOLD raz, je to vytlk
                        isBump = true;
                        break;
                    }
                }
                if (isBump) {
                    isBump = false;
                    final Float data = new Float(delta);
                    if (location != null && data != null) {
                        //pokial je znama aktualna pozicia a intenzita otrasu
                        if (unlock) {
                            Log.d("SVTEST", "sensor");

                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                            Boolean imgSett = prefs.getBoolean("alarm", Boolean.parseBoolean(null));
                            Log.d("SVTEST", "shared " +imgSett);
                            unlock = false;
                            result= detect(location, data);
                            unlock = true;
                        }
                    }
                }
                //najstarsi prvok z LIFO sa vymaze a ulozi sa na koniec najnovsi
                if (LIFO.size() >= LIFOsize) {
                    LIFO.remove(0);
                }
                LIFO.add(currentData);
            }

            return result;
        }




        protected void onPostExecute(String result){
            if (result != null)
                 Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }
    }

    public void calibrate () {

        //values[0], values[1], values[2] = data z akcelerometra pre osi X,Y,Z
        //vypocita sa percentualne rozlozenie gravitacneho zrychlenia
        //na jednotlive osi X,Y,Z
        float sum = values[0] + values[1] + values[2];
        priorityX = Math.abs(values[0]/ sum);
        priorityY = Math.abs(values[1]/ sum);
        priorityZ = Math.abs(values[2]/ sum);
        //normalizacia
        sum = priorityX + priorityY + priorityZ;
        priorityX = priorityX/sum;
        priorityY = priorityY/sum;
        priorityZ = priorityZ/sum;

    }


    public synchronized String detect (Location location, Float data) {
        String result = null;
        boolean isToClose = false;
        //possibleBumps je zoznam vytlkov, ktore sa poslu do databazy
        for (HashMap<Location, Float> bump : possibleBumps) {

            Iterator it = bump.entrySet().iterator();
            while (it.hasNext()) {
                HashMap.Entry pair = (HashMap.Entry)it.next();
                Location hashLocation = (Location) pair.getKey();
                //ak je location je rovnaka, neprida sa vytlk
                if ((location.getLatitude() == hashLocation.getLatitude()) && (location.getLongitude() == hashLocation.getLongitude())) {
                    if (data > (Float) pair.getValue()) {
                        pair.setValue(data);
                        Log.d("detek", "same location");
                        result = "same bump";
                    }
                    isToClose = true;
                }
                else {
                    double distance = getDistance((float) location.getLatitude(), (float) location.getLongitude(),
                            (float) hashLocation.getLatitude(), (float) hashLocation.getLongitude());
                    //nie je to novy bump, pretoze z jeho okolia uz jeden pridavam (okolie 2m)
                    if (distance < 2000.0) {
                        //do databazy sa ulozi najvacsia intenzita s akou sa dany vytlk zaznamenal
                        if (data > (Float) pair.getValue()) {
                            Log.d("detek", "under 2 meters ");
                            result = "under bump";
                            pair.setValue(data);
                        }
                        isToClose = true;
                    }
                }
            }

        }
        if (!isToClose) {

            Log.d("detek", "new dump");
            result = "new bump";
            System.out.println("lat: "+ location.getLatitude() + ",lng: "+ location.getLongitude() + ",data: " + data);
            HashMap<Location, Float> hashToArray = new HashMap();
            hashToArray.put(location,data);
            //zdetegovany vytlk, ktory sa prida do zoznamu vytlkov, ktore sa odoslu do databazy
            possibleBumps.add(hashToArray);
        }
        return result;
    }

    //vzdialenost dvoch pozicii v metroch
    //zdroj http://www.androidsnippets.com/distance-between-two-gps-coordinates-in-meter
    public double getDistance(float lat_a, float lng_a, float lat_b, float lng_b) {
        float pk = (float) (180/3.14169);

        float a1 = lat_a / pk;
        float a2 = lng_a / pk;
        float b1 = lat_b / pk;
        float b2 = lng_b / pk;

        float t1 = FloatMath.cos(a1)*FloatMath.cos(a2)*FloatMath.cos(b1)*FloatMath.cos(b2);
        float t2 = FloatMath.cos(a1)*FloatMath.sin(a2)*FloatMath.cos(b1)*FloatMath.sin(b2);
        float t3 = FloatMath.sin(a1)*FloatMath.sin(b1);
        double tt = Math.acos(t1 + t2 + t3);

        return 6366000*tt;
    }

    public IBinder onBind(Intent intent) {
        Log.d("SVTEST", "Accelerometer service ONBIND");
        return mBinder;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SVTEST", "Accelerometer service ONSTARTCOMMAND");
        return START_STICKY;
    }

    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {


        public Accelerometer getService() {
            Log.d("SVTEST", "Accelerometer service getService");
            return Accelerometer.this;
        }
    }

   @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
