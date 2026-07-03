// App.kt
package com.rk.terminal

import android.app.Application
import android.os.Build
import android.os.StrictMode
import com.github.anrwatchdog.ANRWatchDog
import com.rk.libcommons.application
import com.rk.resources.Res
import com.rk.update.UpdateManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.screens.terminal.TerminalBackEnd

import com.meinname.ssh.Globals

private fun showLog(title: String, message: String) {
    try {
        MainActivity.showLog("App", "[$title] $message")
    } catch (e: Exception) {
        android.util.Log.d("App", "[$title] $message")
    }
}

class App : Application() {

    @OptIn(DelicateCoroutinesApi::class)
    companion object {
        fun getTempDir(): File {
            val tmp = File(application!!.filesDir.parentFile, "tmp")
            if (!tmp.exists()) {
                tmp.mkdir()
                showLog("Info", "📁 Temporärer Ordner erstellt: ${tmp.absolutePath}")
            }
            return tmp
        }
    }
    

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        
        Globals.init(this)
        
        showLog("Info", "🚀 App onCreate gestartet")
        showLog("Debug", "📱 SDK: ${Build.VERSION.SDK_INT}, Device: ${Build.MODEL}")
        showLog("Debug", "📱 Package: ${packageName}")
        
        application = this
        Res.application = this

        // ============================================================
        // TEMPORÄRER ORDNER
        // ============================================================
        GlobalScope.launch(Dispatchers.IO) {
            getTempDir().apply {
                if (exists() && listFiles().isNullOrEmpty().not()) {
                    val fileCount = listFiles()?.size ?: 0
                    showLog("Info", "🗑️ Lösche temporären Ordner ($fileCount Dateien)")
                    deleteRecursively()
                    showLog("Debug", "✅ Temporärer Ordner gelöscht")
                } else {
                    showLog("Debug", "⏭️ Temporärer Ordner ist leer oder existiert nicht")
                }
            }
        }

        // ============================================================
        // ANR WATCHDOG
        // ============================================================
        showLog("Debug", "🔍 Starte ANRWatchDog...")
        ANRWatchDog().start()
        showLog("Info", "✅ ANRWatchDog gestartet")

        // ============================================================
        // UPDATE MANAGER
        // ============================================================
        showLog("Debug", "🔄 Prüfe auf Updates...")
        UpdateManager().onUpdate()
        showLog("Debug", "✅ Update-Check abgeschlossen")

        // ============================================================
        // STRICT MODE
        // ============================================================
        if (BuildConfig.DEBUG) {
            showLog("Info", "🔧 StrictMode aktiviert (Debug-Modus)")
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().apply {
                    detectAll()
                    penaltyLog()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                            showLog("Error", "❌ StrictMode Verletzung: ${violation.message}")
                            violation.printStackTrace()
                            violation.cause?.let { throw it }
                            showLog("Error", "❌ StrictMode Fehler: ${violation.message}")
                        }
                    }
                }.build()
            )
            showLog("Debug", "✅ StrictMode konfiguriert")
        } else {
            showLog("Debug", "⏭️ StrictMode deaktiviert (Release-Modus)")
        }

        showLog("Info", "✅ App onCreate abgeschlossen")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }
    
    
}