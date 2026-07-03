package com.rk.terminal.ui.screens.terminal

import android.os.Environment
import androidx.compose.runtime.mutableStateOf
import com.rk.libcommons.application
import com.rk.libcommons.child
import com.rk.terminal.App
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.meinname.ssh.Globals
import java.io.File

object Rootfs {
    
    // ============================================================
    // ★ LOKALE SHOWLOG FUNKTION
    // ============================================================
    private fun showLog(title: String, message: String) {
        MainActivity.showLog("Rootfs", "[$title] $message")
    }

    val reTerminal = application!!.filesDir
    val localDir = File(reTerminal.parentFile, "local")
    

    init {
        showLog("Info", "🔍 Initialisiere Rootfs...")
        showLog("Debug", "📁 Pfad: ${reTerminal.absolutePath}")
        showLog("Info", "📌 Rootfs-Typ (Globals): ${Globals.getRootfsDisplayName()}")
        showLog("Info", "📌 Rootfs-Datei (Globals): ${Globals.ROOTFS_FILE}")
        showLog("Info", "📌 WorkingDir (Globals): ${getWorkingDir()}")
        
        if (reTerminal.exists().not()) {
            showLog("Info", "📁 Erstelle Verzeichnis: ${reTerminal.absolutePath}")
            val created = reTerminal.mkdirs()
            if (created) {
                showLog("Info", "✅ Verzeichnis erstellt")
            } else {
                showLog("Error", "❌ Fehler beim Erstellen des Verzeichnisses")
            }
        } else {
            showLog("Debug", "⏭️ Verzeichnis existiert bereits")
        }
        
        // Initialen Status prüfen
        val downloaded = isFilesDownloaded()
        showLog("Info", "📊 Initialer Download-Status: ${if (downloaded) "✅ Geladen" else "❌ Nicht geladen"}")
        showLog("Info", "📊 Rootfs-Typ: ${if (Globals.isAlpine()) "Alpine" else if (Globals.isUbuntu()) "Ubuntu" else "Unknown"}")
        showLog("Info", "📊 WorkingDir: ${getWorkingDir()}")
    }

    var isDownloaded = mutableStateOf(isFilesDownloaded())
    
    // ============================================================
    // ★ ★ ★ WORKING DIRECTORY ★ ★ ★
    // ============================================================
    fun getWorkingDir(): String {
        return when {
            Globals.isAndroid() -> "/sdcard"
            //Globals.isUbuntu() -> "/home/test"
            else -> "/home/test"  // Alpine
        }
    }
    
    fun getWorkingDirDisplay(): String {
        return when {
            Globals.isAndroid() -> "📟 Android: /sdcard"
            Globals.isUbuntu() -> "🐧 Ubuntu: /home/test"
            else -> "🐧 Alpine: /home/test"
        }
    }
    
