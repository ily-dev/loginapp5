#define PY_SSIZE_T_CLEAN
#include "Python.h"
#ifndef Py_PYTHON_H
#error Python headers needed to compile C extensions, please install development version of Python.
#else

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <dirent.h>
#include <dlfcn.h>
#include <libgen.h>
#include <jni.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>
#include <android/log.h>
#include <time.h>

#include "bootstrap_name.h"

#ifdef BOOTSTRAP_NAME_SDL2
#include "SDL.h"
#include "SDL_opengles2.h"
#endif

#ifdef BOOTSTRAP_NAME_SDL3
#include "SDL3/SDL.h"
#include "SDL3/SDL_main.h"
#endif

// ★ ★ ★ JNI_OnLoad LOGGING ★ ★ ★
#define LOG_TAG "start.c"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

#define ENTRYPOINT_MAXLEN 128
#define LOG(n, x) __android_log_write(ANDROID_LOG_INFO, (n), (x))
#define P4A_MIN_VER 11

static JavaVM *cached_jvm = NULL;

// ============================================================
// ★ ★ ★ FUNKTIONSDEKLARATIONEN (um "implicit declaration" Fehler zu vermeiden)
// ============================================================
int main(int argc, char *argv[]);
void call_globals_showLog(JNIEnv* env, const char* title, const char* message);
static void LOGP(const char *fmt, ...);
static void LOG_TO_GLOBALS(const char *tag, const char *msg);
static void LOG_TIMESTAMP(const char *msg);
char *setup_symlinks();

// ============================================================
// ★ ★ ★ JNI-HILFSFUNKTION: Globals.showLog AUFRUFEN ★ ★ ★
// ============================================================
void call_globals_showLog(JNIEnv* env, const char* title, const char* message) {
    if (env == NULL) {
        LOGI("❌ JNIEnv ist NULL, kann showLog nicht aufrufen");
        return;
    }

    jclass globalsClass = (*env)->FindClass(env, "com/rk/terminal/Globals");
    if (globalsClass == NULL) {
        LOGE("❌ Globals Klasse nicht gefunden!");
        return;
    }

    jmethodID showLogMethod = (*env)->GetStaticMethodID(env, globalsClass, "showLog", "(Ljava/lang/String;Ljava/lang/String;)V");
    if (showLogMethod == NULL) {
        LOGE("❌ showLog Methode nicht gefunden!");
        return;
    }

    jstring jTitle = (*env)->NewStringUTF(env, title);
    jstring jMessage = (*env)->NewStringUTF(env, message);
    (*env)->CallStaticVoidMethod(env, globalsClass, showLogMethod, jTitle, jMessage);
    (*env)->DeleteLocalRef(env, jTitle);
    (*env)->DeleteLocalRef(env, jMessage);
    (*env)->DeleteLocalRef(env, globalsClass);
}

// ★ ★ ★ LOGP mit Globals.showLog ★ ★ ★
static void LOGP(const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    char buffer[1024];
    vsnprintf(buffer, sizeof(buffer), fmt, args);
    va_end(args);

    __android_log_vprint(ANDROID_LOG_INFO, "python", fmt, args);

    JNIEnv* env = NULL;
    if (cached_jvm != NULL) {
        if ((*cached_jvm)->GetEnv(cached_jvm, (void**)&env, JNI_VERSION_1_6) == JNI_OK) {
            call_globals_showLog(env, "start.c", buffer);
        }
    }
}

// ★ ★ ★ LOG TO GLOBALS ★ ★ ★
static void LOG_TO_GLOBALS(const char *tag, const char *msg) {
    __android_log_write(ANDROID_LOG_INFO, tag, msg);
    JNIEnv* env = NULL;
    if (cached_jvm != NULL) {
        if ((*cached_jvm)->GetEnv(cached_jvm, (void**)&env, JNI_VERSION_1_6) == JNI_OK) {
            call_globals_showLog(env, tag, msg);
        }
    }
}

// ★ ★ ★ DEBUG LOG MIT ZEITSTEMPEL ★ ★ ★
static void LOG_TIMESTAMP(const char *msg) {
    time_t now = time(NULL);
    char timestamp[64];
    strftime(timestamp, sizeof(timestamp), "%H:%M:%S", localtime(&now));
    LOGD("⏰ [%s] %s", timestamp, msg);
    LOG_TO_GLOBALS("start.c", msg);
}

