// core/main/src/main/java/com/rk/terminal/ui/screens/terminal/TerminalLogik.kt
package com.rk.terminal.ui.screens.terminal

import android.content.Context
import android.os.Build
import com.rk.libcommons.child
import com.meinname.ssh.Globals
import com.rk.terminal.ui.screens.terminal.Rootfs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.UnknownHostException

// ============================================================
// ★ DOWNLOADER DATENKLASSEN
// ============================================================
private data class CopyFile(val assetPath: String, val outputFile: File)
private data class DownloadFile(val url: String, val outputFile: File)

private data class AssetFiles(
    val privateTar: String,
    val ubuntuTar: String,
    val alpineTar: String,
    val remAlpine: String,
    val remUbuntu: String,
    val diskUsage: String
)

private data class AbiUrls(
    val talloc: String,
    val proot: String
)

// ============================================================
// ★ DOWNLOADER MAPS
// ============================================================
private val assetFilesMap = mapOf(
    "x86_64" to AssetFiles(
        privateTar = "private.tar",
        ubuntuTar = "ubuntu/ubuntu.tar",
        alpineTar = "alpine/alpine.tar",
        remAlpine = "alpine/rem_alpine5.sh",
        remUbuntu = "ubuntu/rem_ubuntu2.sh",
        diskUsage = "disk_usage.sh"
    ),
    "arm64-v8a" to AssetFiles(
        privateTar = "private.tar",
        ubuntuTar = "ubuntu/ubuntu.tar",
        alpineTar = "alpine/alpine.tar",
        remAlpine = "alpine/rem_alpine5.sh",
        remUbuntu = "ubuntu/rem_ubuntu2.sh",
        diskUsage = "disk_usage.sh"
    ),
    "armeabi-v7a" to AssetFiles(
        privateTar = "private.tar",
        ubuntuTar = "ubuntu/ubuntu.tar",
        alpineTar = "alpine/alpine.tar",
        remAlpine = "alpine/rem_alpine5.sh",
        remUbuntu = "ubuntu/rem_ubuntu2.sh",
        diskUsage = "disk_usage.sh"
    )
)

private val abiMap = mapOf(
    "x86_64" to AbiUrls(
        talloc = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/x86_64/libtalloc.so.2",
        proot = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/x86_64/proot",
    ),
    "arm64-v8a" to AbiUrls(
        talloc = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/aarch64/libtalloc.so.2",
        proot = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/aarch64/proot",
    ),
    "armeabi-v7a" to AbiUrls(
        talloc = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/arm/libtalloc.so.2",
        proot = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/arm/proot",
    )
)

// ============================================================
// ★ HELPER-FUNKTIONEN
// ============================================================
fun getNameOfWorkingMode(workingMode: Int?): String {
    return when (workingMode) {
        Globals.WORKING_MODE_ALPINE -> "ALPINE"
        Globals.WORKING_MODE_UBUNTU -> "UBUNTU"
        Globals.WORKING_MODE_ANDROID -> "ANDROID"
        null -> "null"
        else -> "unknown"
    }
}

fun generateUniqueSessionId(existingStrings: List<String>): String {
    var index = 1
    var newString: String
    do {
        newString = "main$index"
        index++
    } while (newString in existingStrings)
    return newString
}

// ============================================================
// ★ DOWNLOADER HILFSFUNKTIONEN
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
// ★ DOWNLOADER HAUPTFUNKTION
// ============================================================
suspend fun runDownloaderSetup(
    context: Context,
    onProgress: (Float, String) -> Unit,
    onComplete: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    try {
        Globals.showLog("TerminalLogik", "🚀 runDownloaderSetup gestartet")
        Globals.showLog("TerminalLogik", "📌 Rootfs-Typ: ${Globals.ROOTFS_TYPE}")
        Globals.showLog("TerminalLogik", "📌 Rootfs-Datei: ${Globals.ROOTFS_FILE}")

        // 1. ABI erkennen
        val abi = Build.SUPPORTED_ABIS.firstOrNull { it in abiMap }
            ?: throw RuntimeException("Unsupported CPU")
        onProgress(0.05f, "🔍 ABI erkannt: $abi")
        Globals.showLog("TerminalLogik", "✅ ABI: $abi")

        // ============================================================
        // 2. ALLE ASSETS KOPIEREN
        // ============================================================
        onProgress(0.1f, "📦 Kopiere Assets...")
        Globals.showLog("TerminalLogik", "📦 Kopiere Assets...")

        val filesToCopy = listOf(
            "private.tar" to assetFilesMap[abi]!!.privateTar,
            "ubuntu.tar.gz" to assetFilesMap[abi]!!.ubuntuTar,
            "alpine.tar.gz" to assetFilesMap[abi]!!.alpineTar,
            "rem_alpine.sh" to assetFilesMap[abi]!!.remAlpine,
            "rem_ubuntu.sh" to assetFilesMap[abi]!!.remUbuntu,
            "disk_usage.sh" to assetFilesMap[abi]!!.diskUsage
        ).mapNotNull { (name, assetPath) ->
            try {
                context.assets.open(assetPath).close()

                val targetFile = when (name) {
                    "rem_alpine.sh", "rem_ubuntu.sh", "disk_usage.sh" -> {
                        Rootfs.localDir.child(name)
                    }
                    else -> {
                        Rootfs.reTerminal.child(name)
                    }
                }

                CopyFile(assetPath, targetFile)
            } catch (e: IOException) {
                Globals.showLog("TerminalLogik", "⚠️ $assetPath nicht in Assets")
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
            Globals.showLog("TerminalLogik", "✅ Alle Assets bereits vorhanden")
        }

        // ============================================================
        // 3. LIBRARY DATEIEN KOPIEREN
        // ============================================================
        /*
        onProgress(0.3f, "📚 Kopiere Libraries...")
        Globals.showLog("TerminalLogik", "📚 Kopiere Libraries...")

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
                Globals.showLog("TerminalLogik", "📚 Kopiere $libName...")
            }
            libCompleted++
        }
        onProgress(0.5f, "✅ Libraries kopiert")
        Globals.showLog("TerminalLogik", "✅ Libraries kopiert")
        */ 
        // ic rausgenommen

        // ============================================================
        // 4. DATEIEN DOWNLOADEN
        // ============================================================
        onProgress(0.5f, "🌐 Starte Downloads...")
        Globals.showLog("TerminalLogik", "🌐 Starte Downloads...")

        val filesToDownload = listOf(
            "libtalloc.so.2" to abiMap[abi]!!.talloc,
            "proot" to abiMap[abi]!!.proot
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
            Globals.showLog("TerminalLogik", "✅ Alle Dateien bereits vorhanden")
        }

        Rootfs.refreshStatus()
        onProgress(1.0f, "✅ Setup abgeschlossen!")
        Globals.showLog("TerminalLogik", "✅ Setup abgeschlossen!")

        delay(300)

        onComplete(true)

    } catch (e: Exception) {
        if (e is UnknownHostException) {
            onError("Netzwerkfehler: ${e.message}")
            Globals.showLog("TerminalLogik", "❌ Netzwerkfehler: ${e.message}")
        } else {
            onError("Setup-Fehler: ${e.message}")
            Globals.showLog("TerminalLogik", "❌ Setup-Fehler: ${e.message}")
        }
        onComplete(false)
    }
}