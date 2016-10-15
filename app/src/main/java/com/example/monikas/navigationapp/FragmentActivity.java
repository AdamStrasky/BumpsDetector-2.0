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

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.MODE_PRIVATE;
import static com.example.monikas.navigationapp.Bump.isBetween;
import static com.example.monikas.navigationapp.DatabaseOpenHelper.DATABASE_NAME;
import static  com.example.monikas.navigationapp.Provider.bumps_detect.TABLE_NAME_BUMPS;

public class FragmentActivity extends Fragment  implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private Context context;
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
    private int ssave=0;
    private  boolean flagAktualizuj= false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        upgrade_database();


        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        if (sharedPref.contains("save")) {
             ssave = sharedPref.getInt("save", 0);
        }
        else {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("save", 0);
            ssave = 0;
            editor.commit();
        }


        Integer id = sharedPref.getInt("save", 0);
        Log.d("afaghtryjkujyht", "ulozil sasfsdfsdf "+id );


        flagAktualizuj= true ;
        if (!isNetworkAvailable()){
            if (isEneableShowText())
                Toast.makeText(getActivity(), "Network is disabled. Please, connect to network.", Toast.LENGTH_SHORT).show();

        }

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        getActivity().registerReceiver(gpsReceiver, new IntentFilter("android.location.PROVIDERS_CHANGED"));
        
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
           showGPSDisabledAlertToUser();
        } else {
            GPS_FLAG = false;
            initialization();
        }

   //  if (isNetworkAvailable())
    //  upgrade_database();
        new Timer().schedule(new Regular_upgrade(), 180000, 180000);// 3600000
    }

    public void upgrade_database(){

        databaseHelper = new DatabaseOpenHelper(getActivity());
        sb = databaseHelper.getWritableDatabase();

    }

    private class Regular_upgrade extends TimerTask {

        @Override
        public void run() {
            flagAktualizuj= true;
            Log.d("afaghtryjkujyht","nabehol aktualiyuj flag");
            }


    }

    public ArrayList<HashMap<String, String>> getAllBumps(Double latitude, Double longitude) {
        gps.updateMap();

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

        Integer id = sharedPref.getInt("save", 0);
        Log.d("afaghtryjkujyht", String.valueOf(id));

        ArrayList<HashMap<String, String>> List = new ArrayList<HashMap<String, String>>();
        Log.d("afaghtryjkujyht","načitam z databazy");
/*        "SELECT latitude,longitude,count FROM my_bumps WHERE rating/count >= '$level' AND " +
                "last_modified between date_sub(now(),INTERVAL 40 WEEK) and now() and " +
                "((ROUND(latitude,0)=ROUND('$latitude',0)) AND (ROUND(longitude,0)=ROUND('$longitude',0)))"
        */
        SimpleDateFormat databaseDateTimeFormate,format1;
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        databaseDateTimeFormate = new SimpleDateFormat("yyyy-MM-dd");
        String now = databaseDateTimeFormate.format(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE)-280);
        format1 = new SimpleDateFormat("yyyy-MM-dd");
        String ago = format1.format(cal.getTime());
        LatLng convert_location =  gps.getCurrentLatLng();

        sb.beginTransaction();
        String selectQuery = "SELECT latitude,longitude,count,manual FROM my_bumps WHERE rating/count >="+ level +" AND " +
              " ( last_modified BETWEEN '"+ago+" 00:00:00' AND '"+now+" 23:59:59') and  "
                + " (ROUND(latitude,0)==ROUND("+latitude+",0) and ROUND(longitude,0)==ROUND("+longitude+",0)) ";
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                gps.addBumpToMap(new LatLng(cursor.getDouble(0), cursor.getDouble(1)), cursor.getInt(2), cursor.getInt(3));
                List.add(map);
            } while (cursor.moveToNext());
        }
        sb.setTransactionSuccessful();
        sb.endTransaction();
        return List;
    }

    public void getAllBumpsALL() {
        sb.beginTransaction();
        String selectQuery = "SELECT b_id_bumps,rating,count FROM my_bumps ";
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
               Log.d("addgdsfbcs","id "+ cursor.getInt(0));
               Log.d("addgdsfbcs","rating "+ cursor.getInt(1));
               Log.d("addgdsfbcs","count "+ cursor.getInt(2));
            } while (cursor.moveToNext());
        }
        sb.setTransactionSuccessful();
        sb.endTransaction();

    }






    private double langt;
    private double longti;
    private int nets,b_idV,c_idV,updates ,loaded = 0, save = 0, Maxcislo =0 ;



    public void getAllBumps3(Double lang, Double longt,Integer update ) {
        Log.d("afaghtryjkujyht", " getAllBumps3  ");
        SimpleDateFormat databaseDateTimeFormate,format1;
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        databaseDateTimeFormate = new SimpleDateFormat("yyyy-MM-dd");
        String now = databaseDateTimeFormate.format(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE)-280);
        format1 = new SimpleDateFormat("yyyy-MM-dd");
        String ago = format1.format(cal.getTime());

        sb.beginTransaction();
        String selectQuery = "SELECT * FROM collisions where b_id_collisions in (SELECT b_id_bumps FROM " + TABLE_NAME_BUMPS
                + " where (last_modified BETWEEN '"+ago+" 00:00:00' AND '"+now+" 23:59:59') and  "
                + " (ROUND(latitude,0)==ROUND("+lang+",0) and ROUND(longitude,0)==ROUND("+longt+",0)))"
                + " ORDER BY c_id DESC LIMIT 1 ";
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                c_idV =cursor.getInt(0);

                Log.d("afaghtryjkujyht", " Maxnumber1   "+ String.valueOf(c_idV));
            } while (cursor.moveToNext());

        }

        sb.setTransactionSuccessful();
        sb.endTransaction();
        updates = update;
        new MaxNumber1().execute();

    }

    class MaxNumber1 extends AsyncTask<String, Void, JSONArray> {

        protected JSONArray doInBackground(String... args) {

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("latitude", String.valueOf(langt)));
            params.add(new BasicNameValuePair("longitude", String.valueOf(longti)));
            params.add(new BasicNameValuePair("b_id", String.valueOf(b_idV)));
            params.add(new BasicNameValuePair("net", String.valueOf(nets)));
            params.add(new BasicNameValuePair("c_id", String.valueOf(c_idV)));

            JSONObject json = jsonParser.makeHttpRequest("http://sport.fiit.ngnlab.eu/update_collisions.php", "POST", params);

            try {
                int success = json.getInt("success");

                if (success == 0) {
                    Log.d("afaghtryjkujyht", " Maxnumber1  stahovnie ");
                    bumps = json.getJSONArray("bumps");
                    return bumps;
                } else if (success == 1) {
                    JSONArray vvc = new JSONArray();
                    Log.d("afaghtryjkujyht", " Maxnumber1  update ");
                    vvc.put(0, "update");
                    bumps = vvc;
                    return bumps;
                } else {
                    Log.d("afaghtryjkujyht", " Maxnumber1  no update ");
                    return null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                JSONArray vvc = new JSONArray();
                try {
                    vvc.put(0, "error");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                return vvc;
            }

        }

        protected void onPostExecute(JSONArray array) {

            if (array == null) {
                Log.d("afaghtryjkujyht", "not update ");
                if (updates == 1) {
                    Log.d("afaghtryjkujyht", "ale vzvolam  update lebo bolo prve update");
                    GetUpdateAction();
                }else {
                    LatLng convert_location =  gps.getCurrentLatLng();
                    getAllBumps(convert_location.latitude,convert_location.longitude);

                }
                return;
            }

            try {
                if (array.get(0).equals("error")) {
                    LatLng convert_location =  gps.getCurrentLatLng();
                    getAllBumps(convert_location.latitude,convert_location.longitude);
                    return;

                } else  if (array.get(0).equals("update")) {
                   Log.d("afaghtryjkujyht", "not update ");
                    GetUpdateAction();
                } else {
                    Boolean error = false ;
                   //8969835689365863782
                    Log.d("afaghtryjkujyht", "insertujem nove hodnotz  ");
                    sb.beginTransaction();
                    for (int i = 0; i < bumps.length(); i++) {
                        JSONObject c = null;
                        try {
                            c = bumps.getJSONObject(i);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            error= true;
                        }

                        int c_id = 0;
                        int b_id;
                        int intensity = 0;
                        String created_at;

                        if (c != null) {
                            try {
                                c_id = c.getInt("c_id");
                                b_id = c.getInt("b_id");
                                intensity = c.getInt("intensity");
                                created_at = c.getString("created_at");
                                Log.d("afaghtryjkujyht", "nove collision "+ String.valueOf(c_id));
                          /*   if (i  == 0)
                                    b_id = c.getInt("dfffs");*/


                                Log.d("afaghtryjkujyht", "b_id collision " + String.valueOf(b_id));
                                Log.d("afaghtryjkujyht", "b_id collision " + String.valueOf(b_idV));
                                Log.d("afaghtryjkujyht", "b_id collision " + String.valueOf(ssave));

                                if (b_id <= ssave) {
                                    Log.d("afaghtryjkujyht", "updatujem collision "+ String.valueOf(b_id));
                                    int rating=0;
                                    if (isBetween(intensity,0,6)) rating = 1;
                                    if (isBetween(intensity,6,10)) rating = 2;
                                    if (isBetween(intensity,10,10000)) rating = 3;
                                    sb.execSQL("UPDATE "+Provider.bumps_detect.TABLE_NAME_BUMPS+" SET rating=rating+ "+rating+", count=count +1 WHERE b_id_bumps="+b_id );
                                }


                               if (b_id <= b_idV  && ssave < b_id) {

                                    Log.d("afaghtryjkujyht", "updatujem collision " + String.valueOf(b_id));
                                    int rating = 0;
                                    if (isBetween(intensity, 0, 6)) rating = 1;
                                    if (isBetween(intensity, 6, 10)) rating = 2;
                                    if (isBetween(intensity, 10, 10000)) rating = 3;



                                    Cursor cursor = null;
                                    String sql ="SELECT * FROM collisions WHERE b_id_collisions="+b_id;
                                    cursor= sb.rawQuery(sql,null);
                                    Log.d("Cursor Count : ", String.valueOf(cursor.getCount()));

                                    if(cursor.getCount()>0){
                                        Log.d(" afaghtryjkujyht","pridavam"+ b_id);
                                        sql=     "UPDATE " + Provider.bumps_detect.TABLE_NAME_BUMPS + " SET rating=rating+ " + rating + ", count=count +1 WHERE b_id_bumps=" + b_id;
                                    }else{
                                        Log.d(" afaghtryjkujyht","nastavujem na "+ b_id);
                                         sql=   "UPDATE " + Provider.bumps_detect.TABLE_NAME_BUMPS + " SET rating=" + rating + ", count=1 WHERE b_id_bumps=" + b_id ;
                                    }
                                    cursor= sb.rawQuery(sql,null);
                                }


                                ContentValues contentValues = new ContentValues();
                                contentValues.put(Provider.bumps_collision.C_ID, c_id);
                                contentValues.put(Provider.bumps_collision.B_ID_COLLISIONS, b_id);
                                contentValues.put(Provider.bumps_collision.CRETED_AT, created_at);
                                contentValues.put(Provider.bumps_collision.INTENSITY, intensity);
                                sb.insert(Provider.bumps_collision.TABLE_NAME_COLLISIONS, null, contentValues);






                                  /*  Log.d("afaghtryjkujyht", "rating  "+ String.valueOf(rating));
                                    ContentValues cv = new ContentValues();
                                    cv.put("rating",100);
                                    cv.put("count","count + 1");
                                    sb.update(Provider.bumps_detect.TABLE_NAME_BUMPS, cv, "b_id_bumps=="+b_id, null);*/




                             //    iba dat update na c _id
                             //   sb.update(Provider.bumps_collision.TABLE_NAME_COLLISIONS,);
                            //
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.d("afaghtryjkujyht","chzba pocas ");
                                error= true;
                            }
                        }
                    }
                    if (!error) {

                        sb.setTransactionSuccessful();
                        sb.endTransaction();
                        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putInt("save", Maxcislo);
                        ssave=Maxcislo;
                        editor.commit();


                        Integer id = sharedPref.getInt("save", 0);
                         Log.d("afaghtryjkujyht", "ulozil sasfsdfsdf "+id );
                      //  Log.d("afaghtryjkujyht", String.valueOf(id));




                        //    getAllBumpsALL();
                    }
                    else {
                        sb.endTransaction();
                        Log.d("afaghtryjkujyht","chzba na konci");
                        getAllBumpsALL();

                    }

                    LatLng convert_location =  gps.getCurrentLatLng();
                    getAllBumps(convert_location.latitude,convert_location.longitude);

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }





    public void getAllBumps2(Double lang, Double longt, Integer net) {
        Log.d("afaghtryjkujyht", "getAllBumps2 ");
        SimpleDateFormat databaseDateTimeFormate,format1;
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        databaseDateTimeFormate = new SimpleDateFormat("yyyy-MM-dd");
        String now = databaseDateTimeFormate.format(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE)-280);
        format1 = new SimpleDateFormat("yyyy-MM-dd");
        String ago = format1.format(cal.getTime());


        sb.beginTransaction();
        String selectQuery = "SELECT b_id_bumps FROM " + TABLE_NAME_BUMPS
       + " where (last_modified BETWEEN '"+ago+" 00:00:00' AND '"+now+" 23:59:59') and  "
       + " (ROUND(latitude,0)==ROUND("+lang+",0) and ROUND(longitude,0)==ROUND("+longt+",0))"
       + " ORDER BY b_id_bumps DESC LIMIT 1 ";
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                b_idV =cursor.getInt(0);
                Log.d("afaghtryjkujyht", "max b_id " +String.valueOf(b_idV));
            } while (cursor.moveToNext());

        }
        nets =net ;  ///  zmenit
      //  b_idV=0;// zmenit
        langt=lang;
        longti=longt;
        sb.setTransactionSuccessful();
        sb.endTransaction();
        new MaxNumber().execute();

    }



    class MaxNumber extends AsyncTask<String, Void, JSONArray> {

        protected JSONArray doInBackground(String... args) {

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("latitude", String.valueOf(langt)));
            params.add(new BasicNameValuePair("longitude", String.valueOf(longti)));
            params.add(new BasicNameValuePair("b_id", String.valueOf(b_idV)));
            params.add(new BasicNameValuePair("net", String.valueOf(nets)));

            JSONObject json = jsonParser.makeHttpRequest("http://sport.fiit.ngnlab.eu/update_bumps.php", "POST", params);

            try {
                 int success = json.getInt("success");
                JSONArray vvc = new JSONArray();
                if (success == 0) {
                    Log.d("afaghtryjkujyht", " MaxNumber stiahnute data");
                    bumps = json.getJSONArray("bumps");
                     return bumps;
                } else if (success == 1) {

                    Log.d("afaghtryjkujyht", "MaxNumber  data na stiahnutie ");
                    vvc.put(0, "update");
                    bumps = vvc;
                    return bumps;
                } else {
                    Log.d("afaghtryjkujyht", "MaxNumber ziadne na stiahnutie data");
                    return null;

                }
            } catch (JSONException e) {
                e.printStackTrace();
                JSONArray vvc = new JSONArray();
                try {
                    vvc.put(0, "error");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                return vvc;
            }

        }

        protected void onPostExecute(JSONArray array) {


            if (array == null) {
                Log.d("afaghtryjkujyht", " MaxNumber not update ");
                getAllBumps3(langt,longti,0);
                return;
            }

            try {
                if (array.get(0).equals("error")) {
                    return;

                } else  if (array.get(0).equals("update")) {
                    Log.d("afaghtryjkujyht", " MaxNumber  update ");
                    getAllBumps3(langt,longti,1);

                }
                else
                {
                    Boolean error = false ;
                    Log.d("afaghtryjkujyht", " MaxNumber  insert ");
                sb.beginTransaction();
                    for (int i = 0; i < bumps.length(); i++) {
                        JSONObject c = null;
                        try {
                            c = bumps.getJSONObject(i);
                        } catch (JSONException e) {
                            error= true;
                            e.printStackTrace();
                        }
                        double latitude = 0;
                        double longitude = 0;
                        int count = 0;
                        int b_id, rating;
                        int manual = 0;
                        boolean latitude1;
                        String last_modified;

                        if (c != null) {
                            try {

                            //  if (i  == 0)
                             //       latitude1 = c.getBoolean("dfffs");
                                latitude = c.getDouble("latitude");
                                longitude = c.getDouble("longitude");
                                count = c.getInt("count");
                                b_id = c.getInt("b_id");
                                 Maxcislo = b_id;
                                Log.d("afaghtryjkujyht","vkladam inset "+String.valueOf(b_id));
                             //   Log.d("INFO", String.valueOf(b_id));
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
                                Log.d("afaghtryjkujyht","chzba pocas ");
                                error= true;
                                e.printStackTrace();
                              }
                        }
                    }
                    if (!error) {
                        sb.setTransactionSuccessful();
                        sb.endTransaction();

                    /////////  //  getAllBumps3(langt, longti, 0);
                        Log.d("afaghtryjkujyht","v poriadku");
                        getAllBumps3(langt, longti, 0);
                    //    getAllBumpsALL();
                    }
                    else {
                        sb.endTransaction();
                        Log.d("afaghtryjkujyht","chzba na konci");
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
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage("Update for new data is ready. Would you like to download it?");
        alert.setCancelable(false);
        alert.setPositiveButton("YES ",
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        Log.d("afaghtryjkujyht", "stahujem znova ");
                        LatLng convert_location =  gps.getCurrentLatLng();
                        getAllBumps2(convert_location.latitude,convert_location.longitude,1);
                    }
                });
        alert.setNegativeButton("NO ",
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        Log.d("afaghtryjkujyht", "inepovolene , stahujem  ");
                        dialog.cancel();
                        LatLng convert_location =  gps.getCurrentLatLng();
                        getAllBumps(convert_location.latitude,convert_location.longitude);
                    }
                });
        alert.show();
    }

    private static int getDbVersionFromFile(File file) throws Exception {
        RandomAccessFile fp = new RandomAccessFile(file,"r");
        fp.seek(60);
        byte[] buff = new byte[4];
        fp.read(buff, 0, 4);
        return ByteBuffer.wrap(buff).getInt();
    }

    private BroadcastReceiver gpsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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

    public void saveBump(HashMap<Location, Float> bump, Integer manual) {
        Iterator it = bump.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            Location loc = (Location) pair.getKey();
            float data = (float) pair.getValue();
            new Bump(loc, data , manual);
            it.remove();
        }
    }

     private void initialization() {

        buildGoogleApiClient();

        //po 10 sekundach sa spustia metody vykonavajuce sa pravidelne
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if(activity != null) {
                    mLocnServGPS.goTo(mLocnServGPS.getCurrentLatLng(),ZOOM_LEVEL);
                }
                //mapa sa nastavuje kazde 2 minuty
                  new Timer().schedule(new MapSetter(), 0, 120000);   //120000
                //vytlky sa do dabatazy odosielaju kazdu minutu
               new Timer().schedule(new SendBumpsToDb(), 0, 60000);

            }
        },10000);


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

    private class SendBumpsToDb extends TimerTask {

        @Override
        public void run() {

            getActivity().runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    //ak je pripojenie na internet
                    if (isNetworkAvailable()) {
                        ArrayList<HashMap<Location, Float>> list = mLocnServAcc.getPossibleBumps();
                        //pouzivatel je upozorneni na odosielanie vytlkov notifikaciou
                          if (isEneableShowText())
                            Toast.makeText(getActivity(), "Saving bumps...(" + list.size() + ")", Toast.LENGTH_SHORT).show();
                      //  }

                        //kazdy vytlk v zozname vytlkov uloz do databazy
                         int i=0 ;
                         for (HashMap<Location, Float> bump : list) {
                             if (!accelerometer.getBumpsManual().isEmpty()) {
                                 saveBump(bump, accelerometer.getBumpsManual().get(i));
                             }
                             i++;
                         }
                        //vymaz zoznam
                        mLocnServAcc.getPossibleBumps().clear();
                        mLocnServAcc.getBumpsManual().clear();
                    }
                    else {
                          if (isEneableShowText())
                            Toast.makeText(getActivity(), "Please, connect to network.", Toast.LENGTH_SHORT).show();
                      }
                }}
            );
        }
    }


    public void getBumpsWithLevel() {
        //ak je pripojenie na internet
        if (isNetworkAvailable()) {

            if (!(isEneableDownload() && !isConnectedWIFI())) {

                if (isEneableShowText())
                    Toast.makeText(getActivity(), "Setting map", Toast.LENGTH_SHORT).show();
                flagAktualizuj=false;
                Log.d("afaghtryjkujyht","mam povolenie WIFI");
                mLocnServGPS.setLevel(level);
                 LatLng convert_location =  gps.getCurrentLatLng();
                getAllBumps2(convert_location.latitude,convert_location.longitude,1);

            }
            /// ak je to prve spustenie alebo ka6du i-tu hodinu
            else if ( flagAktualizuj ) {
                Log.d("afaghtryjkujyht","prve alebo pravidlne spustenie");
                flagAktualizuj=false;
                LatLng convert_location =  gps.getCurrentLatLng();
                getAllBumps2(convert_location.latitude,convert_location.longitude,0);

            }
            // na4itaj databayu
            else {
                Log.d("afaghtryjkujyht","som bez netu");
                flagAktualizuj=false;
                LatLng convert_location =  gps.getCurrentLatLng();
                getAllBumps(convert_location.latitude,convert_location.longitude);
            }

        }
        // na4itaj databayu
        else {
            LatLng convert_location =  gps.getCurrentLatLng();
            getAllBumps(convert_location.latitude,convert_location.longitude);
        }
    }

    public  boolean isEneableShowText() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Boolean alarm = prefs.getBoolean("alarm", Boolean.parseBoolean(null));
        if ((alarm) || (!alarm && MainActivity.isActivityVisible())) {
            return true;
        }
        else
            return false;
    }

    public  boolean isEneableDownload() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Boolean net = prefs.getBoolean("net", Boolean.parseBoolean(null));
        if (net) {
            return true;
        }
        else
            return false;
    }

    public boolean isConnectedWIFI() {
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
