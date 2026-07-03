// core/main/src/main/java/com/rk/terminal/ui/screens/terminal/DialogMode.kt
package com.rk.terminal.ui.screens.terminal

import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.rk.components.compose.preferences.base.PreferenceGroup
import com.meinname.ssh.Globals
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.screens.settings.SettingsCard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// ============================================================
// ★ DIALOG MODE - NEUE SESSION ERSTELLEN
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogMode(
    mainActivityActivity: MainActivity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog && Rootfs.isDownloaded.value) {
        BasicAlertDialog(
            onDismissRequest = {
                Globals.showLog("DialogMode", "➕ Dialog geschlossen")
                showDialog = false
                onDismiss()
            }
        ) {
            fun createSession(workingMode: Int) {
                fun generateUniqueString(existingStrings: List<String>): String {
                    var index = 1
                    var newString: String
                    do {
                        newString = "main$index"
                        index++
                    } while (newString in existingStrings)
                    return newString
                }

                val sessionId = generateUniqueString(
                    mainActivityActivity.sessionBinder!!.getService().sessionList.keys.toList()
                )

                val modeName = when (workingMode) {
                    Globals.WORKING_MODE_ALPINE -> "Alpine"
                    Globals.WORKING_MODE_UBUNTU -> "Ubuntu"
                    Globals.WORKING_MODE_ANDROID -> "Android"
                    else -> "Unknown"
                }

                Globals.showLog("DialogMode", "📝 Erstelle neue $modeName Session: $sessionId")

                terminalView.get()
                    ?.let {
                        val client = TerminalBackEnd(it, mainActivityActivity)
                        mainActivityActivity.sessionBinder!!.createSession(
                            sessionId,
                            client,
                            mainActivityActivity,
                            workingMode = workingMode
                        )
                        Globals.showLog("DialogMode", "workingMode: $workingMode")
                    }
                changeSession(mainActivityActivity, session_id = sessionId)
                Globals.showLog("DialogMode", "✅ Session $sessionId erstellt ($modeName)")
                showDialog = false
                onDismiss()
            }

            PreferenceGroup {
                // ★ ★ ★ 1. Alpine Option ★ ★ ★
                SettingsCard(
                    title = { Text("🐧 Alpine Linux") },
                    description = { Text("Leichtes Linux mit apk Paketmanager") },
                    onClick = {
                        Globals.showLog("DialogMode", "🐧 Alpine Session ausgewählt")
                        Globals.setAlpine()
                        createSession(workingMode = Globals.WORKING_MODE_ALPINE)
                    }
                )

                // ★ ★ ★ 2. Ubuntu Option ★ ★ ★
                SettingsCard(
                    title = { Text("🐧 Ubuntu Linux") },
                    description = { Text("Vollständiges Linux mit apt Paketmanager") },
                    onClick = {
                        Globals.showLog("DialogMode", "🐧 Ubuntu Session ausgewählt")
                        Globals.setUbuntu()
                        createSession(workingMode = Globals.WORKING_MODE_UBUNTU)
                    }
                )

                // ★ ★ ★ 3. Android Option ★ ★ ★
                SettingsCard(
                    title = { Text("📟 Android Shell") },
                    description = { Text("Native Android Shell (ohne Rootfs)") },
                    onClick = {
                        Globals.showLog("DialogMode", "📟 Android Session ausgewählt")
                        Globals.WORKING_MODE = Globals.WORKING_MODE_ANDROID
                        Globals.ROOTFS_TYPE = "android"
                        createSession(workingMode = Globals.WORKING_MODE_ANDROID)
                    }
                )
            }
        }
    }
}