package navigationapp.main_application;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import java.util.Locale;

/**
 * Created by Adam on 6.1.2017.
 */

public class Language extends Application {


    @Override
    public void onCreate() {
        super.onCreate();

    }

    public static void setLanguage(Context context,String language) {
        if (language.equals("Slovak") || language.equals("Slovenský")) {
            setLocaleFa(context);
        }
        else {
            setLocaleEn(context);
        }


    }


    public static void setLocaleFa (Context context){
        setLanguage("Slovenský",context);
        Locale locale = new Locale("sk");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        context.getApplicationContext().getResources().updateConfiguration(config, null);
    }

    public static void setLocaleEn (Context context){
        setLanguage("English",context);
        Locale locale = new Locale("en_US");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        context.getApplicationContext().getResources().updateConfiguration(config, null);
    }

    public static void setLanguage(String lang, Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = sharedPref.edit(); // Get preference in editor mode
        prefEditor.putString("lang",lang); // set your default value here (could be empty as well)
        prefEditor.commit(); // finally save changes
    }
}
