package com.example.monikas.navigationapp;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.GpsStatus;
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

import static java.security.AccessController.getContext;


public class FragmentActivity extends Fragment  implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

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

    boolean mServiceConnectedAcc = false;
    boolean mBoundAcc = false;
    private Accelerometer mLocnServAcc = null;
    private boolean GPS_FLAG = true;
    boolean mServiceConnectedGPS = false;
    boolean mBoundGPS = false;
    private  GPSLocator mLocnServGPS = null;
    public static final String GPS_ENABLED_CHANGE_ACTION = "android.location.GPS_ENABLED_CHANGE";
    private boolean isGPSEnabled = false;
    protected boolean isVisible;
    LocationManager locationManager;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (!isNetworkAvailable()){
             Toast.makeText(getActivity(),"Network is disabled. Please, connect to network.",Toast.LENGTH_SHORT).show();
        }
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        getActivity().registerReceiver(gpsReceiver, new IntentFilter("android.location.PROVIDERS_CHANGED"));
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(!isGPSEnabled) {
            Log.d("onGpsStatusChanged", "nezapnuta gps ");
            showGPSDisabledAlertToUser();
        } else {
            GPS_FLAG = false;
            aaaa();
        }


    }




    @Override
    public  void setUserVisibleHint(boolean visible){
        super.setUserVisibleHint(visible);
        if (visible){
            isVisible=true;
        }
        else
            isVisible=false;
    }
    private BroadcastReceiver gpsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
               if(  locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER ) && GPS_FLAG) {
                   aaaa();
                   GPS_FLAG = false;
               }
                else
                   Log.d("onGpsStatusChanged"," brodcast ylz");

            }
        }
    };




    ServiceConnection mServconnGPS = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("SVTEST", "GPS service connected");
            GPSLocator.LocalBinder binder = (GPSLocator.LocalBinder) service;
            mLocnServGPS = binder.getService();
            mBoundGPS = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("DISC", "GPS service disconnected");
            mBoundGPS = false;
        }
    };

    ServiceConnection mServconnAcc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("SVTEST", "Accelerometer service connected");
            Accelerometer.LocalBinder binder = (Accelerometer.LocalBinder) service;
            mLocnServAcc = binder.getService();
            mBoundAcc = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("DISC", "Accelerometer service disconnected");
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
                init();
            }
        }, 500);
    }


    public void init () {

        global_mGoogleApiClient= mGoogleApiClient;
        global_MapFragment =  ((MapFragment) getFragmentManager().findFragmentById(R.id.map));
        mServiceConnectedGPS =   getActivity().bindService(new Intent(getActivity().getApplicationContext(), GPSLocator.class), mServconnGPS, Context.BIND_AUTO_CREATE);

         new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                fragment_context =getActivity();
                global_gps =mLocnServGPS;
                mServiceConnectedAcc =   getActivity().bindService(new Intent(getActivity().getApplicationContext(), Accelerometer.class), mServconnAcc, Context.BIND_AUTO_CREATE);
            }
        }, 400);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
               accelerometer = mLocnServAcc;
                gps = mLocnServGPS;
            }
        }, 600);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
       Toast.makeText(getActivity(), "GoogliApiClient connection failed", Toast.LENGTH_LONG).show();
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




    private void aaaa() {
        Log.d("onGpsStatusChanged", "zapnuta gps po broadcaste");
        showCalibrationAlert();
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
        }, 10000);

    }



    public void konci(){

Log.d("DISC","111");
     getActivity().stopService(new Intent(getActivity().getApplicationContext(), Accelerometer.class));
     if (  mServiceConnectedAcc) {
         getActivity().unbindService(mServconnAcc);
         Log.d("DISC","2222");
     }

        Log.d("DISC","3333");
      getActivity().stopService(new Intent(getActivity().getApplicationContext(), GPSLocator.class));
        if (  mServiceConnectedGPS) {
            Log.d("DISC","444");
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
                       Toast.makeText(getActivity(), "Saving bumps...(" + list.size() + ")", Toast.LENGTH_SHORT).show();
                        //kazdy vytlk v zozname vytlkov uloz do databazy

                        for (HashMap<Location, Float> bump : list) {
                            saveBump(bump);
                            Log.d("VLAKNO","prebehlo");
                        }
                        //vymaz zoznam
                        mLocnServAcc.getPossibleBumps().clear();
                    }
                    else {
                       Toast.makeText(getActivity(), "Please, connect to network.", Toast.LENGTH_SHORT).show();
                    }
                }});
        }
    }


    public void getBumpsWithLevel() {
        //ak je pripojenie na internet
        if (isNetworkAvailable()) {
            //pouzivatelovi sa zobrazi notifikacia Setting map
            Toast.makeText(getActivity(), "Setting map", Toast.LENGTH_SHORT).show();
            //level je globalna premenna, na zaklade ktorej sa filtruju zobrazovane vytlky

            mLocnServGPS.setLevel(level);
            mLocnServGPS.updateMap();
        }
        else {
          Toast.makeText(getActivity(), "Please, connect to network.", Toast.LENGTH_SHORT).show();
        }
    }


}