// ★ ★ ★ JNI_OnLoad - WIRD AUTOMATISCH BEIM LADEN AUFGERUFEN ★ ★ ★
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    cached_jvm = vm;

    JNIEnv* env;
    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE("❌ JNI_OnLoad: GetEnv fehlgeschlagen");
        LOG_TO_GLOBALS("start.c", "❌ JNI_OnLoad: GetEnv failed");
        return JNI_ERR;
    }

    LOG_TIMESTAMP("🚀 JNI_OnLoad: start.c wird geladen!");
    LOG_TO_GLOBALS("start.c", "🔥 Native Bibliothek wird geladen");

    call_globals_showLog(env, "start.c JNI_OnLoad", "Native Bibliothek wird geladen");

    // ★ ★ ★ main() AUFRUFEN ★ ★ ★
    char *argv[] = {"Python_app", NULL};
    int result = main(1, argv);

    LOGI("✅ main() beendet mit Code: %d", result);
    LOG_TO_GLOBALS("start.c", "✅ main() completed");

    return JNI_VERSION_1_6;
}

// ============================================================
// ★ ★ ★ HELPER FUNKTIONEN ★ ★ ★
// ============================================================

static PyObject *androidembed_log(PyObject *self, PyObject *args) {
    char *logstr = NULL;
    if (!PyArg_ParseTuple(args, "s", &logstr)) {
        return NULL;
    }
    LOG_TO_GLOBALS(getenv("PYTHON_NAME"), logstr);
    Py_RETURN_NONE;
}

static PyMethodDef AndroidEmbedMethods[] = {
    {"log", androidembed_log, METH_VARARGS, "Log on android platform"},
    {NULL, NULL, 0, NULL}};

#if PY_MAJOR_VERSION >= 3
static struct PyModuleDef androidembed = {PyModuleDef_HEAD_INIT, "androidembed",
                                          "", -1, AndroidEmbedMethods};

PyMODINIT_FUNC initandroidembed(void) {
    return PyModule_Create(&androidembed);
}
#else
PyMODINIT_FUNC initandroidembed(void) {
    (void)Py_InitModule("androidembed", AndroidEmbedMethods);
}
#endif

int dir_exists(char *filename) {
    struct stat st;
    if (stat(filename, &st) == 0) {
        if (S_ISDIR(st.st_mode))
            return 1;
    }
    return 0;
}

int file_exists(const char *filename) {
    return access(filename, F_OK) == 0;
}

static void get_dirname(const char *path, char *dir, size_t size) {
    strncpy(dir, path, size - 1);
    dir[size - 1] = '\0';
    char *last_slash = strrchr(dir, '/');
    if (last_slash) *last_slash = '\0';
    else dir[0] = '\0';
}

// strip "lib" prefix and "bin.so" suffix
static void get_exe_name(const char *filename, char *out, size_t size) {
    size_t len = strlen(filename);
    if (len < 7) {
        strncpy(out, filename, size - 1);
        out[size - 1] = '\0';
        return;
    }

    const char *start = filename;
    if (strncmp(filename, "lib", 3) == 0) start += 3;
    size_t start_len = strlen(start);

    if (start_len > 6) {
        size_t copy_len = start_len - 6;
        if (copy_len >= size) copy_len = size - 1;
        strncpy(out, start, copy_len);
        out[copy_len] = '\0';
    } else {
        strncpy(out, start, size - 1);
        out[size - 1] = '\0';
    }
}

