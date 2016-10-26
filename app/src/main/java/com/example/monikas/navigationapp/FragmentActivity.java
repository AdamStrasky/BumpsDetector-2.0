package com.example.monikas.navigationapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.NotificationManager;
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
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
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
import static com.example.monikas.navigationapp.MainActivity.add_button;
import static com.example.monikas.navigationapp.MainActivity.mapConfirm;

import static com.example.monikas.navigationapp.MainActivity.mapbox;
import static com.example.monikas.navigationapp.MainActivity.navig_on;
import static  com.example.monikas.navigationapp.Provider.bumps_detect.TABLE_NAME_BUMPS;
import android.support.v4.app.NotificationCompat.Builder;

public class FragmentActivity extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private GoogleApiClient mGoogleApiClient;
    public GPSLocator gps = null;
    public Accelerometer accelerometer;
    public static Activity fragment_context;
    public static GPSLocator global_gps;
    public static GoogleApiClient global_mGoogleApiClient;
    //konstanty pre level (podiel rating/count pre vytlk v databaze)
    private final float ALL_BUMPS = 1.0f;
    private final float MEDIUM_BUMPS = 1.5f;
    private final float LARGE_BUMPS = 2.5f;
    //level defaultne nastaveny pre zobrazovanie vsetkych vytlkov
    public float level = ALL_BUMPS;
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
    private boolean isEndNotified;
    public static boolean setOnPosition =true;
    public static boolean flagDownload=false;
    public static boolean lockAdd=false;
    private int regionSelected;
    private boolean mapNotification=true;
    // Offline objects
    private OfflineManager offlineManager;
    private OfflineRegion offlineRegion;
    public static String selectedName = null;
    private final static String TAG = "MainActivity";
    // JSON encoding/decoding
    public final static String JSON_CHARSET = "UTF-8";
    public final static String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";
    private  boolean regular_update = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
        setRetainInstance(true);
        offlineManager = OfflineManager.getInstance(getActivity());
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
        //////////////////////////////odstranit, len na testovanie
       // new Timer().schedule(new VkladanieDoDatabazy(), 15000, 15000);// 3600000
    }

   // pravdepodobne vymať
