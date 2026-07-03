package com.meinname.loginapp3

import android.content.Context
import java.io.File

object FileUtil {
    
    // Holt dynamisch das korrekte interne files-Verzeichnis DEINER App
    fun getFilesDir(context: Context): File {
        return context.filesDir
    }

    // Entspricht dem "local"-Ordner im ursprünglichen Code
    fun localDir(context: Context): File {
        val file = File(getFilesDir(context).parentFile, "local")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    // Entspricht dem "alpine"-Ordner
    fun alpineDir(context: Context): File {
        val file = File(localDir(context), "alpine")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    // DAS ist die Methode, die dein Provider aufruft!
    fun alpineHomeDir(context: Context): File {
        val file = File(alpineDir(context), "root")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }
}