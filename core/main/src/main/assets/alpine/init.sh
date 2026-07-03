#!/bin/sh
#set -e
# ======================================
# APP-SPEZIFISCHE PFADE
# ======================================
APP_DATA_DIR=$PREFIX
ALPINE_DIR="$APP_DATA_DIR/local/alpine"
ALPINE_FLAG="$ALPINE_DIR/.alpine_installed"

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
    
    required_packages="bash gcompat glib nano python3 py3-pip openssh sshpass pure-ftpd nmap "
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
    echo "# ==============================="
    echo "SSHD_PORT: $SSHD_PORT"
    echo "SSHD_ENABLED: $SSHD_ENABLED"
    echo "FTP_PORT: $FTP_PORT"
    echo "FTP_ENABLED: $FTP_ENABLED"
    echo "# ==============================="
    
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
    
    # Root Passwort (optional, falls du root lokal brauchst)
    echo "root:alpine" | chpasswd
    
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
alias nmap="nmap -n -Pn -sT"

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
