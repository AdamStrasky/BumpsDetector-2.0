package navigationapp.main_application;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.monikas.navigationapp.R;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.NoSuchLayerException;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.NoSuchSourceException;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.Point;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static navigationapp.main_application.Bump.isBetween;
import static navigationapp.main_application.FragmentActivity.checkCloseDb;
import static navigationapp.main_application.FragmentActivity.checkIntegrityDB;
import static navigationapp.main_application.FragmentActivity.isEneableShowText;
import static navigationapp.main_application.FragmentActivity.lockAdd;
import static navigationapp.main_application.FragmentActivity.lockZoznam;
import static navigationapp.main_application.FragmentActivity.updatesLock;
import static navigationapp.main_application.MainActivity.mapbox;


public class MapLayer {
    Accelerometer accelerometer = null ;
    private Context context = null;
    private final float ALL_BUMPS = 1.0f;   //level defaultne nastaveny pre zobrazovanie vsetkych vytlkov
    private final float MEDIUM_BUMPS = 1.5f;
    private final float LARGE_BUMPS = 2.5f;
    public float level = ALL_BUMPS;

    private List<Feature> autoMarkerCoordinates = null;  // markery
    private List<Feature> manualMarkerCoordinates = null;

    boolean deleteMap = false;  // flag na mazanie mapy
    boolean clear = true; // flag či mazať celu mapu

    public final String TAG = "MapLayer";

    public MapLayer ( Accelerometer acc, Context context) {
        accelerometer =acc;
        this.context = context;
    }

