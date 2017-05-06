package navigationapp.main_application;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
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
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
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
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
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

import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip;
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
import com.mapbox.mapboxsdk.constants.MyBearingTracking;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
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

import static navigationapp.main_application.FragmentActivity.isNetworkAvailable;
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
    EditText text = null;
    private FragmentActivity fragmentActivity =null;
    public  final String FRAGMENTACTIVITY_TAG = "blankFragment";
    public  final String TAG = "MainActivity";
    private static boolean activityVisible=true;
    public static final String PREF_FILE_NAME = "Settings";
    private Float intensity = null;
    LinearLayout confirm = null;
    Button  save_button, delete_button,downloand_button,back_button;
    public  static MapView mapView = null;
    LinearLayout layoutNavigation = null;
    public static LinearLayout mapConfirm;
    public static Button navig_on,add_button;
    public static MapboxMap mapbox = null;
    public Button set_location = null, show_address = null ;
    public static MapboxAccountManager manager;
    private boolean markerSelectedAuto = false;
    private boolean markerSelectedManual = false;
    private boolean markerSelectedReport = false;
    private boolean markerSelectedReportRepaired = false;
    private boolean markerSelectedBumpRepaired = false;
    private Marker featureMarker =  null ;
    boolean allow_click= false;
    com.mapbox.mapboxsdk.geometry.LatLng  convert_location  =null;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout = null;
    private MapManager mapManager = null;
    private PlaceLocation positionGPS = null;
    ImageView searchLocation = null;
    public Button infosssss = null;
    private Integer numOfPicture = 0;
    private int select_iteam = 0;
    private String select_iteam_text = null;
    private static final int PICK_IMAGE_ID = 234;
    private static final int PICK_IMAGE_ADD_ID = 233;
    public static String androidId =  null;
    private String latitude_photo = null;
    private String longitude_photo = null;
    private String type_photo = null;
    File f = null;
    Boolean panelState = true;
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

                mapbox.getTrackingSettings().setMyLocationTrackingMode(MyLocationTracking.TRACKING_FOLLOW);

                mapbox.getTrackingSettings().setMyBearingTrackingMode(MyBearingTracking.COMPASS);

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
                                String titleMarker = null;
                                switch (select_iteam) {
                                    case 0:
                                        titleMarker = getApplication().getResources().getString(R.string.add_marker_bump);
                                        break;
                                    case 1:
                                        titleMarker = getApplication().getResources().getString(R.string.add_marker_bin);
                                        break;
                                    case 2:
                                        titleMarker = getApplication().getResources().getString(R.string.add_marker_channel);
                                        break;
                                    case 3:
                                        titleMarker = getApplication().getResources().getString(R.string.add_marker_other);
                                        break;
                                    default:
                                        break;
                                }

                                featureMarker = mapboxMap.addMarker(new MarkerViewOptions()
                                        .position(point)
                                        .title(titleMarker)
                                        .icon(icons)
                                );
                                if (fragmentActivity.mapLayer !=null)
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
                        final SymbolLayer markerReport = (SymbolLayer) mapbox.getLayer("selected-marker-layer-report");
                        final SymbolLayer markerBumpRepaired = (SymbolLayer) mapbox.getLayer("selected-marker-layer-bump-repaired");
                        final SymbolLayer markerReportRepaired = (SymbolLayer) mapbox.getLayer("selected-marker-layer-report-repaired");

                        final PointF pixel = mapbox.getProjection().toScreenLocation(point);
                        List<Feature> featureAuto = mapbox.queryRenderedFeatures(pixel, "marker-layer-auto");
                        List<Feature> selectedFeatureAuto = mapbox.queryRenderedFeatures(pixel, "selected-marker-layer-auto");
                        List<Feature> featuresManual = mapbox.queryRenderedFeatures(pixel, "marker-layer-manual");
                        List<Feature> selectedFeatureManual = mapbox.queryRenderedFeatures(pixel, "selected-marker-layer-manual");
                        List<Feature> featuresReport = mapbox.queryRenderedFeatures(pixel, "marker-layer-report");
                        List<Feature> selectedFeatureReport = mapbox.queryRenderedFeatures(pixel, "selected-marker-layer-report");
                        List<Feature> featuresBumpRepaired = mapbox.queryRenderedFeatures(pixel, "marker-layer-bump-repaired");
                        List<Feature> selectedFeatureBumpRepaired = mapbox.queryRenderedFeatures(pixel, "selected-marker-layer-bump-repaired");
                        List<Feature> featuresReportRepaired = mapbox.queryRenderedFeatures(pixel, "marker-layer-report-repaired");
                        List<Feature> selectedFeatureReportRepaired = mapbox.queryRenderedFeatures(pixel, "selected-marker-layer-report-repaired");
                        if (selectedFeatureAuto.size() > 0 && markerSelectedAuto) {
                            return;
                        }
                        if (selectedFeatureManual.size() > 0 && markerSelectedManual) {
                            return;
                        }
                        if (selectedFeatureReport.size() > 0 && markerSelectedReport) {
                            return;
                        }
                        if (selectedFeatureReportRepaired.size() > 0 && markerSelectedReportRepaired) {
                            return;
                        }

                        if (selectedFeatureBumpRepaired.size() > 0 && markerSelectedBumpRepaired) {
                            return;
                        }

                        if (featureAuto.isEmpty() || featuresManual.isEmpty() || featuresReport.isEmpty()
                                || featuresReportRepaired.isEmpty() || featuresBumpRepaired.isEmpty()) {
                            if (markerSelectedAuto) {
                                deselectMarker(markerAuto, 0);
                                return;
                            }
                            if (markerSelectedManual) {
                                deselectMarker(markerManual, 1);
                                return;
                            }
                            if (markerSelectedReport) {
                                deselectMarker(markerReport, 2);
                                return;
                            }
                            if (markerSelectedBumpRepaired) {
                                deselectMarker(markerBumpRepaired, 3);
                                return;
                            }
                            if (markerSelectedReportRepaired) {
                                deselectMarker(markerReport, 4);
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
                        if (featuresReport.size() > 0) {
                            FeatureCollection featureCollectionReport = FeatureCollection.fromFeatures(
                                    new Feature[]{Feature.fromGeometry(featuresReport.get(0).getGeometry())});
                            GeoJsonSource sourceReport = mapbox.getSourceAs("selected-marker-report");
                            if (sourceReport != null)
                                sourceReport.setGeoJson(featureCollectionReport);
                        }
                        if (featuresBumpRepaired.size() > 0) {
                            FeatureCollection featureCollectionBumpRepaired = FeatureCollection.fromFeatures(
                                    new Feature[]{Feature.fromGeometry(featuresBumpRepaired.get(0).getGeometry())});
                            GeoJsonSource sourceBumpRepaired = mapbox.getSourceAs("selected-marker-bump-repaired");
                            if (sourceBumpRepaired != null)
                                sourceBumpRepaired.setGeoJson(featureCollectionBumpRepaired);
                        }
                        if (featuresReportRepaired.size() > 0) {
                            FeatureCollection featureCollectionReportRepaired = FeatureCollection.fromFeatures(
                                    new Feature[]{Feature.fromGeometry(featuresReportRepaired.get(0).getGeometry())});
                            GeoJsonSource sourceReportRepaired = mapbox.getSourceAs("selected-marker-report-repaired");
                            if (sourceReportRepaired != null)
                                sourceReportRepaired.setGeoJson(featureCollectionReportRepaired);
                        }

                        if (markerSelectedAuto) {
                            deselectMarker(markerAuto, 0);
                        }
                        if (markerSelectedManual) {
                            deselectMarker(markerManual, 1);
                        }
                        if (markerSelectedReport) {
                            deselectMarker(markerReport, 2);
                        }
                        if (markerSelectedBumpRepaired) {
                            deselectMarker(markerBumpRepaired, 3);
                        }
                        if (markerSelectedReportRepaired) {
                            deselectMarker(markerReportRepaired, 4);
                        }
                        if (featureAuto.size() > 0) {

                            if (featureAuto.get(0).getStringProperty("property")!=null) {
                                setPanel( featureAuto.get(0).getStringProperty("property"),featureAuto.get(0).getStringProperty("lat"),featureAuto.get(0).getStringProperty("ltn"),featureAuto.get(0).getStringProperty("type"),featureAuto.get(0).getStringProperty("text"));
                            }
                            selectMarker(markerAuto,  0);
                            return;
                        }
                        if (featuresManual.size() > 0) {

                            if (featuresManual.get(0).getStringProperty("property")!=null) {
                                setPanel( featuresManual.get(0).getStringProperty("property"),featuresManual.get(0).getStringProperty("lat"),featuresManual.get(0).getStringProperty("ltn"),featuresManual.get(0).getStringProperty("type"),featuresManual.get(0).getStringProperty("text"));
                            }
                            selectMarker(markerManual, 1);
                            return;
                        }
                        if (featuresReport.size() > 0) {

                            if (featuresReport.get(0).getStringProperty("property")!=null) {
                                setPanel( featuresReport.get(0).getStringProperty("property"),featuresReport.get(0).getStringProperty("lat"),featuresReport.get(0).getStringProperty("ltn"),featuresReport.get(0).getStringProperty("type"),featuresReport.get(0).getStringProperty("text"));
                            }
                            selectMarker(markerReport, 2);
                            return;
                        }
                        if (featuresBumpRepaired.size() > 0) {

                            if (featuresBumpRepaired.get(0).getStringProperty("property")!=null) {
                                setPanel( featuresBumpRepaired.get(0).getStringProperty("property"),featuresBumpRepaired.get(0).getStringProperty("lat"),featuresBumpRepaired.get(0).getStringProperty("ltn"),featuresBumpRepaired.get(0).getStringProperty("type"),featuresBumpRepaired.get(0).getStringProperty("text"));
                            }
                            selectMarker(markerBumpRepaired, 3);
                            return;
                        }
                        if (featuresReportRepaired.size() > 0) {

                            if (featuresReportRepaired.get(0).getStringProperty("property")!=null) {
                                setPanel( featuresReportRepaired.get(0).getStringProperty("property"),featuresReportRepaired.get(0).getStringProperty("lat"),featuresReportRepaired.get(0).getStringProperty("ltn"),featuresReportRepaired.get(0).getStringProperty("type"),featuresReportRepaired.get(0).getStringProperty("text"));
                            }
                            selectMarker(markerReportRepaired, 4);
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
        searchLocation.setVisibility(View.VISIBLE);
        layoutNavigation = (LinearLayout) findViewById(R.id.laytbtns);
        set_location = (Button) findViewById(R.id.set_location);
        show_address = (Button) findViewById(R.id.show_address);
        infosssss = (Button) findViewById(R.id.infosssss);
        navig_on.setVisibility(View.INVISIBLE);
        confirm.setVisibility(View.INVISIBLE);
        mapConfirm.setVisibility(View.INVISIBLE);
        confirm.setOnClickListener(this);
        save_button.setOnClickListener(this);
        delete_button.setOnClickListener(this);
        add_button.setOnClickListener(this);
        downloand_button.setOnClickListener(this);
        back_button.setOnClickListener(this);
        set_location.setOnClickListener(this);
        show_address.setOnClickListener(this);
        navig_on.setOnClickListener(this);
        mapManager = new MapManager(this);
        layoutNavigation.setVisibility(View.INVISIBLE);
        isEneableScreen();  // nastavenie či vypínať displej
        text = (EditText) findViewById(R.id.location);
        text.setVisibility(View.VISIBLE);
        text.setCursorVisible(false);
        mAutocomplete.setOnPlaceSelectedListener(new OnPlaceSelectedListener() {
            @Override
            public void onPlaceSelected(final Place place) {
                mAutocomplete.getDetailsFor(place, new DetailsCallback() {
                    @Override
                    public void onSuccess(final PlaceDetails details) {
                        positionGPS = details.geometry.location;
                    }

                    @Override
                    public void onFailure(final Throwable failure) {
                        Log.d(TAG, "Autocomplete failure " + failure);
                    }
                });
            }
        });




        set_location.setOnClickListener(new View.OnClickListener() {  // vymazenie textu navige to na kliknutie
            @Override
            public void onClick(final View v) {
                if (!fragmentActivity.checkGPSEnable() && mapbox.getMyLocation()==null) {
                    if (isEneableShowText())
                        Toast.makeText(context, context.getResources().getString(R.string.turn_gps), Toast.LENGTH_LONG).show();
                    return ;
                }
                if (fragmentActivity.gps != null || mapbox.getMyLocation()!=null)
                    fragmentActivity.gps.getOnPosition();
                else {
                    if (isEneableShowText())
                        Toast.makeText(context, context.getResources().getString(R.string.not_position), Toast.LENGTH_LONG).show();
                }
            }
        });

        show_address.setOnClickListener(new View.OnClickListener() {  // vymazenie textu navige to na kliknutie
            @Override
            public void onClick(final View v) {
                layoutNavigation.setVisibility(View.VISIBLE);
                layoutNavigation.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.slide_in_left));
            }
        });

        text.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            public void onSwipeLeft() {
                layoutNavigation.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.slide_out_left));
                layoutNavigation.setVisibility(View.INVISIBLE);
                hideKeyboard();
            }

            public void onClick() {
               text.requestFocus();
               InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
               imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        });

        searchLocation.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {

            public void onSwipeLeft() {
                layoutNavigation.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.slide_out_left));
                layoutNavigation.setVisibility(View.INVISIBLE);
                hideKeyboard();
            }

            public void onClick() {
                hideKeyboard();
                text = (EditText) findViewById(R.id.location);
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
                    hideKeyboard();
                    LatLng position = null;
                    String location = text.getText().toString();
                    if (fragmentActivity.isNetworkAvailable(context)) {
                        try {
                            if (positionGPS == null) {
                                address = Route.findLocality(location, getApplicationContext());
                                position = new LatLng(address.getLatitude(), address.getLongitude());
                            } else {
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

        mLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                mDemoSlider.moveNextPosition();
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.EXPANDED ) {
                    if ((isNetworkAvailable(context) && panelState)) {
                        if (panelState) {
                            panelState = false;
                            new Thread() {
                                public void run() {
                                    Looper.prepare();
                                    new GetImageID(fragmentActivity.getActivity(), getApplicationContext(), latitude_photo, longitude_photo, type_photo, mDemoSlider);
                                    Looper.loop();
                                }
                            }.start();
                        }
                    }  else if (!isNetworkAvailable(context) && panelState)
                            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.image_interent), Toast.LENGTH_LONG).show();
                }
            }
        });
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

       /* if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layoutNavigation.setVisibility(View.INVISIBLE);
           //. Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            layoutNavigation.setVisibility(View.VISIBLE);
           // Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }*/
        drawerToggle.onConfigurationChanged(newConfig);
        super.onConfigurationChanged(newConfig);

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

            case R.id.layer:
                if (!fragmentActivity.isNetworkAvailable(context) || mapbox == null) {
                    if (isEneableShowText())
                        Toast.makeText(this,this.getResources().getString(R.string.change_map_style), Toast.LENGTH_LONG).show();
                    return true;
                }
                if ((fragmentActivity==null || fragmentActivity.mapLayer==null) && mapbox.getMyLocation()==null ) {
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
                                        if (lightBumps == null) {
                                            lightBumps = new LatLng(mapbox.getMyLocation().getLatitude(),mapbox.getMyLocation().getLatitude());
                                        }
                                        fragmentActivity.mapLayer.getAllBumps(lightBumps.latitude, lightBumps.longitude);
                                        break;

                                    case 1:
                                        mapbox.setStyleUrl("mapbox://styles/mapbox/satellite-v9");
                                        LatLng satelliteBumps = fragmentActivity.gps.getCurrentLatLng();
                                        if (satelliteBumps == null) {
                                            satelliteBumps = new LatLng(mapbox.getMyLocation().getLatitude(),mapbox.getMyLocation().getLatitude());
                                        }
                                        fragmentActivity.mapLayer.getAllBumps(satelliteBumps.latitude, satelliteBumps.longitude);
                                        break;
                                    case 2:
                                        mapbox.setStyleUrl("mapbox://styles/mapbox/outdoors-v9");
                                        LatLng outdoorsBumps = fragmentActivity.gps.getCurrentLatLng();
                                        if (outdoorsBumps == null) {
                                            outdoorsBumps = new LatLng(mapbox.getMyLocation().getLatitude(),mapbox.getMyLocation().getLatitude());
                                        }
                                        fragmentActivity.mapLayer.getAllBumps(outdoorsBumps.latitude, outdoorsBumps.longitude);

                                        break;
                                }
                            }
                        });
                alert.show();
                return true;

            case R.id.filter: // zobrayenie typu výtlkov
                if (!fragmentActivity.checkGPSEnable() && mapbox.getMyLocation()==null) {
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
                                        if (allBumps == null) {
                                            allBumps = new LatLng(mapbox.getMyLocation().getLatitude(),mapbox.getMyLocation().getLatitude());
                                        }
                                        fragmentActivity.mapLayer.getAllBumps(allBumps.latitude, allBumps.longitude);
                                        break;
                                    case 1:
                                        fragmentActivity.mapLayer.level = MEDIUM_BUMPS;
                                        LatLng mediumBumps = fragmentActivity.gps.getCurrentLatLng();
                                        if (mediumBumps == null) {
                                            mediumBumps = new LatLng(mapbox.getMyLocation().getLatitude(),mapbox.getMyLocation().getLatitude());
                                        }
                                        fragmentActivity.mapLayer.getAllBumps(mediumBumps.latitude, mediumBumps.longitude);
                                        break;
                                    case 2:
                                        fragmentActivity.mapLayer.level = LARGE_BUMPS;
                                        LatLng largeBumps = fragmentActivity.gps.getCurrentLatLng();
                                        if (largeBumps == null) {
                                            largeBumps = new LatLng(mapbox.getMyLocation().getLatitude(),mapbox.getMyLocation().getLatitude());
                                        }
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
    private MenuItem activeMenuItem;
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        if (activeMenuItem != null)
            activeMenuItem.setChecked(false);
        activeMenuItem = item;
        item.setChecked(true);

        drawerLayout.closeDrawers();
        return true;


     //   switch (item.getItemId()) {
         /*   case R.id.calibrate:  // spusti prekalibrovanie aplikácie
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
                if ( fragmentActivity.gps!=null && fragmentActivity.detection!=null ) {
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

                if (f!=null)
                    f.delete();
                f = null;
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
                if (f!=null)
                    f.delete();
                f = null;
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

           case R.id.about:
               showInfo();
               close();
               return true;

            case R.id.exit:
                close();
                if (fragmentActivity.accelerometer!=null) {
                    while (true) {
                        if (updatesLock.tryLock()) {
                            try  {
                                while (true) {
                                    if (lockZoznam.tryLock()) {
                                        try {
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
                                            lockZoznam.unlock();
                                            break;
                                        }
                                    } else {
                                        try {
                                            Thread.sleep(20);
                                        } catch (InterruptedException e) {
                                            e.getMessage();
                                        }
                                    }
                                }
                            }
                            finally {
                                updatesLock.unlock();
                                break;
                            }
                        } else {
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
*/
       /*     default:
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);
                return true;*/
     //   }
    }

    public void showInfo() {

        final TextView web = new TextView(context);
        final SpannableString s = new SpannableString(context.getText(R.string.link_web));
        Linkify.addLinks(s, Linkify.WEB_URLS);
        web.setText(s);
        web.setMovementMethod(LinkMovementMethod.getInstance());
        web.setGravity(Gravity.CENTER_HORIZONTAL);

        SpannableStringBuilder message = new SpannableStringBuilder();
        message.append(new SpannableString(context.getText(R.string.text_about1)));
        message.append(" ");
        SpannableString email= new SpannableString(context.getText(R.string.email));
        email.setSpan(new ForegroundColorSpan(Color.BLUE), 0, context.getText(R.string.email).length(), 0);
        message.append(email);
        message.append(" ");
        message.append(new SpannableString(context.getText(R.string.text_about2)));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(context.getText(R.string.menu_about));
        builder.setMessage(message);
        builder.setView(web);
        builder.setPositiveButton(context.getText(R.string.ok), null);
        AlertDialog dialog = builder.show();
        TextView messageText = (TextView)dialog.findViewById(android.R.id.message);
        messageText.setGravity(Gravity.CENTER);
        TextView titleText = (TextView)dialog.findViewById(android.R.id.title);
        messageText.setGravity(Gravity.CENTER);
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
        private com.mapbox.mapboxsdk.geometry.LatLng  pos = new com.mapbox.mapboxsdk.geometry.LatLng();
        @Override
        public com.mapbox.mapboxsdk.geometry.LatLng evaluate(float fraction, com.mapbox.mapboxsdk.geometry.LatLng startValue, com.mapbox.mapboxsdk.geometry.LatLng endValue) {
            pos.setLatitude(startValue.getLatitude()
                    + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
            pos.setLongitude(startValue.getLongitude()
                    + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
            return pos;
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
        if (i == 0) {
            markerSelectedAuto = true;
            markerAnimator.start();
        }
        else if (i == 1) {
            markerSelectedManual = true;
            markerAnimator.start();
        }
        else  if (i == 2)
            markerSelectedReport = true;
        else  if (i == 3) {
            markerSelectedBumpRepaired = true;
            markerAnimator.start();
        } else  if (i == 4)
            markerSelectedReportRepaired = true;
    }

    private void deselectMarker(final SymbolLayer marker, int i) {
        mDemoSlider.removeAllSliders();
        // animácia na vybraný marker - zmenšenie
        if (marker == null)
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

        if (i == 0) {
            markerSelectedAuto = false;
            markerAnimator.start();
        } else if (i == 1) {
            markerSelectedManual = false;
            markerAnimator.start();
        } else if (i == 2) {
            markerSelectedReport = false;
        } else if (i == 3) {
            markerSelectedBumpRepaired = false;
            markerAnimator.start();
        }else if (i == 4) {
            markerSelectedReportRepaired = false;
        }
    }

    public void isEneableScreen() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean screen = prefs.getBoolean("screen", Boolean.parseBoolean(null));
        if (screen)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void save(boolean save_click) {
        if (!save_click && featureMarker!=null )
            featureMarker.remove();
        featureMarker = null;
        allow_click = false;
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

                    if (mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED && panelState) {
                        new Thread() {
                            public void run() {
                                Looper.prepare();
                                new GetImageID(fragmentActivity.getActivity(), getApplicationContext(), latitude_photo, longitude_photo, type_photo, mDemoSlider);
                                Looper.loop();
                            }
                        }.start();
                    }

                    if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        manager.setConnected(true);
                    }
                    if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                        if (isEneableOnlyWifiMap()) {
                            manager.setConnected(false);
                        }
                        else {
                            manager.setConnected(true);
                        }
                    }
                }
            }
        }
    };

    public boolean isEneableOnlyWifiMap() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean("map", Boolean.parseBoolean(null));
    }

    public Dialog onCreateDialogSingleChoice() {
        freeMemory();
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
                                select_iteam_text= "shaft";
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
                                                if (!regionNameEdit.getText().toString().isEmpty())
                                                    select_iteam_text = regionNameEdit.getText().toString();
                                                else
                                                    select_iteam_text = "Other";
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
            case R.id.infosssss:
               new SimpleTooltip.Builder(this)
                        .anchorView(v)
                        .text("Texto do Tooltip")
                        .gravity(Gravity.BOTTOM)
                        .animated(true)
                        .transparentOverlay(false)
                        .build()
                        .show();
                break;
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

                    if (f != null)
                        save_photo(String.valueOf(location.getLatitude()),String.valueOf(location.getLongitude()), String.valueOf(select_iteam) ,f.getPath());
                    f = null;
                }

                break;
            case R.id.delete_btn:   // mazania označeného markeru

                if (f!=null)
                    f.delete();
                f = null;
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

    public void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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
        return name;
    }

    public void setPanel(String text, final String lat, final String ltn, final String type,final  String info) {
        freeMemory();
        TextView t = (TextView) findViewById(R.id.info);
        t.setText(text);
        latitude_photo = lat;
        longitude_photo = ltn;
        type_photo = type;
        panelState = true;
        Button add = (Button) findViewById(R.id.add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               onPickImage( getCurrentFocus(),PICK_IMAGE_ID);
            }
        });

        Button increase = (Button) findViewById(R.id.increase);
        increase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Location location = new Location("new");
                location.setLatitude(Double.parseDouble(lat));
                location.setLongitude(Double.parseDouble(ltn));
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string. increase_number), Toast.LENGTH_LONG).show();
                add_bump(location, 6, info ,Integer.parseInt(type));
            }
        });
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
                    File sdCard = Environment.getExternalStorageDirectory();
                    File dir = new File(sdCard.getAbsolutePath() + "/.Detector");
                    if(!dir.exists()){
                        dir.mkdirs();
                    }
                    f = new File(new File(String.valueOf(dir)), bitmap.toString() + ".png");
                    try {
                        f.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                    byte[] bitmapdata = bos.toByteArray();

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
                    textSliderView
                            .description(date)
                            .image(f)
                            .setScaleType(BaseSliderView.ScaleType.Fit)
                            .setOnSliderClickListener(this);
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

                       save_photo(latitude_photo,longitude_photo, type_photo,f.getPath());
                }
                break;

            case PICK_IMAGE_ADD_ID:
                Bitmap bitmapa = ImagePicker.getImageFromResult(this, resultCode, data);
                if (bitmapa!=null) {
                    File sdCard = Environment.getExternalStorageDirectory();
                    File dir = new File(sdCard.getAbsolutePath() + "/.Detector");
                    if(!dir.exists()){
                        dir.mkdirs();
                    }

                    f = new File(new File(String.valueOf(dir)), bitmapa.toString() + ".png");
                    try {
                        f.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmapa.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                    byte[] bitmapdata = bos.toByteArray();

                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(f);

                        fos.write(bitmapdata);
                        fos.flush();
                        fos.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
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
                                    try {
                                        fragmentActivity.accelerometer.addPossibleBumps(location, (float) round(intensity,6));
                                        fragmentActivity.accelerometer.addBumpsManual(1);
                                        fragmentActivity.accelerometer.addtextDetect(text);
                                        fragmentActivity.accelerometer.addtypeDetect(type);
                                        if (updatesLock.tryLock()) {
                                            try  {
                                                DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(context);
                                                SQLiteDatabase database = databaseHelper.getReadableDatabase();

                                                fragmentActivity.checkIntegrityDB(database);
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
                                                updatesLock.unlock();
                                            }
                                        }
                                    }
                                    finally {
                                        flag =true;
                                        lockZoznam.unlock();
                                    }
                                }
                            }
                            finally {
                                lockAdd.unlock();
                                if (flag) {
                                    threadLock.getAndSet(false);
                                    break;
                                }
                            }
                        }
                        threadLock.getAndSet(false);
                    }
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

    public static String getDate(long milliSeconds, String dateFormat)  {
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    public void save_photo(String latitude, String longitude,String type, String path) {
        while (true) {
            if (updatesLock.tryLock()) {
                try {
                    DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(this);
                    SQLiteDatabase sb = databaseHelper.getWritableDatabase();
                    fragmentActivity.checkIntegrityDB(sb);
                    sb.beginTransaction();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(Provider.photo.LATITUDE, latitude);
                    contentValues.put(Provider.photo.LONGTITUDE, longitude);
                    contentValues.put(Provider.photo.TYPE, type);
                    contentValues.put(Provider.photo.PATH, path);
                    contentValues.put(Provider.photo.CREATED_AT, getDate(new Date().getTime(), "yyyy-MM-dd HH:mm:ss"));
                    sb.insert(Provider.photo.TABLE_NAME_PHOTO, null, contentValues);
                    sb.setTransactionSuccessful();
                    sb.endTransaction();
                    sb.close();
                    databaseHelper.close();
                    fragmentActivity.checkCloseDb(sb);
                    f = null;
                }
                finally {
                    updatesLock.unlock();
                    break;
                }
            }  else {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.getMessage();
                }
            }
        }
    }
}
