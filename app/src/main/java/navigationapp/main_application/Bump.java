package navigationapp.main_application;

import android.location.Location;
import android.os.AsyncTask;

import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static navigationapp.main_application.MainActivity.getDate;
import static navigationapp.server.Connection.create_bump;

public class Bump {

    JSONParser jsonParser = new JSONParser();
    private float intensity;
    private Location location;
    private int rating;
    private int manual;
    private int type;
    private String text;
    private final String TAG = "Bump";
    private String androidId;

    public Bump(Location location, float delta, Integer manual, Integer type, String text, String androidId) {
        this.intensity = delta;
        this.location = location;
        this.androidId = androidId;
        rating = 1;
        this.type = type;
        this.text = text;
        if (isBetween(intensity, 0, 6)) rating = 1;
        if (isBetween(intensity, 6, 10)) rating = 2;
        if (isBetween(intensity, 10, 10000)) rating = 3;
        this.manual = manual;

    }

    public static boolean isBetween(float x, float from, float to) {
        return from <= x && x <= to;
    }

    public void getResponse(final CallBackReturn returnMethod) {
        CreateNewBump obj = new CreateNewBump();
        String response = null;

        try {
            response = obj.execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (response != null)
            Log.d(TAG, response);
        else
            Log.d(TAG, "return null");
        returnMethod.callback(response);
    }

    class CreateNewBump extends AsyncTask<String, String, String> {
        protected String doInBackground(String... args) {

            String latitude = String.valueOf(location.getLatitude());
            String longitude = String.valueOf(location.getLongitude());
            List<NameValuePair> params = new ArrayList<NameValuePair>();
           //odosielaná datekcia
            params.add(new BasicNameValuePair("latitude", latitude));
            params.add(new BasicNameValuePair("longitude", longitude));
            params.add(new BasicNameValuePair("intensity", Float.toString(intensity)));
            params.add(new BasicNameValuePair("rating", Float.toString(rating)));
            params.add(new BasicNameValuePair("manual", Integer.toString(manual)));
            params.add(new BasicNameValuePair("type", Integer.toString(type)));
            params.add(new BasicNameValuePair("device_id", androidId));
            params.add(new BasicNameValuePair("date",  getDate(location.getTime(), "yyyy-MM-dd HH:mm:ss")));
            params.add(new BasicNameValuePair("actual_date", getDate(new Date().getTime(), "yyyy-MM-dd HH:mm:ss")));
            params.add(new BasicNameValuePair("info", text));

            JSONObject json = jsonParser.makeHttpRequest(create_bump, "POST", params);

            if (json==null) {
                return "error";
            }

            int success = 0;
             try {
                 success = json.getInt("success");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return "error";
                }
            if (success == 1)
                return "success";
            else
                return "error";
        }
    }
}