    synchronized public void getAllBumps(final Double latitude, final Double longitude) {
        if (latitude == null || longitude == null) {
            if (isEneableShowText(context))
            Toast.makeText(context, context.getResources().getString(R.string.no_gps), Toast.LENGTH_LONG).show();
            return;
        }
        new Thread() {
            public void run() {
                Looper.prepare();
                Log.d(TAG, " gettAllBump zobrazenie markerov  spustene ");
                int autoBumpSequence = 0, manualBumpSequence = 0;
                autoMarkerCoordinates = new ArrayList<>();
                manualMarkerCoordinates = new ArrayList<>();
                while (true) {
                    if (updatesLock.tryLock()) {
                        try {
                            Log.d(TAG, " lock získaný ");
                            if (deleteMap && mapbox != null)
                                deleteOldMarker();

                            SimpleDateFormat now, ago;
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(new Date());
                            now = new SimpleDateFormat("yyyy-MM-dd");
                            String now_formated = now.format(cal.getTime());
                            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE) - 280);  // posun od dnesneho dna o 280 dni
                            ago = new SimpleDateFormat("yyyy-MM-dd");
                            String ago_formated = ago.format(cal.getTime());
                            // seleknutie vytlk z oblasti a starych 280 dni
                            String selectQuery = "SELECT latitude,longitude,count,manual,last_modified FROM my_bumps WHERE rating/count >=" + level + " AND " +
                                    " ( last_modified BETWEEN '" + ago_formated + " 00:00:00' AND '" + now_formated + " 23:59:59') and  "
                                    + " (ROUND(latitude,1)==ROUND(" + latitude + ",1) and ROUND(longitude,1)==ROUND(" + longitude + ",1)) ";
                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(context);
                            SQLiteDatabase database = databaseHelper.getReadableDatabase();
                            checkIntegrityDB(database);
                            SimpleDateFormat dataFormatDatabase = new SimpleDateFormat("yyyy-MM-dd");
                            SimpleDateFormat dataFormatShow = new SimpleDateFormat("dd/MM/yyyy");
                            String showFormatData = null;
                            database.beginTransaction();
                            Cursor cursor = null;
                            try {
                                cursor = database.rawQuery(selectQuery, null);
                                if (cursor.moveToFirst()) {
                                    do {
                                        try { // konvertujem datum
                                            showFormatData = dataFormatShow.format(dataFormatDatabase.parse(cursor.getString(4).substring(0, 10)));
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                        if (cursor.getInt(3) == 0) { // nulou je označený  automaticky detegovaný výtlk
                                            autoMarkerCoordinates.add(Feature.fromGeometry(
                                                    Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(cursor.getDouble(1), cursor.getDouble(0))))
                                            ); //  zobrazované vlastnosti výtlku
                                            Feature autoFeature = autoMarkerCoordinates.get(autoBumpSequence);
                                            autoFeature.addStringProperty("property", context.getResources().getString(R.string.auto_bump) + "\n" +
                                                    context.getResources().getString(R.string.number_bump) + " " + cursor.getInt(2) + "\n" +
                                                    context.getResources().getString(R.string.modif) + " " + showFormatData);
                                            autoBumpSequence++;
                                        } else { // jednotkou je označený  manuálne detegovaný výtlk
                                            manualMarkerCoordinates.add(Feature.fromGeometry(
                                                    Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(cursor.getDouble(1), cursor.getDouble(0))))
                                            );
                                            Feature manualFeature = manualMarkerCoordinates.get(manualBumpSequence);
                                            manualFeature.addStringProperty("property", context.getResources().getString(R.string.manual_bump) + "\n" +
                                                    context.getResources().getString(R.string.number_bump) + " " + cursor.getInt(2) + "\n" +
                                                    context.getResources().getString(R.string.modif) + " " + showFormatData);
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
                            databaseHelper.close();
                            checkCloseDb(database);
                        } finally {
                            updatesLock.unlock();
                            Log.d(TAG, " gettAllBump markery z databázy vytiahnuté ");
                            break;
                        }
                    } else {
                        Log.d(TAG, " gettAllBump lock ");
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {}
                    }
                }

                ArrayList<HashMap<android.location.Location, Float>> bumpList = new ArrayList<HashMap<android.location.Location, Float>>();
                ArrayList<Integer> bumpsManual = new ArrayList<Integer>();
                while (true) {
                    if (lockAdd.tryLock()) {
                       try {
                           while (true) {
                               if (lockZoznam.tryLock()) {
                                   try {
                                       if (accelerometer != null && accelerometer.getPossibleBumps().size() > 0) {
                                           Log.d(TAG, " gettAllBump kopírujem zoznam ");
                                           bumpList.addAll(accelerometer.getPossibleBumps());
                                           bumpsManual.addAll(accelerometer.getBumpsManual());
                                        }
                                   } finally {
                                        lockZoznam.unlock();
                                        Log.d(TAG, " gettAllBump zoznam unlock ");
                                        break;
                                    }
                               } else {
                                    try {
                                        Log.d(TAG, " gettAllBump zoznam lock ");
                                        Random ran = new Random();
                                        int x = ran.nextInt(20) + 1;
                                        Thread.sleep(x);

                                    } catch (InterruptedException e) {
                                        e.getMessage();
                                    }
                               }

                           }
                       } finally {
                           Log.d(TAG, " gettAllBump lockadd unlock ");
                           lockAdd.unlock();
                           break;
                       }
                    } else {
                        try {
                            Log.d(TAG, " gettAllBump lockadd lock ");
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                            e.getMessage();
                        }
                    }
                }
                // zobrazenie výtlkov , ktoré boli pridané automaticky / manuálne bez synchronizácie zo serverom
                notSendBumps(bumpList, bumpsManual, autoBumpSequence, manualBumpSequence);
                if (mapbox != null)
                    showNewMarker(); // zobrazenie markerov
                deleteMap = true;
                Looper.loop();

            }
        }.start();
        }


    synchronized public void deleteOldMarker() {  // mazanie starých markerov
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
            Log.d("MapLayer", "DefaultCallback onCancel");
        }

        @Override
        public void onFinish() {
            Log.d("MapLayer", "DefaultCallback onFinish");
        }
    }

