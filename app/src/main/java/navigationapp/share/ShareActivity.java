package navigationapp.share;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

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
    String text = null;
    Integer typeIndex = 0;
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
        sendButton =  (Button)  findViewById(R.id.share_send);
        radioGroup = (RadioGroup)  findViewById(R.id.radioGroup);
        editText = (EditText)  findViewById(R.id.editText);
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
                Log.d("rretyujtr",gps.canGetLocation()+ " getLatitude "+  gps.getLatitude()+ " getLongitude "+  gps.getLongitude() );
                if(gps.canGetLocation() &&  gps.getLatitude()!= 0 && gps.getLongitude()!=0) {
                    final double latitude = gps.getLatitude(); // vratim si polohu
                    final double longitude = gps.getLongitude();

                    if (check) {
                        typeIndex = radioGroup.indexOfChild(findViewById(radioGroup.getCheckedRadioButtonId()));
                        Location loc = new Location("Location");
                        loc.setLatitude(gps.getLatitude());
                        loc.setLongitude(gps.getLongitude());
                        BumpHandler = new Bump(loc, 6.0f, 1,typeIndex,choiseType(typeIndex));
                        BumpHandler.getResponse(new CallBackReturn() {
                            public void callback(String results) {
                                if (results.equals("success")) {
                                    Log.d(TAG,"success handler");
                                } else {
                                    Log.d(TAG,"error handler, zapisujem do db");
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

                                }
                            }
                        });
                        Toast.makeText(getApplication(),  getApplication().getString(R.string.share_was_send), Toast.LENGTH_LONG).show();
                        gps.stopUsingGPS();
                        finish();
                    }
                    else
                        Toast.makeText(getApplication(),  getApplication().getString(R.string.share_check), Toast.LENGTH_LONG).show();

                }
                else {
                    Toast.makeText(getApplication(), getApplication().getString(R.string.share_gps), Toast.LENGTH_LONG).show();
                    gps.stopUsingGPS();
                }

            }

        });
    }

    public  String getDate(long milliSeconds, String dateFormat){
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    void handleSendImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
             ImagePicker aa = new ImagePicker();
             Bitmap bitmap = aa.getImage(getApplicationContext(),imageUri);
            ImageView myImage = (ImageView) findViewById(R.id.imageView);

            myImage.setImageBitmap(bitmap);
        }
    }

    public void initialization_database(){
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
}
