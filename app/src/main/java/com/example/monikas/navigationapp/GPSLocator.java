package com.example.monikas.navigationapp;


import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
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

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

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
        new Get_Bumps().execute("http://sport.fiit.ngnlab.eu/get_bumps.php");
        new Get_Collisions().execute("http://sport.fiit.ngnlab.eu/get_collisions.php");
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
            //int manual=1;
            //parametre su aktualna pozicia a level vytlkov, ktore chceme zobrazit
            params.add(new BasicNameValuePair("latitude", latitude));
            params.add(new BasicNameValuePair("longitude", longitude));
            params.add(new BasicNameValuePair("level", String.valueOf(level)));
            //   params.add(new BasicNameValuePair("manual", String.valueOf(manual))); // neviem či použijem

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
                int hj;
                int manual = 0;
                if (c != null) {
                    try {
                        latitude = c.getDouble("latitude");
                        longitude = c.getDouble("longitude");
                        count = c.getInt("count");
                        if (c.isNull("manual")) {
                            manual = 0;
                        } else
                            manual = c.getInt("manual");

                        //vytlk sa prida do mapy*/
                        addBumpToMap(new LatLng(latitude, longitude), count, manual);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
        class Get_Bumps extends AsyncTask<String, Void, JSONArray> {

            private JSONParser jsonParser = new JSONParser();

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            protected JSONArray doInBackground(String... args) {
                JSONObject json = jsonParser.makeHttpRequest(args[0], "GET", null);
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
                    int index = 0, count = 0, rating = 0, b_id = 0, manual = 0;
                    String last_modified = null;
                    if (c != null) {
                        try {
                            b_id = c.getInt("b_id");
                            rating = c.getInt("rating");
                            count = c.getInt("count");
                            last_modified = c.getString("last_modified");
                            latitude = c.getDouble("latitude");
                            longitude = c.getDouble("longitude");
                            if (c.isNull("manual")) {
                                manual = 0;
                            } else
                                manual = c.getInt("manual");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

            class Get_Collisions extends AsyncTask<String, Void, JSONArray> {

                private JSONParser jsonParser = new JSONParser();

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                }

                protected JSONArray doInBackground(String... args) {
                    JSONObject json = jsonParser.makeHttpRequest(args[0], "GET", null);
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
                        double intensity = 0;
                        int  c_id =0, b_id=0;
                        String created_at = null;
                        if (c != null) {
                            try {
                                b_id= c.getInt("b_id");
                                c_id= c.getInt("c_id");
                                created_at = c.getString("created_at");
                                intensity = c.getDouble("intensity");

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
        }

}
