package navigationapp.main_application;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static android.provider.BaseColumns._ID;
import static navigationapp.main_application.Provider.bumps_detect.ADMIN_FIX;
import static navigationapp.main_application.Provider.bumps_detect.FIX;
import static navigationapp.main_application.Provider.bumps_detect.INFO;
import static  navigationapp.main_application.Provider.bumps_detect.TABLE_NAME_BUMPS;
import static  navigationapp.main_application.Provider.bumps_detect.B_ID_BUMPS;
import static  navigationapp.main_application.Provider.bumps_detect.COUNT;
import static  navigationapp.main_application.Provider.bumps_detect.LAST_MODIFIED;
import static  navigationapp.main_application.Provider.bumps_detect.LATITUDE;
import static  navigationapp.main_application.Provider.bumps_detect.LONGTITUDE;
import static  navigationapp.main_application.Provider.bumps_detect.MANUAL;
import static  navigationapp.main_application.Provider.bumps_detect.RATING;


import static navigationapp.main_application.Provider.bumps_detect.TYPE;
import static navigationapp.main_application.Provider.new_bumps.CREATED_AT;
import static navigationapp.main_application.Provider.new_bumps.INTENSITY;
import static  navigationapp.main_application.Provider.new_bumps.TABLE_NAME_NEW_BUMPS;
import static navigationapp.main_application.Provider.new_bumps.TEXT;
import static navigationapp.main_application.Provider.photo.PATH;
import static navigationapp.main_application.Provider.photo.TABLE_NAME_PHOTO;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
        public final String TAG = "DatabaseOpenHelper";
        public static final String DATABASE_NAME = "bump";
        public static final int DATABASE_VERSION = 1;
        public DatabaseOpenHelper(Context context) {
           super(context, DATABASE_NAME, null,DATABASE_VERSION);
            Log.d(TAG, "DatabaseOpenHelper constructor");
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "onCreate SQLiteDatabase");
            db.execSQL(createTableSqlBumps());
            db.execSQL(createTableSqlNewBump());
            db.execSQL(createTableSqlPhoto());
        }

        private String createTableSqlBumps() {
            Log.d(TAG, "onCreate createTableSqlBumps");
            String sqlTemplate = "CREATE TABLE IF NOT EXISTS %s ("
                    + "%s INTEGER PRIMARY KEY ,"
                    + "%s INTEGER,"
                    + "%s DATETIME,"
                    + "%s DOUBLE,"
                    + "%s DOUBLE,"
                    + "%s INTEGER,"
                    + "%s INTEGER,"
                    + "%s INTEGER,"
                    + "%s INTEGER,"
                    + "%s INTEGER,"
                    + "%s STRING"
                    + ")";
            return String.format(sqlTemplate, TABLE_NAME_BUMPS, B_ID_BUMPS, COUNT, LAST_MODIFIED,LATITUDE,LONGTITUDE,MANUAL,RATING,TYPE,FIX,ADMIN_FIX, INFO);
        }

        private String createTableSqlNewBump() {
         String sqlTemplate = "CREATE TABLE IF NOT EXISTS %s ("
                + "%s INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "%s DOUBLE,"
                + "%s DOUBLE,"
                + "%s DOUBLE,"
                + "%s INTEGER,"
                + "%s DATETIME,"
                + "%s INTEGER,"
                + "%s STRING"
                + ")";
            return String.format(sqlTemplate, TABLE_NAME_NEW_BUMPS,_ID, LATITUDE, LONGTITUDE, INTENSITY,MANUAL,CREATED_AT,TYPE,TEXT);
        }

    private String createTableSqlPhoto() {
        String sqlTemplate = "CREATE TABLE IF NOT EXISTS %s ("
                + "%s INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "%s DOUBLE,"
                + "%s DOUBLE,"
                + "%s DATETIME,"
                + "%s INTEGER,"
                + "%s STRING"
                + ")";
        return String.format(sqlTemplate, TABLE_NAME_PHOTO,_ID, LATITUDE, LONGTITUDE,CREATED_AT,TYPE,PATH);
    }

        @Override
         public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            String query;
            query = "DROP TABLE IF EXISTS " + TABLE_NAME_BUMPS;
            db.execSQL(query);
            query = "DROP TABLE IF EXISTS " + TABLE_NAME_NEW_BUMPS;
            db.execSQL(query);
            query = "DROP TABLE IF EXISTS " + TABLE_NAME_PHOTO;
            db.execSQL(query);
            onCreate(db);
        }
}
