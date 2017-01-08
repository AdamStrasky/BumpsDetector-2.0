package navigationapp.main_application;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.monikas.navigationapp.R;

import static android.R.id.message;
import static com.example.monikas.navigationapp.R.xml.settings;

public class SettingsActivity extends PreferenceActivity  implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String PREF_FILE_NAME = "Settings";
    String lang;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(settings);
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        //setName();
      //
        initializeSummaries();
        lang = getLanguage();
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
            findPreference(key).setSummary(sharedPreferences.getString(key, ""));
        }
        if("lang".equals(key)) {
            findPreference(key).setSummary(sharedPreferences.getString(key, ""));
        }

       if (lang!=null && !lang.equals(getLanguage())) {
         new AlertDialog.Builder(this)
                    .setTitle(getApplication().getResources().getString(R.string.warning))
                    .setMessage(getApplication().getResources().getString(R.string.warning_msg))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(this.getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {


                        public void onClick(DialogInterface dialog, int whichButton) {
                           initializeSummaries();
                            Intent i = getBaseContext().getPackageManager()
                                    .getLaunchIntentForPackage(getBaseContext().getPackageName());
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);

                        }
                    })
                    .setNegativeButton(getApplication().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            setLanguage(lang);
                            initializeSummaries();
                        }


                    }).show();




        }


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
        } else
            return false;
    }

    public void isEneableScreen() {
        Log.d("aasc", "adasdasdasdasdasdasdasdsadsad");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean imgSett = prefs.getBoolean("screen", Boolean.parseBoolean(null));

        Log.d("aasc", String.valueOf(imgSett));
        if (imgSett) {
            Log.d("aasc", "qqqqqqqqqqqqqqqqqqq");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        } else {
            Log.d("aasc", "tttttttttttttttttttt");
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }



    public String getLanguage() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String name = prefs.getString("lang", "");
        return name;
    }

    public void setLanguage(String lang) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        SharedPreferences.Editor prefEditor = sharedPref.edit(); // Get preference in editor mode
        prefEditor.putString("lang",lang); // set your default value here (could be empty as well)
        prefEditor.commit(); // finally save changes
    }







}
