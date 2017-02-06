package navigationapp.share;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import navigationapp.R;
import navigationapp.main_application.ImagePicker;

public class ShareActivity extends AppCompatActivity {

    RadioGroup radioGroup = null;
    EditText editText = null;
    Button sendButton = null;
    Boolean check = false;
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
                if (check) {
                    Toast.makeText(getApplication(),  getApplication().getString(R.string.share_was_send), Toast.LENGTH_LONG).show();
                    finish();
                }
                else
                    Toast.makeText(getApplication(),  getApplication().getString(R.string.share_check), Toast.LENGTH_LONG).show();
            }

        });


}







    void handleSendImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {


            ImagePicker aa = new ImagePicker();
            Bitmap bitmap = aa.getImage(getApplicationContext(),imageUri);


            Toast.makeText(getApplication(), "send is ok", Toast.LENGTH_LONG).show();


            ImageView myImage = (ImageView) findViewById(R.id.imageView);

            myImage.setImageBitmap(bitmap);
        } else
            Toast.makeText(getApplication(), "send not work ", Toast.LENGTH_LONG).show();
    }



}
