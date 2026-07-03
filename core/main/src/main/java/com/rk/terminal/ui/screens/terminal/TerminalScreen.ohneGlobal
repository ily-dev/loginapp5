package com.rk.terminal.ui.screens.terminal

// TerminalScreen.kt - Oben hinzufügen
import android.content.Intent
import androidx.compose.material.icons.filled.Android  // ★ Für Python-Button
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text



// ✅ RICHTIG (verwenden)
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.viewinterop.AndroidView
import android.os.Build

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.widget.doOnTextChanged
import androidx.navigation.NavController
import com.google.android.material.R
import com.rk.components.compose.preferences.base.PreferenceGroup
import com.rk.libcommons.application
import com.rk.resources.strings
import com.rk.libcommons.child
import com.rk.libcommons.dpToPx
import com.rk.libcommons.localDir
import com.rk.libcommons.pendingCommand
import com.rk.settings.Settings
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.routes.MainActivityRoutes
import com.rk.terminal.ui.screens.settings.SettingsCard
import com.rk.terminal.ui.screens.settings.WorkingMode
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysConstants
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysInfo
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysListener
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysView
import com.termux.terminal.TerminalColors
import com.termux.view.TerminalView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.UnknownHostException
import java.util.Properties
import kotlinx.coroutines.delay

// ============================================================
// ★ BESTEHENDE VARIABLEN
// ============================================================
var terminalView = WeakReference<TerminalView?>(null)
var virtualKeysView = WeakReference<VirtualKeysView?>(null)

var darkText = mutableStateOf(Settings.blackTextColor)
var bitmap = mutableStateOf<ImageBitmap?>(null)

private val file = application!!.filesDir.child("font.ttf")
private var font = (if (file.exists() && file.canRead()){
    Typeface.createFromFile(file)
}else{
    Typeface.MONOSPACE
})

suspend fun setFont(typeface: Typeface) = withContext(Dispatchers.Main){
    font = typeface
    terminalView.get()?.apply {
        setTypeface(typeface)
        onScreenUpdated()
    }
}

inline fun getViewColor(): Int{
    return if (darkText.value){
        Color.BLACK
    }else{
        Color.WHITE
    }
}

inline fun getComposeColor(): androidx.compose.ui.graphics.Color{
    return if (darkText.value){
        androidx.compose.ui.graphics.Color.Black
    }else{
        androidx.compose.ui.graphics.Color.White
    }
}

var showToolbar = mutableStateOf(Settings.toolbar)
var showVirtualKeys = mutableStateOf(Settings.virtualKeys)
var showHorizontalToolbar = mutableStateOf(Settings.toolbar)

// ============================================================
// ★ DOWNLOADER DATENKLASSEN (aus Downloader.kt)
// ============================================================
private data class CopyFile(val assetPath: String, val outputFile: File)
private data class DownloadFile(val url: String, val outputFile: File)

private data class AssetFiles(
    val privateTar: String,
    val ubuntuTar: String,
    val alpineTar: String
)

private data class AbiUrls(
    val talloc: String, 
    val proot: String
    //val alpine: String
)


// ============================================================
    // ★ LOKALE SHOWLOG FUNKTION
    // ============================================================
    private fun showLog(title: String, message: String) {
        MainActivity.showLog("MkSession", "[$title] $message")
    }

// ============================================================
// ★ DOWNLOADER MAPS (aus Downloader.kt)
// ============================================================
private val assetFilesMap = mapOf(
    "x86_64" to AssetFiles(
        privateTar = "private.tar",
        ubuntuTar = "ubuntu/ubuntu.tar",
        alpineTar = "alpine/alpine.tar"
    ),
    "arm64-v8a" to AssetFiles(
        privateTar = "private.tar",
        ubuntuTar = "ubuntu/ubuntu.tar",  
        alpineTar = "alpine/alpine.tar"
    ),
    "armeabi-v7a" to AssetFiles(
        privateTar = "private.tar",
        ubuntuTar = "ubuntu/ubuntu.tar",
        alpineTar = "alpine/alpine.tar"
    )
)

