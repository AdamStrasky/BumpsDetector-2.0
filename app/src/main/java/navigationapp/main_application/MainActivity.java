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
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
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

import navigationapp.error.ExceptionHandler;
import navigationapp.R;
import com.google.android.gms.maps.model.LatLng;
import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static navigationapp.main_application.FragmentActivity.lockAdd;
import static navigationapp.main_application.FragmentActivity.lockZoznam;

import static navigationapp.main_application.FragmentActivity.updatesLock;
import static navigationapp.main_application.MapManager.selectedName;
import static navigationapp.main_application.MapManager.setOnPosition;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {
    private Context context;
    private AtomicBoolean threadLock = new AtomicBoolean(false);
    private final float ALL_BUMPS = 1.0f;
    private final float MEDIUM_BUMPS = 1.5f;
    private final float LARGE_BUMPS = 2.5f;
    public static int ZOOM_LEVEL = 16;
    private FragmentActivity fragmentActivity =null;
    public  final String FRAGMENTACTIVITY_TAG = "blankFragment";
    public  final String TAG = "MainActivity";
    private static boolean activityVisible=true;
    public static final String PREF_FILE_NAME = "Settings";
    private Float intensity = null;
    LinearLayout confirm = null;
    Button  save_button, delete_button,downloand_button,back_button;
    public  static MapView mapView = null;
    public static LinearLayout mapConfirm;
    public static Button navig_on,add_button;
    public static MapboxMap mapbox = null;
    public static MapboxAccountManager manager;
    private boolean markerSelectedAuto = false;
    private boolean markerSelectedManual = false;
    private Marker featureMarker =  null ;
    boolean allow_click= false;
    com.mapbox.mapboxsdk.geometry.LatLng  convert_location  =null;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout = null;
    private MapManager mapManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        manager = MapboxAccountManager.start(this,"pk.eyJ1IjoiYWRhbXN0cmFza3kiLCJhIjoiY2l1aDYwYzZvMDAydTJ5b2dwNXoyNHJjeCJ9.XsDrnj02GHMwBExP5Va35w");
        Language.setLanguage(MainActivity.this,getLanguage());
        setContentView(R.layout.activity_main);
        context = this;
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
                        if (allow_click) {  // ak som klikol na plus, možem klikať
                            if (featureMarker != null) { // ak existuje marker, posuvam ho po mape
                                ValueAnimator markerAnimator = ObjectAnimator.ofObject(featureMarker, "position",
                                        new LatLngEvaluator(), featureMarker.getPosition(), point);
                                markerAnimator.setDuration(2000);
                                markerAnimator.start();
                                convert_location = point;
                                Log.d(TAG," point click "+ convert_location.getLongitude() +" " +convert_location.getLatitude() );
                            }
                            else {  // neexistuje , tak ho vytváram
                                Log.d(TAG," Vytvaram marker ");
                                IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
                                Drawable iconDrawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.purple_marker);
                                com.mapbox.mapboxsdk.annotations.Icon icons = iconFactory.fromDrawable(iconDrawable);
                                featureMarker = mapboxMap.addMarker(new MarkerViewOptions()
                                        .position(point)
                                        .title(getApplication().getResources().getString(R.string.add_marker))
                                        .icon(icons)
                                );
                                convert_location = point;
                                Log.d(TAG," point click "+ convert_location.getLongitude() +" " +convert_location.getLatitude() );
                            }
                        }

                        ////////////////////////////////////////////////////////////////
                        final SymbolLayer markerAuto = (SymbolLayer) mapbox.getLayer("selected-marker-layer-auto");
                        final SymbolLayer markerManual = (SymbolLayer) mapbox.getLayer("selected-marker-layer-manual");

                        final PointF pixel = mapbox.getProjection().toScreenLocation(point);
                        List<Feature> featureAuto = mapbox.queryRenderedFeatures(pixel, "marker-layer-auto");
                        List<Feature> selectedFeatureAuto = mapbox.queryRenderedFeatures(pixel, "selected-marker-layer-auto");
                        List<Feature> featuresManual = mapbox.queryRenderedFeatures(pixel, "marker-layer-manual");
                        List<Feature> selectedFeatureManual = mapbox.queryRenderedFeatures(pixel, "selected-marker-layer-manual");

                        if (selectedFeatureAuto.size() > 0 && markerSelectedAuto) {
                            return;
                        }
                        if (selectedFeatureManual.size() > 0 && markerSelectedManual) {
                            return;
                        }

                        if (featureAuto.isEmpty() || featuresManual.isEmpty()) {
                            if (markerSelectedAuto) {
                                deselectMarker(markerAuto, 0);
                                return;
                            }
                            if (markerSelectedManual) {
                                deselectMarker(markerManual, 1);
                                return;
                            }
                        }

                        if (featureAuto.size() > 0) {
                            FeatureCollection featureCollectionAuto = FeatureCollection.fromFeatures(
                                    new Feature[]{Feature.fromGeometry(featureAuto.get(0).getGeometry())});
                            GeoJsonSource sourceAuto = mapbox.getSourceAs("selected-marker-auto");
                            if (sourceAuto != null)
                                sourceAuto.setGeoJson(featureCollectionAuto);
                        }
                        if (featuresManual.size() > 0) {
                            FeatureCollection featureCollectionManual = FeatureCollection.fromFeatures(
                                    new Feature[]{Feature.fromGeometry(featuresManual.get(0).getGeometry())});
                            GeoJsonSource sourceManual = mapbox.getSourceAs("selected-marker-manual");
                            if (sourceManual != null)
                                sourceManual.setGeoJson(featureCollectionManual);
                        }

                        if (markerSelectedAuto) {
                            deselectMarker(markerAuto, 0);
                        }
                        if (markerSelectedManual) {
                            deselectMarker(markerManual, 1);
                        }
                        if (featureAuto.size() > 0) {
                            if (featureAuto.get(0).getStringProperty("property")!=null) {
                                if (isEneableShowText())
                                 Toast.makeText(getApplication(), featureAuto.get(0).getStringProperty("property"), Toast.LENGTH_LONG).show();
                            }
                            selectMarker(markerAuto,  0);
                            return;
                        }
                        if (featuresManual.size() > 0) {
                            if (featuresManual.get(0).getStringProperty("property")!=null) {
                                if (isEneableShowText())
                                    Toast.makeText(getApplication(), featuresManual.get(0).getStringProperty("property"), Toast.LENGTH_LONG).show();
                            }
                            selectMarker(markerManual, 1);
                            return;
                        }
                    }
                });
            }
        });


        final EditText searchBar = (EditText) findViewById(R.id.location);
        searchBar.setText(getApplication().getResources().getString(R.string.navig));
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
        mapManager = new MapManager(this);

        isEneableScreen();  // nastavenie či vypínať displej

        searchBar.setOnClickListener(new View.OnClickListener() {  // vymazenie textu navige to na kliknutie
            @Override
            public void onClick(View v) {
                searchBar.setCursorVisible(true);
                searchBar.setText("");
            }
        });

        registerReceiver(netReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        FragmentManager fragmentManager = getFragmentManager();  // vytváranie fragmentu
        fragmentActivity = (FragmentActivity) fragmentManager.findFragmentByTag(FRAGMENTACTIVITY_TAG);

        if (fragmentActivity == null) {
            fragmentActivity = new FragmentActivity();
            fragmentManager.beginTransaction()
                    .add(fragmentActivity, FRAGMENTACTIVITY_TAG)
                    .commit();
        }
        getSupportActionBar().setHomeButtonEnabled(true);     // vytvorenie hamburger menu
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this,
                drawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {  // reaguje na klik na menu
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {  // preťažený klik spať ak je zobrazené menu
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {   // zovbrezenie menu s ikonami
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {

            case R.id.position:   // nastavenie aktualnej polohy a zoomu po kliku na ikonu
                if (!fragmentActivity.checkGPSEnable()) {
                    if (isEneableShowText())
                        Toast.makeText(this, this.getResources().getString(R.string.turn_gps), Toast.LENGTH_LONG).show();
                    return true;
                }
                if (fragmentActivity.gps != null)
                    fragmentActivity.gps.getOnPosition();
                else {
                    if (isEneableShowText())
                        Toast.makeText(this, this.getResources().getString(R.string.not_position), Toast.LENGTH_LONG).show();
                }
                return true;

            case R.id.layer:
                if (!fragmentActivity.isNetworkAvailable(context) || mapbox == null) {
                    if (isEneableShowText())
                        Toast.makeText(this,this.getResources().getString(R.string.change_map_style), Toast.LENGTH_LONG).show();
                    return true;
                }
                if (fragmentActivity==null || fragmentActivity.mapLayer==null ) {
                    if (isEneableShowText())
                        Toast.makeText(this,this.getResources().getString(R.string.no_gps), Toast.LENGTH_LONG).show();
                    return true;
                }
                // spustenie menu na výber typu vrstvy
                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setTitle(this.getResources().getString(R.string.map));
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                        MainActivity.this, android.R.layout.select_dialog_singlechoice);
                arrayAdapter.add(this.getResources().getString(R.string.street));
                arrayAdapter.add(this.getResources().getString(R.string.satellite));
                arrayAdapter.add(this.getResources().getString(R.string.outdoors));
                alert.setNegativeButton(
                        this.getResources().getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                alert.setAdapter(
                        arrayAdapter,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int select) {
                                switch (select) {
                                    case 0:
                                        mapbox.setStyleUrl("mapbox://styles/mapbox/light-v9");
                                        LatLng lightBumps = fragmentActivity.gps.getCurrentLatLng();
                                        if (featureMarker!=null)
                                            fragmentActivity.mapLayer.setClear(featureMarker);
                                        fragmentActivity.mapLayer.getAllBumps(lightBumps.latitude, lightBumps.longitude);
                                        break;

                                    case 1:
                                        mapbox.setStyleUrl("mapbox://styles/mapbox/satellite-v9");
                                        LatLng satelliteBumps = fragmentActivity.gps.getCurrentLatLng();
                                        if (featureMarker!=null)
                                            fragmentActivity.mapLayer.setClear(featureMarker);
                                        fragmentActivity.mapLayer.getAllBumps(satelliteBumps.latitude, satelliteBumps.longitude);
                                        break;
                                    case 2:
                                        mapbox.setStyleUrl("mapbox://styles/mapbox/outdoors-v9");
                                        LatLng outdoorsBumps = fragmentActivity.gps.getCurrentLatLng();
                                        if (featureMarker!=null)
                                            fragmentActivity.mapLayer.setClear(featureMarker);
                                        fragmentActivity.mapLayer.getAllBumps(outdoorsBumps.latitude, outdoorsBumps.longitude);

                                        break;
                                }
                            }
                        });
                alert.show();
                return true;

            case R.id.filter: // zobrayenie typu výtlkov
                if (!fragmentActivity.checkGPSEnable()) {
                    if (isEneableShowText())
                        Toast.makeText(this, this.getResources().getString(R.string.turn_gps), Toast.LENGTH_LONG).show();
                    return true;
                }
                if (fragmentActivity==null || fragmentActivity.mapLayer==null ) {
                    if (isEneableShowText())
                        Toast.makeText(this, this.getResources().getString(R.string.no_gps), Toast.LENGTH_LONG).show();
                    return true;
                }
                AlertDialog.Builder builderSingles = new AlertDialog.Builder(MainActivity.this);
                builderSingles.setTitle(this.getResources().getString(R.string.sh_bumps));

                final ArrayAdapter<String> arrayAdapters = new ArrayAdapter<String>(
                        MainActivity.this, android.R.layout.select_dialog_singlechoice);
                arrayAdapters.add(this.getResources().getString(R.string.all_bumps));
                arrayAdapters.add(this.getResources().getString(R.string.med_lar_bumps));
                arrayAdapters.add(this.getResources().getString(R.string.lar_bumps));


                builderSingles.setNegativeButton(
                        this.getResources().getString(R.string.cancel),
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
                                                fragmentActivity.mapLayer.level = ALL_BUMPS;
                                                LatLng allBumps = fragmentActivity.gps.getCurrentLatLng();
                                                if (featureMarker!=null)
                                                    fragmentActivity.mapLayer.setClear(featureMarker);
                                                fragmentActivity.mapLayer.getAllBumps(allBumps.latitude, allBumps.longitude);
                                            }
                                        }.start();
                                        break;

                                    case 1:
                                        new Thread() {
                                            public void run() {
                                                fragmentActivity.mapLayer.level = MEDIUM_BUMPS;
                                                LatLng mediumBumps = fragmentActivity.gps.getCurrentLatLng();
                                                if (featureMarker!=null)
                                                    fragmentActivity.mapLayer.setClear(featureMarker);
                                                fragmentActivity.mapLayer.getAllBumps(mediumBumps.latitude, mediumBumps.longitude);

                                            }
                                        }.start();
                                        break;
                                    case 2:
                                        new Thread() {
                                            public void run() {
                                                fragmentActivity.mapLayer.level = LARGE_BUMPS;
                                                LatLng largeBumps = fragmentActivity.gps.getCurrentLatLng();
                                                if (featureMarker!=null)
                                                    fragmentActivity.mapLayer.setClear(featureMarker);
                                                fragmentActivity.mapLayer.getAllBumps(largeBumps.latitude, largeBumps.longitude);
                                            }
                                        }.start();
                                        break;
                                }
                            }
                        });
                builderSingles.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void close () {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
       switch (item.getItemId()) {

           case R.id.calibrate:  // spusti prekalibrovanie aplikácie
                close();
                if ( fragmentActivity.accelerometer!=null) {
                    fragmentActivity.accelerometer.calibrate();
                    if (isEneableShowText())
                        Toast.makeText(context, this.getResources().getString(R.string.calibrate), Toast.LENGTH_SHORT).show();
                }else {
                    if (isEneableShowText())
                        Toast.makeText(context, this.getResources().getString(R.string.gps_calibrate), Toast.LENGTH_SHORT).show();
                }
                return true;

           case R.id.clear_map:  // vyčistí mapu
                close();
                if (mapbox!=null && fragmentActivity!=null && fragmentActivity.gps!=null) {
                    if (featureMarker!=null)
                        fragmentActivity.mapLayer.setClear(featureMarker);
                    fragmentActivity.gps.remove_draw_road();
                    fragmentActivity.mapLayer.deleteOldMarker();

                }
                return true;

           case R.id.navigation:  // ukončuje navigáciu
                close();
                EditText text = (EditText) findViewById(R.id.location);

                text.setText(this.getResources().getString(R.string.navig));
                if ( fragmentActivity.gps!=null) {
                    new Thread() {
                        public void run() {
                            fragmentActivity.gps.remove_draw_road();
                            fragmentActivity.detection.setRoad(false);
                            fragmentActivity.detection.stop_collison_navigate();
                        }
                    }.start();
                }
                return true;


           case R.id.action_settings: // presun do seeting activity
                close();
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

           case R.id.download:

                if (fragmentActivity!=null &&  mapManager!=null) {
                    if (mapManager.isMapTitleExceeded()) {  // upozornenie na prekročenie kapacity map
                        if (isEneableShowText())
                            Toast.makeText(this, this.getResources().getString(R.string.map_exceeded), Toast.LENGTH_LONG).show();
                        return true;
                    }

                }
                close();

                if (!fragmentActivity.checkGPSEnable()) { // nie je gps
                    if (isEneableShowText())
                        Toast.makeText(this,this.getResources().getString(R.string.turn_gps), Toast.LENGTH_LONG).show();
                    return true;
                }
                if ( mapbox==null || fragmentActivity.mapLayer==null) { // nieje načítana mapa
                    if (isEneableShowText())
                        Toast.makeText(this, this.getResources().getString(R.string.map_not_load), Toast.LENGTH_LONG).show();
                    return true;
                }
                save(false);
                setOnPosition =true;
                confirm.setVisibility(View.INVISIBLE);
               fragmentActivity.mapLayer.setClear(featureMarker);
                add_button.setVisibility(View.VISIBLE);
                navig_on.setVisibility(View.INVISIBLE);
                if ( mapManager!=null && !mapManager.isEndNotified()) {
                    if (isEneableShowText())
                        Toast.makeText(this, this.getResources().getString(R.string.download_run), Toast.LENGTH_LONG).show();
                }
                else
                    mapManager.downloadRegionDialog();
                return true;

           case R.id.list:
                close();

               if ( fragmentActivity.mapLayer==null) { // nieje načítana mapa
                   if (isEneableShowText())
                       Toast.makeText(this, this.getResources().getString(R.string.turn_gps), Toast.LENGTH_LONG).show();
                   return true;
               }

                save(false);
                if ( mapbox==null ) {
                    if (isEneableShowText())
                        Toast.makeText(this,this.getResources().getString(R.string.map_not_load), Toast.LENGTH_LONG).show();
                    return true;
                }
                add_button.setVisibility(View.VISIBLE);  //  schovanie tlačidiel
                mapConfirm.setVisibility(View.INVISIBLE);
                confirm.setVisibility(View.INVISIBLE);
               fragmentActivity.mapLayer.setClear(featureMarker);
                navig_on.setVisibility(View.INVISIBLE);
                if (mapManager!=null && !mapManager.isEndNotified()) {
                    if (isEneableShowText())
                        Toast.makeText(this, this.getResources().getString(R.string.download_run_list), Toast.LENGTH_LONG).show();
                }
                else
                    mapManager.downloadedRegionList();
                return true;

           case R.id.error:
               int number = Integer.parseInt("number");
               return true;

            case R.id.exit:
                close();
                if (fragmentActivity.accelerometer!=null) {
                    while (true) {
                        if (updatesLock.tryLock()) {
                            Log.d(TAG," exit updatesLock lock");
                           try  {
                                while (true) {
                                    if (lockZoznam.tryLock()) {
                                        try {
                                            Log.d(TAG," exit lockzoznam lock");
                                            ArrayList<HashMap<Location, Float>> list = fragmentActivity.accelerometer.getPossibleBumps();
                                            ArrayList<Integer> bumpsManual = fragmentActivity.accelerometer.getBumpsManual();
                                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(this);
                                            SQLiteDatabase sb = databaseHelper.getWritableDatabase();
                                            fragmentActivity.checkIntegrityDB(sb);
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
                                                        Log.d(TAG, "exit ukladam data ");
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
                                            sb.close();
                                            databaseHelper.close();
                                            fragmentActivity.checkCloseDb(sb);

                                        } finally {
                                            Log.d(TAG, " exit lockZoznam unlock  ");
                                            lockZoznam.unlock();
                                            break;
                                        }
                                    } else {
                                        Log.d(TAG, " exit lockZoznam try lock  ");
                                        try {
                                            Thread.sleep(20);
                                        } catch (InterruptedException e) {
                                            e.getMessage();
                                        }
                                    }
                                }
                            }
                            finally {
                               Log.d(TAG, " exit updatesLock unlock  ");
                               updatesLock.unlock();
                               break;
                            }
                           } else {
                            Log.d(TAG, " exit updatesLock try lock");
                            try {
                                Thread.sleep(20);
                            } catch (InterruptedException e) {
                                e.getMessage();
                            }
                        }
                    }
                        fragmentActivity.stop_servise();  // ukončujem servises
                }
                onDestroy();
                return true;

           default:
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);
                return true;

       }
    }

    private  class LatLngEvaluator implements TypeEvaluator<com.mapbox.mapboxsdk.geometry.LatLng> {
        // animácia na pohyb markeru
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
        // animácia na vybraný marker - zväčšenie
        if (marker==null)
            return;
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
            markerSelectedAuto = true;
        else
            markerSelectedManual = true;
    }

    private void deselectMarker(final SymbolLayer marker, int i) {
        // animácia na vybraný marker - zmenšenie
        if (marker==null)
            return;
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
        if (i == 0)
            markerSelectedAuto = false;
        else
            markerSelectedManual = false;
    }

    public void isEneableScreen() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean screen = prefs.getBoolean("screen", Boolean.parseBoolean(null));
        Log.d(TAG, "isEneableScreen stav - "+String.valueOf(screen));
        if (screen)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
          getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void save(boolean save_click) {
        Log.d(TAG, "save click stav - " + save_click );
        if (!save_click && featureMarker!=null )
            featureMarker.remove();
        featureMarker=null;
        allow_click=false;
    }

    private BroadcastReceiver netReceiver  = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().matches("android.net.conn.CONNECTIVITY_CHANGE")) {
                ConnectivityManager connectivityManager
                        = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

                boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
                if (isConnected) {
                    if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        Log.d(TAG, "netReceiver TYPE_WIFI ");
                        manager.setConnected(true);
                    }
                    if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                        Log.d(TAG, "netReceiver TYPE_MOBILE ");
                        if (isEneableOnlyWifiMap()) {
                            Log.d(TAG, "netReceiver - setConnected(false)");
                            manager.setConnected(false);
                        }
                        else {
                            Log.d(TAG, "netReceiver - setConnected(true)");
                            manager.setConnected(true);
                        }
                    }
                }
            }
        }
    };

    public boolean isEneableOnlyWifiMap() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d(TAG,"isEneableOnlyWifiMap "+ String.valueOf(prefs.getBoolean("map", Boolean.parseBoolean(null))));
        return prefs.getBoolean("map", Boolean.parseBoolean(null));
    }

    public void onClick(View v) {   // pridanie markeru na stlačenie pluska
        switch (v.getId()) {
            case R.id.add_button:
                if (!fragmentActivity.checkGPSEnable()) {
                    if (isEneableShowText())
                        Toast.makeText(this, this.getResources().getString(R.string.turn_gps), Toast.LENGTH_LONG).show();
                    break;
                }
                if (mapbox==null) {
                    if (isEneableShowText())
                        Toast.makeText(this,this.getResources().getString(R.string.map_not_load), Toast.LENGTH_LONG).show();
                    break;
                }
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this);
                builderSingle.setTitle(this.getResources().getString(R.string.type_bumps));
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                        MainActivity.this,android.R.layout.select_dialog_singlechoice);
                arrayAdapter.add(this.getResources().getString(R.string.type_lage));
                arrayAdapter.add(this.getResources().getString(R.string.type_medium));
                arrayAdapter.add(this.getResources().getString(R.string.type_normal));

                builderSingle.setNegativeButton(
                        this.getResources().getString(R.string.cancel),
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
                                if (fragmentActivity!=null && fragmentActivity.mapLayer!=null)
                                    fragmentActivity.mapLayer.setClear(featureMarker);
                                add_button.setVisibility(View.INVISIBLE);

                            }
                        });
                builderSingle.show();
                break;

            case R.id.save_btn:   // potvrdenie pridania markera
                add_button.setVisibility(View.VISIBLE);
                confirm.setVisibility(View.INVISIBLE);
                if (fragmentActivity!=null && fragmentActivity.mapLayer!=null)
                    fragmentActivity.mapLayer.setClear(null);
                save(true);
                if (convert_location != null) {
                    if (isEneableShowText())
                        Toast.makeText(this, this.getResources().getString(R.string.bump_add), Toast.LENGTH_LONG).show();
                    final double ll = intensity;
                    final Location location = new Location("new");
                    location.setLatitude(round(convert_location.getLatitude(),7));
                    location.setLongitude(round(convert_location.getLongitude(),7));
                    location.setTime(new Date().getTime());
                    convert_location  =null;
                    new Thread() {
                        public void run() {
                            Looper.prepare();
                            while(true) {
                                if (!threadLock.get() ) {
                                    if (lockAdd.tryLock()) {
                                        Log.d(TAG," save button lockAdd lock ");
                                        try {
                                            if (lockZoznam.tryLock()) {
                                                Log.d(TAG," save button lockZoznam lock ");
                                                try {
                                                    Log.d(TAG," save button pridal do zoznamu");
                                                    threadLock.getAndSet(true);
                                                    fragmentActivity.accelerometer.addPossibleBumps(location, (float) round(intensity,6));
                                                    fragmentActivity.accelerometer.addBumpsManual(1);
                                                    if (updatesLock.tryLock()) {
                                                        Log.d(TAG," save button updatesLock lock ");
                                                        try  {
                                                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(context);
                                                            SQLiteDatabase database = databaseHelper.getReadableDatabase();

                                                            fragmentActivity.checkIntegrityDB(database);
                                                            Log.d(TAG," save button vlozil do db ");
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
                                                            databaseHelper.close();
                                                            fragmentActivity.checkCloseDb(database);
                                                        }
                                                        finally {
                                                            Log.d(TAG," save button updatesLock unlock");
                                                            updatesLock.unlock();
                                                        }
                                                    }
                                                     threadLock.getAndSet(false);
                                                     Log.d(TAG," save button casový lock end");
                                                }
                                                finally {
                                                    Log.d(TAG," save button lockZoznam unlock");
                                                    lockZoznam.unlock();
                                                    break;
                                                }
                                            }
                                        }
                                        finally {
                                            Log.d(TAG," save button lockAdd unlock");
                                            lockAdd.unlock();
                                        }
                                    }
                                }
                                Log.d(TAG,"casovy lock");
                                Log.d(TAG, " save button " + String.valueOf(location.getLatitude()));
                                Log.d(TAG, " save button " + String.valueOf(location.getLongitude()));
                                Log.d(TAG, " save button " + String.valueOf(ll));
                                try {
                                    Thread.sleep(20); // sleep for 50 ms so that main UI thread can handle user actions in the meantime
                                } catch (InterruptedException e) {
                                   e.getMessage();
                                }
                            }
                            Looper.loop();
                        }
                    }.start();
                }
                else
                    Log.d(TAG, " save button  null location !!!!!! " );
                break;
            case R.id.delete_btn:   // mazania označeného markeru
                add_button.setVisibility(View.VISIBLE);
                confirm.setVisibility(View.INVISIBLE);
                allow_click=false;  // disable listener na klik
                save(false);
                convert_location  =null;
                if (fragmentActivity!=null && fragmentActivity.mapLayer!=null)
                    fragmentActivity.mapLayer.setClear(null);  // mapa už bude mazať aj marker
                break;
            case R.id.backMap_btn:
                setOnPosition = true;
                SetUpCamera();
                mapManager.alertSelectRegion(selectedName,1); // spusti sa alert dialog na opetovné hladanie mapy
                mapConfirm.setVisibility(View.INVISIBLE);
                add_button.setVisibility(View.VISIBLE);
                 break;
            case R.id.saveMap_btn:
                mapManager.downloadRegion(selectedName, 0);
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
        if ( fragmentActivity==null || fragmentActivity.detection==null) { // nieje načítana mapa
            if (isEneableShowText())
                Toast.makeText(this, this.getResources().getString(R.string.turn_gps), Toast.LENGTH_LONG).show();
            return ;
        }
        Address address = null;
        EditText text = (EditText) findViewById(R.id.location);
        if (isEneableShowText())
            Toast.makeText(this, this.getResources().getString(R.string.fnd_location), Toast.LENGTH_LONG).show();
        text.setCursorVisible(false);
        hideKeyboard(v);
        String location = text.getText().toString();
        if (fragmentActivity.isNetworkAvailable(context)) {
            try {
                address = Route.findLocality(location, this);
                if (address == null) {
                    if (isEneableShowText())
                        Toast.makeText(this, this.getResources().getString(R.string.unable_find), Toast.LENGTH_LONG).show();
                }
                else {
                    final LatLng to_position = new LatLng(address.getLatitude(),address.getLongitude());
                    new Thread() {  // ukončím predchádzajucuc navigáciu ak bola, a vytvorím novú
                        public void run() {
                            fragmentActivity.detection.stop_collison_navigate();
                            fragmentActivity.detection.bumps_on_position(fragmentActivity, to_position);
                        }
                    }.start();
                }
            }
            catch (Exception e) {
                if (isEneableShowText())
                    Toast.makeText(this,  this.getResources().getString(R.string.unable_find), Toast.LENGTH_LONG).show();
            }
        }
        else {
            if (isEneableShowText())
                Toast.makeText(this, this.getResources().getString(R.string.unable_find_net), Toast.LENGTH_LONG).show();
        }
    }

    public void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @Override
    protected void onDestroy() {
        mapManager.endDownloadNotification();
        super.onDestroy();
        mapView.onDestroy();
        super.onUserLeaveHint();
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void SetUpCamera(){
        if (fragmentActivity.gps !=null && fragmentActivity.gps.getmCurrentLocation()!= null) {
            LatLng myPosition = new LatLng(fragmentActivity.gps.getmCurrentLocation().getLatitude(), fragmentActivity.gps.getmCurrentLocation().getLongitude());
            if (myPosition!=null && setOnPosition &&  MainActivity.isActivityVisible() && mapbox!=null) {
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
        isEneableScreen();
        super.onResume();
        SetUpCamera();  // nastavenie kamery
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
        Log.d(TAG,"isEneableShowText stav - "+alarm);
        return ((alarm) || (!alarm && MainActivity.isActivityVisible()));
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public  String getLanguage() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String name = prefs.getString("lang", "");
        Log.d(TAG,"getLanguage stav - "+name);
        return name;
    }
}