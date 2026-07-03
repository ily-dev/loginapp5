#!/bin/bash
# init.sh - Ubuntu Version
#set -e
echo "start init.sh ubuntu"
# ======================================
# APP-SPEZIFISCHE PFADE
# ======================================
APP_DATA_DIR=$PREFIX
UBUNTU_DIR="$APP_DATA_DIR/local/ubuntu"
UBUNTU_FLAG="$UBUNTU_DIR/.ubuntu_installed"

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin
export HOME=/root
export PS1="\[\e[38;5;46m\]\u\[\033[39m\]@reterm \[\033[39m\]\w \[\033[0m\]\\$ "
export PIP_BREAK_SYSTEM_PACKAGES=1

export DEBIAN_FRONTEND=noninteractive  # ★ Für apt ohne Interaktion


# =======================================
# PRÜFUNG OB UBUNTU BEREITS INSTALLIERT IST (über FLAG)
# =======================================
if [ -f "$UBUNTU_FLAG" ]; then
    echo "✅ Ubuntu bereits installiert (Flag gefunden in $UBUNTU_DIR)"
    cd $HOME
else
    # ============================================================
    # UBUNTU INSTALLATION (nur beim ersten Mal)
    # ============================================================
    # Aktuelles Verzeichnis anzeigen
    # Oder mit echo
    echo "Aktuelles Verzeichnis: $(pwd)"
    # In deinem Code:
    pfad=$(pwd)
    echo "📁 Aktueller Pfad: $pfad"
    
    # Prüfen ob Datei existiert
    if [ -f /etc/resolv.conf ]; then
        echo "✅ /etc/resolv.conf existiert"
    else
        echo "❌ /etc/resolv.conf existiert NICHT"
    fi
    
    # Prüfen ob Verzeichnis existiert
    if [ -d /etc ]; then
        echo "✅ /etc Verzeichnis existiert"
    fi
    
    # Prüfen ob Datei existiert UND nicht leer ist
    if [ -s /etc/resolv.conf ]; then
        echo "✅ /etc/resolv.conf existiert und ist nicht leer"
    else
        echo "❌ /etc/resolv.conf existiert nicht oder ist leer"
    fi
    
    # ============================================================
    # DNS Einrichten
    # ============================================================
    if [ ! -s /etc/resolv.conf ]; then
        echo "nameserver 8.8.8.8" > /etc/resolv.conf
    fi
    
    echo "📦 Erste Installation - Ubuntu wird eingerichtet..."
    
    
    # ============================================================
    # PAKETE INSTALLIEREN (apt statt apk)
    # ============================================================
    
    # apt update (erstes Mal)
    echo "🔄 Aktualisiere Paketquellen..."
    apt update
    
    # Benötigte Pakete für Ubuntu
    required_packages="bash openssh-server sshpass pure-ftpd python3 python3-pip python3-venv nano curl wget git sudo nmap"
    
    # Prüfen ob Pakete installiert sind (dpkg -s)
    missing_packages=""
    for pkg in $required_packages; do 
        if ! dpkg -s $pkg >/dev/null 2>&1; then
            missing_packages="$missing_packages $pkg"
        fi
    done
    
    if [ -n "$missing_packages" ]; then
        echo -e "\e[34;1m[*] \e[0mInstalliere Pakete: $missing_packages\e[0m"
        apt install -y $missing_packages
        if [ $? -eq 0 ]; then
            echo -e "\e[32;1m[+] \e[0mErfolgreich installiert\e[0m"
        fi
        echo -e "\e[34m[*] \e[0mNutze \e[32mapt\e[0m um Pakete zu installieren\e[0m"
    fi
    
    # ============================================================
    # BENUTZER 'test' ERSTELLEN
    # ============================================================
    if ! id -u test > /dev/null 2>&1; then
        useradd -m -s /bin/bash test
        echo "test:alpine" | chpasswd
        echo -e "\e[32;1m[+] \e[0mBenutzer 'test' erstellt\e[0m"
    fi
    
    # ============================================================
    # test ZU SUDOERS HINZUFÜGEN
    # ============================================================
    if ! grep -q "^test " /etc/sudoers; then
        echo "test ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers
    fi
    
    # =================================
    # /etc/hosts KORRIGIEREN (für localhost)
    # =================================
    
    # Prüfe ob /etc/hosts existiert
    if [ ! -f /etc/hosts ] || ! grep -q "localhost" /etc/hosts 2>/dev/null; then
        echo "📝 Erstelle /etc/hosts..."
        
        cat > /etc/hosts << 'EOF'