private val abiMap = mapOf(
    "x86_64" to AbiUrls(
        talloc = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/x86_64/libtalloc.so.2",
        proot = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/x86_64/proot",
        //alpine = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/x86_64/alpine-minirootfs-3.21.0-x86_64.tar.gz"
    ),
    "arm64-v8a" to AbiUrls(
        talloc = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/aarch64/libtalloc.so.2",
        proot = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/aarch64/proot",
        //alpine = "https://dl-cdn.alpinelinux.org/alpine/latest-stable/releases/aarch64/alpine-minirootfs-3.24.0-aarch64.tar.gz"
        //alpine = "https://cdimage.ubuntu.com/ubuntu-base/releases/jammy/release/ubuntu-base-22.04.1-base-arm64.tar.gz"
    ),
    "armeabi-v7a" to AbiUrls(
        talloc = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/arm/libtalloc.so.2",
        proot = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/arm/proot",
        //alpine = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/armhf/alpine-minirootfs-3.21.0-armhf.tar.gz"
    )
)

// ============================================================
// ★ DOWNLOADER HILFSFUNKTIONEN (aus Downloader.kt)
// ============================================================
private suspend fun copyAssetFile(
    context: Context, 
    assetPath: String, 
    outputFile: File, 
    onProgress: (Long, Long) -> Unit
) {
    withContext(Dispatchers.IO) {
        outputFile.outputStream().use { output ->
            context.assets.open(assetPath).use { input ->
                val totalBytes = input.available().toLong()
                var copiedBytes = 0L
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    copiedBytes += bytesRead
                    withContext(Dispatchers.Main) {
                        onProgress(copiedBytes, totalBytes)
                    }
                }
            }
        }
    }
}

