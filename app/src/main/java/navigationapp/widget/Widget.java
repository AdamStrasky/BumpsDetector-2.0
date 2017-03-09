package navigationapp.widget;

import android.app.AlertDialog;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RemoteViews;

import navigationapp.R;

public class Widget extends AppWidgetProvider {

    @Override
    public synchronized void onReceive(Context context, Intent intent) {
        if ( intent.getAction()==null || ( intent.getExtras()!=null && intent.getExtras().containsKey("type"))) {

            Integer type = 0;

            if (intent.getExtras()!=null  && intent.getExtras().containsKey("type"))
                type = intent.getExtras().getInt("type",0);

            Intent i = new Intent(context, UpdateService.class);
            i.putExtra("click", true);
            i.putExtra("type", type);
            context.startService(i);
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr,int[] appWidgetIds) {
        context.startService(new Intent(context, UpdateService.class));
    }

    public static class UpdateService extends IntentService {
        public UpdateService() {
            super("Widget$UpdateService");
        }

        @Override
        public void onCreate() {
            super.onCreate();
        }

        @Override
        public void onHandleIntent(Intent intent) {
            Boolean extra = intent.getBooleanExtra("click",false);
            Integer type = intent.getIntExtra("type",0);
            ComponentName componentName=new ComponentName(this, Widget.class);
            AppWidgetManager manager=AppWidgetManager.getInstance(this);
            manager.updateAppWidget(componentName, buildUpdate(this,extra,type));
        }

        private RemoteViews buildUpdate(final Context context, Boolean flag ,final Integer type) {

            RemoteViews updateViews=new RemoteViews(context.getPackageName(), R.layout.widget_horizontal);
            if (flag) {
                String ANDROID_ID = Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ANDROID_ID);
                if (type == 0) {
                    new Click(getApplicationContext(), type, "bump",ANDROID_ID);
                } else if (type == 1) {
                    new Click(getApplicationContext(), type, "bin",ANDROID_ID);
                }  else if (type == 2) {
                    new Click(getApplicationContext(), type, "channel",ANDROID_ID);
                }
                else if (type == 3) {

                    Handler mHandler = new Handler(getMainLooper());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            final EditText edittext = new EditText(context);
                            AlertDialog dialog = new AlertDialog.Builder(context)
                                    .setTitle(context.getResources().getString(R.string.alert_widget))
                                    .setView(edittext)
                                    .setPositiveButton(context.getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                           String  select_iteam_text = "Other";
                                            if (!edittext.getText().toString().isEmpty())
                                                select_iteam_text = edittext.getText().toString();
                                            String ANDROID_ID = Settings.Secure.getString(getContentResolver(),
                                                    Settings.Secure.ANDROID_ID);
                                            new Click(getApplicationContext(), type, select_iteam_text,ANDROID_ID);
                                            dialog.dismiss();
                                        }
                                    })
                                    .setNegativeButton(context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.dismiss();
                                        }
                                    }).create();
                            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                            dialog.show();
                        }
                    });
                }
            }

            Intent bump = new Intent(this, Widget.class);
            bump.putExtra("type", 0);
            PendingIntent PendingIntentBump =PendingIntent.getBroadcast(context,0, bump,0);

            Intent trash = new Intent(this, Widget.class);
            trash.putExtra("type", 1);
            PendingIntent PendingIntentTrash=PendingIntent.getBroadcast(context,1, trash,0);

            Intent canstock = new Intent(this, Widget.class);
            canstock.putExtra("type", 2);
            PendingIntent PendingIntentCanstock=PendingIntent.getBroadcast(context,2, canstock,0);

            Intent select = new Intent(this, Widget.class);
            select.putExtra("type", 3);
            PendingIntent PendingIntentSelect=PendingIntent.getBroadcast(context,3, select,0);

            updateViews.setOnClickPendingIntent(R.id.bump,PendingIntentBump);
            updateViews.setOnClickPendingIntent(R.id.trash,PendingIntentTrash);
            updateViews.setOnClickPendingIntent(R.id.canstock,PendingIntentCanstock);
            updateViews.setOnClickPendingIntent(R.id.select,PendingIntentSelect);

            return(updateViews);
        }


    }
}