// app/src/main/java/com/meinname/loginapp3/MyApplication.java
package com.meinname.loginapp3;

import android.app.Application;
import android.util.Log;

// ★ ★ ★ BEIDE IMPORTIEREN ★ ★ ★
import org.qtproject.qt5.android.bindings.QtApplication;
import com.rk.terminal.App; 

// ★ ★ ★ SHOWLOG IMPORT ★ ★ ★
import org.kivy.android.PythonService;

public class MyApplication extends Application {
    
    private static final String TAG = "MyApplication";
    private static MyApplication instance;
    
    // ============================================================
    // ★ ★ ★ SHOWLOG ★ ★ ★
    // ============================================================
    private static void showLog(String title, String message) {
        // 1. Android Log
        Log.d(TAG, title + ": " + message);
        
        // 2. PythonService.showLog verwenden (für einheitliche Logs)
        PythonService.showLog("MyApplication", title + ": " + message);
    }
    
    @Override
    public void onCreate() {
        showLog("onCreate", "🚀 MyApplication onCreate");
        
        super.onCreate();
        instance = this;
        showLog("onCreate", "📱 Application Instance gesetzt");
        
        // ★ ★ ★ 1. CORE APP INITIALISIEREN ★ ★ ★
        showLog("onCreate", "🔧 Initialisiere Core App...");
        try {
            // ★ ★ ★ Globals.init() mit diesem Application Context aufrufen ★ ★ ★
            com.meinname.ssh.Globals.init(this);
            showLog("onCreate", "✅ Core App initialisiert");
        } catch (Exception e) {
            showLog("onCreate", "❌ Core App Fehler: " + e.getMessage());
            Log.e(TAG, "❌ Core App Fehler: " + e.getMessage());
        }
        
        // ★ ★ ★ 2. QT APP INITIALISIEREN ★ ★ ★
        showLog("onCreate", "🔧 Initialisiere Qt Application...");
        try {
            // QtApplication wird normalerweise nicht manuell initialisiert
            // Es wird automatisch von QtActivity geladen
            showLog("onCreate", "✅ Qt Application bereit (wird von QtActivity geladen)");
        } catch (Exception e) {
            showLog("onCreate", "❌ Qt Application Fehler: " + e.getMessage());
            Log.e(TAG, "❌ Qt Application Fehler: " + e.getMessage());
        }
        
        showLog("onCreate", "✅ MyApplication onCreate abgeschlossen");
    }
    
    @Override
    public void onTerminate() {
        showLog("onTerminate", "🗑️ MyApplication onTerminate");
        super.onTerminate();
        // Aufräumen, falls nötig
        showLog("onTerminate", "✅ onTerminate abgeschlossen");
    }
    
    public static MyApplication getInstance() {
        showLog("getInstance", "📋 getInstance aufgerufen");
        return instance;
    }
}