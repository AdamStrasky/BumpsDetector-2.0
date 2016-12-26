package com.example.monikas.navigationapp;

/**
 * Created by monikas on 23. 3. 2015.
 */

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static com.example.monikas.navigationapp.FragmentActivity.fragment_context;
import static com.example.monikas.navigationapp.FragmentActivity.global_gps;
import static com.example.monikas.navigationapp.FragmentActivity.lockAdd;
import static com.example.monikas.navigationapp.FragmentActivity.lockZoznam;
import static com.example.monikas.navigationapp.FragmentActivity.lockZoznamDB;
import static com.example.monikas.navigationapp.FragmentActivity.updatesLock;
import static com.example.monikas.navigationapp.MainActivity.round;

public class Accelerometer extends Service implements SensorEventListener, LocationListener {

    private boolean flag = false;
    private Context context;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float THRESHOLD = 4.5f;
    private ArrayList<HashMap<Location, Float>> possibleBumps;
    private ArrayList <Integer> BumpsManual;
    private float priorityX = 0.0f;
    private float priorityY = 0.0f;
    private float priorityZ = 0.0f;
    private float[] values = new float[3]; // values is array of X,Y,Z
    private ArrayList<AccData> LIFO;
    private int LIFOsize = 60;
    private float delta;
    private boolean unlock = true;
    SQLiteDatabase sb;
    DatabaseOpenHelper databaseHelper;

    public Accelerometer(){
        this.context = fragment_context;
        LIFO = new ArrayList<>();
        flag = false;
        initialization_database();
        mSensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
        mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        possibleBumps = new ArrayList<>();
        BumpsManual = new ArrayList<>();
    }

     public ArrayList<HashMap<Location, Float>> getPossibleBumps() {
         return possibleBumps;
    }

    public void addPossibleBumps(Location location, Float data) {
        HashMap<Location, Float> hashToArray = new HashMap();
        hashToArray.put(location,data);
        possibleBumps.add(hashToArray);
    }

    public ArrayList<Integer> getBumpsManual() {
        return BumpsManual;
    }