    // ============================================================
    // ★ ★ ★ KERN-FUNKTION: Prüft auf richtige Rootfs-Datei ★ ★ ★
    // ============================================================
    fun isFilesDownloaded(): Boolean {
        // ★ ★ ★ Für Android-Modus: Keine Rootfs-Datei nötig! ★ ★ ★
        if (Globals.isAndroid()) {
            showLog("Debug", "📌 Android-Modus: Keine Rootfs-Datei benötigt")
            val exists = reTerminal.exists() && 
                         reTerminal.child("proot").exists() && 
                         reTerminal.child("libtalloc.so.2").exists()
            
            if (exists) {
                showLog("Debug", "✅ Alle Dateien für Android-Modus vorhanden")
            } else {
                val missingFiles = mutableListOf<String>()
                if (!reTerminal.exists()) missingFiles.add("reTerminal")
                if (!reTerminal.child("proot").exists()) missingFiles.add("proot")
                if (!reTerminal.child("libtalloc.so.2").exists()) missingFiles.add("libtalloc.so.2")
                if (missingFiles.isNotEmpty()) {
                    showLog("Debug", "⚠️ Fehlende Dateien für Android: ${missingFiles.joinToString(", ")}")
                }
            }
            return exists
        }
        
        // ★ ★ ★ Für Alpine/Ubuntu: Rootfs-Datei prüfen ★ ★ ★
        val rootfsFile = getRootfsFileName()
        showLog("Debug", "📌 Prüfe auf Rootfs-Datei: $rootfsFile")
        
        val exists = reTerminal.exists() && 
                     reTerminal.child("proot").exists() && 
                     reTerminal.child("libtalloc.so.2").exists() && 
                     reTerminal.child(rootfsFile).exists()
        
        if (exists) {
            showLog("Debug", "✅ Alle Dateien für ${Globals.getRootfsDisplayName()} vorhanden")
        } else {
            // Zeige welche Dateien fehlen (für Debugging)
            val missingFiles = mutableListOf<String>()
            if (!reTerminal.exists()) missingFiles.add("reTerminal")
            if (!reTerminal.child("proot").exists()) missingFiles.add("proot")
            if (!reTerminal.child("libtalloc.so.2").exists()) missingFiles.add("libtalloc.so.2")
            if (!reTerminal.child(rootfsFile).exists()) missingFiles.add(rootfsFile)
            
            if (missingFiles.isNotEmpty()) {
                showLog("Debug", "⚠️ Fehlende Dateien: ${missingFiles.joinToString(", ")}")
            }
        }
        
        return exists
    }
    
    // ============================================================
    // ★ ★ ★ ROOTFS-DATEINAME AUS GLOBALS ★ ★ ★
    // ============================================================
    fun getRootfsFileName(): String {
        // ★ ★ ★ Für Android-Modus: Keine Rootfs-Datei ★ ★ ★
        if (Globals.isAndroid()) {
            return "android"  // Dummy-Wert
        }
        val file = Globals.ROOTFS_FILE
        val fileName = file.substringAfterLast('/')
        showLog("Debug", "📄 Rootfs-Datei: $fileName (aus Globals: $file)")
        return fileName
    }
    
    // ============================================================
    // ★ ★ ★ PRÜFT OB UBUNTU, ALPINE ODER ANDROID ★ ★ ★
    // ============================================================
    fun isUbuntu(): Boolean = Globals.isUbuntu()
    fun isAlpine(): Boolean = Globals.isAlpine()
    fun isAndroid(): Boolean = Globals.isAndroid()
    
    fun getRootfsType(): String = when {
        isUbuntu() -> "Ubuntu"
        isAndroid() -> "Android Shell"
        else -> "Alpine"
    }
    
    // ============================================================
    // ★ ★ ★ WORKING MODE NAME ★ ★ ★
    // ============================================================
    fun getWorkingModeName(): String = Globals.getWorkingModeName()
    fun getWorkingModeShort(): String = Globals.getWorkingModeShortName()
    
    // ============================================================
    // NEU: Aktualisiert den Download-Status
    // ============================================================
    fun refreshStatus() {
        val previousStatus = isDownloaded.value
        val currentStatus = isFilesDownloaded()
        
        if (previousStatus != currentStatus) {
            showLog("Info", "🔄 Status geändert: ${if (previousStatus) "✅" else "❌"} → ${if (currentStatus) "✅" else "❌"}")
            isDownloaded.value = currentStatus
        } else {
            showLog("Debug", "🔄 Status unverändert: ${if (currentStatus) "✅ Geladen" else "❌ Nicht geladen"}")
        }
    }
    
    // Optional: Status zurücksetzen (für erneuten Download)
    fun resetStatus() {
        showLog("Info", "🔄 Status zurückgesetzt")
        isDownloaded.value = false
    }
    
    // ============================================================
    // ZUSÄTZLICHE HILFSFUNKTIONEN MIT LOGGING
    // ============================================================
    
