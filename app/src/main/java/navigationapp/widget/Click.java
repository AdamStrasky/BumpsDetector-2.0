package navigationapp.widget;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import navigationapp.R;
import navigationapp.main_application.Bump;
import navigationapp.main_application.CallBackReturn;
import navigationapp.main_application.DatabaseOpenHelper;
import navigationapp.main_application.Provider;
import navigationapp.voice_application.GPSPosition;

public class Click {

    private SQLiteDatabase database = null;
    private DatabaseOpenHelper databaseHelper = null;
    private GPSPosition gps = null;
    private final String TAG = "Click";
    private Bump Handler = null;

    public Click(final Context context, final Integer type,final String text, final String deviceID) {
        gps = new GPSPosition(context);

        if(gps.canGetLocation()) { // kontrola GPS
            final double latitude = gps.getLatitude(); // vratim si polohu
            final double longitude = gps.getLongitude();
            Log.d(TAG, " mám polohu " + gps.getLatitude() +" " + gps.getLongitude() );
            Location loc = new Location("Location");
            loc.setLatitude(gps.getLatitude());
            loc.setLongitude(gps.getLongitude());
            loc.setTime(new Date().getTime());
            if (gps.isNetworkAvailable(context)) {
                Handler = new Bump(loc, 6.0f, 1,type,text,deviceID);
                Handler.getResponse(new CallBackReturn() {
                    public void callback(String results) {
                        if (results.equals("success")) {
                            Log.d(TAG, "success handler");
                        } else {
                            Log.d(TAG, "error handler, zapisujem do db");
                            initialization_database(context);
                            BigDecimal bd = new BigDecimal(Float.toString(6));
                            bd = bd.setScale(6, BigDecimal.ROUND_HALF_UP);
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(Provider.new_bumps.LATITUDE, latitude);
                            contentValues.put(Provider.new_bumps.LONGTITUDE, longitude);
                            contentValues.put(Provider.new_bumps.MANUAL, 1);
                            contentValues.put(Provider.new_bumps.INTENSITY, String.valueOf(bd));
                            contentValues.put(Provider.new_bumps.TYPE, type);
                            contentValues.put(Provider.new_bumps.TEXT, text);
                            contentValues.put(Provider.new_bumps.CREATED_AT, getDate(new Date().getTime(), "yyyy-MM-dd HH:mm:ss"));
                            database.insert(Provider.new_bumps.TABLE_NAME_NEW_BUMPS, null, contentValues);
                            close_db();
                        }
                    }
                });
            } else {
                Log.d(TAG, "no internet, zapisujem do db");
                initialization_database(context);
                BigDecimal bd = new BigDecimal(Float.toString(6));
                bd = bd.setScale(6, BigDecimal.ROUND_HALF_UP);
                ContentValues contentValues = new ContentValues();
                contentValues.put(Provider.new_bumps.LATITUDE, latitude);
                contentValues.put(Provider.new_bumps.LONGTITUDE, longitude);
                contentValues.put(Provider.new_bumps.MANUAL, 1);
                contentValues.put(Provider.new_bumps.INTENSITY, String.valueOf(bd));
                contentValues.put(Provider.new_bumps.TYPE, type);
                contentValues.put(Provider.new_bumps.TEXT, text);
                contentValues.put(Provider.new_bumps.CREATED_AT, getDate(new Date().getTime(), "yyyy-MM-dd HH:mm:ss"));
                database.insert(Provider.new_bumps.TABLE_NAME_NEW_BUMPS, null, contentValues);
                close_db();
            }

            android.os.Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    String text =null;
                    if (type == 0)
                        text = context.getResources().getString(R.string.bump_add);
                    if (type == 1)
                        text = context.getResources().getString(R.string.trasht_add);
                    if (type == 2)
                        text = context.getResources().getString(R.string.canstock_add);
                    if (type == 3)
                        text = context.getResources().getString(R.string.select_add);
                    Toast.makeText(context,text, Toast.LENGTH_SHORT).show();
                }
            });

        }else {
            android.os.Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, context.getResources().getString(R.string.no_gps), Toast.LENGTH_SHORT).show();
                }
            });
        }
        gps.stopUsingGPS();
    }

    public void initialization_database(Context context){
        databaseHelper = new DatabaseOpenHelper(context);
        database = databaseHelper.getWritableDatabase();
    }

    public void close_db(){
        database.close();
        databaseHelper.close();
    }
    public  String getDate(long milliSeconds, String dateFormat)
    {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }
}