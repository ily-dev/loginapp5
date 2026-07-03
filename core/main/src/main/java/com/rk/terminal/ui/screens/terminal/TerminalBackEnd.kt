package com.rk.terminal.ui.screens.terminal

import android.content.res.Configuration
import android.content.res.Resources
import android.media.MediaPlayer
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.rk.libcommons.child
import com.rk.libcommons.createFileIfNot
import com.rk.libcommons.dpToPx
import com.rk.settings.Settings
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.screens.terminal.virtualkeys.SpecialButton
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysView
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// ============================================================
// ★ LOKALE SHOWLOG FUNKTION
// ============================================================
private fun showLog(title: String, message: String) {
    MainActivity.showLog("TerminalBackEnd", "[$title] $message")
}

// Oder kürzer:
// private fun showLog(message: String) = MainActivity.showLog("TerminalBackEnd", message)

class TerminalBackEnd(val terminal: TerminalView, val activity: MainActivity) : TerminalViewClient, TerminalSessionClient {
    
    init {
        showLog("Info", "🔧 TerminalBackEnd initialisiert")
    }
    
    override fun onTextChanged(changedSession: TerminalSession) {
        terminal.onScreenUpdated()
        //showLog("Debug", "📝 Text geändert in Session: ${changedSession}")
    }
    
    override fun onTitleChanged(changedSession: TerminalSession) {
        showLog("Debug", "📌 Titel geändert in Session: ${changedSession}")
    }
    