127.0.0.1 localhost
127.0.1.1 ubuntu
::1 localhost ip6-localhost ip6-loopback
fe00::0 ip6-localnet
ff00::0 ip6-mcastprefix
ff02::1 ip6-allnodes
ff02::2 ip6-allrouters
EOF
        
        echo "✅ /etc/hosts erstellt"
    else
        echo "✅ /etc/hosts bereits vorhanden"
    fi
    
    # ===================================
    # GRUPPEN KORRIGIEREN
    # ===================================
    
    if groups 2>&1 | grep -q "cannot find name for group ID"; then
        echo "📝 Korrigiere Android-Gruppen..."
        for gid in $(groups 2>&1 | grep -o 'ID [0-9]*' | cut -d' ' -f2 | sort -u); do
            if ! grep -q "^android_$gid:" /etc/group 2>/dev/null; then
                echo "android_$gid:x:$gid:" >> /etc/group
                echo "   ✅ Gruppe android_$gid hinzugefügt"
            fi
        done
        echo "✅ Gruppen korrigiert"
    fi
    
    # ===================================
    # PROMPT FÜR BENUTZER 'test' IN .profile
    # ===================================
    cat > /home/test/.profile << 'EOF'
# Prompt für test
export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin
export HOME=/home/test

export PS1="\[\e[38;5;46m\]\u\[\033[39m\]@reterm \[\033[39m\]\w \[\033[0m\]\\$ "
export PIP_BREAK_SYSTEM_PACKAGES=1

alias ll='ls -la'
alias cls="clear"
alias python='python3'
EOF
    chown test:test /home/test/.profile
    
    # ============================================================
    # .bashrc FÜR test
    # ============================================================
    cat > /home/test/.bashrc << 'EOF'
# .bashrc für test
export PS1="\[\e[38;5;46m\]\u\[\033[39m\]@reterm \[\033[39m\]\w \[\033[0m\]\\$ "
alias ll='ls -la'
alias cls="clear"
alias python='python3'
EOF
    chown test:test /home/test/.bashrc
    
    # ============================================================
    # SSH EINRICHTEN
    # ============================================================
    echo "SSHD_PORT: $SSHD_PORT"
    echo "SSHD_ENABLED: $SSHD_ENABLED"
    echo "FTP_PORT: $FTP_PORT"
    echo "FTP_ENABLED: $FTP_ENABLED"
    
    if [ "$SSHD_ENABLED" = "true" ]; then
        if [ -f /etc/ssh/sshd_config ]; then
            if [ ! -f /etc/ssh/ssh_host_rsa_key ]; then
                ssh-keygen -A
            fi
            
            cat > /etc/ssh/sshd_config << EOF
Port $SSHD_PORT
PermitRootLogin no
PasswordAuthentication yes
ListenAddress 0.0.0.0
AllowUsers test
EOF
            echo -e "\e[32;1m[+] \e[0mFile sshd_conifg eingefuegt !\e[0m"
        
        else
            echo "❌ file etc/ssh/sshd_confug nicht vorhanden!! "
        fi
    fi
    
    # Root Passwort (optional)
    echo "root:alpine" | chpasswd
    
    # ============================================================
    # SSH-Dienst starten (für Ubuntu)
    # ============================================================
    mkdir -p /run/sshd
    
    # ============================================================
    # .profile FÜR ROOT
    # ============================================================
    ROOT_PROFILE="/root/.profile"
    if [ ! -f "$ROOT_PROFILE" ]; then
        cat > "$ROOT_PROFILE" << 'EOF'