// ============================================================
// ★ ★ ★ SETUP_SYMLINKS ★ ★ ★
// ============================================================
char *setup_symlinks() {
    LOG_TIMESTAMP("📁 START: setup_symlinks() - Erstelle .bin Symlinks");
    LOG_TO_GLOBALS("start.c", "📁 START: Creating .bin symlinks");

    const char *files_dir_env = getenv("ANDROID_APP_PATH");
    if (files_dir_env == NULL) {
        LOGE("❌ setup_symlinks: ANDROID_APP_PATH is NULL");
        LOG_TO_GLOBALS("start.c", "❌ ANDROID_APP_PATH is NULL");
        return NULL;
    }
    LOGD("📁 ANDROID_APP_PATH = %s", files_dir_env);
    LOG_TO_GLOBALS("start.c", "📁 ANDROID_APP_PATH set");

    Dl_info info;
    char lib_path[512];
    char *interpreter = NULL;

    if (!(dladdr((void*)setup_symlinks, &info) && info.dli_fname)) {
        LOGE("❌ setup_symlinks: failed to get libdir");
        LOG_TO_GLOBALS("start.c", "❌ failed to get libdir");
        return interpreter;
    }

    strncpy(lib_path, info.dli_fname, sizeof(lib_path) - 1);
    lib_path[sizeof(lib_path) - 1] = '\0';
    LOGD("📁 lib_path = %s", lib_path);
    LOG_TO_GLOBALS("start.c", "📁 lib_path found");

    char native_lib_dir[512];
    get_dirname(lib_path, native_lib_dir, sizeof(native_lib_dir));
    if (native_lib_dir[0] == '\0') {
        LOGE("❌ setup_symlinks: could not determine lib directory");
        LOG_TO_GLOBALS("start.c", "❌ could not determine lib directory");
        return interpreter;
    }
    LOGD("📁 native_lib_dir = %s", native_lib_dir);
    LOG_TO_GLOBALS("start.c", "📁 native_lib_dir found");

    char bin_dir[512];
    snprintf(bin_dir, sizeof(bin_dir), "%s/.bin", files_dir_env);
    LOGD("📁 bin_dir = %s", bin_dir);
    LOG_TO_GLOBALS("start.c", "📁 bin_dir path");

    if (mkdir(bin_dir, 0755) != 0) {
        if (errno == EEXIST) {
            LOGD("✅ .bin directory already exists: %s", bin_dir);
            LOG_TO_GLOBALS("start.c", "✅ .bin directory exists");
        } else {
            LOGE("❌ Failed to create .bin directory: %s", strerror(errno));
            LOG_TO_GLOBALS("start.c", "❌ Failed to create .bin directory");
            return interpreter;
        }
    } else {
        LOGD("✅ Created .bin directory: %s", bin_dir);
        LOG_TO_GLOBALS("start.c", "✅ Created .bin directory");
    }

    DIR *dir = opendir(native_lib_dir);
    if (!dir) {
        LOGE("❌ Failed to open native lib dir: %s", strerror(errno));
        LOG_TO_GLOBALS("start.c", "❌ Failed to open native lib dir");
        return interpreter;
    }
    LOGD("✅ Opened native lib dir: %s", native_lib_dir);

    struct dirent *entry;
    int symlink_count = 0;
    LOGD("🔍 Scanning for *bin.so files...");
    LOG_TO_GLOBALS("start.c", "🔍 Scanning for bin.so files");

    while ((entry = readdir(dir)) != NULL) {
        const char *name = entry->d_name;
        size_t len = strlen(name);
        if (len < 7) continue;
        if (strcmp(name + len - 6, "bin.so") != 0) continue;

        char exe_name[128];
        get_exe_name(name, exe_name, sizeof(exe_name));
        LOGD("📄 Found: %s → %s", name, exe_name);
        LOG_TO_GLOBALS("start.c", "📄 Found bin.so file");

        char src[512], dst[512];
        snprintf(src, sizeof(src), "%s/%s", native_lib_dir, name);
        snprintf(dst, sizeof(dst), "%s/%s", bin_dir, exe_name);
  
        if (strcmp(exe_name, "python") == 0) {
            interpreter = strdup(dst);
            LOGD("🐍 Found Python interpreter: %s", dst);
            LOG_TO_GLOBALS("start.c", "🐍 Python interpreter found");
        }

        struct stat st;
        if (lstat(dst, &st) == 0) {
            LOGD("⏭️ Symlink already exists: %s", dst);
            continue;
        }
        if (symlink(src, dst) == 0) {
            LOGD("✅ Created symlink: %s → %s", name, exe_name);
            LOG_TO_GLOBALS("start.c", "✅ Symlink created");
            symlink_count++;
        } else {
            LOGE("❌ Symlink failed: %s → %s (errno: %d)", name, exe_name, errno);
            LOG_TO_GLOBALS("start.c", "❌ Symlink failed");
        }
    }

    closedir(dir);
    LOGD("✅ Created %d symlinks", symlink_count);
    LOG_TO_GLOBALS("start.c", "✅ Created symlinks");

    const char *old_path = getenv("PATH");
    char new_path[1024];
    if (old_path && strlen(old_path) > 0) {
        snprintf(new_path, sizeof(new_path), "%s:%s", old_path, bin_dir);
    } else {
        snprintf(new_path, sizeof(new_path), "%s", bin_dir);
    }
    setenv("PATH", new_path, 1);
    LOGD("🔧 PATH = %s", new_path);
    LOG_TO_GLOBALS("start.c", "🔧 PATH updated");

    setenv("LD_LIBRARY_PATH", native_lib_dir, 1);
    LOGD("🔧 LD_LIBRARY_PATH = %s", native_lib_dir);
    LOG_TO_GLOBALS("start.c", "🔧 LD_LIBRARY_PATH updated");

    LOG_TIMESTAMP("✅ END: setup_symlinks() completed");
    LOG_TO_GLOBALS("start.c", "✅ setup_symlinks completed");
    return interpreter;
}

