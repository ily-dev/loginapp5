ALPINE_DIR=$PREFIX/local/alpine
APP_FILES_DIR=$PREFIX/files
LOCAL_DIR=$PREFIX/local
PROFILE=$PREFIX/local/alpine/root/.profile
TARFILE_ALPINE=$PREFIX/files/alpine.tar.gz

if [ ! -d $ALPINE_DIR ]; then
    #ordner local/alpine erstellen
    mkdir -p $ALPINE_DIR
fi

if [ -z "$(ls -A "$ALPINE_DIR" | grep -vE '^(root|tmp)$')" ]; then
    tar -xf $TARFILE_ALPINE -C "$ALPINE_DIR"
    #tarfile von files loeschen
    rm $TARFILE_ALPINE
    #chmod local (nur einmal beim start)
    chmod -R 755 $LOCAL_DIR
fi

[ ! -e "$PREFIX/local/bin/proot" ] && cp "$PREFIX/files/proot" "$PREFIX/local/bin"

for sofile in "$PREFIX/files/"*.so.2; do
    dest="$PREFIX/local/lib/$(basename "$sofile")"
    [ ! -e "$dest" ] && cp "$sofile" "$dest"
done


ARGS="--kill-on-exit"
ARGS="$ARGS -w /"

for system_mnt in /apex /odm /product /system /system_ext /vendor \
 /linkerconfig/ld.config.txt \
 /linkerconfig/com.android.art/ld.config.txt \
 /plat_property_contexts /property_contexts; do

 if [ -e "$system_mnt" ]; then
  system_mnt=$(realpath "$system_mnt")
  ARGS="$ARGS -b ${system_mnt}"
 fi
done
unset system_mnt

ARGS="$ARGS -b /sdcard"
ARGS="$ARGS -b /storage"
ARGS="$ARGS -b /dev"
ARGS="$ARGS -b /data"
ARGS="$ARGS -b /dev/urandom:/dev/random"
ARGS="$ARGS -b /proc"
ARGS="$ARGS -b $PREFIX"
ARGS="$ARGS -b $PREFIX/local/stat:/proc/stat"
ARGS="$ARGS -b $PREFIX/local/vmstat:/proc/vmstat"


# ============================================================
# LOCAL VERZEICHNISSE UND DATEIEN MOUNTEN
# ============================================================

# 1. ALLE ORDNER mounten (außer alpine und ubuntu)
for dir in "$LOCAL_DIR"/*; do
    if [ -d "$dir" ]; then
        basename=$(basename "$dir")
        if [ "$basename" != "alpine" ] && [ "$basename" != "ubuntu" ]; then
            ARGS="$ARGS -b $dir:/root/local/$basename"
            #echo "📁 Mount (Ordner): $dir → /root/local/$basename"
        fi
    fi
done

# 2. ★ ★ ★ ALLE DATEIEN mounten ★ ★ ★
for file in "$LOCAL_DIR"/*; do
    if [ -f "$file" ]; then
        basename=$(basename "$file")
        ARGS="$ARGS -b $file:/root/local/$basename"
        #echo "📄 Mount (Datei): $file → /root/local/$basename"
    fi
done


if [ -d "$APP_FILES_DIR" ]; then
    ARGS="$ARGS -b $APP_FILES_DIR:/root/app_files"
    echo -e "\e[32;1m[+] \e[0mApp-Verzeichnis eingebunden $APP_FILES_DIR --> /root/app_files \e[0m"
else
    echo "⚠️ App-Verzeichnis nicht gefunden: $APP_FILES_DIR"
fi



if [ -e "/proc/self/fd" ]; then
  ARGS="$ARGS -b /proc/self/fd:/dev/fd"
fi

if [ -e "/proc/self/fd/0" ]; then
  ARGS="$ARGS -b /proc/self/fd/0:/dev/stdin"
fi

if [ -e "/proc/self/fd/1" ]; then
  ARGS="$ARGS -b /proc/self/fd/1:/dev/stdout"
fi

if [ -e "/proc/self/fd/2" ]; then
  ARGS="$ARGS -b /proc/self/fd/2:/dev/stderr"
fi

ARGS="$ARGS -b $PREFIX"
ARGS="$ARGS -b /sys"

if [ ! -d "$PREFIX/local/alpine/tmp" ]; then
 mkdir -p "$PREFIX/local/alpine/tmp"
 chmod 1777 "$PREFIX/local/alpine/tmp"
fi
ARGS="$ARGS -b $PREFIX/local/alpine/tmp:/dev/shm"

ARGS="$ARGS -r $PREFIX/local/alpine"
ARGS="$ARGS -0"
ARGS="$ARGS --link2symlink"
ARGS="$ARGS --sysvipc"
ARGS="$ARGS -L"

$LINKER $PREFIX/local/bin/proot $ARGS sh $PREFIX/local/bin/init-alpine "$@"