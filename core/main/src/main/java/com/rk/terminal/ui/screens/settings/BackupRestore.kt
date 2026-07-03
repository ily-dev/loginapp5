// core/main/src/main/java/com/rk/terminal/ui/screens/settings/BackupRestore.kt
package com.rk.terminal.ui.screens.settings

import android.content.Context
import com.rk.terminal.ui.activities.terminal.MainActivity
import kotlinx.coroutines.*
import java.lang.reflect.Method

// ============================================================
// ★ BACKUP/RESTORE LOGIK (MIT REFLECTION)
// ============================================================

private fun showLog(title: String, message: String) {
    MainActivity.showLog("BackupRestore", "[$title] $message")
}

// ============================================================
// ★ DATA CLASS FÜR BACKUP-ERGEBNIS
// ============================================================
data class BackupResult(
    val success: Boolean,
    val message: String,
    val output: String = "",
    val exitCode: Int = -1
)

// ============================================================
// ★ ★ ★ HELPER: FARBCODES ENTFERNEN ★ ★ ★
// ============================================================
fun stripAnsiColors(text: String): String {
    // Entfernt ANSI Escape Sequenzen (Farbcodes)
    val ansiRegex = "\u001B\\[[;\\d]*m"
    return text.replace(Regex(ansiRegex), "")
}

// ============================================================
// ★ ★ ★ SSH CLIENT WRAPPER (MIT REFLECTION) ★ ★ ★
// ============================================================
class SSHClientWrapper {
    
    private var sshClient: Any? = null
    private var connectMethod: Method? = null
    private var cmdMethod: Method? = null
    private var closeMethod: Method? = null
    private var isConnectedMethod: Method? = null
    
    private var commandResultClass: Class<*>? = null
    private var getStdoutMethod: Method? = null
    private var getStderrMethod: Method? = null
    private var getExitCodeMethod: Method? = null
    private var isSuccessMethod: Method? = null
    
    private var isConnected = false
    
    init {
        try {
            val sshClientClass = Class.forName("com.meinname.ssh.SSHClient")
            val constructor = sshClientClass.getConstructor(
                String::class.java,
                Int::class.java,
                String::class.java,
                String::class.java
            )
            
            sshClient = constructor.newInstance("localhost", 2222, "test", "alpine")
            
            connectMethod = sshClientClass.getMethod("connect")
            cmdMethod = sshClientClass.getMethod("cmd", String::class.java)
            closeMethod = sshClientClass.getMethod("close")
            isConnectedMethod = sshClientClass.getMethod("isConnected")
            
            commandResultClass = Class.forName("com.meinname.ssh.SSHClient\$CommandResult")
            getStdoutMethod = commandResultClass?.getMethod("getStdout")
            getStderrMethod = commandResultClass?.getMethod("getStderr")
            getExitCodeMethod = commandResultClass?.getMethod("getExitCode")
            isSuccessMethod = commandResultClass?.getMethod("isSuccess")
            
            showLog("Reflection", "✅ SSHClient geladen: $sshClientClass")
            
        } catch (e: Exception) {
            showLog("Reflection", "❌ Fehler beim Laden: ${e.message}")
        }
    }
    
    fun connect(): Boolean {
        try {
            val result = connectMethod?.invoke(sshClient) as? Boolean
            isConnected = result ?: false
            
            if (isConnected) {
                val connected = isConnectedMethod?.invoke(sshClient) as? Boolean ?: false
                isConnected = connected
                showLog("Reflection", "✅ Verbunden: $isConnected")
            }
            
            return isConnected
        } catch (e: Exception) {
            showLog("Reflection", "❌ connect Fehler: ${e.message}")
            return false
        }
    }
    