// ============================================================
// ★ ★ ★ MAIN ★ ★ ★
// ============================================================

int main(int argc, char *argv[]) {
    LOG_TIMESTAMP("🚀 START: main() - Python for Android initializing");
    LOG_TO_GLOBALS("start.c", "🐍 Starting Python initialization");

    char *env_argument = NULL;
    char *env_entrypoint = NULL;
    char *env_logname = NULL;
    char entrypoint[ENTRYPOINT_MAXLEN];
    int ret = 0;
    FILE *fd;

    LOGP("Initializing Python for Android");

    setenv("P4A_BOOTSTRAP", bootstrap_name, 1);
    env_argument = getenv("ANDROID_ARGUMENT");
    if (env_argument != NULL) {
        LOGD("📁 ANDROID_ARGUMENT = %s", env_argument);
        LOG_TO_GLOBALS("start.c", "📁 ANDROID_ARGUMENT set");
    } else {
        LOGE("❌ ANDROID_ARGUMENT is NULL!");
        LOG_TO_GLOBALS("start.c", "❌ ANDROID_ARGUMENT is NULL");
    }
    setenv("ANDROID_APP_PATH", env_argument, 1);
    env_entrypoint = getenv("ANDROID_ENTRYPOINT");
    env_logname = getenv("PYTHON_NAME");
    if (!getenv("ANDROID_UNPACK")) {
        setenv("ANDROID_UNPACK", env_argument, 1);
    }
    if (env_logname == NULL) {
        env_logname = "python";
        setenv("PYTHON_NAME", "python", 1);
    }

    LOGD("📁 env_entrypoint = %s", env_entrypoint);
    LOG_TO_GLOBALS("start.c", "📁 Entrypoint set");

    LOGP("Setting additional env vars from p4a_env_vars.txt");
    char env_file_path[256];
    snprintf(env_file_path, sizeof(env_file_path),
             "%s/p4a_env_vars.txt", getenv("ANDROID_UNPACK"));
    FILE *env_file_fd = fopen(env_file_path, "r");
    if (env_file_fd) {
        LOGD("✅ Found p4a_env_vars.txt");
        LOG_TO_GLOBALS("start.c", "✅ p4a_env_vars.txt found");
        char* line = NULL;
        size_t len = 0;
        while (getline(&line, &len, env_file_fd) != -1) {
            if (strlen(line) > 0) {
                char *eqsubstr = strstr(line, "=");
                if (eqsubstr) {
                    size_t eq_pos = eqsubstr - line;
                    char env_name[256];
                    strncpy(env_name, line, sizeof(env_name));
                    env_name[eq_pos] = '\0';
                    char env_value[256];
                    strncpy(env_value, (char*)(line + eq_pos + 1), sizeof(env_value));
                    if (strlen(env_value) > 0 &&
                        env_value[strlen(env_value)-1] == '\n') {
                        env_value[strlen(env_value)-1] = '\0';
                        if (strlen(env_value) > 0 &&
                            env_value[strlen(env_value)-1] == '\r') {
                            env_value[strlen(env_value)-1] = '\0';
                        }
                    }
                    setenv(env_name, env_value, 1);
                    LOGD("🔧 %s = %s", env_name, env_value);
                }
            }
        }
        fclose(env_file_fd);
    } else {
        LOGW("⚠️ no p4a_env_vars.txt found!");
        LOG_TO_GLOBALS("start.c", "⚠️ no p4a_env_vars.txt found");
    }

    LOGP("Changing directory to '%s'", env_argument);
    chdir(env_argument);
    LOG_TO_GLOBALS("start.c", "📁 Changed directory");

    LOG_TIMESTAMP("📦 Calling setup_symlinks()...");
    char *interpreter = setup_symlinks();
    
    if (interpreter != NULL) {
        LOGD("✅ Python interpreter path: %s", interpreter);
        LOG_TO_GLOBALS("start.c", "✅ Python interpreter ready");
    } else {
        LOGE("❌ Python interpreter NOT found!");
        LOG_TO_GLOBALS("start.c", "❌ Python interpreter NOT found");
    }

#if PY_MAJOR_VERSION < 3
    Py_NoSiteFlag=1;
#endif

#if PY_MAJOR_VERSION >= 3
    PyImport_AppendInittab("androidembed", initandroidembed);
#endif

    LOGP("Preparing to initialize python");

    char python_bundle_dir[256];
    snprintf(python_bundle_dir, 256,
             "%s/_python_bundle", getenv("ANDROID_UNPACK"));
    LOGD("📁 python_bundle_dir = %s", python_bundle_dir);
    LOG_TO_GLOBALS("start.c", "📁 python_bundle_dir set");

#if PY_MAJOR_VERSION >= 3
    #if PY_MINOR_VERSION >= P4A_MIN_VER
        PyConfig config;
        PyConfig_InitPythonConfig(&config);
        config.program_name = L"android_python";
    #else
        Py_SetProgramName(L"android_python");
    #endif
#else
    Py_SetProgramName("android_python");
#endif

    if (dir_exists(python_bundle_dir)) {
        LOGD("✅ _python_bundle dir exists");
        LOG_TO_GLOBALS("start.c", "✅ _python_bundle dir exists");

        #if PY_MAJOR_VERSION >= 3
            #if PY_MINOR_VERSION >= P4A_MIN_VER
                wchar_t wchar_zip_path[256];
                wchar_t wchar_modules_path[256];
                swprintf(wchar_zip_path, 256, L"%s/stdlib.zip", python_bundle_dir);
                swprintf(wchar_modules_path, 256, L"%s/modules", python_bundle_dir);
                config.module_search_paths_set = 1;
                PyWideStringList_Append(&config.module_search_paths, wchar_zip_path);
                PyWideStringList_Append(&config.module_search_paths, wchar_modules_path);
            #else
                char paths[512];
                snprintf(paths, 512, "%s/stdlib.zip:%s/modules", python_bundle_dir, python_bundle_dir);
                wchar_t *wchar_paths = Py_DecodeLocale(paths, NULL);
                Py_SetPath(wchar_paths);
            #endif
        #endif
        LOGP("set wchar paths...");
    } else {
        LOGW("⚠️ _python_bundle does not exist!");
        LOG_TO_GLOBALS("start.c", "⚠️ _python_bundle does not exist");
    }

    LOG_TIMESTAMP("⚡ Initializing Python...");
    #if PY_MAJOR_VERSION >= 3 && PY_MINOR_VERSION >= P4A_MIN_VER
        PyStatus status = Py_InitializeFromConfig(&config);
        if (PyStatus_Exception(status)) {
            LOGE("❌ Python initialization failed: %s", status.err_msg);
            LOGP("Python initialization failed:");
            LOGP(status.err_msg);
            LOG_TO_GLOBALS("start.c", "❌ Python initialization failed");
        }
    #else
        Py_Initialize();
        LOGP("Python initialized using legacy Py_Initialize().");
        LOG_TO_GLOBALS("start.c", "✅ Python initialized");
    #endif

    LOG_TIMESTAMP("✅ Python initialized");
    LOG_TO_GLOBALS("start.c", "✅ Python initialized");

    #if PY_VERSION_HEX < 0x03090000
        LOGP("Initializing threads (required for Python < 3.9)");
        PyEval_InitThreads();
    #endif

#if PY_MAJOR_VERSION < 3
    initandroidembed();
#endif

    PyRun_SimpleString(
        "import androidembed\n"
        "androidembed.log('testing python print redirection')"
    );

    PyRun_SimpleString("import io, sys, posix\n");

    char add_site_packages_dir[256];

    if (dir_exists(python_bundle_dir)) {
        snprintf(add_site_packages_dir, 256,
                 "sys.path.append('%s/site-packages')",
                 python_bundle_dir);

        PyRun_SimpleString("import sys, os\n"
                          "from os.path import realpath, join, dirname");

        char buf_exec[512];
        char buf_argv[512];
        snprintf(buf_exec, sizeof(buf_exec), "sys.executable = '%s'\n", interpreter);
        snprintf(buf_argv, sizeof(buf_argv), "sys.argv = ['%s']\n", interpreter);
        PyRun_SimpleString(buf_exec);
        PyRun_SimpleString(buf_argv);

        PyRun_SimpleString(add_site_packages_dir);
        PyRun_SimpleString("sys.path = ['.'] + sys.path");
        PyRun_SimpleString("os.environ['PYTHONPATH'] = ':'.join(sys.path)");
        LOG_TO_GLOBALS("start.c", "✅ sys.path configured");
    }

    PyRun_SimpleString(
        "class LogFile(io.IOBase):\n"
        "    def __init__(self):\n"
        "        self.__buffer = ''\n"
        "    def readable(self):\n"
        "        return False\n"
        "    def writable(self):\n"
        "        return True\n"
        "    def write(self, s):\n"
        "        s = self.__buffer + s\n"
        "        lines = s.split('\\n')\n"
        "        for l in lines[:-1]:\n"
        "            androidembed.log(l.replace('\\x00', ''))\n"
        "        self.__buffer = lines[-1]\n"
        "sys.stdout = sys.stderr = LogFile()\n"
        "print('Android kivy bootstrap done. __name__ is', __name__)");

#if PY_MAJOR_VERSION < 3
    PyRun_SimpleString("import site; print site.getsitepackages()\n");
#endif

    char *dot = strrchr(env_entrypoint, '.');
    char *ext = ".pyc";
    if (dot <= 0) {
        LOGP("Invalid entrypoint, abort.");
        LOG_TO_GLOBALS("start.c", "❌ Invalid entrypoint");
        return -1;
    }
    if (strlen(env_entrypoint) > ENTRYPOINT_MAXLEN - 2) {
        LOGP("Entrypoint path is too long, try increasing ENTRYPOINT_MAXLEN.");
        LOG_TO_GLOBALS("start.c", "❌ Entrypoint too long");
        return -1;
    }
    if (!strcmp(dot, ext)) {
        if (!file_exists(env_entrypoint)) {
            strcpy(entrypoint, env_entrypoint);
            entrypoint[strlen(env_entrypoint) - 1] = '\0';
            LOGP(entrypoint);
            if (!file_exists(entrypoint)) {
                LOGP("Entrypoint not found (.pyc, fallback on .py), abort");
                LOG_TO_GLOBALS("start.c", "❌ Entrypoint not found (.pyc)");
                return -1;
            }
        } else {
            strcpy(entrypoint, env_entrypoint);
        }
    } else if (!strcmp(dot, ".py")) {
        strcpy(entrypoint, env_entrypoint);
        entrypoint[strlen(env_entrypoint) + 1] = '\0';
        entrypoint[strlen(env_entrypoint)] = 'c';
        if (!file_exists(entrypoint)) {
            if (!file_exists(env_entrypoint)) {
                LOGP("Entrypoint not found (.py), abort.");
                LOG_TO_GLOBALS("start.c", "❌ Entrypoint not found (.py)");
                return -1;
            }
            strcpy(entrypoint, env_entrypoint);
        }
    } else {
        LOGP("Entrypoint have an invalid extension (must be .py or .pyc), abort.");
        LOG_TO_GLOBALS("start.c", "❌ Invalid entrypoint extension");
        return -1;
    }
    
    LOGD("📁 Entrypoint = %s", entrypoint);
    LOG_TO_GLOBALS("start.c", "📁 Entrypoint selected");

    fd = fopen(entrypoint, "r");
    if (fd == NULL) {
        LOGP("Open the entrypoint failed");
        LOG_TO_GLOBALS("start.c", "❌ Open entrypoint failed");
        return -1;
    }
    
    LOG_TIMESTAMP("🚀 Running Python entrypoint");
    LOG_TO_GLOBALS("start.c", "🚀 Running Python entrypoint");
    
    ret = PyRun_SimpleFile(fd, entrypoint);
    fclose(fd);

    if (PyErr_Occurred() != NULL) {
        ret = 1;
        PyErr_Print();
        PyObject *f = PySys_GetObject("stdout");
        if (PyFile_WriteString("\n", f))
            PyErr_Clear();
        LOG_TO_GLOBALS("start.c", "❌ Python error occurred");
    }

    LOGP("Python for android ended.");
    LOG_TIMESTAMP("✅ Python for android ended");
    LOG_TO_GLOBALS("start.c", "✅ Python for android ended");

#if PY_MAJOR_VERSION < 3
    Py_Finalize();
    LOGP("Unexpectedly reached Py_FinalizeEx(), but was successful.");
#else
    if (Py_FinalizeEx() != 0) {
        LOGP("Unexpectedly reached Py_FinalizeEx(), and got error!");
        LOG_TO_GLOBALS("start.c", "❌ Py_FinalizeEx error");
    }
#endif

    exit(ret);
    return ret;
}

