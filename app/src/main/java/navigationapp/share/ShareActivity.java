package navigationapp.share;

import android.content.Intent;
import android.graphics.Bitmap;
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

import navigationapp.R;
import navigationapp.main_application.ImagePicker;
import navigationapp.voice_application.GPSPosition;

public class ShareActivity extends AppCompatActivity {

    RadioGroup radioGroup = null;
    EditText editText = null;
    Button sendButton = null;
    Boolean check = false;
    private GPSPosition gps = null;
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
                gps = new GPSPosition(ShareActivity.this);
                Log.d("rretyujtr",gps.canGetLocation()+ " getLatitude "+  gps.getLatitude()+ " getLongitude "+  gps.getLongitude() );
                if(gps.canGetLocation() &&  gps.getLatitude()!= 0 && gps.getLongitude()!=0) {
                    final double latitude = gps.getLatitude(); // vratim si polohu
                    final double longitude = gps.getLongitude();

                    if (check) {
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







    void handleSendImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
             ImagePicker aa = new ImagePicker();
             Bitmap bitmap = aa.getImage(getApplicationContext(),imageUri);
            ImageView myImage = (ImageView) findViewById(R.id.imageView);

            myImage.setImageBitmap(bitmap);
        }
    }



}