    fun cmd(command: String): CommandResultWrapper {
        try {
            if (!isConnected) {
                showLog("Reflection", "❌ Nicht verbunden!")
                return CommandResultWrapper("", "Nicht verbunden", -1, false)
            }
            
            val result = cmdMethod?.invoke(sshClient, command)
            if (result == null) {
                return CommandResultWrapper("", "Kein Ergebnis", -1, false)
            }
            
            val stdout = getStdoutMethod?.invoke(result) as? String ?: ""
            val stderr = getStderrMethod?.invoke(result) as? String ?: ""
            val exitCode = getExitCodeMethod?.invoke(result) as? Int ?: -1
            val success = isSuccessMethod?.invoke(result) as? Boolean ?: false
            
            return CommandResultWrapper(stdout, stderr, exitCode, success)
            
        } catch (e: Exception) {
            showLog("Reflection", "❌ cmd Fehler: ${e.message}")
            return CommandResultWrapper("", e.message ?: "Fehler", -1, false)
        }
    }
    
    fun close() {
        try {
            closeMethod?.invoke(sshClient)
            isConnected = false
            showLog("Reflection", "🔌 Verbindung geschlossen")
        } catch (e: Exception) {
            showLog("Reflection", "❌ close Fehler: ${e.message}")
        }
    }
}

// ============================================================
// ★ COMMAND RESULT WRAPPER
// ============================================================
data class CommandResultWrapper(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val success: Boolean
)

// ============================================================
// ★ ★ ★ BACKUP SCRIPT MIT SSH (REFLECTION) ★ ★ ★
// ============================================================
fun runBackupScript(
    context: Context,
    scriptName: String,
    onProgress: (Int) -> Unit,
    onComplete: (BackupResult) -> Unit
) {
    val prefix = context.filesDir.parent
    val scriptPath = "$prefix/local/$scriptName"
    
    showLog("Backup", "📝 Starte Backup-Script via SSH (Reflection): $scriptName")
    
    CoroutineScope(Dispatchers.IO).launch {
        val sshClient = SSHClientWrapper()
        
        try {
            onProgress(10)
            
            if (!sshClient.connect()) {
                showLog("Backup", "❌ SSH Verbindung fehlgeschlagen!")
                withContext(Dispatchers.Main) {
                    onComplete(BackupResult(false, "❌ SSH Verbindung fehlgeschlagen!"))
                }
                return@launch
            }
            
            showLog("Backup", "✅ SSH Verbindung erfolgreich")
            onProgress(20)
            
            val chmodResult = sshClient.cmd("chmod +x $scriptPath")
            showLog("Backup", "🔧 chmod: ${chmodResult.stdout}")
            onProgress(30)
            
            showLog("Backup", "🚀 Führe Script aus: $scriptPath --full")
            
            val compare = sshClient.cmd("$scriptPath --compare")
            onProgress(40)
            
            val clean = sshClient.cmd("$scriptPath --clean")
            onProgress(60)
            
            val compare2 = sshClient.cmd("$scriptPath --compare")
            onProgress(80)
            
            val result = sshClient.cmd("$scriptPath --pack")
            onProgress(90)
            
            // ★ ★ ★ AUSGABE OHNE FARBEN PRÜFEN ★ ★ ★
            val output = result.stdout
            val error = result.stderr
            val exitCode = result.exitCode
            
            // Ausgabe loggen (original)
            output.lines().forEach { line ->
                if (line.isNotBlank()) {
                    showLog("Backup", line)
                }
            }
            
            if (error.isNotBlank()) {
                showLog("Backup", "❌ $error")
            }
            
            showLog("Backup", "📊 Exit Code: $exitCode")
            
            // ★ ★ ★ FARBCODES ENTFERNEN UND PRÜFEN ★ ★ ★
            val cleanOutput = stripAnsiColors(output)
            
            // Prüfe ob Backup erfolgreich war (ohne Farbcodes!)
            val backupSuccess = exitCode == 0 && cleanOutput.contains("[✓] Rootfs erstellt:")
            
            if (backupSuccess) {
                onProgress(100)
                showLog("Backup", "✅ Backup erfolgreich erkannt!")
            } else {
                showLog("Backup", "⚠️ Backup nicht erkannt, aber Exit Code 0")
            }
            
            withContext(Dispatchers.Main) {
                if (backupSuccess) {
                    onProgress(100)
                    onComplete(BackupResult(
                        success = true,
                        message = "✅ Backup erfolgreich!",
                        output = output,
                        exitCode = exitCode
                    ))
                } else {
                    // ★ ★ ★ FALLBACK: Wenn Exit Code 0 ist, trotzdem als Erfolg werten ★ ★ ★
                    if (exitCode == 0) {
                        onProgress(100)
                        onComplete(BackupResult(
                            success = true,
                            message = "✅ Backup erfolgreich! (Exit Code 0)",
                            output = output,
                            exitCode = exitCode
                        ))
                    } else {
                        onComplete(BackupResult(
                            success = false,
                            message = "❌ Backup fehlgeschlagen (Exit: $exitCode)",
                            output = output,
                            exitCode = exitCode
                        ))
                    }
                }
            }
            
        } catch (e: Exception) {
            showLog("Backup", "❌ Fehler: ${e.message}")
            withContext(Dispatchers.Main) {
                onComplete(BackupResult(
                    success = false,
                    message = "❌ Fehler: ${e.message}"
                ))
            }
        } finally {
            sshClient.close()
            showLog("Backup", "🔌 SSH Verbindung geschlossen")
        }
    }
}

