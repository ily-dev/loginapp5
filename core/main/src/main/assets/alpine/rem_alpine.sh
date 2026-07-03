#!/system/bin/sh
# rem_alpine.sh - Alpine Rootfs bereinigen und packen (ausführbar von überall)

# ============================================================
# AUTOMATISCHE PFADERKENNUNG
# ============================================================
find_alpine_dir() {
    # 1. Prüfe ob wir bereits im richtigen Verzeichnis sind
    if [ -d "./alpine" ] && [ -d "./bin" ]; then
        echo "$(pwd)/alpine"
        return 0
    fi
    
    # 2. Prüfe ob PREFIX gesetzt ist
    if [ -n "$PREFIX" ] && [ -d "$PREFIX/local/alpine" ]; then
        echo "$PREFIX/local/alpine"
        return 0
    fi
    
    # 3. Suche im aktuellen Verzeichnis nach local/alpine
    if [ -d "./local/alpine" ]; then
        echo "$(pwd)/local/alpine"
        return 0
    fi
    
    # 4. Suche im Standard-Pfad
    STANDARD_PATHS="/data/user/0/com.meinname.loginapp8.debug /data/data/com.meinname.loginapp8.debug"
    for path in $STANDARD_PATHS; do
        if [ -d "$path/local/alpine" ]; then
            echo "$path/local/alpine"
            return 0
        fi
    done
    
    # 5. Suche im übergeordneten Verzeichnis
    if [ -d "../local/alpine" ]; then
        echo "$(cd .. && pwd)/local/alpine"
        return 0
    fi
    
    # 6. Suche nach beliebigen App-Daten
    for app_dir in /data/user/0/com.*.debug /data/data/com.*.debug; do
        if [ -d "$app_dir/local/alpine" ]; then
            echo "$app_dir/local/alpine"
            return 0
        fi
    done
    
    return 1
}

# Alpine Verzeichnis finden
ALPINE_DIR=$(find_alpine_dir)
if [ -z "$ALPINE_DIR" ]; then
    echo "❌ Alpine Rootfs nicht gefunden!"
    echo ""
    echo "📝 Bitte führe das Script aus einem der folgenden Verzeichnisse aus:"
    echo "   - .../local/ (wo alpine Ordner liegt)"
    echo "   - .../local/alpine/"
    echo ""
    echo "📝 Oder setze PREFIX manuell:"
    echo "   PREFIX=/pfad/zur/app rem_alpine.sh"
    exit 1
fi

# PREFIX aus ALPINE_DIR extrahieren (für files Verzeichnis)
PREFIX=$(dirname "$(dirname "$ALPINE_DIR")")

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
NC='\033[0m'

