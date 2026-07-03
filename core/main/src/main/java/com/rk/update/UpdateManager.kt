package com.rk.update

import com.rk.libcommons.application
import com.rk.libcommons.child
import com.rk.libcommons.createFileIfNot
import com.rk.libcommons.localBinDir
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.meinname.ssh.Globals
import java.io.File

// ============================================================
// ★ LOKALE SHOWLOG FUNKTION
// ============================================================
private fun showLog(title: String, message: String) {
    try {
        MainActivity.showLog("UpdateManager", "[$title] $message")
    } catch (e: Exception) {
        android.util.Log.d("UpdateManager", "[$title] $message")
    }
}

class UpdateManager {
    
    // ============================================================
    // ★ ★ ★ GET ASSET PATH BASED ON WORKING MODE ★ ★ ★
    // ============================================================
    private fun getInitHostAssetPath(): String {
        return when {
            Globals.isAlpine() -> "alpine/init-host.sh"
            Globals.isUbuntu() -> "ubuntu/init-host.sh"
            Globals.isAndroid() -> {
                showLog("Warn", "📟 Android-Modus: Kein init-host benötigt")
                return ""
            }
            else -> {
                showLog("Warn", "⚠️ Unbekannter WorkingMode, verwende Alpine als Fallback")
                "alpine/init-host.sh"
            }
        }
    }
    
    private fun getInitAssetPath(): String {
        return when {
            Globals.isAlpine() -> "alpine/init.sh"
            Globals.isUbuntu() -> "ubuntu/init.sh"
            Globals.isAndroid() -> {
                showLog("Warn", "📟 Android-Modus: Kein init benötigt")
                return ""
            }
            else -> {
                showLog("Warn", "⚠️ Unbekannter WorkingMode, verwende Alpine als Fallback")
                "alpine/init.sh"
            }
        }
    }
    
