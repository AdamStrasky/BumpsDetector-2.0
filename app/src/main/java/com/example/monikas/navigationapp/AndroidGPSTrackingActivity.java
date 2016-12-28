package com.example.monikas.navigationapp;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.location.*;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import java.math.BigDecimal;
import java.util.Locale;


public class AndroidGPSTrackingActivity extends Activity  implements TextToSpeech.OnInitListener {

    SQLiteDatabase sb;
    DatabaseOpenHelper databaseHelper;

    // GPSTracker class
    GPSTracker gps;
    private Bump  Handler;
    private TextToSpeech tts;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        tts = new  TextToSpeech(this , this);



        String query="";
        if (getIntent().getAction() != null && getIntent().getAction().equals("com.google.android.gms.actions.SEARCH_ACTION")) {
            query = getIntent().getStringExtra(SearchManager.QUERY);
            Log.e("Query:",query);   //query is the search word
        }



        // show location button click event

        // create class object
        initialization_database();
        gps = new GPSTracker(AndroidGPSTrackingActivity.this);

        // check if GPS enabled
        if(gps.canGetLocation()){

           final double latitude = gps.getLatitude();
           final double longitude = gps.getLongitude();

            Location loc = new Location("service Provider");
            loc.setLatitude(gps.getLatitude());
            loc.setLongitude(gps.getLongitude());
            Handler = new Bump(loc, 6.0f, 1);
            Handler.getResponse(new CallBackReturn() {
                public void callback(String results) {
                    if (results.equals("success")) {
                        Toast.makeText(getApplicationContext(), "success " + longitude, Toast.LENGTH_LONG).show();


                    } else {
                        Toast.makeText(getApplicationContext(), "error " + longitude, Toast.LENGTH_LONG).show();
                        BigDecimal bd = new BigDecimal(Float.toString(6));
                        bd = bd.setScale(6, BigDecimal.ROUND_HALF_UP);

                        ContentValues contentValues = new ContentValues();
                        contentValues.put(Provider.new_bumps.LATITUDE, latitude);
                        contentValues.put(Provider.new_bumps.LONGTITUDE, longitude);
                        contentValues.put(Provider.new_bumps.MANUAL, 1);
                        contentValues.put(Provider.new_bumps.INTENSITY, String.valueOf(bd));
                        sb.insert(Provider.new_bumps.TABLE_NAME_NEW_BUMPS, null, contentValues);

                    }
                }
            });




            Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();

            String toSpeak = "murkito burito ";
            Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
            convert_text();

        }else{
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }
   //     finish();



    }

    public void initialization_database(){
        // inicializacia databazy
        databaseHelper = new DatabaseOpenHelper(this);
        sb = databaseHelper.getWritableDatabase();
    }


    private void convert_text() {
      final  String speech = "Bump was added Adam";
        tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);

    }
    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS){
            int result = tts.setLanguage(Locale.UK);
            if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                Log.e("DEBUG" , "Language Not Supported");}
            else{

                convert_text();
            }

        }
        else{
            Log.i("DEBUG" , "MISSION FAILED");
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null){
            tts.stop();
            tts.shutdown();
        }
    }
}