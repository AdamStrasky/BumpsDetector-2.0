package navigationapp.main_application;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceActivity;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.WindowManager;

import navigationapp.R;

import static navigationapp.R.xml.settings;


public class SettingsActivity extends PreferenceActivity  implements SharedPreferences.OnSharedPreferenceChangeListener {
    public  final String TAG = "SettingsActivity";
    String lang = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(settings);
        initializeSummaries();
        lang = getLanguage(); // uložím jazyk, aby som vedel či ho mením

    }

    private void initializeSummaries() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        for (String key : sharedPreferences.getAll().keySet()) {
            onSharedPreferenceChanged(sharedPreferences, key);
        }
    }

    @Override
    public void onResume() {
        isEneableScreen();
        super.onResume();
        MainActivity.activityResumed();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        MainActivity.activityPaused();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if("name".equals(key)) {
            findPreference(key).setSummary(sharedPreferences.getString(key, ""));  // zobrazí sa meno pod titlom
        }
        if("lang".equals(key)) {
            findPreference(key).setSummary(sharedPreferences.getString(key, "")); // zobrazí sa jazyk pod titlom
        }

       if (lang!=null && !lang.equals(getLanguage())) {
             new AlertDialog.Builder(this)
                    .setTitle(getApplication().getResources().getString(R.string.warning))
                    .setMessage(getApplication().getResources().getString(R.string.warning_msg))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(this.getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {


                        public void onClick(DialogInterface dialog, int whichButton) {
                           initializeSummaries();
                            Intent i = getBaseContext().getPackageManager()  // reštartujem aplikáciu, lebo mením jazyk
                                    .getLaunchIntentForPackage(getBaseContext().getPackageName());
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);

                        }
                    })
                    .setNegativeButton(getApplication().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            setLanguage(lang);      // vraciam pôvodný jazyk
                            initializeSummaries();
                        }
                    }).show();
       }

         // reaguje na zmenu nastavenia a sucasneho stavu internetu
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        if (isConnected) {
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.d(TAG, "onSharedChanged - TYPE_WIFI");
                MainActivity.manager.setConnected(true);
            }
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                Log.d(TAG, "onSharedChanged - TYPE_MOBILE");
                if (isEneableOnlyWifiMap()) {
                    Log.d(TAG, "onSharedChanged - setConnected(false)");
                    MainActivity.manager.setConnected(false);
                }
                else {
                    Log.d(TAG, "onSharedChanged - setConnected(true)");
                    MainActivity.manager.setConnected(true);
                }
            }
        }
        isEneableScreen();
    }

    public boolean isEneableOnlyWifiMap() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d(TAG,"isEneableOnlyWifiMap "+ String.valueOf(prefs.getBoolean("map", Boolean.parseBoolean(null))));
        return prefs.getBoolean("map", Boolean.parseBoolean(null));
    }

    public void isEneableScreen() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean screen = prefs.getBoolean("screen", Boolean.parseBoolean(null));
        Log.d(TAG , "isEneableScreen " +String.valueOf(screen));
        if (screen) {
           getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
           getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public String getLanguage() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String name = prefs.getString("lang", "");
        Log.d(TAG, "getLanguage - " + name);
        return name;
    }

    public void setLanguage(String lang) {
        Log.d(TAG, "onSharedChasetLanguagenged - " + lang);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putString("lang",lang);
        prefEditor.commit();
    }
}