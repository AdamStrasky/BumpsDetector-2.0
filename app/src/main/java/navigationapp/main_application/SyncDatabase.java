package navigationapp.main_application;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import navigationapp.R;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static navigationapp.main_application.Bump.isBetween;
import static navigationapp.main_application.FragmentActivity.checkCloseDb;
import static navigationapp.main_application.FragmentActivity.checkIntegrityDB;
import static navigationapp.main_application.FragmentActivity.isEneableShowText;
import static navigationapp.main_application.FragmentActivity.isNetworkAvailable;
import static navigationapp.main_application.FragmentActivity.lockAdd;
import static navigationapp.main_application.FragmentActivity.lockZoznam;
import static navigationapp.main_application.FragmentActivity.updatesLock;
import static navigationapp.main_application.Provider.bumps_detect.TABLE_NAME_BUMPS;

public class SyncDatabase {
    private JSONArray bumps = null;
    private JSONParser jsonParser = new JSONParser();
    private Accelerometer accelerometer = null;
    private GPSLocator gps = null;
    private Activity context = null;
    private MapLayer mapLayer = null;
    private boolean regular_update = false;  // flag na pravidelný update
    private boolean regularUpdatesLock = false;
    private boolean lockHandler = false;
    private int loaded_index = 0;   // maximálny index v databáze
    private double lang_database =0 , longt_database = 0;
    private int net, b_id_database = 0, c_id_database = 0, updates = 0, max_number = 0;
    private final String TAG = "SyncDatabase";

    public SyncDatabase(Accelerometer accelerometer, GPSLocator gps, Activity context, MapLayer mapLayer) {
        this.accelerometer =accelerometer;
        this.gps = gps;
        this.context = context;
        this.mapLayer =mapLayer;
        regular_update = true;
        get_loaded_index(); // vytiahnutie maximálneho indexu z databázy
        loadSaveDB(); // načítanie uložených výtlkov , ktoré neboli odoslané na server
        startGPS();
        new Timer().schedule(new Regular_upgrade(),60000, 60000);// 1 hodina == 3600000    // pravidelný update ak nemám povolený internet
    }

    private void get_loaded_index() {
        //  najvyššie uložený index po uspešnej transakcii collision
        SharedPreferences sharedPref = context.getPreferences(Context.MODE_PRIVATE);
        if (sharedPref.contains("save")) {
            loaded_index = sharedPref.getInt("save", 0);
            max_number = loaded_index;
            Log.d(TAG, "get_loaded_index - "+ loaded_index);
        }
        else {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("save", 0);
            max_number = 0;
            loaded_index = 0;
            editor.commit();
        }
    }

