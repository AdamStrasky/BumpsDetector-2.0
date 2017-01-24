package navigationapp.widget;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.math.BigDecimal;

import navigationapp.R;
import navigationapp.main_application.Bump;
import navigationapp.main_application.CallBackReturn;
import navigationapp.main_application.DatabaseOpenHelper;
import navigationapp.main_application.Provider;
import navigationapp.voice_application.GPSPosition;

/**
 * Created by Adam on 23.1.2017.
 */

public class Click {

    private SQLiteDatabase sb = null;
    private DatabaseOpenHelper databaseHelper = null;
    private GPSPosition gps = null;
    private final String TAG = "Click";
    private Bump Handler = null;

    public Click(final Context context) {
        Log.d("QQQQQ","ppppapasdasdas");
        initialization_database(context);
        gps = new GPSPosition(context);
        if(gps.canGetLocation()) { // kontrola GPS
            final double latitude = gps.getLatitude(); // vratim si polohu
            final double longitude = gps.getLongitude();
            Log.d(TAG, " mám polohu " + gps.getLatitude());
            Location loc = new Location("Location");
            loc.setLatitude(gps.getLatitude());
            loc.setLongitude(gps.getLongitude());
            Handler = new Bump(loc, 6.7f, 1);
            Handler.getResponse(new CallBackReturn() {
                public void callback(String results) {
                    if (results.equals("success")) {
                        Log.d(TAG, "success handler");
                    } else {
                        Log.d(TAG, "error handler, zapisujem do db");
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
            android.os.Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, context.getResources().getString(R.string.bump_add), Toast.LENGTH_LONG).show();
                }
            });

        }else {
            android.os.Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, context.getResources().getString(R.string.no_gps), Toast.LENGTH_LONG).show();
                }
            });


        }

        gps.stopUsingGPS();
    }




    public void initialization_database(Context context){
        // inicializacia databazy
        databaseHelper = new DatabaseOpenHelper(context);
        sb = databaseHelper.getWritableDatabase();
    }
}
