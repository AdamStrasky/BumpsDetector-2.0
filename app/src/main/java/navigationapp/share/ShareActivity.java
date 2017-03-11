package navigationapp.share;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import navigationapp.R;
import navigationapp.main_application.Bump;
import navigationapp.main_application.CallBackReturn;
import navigationapp.main_application.DatabaseOpenHelper;
import navigationapp.main_application.ImagePicker;
import navigationapp.main_application.Provider;
import navigationapp.main_application.UploadPhoto;
import navigationapp.voice_application.GPSPosition;

public class ShareActivity extends AppCompatActivity {

    RadioGroup radioGroup = null;
    EditText editText = null;
    Button sendButton = null;
    Boolean check = false;
    private GPSPosition gps = null;
    private Bump BumpHandler = null;
    private final String TAG = "ShareActivity";
    private SQLiteDatabase sb = null;
    private DatabaseOpenHelper databaseHelper = null;
    private UploadPhoto HandlerPhoto = null;
    Integer typeIndex = 0;
    File f = null;
    Bitmap bitmap = null;
    ProgressDialog pDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }
        }
        sendButton = (Button) findViewById(R.id.share_send);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        editText = (EditText) findViewById(R.id.editText);
        editText.setEnabled(false);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                check = true;
                if (checkedId == R.id.other) {
                    editText.setEnabled(true);
                } else
                    editText.setEnabled(false);
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {  // vymazenie textu navige to na kliknutie
            @Override
            public void onClick(final View v) {
                initialization_database();
                gps = new GPSPosition(ShareActivity.this);
                Log.d(TAG, gps.canGetLocation() + " getLatitude " + gps.getLatitude() + " getLongitude " + gps.getLongitude());
                if (gps.canGetLocation() && gps.getLatitude() != 0 && gps.getLongitude() != 0) {
                    final double latitude = gps.getLatitude(); // vratim si polohu
                    final double longitude = gps.getLongitude();

                    if (check) {
                        pDialog = new ProgressDialog(ShareActivity.this);
                        pDialog.setMessage(getApplication().getString(R.string.share_upload));
                        pDialog.show();
                        typeIndex = radioGroup.indexOfChild(findViewById(radioGroup.getCheckedRadioButtonId()));
                        Location loc = new Location("Location");
                        loc.setLatitude(gps.getLatitude());
                        loc.setLongitude(gps.getLongitude());
                        loc.setTime(new Date().getTime());
                        BumpHandler = new Bump(loc, 6.0f, 1, typeIndex, choiseType(typeIndex), Settings.Secure.getString(getContentResolver(),
                                Settings.Secure.ANDROID_ID));
                        BumpHandler.getResponse(new CallBackReturn() {
                            public void callback(String results) {
                                if (results.equals("success")) {
                                    create_file_photo();
                                    HandlerPhoto = new UploadPhoto(getBaseContext(),String.valueOf(latitude), String.valueOf(longitude), String.valueOf(typeIndex), getDate(new Date().getTime(), "yyyy-MM-dd HH:mm:ss"), f.getPath());
                                    HandlerPhoto.getResponse(new CallBackReturn() {
                                        public void callback(String results) {
                                            if (results.equals("success")) {
                                                    Log.d(TAG, "UploadPhoto success ");
                                                    f.delete();
                                            } else {
                                                Log.d(TAG, "UploadPhoto errror ");
                                                save_photo(String.valueOf(latitude),String.valueOf(longitude),String.valueOf(typeIndex),f.getPath());
                                            }
                                        }
                                    });
                                    Log.d(TAG, "success handler");
                                } else {
                                    Log.d(TAG, "error handler, zapisujem do db");
                                    BigDecimal bd = new BigDecimal(Float.toString(6));
                                    bd = bd.setScale(6, BigDecimal.ROUND_HALF_UP);
                                    ContentValues contentValues = new ContentValues();
                                    contentValues.put(Provider.new_bumps.LATITUDE, latitude);
                                    contentValues.put(Provider.new_bumps.LONGTITUDE, longitude);
                                    contentValues.put(Provider.new_bumps.MANUAL, 1);
                                    contentValues.put(Provider.new_bumps.INTENSITY, String.valueOf(bd));
                                    contentValues.put(Provider.new_bumps.TYPE, typeIndex);
                                    contentValues.put(Provider.new_bumps.TEXT, choiseType(typeIndex));
                                    contentValues.put(Provider.new_bumps.CREATED_AT, getDate(new Date().getTime(), "yyyy-MM-dd HH:mm:ss"));
                                    sb.insert(Provider.new_bumps.TABLE_NAME_NEW_BUMPS, null, contentValues);
                                    create_file_photo();
                                    save_photo(String.valueOf(latitude),String.valueOf(longitude),String.valueOf(typeIndex),f.getPath());
                                }
                                pDialog.dismiss();
                            }
                        });
                        Toast.makeText(getApplication(), getApplication().getString(R.string.share_was_send), Toast.LENGTH_LONG).show();
                        gps.stopUsingGPS();
                        finish();
                    } else
                        Toast.makeText(getApplication(), getApplication().getString(R.string.share_check), Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(getApplication(), getApplication().getString(R.string.share_gps), Toast.LENGTH_LONG).show();
                    gps.stopUsingGPS();
                }
            }
        });
    }

    public String getDate(long milliSeconds, String dateFormat) {
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    void handleSendImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            ImagePicker aa = new ImagePicker();
            bitmap = aa.getImage(getApplicationContext(), imageUri);
            ImageView myImage = (ImageView) findViewById(R.id.imageView);
            myImage.setImageBitmap(bitmap);
        }
    }

    public void initialization_database() {
        // inicializacia databazy
        databaseHelper = new DatabaseOpenHelper(this);
        sb = databaseHelper.getWritableDatabase();
    }

    public String choiseType(Integer type) {
        String text = null;
        switch (type) {
            case 0:
                text = "bump";
                break;
            case 1:
                text = "bin";
                break;
            case 2:
                text = "cover";
                break;
            case 3:
                text = editText.toString();
                break;
        }
        return text;
    }

    public void save_photo(String latitude, String longitude, String type, String path) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Provider.photo.LATITUDE, latitude);
        contentValues.put(Provider.photo.LONGTITUDE, longitude);
        contentValues.put(Provider.photo.TYPE, type);
        contentValues.put(Provider.photo.PATH, path);
        contentValues.put(Provider.photo.CREATED_AT, getDate(new Date().getTime(), "yyyy-MM-dd HH:mm:ss"));
        sb.insert(Provider.photo.TABLE_NAME_PHOTO, null, contentValues);
    }

    public void create_file_photo() {
        if (bitmap != null) {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/.Detector");
            if(!dir.exists()){
                dir.mkdirs();
            }

            f = new File(new File(String.valueOf(dir)), bitmap.toString() + ".png");
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
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
        }
    }
}
