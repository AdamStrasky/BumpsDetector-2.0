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
import com.google.android.gms.maps.model.LatLng;

import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import org.w3c.dom.Document;

import java.util.ArrayList;
import android.animation.TypeEvaluator;

import static com.example.monikas.navigationapp.FragmentActivity.setOnPosition;
import static com.example.monikas.navigationapp.FragmentActivity.global_mGoogleApiClient;
import static com.example.monikas.navigationapp.MainActivity.ZOOM_LEVEL;
import static com.example.monikas.navigationapp.MainActivity.mapbox;


/**
 * Created by monikas on 24. 3. 2015.
 */
public class GPSLocator extends Service implements LocationListener,  MapboxMap.OnMyLocationChangeListener{

    private Location mCurrentLocation=null;
    private GoogleApiClient mGoogleApiClient;
    private float level;
    private com.mapbox.mapboxsdk.geometry.LatLng latLng;
    private com.mapbox.mapboxsdk.annotations.Marker marker;

    public GPSLocator () {
        this.mGoogleApiClient = global_mGoogleApiClient;
        startLocationUpdates();
    }

    public com.mapbox.mapboxsdk.geometry.LatLng setUpMap(boolean value) {

        if (!value) {
            mapbox.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                @Override
                public void onMapClick(@NonNull com.mapbox.mapboxsdk.geometry.LatLng point) {

                }
                });

             if (marker != null) {
                 marker.remove();
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

                    if (marker != null) {
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

    public void setLevel(float level) {
        this.level = level;
    }

    //vykresli cestu z miesta from do miesta to
    public void showDirection (LatLng from, LatLng to) {
         Route md = new Route();
         Document doc = md.getDocument(from, to);
         //ArrayList<LatLng> directionPoint = md.getDirection(doc);
      //   com.mapbox.mapboxsdk.geometry.LatLng[] points = new com.mapbox.mapboxsdk.geometry.LatLng[directionPoint.size()];
        /* for(int i = 0 ; i < directionPoint.size() ; i++) {
            points[i] = new com.mapbox.mapboxsdk.geometry.LatLng(
                    directionPoint.get(i).latitude,
                    directionPoint.get(i).longitude);
         }*/

       /* mapbox.addPolyline(new com.mapbox.mapboxsdk.annotations.PolylineOptions()
                .add(points)
                .color(Color.parseColor("#009688"))
                .width(5));*/
    }

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
        if (location!=null && location.hasAccuracy())
            mCurrentLocation = location;
        else
            mCurrentLocation=null;

        if (location!=null && setOnPosition &&  MainActivity.isActivityVisible()) {
            try {
                mapbox.easeCamera(com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLng(new com.mapbox.mapboxsdk.geometry.LatLng(getmCurrentLocation().getLatitude(), getmCurrentLocation().getLongitude())));
            } catch  (NullPointerException e) {
            }
            if (ZoomInit)
               SetZoom();
        }

    }
    private boolean ZoomInit = true;
    public void SetZoom() {
        ZoomInit = false;
        com.mapbox.mapboxsdk.geometry.LatLng yourLatLng = new com.mapbox.mapboxsdk.geometry.LatLng(getmCurrentLocation().getLatitude(), getmCurrentLocation().getLongitude());
        mapbox.setCameraPosition(new CameraPosition.Builder()
                .target(yourLatLng )
                .zoom(ZOOM_LEVEL)
                .build());
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
