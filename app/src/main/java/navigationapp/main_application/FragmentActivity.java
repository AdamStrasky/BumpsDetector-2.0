package navigationapp.main_application;

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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
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
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.monikas.navigationapp.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Projection;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;
import com.mapbox.mapboxsdk.style.layers.NoSuchLayerException;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.NoSuchSourceException;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.Point;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.text.ParseException;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.mapbox.mapboxsdk.offline.OfflineManager.getInstance;
import static navigationapp.main_application.Bump.isBetween;
import static navigationapp.main_application.MainActivity.add_button;
import static navigationapp.main_application.MainActivity.mapConfirm;

import static navigationapp.main_application.MainActivity.mapView;
import static navigationapp.main_application.MainActivity.mapbox;
import static navigationapp.main_application.MainActivity.navig_on;
import static navigationapp.main_application.Provider.bumps_detect.TABLE_NAME_BUMPS;
import android.support.v4.app.NotificationCompat.Builder;

import okhttp3.internal.Util;

public class FragmentActivity extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    public GPSLocator gps = null;
    public Accelerometer accelerometer = null;
    public static Activity fragment_context;
    public static GPSLocator global_gps = null;
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
    private GPSLocator mLocnServGPS = null;
    LocationManager locationManager;

    private JSONArray bumps;
    private int loaded_index;
    private boolean isEndNotified = true;
    public static boolean setOnPosition = true;
    public static boolean flagDownload = false;

    private int regionSelected;
    private boolean mapNotification = true;
    // Offline objects
    private OfflineManager offlineManager;
    private OfflineRegion offlineRegion;
    public static String selectedName = null;
    private final static String TAG = "MainActivity";
    // JSON encoding/decoding
    public final static String JSON_CHARSET = "UTF-8";
    public final static String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";
    private boolean regular_update = false;
    private boolean clear = true;
    static Lock lockZoznam = new ReentrantLock();
    static Lock lockZoznamDB = new ReentrantLock();
    static Lock lockAdd = new ReentrantLock();
    static Lock updatesLock = new ReentrantLock();
    navigationapp.main_application.Location detection = null;

    public final double minZoomDownloadMap = 12;
    public final double maxZoomDownloadMap = 16;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        offlineManager = getInstance(getActivity());
        get_loaded_index();
        // ak sa pripojím na internet požiam o update
        regular_update = true;

        // myView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        if (!isNetworkAvailable()) {
            if (isEneableShowText())

                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.offline_mode), Toast.LENGTH_SHORT).show();
        }
        // reaguje na zapnutie/ vypnutie GPS
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        getActivity().registerReceiver(gpsReceiver, new IntentFilter("android.location.PROVIDERS_CHANGED"));

        // ak nie je povolené GPS , upozornenie na zapnutie
        if (!checkGPSEnable()) {
            showGPSDisabledAlertToUser();
        } else {
            GPS_FLAG = false;
            initialization();
        }

        // pravidelný update ak nemám povolený internet
        new Timer().schedule(new Regular_upgrade(), 180000, 180000);// 3600000

    }

    // presunúť do inej triedy

    /*******************************************************************************************************/
    public void downloadRegionDialog() {
        if (!isNetworkAvailable()) {

            Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.not_net_download), Toast.LENGTH_SHORT).show();
            return;
        }
        selectedName = null;
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(getActivity());
        builderSingle.setIcon(R.drawable.ic_launcher);
        builderSingle.setTitle(getActivity().getResources().getString(R.string.map_download));

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.select_dialog_singlechoice);
        arrayAdapter.add(getActivity().getResources().getString(R.string.current_map));
        arrayAdapter.add(getActivity().getResources().getString(R.string.select_map));

        builderSingle.setNegativeButton(
                getActivity().getResources().getString(R.string.cancel),
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
                        alertSelectRegion(null, which);
                    }
                });
        builderSingle.show();
    }

    public void alertSelectRegion(String region, int which) {
        String strName = getActivity().getResources().getString(R.string.map_for_download);
        // ktorá možnosť bola zvolená - currnet/ select
        final int click = which;
        final EditText regionNameEdit = new EditText(getActivity());
        if (which == 0)
            regionNameEdit.setHint(getActivity().getResources().getString(R.string.name_region));
        else
            regionNameEdit.setHint(getActivity().getResources().getString(R.string.name_region_download));
        // ak bolo znovuotvorene, nech zostane uložený nazov regionu
        if (region != null)
            regionNameEdit.setText(region);

        AlertDialog.Builder windowAlert = new AlertDialog.Builder(getActivity());
        windowAlert.setPositiveButton(getActivity().getResources().getString(R.string.download), null);
        windowAlert.setNegativeButton(getActivity().getResources().getString(R.string.cancel), null);
        // ak volím select, dať možnosť aj zobraziť mapu
        if (which != 0)
            windowAlert.setNeutralButton(getActivity().getResources().getString(R.string.navige_to), null);
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
                            Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.region_empty), Toast.LENGTH_SHORT).show();
                        } else {
                            // zvolené zadanie regionu
                            if (click == 1) {
                                Address address = null;
                                try {
                                    address = Route.findLocality(regionName, getActivity());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                if (address == null) {
                                    Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.region_not_exist), Toast.LENGTH_SHORT).show();
                                } else {
                                    mAlertDialog.cancel();
                                    downloadRegion(regionName, click);
                                }
                            } else {
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

                        Address address = null;
                        String regionName = regionNameEdit.getText().toString();
                        try {
                            address = Route.findLocality(regionName, getActivity());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (address == null) {
                            Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.region_not_exist), Toast.LENGTH_LONG).show();

                        } else {
                            setOnPosition = false;
                            mapbox.setMyLocationEnabled(false);
                            mAlertDialog.cancel();

                            selectedName = regionName;
                            CameraPosition position = new CameraPosition.Builder()
                                    .target(new com.mapbox.mapboxsdk.geometry.LatLng(address.getLatitude(), address.getLongitude())) // Sets the new camera position
                                    .zoom(15) // Sets the zoom
                                    .build(); // Creates a CameraPosition from the builder

                            mapbox.animateCamera(CameraUpdateFactory.newCameraPosition(position), 6000,
                                    new MapboxMap.CancelableCallback() {
                                        @Override
                                        public void onCancel() {

                                        }

                                        @Override
                                        public void onFinish() {

                                        }
                                    });


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

    public void downloadRegion(final String regionName, final int select) {

        Thread t = new Thread() {
            public void run() {

                Looper.prepare();


                mapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(MapboxMap mapboxMap) {

                LatLngBounds bounds = null;
                // ak bola zvolená sučasna obrazovka, vezme mapu zobrazenu na displeji
                if (select == 0) {
                    bounds = mapbox.getProjection().getVisibleRegion().latLngBounds;
                }

                startProgress();
                if (select == 1) {
                    Address address = null;

                    try {
                        address = Route.findLocality(regionName, getActivity());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (address == null) {
                        endProgress(getActivity().getResources().getString(R.string.unable_find));
                        Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.unable_find), Toast.LENGTH_LONG).show();
                        return;
                    } else {
                        bounds = new LatLngBounds.Builder()
                                .include(new com.mapbox.mapboxsdk.geometry.LatLng(address.getLatitude() + 0.2, address.getLongitude() + 0.2)) // Northeast
                                .include(new com.mapbox.mapboxsdk.geometry.LatLng(address.getLatitude() - 0.2, address.getLongitude() - .2)) // Southwest
                                .build();
                    }
                }

                String styleURL = mapbox.getStyleUrl();
                double minZoom = minZoomDownloadMap;
                double maxZoom = maxZoomDownloadMap;
                float pixelRatio = getActivity().getResources().getDisplayMetrics().density;
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
                        errorDownloadNotification();
                        Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.error_download), Toast.LENGTH_LONG).show();

                    }
                });

                    }
                                    });


                Looper.loop();


            }
        };
        t.start();
    }

    public boolean isClear() {

        return clear;
    }

    public void setClear(boolean clear) {
        this.clear = clear;
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
                    endProgress(getActivity().getResources().getString(R.string.download_success));
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
                mapTitleExceeded(true);
                errorDownloadNotification();
                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.map_exceeded), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.no_regions), Toast.LENGTH_SHORT).show();
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
                        .setTitle(getActivity().getResources().getString(R.string.list))
                        .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Track which region the user selects
                                regionSelected = which;
                            }
                        })
                        .setPositiveButton(getActivity().getResources().getString(R.string.navige_to), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {


                                if (!checkGPSEnable()) {
                                    Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.turn_gps), Toast.LENGTH_LONG).show();
                                } else {
                                    setOnPosition = false;
                                    Toast.makeText(getActivity(), items[regionSelected], Toast.LENGTH_LONG).show();

                                    // Get the region bounds and zoom
                                    LatLngBounds bounds = ((OfflineTilePyramidRegionDefinition) offlineRegions[regionSelected].getDefinition()).getBounds();
                                    double regionZoom = ((OfflineTilePyramidRegionDefinition) offlineRegions[regionSelected].getDefinition()).getMinZoom();

                                    CameraPosition position = new CameraPosition.Builder()
                                            .target(bounds.getCenter()) // Sets the new camera position
                                            .zoom(regionZoom) // Sets the zoom
                                            .build(); // Creates a CameraPosition from the builder

                                    mapbox.animateCamera(CameraUpdateFactory.newCameraPosition(position), 6000,
                                            new MapboxMap.CancelableCallback() {
                                                @Override
                                                public void onCancel() {

                                                }

                                                @Override
                                                public void onFinish() {

                                                }
                                            });


                                    // Move camera to new position
                                    add_button.setVisibility(View.INVISIBLE);
                                    navig_on.setVisibility(View.VISIBLE);


                                }


                            }
                        })
                        .setNeutralButton(getActivity().getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
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

                                        Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.region_delete), Toast.LENGTH_LONG).show();
                                        mapTitleExceeded(false);

                                    }

                                    @Override
                                    public void onError(String error) {

                                        Log.e(TAG, "Error: " + error);
                                    }
                                });
                            }
                        })
                        .setNegativeButton(getActivity().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
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
        Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.download_start), Toast.LENGTH_LONG).show();
        downloadNotification();
        flagDownload = true;
        isEndNotified = false;
    }

    private void endProgress(final String message) {
        // Don't notify more than once

        if (isEndNotified) return;

        if (message != null)
            mBuilder.setContentText(getActivity().getResources().getString(R.string.download_complete))
                    // Removes the progress bar
                    .setProgress(0, 0, false);
        mNotifyManager.notify(0, mBuilder.build());

        // Stop and hide the progress bar
        isEndNotified = true;
        flagDownload = false;

        if (message != null)
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    private NotificationManager mNotifyManager;
    private Builder mBuilder;

    public void downloadNotification() {
        mNotifyManager = (NotificationManager) getActivity().getSystemService(getActivity().NOTIFICATION_SERVICE);
        mBuilder = new android.support.v4.app.NotificationCompat.Builder(getActivity());
        mBuilder.setContentTitle(getActivity().getResources().getString(R.string.notif_map_download))
                .setContentText(getActivity().getResources().getString(R.string.notif_map_progress))
                .setSmallIcon(R.drawable.download);


    }

    public void endDownloadNotification() {
        mNotifyManager.cancelAll();
    }

    public void errorDownloadNotification() {
        mBuilder.setContentTitle(getActivity().getResources().getString(R.string.notif_map_error))
                .setContentText(getActivity().getResources().getString(R.string.notif_map_interrupted))
                .setProgress(0, 0, false);
        mNotifyManager.notify(0, mBuilder.build());

    }

    /*******************************************************************************************************/
    public void get_loaded_index() {
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

    boolean mapReady = false;
    List<Feature> autoMarkerCoordinates = null;
    List<Feature> manualMarkerCoordinates = null;


    private class Regular_upgrade extends TimerTask {
        @Override
        public void run() {
            regular_update = true;
        }
    }


    boolean deleteMap = false;

    synchronized public void getAllBumps(final Double latitude, final Double longitude) {
        if (latitude == null || longitude == null) {
            Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.no_gps), Toast.LENGTH_LONG).show();
            return;
        }


        Thread t = new Thread() {
            public void run() {
                Looper.prepare();
                Log.d("getAllBumps", " thread name " + this.getId());

                int autoBumpSequence = 0, manualBumpSequence = 0;
                autoMarkerCoordinates = new ArrayList<>();
                manualMarkerCoordinates = new ArrayList<>();

                while (true) {
                    if (updatesLock.tryLock()) {
                        try {


                            if (deleteMap && mapbox != null) {
                                deleteOldMarker();
                            }


                            SimpleDateFormat now, ago;
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(new Date());
                            now = new SimpleDateFormat("yyyy-MM-dd");
                            String now_formated = now.format(cal.getTime());
                            // posun od dnesneho dna o 280 dni
                            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE) - 280);
                            ago = new SimpleDateFormat("yyyy-MM-dd");
                            String ago_formated = ago.format(cal.getTime());


                            // seleknutie vytlk z oblasti a starych 280 dni
                            String selectQuery = "SELECT latitude,longitude,count,manual,last_modified FROM my_bumps WHERE rating/count >=" + level + " AND " +
                                    " ( last_modified BETWEEN '" + ago_formated + " 00:00:00' AND '" + now_formated + " 23:59:59') and  "
                                    + " (ROUND(latitude,1)==ROUND(" + latitude + ",1) and ROUND(longitude,1)==ROUND(" + longitude + ",1)) ";
                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(getActivity());
                            SQLiteDatabase database = databaseHelper.getReadableDatabase();
                            checkIntegrityDB(database);
                            database.beginTransaction();

                            Cursor cursor = null;
                            try {
                                cursor = database.rawQuery(selectQuery, null);

                                if (cursor.moveToFirst()) {
                                    do {
                                        String ds1 = "2007-06-30";
                                        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
                                        SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyyy");
                                        String ds2 = null;


                                        try {
                                            ds2 = sdf2.format(sdf1.parse(cursor.getString(4).substring(0, 10)));
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }


                                        if (cursor.getInt(3) == 0) {
                                            autoMarkerCoordinates.add(Feature.fromGeometry(
                                                    Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(cursor.getDouble(1), cursor.getDouble(0)))) // Boston Common Park

                                            );
                                            Feature feature = autoMarkerCoordinates.get(autoBumpSequence);
                                            feature.addStringProperty("aaaaaa", getActivity().getResources().getString(R.string.auto_bump) + "\n" +
                                                    getActivity().getResources().getString(R.string.number_bump) + " " + cursor.getInt(2) + "\n" +
                                                    getActivity().getResources().getString(R.string.modif) + " " + ds2);
                                            autoBumpSequence++;
                                        } else {
                                            manualMarkerCoordinates.add(Feature.fromGeometry(
                                                    Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(cursor.getDouble(1), cursor.getDouble(0)))) // Boston Common Park
                                            );
                                            Feature featurea = manualMarkerCoordinates.get(manualBumpSequence);
                                            featurea.addStringProperty("aaaaaa", getActivity().getResources().getString(R.string.manual_bump) + "\n" +
                                                    getActivity().getResources().getString(R.string.number_bump) + " " + cursor.getInt(2) + "\n" +
                                                    getActivity().getResources().getString(R.string.modif) + " " + ds2);
                                            manualBumpSequence++;
                                        }
                                    }
                                    while (cursor.moveToNext());
                                }
                            } finally {
                                if (cursor != null)
                                    cursor.close();
                            }

                            database.setTransactionSuccessful();
                            database.endTransaction();
                            database.close();
                            checkCloseDb(database);


                        } finally {
                            updatesLock.unlock();
                            break;
                        }
                    } else {
                        Log.d("getAllBumps", "getAllBumps thread lockAAAAAA " + this.getId());
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                        }
                    }

                }

                ArrayList<HashMap<Location, Float>> bumpList = new ArrayList<HashMap<Location, Float>>();
                ArrayList<Integer> bumpsManual = new ArrayList<Integer>();
                while (true) {
                    if (lockAdd.tryLock()) {
                        // Got the lock
                        try {

                            while (true) {
                                if (lockZoznam.tryLock()) {
                                    // Got the lock
                                    try {
                                        if (accelerometer != null && accelerometer.getPossibleBumps().size() > 0) {


                                            bumpList.addAll(accelerometer.getPossibleBumps());
                                            bumpsManual.addAll(accelerometer.getBumpsManual());

                                        }
                                    } finally {
                                        lockZoznam.unlock();
                                        break;
                                    }
                                } else {
                                    try {
                                        Random ran = new Random();
                                        int x = ran.nextInt(20) + 1;
                                        Thread.sleep(x);

                                    } catch (InterruptedException e) {
                                    }
                                }

                            }
                        } finally {
                            // Make sure to unlock so that we don't cause a deadlock
                            lockAdd.unlock();
                            break;
                        }
                    } else {
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                        }
                    }
                }

                notSendBumps(bumpList, bumpsManual, autoBumpSequence, manualBumpSequence);
                if (mapbox != null)
                    showNewMarker();
                deleteMap = true;


                Looper.loop();

            }
        };
        t.start();


    }


    synchronized public void deleteOldMarker() {
        mapbox.animateCamera(CameraUpdateFactory.newCameraPosition(mapbox.getCameraPosition()), new DefaultCallback() {
            @Override
            public void onFinish() {
                deleteMarkers();
            }

            @Override
            public void onCancel() {
                deleteMarkers();
            }
        });
    }

    private static class DefaultCallback implements MapboxMap.CancelableCallback {
        @Override
        public void onCancel() {
            Log.d("map", "showNewMarker onCancel");
        }

        @Override
        public void onFinish() {
            Log.d("map", "showNewMarker onFinish");
        }
    }

    synchronized public void showNewMarker() {

        mapbox.animateCamera(CameraUpdateFactory.newCameraPosition(mapbox.getCameraPosition()), new DefaultCallback() {
            @Override
            public void onFinish() {
                showMarkers();
            }

            @Override
            public void onCancel() {
                showMarkers();
            }
        });
    }

    synchronized public void deleteMarkers() {
        Log.d("map", "deleteOldMarker start");

        if (isClear() && mapbox != null) {
            List<Marker> markers = mapbox.getMarkers();
            for (int i = 0; i < markers.size(); i++) {
                mapbox.removeMarker(markers.get(i));
            }
        }


        try {
            if (mapbox.getSource("marker-source-auto") != null)
                mapbox.removeSource("marker-source-auto");
        } catch (NoSuchSourceException e) {
            e.printStackTrace();
            e.getMessage();
        }

        try {
            if (mapbox.getSource("marker-source-manual") != null)
                mapbox.removeSource("marker-source-manual");
        } catch (NoSuchSourceException e) {
            e.printStackTrace();
            e.getMessage();
        }

        mapbox.removeImage("my-marker-image-auto");
        mapbox.removeImage("my-marker-image-manual");

        try {
            if (mapbox.getLayer("marker-layer-auto") != null)
                mapbox.removeLayer("marker-layer-auto");
        } catch (NoSuchLayerException e) {
            e.getMessage();
        }
        try {
            if (mapbox.getLayer("marker-layer-manual") != null)
                mapbox.removeLayer("marker-layer-manual");
        } catch (NoSuchLayerException e) {
            e.getMessage();
        }

        try {
            if (mapbox.getSource("selected-marker-auto") != null)
                mapbox.removeSource("selected-marker-auto");
        } catch (NoSuchSourceException e) {
            e.printStackTrace();
            e.getMessage();
        }

        try {
            if (mapbox.getSource("selected-marker-manual") != null)
                mapbox.removeSource("selected-marker-manual");
        } catch (NoSuchSourceException e) {
            e.printStackTrace();
            e.getMessage();
        }

        try {
            if (mapbox.getLayer("selected-marker-layer-auto") != null)
                mapbox.removeLayer("selected-marker-layer-auto");
        } catch (NoSuchLayerException e) {
            e.getMessage();
        }

        try {
            if (mapbox.getLayer("selected-marker-layer-manual") != null)
                mapbox.removeLayer("selected-marker-layer-manual");
        } catch (NoSuchLayerException e) {
            e.getMessage();
        }

        Log.d("map", "deleteOldMarker finish");
    }


    synchronized public void showMarkers() {

        Log.d("map", "showNewMarker start");

        FeatureCollection featureCollectionAuto = FeatureCollection.fromFeatures(autoMarkerCoordinates);
        Source geoJsonSourceAuto = new GeoJsonSource("marker-source-auto", featureCollectionAuto);
        mapbox.addSource(geoJsonSourceAuto);

        FeatureCollection featureCollectionManual = FeatureCollection.fromFeatures(manualMarkerCoordinates);
        Source geoJsonSourceManual = new GeoJsonSource("marker-source-manual", featureCollectionManual);
        mapbox.addSource(geoJsonSourceManual);


        Bitmap iconAuto = BitmapFactory.decodeResource(FragmentActivity.this.getResources(), R.drawable.default_marker);
        mapbox.addImage("my-marker-image-auto", iconAuto);

        SymbolLayer markerAuto = new SymbolLayer("marker-layer-auto", "marker-source-auto")
                .withProperties(PropertyFactory.iconImage("my-marker-image-auto"));
        mapbox.addLayer(markerAuto);

        Bitmap iconManual = BitmapFactory.decodeResource(FragmentActivity.this.getResources(), R.drawable.green_marker);
        mapbox.addImage("my-marker-image-manual", iconManual);

        SymbolLayer markersManual = new SymbolLayer("marker-layer-manual", "marker-source-manual")
                .withProperties(PropertyFactory.iconImage("my-marker-image-manual"));
        mapbox.addLayer(markersManual);

        FeatureCollection emptySourceAuto = FeatureCollection.fromFeatures(new Feature[]{});
        Source selectedMarkerSourceAuto = new GeoJsonSource("selected-marker-auto", emptySourceAuto);
        mapbox.addSource(selectedMarkerSourceAuto);

        FeatureCollection emptySourceManual = FeatureCollection.fromFeatures(new Feature[]{});
        Source selectedMarkerSourceManual = new GeoJsonSource("selected-marker-manual", emptySourceManual);
        mapbox.addSource(selectedMarkerSourceManual);

        SymbolLayer selectedMarkerAuto = new SymbolLayer("selected-marker-layer-auto", "selected-marker-auto")
                .withProperties(PropertyFactory.iconImage("my-marker-image-auto"));
        mapbox.addLayer(selectedMarkerAuto);

        SymbolLayer selectedMarkerManual = new SymbolLayer("selected-marker-layer-manual", "selected-marker-manual")
                .withProperties(PropertyFactory.iconImage("my-marker-image-manual"));
        mapbox.addLayer(selectedMarkerManual);

        Log.d("map", "showNewMarker finish");

    }


    public void notSendBumps(ArrayList<HashMap<Location, Float>> bumps, ArrayList<Integer> bumpsManual, int a, int b) {
        /// updatesLock=false;      toto tu nema čo robiť podľa mna !!!!!!!!!!!!!!!!!!!!!!!!!
        float rating;
        int i = 0;
        if (bumps.size() > 0) {

            SimpleDateFormat now;
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            now = new SimpleDateFormat("dd/MM/yyyy");
            String now_formated = now.format(cal.getTime());
            for (HashMap<Location, Float> bump : bumps) {

                Iterator it = bump.entrySet().iterator();
                while (it.hasNext()) {
                    HashMap.Entry pair = (HashMap.Entry) it.next();
                    Location loc = (Location) pair.getKey();
                    float data = (float) pair.getValue();
                    rating = 1;                             // 1 ,1,5 2,5
                    if (isBetween(data, 0, 6)) rating = 1;
                    if (isBetween(data, 6, 10)) rating = 1.5f;
                    if (isBetween(data, 10, 10000)) rating = 2.5f;
                    if (rating >= level) {
                        if (bumpsManual.get(i) == 0) {
                            autoMarkerCoordinates.add(Feature.fromGeometry(
                                    Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(loc.getLongitude(), loc.getLatitude()))) // Boston Common Park

                            );

                            Feature feature = autoMarkerCoordinates.get(a);
                            feature.addStringProperty("aaaaaa", getActivity().getResources().getString(R.string.auto_bump) + "\n" +
                                    getActivity().getResources().getString(R.string.number_bump) + "1\n" +
                                    getActivity().getResources().getString(R.string.modif) + " " + now_formated);
                            a++;
                        } else {
                            manualMarkerCoordinates.add(Feature.fromGeometry(
                                    Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(loc.getLongitude(), loc.getLatitude()))) // Boston Common Park
                            );
                            Feature featurea = manualMarkerCoordinates.get(b);
                            featurea.addStringProperty("aaaaaa", getActivity().getResources().getString(R.string.manual_bump) + "\n" +
                                    getActivity().getResources().getString(R.string.number_bump) + "1\n" +
                                    getActivity().getResources().getString(R.string.modif) + " " + now_formated);
                            b++;
                        }
                    }
                    i++;
                }
            }
        }

    }

    private double lang_database, longt_database;
    private int net, b_id_database, c_id_database, updates, max_number = 0;

    public void get_max_collision(final Double latitude, final Double longtitude, final Integer update) {
        Log.d("TTRREEE", "start get_max_collision  ");


        new Thread() {
            public void run() {
                Looper.prepare();


                while (true) {

                    if (updatesLock.tryLock()) {
                        // Got the lock
                        try {
                            SimpleDateFormat now, ago;
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(new Date());
                            now = new SimpleDateFormat("yyyy-MM-dd");
                            String now_formated = now.format(cal.getTime());
                            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE) - 280);
                            ago = new SimpleDateFormat("yyyy-MM-dd");
                            String ago_formated = ago.format(cal.getTime());

                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(getActivity());
                            SQLiteDatabase database = databaseHelper.getReadableDatabase();

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
                                        Log.d("TTRREEE", "max v collisions " + c_id_database);
                                    } while (cursor.moveToNext());
                                }
                            } finally {
                                // this gets called even if there is an exception somewhere above
                                if (cursor != null)
                                    cursor.close();
                            }

                            database.setTransactionSuccessful();
                            database.endTransaction();
                            database.close();
                            checkCloseDb(database);


                        } finally {
                            // Make sure to unlock so that we don't cause a deadlock
                            updatesLock.unlock();
                            break;
                        }
                    } else {
                        Log.d("getAllBumps", "getAllBumps thread lock bbbbbbbb ");
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                        }
                    }
                }


                updates = update;
                new Max_Collision_Number().execute();
                Looper.loop();
            }
        }.start();
    }

    class Max_Collision_Number extends AsyncTask<String, Void, JSONArray> {

        protected JSONArray doInBackground(String... args) {
            Log.d("TTRREEE", "start Max_Collision_Number ");
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            Log.d("TTRREEE", "odosielam lang_database   v Max_Collision_Number " + lang_database);
            Log.d("TTRREEE", "odosielam  longt_database v Max_Collision_Number " + longt_database);
            params.add(new BasicNameValuePair("latitude", String.valueOf(lang_database)));
            params.add(new BasicNameValuePair("longitude", String.valueOf(longt_database)));
            params.add(new BasicNameValuePair("b_id", String.valueOf(b_id_database)));
            params.add(new BasicNameValuePair("net", String.valueOf(net)));
            params.add(new BasicNameValuePair("c_id", String.valueOf(c_id_database)));

            JSONObject json = jsonParser.makeHttpRequest("http://sport.fiit.ngnlab.eu/update_collisions.php", "POST", params);
            if (json == null) {
                JSONArray response = new JSONArray();
                try {
                    response.put(0, "error");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                return response;
            }
            try {
                int success = json.getInt("success");

                if (success == 0) {
                    Log.d("TTRREEE", "5. Max_Collision_Number bumps");
                    // mám povolene stahovať a mám vytlky
                    bumps = json.getJSONArray("bumps");
                    return bumps;
                } else if (success == 1) {
                    // nemám povolene stahovať ale mám vytlky
                    JSONArray response = new JSONArray();
                    Log.d("TTRREEE", "5. Max_Collision_Number update");
                    response.put(0, "update");
                    return response;
                } else {
                    Log.d("TTRREEE", "5. Max_Collision_Number null");
                    // nemám nove vytlky
                    return null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                JSONArray response = new JSONArray();
                try {
                    Log.d("TTRREEE", "5. Max_Collision_Number error");
                    response.put(0, "error");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                return response;
            }

        }

        protected void onPostExecute(JSONArray array) {

            if (array == null) {
                Log.d("TTRREEE", "5. Max_Collision_Number  onPostExecute null");
                // collision nemaju update ale bumps ano
                if (updates == 1 || regularUpdatesLock) {
                    Log.d("TTRREEE", "5. Max_Collision_Number  onPostExecute GetUpdateAction");
                    GetUpdateAction();
                } else {
                    Log.d("TTRREEE", "5. Max_Collision_Number  onPostExecute citam getall");
                    // načítam vytlky na mapu
                    if (gps != null && gps.getCurrentLatLng() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                LatLng convert_location = gps.getCurrentLatLng();
                                getAllBumps(convert_location.latitude, convert_location.longitude);
                            }
                        });


                    }
                }
                return;
            }

            try {
                if (array.get(0).equals("error")) {
                    Log.d("TTRREEE", "5. Max_Collision_Number  onPostExecute error");
                    // nastala chyba, nacitam mapu
                    if (gps != null && gps.getCurrentLatLng() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                LatLng convert_location = gps.getCurrentLatLng();
                                getAllBumps(convert_location.latitude, convert_location.longitude);
                            }
                        });
                    }
                    return;

                } else if (array.get(0).equals("update")) {
                    Log.d("TTRREEE", "5. Max_Collision_Number  onPostExecute update a");
                    // mam vytlky na stiahnutie, ale potrebujem opravnenie od používateľa
                    GetUpdateAction();
                } else {

                    Thread t = new Thread() {
                        public void run() {
                            Log.d("TTRREEE", "6. Max_Collision_Number - thread  ");
                            Looper.prepare();
                            Boolean error = false;
                            while (true) {
                                if (updatesLock.tryLock()) {
                                    // Got the lock
                                    try {
                                        DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(getActivity());
                                        SQLiteDatabase database = databaseHelper.getWritableDatabase();

                                        checkIntegrityDB(database);
                                        database.beginTransaction();
                                        for (int i = 0; i < bumps.length(); i++) {
                                            JSONObject c = null;
                                            try {
                                                c = bumps.getJSONObject(i);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                                error = true;
                                            }

                                            int c_id, b_id;
                                            double intensity = 0;
                                            String created_at;
                                            Log.d("TTRREEE", " bob_id_database  " + b_id_database);
                                            Log.d("TTRREEE", " bloaded_index " + loaded_index);
                                            if (c != null) {
                                                try {
                                                    c_id = c.getInt("c_id");
                                                    b_id = c.getInt("b_id");
                                                    intensity = c.getDouble("intensity");
                                                    created_at = c.getString("created_at");
                                                    Log.d("TTRREEE", "c_id " + c_id);
                                                    Log.d("TTRREEE", "b_id " + b_id);
                                                    Log.d("TTRREEE", "intensity " + intensity);

                                                    // ak nove collision updatuju stare  vytlky
                                                    if (b_id <= loaded_index) {
                                                        Log.d("TTRREEE", " updatujem b_id ");
                                                        int rating = 0;
                                                        if (isBetween((float) intensity, 0, 6))
                                                            rating = 1;
                                                        if (isBetween((float) intensity, 6, 10))
                                                            rating = 2;
                                                        if (isBetween((float) intensity, 10, 10000))
                                                            rating = 3;
                                                        database.execSQL("UPDATE " + Provider.bumps_detect.TABLE_NAME_BUMPS + " SET rating=rating+ " + rating + ", count=count +1 WHERE b_id_bumps=" + b_id);
                                                    }

                                /* ak nastala chyba v transakcii,  musím upraviť udaje
                                  beriem od poslendej uspešnej transakcie collision po načitane max id z bumps
                                 */
                                                    if (b_id <= b_id_database && loaded_index < b_id) {
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
                                                                Log.d("TTRREEE", " bolo ich viac v  b_id ");
                                                                //  ak ich bolo viac pripičítam
                                                                sql = "UPDATE " + Provider.bumps_detect.TABLE_NAME_BUMPS + " SET rating=rating+ " + rating + ", count=count +1 WHERE b_id_bumps=" + b_id;
                                                            } else {
                                                                Log.d("TTRREEE", " bolo prvy b   b_id ");
                                                                // ak bol prvý, nastavujem na 1 count a rating prvého prijateho
                                                                sql = "UPDATE " + Provider.bumps_detect.TABLE_NAME_BUMPS + " SET rating=" + rating + ", count=1 WHERE b_id_bumps=" + b_id;
                                                            }
                                                            database.execSQL(sql);
                                                        } finally {
                                                            // this gets called even if there is an exception somewhere above
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
                                            checkCloseDb(database);


                                            ////////// updatesLock.getAndSet(false);
                                            // uložím najvyššie b_id  z bumps po uspešnej transakcii
                                            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                                            SharedPreferences.Editor editor = sharedPref.edit();
                                            editor.putInt("save", max_number);
                                            loaded_index = max_number;
                                            editor.commit();
                                        } else {
                                            // rollbacknem databazu
                                            database.endTransaction();
                                            database.close();
                                            checkCloseDb(database);

                                            //////////////   updatesLock.getAndSet(false);
                                        }
                                    } finally {
                                        // Make sure to unlock so that we don't cause a deadlock
                                        updatesLock.unlock();
                                        break;
                                    }
                                } else {

                                    Log.d("getAllBumps", "getAllBumps thread lock ccccccccccc");
                                    try {
                                        Random ran = new Random();
                                        int x = ran.nextInt(20) + 1;
                                        Thread.sleep(x);
                                    } catch (InterruptedException e) {
                                    }
                                }
                            }

                            if (!error) {
                                ////////// updatesLock.getAndSet(false);
                                // uložím najvyššie b_id  z bumps po uspešnej transakcii
                                SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putInt("save", max_number);
                                loaded_index = max_number;
                                editor.commit();
                            }
                            // načítam vytlky
                            if (gps != null && gps.getCurrentLatLng() != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        LatLng convert_location = gps.getCurrentLatLng();
                                        getAllBumps(convert_location.latitude, convert_location.longitude);
                                    }
                                });
                            }
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

    public void get_max_bumps(final Double langtitude, final Double longtitude, final Integer nets) {

        Log.d("TTRREEE", "spustam  get_max_bumps");


        new Thread() {
            public void run() {
                Looper.prepare();

                while (true) {
                    if (updatesLock.tryLock()) {
                        // Got the lock
                        try {
                            SimpleDateFormat now, ago;
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(new Date());
                            now = new SimpleDateFormat("yyyy-MM-dd");
                            String now_formated = now.format(cal.getTime());
                            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE) - 280);
                            ago = new SimpleDateFormat("yyyy-MM-dd");
                            String ago_formated = ago.format(cal.getTime());
                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(getActivity());
                            SQLiteDatabase database = databaseHelper.getReadableDatabase();

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
                                        Log.d("TTRREEE", "najvssie  b_id v bumps " + b_id_database);
                                    } while (cursor.moveToNext());

                                }
                            } finally {
                                // this gets called even if there is an exception somewhere above
                                if (cursor != null)
                                    cursor.close();
                            }


                            database.setTransactionSuccessful();
                            database.endTransaction();
                            database.close();
                            checkCloseDb(database);


                        } finally {
                            // Make sure to unlock so that we don't cause a deadlock
                            updatesLock.unlock();
                            break;
                        }
                    } else {

                        Log.d("getAllBumps", "getAllBumps thread lock ddddddddd");
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                        }
                    }
                }


                net = nets;
                lang_database = langtitude;
                longt_database = longtitude;

                new Max_Bump_Number().execute();
                Looper.loop();
            }
        }.start();
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
            if (json == null) {
                JSONArray response = new JSONArray();
                try {
                    response.put(0, "error");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                return response;
            }
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
                    Log.d("TTRREEE", "2. spustam Max_Bump_Number - null ");
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
                get_max_collision(lang_database, longt_database, 0);
                return;
            }

            try {
                if (array.get(0).equals("error")) {
                    Log.d("TTRREEE", "2.onPostExecute spustam Max_Bump_Number - error ");
                    return;

                } else if (array.get(0).equals("update")) {
                    // mam nove data, zisti aj collision a potom upozorni
                    Log.d("TTRREEE", "2.onPostExecute spustam Max_Bump_Number - update  ");
                    get_max_collision(lang_database, longt_database, 1);
                } else {
                    Log.d("TTRREEE", "2.onPostExecute spustam Max_Bump_Number - succes  ");
                    Thread t = new Thread() {
                        public void run() {
                            Looper.prepare();
                            Log.d("TTRREEE", "3. spustam Max_Bump_Number - thread ");
                            // insertujem nove data
                            Boolean error = false;

                            while (true) {
                                if (updatesLock.tryLock()) {
                                    // Got the lock
                                    try {
                                        DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(getActivity());
                                        SQLiteDatabase database = databaseHelper.getWritableDatabase();

                                        checkIntegrityDB(database);
                                        database.beginTransaction();
                                        for (int i = 0; i < bumps.length(); i++) {
                                            JSONObject c = null;
                                            try {
                                                c = bumps.getJSONObject(i);
                                            } catch (JSONException e) {
                                                error = true;
                                                e.printStackTrace();
                                            }
                                            double latitude, longitude;
                                            int count, b_id, rating, manual = 0;
                                            String last_modified;

                                            if (c != null) {
                                                try {
                                                    latitude = c.getDouble("latitude");
                                                    longitude = c.getDouble("longitude");
                                                    Log.d("TTRREEE", "latitude " + latitude);
                                                    Log.d("TTRREEE", "longitude" + longitude);

                                                    count = c.getInt("count");
                                                    b_id = c.getInt("b_id");
                                                    Log.d("TTRREEE", "b_id" + b_id);
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
                                                    database.insert(Provider.bumps_detect.TABLE_NAME_BUMPS, null, contentValues);
                                                } catch (JSONException e) {
                                                    error = true;
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                        if (!error) {
                                            // insert prebehol v poriadku, ukonči transakciu
                                            database.setTransactionSuccessful();
                                            database.endTransaction();
                                            database.close();
                                            checkCloseDb(database);

                                            //////// updatesLock.getAndSet(false);

                                        } else {
                                            // nastala chyba, načitaj uložene vytlky
                                            database.endTransaction();
                                            database.close();
                                            checkCloseDb(database);


                                        }
                                    } finally {
                                        // Make sure to unlock so that we don't cause a deadlock
                                        updatesLock.unlock();
                                        break;
                                    }
                                } else {

                                    Log.d("getAllBumps", "getAllBumps thread lock eeeeeeeeeeeeeeeeeeeee");
                                    try {
                                        Random ran = new Random();
                                        int x = ran.nextInt(20) + 1;
                                        Thread.sleep(x);
                                    } catch (InterruptedException e) {
                                    }
                                }
                            }

                            if (!error) {

                                //////// updatesLock.getAndSet(false);
                                get_max_collision(lang_database, longt_database, 0);
                                Looper.loop();

                            } else {
                                // nastala chyba, načitaj uložene vytlky

                                if (gps != null && gps.getCurrentLatLng() != null) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            LatLng convert_location = gps.getCurrentLatLng();
                                            getAllBumps(convert_location.latitude, convert_location.longitude);
                                        }
                                    });
                                }
                                Looper.loop();
                                //////////// updatesLock.getAndSet(false);


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

    private void GetUpdateAction() {
        // ak nemám dovolené sťahovať dáta,  ale mám update
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage(getActivity().getResources().getString(R.string.update));
        alert.setCancelable(false);
        alert.setPositiveButton(getActivity().getResources().getString(R.string.yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        if (regularUpdatesLock) {
                            lockHandler = false;
                            Log.d("TTRREEE", "regularUpdatesLock == 1 v getupdate");
                            ArrayList<HashMap<Location, Float>> bumpList = new ArrayList<HashMap<Location, Float>>();
                            bumpList.addAll(accelerometer.getPossibleBumps());
                            ArrayList<Integer> bumpsManual = new ArrayList<Integer>();
                            bumpsManual.addAll(accelerometer.getBumpsManual());
                            accelerometer.getPossibleBumps().clear();
                            accelerometer.getBumpsManual().clear();
                            saveBump(bumpList, bumpsManual, 0);

                        } else if (updates == 1) {
                            Log.d("TTRREEE", "updates == 1 v getupdate");
                            updates = 0;
                            // ak povolim, stiahnem data
                            LatLng convert_location = gps.getCurrentLatLng();
                            get_max_bumps(convert_location.latitude, convert_location.longitude, 1);
                        }
                    }
                });
        alert.setNegativeButton(getActivity().getResources().getString(R.string.nope),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        regularUpdatesLock = false;
                        // ak nepovolim, zobrazím aké mam doteraz
                        if (gps != null && gps.getCurrentLatLng() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    LatLng convert_location = gps.getCurrentLatLng();
                                    getAllBumps(convert_location.latitude, convert_location.longitude);
                                }
                            });
                        }
                    }
                });
        alert.show();
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

    private void showGPSDisabledAlertToUser() {
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

    private void showCalibrationAlert() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage(getActivity().getResources().getString(R.string.offer_calibrate));
        alert.setCancelable(false);
        alert.setPositiveButton(getActivity().getResources().getString(R.string.menu_calibrate),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mLocnServAcc.calibrate();
                        if (isEneableShowText())
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

    public void init_servise() {

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
                Log.d("TTRREEE", "spustil sa   sa Thread accelerometer ");


                Looper.prepare();

                while (true) {
                    accelerometer = mLocnServAcc;
                    if (accelerometer != null) {
                        loadSaveDB();
                        break;
                    }
                    try {
                        Thread.sleep(5); // sleep for 50 ms so that main UI thread can handle user actions in the meantime
                    } catch (InterruptedException e) {
                        // NOP (no operation)
                    }

                }
                Looper.loop();
            }
        }.start();


        Thread t = new Thread() {
            public void run() {
                Log.d("TTRREEE", "spustil sa   sa Thread gps ");


                Looper.prepare();

                while (true) {
                    gps = mLocnServGPS;
                    if (gps != null) {
                        LatLng convert_location = gps.getCurrentLatLng();
                        if (convert_location != null && !dbEnd) {
                            Log.d("TTRREEE", "break sa Thread");
                            break;

                        }
                    }

                    Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.fnd_location), Toast.LENGTH_SHORT).show();
                    try {
                        Thread.sleep(1000); // sleep for 50 ms so that main UI thread can handle user actions in the meantime
                    } catch (InterruptedException e) {
                        // NOP (no operation)
                    }


                    //  Log.d("TTRREEE","bezi  sa Thread");
                }
                detection = new navigationapp.main_application.Location();

                startGPS();

                Looper.loop();
                Log.d("TTRREEE", "konci  sa   sa Thread");

            }
        };
        t.start();
    }

    public void startGPS() {
        new Timer().schedule(new SyncDb(), 0, 120000);
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

    private boolean dbEnd = true;

    public void loadSaveDB() {

        Log.d("TTRREEE", "pustilo sa loadSaveDB");
        Thread t = new Thread() {
            public void run() {

                Looper.prepare();

                while (true) {
                    if (updatesLock.tryLock()) {
                        // Got the lock
                        try {
                            String selectQuery = "SELECT latitude,longitude,intensity,manual FROM new_bumps ";
                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(getActivity());
                            SQLiteDatabase database = databaseHelper.getReadableDatabase();

                            checkIntegrityDB(database);
                            Cursor cursor
                                    = database.rawQuery(selectQuery, null);
                            ArrayList<HashMap<Location, Float>> hashToArray = new ArrayList<HashMap<Location, Float>>();
                            ArrayList<Integer> listA = new ArrayList<Integer>();

                            long date = new Date().getTime();
                            int i = 0;

                            if (cursor != null && cursor.moveToFirst()) {
                                database.beginTransaction();
                                do {
                                    if (!cursor.isNull(0) && !cursor.isNull(1) & !cursor.isNull(2) && !cursor.isNull(3)) {
                                        Location location = new Location("new");
                                        location.setLatitude(cursor.getDouble(0));
                                        location.setLongitude(cursor.getDouble(1));
                                        location.setTime(date);
                                        HashMap<Location, Float> hashToArraya = new HashMap();
                                        hashToArraya.put(location, (float) cursor.getDouble(2));
                                        hashToArray.add(hashToArraya);
                                        listA.add(cursor.getInt(3));
                                        Log.d("loadSaveDB", "latitude " + cursor.getDouble(0));
                                        Log.d("loadSaveDB", "longitude " + cursor.getDouble(1));
                                        Log.d("loadSaveDB", "intensity " + cursor.getDouble(2));
                                    }

                                } while (cursor.moveToNext());
                                if (cursor != null) {
                                    accelerometer = mLocnServAcc;
                                    if (mLocnServAcc != null) {
                                        mLocnServAcc.getPossibleBumps().addAll(hashToArray);
                                        mLocnServAcc.getBumpsManual().addAll(listA);
                                    } else
                                        Log.d("loadSaveDB", "NULL ACCELEROMETER");

                                    database.setTransactionSuccessful();
                                    database.endTransaction();
                                    database.close();
                                    checkCloseDb(database);

                                }
                            }
                        } finally {
                            // Make sure to unlock so that we don't cause a deadlock
                            updatesLock.unlock();
                            break;
                        }
                    } else {
                        Log.d("getAllBumps", "getAllBumps thread lock iiiiiiiiiii");
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                        }
                    }
                }


                dbEnd = false;
                Looper.loop();


            }
        };
        t.start();
    }

    public void stop_servise() {
        getActivity().stopService(new Intent(getActivity().getApplicationContext(), Accelerometer.class));
        if (mServiceConnectedAcc) {
            getActivity().unbindService(mServconnAcc);
        }

        getActivity().stopService(new Intent(getActivity().getApplicationContext(), GPSLocator.class));
        if (mServiceConnectedGPS) {
            getActivity().unbindService(mServconnGPS);
        }
    }


    private boolean regularUpdatesLock = false;
    private boolean lockHandler = false;

    private class SyncDb extends TimerTask {

        @Override
        public void run() {

            getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.d("TTRREEE", "SyncDb");
                                                lockHandler = true;
                                                //  ak je pripojenie na internet
                                                if (isNetworkAvailable()) {
                                                    if (!(isEneableDownload() && !isConnectedWIFI())) {
                                                        if (accelerometer != null) {
                                                            ArrayList<HashMap<Location, Float>> list = accelerometer.getPossibleBumps();
                                                            //pouzivatel je upozorneni na odosielanie vytlkov notifikaciou
                                                            if (isEneableShowText())

                                                                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.save_bump) + "(" + list.size() + ")", Toast.LENGTH_SHORT).show();

                                                            if (!list.isEmpty()) {


                                                                regularUpdatesLock = false;
                                                                ArrayList<HashMap<Location, Float>> lista = new ArrayList<HashMap<Location, Float>>();
                                                                lista.addAll(accelerometer.getPossibleBumps());
                                                                ArrayList<Integer> bumpsManual = new ArrayList<Integer>();
                                                                bumpsManual.addAll(accelerometer.getBumpsManual());
                                                                accelerometer.getPossibleBumps().clear();
                                                                accelerometer.getBumpsManual().clear();
                                                                Log.d("TTRREEE", "saveBump spustam");
                                                                saveBump(lista, bumpsManual, 0);
                                                            } else {
                                                                Log.d("TTRREEE", "isEmpty  SyncDb");
                                                                getBumpsWithLevel();
                                                            }
                                                        } else {
                                                            Log.d("TTRREEE", "accelerometer null SyncDb");
                                                            getBumpsWithLevel();
                                                        }
                                                    } else {
                                                        Log.d("TTRREEE", "isConnectedWIFI SyncDb");
                                                        getBumpsWithLevel();
                                                    }
                                                } else {
                                                    Log.d("TTRREEE", "isNetworkAvailable SyncDb");
                                                    getBumpsWithLevel();
                                                }
                                            }
                                        }
            );
        }
    }

    private Bump Handler;
    private boolean lock = true;
    private ArrayList<HashMap<Location, Float>> listHelp;
    private ArrayList<Integer> bumpsManualHelp;
    private Integer poradie;

    public void saveBump(ArrayList<HashMap<Location, Float>> list, ArrayList<Integer> bumpsManual, Integer sequel) {
        listHelp = list;
        bumpsManualHelp = bumpsManual;
        poradie = sequel;
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
                                        int num = poradie;
                                        while (true) {
                                            if (updatesLock.tryLock()) {
                                                // Got the lock
                                                try {
                                                    DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(getActivity());
                                                    SQLiteDatabase database = databaseHelper.getWritableDatabase();


                                                    checkIntegrityDB(database);
                                                    // ak mi prišlo potvrdenie o odoslaní, mažem z db
                                                    database.beginTransaction();
                                                    database.execSQL("DELETE FROM new_bumps WHERE ROUND(latitude,7)= ROUND(" + loc.getLatitude() + ",7)  and ROUND(longitude,7)= ROUND(" + loc.getLongitude() + ",7)"
                                                            + " and  ROUND(intensity,6)==ROUND(" + data + ",6) and manual=" + bumpsManualHelp.get(num) + "");
                                                    Log.d("TEST", "mazem");
                                                    Log.d("mazem", listHelp.get(num).toString());
                                                    Log.d("mazem", bumpsManualHelp.get(num).toString());
                                                    listHelp.remove(num);
                                                    bumpsManualHelp.remove(num);
                                                    database.setTransactionSuccessful();
                                                    database.endTransaction();
                                                    database.close();
                                                    checkCloseDb(database);

                                                } finally {
                                                    // Make sure to unlock so that we don't cause a deadlock
                                                    updatesLock.unlock();
                                                    break;
                                                }
                                            } else {
                                                Log.d("getAllBumps", "getAllBumps thread lock iiiiiiiiiii");
                                                try {
                                                    Random ran = new Random();
                                                    int x = ran.nextInt(20) + 1;
                                                    Thread.sleep(x);
                                                } catch (InterruptedException e) {
                                                }
                                            }

                                        }

                                        lock = true;
                                    } else {
                                        // nastala chyba, nemažem
                                        Log.d("TEST", "error");
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

                while (true) {

                    if (lockAdd.tryLock()) {


                        try {
                            while (true) {
                                if (updatesLock.tryLock()) {
                                    // Got the lock
                                    try {
                                        if (listHelp.size() > 0 && accelerometer.getPossibleBumps().size() > 0) {
                                            int i = 0;
                                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(getActivity());
                                            SQLiteDatabase database = databaseHelper.getWritableDatabase();

                                            checkIntegrityDB(database);
                                            for (HashMap<Location, Float> oldList : listHelp) {
                                                Iterator oldListIteam = oldList.entrySet().iterator();
                                                while (oldListIteam.hasNext()) {
                                                    HashMap.Entry oldData = (HashMap.Entry) oldListIteam.next();
                                                    Location oldLocation = (Location) oldData.getKey();
                                                    i = 0;
                                                    for (HashMap<Location, Float> newList : accelerometer.getPossibleBumps()) {

                                                        Iterator newListIteam = newList.entrySet().iterator();
                                                        while (newListIteam.hasNext()) {
                                                            HashMap.Entry newData = (HashMap.Entry) newListIteam.next();
                                                            Location newLocation = (Location) newData.getKey();
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
                                        // Make sure to unlock so that we don't cause a deadlock
                                        updatesLock.unlock();
                                        break;
                                    }
                                } else {
                                    Log.d("getAllBumps", "getAllBumps thread lock iiiiiiiiiii");
                                    try {
                                        Thread.sleep(5);
                                    } catch (InterruptedException e) {
                                    }
                                }
                            }


                        } finally {
                            // Make sure to unlock so that we don't cause a deadlock
                            lockAdd.unlock();
                            break;
                        }
                    } else {
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                        }
                    }
                }


                // ak nastala chyba, aktualizujem s udajmi s novými udajmi čo som dostal počas behu saveBUmp

                // vypínam locky, nulujem pomocné polia

                listHelp = null;
                bumpsManualHelp = null;


                if (regularUpdatesLock) {
                    Log.d("TTRREEE", "updates == 1 v save");
                    updates = 0;
                    regularUpdatesLock = false;
                    lockHandler = false;
                    LatLng convert_location = gps.getCurrentLatLng();
                    get_max_bumps(convert_location.latitude, convert_location.longitude, 1);

                } else if (lockHandler) {
                    Log.d("TTRREEE", "lockHandler==true");
                    lockHandler = false;
                    getBumpsWithLevel();
                }

                Looper.loop();
            }
        };
        t.start();

    }

    public static void checkCloseDb(SQLiteDatabase database) {
        while (true) {
            if (!database.isOpen())
                break;
        }

    }

    public static void checkIntegrityDB(SQLiteDatabase database) {
        while (true) {
            if (!database.isDbLockedByOtherThreads())
                break;
        }
    }

    public void getBumpsWithLevel() {
        //ak je pripojenie na internet
        if (isNetworkAvailable()) {
            // ak som na wifi alebo mám povolenie
            if (!(isEneableDownload() && !isConnectedWIFI())) {

                if (isEneableShowText())
                    // stiahnem najnovšie udaje a zobrazím mapu
                    Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.map_setting), Toast.LENGTH_SHORT).show();
                regular_update = false;
                mLocnServGPS.setLevel(level);
                gps = mLocnServGPS;
                Log.d("TTRREEE", "gps pred null");
                if (gps != null) {
                    Log.d("TTRREEE", "gps po  null");
                    LatLng convert_location = gps.getCurrentLatLng();
                    if (convert_location != null) {
                        Log.d("TTRREEE", "mam GPS");
                        get_max_bumps(convert_location.latitude, convert_location.longitude, 1);
                    }
                }
            }
            // ak je to prve spustenie alebo pravidelný update
            else if (regular_update) {
                if (accelerometer != null && accelerometer.getPossibleBumps() != null && accelerometer.getPossibleBumps().size() > 0) {
                    regularUpdatesLock = true;
                }
                regular_update = false;
                LatLng convert_location = gps.getCurrentLatLng();
                get_max_bumps(convert_location.latitude, convert_location.longitude, 0);

            }
            // ak mám síce internet ale nemám povolené stahovanie, tk načítam z databazy
            else {
                regular_update = false;
                if (gps != null && gps.getCurrentLatLng() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            LatLng convert_location = gps.getCurrentLatLng();
                            getAllBumps(convert_location.latitude, convert_location.longitude);
                        }
                    });
                }
            }

        }
        // nemám internet, čítam z databazy
        else {
            if (gps != null && gps.getCurrentLatLng() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        LatLng convert_location = gps.getCurrentLatLng();
                        getAllBumps(convert_location.latitude, convert_location.longitude);
                    }
                });
            }
        }
    }

    public boolean isEneableShowText() {
        // či mám povolené ukazovať informácia aj mimo aplikácie
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Boolean alarm = prefs.getBoolean("alarm", Boolean.parseBoolean(null));
        if ((alarm) || (!alarm && MainActivity.isActivityVisible())) {
            return true;
        } else
            return false;
    }

    public boolean isEneableDownload() {
        // či je dovolené sťahovať len na wifi
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Boolean net = prefs.getBoolean("net", Boolean.parseBoolean(null));
        if (net) {
            return true;
        } else
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

    public boolean checkGPSEnable() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void mapTitleExceeded(Boolean value) {


        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor prefEditor = sharedPref.edit(); // Get preference in editor mode
        prefEditor.putBoolean("exceeded", value);
        prefEditor.commit();

    }

    public boolean isMapTitleExceeded() {


        // či je dovolené sťahovať len na wifi
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Boolean net = prefs.getBoolean("exceeded", Boolean.parseBoolean(null));
        if (net) {
            return true;
        } else
            return false;
    }


}

