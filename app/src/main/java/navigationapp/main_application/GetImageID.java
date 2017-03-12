package navigationapp.main_application;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.daimajia.slider.library.SliderLayout;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import navigationapp.R;

public class GetImageID   {
    ProgressDialog pDialog = null;
    String latitude = null;
    String longitude = null;
    String type = null;
    private JSONParser jsonParser = new JSONParser();
    private JSONArray bumps = null;
    Context context = null ;
    SliderLayout mDemoSlider = null;
    Activity activity = null;
    public  final String TAG = "GetImageID";

    public  GetImageID(final Activity activity, final Context context, String latitude, String longitude, String type, SliderLayout mDemoSlider) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.context = context;
        this.activity = activity;
        this.mDemoSlider = mDemoSlider;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                pDialog = new ProgressDialog(activity);
                pDialog.setMessage(activity.getResources().getString(R.string.load_photo));
                pDialog.setCancelable(false);
                pDialog.setCanceledOnTouchOutside(false);
                pDialog.show();
            }
        });
        new GetAllImage().execute();
    }

    class GetAllImage extends AsyncTask<String, Void, JSONArray> {

        protected JSONArray doInBackground(String... args) {

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("latitude", String.valueOf(latitude)));
            params.add(new BasicNameValuePair("longitude", String.valueOf(longitude)));
            params.add(new BasicNameValuePair("type", String.valueOf(type)));
            Log.d(TAG, " odosielam požiadavku na server");
            JSONObject json = jsonParser.makeHttpRequest("http://sport.fiit.ngnlab.eu/get_image_id.php", "POST", params);
            if (json == null) {
                JSONArray response = new JSONArray();
                try {
                    Log.d(TAG, "GetAllImage - error");
                    response.put(0, "error");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                return response;
            }
            try {
                int success = json.getInt("new_data");
                Log.d(TAG, "GetAllImage - new_data" + success);

                if (success == 0) {   // mam nove data na stiahnutie
                    JSONArray response = new JSONArray();
                    response.put(0, json);
                    bumps = json.getJSONArray("bumps");
                    return response;
                } else {
                    Log.d(TAG, "GetAllImage - no data");
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
                Log.d(TAG, "GetAllImage - JSONException");
                return response;
            }
        }

        protected void onPostExecute(JSONArray array) {
            if (array == null) {
                Log.d(TAG, "GetAllImage onPostExecute - no data");
                message(context.getResources().getString(R.string.image_no_data));
                return;
            }

            try {
                if (array.get(0).equals("error")) {
                    Log.d(TAG, "GetAllImage onPostExecute - error");
                    message(context.getResources().getString(R.string.image_interent));
                    return;
                } else if (bumps!=null) {
                    Log.d(TAG, "GetAllImage onPostExecute - new_data");
                    new Thread() {
                        public void run() {
                            Looper.prepare();
                            for (int i = 0; i < bumps.length(); i++) {
                                JSONObject data = null;
                                try {
                                    data = bumps.getJSONObject(i);
                                } catch (JSONException e) {

                                    e.printStackTrace();
                                    break;
                                }
                                try {
                                    new GetImage(context, data.getString("id"), mDemoSlider);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    pDialog.dismiss();
                                }
                            });
                            Looper.loop();
                        }
                    }.start();
                }else {
                    Log.d(TAG, "GetAllImage onPostExecute - no data");
                    message(context.getResources().getString(R.string.image_no_data));
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        pDialog.dismiss();
                    }
                });
            }
        }
    }

     public void message( String text) {
         Toast.makeText(context,text, Toast.LENGTH_LONG).show();
         activity.runOnUiThread(new Runnable() {
             public void run() {
                 pDialog.dismiss();
             }
         });
     }
}
