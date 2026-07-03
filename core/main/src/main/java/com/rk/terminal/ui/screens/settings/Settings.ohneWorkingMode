package com.rk.terminal.ui.screens.settings

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.rk.components.compose.preferences.base.PreferenceGroup
import com.rk.components.compose.preferences.base.PreferenceLayout
import com.rk.components.compose.preferences.base.PreferenceTemplate
import com.rk.resources.strings
import com.rk.settings.Settings
import com.rk.terminal.R
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.components.SettingsToggle
import com.rk.terminal.ui.routes.MainActivityRoutes
import com.rk.terminal.Globals
import kotlinx.coroutines.*

// ============================================================
// ★ LOKALE SHOWLOG FUNKTION
// ============================================================
private fun showLog(title: String, message: String) {
    MainActivity.showLog("Settings", "[$title] $message")
}

// ============================================================
// ★ REFLECTION FUNKTION (startPythonNative)
// ============================================================
fun startPythonViaReflection(func: String, activity: Activity) {
    try {
        val clazz = Class.forName("org.kivy.android.PythonActivity")
        val method = clazz.getMethod(func, Activity::class.java)
        method.invoke(null, activity)
        MainActivity.showLog("Settings", "✅ Python gestartet (Reflection)")
    } catch (e: ClassNotFoundException) {
        MainActivity.showLog("Settings", "❌ PythonActivity nicht gefunden: ${e.message}")
    } catch (e: Exception) {
        MainActivity.showLog("Settings", "❌ Fehler: ${e.message}")
        e.printStackTrace()
    }
}

// ============================================================
// ★ TERMINAL IN HINTERGRUND
// ============================================================
fun moveTerminalToBack(activity: Activity) {
    try {
        activity.moveTaskToBack(true)
        showLog("Settings", "✅ TerminalActivity in den Hintergrund verschoben")
    } catch (e: Exception) {
        showLog("Settings", "❌ Fehler: ${e.message}")
    }
}

fun moveTerminalToBackContext(context: Context) {
    try {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        context.startActivity(intent)
        
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val tasks = am.appTasks
        for (task in tasks) {
            if (task.taskInfo.baseActivity?.className?.contains("MainActivity") == true) {
                task.moveToFront()
                break
            }
        }
        showLog("Settings", "✅ TerminalActivity in den Hintergrund verschoben")
    } catch (e: Exception) {
        showLog("Settings", "❌ Fehler: ${e.message}")
    }
}

// ============================================================
// ★ BACKUP FUNKTIONEN
// ============================================================
private fun runBackupScript(context: Context, scriptName: String, onComplete: (Boolean, String) -> Unit) {
    val scriptPath = "${context.filesDir.parent}/local/$scriptName"
    
    showLog("Backup", "📝 Starte Backup-Script: $scriptName")
    
    // Coroutine für Hintergrundausführung
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 1. chmod +x
            val chmod = Runtime.getRuntime().exec(arrayOf("chmod", "+x", scriptPath))
            chmod.waitFor()
            showLog("Backup", "✅ chmod +x erfolgreich")
            
            // 2. Script ausführen
            val process = Runtime.getRuntime().exec(arrayOf(scriptPath, "--full"))
            
            // Output lesen
            val reader = process.inputStream.bufferedReader()
            val output = StringBuilder()
            reader.useLines { lines ->
                lines.forEach { 
                    output.appendLine(it)
                    showLog("Backup", it)
                }
            }
            
            val errorReader = process.errorStream.bufferedReader()
            val errorOutput = StringBuilder()
            errorReader.useLines { lines ->
                lines.forEach { 
                    errorOutput.appendLine(it)
                    showLog("Backup", "❌ $it")
                }
            }
            
            val exitCode = process.waitFor()
            showLog("Backup", "📊 Exit Code: $exitCode")
            
            if (exitCode == 0) {
                withContext(Dispatchers.Main) {
                    onComplete(true, "✅ Backup erfolgreich erstellt!\n$output")
                }
            } else {
                withContext(Dispatchers.Main) {
                    onComplete(false, "❌ Backup fehlgeschlagen (Exit: $exitCode)\n$errorOutput")
                }
            }
            
        } catch (e: Exception) {
            showLog("Backup", "❌ Fehler: ${e.message}")
            withContext(Dispatchers.Main) {
                onComplete(false, "❌ Fehler: ${e.message}")
            }
        }
    }
}

