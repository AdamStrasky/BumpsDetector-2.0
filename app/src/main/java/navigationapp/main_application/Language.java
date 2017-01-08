package navigationapp.main_application;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Locale;

public class Language extends Application {

    private final static String TAG = "Language";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static void setLanguage(Context context,String language) {
        if (language.equals("Slovak") || language.equals("Slovenský")) {
            setLocaleSK(context);
        }
        else {
            setLocaleEn(context);
        }
    }

    public static void setLocaleSK (Context context){
        Log.d(TAG,"setLocaleSK");
        setLanguage("Slovenský",context);
        Locale locale = new Locale("sk");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        context.getApplicationContext().getResources().updateConfiguration(config, null);
    }

    public static void setLocaleEn (Context context){
        Log.d(TAG,"setLocaleEN");
        setLanguage("English",context);
        Locale locale = new Locale("en_US");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        context.getApplicationContext().getResources().updateConfiguration(config, null);
    }

    public static void setLanguage(String lang, Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putString("lang",lang);
        prefEditor.commit();
    }
}
