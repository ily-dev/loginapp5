package com.meinname.loginapp3;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import java.io.File;

// ★ ★ ★ SHOWLOG IMPORT ★ ★ ★
import org.kivy.android.PythonService;

public class MyCustomActivity extends Activity {
    private static final String TAG = "LOGINAPP_ACTIVITY";
    private boolean pythonAvailable = false;
    private RadioButton rbPythonQt, rbPython, rbTerminal;
    private TextView tvStatus;

    // ============================================================
    // ★ ★ ★ SHOWLOG ★ ★ ★
    // ============================================================
    private static void showLog(String title, String message) {
        // 1. Android Log
        Log.d(TAG, title + ": " + message);
        
        // 2. PythonService.showLog verwenden (für einheitliche Logs)
        PythonService.showLog("MyCustomActivity", title + ": " + message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        showLog("onCreate", "🚀 MyCustomActivity gestartet.");
        Log.d(TAG, "MyCustomActivity gestartet.");
        
        // Hardware-Beschleunigung
        try {
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
            showLog("onCreate", "⚡ Hardware-Beschleunigung aktiviert");
        } catch (Exception e) {
            showLog("onCreate", "❌ Fehler bei Hardware-Beschleunigung: " + e.getMessage());
            Log.e(TAG, "Fehler: " + e.getMessage());
        }

        // Berechtigung für Android 11+
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            showLog("onCreate", "📱 Android 11+ - Prüfe Storage Permission");
            if (!Environment.isExternalStorageManager()) {
                showLog("onCreate", "📂 Fordere Storage Permission an");
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            } else {
                showLog("onCreate", "✅ Storage Permission bereits vorhanden");
            }
        }

        // Prüfen, ob Python verfügbar ist
        pythonAvailable = checkPythonAvailability();
        showLog("onCreate", "🐍 Python verfügbar: " + pythonAvailable);

        // Auswahl-Dialog anzeigen
        showModeSelectionDialog();
    }

    /**
     * Prüft, ob Python verfügbar ist (nur native .so-Dateien)
     */
    private boolean checkPythonAvailability() {
        showLog("checkPythonAvailability", "🔍 Prüfe Python Verfügbarkeit...");
        boolean result = checkPythonNativeLibs();
        showLog("checkPythonAvailability", "✅ Python verfügbar: " + result);
        return result;
    }

    /**
     * Prüft, ob die nativen Python-Bibliotheken (.so) vorhanden sind
     */
    private boolean checkPythonNativeLibs() {
        String nativeLibDir = getApplicationInfo().nativeLibraryDir;
        showLog("checkPythonNativeLibs", "📁 Native Lib Dir: " + nativeLibDir);
        
        String[] requiredLibs = {
            "libpython3.11.so",
            //"libmain.so",
            "libmain_arm64-v8a.so",
            "libpybundle.so"
        };
        
        boolean allFound = true;
        for (String lib : requiredLibs) {
            File libFile = new File(nativeLibDir, lib);
            boolean exists = libFile.exists();
            showLog("checkPythonNativeLibs", "📄 " + lib + ": " + (exists ? "✅" : "❌"));
            if (!exists) {
                Log.w(TAG, "Fehlende native Bibliothek: " + lib);
                allFound = false;
            }
        }
        
        if (allFound) {
            Log.i(TAG, "Alle nativen Python-Bibliotheken gefunden in: " + nativeLibDir);
            showLog("checkPythonNativeLibs", "✅ Alle Python-Bibliotheken gefunden");
        } else {
            showLog("checkPythonNativeLibs", "❌ Python-Bibliotheken fehlen");
        }
        return allFound;
    }

