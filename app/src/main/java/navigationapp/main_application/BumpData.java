package navigationapp.main_application;

/**
 * Created by Adam on 8.1.2017.
 */

public class BumpData {

    public void get_max_collision(final Double latitude, final Double longtitude, final Integer update) {
        Log.d("TTRREEE", "start get_max_collision  ");


        new Thread() {
            public void run() {
                Looper.prepare();


                while (true) {

                    if (updatesLock.tryLock()) {
                        // Got the lock
                        try {
                            SimpleDateFormat now, ago;
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(new Date());
                            now = new SimpleDateFormat("yyyy-MM-dd");
                            String now_formated = now.format(cal.getTime());
                            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE) - 280);
                            ago = new SimpleDateFormat("yyyy-MM-dd");
                            String ago_formated = ago.format(cal.getTime());

                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(getActivity());
                            SQLiteDatabase database = databaseHelper.getReadableDatabase();

                            checkIntegrityDB(database);
                            database.beginTransaction();
                            // max b_id_collisions z databazy
                            String selectQuery = "SELECT * FROM collisions where b_id_collisions in (SELECT b_id_bumps FROM " + TABLE_NAME_BUMPS
                                    + " where (last_modified BETWEEN '" + ago_formated + " 00:00:00' AND '" + now_formated + " 23:59:59') and  "
                                    + " (ROUND(latitude,1)==ROUND(" + latitude + ",1) and ROUND(longitude,1)==ROUND(" + longtitude + ",1)))"
                                    + " ORDER BY c_id DESC LIMIT 1 ";
                            Cursor cursor = null;

                            try {
                                cursor = database.rawQuery(selectQuery, null);

                                if (cursor.moveToFirst()) {
                                    do {
                                        c_id_database = cursor.getInt(0);
                                        Log.d("TTRREEE", "max v collisions " + c_id_database);
                                    } while (cursor.moveToNext());
                                }
                            } finally {
                                // this gets called even if there is an exception somewhere above
                                if (cursor != null)
                                    cursor.close();
                            }

                            database.setTransactionSuccessful();
                            database.endTransaction();
                            database.close();
                            checkCloseDb(database);


                        } finally {
                            // Make sure to unlock so that we don't cause a deadlock
                            updatesLock.unlock();
                            break;
                        }
                    } else {
                        Log.d("getAllBumps", "getAllBumps thread lock bbbbbbbb ");
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                        }
                    }
                }