private fun runRestoreScript(context: Context, scriptName: String, onComplete: (Boolean, String) -> Unit) {
    val scriptPath = "${context.filesDir.parent}/local/$scriptName"
    
    showLog("Restore", "📝 Starte Restore-Script: $scriptName")
    
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 1. chmod +x
            val chmod = Runtime.getRuntime().exec(arrayOf("chmod", "+x", scriptPath))
            chmod.waitFor()
            showLog("Restore", "✅ chmod +x erfolgreich")
            
            // 2. Script ausführen (mit --restore)
            val process = Runtime.getRuntime().exec(arrayOf(scriptPath, "--restore"))
            
            // Output lesen
            val reader = process.inputStream.bufferedReader()
            val output = StringBuilder()
            reader.useLines { lines ->
                lines.forEach { 
                    output.appendLine(it)
                    showLog("Restore", it)
                }
            }
            
            val errorReader = process.errorStream.bufferedReader()
            val errorOutput = StringBuilder()
            errorReader.useLines { lines ->
                lines.forEach { 
                    errorOutput.appendLine(it)
                    showLog("Restore", "❌ $it")
                }
            }
            
            val exitCode = process.waitFor()
            showLog("Restore", "📊 Exit Code: $exitCode")
            
            if (exitCode == 0) {
                withContext(Dispatchers.Main) {
                    onComplete(true, "✅ Restore erfolgreich!\n$output")
                }
            } else {
                withContext(Dispatchers.Main) {
                    onComplete(false, "❌ Restore fehlgeschlagen (Exit: $exitCode)\n$errorOutput")
                }
            }
            
        } catch (e: Exception) {
            showLog("Restore", "❌ Fehler: ${e.message}")
            withContext(Dispatchers.Main) {
                onComplete(false, "❌ Fehler: ${e.message}")
            }
        }
    }
}

// ============================================================
// ★ SETTINGS CARD
// ============================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    title: @Composable () -> Unit,
    description: @Composable () -> Unit = {},
    startWidget: (@Composable () -> Unit)? = null,
    endWidget: (@Composable () -> Unit)? = null,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    PreferenceTemplate(
        modifier = modifier
            .combinedClickable(
                enabled = isEnabled,
                indication = ripple(),
                interactionSource = interactionSource,
                onClick = onClick
            ),
        contentModifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 16.dp)
            .padding(start = 16.dp),
        title = title,
        description = description,
        startWidget = startWidget,
        endWidget = endWidget,
        applyPaddings = false
    )
}

// ============================================================
// ★ INPUT MODE
// ============================================================
object InputMode {
    const val DEFAULT = 0
    const val TYPE_NULL = 1
    const val VISIBLE_PASSWORD = 2
}

