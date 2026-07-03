package com.rk.terminal.ui.screens.settings

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// ★ ★ ★ IMPORTS FÜR BACKUP ★ ★ ★
import com.rk.terminal.ui.screens.settings.runBackupScript
import com.rk.terminal.ui.screens.settings.runRestoreScript
import com.rk.terminal.ui.screens.settings.BackupResult

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
// ★ PROGRESS INDICATOR
// ============================================================
@Composable
fun ProgressIndicatorWithPercent(progress: Int) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            )
        )
    )
    
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(50.dp)
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotationAngle)
        ) {
            val strokeWidth = 4.dp.toPx()
            
            drawArc(
                color = surfaceVariantColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
            
            val sweepAngle = (progress / 100f) * 360f
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }
        
        Text(
            text = if (progress == 100) "✓" else "$progress%",
            fontSize = if (progress == 100) 18.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )
    }
}

// ============================================================
// ★ BACKUP CARD
// ============================================================
@Composable
fun BackupCard(
    title: String,
    description: String,
    isRunning: Boolean,
    progress: Int,
    onClick: () -> Unit
) {
    SettingsCard(
        title = { Text(title) },
        description = { Text(description) },
        isEnabled = !isRunning,
        endWidget = {
            if (isRunning) {
                ProgressIndicatorWithPercent(progress)
            }
        },
        onClick = onClick
    )
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
    
    var toastMessage by remember { mutableStateOf<String?>(null) }
    
    // Alpine Backup States
    var isAlpineBackupRunning by remember { mutableStateOf(false) }
    var alpineBackupProgress by remember { mutableIntStateOf(0) }
    
    // Ubuntu Backup States
    var isUbuntuBackupRunning by remember { mutableStateOf(false) }
    var ubuntuBackupProgress by remember { mutableIntStateOf(0) }
    
    // Alpine Restore States
    var isAlpineRestoreRunning by remember { mutableStateOf(false) }
    var alpineRestoreProgress by remember { mutableIntStateOf(0) }
    
    // Ubuntu Restore States
    var isUbuntuRestoreRunning by remember { mutableStateOf(false) }
    var ubuntuRestoreProgress by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            toastMessage = null
        }
    }

    showLog("Info", "⚙️ Settings Screen geöffnet")
    showLog("Debug", "⚙️ InputMode: $selectedInputMode")

    PreferenceLayout(label = stringResource(strings.settings)) {
        
        // ============================================================
        // ★ ★ ★ BACKUP LINUX DISTRIBUTION ★ ★ ★
        // ============================================================
        PreferenceGroup(heading = "Backup Linux distribution") {
            
            // ★ Alpine Backup
BackupCard(
    title = "💾 Alpine Backup erstellen",
    description = "Erstellt ein Backup von Alpine Linux (rem_alpine.sh)",
    isRunning = isAlpineBackupRunning,
    progress = alpineBackupProgress,
    onClick = {
        if (!isAlpineBackupRunning) {
            isAlpineBackupRunning = true
            alpineBackupProgress = 0
            toastMessage = "📦 Alpine Backup wird erstellt... (bitte warten)"
            showLog("Backup", "📦 Alpine Backup starten...")
            
            runBackupScript(
                context = context,
                scriptName = "rem_alpine.sh",
                onProgress = { progress ->
                    alpineBackupProgress = progress
                },
                onComplete = { result ->
                    isAlpineBackupRunning = false
                    toastMessage = if (result.success) {
                        "✅ Alpine Backup erfolgreich!"
                    } else {
                        "❌ Alpine Backup fehlgeschlagen: ${result.message}"
                    }
                    showLog("Backup", if (result.success) "✅ Backup erfolgreich" else "❌ Backup fehlgeschlagen")
                }
            )
        }
    }
)

// ★ Ubuntu Backup
BackupCard(
    title = "💾 Ubuntu Backup erstellen",
    description = "Erstellt ein Backup von Ubuntu Linux (rem_ubuntu.sh)",
    isRunning = isUbuntuBackupRunning,
    progress = ubuntuBackupProgress,
    onClick = {
        if (!isUbuntuBackupRunning) {
            isUbuntuBackupRunning = true
            ubuntuBackupProgress = 0
            toastMessage = "📦 Ubuntu Backup wird erstellt... (bitte warten)"
            showLog("Backup", "📦 Ubuntu Backup starten...")
            
            runBackupScript(
                context = context,
                scriptName = "rem_ubuntu.sh",
                onProgress = { progress ->
                    ubuntuBackupProgress = progress
                },
                onComplete = { result ->
                    isUbuntuBackupRunning = false
                    toastMessage = if (result.success) {
                        "✅ Ubuntu Backup erfolgreich!"
                    } else {
                        "❌ Ubuntu Backup fehlgeschlagen: ${result.message}"
                    }
                    showLog("Backup", if (result.success) "✅ Backup erfolgreich" else "❌ Backup fehlgeschlagen")
                }
            )
        }
    }
)
            
            // ★ Alpine Restore
            BackupCard(
                title = "🔄 Alpine Restore",
                description = "Stellt Alpine Linux aus Backup wieder her",
                isRunning = isAlpineRestoreRunning,
                progress = alpineRestoreProgress,
                onClick = {
                    if (!isAlpineRestoreRunning) {
                        isAlpineRestoreRunning = true
                        alpineRestoreProgress = 0
                        toastMessage = "🔄 Alpine Restore wird durchgeführt... (bitte warten)"
                        showLog("Restore", "🔄 Alpine Restore starten...")
                        
                        runRestoreScript(
                            context = context,
                            scriptName = "rem_alpine.sh",
                            onProgress = { progress ->
                                alpineRestoreProgress = progress
                            },
                            onComplete = { result ->
                                isAlpineRestoreRunning = false
                                toastMessage = if (result.success) {
                                    "✅ Alpine Restore erfolgreich!"
                                } else {
                                    "❌ Alpine Restore fehlgeschlagen: ${result.message}"
                                }
                                showLog("Restore", if (result.success) "✅ Restore erfolgreich" else "❌ Restore fehlgeschlagen")
                            }
                        )
                    }
                }
            )
            
            // ★ Ubuntu Restore
            BackupCard(
                title = "🔄 Ubuntu Restore",
                description = "Stellt Ubuntu Linux aus Backup wieder her",
                isRunning = isUbuntuRestoreRunning,
                progress = ubuntuRestoreProgress,
                onClick = {
                    if (!isUbuntuRestoreRunning) {
                        isUbuntuRestoreRunning = true
                        ubuntuRestoreProgress = 0
                        toastMessage = "🔄 Ubuntu Restore wird durchgeführt... (bitte warten)"
                        showLog("Restore", "🔄 Ubuntu Restore starten...")
                        
                        runRestoreScript(
                            context = context,
                            scriptName = "rem_ubuntu.sh",
                            onProgress = { progress ->
                                ubuntuRestoreProgress = progress
                            },
                            onComplete = { result ->
                                isUbuntuRestoreRunning = false
                                toastMessage = if (result.success) {
                                    "✅ Ubuntu Restore erfolgreich!"
                                } else {
                                    "❌ Ubuntu Restore fehlgeschlagen: ${result.message}"
                                }
                                showLog("Restore", if (result.success) "✅ Restore erfolgreich" else "❌ Restore fehlgeschlagen")
                            }
                        )
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