/*****************************************************************************************************/
    public  void offlineMapaNaMojejGPS ( ){
        // asi odstranit
        gps=mLocnServGPS;
        if (gps!=null && gps.getCurrentLatLng()!=null) {
            offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
                    @Override
                     public void onList(final OfflineRegion[] offlineRegions) {
                        if (offlineRegions == null || offlineRegions.length == 0)
                             return;

                        boolean isInMap= false;
                        int poradie=0;
                        for (OfflineRegion offlineRegion : offlineRegions) {
                            LatLngBounds bounds = ((OfflineTilePyramidRegionDefinition) offlineRegions[poradie].getDefinition()).getBounds();
                            if (bounds.contains(new com.mapbox.mapboxsdk.geometry.LatLng(gps.getCurrentLatLng().latitude, gps.getCurrentLatLng().longitude)))
                                isInMap=true;
                            poradie++;
                        }
                        if(isInMap) {
                            mapNotification=false;
                        alertMissingMap();
                        }
                    }

                    @Override
                     public void onError(String error) {
                     }
            });
        }
    }
    // asi zmazat, ak nebudem riesit net
    private void alertMissingMap() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Internet connection")
                .setMessage("Your map is not available. Please connect to internet or download map of region if you want use a map")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.show();
    }

    // pravdepodobne vymazať
    private class VkladanieDoDatabazy extends TimerTask {
        @Override
        public void run() {

            if (updatesLock) {
                Log.d("TEaaaaaST"," nepresiel ");
                return;
            }

            if (!updatesLock) {
                updatesLock=true;
                sb.beginTransaction();
                int i=0;
                for (i = 0; i < 100000; i++) {
                    Location location = new Location("new");
                    location.setLatitude(48.222 + i);
                    location.setLongitude(48.222);
                    location.setTime(new Date().getTime());
                    Log.d("TEaaaaaST"," beyiiii");
                    accelerometer.addPossibleBumps(location, 5.5f);
                    accelerometer.addBumpsManual(1);
                    if (i ==99999) {
                        Log.d("TEaaaaaST"," vypol sa lock v regular A");
                        updatesLock = false;
                        sb.setTransactionSuccessful();
                        sb.endTransaction();

                        break;
                    }

                }

            }
            Log.d("TEaaaaaST"," naplnene");

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

    /*****************************************************************************************************/

  // presunúť do inej triedy
  /*******************************************************************************************************/
    public void downloadRegionDialog() {
        if (!isNetworkAvailable()) {
            Toast.makeText(getActivity(), "You must to be connected to internet if you want download map", Toast.LENGTH_SHORT).show();
              return;
        }
        selectedName = null;
        AlertDialog.Builder builderSingle = new AlertDialog.Builder( getActivity());
        builderSingle.setIcon(R.drawable.ic_launcher);
        builderSingle.setTitle("Select map for download");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.select_dialog_singlechoice);
        arrayAdapter.add("Download current map     ");
        arrayAdapter.add("Select region to download");

        builderSingle.setNegativeButton(
                "cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(
                arrayAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // zvolená možnosť vyberu mapy
                      alertSelectRegion(null,which);
                    }
                });
        builderSingle.show();
    }

    public void alertSelectRegion(String region , int which) {
        String strName = "Map for download";
        // ktorá možnosť bola zvolená - currnet/ select
        final int  click=which;
        final EditText regionNameEdit = new EditText(getActivity());
        if (which==0 )
            regionNameEdit.setHint("Write name of region");
        else
            regionNameEdit.setHint("Write name of region for download");
        // ak bolo znovuotvorene, nech zostane uložený nazov regionu
        if (region!=null)
            regionNameEdit.setText(region);

        AlertDialog.Builder windowAlert = new AlertDialog.Builder(getActivity());
        windowAlert.setPositiveButton("Download", null);
        windowAlert.setNegativeButton("Cancel", null);
        // ak volím select, dať možnosť aj zobraziť mapu
        if (which !=0)
            windowAlert.setNeutralButton("Navige to", null);
        windowAlert.setView(regionNameEdit);
        windowAlert.setTitle(strName);
        final AlertDialog mAlertDialog = windowAlert.create();
        mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positive_btn = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positive_btn.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        String regionName = regionNameEdit.getText().toString();
                        if (regionName.length() == 0) {
                            Toast.makeText(getActivity(), "Region name cannot be empty.", Toast.LENGTH_SHORT).show();
                        } else {
                            // zvolené zadanie regionu
                            if (click == 1) {
                                Address address=null;
                                try {
                                    address = Route.findLocality(regionName, getActivity());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                if (address == null) {
                                    Toast.makeText(getActivity(), "Region not exist", Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    mAlertDialog.cancel();
                                    downloadRegion(regionName,click);
                                }
                            }
                            else {
                                mAlertDialog.cancel();
                                downloadRegion(regionName, click);
                            }
                        }
                    }
                });
                // kliknutie na navigovanie na zadani region
                Button neutral_btn = mAlertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                neutral_btn.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        Address address=null;
                        String regionName = regionNameEdit.getText().toString();
                        try {
                            address = Route.findLocality(regionName, getActivity());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (address == null) {
                            Toast.makeText(getActivity(), "Region not exist", Toast.LENGTH_LONG).show();

                        }else {
                           setOnPosition =false;
                            mapbox.setMyLocationEnabled(false);
                            mAlertDialog.cancel();

                            selectedName =regionName;
                            LatLng position = new LatLng(address.getLatitude(), address.getLongitude());
                            CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(new com.mapbox.mapboxsdk.geometry.LatLng(position.latitude, position.longitude))
                                    .zoom(15)
                                    .build();

                            //  posun kamery na novu poziciu
                            mapbox.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                            // zobrazenie buttonu na  vratenie mapy na sucasnu polohu
                            mapConfirm.setVisibility(View.VISIBLE);
                            add_button.setVisibility(View.INVISIBLE);
                        }
                    }
                });

            }
        });
        mAlertDialog.show();
    }

    public void downloadRegion(final String regionName,  int select) {
        LatLngBounds bounds =null;
        // ak bola zvolená sučasna obrazovka, vezme mapu zobrazenu na displeji
        if (select == 0) {
            bounds = mapbox.getProjection().getVisibleRegion().latLngBounds;
        }

        startProgress();
        if (select ==1) {
            Address address = null;

            try {
                address = Route.findLocality(regionName, getActivity());
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (address == null) {
                endProgress("Unable to find location, wrong name!");
                Toast.makeText(getActivity(), "Unable to find location, wrong name!", Toast.LENGTH_LONG).show();
                 return;
            }else {
                bounds = new LatLngBounds.Builder()
                        .include(new com.mapbox.mapboxsdk.geometry.LatLng(address.getLatitude() + 0.2, address.getLongitude() + 0.2)) // Northeast
                        .include(new com.mapbox.mapboxsdk.geometry.LatLng(address.getLatitude() - 0.2, address.getLongitude() - .2)) // Southwest
                        .build();
            }
        }

        String styleURL = mapbox.getStyleUrl();
        double minZoom = mapbox.getCameraPosition().zoom;
        double maxZoom = mapbox.getMaxZoom();
        float pixelRatio = this.getResources().getDisplayMetrics().density;
            OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                    styleURL, bounds, minZoom, maxZoom, pixelRatio);

       // Build a JSONObject using the user-defined offline region title,
        // convert it into string, and use it to create a metadata variable.
        // The metadata varaible will later be passed to createOfflineRegion()
        byte[] metadata;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_FIELD_REGION_NAME, regionName);
            String json = jsonObject.toString();
            metadata = json.getBytes(JSON_CHARSET);
        } catch (Exception e) {
            Log.e(TAG, "Failed to encode metadata: " + e.getMessage());
            metadata = null;
        }

        // Create the offline region and launch the download
        offlineManager.createOfflineRegion(definition, metadata, new OfflineManager.CreateOfflineRegionCallback() {
            @Override
            public void onCreate(OfflineRegion offlineRegion) {
                Log.d(TAG, "Offline region created: " + regionName);
                FragmentActivity.this.offlineRegion = offlineRegion;
                launchDownload();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error: " + error);
            }
        });
    }

    private void launchDownload() {
        // Set up an observer to handle download progress and
        // notify the user when the region is finished downloading
        offlineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {
            @Override
            public void onStatusChanged(OfflineRegionStatus status) {
                // Compute a percentage
                 double percentage = status.getRequiredResourceCount() >= 0 ?
                        (100.0 * status.getCompletedResourceCount() / status.getRequiredResourceCount()) :
                        0.0;

                mBuilder.setProgress(100, (int) Math.round(percentage), false);
                mNotifyManager.notify(0, mBuilder.build());

                if (status.isComplete()) {
                    // Download complete
                    endProgress("Region downloaded successfully.");
                    return;
                }
                // Log what is being currently downloaded
                Log.d(TAG, String.format("%s/%s resources; %s bytes downloaded.",
                        String.valueOf(status.getCompletedResourceCount()),
                        String.valueOf(status.getRequiredResourceCount()),
                        String.valueOf(status.getCompletedResourceSize())));
            }

            @Override
            public void onError(OfflineRegionError error) {
                Log.e(TAG, "onError reason: " + error.getReason());
                Log.e(TAG, "onError message: " + error.getMessage());
            }

            @Override
            public void mapboxTileCountLimitExceeded(long limit) {
                Log.e(TAG, "Mapbox tile count limit exceeded: " + limit);
                endProgress(null);

            }
        });

        // Change the region state
        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);
    }

    public void downloadedRegionList() {
        // Build a region list when the user clicks the list button

        // Reset the region selected int to 0
        regionSelected = 0;

        // Query the DB asynchronously
        offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
            @Override
            public void onList(final OfflineRegion[] offlineRegions) {
                // Check result. If no regions have been
                // downloaded yet, notify user and return
                if (offlineRegions == null || offlineRegions.length == 0) {
                    Toast.makeText(getActivity(), "You have no regions yet.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Add all of the region names to a list
                ArrayList<String> offlineRegionsNames = new ArrayList<>();
                for (OfflineRegion offlineRegion : offlineRegions) {
                    offlineRegionsNames.add(getRegionName(offlineRegion));
                }
                final CharSequence[] items = offlineRegionsNames.toArray(new CharSequence[offlineRegionsNames.size()]);

                // Build a dialog containing the list of regions
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setTitle("List")
                        .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Track which region the user selects
                                regionSelected = which;
                            }
                        })
                        .setPositiveButton("Navigate to", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                setOnPosition = false;
                                Toast.makeText(getActivity(), items[regionSelected], Toast.LENGTH_LONG).show();

                                // Get the region bounds and zoom
                                LatLngBounds bounds = ((OfflineTilePyramidRegionDefinition) offlineRegions[regionSelected].getDefinition()).getBounds();
                                double regionZoom = ((OfflineTilePyramidRegionDefinition) offlineRegions[regionSelected].getDefinition()).getMinZoom();

                                // Create new camera position
                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                        .target(bounds.getCenter())
                                        .zoom(regionZoom)
                                        .build();

                                // Move camera to new position
                                add_button.setVisibility(View.INVISIBLE);
                                navig_on.setVisibility(View.VISIBLE);
                                mapbox.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                            }
                        })
                        .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // Make progressBar indeterminate and
                                // set it to visible to signal that
                                // the deletion process has begun


                                // Begin the deletion process
                                offlineRegions[regionSelected].delete(new OfflineRegion.OfflineRegionDeleteCallback() {
                                    @Override
                                    public void onDelete() {
                                        // Once the region is deleted, remove the
                                        // progressBar and display a toast

                                        Toast.makeText(getActivity(), "Region deleted", Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onError(String error) {

                                        Log.e(TAG, "Error: " + error);
                                    }
                                });
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // When the user cancels, don't do anything.
                                // The dialog will automatically close
                            }
                        }).create();
                dialog.show();

            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error: " + error);
            }
        });
    }

    private String getRegionName(OfflineRegion offlineRegion) {
        // Get the retion name from the offline region metadata
        String regionName;

        try {
            byte[] metadata = offlineRegion.getMetadata();
            String json = new String(metadata, JSON_CHARSET);
            JSONObject jsonObject = new JSONObject(json);
            regionName = jsonObject.getString(JSON_FIELD_REGION_NAME);
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode metadata: " + e.getMessage());
            regionName = "Region " + offlineRegion.getID();
        }
        return regionName;
    }

    // Progress bar methods
    private void startProgress() {
        Toast.makeText(getActivity(), "Download start", Toast.LENGTH_LONG).show();
        downloadNotification();
        flagDownload=true;
        isEndNotified = false;
    }

    private void endProgress(final String message) {
        // Don't notify more than once
        if (isEndNotified) return;
        mBuilder.setContentText("Download complete")
                // Removes the progress bar
                .setProgress(0,0,false);
        mNotifyManager.notify(0, mBuilder.build());

        // Stop and hide the progress bar
        isEndNotified = true;
        flagDownload=false;

        if (message!=null)
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    private NotificationManager mNotifyManager;
    private Builder mBuilder;

    public void downloadNotification() {
        mNotifyManager = (NotificationManager)getActivity().getSystemService(getActivity().NOTIFICATION_SERVICE);
        mBuilder = new android.support.v4.app.NotificationCompat.Builder(getActivity());
        mBuilder.setContentTitle("Map download")
                .setContentText("Download in progress")
                .setSmallIcon(R.drawable.bump_white);
    }
    /*******************************************************************************************************/
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

    private class Regular_upgrade extends TimerTask {
        @Override
        public void run() {
            regular_update = true;
        }
    }

    public void getAllBumps(final Double latitude, final Double longitude) {
       // vyčistenie mapy a uprava cesty

        mapbox.removeAnnotations();

        Thread t = new Thread() {
            public void run() {

                Looper.prepare();
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

        // seleknutie vytlk z oblasti a starych 280 dni
        String selectQuery = "SELECT latitude,longitude,count,manual FROM my_bumps WHERE rating/count >="+ level +" AND " +
              " ( last_modified BETWEEN '"+ago_formated+" 00:00:00' AND '"+now_formated+" 23:59:59') and  "
                + " (ROUND(latitude,0)==ROUND("+latitude+",0) and ROUND(longitude,0)==ROUND("+longitude+",0)) ";
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

                Cursor cursor  =null;
                try {
                cursor   = database.rawQuery(selectQuery, null);
                int i= 0 ;
        if (cursor.moveToFirst()) {
            do {
                 i++;
                // pridavanie do mapy
             gps.addBumpToMap(new com.mapbox.mapboxsdk.geometry.LatLng(cursor.getDouble(0), cursor.getDouble(1)), cursor.getInt(2), cursor.getInt(3));
               try {
                        Thread.sleep(20); // sleep for 50 ms so that main UI thread can handle user actions in the meantime
                    } catch (InterruptedException e) {
                        // NOP (no operation)
                    }
               if (i== 1000)
                    break;
            }
            while (cursor.moveToNext());
        }
                } finally {
                    // this gets called even if there is an exception somewhere above
                    if(cursor != null)
                        cursor.close();
                }

                if( !updatesLock)
                    updatesLock=true;
                if (accelerometer!= null && accelerometer.getPossibleBumps().size() > 0) {
                    notSendBumps(accelerometer.getPossibleBumps(), accelerometer.getBumpsManual());
                }else
                    updatesLock=false;


                Looper.loop();



            }
        };
        t.start();

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
                        gps.addBumpToMap(new com.mapbox.mapboxsdk.geometry.LatLng(loc.getLatitude(), loc.getLongitude()),1,bumpsManual.get(i));
                    i++;
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {

                }
            }
        }
    }

    private double lang_database,longt_database;
    private int net, b_id_database, c_id_database,updates, max_number =0 ;

    public void get_max_collision(Double latitude, Double longtitude, Integer update ) {
        Log.d("TTRREEE", "4. get_max_collision  ");
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
        Cursor cursor = null;

        try {
        cursor     = database.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                c_id_database =cursor.getInt(0);
            } while (cursor.moveToNext());
        }
        } finally {
            // this gets called even if there is an exception somewhere above
            if(cursor != null)
                cursor.close();
        }

        sb.setTransactionSuccessful();
        sb.endTransaction();
        updates = update;
        new Max_Collision_Number().execute();

    }

    class Max_Collision_Number extends AsyncTask<String, Void, JSONArray> {

        protected JSONArray doInBackground(String... args) {
            Log.d("TTRREEE", "5. Max_Collision_Number ");
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

                    Thread t = new Thread() {
                        public void run() {
                            Log.d("TTRREEE", "6. Max_Collision_Number - thread  ");
                            Looper.prepare();
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

                                   try {
                                     cursor= sb.rawQuery(sql,null);

                                    if(cursor.getCount()>0  ){
                                        //  ak ich bolo viac pripičítam
                                        sql=     "UPDATE " + Provider.bumps_detect.TABLE_NAME_BUMPS + " SET rating=rating+ " + rating + ", count=count +1 WHERE b_id_bumps=" + b_id;
                                    }else{
                                       // ak bol prvý, nastavujem na 1 count a rating prvého prijateho
                                         sql=   "UPDATE " + Provider.bumps_detect.TABLE_NAME_BUMPS + " SET rating=" + rating + ", count=1 WHERE b_id_bumps=" + b_id ;
                                    }
                                    sb.execSQL(sql);
                                   } finally {
                                       // this gets called even if there is an exception somewhere above
                                       if(cursor != null)
                                           cursor.close();
                                   }
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
                    Looper.loop();
                    }
                    };
                    t.start();


                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    public void get_max_bumps(Double langtitude, Double longtitude, Integer net) {

        Log.d("TTRREEE", "spustam  get_max_bumps");
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
        Cursor cursor=null;
        try {
        cursor = database.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                b_id_database =cursor.getInt(0);
            } while (cursor.moveToNext());

        }
        } finally {
        // this gets called even if there is an exception somewhere above
        if(cursor != null)
            cursor.close();
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
                 Log.d("TTRREEE", "2. spustam Max_Bump_Number");
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
                         Log.d("TTRREEE", "2. spustam Max_Bump_Number - success ");
                         return bumps;
                     } else if (success == 1) {
                         // potrebujem potvrdit nove data na stiahnutie
                         response.put(0, "update");
                         Log.d("TTRREEE", "2. spustam Max_Bump_Number - update ");
                         return response;
                     } else {
                         Log.d("TTRREEE", "2. spustam Max_Bump_Number - error ");
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
                Log.d("TTRREEE", "2.onPostExecute spustam Max_Bump_Number - null ");
                get_max_collision(lang_database, longt_database,0);
                return;
            }

            try {
                if (array.get(0).equals("error")) {
                    Log.d("TTRREEE", "2.onPostExecute spustam Max_Bump_Number - error ");
                    return;

                } else  if (array.get(0).equals("update")) {
                    // mam nove data, zisti aj collision a potom upozorni
                    Log.d("TTRREEE", "2.onPostExecute spustam Max_Bump_Number - update  ");
                    get_max_collision(lang_database, longt_database,1);
                }else  {
                    Log.d("TTRREEE", "2.onPostExecute spustam Max_Bump_Number - succes  ");
                    Thread t = new Thread() {
                        public void run() {
                            Looper.prepare();
                            Log.d("TTRREEE", "3. spustam Max_Bump_Number - thread ");
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
                                      latitude = c.getDouble("latitude");
                                        longitude = c.getDouble("longitude");
                                        count = c.getInt("count");
                                        b_id = c.getInt("b_id");
                                        max_number = b_id;
                                        rating = c.getInt("rating");
                                        last_modified = c.getString("last_modified");
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
                                Looper.loop();
                                get_max_collision(lang_database, longt_database, 0);
                            } else {
                                // nastala chyba, načitaj uložene vytlky
                                sb.endTransaction();
                                LatLng convert_location =  gps.getCurrentLatLng();
                                getAllBumps(convert_location.latitude,convert_location.longitude);
                                Looper.loop();

                            }
                         }
                    };
                    t.start();
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
        mServiceConnectedGPS =   getActivity().bindService(new Intent(getActivity().getApplicationContext(), GPSLocator.class), mServconnGPS, Context.BIND_AUTO_CREATE);

         new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                fragment_context = getActivity();
                global_gps =mLocnServGPS;
                mServiceConnectedAcc =   getActivity().bindService(new Intent(getActivity().getApplicationContext(), Accelerometer.class), mServconnAcc, Context.BIND_AUTO_CREATE);
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

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadSaveDB();
                //vytlky sa do dabatazy odosielaju kazdu minutu
                new Timer().schedule(new SendBumpsToDb(), 0, 120000);
                //mapa sa nastavuje kazde 2 minuty
                new Timer().schedule(new MapSetter(), 0, 120000);   //120000


            }
        },5000);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (isEneableShowText())
            Toast.makeText(getActivity(), "GoogliApiClient connection failed", Toast.LENGTH_LONG).show();
    }

     private void initialization() {
        buildGoogleApiClient();
    }

    public void loadSaveDB(){
        if (updatesLock)
          return;

        Thread t = new Thread() {
            public void run() {

                Looper.prepare();

        updatesLock=true;
        String selectQuery = "SELECT latitude,longitude,intensity,manual FROM new_bumps ";
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Cursor cursor
                = database.rawQuery(selectQuery, null);
        ArrayList<HashMap<Location, Float>> hashToArray = new  ArrayList <HashMap<Location, Float>>();
        ArrayList <Integer> listA = new ArrayList <Integer>();

        long date = new Date().getTime();
        int i=0;

        if (cursor!= null && cursor.moveToFirst()) {
            sb.beginTransaction();
            do {
                if(!cursor.isNull(0) && !cursor.isNull(1) & !cursor.isNull(2) && !cursor.isNull(3)){
                    Location location = new Location("new");
                    location.setLatitude(cursor.getDouble(0));
                    location.setLongitude(cursor.getDouble(1));
                    location.setTime(date);
                    HashMap<Location, Float> hashToArraya = new HashMap();
                    hashToArraya.put(location, (float) cursor.getDouble(2));
                    hashToArray.add(hashToArraya);
                    listA.add(cursor.getInt(3));
                    Log.d("loadSaveDB", "latitude " + cursor.getDouble(0));
                    Log.d("loadSaveDB", "longitude "+ cursor.getDouble(1));
                    Log.d("loadSaveDB", "intensity "+ cursor.getDouble(2));
                }

           } while (cursor.moveToNext());
            if (cursor!= null) {
                accelerometer = mLocnServAcc;
                if (mLocnServAcc!= null) {
                    mLocnServAcc.getPossibleBumps().addAll(hashToArray);
                    mLocnServAcc.getBumpsManual().addAll(listA);
                }else
                    Log.d("loadSaveDB","NULL ACCELEROMETER");
                 updatesLock = false;
                 sb.setTransactionSuccessful();
                 sb.endTransaction();
            }


        } else
            updatesLock = false;
                Looper.loop();



            }
        };
        t.start();
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

    public static boolean updatesLock = false;
    private boolean regularUpdatesLock = false;

    private class SendBumpsToDb extends TimerTask {

        @Override
        public void run() {

            getActivity().runOnUiThread(new Runnable(){
                @Override
                public void run() {
                  //  ak je pripojenie na internet
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
                }}
            );
        }
    }
    private Bump  Handler;
    private boolean  lock =true;
    private ArrayList<HashMap<Location, Float>> listHelp;
    private ArrayList<Integer> bumpsManualHelp;
    private Integer  poradie;
    public void saveBump( ArrayList<HashMap<Location, Float>> list,  ArrayList<Integer> bumpsManual,  Integer sequel) {
        listHelp=list;
        bumpsManualHelp= bumpsManual;
        poradie=sequel;
        Thread t = new Thread() {
            public void run() {
                Looper.prepare();
                while (true) {
                    if (lock) {
                      if (!listHelp.isEmpty() && listHelp.size() > poradie) {
                            Iterator it = listHelp.get(poradie).entrySet().iterator();
                            HashMap.Entry pair = (HashMap.Entry) it.next();    //next
                            final Location loc = (Location) pair.getKey();
                            final float data = (float) pair.getValue();
                            Handler = new Bump(loc, data, bumpsManualHelp.get(poradie));
                            Handler.getResponse(new CallBackReturn() {
                                public void callback(String results) {
                                    if (results.equals("success")) {
                                        int num =poradie;

                                        // ak mi prišlo potvrdenie o odoslaní, mažem z db
                                        sb.beginTransaction();
                                        sb.execSQL("DELETE FROM new_bumps WHERE latitude=" + loc.getLatitude() + " and  longitude=" + loc.getLongitude()
                                               + " and  ROUND(intensity,6)==ROUND("+data+",6) and manual="+bumpsManualHelp.get(num)+"");

                                        listHelp.remove(num);
                                        bumpsManualHelp.remove(num);
                                        sb.setTransactionSuccessful();
                                        sb.endTransaction();
                                        lock = true;
                                    } else {
                                        // nastala chyba, nemažem
                                        Log.d("TEST", "error");
                                        int num =poradie;
                                        num++;
                                        poradie=num;
                                        lock = true;
                                    }
                                }
                            });

                      } else {
                         break;
                      }
                    }
                }
                lockAdd=true;
                // ak nastala chyba, aktualizujem s udajmi s novými udajmi čo som dostal počas behu saveBUmp
                if (listHelp.size()>0 && accelerometer.getPossibleBumps().size()>0) {
                    int i=0;
                    for (HashMap<Location, Float> oldList : listHelp) {
                        Iterator oldListIteam = oldList.entrySet().iterator();
                        while (oldListIteam.hasNext()) {
                            HashMap.Entry oldData = (HashMap.Entry) oldListIteam.next();
                            Location oldLocation = (Location) oldData.getKey();
                           i=0;
                            for (HashMap<Location, Float> newList : accelerometer.getPossibleBumps()) {

                                Iterator newListIteam = newList.entrySet().iterator();
                                while (newListIteam.hasNext()) {
                                    HashMap.Entry newData = (HashMap.Entry) newListIteam.next();
                                    Location newLocation = (Location) newData.getKey();
                                    // ak sa zhoduju location, tak updatujem hodnoty
                                    if ((oldLocation.getLatitude() == newLocation.getLatitude()) &&
                                            (oldLocation.getLongitude() == newLocation.getLongitude()) ) {
                                        // staršia hodnota je väčšia, tak prepíšem na väčšiu hodnotu
                                         if ( (Float) oldData.getValue() >(Float) newData.getValue()  )
                                               accelerometer.getPossibleBumps().get(0).put(newLocation,(Float) oldData.getValue());
                                        // ak je stará hodnota menšia, updatujem databazu kde je uložená menšia
                                        if ( (Float) oldData.getValue() <(Float) newData.getValue())
                                            sb.execSQL("UPDATE new_bumps  SET intensity=ROUND("+(Float) newData.getValue()+",6) WHERE latitude=" + oldLocation.getLatitude() + " and  longitude=" + oldLocation.getLongitude()
                                                + " and  ROUND(intensity,6)==ROUND("+(Float) oldData.getValue()+",6)");
                                        // mažem s pomocného zoznamu updatnuté hodnoty
                                        listHelp.remove(i);
                                        bumpsManualHelp.remove(i);
                                    }
                                }
                                i++;
                            }
                        }
                    }
                    // doplnim do zoznamu povodné, ktoré sa nezmenili
                    accelerometer.getPossibleBumps().addAll(listHelp);
                    accelerometer.getBumpsManual().addAll(bumpsManualHelp);
                }
                else if (listHelp.size()>0) {
                    // nepribudli nové hodnoty, tak tam vrátim pôvodné
                    accelerometer.getPossibleBumps().addAll(listHelp);
                    accelerometer.getBumpsManual().addAll(bumpsManualHelp);
                }
                // vypínam locky, nulujem pomocné polia
                lockAdd=false;
                listHelp=null;
                bumpsManualHelp=null;
                updatesLock=false;
                Looper.loop();
        } };
        t.start();

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
                     gps = mLocnServGPS;
                     if (gps!=null ) {
                        LatLng convert_location = gps.getCurrentLatLng();
                        if (convert_location != null) {
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
            if (gps!=null && gps.getCurrentLatLng()!=null ) {
                LatLng convert_location = gps.getCurrentLatLng();
                if (convert_location != null )
                    getAllBumps(convert_location.latitude, convert_location.longitude);
            }
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

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
