package com.example.monikas.navigationapp;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.MapFragment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.BIND_AUTO_CREATE;


public class BlankFragment extends Fragment  implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private Context context;
    public GPSLocator gps;
    public static Activity contexts;
    public static GPSLocator gpss;
    public Accelerometer accelerometer;
    //konstanty pre level (podiel rating/count pre vytlk v databaze)
    private final float ALL_BUMPS = 1.0f;
    private final float MEDIUM_BUMPS = 1.5f;
    private final float LARGE_BUMPS = 2.5f;
    //level defaultne nastaveny pre zobrazovanie vsetkych vytlkov
    public float level = ALL_BUMPS;
    public static int ZOOM_LEVEL = 18;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Activity activityobj = this.getActivity();

        if (!isNetworkAvailable()){
             Toast.makeText(activityobj,"Network is disabled. Please, connect to network.",Toast.LENGTH_SHORT).show();
        }
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            showGPSDisabledAlertToUser();
        }






     /*   Log.d("SVTEST", "Activity onStart");
        mServiceConnected = getActivity().bindService(new Intent(
                        "com.example.monikas.navigationapp.ACCELEROMETER"), mServconn,
                BIND_AUTO_CREATE);*/

       buildGoogleApiClient();
      showCalibrationAlert();
        //po 10 sekundach sa spustia metody vykonavajuce sa pravidelne
      new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                gps.goTo(gps.getCurrentLatLng(),ZOOM_LEVEL);
                //mapa sa nastavuje kazde 2 minuty
                new Timer().schedule(new MapSetter(), 0, 120000);   //120000
                //vytlky sa do dabatazy odosielaju kazdu minutu
                new Timer().schedule(new SendBumpsToDb(), 0, 60000);
            }
        }, 10000);
    }

    /*********************/
    boolean mServiceConnected = false;
    boolean mBound = false;
    private Accelerometer mLocnServ;

    ServiceConnection mServconn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("SVTEST", "Activity service connected");
            Accelerometer.LocalBinder binder = (Accelerometer.LocalBinder) service;
            mLocnServ = binder.getService();
            // Can't call this methodInTheService UNTIL IT'S BOUND!
            mLocnServ.methodInTheService();

            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getActivity(), "Service is connected", Toast.LENGTH_SHORT).show();

                }



            });
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("SVTEST", "Activity service disconnected");
            mBound = false;
        }
    };


 /*  @Override
    public void onStart() {
       super.onStart();
       Log.d("SVTEST", "Activity onStart");







       mServiceConnected=   getActivity().bindService(new Intent(getActivity().getApplicationContext(), Accelerometer.class), mServconn, Context.BIND_AUTO_CREATE);

       if (mServiceConnected)
       Log.d("SVTEST", "aaaaa onStart"+ mServiceConnected);
       else
           Log.d("SVTEST", "eeeeee onStart"+ mServiceConnected);

   }*/
      /*  super.onStart();
        Log.d("SVTEST", "Activity onStart");
        mServiceConnected = getActivity().bindService(new Intent(
                        "com.example.monikas.navigationapp.Accelerometer"), mServconn,
                BIND_AUTO_CREATE);
       Log.d("SVTEST", "Activity onStart");
*/
     // mLocnServ.methodInTheService();

 /*   @Override
    protected void onResume() {
        super.onResume();
        Log.d("SVTEST", "Activity onResume");
    }
    @Override
    public void onPause() {
        Log.d("SVTEST", "Activity onPause");
        super.onPause();
    }
    @Override
    public void onStop() {
        Log.d("SVTEST", "Activity onStop");
        if (mBound) {
            unbindService(mServconn);
            mBound = false;
        }
        super.onStop();
    }*/


    /*************************/






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
                        mLocnServ.calibrate();
                       // Toast.makeText(context,"Your phone was calibrated.",Toast.LENGTH_SHORT).show();
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
                init();
            }
        }, 500);
    }


    public void init () {

        gps = new GPSLocator(mGoogleApiClient, ((MapFragment) getFragmentManager().findFragmentById(R.id.map)));
      //  accelerometer = new Accelerometer(getActivity(), gps);

        contexts=getActivity();
        gpss=gps;
        Log.d("SVTEST", "Activity onStart");







        mServiceConnected=   getActivity().bindService(new Intent(getActivity().getApplicationContext(), Accelerometer.class), mServconn, Context.BIND_AUTO_CREATE);

        if (mServiceConnected)
            Log.d("SVTEST", "aaaaa onStart"+ mServiceConnected);
        else
            Log.d("SVTEST", "eeeeee onStart"+ mServiceConnected);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
      //  Toast.makeText(this, "GoogliApiClient connection failed", Toast.LENGTH_LONG).show();
    }

    public void saveBump(HashMap<Location, Float> bump) {
        Iterator it = bump.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            Location loc = (Location) pair.getKey();
            float data = (float) pair.getValue();
            new Bump(loc, data);
            it.remove();
        }
    }


    private class MapSetter extends TimerTask {

        @Override
        public void run() {

            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getActivity(), "AAAAAAAAA", Toast.LENGTH_LONG).show();

                }
            });

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
                        ArrayList<HashMap<Location, Float>> list = mLocnServ.getPossibleBumps();
                        //pouzivatel je upozorneni na odosielanie vytlkov notifikaciou
                      //  Toast.makeText(context, "Saving bumps...(" + list.size() + ")", Toast.LENGTH_SHORT).show();
                        //kazdy vytlk v zozname vytlkov uloz do databazy
                        for (HashMap<Location, Float> bump : list) {
                            saveBump(bump);
                        }
                        //vymaz zoznam
                        mLocnServ.getPossibleBumps().clear();
                    }
                    else {
                       // Toast.makeText(context, "Please, connect to network.", Toast.LENGTH_SHORT).show();
                    }
                }});
        }
    }


    public void getBumpsWithLevel() {
        //ak je pripojenie na internet
        if (isNetworkAvailable()) {
            //pouzivatelovi sa zobrazi notifikacia Setting map
          //  Toast.makeText(this, "Setting map", Toast.LENGTH_SHORT).show();
            //level je globalna premenna, na zaklade ktorej sa filtruju zobrazovane vytlky
            gps.setLevel(level);
            gps.updateMap();
        }
        else {
           // Toast.makeText(context, "Please, connect to network.", Toast.LENGTH_SHORT).show();
        }
    }


}
