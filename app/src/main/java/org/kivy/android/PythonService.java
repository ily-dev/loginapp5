package org.kivy.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import android.os.Handler;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.os.Environment;

public class PythonService extends Service implements Runnable {
    
    // Als Feld in der Klasse:
    private Handler handler = new Handler();

    // Thread for Python code
    private Thread pythonThread = null;
    
    private static final String TAG = "PythonService";
    
    private static Context appContext;

    // Python environment variables
    private String androidPrivate;
    private String androidArgument;
    private String pythonName;
    private String pythonHome;
    private String pythonPath;
    private String serviceEntrypoint;
    // Argument to pass to Python code,
    private String pythonServiceArgument;

    public static PythonService mService = null;
    private Intent startIntent = null;
    

    private boolean autoRestartService = false;

    public void setAutoRestartService(boolean restart) {
        autoRestartService = restart;
    }

    public int startType() {
        return START_NOT_STICKY;
    }
    
    public static void setAppContext(Context ctx) {
        appContext = ctx;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;  // ← Context speichern
        
    }
    
    // ============================================================
    // NEUE STATISCHE METHODE: Startet Python ohne Activity
    // ============================================================
    public static void startPython() {
        try {
            // Benutze appContext statt mActivity
            Context ctx = appContext;
            
            
            
            if (ctx == null) {
                // Fallback: Versuche mActivity
                //ctx = mActivity;
            }
            
            if (ctx != null) {
                Intent intent = new Intent(ctx, PythonActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                showLog(TAG, "✅ startPython wird ausgeführt!");
                ctx.startActivity(intent);
            } else {
                showLog(TAG, "❌ Context ist null - kann nicht starten");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showLog(TAG, "❌ startPython failed: " + e.getMessage());
        }
    }
    

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (pythonThread != null) {
            showLog("python service", "service exists, do not start again");
            return startType();
        }
        // intent is null if OS restarts a STICKY service
        if (intent == null) {
            Context context = getApplicationContext();
            intent = getThisDefaultIntent(context, "");
        }

        startIntent = intent;
        Bundle extras = intent.getExtras();
        androidPrivate = extras.getString("androidPrivate");
        androidArgument = extras.getString("androidArgument");
        serviceEntrypoint = extras.getString("serviceEntrypoint");
        pythonName = extras.getString("pythonName");
        pythonHome = extras.getString("pythonHome");
        pythonPath = extras.getString("pythonPath");
        boolean serviceStartAsForeground =
                (extras.getString("serviceStartAsForeground").equals("true"));
        pythonServiceArgument = extras.getString("pythonServiceArgument");
        pythonThread = new Thread(this);
        pythonThread.start();

        if (serviceStartAsForeground) {
            doStartForeground(extras);
        }

        return startType();
    }

    protected int getServiceId() {
        return 1;
    }
    
    // Native part
    public static native void nativeStart(
            String androidPrivate,
            String androidArgument,
            String serviceEntrypoint,
            String pythonName,
            String pythonHome,
            String pythonPath,
            String pythonServiceArgument);

    protected Intent getThisDefaultIntent(Context ctx, String pythonServiceArgument) {
        return null;
    }

    protected void doStartForeground(Bundle extras) {
        String serviceTitle = extras.getString("serviceTitle");
        String smallIconName = extras.getString("smallIconName");
        String contentTitle = extras.getString("contentTitle");
        String contentText = extras.getString("contentText");
        Notification notification;
        Context context = getApplicationContext();
        Intent contextIntent = new Intent(context, PythonActivity.class);
        PendingIntent pIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        contextIntent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Unspecified icon uses default.
        int smallIconId = context.getApplicationInfo().icon;
        if (smallIconName != null) {
            if (!smallIconName.equals("")) {
                int resId = getResources().getIdentifier(smallIconName, "mipmap", getPackageName());
                if (resId == 0) {
                    resId =
                            getResources()
                                    .getIdentifier(smallIconName, "drawable", getPackageName());
                }
                if (resId != 0) {
                    smallIconId = resId;
                }
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // This constructor is deprecated
            notification = new Notification(smallIconId, serviceTitle, System.currentTimeMillis());
            try {
                // prevent using NotificationCompat, this saves 100kb on apk
                Method func =
                        notification
                                .getClass()
                                .getMethod(
                                        "setLatestEventInfo",
                                        Context.class,
                                        CharSequence.class,
                                        CharSequence.class,
                                        PendingIntent.class);
                func.invoke(notification, context, contentTitle, contentText, pIntent);
            } catch (NoSuchMethodException
                    | IllegalAccessException
                    | IllegalArgumentException
                    | InvocationTargetException e) {
            }
        } else {
            // for android 8+ we need to create our own channel
            // https://stackoverflow.com/questions/47531742/startforeground-fail-after-upgrade-to-android-8-1
            String NOTIFICATION_CHANNEL_ID = "org.kivy.p4a" + getServiceId();
            String channelName = "Background Service" + getServiceId();
            NotificationChannel chan =
                    new NotificationChannel(
                            NOTIFICATION_CHANNEL_ID,
                            channelName,
                            NotificationManager.IMPORTANCE_NONE);

            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(chan);

            Notification.Builder builder =
                    new Notification.Builder(context, NOTIFICATION_CHANNEL_ID);
            builder.setContentTitle(contentTitle);
            builder.setContentText(contentText);
            builder.setContentIntent(pIntent);
            builder.setSmallIcon(smallIconId);
            notification = builder.build();
        }
        startForeground(getServiceId(), notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pythonThread = null;
        if (autoRestartService && startIntent != null) {
            showLog("python service", "service restart requested");
            startService(startIntent);
        }
        Process.killProcess(Process.myPid());
    }

    /**
     * Stops the task gracefully when killed. Calling stopSelf() will trigger a onDestroy() call
     * from the system.
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // sticky service runtime/restart is managed by the OS. leave it running when app is closed
        if (startType() != START_STICKY) {
            stopSelf();
        }
    }

    @Override
    public void run() {
    try {
        showLog("DEBUG", "=== SERVICE START ===");
        
        String filesDir = getFilesDir().getAbsolutePath();
        String app_root = filesDir + "/app";
        String androidArgument = filesDir + "/app";
        
        File app_root_file = new File(app_root);
        
        showLog("DEBUG", "filesDir: " + filesDir);
        showLog("DEBUG", "app_root: " + app_root);
        showLog("DEBUG", "androidArgument: " + androidArgument);
        showLog("DEBUG", "serviceEntrypoint: " + serviceEntrypoint);
        
        // Mögliche Suchpfade
        String[] possiblePaths = {
            androidArgument + "/" + serviceEntrypoint,
            app_root + "/service/main.pyc",
            app_root + "/main.pyc"
        };
        
        File foundEntrypoint = null;
        
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                showLog("FOUND", "✅ " + path);
                if (foundEntrypoint == null) {
                    foundEntrypoint = file;
                }
            } else {
                showLog("NOT FOUND", "❌ " + path);
            }
        }
        
        // Prüfe app_root
        if (!app_root_file.exists()) {
            showLog("ERROR", "❌ app_root fehlt: " + app_root);
            stopSelf();
            return;
        }
        
        // Zeige Verzeichnis-Inhalt
        String[] appContents = app_root_file.list();
        if (appContents != null) {
            showLog("CONTENT", "📁 app_root Inhalt (" + appContents.length + " Dateien):");
            for (String file : appContents) {
                showLog("CONTENT", "   - " + file);
            }
        }
        
        if (foundEntrypoint == null) {
            showLog("ERROR", "❌ Keine main.py/service gefunden!");
            stopSelf();
            return;
        }
        
        showLog("SUCCESS", "✅ Service startet mit: " + foundEntrypoint.getAbsolutePath());
        
        // Libraries laden
        PythonUtil.loadLibraries(app_root_file, new File(getApplicationInfo().nativeLibraryDir));
        
        showLog("NATIVE", "=== NATIVE START ===");
        showLog("NATIVE", "androidArgument: " + androidArgument);
        showLog("NATIVE", "serviceEntrypoint: " + serviceEntrypoint);
        showLog("NATIVE", "pythonName: " + pythonName);
        showLog("NATIVE", "pythonHome: " + pythonHome);
        showLog("NATIVE", "pythonPath: " + pythonPath);
                
        this.mService = this;
        
        nativeStart(
                androidPrivate,
                androidArgument,
                serviceEntrypoint,
                pythonName,
                pythonHome,
                pythonPath,
                pythonServiceArgument);
                
        showLog("NATIVE", "✅ nativeStart erfolgreich beendet");
                
    } catch (Exception e) {
        showLog("NATIVE", "❌ nativeStart Fehler: " + e.getMessage());
        
    } finally {
        stopSelf();
    }
}

    

private void showDialog(final String title, final String message) {
    handler.post(new Runnable() {
        @Override
        public void run() {
            AlertDialog.Builder builder = new AlertDialog.Builder(PythonService.this);
            builder.setTitle(title);
            builder.setMessage(message);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setCancelable(false);
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    });
}

// Hilfsmethode für Toast (muss auf UI-Thread laufen)
private void showToast(final String message) {
    handler.post(new Runnable() {
        @Override
        public void run() {
            Toast.makeText(PythonService.this, message, Toast.LENGTH_LONG).show();
        }
    });
}

public static void showLog(String title, String message) {
    // Android Log
    Log.d("PythonService", title + ": " + message);
    
    try {
        // ★★ appContext wird ohne Prüfung verwendet ★★
        File sdCard = Environment.getExternalStorageDirectory();
        File logFile = new File(sdCard, "service.log");
        java.io.FileWriter fw = new java.io.FileWriter(logFile, true);
        fw.write(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) 
            + " - " + title + ": " + message + "\n");
        fw.close();
    } catch (Exception e) {
        Log.e("PythonService", "Fehler: " + e.getMessage());
    }
}

}

/*
public static void showLog(String title, String message) {
        // Android Log
        Log.d("PythonService", title + ": " + message);
        

        try {
            if (appContext != null) {
            
                // In internen Speicher schreiben
                // File logFile = new File(appContext.getFilesDir(), "service_debug.log");
                
                // 1. SD-Karte Pfad: /storage/emulated/0/
                File sdCard = Environment.getExternalStorageDirectory();
                File logFile = new File(sdCard, "service.log");
                
                java.io.FileWriter fw = new java.io.FileWriter(logFile, true);
                fw.write(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) 
                    + " - " + title + ": " + message + "\n");
                fw.close();
            }
        } catch (Exception e) {
            Log.e("PythonService", "Fehler: " + e.getMessage());
        }
    }
}
*/
