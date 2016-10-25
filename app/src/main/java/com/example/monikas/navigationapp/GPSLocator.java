package com.example.monikas.navigationapp;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;


import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;

import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.PolylineOptions;

import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import org.w3c.dom.Document;

import java.util.ArrayList;

import static com.example.monikas.navigationapp.FragmentActivity.setOnPosition;
import static com.example.monikas.navigationapp.FragmentActivity.global_MapFragment;
import static com.example.monikas.navigationapp.FragmentActivity.global_mGoogleApiClient;

import static com.example.monikas.navigationapp.MainActivity.mapbox;

import android.animation.TypeEvaluator;


/**
 * Created by monikas on 24. 3. 2015.
 */
public class GPSLocator extends Service implements LocationListener,  MapboxMap.OnMyLocationChangeListener{

    private Location mCurrentLocation;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap map;
    private boolean navigation;
    private PolylineOptions road;
    private float level;
    private com.mapbox.mapboxsdk.geometry.LatLng latLng;
    private com.mapbox.mapboxsdk.annotations.Marker marker;

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

    public com.mapbox.mapboxsdk.geometry.LatLng setUpMap(boolean value) {

        if (!value) {
            //////////
            mapbox.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                @Override
                public void onMapClick(@NonNull com.mapbox.mapboxsdk.geometry.LatLng point) {
                }
                });

             if (marker != null) {
                 marker.remove();
                // mapbox.removeMarker();
             }
         return  latLng;
        }
        else {
            latLng = null;
             marker=null;
            mapbox.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                @Override
                public void onMapClick(@NonNull com.mapbox.mapboxsdk.geometry.LatLng point) {
                    latLng = point;

                    if (marker != null) {   // odstranenie predchadzajuceho markera
                       // marker.remove();
                        ValueAnimator markerAnimator = ObjectAnimator.ofObject(marker, "position",
                                new LatLngEvaluator(), marker.getPosition(), point);
                        markerAnimator.setDuration(2000);
                        markerAnimator.start();

                    } else {
                        IconFactory iconFactory = IconFactory.getInstance(GPSLocator.this);
                        Drawable iconDrawable = ContextCompat.getDrawable(GPSLocator.this, R.drawable.green_icon);
                        com.mapbox.mapboxsdk.annotations.Icon icons = iconFactory.fromDrawable(iconDrawable);
                        marker = mapbox.addMarker(new com.mapbox.mapboxsdk.annotations.MarkerOptions().title("Selected point")
                                .position(point)
                                .icon(icons));
                    }
                }


            });

            return null;





          /*  map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

                @Override
                public void onMapClick(LatLng point) {

                    latLng = point;

                    if (marker != null) {   // odstranenie predchadzajuceho markera
                        marker.remove();
                    }

                     // vytvorenie markera
                     marker = map.addMarker(new com.google.android.gms.maps.model.MarkerOptions().position(point).title("Selected point")
                         .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));

                   IconFactory iconFactory = IconFactory.getInstance(GPSLocator.this);
                    Drawable iconDrawable = ContextCompat.getDrawable(GPSLocator.this, R.drawable.red_icon);
                    com.mapbox.mapboxsdk.annotations.Icon icons = iconFactory.fromDrawable(iconDrawable);
                    mapbox.addMarker(new com.mapbox.mapboxsdk.annotations.MarkerOptions().title("Selected point")
                            .icon(icons));

                }
            });
            return null;*/
        }
     }

    private static class LatLngEvaluator implements TypeEvaluator<com.mapbox.mapboxsdk.geometry.LatLng> {
        // Method is used to interpolate the marker animation.

        private com.mapbox.mapboxsdk.geometry.LatLng  aa = new com.mapbox.mapboxsdk.geometry.LatLng();

        @Override
        public com.mapbox.mapboxsdk.geometry.LatLng evaluate(float fraction, com.mapbox.mapboxsdk.geometry.LatLng startValue, com.mapbox.mapboxsdk.geometry.LatLng endValue) {
            aa.setLatitude(startValue.getLatitude()
                    + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
            aa.setLongitude(startValue.getLongitude()
                    + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
            return aa;
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
        com.mapbox.mapboxsdk.geometry.LatLng[] points = new com.mapbox.mapboxsdk.geometry.LatLng[directionPoint.size()];
        for(int i = 0 ; i < directionPoint.size() ; i++) {
            road.add(directionPoint.get(i));
            points[i] = new com.mapbox.mapboxsdk.geometry.LatLng(
                    directionPoint.get(i).latitude,
                    directionPoint.get(i).longitude);
        }
        mapbox.addPolyline(new com.mapbox.mapboxsdk.annotations.PolylineOptions()
                .add(points)
                .color(Color.parseColor("#009688"))
                .width(5));
        updateMap();

    }

    //icon dowloaded from http://www.flaticon.com/free-icon/map-pin_34493
    public void addBumpToMap (com.mapbox.mapboxsdk.geometry.LatLng  position, int count, int manual ) {
        if (position == null) {
            return;
        }
        IconFactory iconFactory = IconFactory.getInstance(GPSLocator.this);
        if (manual == 0 ) {

            Drawable iconDrawable = ContextCompat.getDrawable(GPSLocator.this, R.drawable.red_icon);
            com.mapbox.mapboxsdk.annotations.Icon icons = iconFactory.fromDrawable(iconDrawable);
            mapbox.addMarker(new com.mapbox.mapboxsdk.annotations.MarkerOptions()
                    .position(new com.mapbox.mapboxsdk.geometry.LatLng(position.getLatitude(), position.getLongitude()))
                    .title("Number of detections: " + count )
                    .icon(icons));
        }
        else {
            Drawable iconDrawable = ContextCompat.getDrawable(GPSLocator.this, R.drawable.green_icon);
            com.mapbox.mapboxsdk.annotations.Icon icons = iconFactory.fromDrawable(iconDrawable);
            mapbox.addMarker(new com.mapbox.mapboxsdk.annotations.MarkerOptions()
                    .position(new com.mapbox.mapboxsdk.geometry.LatLng(position.getLatitude(), position.getLongitude()))
                    .title("Manually added " +
                            "Number of detections:" + count )
                    .icon(icons));
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

        if (location!=null && setOnPosition) {
            try {
                mapbox.easeCamera(com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLng(new com.mapbox.mapboxsdk.geometry.LatLng(getmCurrentLocation().getLatitude(), getmCurrentLocation().getLongitude())));
            } catch  (NullPointerException e) {
            }
        }
    }

    /*
     Pridanie markeru, ešte to využijem
     for(int i=0; i < 10000; i++)
            mapbox.addMarker(new com.mapbox.mapboxsdk.annotations.MarkerOptions()
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
