package org.kivy.android;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
//import org.qtproject.qt.android.bindings.QtActivity;
import org.qtproject.qt5.android.bindings.QtActivity;

public class PythonActivity extends QtActivity {

    private static final String TAG = "PythonActivity";

    public static PythonActivity mActivity = null;

    private Bundle mMetaData = null;
    private PowerManager.WakeLock mWakeLock = null;
    
    // ============================================================
    // ★ ★ ★ SHOWLOG ★ ★ ★
    // ============================================================
    private void showLog(String title, String message) {
        // 1. Android Log
        Log.d(TAG, title + ": " + message);
        
        // 2. PythonService.showLog verwenden (für einheitliche Logs)
        PythonService.showLog("PythonActivity", title + ": " + message);
    }
    
    // ★ ★ ★ MAIN SHARED OBJECT LADEN ★ ★ ★
    protected String getMainSharedObject() {
        try {
            // 1. Versuche libmain.so im nativeLibraryDir
            String libPath = getApplicationInfo().nativeLibraryDir + "/libmain.so";
            showLog("getMainSharedObject", "🔍 Suche libmain.so in: " + libPath);
            
            if (new File(libPath).exists()) {
                showLog("getMainSharedObject", "✅ libmain.so gefunden: " + libPath);
                return libPath;
            }
            
            // 2. Fallback: libmain_arm64-v8a.so
            String libPath2 = getApplicationInfo().nativeLibraryDir + "/libmain_arm64-v8a.so";
            if (new File(libPath2).exists()) {
                showLog("getMainSharedObject", "✅ libmain_arm64-v8a.so gefunden: " + libPath2);
                return libPath2;
            }
            
            // 3. Fallback: libmain.so im libs Ordner
            String libPath3 = getApplicationInfo().dataDir + "/lib/libmain.so";
            if (new File(libPath3).exists()) {
                showLog("getMainSharedObject", "✅ libmain.so im dataDir gefunden: " + libPath3);
                return libPath3;
            }
            
            showLog("getMainSharedObject", "❌ Keine libmain.so gefunden!");
            return null;
            
        } catch (Exception e) {
            showLog("getMainSharedObject", "❌ Fehler beim Suchen: " + e.getMessage());
            return null;
        }
    }
    
    // ★ ★ ★ libmain.so LADEN ★ ★ ★
    // PythonActivity.java
protected void loadMainLibrary() {
    showLog("loadMainLibrary", "📚 Lade libmain.so...");
    
    try {
        // 1. Prüfe ob die Datei existiert
        String libPath = getApplicationInfo().nativeLibraryDir + "/libmain.so";
        File libFile = new File(libPath);
        showLog("loadMainLibrary", "📁 libmain.so Pfad: " + libPath);
        showLog("loadMainLibrary", "📁 Existiert: " + libFile.exists());
        showLog("loadMainLibrary", "📁 Größe: " + (libFile.exists() ? libFile.length() : "0"));
        
        // 2. System.loadLibrary versuchen
        try {
            showLog("loadMainLibrary", "🔧 Versuche System.loadLibrary('main')...");
            System.loadLibrary("main");
            showLog("loadMainLibrary", "✅ libmain.so geladen (System.loadLibrary)");
            return;
        } catch (UnsatisfiedLinkError e) {
            showLog("loadMainLibrary", "⚠️ System.loadLibrary('main') fehlgeschlagen: " + e.getMessage());
            showLog("loadMainLibrary", "📝 Fehler-Details: " + e.toString());
        }
        
        // 3. Mit vollständigem Pfad laden
        if (libFile.exists()) {
            showLog("loadMainLibrary", "🔧 Versuche System.load('" + libPath + "')...");
            System.load(libPath);
            showLog("loadMainLibrary", "✅ libmain.so geladen von: " + libPath);
            return;
        }
        
        // 4. Fallback: libmain_arm64-v8a.so
        String libPath2 = getApplicationInfo().nativeLibraryDir + "/libmain_arm64-v8a.so";
        File libFile2 = new File(libPath2);
        if (libFile2.exists()) {
            showLog("loadMainLibrary", "🔧 Versuche System.load('" + libPath2 + "')...");
            System.load(libPath2);
            showLog("loadMainLibrary", "✅ libmain_arm64-v8a.so geladen");
            return;
        }
        
        showLog("loadMainLibrary", "❌ libmain.so konnte nicht geladen werden!");
        
    } catch (Exception e) {
        showLog("loadMainLibrary", "❌ Fehler beim Laden: " + e.getMessage());
        showLog("loadMainLibrary", "❌ Exception: " + e.toString());
        e.printStackTrace();
    }
}
    
    public String getAppRoot() {
        String app_root = getFilesDir().getAbsolutePath() + "/app";
        showLog("getAppRoot", "📁 App Root: " + app_root);
        return app_root;
    }

