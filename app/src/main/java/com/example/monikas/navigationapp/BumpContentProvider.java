package com.example.monikas.navigationapp;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;

import static com.example.monikas.navigationapp.DatabaseOpenHelper.DATABASE_NAME;
import static com.example.monikas.navigationapp.Provider.*;
import static com.example.monikas.navigationapp.Provider.bumps_detect.TABLE_NAME_BUMPS;
//import static com.example.monikas.navigationapp.Provider.bumps_detect.TABLE_NAME_BUMPS;

/**
 * Created by Adam on 8.10.2016.
 */



public class BumpContentProvider  extends ContentProvider {
    public static ContentResolver dsd;
    public static final String AUTHORITY = "com.example.monikas.navigationapp.BumpContentProvider";
    public static final Uri
            CONTENT_URI = new Uri.Builder()
            .scheme("content")
            .authority(AUTHORITY)
            .appendPath(TABLE_NAME_BUMPS)
            .build();
    private static final String[]  ALL_COLUMNS =null ;
    private static final String[]  NO_SELECTION =null ;
    private static final String[]  NO_SELECTION_ARGS =null ;
    private static final String[]  NO_GROUP_BY =null ;
    private static final String[]  NO_HAVING =null ;
    private static final String[]  NO_SORT_ORDER =null ;

    private UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private DatabaseOpenHelper databaseHelper;

    private static final int URI_MATCH_NOTES = 0;
    private static final int URI_MATCH_NOTE_BY_ID = 1;
    @Override
    public boolean onCreate() {
        uriMatcher.addURI(AUTHORITY, TABLE_NAME_BUMPS, URI_MATCH_NOTES);

        this.databaseHelper = new DatabaseOpenHelper(getContext());
        return true;

    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;

        switch(uriMatcher.match(uri)) {


            case URI_MATCH_NOTES:
                Log.d("aaaaaaaaaaaaaaa,","bbbbbbbbbbbbbbbbbb");
                cursor = listNotes();



                Log.d("aaaaaaaaaaaaaaa,","oooooooooooo");
             ///   getContext().getApplicationContext().getContentResolver().notifyChange(uri, null);
            //   getContext().getContentResolver().notifyChange(uri, null);
              //   cursor.setNotificationUri(getContext().databaseList(), uri);
                //  setNotificationUri(ContentResolver cursor, Uri uri);
                  cursor.setNotificationUri(getContext().getContentResolver(), uri);
                  cursor.setNotificationUri(getContext().getContentResolver(), uri);
                 // cursor.setNotificationUri(getContentResolver(), uri);
                return cursor;

            default:
                Log.d("aaaaaaaaaaaaaaa,","ddddddddddddddddddd");
                return null;
        }
    }

    public  Cursor listNotes() {
        Log.d("aaaaaaaaaaaaaaa,","cccccccccccccccccc");
        int version =0;
        File dbpath = getContext().getDatabasePath(DATABASE_NAME);
        try {
            version = FragmentActivity.getDbVersionFromFile(dbpath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        databaseHelper = new DatabaseOpenHelper(getContext(),version);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        return db.query(TABLE_NAME_BUMPS , null, null, null, null, null, null);
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }


}