// ============================================================
// ★ ★ ★ RESTORE SCRIPT MIT SSH (REFLECTION) ★ ★ ★
// ============================================================
fun runRestoreScript(
    context: Context,
    scriptName: String,
    onProgress: (Int) -> Unit,
    onComplete: (BackupResult) -> Unit
) {
    val prefix = context.filesDir.parent
    val scriptPath = "$prefix/local/$scriptName"
    
    showLog("Restore", "📝 Starte Restore-Script via SSH (Reflection): $scriptName")
    
    CoroutineScope(Dispatchers.IO).launch {
        val sshClient = SSHClientWrapper()
        
        try {
            onProgress(10)
            
            if (!sshClient.connect()) {
                showLog("Restore", "❌ SSH Verbindung fehlgeschlagen!")
                withContext(Dispatchers.Main) {
                    onComplete(BackupResult(false, "❌ SSH Verbindung fehlgeschlagen!"))
                }
                return@launch
            }
            
            showLog("Restore", "✅ SSH Verbindung erfolgreich")
            onProgress(20)
            
            val chmodResult = sshClient.cmd("chmod +x $scriptPath")
            showLog("Restore", "🔧 chmod: ${chmodResult.stdout}")
            onProgress(30)
            
            showLog("Restore", "🚀 Führe Script aus: $scriptPath --restore")
            val result = sshClient.cmd("$scriptPath --restore")
            onProgress(50)
            
            val output = result.stdout
            val error = result.stderr
            val exitCode = result.exitCode
            
            output.lines().forEach { line ->
                if (line.isNotBlank()) {
                    showLog("Restore", line)
                }
            }
            
            if (error.isNotBlank()) {
                showLog("Restore", "❌ $error")
            }
            
            showLog("Restore", "📊 Exit Code: $exitCode")
            
            val cleanOutput = stripAnsiColors(output)
            val restoreSuccess = exitCode == 0 && cleanOutput.contains("[✓] Restore erfolgreich!")
            
            withContext(Dispatchers.Main) {
                if (restoreSuccess) {
                    onProgress(100)
                    onComplete(BackupResult(
                        success = true,
                        message = "✅ Restore erfolgreich!",
                        output = output,
                        exitCode = exitCode
                    ))
                } else if (exitCode == 0) {
                    onProgress(100)
                    onComplete(BackupResult(
                        success = true,
                        message = "✅ Restore erfolgreich! (Exit Code 0)",
                        output = output,
                        exitCode = exitCode
                    ))
                } else {
                    onComplete(BackupResult(
                        success = false,
                        message = "❌ Restore fehlgeschlagen (Exit: $exitCode)",
                        output = output,
                        exitCode = exitCode
                    ))
                }
            }
            
        } catch (e: Exception) {
            showLog("Restore", "❌ Fehler: ${e.message}")
            withContext(Dispatchers.Main) {
                onComplete(BackupResult(
                    success = false,
                    message = "❌ Fehler: ${e.message}"
                ))
            }
        } finally {
            sshClient.close()
            showLog("Restore", "🔌 SSH Verbindung geschlossen")
        }
    }
}