    public void addBumpsManual(int manual) {
        BumpsManual.add(manual);
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
            values[0] = event.values[0];
            values[1] = event.values[1];
            values[2] = event.values[2];
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            if (!flag) {
                flag = true;
                currentData = new AccData(x, y, z);
                //premenna LIFO je pole velkosti 60, obsahuje objekty AccData
                LIFO.add(currentData);
            } else {
                currentData = new AccData(x, y, z);

                if (global_gps != null && global_gps.getmCurrentLocation().getSpeed()> 3) {
                    Log.d("QWER","dostatočná  rýchlosť " + global_gps.getmCurrentLocation().getSpeed());
                    final Location location = global_gps.getmCurrentLocation();
                    //prechadza sa cele LIFO, kontroluje sa, ci zmena zrychlenia neprekrocila THRESHOLD
                    for (AccData temp : LIFO) {
                        //pre kazdu os X,Y,Z sa vypocita zmena zrychlenia
                        deltaX = Math.abs(temp.getX() - currentData.getX());
                        deltaY = Math.abs(temp.getY() - currentData.getY());
                        deltaZ = Math.abs(temp.getZ() - currentData.getZ());
                        //na zaklade priorit jednotlivych osi sa vypocita celkova zmena zrychlenia
                        delta = priorityX * deltaX + priorityY * deltaY + priorityZ * deltaZ;
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
                                Log.d("ACC", "sensor Accelerometer");
                                unlock = false;
                                result = detect(location, data);
                                unlock = true;
                            }
                        }
                    }
                    //najstarsi prvok z LIFO sa vymaze a ulozi sa na koniec najnovsi
                    if (LIFO.size() >= LIFOsize) {
                        LIFO.remove(0);
                    }
                    LIFO.add(currentData);
                } else {} //
                    // Log.d("QWER","mala rýchlosť "  + global_gps.getmCurrentLocation().getSpeed());
            }
            return result;
        }

        protected void onPostExecute(String result) {
            if (result != null) {
               if (isEneableShowText())
                    Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void initialization_database(){
        // inicializacia databazy
        databaseHelper = new DatabaseOpenHelper(context);
        sb = databaseHelper.getWritableDatabase();
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
        if (lockAdd)
            return "lock";
        //possibleBumps je zoznam vytlkov, ktore sa poslu do databazy
        lockZoznam = true;
        for (HashMap<Location, Float> bump : possibleBumps) {

            Iterator it = bump.entrySet().iterator();
            while (it.hasNext()) {
                HashMap.Entry pair = (HashMap.Entry)it.next();
                Location hashLocation = (Location) pair.getKey();
                //ak je location je rovnaka, neprida sa vytlk
                if ((location.getLatitude() == hashLocation.getLatitude()) && (location.getLongitude() == hashLocation.getLongitude())) {
                    if (data > (Float) pair.getValue()) {
                        pair.setValue(data);
                        Log.d("DETECT", "same location");
                        if (!updatesLock) {
                            lockZoznamDB=true;
                            sb.execSQL("UPDATE new_bumps  SET intensity=ROUND(" + data + ",6) WHERE ROUND(latitude,7)==ROUND("+hashLocation.getLatitude()+",7)  and ROUND(longitude,7)==ROUND("+hashLocation.getLongitude()+",7) ");
                            lockZoznamDB=false;
                        }
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
                            Log.d("DETECT", "under 2 meters ");
                            pair.setValue(data);
                            if (!updatesLock) {
                                lockZoznamDB=true;
                                sb.execSQL("UPDATE new_bumps  SET intensity=ROUND(" + data + ",6) WHERE ROUND(latitude,7)==ROUND("+hashLocation.getLatitude()+",7)  and ROUND(longitude,7)==ROUND("+hashLocation.getLongitude()+",7) ");
                                lockZoznamDB=false;
                            }
                            result = "under bump";
                        }
                        isToClose = true;
                    }
                }
            }

        }
        lockZoznam = false;
        if (!isToClose) {
            Log.d("DETECT", "new dump");
            result = "new bump";
            System.out.println("lat: "+ location.getLatitude() + ",lng: "+ location.getLongitude() + ",data: " + data);
            HashMap<Location, Float> hashToArray = new HashMap();
            location.setLatitude(round(location.getLatitude(),7));
            location.setLongitude(round(location.getLongitude(),7));
            hashToArray.put(location,data);
            //zdetegovany vytlk, ktory sa prida do zoznamu vytlkov, ktore sa odoslu do databazy
            lockZoznam = true;

            possibleBumps.add(hashToArray);
            BumpsManual.add(0);
            lockZoznam = false;
            if (!updatesLock) {
                lockZoznamDB=true;
                BigDecimal bd = new BigDecimal(Float.toString(data));
                bd = bd.setScale(6, BigDecimal.ROUND_HALF_UP);
                hashToArray.put(location,data);
                ContentValues contentValues = new ContentValues();
                contentValues.put(Provider.new_bumps.LATITUDE, location.getLatitude());
                contentValues.put(Provider.new_bumps.LONGTITUDE, location.getLongitude());
                contentValues.put(Provider.new_bumps.MANUAL, 0);
                contentValues.put(Provider.new_bumps.INTENSITY, String.valueOf(bd));
                sb.insert(Provider.new_bumps.TABLE_NAME_NEW_BUMPS, null, contentValues);
                lockZoznamDB=false;
            }
        }
        return result;
    }

    //vzdialenost dvoch pozicii v metroch
    //zdroj http://www.androidsnippets.com/distance-between-two-gps-coordinates-in-meter
    public static double getDistance(float lat_a, float lng_a, float lat_b, float lng_b) {
        float pk = (float) (180/3.14169);

        float a1 = lat_a / pk;
        float a2 = lng_a / pk;
        float b1 = lat_b / pk;
        float b2 = lng_b / pk;

        double t1 = Math.cos(a1)*Math.cos(a2)*Math.cos(b1)*Math.cos(b2);
        double t2 = Math.cos(a1)*Math.sin(a2)*Math.cos(b1)*Math.sin(b2);
        double t3 = Math.sin(a1)*Math.sin(b1);
        double tt = Math.acos(t1 + t2 + t3);

        return 6366000*tt;
    }

    public IBinder onBind(Intent intent) {
        Log.d("BIND_ACC", "Accelerometer service ONBIND");
        return mBinder;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("BIND_ACC", "Accelerometer service ONSTARTCOMMAND");
        return START_STICKY;
    }

    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {

        public Accelerometer getService() {
            Log.d("BIND_ACC", "Accelerometer service getService");
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

    public  boolean isEneableShowText() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean alarm = prefs.getBoolean("alarm", Boolean.parseBoolean(null));
        if ((alarm) || (!alarm && MainActivity.isActivityVisible())) {
            return true;
        }
        else
            return false;
    }
}