                updates = update;
                new Max_Collision_Number().execute();
                Looper.loop();
            }
        }.start();
    }

    class Max_Collision_Number extends AsyncTask<String, Void, JSONArray> {

        protected JSONArray doInBackground(String... args) {
            Log.d("TTRREEE", "start Max_Collision_Number ");
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            Log.d("TTRREEE", "odosielam lang_database   v Max_Collision_Number " + lang_database);
            Log.d("TTRREEE", "odosielam  longt_database v Max_Collision_Number " + longt_database);
            params.add(new BasicNameValuePair("latitude", String.valueOf(lang_database)));
            params.add(new BasicNameValuePair("longitude", String.valueOf(longt_database)));
            params.add(new BasicNameValuePair("b_id", String.valueOf(b_id_database)));
            params.add(new BasicNameValuePair("net", String.valueOf(net)));
            params.add(new BasicNameValuePair("c_id", String.valueOf(c_id_database)));

            JSONObject json = jsonParser.makeHttpRequest("http://sport.fiit.ngnlab.eu/update_collisions.php", "POST", params);
            if (json == null) {
                JSONArray response = new JSONArray();
                try {
                    response.put(0, "error");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                return response;
            }
            try {
                int success = json.getInt("success");

                if (success == 0) {
                    Log.d("TTRREEE", "5. Max_Collision_Number bumps");
                    // mám povolene stahovať a mám vytlky
                    bumps = json.getJSONArray("bumps");
                    return bumps;
                } else if (success == 1) {
                    // nemám povolene stahovať ale mám vytlky
                    JSONArray response = new JSONArray();
                    Log.d("TTRREEE", "5. Max_Collision_Number update");
                    response.put(0, "update");
                    return response;
                } else {
                    Log.d("TTRREEE", "5. Max_Collision_Number null");
                    // nemám nove vytlky
                    return null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                JSONArray response = new JSONArray();
                try {
                    Log.d("TTRREEE", "5. Max_Collision_Number error");
                    response.put(0, "error");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                return response;
            }

        }

        protected void onPostExecute(JSONArray array) {

            if (array == null) {
                Log.d("TTRREEE", "5. Max_Collision_Number  onPostExecute null");
                // collision nemaju update ale bumps ano
                if (updates == 1 || regularUpdatesLock) {
                    Log.d("TTRREEE", "5. Max_Collision_Number  onPostExecute GetUpdateAction");
                    GetUpdateAction();
                } else {
                    Log.d("TTRREEE", "5. Max_Collision_Number  onPostExecute citam getall");
                    // načítam vytlky na mapu
                    if (gps != null && gps.getCurrentLatLng() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                LatLng convert_location = gps.getCurrentLatLng();
                                getAllBumps(convert_location.latitude, convert_location.longitude);
                            }
                        });


                    }
                }
                return;
            }

            try {
                if (array.get(0).equals("error")) {
                    Log.d("TTRREEE", "5. Max_Collision_Number  onPostExecute error");
                    // nastala chyba, nacitam mapu
                    if (gps != null && gps.getCurrentLatLng() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                LatLng convert_location = gps.getCurrentLatLng();
                                getAllBumps(convert_location.latitude, convert_location.longitude);
                            }
                        });
                    }
                    return;

                } else if (array.get(0).equals("update")) {
                    Log.d("TTRREEE", "5. Max_Collision_Number  onPostExecute update a");
                    // mam vytlky na stiahnutie, ale potrebujem opravnenie od používateľa
                    GetUpdateAction();
                } else {

                    Thread t = new Thread() {
                        public void run() {
                            Log.d("TTRREEE", "6. Max_Collision_Number - thread  ");
                            Looper.prepare();
                            Boolean error = false;
                            while (true) {
                                if (updatesLock.tryLock()) {
                                    // Got the lock
                                    try {
                                        DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(getActivity());
                                        SQLiteDatabase database = databaseHelper.getWritableDatabase();

                                        checkIntegrityDB(database);
                                        database.beginTransaction();
                                        for (int i = 0; i < bumps.length(); i++) {
                                            JSONObject c = null;
                                            try {
                                                c = bumps.getJSONObject(i);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                                error = true;
                                            }

                                            int c_id, b_id;
                                            double intensity = 0;
                                            String created_at;
                                            Log.d("TTRREEE", " bob_id_database  " + b_id_database);
                                            Log.d("TTRREEE", " bloaded_index " + loaded_index);
                                            if (c != null) {
                                                try {
                                                    c_id = c.getInt("c_id");
                                                    b_id = c.getInt("b_id");
                                                    intensity = c.getDouble("intensity");
                                                    created_at = c.getString("created_at");
                                                    Log.d("TTRREEE", "c_id " + c_id);
                                                    Log.d("TTRREEE", "b_id " + b_id);
                                                    Log.d("TTRREEE", "intensity " + intensity);

                                                    // ak nove collision updatuju stare  vytlky
                                                    if (b_id <= loaded_index) {
                                                        Log.d("TTRREEE", " updatujem b_id ");
                                                        int rating = 0;
                                                        if (isBetween((float) intensity, 0, 6))
                                                            rating = 1;
                                                        if (isBetween((float) intensity, 6, 10))
                                                            rating = 2;
                                                        if (isBetween((float) intensity, 10, 10000))
                                                            rating = 3;
                                                        database.execSQL("UPDATE " + Provider.bumps_detect.TABLE_NAME_BUMPS + " SET rating=rating+ " + rating + ", count=count +1 WHERE b_id_bumps=" + b_id);
                                                    }

                                /* ak nastala chyba v transakcii,  musím upraviť udaje
                                  beriem od poslendej uspešnej transakcie collision po načitane max id z bumps
                                 */
                                                    if (b_id <= b_id_database && loaded_index < b_id) {
                                                        int rating = 0;
                                                        if (isBetween((float) intensity, 0, 6))
                                                            rating = 1;
                                                        if (isBetween((float) intensity, 6, 10))
                                                            rating = 2;
                                                        if (isBetween((float) intensity, 10, 10000))
                                                            rating = 3;

                                                        Cursor cursor = null;
                                                        String sql = "SELECT * FROM collisions WHERE b_id_collisions=" + b_id;

                                                        try {
                                                            cursor = database.rawQuery(sql, null);

                                                            if (cursor.getCount() > 0) {
                                                                Log.d("TTRREEE", " bolo ich viac v  b_id ");
                                                                //  ak ich bolo viac pripičítam
                                                                sql = "UPDATE " + Provider.bumps_detect.TABLE_NAME_BUMPS + " SET rating=rating+ " + rating + ", count=count +1 WHERE b_id_bumps=" + b_id;
                                                            } else {
                                                                Log.d("TTRREEE", " bolo prvy b   b_id ");
                                                                // ak bol prvý, nastavujem na 1 count a rating prvého prijateho
                                                                sql = "UPDATE " + Provider.bumps_detect.TABLE_NAME_BUMPS + " SET rating=" + rating + ", count=1 WHERE b_id_bumps=" + b_id;
                                                            }
                                                            database.execSQL(sql);
                                                        } finally {
                                                            // this gets called even if there is an exception somewhere above
                                                            if (cursor != null)
                                                                cursor.close();
                                                        }
                                                    }

                                                    // insert novych udajov
                                                    ContentValues contentValues = new ContentValues();
                                                    contentValues.put(Provider.bumps_collision.C_ID, c_id);
                                                    contentValues.put(Provider.bumps_collision.B_ID_COLLISIONS, b_id);
                                                    contentValues.put(Provider.bumps_collision.CRETED_AT, created_at);
                                                    contentValues.put(Provider.bumps_collision.INTENSITY, intensity);
                                                    database.insert(Provider.bumps_collision.TABLE_NAME_COLLISIONS, null, contentValues);

                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                    // ak nastane chyba, tak si ju poznačim
                                                    error = true;
                                                }
                                            }
                                        }
                                        if (!error) {
                                            // ak nenastala chyba, transakci je uspešna
                                            database.setTransactionSuccessful();
                                            database.endTransaction();
                                            database.close();
                                            checkCloseDb(database);


                                            ////////// updatesLock.getAndSet(false);
                                            // uložím najvyššie b_id  z bumps po uspešnej transakcii
                                            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                                            SharedPreferences.Editor editor = sharedPref.edit();
                                            editor.putInt("save", max_number);
                                            loaded_index = max_number;
                                            editor.commit();
                                        } else {
                                            // rollbacknem databazu
                                            database.endTransaction();
                                            database.close();
                                            checkCloseDb(database);

                                            //////////////   updatesLock.getAndSet(false);
                                        }
                                    } finally {
                                        // Make sure to unlock so that we don't cause a deadlock
                                        updatesLock.unlock();
                                        break;
                                    }
                                } else {

                                    Log.d("getAllBumps", "getAllBumps thread lock ccccccccccc");
                                    try {
                                        Random ran = new Random();
                                        int x = ran.nextInt(20) + 1;
                                        Thread.sleep(x);
                                    } catch (InterruptedException e) {
                                    }
                                }
                            }

                            if (!error) {
                                ////////// updatesLock.getAndSet(false);
                                // uložím najvyššie b_id  z bumps po uspešnej transakcii
                                SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putInt("save", max_number);
                                loaded_index = max_number;
                                editor.commit();
                            }
                            // načítam vytlky
                            if (gps != null && gps.getCurrentLatLng() != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        LatLng convert_location = gps.getCurrentLatLng();
                                        getAllBumps(convert_location.latitude, convert_location.longitude);
                                    }
                                });
                            }
                            Looper.loop();
                        }
                    };
                    t.start();


                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    public void get_max_bumps(final Double langtitude, final Double longtitude, final Integer nets) {

        Log.d("TTRREEE", "spustam  get_max_bumps");


        new Thread() {
            public void run() {
                Looper.prepare();

                while (true) {
                    if (updatesLock.tryLock()) {
                        // Got the lock
                        try {
                            SimpleDateFormat now, ago;
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(new Date());
                            now = new SimpleDateFormat("yyyy-MM-dd");
                            String now_formated = now.format(cal.getTime());
                            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DATE) - 280);
                            ago = new SimpleDateFormat("yyyy-MM-dd");
                            String ago_formated = ago.format(cal.getTime());
                            DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(getActivity());
                            SQLiteDatabase database = databaseHelper.getReadableDatabase();

                            checkIntegrityDB(database);
                            database.beginTransaction();
                            // vytiahnem najvyššie b_id z bumps
                            String selectQuery = "SELECT b_id_bumps FROM " + TABLE_NAME_BUMPS
                                    + " where (last_modified BETWEEN '" + ago_formated + " 00:00:00' AND '" + now_formated + " 23:59:59') and  "
                                    + " (ROUND(latitude,1)==ROUND(" + langtitude + ",1) and ROUND(longitude,1)==ROUND(" + longtitude + ",1))"
                                    + " ORDER BY b_id_bumps DESC LIMIT 1 ";
                            Cursor cursor = null;
                            try {
                                cursor = database.rawQuery(selectQuery, null);

                                if (cursor.moveToFirst()) {
                                    do {
                                        b_id_database = cursor.getInt(0);
                                        Log.d("TTRREEE", "najvssie  b_id v bumps " + b_id_database);
                                    } while (cursor.moveToNext());

                                }
                            } finally {
                                // this gets called even if there is an exception somewhere above
                                if (cursor != null)
                                    cursor.close();
                            }


                            database.setTransactionSuccessful();
                            database.endTransaction();
                            database.close();
                            checkCloseDb(database);


                        } finally {
                            // Make sure to unlock so that we don't cause a deadlock
                            updatesLock.unlock();
                            break;
                        }
                    } else {

                        Log.d("getAllBumps", "getAllBumps thread lock ddddddddd");
                        try {
                            Random ran = new Random();
                            int x = ran.nextInt(20) + 1;
                            Thread.sleep(x);
                        } catch (InterruptedException e) {
                        }
                    }
                }


                net = nets;
                lang_database = langtitude;
                longt_database = longtitude;

                new Max_Bump_Number().execute();
                Looper.loop();
            }
        }.start();
    }

    class Max_Bump_Number extends AsyncTask<String, Void, JSONArray> {

        protected JSONArray doInBackground(String... args) {
            Log.d("TTRREEE", "2. spustam Max_Bump_Number");
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("latitude", String.valueOf(lang_database)));
            params.add(new BasicNameValuePair("longitude", String.valueOf(longt_database)));
            params.add(new BasicNameValuePair("b_id", String.valueOf(b_id_database)));
            params.add(new BasicNameValuePair("net", String.valueOf(net)));

            JSONObject json = jsonParser.makeHttpRequest("http://sport.fiit.ngnlab.eu/update_bumps.php", "POST", params);
            if (json == null) {
                JSONArray response = new JSONArray();
                try {
                    response.put(0, "error");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                return response;
            }
            try {
                int success = json.getInt("success");
                JSONArray response = new JSONArray();
                if (success == 0) {
                    // mam nove data na stiahnutie
                    bumps = json.getJSONArray("bumps");
                    Log.d("TTRREEE", "2. spustam Max_Bump_Number - success ");
                    return bumps;
                } else if (success == 1) {
                    // potrebujem potvrdit nove data na stiahnutie
                    response.put(0, "update");
                    Log.d("TTRREEE", "2. spustam Max_Bump_Number - update ");
                    return response;
                } else {
                    Log.d("TTRREEE", "2. spustam Max_Bump_Number - null ");
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
                return response;
            }
        }

        protected void onPostExecute(JSONArray array) {
            if (array == null) {
                // žiadne nové data v bumps, zisti collisons
                Log.d("TTRREEE", "2.onPostExecute spustam Max_Bump_Number - null ");
                get_max_collision(lang_database, longt_database, 0);
                return;
            }

            try {
                if (array.get(0).equals("error")) {
                    Log.d("TTRREEE", "2.onPostExecute spustam Max_Bump_Number - error ");
                    return;

                } else if (array.get(0).equals("update")) {
                    // mam nove data, zisti aj collision a potom upozorni
                    Log.d("TTRREEE", "2.onPostExecute spustam Max_Bump_Number - update  ");
                    get_max_collision(lang_database, longt_database, 1);
                } else {
                    Log.d("TTRREEE", "2.onPostExecute spustam Max_Bump_Number - succes  ");
                    Thread t = new Thread() {
                        public void run() {
                            Looper.prepare();
                            Log.d("TTRREEE", "3. spustam Max_Bump_Number - thread ");
                            // insertujem nove data
                            Boolean error = false;

                            while (true) {
                                if (updatesLock.tryLock()) {
                                    // Got the lock
                                    try {
                                        DatabaseOpenHelper databaseHelper = new DatabaseOpenHelper(getActivity());
                                        SQLiteDatabase database = databaseHelper.getWritableDatabase();

                                        checkIntegrityDB(database);
                                        database.beginTransaction();
                                        for (int i = 0; i < bumps.length(); i++) {
                                            JSONObject c = null;
                                            try {
                                                c = bumps.getJSONObject(i);
                                            } catch (JSONException e) {
                                                error = true;
                                                e.printStackTrace();
                                            }
                                            double latitude, longitude;
                                            int count, b_id, rating, manual = 0;
                                            String last_modified;

                                            if (c != null) {
                                                try {
                                                    latitude = c.getDouble("latitude");
                                                    longitude = c.getDouble("longitude");
                                                    Log.d("TTRREEE", "latitude " + latitude);
                                                    Log.d("TTRREEE", "longitude" + longitude);

                                                    count = c.getInt("count");
                                                    b_id = c.getInt("b_id");
                                                    Log.d("TTRREEE", "b_id" + b_id);
                                                    max_number = b_id;
                                                    rating = c.getInt("rating");
                                                    last_modified = c.getString("last_modified");
                                                    manual = c.getInt("manual");
                                                    ContentValues contentValues = new ContentValues();
                                                    contentValues.put(Provider.bumps_detect.B_ID_BUMPS, b_id);
                                                    contentValues.put(Provider.bumps_detect.COUNT, count);
                                                    contentValues.put(Provider.bumps_detect.LAST_MODIFIED, last_modified);
                                                    contentValues.put(Provider.bumps_detect.LATITUDE, latitude);
                                                    contentValues.put(Provider.bumps_detect.LONGTITUDE, longitude);
                                                    contentValues.put(Provider.bumps_detect.MANUAL, manual);
                                                    contentValues.put(Provider.bumps_detect.RATING, rating);
                                                    database.insert(Provider.bumps_detect.TABLE_NAME_BUMPS, null, contentValues);
                                                } catch (JSONException e) {
                                                    error = true;
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                        if (!error) {
                                            // insert prebehol v poriadku, ukonči transakciu
                                            database.setTransactionSuccessful();
                                            database.endTransaction();
                                            database.close();
                                            checkCloseDb(database);

                                            //////// updatesLock.getAndSet(false);

                                        } else {
                                            // nastala chyba, načitaj uložene vytlky
                                            database.endTransaction();
                                            database.close();
                                            checkCloseDb(database);


                                        }
                                    } finally {
                                        // Make sure to unlock so that we don't cause a deadlock
                                        updatesLock.unlock();
                                        break;
                                    }
                                } else {

                                    Log.d("getAllBumps", "getAllBumps thread lock eeeeeeeeeeeeeeeeeeeee");
                                    try {
                                        Random ran = new Random();
                                        int x = ran.nextInt(20) + 1;
                                        Thread.sleep(x);
                                    } catch (InterruptedException e) {
                                    }
                                }
                            }

                            if (!error) {

                                //////// updatesLock.getAndSet(false);
                                get_max_collision(lang_database, longt_database, 0);
                                Looper.loop();

                            } else {
                                // nastala chyba, načitaj uložene vytlky

                                if (gps != null && gps.getCurrentLatLng() != null) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            LatLng convert_location = gps.getCurrentLatLng();
                                            getAllBumps(convert_location.latitude, convert_location.longitude);
                                        }
                                    });
                                }
                                Looper.loop();
                                //////////// updatesLock.getAndSet(false);


                            }

                        }
                    };
                    t.start();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
