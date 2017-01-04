package navigationapp.main_application;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.location.Address;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.monikas.navigationapp.R;
import com.google.android.gms.maps.model.LatLng;
import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.*;
import com.mapbox.services.commons.models.Position;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static navigationapp.main_application.FragmentActivity.flagDownload;
import static navigationapp.main_application.FragmentActivity.lockAdd;
import static navigationapp.main_application.FragmentActivity.lockZoznam;
import static navigationapp.main_application.FragmentActivity.lockZoznamDB;
import static navigationapp.main_application.FragmentActivity.setOnPosition;
import static navigationapp.main_application.FragmentActivity.selectedName;
import static navigationapp.main_application.FragmentActivity.updatesLock;

public class MainActivity extends ActionBarActivity  implements View.OnClickListener {
    private Context context;
    private AtomicBoolean threadLock = new AtomicBoolean(false);
    private final float ALL_BUMPS = 1.0f;
    private final float MEDIUM_BUMPS = 1.5f;
    private final float LARGE_BUMPS = 2.5f;
    public static int ZOOM_LEVEL = 16;
    private FragmentActivity fragmentActivity =null;
    public static final String FRAGMENTACTIVITY_TAG = "blankFragment";
    private static boolean activityVisible=true;
    public static final String PREF_FILE_NAME = "Settings";
    private Float intensity = null;
    LinearLayout confirm;
    Button  save_button, delete_button,downloand_button,back_button;
    public  MapView mapView = null;
    public static LinearLayout mapConfirm;
    public static Button navig_on,add_button;
    public static MapboxMap mapbox;
    public static MapboxAccountManager manager;
    private boolean markerSelected = false;
    private boolean markerSelected1 = false;
    private Marker featureMarker;
    boolean allow_click= false;
    com.mapbox.mapboxsdk.geometry.LatLng  convert_location  =null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        manager = MapboxAccountManager.start(this,"pk.eyJ1IjoiYWRhbXN0cmFza3kiLCJhIjoiY2l1aDYwYzZvMDAydTJ5b2dwNXoyNHJjeCJ9.XsDrnj02GHMwBExP5Va35w");
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapboxMarkerMapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                mapbox = mapboxMap;

                if (setOnPosition)
                    mapbox.setMyLocationEnabled(true);





