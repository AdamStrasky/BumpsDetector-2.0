package navigationapp.main_application;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
import android.util.Log;
import android.widget.Toast;

import navigationapp.R;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static navigationapp.main_application.FragmentActivity.checkCloseDb;
import static navigationapp.main_application.FragmentActivity.checkIntegrityDB;
import static navigationapp.main_application.FragmentActivity.fragment_context;
import static navigationapp.main_application.FragmentActivity.global_gps;
import static navigationapp.main_application.FragmentActivity.isEneableShowText;
import static navigationapp.main_application.FragmentActivity.lockAdd;
import static navigationapp.main_application.FragmentActivity.lockZoznam;
import static navigationapp.main_application.FragmentActivity.updatesLock;
import static navigationapp.main_application.MainActivity.getDate;
import static navigationapp.main_application.MainActivity.round;

public class Accelerometer extends Service implements SensorEventListener {
    private boolean flag = false;
    private Context contexts = null;
    private SensorManager mSensorManager = null;
    private Sensor mAccelerometer = null;
    private float THRESHOLD = 4.5f;
    private ArrayList<HashMap<Location, Float>> possibleBumps = null;
    private ArrayList <Integer> BumpsManual = null;
    private ArrayList <Integer> typeDetect = null;
    private ArrayList <String> textDetect = null;
    private float priorityX = 0.0f;
    private float priorityY = 0.0f;
    private float priorityZ = 0.0f;
    private float[] values = new float[3]; // values is array of X,Y,Z
    private ArrayList<AccData> LIFO = null;
    private int LIFOsize = 60;
    private float delta = 0;
    private boolean recalibrate = true;
    private boolean unlock = true;
    public final String TAG = "Accelerometer";
    private  Timer timer = new Timer();
    Lock calibrateLock = new ReentrantLock();

    public Accelerometer(){
        Log.d(TAG, "initialization");
        this.contexts = fragment_context;
        LIFO = new ArrayList<>();
        flag = false;
        mSensorManager = (SensorManager) contexts.getSystemService(contexts.SENSOR_SERVICE);
        mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        possibleBumps = new ArrayList<>();
        BumpsManual = new ArrayList<>();
        typeDetect = new ArrayList<>();
        textDetect = new ArrayList<>();
    }

    private void startRecalibrate() {// spustenie pravidelneho rekalibrovania
        Log.d(TAG, "startRecalibrate start");
        recalibrate=false;
        timer.scheduleAtFixedRate(new Recalibrate(), 60000, 600000);
    }

    private class Recalibrate extends TimerTask {
        public void run() {
            recalibrate();
        }
    }

