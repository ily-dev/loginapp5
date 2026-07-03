#!/system/bin/sh
# rem_ubuntu.sh - Ubuntu Rootfs bereinigen und packen (ausführbar von überall)

# ============================================================
# AUTOMATISCHE PFADERKENNUNG
# ============================================================
find_ubuntu_dir() {
    # 1. Prüfe ob wir bereits im richtigen Verzeichnis sind
    if [ -d "./ubuntu" ] && [ -d "./bin" ]; then
        echo "$(pwd)/ubuntu"
        return 0
    fi
    
    # 2. Prüfe ob PREFIX gesetzt ist
    if [ -n "$PREFIX" ] && [ -d "$PREFIX/local/ubuntu" ]; then
        echo "$PREFIX/local/ubuntu"
        return 0
    fi
    
    # 3. Suche im aktuellen Verzeichnis nach local/ubuntu
    if [ -d "./local/ubuntu" ]; then
        echo "$(pwd)/local/ubuntu"
        return 0
    fi
    
    # 4. Suche im Standard-Pfad
    STANDARD_PATHS="/data/user/0/com.meinname.loginapp8.debug /data/data/com.meinname.loginapp8.debug"
    for path in $STANDARD_PATHS; do
        if [ -d "$path/local/ubuntu" ]; then
            echo "$path/local/ubuntu"
            return 0
        fi
    done
    
    # 5. Suche im übergeordneten Verzeichnis
    if [ -d "../local/ubuntu" ]; then
        echo "$(cd .. && pwd)/local/ubuntu"
        return 0
    fi
    
    # 6. Suche nach beliebigen App-Daten
    for app_dir in /data/user/0/com.*.debug /data/data/com.*.debug; do
        if [ -d "$app_dir/local/ubuntu" ]; then
            echo "$app_dir/local/ubuntu"
            return 0
        fi
    done
    
    return 1
}

# Ubuntu Verzeichnis finden
UBUNTU_DIR=$(find_ubuntu_dir)
if [ -z "$UBUNTU_DIR" ]; then
    echo "❌ Ubuntu Rootfs nicht gefunden!"
    echo ""
    echo "📝 Bitte führe das Script aus einem der folgenden Verzeichnisse aus:"
    echo "   - .../local/ (wo ubuntu Ordner liegt)"
    echo "   - .../local/ubuntu/"
    echo ""
    echo "📝 Oder setze PREFIX manuell:"
    echo "   PREFIX=/pfad/zur/app rem_ubuntu.sh"
    exit 1
fi

# PREFIX aus UBUNTU_DIR extrahieren (für files Verzeichnis)
PREFIX=$(dirname "$(dirname "$UBUNTU_DIR")")

# Ausgabeverzeichnis
OUTPUT_DIR="${OUTPUT_DIR:-/sdcard}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# ============================================================
# FARBEN
# ============================================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# ============================================================
# HILFE
# ============================================================
show_help() {
    cat << EOF
${GREEN}rem_ubuntu.sh - Ubuntu Rootfs bereinigen und packen${NC}

${YELLOW}Verwendung:${NC}
    rem_ubuntu.sh [OPTIONEN]

${YELLOW}Optionen:${NC}
    ${GREEN}--clean${NC}              Nur bereinigen (kein Packen)
    ${GREEN}--pack${NC}               Nur packen (kein Bereinigen)
    ${GREEN}--full${NC}               Bereinigen und packen (Standard)
    ${GREEN}--remove-ssh-keys${NC}    SSH Host Keys löschen
    ${GREEN}--purge${NC}              Maximale Bereinigung (auch Docs/Man/Locale)
    ${GREEN}--output${NC}             Ausgabeverzeichnis (Standard: /sdcard)
    ${GREEN}--no-compare${NC}         Keine Größenvergleiche
    ${GREEN}--help${NC}               Diese Hilfe anzeigen

${YELLOW}Beispiele:${NC}
    rem_ubuntu.sh                     # Bereinigen + Packen
    rem_ubuntu.sh --clean             # Nur bereinigen
    rem_ubuntu.sh --purge             # Maximale Bereinigung
    rem_ubuntu.sh --remove-ssh-keys   # SSH Keys löschen
    rem_ubuntu.sh --output /data/local/tmp

${YELLOW}Erstellt:${NC}
    ubuntu_custom_YYYYMMDD_HHMMSS.tar.gz
EOF
}

