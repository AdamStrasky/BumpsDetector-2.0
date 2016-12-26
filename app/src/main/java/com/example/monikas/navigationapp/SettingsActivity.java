package com.example.monikas.navigationapp;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceActivity;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;

import static com.example.monikas.navigationapp.MainActivity.PREF_FILE_NAME;

public class SettingsActivity extends PreferenceActivity  implements SharedPreferences.OnSharedPreferenceChangeListener  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);


    }

    @Override
    public void onResume() {
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
       // reaguje na zmenu nastavenia a sucasneho stavu internetu
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean NisConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        if (NisConnected) {
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.d("onSharedChanged", "TYPE_WIFI");
                MainActivity.manager.setConnected(true);
            }
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                Log.d("onSharedChanged", "TYPE_MOBILE");
                if (isEneableOnlyWifiMap())
                    MainActivity.manager.setConnected(false);
                else
                    MainActivity.manager.setConnected(true);
            }
        }

        isEneableScreen();

     }

    public boolean isEneableOnlyWifiMap() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean map = prefs.getBoolean("map", Boolean.parseBoolean(null));
        Log.d("xxxxx", String.valueOf(map));
        if ((map)) {
            return true;
        }
        else
            return false;
    }
    public void isEneableScreen() {
        Log.d("aasc","adasdasdasdasdasdasdasdsadsad");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean imgSett = prefs.getBoolean("screen", Boolean.parseBoolean(null));

        Log.d("aasc", String.valueOf(imgSett));
        if (imgSett) {
            Log.d("aasc","qqqqqqqqqqqqqqqqqqq");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        }
        else {
            Log.d("aasc","tttttttttttttttttttt");
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
