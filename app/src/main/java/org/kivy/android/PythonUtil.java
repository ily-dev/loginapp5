package org.kivy.android;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.renpy.android.AssetExtract;
import org.kivy.android.PythonService;
import java.io.IOException;

public class PythonUtil {
    private static final String TAG = "pythonutil";

    protected static void addLibraryIfExists(
            ArrayList<String> libsList, String pattern, File libsDir) {
        // pattern should be the name of the lib file, without the
        // preceding "lib" or suffix ".so", for instance "ssl.*" will
        // match files of the form "libssl.*.so".
        File[] files = libsDir.listFiles();

        pattern = "lib" + pattern + "\\.so";
        Pattern p = Pattern.compile(pattern);
        for (int i = 0; i < files.length; ++i) {
            File file = files[i];
            String name = file.getName();
            showLog(TAG, "Checking pattern " + pattern + " against " + name);
            if (p.matcher(name).matches()) {
                showLog(TAG, "Pattern " + pattern + " matched file " + name);
                libsList.add(name.substring(3, name.length() - 3));
            }
        }
    }

    protected static ArrayList<String> getLibraries(File libsDir) {
        ArrayList<String> libsList = new ArrayList<>();

        String[] libNames = {
            "sqlite3",
            "ffi",
            "png16",
            "ssl.*",
            "crypto.*",
            "SDL2",
            "SDL2_image",
            "SDL2_mixer",
            "SDL2_ttf",
            "SDL3",
            "SDL3_image",
            "SDL3_mixer",
            "SDL3_ttf"
        };

        for (String name : libNames) {
            addLibraryIfExists(libsList, name, libsDir);
        }

        for (int v = 14; v >= 8; v--) {
            libsList.add("python3." + v);
        }

        libsList.add("main");
        return libsList;
    }

    public static void loadLibraries(File filesDir, File libsDir) {
        boolean foundPython = false;

        for (String lib : getLibraries(libsDir)) {
            if (lib.startsWith("python") && foundPython) {
                continue;
            }
            showLog(TAG, "Loading library: " + lib);
            try {
                System.loadLibrary(lib);
                if (lib.startsWith("python")) {
                    foundPython = true;
                }
            } catch (UnsatisfiedLinkError e) {
                // If this is the last possible libpython
                // load, and it has failed, give a more
                // general error
                showLog(TAG, "Library loading error: " + e.getMessage());
                if (lib.startsWith("python3.8") && !foundPython) {
                    throw new RuntimeException("Could not load any libpythonXXX.so");
                } else if (lib.startsWith("python")) {
                    continue;
                } else {
                    showLog(TAG, "An UnsatisfiedLinkError occurred loading " + lib);
                    throw e;
                }
            }
        }

        showLog(TAG, "Loaded everything!");
    }

    public static String getAppRoot(Context ctx) {
        String appRoot = ctx.getFilesDir().getAbsolutePath() + "/app";
        return appRoot;
    }

    public static String getResourceString(Context ctx, String name) {
        // Taken from org.renpy.android.ResourceManager
        Resources res = ctx.getResources();
        int id = res.getIdentifier(name, "string", ctx.getPackageName());
        return res.getString(id);
    }

