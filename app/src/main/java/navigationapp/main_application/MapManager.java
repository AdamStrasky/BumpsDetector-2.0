package navigationapp.main_application;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Address;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import navigationapp.error.ExceptionHandler;
import navigationapp.R;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import static com.mapbox.mapboxsdk.offline.OfflineManager.getInstance;

import static navigationapp.main_application.FragmentActivity.isEneableShowText;
import static navigationapp.main_application.MainActivity.add_button;
import static navigationapp.main_application.MainActivity.mapConfirm;
import static navigationapp.main_application.MainActivity.mapView;
import static navigationapp.main_application.MainActivity.mapbox;
import static navigationapp.main_application.MainActivity.navig_on;

public class MapManager extends Activity {

    public static String selectedName = null;
    private OfflineManager offlineManager = null;
    private OfflineRegion offlineRegion = null;
    private final  String TAG = "MapManager";
    public final double minZoomDownloadMap = 12;
    public final double maxZoomDownloadMap = 16;
    public static boolean setOnPosition = true;
    private int regionSelected = 0;
    private boolean isEndNotified = true;
    Context context = null;
    public final String JSON_CHARSET = "UTF-8";
    public final String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";

    public MapManager(Context context) {
        offlineManager = getInstance(context);
        this.context = context;
    }

    public void downloadRegionDialog() {
        Log.d(TAG,"downloadRegionDialog start");
        if (!isNetworkAvailable()) {
            if (isEneableShowText(context))
                Toast.makeText(context, context.getResources().getString(R.string.not_net_download), Toast.LENGTH_SHORT).show();
            return;
        }
        selectedName = null;
        // zobrazenie dialogu na vyber aktualnej mapy alebo zvolenej
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(context.getResources().getString(R.string.map_download));
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                context,android.R.layout.select_dialog_singlechoice);
        arrayAdapter.add(context.getResources().getString(R.string.current_map));
        arrayAdapter.add(context.getResources().getString(R.string.select_map));

