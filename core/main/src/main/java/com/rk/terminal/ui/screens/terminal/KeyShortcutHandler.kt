package com.rk.terminal.ui.screens.terminal

import android.view.KeyEvent
import com.blankj.utilcode.util.ClipboardUtils
import com.rk.settings.Settings
import com.rk.terminal.ui.activities.terminal.MainActivity

// ============================================================
// ★ LOKALE SHOWLOG FUNKTION
// ============================================================
private fun showLog(title: String, message: String) {
    MainActivity.showLog("KeyShortcutHandler", "[$title] $message")
}

// Oder kürzer:
// private fun showLog(message: String) = MainActivity.showLog("KeyShortcutHandler", message)

/**
 * Centralized keyboard shortcut handler for the terminal.
 * Reads configurable bindings from Settings and dispatches actions.
 */
object KeyShortcutHandler {

    /**
     * Handle a key event. Returns true if the key was consumed by a shortcut.
     */
    fun handle(keyCode: Int, event: KeyEvent, activity: MainActivity): Boolean {
        if (!Settings.shortcuts_enabled) {
            return false
        }

        // Try each action's binding
        for (action in ShortcutAction.entries) {
            val binding = Settings.getShortcutBinding(action)
            if (binding.matches(event)) {
                // ★ Log: Shortcut erkannt
                showLog("Info", "⌨️ Shortcut erkannt: ${action.name} (${binding.toDisplayString()})")
                
                val handled = dispatch(action, activity)
                if (handled) {
                    showLog("Debug", "✅ Shortcut ausgeführt: ${action.name}")
                } else {
                    showLog("Warn", "⚠️ Shortcut ausführung fehlgeschlagen: ${action.name}")
                }
                return true
            }
        }
        
        // ★ Log: Kein Shortcut gefunden (nur für Debugging, auskommentiert wegen Performance)
        // showLog("Debug", "⌨️ Kein Shortcut für keyCode=$keyCode")
        return false
    }

    private fun dispatch(action: ShortcutAction, activity: MainActivity): Boolean {
        return when (action) {
            ShortcutAction.PASTE -> {
                showLog("Debug", "📋 Paste-Aktion ausgeführt")
                handlePaste()
            }
            ShortcutAction.NEW_SESSION -> {
                showLog("Info", "📝 Neue Session via Shortcut")
                handleNewSession(activity)
            }
            ShortcutAction.CLOSE_SESSION -> {
                showLog("Info", "❌ Session schließen via Shortcut")
                handleCloseSession(activity)
            }
            ShortcutAction.SWITCH_SESSION_PREV -> {
                showLog("Debug", "⬅️ Vorherige Session via Shortcut")
                handleSwitchSession(activity, forward = false)
            }
            ShortcutAction.SWITCH_SESSION_NEXT -> {
                showLog("Debug", "➡️ Nächste Session via Shortcut")
                handleSwitchSession(activity, forward = true)
            }
        }
    }

    private fun handlePaste(): Boolean {
        val clip = ClipboardUtils.getText()?.toString() ?: return true
        if (clip.trim().isNotEmpty()) {
            terminalView.get()?.mEmulator?.paste(clip)
            showLog("Debug", "📋 ${clip.length} Zeichen eingefügt")
        } else {
            showLog("Debug", "📋 Zwischenablage leer")
        }
        return true
    }

    private fun handleNewSession(activity: MainActivity): Boolean {
        val binder = activity.sessionBinder ?: return true
        val service = binder.getService()

        val sessionId = generateUniqueSessionId(service.sessionList.keys.toList())
        showLog("Info", "📝 Erstelle neue Session: $sessionId via Shortcut")
        
        terminalView.get()?.let {
            val client = TerminalBackEnd(it, activity)
            binder.createSession(sessionId, client, activity, workingMode = Settings.working_Mode)
            showLog("Debug", "✅ Session $sessionId erstellt")
        }
        changeSession(activity, session_id = sessionId)
        showLog("Info", "✅ Zu Session $sessionId gewechselt")
        return true
    }

    private fun handleCloseSession(activity: MainActivity): Boolean {
        val binder = activity.sessionBinder ?: return true
        val service = binder.getService()
        val currentId = service.currentSession.value.first
        val sessionKeys = service.sessionList.keys.toList()

        showLog("Info", "❌ Schließe Session: $currentId (${sessionKeys.size} Sessions aktiv)")

        if (sessionKeys.size <= 1) {
            showLog("Info", "🏁 Letzte Session wird geschlossen")
            binder.terminateSession(currentId)
            if (service.sessionList.isEmpty()) {
                showLog("Info", "🏁 Keine Sessions mehr, beende Activity")
                activity.finish()
            }
        } else {
            val currentIndex = sessionKeys.indexOf(currentId)
            val nextId = if (currentIndex < sessionKeys.size - 1) {
                sessionKeys[currentIndex + 1]
            } else {
                sessionKeys[currentIndex - 1]
            }
            showLog("Debug", "🔄 Wechsle zu Session: $nextId")
            changeSession(activity, session_id = nextId)
            binder.terminateSession(currentId)
            showLog("Info", "✅ Session $currentId geschlossen, jetzt bei $nextId")
        }
        return true
    }

    private fun handleSwitchSession(activity: MainActivity, forward: Boolean): Boolean {
        val binder = activity.sessionBinder ?: return true
        val service = binder.getService()
        val sessionKeys = service.sessionList.keys.toList()

        if (sessionKeys.size <= 1) {
            showLog("Debug", "↔️ Nur eine Session, kein Wechsel nötig")
            return true
        }

        val currentId = service.currentSession.value.first
        val currentIndex = sessionKeys.indexOf(currentId)

        val nextIndex = if (forward) {
            (currentIndex + 1) % sessionKeys.size
        } else {
            (currentIndex - 1 + sessionKeys.size) % sessionKeys.size
        }

        val nextId = sessionKeys[nextIndex]
        showLog("Info", "🔄 Wechsle von $currentId zu $nextId (${if (forward) "vorwärts" else "rückwärts"})")
        
        changeSession(activity, session_id = nextId)
        return true
    }

    private fun generateUniqueSessionId(existingIds: List<String>): String {
        var index = 1
        var newId: String
        do {
            newId = "main$index"
            index++
        } while (newId in existingIds)
        
        showLog("Debug", "📝 Generierte Session-ID: $newId (${existingIds.size} existierende)")
        return newId
    }
}