# ============================================================
# LOGGING
# ============================================================
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[✓]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[⚠]${NC} $1"; }
log_error() { echo -e "${RED}[✗]${NC} $1"; }
log_progress() { echo -e "${CYAN}[➜]${NC} $1"; }
log_ubuntu() { echo -e "${MAGENTA}[🐧]${NC} $1"; }

# ============================================================
# GRÖSSEN-HELPER
# ============================================================
get_size() {
    du -sb "$1" 2>/dev/null | cut -f1
}

format_size() {
    local size="$1"
    if [ -z "$size" ] || [ "$size" -eq 0 ]; then
        echo "0B"
        return
    fi
    if [ "$size" -gt 1073741824 ]; then
        echo "$(echo "scale=2; $size/1073741824" | bc) GB"
    elif [ "$size" -gt 1048576 ]; then
        echo "$(echo "scale=2; $size/1048576" | bc) MB"
    elif [ "$size" -gt 1024 ]; then
        echo "$(echo "scale=2; $size/1024" | bc) KB"
    else
        echo "${size}B"
    fi
}

# ============================================================
# ANALYSE VORHER
# ============================================================
analyze_before() {
    log_info "Analysiere Ubuntu Rootfs VOR der Bereinigung..."
    
    TMP_SIZE=$(get_size "$UBUNTU_DIR/tmp")
    CACHE_SIZE=$(get_size "$UBUNTU_DIR/var/cache")
    LOG_SIZE=$(get_size "$UBUNTU_DIR/var/log")
    VAR_TMP_SIZE=$(get_size "$UBUNTU_DIR/var/tmp")
    ROOT_CACHE=$(get_size "$UBUNTU_DIR/root/.cache")
    HOME_CACHE=$(find "$UBUNTU_DIR/home" -name ".cache" -type d -exec du -sb {} + 2>/dev/null | awk '{sum+=$1} END {print sum+0}')
    APT_CACHE=$(get_size "$UBUNTU_DIR/var/cache/apt")
    APT_ARCHIVES=$(get_size "$UBUNTU_DIR/var/cache/apt/archives")
    SSH_KEYS=$(get_size "$UBUNTU_DIR/etc/ssh/ssh_host_"* 2>/dev/null)
    LOG_FILES=$(find "$UBUNTU_DIR" -name "*.log" -type f -exec du -sb {} + 2>/dev/null | awk '{sum+=$1} END {print sum+0}')
    HISTORY=$(find "$UBUNTU_DIR" -name "*.bash_history" -o -name "*.zsh_history" 2>/dev/null | xargs du -sb 2>/dev/null | awk '{sum+=$1} END {print sum+0}')
    MAN_PAGES=$(get_size "$UBUNTU_DIR/usr/share/man")
    DOCS=$(get_size "$UBUNTU_DIR/usr/share/doc")
    LOCALE=$(get_size "$UBUNTU_DIR/usr/share/locale")
    INFO=$(get_size "$UBUNTU_DIR/usr/share/info")
    
    TOTAL_BEFORE=$(get_size "$UBUNTU_DIR")
    
    echo ""
    echo "============================================================"
    echo "📊 ${CYAN}SPEICHERANALYSE VOR BEREINIGUNG${NC}"
    echo "============================================================"
    printf "%-35s %15s\n" "Verzeichnis/Datei" "Größe"
    echo "------------------------------------------------------------"
    [ "$TMP_SIZE" -gt 0 ] && printf "%-35s %15s\n" "tmp/" "$(format_size $TMP_SIZE)"
    [ "$CACHE_SIZE" -gt 0 ] && printf "%-35s %15s\n" "var/cache/" "$(format_size $CACHE_SIZE)"
    [ "$APT_CACHE" -gt 0 ] && printf "%-35s %15s\n" "var/cache/apt/" "$(format_size $APT_CACHE)"
    [ "$APT_ARCHIVES" -gt 0 ] && printf "%-35s %15s\n" "  ├─ archives/" "$(format_size $APT_ARCHIVES)"
    [ "$LOG_SIZE" -gt 0 ] && printf "%-35s %15s\n" "var/log/" "$(format_size $LOG_SIZE)"
    [ "$VAR_TMP_SIZE" -gt 0 ] && printf "%-35s %15s\n" "var/tmp/" "$(format_size $VAR_TMP_SIZE)"
    [ "$ROOT_CACHE" -gt 0 ] && printf "%-35s %15s\n" "root/.cache/" "$(format_size $ROOT_CACHE)"
    [ "$HOME_CACHE" -gt 0 ] && printf "%-35s %15s\n" "home/*/.cache/" "$(format_size $HOME_CACHE)"
    [ "$SSH_KEYS" -gt 0 ] && printf "%-35s %15s\n" "SSH Host Keys" "$(format_size $SSH_KEYS)"
    [ "$LOG_FILES" -gt 0 ] && printf "%-35s %15s\n" "*.log Dateien" "$(format_size $LOG_FILES)"
    [ "$HISTORY" -gt 0 ] && printf "%-35s %15s\n" "History Dateien" "$(format_size $HISTORY)"
    [ "$MAN_PAGES" -gt 0 ] && printf "%-35s %15s\n" "usr/share/man/" "$(format_size $MAN_PAGES)"
    [ "$DOCS" -gt 0 ] && printf "%-35s %15s\n" "usr/share/doc/" "$(format_size $DOCS)"
    [ "$LOCALE" -gt 0 ] && printf "%-35s %15s\n" "usr/share/locale/" "$(format_size $LOCALE)"
    [ "$INFO" -gt 0 ] && printf "%-35s %15s\n" "usr/share/info/" "$(format_size $INFO)"
    echo "------------------------------------------------------------"
    printf "${GREEN}%-35s ${CYAN}%15s${NC}\n" "GESAMT" "$(format_size $TOTAL_BEFORE)"
    echo "============================================================"
    
    export TOTAL_BEFORE
}

