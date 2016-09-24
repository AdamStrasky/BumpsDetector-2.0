package com.example.monikas.navigationapp;

import android.content.SharedPreferences;
import android.preference.PreferenceActivity;

import android.os.Bundle;

public class SettingsActivity extends PreferenceActivity  implements SharedPreferences.OnSharedPreferenceChangeListener  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        initializeSummaries();
    }

    private void initializeSummaries() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        for (String key : sharedPreferences.getAll().keySet()) {
            onSharedPreferenceChanged(sharedPreferences, key);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }


    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      /*  if("".equals(key)) {
            findPreference(key).setSummary(sharedPreferences.getString(key, ""));
        }*/
    }
}