    synchronized private void showNewMarker() {  // zobrazenie nových markerov
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

    synchronized private void deleteMarkers() {

        Log.d(TAG, "deleteOldMarker start");
        try {
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
        } catch (ConcurrentModificationException e) {
           e.printStackTrace();
           e.getMessage();
        }

        Log.d(TAG, "deleteOldMarker finish");
    }

    synchronized private void showMarkers() {
        Log.d(TAG, "showNewMarker start");

        try {
            FeatureCollection featureCollectionAuto = FeatureCollection.fromFeatures(autoMarkerCoordinates);
            Source geoJsonSourceAuto = new GeoJsonSource("marker-source-auto", featureCollectionAuto);
            mapbox.addSource(geoJsonSourceAuto);

            FeatureCollection featureCollectionManual = FeatureCollection.fromFeatures(manualMarkerCoordinates);
            Source geoJsonSourceManual = new GeoJsonSource("marker-source-manual", featureCollectionManual);
            mapbox.addSource(geoJsonSourceManual);

            Bitmap iconAuto = BitmapFactory.decodeResource(context.getResources(), R.drawable.default_marker);
            mapbox.addImage("my-marker-image-auto", iconAuto);

            SymbolLayer markerAuto = new SymbolLayer("marker-layer-auto", "marker-source-auto")
                    .withProperties(PropertyFactory.iconImage("my-marker-image-auto"));
            mapbox.addLayer(markerAuto);

            Bitmap iconManual = BitmapFactory.decodeResource(context.getResources(), R.drawable.green_marker);
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

        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
            e.getMessage();
        }

        Log.d(TAG, "showNewMarker finish");
    }

    public void notSendBumps(ArrayList<HashMap<android.location.Location, Float>> bumps, ArrayList<Integer> bumpsManual, int autoSeq, int manulSeq) {
        Log.d(TAG, "notSendBumps dopnanie zoznamu");
        float rating = 1;
        int i = 0;
        if (bumps.size() > 0) {
            Log.d(TAG, "notSendBumps obsahuje zoznamy");
            SimpleDateFormat now;
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            now = new SimpleDateFormat("dd/MM/yyyy");
            String now_formated = now.format(cal.getTime());
            for (HashMap<android.location.Location, Float> bump : bumps) {
                Iterator it = bump.entrySet().iterator();
                while (it.hasNext()) {
                    HashMap.Entry pair = (HashMap.Entry) it.next();
                    android.location.Location loc = (android.location.Location) pair.getKey();
                    float data = (float) pair.getValue();
                    rating = 1;
                    if (isBetween(data, 0, 6)) rating = 1;
                    if (isBetween(data, 6, 10)) rating = 1.5f;
                    if (isBetween(data, 10, 10000)) rating = 2.5f;
                    if (rating >= level) {
                        if (bumpsManual.get(i) == 0) {
                            if (autoMarkerCoordinates.size() == autoSeq) {
                                autoMarkerCoordinates.add(Feature.fromGeometry(
                                        Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(loc.getLongitude(), loc.getLatitude()))) // Boston Common Park
                                );
                                Feature autoFeature = autoMarkerCoordinates.get(autoSeq);
                                autoFeature.addStringProperty("property", context.getResources().getString(R.string.auto_bump) + "\n" +
                                        context.getResources().getString(R.string.number_bump) + "1\n" +
                                        context.getResources().getString(R.string.modif) + " " + now_formated);
                                autoSeq++;
                            }

                        } else
                            if (manualMarkerCoordinates.size() == manulSeq) {
                            manualMarkerCoordinates.add(Feature.fromGeometry(
                                    Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(loc.getLongitude(), loc.getLatitude()))) // Boston Common Park
                            );
                            Feature manualFeature = manualMarkerCoordinates.get(manulSeq);
                            manualFeature.addStringProperty("property",context.getResources().getString(R.string.manual_bump) + "\n" +
                                        context.getResources().getString(R.string.number_bump) + "1\n" +
                                        context.getResources().getString(R.string.modif) + " " + now_formated);
                            manulSeq++;
                        }
                    }
                    i++;
                }
            }
        }
        Log.d(TAG, "notSendBumps koniec");

    }

    public boolean isClear() {
        return clear;
    }

    public void setClear(boolean clear) {
        this.clear = clear;
    }
}
