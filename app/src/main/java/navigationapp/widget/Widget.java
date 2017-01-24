package navigationapp.widget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import navigationapp.R;

public class Widget extends AppWidgetProvider {

    @Override
    public synchronized void onReceive(Context context, Intent intent) {
       if (intent.getAction()==null) {
           Intent i = new Intent(context, UpdateService.class);
           i.putExtra("click", true);
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
           ComponentName componentName=new ComponentName(this, Widget.class);
           AppWidgetManager manager=AppWidgetManager.getInstance(this);
           manager.updateAppWidget(componentName, buildUpdate(this,extra));
        }

        private RemoteViews buildUpdate(Context context,Boolean flag) {

           RemoteViews updateViews=new RemoteViews(context.getPackageName(), R.layout.widget);
           if (flag) {
               new Click(getApplicationContext());
           }

           Intent i=new Intent(this, Widget.class);
           PendingIntent pi=PendingIntent.getBroadcast(context,0, i,0);
           updateViews.setOnClickPendingIntent(R.id.actionButton,pi);
           return(updateViews);
        }
    }
}