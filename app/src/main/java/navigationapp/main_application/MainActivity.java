package navigationapp.main_application;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;

import navigationapp.Error.ExceptionHandler;
import navigationapp.R;

import com.daimajia.slider.library.Animations.DescriptionAnimation;
import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.TextSliderView;
import com.daimajia.slider.library.Tricks.ViewPagerEx;
import com.google.android.gms.maps.model.LatLng;
import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.seatgeek.placesautocomplete.DetailsCallback;
import com.seatgeek.placesautocomplete.OnPlaceSelectedListener;
import com.seatgeek.placesautocomplete.PlacesAutocompleteTextView;
import com.seatgeek.placesautocomplete.model.Place;
import com.seatgeek.placesautocomplete.model.PlaceDetails;
import com.seatgeek.placesautocomplete.model.PlaceLocation;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener , BaseSliderView.OnSliderClickListener, ViewPagerEx.OnPageChangeListener{

    private SlidingUpPanelLayout mLayout;
    private SliderLayout mDemoSlider;
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
    private PlaceLocation positionGPS = null;
    ImageView searchLocation = null;
    private Integer numOfPicture = 0;
    private int select_iteam = 0;
    private String select_iteam_text = null;
    private static final int PICK_IMAGE_ID = 234;
    private static final int PICK_IMAGE_ADD_ID = 233;
    public static String androidId =  null;
    @InjectView(R.id.location)
    PlacesAutocompleteTextView mAutocomplete;
    com.mapbox.mapboxsdk.geometry.LatLng points =null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        manager = MapboxAccountManager.start(this,"pk.eyJ1IjoiYWRhbXN0cmFza3kiLCJhIjoiY2l1aDYwYzZvMDAydTJ5b2dwNXoyNHJjeCJ9.XsDrnj02GHMwBExP5Va35w");
        Language.setLanguage(MainActivity.this,getLanguage());
        setContentView(R.layout.activity_main);
        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mDemoSlider = (SliderLayout)findViewById(R.id.slider);
        mDemoSlider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom);
        mDemoSlider.setCustomAnimation(new DescriptionAnimation());
        mDemoSlider.setDuration(8000);
        mDemoSlider.addOnPageChangeListener(this);
        mDemoSlider.setPresetTransformer(SliderLayout.Transformer.RotateDown);
        ButterKnife.inject(this);
        context = this;
        mapView = (MapView) findViewById(R.id.mapboxMarkerMapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                mapbox = mapboxMap;
                mapbox.setMyLocationEnabled(false);
                if (setOnPosition)
                    mapbox.setMyLocationEnabled(true);

                mapbox.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull com.mapbox.mapboxsdk.geometry.LatLng point) {
                        if (allow_click) {  // ak som klikol na plus, možem klikať
                            points =point;
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
                                fragmentActivity.mapLayer.setClear(featureMarker);
                                convert_location = point;
                                Log.d(TAG," point click "+ convert_location.getLongitude() +" " +convert_location.getLatitude() );
                            }
                        }

                        mapboxMap.setOnCameraChangeListener(new MapboxMap.OnCameraChangeListener() {
                            @Override
                            public void onCameraChange(CameraPosition position) {
                                if (allow_click) {
                                    if (featureMarker != null) {
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                show();
                                            }
                                        });
                                    }
                                }
                            }
                        });

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
                                setPanel( featureAuto.get(0).getStringProperty("property"),featureAuto.get(0).getStringProperty("lat"),featureAuto.get(0).getStringProperty("ltn"));
                            }
                            selectMarker(markerAuto,  0);
                            return;
                        }
                        if (featuresManual.size() > 0) {

                            if (featuresManual.get(0).getStringProperty("property")!=null) {
                                setPanel( featuresManual.get(0).getStringProperty("property"),featuresManual.get(0).getStringProperty("lat"),featuresManual.get(0).getStringProperty("ltn"));
                            }
                            selectMarker(markerManual, 1);
                            return;
                        }
                    }
                });
            }
        });
        androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        add_button = (Button) findViewById(R.id.add_button);
        save_button = (Button) findViewById(R.id.save_btn);
        delete_button = (Button) findViewById(R.id.delete_btn);
        downloand_button = (Button) findViewById(R.id.saveMap_btn);
        back_button = (Button) findViewById(R.id.backMap_btn);
        navig_on = (Button) findViewById(R.id.navig_on);
        mapConfirm = (LinearLayout) findViewById(R.id.mapConfirm);
        confirm = (LinearLayout) findViewById(R.id.confirm);
        searchLocation = (ImageView) findViewById(R.id.search_img);
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
        EditText text = (EditText) findViewById(R.id.location);
        text.setCursorVisible(false);
        mAutocomplete.setOnPlaceSelectedListener(new OnPlaceSelectedListener() {
            @Override
            public void onPlaceSelected(final Place place) {
                mAutocomplete.getDetailsFor(place, new DetailsCallback() {
                    @Override
                    public void onSuccess(final PlaceDetails details) {
                        positionGPS = details.geometry.location;
                        Log.d(TAG," Autocomplete positionGPS.lat;"+  positionGPS.lat +" positionGPS.lng "+positionGPS.lng);
                    }

                    @Override
                    public void onFailure(final Throwable failure) {
                        Log.d(TAG, "Autocomplete failure " + failure);
                    }
                });
            }
        });
        searchLocation.setOnClickListener(new View.OnClickListener() {  // vymazenie textu navige to na kliknutie
            @Override
            public void onClick(final View v) {
                EditText text = (EditText) findViewById(R.id.location);
                text.setCursorVisible(false);
                new Thread() {  // ukončím predchádzajucuc navigáciu ak bola, a vytvorím novú
                    public void run() {
                        Looper.prepare();
                        Log.d(TAG, "onClick searchLocation");
                        if (fragmentActivity == null || fragmentActivity.detection == null) { // nieje načítana mapa
                            if (isEneableShowText())
                                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.turn_gps), Toast.LENGTH_LONG).show();
                            return;
                        }
                        Address address = null;
                        EditText text = (EditText) findViewById(R.id.location);
                        if (isEneableShowText())
                            Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.fnd_location), Toast.LENGTH_LONG).show();
                        hideKeyboard(v);
                        LatLng position = null;
                        String location = text.getText().toString();
                        if (fragmentActivity.isNetworkAvailable(context)) {
                            try {
                                if (positionGPS == null) {
                                    Log.d(TAG, "searchLocation positionGPS bola null");
                                    address = Route.findLocality(location, getApplicationContext());
                                    position = new LatLng(address.getLatitude(), address.getLongitude());
                                } else {
                                    Log.d(TAG, "searchLocation positionGPS not null");
                                    position = new LatLng(positionGPS.lat, positionGPS.lng);
                                }

                                if (address == null && positionGPS == null) {
                                    if (isEneableShowText())
                                        Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.unable_find), Toast.LENGTH_LONG).show();
                                } else {
                                    positionGPS = null;
                                    final LatLng to_position = new LatLng(position.latitude, position.longitude);
                                    fragmentActivity.detection.stop_collison_navigate();
                                    fragmentActivity.detection.bumps_on_position(fragmentActivity, to_position);
                                }
                            } catch (Exception e) {
                                if (isEneableShowText())
                                    Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.unable_find), Toast.LENGTH_LONG).show();
                                if (fragmentActivity.gps != null) {
                                    fragmentActivity.gps.remove_draw_road();
                                    fragmentActivity.detection.setRoad(false);
                                    fragmentActivity.detection.stop_collison_navigate();
                                }
                            }
                        } else {
                            if (isEneableShowText())
                                Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.unable_find_net), Toast.LENGTH_LONG).show();
                        }
                        Looper.loop();
                    }
                }.start();

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
        SpannableString spannable = new SpannableString( this.getResources().getString(R.string.app_name));
        spannable.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 0, spannable.length(),  Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        getSupportActionBar().setTitle(spannable);
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

    synchronized  public  void show () {
        if (featureMarker != null) {
            ValueAnimator markerAnimator = ObjectAnimator.ofObject(featureMarker, "position",
                    new LatLngEvaluator(), featureMarker.getPosition(), points);
            markerAnimator.setDuration(2000);
            markerAnimator.start();
        }
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
        }  else if (mLayout != null &&
                (mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED || mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
            mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            // super.onBackPressed();
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
                                        fragmentActivity.mapLayer.getAllBumps(lightBumps.latitude, lightBumps.longitude);
                                        break;

                                    case 1:
                                        mapbox.setStyleUrl("mapbox://styles/mapbox/satellite-v9");
                                        LatLng satelliteBumps = fragmentActivity.gps.getCurrentLatLng();
                                        fragmentActivity.mapLayer.getAllBumps(satelliteBumps.latitude, satelliteBumps.longitude);
                                        break;
                                    case 2:
                                        mapbox.setStyleUrl("mapbox://styles/mapbox/outdoors-v9");
                                        LatLng outdoorsBumps = fragmentActivity.gps.getCurrentLatLng();
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

                                        fragmentActivity.mapLayer.level = ALL_BUMPS;
                                        LatLng allBumps = fragmentActivity.gps.getCurrentLatLng();
                                        fragmentActivity.mapLayer.getAllBumps(allBumps.latitude, allBumps.longitude);

                                        break;

                                    case 1:

                                        fragmentActivity.mapLayer.level = MEDIUM_BUMPS;
                                        LatLng mediumBumps = fragmentActivity.gps.getCurrentLatLng();
                                        fragmentActivity.mapLayer.getAllBumps(mediumBumps.latitude, mediumBumps.longitude);

                                        break;
                                    case 2:

                                        fragmentActivity.mapLayer.level = LARGE_BUMPS;
                                        LatLng largeBumps = fragmentActivity.gps.getCurrentLatLng();
                                        fragmentActivity.mapLayer.getAllBumps(largeBumps.latitude, largeBumps.longitude);

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
                    fragmentActivity.gps.remove_draw_road();
                    fragmentActivity.mapLayer.deleteOldMarker();

                }
                return true;

            case R.id.navigation:  // ukončuje navigáciu
                close();
                EditText text = (EditText) findViewById(R.id.location);
                positionGPS =null;
                text.setText("");
                if ( fragmentActivity.gps!=null) {

                    fragmentActivity.gps.remove_draw_road();
                    fragmentActivity.detection.setRoad(false);
                    fragmentActivity.detection.stop_collison_navigate();

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

         /*  case R.id.error:
               int number = Integer.parseInt("number");
               return true;*/

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
                                            ArrayList<Integer> bumpsType = fragmentActivity.accelerometer.gettypeDetect();
                                            ArrayList<String> bumpstext = fragmentActivity.accelerometer.gettextDetect();
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
                                                            + " and  ROUND(intensity,6)==ROUND(" + data + ",6)  and type="+bumpsType.get(i)+" and manual=" + bumpsManual.get(i);

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
                                                        contentValues.put(Provider.new_bumps.TYPE, bumpsType.get(i));
                                                        contentValues.put(Provider.new_bumps.TEXT, bumpstext.get(i));
                                                        contentValues.put(Provider.new_bumps.CREATED_AT, getDate(loc.getTime(), "yyyy-MM-dd HH:mm:ss"));
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

    @Override
    public void onSliderClick(BaseSliderView slider) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

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
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
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
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
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

    public Dialog onCreateDialogSingleChoice() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        CharSequence[] array = {
                getApplicationContext().getResources().getString(R.string.type_problem_bump),
                getApplicationContext().getResources().getString(R.string.type_problem_basket),
                getApplicationContext().getResources().getString(R.string.type_problem_canstock),
                getApplicationContext().getResources().getString(R.string.type_problem_other)};

        builder.setTitle( getApplicationContext().getResources().getString(R.string.type_problem))
                .setSingleChoiceItems(array, select_iteam, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        select_iteam = which;
                    }
                })
                .setPositiveButton(getApplicationContext().getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        switch (select_iteam) {
                            case 0:
                                select_iteam_text= "bump";
                                choose_bump();
                                break;
                            case 1:
                                select_iteam_text= "bin";
                                intensity =0.f;
                                allow_click=true;
                                confirm.setVisibility(View.VISIBLE);
                                add_button.setVisibility(View.INVISIBLE);
                                break;
                            case 2:
                                select_iteam_text= "channel";
                                intensity =0.f;
                                allow_click=true;
                                confirm.setVisibility(View.VISIBLE);
                                add_button.setVisibility(View.INVISIBLE);
                                break;
                            case 3:
                                final EditText regionNameEdit = new EditText(context);
                                AlertDialog.Builder windowAlert = new AlertDialog.Builder(context);
                                windowAlert.setPositiveButton(context.getResources().getString(R.string.confirm), null);
                                windowAlert.setNegativeButton(context.getResources().getString(R.string.cancel), null);
                                windowAlert.setView(regionNameEdit);
                                windowAlert.setTitle("Other");
                                final AlertDialog mAlertDialog = windowAlert.create();
                                mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface dialog) {
                                        Button positive_btn = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                        positive_btn.setOnClickListener(new View.OnClickListener() {

                                            @Override
                                            public void onClick(View view) {
                                                select_iteam_text = regionNameEdit.getText().toString();
                                                intensity =0.f;
                                                allow_click=true;
                                                confirm.setVisibility(View.VISIBLE);
                                                add_button.setVisibility(View.INVISIBLE);
                                                mAlertDialog.cancel();
                                            }
                                        });

                                    }
                                });
                                mAlertDialog.show();
                                break;
                            default:
                                select_iteam_text= "default";
                                intensity =0.f;
                                allow_click=true;
                                confirm.setVisibility(View.VISIBLE);
                                add_button.setVisibility(View.INVISIBLE);
                                break;
                        }
                    }
                })
                .setNeutralButton(getApplicationContext().getResources().getString(R.string.add_photo), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        freeMemory();
                        onPickImage( getCurrentFocus(),PICK_IMAGE_ADD_ID);
                    }
                })
                .setNegativeButton(getApplicationContext().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return builder.create();
    }

    public void choose_bump() {
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
                        switch (select) {
                            case 0:
                                intensity = 10.f;
                                break;
                            case 1:
                                intensity = 6.f;
                                break;
                            case 2:
                                intensity =0.f;
                                break;
                            default:
                               break;
                        }
                        allow_click=true;
                        confirm.setVisibility(View.VISIBLE);
                        add_button.setVisibility(View.INVISIBLE);
                    }
                });
        builderSingle.show();
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
                select_iteam=0;
                Dialog dialog = onCreateDialogSingleChoice();
                dialog.show();
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
                    final Location location = new Location("new");
                    location.setLatitude(round(convert_location.getLatitude(),7));
                    location.setLongitude(round(convert_location.getLongitude(),7));
                    convert_location  =null;
                    add_bump(location, intensity,select_iteam_text ,select_iteam );
                }
                else
                    Log.d(TAG, " save button  null location !!!!!! " );
                break;
            case R.id.delete_btn:   // mazania označeného markeru
                add_button.setVisibility(View.VISIBLE);
                confirm.setVisibility(View.INVISIBLE);
                allow_click=false;  // disable listener na klik

                convert_location  =null;
                if (fragmentActivity!=null && fragmentActivity.mapLayer!=null)
                    fragmentActivity.mapLayer.setClear(null);  // mapa už bude mazať aj marker
                save(false);
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
                positionGPS = null;
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

    public void setPanel(String text, final String lat,final String ltn) {

        TextView t = (TextView) findViewById(R.id.info);
        t.setText(text);

        Button f = (Button) findViewById(R.id.add);

        f.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                freeMemory();
                onPickImage( getCurrentFocus(),PICK_IMAGE_ID);
            }
        });

        Button i = (Button) findViewById(R.id.increase);

        i.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Location location = new Location("new");
                location.setLatitude(Double.parseDouble(lat));
                location.setLongitude(Double.parseDouble(ltn));

                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string. increase_number), Toast.LENGTH_LONG).show();
                add_bump(location,6, "bump" ,0);

            }
        });

        mDemoSlider.removeAllSliders();
        HashMap<String,Integer> file_maps = new HashMap<String, Integer>();
        file_maps.put("27/07/2016",R.drawable.bump_1);
        file_maps.put("19/10/2016",R.drawable.bump_2);
        file_maps.put("09/12/2016",R.drawable.bump_3);
        file_maps.put("14/01/2017", R.drawable.bump_4);

        numOfPicture =0 ;
        for(String name : file_maps.keySet()){
            numOfPicture++;
            TextSliderView textSliderView = new TextSliderView(this);
            textSliderView
                    .description(name)
                    .image(file_maps.get(name))
                    .setScaleType(BaseSliderView.ScaleType.Fit)
                    .setOnSliderClickListener(this);
            textSliderView.bundle(new Bundle());
            textSliderView.getBundle()
                    .putString("extra",name);
            mDemoSlider.addSlider(textSliderView);
        }
    }

    public void onPickImage(View view,Integer id) {
        Intent chooseImageIntent = ImagePicker.getPickImageIntent(this);
        startActivityForResult(chooseImageIntent, id);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case PICK_IMAGE_ID:
                Bitmap bitmap = ImagePicker.getImageFromResult(this, resultCode, data);

                if (bitmap!=null) {

                    File f = new File(context.getCacheDir(), bitmap.toString() + ".png");
                    try {
                        f.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                    byte[] bitmapdata = bos.toByteArray();

//write the bytes in file
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(f);

                        fos.write(bitmapdata);
                        fos.flush();
                        fos.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    String date = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
                    HashMap<String, Integer> file_maps = new HashMap<String, Integer>();
                    file_maps.put(date, bitmap.describeContents());



                    TextSliderView textSliderView = new TextSliderView(this);
                    // initialize a SliderLayout
                    textSliderView
                            .description(date)
                            .image(f)
                            .setScaleType(BaseSliderView.ScaleType.Fit)
                            .setOnSliderClickListener(this);

                    //add your extra information
                    textSliderView.bundle(new Bundle());
                    textSliderView.getBundle()
                            .putString("extra", date);

                    mDemoSlider.addSlider(textSliderView);
                    numOfPicture ++ ;
                    mDemoSlider.setCurrentPosition(numOfPicture-2);
                    mDemoSlider.setDuration(100);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mDemoSlider.setDuration(8000);
                        }
                    }, 150);



                }
                break;

            case PICK_IMAGE_ADD_ID:
                Bitmap bitmapa = ImagePicker.getImageFromResult(this, resultCode, data);
                Dialog dialog = onCreateDialogSingleChoice();
                dialog.show();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    public void freeMemory(){
        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();
        super.onLowMemory();
        mapView.onLowMemory();
    }

    synchronized public void add_bump(final Location location, final double ll,final String text,final Integer type) {
        location.setTime(new Date().getTime());
        new Thread() {
            public void run() {
                Looper.prepare();
                while(true) {
                    if (!threadLock.get() ) {
                        threadLock.getAndSet(true);
                        boolean flag =false;
                        if (lockAdd.tryLock()) {
                            try {
                                if (lockZoznam.tryLock()) {
                                    Log.d(TAG," save button lockZoznam lock ");
                                    try {
                                        Log.d(TAG," save button pridal do zoznamu");

                                        fragmentActivity.accelerometer.addPossibleBumps(location, (float) round(intensity,6));
                                        fragmentActivity.accelerometer.addBumpsManual(1);
                                        fragmentActivity.accelerometer.addtextDetect(text);
                                        fragmentActivity.accelerometer.addtypeDetect(type);
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
                                                contentValues.put(Provider.new_bumps.TYPE, type);
                                                contentValues.put(Provider.new_bumps.TEXT, text);
                                                contentValues.put(Provider.new_bumps.CREATED_AT, getDate(location.getTime(), "yyyy-MM-dd HH:mm:ss"));
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
                                        }  else {
                                            Log.d(TAG," lockzoznam");
                                        }
                                            Log.d(TAG," save button casový lock end");
                                    }
                                    finally {
                                        Log.d(TAG," save button lockZoznam unlock");
                                        flag =true;
                                        lockZoznam.unlock();
                                    }
                                }
                                else{   Log.d(TAG," loczoznam");}
                            }
                            finally {
                                Log.d(TAG," save button lockAdd unlock");
                            lockAdd.unlock();
                                if (flag) {
                                    threadLock.getAndSet(false);
                                    break;
                                }
                            }
                        }else {
                            Log.d(TAG," lockAdd");}
                        threadLock.getAndSet(false);
                    }
                    else  {
                        Log.d(TAG," threadLock");
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

    public static String getDate(long milliSeconds, String dateFormat)
    {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }
}