    synchronized public void recalibrate() {
        Log.d(TAG, "Recalibrate start");
        if (delta < THRESHOLD || String.valueOf(delta).equals("NaN")) {
            Log.d(TAG, "sensor Accelerometer automatic re-calibrate");
            calibrate();
        }
        else
            Log.d(TAG, "sensor Accelerometer automatic delta > THRESHOLD" + delta);
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

    public ArrayList<Integer> gettypeDetect() {
        return typeDetect;
    }

    public void addtypeDetect(int type) {
        typeDetect.add(type);
    }

    public ArrayList<String> gettextDetect() {
        return textDetect;
    }

    public void addtextDetect(String text) {
        textDetect.add(text);
    }


    @Override
    //pri zmene dat z akcelerometra nam metoda dava tieto data v premennej event.values[]
    public synchronized void onSensorChanged(SensorEvent event) {
        new SensorEventLoggerTask().execute(event);
    }

    private class SensorEventLoggerTask extends AsyncTask<SensorEvent, Void, String> {
        @Override
        protected String doInBackground(SensorEvent... events) {
            //Log.d(TAG, "sensor Accelerometer running");
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
                Log.d(TAG, " pridanie 1.hodnoty, vykoná sa len raz ");
            } else {
                currentData = new AccData(x, y, z);
                if (global_gps != null && global_gps.getmCurrentLocation()!=null) {
                       if (global_gps.getmCurrentLocation().getSpeed() > 3) {
                           // rýchlosť 10 km/h
                       }
                    final Location location = global_gps.getmCurrentLocation();
                    //prechadza sa cele LIFO, kontroluje sa, ci zmena zrychlenia neprekrocila THRESHOLD
                    for (AccData temp : LIFO) {
                        //pre kazdu os X,Y,Z sa vypocita zmena zrychlenia
                        deltaX = Math.abs(temp.getX() - currentData.getX());
                        deltaY = Math.abs(temp.getY() - currentData.getY());
                        deltaZ = Math.abs(temp.getZ() - currentData.getZ());
                        //na zaklade priorit jednotlivych osi sa vypocita celkova zmena zrychlenia
                        if (calibrateLock.tryLock()) {
                            try {
                                delta = priorityX * deltaX + priorityY * deltaY + priorityZ * deltaZ;
                            } finally {
                                calibrateLock.unlock();
                            }
                        }else {
                            return null;
                        }
                        if (String.valueOf(delta).equals("NaN")) {
                            Log.d(TAG, "NaN hodnota !!!!! ");
                            recalibrate();
                        }
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
                                result = detect(location, data);
                                unlock = true;
                            }
                        }
                    }
                    else
                        //najstarsi prvok z LIFO sa vymaze a ulozi sa na koniec najnovsi
                        if (LIFO.size() >= LIFOsize) {
                            LIFO.remove(0);
                        }
                    LIFO.add(currentData);
                }else
                    Log.d(TAG, "no gps for bump");
            }
            return result;
        }

        protected void onPostExecute(final String result) {
            if (result != null) {
                if (isEneableShowText(getApplicationContext())) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), fragment_context.getResources().getString(R.string.detect_bump),Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }
    }

    public  synchronized void calibrate () {
        Log.d(TAG, "calibrate start");
        if (recalibrate)
            startRecalibrate();
        //values[0], values[1], values[2] = data z akcelerometra pre osi X,Y,Z
        //vypocita sa percentualne rozlozenie gravitacneho zrychlenia
        //na jednotlive osi X,Y,Z
        new Thread() {
            public void run() {
                Looper.prepare();
                while (true) {
                    if (calibrateLock.tryLock()) {
                        try {
                            float sum = values[0] + values[1] + values[2];
                            priorityX = Math.abs(values[0] / sum);
                            priorityY = Math.abs(values[1] / sum);
                            priorityZ = Math.abs(values[2] / sum);
                            //normalizacia
                            sum = priorityX + priorityY + priorityZ;
                            priorityX = priorityX / sum;
                            priorityY = priorityY / sum;
                            priorityZ = priorityZ / sum;
                        } finally {
                            calibrateLock.unlock();
                            break;
                        }
                    }else {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.getMessage();
                        }
                    }
                }
                Looper.loop();
            }
        }.start();
    }

    public synchronized String detect (Location location, Float data) {
        String result = null;
        boolean isToClose = false;
        if (lockAdd.tryLock()) {
            try {
                if (lockZoznam.tryLock()) {
                    try {

                        if (possibleBumps.size() > 0) {
                            Float avgData = 0.f;
                            Location loc_avg = new Location("Location");
                            Double lat_avg = 0.0, longt_avg = 0.0;
                            for (HashMap<Location, Float> bump : possibleBumps) {
                                Iterator it = bump.entrySet().iterator();
                                while (it.hasNext()) {
                                    HashMap.Entry pair = (HashMap.Entry) it.next();
                                    Location hashLocation = (Location) pair.getKey();
                                    avgData += (Float) pair.getValue();
                                    lat_avg += hashLocation.getLatitude();
                                    longt_avg += hashLocation.getLongitude();
                                }
                            }

                            loc_avg.setLatitude(lat_avg / possibleBumps.size());
                            loc_avg.setLongitude(longt_avg / possibleBumps.size());
                            avgData = avgData / possibleBumps.size();

                            //ak je location rovnaka, neprida sa vytlk
                            if ((location.getLatitude() == loc_avg.getLatitude()) && (location.getLongitude() == loc_avg.getLongitude())) {
                                if (data > (Float) avgData) {
                                    for (HashMap<Location, Float> bump : possibleBumps) {
                                        Iterator it = bump.entrySet().iterator();
                                        while (it.hasNext()) {
                                            HashMap.Entry pair = (HashMap.Entry) it.next();
                                            Location hashLocation = (Location) pair.getKey();
                                            if ((hashLocation.getLatitude() == loc_avg.getLatitude()) && (hashLocation.getLongitude() == loc_avg.getLongitude()))
                                                pair.setValue(data);
                                                break;
                                        }
                                    }

                                    Log.d(TAG, "detect - same location, bigger data " + data);
                                    if (updatesLock.tryLock()) {
                                        try {
                                            DatabaseOpenHelper databaseHelper = null;
                                            SQLiteDatabase database = null;
                                            try {
                                                databaseHelper = new DatabaseOpenHelper(this);
                                                database = databaseHelper.getWritableDatabase();
                                                checkIntegrityDB(database);
                                                database.execSQL("UPDATE new_bumps  SET intensity=ROUND(" + data + ",6), created_at='" + getDate(location.getTime(), "yyyy-MM-dd HH:mm:ss") + "' WHERE ROUND(latitude,7)==ROUND(" + location.getLatitude() + ",7)  and ROUND(longitude,7)==ROUND(" + location.getLongitude() + ",7)  AND type=0 ");
                                            } finally {
                                                if (database != null) {
                                                    database.close();
                                                    databaseHelper.close();
                                                }
                                            }
                                            checkCloseDb(database);
                                        } finally {
                                            updatesLock.unlock();
                                        }
                                    }
                                    // result =  fragment_context.getResources().getString(R.string.same_bump);
                                    result = null;
                                } else {
                                }
                                isToClose = true;
                                Log.d(TAG, "detect - same");
                            } else {
                                double distance = getDistance((float) location.getLatitude(), (float) location.getLongitude(),
                                        (float) loc_avg.getLatitude(), (float) loc_avg.getLongitude());
                                //nie je to novy bump, pretoze z jeho okolia uz jeden pridavam (okolie 2m)
                                if (distance < 2000.0) {
                                    //do databazy sa ulozi najvacsia intenzita s akou sa dany vytlk zaznamenal
                                    if (data > (Float) avgData) {
                                        Location firstLocation = null;
                                        Log.d(TAG, "detect - under 2 meters, bigger data " + data);
                                        for (HashMap<Location, Float> bump : possibleBumps) {
                                            Iterator it = bump.entrySet().iterator();
                                            while (it.hasNext()) {
                                                HashMap.Entry pair = (HashMap.Entry) it.next();
                                                firstLocation = (Location) pair.getKey();
                                                pair.setValue(data);
                                                break;
                                            }
                                            break;
                                        }
                                        if (updatesLock.tryLock()) {
                                            try {
                                                Log.d(TAG, "detect - under write DB");
                                                DatabaseOpenHelper databaseHelper = null;
                                                SQLiteDatabase database = null;
                                                try {
                                                    databaseHelper = new DatabaseOpenHelper(this);
                                                    database = databaseHelper.getWritableDatabase();
                                                    checkIntegrityDB(database);
                                                    database.execSQL("UPDATE new_bumps  SET intensity=ROUND(" + data + ",6), created_at='" + getDate(location.getTime(), "yyyy-MM-dd HH:mm:ss") + "' WHERE ROUND(latitude,7)==ROUND(" + firstLocation.getLatitude() + ",7)  and ROUND(longitude,7)==ROUND(" + firstLocation.getLongitude() + ",7) AND type=0 ");
                                                } finally {
                                                    if (database != null) {
                                                        database.close();
                                                        databaseHelper.close();
                                                    }
                                                }
                                                checkCloseDb(database);
                                            } finally {
                                                updatesLock.unlock();
                                            }
                                        }
                                        result = null;
                                        // result = fragment_context.getResources().getString(R.string.under_bump);
                                    } else {
                                        Log.d(TAG, "detect - under 2 meters, lower data new-" + data + " old " + avgData);
                                    }
                                    Log.d(TAG, "detect - under 2 meters");
                                    isToClose = true;
                                }
                            }
                        }
                    }
                    finally {
                       lockZoznam.unlock();
                    }
                }
                if (!isToClose) {
                    Log.d(TAG, "detect - new dump");
                    result = fragment_context.getResources().getString(R.string.new_bump);
                    HashMap<Location, Float> hashToArray = new HashMap();
                    location.setLatitude(round(location.getLatitude(),7));
                    location.setLongitude(round(location.getLongitude(),7));

                    hashToArray.put(location,data);
                    //zdetegovany vytlk, ktory sa prida do zoznamu vytlkov, ktore sa odoslu do databazy
                    if (lockZoznam.tryLock()) {
                        try {
                            possibleBumps.add(hashToArray);
                            BumpsManual.add(0);
                            typeDetect.add(0);
                            textDetect.add("bump");
                        }
                        finally {
                            lockZoznam.unlock();
                        }
                    }
                    if (updatesLock.tryLock())  {
                        try {
                            Log.d(TAG, "detect - add to DB new dump");
                            DatabaseOpenHelper databaseHelper =null;
                            SQLiteDatabase database = null;
                            try {
                                databaseHelper = new DatabaseOpenHelper(this);
                                database = databaseHelper.getWritableDatabase();
                                checkIntegrityDB(database);
                                BigDecimal bd = new BigDecimal(Float.toString(data));
                                bd = bd.setScale(6, BigDecimal.ROUND_HALF_UP);
                                hashToArray.put(location, data);
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(Provider.new_bumps.LATITUDE, location.getLatitude());
                                contentValues.put(Provider.new_bumps.LONGTITUDE, location.getLongitude());
                                contentValues.put(Provider.new_bumps.MANUAL, 0);
                                contentValues.put(Provider.new_bumps.TYPE, 0);
                                contentValues.put(Provider.new_bumps.TEXT, "bump");
                                contentValues.put(Provider.new_bumps.CREATED_AT, getDate(location.getTime(), "yyyy-MM-dd HH:mm:ss"));
                                contentValues.put(Provider.new_bumps.INTENSITY, String.valueOf(bd));
                                database.insert(Provider.new_bumps.TABLE_NAME_NEW_BUMPS, null, contentValues);
                            }
                            finally {
                                if (database!=null) {
                                    database.close();
                                    databaseHelper.close();
                                }
                            }
                            checkCloseDb(database);
                        }
                        finally {
                            updatesLock.unlock();
                        }
                    }
                }
            }
            finally {
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
        //Log.d("Accelerometer", "getDistance bump - "+ 6366000*tt);
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

}