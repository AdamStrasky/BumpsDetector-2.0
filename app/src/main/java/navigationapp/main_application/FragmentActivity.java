package navigationapp.main_application;

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
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.example.monikas.navigationapp.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FragmentActivity extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient = null;

    public Accelerometer accelerometer = null;  // Accelerometer servises
    private Accelerometer mLocnServAcc = null; // flagy na vytvorenie services
    boolean mServiceConnectedAcc = false;
    boolean mBoundAcc = false;

    public GPSLocator gps = null;             // GPS servises
    private GPSLocator mLocnServGPS = null;  // flagy na vytvorenie services
    boolean mServiceConnectedGPS = false;
    boolean mBoundGPS = false;

    private boolean GPS_FLAG = true;        // nastavuje sa podľa povolenia GPS

    MapLayer mapLayer = null;           // zobrazuje markery vo vrstvách
    SyncDatabase syncDatabase = null;   // synchronizacia databázy
    LocationManager locationManager = null;
    navigationapp.main_application.Location detection = null;  //hľadanie blízkych výtlkov

    static Lock lockZoznam = new ReentrantLock();  // zámok na zoznam
    static Lock lockAdd = new ReentrantLock();  // zámok na odosielanie na server
    static Lock updatesLock = new ReentrantLock();  // zámok na databazu
    public static Activity fragment_context  =null;
    public static GPSLocator global_gps = null;
    public static GoogleApiClient global_mGoogleApiClient = null;
    public final String TAG = "FragmentActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (!isNetworkAvailable(getActivity())) {  // kontrola internetu
            if (isEneableShowText(getActivity()))
                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.offline_mode), Toast.LENGTH_SHORT).show();
        }

        // reaguje na zapnutie/ vypnutie GPS
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        getActivity().registerReceiver(gpsReceiver, new IntentFilter("android.location.PROVIDERS_CHANGED"));

       if (!checkGPSEnable()) {   // ak nie je povolené GPS , upozornenie na zapnutie
            showGPSDisabledAlertToUser();
       } else {
           GPS_FLAG = false;
           initialization();  // ak je povolená, inicializujem
       }
    }

    private BroadcastReceiver gpsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // reaguje na zmenu stavu GPS
            if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && GPS_FLAG) {
                    initialization();
                    GPS_FLAG = false;
                }
            }
        }
    };

    ServiceConnection mServconnGPS = new ServiceConnection() {  // pripojenie k services
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "GPS service connected");
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

    ServiceConnection mServconnAcc = new ServiceConnection() {  // pripojenie k services
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

    private void showGPSDisabledAlertToUser() {  // upozornenie na zapnutie GPS
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage(getActivity().getResources().getString(R.string.enable_gps));
        alert.setCancelable(false);
        alert.setPositiveButton(getActivity().getResources().getString(R.string.change_gps),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
        alert.setNegativeButton(getActivity().getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        alert.show();
    }

    private void showCalibrationAlert() {  // upozornenie na potvrdenie kalibrácie
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage(getActivity().getResources().getString(R.string.offer_calibrate));
        alert.setCancelable(false);
        alert.setPositiveButton(getActivity().getResources().getString(R.string.menu_calibrate),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mLocnServAcc.calibrate();
                        if (isEneableShowText(getActivity()))
                            Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.calibrate), Toast.LENGTH_SHORT).show();
                    }
                });
        alert.show();
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
        init_servise();
    }
    boolean init = false;
    public void init_servise() {  // inicializujem  služby

        global_mGoogleApiClient = mGoogleApiClient;
        mServiceConnectedGPS = getActivity().bindService(new Intent(getActivity().getApplicationContext(), GPSLocator.class), mServconnGPS, Context.BIND_AUTO_CREATE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                fragment_context = getActivity();
                global_gps = mLocnServGPS;
                mServiceConnectedAcc = getActivity().bindService(new Intent(getActivity().getApplicationContext(), Accelerometer.class), mServconnAcc, Context.BIND_AUTO_CREATE);
            }
        }, 1000);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                accelerometer = mLocnServAcc;
                gps = mLocnServGPS;
                showCalibrationAlert();
            }
        }, 2000);


        new Thread() {
            public void run() {
                Log.d(TAG, "spúštam získavanie akcelerometra");
                Looper.prepare();

                while (true) {
                    accelerometer = mLocnServAcc;
                    if (accelerometer != null) {
                        init = true;
                        Log.d(TAG, "akcelerometer získaný");
                        break;
                    }
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                      e.getMessage();
                    }
                }
                Looper.loop();
            }
        }.start();

        new Thread() {
            public void run() {
                Log.d(TAG, "spúštam získavanie pozície");
                Looper.prepare();
                while (true) {
                    gps = mLocnServGPS;
                    if (gps != null && init) {
                        LatLng convert_location = gps.getCurrentLatLng();
                        if (convert_location != null) {
                            Log.d(TAG, "pozícia získana");
                            break;
                        }
                    } else {
                        Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.fnd_location), Toast.LENGTH_SHORT).show();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.getMessage();
                        }
                        Log.d(TAG, "hľadám pozíciu");
                    }
                }
                detection = new navigationapp.main_application.Location(fragment_context); // detekcia blížiacich sa výtlkov
                mapLayer = new MapLayer(accelerometer,fragment_context); // zobrazovanie markerov
                syncDatabase = new SyncDatabase(accelerometer,gps,fragment_context,mapLayer); // trieda na synchr. databázy
                Looper.loop();
            }
        }.start();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (isEneableShowText(getActivity()))
            Toast.makeText(getActivity(), "GoogliApiClient connection failed", Toast.LENGTH_LONG).show();
    }

    private void initialization() {
        buildGoogleApiClient();
    }

    public void stop_servise() {  // ukončenie služieb na vypnutie aplikácie
        getActivity().stopService(new Intent(getActivity().getApplicationContext(), Accelerometer.class));
        if (mServiceConnectedAcc) {
            getActivity().unbindService(mServconnAcc);
        }

        getActivity().stopService(new Intent(getActivity().getApplicationContext(), GPSLocator.class));
        if (mServiceConnectedGPS) {
            getActivity().unbindService(mServconnGPS);
        }
        Log.d(TAG, "services odpojené");
    }

    public static void checkCloseDb(SQLiteDatabase database) {
        while (true) {  // kontrola či je databáza zatvorená
            try {
                if (!database.isOpen() )
                    break;
                Log.d("XXXXXXXXX", "checkCloseDb");
            }catch(SQLiteException e){
                e.getMessage();
            }
        }
    }

    public static void checkIntegrityDB(SQLiteDatabase database) {
        while (true) {  // kontrola či je databázu nepoužíva iné vlákno
            try {
                if (!database.isDbLockedByOtherThreads())
                    break;
                Log.d("XXXXXXXXX", "checkIntegrityDB");
            }catch(SQLiteException e){
                e.getMessage();
            }
        }
    }

    public static boolean isEneableShowText(Context context) {
        // či mám povolené ukazovať informácia aj mimo aplikácie
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean alarm = prefs.getBoolean("alarm", Boolean.parseBoolean(null));
        Log.d("FragmentActivity", " zobrazenie textu mimo aplikácie stav - " +alarm);
        return (alarm || (!alarm && MainActivity.isActivityVisible())) ;
    }

    public static  boolean isNetworkAvailable(Context context) {  // kontrola pripojeného internetu
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public boolean checkGPSEnable() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}