import java.util.Properties
import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import java.security.DigestInputStream
import java.security.MessageDigest
import java.math.BigInteger

// Lokale Gradle-Variable aus dem p4a-Package-Namen generieren
val appName = "loginapp3"

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    // Platzhalter für dynamische p4a-Plugins
    
}

android {
    // Dynamischer Namespace basierend auf deiner buildozer.spec Konfiguration
    namespace = "com.meinname.loginapp3"
    compileSdk = 36

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            isCrunchPngs = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            resValue("string", "app_name", appName)
            
            // ZUSÄTZLICH: Wir merken uns den Wunschnamen für Release
            project.ext.set("apkName", "loginapp3".lowercase().replace(" ", "_") + "-release.apk")
        }
        
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            resValue("string", "app_name", "$appName-Debug")
            
            // ZUSÄTZLICH: Wir merken uns den Wunschnamen für Debug
            project.ext.set("apkName", "loginapp3".lowercase().replace(" ", "_") + "-debug.apk")
        }
        
        
    }
    
    defaultConfig {
        applicationId = "com.meinname.loginapp3"
        minSdk = 26
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 28

        // Versioning
        versionCode = 8
        versionName = "1.2.1"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // App fest auf 64-Bit ARM beschränken
        ndk {
            abiFilters.add("arm64-v8a")
        }
        
        // p4a-spezifische Konfigurationen (wie z.B. manifestPlaceholders)
        
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    buildFeatures {
        viewBinding = true
        compose = true
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
	
    sourceSets {
        getByName("main") {
            // Nur noch die lokalen Ressourcen der App einbinden
            res.srcDirs("src/main/res")
            
            // Nur die lokalen Java/Kotlin-Dateien kompilieren
            java.srcDirs(
                "src/main/java"
                // Platzhalter für Java-Quellen, die p4a dynamisch generiert (z.B. PythonActivity)
                
            )
            
            // Lokale JNI-Bibliotheken
            jniLibs.srcDirs(
                "src/main/jniLibs",
                "../libs"
                
            )
            
            // Lokale Assets
            assets.srcDirs(
                "src/main/assets"
                
            )
        }
        
    }
    
    // Ermöglicht p4a das Anhängen weiterer interner Android-Konfigurationen
    
}

dependencies {
    // Verknüpft dein Haupt-Core-Projekt (das bringt Compose, PRoot und Co. mit)
    // Verknüpft dein Haupt-Core-Projekt
    implementation(project(":core:main"))
    
    //SSHClient im apk einfuegen
    implementation(project(":sshclient"))

    // Platzhalter für Bibliotheken, die p4a über seine Rezepte (Recipes) anfordert
    
}

// =============================================================================
// PREBUILT PROOT LOADER DOWNLOADER LOGIC
// =============================================================================

fun downloadFile(localUrl: String, remoteUrl: String, expectedChecksum: String) {
    val digest = MessageDigest.getInstance("SHA-256")

    val file = File(projectDir, localUrl)
    if (file.exists()) {
        val buffer = ByteArray(8192)
        val input = FileInputStream(file)
        while (true) {
            val readBytes = input.read(buffer)
            if (readBytes < 0) break
            digest.update(buffer, 0, readBytes)
        }
        input.close()
        var checksum = BigInteger(1, digest.digest()).toString(16)
        while (checksum.length < 64) { checksum = "0$checksum" }
        if (checksum == expectedChecksum) {
            return
        } else {
            logger.warn("Deleting old local file with wrong hash: $localUrl: expected: $expectedChecksum, actual: $checksum")
            file.delete()
        }
    }

    logger.quiet("Downloading $remoteUrl ...")

    file.parentFile.mkdirs()
    val out = BufferedOutputStream(FileOutputStream(file))

    val connection = URI(remoteUrl).toURL().openConnection()
    val digestStream = DigestInputStream(connection.inputStream, digest)
    digestStream.transferTo(out)
    out.close()
    digestStream.close()

    var checksum = BigInteger(1, digest.digest()).toString(16)
    while (checksum.length < 64) { checksum = "0$checksum" }
    if (checksum != expectedChecksum) {
        file.delete()
        throw GradleException("Wrong checksum for $remoteUrl:\n Expected: $expectedChecksum\n Actual:   $checksum")
    }
}

tasks.register("downloadPrebuilt") {
    doLast {
        val prootTag = "proot-2025.01.15-r2"
        val prootVersion = "5.1.107-66"
        var prootUrl = "https://github.com/termux-play-store/termux-packages/releases/download/${prootTag}/libproot-loader-ARCH-${prootVersion}.so"

        downloadFile("src/main/jniLibs/armeabi-v7a/libproot-loader.so", prootUrl.replace("ARCH", "arm"), "eb1d64e9ef875039534ce7a8eeffa61bbc4c0ae5722cb48c9112816b43646a3e")
        downloadFile("src/main/jniLibs/arm64-v8a/libproot-loader.so", prootUrl.replace("ARCH", "aarch64"), "8814b72f760cd26afe5350a1468cabb6622b4871064947733fcd9cd06f1c8cb8")
        downloadFile("src/main/jniLibs/x86_64/libproot-loader.so", prootUrl.replace("ARCH", "x86_64"), "1a52cc9cc5fdecbf4235659ffeac8c51e4fefd7c75cc205f52d4884a3a0a0ba1")
        prootUrl = "https://github.com/termux-play-store/termux-packages/releases/download/${prootTag}/libproot-loader32-ARCH-${prootVersion}.so"
        downloadFile("src/main/jniLibs/arm64-v8a/libproot-loader32.so", prootUrl.replace("ARCH", "aarch64"), "ff56a5e3a37104f6778420d912e3edf31395c15d1528d28f0eb7d13a64481b99")
        downloadFile("src/main/jniLibs/x86_64/libproot-loader32.so", prootUrl.replace("ARCH", "x86_64"), "5460a597e473f57f0d33405891e35ca24709173ca0a38805d395e3544ab8b1b4")
    }
}

afterEvaluate {
    android.applicationVariants.all { variant ->
        variant.javaCompileProvider.dependsOn("downloadPrebuilt")
        true
    }
}


// =============================================================================
// UNIVERSELLER AUTOMATISCHER KOPIER-TASK
// =============================================================================

tasks.register("copyApkToProjectBin") {
    mustRunAfter("assemble")
    doLast {
        val apkBuildDir = file("build/outputs/apk")
        if (apkBuildDir.exists()) {
            apkBuildDir.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "apk") {
                    val projectBinDir = file("${project.rootDir}/bin")
                    projectBinDir.mkdirs()
                    
                    // HIER den Namen ändern:
                    val targetFile = file("${projectBinDir}/loginapp3-debug.apk")
                    file.copyTo(targetFile, overwrite = true)
                    
                    println("🎉 APK kopiert als: loginapp3-debug.apk")
                }
            }
        }
    }
}

// Copy-Task aktivieren:
tasks.matching { it.name.startsWith("assemble") }.configureEach {
    finalizedBy("copyApkToProjectBin")
}

// Extra-Platzhalter für zusätzliche Build-Logik am Ende der Datei
