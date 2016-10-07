package com.example.monikas.navigationapp;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static com.example.monikas.navigationapp.DatabaseOpenHelper.DATABASE_NAME;
import static com.example.monikas.navigationapp.Provider.bumps_detect.TABLE_NAME_BUMPS;

public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private GoogleApiClient mGoogleApiClient;
    private Context context;
    private final float ALL_BUMPS = 1.0f;
    private final float MEDIUM_BUMPS = 1.5f;
    private final float LARGE_BUMPS = 2.5f;
    public static int ZOOM_LEVEL = 18;
    private  FragmentActivity fragmentActivity;
    public static final String FRAGMENTACTIVITY_TAG = "blankFragment";
    private static boolean activityVisible=true;
    public static final String PREF_FILE_NAME = "Settings";
    private Float intensity = null;
    LinearLayout confirm;
    Button add_button, save_button, delete_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final EditText searchBar = (EditText) findViewById(R.id.location);
        add_button = (Button) findViewById(R.id.add_button);
        save_button = (Button) findViewById(R.id.save_btn);
        delete_button = (Button) findViewById(R.id.delete_btn);
        confirm = (LinearLayout) findViewById(R.id.confirm);
        confirm.setVisibility(View.INVISIBLE);
        confirm.setOnClickListener(this);
        save_button.setOnClickListener(this);
        delete_button.setOnClickListener(this);
        add_button.setOnClickListener(this);



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

    public GoogleApiClient getmGoogleApiClient() {
        return mGoogleApiClient;
    }

    public void setmGoogleApiClient(GoogleApiClient mGoogleApiClient) {
        this.mGoogleApiClient = mGoogleApiClient;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_button:
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this);
                builderSingle.setTitle("Select type of bump");
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                        MainActivity.this,android.R.layout.select_dialog_singlechoice);
                arrayAdapter.add("Large");
                arrayAdapter.add("Medium");
                arrayAdapter.add("Normal");

                builderSingle.setNegativeButton(
                        "Cancel",
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
                                if (select== 0)
                                    intensity =10.f;
                                else if (select== 0)
                                    intensity =6.f;
                                else
                                    intensity =0.f;
                                // spustenie listenera na mapu
                                fragmentActivity.gps.setUpMap(true);
                                confirm.setVisibility(View.VISIBLE);
                                add_button.setVisibility(View.INVISIBLE);

                            }
                        });
                builderSingle.show();
                break;

            case R.id.save_btn:
                add_button.setVisibility(View.VISIBLE);
                confirm.setVisibility(View.INVISIBLE);
                // vrati polohu  kde som stlačil na mape
                LatLng convert_location =  fragmentActivity.gps.setUpMap(false);
                //vytvorenie markera
                fragmentActivity.gps.addBumpToMap (convert_location,1,1);
                if (convert_location != null) {
                    Location location = new Location("new");
                    location.setLatitude(convert_location.latitude);
                    location.setLongitude(convert_location.longitude);
                    location.setTime(new Date().getTime());


                   fragmentActivity.accelerometer.addPossibleBumps(location,intensity);
                    // manuálny výtlk
                   fragmentActivity.accelerometer.addBumpsManual(1);


                    // vytvori novy vytlk

                 /*   Address address = null;
                    EditText text = (EditText) findViewById(R.id.location);
                    String locations = text.getText().toString();
                    LatLng to_position=null;
                    LatLng myPosition=null;
                        try {
                            address = Route.findLocality(locations, this);
                            if (address == null) {
                                if (isEneableShowText())
                                    Toast.makeText(this, "Unable to find location, wrong name!", Toast.LENGTH_LONG).show();
                            }
                            else {
                                 to_position = new LatLng(address.getLatitude(),address.getLongitude());
                                 myPosition = new LatLng(fragmentActivity.gps.getmCurrentLocation().getLatitude(), fragmentActivity.gps.getmCurrentLocation().getLongitude());

                            }
                        }
                        catch (Exception e) {
                            if (isEneableShowText())
                                Toast.makeText(this, "Unable to find location!", Toast.LENGTH_LONG).show();
                        }


                    Route md = new Route();
                    LatLng from = fragmentActivity.gps.getCurrentLatLng();
                    LatLng to = convert_location;
                    Document doc = md.getDocument(myPosition, to_position);
                    ArrayList<LatLng> directionPoint = md.getDirection(doc);

                    boolean value = isLocationOnEdge(convert_location,directionPoint,true,2.0);

                    //    new Bump(location, intensity);
                    Toast.makeText(this, "New bump added" + value, Toast.LENGTH_LONG).show();*/
                    Toast.makeText(this, "New bump added" , Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.delete_btn:
                add_button.setVisibility(View.VISIBLE);
                confirm.setVisibility(View.INVISIBLE);
                // disable listener na klik
                fragmentActivity.gps.setUpMap(false);
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
                    if (isEneableShowText())
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
                if (isEneableShowText())
                     Toast.makeText(this, "Unable to find location!", Toast.LENGTH_LONG).show();
            }
        }
        else {
            if (isEneableShowText())
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
                if (isEneableShowText())
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

            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;


            case R.id.exit:
                if (fragmentActivity.isNetworkAvailable()) {
                     new EndAsyncTask().execute();
                }
                fragmentActivity.stop_servise();
                onDestroy();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class EndAsyncTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            int i= 0;
            for (HashMap<Location, Float> bump : fragmentActivity.accelerometer.getPossibleBumps()) {
                if (!fragmentActivity.accelerometer.getBumpsManual().isEmpty()) {
                    fragmentActivity.saveBump(bump, fragmentActivity.accelerometer.getBumpsManual().get(i));
                    i++;
                }
            }
            fragmentActivity.accelerometer.getPossibleBumps().clear();
            fragmentActivity.accelerometer.getBumpsManual().clear();
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
        if (fragmentActivity.gps !=null && fragmentActivity.gps.getmCurrentLocation()!= null) {
            LatLng myPosition = new LatLng(fragmentActivity.gps.getmCurrentLocation().getLatitude(), fragmentActivity.gps.getmCurrentLocation().getLongitude());
            fragmentActivity.gps.goTo(myPosition, ZOOM_LEVEL);
        }
        MainActivity.activityResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainActivity.activityPaused();
    }

    public boolean isEneableShowText() {

        SharedPreferences preferences = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        Boolean alarm = preferences.getBoolean("alarm", Boolean.parseBoolean(null));
        if ((alarm) || (!alarm && MainActivity.isActivityVisible())) {
           return true;
        }
        else
            return false;
    }

}
