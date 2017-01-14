package navigationapp.voice_application;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;

import navigationapp.R;

import navigationapp.error.ExceptionHandler;
import navigationapp.main_application.Bump;
import navigationapp.main_application.CallBackReturn;
import navigationapp.main_application.DatabaseOpenHelper;
import navigationapp.main_application.Provider;
import java.math.BigDecimal;
import java.util.Locale;

public class VoiceMainActivity extends Activity  {

    private SQLiteDatabase sb = null;
    private DatabaseOpenHelper databaseHelper = null;
    private Boolean voice = false;
    private GPSPosition gps = null;
    private Bump Handler = null;
    private TextToSpeech talker = null;
    private final String TAG = "VoiceMainActivity";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        String query= "";
        if (getIntent().getAction() != null && getIntent().getAction().equals("com.google.android.gms.actions.SEARCH_ACTION")) {
            query = getIntent().getStringExtra(SearchManager.QUERY);
            voice = isEneableVoice();

            if (!query.equals("bump")) {
                Log.d(TAG,"zly hlasový príkaz");
                if (voice) {
                    talker=new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {
                            if (status == TextToSpeech.SUCCESS) {
                                int result = talker.setLanguage(Locale.UK);
                                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                    Toast.makeText(getApplicationContext(),"Please download UK language package " ,Toast.LENGTH_SHORT).show();
                                } else {
                                    talker.speak("Wrong voice command " + setting_name(), TextToSpeech.QUEUE_FLUSH, null);
                                    while (talker.isSpeaking()){}
                                }
                            } else {
                                Log.i("DEBUG", "MISSION FAILED");
                            }
                        }
                    });
                }
                else
                    Toast.makeText(this,"Wrong voice command " ,Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        initialization_database(); // inicializácia databázy
        gps = new GPSPosition(VoiceMainActivity.this);

        if(gps.canGetLocation()){ // kontrola GPS
            final double latitude = gps.getLatitude(); // vratim si polohu
            final double longitude = gps.getLongitude();
            Log.d(TAG," mám polohu ");
            Location loc = new Location("Location");
            loc.setLatitude(gps.getLatitude());
            loc.setLongitude(gps.getLongitude());
            Handler = new Bump(loc, 6.0f, 1);
            Handler.getResponse(new CallBackReturn() {
                public void callback(String results) {
                    if (results.equals("success")) {
                        Log.d(TAG,"success handler");
                    } else {
                        Log.d(TAG,"error handler, zapisujem do db");
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
                                Toast.makeText(getApplicationContext(), getApplication().getResources().getString(R.string.voice_language_not_supported), Toast.LENGTH_LONG).show();
                                Log.e("DEBUG", "Language Not Supported");
                            } else {
                                talker.speak("Bump was added " + setting_name(), TextToSpeech.QUEUE_FLUSH, null);
                                while (talker.isSpeaking()){}
                            }
                        } else {
                            Log.i("DEBUG", "MISSION FAILED");
                        }
                    }
                });
            }
            else
                Toast.makeText(this, getApplication().getResources().getString(R.string.bump_add)  ,Toast.LENGTH_LONG).show();
        }else {
            if (voice) {
                talker=new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                        @Override
                         public void onInit(int status) {
                      if (status == TextToSpeech.SUCCESS) {
                          int result = talker.setLanguage(Locale.UK);
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                Toast.makeText(getApplicationContext(),getApplication().getResources().getString(R.string.voice_language_not_supported), Toast.LENGTH_LONG).show();
                                Log.e("DEBUG", "Language Not Supported");
                            } else {
                                talker.speak("Please turn on your GPS for use this function" + setting_name(), TextToSpeech.QUEUE_FLUSH, null);
                                while (talker.isSpeaking()){}
                            }
                      } else {
                          Log.i("DEBUG", "MISSION FAILED");
                      }
                    }
                });
            }
            else
                Toast.makeText(this,getApplication().getResources().getString(R.string.voice_turn_gps) ,Toast.LENGTH_LONG).show();
        }
         finish();
    }

    public void initialization_database(){
        // inicializacia databazy
        databaseHelper = new DatabaseOpenHelper(this);
        sb = databaseHelper.getWritableDatabase();
    }

    public  String setting_name() { // ziskavam používatelove meno z nastavení
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d(TAG,"retur name "+ prefs.getString("name", ""));
        return prefs.getString("name", "");

    }

    public  boolean isEneableVoice() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d(TAG,"isEneableVoice stav "+prefs.getBoolean("voice_alarm", Boolean.parseBoolean(null)));
        return prefs.getBoolean("voice_alarm", Boolean.parseBoolean(null));
    }
}