    private fun getRootfsTypeName(): String {
        return when {
            Globals.isAlpine() -> "Alpine"
            Globals.isUbuntu() -> "Ubuntu"
            Globals.isAndroid() -> "Android"
            else -> "Unknown"
        }
    }
    // ============================================================
    // ★ ★ ★ UPDATE LOGIC ★ ★ ★
    // ============================================================
    fun onUpdate() {
        showLog("Info", "🔄 UpdateManager: Starte Update-Check")
        showLog("Info", "📌 Aktueller WorkingMode: ${Globals.getWorkingModeName()}")
        
        // ★ ★ ★ Für Android-Modus: Keine init-Skripte nötig! ★ ★ ★
        if (Globals.isAndroid()) {
            showLog("Info", "📟 Android-Modus: Überspringe init-Skripte")
            showLog("Info", "✅ UpdateManager: Update abgeschlossen (Android)")
            return
        }
        // ============================================================
        // INIT-HOST FILE
        // ============================================================
        val initFileAlpine: File = localBinDir().child("init-host-alpine")
        val initFileUbuntu: File = localBinDir().child("init-host-ubuntu")
        //val initHostAsset = getInitHostAssetPath()
        val initHostAlpine = "alpine/init-host.sh"
        val initHostUbuntu = "ubuntu/init-host.sh"
        
        //showLog("Debug", "📌 init-host Asset: $initHostAlpine")
        //showLog("Debug", "📌 Rootfs-Typ: ${getRootfsTypeName()}")
        
        if (initFileAlpine.exists()) {
            //showLog("Debug", "📄 Alte Alpine init-host Datei gefunden, lösche...")
            initFileAlpine.delete()
            showLog("Debug", "✅ Alpine init-host gelöscht")
        }
        
        if (initFileUbuntu.exists()) {
            //showLog("Debug", "📄 Alte Ubuntu init-host Datei gefunden, lösche...")
            initFileUbuntu.delete()
            showLog("Debug", "✅ Ubuntu init-host gelöscht")
        }
        
        //Alpine init-host
        if (initFileAlpine.exists().not()) {
            showLog("Info", "📄 Erstelle neue init-host Datei aus: $initHostAlpine")
            try {
                initFileAlpine.createFileIfNot()
                val script = application!!.assets.open(initHostAlpine).bufferedReader().use { it.readText() }
                initFileAlpine.writeText(script)
                showLog("Info", "✅ Alpine init-host erstellt (${script.length} bytes)")
                //showLog("Debug", "📄 Alpine init-host Pfad: ${initFileAlpine.absolutePath}")
            } catch (e: Exception) {
                showLog("Error", "❌ Fehler beim Erstellen von Alpine init-host: ${e.message}")
                e.printStackTrace()
            }
        }
        
        //Ubuntu init-host
        if (initFileUbuntu.exists().not()) {
            showLog("Info", "📄 Erstelle neue init-host Datei aus: $initHostUbuntu")
            try {
                initFileUbuntu.createFileIfNot()
                val script = application!!.assets.open(initHostUbuntu).bufferedReader().use { it.readText() }
                initFileUbuntu.writeText(script)
                showLog("Info", "✅ Ubuntu init-host erstellt (${script.length} bytes)")
                //showLog("Debug", "📄 Ubuntu init-host Pfad: ${initFileUbuntu.absolutePath}")
            } catch (e: Exception) {
                showLog("Error", "❌ Fehler beim Erstellen von Ubuntu init-host: ${e.message}")
                e.printStackTrace()
            }
        }
        
        // ============================================================
        // INIT FILE
        // ============================================================
        val initFileAlpine2: File = localBinDir().child("init-alpine")
        val initFileUbuntu2: File = localBinDir().child("init-ubuntu")
        
        //val initAsset = getInitAssetPath()
        val initAlpine = "alpine/init.sh"
        val initUbuntu = "ubuntu/init.sh"
        
        //showLog("Debug", "📌 init Asset: $initAsset")

        if (initFileAlpine2.exists()) {
            //showLog("Debug", "📄 Alte init Datei gefunden, loesche...")
            initFileAlpine2.delete()
            showLog("Debug", "✅ init-alpine geloescht")
        }
        
        if (initFileUbuntu2.exists()) {
            //showLog("Debug", "📄 Alte init Datei gefunden, loesche...")
            initFileUbuntu2.delete()
            showLog("Debug", "✅ init-ubuntu geloescht")
        }
        

        if (initFileAlpine2.exists().not()) {
            showLog("Info", "📄 Erstelle neue Alpine init Datei aus: $initAlpine")
            try {
                initFileAlpine2.createFileIfNot()
                val script = application!!.assets.open(initAlpine).bufferedReader().use { it.readText() }
                initFileAlpine2.writeText(script)
                showLog("Info", "✅ Alpine init erstellt (${script.length} bytes)")
                //showLog("Debug", "📄 init Pfad: ${initFileAlpine2.absolutePath}")
            } catch (e: Exception) {
                showLog("Error", "❌ Fehler beim Erstellen von Alpine init: ${e.message}")
                e.printStackTrace()
            }
        }
        
        if (initFileUbuntu2.exists().not()) {
            showLog("Info", "📄 Erstelle neue Ubuntu init Datei aus: $initUbuntu")
            try {
                initFileUbuntu2.createFileIfNot()
                val script = application!!.assets.open(initUbuntu).bufferedReader().use { it.readText() }
                initFileUbuntu2.writeText(script)
                showLog("Info", "✅ Ubuntu init erstellt (${script.length} bytes)")
                //showLog("Debug", "📄 init Pfad: ${initFileUbuntu2.absolutePath}")
            } catch (e: Exception) {
                showLog("Error", "❌ Fehler beim Erstellen von Ubuntu init: ${e.message}")
                e.printStackTrace()
            }
        }
        
        
        showLog("Info", "✅ UpdateManager: Update abgeschlossen")
    }
}