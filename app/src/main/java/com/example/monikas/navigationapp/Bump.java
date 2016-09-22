package com.example.monikas.navigationapp;

/**
 * Created by monikas on 23. 3. 2015.
 */

import android.location.Location;
import android.os.AsyncTask;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

public class Bump {

    JSONParser jsonParser = new JSONParser();
    private static String url_create_product = "http://sport.fiit.ngnlab.eu/create.php";
    private float intensity;
    private Location location;
    private int rating;

    public Bump(Location location, float delta) {
        this.intensity = delta;
        this.location = location;
        rating = 1;
        if (isBetween(intensity,0,6)) rating = 1;
        if (isBetween(intensity,6,10)) rating = 2;
        if (isBetween(intensity,10,10000)) rating = 3;
        new CreateNewBump().execute();
    }

    public static boolean isBetween(float x, float from, float to) {
        return from <= x && x <= to;
    }


    class CreateNewBump extends AsyncTask<String, String, String> {

        protected String doInBackground(String... args) {
            String latitude = String.valueOf(location.getLatitude());
            String longitude = String.valueOf(location.getLongitude());
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            //do databazy sa posiela vytlk s informaciami o jeho polohe, intenzite a ratingu, ktory sa vypocital na zaklade intenzity
            params.add(new BasicNameValuePair("latitude", latitude));
            params.add(new BasicNameValuePair("longitude", longitude));
            params.add(new BasicNameValuePair("intensity", Float.toString(intensity)));
            params.add(new BasicNameValuePair("rating", Float.toString(rating)));
            jsonParser.makeHttpRequest(url_create_product, "POST", params);
            return null;
        }
    }

}