# ============================================================
# HILFE
# ============================================================
show_help() {
    cat << EOF
${GREEN}rem_alpine.sh - Alpine Rootfs bereinigen und packen${NC}

${YELLOW}Verwendung:${NC}
    rem_alpine.sh [OPTIONEN]

${YELLOW}Optionen:${NC}
    ${GREEN}--clean${NC}              Nur bereinigen (kein Packen)
    ${GREEN}--pack${NC}               Nur packen (kein Bereinigen)
    ${GREEN}--full${NC}               Bereinigen und packen (Standard)
    ${GREEN}--remove-ssh-keys${NC}    SSH Host Keys löschen
    ${GREEN}--output${NC}             Ausgabeverzeichnis (Standard: /sdcard)
    ${GREEN}--no-compare${NC}         Keine Größenvergleiche
    ${GREEN}--help${NC}               Diese Hilfe anzeigen

${YELLOW}Beispiele:${NC}
    rem_alpine.sh                     # Bereinigen + Packen
    rem_alpine.sh --clean             # Nur bereinigen
    rem_alpine.sh --remove-ssh-keys   # SSH Keys löschen
    rem_alpine.sh --output /data/local/tmp

${YELLOW}Erstellt:${NC}
    alpine_custom_YYYYMMDD_HHMMSS.tar.gz
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
    log_info "Analysiere Alpine Rootfs VOR der Bereinigung..."
    
    TMP_SIZE=$(get_size "$ALPINE_DIR/tmp")
    CACHE_SIZE=$(get_size "$ALPINE_DIR/var/cache")
    LOG_SIZE=$(get_size "$ALPINE_DIR/var/log")
    VAR_TMP_SIZE=$(get_size "$ALPINE_DIR/var/tmp")
    ROOT_CACHE=$(get_size "$ALPINE_DIR/root/.cache")
    HOME_CACHE=$(find "$ALPINE_DIR/home" -name ".cache" -type d -exec du -sb {} + 2>/dev/null | awk '{sum+=$1} END {print sum+0}')
    APK_CACHE=$(get_size "$ALPINE_DIR/etc/apk/cache")
    SSH_KEYS=$(get_size "$ALPINE_DIR/etc/ssh/ssh_host_"* 2>/dev/null)
    LOG_FILES=$(find "$ALPINE_DIR" -name "*.log" -type f -exec du -sb {} + 2>/dev/null | awk '{sum+=$1} END {print sum+0}')
    HISTORY=$(find "$ALPINE_DIR" -name "*.bash_history" -o -name "*.ash_history" 2>/dev/null | xargs du -sb 2>/dev/null | awk '{sum+=$1} END {print sum+0}')
    
    TOTAL_BEFORE=$(get_size "$ALPINE_DIR")
    
    echo ""
    echo "============================================================"
    echo "📊 ${CYAN}SPEICHERANALYSE VOR BEREINIGUNG${NC}"
    echo "============================================================"
    printf "%-30s %15s\n" "Verzeichnis/Datei" "Größe"
    echo "------------------------------------------------------------"
    [ "$TMP_SIZE" -gt 0 ] && printf "%-30s %15s\n" "tmp/" "$(format_size $TMP_SIZE)"
    [ "$CACHE_SIZE" -gt 0 ] && printf "%-30s %15s\n" "var/cache/" "$(format_size $CACHE_SIZE)"
    [ "$LOG_SIZE" -gt 0 ] && printf "%-30s %15s\n" "var/log/" "$(format_size $LOG_SIZE)"
    [ "$VAR_TMP_SIZE" -gt 0 ] && printf "%-30s %15s\n" "var/tmp/" "$(format_size $VAR_TMP_SIZE)"
    [ "$ROOT_CACHE" -gt 0 ] && printf "%-30s %15s\n" "root/.cache/" "$(format_size $ROOT_CACHE)"
    [ "$HOME_CACHE" -gt 0 ] && printf "%-30s %15s\n" "home/*/.cache/" "$(format_size $HOME_CACHE)"
    [ "$APK_CACHE" -gt 0 ] && printf "%-30s %15s\n" "etc/apk/cache/" "$(format_size $APK_CACHE)"
    [ "$SSH_KEYS" -gt 0 ] && printf "%-30s %15s\n" "SSH Host Keys" "$(format_size $SSH_KEYS)"
    [ "$LOG_FILES" -gt 0 ] && printf "%-30s %15s\n" "*.log Dateien" "$(format_size $LOG_FILES)"
    [ "$HISTORY" -gt 0 ] && printf "%-30s %15s\n" "History Dateien" "$(format_size $HISTORY)"
    echo "------------------------------------------------------------"
    printf "${GREEN}%-30s ${CYAN}%15s${NC}\n" "GESAMT" "$(format_size $TOTAL_BEFORE)"
    echo "============================================================"
    
    export TOTAL_BEFORE
}