    /**
     * Prüft ob eine bestimmte Datei existiert
     */
    fun fileExists(filename: String): Boolean {
        val file = reTerminal.child(filename)
        val exists = file.exists()
        showLog("Debug", "📄 Prüfe $filename: ${if (exists) "✅" else "❌"}")
        return exists
    }
    
    /**
     * Gibt die Größe einer Datei zurück (für Debugging)
     */
    fun getFileSize(filename: String): Long {
        val file = reTerminal.child(filename)
        val size = if (file.exists()) file.length() else -1
        showLog("Debug", "📏 Größe $filename: ${if (size > 0) "${size / 1024} KB" else "Nicht vorhanden"}")
        return size
    }
    
    /**
     * Zeige alle Dateien im reTerminal-Verzeichnis an
     */
    fun listFiles(): List<String> {
        val files = reTerminal.listFiles()?.map { it.name } ?: emptyList()
        showLog("Debug", "📂 Dateien im Rootfs: ${files.joinToString(", ")}")
        return files
    }
    
    /**
     * Prüft ob das Verzeichnis existiert
     */
    fun isRootfsAvailable(): Boolean {
        val exists = reTerminal.exists()
        showLog("Debug", "📁 Rootfs verfügbar: ${if (exists) "✅" else "❌"}")
        return exists
    }
    
    /**
     * Löscht alle Dateien im reTerminal-Verzeichnis (für Reset)
     */
    fun deleteAllFiles(): Boolean {
        showLog("Info", "🗑️ Lösche alle Dateien in ${reTerminal.absolutePath}")
        
        if (!reTerminal.exists()) {
            showLog("Warn", "⚠️ Verzeichnis existiert nicht")
            return false
        }
        
        var success = true
        val files = reTerminal.listFiles() ?: emptyArray()
        
        for (file in files) {
            val deleted = file.delete()
            if (deleted) {
                showLog("Debug", "🗑️ Gelöscht: ${file.name}")
            } else {
                showLog("Error", "❌ Fehler beim Löschen: ${file.name}")
                success = false
            }
        }
        
        if (success) {
            showLog("Info", "✅ Alle Dateien gelöscht")
        }
        
        refreshStatus()
        return success
    }
    
    /**
     * Zeige detaillierte Informationen über den Rootfs-Status
     */
    fun showStatus() {
        val rootfsType = getRootfsType()
        val rootfsFile = getRootfsFileName()
        val workingDir = getWorkingDir()
        val modeName = getWorkingModeName()
        
        showLog("Info", "📊 ROOTFS-STATUS")
        showLog("Info", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        showLog("Info", "📁 Pfad: ${reTerminal.absolutePath}")
        showLog("Info", "📁 Existiert: ${if (reTerminal.exists()) "✅" else "❌"}")
        showLog("Info", "📌 Working Mode: $modeName")
        showLog("Info", "📌 Working Dir: $workingDir")
        showLog("Info", "📌 Rootfs-Typ: $rootfsType")
        showLog("Info", "📌 Rootfs-Datei: $rootfsFile")
        
        val files = when {
            isAndroid() -> listOf("proot", "libtalloc.so.2")
            else -> listOf("proot", "libtalloc.so.2", rootfsFile)
        }
        for (file in files) {
            val exists = reTerminal.child(file).exists()
            showLog("Info", "📄 $file: ${if (exists) "✅" else "❌"}")
        }
        
        val totalSize = getTotalSize()
        showLog("Info", "📏 Gesamtgröße: ${if (totalSize > 0) "${totalSize / (1024 * 1024)} MB" else "N/A"}")
        showLog("Info", "📊 Download-Status: ${if (isDownloaded.value) "✅ Fertig" else "❌ Wird benötigt"}")
        showLog("Info", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
    
    /**
     * Berechnet die Gesamtgröße aller Dateien
     */
    fun getTotalSize(): Long {
        var total = 0L
        val files = reTerminal.listFiles() ?: return 0
        
        for (file in files) {
            if (file.isFile) {
                total += file.length()
            }
        }
        return total
    }
}