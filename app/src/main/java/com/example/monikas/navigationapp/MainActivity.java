package com.example.monikas.navigationapp;

import android.app.FragmentManager;
import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.HashMap;

public class MainActivity extends ActionBarActivity {

    private GoogleApiClient mGoogleApiClient;
    private Context context;
    private final float ALL_BUMPS = 1.0f;
    private final float MEDIUM_BUMPS = 1.5f;
    private final float LARGE_BUMPS = 2.5f;
    public static int ZOOM_LEVEL = 18;
    private FragmentActivity fragmentActivity;
    public static final String FRAGMENTACTIVITY_TAG = "blankFragment";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final EditText searchBar = (EditText) findViewById(R.id.location);
        searchBar.requestFocus();
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

        FragmentManager fragmentManager = getFragmentManager();
        fragmentActivity = (FragmentActivity) fragmentManager.findFragmentByTag(FRAGMENTACTIVITY_TAG);
        if (fragmentActivity == null) {
            fragmentActivity = new FragmentActivity();
            fragmentManager.beginTransaction()
                    .add(fragmentActivity, FRAGMENTACTIVITY_TAG)
                    .commit();
        }

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
                    Toast.makeText(this, "Unable to find location, wrong name!", Toast.LENGTH_LONG).show();

                }
                else {
                    LatLng to_position = new LatLng(address.getLatitude(),address.getLongitude());
                    LatLng myPosition = new LatLng(fragmentActivity.gps.getmCurrentLocation().getLatitude(), fragmentActivity.gps.getmCurrentLocation().getLongitude());
                    fragmentActivity.gps.goTo(myPosition, ZOOM_LEVEL);
                    fragmentActivity.gps.showDirection(myPosition, to_position);
                    fragmentActivity.gps.setNavigation(true);
                }
            }
            catch (Exception e) {
                Toast.makeText(this, "Unable to find location!", Toast.LENGTH_LONG).show();
            }
        }
        else {
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

            case R.id.satellite:
                fragmentActivity.gps.getMap().setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                return true;

            case R.id.normal:
                fragmentActivity.gps.getMap().setMapType(GoogleMap.MAP_TYPE_NORMAL);
                return true;

            case R.id.clear_map:
                fragmentActivity. gps.setRoad(null);
                fragmentActivity.gps.getMap().clear();
                return true;

            case R.id.calibrate:
                fragmentActivity.accelerometer.calibrate();
                Toast.makeText(context,"Your phone was calibrated.",Toast.LENGTH_SHORT).show();
                return true;

            case R.id.navigation:
                fragmentActivity.gps.setNavigation(false);
                EditText text = (EditText) findViewById(R.id.location);
                text.setText("Navigate to...");
                return true;

            case R.id.all_bumps:
                fragmentActivity.level = ALL_BUMPS;
                fragmentActivity.getBumpsWithLevel();
                return true;

            case R.id.medium_bumps:
                fragmentActivity.level = MEDIUM_BUMPS;
                fragmentActivity.getBumpsWithLevel();
                return true;

            case R.id.large_bumps:
                fragmentActivity.level = LARGE_BUMPS;
                fragmentActivity.getBumpsWithLevel();
                return true;

            case R.id.exit:
                if (fragmentActivity.isNetworkAvailable()) {
                     new SensorEventLoggerTask().execute();
                }
                onDestroy();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class SensorEventLoggerTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            for (HashMap<Location, Float> bump : fragmentActivity.accelerometer.getPossibleBumps()) {
                fragmentActivity.saveBump(bump);
            }
            fragmentActivity.accelerometer.getPossibleBumps().clear();
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    protected void onResume() {
        super.onResume();
        Log.d("III","znova zapnutie obrazovky ");
        if (fragmentActivity.gps !=null ) {
            Log.d("III","zvzkonalo sa  ");
            LatLng myPosition = new LatLng(fragmentActivity.gps.getmCurrentLocation().getLatitude(), fragmentActivity.gps.getmCurrentLocation().getLongitude());
            fragmentActivity.gps.goTo(myPosition, ZOOM_LEVEL);
        }
    }

}