    public String getEntryPoint(String search_dir) {
        showLog("getEntryPoint", "🔍 Suche EntryPoint in: " + search_dir);
        
        List<String> entryPoints = new ArrayList<String>();
        entryPoints.add("main.pyc"); // python 3 compiled files
        for (String value : entryPoints) {
            File mainFile = new File(search_dir + "/" + value);
            if (mainFile.exists()) {
                showLog("getEntryPoint", "✅ Gefunden: " + value);
                return value;
            }
        }
        showLog("getEntryPoint", "📄 Verwende main.py (Fallback)");
        return "main.py";
    }

    public void setEnvironmentVariable(String key, String value) {
        /** Sets an environment variable based on key/value. */
        showLog("setEnvironmentVariable", "🔧 " + key + " = " + value);
        try {
            android.system.Os.setenv(key, value, true);
        } catch (Exception e) {
            Log.e("Qt bootstrap", "Unable set environment variable:" + key + "=" + value);
            showLog("setEnvironmentVariable", "❌ Fehler beim Setzen von " + key + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        showLog("onCreate", "🚀 PythonActivity onCreate");
        
        this.mActivity = this;
        
        // ★ ★ ★ Bundle prüfen (für Qt) ★ ★ ★
        if (savedInstanceState == null) {
            savedInstanceState = new Bundle();
            showLog("onCreate", "📦 Bundle erstellt (war null)");
        }
        
        showLog("onCreate", "📦 Unpacke Assets...");
        File app_root_file = new File(getAppRoot());
        PythonUtil.unpackAsset(mActivity, "private", app_root_file, true);
        PythonUtil.unpackPyBundle(
                mActivity,
                getApplicationInfo().nativeLibraryDir + "/" + "libpybundle",
                app_root_file,
                false);

        Log.v("Python", "Device: " + android.os.Build.DEVICE);
        Log.v("Python", "Model: " + android.os.Build.MODEL);
        showLog("onCreate", "📱 Device: " + android.os.Build.DEVICE + ", Model: " + android.os.Build.MODEL);
        
        // ★ ★ ★ 1. libmain.so LADEN ★ ★ ★
        loadMainLibrary();

        // Set up the Python environment
        String app_root_dir = getAppRoot();
        String mFilesDirectory = mActivity.getFilesDir().getAbsolutePath();
        String entry_point = getEntryPoint(app_root_dir);

        showLog("onCreate", "⚙️ Setze Environment Variablen...");
        setEnvironmentVariable("ANDROID_ENTRYPOINT", entry_point);
        setEnvironmentVariable("ANDROID_ARGUMENT", app_root_dir);
        setEnvironmentVariable("ANDROID_APP_PATH", app_root_dir);
        setEnvironmentVariable("ANDROID_PRIVATE", mFilesDirectory);
        setEnvironmentVariable("ANDROID_UNPACK", app_root_dir);
        setEnvironmentVariable("PYTHONHOME", app_root_dir);
        setEnvironmentVariable("PYTHONPATH", app_root_dir + ":" + app_root_dir + "/lib");
        setEnvironmentVariable("PYTHONOPTIMIZE", "2");

        showLog("onCreate", "📝 Rufe super.onCreate() auf...");
        super.onCreate(savedInstanceState);
        showLog("onCreate", "✅ super.onCreate() abgeschlossen");

        this.mActivity = this;
        try {
            showLog("onCreate", "📋 Lese Meta-Data...");
            mActivity.mMetaData =
                    mActivity
                            .getPackageManager()
                            .getApplicationInfo(
                                    mActivity.getPackageName(), PackageManager.GET_META_DATA)
                            .metaData;

            PowerManager pm = (PowerManager) mActivity.getSystemService(Context.POWER_SERVICE);
            if (mActivity.mMetaData.getInt("wakelock") == 1) {
                mActivity.mWakeLock =
                        pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Screen On");
                mActivity.mWakeLock.acquire();
                showLog("onCreate", "💡 WakeLock aktiviert");
            }
        } catch (PackageManager.NameNotFoundException e) {
            showLog("onCreate", "⚠️ Meta-Data nicht gefunden: " + e.getMessage());
        }
        
        showLog("onCreate", "✅ PythonActivity.onCreate abgeschlossen");
    }

    @Override
    public void onDestroy() {
        showLog("onDestroy", "🗑️ PythonActivity onDestroy");
        Log.i("Destroy", "end of app");
        super.onDestroy();

        // make sure all child threads (python_thread) are stopped
        android.os.Process.killProcess(android.os.Process.myPid());
        showLog("onDestroy", "✅ Prozess beendet");
    }

    long lastBackClick = SystemClock.elapsedRealtime();

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (SystemClock.elapsedRealtime() - lastBackClick > 2000) {
                lastBackClick = SystemClock.elapsedRealtime();
                Toast.makeText(this, "Click again to close the app", Toast.LENGTH_LONG).show();
                showLog("onKeyDown", "🔙 Back-Taste gedrückt (Toast)");
                return true;
            }
            lastBackClick = SystemClock.elapsedRealtime();
            showLog("onKeyDown", "🔙 Back-Taste doppelt = beenden");
        }
        return super.onKeyDown(keyCode, event);
    }

