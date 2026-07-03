#!/system/bin/sh
# rem_alpine.sh - Alpine Rootfs bereinigen und packen mit Speicheranalyse

# ============================================================
# KONFIGURATION
# ============================================================
PREFIX="${PREFIX:-/data/user/0/com.meinname.loginapp8.debug}"
ALPINE_DIR="$PREFIX/local/alpine"
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
NC='\033[0m' # No Color

# ============================================================
# HILFE
# ============================================================
show_help() {
    cat << EOF
${GREEN}rem_alpine.sh - Alpine Rootfs bereinigen und packen${NC}

${YELLOW}Verwendung:${NC}
    rem_alpine.sh [OPTIONEN]

${YELLOW}Optionen:${NC}
    ${GREEN}--clean${NC}      Nur bereinigen (kein Packen)
    ${GREEN}--pack${NC}       Nur packen (kein Bereinigen)
    ${GREEN}--full${NC}       Bereinigen und packen (Standard)
    ${GREEN}--output${NC}     Ausgabeverzeichnis (Standard: /sdcard)
    ${GREEN}--no-compare${NC} Keine Größenvergleiche
    ${GREEN}--help${NC}       Diese Hilfe anzeigen

${YELLOW}Beispiele:${NC}
    rem_alpine.sh                     # Bereinigen + Packen + Vergleich
    rem_alpine.sh --clean             # Nur bereinigen
    rem_alpine.sh --pack --output /data/local/tmp
    rem_alpine.sh --no-compare        # Ohne Größenvergleich

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
    local dir="$1"
    du -sb "$dir" 2>/dev/null | cut -f1
}

get_size_human() {
    local size=$(get_size "$1")
    if [ -z "$size" ]; then
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

format_size() {
    local size="$1"
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
    
    # Größe der einzelnen Verzeichnisse
    TMP_SIZE=$(get_size "$ALPINE_DIR/tmp")
    CACHE_SIZE=$(get_size "$ALPINE_DIR/var/cache")
    LOG_SIZE=$(get_size "$ALPINE_DIR/var/log")
    VAR_TMP_SIZE=$(get_size "$ALPINE_DIR/var/tmp")
    ROOT_CACHE=$(get_size "$ALPINE_DIR/root/.cache")
    HOME_CACHE=$(get_size "$ALPINE_DIR/home/*/.cache" 2>/dev/null)
    APK_CACHE=$(get_size "$ALPINE_DIR/etc/apk/cache")
    SSH_KEYS=$(get_size "$ALPINE_DIR/etc/ssh/ssh_host_*" 2>/dev/null)
    LOG_FILES=$(find "$ALPINE_DIR" -name "*.log" -type f -exec du -sb {} + 2>/dev/null | awk '{sum+=$1} END {print sum+0}')
    HISTORY=$(find "$ALPINE_DIR" -name "*.bash_history" -o -name "*.ash_history" 2>/dev/null | xargs du -sb 2>/dev/null | awk '{sum+=$1} END {print sum+0}')
    
    # Gesamtgröße
    TOTAL_BEFORE=$(get_size "$ALPINE_DIR")
    
    # Zusammenfassung
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
    
    # Speichere Werte für später
    export TOTAL_BEFORE
    export TMP_SIZE CACHE_SIZE LOG_SIZE VAR_TMP_SIZE
    export ROOT_CACHE HOME_CACHE APK_CACHE SSH_KEYS LOG_FILES HISTORY
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
    
    # Balkendiagramm
    if [ "$PERCENT" -gt 0 ]; then
        echo ""
        echo "📊 ${CYAN}Balkendiagramm:${NC}"
        local BARS=$((PERCENT / 5))
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
    
    # 1. Temporäre Dateien
    log_progress "Lösche temporäre Dateien..."
    rm -rf tmp/* 2>/dev/null
    rm -rf var/cache/* 2>/dev/null
    rm -rf var/log/* 2>/dev/null
    rm -rf var/tmp/* 2>/dev/null
    rm -rf root/.cache/* 2>/dev/null
    rm -rf home/*/.cache/* 2>/dev/null
    
    # 2. APK Cache
    log_progress "Leere APK Cache..."
    apk cache clean 2>/dev/null
    rm -rf /etc/apk/cache/* 2>/dev/null
    
    # 3. Logs
    log_progress "Lösche Logs..."
    find . -name "*.log" -type f -delete 2>/dev/null
    truncate -s 0 var/log/* 2>/dev/null
    
    # 4. History
    log_progress "Lösche History..."
    rm -f root/.bash_history 2>/dev/null
    rm -f home/*/.bash_history 2>/dev/null
    rm -f root/.ash_history 2>/dev/null
    rm -f home/*/.ash_history 2>/dev/null
    
    # 5. SSH Host Keys (nur wenn gewünscht)
    if [ "$REMOVE_SSH_KEYS" = "true" ]; then
        log_progress "Lösche SSH Host Keys..."
        rm -f etc/ssh/ssh_host_* 2>/dev/null
    else
        log_info "Behalte SSH Host Keys..."
    fi
    
    # 6. Installations-Flag
    log_progress "Lösche Installations-Flag..."
    rm -f .alpine_installed 2>/dev/null
    
    # 7. Leere Verzeichnisse
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
    # Defaults
    ACTION="full"
    REMOVE_SSH_KEYS="false"
    DO_COMPARE="true"
    
    # Argumente parsen
    while [ $# -gt 0 ]; do
        case "$1" in
            --clean)
                ACTION="clean"
                shift
                ;;
            --pack)
                ACTION="pack"
                shift
                ;;
            --full)
                ACTION="full"
                shift
                ;;
            --remove-ssh-keys)
                REMOVE_SSH_KEYS="true"
                shift
                ;;
            --output)
                OUTPUT_DIR="$2"
                shift 2
                ;;
            --no-compare)
                DO_COMPARE="false"
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                log_error "Unbekannte Option: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Banner
    echo "============================================================"
    echo "  ${GREEN}rem_alpine.sh${NC} - Alpine Rootfs Tool"
    echo "============================================================"
    echo ""
    
    # Prüfungen
    if [ ! -d "$ALPINE_DIR" ]; then
        log_error "Alpine Verzeichnis nicht gefunden: $ALPINE_DIR"
        exit 1
    fi
    log_success "Alpine Verzeichnis: $ALPINE_DIR"
    
    # Aktion ausführen
    case "$ACTION" in
        clean)
            if [ "$DO_COMPARE" = "true" ]; then
                analyze_before
            fi
            clean_alpine
            if [ "$DO_COMPARE" = "true" ]; then
                analyze_after
            fi
            ;;
        pack)
            pack_alpine
            ;;
        full)
            if [ "$DO_COMPARE" = "true" ]; then
                analyze_before
            fi
            clean_alpine
            if [ "$DO_COMPARE" = "true" ]; then
                analyze_after
            fi
            pack_alpine
            ;;
    esac
    
    echo ""
    echo "============================================================"
    log_success "Fertig!"
    echo "============================================================"
}

# ============================================================
# AUSFÜHRUNG
# ============================================================
main "$@"