package navigationapp.main_application;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import navigationapp.R;
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
import java.util.TimeZone;

import static navigationapp.main_application.Bump.isBetween;
import static navigationapp.main_application.FragmentActivity.checkCloseDb;
import static navigationapp.main_application.FragmentActivity.checkIntegrityDB;
import static navigationapp.main_application.FragmentActivity.isEneableShowText;
import static navigationapp.main_application.FragmentActivity.lockAdd;
import static navigationapp.main_application.FragmentActivity.lockZoznam;
import static navigationapp.main_application.FragmentActivity.updatesLock;
import static navigationapp.main_application.MainActivity.getDate;
import static navigationapp.main_application.MainActivity.mapbox;


public class MapLayer {
    Accelerometer accelerometer = null ;
    private Context context = null;
    private final float ALL_BUMPS = 1.0f;   //level defaultne nastaveny pre zobrazovanie vsetkych vytlkov
    private final float MEDIUM_BUMPS = 1.5f;
    private final float LARGE_BUMPS = 2.5f;
    private final static long MILLISECS_PER_DAY = 24 * 60 * 60 * 1000;
    public float level = ALL_BUMPS;

    private List<Feature> autoMarkerCoordinates = null;  // markery
    private List<Feature> manualMarkerCoordinates = null;
    private List<Feature> reportMarkerCoordinates = null;
    private List<Feature> repairedBumpMarkerCoordinates = null;
    private List<Feature> repairedReportMarkerCoordinates = null;

    boolean deleteMap = false;  // flag na mazanie mapy
    boolean clear = true; // flag či mazať celu mapu
    Marker marker = null;
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