# ~/.profile: executed by Bourne-compatible login shells.
echo "✅ profile aktiv"
if [ -f /root/.bashrc ]; then
    source /root/.bashrc
fi

mesg n 2> /dev/null || true
EOF
        chmod 644 "$ROOT_PROFILE"
        echo -e "\e[32;1m[✓] \e[0m/root/.profile erstellt!"
    fi
    
    # ============================================================
    # .bashrc FÜR ROOT
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
alias nmap="nmap -n -Pn -sT"
alias pip='env -u ANDROID_ROOT -u ANDROID_DATA pip'
alias pip3='env -u ANDROID_ROOT -u ANDROID_DATA pip3'

echo "SSHD_PORT: $SSHD_PORT"
echo "SSHD_ENABLED: $SSHD_ENABLED"
echo "FTP_PORT: $FTP_PORT"
echo "FTP_ENABLED: $FTP_ENABLED"

# SSH starten
if [ "$SSHD_ENABLED" = "true" ]; then
    if ! nmap -p $SSHD_PORT localhost 2>/dev/null | grep -q "open"; then
        if [ -x /usr/sbin/sshd ]; then
            nohup /usr/sbin/sshd &
            echo -e "\e[32;1m[+] \e[0mSSHD gestartet\e[0m"
        else
            echo "⚠️ pure-ftpd nicht installiert"
        fi
    else
        echo -e "\e[31;1m[-] \e[0mSSHD schon aktiv \e[0m"
    fi
fi

# FTP Server im Hintergrund starten
if [ "$FTP_ENABLED" = "true" ]; then
    if ! nmap -p $FTP_PORT localhost 2>/dev/null | grep -q "open"; then
        if [ "$(id -u)" -eq 0 ]; then
            if [ -x /usr/sbin/pure-ftpd ]; then
                echo -e "\e[32;1m[+] \e[0m🚀 Starte FTP-Server...  \e[0m"
                /usr/sbin/pure-ftpd -S $FTP_PORT -4 -E -j -D >/dev/null 2>&1 &
                echo "✅ pure-ftpd gestartet"
            else
                echo "⚠️ pure-ftpd nicht installiert"
            fi
        else
            echo -e "[Autostart] Fehler: pure-ftpd kann nur im root gestartet werden"
        fi
    else
        echo -e "\e[31;1m[+] \e[0mFTP-Server laeuft bereits \e[0m"
    fi
fi
EOF
        chown root:root "$BASHRC_PATH"
        chmod 644 "$BASHRC_PATH"
        echo -e "\e[32;1m[✓] \e[0m/root/.bashrc erstellt!"
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
    # ======================================
    touch "$UBUNTU_FLAG"
    echo -e "\e[32;1m[✓] \e[0mUbuntu Installation abgeschlossen. Flag gesetzt: $UBUNTU_FLAG"
    cd $HOME
fi

# In init-ubuntu.sh - GANZ AM ENDE (vor exec):

# ============================================================
# GRUPPEN FIX (damit keine Meldungen mehr kommen)
# ============================================================
# Füge alle Android-Gruppen ein (die bekannt sind)
#for gid in 3003 9997 21915 51915 99909997; do
#    if ! grep -q "^android_$gid:" /etc/group 2>/dev/null; then
#        echo "android_$gid:x:$gid:" >> /etc/group
#    fi
#done

# Jetzt sollte groups keine Fehler mehr melden
groups > /dev/null 2>&1

# ============================================================
# INTERAKTIVE SHELL
# ============================================================
exec /bin/bash -l