    // ----------------------------------------------------------------------------
    // Listener interface for onNewIntent
    //

    public interface NewIntentListener {
        void onNewIntent(Intent intent);
    }

    private List<NewIntentListener> newIntentListeners = null;

    public void registerNewIntentListener(NewIntentListener listener) {
        if (this.newIntentListeners == null)
            this.newIntentListeners =
                    Collections.synchronizedList(new ArrayList<NewIntentListener>());
        this.newIntentListeners.add(listener);
        showLog("registerNewIntentListener", "📝 Listener registriert");
    }

    public void unregisterNewIntentListener(NewIntentListener listener) {
        if (this.newIntentListeners == null) return;
        this.newIntentListeners.remove(listener);
        showLog("unregisterNewIntentListener", "🗑️ Listener entfernt");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        showLog("onNewIntent", "📨 New Intent: " + intent);
        if (this.newIntentListeners == null) return;
        this.onResume();
        synchronized (this.newIntentListeners) {
            Iterator<NewIntentListener> iterator = this.newIntentListeners.iterator();
            while (iterator.hasNext()) {
                (iterator.next()).onNewIntent(intent);
            }
        }
    }

    // ----------------------------------------------------------------------------
    // Listener interface for onActivityResult
    //

    public interface ActivityResultListener {
        void onActivityResult(int requestCode, int resultCode, Intent data);
    }

    private List<ActivityResultListener> activityResultListeners = null;

    public void registerActivityResultListener(ActivityResultListener listener) {
        if (this.activityResultListeners == null)
            this.activityResultListeners =
                    Collections.synchronizedList(new ArrayList<ActivityResultListener>());
        this.activityResultListeners.add(listener);
        showLog("registerActivityResultListener", "📝 ActivityResultListener registriert");
    }

    public void unregisterActivityResultListener(ActivityResultListener listener) {
        if (this.activityResultListeners == null) return;
        this.activityResultListeners.remove(listener);
        showLog("unregisterActivityResultListener", "🗑️ ActivityResultListener entfernt");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        showLog("onActivityResult", "📨 onActivityResult: " + requestCode + ", " + resultCode);
        if (this.activityResultListeners == null) return;
        this.onResume();
        synchronized (this.activityResultListeners) {
            Iterator<ActivityResultListener> iterator = this.activityResultListeners.iterator();
            while (iterator.hasNext())
                (iterator.next()).onActivityResult(requestCode, resultCode, intent);
        }
    }

    public static void start_service(
            String serviceTitle, String serviceDescription, String pythonServiceArgument) {
        _do_start_service(serviceTitle, serviceDescription, pythonServiceArgument, true);
    }

    public static void start_service_not_as_foreground(
            String serviceTitle, String serviceDescription, String pythonServiceArgument) {
        _do_start_service(serviceTitle, serviceDescription, pythonServiceArgument, false);
    }

    public static void _do_start_service(
            String serviceTitle,
            String serviceDescription,
            String pythonServiceArgument,
            boolean showForegroundNotification) {
        PythonService.showLog("PythonActivity", "🚀 Starte Service: " + serviceTitle);
        
        Intent serviceIntent = new Intent(PythonActivity.mActivity, PythonService.class);
        String argument = PythonActivity.mActivity.getFilesDir().getAbsolutePath();
        String app_root_dir = PythonActivity.mActivity.getAppRoot();
        String entry_point = PythonActivity.mActivity.getEntryPoint(app_root_dir + "/service");
        
        serviceIntent.putExtra("androidPrivate", argument);
        serviceIntent.putExtra("androidArgument", app_root_dir);
        serviceIntent.putExtra("serviceEntrypoint", "service/" + entry_point);
        serviceIntent.putExtra("pythonName", "python");
        serviceIntent.putExtra("pythonHome", app_root_dir);
        serviceIntent.putExtra("pythonPath", app_root_dir + ":" + app_root_dir + "/lib");
        serviceIntent.putExtra(
                "serviceStartAsForeground", (showForegroundNotification ? "true" : "false"));
        serviceIntent.putExtra("serviceTitle", serviceTitle);
        serviceIntent.putExtra("serviceDescription", serviceDescription);
        serviceIntent.putExtra("pythonServiceArgument", pythonServiceArgument);
        
        PythonActivity.mActivity.startService(serviceIntent);
        PythonService.showLog("PythonActivity", "✅ Service gestartet: " + serviceTitle);
    }

    public static void stop_service() {
        PythonService.showLog("PythonActivity", "⏹️ Stoppe Service");
        Intent serviceIntent = new Intent(PythonActivity.mActivity, PythonService.class);
        PythonActivity.mActivity.stopService(serviceIntent);
        PythonService.showLog("PythonActivity", "✅ Service gestoppt");
    }
}