        alert.setNegativeButton(
                context.getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }
        );

        alert.setAdapter(
                arrayAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // zvolená možnosť vyberu mapy
                        alertSelectRegion(null, which);
                    }
                }
        );
        alert.show();
    }

    public void alertSelectRegion(final String region,final int which) {
        Log.d(TAG, "alertSelectRegion start");
        String strName = context.getResources().getString(R.string.map_for_download);
        // ktorá možnosť bola zvolená - currnet/ select
        final int click = which;
        final EditText regionNameEdit = new EditText(context);
        if (which == 0)
            regionNameEdit.setHint(context.getResources().getString(R.string.name_region));
        else
            regionNameEdit.setHint(context.getResources().getString(R.string.name_region_download));
        // ak bolo znovuotvorene, nech zostane uložený nazov regionu
        if (region != null)
            regionNameEdit.setText(region);

        AlertDialog.Builder windowAlert = new AlertDialog.Builder(context);
        windowAlert.setPositiveButton(context.getResources().getString(R.string.download), null);
        windowAlert.setNegativeButton(context.getResources().getString(R.string.cancel), null);
        // ak volím select, dať možnosť aj zobraziť mapu
        if (which != 0)
            windowAlert.setNeutralButton(context.getResources().getString(R.string.navige_to), null);
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
                            if (isEneableShowText(context))
                                Toast.makeText(context, context.getResources().getString(R.string.region_empty), Toast.LENGTH_SHORT).show();
                        } else {
                            // zvolené zadanie regionu
                            if (click == 1) {
                                Address address = null;
                                try {
                                    address = Route.findLocality(regionName, context);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                if (address == null) {
                                    if (isEneableShowText(context))
                                        Toast.makeText(context, context.getResources().getString(R.string.region_not_exist), Toast.LENGTH_SHORT).show();
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
                            address = Route.findLocality(regionName, context);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (address == null) {
                            if (isEneableShowText(context))
                                Toast.makeText(context, context.getResources().getString(R.string.region_not_exist), Toast.LENGTH_LONG).show();

                        } else {
                            setOnPosition = false;
                            mapbox.setMyLocationEnabled(false);
                            mAlertDialog.cancel();

                            selectedName = regionName;
                            animetaCamera(address.getLatitude(), address.getLongitude());
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

    private void animetaCamera(double latitude, double longitude) {
        final CameraPosition position = new CameraPosition.Builder()
                .target(new com.mapbox.mapboxsdk.geometry.LatLng(latitude,longitude)) // Sets the new camera position
                .zoom(15) // Sets the zoom
                .build(); // Creates a CameraPosition from the builder

        mapbox.animateCamera(CameraUpdateFactory.newCameraPosition(position), 6000,
                new MapboxMap.CancelableCallback() {
                    @Override
                    public void onCancel() {
                        mapbox.animateCamera(CameraUpdateFactory.newCameraPosition(position), 6000);
                    }

                    @Override
                    public void onFinish() {
                        mapbox.animateCamera(CameraUpdateFactory.newCameraPosition(position), 6000);
                    }
                });
    }

    public void downloadRegion(final String regionName, final int select) {

       new Thread() {
            public void run() {
                Looper.prepare();
                mapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(MapboxMap mapboxMap) {
                        LatLngBounds bounds = null;
                        // ak bola zvolená sučasna obrazovka, vezme mapu zobrazenu na displeji
                        if (select == 0) {
                            Log.d(TAG, "downloadRegion sťahujem aktuálnu obrazovku");
                            bounds = mapbox.getProjection().getVisibleRegion().latLngBounds;
                        }
                        startProgress();
                        if (select == 1) {  // ak je 1, vyhľadám podľa názvu
                            Address address = null;
                            try {
                                address = Route.findLocality(regionName, context);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (address == null) {
                                Log.d(TAG, "downloadRegion sťahovanie podla názvu neuspešne");
                                endProgress(context.getResources().getString(R.string.unable_find));
                                if (isEneableShowText(context))
                                    Toast.makeText(context, context.getResources().getString(R.string.unable_find), Toast.LENGTH_LONG).show();
                                return;
                            } else {
                                Log.d(TAG, "downloadRegion sťahujem podla názvu");
                                bounds = new LatLngBounds.Builder()
                                        .include(new com.mapbox.mapboxsdk.geometry.LatLng(address.getLatitude() + 0.2, address.getLongitude() + 0.2)) // Northeast
                                        .include(new com.mapbox.mapboxsdk.geometry.LatLng(address.getLatitude() - 0.2, address.getLongitude() - .2)) // Southwest
                                        .build();
                            }
                        }
                        String styleURL = mapbox.getStyleUrl();
                        double minZoom = minZoomDownloadMap;
                        double maxZoom = maxZoomDownloadMap;
                        float pixelRatio = context.getResources().getDisplayMetrics().density;
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
                                MapManager.this.offlineRegion = offlineRegion;
                                launchDownload();
                            }

                                @Override
                            public void onError(String error) {
                                Log.e(TAG, "Error: " + error);
                                errorDownloadNotification();
                                if (isEneableShowText(context))
                                    Toast.makeText(context, context.getResources().getString(R.string.error_download), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
                Looper.loop();
            }
       }.start();
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
                    endProgress(context.getResources().getString(R.string.download_success));
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
                if (isEneableShowText(context))
                    Toast.makeText(context, context.getResources().getString(R.string.map_exceeded), Toast.LENGTH_LONG).show();
            }
        });

        // Change the region state
        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);
    }

    public void downloadedRegionList() {
        Log.d(TAG, "downloadedRegionList start ");
        // Build a region list when the user clicks the list button
        regionSelected = 0; // Reset the region selected int to 0

        // Query the DB asynchronously
        offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
            @Override
            public void onList(final OfflineRegion[] offlineRegions) {
                // Check result. If no regions have been
                // downloaded yet, notify user and return
                if (offlineRegions == null || offlineRegions.length == 0) {
                    if (isEneableShowText(context))
                        Toast.makeText(context, context.getResources().getString(R.string.no_regions), Toast.LENGTH_SHORT).show();
                    return;
                }
                // Add all of the region names to a list
                ArrayList<String> offlineRegionsNames = new ArrayList<>();
                for (OfflineRegion offlineRegion : offlineRegions) {
                    offlineRegionsNames.add(getRegionName(offlineRegion));
                }
                final CharSequence[] items = offlineRegionsNames.toArray(new CharSequence[offlineRegionsNames.size()]);

                // Build a dialog containing the list of regions
                AlertDialog dialog = new AlertDialog.Builder(context)
                        .setTitle(context.getResources().getString(R.string.list))
                        .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                              regionSelected = which;
                            }
                        })
                        .setPositiveButton(context.getResources().getString(R.string.navige_to), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                    setOnPosition = false;
                                    if (isEneableShowText(context))
                                        Toast.makeText(context, items[regionSelected], Toast.LENGTH_LONG).show();
                                    // Get the region bounds and zoom
                                    LatLngBounds bounds = ((OfflineTilePyramidRegionDefinition) offlineRegions[regionSelected].getDefinition()).getBounds();
                                    double regionZoom = ((OfflineTilePyramidRegionDefinition) offlineRegions[regionSelected].getDefinition()).getMinZoom();
                                    // presuniem sa na pozíciu
                                    animetaCamera(bounds.getCenter().getLatitude(), bounds.getCenter().getLongitude());

                                    add_button.setVisibility(View.INVISIBLE);
                                    navig_on.setVisibility(View.VISIBLE);
                            }
                        })
                        .setNeutralButton(context.getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // Begin the deletion process
                                offlineRegions[regionSelected].delete(new OfflineRegion.OfflineRegionDeleteCallback() {
                                    @Override
                                    public void onDelete() {
                                        if (isEneableShowText(context))
                                            Toast.makeText(context, context.getResources().getString(R.string.region_delete), Toast.LENGTH_LONG).show();
                                        mapTitleExceeded(false); // zrušim prekročenie veľkosti databazy

                                    }
                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "Error: " + error);
                                    }
                                });
                            }
                        })
                        .setNegativeButton(context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        }).create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
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

    public boolean isEndNotified() {
        return isEndNotified;
    }

    private void startProgress() {
        if (isEneableShowText(context))
            Toast.makeText(context, context.getResources().getString(R.string.download_start), Toast.LENGTH_LONG).show();
        downloadNotification();
        isEndNotified = false;
    }

    private void endProgress(final String message) {
       if (isEndNotified)
           return;
       if (message != null) {
           Log.d(TAG, "download end  notification");
           mBuilder.setContentText(context.getResources().getString(R.string.download_complete))
                   .setProgress(0, 0, false);
           mNotifyManager.notify(0, mBuilder.build());
       }

        isEndNotified = true;

        if (message != null) {
            if (isEneableShowText(context))
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    private NotificationManager mNotifyManager = null;
    private NotificationCompat.Builder mBuilder = null;

    public void downloadNotification() {
        Log.d(TAG, "downloadNotification start");
        mNotifyManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        mBuilder = new android.support.v4.app.NotificationCompat.Builder(context);
        mBuilder.setContentTitle(context.getResources().getString(R.string.notif_map_download))
                .setContentText(context.getResources().getString(R.string.notif_map_progress))
                .setSmallIcon(R.drawable.download);
    }

    public void endDownloadNotification() {
        Log.d(TAG, "remove notification");
        if (mNotifyManager!=null)
            mNotifyManager.cancelAll();
    }

    public void errorDownloadNotification() {
        Log.d(TAG, "errorDownloadNotification set");
        mBuilder.setContentTitle(context.getResources().getString(R.string.notif_map_error))
                .setContentText(context.getResources().getString(R.string.notif_map_interrupted))
                .setProgress(0, 0, false);
        mNotifyManager.notify(0, mBuilder.build());
    }

    public boolean isNetworkAvailable() {
       ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return (activeNetworkInfo != null && activeNetworkInfo.isConnected());
    }

    public void mapTitleExceeded(Boolean value) {
        // nastavenie prečerpaného množstva máp
        Log.d(TAG, "mapTitleExceeded set" + value);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putBoolean("exceeded", value);
        prefEditor.commit();
    }

    public boolean isMapTitleExceeded() {
        // kontrolo prečerpaného množstva máp
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d(TAG, "isMapTitleExceeded" + prefs.getBoolean("exceeded", Boolean.parseBoolean(null)));
        return  prefs.getBoolean("exceeded", Boolean.parseBoolean(null));
    }
}
