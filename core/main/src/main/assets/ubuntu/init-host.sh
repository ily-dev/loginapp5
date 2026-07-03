#!/bin/bash
# init-host.sh - Ubuntu Version (KOMPLETT FIXED)

echo "start init-host.sh ubuntu"

# ============================================================
# VERZEICHNISSE
# ============================================================
UBUNTU_DIR=$PREFIX/local/ubuntu
ROOT=$PREFIX/local/ubuntu/root
APP_FILES_DIR=$PREFIX/files
LOCAL_DIR=$PREFIX/local
TARFILE_UBUNTU=$PREFIX/files/ubuntu.tar.gz

if [ ! -d $UBUNTU_DIR ]; then
    #local/ubuntu erstellen
    mkdir -p $UBUNTU_DIR
fi

# ============================================================
# UBUNTU ROOTFS ENTPACKEN
# ============================================================
if [ -z "$(ls -A "$UBUNTU_DIR" | grep -vE '^(root|tmp)$')" ]; then
    echo "📦 Entpacke Ubuntu Rootfs..."
    tar -xf $TARFILE_UBUNTU -C $UBUNTU_DIR --no-same-owner --no-same-permissions 2>/dev/null
    #tarfile wieder vom files loeschen
    rm $TARFILE_UBUNTU
    #chmod local
    chmod -R 755 $LOCAL_DIR

    rm -f $ROOT/.bashrc $ROOT/.profile $ROOT/.bash_history $ROOT/.ash_history 2>/dev/null
    echo "✅ Ubuntu Rootfs entpackt"
fi

# ============================================================
# /etc/fstab VORBEREITEN (für pgrep/ps)
# ============================================================
echo "📝 Erstelle /etc/fstab mit /proc Eintrag..."
cat > "$UBUNTU_DIR/etc/fstab" << EOF
# /etc/fstab - Für Ubuntu in Proot
proc /proc proc defaults 0 0
tmpfs /tmp tmpfs defaults 0 0
EOF
echo "✅ /etc/fstab erstellt"

# ============================================================
# PROOT BINARY KOPIEREN
# ============================================================
[ ! -e "$PREFIX/local/bin/proot" ] && cp "$PREFIX/files/proot" "$PREFIX/local/bin"

for sofile in "$PREFIX/files/"*.so.2; do
    dest="$PREFIX/local/lib/$(basename "$sofile")"
    [ ! -e "$dest" ] && cp "$sofile" "$dest"
done

# In init-host.sh (vor Proot-Start):
mkdir -p $PREFIX/files/support

# /proc/uptime erstellen
echo "123456.78 987654.32" > $PREFIX/files/support/uptime

# /proc/version erstellen
echo "Linux version 5.10.0 (fake@proot) #1 $(date)" > $PREFIX/files/support/version


# ============================================================
# PROOT ARGUMENTE (MIT ptrace!)
# ============================================================
ARGS="--kill-on-exit"
ARGS="$ARGS -w /"

# System-Mounts
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

# Standard-Mounts
ARGS="$ARGS -b /sdcard"
ARGS="$ARGS -b /storage"
ARGS="$ARGS -b /dev"
ARGS="$ARGS -b /data"
ARGS="$ARGS -b /dev/urandom:/dev/random"

# ★ ★ ★ /proc KORREKT MOUNTEN ★ ★ ★
ARGS="$ARGS -b /proc"
ARGS="$ARGS -b /proc/mounts"

ARGS="$ARGS -b $PREFIX"
ARGS="$ARGS -b $PREFIX/local/stat:/proc/stat"
ARGS="$ARGS -b $PREFIX/local/vmstat:/proc/vmstat"
ARGS="$ARGS -b $PREFIX/files/support/uptime:/proc/uptime"
ARGS="$ARGS -b $PREFIX/files/support/version:/proc/version"


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

# App-Verzeichnis einbinden
if [ -d "$APP_FILES_DIR" ]; then
    ARGS="$ARGS -b $APP_FILES_DIR:/root/app_files"
    echo -e "\e[32;1m[+] \e[0mApp-Verzeichnis eingebunden"
fi

# FDs
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

ARGS="$ARGS -b /sys"

# TMP-Verzeichnis
if [ ! -d "$PREFIX/local/ubuntu/tmp" ]; then
 mkdir -p "$PREFIX/local/ubuntu/tmp"
 chmod 1777 "$PREFIX/local/ubuntu/tmp"
fi
ARGS="$ARGS -b $PREFIX/local/ubuntu/tmp:/dev/shm"

# Ubuntu Rootfs
ARGS="$ARGS -r $PREFIX/local/ubuntu"

# PROOT-Flags
ARGS="$ARGS -0"
#ARGS="$ARGS -p"        # ← ptrace aktivieren!
#ARGS="$ARGS -H"        # ← Host-Namensraum!
#ARGS="$ARGS -l"        # ← Links verfolgen
ARGS="$ARGS -L"        # ← Lese/Schreib-Links
ARGS="$ARGS --link2symlink"
ARGS="$ARGS --sysvipc"

# ============================================================
# STARTE INIT-UBUNTU.SH
# ============================================================

$LINKER $PREFIX/local/bin/proot $ARGS sh $PREFIX/local/bin/init-ubuntu "$@"

#echo "⚠️ End init-host.sh"