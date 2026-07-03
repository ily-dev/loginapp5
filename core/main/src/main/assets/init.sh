#!/bin/sh
set -e
# ======================================
# APP-SPEZIFISCHE PFADE
# ======================================
APP_DATA_DIR=$PREFIX
ALPINE_DIR="$APP_DATA_DIR/local/alpine"
ALPINE_FLAG="$ALPINE_DIR/.alpine_installed"
PYTHON_APP_DIR="$APP_DATA_DIR/files/app"
PYTHON_APP_MAIN="$PYTHON_APP_DIR/main.pyc"

# =======================================
# PRÜFUNG OB ALPINE BEREITS INSTALLIERT IST (über FLAG)
# =======================================
if [ -f "$ALPINE_FLAG" ]; then
    echo "✅ Alpine bereits installiert (Flag gefunden in $ALPINE_DIR)"
    
fi

# ============================================================
# ALPINE INSTALLATION (nur beim ersten Mal)
# ============================================================
echo "📦 Erste Installation - Alpine wird eingerichtet..."

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin
export HOME=/root

# DNS
if [ ! -s /etc/resolv.conf ]; then
    echo "nameserver 8.8.8.8" > /etc/resolv.conf
fi

# ============================================================
# PAKETE INSTALLIEREN (bleibt wie gehabt)
# ============================================================

export PS1="\[\e[38;5;46m\]\u\[\033[39m\]@reterm \[\033[39m\]\w \[\033[0m\]\\$ "
export PIP_BREAK_SYSTEM_PACKAGES=1

required_packages="bash gcompat glib nano python3 py3-pip openssh sshpass"
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

# Root Passwort (optional, falls du root lokal brauchst)
echo "root:alpine" | chpasswd

# SSH starten
if ! pgrep sshd > /dev/null; then
    /usr/sbin/sshd &
    echo -e "\e[32;1m[+] \e[0mSSHD gestartet\e[0m"
fi

# ============================================================
# LINKER WARNING FIX
# ============================================================

if [[ ! -f /linkerconfig/ld.config.txt ]]; then
    mkdir -p /linkerconfig
    touch /linkerconfig/ld.config.txt
fi

# ============================================================
# SHELL STARTEN (als test)
# ============================================================

#if [ "$#" -eq 0 ]; then
    # Wechsle zu test und starte Shell mit Prompt
#    su - test
#else
    # Befehl als test ausführen
#    su - test -c "$*"
#fi

# ======================================
# NACH ERFOLGREICHER INSTALLATION: FLAG SETZEN
# =====================================
touch "$ALPINE_FLAG"
echo "✅ Alpine Installation abgeschlossen. Flag gesetzt: $ALPINE_FLAG"


# Shell
exec /bin/ash