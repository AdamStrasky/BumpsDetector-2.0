package navigationapp.main_application;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.model.LatLng;

import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.util.List;

import navigationapp.error.ExceptionHandler;

import static navigationapp.main_application.FragmentActivity.global_mGoogleApiClient;
import static navigationapp.main_application.MainActivity.ZOOM_LEVEL;
import static navigationapp.main_application.MainActivity.mapbox;
import static navigationapp.main_application.MapManager.setOnPosition;

public class GPSLocator extends Service implements LocationListener,  MapboxMap.OnMyLocationChangeListener{

    private Location mCurrentLocation=null;
    private GoogleApiClient mGoogleApiClient;
    private final String TAG = "GPSLocator";
    private boolean draw_road= false;

    public GPSLocator () {
        this.mGoogleApiClient = global_mGoogleApiClient;
        startLocationUpdates();
    }

    //vykresli cestu
    public void showDirection (final List<LatLng> directionPoint) {
        Log.d(TAG, "showDirection - vykreslujem cestu");
        new Thread() {
            public void run() {
                Looper.prepare();
        com.mapbox.mapboxsdk.geometry.LatLng[] points = new com.mapbox.mapboxsdk.geometry.LatLng[directionPoint.size()];
        for(int i = 0 ; i < directionPoint.size() ; i++) {
            points[i] = new com.mapbox.mapboxsdk.geometry.LatLng(
                    directionPoint.get(i).latitude,
                    directionPoint.get(i).longitude);
        }

        draw_road = true;
        mapbox.addPolyline(new PolylineOptions()
                .add(points)
                .color(Color.parseColor("#009688"))
                .width(5));
                Looper.loop();
            }
        }.start();
    }

    public void remove_draw_road () { // maže vykreslenu cestu
       Log.d(TAG, "remove_draw_road");
       if (draw_road) {
           if(mapbox!=null) {
               List<Polyline> markers =  mapbox.getPolylines();
               for (int i = 0; i < markers.size(); i++) {
                   mapbox.removePolyline(markers.get(i));
               }
            draw_road=false;
           }
       }
    }

    public Location getmCurrentLocation() {   // vracia aktuálnu polohu
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
        Log.d(TAG, " change GPS position");
        if (location!=null && location.hasAccuracy())
            mCurrentLocation = location;
        else
            mCurrentLocation=null;

      if (location!=null && setOnPosition &&  MainActivity.isActivityVisible()) {   // nastavujem kameru ak zmenim pozíciu
            try {
                if (mapbox!=null)
                mapbox.easeCamera(com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLng(new com.mapbox.mapboxsdk.geometry.LatLng(getmCurrentLocation().getLatitude(), getmCurrentLocation().getLongitude())));
            } catch  (NullPointerException e) {
                e.getMessage();
            }
            if (ZoomInit)
               SetZoom();
      }
    }
    private boolean ZoomInit = true;

    public void SetZoom() {    // nastavujem kam sa ma presunúť kamera s akým zoomov
        ZoomInit = false;
        if (mapbox!=null  && getmCurrentLocation()!=null) {
         final   com.mapbox.mapboxsdk.geometry.LatLng yourLatLng = new com.mapbox.mapboxsdk.geometry.LatLng(getmCurrentLocation().getLatitude(), getmCurrentLocation().getLongitude());
            if (mapbox != null && yourLatLng != null) {
                final CameraPosition position = new CameraPosition.Builder()
                        .target(yourLatLng)
                        .zoom(ZOOM_LEVEL)
                        .build();
                mapbox.animateCamera(CameraUpdateFactory.newCameraPosition(position), 6000,
                        new MapboxMap.CancelableCallback() {
                            @Override
                            public void onCancel() {
                                mapbox.animateCamera(CameraUpdateFactory.newCameraPosition(position), 6000);
                                Log.d("XXXXX","SetZoom onCancel ");
                            }

                            @Override
                            public void onFinish() {
                                Log.d("XXXXX","SetZoom onFinish ");
                                mapbox.animateCamera(CameraUpdateFactory.newCameraPosition(position), 6000);
                             }
                        });
            }
        }
    }

    public void getOnPosition() {
        ZoomInit = true;
        SetZoom();
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