private suspend fun downloadFile(url: String, outputFile: File, onProgress: (Long, Long) -> Unit) {
    withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to download file: ${response.code}")

            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            outputFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        withContext(Dispatchers.Main) {
                            onProgress(downloadedBytes, totalBytes)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// ★ DOWNLOADER LOGIK (aus Downloader.kt)
// ============================================================
suspend fun runDownloaderSetup(
    context: Context,
    onProgress: (Float, String) -> Unit,
    onComplete: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    try {
        // 1. ABI erkennen
        val abi = Build.SUPPORTED_ABIS.firstOrNull { it in abiMap } 
            ?: throw RuntimeException("Unsupported CPU")
        onProgress(0.05f, "🔍 ABI erkannt: $abi")
        
        // ============================================================
        // 2. ALLE ASSETS KOPIEREN (private.tar + ubuntu.tar.gz)
        // ============================================================
        onProgress(0.1f, "📦 Kopiere Assets...")
        
        // ★ ★ ★ ALLE ASSETS ZUM KOPIEREN ★ ★ ★
        val filesToCopy = listOf(
            "private.tar" to assetFilesMap[abi]!!.privateTar,
            "ubuntu.tar.gz" to assetFilesMap[abi]!!.ubuntuTar,
            "alpine.tar.gz" to assetFilesMap[abi]!!.alpineTar
            
        ).mapNotNull { (name, assetPath) -> 
            try {
                context.assets.open(assetPath).close()
                CopyFile(assetPath, Rootfs.reTerminal.child(name))
            } catch (e: IOException) {
                onProgress(0.15f, "⚠️ $assetPath nicht in Assets")
                null
            }
        }
        
        val needsCopy = filesToCopy.any { !it.outputFile.exists() }
        
        if (needsCopy) {
            var completedFiles = 0
            val totalFiles = filesToCopy.size
            
            if (filesToCopy.isNotEmpty()) {
                filesToCopy.forEach { file ->
                    val outputFile = file.outputFile.apply { parentFile?.mkdirs() }
                    if (!outputFile.exists()) {
                        copyAssetFile(context, file.assetPath, outputFile) { copied, total ->
                            val currentProgress = if (total > 0) copied.toFloat() / total else 0f
                            val progress = 0.1f + ((completedFiles + currentProgress) / totalFiles) * 0.2f
                            onProgress(progress, "📦 Kopiere ${file.assetPath}...")
                        }
                    }
                    completedFiles++
                    val progress = 0.1f + (completedFiles.toFloat() / totalFiles) * 0.2f
                    onProgress(progress, "✅ ${file.assetPath} kopiert")
                }
            }
        } else {
            onProgress(0.3f, "✅ Alle Assets bereits vorhanden")
        }

        // ============================================================
        // 3. LIBRARY DATEIEN KOPIEREN
        // ============================================================
        onProgress(0.3f, "📚 Kopiere Libraries...")
        
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val targetFilesDir = Rootfs.reTerminal
        
        val libsToCopy = listOf(
            Pair("libpybundle.so", "libpybundle.tar"),
            Pair("libpython3.11.so", "libpython3.11.so"),
            Pair("libmain.so", "libmain.so")
        )
        
        var libCompleted = 0
        for ((libName, targetName) in libsToCopy) {
            val sourceFile = File(nativeLibDir, libName)
            val targetFile = File(targetFilesDir, targetName)
            
            if (sourceFile.exists() && !targetFile.exists()) {
                sourceFile.copyTo(targetFile, overwrite = true)
                onProgress(0.3f + (libCompleted.toFloat() / libsToCopy.size) * 0.2f, "📚 Kopiere $libName...")
            }
            libCompleted++
        }
        onProgress(0.5f, "✅ Libraries kopiert")

        // ============================================================
        // 4. DATEIEN DOWNLOADEN (nur wenn nicht vorhanden)
        // ============================================================
        onProgress(0.5f, "🌐 Starte Downloads...")
        
        val filesToDownload = listOf(
            "libtalloc.so.2" to abiMap[abi]!!.talloc,
            "proot" to abiMap[abi]!!.proot
            //"alpine.tar.gz" to abiMap[abi]!!.alpine  // Fallback
        ).map { (name, url) -> DownloadFile(url, Rootfs.reTerminal.child(name)) }

        val needsDownload = filesToDownload.any { !it.outputFile.exists() }

        if (needsDownload) {
            var completedFiles = 0
            val totalFiles = filesToDownload.size

            filesToDownload.forEach { file ->
                val outputFile = file.outputFile.apply { parentFile?.mkdirs() }
                if (!outputFile.exists()) {
                    downloadFile(file.url, outputFile) { downloaded, total ->
                        val currentProgress = if (total > 0) downloaded.toFloat() / total else 0f
                        val progress = 0.5f + ((completedFiles + currentProgress) / totalFiles) * 0.4f
                        onProgress(progress, "📥 ${outputFile.name}...")
                    }
                }
                completedFiles++
                val progress = 0.5f + (completedFiles.toFloat() / totalFiles) * 0.4f
                onProgress(progress, "✅ ${file.outputFile.name} fertig")
                outputFile.setExecutable(true, false)
            }
        } else {
            onProgress(0.9f, "✅ Alle Dateien bereits vorhanden")
        }

        Rootfs.refreshStatus()
        onProgress(1.0f, "✅ Setup abgeschlossen!")
        delay(500)
        onComplete(true)

    } catch (e: Exception) {
        if (e is UnknownHostException) {
            onError("Netzwerkfehler: ${e.message}")
        } else {
            onError("Setup-Fehler: ${e.message}")
        }
        onComplete(false)
    }
}





// ============================================================
// ★ HELPER-FUNKTIONEN
// ============================================================
fun getNameOfWorkingMode(workingMode: Int?): String {
    return when (workingMode) {
        0 -> "ALPINE".lowercase()
        1 -> "ANDROID".lowercase()
        null -> "null"
        else -> "unknown"
    }
}

// ============================================================
// ★ TERMINALSCREEN MIT INTEGRIERTEM DOWNLOADER
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    modifier: Modifier = Modifier,
    mainActivityActivity: MainActivity,
    navController: NavController
) {
    val context = LocalContext.current
    val isDarkMode = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    
    // ★ DOWNLOADER-STATE
    var progress by remember { mutableFloatStateOf(0f) }
    var progressText by remember { mutableStateOf("Initialisiere...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var setupComplete by remember { mutableStateOf(Rootfs.isDownloaded.value) }

    // ★ DOWNLOADER STARTEN (wenn nötig)
    LaunchedEffect(Unit) {
        if (!Rootfs.isDownloaded.value && !isDownloading) {
            isDownloading = true
            runDownloaderSetup(
                context = context,
                onProgress = { prog, msg ->
                    progress = prog
                    progressText = msg
                },
                onComplete = { success ->
                    isDownloading = false
                    if (success) {
                        Rootfs.refreshStatus()
                        setupComplete = true
                    }
                },
                onError = { error ->
                    isDownloading = false
                    errorMessage = error
                }
            )
        }
    }

    // ★ TERMINAL-INIT (bestehend)
    LaunchedEffect(Unit){
        withContext(Dispatchers.IO){
            if (context.filesDir.child("background").exists().not()){
                darkText.value = !isDarkMode
            }else if (bitmap.value == null){
                val fullBitmap = BitmapFactory.decodeFile(context.filesDir.child("background").absolutePath)?.asImageBitmap()
                if (fullBitmap != null) bitmap.value = fullBitmap
            }
        }

        scope.launch(Dispatchers.Main){
            virtualKeysView.get()?.apply {
                virtualKeysViewClient =
                    terminalView.get()?.mTermSession?.let {
                        VirtualKeysListener(it)
                    }
                buttonTextColor = getViewColor()
                reload(
                    VirtualKeysInfo(
                        VIRTUAL_KEYS,
                        "",
                        VirtualKeysConstants.CONTROL_CHARS_ALIASES
                    )
                )
            }

            terminalView.get()?.apply {
                onScreenUpdated()
                mEmulator?.mColors?.mCurrentColors?.apply {
                    set(256, getViewColor())
                    set(258, getViewColor())
                }
            }
        }
    }

    Box {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        val drawerWidth = (screenWidthDp * 0.84).dp
        var showAddDialog by remember { mutableStateOf(false) }

        BackHandler(enabled = drawerState.isOpen) {
            scope.launch {
                drawerState.close()
            }
        }

        if (drawerState.isClosed){
            SetStatusBarTextColor(isDarkIcons = darkText.value)
        }else{
            SetStatusBarTextColor(isDarkIcons = !isDarkMode)
        }

        // ★ DIALOG (nur im Terminal-Modus)
        if (showAddDialog && Rootfs.isDownloaded.value){
            BasicAlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                }
            ) {
                fun createSession(workingMode:Int){
                    fun generateUniqueString(existingStrings: List<String>): String {
                        var index = 1
                        var newString: String
                        do {
                            newString = "main$index"
                            index++
                        } while (newString in existingStrings)
                        return newString
                    }

                    val sessionId = generateUniqueString(mainActivityActivity.sessionBinder!!.getService().sessionList.keys.toList())

                    terminalView.get()
                        ?.let {
                            val client = TerminalBackEnd(it, mainActivityActivity)
                            mainActivityActivity.sessionBinder!!.createSession(
                                sessionId,
                                client,
                                mainActivityActivity, workingMode = workingMode
                            )
                        }
                    changeSession(mainActivityActivity, session_id = sessionId)
                }

                PreferenceGroup {
                    SettingsCard(
                        title = { Text("Alpine") },
                        description = {Text(stringResource(strings.alpine_desc))},
                        onClick = {
                           createSession(workingMode = WorkingMode.ALPINE)
                            showAddDialog = false
                        })

                    SettingsCard(
                        title = { Text("Android") },
                        description = {Text(stringResource(strings.android_desc))},
                        onClick = {
                            createSession(workingMode = WorkingMode.ANDROID)
                            showAddDialog = false
                        })
                }
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen || !(showToolbar.value && (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE || showHorizontalToolbar.value)),
            drawerContent = {
                // ★ DRAWER (nur im Terminal-Modus)
                if (Rootfs.isDownloaded.value) {
                    ModalDrawerSheet(modifier = Modifier.width(drawerWidth)) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(strings.session),
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Row {
                                    val keyboardController = LocalSoftwareKeyboardController.current
                                    IconButton(onClick = {
                                        navController.navigate(MainActivityRoutes.Settings.route)
                                        keyboardController?.hide()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Settings,
                                            contentDescription = null
                                        )
                                    }

                                    IconButton(onClick = {
                                        showAddDialog = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }

                            mainActivityActivity.sessionBinder?.getService()?.sessionList?.keys?.toList()?.let {
                                LazyColumn {
                                    items(it) { session_id ->
                                        SelectableCard(
                                            selected = session_id == mainActivityActivity.sessionBinder?.getService()?.currentSession?.value?.first,
                                            onSelect = {
                                                changeSession(
                                                    mainActivityActivity,
                                                    session_id
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = session_id,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )

                                                if (session_id != mainActivityActivity.sessionBinder?.getService()?.currentSession?.value?.first) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    IconButton(
                                                        onClick = {
                                                            println(session_id)
                                                            mainActivityActivity.sessionBinder?.terminateSession(
                                                                session_id
                                                            )
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Delete,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            content = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BackgroundImage()
                    val color = getComposeColor()
                    Column {
                        // ★ TOPAPPBAR
                        if (showToolbar.value && (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE || showHorizontalToolbar.value)){
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent
                                ),
                                title = {
                                    Column {
                                        Text(
                                            text = if (Rootfs.isDownloaded.value) "ReTerminal" else "Downloader",
                                            color = color
                                        )
                                        Text(
                                            style = MaterialTheme.typography.bodySmall,
                                            text = if (Rootfs.isDownloaded.value) {
                                                mainActivityActivity.sessionBinder?.getService()?.currentSession?.value?.first + " (${getNameOfWorkingMode(mainActivityActivity.sessionBinder?.getService()?.currentSession?.value?.second)})"
                                            } else {
                                                progressText
                                            },
                                            color = color
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        scope.launch { drawerState.open() }
                                    }) {
                                        Icon(Icons.Default.Menu, null, tint = color)
                                    }
                                },
                                
                                actions = {
            if (Rootfs.isDownloaded.value) {
                // ★ Python-Button
                IconButton(
                    onClick = {
                        showLog("Info", "🐍 Python-Button gedrückt!")
                        try {
                            val intent = Intent(mainActivityActivity, Class.forName("org.kivy.android.PythonActivity"))
                            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            mainActivityActivity.startActivity(intent)
                            showLog("Info", "✅ Python in Vordergrund geholt")
                        } catch (e: ClassNotFoundException) {
                            showLog("Error", "❌ PythonActivity nicht gefunden: ${e.message}")
                        } catch (e: Exception) {
                            showLog("Error", "❌ Fehler: ${e.message}")
                        }
                    }
                ) {
                    Text(
                        text = "🤖",
                        fontSize = 24.sp,
                        color = color
                    )
                }
                
                // ★ Add-Button
                IconButton(
                    onClick = {
                        showAddDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, null, tint = color)
                }
            }
        }
                                
                                
                                
                                
                                
                            )
                        }

                        val density = LocalDensity.current
                        Column(modifier = Modifier.imePadding().navigationBarsPadding().padding(top = if (showToolbar.value){0.dp}else{
                            with(density){
                                TopAppBarDefaults.windowInsets.getTop(density).toDp()
                            }
                        })) {
                            
                            // ★ ★ ★ ENTWEDER DOWNLOADER ODER TERMINAL ★ ★ ★
                            if (Rootfs.isDownloaded.value) {
                                // ★ ★ ★ TERMINAL ANSICHT ★ ★ ★
                                AndroidView(
                                    factory = { context ->
                                        TerminalView(context, null).apply {
                                            terminalView = WeakReference(this)
                                            setTextSize(
                                                dpToPx(
                                                    Settings.terminal_font_size.toFloat(),
                                                    context
                                                )
                                            )
                                            val client = TerminalBackEnd(this, mainActivityActivity)

                                            val session = if (pendingCommand != null) {
                                                mainActivityActivity.sessionBinder!!.getService().currentSession.value = Pair(
                                                    pendingCommand!!.id, pendingCommand!!.workingMode)
                                                mainActivityActivity.sessionBinder!!.getSession(
                                                    pendingCommand!!.id
                                                )
                                                    ?: mainActivityActivity.sessionBinder!!.createSession(
                                                        pendingCommand!!.id,
                                                        client,
                                                        mainActivityActivity, workingMode = Settings.working_Mode
                                                    )
                                            } else {
                                                mainActivityActivity.sessionBinder!!.getSession(
                                                    mainActivityActivity.sessionBinder!!.getService().currentSession.value.first
                                                )
                                                    ?: mainActivityActivity.sessionBinder!!.createSession(
                                                        mainActivityActivity.sessionBinder!!.getService().currentSession.value.first,
                                                        client,
                                                        mainActivityActivity,workingMode = Settings.working_Mode
                                                    )
                                            }

                                            session.updateTerminalSessionClient(client)
                                            attachSession(session)
                                            setTerminalViewClient(client)
                                            setTypeface(font)

                                            post {
                                                val color = getViewColor()
                                                keepScreenOn = true
                                                requestFocus()
                                                isFocusableInTouchMode = true

                                                mEmulator?.mColors?.mCurrentColors?.apply {
                                                    set(256, color)
                                                    set(258, color)
                                                }

                                                val colorsFile = localDir().child("colors.properties")
                                                if (colorsFile.exists() && colorsFile.isFile){
                                                    val props = Properties()
                                                    FileInputStream(colorsFile).use { input ->
                                                        props.load(input)
                                                    }
                                                    TerminalColors.COLOR_SCHEME.updateWith(props)
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    update = { terminalView ->
                                        terminalView.onScreenUpdated()
                                        val color = getViewColor()
                                        terminalView.mEmulator?.mColors?.mCurrentColors?.apply {
                                            set(256, color)
                                            set(258, color)
                                        }
                                    },
                                )

                                // ★ VIRTUALKEYS (nur im Terminal-Modus)
                                if (showVirtualKeys.value){
                                    val pagerState = rememberPagerState(pageCount = { 2 })
                                    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(75.dp)
                                    ) { page ->
                                        when (page) {
                                            0 -> {
                                                terminalView.get()?.requestFocus()
                                                AndroidView(
                                                    factory = { context ->
                                                        VirtualKeysView(context, null).apply {
                                                            virtualKeysView = WeakReference(this)
                                                            virtualKeysViewClient =
                                                                terminalView.get()?.mTermSession?.let {
                                                                    VirtualKeysListener(it)
                                                                }
                                                            buttonTextColor = onSurfaceColor!!
                                                            reload(
                                                                VirtualKeysInfo(
                                                                    VIRTUAL_KEYS,
                                                                    "",
                                                                    VirtualKeysConstants.CONTROL_CHARS_ALIASES
                                                                )
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(75.dp)
                                                )
                                            }
                                            1 -> {
                                                var text by rememberSaveable { mutableStateOf("") }
                                                AndroidView(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(75.dp),
                                                    factory = { ctx ->
                                                        EditText(ctx).apply {
                                                            maxLines = 1
                                                            isSingleLine = true
                                                            imeOptions = EditorInfo.IME_ACTION_DONE
                                                            doOnTextChanged { textInput, _, _, _ ->
                                                                text = textInput.toString()
                                                            }
                                                            setOnEditorActionListener { v, actionId, event ->
                                                                if (actionId == EditorInfo.IME_ACTION_DONE) {
                                                                    if (text.isEmpty()) {
                                                                        val eventDown = KeyEvent(
                                                                            KeyEvent.ACTION_DOWN,
                                                                            KeyEvent.KEYCODE_ENTER
                                                                        )
                                                                        val eventUp = KeyEvent(
                                                                            KeyEvent.ACTION_UP,
                                                                            KeyEvent.KEYCODE_ENTER
                                                                        )
                                                                        terminalView.get()
                                                                            ?.dispatchKeyEvent(eventDown)
                                                                        terminalView.get()
                                                                            ?.dispatchKeyEvent(eventUp)
                                                                    } else {
                                                                        terminalView.get()?.currentSession?.write(
                                                                            text
                                                                        )
                                                                        setText("")
                                                                    }
                                                                    true
                                                                } else {
                                                                    false
                                                                }
                                                            }
                                                        }
                                                    },
                                                    update = { editText ->
                                                        if (editText.text.toString() != text) {
                                                            editText.setText(text)
                                                            editText.setSelection(text.length)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    virtualKeysView = WeakReference(null)
                                }
                            } else {
                                // ★ ★ ★ DOWNLOADER ANSICHT (Spinner + Fortschritt) ★ ★ ★
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // ★ Spinner
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(80.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 6.dp
                                    )
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    // ★ Status-Text
                                    Text(
                                        text = progressText,
                                        color = color,
                                        style = MaterialTheme.typography.headlineSmall,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // ★ Fortschrittsbalken
                                    LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .height(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // ★ Prozentzahl
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        color = color.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    
                                    // ★ Fehlermeldung
                                    if (errorMessage != null) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "❌ $errorMessage",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

// ============================================================
// ★ BESTEHENDE FUNKTIONEN (unverändert)
// ============================================================
var wallAlpha by mutableFloatStateOf(Settings.wallTransparency)

@Composable
fun BackgroundImage() {
    bitmap.value?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(wallAlpha)
                .zIndex(-1f)
        )
    }
}

@Composable
fun SetStatusBarTextColor(isDarkIcons: Boolean) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return
    SideEffect {
        WindowCompat.getInsetsController(window, view)?.isAppearanceLightStatusBars = isDarkIcons
    }
}

@Composable
fun SelectableCard(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        label = "containerColor"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 8.dp else 2.dp
        ),
        enabled = enabled,
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

fun changeSession(mainActivityActivity: MainActivity, session_id: String) {
    terminalView.get()?.apply {
        val client = TerminalBackEnd(this, mainActivityActivity)
        val session =
            mainActivityActivity.sessionBinder!!.getSession(session_id)
                ?: mainActivityActivity.sessionBinder!!.createSession(
                    session_id,
                    client,
                    mainActivityActivity,workingMode = Settings.working_Mode
                )
        session.updateTerminalSessionClient(client)
        attachSession(session)
        setTerminalViewClient(client)
        post {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(
                R.attr.colorOnSurface,
                typedValue,
                true
            )
            keepScreenOn = true
            requestFocus()
            isFocusableInTouchMode = true
            mEmulator?.mColors?.mCurrentColors?.apply {
                set(256, typedValue.data)
                set(258, typedValue.data)
            }
        }
        virtualKeysView.get()?.apply {
            virtualKeysViewClient =
                terminalView.get()?.mTermSession?.let { VirtualKeysListener(it) }
        }
    }
    mainActivityActivity.sessionBinder!!.getService().currentSession.value = Pair(session_id,mainActivityActivity.sessionBinder!!.getService().sessionList[session_id]!!)
}

const val VIRTUAL_KEYS =
    ("[" + "\n  [" + "\n    \"ESC\"," + "\n    {" + "\n      \"key\": \"/\"," + "\n      \"popup\": \"\\\\\"" + "\n    }," + "\n    {" + "\n      \"key\": \"-\"," + "\n      \"popup\": \"|\"" + "\n    }," + "\n    \"HOME\"," + "\n    \"UP\"," + "\n    \"END\"," + "\n    \"PGUP\"" + "\n  ]," + "\n  [" + "\n    \"TAB\"," + "\n    \"CTRL\"," + "\n    \"ALT\"," + "\n    \"LEFT\"," + "\n    \"DOWN\"," + "\n    \"RIGHT\"," + "\n    \"PGDN\"" + "\n  ]" + "\n]")