package com.example.monikas.navigationapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.monikas.navigationapp.Bump.isBetween;
import static com.example.monikas.navigationapp.MainActivity.mapView;
import static  com.example.monikas.navigationapp.Provider.bumps_detect.TABLE_NAME_BUMPS;

public class FragmentActivity extends Fragment  implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MapboxMap.OnMyLocationChangeListener
    {

    private GoogleApiClient mGoogleApiClient;
    public GPSLocator gps = null;
    public Accelerometer accelerometer;
    public static Activity fragment_context;
    public static GPSLocator global_gps;
    public static GoogleApiClient global_mGoogleApiClient;
    public static MapFragment global_MapFragment;
    //konstanty pre level (podiel rating/count pre vytlk v databaze)
    private final float ALL_BUMPS = 1.0f;
    private final float MEDIUM_BUMPS = 1.5f;
    private final float LARGE_BUMPS = 2.5f;
    //level defaultne nastaveny pre zobrazovanie vsetkych vytlkov
    public float level = ALL_BUMPS;
    public static int ZOOM_LEVEL = 18;
    JSONParser jsonParser = new JSONParser();
    boolean mServiceConnectedAcc = false;
    boolean mBoundAcc = false;
    private Accelerometer mLocnServAcc = null;
    private boolean GPS_FLAG = true;
    boolean mServiceConnectedGPS = false;
    boolean mBoundGPS = false;
    private  GPSLocator mLocnServGPS = null;
    LocationManager locationManager;
    SQLiteDatabase sb;
    DatabaseOpenHelper databaseHelper;
    private JSONArray bumps;
    private int loaded_index;
        private MapboxMap map;
    private  boolean regular_update = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

       mapView.onCreate(savedInstanceState);
       mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                map = mapboxMap;

                mapboxMap.setOnMyLocationChangeListener(FragmentActivity.this);
                mapboxMap.setMyLocationEnabled(true);




             // mapboxMap.setStyle(Style.MAPBOX_STREETS);
             ///   mapboxMap.addMarker(new MarkerOptions()
                   //     .position(new com.mapbox.mapboxsdk.geometry.LatLng(52.6885, 70.1395))
                   //     .title("Hello World!")
                     //   .snippet("Welcome to the marker."));
            }
        });



        initialization_database();
        get_loaded_index();
        // ak sa pripojím na internet požiam o update
        regular_update = true ;

        if (!isNetworkAvailable()){
            if (isEneableShowText())
                Toast.makeText(getActivity(), "Network is disabled.You are in offline mode.", Toast.LENGTH_SHORT).show();
        }
        // reaguje na zapnutie/ vypnutie GPS
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        getActivity().registerReceiver(gpsReceiver, new IntentFilter("android.location.PROVIDERS_CHANGED"));

        // ak nie je povolené GPS , upozornenie na zapnutie
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
           showGPSDisabledAlertToUser();
        } else {
            GPS_FLAG = false;
            initialization();
        }
        // pravidelný update ak nemám povolený internet
        new Timer().schedule(new Regular_upgrade(), 60000, 60000);// 3600000
    }

    public void get_loaded_index (){
        //  najvyššie uložený index po uspešnej transakcie collision
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        if (sharedPref.contains("save"))
            loaded_index = sharedPref.getInt("save", 0);
        else {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("save", 0);
            loaded_index = 0;
            editor.commit();
        }
    }

    public void initialization_database(){
        // inicializacia databazy
        databaseHelper = new DatabaseOpenHelper(getActivity());
        sb = databaseHelper.getWritableDatabase();
    }

        @Override
        public void onMyLocationChange(@Nullable Location location) {
            if (location != null) {
                if (gps != null && gps.getCurrentLatLng()!= null) {
                 map.easeCamera(CameraUpdateFactory.newLatLng(new com.mapbox.mapboxsdk.geometry.LatLng(gps.getCurrentLatLng().latitude,gps.getCurrentLatLng().longitude)));
                }
            }
        }

        private class Regular_upgrade extends TimerTask {
        @Override
        public void run() {
            regular_update = true;
        }
    }

    public void getAllBumps(Double latitude, Double longitude) {
        // vyčistenie mapy a uprava cesty
        gps.updateMap();
        SimpleDateFormat now,ago;
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        now = new SimpleDateFormat("yyyy-MM-dd");
        String now_formated = now.format(cal.getTime());
        // posun od dnesneho dna o 280 dni
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE)-280);
        ago = new SimpleDateFormat("yyyy-MM-dd");
        String ago_formated = ago.format(cal.getTime());
        // ziskam sučasnu poziciu
        LatLng convert_location =  gps.getCurrentLatLng();

        sb.beginTransaction();
        // seleknutie vytlk z oblasti a starych 280 dni
        String selectQuery = "SELECT latitude,longitude,count,manual FROM my_bumps WHERE rating/count >="+ level +" AND " +
              " ( last_modified BETWEEN '"+ago_formated+" 00:00:00' AND '"+now_formated+" 23:59:59') and  "
                + " (ROUND(latitude,0)==ROUND("+latitude+",0) and ROUND(longitude,0)==ROUND("+longitude+",0)) ";
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                // pridavanie do mapy
                gps.addBumpToMap(new LatLng(cursor.getDouble(0), cursor.getDouble(1)), cursor.getInt(2), cursor.getInt(3));
            } while (cursor.moveToNext());
        }
       if( !updatesLock)
           updatesLock=true;
        if (accelerometer!= null && accelerometer.getPossibleBumps().size() > 0) {
            notSendBumps(accelerometer.getPossibleBumps(), accelerometer.getBumpsManual());
        }else
            updatesLock=false;
         sb.setTransactionSuccessful();
         sb.endTransaction();
    }

    public void notSendBumps( ArrayList<HashMap<Location, Float>> bumps, ArrayList<Integer> bumpsManual){
        updatesLock=false;
        int rating;
        int i=0;
        if (bumps.size()> 0) {
            for (HashMap<Location, Float> bump : bumps) {
                Iterator it = bump.entrySet().iterator();
                while (it.hasNext()) {
                    HashMap.Entry pair = (HashMap.Entry) it.next();
                    Location loc = (Location) pair.getKey();
                    float data = (float) pair.getValue();
                    rating = 1;
                    if (isBetween(data, 0, 6)) rating = 1;
                    if (isBetween(data, 6, 10)) rating = 2;
                    if (isBetween(data, 10, 10000)) rating = 3;
                    if (rating == level)
                        gps.addBumpToMap(new LatLng(loc.getLatitude(), loc.getLongitude()),1,bumpsManual.get(i));
                    i++;
                }
            }
        }
    }

    ///////// pomocna funkcia, možem zmazať
    public void getAllBumpsALL() {
        sb.beginTransaction();
        String selectQuery = "SELECT b_id_bumps,rating,count FROM my_bumps ";
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
               Log.d("TEST","id "+ cursor.getInt(0));
               Log.d("TEST","rating "+ cursor.getInt(1));
               Log.d("TEST","count "+ cursor.getInt(2));
            } while (cursor.moveToNext());
        }
        sb.setTransactionSuccessful();
        sb.endTransaction();
    }

    private double lang_database,longt_database;
    private int net, b_id_database, c_id_database,updates, max_number =0 ;

    public void get_max_collision(Double latitude, Double longtitude, Integer update ) {
        SimpleDateFormat now,ago;
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        now = new SimpleDateFormat("yyyy-MM-dd");
        String now_formated = now.format(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE)-280);
        ago = new SimpleDateFormat("yyyy-MM-dd");
        String ago_formated = ago.format(cal.getTime());

        sb.beginTransaction();
        // max b_id_collisions z databazy
        String selectQuery = "SELECT * FROM collisions where b_id_collisions in (SELECT b_id_bumps FROM " + TABLE_NAME_BUMPS
                + " where (last_modified BETWEEN '"+ago_formated+" 00:00:00' AND '"+now_formated+" 23:59:59') and  "
                + " (ROUND(latitude,0)==ROUND("+latitude+",0) and ROUND(longitude,0)==ROUND("+longtitude+",0)))"
                + " ORDER BY c_id DESC LIMIT 1 ";
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                c_id_database =cursor.getInt(0);
            } while (cursor.moveToNext());
        }

        sb.setTransactionSuccessful();
        sb.endTransaction();
        updates = update;
        new Max_Collision_Number().execute();

    }

    class Max_Collision_Number extends AsyncTask<String, Void, JSONArray> {

        protected JSONArray doInBackground(String... args) {

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("latitude", String.valueOf(lang_database)));
            params.add(new BasicNameValuePair("longitude", String.valueOf(longt_database)));
            params.add(new BasicNameValuePair("b_id", String.valueOf(b_id_database)));
            params.add(new BasicNameValuePair("net", String.valueOf(net)));
            params.add(new BasicNameValuePair("c_id", String.valueOf(c_id_database)));

            JSONObject json = jsonParser.makeHttpRequest("http://sport.fiit.ngnlab.eu/update_collisions.php", "POST", params);

            try {
                int success = json.getInt("success");

                if (success == 0) {
                    // mám povolene stahovať a mám vytlky
                    bumps = json.getJSONArray("bumps");
                    return bumps;
                } else if (success == 1) {
                    // nemám povolene stahovať ale mám vytlky
                    JSONArray response = new JSONArray();
                    response.put(0, "update");
                    return response;
                } else {
                   // nemám nove vytlky
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
                return response;
            }

        }

        protected void onPostExecute(JSONArray array) {

            if (array == null) {
                // collision nemaju update ale bumps ano
                if (updates == 1 || regularUpdatesLock) {
                    GetUpdateAction();
                }else {
                    // načítam vytlky na mapu
                    if (gps!=null &&  gps.getCurrentLatLng()!=null ) {
                        LatLng convert_location = gps.getCurrentLatLng();
                        getAllBumps(convert_location.latitude, convert_location.longitude);
                    }
                }
                return;
            }

            try {
                if (array.get(0).equals("error")) {
                    // nastala chyba, nacitam mapu
                    LatLng convert_location =  gps.getCurrentLatLng();
                    getAllBumps(convert_location.latitude,convert_location.longitude);
                    return;

                } else  if (array.get(0).equals("update")) {
                    // mam vytlky na stiahnutie, ale potrebujem opravnenie od používateľa
                    GetUpdateAction();
                } else {
                    Boolean error = false ;

                    sb.beginTransaction();
                    for (int i = 0; i < bumps.length(); i++) {
                        JSONObject c = null;
                        try {
                            c = bumps.getJSONObject(i);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            error= true;
                        }

                        int c_id , b_id, intensity = 0;
                        String created_at;

                        if (c != null) {
                            try {
                                c_id = c.getInt("c_id");
                                b_id = c.getInt("b_id");
                                intensity = c.getInt("intensity");
                                created_at = c.getString("created_at");

                                // ak nove collision updatuju stare  vytlky
                                if (b_id <= loaded_index) {
                                    int rating=0;
                                    if (isBetween(intensity,0,6)) rating = 1;
                                    if (isBetween(intensity,6,10)) rating = 2;
                                    if (isBetween(intensity,10,10000)) rating = 3;
                                    sb.execSQL("UPDATE "+Provider.bumps_detect.TABLE_NAME_BUMPS+" SET rating=rating+ "+rating+", count=count +1 WHERE b_id_bumps="+b_id );
                                }

                                /* ak nastala chyba v transakcii,  musím upraviť udaje
                                  beriem od poslendej uspešnej transakcie collision po načitane max id z bumps
                                 */
                               if (b_id <= b_id_database && loaded_index < b_id) {
                                    int rating = 0;
                                    if (isBetween(intensity, 0, 6)) rating = 1;
                                    if (isBetween(intensity, 6, 10)) rating = 2;
                                    if (isBetween(intensity, 10, 10000)) rating = 3;

                                    Cursor cursor = null;
                                    String sql ="SELECT * FROM collisions WHERE b_id_collisions="+b_id;
                                    cursor= sb.rawQuery(sql,null);

                                    if(cursor.getCount()>0){
                                        //  ak ich bolo viac pripičítam
                                        sql=     "UPDATE " + Provider.bumps_detect.TABLE_NAME_BUMPS + " SET rating=rating+ " + rating + ", count=count +1 WHERE b_id_bumps=" + b_id;
                                    }else{
                                       // ak bol prvý, nastavujem na 1 count a rating prvého prijateho
                                         sql=   "UPDATE " + Provider.bumps_detect.TABLE_NAME_BUMPS + " SET rating=" + rating + ", count=1 WHERE b_id_bumps=" + b_id ;
                                    }
                                    cursor= sb.rawQuery(sql,null);
                                }

                                // insert novych udajov
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(Provider.bumps_collision.C_ID, c_id);
                                contentValues.put(Provider.bumps_collision.B_ID_COLLISIONS, b_id);
                                contentValues.put(Provider.bumps_collision.CRETED_AT, created_at);
                                contentValues.put(Provider.bumps_collision.INTENSITY, intensity);
                                sb.insert(Provider.bumps_collision.TABLE_NAME_COLLISIONS, null, contentValues);

                            } catch (JSONException e) {
                                e.printStackTrace();
                                // ak nastane chyba, tak si ju poznačim
                                error= true;
                            }
                        }
                    }
                    if (!error) {
                        // ak nenastala chyba, transakci je uspešna
                        sb.setTransactionSuccessful();
                        sb.endTransaction();
                        // uložím najvyššie b_id  z bumps po uspešnej transakcii
                        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putInt("save", max_number);
                        loaded_index = max_number;
                        editor.commit();
                    }
                    else {
                        // rollbacknem databazu
                        sb.endTransaction();
                    }
                    // načítam vytlky
                    LatLng convert_location =  gps.getCurrentLatLng();
                    getAllBumps(convert_location.latitude,convert_location.longitude);

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    public void get_max_bumps(Double langtitude, Double longtitude, Integer net) {
        SimpleDateFormat now,ago;
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        now = new SimpleDateFormat("yyyy-MM-dd");
        String now_formated = now.format(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE)-280);
        ago = new SimpleDateFormat("yyyy-MM-dd");
        String ago_formated = ago.format(cal.getTime());


        sb.beginTransaction();
        // vytiahnem najvyššie b_id z bumps
        String selectQuery = "SELECT b_id_bumps FROM " + TABLE_NAME_BUMPS
       + " where (last_modified BETWEEN '"+ago_formated+" 00:00:00' AND '"+now_formated+" 23:59:59') and  "
       + " (ROUND(latitude,0)==ROUND("+langtitude+",0) and ROUND(longitude,0)==ROUND("+longtitude+",0))"
       + " ORDER BY b_id_bumps DESC LIMIT 1 ";
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                b_id_database =cursor.getInt(0);
            } while (cursor.moveToNext());

        }
        this.net =net ;
        lang_database =langtitude;
        longt_database =longtitude;
        sb.setTransactionSuccessful();
        sb.endTransaction();
        new Max_Bump_Number().execute();

    }

     class Max_Bump_Number extends AsyncTask<String, Void, JSONArray> {

        protected JSONArray doInBackground(String... args) {

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("latitude", String.valueOf(lang_database)));
            params.add(new BasicNameValuePair("longitude", String.valueOf(longt_database)));
            params.add(new BasicNameValuePair("b_id", String.valueOf(b_id_database)));
            params.add(new BasicNameValuePair("net", String.valueOf(net)));

            JSONObject json = jsonParser.makeHttpRequest("http://sport.fiit.ngnlab.eu/update_bumps.php", "POST", params);

            try {
                 int success = json.getInt("success");
                JSONArray response = new JSONArray();
                if (success == 0) {
                    // mam nove data na stiahnutie
                    bumps = json.getJSONArray("bumps");
                    return bumps;
                } else if (success == 1) {
                    // potrebujem potvrdit nove data na stiahnutie
                    response.put(0, "update");
                    return response;
                } else {
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
                return response;
            }
        }

        protected void onPostExecute(JSONArray array) {


            if (array == null) {
              // žiadne nové data v bumps, zisti collisons
                get_max_collision(lang_database, longt_database,0);
                return;
            }

            try {
                if (array.get(0).equals("error")) {
                    return;

                } else  if (array.get(0).equals("update")) {
                    // mam nove data, zisti aj collision a potom upozorni
                    get_max_collision(lang_database, longt_database,1);
                }else  {
                    // insertujem nove data
                    Boolean error = false ;

                    sb.beginTransaction();
                    for (int i = 0; i < bumps.length(); i++) {
                        JSONObject c = null;
                        try {
                            c = bumps.getJSONObject(i);
                        } catch (JSONException e) {
                            error= true;
                            e.printStackTrace();
                        }
                        double latitude, longitude ;
                        int count, b_id, rating, manual = 0;
                        String last_modified;

                        if (c != null) {
                            try {

                                Log.d("asdasd", String.valueOf(c.getDouble("latitude")));
                                latitude = c.getDouble("latitude");
                                longitude = c.getDouble("longitude");
                                count = c.getInt("count");
                                b_id = c.getInt("b_id");
                                max_number = b_id;
                                rating = c.getInt("rating");
                                last_modified = c.getString("last_modified");
                                if (c.isNull("manual")) {
                                    manual = 0;
                                } else
                                    manual = c.getInt("manual");
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(Provider.bumps_detect.B_ID_BUMPS, b_id);
                                contentValues.put(Provider.bumps_detect.COUNT, count);
                                contentValues.put(Provider.bumps_detect.LAST_MODIFIED, last_modified);
                                contentValues.put(Provider.bumps_detect.LATITUDE, latitude);
                                contentValues.put(Provider.bumps_detect.LONGTITUDE, longitude);
                                contentValues.put(Provider.bumps_detect.MANUAL, manual);
                                contentValues.put(Provider.bumps_detect.RATING, rating);
                                sb.insert(Provider.bumps_detect.TABLE_NAME_BUMPS, null, contentValues);
                            } catch (JSONException e) {
                                error= true;
                                e.printStackTrace();
                              }
                        }
                    }
                    if (!error) {
                        // insert prebehol v poriadku, ukonči transakciu
                        sb.setTransactionSuccessful();
                        sb.endTransaction();
                        get_max_collision(lang_database, longt_database, 0);
                    } else {
                        // nastala chyba, načitaj uložene vytlky
                        sb.endTransaction();
                        getAllBumpsALL();
                        return;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

     private void GetUpdateAction(){
         // ak nemám dovolené sťahovať dáta,  ale mám update
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage("Update for new data is ready. Would you like to download it?");
        alert.setCancelable(false);
        alert.setPositiveButton("YES ",
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        dialog.cancel();
                    if (!updatesLock && regularUpdatesLock){
                        updatesLock=true;
                        ArrayList<HashMap<Location, Float>> bumpList = new ArrayList<HashMap<Location, Float>>();
                        bumpList.addAll(accelerometer.getPossibleBumps());
                        ArrayList<Integer> bumpsManual = new ArrayList<Integer> ();
                        bumpsManual.addAll(  accelerometer.getBumpsManual());
                        accelerometer.getPossibleBumps().clear();
                        accelerometer.getBumpsManual().clear();
                        saveBump(bumpList, bumpsManual,0);
                    }
                    if (updates==1) {
                            // ak povolim, stiahnem data
                        LatLng convert_location = gps.getCurrentLatLng();
                        get_max_bumps(convert_location.latitude, convert_location.longitude, 1);
                    }
                    }
                });
        alert.setNegativeButton("NO ",
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        dialog.cancel();
                        regularUpdatesLock =false;
                        // ak nepovolim, zobrazím aké mam doteraz
                        LatLng convert_location =  gps.getCurrentLatLng();
                        getAllBumps(convert_location.latitude,convert_location.longitude);
                    }
                });
        alert.show();
    }

    private BroadcastReceiver gpsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // reaguje na zmenu stavu GPS
            if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
               if(  locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER ) && GPS_FLAG) {
                   initialization();
                   GPS_FLAG = false;
               }
            }
        }
    };

    ServiceConnection mServconnGPS = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("GPS_SERV", "GPS service connected");
            GPSLocator.LocalBinder binder = (GPSLocator.LocalBinder) service;
            mLocnServGPS = binder.getService();
            mBoundGPS = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("GPS_SERV", "GPS service disconnected");
            mBoundGPS = false;
        }
    };

    ServiceConnection mServconnAcc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("ACC_SERV", "Accelerometer service connected");
            Accelerometer.LocalBinder binder = (Accelerometer.LocalBinder) service;
            mLocnServAcc = binder.getService();
            mBoundAcc = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("ACC_SERV", "Accelerometer service disconnected");
            mBoundAcc = false;
        }
    };

    private void showGPSDisabledAlertToUser(){
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage("GPS is disabled. Would you like to enable it?");
        alert.setCancelable(false);
        alert.setPositiveButton("Go to settings",
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        Intent callGPSSettingIntent = new Intent(
                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(callGPSSettingIntent);
                    }
                });
        alert.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        dialog.cancel();
                    }
                });
        alert.show();
    }

    private void showCalibrationAlert() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage("Please, calibrate your phone before start using this application.");
        alert.setCancelable(false);
        alert.setPositiveButton("Calibrate",
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        mLocnServAcc.calibrate();
                        if (isEneableShowText())
                            Toast.makeText(getActivity(),"Your phone was calibrated.",Toast.LENGTH_SHORT).show();
                   }
                });
        alert.show();
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                init_servise();
            }
        }, 500);
    }


    public void init_servise() {

        global_mGoogleApiClient= mGoogleApiClient;
        global_MapFragment =  ((MapFragment) getFragmentManager().findFragmentById(R.id.map));
        mServiceConnectedGPS =   getActivity().bindService(new Intent(getActivity().getApplicationContext(), GPSLocator.class), mServconnGPS, Context.BIND_AUTO_CREATE);

         new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                fragment_context = getActivity();
                global_gps =mLocnServGPS;
                mServiceConnectedAcc =   getActivity().bindService(new Intent(getActivity().getApplicationContext(), Accelerometer.class), mServconnAcc, Context.BIND_AUTO_CREATE);
            }
        }, 400);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
               accelerometer = mLocnServAcc;
                gps = mLocnServGPS;
                showCalibrationAlert();
            }
        }, 600);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (isEneableShowText())
            Toast.makeText(getActivity(), "GoogliApiClient connection failed", Toast.LENGTH_LONG).show();

    }
    private Bump  Handler;
     private void initialization() {

        buildGoogleApiClient();

        //po 10 sekundach sa spustia metody vykonavajuce sa pravidelne
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if(activity != null && mLocnServGPS.getCurrentLatLng()!=null ) {
                     mLocnServGPS.goTo(mLocnServGPS.getCurrentLatLng(),ZOOM_LEVEL);
                }
                nacitajDB();

                //vytlky sa do dabatazy odosielaju kazdu minutu
                  new Timer().schedule(new SendBumpsToDb(), 0, 180000);

                //mapa sa nastavuje kazde 2 minuty
                 new Timer().schedule(new MapSetter(), 0, 180000);   //120000


            }
        },10000);


    }

    public void nacitajDB(){
        sb.beginTransaction();
        String selectQuery = "SELECT latitude,longitude,intensity,manual FROM new_bumps ";
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        HashMap<Location, Float> hashToArray = new HashMap();

        if (cursor.moveToFirst()) {
            do {
                Location location = new Location("new");
                location.setLatitude(cursor.getDouble(0));
                location.setLongitude(cursor.getDouble(1));
                location.setTime(new Date().getTime());

                hashToArray.put(location, (float) cursor.getDouble(2));
                accelerometer.addPossibleBumps(location,(float) cursor.getDouble(2));
                //zdetegovany vytlk, ktory sa prida do zoznamu vytlkov, ktore sa odoslu do databazy
                accelerometer.addBumpsManual(cursor.getInt(3));


                Log.d("TEST","latitude "+ cursor.getDouble(0));
                Log.d("TEST","longitude "+ cursor.getDouble(1));
                Log.d("TEST","intensity "+ cursor.getDouble(2));
            } while (cursor.moveToNext());
        }
        sb.setTransactionSuccessful();
        sb.endTransaction();
    }

    public void stop_servise(){
         getActivity().stopService(new Intent(getActivity().getApplicationContext(), Accelerometer.class));
         if (  mServiceConnectedAcc) {
             getActivity().unbindService(mServconnAcc);
         }

        getActivity().stopService(new Intent(getActivity().getApplicationContext(), GPSLocator.class));
        if (  mServiceConnectedGPS) {
           getActivity().unbindService(mServconnGPS);
        }

    }


    private class MapSetter extends TimerTask {
         @Override
        public void run() {
            getActivity().runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    getBumpsWithLevel();
                }});
        }
    }
 public  boolean updatesLock = false;
    private boolean regularUpdatesLock = false;
    private class SendBumpsToDb extends TimerTask {

        @Override
        public void run() {

            getActivity().runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    //ak je pripojenie na internet
                    if (isNetworkAvailable()) {
                         if (!(isEneableDownload() && !isConnectedWIFI())) {

                             if (accelerometer != null) {
                                 ArrayList<HashMap<Location, Float>> list = accelerometer.getPossibleBumps();
                                 //pouzivatel je upozorneni na odosielanie vytlkov notifikaciou
                                 if (isEneableShowText())
                                     Toast.makeText(getActivity(), "Saving bumps...(" + list.size() + ")", Toast.LENGTH_SHORT).show();

                                 if (!list.isEmpty()) {
                                     if (!updatesLock) {

                                         updatesLock = true;
                                         regularUpdatesLock = false;
                                         ArrayList<HashMap<Location, Float>> lista = new ArrayList<HashMap<Location, Float>>();
                                         lista.addAll(accelerometer.getPossibleBumps());
                                         ArrayList<Integer> bumpsManual = new ArrayList<Integer>();
                                         bumpsManual.addAll(accelerometer.getBumpsManual());
                                         accelerometer.getPossibleBumps().clear();
                                         accelerometer.getBumpsManual().clear();
                                         saveBump(lista, bumpsManual, 0);
                                     }
                                 }
                             }
                         }
                    }
                    else {
                          if (isEneableShowText())
                            Toast.makeText(getActivity(), "Please, connect to network.", Toast.LENGTH_SHORT).show();
                      }
                }}
            );
        }
    }
    public void saveBump(final ArrayList<HashMap<Location, Float>> list, final ArrayList<Integer> bumpsManual, final Integer sequel) {

        if (list.size() >  sequel) {
            Iterator it = list.get(sequel).entrySet().iterator();
            HashMap.Entry pair = (HashMap.Entry) it.next();    //next
            final Location loc = (Location) pair.getKey();
            final float data = (float) pair.getValue();
            Handler = new Bump(loc, data,bumpsManual.get(sequel));
            Handler.getResponse(new CallBackReturn() {
                public void callback(String results) {
                    if ( results.equals("success")) {
                        ArrayList<HashMap<Location, Float>> lista = new ArrayList<HashMap<Location, Float>>();
                        lista.addAll(list);
                        ArrayList<Integer> bumpsManuala = new ArrayList<Integer> ();
                        bumpsManuala.addAll(bumpsManual);
                        int a =sequel;
                        lista.remove(a);
                        bumpsManuala.remove(a);
                        sb.execSQL("DELETE FROM new_bumps WHERE latitude="+loc.getLatitude() +" and  longitude="+loc.getLongitude()
                                +" and intensity="+ data );
                        saveBump(lista, bumpsManuala,sequel);
                    } else {
                        Log.d("asdfgsa","error");
                        saveBump(list, bumpsManual,sequel+1);
                    }

                }
            });

        }
        else {
            updatesLock=false;
           if (list.size() > 0) {
                mLocnServAcc.getPossibleBumps().addAll(list);
                mLocnServAcc.getBumpsManual().addAll(bumpsManual);
            } else {

            }
        }

    }

    public void getBumpsWithLevel() {
        //ak je pripojenie na internet
        if (isNetworkAvailable()) {
                // ak som na wifi alebo mám povolenie
            if (!(isEneableDownload() && !isConnectedWIFI())) {

                if (isEneableShowText())
                    // stiahnem najnovšie udaje a zobrazím mapu
                     Toast.makeText(getActivity(), "Setting map", Toast.LENGTH_SHORT).show();
                     regular_update =false;
                     mLocnServGPS.setLevel(level);
                Log.d("asdfgsa","pred null");
                  gps = mLocnServGPS;
                  if (gps!=null ) {
                      Log.d("asdfgsa","po null  null");
                      LatLng convert_location = gps.getCurrentLatLng();

                      if (convert_location != null) {
                          Log.d("asdfgsa","convert_location  null");
                          get_max_bumps(convert_location.latitude, convert_location.longitude, 1);
                      }
                  }
            }
            /// ak je to prve spustenie alebo pravidelný update
            else if (regular_update) {
             if ( accelerometer.getPossibleBumps()!=null  && accelerometer.getPossibleBumps().size() >0 ) {
                regularUpdatesLock = true;
               }
                 regular_update =false;
                LatLng convert_location =  gps.getCurrentLatLng();
                get_max_bumps(convert_location.latitude,convert_location.longitude,0);

            }
            // ak mám síce internet ale nemám povolené stahovanie, tk načítam z databazy
            else {
                regular_update =false;
                LatLng convert_location =  gps.getCurrentLatLng();
                getAllBumps(convert_location.latitude,convert_location.longitude);
            }

        }
        // nemám internet, čítam z databazy
        else {
            LatLng convert_location =  gps.getCurrentLatLng();
            if (convert_location!= null)
             getAllBumps(convert_location.latitude,convert_location.longitude);
        }
    }

    public  boolean isEneableShowText() {
        // či mám povolené ukazovať informácia aj mimo aplikácie
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Boolean alarm = prefs.getBoolean("alarm", Boolean.parseBoolean(null));
        if ((alarm) || (!alarm && MainActivity.isActivityVisible())) {
            return true;
        }
        else
            return false;
    }

    public  boolean isEneableDownload() {
        // či je dovolené sťahovať len na wifi
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Boolean net = prefs.getBoolean("net", Boolean.parseBoolean(null));
        if (net) {
            return true;
        }
        else
            return false;
    }

    public boolean isConnectedWIFI() {
        // či je pripojená wifi alebo mobilný internet
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        boolean NisConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        if (NisConnected) {
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI)
                return true;
             else
                return false;
        }
        return false;
    }
}