    /**
     * Zeigt den Auswahl-Dialog mit RadioButtons
     */
    private void showModeSelectionDialog() {
        showLog("showModeSelectionDialog", "📋 Zeige Modus-Auswahl Dialog");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        // Status-Text
        tvStatus = new TextView(this);
        String statusText = pythonAvailable 
            ? "✅ Python/Kivy ist verfügbar." 
            : "❌ Python wurde NICHT gefunden! Nur Terminal verfügbar.";
        tvStatus.setText(statusText);
        tvStatus.setTextSize(16);
        tvStatus.setPadding(0, 0, 0, 20);
        layout.addView(tvStatus);
        showLog("showModeSelectionDialog", "📊 Status: " + statusText);

        // RadioGroup (sorgt dafür, dass nur EIN Button aktiv ist!)
        final RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(LinearLayout.VERTICAL);
        
        // RadioButton 1: Python Qt (nur anzeigen, wenn verfügbar)
        if (pythonAvailable) {
            rbPythonQt = new RadioButton(this);
            rbPythonQt.setId(View.generateViewId());
            rbPythonQt.setText("🐍 Python (Qt) - Hauptmodus");
            rbPythonQt.setPadding(0, 10, 0, 10);
            radioGroup.addView(rbPythonQt);
            showLog("showModeSelectionDialog", "➕ Python Qt RadioButton hinzugefügt");
        }

        // RadioButton 2: Python (nur anzeigen, wenn verfügbar)
        if (pythonAvailable) {
            rbPython = new RadioButton(this);
            rbPython.setId(View.generateViewId());
            rbPython.setText("🐍 Python (Kivy) - Hauptmodus");
            rbPython.setPadding(0, 10, 0, 10);
            radioGroup.addView(rbPython);
            showLog("showModeSelectionDialog", "➕ Python RadioButton hinzugefügt");
        }

        // RadioButton 3: Terminal (immer verfügbar)
        rbTerminal = new RadioButton(this);
        rbTerminal.setId(View.generateViewId());
        rbTerminal.setText("💻 Terminal (ReTerminal)");
        rbTerminal.setPadding(0, 10, 0, 10);
        radioGroup.addView(rbTerminal);
        showLog("showModeSelectionDialog", "➕ Terminal RadioButton hinzugefügt");

        // Standard-Auswahl setzen
        if (pythonAvailable && rbPython != null) {
            rbPython.setChecked(true);
            showLog("showModeSelectionDialog", "🎯 Standard: Python ausgewählt");
        } else {
            rbTerminal.setChecked(true);
            showLog("showModeSelectionDialog", "🎯 Standard: Terminal ausgewählt");
        }

        layout.addView(radioGroup);

        // Hinweis bei fehlendem Python
        if (!pythonAvailable) {
            TextView tvHint = new TextView(this);
            tvHint.setText("\n💡 Tipp: Führe 'buildozer -v android debug' aus,\num den Python-Bundle zu erstellen.");
            tvHint.setTextSize(12);
            tvHint.setPadding(0, 10, 0, 0);
            layout.addView(tvHint);
            showLog("showModeSelectionDialog", "💡 Hinweis für fehlendes Python hinzugefügt");
        }

        // Dialog bauen
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modus auswählen");
        builder.setView(layout);
        builder.setCancelable(false);
        showLog("showModeSelectionDialog", "📋 Dialog erstellt");

        builder.setPositiveButton("STARTEN", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                showLog("showModeSelectionDialog", "▶️ STARTEN geklickt, selectedId: " + selectedId);
                
                // Prüfen, WELCHER RadioButton ausgewählt ist:
                if (pythonAvailable && rbPython != null && selectedId == rbPython.getId()) {
                    showLog("showModeSelectionDialog", "🐍 Python Modus ausgewählt");
                    //start Python kivy
                    startPythonMode();
                   
                    
                 } else if (rbPythonQt != null && selectedId == rbPythonQt.getId()) {
                 showLog("showModeSelectionDialog", "🐍 Qt Modus ausgewählt");
                 //start Python Qt
                 startPythonQt();    // ← NEUE Funktion (Qt)
                 } else {
            showLog("showModeSelectionDialog", "💻 Terminal Modus ausgewählt");
                startReTerminalMode();
                }
                
                
            }
        });

        builder.setNegativeButton("Beenden", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showLog("showModeSelectionDialog", "❌ Beenden ausgewählt");
                finish();
            }
        });

        builder.show();
        showLog("showModeSelectionDialog", "✅ Dialog angezeigt");
    }
    
    
    /**
     * Startet den Qt-Modus (PySide6/PyQt) - NEU!
     */
    private void startPythonQt() {
        showLog("startPythonQt", "🐍 Starte Qt-Modus");
        Log.d(TAG, "Qt-Modus gewählt");
        
        try {
            showLog("startPythonQt", "📝 Starte MyPythonActivity (Qt)");
            Intent intent = new Intent(this, com.meinname.loginapp3.MyPythonActivity.class);
            startActivity(intent);
            showLog("startPythonQt", "✅ MyPythonActivity gestartet");
            finish();
        } catch (Exception e) {
            showLog("startPythonQt", "❌ Fehler beim Starten von Qt: " + e.getMessage());
            Toast.makeText(this, "Fehler beim Starten von Qt: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Startet den Python-Modus
     */
    private void startPythonMode() {
        showLog("startPythonMode", "🐍 Starte Python-Modus");
        Log.d(TAG, "Python-Modus gewählt");
        
        if (pythonAvailable) {
            try {
                showLog("startPythonMode", "📝 Starte PythonActivity");
                Intent intent = new Intent(this, org.kivy.android.PythonActivity.class);
                startActivity(intent);
                showLog("startPythonMode", "✅ PythonActivity gestartet");
                finish();
            } catch (Exception e) {
                showLog("startPythonMode", "❌ Fehler beim Starten: " + e.getMessage());
                Toast.makeText(this, "Fehler beim Starten von Python: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            showLog("startPythonMode", "❌ Python nicht verfügbar");
            Toast.makeText(this, "Python-Bibliotheken fehlen!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Startet ReTerminal direkt
     */
    private void startReTerminalMode() {
        showLog("startReTerminalMode", "💻 Starte Terminal-Modus");
        Log.d(TAG, "Terminal-Modus gewählt");
        try {
            Intent intent = new Intent(MyCustomActivity.this, 
                com.rk.terminal.ui.activities.terminal.MainActivity.class);
            startActivity(intent);
            showLog("startReTerminalMode", "✅ Terminal gestartet");
            finish();
        } catch (Exception e) {
            showLog("startReTerminalMode", "❌ Fehler beim Starten: " + e.getMessage());
            Toast.makeText(this, "Fehler beim Starten von Terminal: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        showLog("onDestroy", "🗑️ MyCustomActivity onDestroy");
    }

    @Override
    protected void onPause() {
        super.onPause();
        showLog("onPause", "⏸️ MyCustomActivity onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        showLog("onResume", "▶️ MyCustomActivity onResume");
    }
}