# ============================================================
# ANALYSE NACHHER
# ============================================================
analyze_after() {
    local TOTAL_AFTER=$(get_size "$UBUNTU_DIR")
    local SAVED=$((TOTAL_BEFORE - TOTAL_AFTER))
    local PERCENT=0
    
    if [ "$TOTAL_BEFORE" -gt 0 ]; then
        PERCENT=$(echo "scale=2; ($SAVED * 100) / $TOTAL_BEFORE" | bc)
    fi
    
    echo ""
    echo "============================================================"
    echo "📊 ${GREEN}SPEICHERERSPARNIS NACH BEREINIGUNG${NC}"
    echo "============================================================"
    printf "%-35s %15s\n" "" "Größe"
    echo "------------------------------------------------------------"
    printf "%-35s ${RED}%15s${NC}\n" "Vorher" "$(format_size $TOTAL_BEFORE)"
    printf "%-35s ${GREEN}%15s${NC}\n" "Nachher" "$(format_size $TOTAL_AFTER)"
    echo "------------------------------------------------------------"
    printf "%-35s ${YELLOW}%15s${NC}\n" "Gespart" "$(format_size $SAVED)"
    printf "%-35s ${CYAN}%15s${NC}\n" "Ersparnis in %" "${PERCENT}%"
    
    if [ "$PERCENT" -gt 0 ]; then
        echo ""
        echo "📊 ${CYAN}Balkendiagramm:${NC}"
        local BARS=$(echo "$PERCENT / 5" | bc)
        [ "$BARS" -gt 20 ] && BARS=20
        local EMPTY=$((20 - BARS))
        echo -n "  ["
        printf "%${BARS}s" | tr ' ' '█'
        printf "%${EMPTY}s" | tr ' ' '░'
        echo "] ${PERCENT}% gespart"
    fi
    echo "============================================================"
}

