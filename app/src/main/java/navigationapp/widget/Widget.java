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
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Toast;

import navigationapp.R;

public class Widget extends AppWidgetProvider {

    @Override
    public synchronized void onReceive(Context context, Intent intent) {
       if (intent.getAction()==null || intent.getExtras().getString("bucketno")!=null) {
           String value = "eeee";
        if ( intent.getExtras().getString("bucketno")!=null)
            value = intent.getExtras().getString("bucketno");
           Intent i = new Intent(context, UpdateService.class);
           i.putExtra("click", true);
           i.putExtra("value", value);

           context.startService(i);
       }
       else {
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
           String value = intent.getStringExtra("value");
           ComponentName componentName=new ComponentName(this, Widget.class);
           AppWidgetManager manager=AppWidgetManager.getInstance(this);
           manager.updateAppWidget(componentName, buildUpdate(this,extra,value));
        }

        private RemoteViews buildUpdate(final Context context, Boolean flag, final String value) {

           RemoteViews updateViews=new RemoteViews(context.getPackageName(), R.layout.widget);
           if (flag) {

               Handler mHandler = new Handler(getMainLooper());
               mHandler.post(new Runnable() {
                   @Override
                   public void run() {
                       Toast.makeText(getApplicationContext(), value, Toast.LENGTH_LONG).show();
               AlertDialog dialog = new AlertDialog.Builder(context)
                       .setTitle(context.getResources().getString(R.string.list))
                      .setPositiveButton(context.getResources().getString(R.string.navige_to), new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialog, int id) {
                               dialog.dismiss();
                           }
                       })
                       .setNeutralButton(context.getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialog, int id) {
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

              // new Click(getApplicationContext(), type, text);

            }

           Intent i=new Intent(this, Widget.class);
            i.putExtra("bucketno", "aaaa");
            i.putExtra("bucketna", 5);
           PendingIntent pi=PendingIntent.getBroadcast(context,0, i,0);

           updateViews.setOnClickPendingIntent(R.id.actionButton,pi);
           return(updateViews);
        }


    }
}