# ============================================================
# ANALYSE NACHHER
# ============================================================
analyze_after() {
    local TOTAL_AFTER=$(get_size "$ALPINE_DIR")
    local SAVED=$((TOTAL_BEFORE - TOTAL_AFTER))
    local PERCENT=0
    
    if [ "$TOTAL_BEFORE" -gt 0 ]; then
        PERCENT=$(echo "scale=2; ($SAVED * 100) / $TOTAL_BEFORE" | bc)
    fi
    
    echo ""
    echo "============================================================"
    echo "📊 ${GREEN}SPEICHERERSPARNIS NACH BEREINIGUNG${NC}"
    echo "============================================================"
    printf "%-30s %15s\n" "" "Größe"
    echo "------------------------------------------------------------"
    printf "%-30s ${RED}%15s${NC}\n" "Vorher" "$(format_size $TOTAL_BEFORE)"
    printf "%-30s ${GREEN}%15s${NC}\n" "Nachher" "$(format_size $TOTAL_AFTER)"
    echo "------------------------------------------------------------"
    printf "%-30s ${YELLOW}%15s${NC}\n" "Gespart" "$(format_size $SAVED)"
    printf "%-30s ${CYAN}%15s${NC}\n" "Ersparnis in %" "${PERCENT}%"
    
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
clean_alpine() {
    log_info "Starte Bereinigung von Alpine..."
    
    cd "$ALPINE_DIR" || {
        log_error "Kann nicht in $ALPINE_DIR wechseln"
        return 1
    }
    
    log_progress "Lösche temporäre Dateien..."
    rm -rf tmp/* 2>/dev/null
    rm -rf var/cache/* 2>/dev/null
    rm -rf var/log/* 2>/dev/null
    rm -rf var/tmp/* 2>/dev/null
    rm -rf root/.cache/* 2>/dev/null
    rm -rf home/*/.cache/* 2>/dev/null
    
    log_progress "Leere APK Cache..."
    apk cache clean 2>/dev/null
    rm -rf /etc/apk/cache/* 2>/dev/null
    
    log_progress "Lösche Logs..."
    find . -name "*.log" -type f -delete 2>/dev/null
    truncate -s 0 var/log/* 2>/dev/null
    
    log_progress "Lösche History..."
    rm -f root/.bash_history 2>/dev/null
    rm -f home/*/.bash_history 2>/dev/null
    rm -f root/.ash_history 2>/dev/null
    rm -f home/*/.ash_history 2>/dev/null
    
    if [ "$REMOVE_SSH_KEYS" = "true" ]; then
        log_progress "Lösche SSH Host Keys..."
        rm -f etc/ssh/ssh_host_* 2>/dev/null
    else
        log_info "Behalte SSH Host Keys..."
    fi
    
    log_progress "Lösche Installations-Flag..."
    rm -f .alpine_installed 2>/dev/null
    
    log_progress "Räume leere Verzeichnisse auf..."
    find . -type d -empty -delete 2>/dev/null
    
    log_success "Bereinigung abgeschlossen!"
}

# ============================================================
# ROOTFS PACKEN
# ============================================================
pack_alpine() {
    log_info "Starte Packen von Alpine..."
    
    cd "$ALPINE_DIR" || {
        log_error "Kann nicht in $ALPINE_DIR wechseln"
        return 1
    }
    
    OUTPUT_FILE="$OUTPUT_DIR/alpine_custom_$TIMESTAMP.tar.gz"
    
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
        --exclude='.alpine_installed' \
        --exclude='etc/ssh/ssh_host_*' \
        --exclude='root/.bash_history' \
        --exclude='home/*/.bash_history' \
        --exclude='root/.ash_history' \
        --exclude='home/*/.ash_history' \
        --exclude='usr/share/doc/*' \
        --exclude='usr/share/man/*' \
        --exclude='usr/share/info/*' \
        --exclude='usr/share/locale/*' \
        --exclude='lib/apk/db/scripts/*' \
        . 2>/dev/null
    
    if [ $? -eq 0 ]; then
        SIZE=$(du -h "$OUTPUT_FILE" | cut -f1)
        log_success "Rootfs erstellt: $OUTPUT_FILE"
        log_success "Größe: $SIZE"
        
        if [ -d "$PREFIX/files" ]; then
            cp "$OUTPUT_FILE" "$PREFIX/files/alpine.tar.gz"
            log_success "Kopiert nach: $PREFIX/files/alpine.tar.gz"
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
    DO_COMPARE="true"
    
    while [ $# -gt 0 ]; do
        case "$1" in
            --clean) ACTION="clean"; shift ;;
            --pack) ACTION="pack"; shift ;;
            --full) ACTION="full"; shift ;;
            --remove-ssh-keys) REMOVE_SSH_KEYS="true"; shift ;;
            --output) OUTPUT_DIR="$2"; shift 2 ;;
            --no-compare) DO_COMPARE="false"; shift ;;
            --help|-h) show_help; exit 0 ;;
            *) log_error "Unbekannte Option: $1"; show_help; exit 1 ;;
        esac
    done
    
    echo "============================================================"
    echo "  ${GREEN}rem_alpine.sh${NC} - Alpine Rootfs Tool"
    echo "============================================================"
    echo ""
    echo "📁 Alpine Verzeichnis: $ALPINE_DIR"
    echo "📁 Ausgabe: $OUTPUT_DIR"
    echo ""
    
    case "$ACTION" in
        clean)
            [ "$DO_COMPARE" = "true" ] && analyze_before
            clean_alpine
            [ "$DO_COMPARE" = "true" ] && analyze_after
            ;;
        pack)
            pack_alpine
            ;;
        full)
            [ "$DO_COMPARE" = "true" ] && analyze_before
            clean_alpine
            [ "$DO_COMPARE" = "true" ] && analyze_after
            pack_alpine
            ;;
    esac
    
    echo ""
    echo "============================================================"
    log_success "Fertig!"
    echo "============================================================"
}

main "$@"