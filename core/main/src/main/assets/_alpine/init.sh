#!/bin/sh
#set -e
# ======================================
# APP-SPEZIFISCHE PFADE
# ======================================
APP_DATA_DIR=$PREFIX
ALPINE_DIR="$APP_DATA_DIR/local/alpine"
ALPINE_FLAG="$ALPINE_DIR/.alpine_installed"
PYTHON_APP_DIR="$APP_DATA_DIR/files/app"
PYTHON_APP_MAIN="$PYTHON_APP_DIR/main.pyc"
TARFILE="$APP_DATA_DIR/files/private.tar"

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin
export HOME=/root
export PS1="\[\e[38;5;46m\]\u\[\033[39m\]@reterm \[\033[39m\]\w \[\033[0m\]\\$ "
export PIP_BREAK_SYSTEM_PACKAGES=1
    
# =======================================
# PRÜFUNG OB ALPINE BEREITS INSTALLIERT IST (über FLAG)
# =======================================
if [ -f "$ALPINE_FLAG" ]; then
    echo "✅ Alpine bereits installiert (Flag gefunden in $ALPINE_DIR)"
else
    # ============================================================
    # ALPINE INSTALLATION (nur beim ersten Mal)
    # ============================================================
    
    echo "Aktuelles Verzeichnis: $(pwd)"
    # In deinem Code:
    pfad=$(pwd)
    echo "📁 Aktueller Pfad: $pfad"
    
    return 0
    
    
    # ============================================================
    # DNS Einrichten / Domain 
    # ============================================================
    
    # DNS
    if [ ! -s /etc/resolv.conf ]; then
        echo "nameserver 8.8.8.8" > /etc/resolv.conf
    fi
    
    echo "📦 Erste Installation - Alpine wird eingerichtet..."
    
    # ============================================================
    # PAKETE INSTALLIEREN (bleibt wie gehabt)
    # ============================================================
    
    required_packages="bash gcompat glib nano python3 py3-pip openssh sshpass pure-ftpd "
    missing_packages=""
    for pkg in $required_packages; do 
        if ! apk info -e $pkg >/dev/null 2>&1; then
            missing_packages="$missing_packages $pkg"
        fi
    done
    
    if [ -n "$missing_packages" ]; then
        echo -e "\e[34;1m[*] \e[0mInstalliere Pakete: $missing_packages\e[0m"
        apk update && apk upgrade
        apk add $missing_packages
        if [ $? -eq 0 ]; then
            echo -e "\e[32;1m[+] \e[0mErfolgreich installiert\e[0m"
        fi
        echo -e "\e[34m[*] \e[0mNutze \e[32mapk\e[0m um Pakete zu installieren\e[0m"
    fi
    
    # ============================================================
    # BENUTZER 'test' ERSTELLEN
    # ============================================================

    if ! id -u test > /dev/null 2>&1; then
        adduser -D -h /home/test test
        echo "test:alpine" | chpasswd
        echo -e "\e[32;1m[+] \e[0mBenutzer 'test' erstellt\e[0m"
    fi
    
    # ============================================================
    # PROMPT FÜR BENUTZER 'test' IN .profile
    # ============================================================

    cat > /home/test/.profile << 'EOF'
# Prompt für test (gleicher Stil)
export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin
export HOME=/root

export PS1="\[\e[38;5;46m\]\u\[\033[39m\]@reterm \[\033[39m\]\w \[\033[0m\]\\$ "
export PIP_BREAK_SYSTEM_PACKAGES=1
export PATH=/usr/bin:/bin:/usr/local/bin

alias ll='ls -la'
alias cls="clear"
alias python='python3'
EOF
    
    chown test:test /home/test/.profile
    
    # ============================================================
    # SSH EINRICHTEN
    # ============================================================
    
    if [ -f /etc/ssh/sshd_config ]; then
        if [ ! -f /etc/ssh/ssh_host_rsa_key ]; then
            ssh-keygen -A
        fi
        
        cat > /etc/ssh/sshd_config << EOF