    private void loadSaveDB() {
        Log.d(TAG, "loadSaveDB start");
        new Thread() {
            public void run() {
                while (true) {
                    if (updatesLock.tryLock()) {
                        try {   // načítam všetky uložené výtlky ktoré neboli synchronizovane zo serverom
                            String selectQuery = "SELECT latitude,longitude,intensity,manual FROM new_bumps ";
                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(context);
                            SQLiteDatabase database = databaseHelper.getReadableDatabase();
                            checkIntegrityDB(database);
                            database.beginTransaction();
                            Cursor cursor = database.rawQuery(selectQuery, null);
                            ArrayList<HashMap< android.location.Location , Float>> hashToArray = new ArrayList<HashMap< android.location.Location , Float>>();
                            ArrayList<Integer> listA = new ArrayList<Integer>();
                            long date = new Date().getTime();
                            if (cursor != null && cursor.moveToFirst()) {
                                do {
                                    if (!cursor.isNull(0) && !cursor.isNull(1) & !cursor.isNull(2) && !cursor.isNull(3)) {
                                        android.location.Location  location = new android.location.Location ("new");
                                        location.setLatitude(cursor.getDouble(0));
                                        location.setLongitude(cursor.getDouble(1));
                                        location.setTime(date);
                                        HashMap<android.location.Location, Float> hashToArraya = new HashMap();
                                        hashToArraya.put(location, (float) cursor.getDouble(2));
                                        hashToArray.add(hashToArraya);
                                        listA.add(cursor.getInt(3));
                                        Log.d(TAG, "loadSaveDB latitude " + cursor.getDouble(0));
                                        Log.d(TAG, "loadSaveDB longitude " + cursor.getDouble(1));
                                        Log.d(TAG, "loadSaveDB intensity " + cursor.getDouble(2));
                                    }
                                } while (cursor.moveToNext());
                                if (cursor != null) {
                                    if (accelerometer != null) {
                                        while (true) {
                                            if (lockZoznam.tryLock()) {
                                                try {
                                                    Log.d(TAG, "loadSaveDB copy old bump");
                                                    accelerometer.getPossibleBumps().addAll(hashToArray);
                                                    accelerometer.getBumpsManual().addAll(listA);
                                                } finally {
                                                    Log.d(TAG, "loadSaveDB lockZoznam unlock");
                                                    lockZoznam.unlock();
                                                    break;
                                                }
                                            } else {
                                                Log.d(TAG, "loadSaveDB lockZoznam try lock");
                                                try {
                                                    Random ran = new Random();
                                                    int x = ran.nextInt(20) + 1;
                                                    Thread.sleep(x);
                                                } catch (InterruptedException e) {
                                                    e.getMessage();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            database.setTransactionSuccessful();
                            database.endTransaction();
                            database.close();
                            databaseHelper.close();
                            checkCloseDb(database);
                        } finally {
                            Log.d(TAG, "loadSaveDB unlock");
                            updatesLock.unlock();
                            break;
                        }
                    } else {
                        Log.d(TAG, "loadSaveDB try lock");
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                            e.getMessage();
                        }
                    }
                }

            }
        }.start();
    }

    private void startGPS() {
        new Timer().schedule(new SyncDb(), 0, 120000);
        Log.d(TAG, " start regular update SyncDb");
    }

    private class Regular_upgrade extends TimerTask {
        @Override
        public void run() {
            Log.d(TAG, " Regular_upgrade set true ");
            regular_update = true;
        }
    }
    ArrayList<HashMap<android.location.Location, Float>> bumpsQQ = new ArrayList<HashMap<android.location.Location, Float>>();
    ArrayList<Integer> bumpsManualQQ = new ArrayList<Integer>();

    private class SyncDb extends TimerTask {
        @Override
        public void run() {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    lockHandler = true;
                    if (isNetworkAvailable(context)) {   //  ak je pripojenie na internet
                        if (!(isEneableDownload() && !isConnectedWIFI())) { // povolené sťahovanie
                            if (accelerometer != null) {
                                ArrayList<HashMap<android.location.Location, Float>> list = accelerometer.getPossibleBumps();
                                //pouzivatel je upozorneni na odosielanie vytlkov notifikaciou
                                if (isEneableShowText(context))
                                    Toast.makeText(context, context.getResources().getString(R.string.save_bump) + "(" + list.size() + ")", Toast.LENGTH_SHORT).show();
                                if (!list.isEmpty()) {
                                    Log.d(TAG, "SyncDb zoznam nie je prázdny ");
                                    regularUpdatesLock = false;
                                    Thread t = new Thread() {
                                        public void run() {
                                            while (true) {
                                                if (lockZoznam.tryLock()) {
                                                    try {
                                                        bumpsQQ.addAll(accelerometer.getPossibleBumps());
                                                        bumpsManualQQ.addAll(accelerometer.getBumpsManual());
                                                        accelerometer.getPossibleBumps().clear();
                                                        accelerometer.getBumpsManual().clear();
                                                    } finally {
                                                        Log.d(TAG, "SyncDb lockZoznam unlock");
                                                        lockZoznam.unlock();
                                                        break;
                                                    }
                                                } else {
                                                    Log.d(TAG, "SyncDb lockZoznam try lock");
                                                    try {
                                                        Random ran = new Random();
                                                        int x = ran.nextInt(20) + 1;
                                                        Thread.sleep(x);
                                                    } catch (InterruptedException e) {
                                                        e.getMessage();
                                                    }
                                                }
                                            }
                                        }
                                    };
                                    t.start();
                                    try {
                                        t.join();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    saveBump(bumpsQQ, bumpsManualQQ, 0);
                                } else {
                                    Log.d(TAG, "SyncDb zoznam je prázdny ");
                                    getBumpsWithLevel();
                                }
                            } else {
                                Log.d(TAG, "SyncDb acc null, nemalo by sa stať !!!!!!! ");
                                getBumpsWithLevel();
                            }
                        } else {
                            Log.d(TAG, "SyncDb nepovolený internet");
                            getBumpsWithLevel();
                        }
                    } else {
                        Log.d(TAG, "SyncDb nemám internet");
                        getBumpsWithLevel();
                    }
                }
            });
        }
    }

    public void getBumpsWithLevel() {
        Log.d(TAG, "getBumpsWithLevel start");
        if (isNetworkAvailable(context)) {   //ak je pripojenie na internet
           if (!(isEneableDownload() && !isConnectedWIFI())) {  // ak som na wifi alebo mám povolenie
                if (isEneableShowText(context)) // hláška na nastavovanie mapy
                    Toast.makeText(context, context.getResources().getString(R.string.map_setting), Toast.LENGTH_SHORT).show();
                regular_update = false;
                if (gps != null && gps.getCurrentLatLng() != null) {
                    Log.d(TAG, "getBumpsWithLevel mam povolenie alebo som na wifi");
                    get_max_bumps(gps.getCurrentLatLng().latitude,gps.getCurrentLatLng().longitude, 1);
                }
           } else if (regular_update) {  // ak je to prve spustenie alebo pravidelný update
                if (accelerometer != null && accelerometer.getPossibleBumps() != null && accelerometer.getPossibleBumps().size() > 0) {
                    regularUpdatesLock = true;
                }
                regular_update = false;
               if (gps != null && gps.getCurrentLatLng() != null) {
                   Log.d(TAG, "getBumpsWithLevel prvé spustenie alebo pravidelný update");
                   get_max_bumps(gps.getCurrentLatLng().latitude, gps.getCurrentLatLng().longitude, 0);
               }

           } else { // ak mám síce internet ale nemám povolené stahovanie, tk načítam z databazy
                regular_update = false;
                if (gps != null && gps.getCurrentLatLng() != null) {
                   Log.d(TAG, "getBumpsWithLevel mám internet, ale nepovolený");
                   context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mapLayer.getAllBumps(gps.getCurrentLatLng().latitude,gps.getCurrentLatLng().longitude);
                        }
                   });
                }
           }
        } else { // nemám internet, čítam z databazy
            if (gps != null && gps.getCurrentLatLng() != null) {
                Log.d(TAG, "getBumpsWithLevel nemám internet, čítam z databazy");
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mapLayer.getAllBumps(gps.getCurrentLatLng().latitude,gps.getCurrentLatLng().longitude);
                    }
                });
            }
        }
    }

    public void get_max_bumps(final Double langtitude, final Double longtitude, final Integer nets) {
        Log.d(TAG, "get_max_bumps start ");
        Thread t =   new Thread() {
            public void run() {
                while (true) {
                    if (updatesLock.tryLock()) {
                       try {
                            SimpleDateFormat now, ago;
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(new Date());
                            now = new SimpleDateFormat("yyyy-MM-dd");
                            String now_formated = now.format(cal.getTime());
                            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE) - 280);
                            ago = new SimpleDateFormat("yyyy-MM-dd");
                            String ago_formated = ago.format(cal.getTime());
                            DatabaseOpenHelper databaseHelper =null;
                            SQLiteDatabase database = null;
                            try {
                               databaseHelper = new DatabaseOpenHelper(context);
                               database = databaseHelper.getReadableDatabase();
                               checkIntegrityDB(database);
                               database.beginTransaction();
                               // vytiahnem najvyššie b_id z bumps
                               String selectQuery = "SELECT b_id_bumps FROM " + TABLE_NAME_BUMPS
                                       + " where (last_modified BETWEEN '" + ago_formated + " 00:00:00' AND '" + now_formated + " 23:59:59') and  "
                                       + " (ROUND(latitude,1)==ROUND(" + langtitude + ",1) and ROUND(longitude,1)==ROUND(" + longtitude + ",1))"
                                       + " ORDER BY b_id_bumps DESC LIMIT 1 ";
                               Cursor cursor = null;
                               try {
                                   cursor = database.rawQuery(selectQuery, null);
                                   if (cursor.moveToFirst()) {
                                       do {
                                           b_id_database = cursor.getInt(0);
                                           Log.d(TAG, "get_max_bumps najvyšší index - " + b_id_database);
                                       } while (cursor.moveToNext());
                                   }
                               } finally {
                                   if (cursor != null)
                                       cursor.close();
                               }
                               database.setTransactionSuccessful();
                               database.endTransaction();
                            } finally {
                               if (database!=null) {
                                   database.close();
                                   databaseHelper.close();
                               }
                            }
                            checkCloseDb(database);
                       } finally {
                           Log.d(TAG, "get_max_bumps unlock ");
                           updatesLock.unlock();
                           break;
                       }
                    } else {
                        Log.d(TAG, "get_max_bumps try lock ");
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                            e.getMessage();
                        }
                    }
                }
                net = nets;  // internet flag, či ide o pravidelný update alebo môžem sťahovať
                lang_database = langtitude; // ukladam pozície, aby som všade pracoval s rovnakými
                longt_database = longtitude;
                Log.d(TAG, "get_max_bumps končí ");
            }
        };
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Max_Bump_Number().execute();
    }

    class Max_Bump_Number extends AsyncTask<String, Void, JSONArray> {

        protected JSONArray doInBackground(String... args) {
            Log.d(TAG, "Max_Bump_Number start");
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("latitude", String.valueOf(lang_database)));
            params.add(new BasicNameValuePair("longitude", String.valueOf(longt_database)));
            params.add(new BasicNameValuePair("b_id", String.valueOf(b_id_database)));
            params.add(new BasicNameValuePair("net", String.valueOf(net)));
            Log.d(TAG, "Max_Bump_Number odosielam požiadavku na server");
            JSONObject json = jsonParser.makeHttpRequest("http://sport.fiit.ngnlab.eu/update_bumps.php", "POST", params);
            if (json == null) {
                JSONArray response = new JSONArray();
                try {
                    Log.d(TAG, "Max_Bump_Number - error");
                    response.put(0, "error");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                return response;
            }
            try {
                int success = json.getInt("success");
                JSONArray response = new JSONArray();
                if (success == 0) {   // mam nove data na stiahnutie
                    bumps = json.getJSONArray("bumps");
                    Log.d(TAG, "Max_Bump_Number - new data");
                    return bumps;
                } else if (success == 1) {  // potrebujem potvrdit nove data na stiahnutie , nemám povolený net ale bol flag
                    response.put(0, "update");
                    Log.d(TAG, "Max_Bump_Number - need update");
                    return response;
                } else {
                    Log.d(TAG, "Max_Bump_Number - no data");
                    return null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                JSONArray response = new JSONArray();
                try {
                    response.put(0, "error");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                Log.d(TAG, "Max_Bump_Number - JSONException");
                return response;
            }
        }
       // Boolean errorQ = false;
        protected void onPostExecute(JSONArray array) {
            if (array == null) { // žiadne nové data v bumps, zisti collisons
                Log.d(TAG, "Max_Bump_Number onPostExecute - no data");
                get_max_collision(lang_database, longt_database, 0);
                return;
            }

            try {
                if (array.get(0).equals("error")) {
                    Log.d(TAG, "Max_Bump_Number onPostExecute - error");
                        if (gps != null && gps.getCurrentLatLng() != null) {
                            context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mapLayer.getAllBumps(gps.getCurrentLatLng().latitude, gps.getCurrentLatLng().longitude);
                            }
                        });
                        return;
                    }
                } else if (array.get(0).equals("update")) { // mam nove data, zistit aj collision a potom upozorni uživatela
                    Log.d(TAG, "Max_Bump_Number onPostExecute - update");
                    get_max_collision(lang_database, longt_database, 1);
                } else {
                    Log.d(TAG, "Max_Bump_Number onPostExecute - new data, update databazu");

                    Thread t = new Thread() {    // insertujem nove data do databazy
                        public void run() {

                            Boolean error = false;
                            while (true) {
                                if (updatesLock.tryLock()) {
                                    try {
                                        DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(context);
                                        SQLiteDatabase database = databaseHelper.getWritableDatabase();
                                        checkIntegrityDB(database);
                                        database.beginTransaction();
                                        for (int i = 0; i < bumps.length(); i++) {
                                            JSONObject data = null;
                                            try {
                                                data = bumps.getJSONObject(i);
                                            } catch (JSONException e) {
                                                error = true;  // zle parsovanie dat, zaznamenám chybu, neuložim do databázy
                                                e.printStackTrace();
                                                break;
                                            }
                                            double latitude = 0, longitude = 0;
                                            int count = 0, b_id = 0, rating = 0, manual = 0;
                                            String last_modified = null;
                                            if (data != null) {
                                                try {   // zaznamenam do databazy
                                                    latitude = data.getDouble("latitude");
                                                    longitude = data.getDouble("longitude");
                                                    Log.d(TAG, "Max_Bump_Number latitude " + latitude);
                                                    Log.d(TAG, "Max_Bump_Number longitude" + longitude);
                                                    count = data.getInt("count");
                                                    b_id = data.getInt("b_id");
                                                    Log.d(TAG, "Max_Bump_Number b_id" + b_id);
                                                    max_number = b_id;
                                                    rating = data.getInt("rating");
                                                    last_modified = data.getString("last_modified");
                                                    manual = data.getInt("manual");
                                                    ContentValues contentValues = new ContentValues();
                                                    contentValues.put(Provider.bumps_detect.B_ID_BUMPS, b_id);
                                                    contentValues.put(Provider.bumps_detect.COUNT, count);
                                                    contentValues.put(Provider.bumps_detect.LAST_MODIFIED, last_modified);
                                                    contentValues.put(Provider.bumps_detect.LATITUDE, latitude);
                                                    contentValues.put(Provider.bumps_detect.LONGTITUDE, longitude);
                                                    contentValues.put(Provider.bumps_detect.MANUAL, manual);
                                                    contentValues.put(Provider.bumps_detect.RATING, rating);
                                                    database.insert(TABLE_NAME_BUMPS, null, contentValues);
                                                } catch (JSONException e) {
                                                    error = true;
                                                    e.printStackTrace();
                                                    break;
                                                }
                                            }
                                        }
                                        if (!error) {  // insert prebehol v poriadku, ukonči transakciu
                                            database.setTransactionSuccessful();
                                            database.endTransaction();
                                            database.close();
                                            databaseHelper.close();
                                            checkCloseDb(database);
                                            Log.d(TAG, "Max_Bump_Number no error");
                                        } else { // nastala chyba, načitaj uložene vytlky
                                            Log.d(TAG, "Max_Bump_Number  error");
                                        }
                                        database.endTransaction();
                                        database.close();
                                        databaseHelper.close();
                                        checkCloseDb(database);
                                    } finally {
                                        updatesLock.unlock();
                                        break;
                                    }
                                } else {
                                    Log.d(TAG, "Max_Bump_Number try lock");
                                    try {
                                        Random ran = new Random();
                                        int x = ran.nextInt(20) + 1;
                                        Thread.sleep(x);
                                    } catch (InterruptedException e) {
                                        e.getMessage();
                                    }
                                }
                            }

                            if (!error) {
                                get_max_collision(lang_database, longt_database, 0);

                            } else {  // nastala chyba, načitaj uložene vytlky
                                if (gps != null && gps.getCurrentLatLng() != null) {
                                    context.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mapLayer.getAllBumps(gps.getCurrentLatLng().latitude, gps.getCurrentLatLng().longitude);
                                        }
                                    });
                                }

                            }
                            Log.d(TAG, "Max_Bump_Number končí");
                        }
                    };
                    t.start();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void get_max_collision(final Double latitude, final Double longtitude, final Integer update) {
        Log.d(TAG, "get_max_collision start");
        Thread t = new Thread() {
            public void run() {
                while (true) {
                    if (updatesLock.tryLock()) {
                        try {
                            SimpleDateFormat now, ago;
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(new Date());
                            now = new SimpleDateFormat("yyyy-MM-dd");
                            String now_formated = now.format(cal.getTime());
                            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE) - 280);
                            ago = new SimpleDateFormat("yyyy-MM-dd");
                            String ago_formated = ago.format(cal.getTime());
                            DatabaseOpenHelper databaseHelper =null;
                            SQLiteDatabase database = null;
                            try {
                                 databaseHelper = new DatabaseOpenHelper(context);
                                 database = databaseHelper.getReadableDatabase();
                                checkIntegrityDB(database);
                                database.beginTransaction();
                                // max b_id_collisions z databazy
                                String selectQuery = "SELECT * FROM collisions where b_id_collisions in (SELECT b_id_bumps FROM " + TABLE_NAME_BUMPS
                                        + " where (last_modified BETWEEN '" + ago_formated + " 00:00:00' AND '" + now_formated + " 23:59:59') and  "
                                        + " (ROUND(latitude,1)==ROUND(" + latitude + ",1) and ROUND(longitude,1)==ROUND(" + longtitude + ",1)))"
                                        + " ORDER BY c_id DESC LIMIT 1 ";
                                Cursor cursor = null;
                                try {
                                    cursor = database.rawQuery(selectQuery, null);
                                    if (cursor.moveToFirst()) {
                                        do {
                                            c_id_database = cursor.getInt(0);
                                            Log.d(TAG, "get_max_collision c_id_database - " + c_id_database);
                                        } while (cursor.moveToNext());
                                    }
                                } finally {
                                    if (cursor != null)
                                        cursor.close();
                                }
                                database.setTransactionSuccessful();
                                database.endTransaction();
                            } finally {
                                if (database!=null) {
                                    database.close();
                                    databaseHelper.close();
                                }
                            }
                            checkCloseDb(database);
                        } finally {
                            Log.d(TAG, "get_max_collision unlock " );
                            updatesLock.unlock();
                            break;
                        }
                    } else {
                        Log.d(TAG, "get_max_collision try lock ");
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                            e.getMessage();
                        }
                    }
                }
                updates = update;
                Log.d(TAG, "get_max_collision finish");
            }
        };
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Max_Collision_Number().execute();  // odošlem dáta najdenie nových
    }

    class Max_Collision_Number extends AsyncTask<String, Void, JSONArray> {

        protected JSONArray doInBackground(String... args) {
            Log.d(TAG, "Max_Collision_Number start");
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            Log.d("TTRREEE", "odosielam lang_database   v Max_Collision_Number " + lang_database);
            Log.d("TTRREEE", "odosielam  longt_database v Max_Collision_Number " + longt_database);
            params.add(new BasicNameValuePair("latitude", String.valueOf(lang_database)));
            params.add(new BasicNameValuePair("longitude", String.valueOf(longt_database)));
            params.add(new BasicNameValuePair("b_id", String.valueOf(b_id_database)));
            params.add(new BasicNameValuePair("net", String.valueOf(net)));
            params.add(new BasicNameValuePair("c_id", String.valueOf(c_id_database)));
            Log.d(TAG, "Max_Collision_Number odosielam požiadavku");
            JSONObject json = jsonParser.makeHttpRequest("http://sport.fiit.ngnlab.eu/update_collisions.php", "POST", params);
            if (json == null) {
                JSONArray response = new JSONArray();
                try {
                    response.put(0, "error");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                Log.d(TAG, "Max_Collision_Number - error");
                return response;
            }
            try {
                int success = json.getInt("success");

                if (success == 0) { // mám povolene stahovať a mám vytlky
                    bumps = json.getJSONArray("bumps");
                    Log.d(TAG, "Max_Collision_Number - new data");
                    return bumps;
                } else if (success == 1) { // nemám povolene stahovať ale mám vytlky
                    JSONArray response = new JSONArray();
                    Log.d(TAG, "Max_Collision_Number - need update");
                    response.put(0, "update");
                    return response;
                } else {  // nemám nove vytlky
                    Log.d(TAG, "Max_Collision_Number - no data");
                    return null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                JSONArray response = new JSONArray();
                try {
                    Log.d(TAG, "Max_Collision_Number - JSONException");
                    response.put(0, "error");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                return response;
            }
        }

        protected void onPostExecute(JSONArray array) {

            if (array == null) {
                Log.d(TAG, "Max_Collision_Number onPostExecute - null");
                // collision nemaju update ale bumps ano
                if (updates == 1 || regularUpdatesLock) {
                    Log.d(TAG, "Max_Collision_Number onPostExecute - GetUpdateAction");
                    GetUpdateAction();
                } else {
                    Log.d(TAG, "Max_Collision_Number onPostExecute - getAllBumps");
                    // načítam vytlky na mapu
                    if (gps != null && gps.getCurrentLatLng() != null) {
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mapLayer.getAllBumps(gps.getCurrentLatLng().latitude, gps.getCurrentLatLng().longitude);
                            }
                        });
                    }
                }
                return;
            }
            try {
                if (array.get(0).equals("error")) {
                    Log.d(TAG, "Max_Collision_Number onPostExecute - error");
                    // nastala chyba, nacitam mapu
                    if (gps != null && gps.getCurrentLatLng() != null) {
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            mapLayer.getAllBumps(gps.getCurrentLatLng().latitude, gps.getCurrentLatLng().longitude);
                            }
                        });
                    }
                    return;
                } else if (array.get(0).equals("update")) {
                    Log.d(TAG, "Max_Collision_Number onPostExecute -  GetUpdateAction");
                    // mam vytlky na stiahnutie, ale potrebujem opravnenie od používateľa
                    GetUpdateAction();
                } else {
                    new Thread() {
                        public void run() {
                            Log.d(TAG, "Max_Collision_Number onPostExecute -  updata DB");

                            Boolean error = false;
                            while (true) {
                                if (updatesLock.tryLock()) {
                                    try {
                                        DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(context);
                                        SQLiteDatabase database = databaseHelper.getWritableDatabase();
                                        checkIntegrityDB(database);
                                        database.beginTransaction();
                                        for (int i = 0; i < bumps.length(); i++) {
                                            JSONObject data = null;
                                            try {
                                                data = bumps.getJSONObject(i);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                                error = true;
                                            }
                                            int c_id = 0, b_id = 0;
                                            double intensity = 0;
                                            String created_at = null;
                                            Log.d(TAG, "Max_Collision_Number b_id_database - " + b_id_database);
                                            Log.d(TAG, "Max_Collision_Number loaded_index - " + loaded_index);
                                            if (data != null) {
                                                try {
                                                    c_id = data.getInt("c_id");
                                                    b_id = data.getInt("b_id");
                                                    intensity = data.getDouble("intensity");
                                                    created_at = data.getString("created_at");
                                                    Log.d(TAG, "Max_Collision_Number c_id - " + c_id);
                                                    Log.d(TAG, "Max_Collision_Number b_id - " + b_id);
                                                    Log.d(TAG, "Max_Collision_Number intensity - " + intensity);
                                                    Log.d(TAG, "Max_Collision_Number created_at - " + created_at);

                                                    // ak nove collision updatuju stare  vytlky
                                                    if (b_id <= loaded_index) {
                                                        Log.d(TAG, "Max_Collision_Number nove collision update stare");
                                                        int rating = 0;
                                                        if (isBetween((float) intensity, 0, 6))
                                                            rating = 1;
                                                        if (isBetween((float) intensity, 6, 10))
                                                            rating = 2;
                                                        if (isBetween((float) intensity, 10, 10000))
                                                            rating = 3;
                                                        database.execSQL("UPDATE " + TABLE_NAME_BUMPS + " SET rating=rating+ " + rating + ", count=count +1, last_modified='"+created_at+"' WHERE b_id_bumps=" + b_id);
                                                    }

                                                    /* ak nastala chyba v transakcii,  musím upraviť udaje
                                                    beriem od poslendej uspešnej transakcie collision po načitane max id z bumps
                                                     */
                                                    // nove collision aj nove update takže počet dávam od 1 a pripočítavam
                                                    if (b_id <= b_id_database && loaded_index < b_id) {
                                                        Log.d(TAG, "Max_Collision_Number nove collision aj bump");
                                                        int rating = 0;
                                                        if (isBetween((float) intensity, 0, 6))
                                                            rating = 1;
                                                        if (isBetween((float) intensity, 6, 10))
                                                            rating = 2;
                                                        if (isBetween((float) intensity, 10, 10000))
                                                            rating = 3;
                                                        Cursor cursor = null;
                                                        String sql = "SELECT * FROM collisions WHERE b_id_collisions=" + b_id;

                                                        try {
                                                            cursor = database.rawQuery(sql, null);
                                                            if (cursor.getCount() > 0) {
                                                                Log.d(TAG, "Max_Collision_Number viac b_id");
                                                                //  ak ich bolo viac pripičítam
                                                                sql = "UPDATE " + TABLE_NAME_BUMPS + " SET rating=rating+ " + rating + ", count=count +1, last_modified='"+created_at+"' WHERE b_id_bumps=" + b_id;
                                                            } else {
                                                                Log.d(TAG, "Max_Collision_Number prvý nový b_id");
                                                                // ak bol prvý, nastavujem na 1 count a rating prvého prijateho
                                                                sql = "UPDATE " + TABLE_NAME_BUMPS + " SET rating=" + rating + ", count=1, last_modified='"+created_at+"'  WHERE b_id_bumps=" + b_id;
                                                            }
                                                            database.execSQL(sql);
                                                        } finally {
                                                            if (cursor != null)
                                                                cursor.close();
                                                        }
                                                    }

                                                    // insert novych udajov
                                                    ContentValues contentValues = new ContentValues();
                                                    contentValues.put(Provider.bumps_collision.C_ID, c_id);
                                                    contentValues.put(Provider.bumps_collision.B_ID_COLLISIONS, b_id);
                                                    contentValues.put(Provider.bumps_collision.CRETED_AT, created_at);
                                                    contentValues.put(Provider.bumps_collision.INTENSITY, intensity);
                                                    database.insert(Provider.bumps_collision.TABLE_NAME_COLLISIONS, null, contentValues);

                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                    // ak nastane chyba, tak si ju poznačim
                                                    error = true;
                                                }
                                            }
                                        }
                                        if (!error) {
                                            // ak nenastala chyba, transakci je uspešna
                                            database.setTransactionSuccessful();
                                            database.endTransaction();
                                            database.close();
                                            databaseHelper.close();
                                            checkCloseDb(database);
                                            // uložím najvyššie b_id  z bumps po uspešnej transakcii
                                            SharedPreferences sharedPref = context.getPreferences(Context.MODE_PRIVATE);
                                            SharedPreferences.Editor editor = sharedPref.edit();
                                            editor.putInt("save", max_number);
                                            loaded_index = max_number;
                                            editor.commit();
                                            Log.d(TAG, "Max_Collision_Number no error, save max id - " + max_number);
                                        } else {
                                            // rollbacknem databazu
                                            database.endTransaction();
                                            database.close();
                                            databaseHelper.close();
                                            checkCloseDb(database);
                                            Log.d(TAG, "Max_Collision_Number  error " );

                                        }
                                    } finally {
                                        Log.d(TAG, "Max_Collision_Number  unlock " );
                                        updatesLock.unlock();
                                        break;
                                    }
                                } else {
                                    Log.d(TAG, "Max_Collision_Number  try lock " );
                                    try {
                                        Random ran = new Random();
                                        int x = ran.nextInt(20) + 1;
                                        Thread.sleep(x);
                                    } catch (InterruptedException e) {
                                        e.getMessage();
                                    }
                                }
                            }
                            // načítam vytlky
                            if (gps != null && gps.getCurrentLatLng() != null) {
                                context.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mapLayer.getAllBumps(gps.getCurrentLatLng().latitude, gps.getCurrentLatLng().longitude);
                                    }
                                });
                            }
                            Log.d(TAG, "Max_Collision_Number  končí " );

                        }
                    }.start();

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void GetUpdateAction() {
        Log.d(TAG, "GetUpdateAction  start " );
        // ak nemám dovolené sťahovať dáta,  ale mám update
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setMessage(context.getResources().getString(R.string.update));
        alert.setCancelable(false);
        alert.setPositiveButton(context.getResources().getString(R.string.yes),
                 new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        if (regularUpdatesLock) {
                            lockHandler = false;
                           Thread t = new Thread() {
                                public void run() {
                                        while (true) {
                                        if (lockZoznam.tryLock()) {
                                            try {
                                                bumpsQQ.addAll(accelerometer.getPossibleBumps());
                                                bumpsManualQQ.addAll(accelerometer.getBumpsManual());
                                                accelerometer.getPossibleBumps().clear();
                                                accelerometer.getBumpsManual().clear();
                                            } finally {
                                                Log.d(TAG, "GetUpdateAction lockZoznam unlock");
                                                lockZoznam.unlock();
                                                break;
                                            }
                                        } else {
                                            Log.d(TAG, "GetUpdateAction lockZoznam try lock");
                                            try {
                                                Random ran = new Random();
                                                int x = ran.nextInt(20) + 1;
                                                Thread.sleep(x);
                                            } catch (InterruptedException e) {
                                                e.getMessage();
                                            }
                                        }
                                    }


                                }
                            };
                            t.start();
                            try {
                                t.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            saveBump(bumpsQQ, bumpsManualQQ, 0);
                        } else if (updates == 1) {
                            Log.d(TAG, "GetUpdateAction updates == 1 v getupdate" );
                            updates = 0;
                            // ak povolim, stiahnem data
                            get_max_bumps(gps.getCurrentLatLng().latitude, gps.getCurrentLatLng().longitude, 1);
                        }
                    }
                });
        alert.setNegativeButton(context.getResources().getString(R.string.nope),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        regularUpdatesLock = false;
                        // ak nepovolim, zobrazím aké mam doteraz
                        if (gps != null && gps.getCurrentLatLng() != null) {
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                mapLayer.getAllBumps(gps.getCurrentLatLng().latitude, gps.getCurrentLatLng().longitude);
                                }
                            });
                        }
                    }
                });
        alert.show();
    }

    private Bump Handler;
    private boolean lock = true;
    private ArrayList<HashMap< android.location.Location, Float>> listHelp = null;
    private ArrayList<Integer> bumpsManualHelp = null;
    private Integer poradie = 0;

    public void saveBump(ArrayList<HashMap< android.location.Location, Float>> list, ArrayList<Integer> bumpsManual, Integer sequel) {
        Log.d(TAG, "saveBump start" );
        listHelp = list;
        bumpsManualHelp = bumpsManual;
        poradie = sequel;
        new Thread() {
            public void run() {
                Looper.prepare();
                while (true) {
                    if (lock) {
                        if (listHelp!=null && !listHelp.isEmpty() && listHelp.size() > poradie) {
                            Iterator it = listHelp.get(poradie).entrySet().iterator();
                            HashMap.Entry pair = (HashMap.Entry) it.next();
                            final  android.location.Location  loc = ( android.location.Location) pair.getKey();
                            final float data = (float) pair.getValue();
                            Handler = new Bump(loc, data, bumpsManualHelp.get(poradie));
                            Handler.getResponse(new CallBackReturn() {
                                public void callback(String results) {
                                    if (results.equals("success")) {
                                        Log.d(TAG, "saveBump success " );
                                        int num = poradie;
                                        while (true) {
                                            if (updatesLock.tryLock()) {
                                                try {
                                                    DatabaseOpenHelper databaseHelper =null;
                                                    SQLiteDatabase database = null;
                                                    try {
                                                        databaseHelper = new DatabaseOpenHelper(context);
                                                        database = databaseHelper.getWritableDatabase();
                                                        checkIntegrityDB(database);
                                                        // ak mi prišlo potvrdenie o odoslaní, mažem z db
                                                        database.beginTransaction();
                                                        Log.d(TAG, "saveBump success delete db ");
                                                        database.execSQL("DELETE FROM new_bumps WHERE ROUND(latitude,7)= ROUND(" + loc.getLatitude() + ",7)  and ROUND(longitude,7)= ROUND(" + loc.getLongitude() + ",7)"
                                                                + " and  ROUND(intensity,6)==ROUND(" + data + ",6) and manual=" + bumpsManualHelp.get(num) + "");
                                                        Log.d("TEST", "mazem");
                                                        Log.d(TAG, "mazem " + listHelp.get(num).toString());
                                                        Log.d(TAG, "mazem " + bumpsManualHelp.get(num).toString());
                                                        listHelp.remove(num);
                                                        bumpsManualHelp.remove(num);
                                                        database.setTransactionSuccessful();
                                                        database.endTransaction();
                                                    }
                                                    finally {
                                                        if (database!=null) {
                                                            database.close();
                                                            databaseHelper.close();
                                                        }
                                                    }
                                                    checkCloseDb(database);

                                                } finally {
                                                    Log.d(TAG, "saveBump unlock " );
                                                    updatesLock.unlock();
                                                    break;
                                                }
                                            } else {
                                                Log.d(TAG, "saveBump try lock " );
                                                try {
                                                    Random ran = new Random();
                                                    int x = ran.nextInt(20) + 1;
                                                    Thread.sleep(x);
                                                } catch (InterruptedException e) {
                                                    e.getMessage();
                                                }
                                            }

                                        }
                                        lock = true;
                                    } else {
                                        // nastala chyba, nemažem
                                        Log.d(TAG, "saveBump error" );
                                        int num = poradie;
                                        num++;
                                        poradie = num;
                                        lock = true;
                                    }
                                }
                            });
                        } else {
                            break;
                        }
                    }
                }
                Log.d(TAG, "saveBump odosielanie na server skončilo" );
                while (true) {
                    if (lockAdd.tryLock()) {   // zamknem zoznam, a tie čo sa mi nepodarilo odoslať vrátim
                        try {
                            while (true) {
                                if (updatesLock.tryLock()) {
                                    try {
                                        if (listHelp.size() > 0 && accelerometer.getPossibleBumps().size() > 0) {
                                            Log.d(TAG, "saveBump aktualizujem zoznam" );
                                            int i = 0;
                                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(context);
                                            SQLiteDatabase database = databaseHelper.getWritableDatabase();
                                            checkIntegrityDB(database);
                                            for (HashMap< android.location.Location, Float> oldList : listHelp) {
                                                Iterator oldListIteam = oldList.entrySet().iterator();
                                                while (oldListIteam.hasNext()) {
                                                    HashMap.Entry oldData = (HashMap.Entry) oldListIteam.next();
                                                    android.location.Location oldLocation = ( android.location.Location) oldData.getKey();
                                                    i = 0;
                                                    for (HashMap< android.location.Location, Float> newList : accelerometer.getPossibleBumps()) {

                                                        Iterator newListIteam = newList.entrySet().iterator();
                                                        while (newListIteam.hasNext()) {
                                                            HashMap.Entry newData = (HashMap.Entry) newListIteam.next();
                                                            android.location.Location newLocation = ( android.location.Location) newData.getKey();
                                                            // ak sa zhoduju location, tak updatujem hodnoty
                                                            if ((oldLocation.getLatitude() == newLocation.getLatitude()) &&
                                                                    (oldLocation.getLongitude() == newLocation.getLongitude())) {
                                                                // staršia hodnota je väčšia, tak prepíšem na väčšiu hodnotu
                                                                if ((Float) oldData.getValue() > (Float) newData.getValue())
                                                                    accelerometer.getPossibleBumps().get(0).put(newLocation, (Float) oldData.getValue());
                                                                // ak je stará hodnota menšia, updatujem databazu kde je uložená menšia
                                                                if ((Float) oldData.getValue() < (Float) newData.getValue())
                                                                    database.execSQL("UPDATE new_bumps  SET intensity=ROUND(" + (Float) newData.getValue() + ",6) WHERE latitude=" + oldLocation.getLatitude() + " and  longitude=" + oldLocation.getLongitude()
                                                                            + " and  ROUND(intensity,6)==ROUND(" + (Float) oldData.getValue() + ",6)");
                                                                // mažem s pomocného zoznamu updatnuté hodnoty
                                                                listHelp.remove(i);
                                                                bumpsManualHelp.remove(i);
                                                            }
                                                        }
                                                        i++;
                                                    }
                                                }
                                            }
                                            database.close();
                                            databaseHelper.close();
                                            checkCloseDb(database);
                                            // doplnim do zoznamu povodné, ktoré sa nezmenili
                                            accelerometer.getPossibleBumps().addAll(listHelp);
                                            accelerometer.getBumpsManual().addAll(bumpsManualHelp);
                                        } else if (listHelp.size() > 0) {
                                            // nepribudli nové hodnoty, tak tam vrátim pôvodné
                                            accelerometer.getPossibleBumps().addAll(listHelp);
                                            accelerometer.getBumpsManual().addAll(bumpsManualHelp);
                                        }
                                    } finally {
                                        updatesLock.unlock();
                                        break;
                                    }
                                } else {
                                    Log.d(TAG, "saveBump db try lock" );
                                    try {
                                        Thread.sleep(5);
                                    } catch (InterruptedException e) {
                                        e.getMessage();
                                    }
                                }
                            }
                        } finally {
                            Log.d(TAG, "saveBump zoznam unlock" );
                            lockAdd.unlock();
                            break;
                        }
                    } else {
                        try {
                            Log.d(TAG, "saveBump zoznam try lock" );
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                        }
                    }
                }

                listHelp = null;
                bumpsManualHelp = null;

                if (regularUpdatesLock) {
                    Log.d(TAG, "updates == 1 v save" );
                    // updates == 1 v save, takže je potrebne  vyžiadať update
                    updates = 0;
                    regularUpdatesLock = false;
                    lockHandler = false;
                    get_max_bumps(gps.getCurrentLatLng().latitude, gps.getCurrentLatLng().longitude, 1);

                } else if (lockHandler) {
                    lockHandler = false;
                    getBumpsWithLevel();
                }
                Looper.loop();
            }
        }.start();
    }

    private  boolean isConnectedWIFI() {   // či je pripojená wifi alebo mobilný internet
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        Log.d(TAG, "isConnectedWIFI stav " + isConnected);
        if (isConnected) {
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI)
                return true;
            else
                return false;
        }
        return false;
    }

    private boolean isEneableDownload() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d(TAG, "isEneableDownload stav - " +String.valueOf(prefs.getBoolean("net", Boolean.parseBoolean(null))));
        return prefs.getBoolean("net", Boolean.parseBoolean(null));
    }
}
