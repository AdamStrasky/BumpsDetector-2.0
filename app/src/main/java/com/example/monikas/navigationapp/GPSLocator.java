package com.example.monikas.navigationapp;


import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.example.monikas.navigationapp.FragmentActivity.global_MapFragment;
import static com.example.monikas.navigationapp.FragmentActivity.global_mGoogleApiClient;

/**
 * Created by monikas on 24. 3. 2015.
 */
public class GPSLocator extends Service implements LocationListener {

    private Location mCurrentLocation;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap map;
    private boolean navigation;
    private JSONArray bumps;
    private PolylineOptions road;
    private float level;

    private GoogleMap mMap;
    private LatLng latLng;
    private Marker marker;
    Geocoder geocoder;


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

        geocoder = new Geocoder(this, Locale.getDefault());
     //   if (map != null) {
       //     setUpMap();
        //}
       // setUpMapIfNeeded();
    }


    /*private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.

        }
    }*/

    public void setUpMap() {

      //  map.setMyLocationEnabled(true);
      //  map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
      //  map.getUiSettings().setMapToolbarEnabled(false);

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {
                //save current location
                latLng = point;

                List<Address> addresses = new ArrayList<>();
                try {
                    addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                android.location.Address address = addresses.get(0);

                if (address != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                        sb.append(address.getAddressLine(i) + "\n");
                    }
                    //   Toast.makeText(MainActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
                }

                //remove previously placed Marker
                if (marker != null) {
                    marker.remove();
                }

                //place marker where user just clicked
                marker = map.addMarker(new MarkerOptions().position(point).title("Marker")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));

              /*  AlertDialog.Builder builderInner = new AlertDialog.Builder(GPSLocator.this);
                //   builderInner.setMessage(strName);
                builderInner.setTitle("Select on map a bump");

                builderInner.setPositiveButton(
                        "Ok",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialog,
                                    int which) {


                                dialog.dismiss();
                            }
                        });

                builderInner.setNegativeButton(
                        "cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builderInner.show();*/

            }
        });


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
    public void addBumpToMap (LatLng position, int count) {
        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.bump);
        map.addMarker(new MarkerOptions()
                .alpha(0.8f)
                .flat(false)
                .position(position)                                                                        // at the location you needed
                .title("Number of detections: " + count)
                .icon(icon));

    }


    public Location getmCurrentLocation() {
        return mCurrentLocation;
    }

    public LatLng getCurrentLatLng() {
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
        }


    public void updateMap () {
        map.clear();
        //ak je nejaka trasa, vykresli ju
        if (road != null) map.addPolyline(road);
        new GetAllBumps().execute();
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
    public class LocalBinder extends Binder {
        public GPSLocator getService() {
            Log.d("BIND_GPS", "GPS service ONDESTROY");
            return GPSLocator.this;
        }
    }

    class GetAllBumps extends AsyncTask<String, Void, JSONArray> {

        private JSONParser jsonParser = new JSONParser();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected JSONArray doInBackground(String... args) {

            String url_all_bumps = "http://sport.fiit.ngnlab.eu/get_all_bumps.php";
            String latitude = String.valueOf(getmCurrentLocation().getLatitude());
            String longitude = String.valueOf(getmCurrentLocation().getLongitude());
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            //parametre su aktualna pozicia a level vytlkov, ktore chceme zobrazit
            params.add(new BasicNameValuePair("latitude", latitude));
            params.add(new BasicNameValuePair("longitude", longitude));
            params.add(new BasicNameValuePair("level", String.valueOf(level)));
            JSONObject json = jsonParser.makeHttpRequest(url_all_bumps, "POST", params);
            try {
                int success = json.getInt("success");
                if (success == 1) {
                    bumps = json.getJSONArray("bumps");
                    //v pripade uspechu nam poziadavka vrati zoznam vytlkov
                    return bumps;
                } else {
                    return null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(JSONArray array) {

            for (int i = 0; i < bumps.length(); i++) {
                JSONObject c = null;
                try {
                    c = bumps.getJSONObject(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                double latitude = 0;
                double longitude = 0;
                int count = 0;
                if (c != null) {
                    try {
                        latitude = c.getDouble("latitude");
                        longitude = c.getDouble("longitude");
                        count = c.getInt("count");
                        //vytlk sa prida do mapy
                        addBumpToMap(new LatLng(latitude, longitude), count);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }



}
