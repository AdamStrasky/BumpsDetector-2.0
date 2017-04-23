package navigationapp.main_application;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import navigationapp.R;
import com.google.android.gms.maps.model.LatLng;

import org.w3c.dom.Document;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static navigationapp.main_application.Accelerometer.getDistance;
import static navigationapp.main_application.FragmentActivity.checkCloseDb;
import static navigationapp.main_application.FragmentActivity.checkIntegrityDB;
import static navigationapp.main_application.FragmentActivity.global_gps;
import static com.google.maps.android.PolyUtil.isLocationOnEdge;
import static navigationapp.main_application.FragmentActivity.isEneableShowText;
import static navigationapp.main_application.FragmentActivity.updatesLock;

public class Location {

    private final int lifo_size = 60;
    private boolean  first_start_estimation = false;
    private boolean  road = false;  // nastaviť na tru ked kliknem  na navigate, ked kliknem na end tak false
    Lock lock_position = new ReentrantLock();
    Lock lock_choise = new ReentrantLock();
    private boolean  new_data = false;
    private boolean  start_bumps = true;
    Lock lock_stop = new ReentrantLock();
    private boolean  get_road = false;
    Thread collision_thread = null ,estimation_thread = null;
    FragmentActivity activity =null;
    private ArrayList<Position> LIFO;
    ArrayList<LatLng> select_road = null,  choise_bump = null;
    LatLng position = null;
    TextToSpeech tts = null;
    Activity context = null;
    public  final String TAG = "Location";
    public Location(Activity context) {
        this.context= context;
        select_road = new  ArrayList<LatLng>();
        LIFO = new ArrayList<Position>();
        tts=new TextToSpeech(context, new TextToSpeech.OnInitListener() {
             @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    int result=tts.setLanguage(Locale.UK);
                    if(result==TextToSpeech.LANG_MISSING_DATA ||
                            result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("error", "This Language is not supported");
                    }
                }
                else
                    Log.e("error", "Initilization Failed!");
            }
        });
        Log.d(TAG, "Location inicialization");
        analyzePosition();
    }

    public void analyzePosition() {
        Thread analyze_thread = new Thread() {
            android.location.Location location = null;
            public void run() {
                Looper.prepare();
                while(true) {
                    if (global_gps != null && global_gps.getmCurrentLocation() != null) {
                        if (lock_position.tryLock()) {
                            try {
                                if (LIFO.size() == lifo_size)
                                    LIFO.remove(0);
                                location = global_gps.getmCurrentLocation();
                                Position new_position = new Position(location.getSpeed(), location.getLatitude(), location.getLongitude(), location.getTime());
                                LIFO.add(new_position);
                            } finally {
                                lock_position.unlock();
                            }
                        }
                        if (road && select_road != null) {   // vychylil som sa s trasy, nutne prepočítať trasu
                            if ( get_road && select_road != null && !isLocationOnEdge(new LatLng(location.getLatitude(), location.getLongitude()), select_road, true, 4.0)) {
                                get_road = false;
                               if (collision_thread != null && collision_thread.isAlive()) {
                                    if (lock_stop.tryLock()) {
                                       try {
                                            collision_thread.interrupt();
                                            collision_thread = null;
                                            collision_places();
                                            first_start_estimation = false;
                                            collision_thread.start();
                                            if (estimation_thread != null && estimation_thread.isAlive()) {
                                                estimation_thread.interrupt();
                                                estimation_thread = null;
                                            }
                                       } finally {
                                            lock_stop.unlock();
                                       }
                                    }
                                }
                            } else {
                               if (position != null && getDistance((float) location.getLatitude(), (float) location.getLongitude(), (float) position.latitude, (float) position.longitude) < 10) {
                                    if (lock_stop.tryLock()) {
                                        try {   // ukončujem navigaciu, som v cieli
                                            stop_collision_thread();
                                            stop_estimation_thread();
                                        } finally {
                                            lock_stop.unlock();
                                        }
                                    }
                                }
                            }
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) { }
                }
            }
        };
        analyze_thread.start();
    }

    public void stop_collison_navigate() {
         start_bumps = true;
        Thread stop_navigate = new Thread() {
            public void run() {
                Looper.prepare();
                while (true) {
                    if (lock_stop.tryLock()) {
                        try {
                            stop_collision_thread();
                            stop_estimation_thread();
                            start_bumps = false;
                            get_road = false;
                        } finally {
                            lock_stop.unlock();
                            break;
                        }
                    } else {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.getMessage();
                        }
                    }
                }
            }
        };
        stop_navigate.start();
    }

    public void stop_collision_thread() {
        if (collision_thread!=null && collision_thread.isAlive()) {
            collision_thread.interrupt();
            collision_thread= null;
            first_start_estimation = false;
            position=null;
        }
    }

    public void stop_estimation_thread() {
        if (estimation_thread!=null && estimation_thread.isAlive()) {
            estimation_thread.interrupt();
            estimation_thread= null;
        }
    }

    public void bumps_on_position(FragmentActivity fragmentActivity, final LatLng to_position) {
        activity = fragmentActivity;
        Thread t = new Thread() {
            public void run() {
                Looper.prepare();
                while (true) {
                    if (start_bumps == false ) {
                        road = true;
                        Log.d(TAG, "bumps_on_position start ");
                        position = to_position;
                        break;
                    }
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.getMessage();
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
        collision_places();
        collision_thread.start();
    }

    public void collision_places() {
        collision_thread = new Thread() {
            public void run() {
                Looper.prepare();
                try {
                    if (this.isInterrupted()) {
                        Log.d(TAG, "collision_places  isInterrupted ");
                    }
                    while(!this.isInterrupted() ) {
                        int time_to_sleeep = 40000;
                        Log.d(TAG, "collision_places - name of pid " + String.valueOf(this.getId()));

                        if (Thread.currentThread().isInterrupted()) {
                            Log.d(TAG, "collision_places  InterruptedException ");
                            throw new InterruptedException();
                        }

                        double longitude = 0, latitude = 0;
                        if (global_gps != null && global_gps.getmCurrentLocation() != null) {
                            Log.d(TAG, "collision_places  get gps ");
                            latitude = global_gps.getmCurrentLocation().getLatitude();
                            longitude = global_gps.getmCurrentLocation().getLongitude();
                            LatLng my_position = new LatLng(latitude,longitude);
                            if (!get_road) {
                                Log.d(TAG, "collision_places - get_road false ");
                                select_road = directionPoint(position, my_position, this);  // vratim cestu
                                if (select_road!=null) {
                                    Log.d(TAG, "collision_places - roud found ");
                                    get_road = true;
                                }else
                                    Log.d(TAG, "collision_places - roud not found ");
                            }else
                                Log.d(TAG, "collision_places - get_road true ");;

                            ArrayList<LatLng> all_bumps = new ArrayList<LatLng>();
                            while (true) {
                                if (updatesLock.tryLock())  {
                                    try  {
                                        SimpleDateFormat now, ago;
                                        Calendar cal = Calendar.getInstance();
                                        cal.setTime(new Date());
                                        now = new SimpleDateFormat("yyyy-MM-dd");
                                        String now_formated = now.format(cal.getTime());
                                        // posun od dnesneho dna o 280 dni
                                        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE) - 280);
                                        ago = new SimpleDateFormat("yyyy-MM-dd");
                                        String ago_formated = ago.format(cal.getTime());
                                        // ziskam sučasnu poziciu
                                        // seleknutie vytlk z oblasti a starych 280 dni
                                        DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(context);
                                        SQLiteDatabase database = databaseHelper.getWritableDatabase();
                                        checkIntegrityDB(database);
                                        database.beginTransaction();
                                        String selectQuery = "SELECT latitude,longitude FROM my_bumps WHERE admin_fix=0 AND " +
                                                " ( last_modified BETWEEN '" + ago_formated + " 00:00:00' AND '" + now_formated + " 23:59:59') and  "
                                                + " (ROUND(latitude,2)==ROUND(" + latitude + ",2) and ROUND(longitude,2)==ROUND(" + longitude + ",2)) AND type=0";

                                        Cursor cursor = null;
                                        try {
                                            cursor = database.rawQuery(selectQuery, null);
                                            if (cursor.moveToFirst()) {
                                                do {
                                                    all_bumps.add(new LatLng(cursor.getDouble(0), cursor.getDouble(1)));
                                                }
                                                while (cursor.moveToNext());
                                            }
                                        } finally {
                                            if (cursor != null)
                                                cursor.close();
                                        }
                                        database.setTransactionSuccessful();
                                        database.endTransaction();
                                        database.close();
                                        databaseHelper.close();
                                        checkCloseDb(database);
                                        Log.d(TAG, "collision_places  bump size "+all_bumps.size());
                                    }
                                    finally {
                                        updatesLock.unlock();
                                        break;
                                    }
                                } else {
                                    try {
                                        Thread.sleep(20);
                                    } catch (InterruptedException e) {
                                        Log.d(TAG, "collision_places  InterruptedException ");
                                        throw new InterruptedException("Stopped by ifInterruptedStop()");
                                    }
                                }
                            }

                            if (select_road != null && select_road.size() > 0) {
                                while (true) {
                                    if (lock_choise.tryLock()) {
                                        try {
                                            choise_bump = new ArrayList<LatLng>();
                                            for (int i = 0; i < all_bumps.size(); i++) {
                                                if (select_road!= null && isLocationOnEdge(all_bumps.get(i), select_road, true, 4.0)) {
                                                    choise_bump.add(all_bumps.get(i));
                                                    Log.d(TAG, "collision_places  add bump to warning " + i);
                                                }
                                            }
                                        }
                                        finally {
                                            lock_choise.unlock();
                                            break;
                                        }
                                    }
                                    else   {
                                        try {
                                            Thread.sleep(20);
                                        } catch (InterruptedException e) {
                                            Log.d(TAG, "collision_places  InterruptedException ");
                                            throw new InterruptedException();
                                        }
                                    }
                                }
                                all_bumps = new ArrayList<LatLng>();

                                if (this.isInterrupted()) {
                                    Log.d(TAG, "collision_places  InterruptedException, pred estimation ");
                                    throw new InterruptedException();
                                } else {
                                    new_data = true;
                                    if (!first_start_estimation && estimation_thread == null) {
                                        first_start_estimation = true;
                                        estimation();
                                        Log.d(TAG, "collision_places  estimation start ");
                                        estimation_thread.start();
                                    }
                                }
                            } else {
                                time_to_sleeep = 5000;
                                Log.d(TAG, "collision_places  no selected road");
                            }
                            try {
                                Thread.sleep(time_to_sleeep);
                            } catch (InterruptedException e) {
                                Log.d(TAG,"collision_places throw sleep  for 40 seconds ");
                                throw new InterruptedException();
                            }
                        }else
                            Log.d(TAG, "collision_places no gps " );
                    }
                } catch (InterruptedException t) {
                    Log.d(TAG, "cath InterruptedException " + this.getId());
                }
                Log.d(TAG, " collision_places end pid  "+String.valueOf(this.getId()));
            }
        };
    }

    public void estimation() {
        Log.d(TAG, "estimation start");
        estimation_thread = new Thread() {
            public void run() {
                Looper.prepare();
                float speed = 0;
                float avarage_speed = 0;
                long time = 0;
                double distance= 0;
                float avarage_speed_second = 0;
                float best_cause = 0;
                double longitude = 0, latitude= 0;
                int time_stop = 1000;
                List<LatLng> directionPoint = null;
                double  previous_distance = - 1;
                ArrayList<LatLng> bump_actual = new ArrayList<LatLng>();
                try {
                    while(!this.isInterrupted() ) {
                        while (true) {
                            if (lock_position.tryLock()) {
                                try {
                                    for (int i = 0; i < LIFO.size(); i++) {
                                        speed += LIFO.get(i).getSpeed();
                                    }
                                    avarage_speed = speed / LIFO.size();
                                    distance = getDistance((float) LIFO.get(0).getLatitude(), (float) LIFO.get(0).getLongitude(), (float) LIFO.get(LIFO.size() - 1).getLatitude(), (float) LIFO.get(LIFO.size() - 1).getLongitude());
                                    time = LIFO.get(LIFO.size() - 1).getTime() - LIFO.get(0).getTime();
                                } finally {
                                    lock_position.unlock();
                                    break;
                                }
                            } else {
                                try {
                                    Thread.sleep(20);
                                } catch (InterruptedException e) {
                                    throw new InterruptedException("");
                                }
                            }
                        }

                        long minutes = TimeUnit.MILLISECONDS.toMinutes((long) time);
                        time -= TimeUnit.MINUTES.toMillis(minutes);
                        long seconds = TimeUnit.MILLISECONDS.toSeconds(time);
                        avarage_speed_second = (float) (distance/ (minutes+seconds));

                        if (avarage_speed > avarage_speed_second )
                            best_cause = avarage_speed;
                        else
                            best_cause = avarage_speed_second;

                        double actual_distance = 999999999 ;

                        if (global_gps != null && global_gps.getmCurrentLocation() != null) {
                            latitude = global_gps.getmCurrentLocation().getLatitude();
                            longitude = global_gps.getmCurrentLocation().getLongitude();
                            if (new_data) {
                                if (lock_choise.tryLock()) {
                                    try {
                                        bump_actual = new ArrayList<LatLng>();
                                        if (choise_bump != null && choise_bump.size() > 0)
                                            bump_actual.addAll(choise_bump);
                                    } finally {
                                        lock_choise.unlock();
                                    }
                                    new_data = false;
                                    if (bump_actual != null && bump_actual.size() > 0) {
                                        directionPoint = sortLocations(bump_actual, latitude, longitude);
                                        actual_distance = getDistance((float) latitude, (float) longitude, (float) directionPoint.get(0).latitude, (float) directionPoint.get(0).longitude);
                                        previous_distance = actual_distance;
                                    }
                                } else {
                                    if (directionPoint != null &&  directionPoint.size() > 0 )
                                        actual_distance = getDistance((float) latitude, (float) longitude, (float) directionPoint.get(0).latitude, (float) directionPoint.get(0).longitude);

                                    if (previous_distance < actual_distance) {
                                        if (bump_actual != null && bump_actual.size() > 0) {
                                            if (directionPoint != null) {
                                                directionPoint = sortLocations(bump_actual, latitude, longitude);
                                                actual_distance = getDistance((float) latitude, (float) longitude, (float) directionPoint.get(0).latitude, (float) directionPoint.get(0).longitude);
                                                previous_distance = actual_distance;
                                            }
                                        }
                                    }
                                }

                            } else {
                                if (directionPoint != null &&  directionPoint.size() > 0 )
                                    actual_distance = getDistance((float) latitude, (float) longitude, (float) directionPoint.get(0).latitude, (float) directionPoint.get(0).longitude);

                                if (previous_distance < actual_distance) {
                                    if (bump_actual != null && bump_actual.size() > 0) {
                                        if (directionPoint != null) {
                                            directionPoint = sortLocations(bump_actual, latitude, longitude);
                                            actual_distance = getDistance((float) latitude, (float) longitude, (float) directionPoint.get(0).latitude, (float) directionPoint.get(0).longitude);
                                            previous_distance = actual_distance;
                                        }
                                    }
                                }
                            }

                            double times_to_sleep = actual_distance / best_cause;

                            //  - čas pred výtlkom
                            if (String.valueOf(times_to_sleep).equals("NaN") || String.valueOf(times_to_sleep).equals("Infinity")) {
                                times_to_sleep = 999999;
                            }
                            long result = TimeUnit.SECONDS.toMillis((long) times_to_sleep);
                            double convert_time = result;
                            time_stop = 0;
                            int treshold = 20000;

                            if (convert_time > treshold || convert_time < 0)
                                time_stop = treshold;
                            else if (convert_time < 10000) {
                                if (this.isInterrupted()) {
                                    throw new InterruptedException("");
                                }
                                    if (!isEneableVoice()) {
                                    final  int rounded_distance = (int) Math.round(actual_distance);

                                    while (tts.isSpeaking()){ }
                                    context.runOnUiThread(new Runnable() {
                                        public void run() {
                                            tts.speak("for" + rounded_distance + " meters is detected bump", TextToSpeech.QUEUE_FLUSH, null);
                                        }
                                    });
                                    while (tts.isSpeaking()){ }
                                }
                                else {
                                   if (isEneableShowText(context)) {
                                       context.runOnUiThread(new Runnable() {
                                           public void run() {
                                               Toast.makeText(context, context.getResources().getString(R.string.attention_bump), Toast.LENGTH_SHORT).show();
                                           }
                                       });
                                   }
                                }
                                previous_distance = -1;
                                if (directionPoint!= null && directionPoint.size() > 0)
                                    directionPoint.remove(0);
                                time_stop = 0;
                            } else
                                time_stop = (int) convert_time;

                        } else
                            Log.d(TAG, "estimation no gps ");
                        try {
                            Thread.sleep(time_stop);
                        } catch (InterruptedException e) {
                            throw new InterruptedException();
                        }
                    }
                } catch (InterruptedException t) {
                    Log.d(TAG, "cath interuption " + this.getId());
                }
                Looper.loop();
            }
        };
    }

    public static List<LatLng> sortLocations(List<LatLng> locations, final double myLatitude,final double myLongitude) {
        Comparator comp = new Comparator<LatLng>() {
            @Override
            public int compare(LatLng o, LatLng o2) {
                float[] result1 = new float[3];
                android.location.Location.distanceBetween(myLatitude, myLongitude, o.latitude, o.longitude, result1);
                Float distance1 = result1[0];

                float[] result2 = new float[3];
                android.location.Location.distanceBetween(myLatitude, myLongitude,o2.latitude,  o2.longitude, result2);
                Float distance2 = result2[0];

                return distance1.compareTo(distance2);
            }
        };
        Collections.sort(locations, comp);
        return locations;
    }

    public  ArrayList<LatLng>  directionPoint(LatLng to_position, LatLng my_position, Thread thread) {
        ArrayList<LatLng> directionPoint = null;
        double latitude,longitude ;
        Route md = new Route();
        Document doc = md.getDocument(my_position, to_position);
        if (doc == null)
            return null;
        directionPoint = new  ArrayList<LatLng>();
        directionPoint = md.getDirection(doc);
       if (directionPoint== null || directionPoint.size() ==0 ) {
           return null;
        }
        else {
            activity.gps.remove_draw_road();
            activity.gps.showDirection(directionPoint);
            return directionPoint;
        }
    }

    public boolean isRoad() {
        return road;
    }

    public void setRoad(boolean road) {
        this.road = road;
    }

    public  boolean isEneableVoice() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean voice = prefs.getBoolean("voice", Boolean.parseBoolean(null));
       return (voice && isEneableShowText(context));
    }
}