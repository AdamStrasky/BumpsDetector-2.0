package navigationapp.main_application;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import navigationapp.R;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static navigationapp.main_application.FragmentActivity.checkCloseDb;
import static navigationapp.main_application.FragmentActivity.checkIntegrityDB;
import static navigationapp.main_application.FragmentActivity.isEneableShowText;
import static navigationapp.main_application.FragmentActivity.isNetworkAvailable;
import static navigationapp.main_application.FragmentActivity.lockAdd;
import static navigationapp.main_application.FragmentActivity.lockZoznam;
import static navigationapp.main_application.FragmentActivity.updatesLock;
import static navigationapp.main_application.MainActivity.androidId;
import static navigationapp.main_application.MainActivity.getDate;
import static navigationapp.main_application.Provider.bumps_detect.TABLE_NAME_BUMPS;

public class SyncDatabase {
    private JSONArray bumps = null;
    private JSONParser jsonParser = new JSONParser();
    private Accelerometer accelerometer = null;
    private GPSLocator gps = null;
    private Activity context = null;
    private MapLayer mapLayer = null;
    private boolean regular_update = false;
    private boolean regularUpdatesLock = false;
    private boolean lockHandler = false;
    private double lang_database = 0 , longt_database = 0;
    private int net, updates = 0;
    private final String TAG = "SyncDatabase";
    int numRecord =0 , numUpdate =0 ;

    public SyncDatabase(Accelerometer accelerometer, GPSLocator gps, Activity context, MapLayer mapLayer) {
        this.accelerometer =accelerometer;
        this.gps = gps;
        this.context = context;
        this.mapLayer = mapLayer;
        regular_update = true;
        loadSaveDB(); // načítanie uložených výtlkov , ktoré neboli odoslané na server
        startGPS();
        new Timer().schedule(new Regular_upgrade(),3600000, 3600000);// 1 hodina == 3600000    // pravidelný update ak nemám povolený internet
    }

    private void loadSaveDB() {
        Log.d(TAG, "loadSaveDB start");
        new Thread() {
            public void run() {
                while (true) {
                    if (updatesLock.tryLock()) {
                        try {   // načítam všetky uložené výtlky ktoré neboli synchronizovane zo serverom
                            String selectQuery = "SELECT latitude,longitude,intensity,manual,type,text,created_at FROM new_bumps ";
                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(context);
                            SQLiteDatabase database = databaseHelper.getReadableDatabase();
                            checkIntegrityDB(database);
                            database.beginTransaction();
                            Cursor cursor = database.rawQuery(selectQuery, null);
                            ArrayList<HashMap<android.location.Location,Float>> listLocation = new ArrayList<HashMap< android.location.Location , Float>>();
                            ArrayList<Integer> listManual = new ArrayList<Integer>();
                            ArrayList<Integer> listType = new ArrayList<Integer>();
                            ArrayList<String>  listText = new ArrayList<String>();
                            
                            if (cursor != null && cursor.moveToFirst()) {
                                do {
                                    if (!cursor.isNull(0) && !cursor.isNull(1) & !cursor.isNull(2) && !cursor.isNull(3)) {
                                        android.location.Location  location = new android.location.Location ("new");
                                        location.setLatitude(cursor.getDouble(0));
                                        location.setLongitude(cursor.getDouble(1));
                                        try {
                                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                            Date date = format.parse(cursor.getString(6));
                                            location.setTime(date.getTime());
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                        HashMap<android.location.Location, Float> locationHashMap = new HashMap();
                                        locationHashMap.put(location, (float) cursor.getDouble(2));
                                        listLocation.add(locationHashMap);
                                        listManual.add(cursor.getInt(3));
                                        listType.add(cursor.getInt(4));
                                        listText.add(cursor.getString(5));
                                        Log.d(TAG, "loadSaveDB latitude " + cursor.getDouble(0));
                                        Log.d(TAG, "loadSaveDB longitude " + cursor.getDouble(1));
                                        Log.d(TAG, "loadSaveDB listType " + cursor.getInt(4));
                                        Log.d(TAG, "loadSaveDB listText " + cursor.getString(5));
                                        Log.d(TAG, "loadSaveDB date"+ getDate(location.getTime(), "yyyy-MM-dd HH:mm:ss"));
                                    }
                                } while (cursor.moveToNext());
                                if (cursor != null && accelerometer != null) {
                                    while (true) {
                                        if (lockZoznam.tryLock()) {
                                            try {
                                                Log.d(TAG, "loadSaveDB copy old bump");
                                                accelerometer.getPossibleBumps().addAll(listLocation);
                                                accelerometer.getBumpsManual().addAll(listManual);
                                                accelerometer.gettypeDetect().addAll(listType);
                                                accelerometer.gettextDetect().addAll(listText);
                                            } finally {
                                                Log.d(TAG, "loadSaveDB lockZoznam unlock");
                                                lockZoznam.unlock();
                                                break;
                                            }
                                        } else {
                                            Log.d(TAG, "loadSaveDB lockZoznam try lock");
                                            try {
                                                Random ran = new Random();
                                                int x = ran.nextInt(20) + 1;
                                                Thread.sleep(x);
                                            } catch (InterruptedException e) {
                                                e.getMessage();
                                            }
                                        }
                                    }
                                }
                            }
                            database.setTransactionSuccessful();
                            database.endTransaction();
                            database.close();
                            databaseHelper.close();
                            checkCloseDb(database);
                        } finally {
                            Log.d(TAG, "loadSaveDB unlock");
                            updatesLock.unlock();
                            break;
                        }
                    } else {
                        Log.d(TAG, "loadSaveDB try lock");
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                            e.getMessage();
                        }
                    }
                }
            }
        }.start();
    }

