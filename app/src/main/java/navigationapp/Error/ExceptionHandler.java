package navigationapp.Error;

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler defaultUEH;

    public ExceptionHandler() {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void uncaughtException(Thread t, Throwable e) {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = df.format(c.getTime());
        StackTraceElement[] arr = e.getStackTrace();
        String report = "";
        report += "--------- New error ---------\n\n";
        report += "---------"+ formattedDate +"---------\n\n";
        report+= e.toString()+"\n\n";

        report += "--------- Stack trace ---------\n\n";
        for (int i=0; i<arr.length; i++)
        {
            report += "    "+arr[i].toString()+"\n";
        }
        report += "-------------------------------\n\n";

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        report += "--------- Cause ---------\n\n";
        Throwable cause = e.getCause();
        if(cause != null) {
            report += cause.toString() + "\n\n";
            arr = cause.getStackTrace();
            for (int i=0; i<arr.length; i++) {
                report += "    "+arr[i].toString()+"\n";
            }
        }
        report += "-------------------------------\n\n";
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + "/Logger");
        if(!dir.exists()){
           dir.mkdirs();
        }

        File out = null;
        OutputStreamWriter outStreamWriter = null;
        FileOutputStream outStream = null;

        out = new File(new File(String.valueOf(dir)), "error.txt");
        if ( !out.exists()){
            try {
                out.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        try {
            outStream = new FileOutputStream(out, true);
            outStreamWriter = new OutputStreamWriter(outStream);
        } catch (FileNotFoundException e1) {
            e.printStackTrace();
        }

        try {
            outStreamWriter.append(report);
            outStreamWriter.flush();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        defaultUEH.uncaughtException(t, e);
    }
}
