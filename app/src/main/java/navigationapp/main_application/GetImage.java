package navigationapp.main_application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;



public class GetImage {
    private JSONParser jsonParser = new JSONParser();
    Context context =null;
    JSONArray address =null;
    String image = null;
    Bitmap bmp  = null;
        public GetImage(Context context) {
        this.context = context;
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("latitude", "latitude"));
            params.add(new BasicNameValuePair("longitude", "latitude"));


        JSONObject json = jsonParser.makeHttpRequest("http://sport.fiit.ngnlab.eu/sync_bump.php", "POST", params);

        Log.d("Image: ", json.toString());

        try {
            int success = json.getInt("succes");

            if (success == 1) {
                address = json.getJSONArray("photo");
                for (int i = 0; i < address.length(); i++) {
                    JSONObject c = address.getJSONObject(i);
                    image = c.getString("photoNUM");
                    byte[] decodedString = Base64.decode(image, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                  /*  byte[] dwimage = Base64.decode(image.getBytes());
                    System.out.println(dwimage);
                    bmp = BitmapFactory.decodeByteArray(dwimage, 0, dwimage.length);*/
                }
            } else {

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