    private void startGPS() {
        new Timer().schedule(new SyncDb(), 0, 300000);  //300000    60000
        Log.d(TAG, " start regular update SyncDb");
    }

    private class Regular_upgrade extends TimerTask {
        @Override
        public void run() {
            Log.d(TAG, " Regular_upgrade set true ");
            regular_update = true;
        }
    }
    
    ArrayList<HashMap<android.location.Location, Float>> bumpsQQ = new ArrayList<HashMap<android.location.Location, Float>>();
    ArrayList<Integer> bumpsManualQQ = new ArrayList<Integer>();
    ArrayList<Integer> bumpsTypeQQ = new ArrayList<Integer>();
    ArrayList<String> bumpsTextQQ = new ArrayList<String>();

    private class SyncDb extends TimerTask {
        @Override
        public void run() {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    freeMemory();
                    lockHandler = true;
                    if (isNetworkAvailable(context)) {  //  ak je pripojenie na internet
                        if (!(isEneableDownload() && !isConnectedWIFI())) { // povolené sťahovanie
                            if (accelerometer != null) {
                                ArrayList<HashMap<android.location.Location, Float>> list = accelerometer.getPossibleBumps();
                                //pouzivatel je upozorneni na odosielanie vytlkov notifikaciou
                                if (isEneableShowText(context))
                                    Toast.makeText(context, context.getResources().getString(R.string.save_bump) + "(" + list.size() + ")", Toast.LENGTH_SHORT).show();
                                if (!list.isEmpty()) {
                                    Log.d(TAG, "SyncDb zoznam nie je prázdny ");
                                    regularUpdatesLock = false;
                                    Thread t = new Thread() {
                                        public void run() {
                                            while (true) {
                                                if (lockZoznam.tryLock()) {
                                                    try {
                                                        bumpsQQ.addAll(accelerometer.getPossibleBumps());
                                                        bumpsManualQQ.addAll(accelerometer.getBumpsManual());
                                                        bumpsTypeQQ.addAll(accelerometer.gettypeDetect());
                                                        bumpsTextQQ.addAll(accelerometer.gettextDetect());
                                                        accelerometer.getPossibleBumps().clear();
                                                        accelerometer.getBumpsManual().clear();
                                                        accelerometer.gettypeDetect().clear();
                                                        accelerometer.gettextDetect().clear();
                                                    } finally {
                                                        Log.d(TAG, "SyncDb lockZoznam unlock");
                                                        lockZoznam.unlock();
                                                        break;
                                                    }
                                                } else {
                                                    Log.d(TAG, "SyncDb lockZoznam try lock");
                                                    try {
                                                        Random ran = new Random();
                                                        int x = ran.nextInt(20) + 1;
                                                        Thread.sleep(x);
                                                    } catch (InterruptedException e) {
                                                        e.getMessage();
                                                    }
                                                }
                                            }
                                        }
                                    };
                                    t.start();
                                    try {
                                        t.join();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    saveBump(bumpsQQ, bumpsManualQQ,bumpsTypeQQ,bumpsTextQQ, 0);
                                } else {
                                    Log.d(TAG, "SyncDb zoznam je prázdny ");
                                    getBumpsWithLevel();
                                }
                            } else {
                                Log.d(TAG, "SyncDb acc null, nemalo by sa stať !!!!!!! ");
                                getBumpsWithLevel();
                            }
                        } else {
                            Log.d(TAG, "SyncDb nepovolený internet");
                            getBumpsWithLevel();
                        }
                    } else {
                        Log.d(TAG, "SyncDb nemám internet");
                        getBumpsWithLevel();
                    }
                }
            });
        }
    }

    public void getBumpsWithLevel() {
        Log.d(TAG, "getBumpsWithLevel start");
        if (isNetworkAvailable(context)) {   //ak je pripojenie na internet
           if (!(isEneableDownload() && !isConnectedWIFI())) {  // ak som na wifi alebo mám povolenie
                if (isEneableShowText(context)) // hláška na nastavovanie mapy
                    Toast.makeText(context, context.getResources().getString(R.string.map_setting), Toast.LENGTH_SHORT).show();
                regular_update = false;
                if (gps != null && gps.getCurrentLatLng() != null) {
                    Log.d(TAG, "getBumpsWithLevel mam povolenie alebo som na wifi");
                    syncList(gps.getCurrentLatLng().latitude,gps.getCurrentLatLng().longitude, 1);
                }
           } else if (regular_update) {  // ak je to prve spustenie alebo pravidelný update
                if (accelerometer != null && accelerometer.getPossibleBumps() != null && accelerometer.getPossibleBumps().size() > 0) {
                    regularUpdatesLock = true;
                }
                regular_update = false;
               if (gps != null && gps.getCurrentLatLng() != null) {
                   Log.d(TAG, "getBumpsWithLevel prvé spustenie alebo pravidelný update");
                    syncList(gps.getCurrentLatLng().latitude,gps.getCurrentLatLng().longitude, 0);
               }
            } else { // ak mám síce internet ale nemám povolené stahovanie, tk načítam z databazy
                regular_update = false;
                if (gps != null && gps.getCurrentLatLng() != null) {
                   Log.d(TAG, "getBumpsWithLevel mám internet, ale nepovolený");
                   context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mapLayer.getAllBumps(gps.getCurrentLatLng().latitude,gps.getCurrentLatLng().longitude);
                        }
                   });
                }
           }
        } else { // nemám internet, čítam z databazy
            if (gps != null && gps.getCurrentLatLng() != null) {
                Log.d(TAG, "getBumpsWithLevel nemám internet, čítam z databazy");
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mapLayer.getAllBumps(gps.getCurrentLatLng().latitude,gps.getCurrentLatLng().longitude);
                    }
                });
            }
        }
    }

    private void GetUpdateAction() {
        Log.d(TAG, "GetUpdateAction  start " );
        // ak nemám dovolené sťahovať dáta,  ale mám update
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setMessage(context.getResources().getString(R.string.update));
        alert.setCancelable(false);
        alert.setPositiveButton(context.getResources().getString(R.string.yes),
                 new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        if (regularUpdatesLock) {
                            lockHandler = false;
                           Thread t = new Thread() {
                                public void run() {
                                        while (true) {
                                        if (lockZoznam.tryLock()) {
                                            try {
                                                bumpsQQ.addAll(accelerometer.getPossibleBumps());
                                                bumpsManualQQ.addAll(accelerometer.getBumpsManual());
                                                bumpsTypeQQ.addAll(accelerometer.gettypeDetect());
                                                bumpsTextQQ.addAll(accelerometer.gettextDetect());
                                                accelerometer.getPossibleBumps().clear();
                                                accelerometer.getBumpsManual().clear();
                                                bumpsTypeQQ.addAll(accelerometer.gettypeDetect());
                                                bumpsTextQQ.addAll(accelerometer.gettextDetect());
                                            } finally {
                                                Log.d(TAG, "GetUpdateAction lockZoznam unlock");
                                                lockZoznam.unlock();
                                                break;
                                            }
                                        } else {
                                            Log.d(TAG, "GetUpdateAction lockZoznam try lock");
                                            try {
                                                Random ran = new Random();
                                                int x = ran.nextInt(20) + 1;
                                                Thread.sleep(x);
                                            } catch (InterruptedException e) {
                                                e.getMessage();
                                            }
                                        }
                                    }


                                }
                            };
                            t.start();
                            try {
                                t.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            saveBump(bumpsQQ, bumpsManualQQ,bumpsTypeQQ,bumpsTextQQ, 0);
                        } else if (updates == 1) {
                            Log.d(TAG, "GetUpdateAction updates == 1 v getupdate" );
                            updates = 0;
                            // ak povolim, stiahnem data
                     syncList(gps.getCurrentLatLng().latitude,gps.getCurrentLatLng().longitude, 1);
                            //get_max_bumps(gps.getCurrentLatLng().latitude, gps.getCurrentLatLng().longitude, 1);
                        }
                    }
                });
        alert.setNegativeButton(context.getResources().getString(R.string.nope),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        regularUpdatesLock = false;
                        // ak nepovolim, zobrazím aké mam doteraz
                        if (gps != null && gps.getCurrentLatLng() != null) {
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                mapLayer.getAllBumps(gps.getCurrentLatLng().latitude, gps.getCurrentLatLng().longitude);
                                }
                            });
                        }
                    }
                });
        alert.show();
    }

    private Bump Handler;
    private UploadPhoto HandlerPhoto;
    private boolean lock = true;
    private ArrayList<HashMap< android.location.Location, Float>> listHelp = null;
    private ArrayList<Integer> bumpsManualHelp = null;
    private ArrayList<Integer> bumpsTypeHelp = null;
    private ArrayList<String> bumpsTextlHelp = null;
    private Integer poradie = 0;

    public void saveBump(ArrayList<HashMap< android.location.Location, Float>> list, ArrayList<Integer> bumpsManual, ArrayList<Integer> bumpsType,ArrayList<String> bumpsText, Integer sequel) {
        Log.d(TAG, "saveBump start" );
        Log.d(TAG, "saveBump start" +list.size());
        listHelp = list;
        bumpsManualHelp = bumpsManual;
        bumpsTypeHelp = bumpsType;
        bumpsTextlHelp = bumpsText;
        poradie = sequel;
        new Thread() {
            public void run() {
                Looper.prepare();
                while (true) {
                    if (lock) {
                        if (listHelp!=null && !listHelp.isEmpty() && listHelp.size() > poradie) {
                            Iterator it = listHelp.get(poradie).entrySet().iterator();
                            HashMap.Entry pair = (HashMap.Entry) it.next();
                            final  android.location.Location  loc = ( android.location.Location) pair.getKey();
                            final float data = (float) pair.getValue();
                            Handler = new Bump(loc, data, bumpsManualHelp.get(poradie),bumpsTypeHelp.get(poradie),bumpsTextlHelp.get(poradie),androidId);
                            Handler.getResponse(new CallBackReturn() {
                                public void callback(String results) {
                                    if (results.equals("success")) {
                                        Log.d(TAG, "saveBump success " );
                                        int num = poradie;
                                        while (true) {
                                            if (updatesLock.tryLock()) {
                                                try {
                                                    DatabaseOpenHelper databaseHelper =null;
                                                    SQLiteDatabase database = null;
                                                    try {
                                                        databaseHelper = new DatabaseOpenHelper(context);
                                                        database = databaseHelper.getWritableDatabase();
                                                        checkIntegrityDB(database);
                                                        // ak mi prišlo potvrdenie o odoslaní, mažem z db
                                                        database.beginTransaction();
                                                        Log.d(TAG, "saveBump success delete db ");
                                                        database.execSQL("DELETE FROM new_bumps WHERE ROUND(latitude,7)= ROUND(" + loc.getLatitude() + ",7)  and ROUND(longitude,7)= ROUND(" + loc.getLongitude() + ",7)"
                                                                + " and  ROUND(intensity,6)==ROUND(" + data + ",6) and type="+bumpsTypeHelp.get(num)+" and manual=" + bumpsManualHelp.get(num) + "");
                                                        Log.d("TEST", "mazem");
                                                        Log.d(TAG, "mazem " + listHelp.get(num).toString());
                                                        Log.d(TAG, "mazem " + bumpsManualHelp.get(num).toString());
                                                        listHelp.remove(num);
                                                        bumpsTypeHelp.remove(num);
                                                        bumpsManualHelp.remove(num);
                                                        bumpsTextlHelp.remove(num);
                                                        database.setTransactionSuccessful();
                                                        database.endTransaction();
                                                    }
                                                    finally {
                                                        if (database!=null) {
                                                            database.close();
                                                            databaseHelper.close();
                                                        }
                                                    }
                                                    checkCloseDb(database);

                                                } finally {
                                                    Log.d(TAG, "saveBump unlock " );
                                                    updatesLock.unlock();
                                                    break;
                                                }
                                            } else {
                                                Log.d(TAG, "saveBump try lock " );
                                                try {
                                                    Random ran = new Random();
                                                    int x = ran.nextInt(20) + 1;
                                                    Thread.sleep(x);
                                                } catch (InterruptedException e) {
                                                    e.getMessage();
                                                }
                                            }
                                        }
                                        lock = true;
                                    } else {
                                        Log.d(TAG, "saveBump error" );
                                        int num = poradie;
                                        num++;
                                        poradie = num;
                                        lock = true;
                                    }
                                }
                            });
                        } else {
                            break;
                        }
                    }
                }
                Log.d(TAG, "saveBump odosielanie na server skončilo" );
                while (true) {
                    if (lockAdd.tryLock()) {   // zamknem zoznam, a tie čo sa mi nepodarilo odoslať vrátim
                        try {
                            while (true) {
                                if (updatesLock.tryLock()) {
                                    try {
                                        if (listHelp.size() > 0 && accelerometer.getPossibleBumps().size() > 0) {
                                            Log.d(TAG, "saveBump aktualizujem zoznam" );
                                            int i = 0;
                                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(context);
                                            SQLiteDatabase database = databaseHelper.getWritableDatabase();
                                            checkIntegrityDB(database);
                                            for (HashMap< android.location.Location, Float> oldList : listHelp) {
                                                Iterator oldListIteam = oldList.entrySet().iterator();
                                                while (oldListIteam.hasNext()) {
                                                    HashMap.Entry oldData = (HashMap.Entry) oldListIteam.next();
                                                    android.location.Location oldLocation = ( android.location.Location) oldData.getKey();
                                                    i = 0;
                                                    for (HashMap< android.location.Location, Float> newList : accelerometer.getPossibleBumps()) {

                                                        Iterator newListIteam = newList.entrySet().iterator();
                                                        while (newListIteam.hasNext()) {
                                                            HashMap.Entry newData = (HashMap.Entry) newListIteam.next();
                                                            android.location.Location newLocation = ( android.location.Location) newData.getKey();
                                                            // ak sa zhoduju location, tak updatujem hodnoty
                                                            if ((oldLocation.getLatitude() == newLocation.getLatitude()) &&
                                                                    (oldLocation.getLongitude() == newLocation.getLongitude())) {
                                                                // staršia hodnota je väčšia, tak prepíšem na väčšiu hodnotu
                                                                if ((Float) oldData.getValue() > (Float) newData.getValue())
                                                                    accelerometer.getPossibleBumps().get(0).put(newLocation, (Float) oldData.getValue());
                                                                // ak je stará hodnota menšia, updatujem databazu kde je uložená menšia
                                                                if ((Float) oldData.getValue() < (Float) newData.getValue())
                                                                    database.execSQL("UPDATE new_bumps  SET intensity=ROUND(" + (Float) newData.getValue() + ",6) WHERE latitude=" + oldLocation.getLatitude() + " and  longitude=" + oldLocation.getLongitude()
                                                                            + " and  ROUND(intensity,6)==ROUND(" + (Float) oldData.getValue() + ",6)");
                                                                // mažem s pomocného zoznamu updatnuté hodnoty
                                                                listHelp.remove(i);
                                                                bumpsManualHelp.remove(i);
                                                                bumpsTypeHelp.remove(i);
                                                                bumpsTextlHelp.remove(i);
                                                            }
                                                        }
                                                        i++;
                                                    }
                                                }
                                            }
                                            database.close();
                                            databaseHelper.close();
                                            checkCloseDb(database);
                                            // doplnim do zoznamu povodné, ktoré sa nezmenili
                                            accelerometer.getPossibleBumps().addAll(listHelp);
                                            accelerometer.getBumpsManual().addAll(bumpsManualHelp);
                                            accelerometer.gettypeDetect().addAll(bumpsTypeHelp);
                                            accelerometer.gettextDetect().addAll(bumpsTextlHelp);
                                        } else if (listHelp.size() > 0) {
                                            // nepribudli nové hodnoty, tak tam vrátim pôvodné
                                            accelerometer.getPossibleBumps().addAll(listHelp);
                                            accelerometer.getBumpsManual().addAll(bumpsManualHelp);
                                            accelerometer.gettypeDetect().addAll(bumpsTypeHelp);
                                            accelerometer.gettextDetect().addAll(bumpsTextlHelp);
                                        }
                                    } finally {
                                        updatesLock.unlock();
                                        break;
                                    }
                                } else {
                                    Log.d(TAG, "saveBump db try lock" );
                                    try {
                                        Thread.sleep(5);
                                    } catch (InterruptedException e) {
                                        e.getMessage();
                                    }
                                }
                            }
                        } finally {
                            Log.d(TAG, "saveBump zoznam unlock" );
                            lockAdd.unlock();
                            break;
                        }
                    } else {
                        try {
                            Log.d(TAG, "saveBump zoznam try lock" );
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                        }
                    }
                }

                listHelp = null;
                bumpsManualHelp = null;
                bumpsTypeHelp = null;
                bumpsTextlHelp = null;

                if (regularUpdatesLock) {
                    Log.d(TAG, "updates == 1 v save" );
                    // updates == 1 v save, takže je potrebne  vyžiadať update
                    updates = 0;
                    regularUpdatesLock = false;
                    lockHandler = false;
                    syncList(gps.getCurrentLatLng().latitude,gps.getCurrentLatLng().longitude, 1);
                } else if (lockHandler) {
                    lockHandler = false;
                    getBumpsWithLevel();
                }
                Looper.loop();
            }
        }.start();
    }

    String StringlistID = null;
    String StringlistDate = null;
    String StringlistCount = null;
    
    private void syncList(final Double langtitude, final Double longtitude, final Integer enable_net)  {
        Log.d(TAG, "syncList start");
        new Thread() {
            public void run() {
                while (true) {
                    if (updatesLock.tryLock()) {
                        try {
                            SimpleDateFormat now, ago;
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(new Date());
                            now = new SimpleDateFormat("yyyy-MM-dd");       // vztiahnutie aktu8lneho datumu
                            String now_formated = now.format(cal.getTime());
                            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE) - 280); // datumu z pred 280 dni
                            ago = new SimpleDateFormat("yyyy-MM-dd");
                            String ago_formated = ago.format(cal.getTime());
                            StringlistID = null;   // nulovanie hodnot
                            StringlistDate = null;
                            StringlistCount = null;

                            String selectQuery = "SELECT b_id_bumps, last_modified, count  FROM " + TABLE_NAME_BUMPS
                                    + " where (last_modified BETWEEN '" + ago_formated + " 00:00:00' AND '" + now_formated + " 23:59:59') and  "
                                    + " (ROUND(latitude,1)==ROUND(" + langtitude + ",1) and ROUND(longitude,1)==ROUND(" + longtitude + ",1))";

                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(context);
                            SQLiteDatabase database = databaseHelper.getReadableDatabase();
                            checkIntegrityDB(database);
                            database.beginTransaction();
                            Cursor cursor = database.rawQuery(selectQuery, null);
                            numRecord = 0 ;
                            if (cursor != null && cursor.moveToFirst()) {
                                do {
                                    if (!cursor.isNull(0) && !cursor.isNull(1) ) {
                                        StringlistID = StringlistID+ "@"+cursor.getInt(0);
                                        StringlistDate = StringlistDate+ "@"+cursor.getString(1);
                                        StringlistCount = StringlistCount+ "@"+cursor.getInt(2);
                                        numRecord++;
                                        Log.d(TAG, "syncList b_id_bumps " + cursor.getInt(0));
                                        Log.d(TAG, "syncList last_modified " + cursor.getString(1));
                                    }
                                } while (cursor.moveToNext());
                            }
                            database.setTransactionSuccessful();
                            database.endTransaction();
                            database.close();
                            databaseHelper.close();
                            checkCloseDb(database);
                        } finally {
                            Log.d(TAG, "syncList unlock");
                            updatesLock.unlock();
                            break;
                        }
                    } else {
                        Log.d(TAG, "syncList try lock");
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                            e.getMessage();
                        }
                    }
                }
                net = enable_net; // povolenie na aktualizaciu alebo update
                lang_database = langtitude; // ukladam pozície, aby som všade pracoval s rovnakými
                longt_database = longtitude;
                new UpdateList().execute();
            }
        }.start();
    }

    class UpdateList extends AsyncTask<String, Void, JSONArray> {

        protected JSONArray doInBackground(String... args) {
            Log.d(TAG, "UpdateList start");
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("date", getDate(new Date().getTime(), "yyyy-MM-dd HH:mm:ss")));
            params.add(new BasicNameValuePair("latitude", String.valueOf(lang_database)));
            params.add(new BasicNameValuePair("longitude", String.valueOf(longt_database)));
            params.add(new BasicNameValuePair("listID", StringlistID));
            params.add(new BasicNameValuePair("listDate", StringlistDate));
            params.add(new BasicNameValuePair("listCount", StringlistCount));
            params.add(new BasicNameValuePair("net", String.valueOf(net)));
            Log.d(TAG, "UpdateList odosielam požiadavku na server");
            JSONObject json = jsonParser.makeHttpRequest("http://sport.fiit.ngnlab.eu/sync_bump.php", "POST", params);
            if (json == null) {
                JSONArray response = new JSONArray();
                try {
                    Log.d(TAG, "UpdateList - error");
                    response.put(0, "error");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                return response;
            }
            try {
                int success = json.getInt("success");
                Log.d(TAG, "UpdateList - success" + success);
                JSONArray response = new JSONArray();
                if (success == 0) {   // mam nove data na stiahnutie
                    bumps = json.getJSONArray("bumps");
                    Log.d(TAG, "UpdateList - new data");
                    return bumps;
                } else if (success == 1) {  // potrebujem potvrdit nove data na stiahnutie , nemám povolený net ale bol flag
                    response.put(0, "update");
                    Log.d(TAG, "UpdateList - need update");
                    return response;
                } else {
                    Log.d(TAG, "UpdateList - no data");
                    return null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                JSONArray response = new JSONArray();
                try {
                    response.put(0, "error");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                Log.d(TAG, "UpdateList - JSONException");
                return response;
            }
        }

        protected void onPostExecute(JSONArray array) {
            if (array == null) { // žiadne nové data v bumps, zisti collisons
                Log.d(TAG, "UpdateList onPostExecute - no data");
                if (gps != null && gps.getCurrentLatLng() != null) {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mapLayer.getAllBumps(gps.getCurrentLatLng().latitude, gps.getCurrentLatLng().longitude);
                        }
                    });
                }
                save_photo();
                return;
            }

            try {
                 if (array.get(0).equals("error")) {
                    Log.d(TAG, "UpdateList onPostExecute - error");
                    if (gps != null && gps.getCurrentLatLng() != null) {
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mapLayer.getAllBumps(gps.getCurrentLatLng().latitude, gps.getCurrentLatLng().longitude);

                            }
                        });
                        save_photo();
                        return;
                    }
                } else if (array.get(0).equals("update")) { // mam nove data, zistit aj collision a potom upozorni uživatela
                    Log.d(TAG, "UpdateList onPostExecute - update");
                    GetUpdateAction();
                } else {
                     Log.d(TAG, "UpdateList onPostExecute - new data, update databazu");
                     Thread t = new Thread() {    // insertujem nove data do databazy
                         public void run() {
                            Boolean error = false;
                             while (true) {
                                 if (updatesLock.tryLock()) {
                                     try {
                                         numUpdate = 0;
                                         DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(context);
                                         SQLiteDatabase database = databaseHelper.getWritableDatabase();
                                         checkIntegrityDB(database);
                                         database.beginTransaction();
                                         for (int i = 0; i < bumps.length(); i++) {
                                             numUpdate++;
                                             JSONObject data = null;
                                             try {
                                                 data = bumps.getJSONObject(i);
                                             } catch (JSONException e) {
                                                 error = true;  // zle parsovanie dat, zaznamenám chybu, neuložim do databázy
                                                 e.printStackTrace();
                                                 break;
                                             }
                                             double latitude = 0, longitude = 0;
                                             int count = 0, b_id = 0, rating = 0, manual = 0, type = 0, fix =0;
                                             String last_modified = null, info = null;
                                             if (data != null) {
                                                 try {
                                                     b_id = data.getInt("b_id");
                                                     latitude = data.getDouble("latitude");
                                                     longitude = data.getDouble("longitude");
                                                     type = data.getInt("type");
                                                     count = data.getInt("count");
                                                     fix = data.getInt("fix");
                                                     info = data.getString("info");
                                                     rating = data.getInt("rating");
                                                     last_modified = data.getString("last_modified");
                                                     manual = data.getInt("manual");
                                                     Log.d(TAG, "UpdateList b_id" + b_id);
                                                     Log.d(TAG, "UpdateList latitude " + latitude);
                                                     Log.d(TAG, "UpdateList longitude " + longitude);
                                                     Log.d(TAG, "UpdateList last_modified " + last_modified);
                                                     Log.d(TAG, "UpdateList info " + info);
                                                     Cursor cursor = null;
                                                     String sql = "SELECT * FROM my_bumps WHERE b_id_bumps=" + b_id;
                                                     try {
                                                         cursor = database.rawQuery(sql, null);
                                                         if (cursor.getCount() > 0) {
                                                             Log.d(TAG, "UpdateList exist b_id");
                                                             sql = "UPDATE " + TABLE_NAME_BUMPS + " SET rating=rating+ " + rating + ", fix="+fix+", count=" + count +", last_modified='"+last_modified+"' WHERE b_id_bumps=" + b_id;
                                                             database.execSQL(sql);
                                                         }
                                                         else {
                                                             Log.d(TAG, "UpdateList new b_id");
                                                             ContentValues contentValues = new ContentValues();
                                                             contentValues.put(Provider.bumps_detect.B_ID_BUMPS, b_id);
                                                             contentValues.put(Provider.bumps_detect.COUNT, count);
                                                             contentValues.put(Provider.bumps_detect.LAST_MODIFIED, last_modified);
                                                             contentValues.put(Provider.bumps_detect.LATITUDE, latitude);
                                                             contentValues.put(Provider.bumps_detect.LONGTITUDE, longitude);
                                                             contentValues.put(Provider.bumps_detect.MANUAL, manual);
                                                             contentValues.put(Provider.bumps_detect.INFO, info);
                                                             contentValues.put(Provider.bumps_detect.RATING, rating);
                                                             contentValues.put(Provider.bumps_detect.TYPE, type);
                                                             contentValues.put(Provider.bumps_detect.FIX, fix);
                                                             database.insert(TABLE_NAME_BUMPS, null, contentValues);
                                                         }
                                                     } finally {
                                                         if (cursor != null)
                                                             cursor.close();
                                                     }
                                                 } catch (JSONException e) {
                                                     error = true;
                                                     e.printStackTrace();
                                                     break;
                                                 }
                                             }
                                         }
                                         if (!error) {  // insert prebehol v poriadku, ukonči transakciu
                                             database.setTransactionSuccessful();
                                             database.endTransaction();
                                             database.close();
                                             databaseHelper.close();
                                             checkCloseDb(database);
                                             Log.d(TAG, "UpdateList no error");
                                         } else { // nastala chyba, načitaj uložene vytlky
                                             database.endTransaction();
                                             database.close();
                                             databaseHelper.close();
                                             checkCloseDb(database);
                                             Log.d(TAG, "UpdateList  error");
                                         }
                                     } finally {
                                         updatesLock.unlock();
                                         break;
                                     }
                                 } else {
                                     Log.d(TAG, "UpdateList try lock");
                                     try {
                                         Random ran = new Random();
                                         int x = ran.nextInt(20) + 1;
                                         Thread.sleep(x);
                                     } catch (InterruptedException e) {
                                         e.getMessage();
                                     }
                                 }
                             }
                             if (gps != null && gps.getCurrentLatLng() != null) {
                                 context.runOnUiThread(new Runnable() {
                                     @Override
                                     public void run() {
                                         mapLayer.getAllBumps(gps.getCurrentLatLng().latitude, gps.getCurrentLatLng().longitude);
                                     }
                                 });
                             }
                             save_photo();
                             Log.d(TAG, "UpdateList končí");
                             Log.d(TAG, "UpdateList numRecord- " + numRecord + " numUpdate- " + numUpdate);
                         }
                     };
                     t.start();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private  boolean isConnectedWIFI() {   // či je pripojená wifi alebo mobilný internet
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        Log.d(TAG, "isConnectedWIFI stav " + isConnected);
        if (isConnected) {
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI)
                return true;
            else
                return false;
        }
        return false;
    }

    private boolean isEneableDownload() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d(TAG, "isEneableDownload stav - " +String.valueOf(prefs.getBoolean("net", Boolean.parseBoolean(null))));
        return prefs.getBoolean("net", Boolean.parseBoolean(null));
    }

    public void freeMemory(){
        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();
    }

    ArrayList<String> listLatitude = new ArrayList<String>();
    ArrayList<String> listLongitude = new ArrayList<String>();
    ArrayList<String> listType = new ArrayList<String>();
    ArrayList<String> listDate = new ArrayList<String>();
    ArrayList<String> listPath = new ArrayList<String>();
    int por_photo = 0;

    public void save_photo() {
        new Thread() {
            public void run() {
                while (true) {
                    if (updatesLock.tryLock()) {
                        try {
                            Log.d(TAG, "save_photo start ");
                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(context);
                            SQLiteDatabase database = databaseHelper.getWritableDatabase();
                            checkIntegrityDB(database);
                            database.beginTransaction();
                            String selectQuery = "SELECT latitude,longitude,type,created_at,path FROM new_photo ";
                            Cursor cursor = database.rawQuery(selectQuery, null);
                            if (cursor != null && cursor.moveToFirst()) {
                                do {
                                    listLatitude.add(cursor.getString(0));
                                    listLongitude.add(cursor.getString(1));
                                    listType.add(cursor.getString(2));
                                    listDate.add(cursor.getString(3));
                                    listPath.add(cursor.getString(4));
                                } while (cursor.moveToNext());
                            }
                            database.setTransactionSuccessful();
                            database.endTransaction();
                            database.close();
                            databaseHelper.close();
                            checkCloseDb(database);
                        } finally {
                            updatesLock.unlock();
                            break;
                        }
                    } else {
                        Log.d(TAG, "save_photo try lock");
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                            e.getMessage();
                        }
                    }
                }

                for (por_photo = 0; por_photo < listLatitude.size(); por_photo++ ) {
                    File file = new File(listPath.get(por_photo));
                    if (file.exists()) {
                        HandlerPhoto = new UploadPhoto(context, listLatitude.get(por_photo), listLongitude.get(por_photo), listType.get(por_photo), listDate.get(por_photo), listPath.get(por_photo));
                        HandlerPhoto.getResponse(new CallBackReturn() {
                            public void callback(String results) {
                                if (results.equals("success")) {
                                    Log.d(TAG, "UploadPhoto success ");
                                    delete_db_photo ();
                                } else
                                    Log.d(TAG, "UploadPhoto errror ");
                            }
                        });
                    }else
                        delete_db_photo ();
                }
                listLatitude.clear();
                listLongitude.clear();
                listType.clear();
                listDate.clear();
                listPath.clear();
            }
        }.start();
    }

    public void  delete_db_photo () {
        File file = new File(listPath.get(por_photo));
        if (file.exists()) {
            boolean deleted = file.delete();
            Log.d(TAG, "file delete  " + deleted);
        }

        while (true) {
            if (updatesLock.tryLock()) {
                try {
                    DatabaseOpenHelper databaseHelper = null;
                    SQLiteDatabase database = null;
                    try {
                        databaseHelper = new DatabaseOpenHelper(context);
                        database = databaseHelper.getWritableDatabase();
                        checkIntegrityDB(database);
                        database.beginTransaction();
                        database.execSQL("DELETE FROM new_photo WHERE path='" + listPath.get(por_photo) + "' ");
                        Log.d(TAG, " save_photo delete from DB");
                        database.setTransactionSuccessful();
                        database.endTransaction();
                    } finally {
                        if (database != null) {
                            database.close();
                            databaseHelper.close();
                        }
                    }
                    checkCloseDb(database);

                } finally {
                    Log.d(TAG, "save_photo unlock ");
                    updatesLock.unlock();
                    break;
                }
            }
        }
    }
}