package com.rk.terminal.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.rk.resources.drawables
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.screens.terminal.MkSession
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

// ============================================================
// ★ LOKALE SHOWLOG FUNKTION
// ============================================================
private fun showLog(title: String, message: String) {
    try {
        MainActivity.showLog("SessionService", "[$title] $message")
    } catch (e: Exception) {
        android.util.Log.d("SessionService", "[$title] $message")
    }
}

class SessionService : Service() {

    companion object {
        private lateinit var instance: SessionService
        
        fun getInstance(): SessionService {
            return instance
        }
    }

    private val sessions = hashMapOf<String, TerminalSession>()
    val sessionList = mutableStateMapOf<String, Int>()
    var currentSession = mutableStateOf(Pair("main", com.rk.settings.Settings.working_Mode))

    inner class SessionBinder : Binder() {
        fun getService(): SessionService {
            return this@SessionService
        }
        
        fun terminateAllSessions() {
            showLog("Info", "🗑️ Terminiere alle Sessions (${sessions.size})")
            sessions.values.forEach { it.finishIfRunning() }
            sessions.clear()
            sessionList.clear()
            updateNotification()
            showLog("Info", "✅ Alle Sessions terminiert")
        }
        
        fun createSession(id: String, client: TerminalSessionClient, activity: MainActivity, workingMode: Int): TerminalSession {
            showLog("Info", "📝 Erstelle Session: $id, workingMode: $workingMode")
            return MkSession.createSession(activity, client, id, workingMode = workingMode).also {
                sessions[id] = it
                sessionList[id] = workingMode
                showLog("Debug", "✅ Session $id erstellt (${sessions.size} Sessions aktiv)")
                updateNotification()
            }
        }
        
        fun getSession(id: String): TerminalSession? {
            val session = sessions[id]
            if (session != null) {
                showLog("Debug", "🔍 Session $id gefunden")
            } else {
                showLog("Debug", "⚠️ Session $id nicht gefunden")
            }
            return session
        }
        
        fun terminateSession(id: String) {
            showLog("Info", "🗑️ Terminiere Session: $id")
            runCatching {
                sessions[id]?.apply {
                    if (emulator != null) {
                        sessions[id]?.finishIfRunning()
                        showLog("Debug", "✅ Session $id beendet")
                    }
                }
                sessions.remove(id)
                sessionList.remove(id)
                showLog("Debug", "📊 ${sessions.size} Sessions verbleiben")
                
                if (sessions.isEmpty()) {
                    showLog("Info", "🏁 Keine Sessions mehr, beende Service")
                    stopSelf()
                } else {
                    updateNotification()
                    showLog("Debug", "🔔 Notification aktualisiert")
                }
            }.onFailure { 
                it.printStackTrace()
                showLog("Error", "❌ Fehler beim Terminieren von Session $id: ${it.message}")
            }
        }
    }

    private val binder = SessionBinder()
    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    override fun onBind(intent: Intent?): IBinder {
        showLog("Debug", "🔗 Service gebunden")
        return binder
    }

    override fun onDestroy() {
        showLog("Info", "🛑 Service wird zerstört")
        showLog("Debug", "📊 ${sessions.size} Sessions werden beendet")
        sessions.forEach { s -> s.value.finishIfRunning() }
        sessions.clear()
        sessionList.clear()
        showLog("Info", "✅ Service zerstört")
        super.onDestroy()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        showLog("Info", "🚀 SessionService onCreate")
        showLog("Debug", "📱 Build: ${Build.VERSION.SDK_INT}")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            showLog("Debug", "🔔 Notification Channel erstellt (API >= O)")
        }
        val notification = createNotification()
        showLog("Debug", "🔔 Notification erstellt")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            showLog("Debug", "🔔 Foreground Service gestartet (API >= UPSIDE_DOWN_CAKE)")
        } else {
            startForeground(1, notification)
            showLog("Debug", "🔔 Foreground Service gestartet")
        }
        
        showLog("Info", "✅ SessionService initialisiert")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showLog("Debug", "📨 onStartCommand: action=${intent?.action}")
        
        when (intent?.action) {
            "ACTION_EXIT" -> {
                showLog("Info", "🚪 ACTION_EXIT empfangen")
                sessions.forEach { s -> s.value.finishIfRunning() }
                stopSelf()
            }
        }
    return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotification(): Notification {
        showLog("Debug", "🔔 Erstelle Notification")
        
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val exitIntent = Intent(this, SessionService::class.java).apply {
            action = "ACTION_EXIT"
        }
        val exitPendingIntent = PendingIntent.getService(
            this, 1, exitIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationText = getNotificationContentText()
        showLog("Debug", "🔔 Notification Text: $notificationText")

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ReTerminal")
            .setContentText(notificationText)
            .setSmallIcon(drawables.terminal)
            .setContentIntent(pendingIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    "EXIT",
                    exitPendingIntent
                ).build()
            )
            .setOngoing(true)
            .build()
    }

    private val CHANNEL_ID = "session_service_channel"

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        showLog("Debug", "🔔 Erstelle Notification Channel: $CHANNEL_ID")
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Session Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification for Terminal Service"
        }
        notificationManager.createNotificationChannel(channel)
        showLog("Debug", "✅ Notification Channel erstellt")
    }

    private fun updateNotification() {
        val sessionCount = sessions.size
        showLog("Debug", "🔔 Update Notification: $sessionCount Sessions")
        val notification = createNotification()
        notificationManager.notify(1, notification)
    }

    private fun getNotificationContentText(): String {
        val count = sessions.size
        return if (count == 1) {
            "1 session running"
        } else {
            "$count sessions running"
        }
    }
}