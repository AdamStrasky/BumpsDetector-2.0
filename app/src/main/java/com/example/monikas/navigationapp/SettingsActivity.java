package com.example.monikas.navigationapp;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceActivity;

import android.os.Bundle;
import android.util.Log;

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

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        Log.d("fdsgszdf","change share ");
        boolean NisConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        if (NisConnected) {
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.d("fdsgszdf", "TYPE_WIFI");
                MainActivity.manager.setConnected(true);
            }
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                Log.d("fdsgszdf", "TYPE_MOBILE");
                if (isEneableOnlyWifiMap())
                    MainActivity.manager.setConnected(false);
                else
                    MainActivity.manager.setConnected(true);
            }
        }
        Log.d("fdsgszdf","change share ");
    }

    public boolean isEneableOnlyWifiMap() {
        SharedPreferences preferences = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        Boolean map = preferences.getBoolean("map", Boolean.parseBoolean(null));
        if ((map)) {
            return true;
        }
        else
            return false;
    }
}
