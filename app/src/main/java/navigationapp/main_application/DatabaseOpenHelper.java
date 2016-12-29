package navigationapp.main_application;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static android.provider.BaseColumns._ID;
import static  navigationapp.main_application.Provider.bumps_detect.TABLE_NAME_BUMPS;
import static  navigationapp.main_application.Provider.bumps_detect.B_ID_BUMPS;
import static  navigationapp.main_application.Provider.bumps_detect.COUNT;
import static  navigationapp.main_application.Provider.bumps_detect.LAST_MODIFIED;
import static  navigationapp.main_application.Provider.bumps_detect.LATITUDE;
import static  navigationapp.main_application.Provider.bumps_detect.LONGTITUDE;
import static  navigationapp.main_application.Provider.bumps_detect.MANUAL;
import static  navigationapp.main_application.Provider.bumps_detect.RATING;

import static  navigationapp.main_application.Provider.bumps_collision.TABLE_NAME_COLLISIONS;
import static  navigationapp.main_application.Provider.bumps_collision.C_ID;
import static  navigationapp.main_application.Provider.bumps_collision.B_ID_COLLISIONS;
import static  navigationapp.main_application.Provider.bumps_collision.INTENSITY;
import static  navigationapp.main_application.Provider.bumps_collision.CRETED_AT;
import static  navigationapp.main_application.Provider.new_bumps.TABLE_NAME_NEW_BUMPS;

/**
 * Created by Adam on 6.10.2016.
 */

public class DatabaseOpenHelper extends SQLiteOpenHelper {

        public static final String DATABASE_NAME = "bump";
        public static final int DATABASE_VERSION = 1;
        public DatabaseOpenHelper(Context context) {
             super(context, DATABASE_NAME, null,DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(createTableSqlBumps());
            db.execSQL(createTableSqlCollisions());
            db.execSQL(createTableSqlNewBump());
         }

        private String createTableSqlBumps() {
            String sqlTemplate = "CREATE TABLE IF NOT EXISTS %s ("
                    + "%s INTEGER PRIMARY KEY ,"
                    + "%s INTEGER,"
                    + "%s DATETIME,"
                    + "%s DOUBLE,"
                    + "%s DOUBLE,"
                    + "%s INTEGER,"
                    + "%s INTEGER"
                    + ")";
            return String.format(sqlTemplate, TABLE_NAME_BUMPS, B_ID_BUMPS, COUNT, LAST_MODIFIED,LATITUDE,LONGTITUDE,MANUAL,RATING);
        }

        private String createTableSqlCollisions() {
             String sqlTemplate = "CREATE TABLE IF NOT EXISTS %s ("
                + "%s INTEGER PRIMARY KEY ,"
                + "%s INTEGER,"
                + "%s DOUBLE,"
                + "%s DATETIME"
                + ")";
            return String.format(sqlTemplate, TABLE_NAME_COLLISIONS, C_ID, B_ID_COLLISIONS, INTENSITY,CRETED_AT);
        }

         private String createTableSqlNewBump() {
         String sqlTemplate = "CREATE TABLE IF NOT EXISTS %s ("
                + "%s INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "%s DOUBLE,"
                + "%s DOUBLE,"
                + "%s DOUBLE,"
                + "%s INTEGER"
                + ")";
            return String.format(sqlTemplate, TABLE_NAME_NEW_BUMPS,_ID, LATITUDE, LONGTITUDE, INTENSITY,MANUAL);
        }

        @Override
         public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            String query;
            query = "DROP TABLE IF EXISTS " + TABLE_NAME_BUMPS;
            db.execSQL(query);
            query = "DROP TABLE IF EXISTS " + TABLE_NAME_COLLISIONS;
            db.execSQL(query);
            onCreate(db);
        }
}
