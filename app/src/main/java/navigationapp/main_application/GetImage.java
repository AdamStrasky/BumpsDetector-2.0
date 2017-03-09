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
import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.google.common.base.Charsets;


public class GetImage {

    private JSONParser jsonParser = new JSONParser();
    Context context =null;
    JSONArray address =null;
    String image = null;
    Bitmap bmp  = null;

    public GetImage(Context context, String ID) {
        this.context = context;
        Log.d("imagefff ", "f.GetImage start ");
        new imaga().execute();
    }


    class imaga extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... args) {
            Log.d("imagefff ", "f.GetImage AsyncTask ");
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("id", "5"));
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

                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File(sdCard.getAbsolutePath() + "/Detector");
                if(!dir.exists()){
                    dir.mkdirs();
                }

                File f = new File(String.valueOf(dir), decodedByte.toString() + ".png");
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


            } else
                Log.d("imagefff ", "decodedByte  null ");






        }
    }

}