// ============================================================
// ★ ★ ★ JNI METHODEN ★ ★ ★
// ============================================================

JNIEXPORT void JNICALL Java_org_kivy_android_PythonService_nativeStart(
    JNIEnv *env,
    jobject thiz,
    jstring j_android_private,
    jstring j_android_argument,
    jstring j_service_entrypoint,
    jstring j_python_name,
    jstring j_python_home,
    jstring j_python_path,
    jstring j_arg) {
    LOG_TIMESTAMP("🔥 PythonService_nativeStart called");
    LOG_TO_GLOBALS("start.c", "🔥 PythonService_nativeStart");

    jboolean iscopy;
    const char *android_private =
        (*env)->GetStringUTFChars(env, j_android_private, &iscopy);
    const char *android_argument =
        (*env)->GetStringUTFChars(env, j_android_argument, &iscopy);
    const char *service_entrypoint =
        (*env)->GetStringUTFChars(env, j_service_entrypoint, &iscopy);
    const char *python_name =
        (*env)->GetStringUTFChars(env, j_python_name, &iscopy);
    const char *python_home =
        (*env)->GetStringUTFChars(env, j_python_home, &iscopy);
    const char *python_path =
        (*env)->GetStringUTFChars(env, j_python_path, &iscopy);
    const char *arg = (*env)->GetStringUTFChars(env, j_arg, &iscopy);

    LOGD("📁 android_private = %s", android_private);
    LOGD("📁 android_argument = %s", android_argument);
    LOGD("📁 service_entrypoint = %s", service_entrypoint);
    LOG_TO_GLOBALS("start.c", "📁 Service entrypoint set");

    setenv("ANDROID_PRIVATE", android_private, 1);
    setenv("ANDROID_ARGUMENT", android_argument, 1);
    setenv("ANDROID_APP_PATH", android_argument, 1);
    setenv("ANDROID_ENTRYPOINT", service_entrypoint, 1);
    setenv("PYTHONOPTIMIZE", "2", 1);
    setenv("PYTHON_NAME", python_name, 1);
    setenv("PYTHONHOME", python_home, 1);
    setenv("PYTHONPATH", python_path, 1);
    setenv("PYTHON_SERVICE_ARGUMENT", arg, 1);
    setenv("P4A_BOOTSTRAP", bootstrap_name, 1);

    char *argv[] = {"."};
    LOG_TIMESTAMP("🚀 Calling main() from service...");
    main(1, argv);
    LOG_TIMESTAMP("✅ Service main() completed");
}

#if defined(BOOTSTRAP_NAME_WEBVIEW) || defined(BOOTSTRAP_NAME_SERVICEONLY)

void Java_org_kivy_android_PythonActivity_nativeSetenv(
                                    JNIEnv* env, jclass cls,
                                    jstring name, jstring value)
{
    const char *utfname = (*env)->GetStringUTFChars(env, name, NULL);
    const char *utfvalue = (*env)->GetStringUTFChars(env, value, NULL);
    LOGD("🔧 nativeSetenv: %s = %s", utfname, utfvalue);
    setenv(utfname, utfvalue, 1);
    (*env)->ReleaseStringUTFChars(env, name, utfname);
    (*env)->ReleaseStringUTFChars(env, value, utfvalue);
}

void Java_org_kivy_android_PythonActivity_nativeInit(JNIEnv* env, jclass cls, jobject obj)
{
    LOG_TIMESTAMP("🔥 nativeInit called");
    LOG_TO_GLOBALS("start.c", "🔥 nativeInit");
    int status;
    char *argv[2];
    argv[0] = "Python_app";
    argv[1] = NULL;
    main(1, argv);
    LOG_TIMESTAMP("✅ nativeInit completed");
}
#endif

#endif