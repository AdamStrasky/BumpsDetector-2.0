package navigationapp.main_application;

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
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import static navigationapp.main_application.FragmentActivity.fragment_context;
import static navigationapp.main_application.FragmentActivity.global_gps;
import static navigationapp.main_application.FragmentActivity.lockAdd;
import static navigationapp.main_application.FragmentActivity.lockZoznam;
import static navigationapp.main_application.FragmentActivity.lockZoznamDB;
import static navigationapp.main_application.FragmentActivity.updatesLock;
import static navigationapp.main_application.MainActivity.round;

public class Accelerometer extends Service implements SensorEventListener {

    private boolean flag = false;
    private Context contexts;
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
    private boolean recalibrate = true;
    private boolean unlock = true;

    private static Timer timer = new Timer();
    public Accelerometer(){
        this.contexts = fragment_context;
        LIFO = new ArrayList<>();
        flag = false;
        mSensorManager = (SensorManager) contexts.getSystemService(contexts.SENSOR_SERVICE);
        mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        possibleBumps = new ArrayList<>();
        BumpsManual = new ArrayList<>();
    }
    private void startService() {
        recalibrate=false;
        //  timer.scheduleAtFixedRate(new mainTask(), 0, 60000);
    }


    private class mainTask extends TimerTask
    {
        public void run()
        {
            if (global_gps != null && global_gps.getmCurrentLocation().getSpeed()== 0) {
                Log.d("ACC", "sensor Accelerometer automatic re-calibrate");
                calibrate();
            }
            else
                Log.d("ACC", "sensor Accelerometer automatic re-calibrate no gps");
        }
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
            /////////////////   Log.d("ACC", "sensor Accelerometer running");
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
                Log.d("ACC", "sensor Accelerometer toto by malo byt iba raz");
            } else {

                currentData = new AccData(x, y, z);
                if (global_gps != null) {
                    /////////////////       Log.d("ACC", "sensor Accelerometer mam aj gps");
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
                                //////////////////            Log.d("ACC", "sensor Accelerometer detect bump");
                                unlock = false;
                                result = detect(location, data);
                                unlock = true;
                            }
                        }
                    }
                    else
                        //    Log.d("ACC", "sensor Accelerometer no detect bump");
                        //najstarsi prvok z LIFO sa vymaze a ulozi sa na koniec najnovsi
                        if (LIFO.size() >= LIFOsize) {
                            LIFO.remove(0);
                        }
                    LIFO.add(currentData);
                }
            }

            return result;
        }

        protected void onPostExecute(final String result) {
            //   Log.d("ACC", "sensor Accelerometer result" + result);
            if (result != null) {
                if (isEneableShowText()) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), result , Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            }
        }
    }



    public void calibrate () {
        if (recalibrate)
            startService();
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



        if (lockAdd.tryLock())
        {
            // Got the lock
            try
            {
                if (lockZoznam.tryLock())
                {
                    // Got the lock
                    try
                    {
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
                                            updatesLock=true;
                                            if (lockZoznamDB.tryLock())
                                            {
                                                // Got the lock
                                                try
                                                {
                                                    DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(this);
                                                    SQLiteDatabase database = databaseHelper.getWritableDatabase();
                                                    database.execSQL("UPDATE new_bumps  SET intensity=ROUND(" + data + ",6) WHERE ROUND(latitude,7)==ROUND("+hashLocation.getLatitude()+",7)  and ROUND(longitude,7)==ROUND("+hashLocation.getLongitude()+",7) ");
                                                    database.close();
                                                }
                                                finally
                                                {
                                                    // Make sure to unlock so that we don't cause a deadlock
                                                    lockZoznamDB.unlock();
                                                }
                                            }
                                            updatesLock=false;

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
                                                updatesLock=true;
                                                if (lockZoznamDB.tryLock())
                                                {
                                                    // Got the lock
                                                    try
                                                    {
                                                        DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(this);
                                                        SQLiteDatabase database = databaseHelper.getWritableDatabase();
                                                        database.execSQL("UPDATE new_bumps  SET intensity=ROUND(" + data + ",6) WHERE ROUND(latitude,7)==ROUND("+hashLocation.getLatitude()+",7)  and ROUND(longitude,7)==ROUND("+hashLocation.getLongitude()+",7) ");
                                                        database.close();
                                                    }
                                                    finally
                                                    {
                                                        // Make sure to unlock so that we don't cause a deadlock
                                                        lockZoznamDB.unlock();
                                                    }
                                                }
                                                updatesLock=false;
                                            }
                                            result = "under bump";
                                        }
                                        isToClose = true;
                                    }
                                }
                            }

                        }
                    }
                    finally
                    {
                        // Make sure to unlock so that we don't cause a deadlock
                        lockZoznam.unlock();
                    }
                }

                if (!isToClose) {
                    Log.d("DETECT", "new dump");
                    result = "new bump";
                    System.out.println("lat: "+ location.getLatitude() + ",lng: "+ location.getLongitude() + ",data: " + data);
                    HashMap<Location, Float> hashToArray = new HashMap();
                    location.setLatitude(round(location.getLatitude(),7));
                    location.setLongitude(round(location.getLongitude(),7));
                    hashToArray.put(location,data);
                    //zdetegovany vytlk, ktory sa prida do zoznamu vytlkov, ktore sa odoslu do databazy

                    if (lockZoznam.tryLock())
                    {
                        // Got the lock
                        try
                        {
                            possibleBumps.add(hashToArray);
                            BumpsManual.add(0);
                        }
                        finally
                        {
                            // Make sure to unlock so that we don't cause a deadlock
                            lockZoznam.unlock();
                        }
                    }

                    if (!updatesLock) {
                        updatesLock=true;
                        if (lockZoznamDB.tryLock())
                        {
                            // Got the lock
                            try
                            {
                                DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(this);
                                SQLiteDatabase database = databaseHelper.getWritableDatabase();
                                BigDecimal bd = new BigDecimal(Float.toString(data));
                                bd = bd.setScale(6, BigDecimal.ROUND_HALF_UP);
                                hashToArray.put(location,data);
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(Provider.new_bumps.LATITUDE, location.getLatitude());
                                contentValues.put(Provider.new_bumps.LONGTITUDE, location.getLongitude());
                                contentValues.put(Provider.new_bumps.MANUAL, 0);
                                contentValues.put(Provider.new_bumps.INTENSITY, String.valueOf(bd));
                                database.insert(Provider.new_bumps.TABLE_NAME_NEW_BUMPS, null, contentValues);
                                database.close();
                            }
                            finally
                            {
                                // Make sure to unlock so that we don't cause a deadlock
                                lockZoznamDB.unlock();
                            }
                        }
                        updatesLock=false;
                    }
                }

            }
            finally
            {
                // Make sure to unlock so that we don't cause a deadlock
                lockAdd.unlock();
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

    public  boolean isEneableShowText() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean alarm = prefs.getBoolean("alarm", Boolean.parseBoolean(null));
        if ((alarm) || (!alarm && MainActivity.isActivityVisible())) {
            return true;
        }
        else
            return false;
    }


}