package com.example.monikas.navigationapp;


import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

import static com.example.monikas.navigationapp.FragmentActivity.flagMap;
import static com.example.monikas.navigationapp.FragmentActivity.global_MapFragment;
import static com.example.monikas.navigationapp.FragmentActivity.global_mGoogleApiClient;

import static com.example.monikas.navigationapp.FragmentActivity.mapboxik;
import static com.example.monikas.navigationapp.MainActivity.mapView;

/**
 * Created by monikas on 24. 3. 2015.
 */
public class GPSLocator extends Service implements LocationListener,  MapboxMap.OnMyLocationChangeListener{

    private Location mCurrentLocation;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap map;
    private boolean navigation;
    private JSONArray bumps;
    private PolylineOptions road;
    private float level;
    private LatLng latLng;
    private Marker marker;

    public GPSLocator () {

        MapFragment fragment = global_MapFragment ;
        this.mGoogleApiClient = global_mGoogleApiClient;

        if (map == null) {
            map = fragment.getMap();
            map.setBuildingsEnabled(true);
            map.setMyLocationEnabled(true);
            map.setTrafficEnabled(true);
            startLocationUpdates();
        }
    }

    public LatLng setUpMap(boolean value) {

        if (!value) {
             map.setOnMapClickListener(null);
             if (marker != null) {
                marker.remove();
             }
         return  latLng;
        }
        else {
            latLng = null;
            map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

                @Override
                public void onMapClick(LatLng point) {

                    latLng = point;

                    if (marker != null) {   // odstranenie predchadzajuceho markera
                        marker.remove();
                    }

                     // vytvorenie markera
                     marker = map.addMarker(new MarkerOptions().position(point).title("Selected point")
                         .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));

                }
            });
            return null;
        }
     }


    public void setRoad(PolylineOptions road) {
        this.road = road;
    }

    public void setLevel(float level) {
        this.level = level;
    }

    public boolean isNavigation() {
        return navigation;
    }

    public void setNavigation(boolean navigation) {
        this.navigation = navigation;
    }

    public GoogleMap getMap() {
        return map;
    }


    //mapa ukaze na toto miesto
    public void goTo (LatLng latlong, float zoom) {
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latlong, zoom);
            map.moveCamera(update);
     }

    //vykresli cestu z miesta from do miesta to
    public void showDirection (LatLng from, LatLng to) {

        Route md = new Route();
        Document doc = md.getDocument(from, to);
        ArrayList<LatLng> directionPoint = md.getDirection(doc);
        road = new PolylineOptions().width(27).color(Color.MAGENTA);

        for(int i = 0 ; i < directionPoint.size() ; i++) {
            road.add(directionPoint.get(i));
        }
        updateMap();

    }

    //icon dowloaded from http://www.flaticon.com/free-icon/map-pin_34493
    public void addBumpToMap (LatLng position, int count, int manual ) {
        if (position == null) {
            return;
        }


          /////////////////////////////////////////////////////
        if (manual == 0 ) {
            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.bump);
            map.addMarker(new MarkerOptions()
                    .alpha(0.8f)
                    .flat(false)
                    .position(position)                                                                        // at the location you needed
                    .title("Number of detections: " + count )
                    .icon(icon));
      }
        else {
            BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA);
            map.addMarker(new MarkerOptions()
                    .alpha(0.8f)
                    .flat(false)
                    .position(position)                                                                        // at the location you needed
                    .title("Manually added " +
                            "Number of detections:" + count )
                    .icon(icon));

       }

    }


    public Location getmCurrentLocation() {
        return mCurrentLocation;
    }

    public LatLng getCurrentLatLng() {
        if (mCurrentLocation == null )
            return null ;
            else
      return new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());
    }


    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, createLocationRequest(), this);
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    public void onLocationChanged(Location location) {
        Log.d("GPS", " change GPS position");
        mCurrentLocation = location;
        //ak je zapnuta navigacia, obrazovka sa hybe spolu s meniacou sa polohou]

        if (isNavigation() && MainActivity.isActivityVisible())
            goTo(new LatLng(getmCurrentLocation().getLatitude(), getmCurrentLocation().getLongitude()), MainActivity.ZOOM_LEVEL);

        if (location!=null && flagMap) {
            try {
                mapboxik.easeCamera(com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLng(new com.mapbox.mapboxsdk.geometry.LatLng(getmCurrentLocation().getLatitude(), getmCurrentLocation().getLongitude())));

            } catch (NullPointerException e) {
            }
        }


    }
    /*
     for(int i=0; i < 10000; i++)
            mapboxik.addMarker(new com.mapbox.mapboxsdk.annotations.MarkerOptions()
                .position(new com.mapbox.mapboxsdk.geometry.LatLng(49.2046277+i, 18.8356887))
                .title("Hello World!")
                .snippet("Welcome to my marker."));
     */


    public void updateMap () {
        map.clear();
        //ak je nejaka trasa, vykresli ju
        if (road != null) map.addPolyline(road);
    }

   public IBinder onBind(Intent intent) {
        Log.d("BIND_GPS", "GPS Loc service ONBIND");
        return mBinder;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("BIND_GPS", "GPS Loc service ONSTARTCOMMAND");
        return START_STICKY;
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onMyLocationChange(@Nullable Location location) {

    }

    public class LocalBinder extends Binder {
        public GPSLocator getService() {
            Log.d("BIND_GPS", "GPS service ONDESTROY");
            return GPSLocator.this;
        }
    }
}
