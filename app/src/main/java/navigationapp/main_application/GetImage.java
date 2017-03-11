package navigationapp.main_application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.TextSliderView;
import com.google.common.base.Charsets;


public class GetImage {

    private JSONParser jsonParser = new JSONParser();
    Context context =null;
    String id = null ;

    SliderLayout mDemoSlider = null;

    public GetImage(Context context, String id, SliderLayout mDemoSlider) {
        this.context = context;
        this.id = id;
        this.mDemoSlider = mDemoSlider;
        Log.d("imagefff ", "f.GetImage start ");
        new ImageByID().execute();

    }


    class ImageByID extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... args) {
            Log.d("imagefff ", "f.GetImage AsyncTask ");
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            Log.d("imagefff ", "f.GetImage AsyncTask  id" + id);
            params.add(new BasicNameValuePair("id", id));
            String json = jsonParser.makeHttpRequest1("http://sport.fiit.ngnlab.eu/get_image.php", "POST", params);
            Log.d("imagefff ", "f.GetImage json return ");
            return json;
        }

        protected void onPostExecute(String array) {
            Log.d("imagefff ", array);
            byte[] data = Base64.decode(array, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(data, 0, data.length);

            if (decodedByte != null) {
                Log.d("imagefff ", "decodedByte not null ");

                File f = new File(context.getCacheDir(), decodedByte.toString() + ".png");
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d("imagefff ", "f.getAbsolutePath() "+ f.getAbsolutePath());

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                decodedByte.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                byte[] bitmapdata = bos.toByteArray();

//write the bytes in file
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(f);

                    fos.write(bitmapdata);
                    fos.flush();
                    fos.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                String date = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
                HashMap<String, Integer> file_maps = new HashMap<String, Integer>();
                file_maps.put(date, decodedByte.describeContents());

                TextSliderView textSliderView = new TextSliderView(context);
                // initialize a SliderLayout
                textSliderView
                        .description(date)
                        .image(f)
                        .setScaleType(BaseSliderView.ScaleType.Fit);
                 //       .setOnSliderClickListener(this);

                //add your extra information
                textSliderView.bundle(new Bundle());
                textSliderView.getBundle()
                        .putString("extra", date);



              //  int  position = mDemoSlider.getCurrentPosition();
                mDemoSlider.addSlider(textSliderView);
                //f.delete();


            } else
                Log.d("imagefff ", "decodedByte  null ");






        }
    }

}
