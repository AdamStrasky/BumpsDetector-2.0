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
    public synchronized void onReceive(Context ctxt, Intent intent) {
       if (intent.getAction()==null) {
            Intent i = new Intent(ctxt, UpdateService.class);
            i.putExtra("click", true);
            ctxt.startService(i);
       }
       else {
         super.onReceive(ctxt, intent);
       }
    }

    @Override
    public void onUpdate(Context ctxt, AppWidgetManager mgr,int[] appWidgetIds) {
        ctxt.startService(new Intent(ctxt, UpdateService.class));
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
           ComponentName me=new ComponentName(this, Widget.class);
           AppWidgetManager mgr=AppWidgetManager.getInstance(this);
           mgr.updateAppWidget(me, buildUpdate(this,extra));
        }

        private RemoteViews buildUpdate(Context context,Boolean flag) {

           RemoteViews updateViews=new RemoteViews(context.getPackageName(), R.layout.widget);
           if (flag) {
               Click a = new Click(getApplicationContext());
            }
            Intent i=new Intent(this, Widget.class);
            PendingIntent pi=PendingIntent.getBroadcast(context,0, i,0);
            updateViews.setOnClickPendingIntent(R.id.actionButton,pi);
            return(updateViews);
        }
    }
}