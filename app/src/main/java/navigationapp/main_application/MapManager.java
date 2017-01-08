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
import android.os.Bundle;
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

import com.example.monikas.navigationapp.R;
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
import static navigationapp.main_application.FragmentActivity.JSON_CHARSET;
import static navigationapp.main_application.FragmentActivity.JSON_FIELD_REGION_NAME;
import static navigationapp.main_application.MainActivity.add_button;
import static navigationapp.main_application.MainActivity.mapConfirm;
import static navigationapp.main_application.MainActivity.mapView;
import static navigationapp.main_application.MainActivity.mapbox;
import static navigationapp.main_application.MainActivity.navig_on;

/**
 * Created by Adam on 8.1.2017.
 */

public class MapManager extends Activity {


    public static String selectedName = null;
    private OfflineManager offlineManager;
    private OfflineRegion offlineRegion;
    private final static String TAG = "MainActivity";
    public final double minZoomDownloadMap = 12;
    public final double maxZoomDownloadMap = 16;
    public static boolean setOnPosition = true;
    private int regionSelected;
    public static boolean flagDownload = false;
    private boolean isEndNotified = true;
    Context context;

    public MapManager(Context context) {
        offlineManager = getInstance(context);
        this.context = context;

    }


    public void downloadRegionDialog() {
        if (!isNetworkAvailable()) {

            Toast.makeText(context, context.getResources().getString(R.string.not_net_download), Toast.LENGTH_SHORT).show();
            return;
        }
        selectedName = null;

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);

        builderSingle.setIcon(R.drawable.ic_launcher);
        builderSingle.setTitle(context.getResources().getString(R.string.map_download));

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                context,
                android.R.layout.select_dialog_singlechoice);
        arrayAdapter.add(context.getResources().getString(R.string.current_map));
        arrayAdapter.add(context.getResources().getString(R.string.select_map));

        builderSingle.setNegativeButton(
                context.getResources().getString(R.string.cancel),
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
                            Toast.makeText(context, context.getResources().getString(R.string.region_not_exist), Toast.LENGTH_LONG).show();

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
        mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION);
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
                                address = Route.findLocality(regionName, context);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            if (address == null) {
                                endProgress(context.getResources().getString(R.string.unable_find));
                                Toast.makeText(context, context.getResources().getString(R.string.unable_find), Toast.LENGTH_LONG).show();
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
                                Toast.makeText(context, context.getResources().getString(R.string.error_download), Toast.LENGTH_LONG).show();

                            }
                        });

                    }
                });


                Looper.loop();


            }
        };
        t.start();
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
                Toast.makeText(context, context.getResources().getString(R.string.map_exceeded), Toast.LENGTH_LONG).show();
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
                                // Track which region the user selects
                                regionSelected = which;
                            }
                        })
                        .setPositiveButton(context.getResources().getString(R.string.navige_to), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {



                                    setOnPosition = false;
                                    Toast.makeText(context, items[regionSelected], Toast.LENGTH_LONG).show();

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
                        })
                        .setNeutralButton(context.getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
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

                                        Toast.makeText(context, context.getResources().getString(R.string.region_delete), Toast.LENGTH_LONG).show();
                                        mapTitleExceeded(false);

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
                                // When the user cancels, don't do anything.
                                // The dialog will automatically close
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

    // Progress bar methods
    private void startProgress() {
        Toast.makeText(context, context.getResources().getString(R.string.download_start), Toast.LENGTH_LONG).show();
        downloadNotification();
        flagDownload = true;
        isEndNotified = false;
    }

    private void endProgress(final String message) {
        // Don't notify more than once

        if (isEndNotified) return;

        if (message != null)
            mBuilder.setContentText(context.getResources().getString(R.string.download_complete))
                    // Removes the progress bar
                    .setProgress(0, 0, false);
        mNotifyManager.notify(0, mBuilder.build());

        // Stop and hide the progress bar
        isEndNotified = true;
        flagDownload = false;

        if (message != null)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private NotificationManager mNotifyManager=null;
    private NotificationCompat.Builder mBuilder=null;

    public void downloadNotification() {
        mNotifyManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        mBuilder = new android.support.v4.app.NotificationCompat.Builder(context);
        mBuilder.setContentTitle(context.getResources().getString(R.string.notif_map_download))
                .setContentText(context.getResources().getString(R.string.notif_map_progress))
                .setSmallIcon(R.drawable.download);


    }

    public void endDownloadNotification() {
        if (mNotifyManager!=null)
            mNotifyManager.cancelAll();
    }

    public void errorDownloadNotification() {
        mBuilder.setContentTitle(context.getResources().getString(R.string.notif_map_error))
                .setContentText(context.getResources().getString(R.string.notif_map_interrupted))
                .setProgress(0, 0, false);
        mNotifyManager.notify(0, mBuilder.build());

    }



    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    public void mapTitleExceeded(Boolean value) {


        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = sharedPref.edit(); // Get preference in editor mode
        prefEditor.putBoolean("exceeded", value);
        prefEditor.commit();

    }

    public boolean isMapTitleExceeded() {


        // či je dovolené sťahovať len na wifi
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean net = prefs.getBoolean("exceeded", Boolean.parseBoolean(null));
        if (net) {
            return true;
        } else
            return false;
    }
}