    override fun onSessionFinished(finishedSession: TerminalSession) {
        showLog("Info", "🏁 Session beendet: ${finishedSession}")
    }
    
    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
        ClipboardUtils.copyText("Terminal", text)
        showLog("Info", "📋 Text in Zwischenablage kopiert: ${text.length} Zeichen")
        showLog("Debug", "📋 Kopierter Text: ${text.take(50)}...")
    }
    
    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        val clip = ClipboardUtils.getText().toString()
        if (clip.trim { it <= ' ' }.isNotEmpty() && terminal.mEmulator != null) {
            terminal.mEmulator.paste(clip)
            showLog("Info", "📋 Text aus Zwischenablage eingefügt: ${clip.length} Zeichen")
            showLog("Debug", "📋 Eingefügter Text: ${clip.take(50)}...")
        } else {
            showLog("Debug", "📋 Zwischenablage leer oder Terminal nicht bereit")
        }
    }

    override fun setTerminalShellPid(
        session: TerminalSession,
        pid: Int
    ) {
        showLog("Debug", "🔧 Shell PID gesetzt: $pid für Session: ${session}")
    }

    override fun onBell(session: TerminalSession) {
        if (Settings.bell) {
            showLog("Info", "🔔 Bell ausgelöst für Session: ${session}")
            
            activity.lifecycleScope.launch {
                val bellFile = activity.cacheDir.child("bell.oga")
                if (bellFile.exists().not()) {
                    showLog("Debug", "🔔 Erstelle Bell-Datei: ${bellFile.absolutePath}")
                    bellFile.createNewFile()
                    withContext(Dispatchers.IO) {
                        activity.assets.open("bell.oga").use { assetIS ->
                            FileOutputStream(bellFile).use { bellFileOutS ->
                                assetIS.copyTo(bellFileOutS)
                            }
                        }
                    }
                    showLog("Info", "🔔 Bell-Datei erstellt")
                }

                val mediaPlayer = MediaPlayer()
                mediaPlayer.setOnCompletionListener {
                    it?.release()
                    showLog("Debug", "🔔 MediaPlayer freigegeben")
                }
                mediaPlayer.setDataSource(bellFile.absolutePath)
                mediaPlayer.prepare()
                mediaPlayer.start()
                showLog("Debug", "🔔 Bell abgespielt")
            }
        } else {
            showLog("Debug", "🔔 Bell ignoriert (Settings.bell = false)")
        }
    }
    
    override fun onColorsChanged(session: TerminalSession) {
        showLog("Debug", "🎨 Farben geändert für Session: ${session}")
    }
    
    override fun onTerminalCursorStateChange(state: Boolean) {
        showLog("Debug", "🖱️ Cursor-Status geändert: ${if (state) "sichtbar" else "unsichtbar"}")
    }
    
    override fun getTerminalCursorStyle(): Int {
        return TerminalEmulator.DEFAULT_TERMINAL_CURSOR_STYLE
    }
    
    override fun logError(tag: String?, message: String?) {
        Log.e(tag.toString(), message.toString())
        showLog("Error", "[$tag] $message")
    }
    
    override fun logWarn(tag: String?, message: String?) {
        Log.w(tag.toString(), message.toString())
        showLog("Warn", "[$tag] $message")
    }
    
    override fun logInfo(tag: String?, message: String?) {
        Log.i(tag.toString(), message.toString())
        showLog("Info", "[$tag] $message")
    }
    
    override fun logDebug(tag: String?, message: String?) {
        Log.d(tag.toString(), message.toString())
        showLog("Debug", "[$tag] $message")
    }
    
    override fun logVerbose(tag: String?, message: String?) {
        Log.v(tag.toString(), message.toString())
        showLog("Debug", "[$tag] $message")
    }
    
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {
        Log.e(tag.toString(), message.toString())
        e?.printStackTrace()
        showLog("Error", "[$tag] $message")
        showLog("Error", "Stacktrace: ${e?.stackTraceToString()?.take(500)}...")
    }
    
    override fun logStackTrace(tag: String?, e: Exception?) {
        e?.printStackTrace()
        showLog("Error", "[$tag] Stacktrace: ${e?.stackTraceToString()?.take(500)}...")
    }

    override fun onScale(scale: Float): Float {
        val fontScale = scale.coerceIn(11f, 45f)
        terminal.setTextSize(fontScale.toInt())
        showLog("Info", "🔍 Schriftgröße geändert: ${fontScale.toInt()}dp (von ${scale})")
        return fontScale
    }

    val isHardwareKeyboardConnected: Boolean
        get() {
            val config = Resources.getSystem().configuration
            return config.keyboard != Configuration.KEYBOARD_NOKEYS
        }

    override fun onSingleTapUp(e: MotionEvent) {
        if (!(isHardwareKeyboardConnected && Settings.hide_soft_keyboard_if_hwd)) {
            showSoftInput()
            showLog("Debug", "👆 Single-Tap: Soft-Keyboard angezeigt")
        } else {
            showLog("Debug", "👆 Single-Tap: Soft-Keyboard ausgeblendet (Hardware-Tastatur)")
        }
    }
    
    override fun shouldBackButtonBeMappedToEscape(): Boolean {
        return false
    }
    
    override fun shouldEnforceCharBasedInput(): Boolean {
        return Settings.input_mode != 1 // TYPE_NULL mode uses TYPE_NULL inputType
    }

    override fun getInputMode(): Int {
        return Settings.input_mode
    }
    
    override fun shouldUseCtrlSpaceWorkaround(): Boolean {
        return true
    }
    
    override fun isTerminalViewSelected(): Boolean {
        return true
    }
    
    override fun copyModeChanged(copyMode: Boolean) {
        showLog("Debug", "📋 Copy-Mode: ${if (copyMode) "aktiviert" else "deaktiviert"}")
    }
    
    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean {
        showLog("Debug", "⌨️ KeyDown: keyCode=$keyCode, key=${KeyEvent.keyCodeToString(keyCode)}")
        
        if (KeyShortcutHandler.handle(keyCode, e, activity)) {
            showLog("Debug", "⌨️ KeyShortcut-Handler hat verarbeitet")
            return true
        }
        
        if (keyCode == KeyEvent.KEYCODE_ENTER && !session.isRunning) {
            showLog("Info", "⌨️ ENTER auf beendeter Session: Terminiere Session")
            activity.sessionBinder?.terminateSession(activity.sessionBinder!!.getService().currentSession.value.first)
            
            if (activity.sessionBinder!!.getService().sessionList.isEmpty()) {
                showLog("Info", "🏁 Keine Sessions mehr, beende Activity")
                activity.finish()
            } else {
                val newSession = activity.sessionBinder!!.getService().sessionList.keys.first()
                showLog("Info", "🔄 Wechsle zu Session: $newSession")
                changeSession(activity, newSession)
            }
            return true
        }
        return false
    }
    
    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean {
        showLog("Debug", "⌨️ KeyUp: keyCode=$keyCode, key=${KeyEvent.keyCodeToString(keyCode)}")
        return false
    }
    
    override fun onLongPress(event: MotionEvent): Boolean {
        showLog("Debug", "👆 LongPress: x=${event.x}, y=${event.y}")
        return false
    }
    
    // keys
    override fun readControlKey(): Boolean {
        val state = virtualKeysView.get()?.readSpecialButton(
            SpecialButton.CTRL, true)
        val result = state != null && state
        if (result) {
            showLog("Debug", "⌨️ CTRL gedrückt")
        }
        return result
    }
    
    override fun readAltKey(): Boolean {
        val state = virtualKeysView.get()?.readSpecialButton(
            SpecialButton.ALT, true)
        val result = state != null && state
        if (result) {
            showLog("Debug", "⌨️ ALT gedrückt")
        }
        return result
    }
    
    override fun readShiftKey(): Boolean {
        val state = virtualKeysView.get()?.readSpecialButton(
            SpecialButton.SHIFT, true)
        val result = state != null && state
        if (result) {
            showLog("Debug", "⌨️ SHIFT gedrückt")
        }
        return result
    }
    
    override fun readFnKey(): Boolean {
        val state = virtualKeysView.get()?.readSpecialButton(
            SpecialButton.FN, true)
        val result = state != null && state
        if (result) {
            showLog("Debug", "⌨️ FN gedrückt")
        }
        return result
    }
    
    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean {
        showLog("Debug", "⌨️ CodePoint: $codePoint, ctrlDown=$ctrlDown")
        return false
    }
    
    override fun onEmulatorSet() {
        setTerminalCursorBlinkingState(true)
        showLog("Info", "🖥️ Emulator gesetzt, Cursor blinkt")
    }
    
    private fun setTerminalCursorBlinkingState(start: Boolean) {
        if (terminal.mEmulator != null) {
            terminal.setTerminalCursorBlinkerState(start, true)
        }
    }
    
    private fun showSoftInput() {
        terminal.requestFocus()
        KeyboardUtils.showSoftInput(terminal)
    }
}