                mapbox.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull com.mapbox.mapboxsdk.geometry.LatLng point) {


                        if (allow_click) {
                            if (featureMarker != null) {
                                ValueAnimator markerAnimator = ObjectAnimator.ofObject(featureMarker, "position",
                                        new LatLngEvaluator(), featureMarker.getPosition(), point);
                                markerAnimator.setDuration(2000);
                                markerAnimator.start();
                                convert_location = point;
                            }
                           else {
                                featureMarker = mapboxMap.addMarker(new MarkerViewOptions()
                                        .position(point)
                                        .title("Properties:")
                                );
                            }


                      }












                        ////////////////////////////////////////////////////////////////
                        final SymbolLayer marker = (SymbolLayer) mapbox.getLayer("selected-marker-layer-auto");
                        final SymbolLayer marker1 = (SymbolLayer) mapbox.getLayer("selected-marker-layer-manual");

                        final PointF pixel = mapbox.getProjection().toScreenLocation(point);
                        List<Feature> features = mapbox.queryRenderedFeatures(pixel, "marker-layer-auto");
                        List<Feature> selectedFeature = mapbox.queryRenderedFeatures(pixel, "selected-marker-layer-auto");
                        List<Feature> features1 = mapbox.queryRenderedFeatures(pixel, "marker-layer-manual");
                        List<Feature> selectedFeature1 = mapbox.queryRenderedFeatures(pixel, "selected-marker-layer-manual");

                        if (selectedFeature.size() > 0 && markerSelected) {
                            return;
                        }
                        if (selectedFeature1.size() > 0 && markerSelected1) {
                            return;
                        }

                        if (features.isEmpty() || features1.isEmpty()) {
                            if (markerSelected) {
                                deselectMarker(marker, 0);
                                return;
                            }
                            if (markerSelected1) {
                                deselectMarker(marker1, 1);

                            }


                        }


                        if (features.size() > 0) {
                            FeatureCollection featureCollection = FeatureCollection.fromFeatures(
                                    new Feature[]{Feature.fromGeometry(features.get(0).getGeometry())});
                            GeoJsonSource source = mapbox.getSourceAs("selected-marker-auto");
                            if (source != null) {
                                source.setGeoJson(featureCollection);
                            }
                        }
                        if (features1.size() > 0) {
                            FeatureCollection featureCollection1 = FeatureCollection.fromFeatures(
                                    new Feature[]{Feature.fromGeometry(features1.get(0).getGeometry())});
                            GeoJsonSource source1 = mapbox.getSourceAs("selected-marker-manual");
                            if (source1 != null) {
                                source1.setGeoJson(featureCollection1);
                            }

                        }
                        if (markerSelected) {
                            deselectMarker(marker, 0);
                        }
                        if (markerSelected1) {
                            deselectMarker(marker1, 1);
                        }
                        if (features.size() > 0) {
                            Toast.makeText(getApplication(), "aaaaaaaaa", Toast.LENGTH_LONG).show();
                            if (features.get(0).getStringProperty("aaaaaa")!=null)
                            Toast.makeText(getApplication(), features.get(0).getStringProperty("aaaaaa"), Toast.LENGTH_LONG).show();

                            selectMarker(marker,  0);
                        }
                        if (features1.size() > 0) {
                             Toast.makeText(getApplication(), "bbbbbbb", Toast.LENGTH_LONG).show();
                           if (features1.get(0).getStringProperty("aaaaaa")!=null)
                            Toast.makeText(getApplication(), features1.get(0).getStringProperty("aaaaaa"), Toast.LENGTH_LONG).show();

                            selectMarker(marker1, 1);
                        }

                    }
                });



            }
        });


        final EditText searchBar = (EditText) findViewById(R.id.location);
        add_button = (Button) findViewById(R.id.add_button);
        save_button = (Button) findViewById(R.id.save_btn);
        delete_button = (Button) findViewById(R.id.delete_btn);
        downloand_button = (Button) findViewById(R.id.saveMap_btn);
        back_button = (Button) findViewById(R.id.backMap_btn);
        navig_on = (Button) findViewById(R.id.navig_on);
        mapConfirm = (LinearLayout) findViewById(R.id.mapConfirm);
        confirm = (LinearLayout) findViewById(R.id.confirm);

        navig_on.setVisibility(View.INVISIBLE);
        confirm.setVisibility(View.INVISIBLE);
        mapConfirm.setVisibility(View.INVISIBLE);
        confirm.setOnClickListener(this);
        save_button.setOnClickListener(this);
        delete_button.setOnClickListener(this);
        add_button.setOnClickListener(this);
        downloand_button.setOnClickListener(this);
        back_button.setOnClickListener(this);
        navig_on.setOnClickListener(this);


        isEneableScreen();





        searchBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == searchBar.getId()) {
                    searchBar.setCursorVisible(true);
                    searchBar.setText("");
                }
            }
        });
        context = this;

        registerReceiver(netReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        FragmentManager fragmentManager = getFragmentManager();
        fragmentActivity = (FragmentActivity) fragmentManager.findFragmentByTag(FRAGMENTACTIVITY_TAG);

        if (fragmentActivity == null) {
            fragmentActivity = new FragmentActivity();
            fragmentManager.beginTransaction()
                    .add(fragmentActivity, FRAGMENTACTIVITY_TAG)
                    .commit();
        }
    }

    private  class LatLngEvaluator implements TypeEvaluator<com.mapbox.mapboxsdk.geometry.LatLng> {
        // Method is used to interpolate the marker animation.
        private com.mapbox.mapboxsdk.geometry.LatLng  psoition = new com.mapbox.mapboxsdk.geometry.LatLng();

        @Override
        public com.mapbox.mapboxsdk.geometry.LatLng evaluate(float fraction, com.mapbox.mapboxsdk.geometry.LatLng startValue, com.mapbox.mapboxsdk.geometry.LatLng endValue) {
            psoition.setLatitude(startValue.getLatitude()
                    + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
            psoition.setLongitude(startValue.getLongitude()
                    + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
            return psoition;
        }
    }



    private void selectMarker(final SymbolLayer marker, int i) {
        ValueAnimator markerAnimator = new ValueAnimator();
        markerAnimator.setObjectValues(1f, 2f);
        markerAnimator.setDuration(300);
        markerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                marker.setProperties(
                        PropertyFactory.iconSize((float) animator.getAnimatedValue())

                );
            }
        });
        markerAnimator.start();
        if (i==0)
            markerSelected = true;
        else
            markerSelected1 = true;
    }

    private void deselectMarker(final SymbolLayer marker, int i) {
        ValueAnimator markerAnimator = new ValueAnimator();
        markerAnimator.setObjectValues(2f, 1f);
        markerAnimator.setDuration(300);
        markerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                marker.setProperties(
                        PropertyFactory.iconSize((float) animator.getAnimatedValue())
                );
            }
        });
        markerAnimator.start();
        if (i== 0)
            markerSelected = false;
        else
            markerSelected1 = false;
    }

    public void isEneableScreen() {
        Log.d("rrrrrr","adasdasdasdasdasdasdasdsadsad");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean imgSett = prefs.getBoolean("screen", Boolean.parseBoolean(null));

        Log.d("rrrrrr", String.valueOf(imgSett));
        if (imgSett) {
            Log.d("aasc","qqqqqqqqqqqqqqqqqqq");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        }
        else {
            Log.d("aasc","tttttttttttttttttttt");
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }


    public void save(boolean save_click) {
        if (!save_click && featureMarker!=null )
            featureMarker.remove();
         featureMarker=null;
     }

    private BroadcastReceiver netReceiver  = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().matches("android.net.conn.CONNECTIVITY_CHANGE")) {
                ConnectivityManager connectivityManager
                        = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

                boolean NisConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
                if (NisConnected) {
                    if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        Log.d("fdsgszdf", "TYPE_WIFI");
                        manager.setConnected(true);
                    }
                    if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                        Log.d("fdsgszdf", "TYPE_MOBILE");
                        if (isEneableOnlyWifiMap())
                            manager.setConnected(false);
                        else
                            manager.setConnected(true);
                    }
                }
            }
        }
    };

    public boolean isEneableOnlyWifiMap() {
        SharedPreferences preferences = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        Boolean map = preferences.getBoolean("map", Boolean.parseBoolean(null));
        if ((map))
            return true;
        else
            return false;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_button:
                if (!fragmentActivity.checkGPSEnable()) {
                    Toast.makeText(this, "Turn on your GPS", Toast.LENGTH_LONG).show();
                    break;
                }
                if (mapbox==null) {
                    Toast.makeText(this, "Map is not loaded", Toast.LENGTH_LONG).show();
                    break;
                }
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this);
                builderSingle.setTitle("Select type of bump");
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                        MainActivity.this,android.R.layout.select_dialog_singlechoice);
                arrayAdapter.add("Large");
                arrayAdapter.add("Medium");
                arrayAdapter.add("Normal");

                builderSingle.setNegativeButton(
                        "Cancel",
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
                            public void onClick(DialogInterface dialog, int select) {
                                // vybrana intenzita noveho vytlku
                                if (select== 0) {
                                    intensity = 10.f;
                                }
                                else if (select== 1) {
                                    intensity = 6.f;
                                }
                                else
                                    intensity =0.f;
                                // spustenie listenera na mapu
                                allow_click=true;
                                confirm.setVisibility(View.VISIBLE);
                                fragmentActivity.setClear(false);
                                add_button.setVisibility(View.INVISIBLE);

                            }
                        });
                builderSingle.show();
                break;

            case R.id.save_btn:

                add_button.setVisibility(View.VISIBLE);
                confirm.setVisibility(View.INVISIBLE);
                fragmentActivity.setClear(true);
                // vrati polohu  kde som stlačil na mape
                allow_click=false;
                save(true);

                //vytvorenie markera
                //fragmentActivity.gps.addBumpToMap (convert_location,1,1);
                if (convert_location != null) {

                    final double ll = intensity;
                    final Location location = new Location("new");
                    location.setLatitude(round(convert_location.getLatitude(),7));
                    location.setLongitude(round(convert_location.getLongitude(),7));
                    location.setTime(new Date().getTime());

                    new Thread() {
                        public void run() {
                            Looper.prepare();

                            while(true) {

                                if (!threadLock.get() ) {

                                    if (lockAdd.tryLock())
                                    {
                                        // Got the lock
                                        try
                                        {
                                            if (lockZoznam.tryLock())
                                            {
                                                // Got the lock
                                                try
                                                {
                                                    Log.d("TREEEE","vlozil do zoznamu  ");
                                                    threadLock.getAndSet(true);
                                                    fragmentActivity.accelerometer.addPossibleBumps(location, (float) round(intensity,6));
                                                    fragmentActivity.accelerometer.addBumpsManual(1);
                                                    if (updatesLock.tryLock())
                                                    {
                                                        // Got the lock
                                                        try
                                                        {
                                                            if (lockZoznamDB.tryLock())
                                                            {
                                                                // Got the lock
                                                                try
                                                                {
                                                                    DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(context);
                                                                    SQLiteDatabase database = databaseHelper.getReadableDatabase();
                                                                    Log.d("TREEEE","vlozil do db ");
                                                                    database.beginTransaction();
                                                                    ContentValues contentValues = new ContentValues();
                                                                    contentValues.put(Provider.new_bumps.LATITUDE, location.getLatitude());
                                                                    contentValues.put(Provider.new_bumps.LONGTITUDE, location.getLongitude());
                                                                    contentValues.put(Provider.new_bumps.MANUAL, 1);
                                                                    contentValues.put(Provider.new_bumps.INTENSITY, (float) round(intensity,6));
                                                                    database.insert(Provider.new_bumps.TABLE_NAME_NEW_BUMPS, null, contentValues);
                                                                    database.setTransactionSuccessful();
                                                                    database.endTransaction();
                                                                    database.close();
                                                                }
                                                                finally
                                                                {
                                                                    // Make sure to unlock so that we don't cause a deadlock
                                                                    lockZoznamDB.unlock();
                                                                }
                                                            }
                                                        }
                                                        finally
                                                        {
                                                            // Make sure to unlock so that we don't cause a deadlock
                                                            updatesLock.unlock();
                                                        }
                                                    }




                                                    threadLock.getAndSet(false);
                                                    Log.d("TREEEE","casovy lock koniec");

                                                }
                                                finally
                                                {
                                                    // Make sure to unlock so that we don't cause a deadlock
                                                    lockZoznam.unlock();
                                                    break;
                                                }
                                            }
                                        }
                                        finally
                                        {
                                            // Make sure to unlock so that we don't cause a deadlock
                                            lockAdd.unlock();
                                        }
                                    }
                                }






                                Log.d("TREEEE","casovy lock");
                                Log.d("TREEEE", String.valueOf(location.getLatitude()));
                                Log.d("TREEEE", String.valueOf(location.getLongitude()));
                                Log.d("TREEEE", String.valueOf(ll));
                                try {
                                    Thread.sleep(20); // sleep for 50 ms so that main UI thread can handle user actions in the meantime
                                } catch (InterruptedException e) {
                                    // NOP (no operation)
                                }
                            }

                            Looper.loop();
                        }
                    }.start();
                }
                break;
            case R.id.delete_btn:

                add_button.setVisibility(View.VISIBLE);
                confirm.setVisibility(View.INVISIBLE);

                // disable listener na klik
                allow_click=false;
                save(false);
                fragmentActivity.setClear(true);
                break;
            case R.id.backMap_btn:
                setOnPosition =true;
                SetUpCamera();
                // spusti sa alert dialog na opetovné hladanie mapy
                fragmentActivity.alertSelectRegion(selectedName,1);
                mapConfirm.setVisibility(View.INVISIBLE);
                add_button.setVisibility(View.VISIBLE);

                break;
            case R.id.saveMap_btn:
                fragmentActivity.downloadRegion(selectedName, 0);
                mapConfirm.setVisibility(View.INVISIBLE);
                add_button.setVisibility(View.VISIBLE);
                setOnPosition =true;
                if (fragmentActivity.gps!=null)
                    fragmentActivity.gps.getOnPosition();

                break;
            case R.id.navig_on:
                setOnPosition =true;
                if (fragmentActivity.gps!=null)
                    fragmentActivity.gps.getOnPosition();
                add_button.setVisibility(View.VISIBLE);
                navig_on.setVisibility(View.INVISIBLE);
                break;
        }
    }



    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }

    public void onClick_Search(View v) throws IOException {
        Address address = null;
        EditText text = (EditText) findViewById(R.id.location);
        Toast.makeText(this, "Finding location...", Toast.LENGTH_LONG).show();
        text.setCursorVisible(false);
        hideKeyboard(v);
        String location = text.getText().toString();
        if (fragmentActivity.isNetworkAvailable()) {
            try {
                address = Route.findLocality(location, this);
                if (address == null) {
                    if (isEneableShowText())
                        Toast.makeText(this, "Unable to find location, wrong name!", Toast.LENGTH_LONG).show();
                }
                else {
                    final LatLng to_position = new LatLng(address.getLatitude(),address.getLongitude());

                    new Thread() {
                        public void run() {

                            fragmentActivity.detection.stop_collison_navigate();
                            fragmentActivity.detection.bumps_on_position(fragmentActivity, to_position);
                        }
                    }.start();
                }
            }
            catch (Exception e) {
                if (isEneableShowText())
                    Toast.makeText(this, "Unable to find location!", Toast.LENGTH_LONG).show();
            }
        }
        else {
            if (isEneableShowText())
                Toast.makeText(this, "Unable to find location! Please, connect to network.", Toast.LENGTH_LONG).show();
        }
    }

    public void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {


            case R.id.position:
                if (!fragmentActivity.checkGPSEnable()) {
                    Toast.makeText(this, "Turn on your GPS", Toast.LENGTH_LONG).show();
                    return true;
                }
                if (fragmentActivity.gps!=null)
                    fragmentActivity.gps.getOnPosition();
                return true;

            case R.id.layer:
                if (!fragmentActivity.isNetworkAvailable() || mapbox==null) {
                    Toast.makeText(this, "Please connect to internet to change map", Toast.LENGTH_LONG).show();
                    return true;
                }


                AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this);
                builderSingle.setTitle("Maps");
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                        MainActivity.this,android.R.layout.select_dialog_singlechoice);
                arrayAdapter.add("Street");
                arrayAdapter.add("Satellite");
                arrayAdapter.add("Outdoors");




                builderSingle.setNegativeButton(
                        "Cancel",
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
                            public void onClick(DialogInterface dialog, int select) {
                                switch (select) {
                                    case 0:
                                        mapbox.setStyleUrl("mapbox://styles/mapbox/light-v9");
                                        break;

                                    case 1:
                                        mapbox.setStyleUrl("mapbox://styles/mapbox/satellite-v9");
                                        break;
                                    case 2:
                                        mapbox.setStyleUrl("mapbox://styles/mapbox/outdoors-v9");
                                        break;
                                }
                            }
                        });
                builderSingle.show();
                return true;

            case R.id.filter:
                if (!fragmentActivity.checkGPSEnable()) {
                    Toast.makeText(this, "Turn on your GPS", Toast.LENGTH_LONG).show();
                    return true;
                }
                AlertDialog.Builder builderSingles = new AlertDialog.Builder(MainActivity.this);
                builderSingles.setTitle("Show bumps");
                final ArrayAdapter<String> arrayAdapters = new ArrayAdapter<String>(
                        MainActivity.this,android.R.layout.select_dialog_singlechoice);
                arrayAdapters.add("All bumps");
                arrayAdapters.add("Medium & large bumps");
                arrayAdapters.add("Large bumps");




                builderSingles.setNegativeButton(
                        "Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                builderSingles.setAdapter(
                        arrayAdapters,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int select) {
                                switch (select) {
                                    case 0:
                                        new Thread() {
                                            public void run() {
                                                fragmentActivity.level = ALL_BUMPS;
                                                LatLng allBumps =  fragmentActivity.gps.getCurrentLatLng();
                                                fragmentActivity.getAllBumps(allBumps.latitude,allBumps.longitude);

                                            }
                                        }.start();
                                        break;

                                    case 1:
                                        new Thread() {
                                            public void run() {
                                                fragmentActivity.level = MEDIUM_BUMPS;
                                                LatLng mediumBumps =  fragmentActivity.gps.getCurrentLatLng();
                                                fragmentActivity.getAllBumps(mediumBumps.latitude,mediumBumps.longitude);

                                            }
                                        }.start();
                                        break;
                                    case 2:
                                        new Thread() {
                                            public void run() {
                                                fragmentActivity.level = LARGE_BUMPS;
                                                LatLng largeBumps = fragmentActivity.gps.getCurrentLatLng();
                                                fragmentActivity.getAllBumps(largeBumps.latitude, largeBumps.longitude);
                                            }
                                        }.start();
                                        break;
                                }
                            }
                        });
                builderSingles.show();
                return true;

            case R.id.clear_map:

                if(confirm.isShown()){
                    Toast.makeText(context,"Vyber najskôr výtlk",Toast.LENGTH_SHORT).show();
                }else {
                    if (mapbox!=null)
                        mapbox.clear();
                }
                return true;

            case R.id.calibrate:
                if ( fragmentActivity.accelerometer!=null) {
                    fragmentActivity.accelerometer.calibrate();
                    if (isEneableShowText())
                        Toast.makeText(context, "Your phone was calibrated.", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(context, "Turn on your GPS before calibrate", Toast.LENGTH_SHORT).show();
                }
                return true;

            case R.id.navigation:
                EditText text = (EditText) findViewById(R.id.location);
                text.setText("Navigate to...");
                if ( fragmentActivity.gps!=null) {
                    new Thread() {
                        public void run() {
                            fragmentActivity.gps.remove_draw_road();
                            if (fragmentActivity.gps.getCurrentLatLng() != null) {
                                LatLng bumps = fragmentActivity.gps.getCurrentLatLng();
                                fragmentActivity.getAllBumps(bumps.latitude, bumps.longitude);
                            }
                            fragmentActivity.detection.setRoad(false);
                            fragmentActivity.detection.stop_collison_navigate();

                        }
                    }.start();
                }
                return true;


            case R.id.action_settings:

                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.download:
                if (!fragmentActivity.checkGPSEnable()) {
                    Toast.makeText(this, "Turn on your GPS", Toast.LENGTH_LONG).show();
                    return true;
                }
                if ( mapbox==null) {
                    Toast.makeText(this, "Map is not loaded", Toast.LENGTH_LONG).show();
                    return true;
                }
                save(false);
               // fragmentActivity.gps.setUpMap(false);
                setOnPosition =true;
                confirm.setVisibility(View.INVISIBLE);
                fragmentActivity.setClear(true);
                add_button.setVisibility(View.VISIBLE);
                navig_on.setVisibility(View.INVISIBLE);
                if ( flagDownload)
                    Toast.makeText(this, "Momentálne sťahujete,nemožte 2 naraz", Toast.LENGTH_LONG).show();
                else
                    fragmentActivity.downloadRegionDialog();
                return true;

            case R.id.list:
                save(false);

                if ( mapbox==null) {
                    Toast.makeText(this, "Map is not loaded", Toast.LENGTH_LONG).show();
                    return true;
                }

                add_button.setVisibility(View.VISIBLE);
                mapConfirm.setVisibility(View.INVISIBLE);
                confirm.setVisibility(View.INVISIBLE);
                fragmentActivity.setClear(true);
                navig_on.setVisibility(View.INVISIBLE);
                if (flagDownload)
                    Toast.makeText(this, "Momentálne sťahujete,nemožte pristupiť k stiahnutým mapám", Toast.LENGTH_LONG).show();
                else
                    fragmentActivity.downloadedRegionList();
                return true;

            case R.id.exit:

                if (fragmentActivity.accelerometer!=null) {
                    while (true) {
                        if (updatesLock.tryLock())
                        {
                            // Got the lock
                            try
                            {
                                ArrayList<HashMap<Location, Float>> list = fragmentActivity.accelerometer.getPossibleBumps();
                                ArrayList<Integer> bumpsManual = fragmentActivity.accelerometer.getBumpsManual();
                                DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(this);
                                SQLiteDatabase sb = databaseHelper.getWritableDatabase();
                                sb.beginTransaction();
                                int i = 0;
                                for (HashMap<Location, Float> bump : list) {
                                    Iterator it = bump.entrySet().iterator();
                                    while (it.hasNext()) {
                                        HashMap.Entry pair = (HashMap.Entry) it.next();
                                        Location loc = (Location) pair.getKey();
                                        float data = (float) pair.getValue();
                                        String sql = "SELECT intensity FROM new_bumps WHERE ROUND(latitude,7)==ROUND(" + loc.getLatitude() + ",7)  and ROUND(longitude,7)==ROUND(" + loc.getLongitude() + ",7) "
                                                + " and  ROUND(intensity,6)==ROUND(" + data + ",6)  and manual=" + bumpsManual.get(i);

                                        BigDecimal bd = new BigDecimal(Float.toString(data));
                                        bd = bd.setScale(6, BigDecimal.ROUND_HALF_UP);
                                        Cursor cursor = sb.rawQuery(sql, null);
                                        if (cursor.getCount() == 0) {
                                            Log.d("MainActivity", "vkladam ");
                                            ContentValues contentValues = new ContentValues();
                                            contentValues.put(Provider.new_bumps.LATITUDE, loc.getLatitude());
                                            contentValues.put(Provider.new_bumps.LONGTITUDE, loc.getLongitude());
                                            contentValues.put(Provider.new_bumps.MANUAL, bumpsManual.get(i));
                                            contentValues.put(Provider.new_bumps.INTENSITY, String.valueOf(bd));
                                            sb.insert(Provider.new_bumps.TABLE_NAME_NEW_BUMPS, null, contentValues);
                                        }
                                    }
                                    i++;
                                }
                                sb.setTransactionSuccessful();
                                sb.endTransaction();

                            }
                            finally
                            {
                                // Make sure to unlock so that we don't cause a deadlock
                                updatesLock.unlock();
                                break;
                            }
                        } else {
                            Log.d("getAllBumps", "getAllBumps thread lock iiiiiiiiiii");
                            try {
                                Thread.sleep(20);
                            } catch (InterruptedException e) {
                            }
                        }


                        Log.d("getAllBumps", "getAllBumps thread lock iiiiiiiiiii");
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                        }
                    }

                    fragmentActivity.stop_servise();
                }
                onDestroy();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }




    @Override
    protected void onDestroy() {
        fragmentActivity.downloadNotification(false);
        super.onDestroy();
        mapView.onDestroy();
        finish();
      //  android.os.Process.killProcess(android.os.Process.myPid());

    }

    public void SetUpCamera(){
        if (fragmentActivity.gps !=null && fragmentActivity.gps.getmCurrentLocation()!= null) {
            LatLng myPosition = new LatLng(fragmentActivity.gps.getmCurrentLocation().getLatitude(), fragmentActivity.gps.getmCurrentLocation().getLongitude());
            if (myPosition!=null && setOnPosition &&  MainActivity.isActivityVisible()) {
                try {
                    mapbox.easeCamera(com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLng(new com.mapbox.mapboxsdk.geometry.LatLng(myPosition.latitude, myPosition.longitude)));
                } catch  (NullPointerException e) {
                }
            }
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    protected void onResume() {
        Log.d("rrrrrr","onResume run ");
        isEneableScreen();
        super.onResume();
        SetUpCamera();
        mapView.onResume();
        MainActivity.activityResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        MainActivity.activityPaused();
    }

    public boolean isEneableShowText() {
        SharedPreferences preferences = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        Boolean alarm = preferences.getBoolean("alarm", Boolean.parseBoolean(null));
        if ((alarm) || (!alarm && MainActivity.isActivityVisible())) {
            return true;
        }
        else
            return false;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }



}