ALPINE_DIR=$PREFIX/local/alpine
APP_FILES_DIR=$PREFIX/files
PROFILE=$PREFIX/local/alpine/root/.profile

mkdir -p $ALPINE_DIR

if [ -z "$(ls -A "$ALPINE_DIR" | grep -vE '^(root|tmp)$')" ]; then
    #tar -xf "$PREFIX/files/alpine.tar.gz" -C "$ALPINE_DIR"
    #fur ubuntu
    tar -xf "$PREFIX/files/alpine.tar.gz" -C "$ALPINE_DIR" --no-same-owner --no-same-permissions 2>/dev/null
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
# APP-VERZEICHNIS AUS PREFIX ERMITTELN
# ============================================================
# $PREFIX ist normalerweise: /data/data/com.xxx.xxx/files/usr
# Das App-Verzeichnis ist dann: $(dirname $PREFIX)/app


if [ -d "$APP_FILES_DIR" ]; then
    ARGS="$ARGS -b $APP_FILES_DIR:/root/app_files"
    echo -e "\e[32;1m[+] \e[0mApp-Verzeichnis eingebunden $APP_FILES_DIR --> /root/app_files \e[0m"
else
    echo "⚠️ App-Verzeichnis nicht gefunden: $APP_FILES_DIR"
fi

#if  [ -f "$PROFILE" ]; then
#    source "$PROFILE"
#fi

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

$LINKER $PREFIX/local/bin/proot $ARGS sh $PREFIX/local/bin/init "$@"
