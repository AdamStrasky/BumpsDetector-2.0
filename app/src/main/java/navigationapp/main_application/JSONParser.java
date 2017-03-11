package navigationapp.main_application;

//inspiracia z https://excel-to-vb.googlecode.com/svn/trunk/MineralsAtlas/src/com/hk/mineralsatlas/dao/JSONParser.java

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONParser {

     InputStream is = null;
     JSONObject jObj = null;
     String json = "";

    public JSONObject makeHttpRequest(String url, String method, List<NameValuePair> params) {

        try {
            if (method == "POST") {
                // request method is POST
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(url);
                httpPost.setEntity(new UrlEncodedFormEntity(params));
                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity httpEntity = httpResponse.getEntity();
                is = httpEntity.getContent();
            }
            if (method == "GET") {
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpGet request = new HttpGet(url);
                HttpResponse response = httpClient.execute(request);
                HttpEntity httpEntity = response.getEntity();
                is = httpEntity.getContent();
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            try {
                return null;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return null;
        } catch(HttpHostConnectException e)
        {
            System.err.println("Unable to connect to the server");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            json = sb.toString();
        } catch (Exception e) {
            Log.e("Buffer Error", "Error converting result " + e.toString());
            return  null;
        }

        // parse the string to a JSON object
        try {
            jObj = new JSONObject(json);
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
            return  null;
        }
        return jObj;
    }
}