// ============================================================
// ★ SETTINGS COMPOSABLE
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(modifier: Modifier = Modifier, navController: NavController, mainActivity: MainActivity) {
    val context = LocalContext.current
    var selectedInputMode by remember { mutableIntStateOf(Settings.input_mode) }
    var selectedWorkingMode by remember { mutableIntStateOf(Globals.WORKING_MODE) }
    
    // States für Backup/Toast
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var isBackupRunning by remember { mutableStateOf(false) }
    var isRestoreRunning by remember { mutableStateOf(false) }
    
    // Toast anzeigen
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            toastMessage = null
        }
    }

    showLog("Info", "⚙️ Settings Screen geöffnet")
    showLog("Debug", "⚙️ WorkingMode: ${Globals.getWorkingModeName()} (${Globals.WORKING_MODE})")
    showLog("Debug", "⚙️ InputMode: $selectedInputMode")

    PreferenceLayout(label = stringResource(strings.settings)) {
        
        // ============================================================
        // ★ ★ ★ WORKING MODE (mit Globals) ★ ★ ★
        // ============================================================
        PreferenceGroup(heading = "Working Mode") {
            
            // ★ Alpine
            SettingsCard(
                title = { Text("🐧 Alpine Linux") },
                description = { Text("Leichtes Linux mit apk Paketmanager") },
                startWidget = {
                    RadioButton(
                        modifier = Modifier.padding(start = 8.dp),
                        selected = selectedWorkingMode == Globals.WORKING_MODE_ALPINE,
                        onClick = {
                            selectedWorkingMode = Globals.WORKING_MODE_ALPINE
                            Globals.WORKING_MODE = selectedWorkingMode
                            Settings.working_Mode = selectedWorkingMode
                            showLog("Info", "🐧 WorkingMode geändert zu: ALPINE")
                        })
                },
                onClick = {
                    selectedWorkingMode = Globals.WORKING_MODE_ALPINE
                    Globals.WORKING_MODE = selectedWorkingMode
                    Settings.working_Mode = selectedWorkingMode
                    showLog("Info", "🐧 WorkingMode geändert zu: ALPINE (via Click)")
                })
            
            // ★ Ubuntu
            SettingsCard(
                title = { Text("🐧 Ubuntu Linux") },
                description = { Text("Vollständiges Linux mit apt Paketmanager") },
                startWidget = {
                    RadioButton(
                        modifier = Modifier.padding(start = 8.dp),
                        selected = selectedWorkingMode == Globals.WORKING_MODE_UBUNTU,
                        onClick = {
                            selectedWorkingMode = Globals.WORKING_MODE_UBUNTU
                            Globals.WORKING_MODE = selectedWorkingMode
                            Settings.working_Mode = selectedWorkingMode
                            showLog("Info", "🐧 WorkingMode geändert zu: UBUNTU")
                        })
                },
                onClick = {
                    selectedWorkingMode = Globals.WORKING_MODE_UBUNTU
                    Globals.WORKING_MODE = selectedWorkingMode
                    Settings.working_Mode = selectedWorkingMode
                    showLog("Info", "🐧 WorkingMode geändert zu: UBUNTU (via Click)")
                })
            
            // ★ Android
            SettingsCard(
                title = { Text("📟 Android Shell") },
                description = { Text("Native Android Shell (ohne Rootfs)") },
                startWidget = {
                    RadioButton(
                        modifier = Modifier.padding(start = 8.dp),
                        selected = selectedWorkingMode == Globals.WORKING_MODE_ANDROID,
                        onClick = {
                            selectedWorkingMode = Globals.WORKING_MODE_ANDROID
                            Globals.WORKING_MODE = selectedWorkingMode
                            Settings.working_Mode = selectedWorkingMode
                            showLog("Info", "📟 WorkingMode geändert zu: ANDROID")
                        })
                },
                onClick = {
                    selectedWorkingMode = Globals.WORKING_MODE_ANDROID
                    Globals.WORKING_MODE = selectedWorkingMode
                    Settings.working_Mode = selectedWorkingMode
                    showLog("Info", "📟 WorkingMode geändert zu: ANDROID (via Click)")
                })
        }

        // ============================================================
        // ★ ★ ★ BACKUP LINUX DISTRIBUTION ★ ★ ★
        // ============================================================
        PreferenceGroup(heading = "Backup Linux distribution") {
            
            // ★ Alpine Backup
            SettingsCard(
                title = { Text("💾 Alpine Backup erstellen") },
                description = { Text("Erstellt ein Backup von Alpine Linux (rem_alpine.sh)") },
                isEnabled = !isBackupRunning,
                onClick = {
                    if (!isBackupRunning) {
                        isBackupRunning = true
                        toastMessage = "📦 Alpine Backup wird erstellt... (bitte warten)"
                        showLog("Backup", "📦 Alpine Backup starten...")
                        
                        runBackupScript(context, "rem_alpine.sh") { success, message ->
                            isBackupRunning = false
                            toastMessage = if (success) {
                                "✅ Alpine Backup erfolgreich! /sdcard/alpine_custom_*.tar.gz"
                            } else {
                                "❌ Alpine Backup fehlgeschlagen: $message"
                            }
                            showLog("Backup", if (success) "✅ Backup erfolgreich" else "❌ Backup fehlgeschlagen")
                        }
                    }
                }
            )
            
            // ★ Ubuntu Backup
            SettingsCard(
                title = { Text("💾 Ubuntu Backup erstellen") },
                description = { Text("Erstellt ein Backup von Ubuntu Linux (rem_ubuntu.sh)") },
                isEnabled = !isBackupRunning,
                onClick = {
                    if (!isBackupRunning) {
                        isBackupRunning = true
                        toastMessage = "📦 Ubuntu Backup wird erstellt... (bitte warten)"
                        showLog("Backup", "📦 Ubuntu Backup starten...")
                        
                        runBackupScript(context, "rem_ubuntu.sh") { success, message ->
                            isBackupRunning = false
                            toastMessage = if (success) {
                                "✅ Ubuntu Backup erfolgreich! /sdcard/ubuntu_custom_*.tar.gz"
                            } else {
                                "❌ Ubuntu Backup fehlgeschlagen: $message"
                            }
                            showLog("Backup", if (success) "✅ Backup erfolgreich" else "❌ Backup fehlgeschlagen")
                        }
                    }
                }
            )
            
            // ★ Alpine Restore
            SettingsCard(
                title = { Text("🔄 Alpine Restore") },
                description = { Text("Stellt Alpine Linux aus Backup wieder her") },
                isEnabled = !isRestoreRunning,
                onClick = {
                    if (!isRestoreRunning) {
                        isRestoreRunning = true
                        toastMessage = "🔄 Alpine Restore wird durchgeführt... (bitte warten)"
                        showLog("Restore", "🔄 Alpine Restore starten...")
                        
                        runRestoreScript(context, "rem_alpine.sh") { success, message ->
                            isRestoreRunning = false
                            toastMessage = if (success) {
                                "✅ Alpine Restore erfolgreich!"
                            } else {
                                "❌ Alpine Restore fehlgeschlagen: $message"
                            }
                            showLog("Restore", if (success) "✅ Restore erfolgreich" else "❌ Restore fehlgeschlagen")
                        }
                    }
                }
            )
            
            // ★ Ubuntu Restore
            SettingsCard(
                title = { Text("🔄 Ubuntu Restore") },
                description = { Text("Stellt Ubuntu Linux aus Backup wieder her") },
                isEnabled = !isRestoreRunning,
                onClick = {
                    if (!isRestoreRunning) {
                        isRestoreRunning = true
                        toastMessage = "🔄 Ubuntu Restore wird durchgeführt... (bitte warten)"
                        showLog("Restore", "🔄 Ubuntu Restore starten...")
                        
                        runRestoreScript(context, "rem_ubuntu.sh") { success, message ->
                            isRestoreRunning = false
                            toastMessage = if (success) {
                                "✅ Ubuntu Restore erfolgreich!"
                            } else {
                                "❌ Ubuntu Restore fehlgeschlagen: $message"
                            }
                            showLog("Restore", if (success) "✅ Restore erfolgreich" else "❌ Restore fehlgeschlagen")
                        }
                    }
                }
            )
        }

        // ============================================================
        // INPUT MODE
        // ============================================================
        PreferenceGroup(heading = stringResource(strings.input_mode)) {
            SettingsCard(
                title = { Text(stringResource(strings.input_mode_default)) },
                description = { Text(stringResource(strings.input_mode_default_desc)) },
                startWidget = {
                    RadioButton(
                        modifier = Modifier.padding(start = 8.dp),
                        selected = selectedInputMode == InputMode.DEFAULT,
                        onClick = {
                            selectedInputMode = InputMode.DEFAULT
                            Settings.input_mode = selectedInputMode
                            showLog("Info", "⌨️ InputMode geändert zu: DEFAULT")
                        })
                },
                onClick = {
                    selectedInputMode = InputMode.DEFAULT
                    Settings.input_mode = selectedInputMode
                    showLog("Info", "⌨️ InputMode geändert zu: DEFAULT (via Click)")
                })

            SettingsCard(
                title = { Text(stringResource(strings.input_mode_type_null)) },
                description = { Text(stringResource(strings.input_mode_type_null_desc)) },
                startWidget = {
                    RadioButton(
                        modifier = Modifier.padding(start = 8.dp),
                        selected = selectedInputMode == InputMode.TYPE_NULL,
                        onClick = {
                            selectedInputMode = InputMode.TYPE_NULL
                            Settings.input_mode = selectedInputMode
                            showLog("Info", "⌨️ InputMode geändert zu: TYPE_NULL")
                        })
                },
                onClick = {
                    selectedInputMode = InputMode.TYPE_NULL
                    Settings.input_mode = selectedInputMode
                    showLog("Info", "⌨️ InputMode geändert zu: TYPE_NULL (via Click)")
                })

            SettingsCard(
                title = { Text(stringResource(strings.input_mode_visible_password)) },
                description = { Text(stringResource(strings.input_mode_visible_password_desc)) },
                startWidget = {
                    RadioButton(
                        modifier = Modifier.padding(start = 8.dp),
                        selected = selectedInputMode == InputMode.VISIBLE_PASSWORD,
                        onClick = {
                            selectedInputMode = InputMode.VISIBLE_PASSWORD
                            Settings.input_mode = selectedInputMode
                            showLog("Info", "⌨️ InputMode geändert zu: VISIBLE_PASSWORD")
                        })
                },
                onClick = {
                    selectedInputMode = InputMode.VISIBLE_PASSWORD
                    Settings.input_mode = selectedInputMode
                    showLog("Info", "⌨️ InputMode geändert zu: VISIBLE_PASSWORD (via Click)")
                })
        }

        // ============================================================
        // CUSTOMIZATIONS
        // ============================================================
        PreferenceGroup {
            SettingsToggle(
                label = stringResource(strings.customizations),
                showSwitch = false,
                default = false,
                sideEffect = {
                    showLog("Info", "🎨 Navigation zu Customizations")
                    navController.navigate(MainActivityRoutes.Customization.route)
                },
                endWidget = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp)
                    )
                })
        }

        // ============================================================
        // SECCOMP & FILE ACCESS
        // ============================================================
        PreferenceGroup {
            SettingsToggle(
                label = stringResource(strings.seccomp),
                description = stringResource(strings.seccomp_desc),
                showSwitch = true,
                default = Settings.seccomp,
                sideEffect = {
                    Settings.seccomp = it
                    showLog("Info", "🔒 Seccomp: ${if (it) "aktiviert" else "deaktiviert"}")
                })

            SettingsToggle(
                label = stringResource(strings.all_file_access),
                description = stringResource(strings.all_file_access_desc),
                showSwitch = false,
                default = false,
                sideEffect = {
                    showLog("Info", "📂 All File Access angefordert")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        runCatching {
                            val intent = Intent(
                                android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                "package:${context.packageName}".toUri()
                            )
                            context.startActivity(intent)
                            showLog("Debug", "📂 File Access Intent gestartet (Android 11+)")
                        }.onFailure {
                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            context.startActivity(intent)
                            showLog("Debug", "📂 File Access Intent gestartet (Fallback)")
                        }
                    } else {
                        val intent = Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${context.packageName}".toUri()
                        )
                        context.startActivity(intent)
                        showLog("Debug", "📂 App-Details geöffnet (Android < 11)")
                    }
                })
        }

        // ============================================================
        // PYTHON ENVIRONMENT
        // ============================================================
        PreferenceGroup(heading = "Python Environment") {
            
            // ★ Button 1: Python App starten (Intent)
            SettingsCard(
                title = { Text("🐍 Python App starten") },
                description = { Text("Startet main.pyc (Kivy-App mit GUI)") },
                onClick = {
                    showLog("Info", "🐍 Python App starten...")
                    try {
                        val intent = Intent(context, Class.forName("org.kivy.android.PythonActivity"))
                        context.startActivity(intent)
                        showLog("Info", "✅ Python App gestartet")
                        Toast.makeText(context, "Python App wird gestartet...", Toast.LENGTH_SHORT).show()
                    } catch (e: ClassNotFoundException) {
                        showLog("Error", "❌ PythonActivity nicht gefunden: ${e.message}")
                        Toast.makeText(context, "Python-Umgebung nicht gefunden!", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        showLog("Error", "❌ Fehler beim Starten: ${e.message}")
                        Toast.makeText(context, "Fehler: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
            
            // ★ Button 2: Python Reflection starten
            SettingsCard(
                title = { Text("🎇 Python Reflection starten") },
                description = { Text("Startet Python über Reflection") },
                onClick = {
                    showLog("Info", "🎇 Python Reflection starten...")
                    try {
                        val clazz = Class.forName("org.kivy.android.PythonActivity")
                        val method = clazz.getMethod("startPython")
                        method.invoke(null)
                        showLog("Info", "✅ Python Reflection gestartet")
                        Toast.makeText(context, "Python wird gestartet...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        showLog("Error", "❌ Fehler: ${e.message}")
                        Toast.makeText(context, "Fehler: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
            
            // ★ Button 3: Python Service (Reflection)
            SettingsCard(
                title = { Text("⚙️ Python Service starten") },
                description = { Text("Startet service/main.pyc (Hintergrunddienst)") },
                onClick = {
                    showLog("Info", "⚙️ Python Service starten...")
                    Thread {
                        try {
                            val serviceClass = Class.forName("com.meinname.loginapp8.ServiceMyservice")
                            val method = serviceClass.getMethod("start", Context::class.java, String::class.java)
                            method.invoke(null, context, "my_arg")
                            showLog("Info", "✅ Python Service gestartet")
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                Toast.makeText(context, "Python Service gestartet", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: ClassNotFoundException) {
                            showLog("Error", "❌ Service-Klasse nicht gefunden: ${e.message}")
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                Toast.makeText(context, "Service-Klasse nicht gefunden", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            showLog("Error", "❌ Service Fehler: ${e.message}")
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                Toast.makeText(context, "Fehler: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }.start()
                }
            )
            
            // ★ Button 4: Python Entrypoint
            SettingsCard(
                title = { Text("⚙️ Python Entrypoint starten") },
                description = { Text("Startet service/main.pyc (ANDROID_ENTRYPOINT)") },
                onClick = {
                    showLog("Info", "⚙️ Python Entrypoint starten...")
                    try {
                        val intent = Intent(context, Class.forName("org.kivy.android.PythonActivity"))
                        intent.putExtra("ANDROID_ENTRYPOINT", "service/main.pyc")
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        showLog("Info", "✅ Python Entrypoint gestartet")
                        Toast.makeText(context, "Python Service wird gestartet...", Toast.LENGTH_SHORT).show()
                    } catch (e: ClassNotFoundException) {
                        showLog("Error", "❌ PythonActivity nicht gefunden: ${e.message}")
                        Toast.makeText(context, "PythonActivity nicht gefunden!", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        showLog("Error", "❌ Service Fehler: ${e.message}")
                        Toast.makeText(context, "Service Fehler: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
            
            // ★ Button 5: Python startPythonNative()
            SettingsCard(
                title = { Text("💯 Python native starten (startPythonNative)") },
                description = { Text("Startet Python über startPythonNative() Methode") },
                onClick = {
                    showLog("Info", "💯 Python native starten...")
                    try {
                        startPythonViaReflection("startPythonNative", mainActivity)
                        showLog("Info", "✅ Python native gestartet")
                        Toast.makeText(context, "Python native gestartet...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        showLog("Error", "❌ Fehler: ${e.message}")
                        Toast.makeText(context, "Fehler: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
            
            // ★ Button 6: Python in Vordergrund holen
            SettingsCard(
                title = { Text("🔄 Python in Vordergrund holen") },
                description = { Text("Bringt PythonActivity in den Vordergrund (löst onResume aus)") },
                onClick = {
                    showLog("Info", "🔄 Python in Vordergrund holen...")
                    try {
                        val pythonActivityClass = Class.forName("org.kivy.android.PythonActivity")
                        val intent = Intent(context, pythonActivityClass)
                        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        context.startActivity(intent)
                        showLog("Info", "✅ Python im Vordergrund")
                        Toast.makeText(context, "Python im Vordergrund!", Toast.LENGTH_SHORT).show()
                    } catch (e: ClassNotFoundException) {
                        showLog("Error", "❌ PythonActivity nicht gefunden: ${e.message}")
                        Toast.makeText(context, "PythonActivity nicht gefunden!", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        showLog("Error", "❌ Fehler: ${e.message}")
                        Toast.makeText(context, "Fehler: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
            
            // ★ Button 7: Terminal in Hintergrund
            SettingsCard(
                title = { Text("📱 Terminal in Hintergrund") },
                description = { Text("Verschiebt TerminalActivity in den Hintergrund") },
                onClick = {
                    showLog("Info", "📱 Terminal in Hintergrund...")
                    moveTerminalToBack(mainActivity)
                    Toast.makeText(context, "Terminal im Hintergrund!", Toast.LENGTH_SHORT).show()
                }
            )
            
            // ★ Button 8: TerminalActivity beenden
            SettingsCard(
                title = { Text("❌ TerminalActivity beenden") },
                description = { Text("Beendet die TerminalActivity komplett") },
                onClick = {
                    showLog("Info", "❌ TerminalActivity beenden...")
                    try {
                        mainActivity.finishAffinity()
                        showLog("Info", "✅ TerminalActivity beendet")
                        Toast.makeText(context, "TerminalActivity beendet!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        showLog("Error", "❌ Fehler: ${e.message}")
                        Toast.makeText(context, "Fehler: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }
}