    /** Show an error using a toast. (Only makes sense from non-UI threads.) */
    protected static void toastError(final Activity activity, final String msg) {
        activity.runOnUiThread(
                new Runnable() {
                    public void run() {
                        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                    }
                });

        // Wait to show the error.
        synchronized (activity) {
            try {
                activity.wait(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    protected static void recursiveDelete(File f) {
        if (f.isDirectory()) {
            for (File r : f.listFiles()) {
                recursiveDelete(r);
            }
        }
        f.delete();
    }

    public static void unpackAsset(
            Context ctx, final String resource, File target, boolean cleanup_on_version_update) {

        showLog(TAG, "Unpacking " + resource + " " + target.getName());

        // The version of data in memory and on disk.
        String dataVersion = getResourceString(ctx, resource + "_version");
        String diskVersion = null;

        showLog(TAG, "Data version is " + dataVersion);

        // If no version, no unpacking is necessary.
        if (dataVersion == null) {
            showLog(TAG, "✅ Asset schon vorhanden! ");
            return;
        }

        // Check the current disk version, if any.
        String filesDir = target.getAbsolutePath();
        String diskVersionFn = filesDir + "/" + resource + ".version";

        try {
            byte buf[] = new byte[64];
            InputStream is = new FileInputStream(diskVersionFn);
            int len = is.read(buf);
            diskVersion = new String(buf, 0, len);
            is.close();
        } catch (Exception e) {
            diskVersion = "";
        }

        // If the disk data is out of date, extract it and write the version file.
        if (!dataVersion.equals(diskVersion)) {
            showLog(TAG, "Extracting " + resource + " assets.");

            if (cleanup_on_version_update) {
                recursiveDelete(target);
            }
            target.mkdirs();

            AssetExtract ae = new AssetExtract(ctx);
            if (!ae.extractTar(resource + ".tar", target.getAbsolutePath(), "private")) {
                String msg = "Could not extract " + resource + " data.";
                if (ctx instanceof Activity) {
                    toastError((Activity) ctx, msg);
                } else {
                    showLog(TAG, msg);
                }
            }

            try {
                // Write .nomedia.
                new File(target, ".nomedia").createNewFile();

                // Write version file.
                FileOutputStream os = new FileOutputStream(diskVersionFn);
                os.write(dataVersion.getBytes());
                os.close();
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }
    }
    
    // Von private zu public static
    public static void showLog(String tag, String message) {
    
    //"PythonUtil"

    // Dann einfach:
    PythonService.showLog( tag, message);
    
    }

    public static void unpackPyBundle(
            Context ctx, final String resource, File target, boolean cleanup_on_version_update) {

        showLog(TAG, "Unpacking " + resource + " " + target.getName());

        // The version of data in memory and on disk.
        String dataVersion = getResourceString(ctx, "private_version");
        String diskVersion = null;

        showLog(TAG, "Data version is " + dataVersion);

        // If no version, no unpacking is necessary.
        if (dataVersion == null) {
            showLog(TAG, " ✅ schon vorhanden nicht entpacken !");
            return;
        }

        // Check the current disk version, if any.
        String filesDir = target.getAbsolutePath();
        String diskVersionFn = filesDir + "/" + "libpybundle" + ".version";

        try {
            byte buf[] = new byte[64];
            InputStream is = new FileInputStream(diskVersionFn);
            int len = is.read(buf);
            diskVersion = new String(buf, 0, len);
            is.close();
        } catch (Exception e) {
            diskVersion = "";
        }

        if (!dataVersion.equals(diskVersion)) {
            // If the disk data is out of date, extract it and write the version file.
            showLog(TAG, "Extracting " + resource + " assets.");

            if (cleanup_on_version_update) {
                recursiveDelete(target);
            }
            target.mkdirs();

            AssetExtract ae = new AssetExtract(ctx);
            if (!ae.extractTar(resource + ".so", target.getAbsolutePath(), "pybundle")) {
                String msg = "Could not extract " + resource + " data.";
                if (ctx instanceof Activity) {
                    toastError((Activity) ctx, msg);
                } else {
                    showLog(TAG, msg);
                }
            }

            try {
                // Write version file.
                FileOutputStream os = new FileOutputStream(diskVersionFn);
                os.write(dataVersion.getBytes());
                os.close();
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }
    }
    
    public static void copyQtLibraries(Context ctx, File appRootDir) {
        String nativeLibDir = ctx.getApplicationInfo().nativeLibraryDir;
        File modulesDir = new File(appRootDir, "_python_bundle/modules");
        if (!modulesDir.exists()) {
            modulesDir.mkdirs();
            showLog(TAG, "Created modules directory: " + modulesDir.getAbsolutePath());
        }
    
        // Quelle: Name im APK → Ziel: Name im modules
        String[][] libs = {
            {"libQt6Core.so", "libQt6Core.so"},
            {"libQt6Gui.so", "libQt6Gui.so"},
            {"libQt6Widgets.so", "libQt6Widgets.so"},
            {"Shiboken.abi3.so", "Shiboken.abi3.so"},
            
            {"libstdc++.so", "libstdc++.so.6"}  // ← Umbenennung!
        };
    
        for (String[] pair : libs) {
            String srcName = pair[0];
            String dstName = pair[1];
            File src = new File(nativeLibDir, srcName);
            File dst = new File(modulesDir, dstName);
            
            if (src.exists()) {
                try {
                    FileInputStream fis = new FileInputStream(src);
                    FileOutputStream fos = new FileOutputStream(dst);
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fis.close();
                    fos.close();
                    showLog(TAG, "✅ Kopiert: " + srcName + " -> " + dst.getAbsolutePath());
                } catch (IOException e) {
                    showLog(TAG, "❌ Fehler beim Kopieren von " + srcName + ": " + e.getMessage());
                }
            } else {
                showLog(TAG, "⚠️ Quelldatei nicht gefunden: " + src.getAbsolutePath());
            }
        }
    }
    
}
