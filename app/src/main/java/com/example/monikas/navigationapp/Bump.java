package com.example.monikas.navigationapp;

/**
 * Created by monikas on 23. 3. 2015.
 */

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Bump {

    JSONParser jsonParser = new JSONParser();
    private static String url_create_product = "http://sport.fiit.ngnlab.eu/create.php";
    private float intensity;
    private Location location;
    private int rating;
    private int manual;

    public   Bump(Location location, float delta ,Integer manual ) {
        this.intensity = delta;
        this.location = location;
        rating = 1;
        if (isBetween(intensity,0,6)) rating = 1;
        if (isBetween(intensity,6,10)) rating = 2;
        if (isBetween(intensity,10,10000)) rating = 3;
        this.manual=manual;

    }

    public static boolean isBetween(float x, float from, float to) {
        return from <= x && x <= to;
    }


    public void getOneIteam(final CallBackReturn returnMethod) {
        Log.d("asdfgsa", "spustam getOneIteam ");
      CreateNewBump apiObj = new CreateNewBump();
        String aaa  = null;

        try {
             aaa = apiObj.execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        Log.d("asdfgsa", "vraciam  " +aaa);
            returnMethod.callback(aaa);
    }

    class CreateNewBump extends AsyncTask<String, String, String> {
    //    public IApiAccessResponse delegate=null;

        protected String doInBackground(String... args) {
            String latitude = String.valueOf(location.getLatitude());
            String longitude = String.valueOf(location.getLongitude());
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            //do databazy sa posiela vytlk s informaciami o jeho polohe, intenzite a ratingu, ktory sa vypocital na zaklade intenzity
            params.add(new BasicNameValuePair("latitude", latitude));
            params.add(new BasicNameValuePair("longitude", longitude));
            params.add(new BasicNameValuePair("intensity", Float.toString(intensity)));
            params.add(new BasicNameValuePair("rating", Float.toString(rating)));
            params.add(new BasicNameValuePair("manual", Integer.toString(manual)));

            JSONObject json = jsonParser.makeHttpRequest(url_create_product, "POST", params);
            Log.d("BUMP", "makeHttpRequest");

            int success = 1;
            if (!json.has("send"))
                return "success";
            else
                return "error";
        }

    }

}