# ============================================================
# BEREINIGEN
# ============================================================
clean_ubuntu() {
    log_ubuntu "Starte Bereinigung von Ubuntu..."
    
    cd "$UBUNTU_DIR" || {
        log_error "Kann nicht in $UBUNTU_DIR wechseln"
        return 1
    }
    
    log_progress "Lösche temporäre Dateien..."
    rm -rf tmp/* 2>/dev/null
    rm -rf var/cache/* 2>/dev/null
    rm -rf var/log/* 2>/dev/null
    rm -rf var/tmp/* 2>/dev/null
    rm -rf root/.cache/* 2>/dev/null
    rm -rf home/*/.cache/* 2>/dev/null
    
    log_progress "Leere APT Cache..."
    if [ "$PURGE" = "true" ]; then
        log_info "Führe apt-get clean & autoclean durch..."
        chroot . /usr/bin/apt-get clean 2>/dev/null
        chroot . /usr/bin/apt-get autoclean 2>/dev/null
        chroot . /usr/bin/apt-get autoremove -y 2>/dev/null
    fi
    rm -rf var/cache/apt/archives/* 2>/dev/null
    rm -rf var/cache/apt/pkgcache.bin 2>/dev/null
    rm -rf var/cache/apt/srcpkgcache.bin 2>/dev/null
    
    log_progress "Lösche Logs..."
    find . -name "*.log" -type f -delete 2>/dev/null
    truncate -s 0 var/log/* 2>/dev/null
    find var/log -type f -exec truncate -s 0 {} \; 2>/dev/null
    
    log_progress "Lösche History..."
    rm -f root/.bash_history 2>/dev/null
    rm -f home/*/.bash_history 2>/dev/null
    rm -f root/.zsh_history 2>/dev/null
    rm -f home/*/.zsh_history 2>/dev/null
    rm -f root/.history 2>/dev/null
    rm -f home/*/.history 2>/dev/null
    
    if [ "$REMOVE_SSH_KEYS" = "true" ]; then
        log_progress "Lösche SSH Host Keys..."
        rm -f etc/ssh/ssh_host_* 2>/dev/null
    else
        log_info "Behalte SSH Host Keys..."
    fi
    
    log_progress "Lösche Installations-Flag..."
    rm -f .ubuntu_installed 2>/dev/null
    
    if [ "$PURGE" = "true" ]; then
        log_progress "Lösche Dokumentation und Man-Pages..."
        rm -rf usr/share/doc/* 2>/dev/null
        rm -rf usr/share/man/* 2>/dev/null
        rm -rf usr/share/info/* 2>/dev/null
        rm -rf usr/share/locale/* 2>/dev/null
        rm -rf usr/share/gnome/help/* 2>/dev/null
        rm -rf usr/share/gtk-doc/* 2>/dev/null
    fi
    
    log_progress "Räume leere Verzeichnisse auf..."
    find . -type d -empty -delete 2>/dev/null
    
    log_progress "Ubuntu-spezifische Bereinigung..."
    rm -f root/.bashrc.bak 2>/dev/null
    rm -f root/.profile.bak 2>/dev/null
    rm -f etc/motd 2>/dev/null
    rm -f etc/update-motd.d/* 2>/dev/null
    
    log_success "Bereinigung abgeschlossen!"
}

# ============================================================
# ROOTFS PACKEN
# ============================================================
pack_ubuntu() {
    log_ubuntu "Starte Packen von Ubuntu..."
    
    cd "$UBUNTU_DIR" || {
        log_error "Kann nicht in $UBUNTU_DIR wechseln"
        return 1
    }
    
    OUTPUT_FILE="$OUTPUT_DIR/ubuntu_custom_$TIMESTAMP.tar.gz"
    
    log_progress "Erstelle: $OUTPUT_FILE"
    
    tar -czf "$OUTPUT_FILE" \
        --no-same-owner \
        --no-same-permissions \
        --exclude='tmp/*' \
        --exclude='proc/*' \
        --exclude='sys/*' \
        --exclude='dev/*' \
        --exclude='run/*' \
        --exclude='var/cache/*' \
        --exclude='var/log/*' \
        --exclude='var/tmp/*' \
        --exclude='root/.cache/*' \
        --exclude='home/*/.cache/*' \
        --exclude='*.log' \
        --exclude='*.pid' \
        --exclude='.ubuntu_installed' \
        --exclude='etc/ssh/ssh_host_*' \
        --exclude='root/.bash_history' \
        --exclude='home/*/.bash_history' \
        --exclude='root/.zsh_history' \
        --exclude='home/*/.zsh_history' \
        --exclude='usr/share/doc/*' \
        --exclude='usr/share/man/*' \
        --exclude='usr/share/info/*' \
        --exclude='usr/share/locale/*' \
        --exclude='var/lib/apt/lists/*' \
        --exclude='var/cache/apt/*' \
        --exclude='etc/apt/sources.list.d/*.save' \
        . 2>/dev/null
    
    if [ $? -eq 0 ]; then
        SIZE=$(du -h "$OUTPUT_FILE" | cut -f1)
        log_success "Rootfs erstellt: $OUTPUT_FILE"
        log_success "Größe: $SIZE"
        
        if [ -d "$PREFIX/files" ]; then
            cp "$OUTPUT_FILE" "$PREFIX/files/ubuntu.tar.gz"
            log_success "Kopiert nach: $PREFIX/files/ubuntu.tar.gz"
        fi
    else
        log_error "Fehler beim Erstellen des Rootfs!"
        return 1
    fi
}

# ============================================================
# HAUPT-FUNKTION
# ============================================================
main() {
    ACTION="full"
    REMOVE_SSH_KEYS="false"
    PURGE="false"
    DO_COMPARE="true"
    
    while [ $# -gt 0 ]; do
        case "$1" in
            --clean) ACTION="clean"; shift ;;
            --pack) ACTION="pack"; shift ;;
            --full) ACTION="full"; shift ;;
            --remove-ssh-keys) REMOVE_SSH_KEYS="true"; shift ;;
            --purge) PURGE="true"; shift ;;
            --output) OUTPUT_DIR="$2"; shift 2 ;;
            --no-compare) DO_COMPARE="false"; shift ;;
            --help|-h) show_help; exit 0 ;;
            *) log_error "Unbekannte Option: $1"; show_help; exit 1 ;;
        esac
    done
    
    echo "============================================================"
    echo "  ${MAGENTA}rem_ubuntu.sh${NC} - Ubuntu Rootfs Tool"
    echo "============================================================"
    echo ""
    echo "📁 Ubuntu Verzeichnis: $UBUNTU_DIR"
    echo "📁 Ausgabe: $OUTPUT_DIR"
    
    if [ "$PURGE" = "true" ]; then
        echo ""
        log_warn "PURGE-Modus aktiv: Dokumentation, Man-Pages und Locale werden gelöscht!"
    fi
    echo ""
    
    case "$ACTION" in
        clean)
            [ "$DO_COMPARE" = "true" ] && analyze_before
            clean_ubuntu
            [ "$DO_COMPARE" = "true" ] && analyze_after
            ;;
        pack)
            pack_ubuntu
            ;;
        full)
            [ "$DO_COMPARE" = "true" ] && analyze_before
            clean_ubuntu
            [ "$DO_COMPARE" = "true" ] && analyze_after
            pack_ubuntu
            ;;
    esac
    
    echo ""
    echo "============================================================"
    log_success "Fertig!"
    echo "============================================================"
}

main "$@"