                Log.d(TAG, " gettAllBump zobrazenie markerov  spustene ");
                int autoBumpSequence = 0, manualBumpSequence = 0,
                    reportSequence = 0, repairedBumpSequence= 0,
                    repairedReportSequence = 0;
                autoMarkerCoordinates = new ArrayList<>();
                manualMarkerCoordinates = new ArrayList<>();
                reportMarkerCoordinates = new ArrayList<>();
                repairedBumpMarkerCoordinates = new ArrayList<>();
                repairedReportMarkerCoordinates = new ArrayList<>();
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
                            String selectQuery = "SELECT latitude,longitude,count,manual,last_modified,info,fix FROM my_bumps WHERE admin_fix=0 AND rating/count >=" + level + " AND " +
                                    " ( last_modified BETWEEN '" + ago_formated + " 00:00:00' AND '" + now_formated + " 23:59:59') and  "
                                    + " (ROUND(latitude,1)==ROUND(" + latitude + ",1) and ROUND(longitude,1)==ROUND(" + longitude + ",1) AND type=0) ";
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
                                        if (cursor.getInt(3) == 0 && cursor.getInt(6) == 0) { // nulou je označený  automaticky detegovaný výtlk
                                            autoMarkerCoordinates.add(Feature.fromGeometry(
                                                    Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(cursor.getDouble(1), cursor.getDouble(0))))
                                            ); //  zobrazované vlastnosti výtlku
                                            Feature autoFeature = autoMarkerCoordinates.get(autoBumpSequence);
                                            autoFeature.addStringProperty("property", context.getResources().getString(R.string.auto_bump) + "\n" +
                                                    context.getResources().getString(R.string.number_bump) + " " + cursor.getInt(2) + "\n" +
                                                    context.getResources().getString(R.string.modif) + " " + showFormatData);
                                            autoFeature.addStringProperty("lat", String.valueOf(cursor.getDouble(0)));
                                            autoFeature.addStringProperty("ltn", String.valueOf(cursor.getDouble(1)));
                                            autoFeature.addStringProperty("type", String.valueOf(0));
                                            autoFeature.addStringProperty("text", String.valueOf( cursor.getString(5)));
                                            autoBumpSequence++;
                                        } else if (cursor.getInt(3) == 1 && cursor.getInt(6) == 0) { // jednotkou je označený  manuálne detegovaný výtlk
                                            manualMarkerCoordinates.add(Feature.fromGeometry(
                                                    Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(cursor.getDouble(1), cursor.getDouble(0))))
                                            );
                                            Feature manualFeature = manualMarkerCoordinates.get(manualBumpSequence);
                                            manualFeature.addStringProperty("property", context.getResources().getString(R.string.manual_bump) + "\n" +
                                                    context.getResources().getString(R.string.number_bump) + " " + cursor.getInt(2) + "\n" +
                                                    context.getResources().getString(R.string.modif) + " " + showFormatData);
                                            manualFeature.addStringProperty("lat", String.valueOf(cursor.getDouble(0)));
                                            manualFeature.addStringProperty("ltn", String.valueOf(cursor.getDouble(1)));
                                            manualFeature.addStringProperty("type", String.valueOf(0));
                                            manualFeature.addStringProperty("text", String.valueOf( cursor.getString(5)));
                                            manualBumpSequence++;
                                        }
                                        else if ( cursor.getInt(6) == 1 )  {
                                            repairedBumpMarkerCoordinates.add(Feature.fromGeometry(
                                                    Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(cursor.getDouble(1), cursor.getDouble(0))))
                                            ); //  zobrazované vlastnosti výtlku
                                            Feature repairedFeature = repairedBumpMarkerCoordinates.get(repairedBumpSequence);
                                            repairedFeature.addStringProperty("property", context.getResources().getString(R.string.repaired_bump) + "\n" +
                                                    context.getResources().getString(R.string.number_bump) + " " + cursor.getInt(2) + "\n" +
                                                    context.getResources().getString(R.string.modif) + " " + showFormatData);
                                            repairedFeature.addStringProperty("lat", String.valueOf(cursor.getDouble(0)));
                                            repairedFeature.addStringProperty("ltn", String.valueOf(cursor.getDouble(1)));
                                            repairedFeature.addStringProperty("type", String.valueOf(0));
                                            repairedFeature.addStringProperty("text", String.valueOf( cursor.getString(5)));
                                            repairedBumpSequence++;
                                        }
                                    }
                                    while (cursor.moveToNext());
                                }
                            } finally {
                                if (cursor != null)
                                    cursor.close();
                            }
                            /////////////////////////////////////////////////////////////

                            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE) - 3);  // posun od dnesneho dna o 280 dni
                            ago = new SimpleDateFormat("yyyy-MM-dd");
                            ago_formated = ago.format(cal.getTime());
                            selectQuery = "SELECT latitude,longitude,count,last_modified,info,type, fix FROM my_bumps WHERE admin_fix=0 AND " +
                                    " ( last_modified BETWEEN '" + ago_formated + " 00:00:00' AND '" + now_formated + " 23:59:59') and  "
                                    + " (ROUND(latitude,1)==ROUND(" + latitude + ",1) and ROUND(longitude,1)==ROUND(" + longitude + ",1)  AND type>0) ";

                            cursor = null;
                            try {
                                cursor = database.rawQuery(selectQuery, null);
                                if (cursor.moveToFirst()) {
                                    do {
                                        try { // konvertujem datum
                                            showFormatData = dataFormatShow.format(dataFormatDatabase.parse(cursor.getString(3).substring(0, 10)));
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }

                                        String select_iteam = cursor.getString(4);
                                        switch (cursor.getInt(5)) {
                                            case 1:
                                                select_iteam = context.getResources().getString(R.string.type_problem_basket);
                                                break;
                                            case 2:
                                                select_iteam = context.getResources().getString(R.string.type_problem_canstock);
                                                break;
                                        }
                                         if (cursor.getInt(6) == 0) {
                                             reportMarkerCoordinates.add(Feature.fromGeometry(
                                                     Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(cursor.getDouble(1), cursor.getDouble(0))))
                                             ); //  zobrazované vlastnosti výtlku
                                             Feature reportFeature = reportMarkerCoordinates.get(reportSequence);
                                             reportFeature.addStringProperty("property", select_iteam +  "\n" +
                                                     context.getResources().getString(R.string.number_bump) + " " + cursor.getInt(2) + "\n" +
                                                     context.getResources().getString(R.string.modif) + " " + showFormatData);
                                             reportFeature.addStringProperty("lat", String.valueOf(cursor.getDouble(0)));
                                             reportFeature.addStringProperty("ltn", String.valueOf(cursor.getDouble(1)));
                                             reportFeature.addStringProperty("type", String.valueOf(cursor.getInt(5)));
                                             reportFeature.addStringProperty("text", String.valueOf(cursor.getString(4)));
                                             reportSequence++;
                                         } else  if (cursor.getInt(6) == 1) {
                                             repairedReportMarkerCoordinates.add(Feature.fromGeometry(
                                                     Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(cursor.getDouble(1), cursor.getDouble(0))))
                                             ); //  zobrazované vlastnosti výtlku
                                             Feature repairedFeature = repairedReportMarkerCoordinates.get(repairedReportSequence);
                                             repairedFeature.addStringProperty("property", select_iteam + " "+  context.getResources().getString(R.string.type_problem_repaired)+ "\n" +
                                                     context.getResources().getString(R.string.number_bump) + " " + cursor.getInt(2) + "\n" +
                                                     context.getResources().getString(R.string.modif) + " " + showFormatData);
                                             repairedFeature.addStringProperty("lat", String.valueOf(cursor.getDouble(0)));
                                             repairedFeature.addStringProperty("ltn", String.valueOf(cursor.getDouble(1)));
                                             repairedFeature.addStringProperty("type", String.valueOf(cursor.getInt(5)));
                                             repairedFeature.addStringProperty("text", String.valueOf(cursor.getString(4)));
                                             repairedReportSequence++;
                                        }
                                    }
                                    while (cursor.moveToNext());
                                }
                            } finally {
                                if (cursor != null)
                                    cursor.close();
                            }
                            ///////////////////////////////////////////////////////////
                            database.setTransactionSuccessful();
                            database.endTransaction();
                            database.close();
                            databaseHelper.close();
                            checkCloseDb(database);
                        } finally {
                            updatesLock.unlock();
                            break;
                        }
                    } else {
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {}
                    }
                }

                ArrayList<HashMap<android.location.Location, Float>> bumpList = new ArrayList<HashMap<android.location.Location, Float>>();
                ArrayList<Integer> bumpsManual = new ArrayList<Integer>();
                ArrayList<Integer> detectType = new ArrayList<Integer>();
                ArrayList<String> detectText = new ArrayList<String>();
                while (true) {
                    if (lockAdd.tryLock()) {
                       try {
                           while (true) {
                               if (lockZoznam.tryLock()) {
                                   try {
                                       if (accelerometer != null && accelerometer.getPossibleBumps().size() > 0) {
                                           bumpList.addAll(accelerometer.getPossibleBumps());
                                           bumpsManual.addAll(accelerometer.getBumpsManual());
                                           detectType.addAll(accelerometer.gettypeDetect());
                                           detectText.addAll(accelerometer.gettextDetect());
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
                                        e.getMessage();
                                    }
                               }

                           }
                       } finally {
                           lockAdd.unlock();
                           break;
                       }
                    } else {
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                            e.getMessage();
                        }
                    }
                }
                // zobrazenie výtlkov , ktoré boli pridané automaticky / manuálne bez synchronizácie zo serverom
                notSendBumps(bumpList, bumpsManual, autoBumpSequence, manualBumpSequence,reportSequence, detectType,detectText);
                if (mapbox != null)
                    showNewMarker(); // zobrazenie markerov
                deleteMap = true;


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

        }

        @Override
        public void onFinish() {
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

       try {
            if ( mapbox != null) {
                List<Marker> markers = mapbox.getMarkers();
                for (int i = 0; i < markers.size(); i++) {
                    if (markers.get(i)!= marker )
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

            try {
                if (mapbox.getSource("marker-source-report") != null)
                    mapbox.removeSource("marker-source-report");
            } catch (NoSuchSourceException e) {
                e.printStackTrace();
                e.getMessage();
            }

            try {
                if (mapbox.getSource("marker-source-report-repaired") != null)
                    mapbox.removeSource("marker-source-report-repaired");
            } catch (NoSuchSourceException e) {
                e.printStackTrace();
                e.getMessage();
            }

            try {
                if (mapbox.getSource("marker-source-bump-repaired") != null)
                    mapbox.removeSource("marker-source-bump-repaired");
            } catch (NoSuchSourceException e) {
                e.printStackTrace();
                e.getMessage();
            }

            mapbox.removeImage("my-marker-image-auto");
            mapbox.removeImage("my-marker-image-manual");
            mapbox.removeImage("my-marker-image-report");
            mapbox.removeImage("my-marker-image-report-repaired");
            mapbox.removeImage("my-marker-image-bump-repaired");

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
                if (mapbox.getLayer("marker-layer-report") != null)
                    mapbox.removeLayer("marker-layer-report");
            } catch (NoSuchLayerException e) {
                e.getMessage();
            }

            try {
                if (mapbox.getLayer("marker-layer-report-repaired") != null)
                    mapbox.removeLayer("marker-layer-report-repaired");
            } catch (NoSuchLayerException e) {
                e.getMessage();
            }

            try {
                if (mapbox.getLayer("marker-layer-bump-repaired") != null)
                    mapbox.removeLayer("marker-layer-bump-repaired");
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
                if (mapbox.getSource("selected-marker-report") != null)
                    mapbox.removeSource("selected-marker-report");
            } catch (NoSuchSourceException e) {
                e.printStackTrace();
                e.getMessage();
            }

            try {
                if (mapbox.getSource("selected-marker-report-repaired") != null)
                    mapbox.removeSource("selected-marker-report-repaired");
            } catch (NoSuchSourceException e) {
                e.printStackTrace();
                e.getMessage();
            }

            try {
                if (mapbox.getSource("selected-marker-bump-repaired") != null)
                    mapbox.removeSource("selected-marker-bump-repaired");
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
            try {
                if (mapbox.getLayer("selected-marker-layer-report") != null)
                    mapbox.removeLayer("selected-marker-layer-report");
            } catch (NoSuchLayerException e) {
                e.getMessage();
            }

            try {
                if (mapbox.getLayer("selected-marker-layer-report-repaired") != null)
                    mapbox.removeLayer("selected-marker-layer-report-repaired");
            } catch (NoSuchLayerException e) {
                e.getMessage();
            }

            try {
                if (mapbox.getLayer("selected-marker-layer-bump-repaired") != null)
                    mapbox.removeLayer("selected-marker-layer-bump-repaired");
            } catch (NoSuchLayerException e) {
                e.getMessage();
            }
        } catch (ConcurrentModificationException e) {
           e.printStackTrace();
           e.getMessage();
        }

    }

    synchronized private void showMarkers() {


        try {
            FeatureCollection featureCollectionAuto = FeatureCollection.fromFeatures(autoMarkerCoordinates);
            Source geoJsonSourceAuto = new GeoJsonSource("marker-source-auto", featureCollectionAuto);
            mapbox.addSource(geoJsonSourceAuto);

            FeatureCollection featureCollectionManual = FeatureCollection.fromFeatures(manualMarkerCoordinates);
            Source geoJsonSourceManual = new GeoJsonSource("marker-source-manual", featureCollectionManual);
            mapbox.addSource(geoJsonSourceManual);

            FeatureCollection featureCollectionReport = FeatureCollection.fromFeatures(reportMarkerCoordinates);
            Source geoJsonSourceReport = new GeoJsonSource("marker-source-report", featureCollectionReport);
            mapbox.addSource(geoJsonSourceReport);

            FeatureCollection featureCollectionReportRepaired = FeatureCollection.fromFeatures(repairedReportMarkerCoordinates);
            Source geoJsonSourceReportRepaired = new GeoJsonSource("marker-source-report-repaired", featureCollectionReportRepaired);
            mapbox.addSource(geoJsonSourceReportRepaired);

            FeatureCollection featureCollectionBumpRepaired = FeatureCollection.fromFeatures(repairedBumpMarkerCoordinates);
            Source geoJsonSourceBumpRepaired = new GeoJsonSource("marker-source-bump-repaired", featureCollectionBumpRepaired);
            mapbox.addSource(geoJsonSourceBumpRepaired);
            /*************************************************/

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

            Bitmap iconReport = BitmapFactory.decodeResource(context.getResources(), R.drawable.detect);
            mapbox.addImage("my-marker-image-report", iconReport);

            SymbolLayer markersReport = new SymbolLayer("marker-layer-report", "marker-source-report")
                    .withProperties(PropertyFactory.iconImage("my-marker-image-report"));
            mapbox.addLayer(markersReport);

            Bitmap iconReportRepaired = BitmapFactory.decodeResource(context.getResources(), R.drawable.gray_detect);
            mapbox.addImage("my-marker-image-report-repaired", iconReportRepaired);

            SymbolLayer markersReportRepaired = new SymbolLayer("marker-layer-report-repaired", "marker-source-report-repaired")
                    .withProperties(PropertyFactory.iconImage("my-marker-image-report-repaired"));
            mapbox.addLayer(markersReportRepaired);

            Bitmap iconBumpRepaired = BitmapFactory.decodeResource(context.getResources(), R.drawable.gray_marker);
            mapbox.addImage("my-marker-image-bump-repaired", iconBumpRepaired);

            SymbolLayer markersBumpRepaired = new SymbolLayer("marker-layer-bump-repaired", "marker-source-bump-repaired")
                    .withProperties(PropertyFactory.iconImage("my-marker-image-bump-repaired"));
            mapbox.addLayer(markersBumpRepaired);

           /*************************************************/
            FeatureCollection emptySourceAuto = FeatureCollection.fromFeatures(new Feature[]{});
            Source selectedMarkerSourceAuto = new GeoJsonSource("selected-marker-auto", emptySourceAuto);
            mapbox.addSource(selectedMarkerSourceAuto);

            FeatureCollection emptySourceManual = FeatureCollection.fromFeatures(new Feature[]{});
            Source selectedMarkerSourceManual = new GeoJsonSource("selected-marker-manual", emptySourceManual);
            mapbox.addSource(selectedMarkerSourceManual);

            FeatureCollection emptySourceReport = FeatureCollection.fromFeatures(new Feature[]{});
            Source selectedMarkerSourceReport = new GeoJsonSource("selected-marker-report", emptySourceReport);
            mapbox.addSource(selectedMarkerSourceReport);

            FeatureCollection emptySourceReportRepaired = FeatureCollection.fromFeatures(new Feature[]{});
            Source selectedMarkerSourceReportRepaired = new GeoJsonSource("selected-marker-report-repaired", emptySourceReportRepaired);
            mapbox.addSource(selectedMarkerSourceReportRepaired);

            FeatureCollection emptySourceBumpRepaired = FeatureCollection.fromFeatures(new Feature[]{});
            Source selectedMarkerSourceBumpRepaired = new GeoJsonSource("selected-marker-bump-repaired", emptySourceBumpRepaired);
            mapbox.addSource(selectedMarkerSourceBumpRepaired);
            /*************************************************/
            SymbolLayer selectedMarkerAuto = new SymbolLayer("selected-marker-layer-auto", "selected-marker-auto")
                    .withProperties(PropertyFactory.iconImage("my-marker-image-auto"));
            mapbox.addLayer(selectedMarkerAuto);

            SymbolLayer selectedMarkerManual = new SymbolLayer("selected-marker-layer-manual", "selected-marker-manual")
                    .withProperties(PropertyFactory.iconImage("my-marker-image-manual"));
            mapbox.addLayer(selectedMarkerManual);

            SymbolLayer selectedMarkerReport = new SymbolLayer("selected-marker-layer-report", "selected-marker-report")
                    .withProperties(PropertyFactory.iconImage("my-marker-image-report"));
            mapbox.addLayer(selectedMarkerReport);

            SymbolLayer selectedMarkerReportRepaired = new SymbolLayer("selected-marker-layer-report-repaired", "selected-marker-report-repaired")
                    .withProperties(PropertyFactory.iconImage("my-marker-image-report-repaired"));
            mapbox.addLayer(selectedMarkerReportRepaired);

            SymbolLayer selectedMarkerBumpRepaired = new SymbolLayer("selected-marker-layer-bump-repaired", "selected-marker-bump-repaired")
                    .withProperties(PropertyFactory.iconImage("my-marker-image-bump-repaired"));
            mapbox.addLayer(selectedMarkerBumpRepaired);

        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
            e.getMessage();
        }


    }

    public void notSendBumps(ArrayList<HashMap<Location, Float>> bumps, ArrayList<Integer> bumpsManual, int autoSeq, int manulSeq, int reportSequence, ArrayList<Integer> detectType, ArrayList<String> detectText) {
        float rating = 1;
        int i = 0;
        if (bumps.size() > 0) {
            for (HashMap<android.location.Location, Float> bump : bumps) {
                Iterator it = bump.entrySet().iterator();
                while (it.hasNext()) {
                    HashMap.Entry pair = (HashMap.Entry) it.next();
                    android.location.Location loc = (android.location.Location) pair.getKey();
                    if (detectType.get(i) == 0 ) {
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
                                            context.getResources().getString(R.string.modif) + " " + getDate(loc.getTime(), "dd/MM/yyyy"));
                                    autoFeature.addStringProperty("lat", String.valueOf(loc.getLatitude()));
                                    autoFeature.addStringProperty("ltn", String.valueOf(loc.getLongitude()));
                                    autoFeature.addStringProperty("type", String.valueOf(detectType.get(i)));
                                    autoFeature.addStringProperty("text", String.valueOf( detectText.get(i)));
                                    autoSeq++;
                                }

                            } else if (manualMarkerCoordinates.size() == manulSeq) {
                                manualMarkerCoordinates.add(Feature.fromGeometry(
                                        Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(loc.getLongitude(), loc.getLatitude()))) // Boston Common Park
                                );
                                Feature manualFeature = manualMarkerCoordinates.get(manulSeq);
                                manualFeature.addStringProperty("property", context.getResources().getString(R.string.manual_bump) + "\n" +
                                        context.getResources().getString(R.string.number_bump) + "1\n" +
                                        context.getResources().getString(R.string.modif) + " " + getDate(loc.getTime(), "dd/MM/yyyy"));
                                manualFeature.addStringProperty("lat", String.valueOf(loc.getLatitude()));
                                manualFeature.addStringProperty("ltn", String.valueOf(loc.getLongitude()));
                                manualFeature.addStringProperty("type", String.valueOf(detectType.get(i)));
                                manualFeature.addStringProperty("text", String.valueOf( detectText.get(i)));
                                manulSeq++;
                            }
                        }
                    }else {
                        if (getSignedDiffInDays(getGMTDate(loc.getTime()),new Date()) <=3) {
                        String select_iteam = detectText.get(i);
                            switch (detectType.get(i)) {
                                case 1:
                                    select_iteam = context.getResources().getString(R.string.type_problem_basket);
                                    break;
                                case 2:
                                    select_iteam = context.getResources().getString(R.string.type_problem_canstock);
                                    break;
                            }
                            reportMarkerCoordinates.add(Feature.fromGeometry(
                                    Point.fromCoordinates(com.mapbox.services.commons.models.Position.fromCoordinates(loc.getLongitude(), loc.getLatitude())))
                            );
                            Feature reportFeature = reportMarkerCoordinates.get(reportSequence);

                            reportFeature.addStringProperty("property", select_iteam + "\n" +
                                    context.getResources().getString(R.string.number_bump) + "1\n" +
                                    context.getResources().getString(R.string.modif) + " " + getDate(loc.getTime(), "dd/MM/yyyy"));
                            reportFeature.addStringProperty("lat", String.valueOf(loc.getLatitude()));
                            reportFeature.addStringProperty("ltn", String.valueOf(loc.getLongitude()));
                            reportFeature.addStringProperty("type", String.valueOf(detectType.get(i)));
                            reportFeature.addStringProperty("text", String.valueOf( detectText.get(i)));
                            reportSequence++;
                        }

                    }
                    i++;
                }
            }
        }


    }
    private static long getDateToLong(Date date) {
        return Date.UTC(date.getYear(), date.getMonth(), date.getDate(), 0, 0, 0);
    }


    public static int getSignedDiffInDays(Date beginDate, Date endDate) {
        long beginMS = getDateToLong(beginDate);
        long endMS = getDateToLong(endDate);
        long diff = (endMS - beginMS) / (MILLISECS_PER_DAY);
        return (int)diff;
    }
    private Date getGMTDate(long date) {
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat(
                "yyyy-MMM-dd HH:mm:ss");
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        SimpleDateFormat dateFormatLocal = new SimpleDateFormat(
                "yyyy-MMM-dd HH:mm:ss");

        Date temp = new Date(date);

        try {
            return dateFormatLocal.parse(dateFormatGmt.format(temp));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return temp;
    }

    public void setClear(Marker marker) {
        this.marker = marker;
    }
}
