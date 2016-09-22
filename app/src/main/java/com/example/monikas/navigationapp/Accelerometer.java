package com.example.monikas.navigationapp;

/**
 * Created by monikas on 23. 3. 2015.
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.FloatMath;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static android.app.Service.START_STICKY;
import static com.example.monikas.navigationapp.BlankFragment.contexts;
import static com.example.monikas.navigationapp.BlankFragment.gpss;


public class Accelerometer extends Service implements SensorEventListener, LocationListener {

    private boolean flag = false;
    private Context context;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private GPSLocator gps;
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


        Log.d("SVTEST", "FUUUUK");
        this.context = contexts;
        this.gps = gpss;
        LIFO = new ArrayList<>();
        flag = false;
        mSensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
        mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        possibleBumps = new ArrayList<>();
    }

    public Accelerometer(Context context, GPSLocator gps) {
        Log.d("SVTEST", "PIIIIIIIIIIIC");
        this.context = contexts;
        this.gps = gpss;
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
            currentData = new AccData(x,y,z);
            //premenna LIFO je pole velkosti 60, obsahuje objekty AccData
            LIFO.add(currentData);
        } else {
            currentData = new AccData(x,y,z);
            final Location location = gps.getmCurrentLocation();
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


                        unlock = false;
                        detect(location, data);
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


    public synchronized void detect (Location location, Float data) {

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
                        Log.d("SVTEST", "aaaaaaa");
                        Toast.makeText(context,"BUMPaaa!!!",Toast.LENGTH_SHORT).show();
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
                            Log.d("SVTEST", "bbbbbbbbbbb");
                            Toast.makeText(context,"BUMPbbbb!!!",Toast.LENGTH_SHORT).show();
                            pair.setValue(data);
                        }
                        isToClose = true;
                    }
                }
            }

        }
        if (!isToClose) {
            Toast.makeText(context,"BUMPcccc!!!",Toast.LENGTH_SHORT).show();
            Log.d("SVTEST", "ccccccc");
            System.out.println("lat: "+ location.getLatitude() + ",lng: "+ location.getLongitude() + ",data: " + data);
            HashMap<Location, Float> hashToArray = new HashMap();
            hashToArray.put(location,data);
            //zdetegovany vytlk, ktory sa prida do zoznamu vytlkov, ktore sa odoslu do databazy
            possibleBumps.add(hashToArray);
        }
//aaaasasxasd
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

    //  private final IBinder mBinder = new LocalBinder();



    public IBinder onBind(Intent intent) {
        Log.d("SVTEST", "Loc service ONBIND");
        return mBinder;
    }



   /* @Override
    public boolean onUnbind(Intent intent) {
        Log.d("SVTEST", "Loc service ONUNBIND");
        return super.onUnbind(intent);
    }*/

    public int onStartCommand(Intent intent, int flags, int startId) {
        // Won't run unless it's EXPLICITLY STARTED
        Log.d("SVTEST", "Loc service ONSTARTCOMMAND");
         return START_STICKY;
        //.getClass().onStartCommand(intent, flags, startId);
    }
 /*   @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("SVTEST", "Loc service ONDESTROY");
    }*/

   /* public class LocalBinder extends Binder {
        Accelerometer getService() {
            // Return this instance of LocalService so clients can call public methods
            return Accelerometer.this;
        }
    }*/


    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {


        public Accelerometer getService() {
            Log.d("SVTEST", "Loc service ONDESTROY");
            return Accelerometer.this;
        }
    }

    public void methodInTheService() {
        // A method you can call in the service
        Log.d("SVTEST", "Loc service EXECUTING THE METHOD");
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
