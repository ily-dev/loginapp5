package com.rk.terminal.ui.screens.terminal

import android.os.Environment
//import com.rk.libcommons.alpineDir
import com.rk.libcommons.alpineHomeDir
import com.rk.libcommons.ubuntuHomeDir
import com.rk.libcommons.application
import com.rk.libcommons.child
import com.rk.libcommons.createFileIfNot
import com.rk.libcommons.localBinDir
import com.rk.libcommons.localDir
import com.rk.libcommons.localLibDir
import com.rk.libcommons.pendingCommand
import com.rk.settings.Settings
import com.rk.terminal.App
import com.rk.terminal.App.Companion.getTempDir
import com.rk.terminal.BuildConfig
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.meinname.ssh.Globals
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File
import java.io.FileOutputStream

object MkSession {
    
    // ============================================================
    // ★ LOKALE SHOWLOG FUNKTION
    // ============================================================
    private fun showLog(title: String, message: String) {
        MainActivity.showLog("MkSession", "[$title] $message")
    }

    // ============================================================
    // ★ @JvmStatic HINZUGEFÜGT
    // ============================================================
    @JvmStatic
    fun createSession(
        activity: MainActivity, 
        sessionClient: TerminalSessionClient, 
        session_id: String,
        workingMode: Int
    ): TerminalSession {
        
        showLog("Info", "🔍 Erstelle Session: $session_id")
        showLog("Info", "📌 Globals WORKING_MODE: ${Globals.WORKING_MODE} (${Globals.getWorkingModeName()})")
        
        with(activity) {
            val envVariables = mapOf(
                "ANDROID_ART_ROOT" to System.getenv("ANDROID_ART_ROOT"),
                "ANDROID_DATA" to System.getenv("ANDROID_DATA"),
                "ANDROID_I18N_ROOT" to System.getenv("ANDROID_I18N_ROOT"),
                "ANDROID_ROOT" to System.getenv("ANDROID_ROOT"),
                "ANDROID_RUNTIME_ROOT" to System.getenv("ANDROID_RUNTIME_ROOT"),
                "ANDROID_TZDATA_ROOT" to System.getenv("ANDROID_TZDATA_ROOT"),
                "BOOTCLASSPATH" to System.getenv("BOOTCLASSPATH"),
                "DEX2OATBOOTCLASSPATH" to System.getenv("DEX2OATBOOTCLASSPATH"),
                "EXTERNAL_STORAGE" to System.getenv("EXTERNAL_STORAGE")
            )

            // ============================================================
            // ★ ★ ★ WORKING DIRECTORY AUS GLOBALS ★ ★ ★
            // ============================================================
            val workingDir = when {
                pendingCommand?.workingDir != null -> pendingCommand!!.workingDir
                
                Globals.isUbuntu() -> ubuntuHomeDir().path // Ubuntu: /root
                
                Globals.isAlpine() -> alpineHomeDir().path
                
                Globals.isAndroid() -> "/sdcard"  // Android Shell: SD-Karte
                
                else -> ubuntuHomeDir().path  // Alpine: /root
            }
            val ubuntuPath = ubuntuHomeDir().path
            showLog("Debug", "📁 WorkingDir: $workingDir")
            showLog("Debug", "📌 WorkingMode: ${Globals.getWorkingModeName()}")
            
            

            // ============================================================
            // INIT-HOST FILE (nur für Alpine/Ubuntu)
            // ============================================================
            val initFileAlpine: File = localBinDir().child("init-host-alpine")
            val initFileUbuntu: File = localBinDir().child("init-host-ubuntu")
            
            /*

            if (!Globals.isAndroid()) {
                if (initFile.exists().not()) {
                    showLog("Info", "📄 Erstelle init-host...")
                    initFile.createFileIfNot()
                    val script = assets.open("init-host.sh").bufferedReader().use { it.readText() }
                    initFile.writeText(script)
                    showLog("Info", "✅ init-host erstellt (${script.length} bytes)")
                } else {
                    showLog("Debug", "⏭️ init-host bereits vorhanden")
                }

                // ============================================================
                // INIT FILE (nur für Alpine/Ubuntu)
                // ============================================================
                localBinDir().child("init").apply {
                    if (exists().not()) {
                        showLog("Info", "📄 Erstelle init...")
                        createFileIfNot()
                        val script = assets.open("init.sh").bufferedReader().use { it.readText() }
                        writeText(script)
                        showLog("Info", "✅ init erstellt (${script.length} bytes)")
                    } else {
                        showLog("Debug", "⏭️ init bereits vorhanden")
                    }
                }
            } else {
                showLog("Info", "📟 Android-Modus: Keine init-Skripte benötigt")
            }
            */

            // ============================================================
            // ENVIRONMENT VARIABLES
            // ============================================================
            val env = mutableListOf(
                "PATH=${System.getenv("PATH")}:/sbin:${localBinDir().absolutePath}",
                "HOME=/sdcard",
                "PUBLIC_HOME=${getExternalFilesDir(null)?.absolutePath}",
                "COLORTERM=truecolor",
                "TERM=xterm-256color",
                "LANG=C.UTF-8",
                "BIN=${localBinDir()}",
                "DEBUG=${BuildConfig.DEBUG}",
                "PREFIX=${filesDir.parentFile!!.path}",
                "LD_LIBRARY_PATH=${localLibDir().absolutePath}",
                "LINKER=${if(File("/system/bin/linker64").exists()){"/system/bin/linker64"}else{"/system/bin/linker"}}",
                "NATIVE_LIB_DIR=${applicationInfo.nativeLibraryDir}",
                "PKG=${packageName}",
                "RISH_APPLICATION_ID=${packageName}",
                "PKG_PATH=${applicationInfo.sourceDir}",
                "PROOT_TMP_DIR=${getTempDir().child(session_id).also { if (it.exists().not()){it.mkdirs()} }}",
                "TMPDIR=${getTempDir().absolutePath}",
                "SSHD_PORT=${Globals.SSHD_PORT}",
                "SSHD_ENABLED=${Globals.SSHD_ENABLED}",
                "FTP_PORT=${Globals.FTP_PORT}",
                "FTP_ENABLED=${Globals.FTP_ENABLED}"
                
            )

            showLog("Debug", "🔧 ${env.size} Environment-Variablen gesetzt")

            // ============================================================
            // PROOT LOADER (nur für Alpine/Ubuntu)
            // ============================================================
            if (!Globals.isAndroid()) {
                if (File(applicationInfo.nativeLibraryDir).child("libproot-loader32.so").exists()) {
                    env.add("PROOT_LOADER32=${applicationInfo.nativeLibraryDir}/libproot-loader32.so")
                    showLog("Debug", "🔧 PROOT_LOADER32 gesetzt")
                }

                if (File(applicationInfo.nativeLibraryDir).child("libproot-loader.so").exists()) {
                    env.add("PROOT_LOADER=${applicationInfo.nativeLibraryDir}/libproot-loader.so")
                    showLog("Debug", "🔧 PROOT_LOADER gesetzt")
                }

                if (Settings.seccomp) {
                    env.add("SECCOMP=1")
                    showLog("Debug", "🔧 SECCOMP aktiviert")
                }
            } else {
                showLog("Debug", "📟 Android-Modus: Keine Proot-Loader benötigt")
            }

            env.addAll(envVariables.map { "${it.key}=${it.value}" })

            // ============================================================
            // STAT / VMSTAT
            // ============================================================
            localDir().child("stat").apply {
                if (exists().not()) {
                    writeText(stat)
                    showLog("Debug", "📄 stat erstellt")
                }
            }

            localDir().child("vmstat").apply {
                if (exists().not()) {
                    writeText(vmstat)
                    showLog("Debug", "📄 vmstat erstellt")
                }
            }

            pendingCommand?.env?.let {
                env.addAll(it)
                showLog("Debug", "🔧 ${it.size} Pending-Env-Variablen hinzugefügt")
            }

            // ============================================================
            // ★ ★ ★ SHELL & ARGS BASIEREND AUF GLOBALS.WORKING_MODE ★ ★ ★
            // ============================================================
            val args: Array<String>
            val shell = if (pendingCommand == null) {
                when (Globals.WORKING_MODE) {
                    Globals.WORKING_MODE_ALPINE -> {
                        showLog("Info", "🐧 Starte Alpine Linux mit init-host")
                        args = arrayOf("-c", initFileAlpine.absolutePath)
                        "/system/bin/sh"
                    }
                    Globals.WORKING_MODE_UBUNTU -> {
                        showLog("Info", "🐧 Starte Ubuntu Linux mit init-host")
                        args = arrayOf("-c", initFileUbuntu.absolutePath)
                        "/system/bin/sh"
                    }
                    Globals.WORKING_MODE_ANDROID -> {
                        showLog("Info", "📟 Starte Android Shell")
                        args = arrayOf()
                        "/system/bin/sh"
                    }
                    else -> {
                        showLog("Warn", "⚠️ Unbekannter WorkingMode: ${Globals.WORKING_MODE}, verwende Alpine")
                        args = arrayOf()
                        "/system/bin/sh"
                    }
                }
            } else {
                //args = pendingCommand!!.args
                //val cmdShell = pendingCommand!!.shell
                //showLog("Info", "📟 Starte mit Pending-Command: $cmdShell ${args.joinToString(" ")}")
                //cmdShell
                args = pendingCommand!!.args
                pendingCommand!!.shell
            }

            pendingCommand = null

            showLog("Info", "✅ Session erstellt: shell=$shell, workingDir=$workingDir")
            showLog("Info", "📋 WorkingMode: ${Globals.getWorkingModeName()}")
            showLog("Debug", "📋 Args: ${args.joinToString(" ")}")
            showLog("Debug", "📋 Env: ${env.joinToString(" ")}")

            return TerminalSession(
                shell,
                workingDir,
                args,
                env.toTypedArray(),
                TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
                sessionClient,
            )
        }
    }
}