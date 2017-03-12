package navigationapp.main_application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.TextSliderView;

public class GetImage {

    private JSONParser jsonParser = new JSONParser();
    Context context =null;
    String id = null ;
    SliderLayout mDemoSlider = null;
    public  final String TAG = "GetImage";

    public GetImage(Context context, String id, SliderLayout mDemoSlider) {
        this.context = context;
        this.id = id;
        this.mDemoSlider = mDemoSlider;
        Log.d(TAG, "GetImage start ");
        try {
            new ImageByID().execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    class ImageByID extends AsyncTask<String, Void, JSONObject> {

        protected JSONObject doInBackground(String... args) {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("id", id));
            JSONObject json = jsonParser.makeHttpRequest("http://sport.fiit.ngnlab.eu/get_image.php", "POST", params);
            return json;
        }
        protected void onPostExecute(JSONObject array) {
            String date = null;
            try {
                date = array.getString("date");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String image = null;
            try {
                image = array.getString("file");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "image" + image);
            byte[] data = Base64.decode(image, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(data, 0, data.length);

            if (decodedByte != null) {
                Log.d(TAG, "decodedByte not null ");
                File f = new File(context.getCacheDir(), decodedByte.toString() + ".png");
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                decodedByte.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                byte[] bitmapdata = bos.toByteArray();

                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(f);

                    fos.write(bitmapdata);
                    fos.flush();
                    fos.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                HashMap<String, Integer> file_maps = new HashMap<String, Integer>();
                file_maps.put(date, decodedByte.describeContents());

                TextSliderView textSliderView = new TextSliderView(context);
                textSliderView
                        .description(date)
                        .image(f)
                        .setScaleType(BaseSliderView.ScaleType.Fit);
                 //       .setOnSliderClickListener(this);

                textSliderView.bundle(new Bundle());
                textSliderView.getBundle()
                        .putString("extra", date);

                mDemoSlider.addSlider(textSliderView);
            } else
                Log.d(TAG, "decodedByte  null ");
        }
    }
}
