package navigationapp.main_application;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.*;
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
import static navigationapp.main_application.FragmentActivity.lockAdd;
import static navigationapp.main_application.FragmentActivity.lockZoznam;
import static navigationapp.main_application.FragmentActivity.updatesLock;
import static navigationapp.main_application.MainActivity.mapbox;

/**
 * Created by Adam on 8.1.2017.
 */

public class MapLayer {
    Accelerometer accelerometer;
    private Context context;
    public MapLayer ( Accelerometer acc, Context context) {
        accelerometer =acc;
        this.context = context;
    }

    private final float ALL_BUMPS = 1.0f;   //level defaultne nastaveny pre zobrazovanie vsetkych vytlkov
    private final float MEDIUM_BUMPS = 1.5f;
    private final float LARGE_BUMPS = 2.5f;
    public float level = ALL_BUMPS;

    List<Feature> autoMarkerCoordinates = null;
    List<Feature> manualMarkerCoordinates = null;
    boolean deleteMap = false;
    synchronized public void getAllBumps(final Double latitude, final Double longitude) {
        if (latitude == null || longitude == null) {
            Toast.makeText(context, context.getResources().getString(R.string.no_gps), Toast.LENGTH_LONG).show();
            return;
        }
        new Thread() {
            public void run() {
                Looper.prepare();
                int autoBumpSequence = 0, manualBumpSequence = 0;
                autoMarkerCoordinates = new ArrayList<>();
                manualMarkerCoordinates = new ArrayList<>();
                while (true) {
                    if (updatesLock.tryLock()) {
                        try {
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
                            checkCloseDb(database);
                        } finally {
                            updatesLock.unlock();
                            break;
                        }
                    } else {
                        Log.d("aaa", "getAllBumps lock" + this.getId());
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
        }.start();


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

    boolean clear = true;

    public boolean isClear() {

        return clear;
    }

    public void setClear(boolean clear) {
        this.clear = clear;
    }

    synchronized public void deleteMarkers() {
        Log.d("map", "deleteOldMarker start");
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
        }catch (ConcurrentModificationException cme) {
            System.out.println("--- Stack Trace ---");
            cme.printStackTrace();
            cme.getMessage();
        }

        Log.d("map", "deleteOldMarker finish");
    }


    synchronized public void showMarkers() {

        Log.d("map", "showNewMarker start");
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

        }catch (ConcurrentModificationException cme) {
            System.out.println("--- Stack Trace ---");
            cme.printStackTrace();
            cme.getMessage();
        }

        Log.d("map", "showNewMarker finish");

    }


    public void notSendBumps(ArrayList<HashMap<android.location.Location, Float>> bumps, ArrayList<Integer> bumpsManual, int a, int b) {
        /// updatesLock=false;      toto tu nema čo robiť podľa mna !!!!!!!!!!!!!!!!!!!!!!!!!
        float rating;
        int i = 0;
        if (bumps.size() > 0) {

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
                    rating = 1;                             // 1 ,1,5 2,5
                    if (isBetween(data, 0, 6)) rating = 1;
                    if (isBetween(data, 6, 10)) rating = 1.5f;
                    if (isBetween(data, 10, 10000)) rating = 2.5f;
                    if (rating >= level) {
                        if (bumpsManual.get(i) == 0) {
                            autoMarkerCoordinates.add(Feature.fromGeometry(
                                    Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(loc.getLongitude(), loc.getLatitude()))) // Boston Common Park

                            );
                            if (autoMarkerCoordinates.size() ==a+1) {
                                Feature feature = autoMarkerCoordinates.get(a);
                                feature.addStringProperty("property", context.getResources().getString(R.string.auto_bump) + "\n" +
                                        context.getResources().getString(R.string.number_bump) + "1\n" +
                                        context.getResources().getString(R.string.modif) + " " + now_formated);
                                a++;
                            }
                        } else {
                            manualMarkerCoordinates.add(Feature.fromGeometry(
                                    Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(loc.getLongitude(), loc.getLatitude()))) // Boston Common Park
                            );
                            if (manualMarkerCoordinates.size() ==b+1) {
                                Feature featurea = manualMarkerCoordinates.get(b);
                                featurea.addStringProperty("property",context.getResources().getString(R.string.manual_bump) + "\n" +
                                        context.getResources().getString(R.string.number_bump) + "1\n" +
                                        context.getResources().getString(R.string.modif) + " " + now_formated);
                                b++;
                            }
                        }
                    }
                    i++;
                }
            }
        }

    }




}