Port 2222
PermitRootLogin no
PasswordAuthentication yes
ListenAddress 0.0.0.0
AllowUsers test
EOF
        
        echo -e "\e[32;1m[+] \e[0mFile sshd_conifg eingefuegt !\e[0m"
    
    else
        echo "❌ file etc/ssh/sshd_confug nicht vorhanden!! "
    fi
    
    # Root Passwort (optional, falls du root lokal brauchst)
    echo "root:alpine" | chpasswd
    
    # ============================================================
    # App Ordner erstellen wenn nicht existiert
    # ============================================================

    if [[ ! -d $PYTHON_APP_DIR ]]; then
        mkdir -p $PYTHON_APP_DIR
        echo -e "\e[32;1m[+] \e[0mapp erstellt\e[0m"
    else
        echo -e "\e[32;1m[+] \e[0mapp schon vorhanden !!\e[0m"
    fi
    # ============================================================
    # App Ordner erstellen 
    # ============================================================
    # Prüfe ob main.py schon existiert
    if [ ! -f "$PYTHON_APP_DIR/main.py" ] && [ ! -f "$PYTHON_APP_DIR/main.pyc" ]; then
        if [ -f "$TARFILE" ]; then
            echo -e "\e[34;1m[*] \e[0mEntpacke private.tar nach $PYTHON_APP_DIR..."
            tar -xf "$TARFILE" -C "$PYTHON_APP_DIR"
            echo -e "\e[32;1m[+] \e[0mprivate.tar entpackt!"
            chmod -R 755 $PYTHON_APP_DIR
            echo -e "\e[32;1m[+] \e[0mBerechtigung fur app Verzeichnis!"
            
        else
            echo -e "\e[33;1m[!] \e[0mprivate.tar nicht gefunden: $TARFILE"
        fi
    else
        echo -e "\e[32;1m[✓] \e[0mmain.py/main.pyc bereits vorhanden, überspringe Entpacken"
    fi
    # ============================================================
    # .profile FÜR ROOT ERSTELLEN (wenn nicht vorhanden)
    # ============================================================

    ROOT_PROFILE="/root/.profile"
    if [ ! -f "$ROOT_PROFILE" ]; then
        echo "📝 Erstelle .profile für root..."
        
        cat > "$ROOT_PROFILE" << 'EOF'
# ~/.profile: executed by Bourne-compatible login shells.
echo "✅ profile aktiv"
if [ -f /root/.bashrc ]; then
    source /root/.bashrc
fi

mesg n 2> /dev/null || true
EOF
    
        chmod 644 "$ROOT_PROFILE"
        echo -e "\e[32;1m[✓] \e[0m/root/.profile erstellt !"
    else
        echo -e "\e[32;1m[✓] \e[0m/root/.profile existiert bereits !"
    fi
    # ============================================================
    # .bashrc FÜR ROOT ERSTELLEN
    # ============================================================
    BASHRC_PATH="/root/.bashrc"
    if [ ! -f "$BASHRC_PATH" ]; then
        cat > "$BASHRC_PATH" << 'EOF'
echo '✅ bashrc aktiv'
# DEINE BASHRC KONFIGURATION
alias ls='ls --color=auto'
alias ll='ls -l'
alias la='ls -a'
alias lal='ls -la'
alias cls=clear
alias build="buildozer -v android debug"
alias dists="cd /root/.buildozer/android/platform/build-arm64-v8a/dists"

# SSH starten
if ! pgrep sshd > /dev/null; then
    /usr/sbin/sshd &
    echo -e "\e[32;1m[+] \e[0mSSHD gestartet\e[0m"
else
    echo -e "\e[31;1m[-] \e[0mSSHD schon aktiv \e[0m"
fi

# FTP Server im Hintergrund starten
if [ "$(id -u)" -eq 0 ]; then
    if pgrep pure-ftpd > /dev/null; then
    echo -e "\e[31;1m[+] \e[0mFTP-Server laeuft bereits \e[0m"
else
    if [ -x /usr/sbin/pure-ftpd ]; then
        echo -e "\e[32;1m[+] \e[0m🚀 Starte FTP-Server...  \e[0m"
        /usr/sbin/pure-ftpd -S 127.0.0.1,2135 -4 -E -j >/dev/null 2>&1 &
        echo "✅ pure-ftpd gestartet"
    else
        echo "⚠️ pure-ftpd nicht installiert"
    fi
fi
else
    echo -e "[Autostart] Fehler: pure-ftpd kann nur im root gestartet werden"
fi
EOF
        chown root:root "$BASHRC_PATH"
        chmod 644 "$BASHRC_PATH"
        echo -e "\e[32;1m[✓] \e[0m/root/.bashrc erstellt !"
    fi
    
    # ============================================================
    # LINKER WARNING FIX
    # ============================================================

    if [[ ! -f /linkerconfig/ld.config.txt ]]; then
        mkdir -p /linkerconfig
        touch /linkerconfig/ld.config.txt
    fi
    
    # ======================================
    # NACH ERFOLGREICHER INSTALLATION: FLAG SETZEN
    # =====================================

    if [ -f "$ALPINE_FLAG" ]; then
        echo "✅ Alpine bereits installiert (Flag gefunden in $ALPINE_DIR)"
    else
        # flag setzen
        touch "$ALPINE_FLAG"
        echo -e "\e[32;1m[✓] \e[0mAlpine Installation abgeschlossen. Flag gesetzt: $ALPINE_FLAG"
    fi
fi

# Shell .prolie ausgefuhrt
/bin/ash -l
