package com.example.monikas.navigationapp;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.location.*;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import java.math.BigDecimal;
import java.util.Locale;


public class AndroidGPSTrackingActivity extends Activity  {

    SQLiteDatabase sb;
    DatabaseOpenHelper databaseHelper;
    Boolean voice = false;
    // GPSTracker class
    GPSTracker gps;
    private Bump  Handler;
    TextToSpeech talker;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        voice = isEneableVoice();








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



            if (voice) {
                talker=new TextToSpeech(this, new TextToSpeech.OnInitListener() {

                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            int result = talker.setLanguage(Locale.UK);
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                Log.e("DEBUG", "Language Not Supported");
                            } else {

                                talker.speak("Bump was added " + isName(), TextToSpeech.QUEUE_FLUSH, null);
                                while (talker.isSpeaking()){}
                            }

                        } else {
                            Log.i("DEBUG", "MISSION FAILED");
                        }

                    }
                });

            }
            else
                Toast.makeText(this,"Bump was added "  ,Toast.LENGTH_SHORT).show();


        }else{
            if (voice) {
                talker=new TextToSpeech(this, new TextToSpeech.OnInitListener() {

                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            int result = talker.setLanguage(Locale.UK);
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                Log.e("DEBUG", "Language Not Supported");
                            } else {

                                talker.speak("Please turn on your GPS for use this function" + isName(), TextToSpeech.QUEUE_FLUSH, null);
                                while (talker.isSpeaking()){}
                            }

                        } else {
                            Log.i("DEBUG", "MISSION FAILED");
                        }

                    }
                });

            }
            else
                Toast.makeText(this,"Please turn on your GPS for use this function " ,Toast.LENGTH_SHORT).show();
        }



    finish();


    }

    public void initialization_database(){
        // inicializacia databazy
        databaseHelper = new DatabaseOpenHelper(this);
        sb = databaseHelper.getWritableDatabase();
    }

    public  String isName() {
        // či mám povolené ukazovať informácia aj mimo aplikácie
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String alarm = prefs.getString("name", "");
        if (alarm.equals("Your name"))
            return "";
        else
            return alarm;
    }

    public  boolean isEneableVoice() {
        // či mám povolené ukazovať informácia aj mimo aplikácie
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean alarm = prefs.getBoolean("voice", Boolean.parseBoolean(null));
        if (alarm) {
            return true;
        }
        else
            return false;
    }



}