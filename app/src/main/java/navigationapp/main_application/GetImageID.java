package navigationapp.main_application;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.TextSliderView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import navigationapp.R;

/**
 * Created by Adam on 9.3.2017.
 */

public class GetImageID {
    String latitude = null;
    String longitude = null;
    String type = null;
    private JSONParser jsonParser = new JSONParser();
    private JSONArray bumps = null;
    Context context = null ;
    SliderLayout mDemoSlider = null;

    public  GetImageID (Context context, String latitude , String longitude , String type, SliderLayout mDemoSlider) {
        Log.d(TAG, "GetAllImage constructor");
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.context = context;
        this.mDemoSlider = mDemoSlider;
        new GetAllImage().execute();
    }

    public  final String TAG = "GetImageID";

    class GetAllImage extends AsyncTask<String, Void, JSONArray> {

        protected JSONArray doInBackground(String... args) {
            Log.d(TAG, "GetAllImage start");
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("latitude", String.valueOf(latitude)));
            params.add(new BasicNameValuePair("longitude", String.valueOf(longitude)));
            params.add(new BasicNameValuePair("type", String.valueOf(type)));

            Log.d(TAG, "GetAllImage odosielam požiadavku na server");
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
            if (array == null) { // žiadne nové data v bumps, zisti collisons
                Log.d(TAG, "GetAllImage onPostExecute - no data");
                no_data();
                return;
            }

            try {
                if (array.get(0).equals("error")) {
                    Log.d(TAG, "GetAllImage onPostExecute - error");
                    no_data();
                    return;
                } else if (bumps!=null) { // mam nove data, zistit aj collision a potom upozorni uživatela
                    Log.d(TAG, "GetAllImage onPostExecute - new_data");

                    for (int i = 0; i < bumps.length(); i++) {
                        JSONObject data = null;
                        try {
                            data = bumps.getJSONObject(i);
                        } catch (JSONException e) {

                            e.printStackTrace();
                            break;
                        }
                        String  aaa= data.getString("id");
                        Log.d(TAG, "GetAllImage onPostExecute  - id hladane" + aaa);
                        new GetImage(context, aaa, mDemoSlider);
                    }




                       /* try {
                            data = bumps.getJSONObject(i).getJSONObject("bumps");
                             new GetImage(context, data.getString("id"), mDemoSlider);

                        } catch (JSONException e) {
                            Log.d(TAG, "GetAllImage onPostExecute - JSONException error");
                            e.printStackTrace();


                        }*/

                }else {
                    Log.d(TAG, "GetAllImage onPostExecute - no data");
                    no_data();

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

     public void no_data() {
         TextSliderView textSliderView = new TextSliderView(context);
         textSliderView
                 .description("14/01/2017")
                 .image(R.drawable.no_internet)
                 .setScaleType(BaseSliderView.ScaleType.Fit);
         //.setOnSliderClickListener(this);
         textSliderView.bundle(new Bundle());
         textSliderView.getBundle()
                 .putString("extra", "14/01/2017");
         mDemoSlider.addSlider(textSliderView);
     }




}
