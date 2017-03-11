package navigationapp.main_application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutionException;

import cz.msebera.android.httpclient.Header;

import static navigationapp.main_application.FragmentActivity.updatesLock;

public class UploadPhoto {
    public  final String TAG = "UploadPhoto";
    private RequestParams params = new RequestParams();
    private Bitmap bitmap = null;
    private String encodedString = null;
    private Context context =null;
    private String date = null, latitude = null, longitude = null, type = null ,path = null;

    public UploadPhoto(Context context, String latitude, String longitude, String type, String date, String path) {
        this.context = context ;
        this.latitude = latitude ;
        this.longitude = longitude ;
        this.type = type ;
        this.date = date ;
        this.path = path ;
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
            Log.d(TAG,"bla bla vla " +  response);
        else
            Log.d(TAG, "return null");
        returnMethod.callback(response);
    }

    class CreateNewBump extends AsyncTask<Void, Void, String> {

           @Override
            protected String doInBackground(Void... args) {
                BitmapFactory.Options options = null;
                options = new BitmapFactory.Options();
                options.inSampleSize = 3;
                bitmap = BitmapFactory.decodeFile(path,
                        options);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, stream);
                byte[] byte_arr = stream.toByteArray();
                encodedString = Base64.encodeToString(byte_arr, 0);
                params.put("image", encodedString);
                params.put("latitude", latitude);
                params.put("longitude", longitude);
                params.put("type", type);
                params.put("date", date);
                params.put("filename", bitmap.getGenerationId()+date+".jpg");
                return makeHTTPCall();
            }

            @Override
            protected void onPostExecute(String result) {
            super.onPostExecute(result);

        }

    }
    String aaa=  "success1";
// AsyncHttpResponseHandler
    public String makeHTTPCall() {

        SyncHttpClient client = new SyncHttpClient();
        client.post("http://sport.fiit.ngnlab.eu/update_image.php",
                params, new AsyncHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        aaa=  "success";
                    }
                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        if (statusCode == 404) {
                            Toast.makeText(context,
                                    "Requested resource not found",
                                    Toast.LENGTH_LONG).show();
                        }
                        else if (statusCode == 500) {
                            Toast.makeText(context,
                                    "Something went wrong at server end",
                                    Toast.LENGTH_LONG).show();
                        }
                        // When Http response code other than 404, 500
                        else {
                            Toast.makeText(context,"Error: "+ statusCode, Toast.LENGTH_LONG).show();
                        }
                    }
                });
        return aaa;
    }
}
