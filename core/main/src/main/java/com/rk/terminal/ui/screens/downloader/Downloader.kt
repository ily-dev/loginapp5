package com.rk.terminal.ui.screens.downloader

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rk.libcommons.*
import com.rk.resources.strings
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.screens.terminal.Rootfs
import com.rk.terminal.ui.screens.terminal.TerminalScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.UnknownHostException

@Composable
fun Downloader(
    modifier: Modifier = Modifier,
    mainActivity: MainActivity,
    navController: NavHostController
) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    
    val installingStr = stringResource(strings.installing)
    val networkErrorStr = stringResource(strings.network_error)
    val setupFailedStr = stringResource(strings.setup_failed)
    
    var progressText by remember { mutableStateOf(installingStr) }
    var isSetupComplete by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Fehler-Snackbar anzeigen, falls etwas schiefgeht
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, actionLabel = "OK")
            errorMessage = null
        }
    }

    LaunchedEffect(Unit) {
        try {
            val abi = Build.SUPPORTED_ABIS.firstOrNull { it in abiMap } 
                ?: throw RuntimeException("Unsupported CPU")
            
            // ============================================================
            // 1. DATEIEN KOPIEREN (Sequentiell)
            // ============================================================
            val filesToCopy = listOf(
                "private.tar" to assetFilesMap[abi]!!.privateTar
            ).map { (name, assetPath) -> CopyFile(assetPath, Rootfs.reTerminal.child(name)) }

            val needsCopy = filesToCopy.any { !it.outputFile.exists() }
            
            if (needsCopy) {
                var completedFiles = 0
                val totalFiles = filesToCopy.size

                filesToCopy.forEach { file ->
                    val outputFile = file.outputFile.apply { parentFile?.mkdirs() }
                    if (!outputFile.exists()) {
                        copyAssetFile(context, file.assetPath, outputFile) { copied, total ->
                            val currentProgress = if (total > 0) copied.toFloat() / total else 0f
                            progress = ((completedFiles + currentProgress) / totalFiles).coerceIn(0f, 1f)
                            progressText = "Kopiere ... ${(progress * 100).toInt()}%"
                        }
                    }
                    completedFiles++
                    progress = (completedFiles.toFloat() / totalFiles).coerceIn(0f, 1f)
                }
            }

            // ============================================================
            // 2. DATEIEN DOWNLOADEN (Erst wenn Kopieren fertig ist!)
            // ============================================================
            val filesToDownload = listOf(
                "libtalloc.so.2" to abiMap[abi]!!.talloc,
                "proot" to abiMap[abi]!!.proot,
                "alpine.tar.gz" to abiMap[abi]!!.alpine
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
                            progress = ((completedFiles + currentProgress) / totalFiles).coerceIn(0f, 1f)
                            progressText = "Downloading.. ${(progress * 100).toInt()}%"
                        }
                    }
                    completedFiles++
                    progress = (completedFiles.toFloat() / totalFiles).coerceIn(0f, 1f)
                    outputFile.setExecutable(true, false)
                }
            }

            // Alles erfolgreich erledigt!
            isSetupComplete = true

        } catch (e: Exception) {
            if (e is UnknownHostException) {
                errorMessage = networkErrorStr
            } else {
                errorMessage = setupFailedStr.format(e.message)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!isSetupComplete) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(progressText, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(0.8f))
            }
        } else {
            TerminalScreen(mainActivityActivity = mainActivity, navController = navController)
        }

        // Der korrekte Platz für die Snackbar im UI-Tree
        SnackbarHost(
            hostState = snackbarHostState, 
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private data class CopyFile(val assetPath: String, val outputFile: File)
private data class DownloadFile(val url: String, val outputFile: File)

// ============================================================
// KOPIER-LOGIK (Sicher gegen Kompression geschützt)
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
                // available() liefert bei Assets die geschätzte/tatsächliche Größe ohne openFd Absturz
                val totalBytes = input.available().toLong()
                var copiedBytes = 0L
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    copiedBytes += bytesRead
                    // Direktes Zurückmelden an den Main-Thread via State-Zuweisung im UI
                    withContext(Dispatchers.Main) {
                        onProgress(copiedBytes, totalBytes)
                    }
                }
            }
        }
    }
}

// ============================================================
// DOWNLOAD-LOGIK
// ============================================================
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

// Maps bleiben unverändert wie in deinem Code
private val assetFilesMap = mapOf(
    "x86_64" to AssetFiles(privateTar = "private.tar"),
    "arm64-v8a" to AssetFiles(privateTar = "private.tar"),
    "armeabi-v7a" to AssetFiles(privateTar = "private.tar")
)

private data class AssetFiles(val privateTar: String)

private val abiMap = mapOf(
    "x86_64" to AbiUrls(
        talloc = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/x86_64/libtalloc.so.2",
        proot = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/x86_64/proot",
        alpine = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/x86_64/alpine-minirootfs-3.21.0-x86_64.tar.gz"
    ),
    "arm64-v8a" to AbiUrls(
        talloc = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/aarch64/libtalloc.so.2",
        proot = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/aarch64/proot",
        alpine = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/aarch64/alpine-minirootfs-3.21.0-aarch64.tar.gz"
    ),
    "armeabi-v7a" to AbiUrls(
        talloc = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/arm/libtalloc.so.2",
        proot = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/arm/proot",
        alpine = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/armhf/alpine-minirootfs-3.21.0-armhf.tar.gz"
    )
)

private data class AbiUrls(val talloc: String, val proot: String, val alpine: String)
