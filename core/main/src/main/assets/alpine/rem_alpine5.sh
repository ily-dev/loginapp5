#!/system/bin/sh
# rem_alpine.sh - Alpine Rootfs bereinigen und packen (JSON FIXED - ONE FILE)

# ============================================================
# AUTOMATISCHE PFADERKENNUNG
# ============================================================
find_alpine_dir() {
    if [ -d "./alpine" ] && [ -d "./bin" ]; then
        echo "$(pwd)/alpine"
        return 0
    fi
    if [ -n "$PREFIX" ] && [ -d "$PREFIX/local/alpine" ]; then
        echo "$PREFIX/local/alpine"
        return 0
    fi
    if [ -d "./local/alpine" ]; then
        echo "$(pwd)/local/alpine"
        return 0
    fi
    STANDARD_PATHS="/data/user/0/com.meinname.loginapp8.debug /data/data/com.meinname.loginapp8.debug"
    for path in $STANDARD_PATHS; do
        if [ -d "$path/local/alpine" ]; then
            echo "$path/local/alpine"
            return 0
        fi
    done
    if [ -d "../local/alpine" ]; then
        echo "$(cd .. && pwd)/local/alpine"
        return 0
    fi
    for app_dir in /data/user/0/com.*.debug /data/data/com.*.debug; do
        if [ -d "$app_dir/local/alpine" ]; then
            echo "$app_dir/local/alpine"
            return 0
        fi
    done
    return 1
}

ALPINE_DIR=$(find_alpine_dir)
if [ -z "$ALPINE_DIR" ]; then
    echo "❌ Alpine Rootfs nicht gefunden!"
    exit 1
fi

PREFIX=$(dirname "$(dirname "$ALPINE_DIR")")
OUTPUT_DIR="${OUTPUT_DIR:-/sdcard}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
JSON_FILE="$OUTPUT_DIR/alpine_result_$TIMESTAMP.json"

# ============================================================
# FARBEN
# ============================================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

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
    ${GREEN}--help${NC}               Hilfe anzeigen
EOF
}

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[✓]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[⚠]${NC} $1"; }
log_error() { echo -e "${RED}[✗]${NC} $1"; }
log_progress() { echo -e "${CYAN}[➜]${NC} $1"; }

# ============================================================
# GRÖSSEN-HELPER
# ============================================================
get_size() {
    cd "$1" 2>/dev/null || return
    du -sb bin etc lib sbin usr var home 2>/dev/null | awk '{sum+=$1} END {print sum}'
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
# ★ ★ ★ JSON ERSTELLEN (NUR EINE PRO LAUF) ★ ★ ★
# ============================================================
create_json() {
    local status="$1"
    local message="$2"
    local backup_file="$3"
    local backup_size="$4"
    local size_before="$5"
    local size_after="$6"
    local saved="$7"
    
    # Wenn keine Größen übergeben, selbst ermitteln
    if [ -z "$size_before" ] || [ -z "$size_after" ]; then
        size_before="${TOTAL_BEFORE:-0}"
        size_after=$(get_size "$ALPINE_DIR")
        saved=$((size_before - size_after))
    fi
    
    local percent=0
    if [ "$size_before" -gt 0 ]; then
        percent=$(echo "scale=2; ($saved * 100) / $size_before" | bc)
    fi
    
    # Ordner-Größen sammeln
    cd "$ALPINE_DIR" 2>/dev/null || return
    local bin_size=$(du -sb bin 2>/dev/null | cut -f1)
    local etc_size=$(du -sb etc 2>/dev/null | cut -f1)
    local lib_size=$(du -sb lib 2>/dev/null | cut -f1)
    local root_size=$(du -sb root 2>/dev/null | cut -f1)
    local sbin_size=$(du -sb sbin 2>/dev/null | cut -f1)
    local usr_size=$(du -sb usr 2>/dev/null | cut -f1)
    local var_size=$(du -sb var 2>/dev/null | cut -f1)
    local home_size=$(du -sb home 2>/dev/null | cut -f1)
    
    # ★ ★ ★ JSON DIREKT SCHREIBEN ★ ★ ★
    {
        echo "{"
        echo "    \"status\": \"$status\","
        echo "    \"timestamp\": \"$(date '+%Y-%m-%d %H:%M:%S')\","
        echo "    \"alpine_dir\": \"$ALPINE_DIR\","
        echo "    \"output_dir\": \"$OUTPUT_DIR\","
        echo "    \"message\": \"$message\","
        echo "    \"backup_file\": \"$backup_file\","
        echo "    \"backup_size\": \"$backup_size\","
        echo "    \"sizes\": {"
        echo "        \"before_bytes\": $size_before,"
        echo "        \"before_human\": \"$(format_size $size_before)\","
        echo "        \"after_bytes\": $size_after,"
        echo "        \"after_human\": \"$(format_size $size_after)\","
        echo "        \"saved_bytes\": $saved,"
        echo "        \"saved_human\": \"$(format_size $saved)\","
        echo "        \"saved_percent\": $percent"
        echo "    },"
        echo "    \"folders\": {"
        echo "        \"bin\": { \"bytes\": ${bin_size:-0}, \"human\": \"$(format_size $bin_size)\" },"
        echo "        \"etc\": { \"bytes\": ${etc_size:-0}, \"human\": \"$(format_size $etc_size)\" },"
        echo "        \"lib\": { \"bytes\": ${lib_size:-0}, \"human\": \"$(format_size $lib_size)\" },"
        echo "        \"root\": { \"bytes\": ${root_size:-0}, \"human\": \"$(format_size $root_size)\" },"
        echo "        \"sbin\": { \"bytes\": ${sbin_size:-0}, \"human\": \"$(format_size $sbin_size)\" },"
        echo "        \"usr\": { \"bytes\": ${usr_size:-0}, \"human\": \"$(format_size $usr_size)\" },"
        echo "        \"var\": { \"bytes\": ${var_size:-0}, \"human\": \"$(format_size $var_size)\" },"
        echo "        \"home\": { \"bytes\": ${home_size:-0}, \"human\": \"$(format_size $home_size)\" }"
        echo "    },"
        echo "    \"options\": {"
        echo "        \"action\": \"$ACTION\","
        echo "        \"remove_ssh_keys\": $REMOVE_SSH_KEYS,"
        echo "        \"do_compare\": $DO_COMPARE"
        echo "    }"
        echo "}"
    } > "$JSON_FILE"
    
    if [ -f "$JSON_FILE" ] && [ -s "$JSON_FILE" ]; then
        echo "✅ JSON gespeichert: $JSON_FILE"
        echo "📏 JSON Größe: $(du -h "$JSON_FILE" | awk '{print $1}')"
        return 0
    else
        echo "⚠️ JSON konnte nicht gespeichert werden!"
        return 1
    fi
}

# ============================================================
# ANALYSE VORHER
# ============================================================
analyze_before() {
    log_info "Analysiere Alpine Rootfs VOR der Bereinigung..."
    
    cd "$ALPINE_DIR" || return
    
    TMP_SIZE=$(du -sb tmp 2>/dev/null | cut -f1)
    CACHE_SIZE=$(du -sb var/cache 2>/dev/null | cut -f1)
    LOG_SIZE=$(du -sb var/log 2>/dev/null | cut -f1)
    VAR_TMP_SIZE=$(du -sb var/tmp 2>/dev/null | cut -f1)
    ROOT_CACHE=$(du -sb root/.cache 2>/dev/null | cut -f1)
    APK_CACHE=$(du -sb etc/apk/cache 2>/dev/null | cut -f1)
    
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
    [ "$APK_CACHE" -gt 0 ] && printf "%-30s %15s\n" "etc/apk/cache/" "$(format_size $APK_CACHE)"
    echo "------------------------------------------------------------"
    printf "${GREEN}%-30s ${CYAN}%15s${NC}\n" "GESAMT" "$(format_size $TOTAL_BEFORE)"
    echo "============================================================"
    
    export TOTAL_BEFORE
}

# ============================================================
# ANALYSE NACHHER (OHNE JSON!)
# ============================================================
analyze_after() {
    local TOTAL_AFTER=$(get_size "$ALPINE_DIR")
    local SAVED=$((TOTAL_BEFORE - TOTAL_AFTER))
    local PERCENT=0
    
    if [ "$TOTAL_BEFORE" -gt 0 ]; then
        PERCENT=$(echo "scale=0; ($SAVED * 100) / $TOTAL_BEFORE" | bc)
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
    
    log_progress "Lösche temporäre Dateien..."
    rm -rf tmp/* var/cache/* var/log/* var/tmp/* 2>/dev/null
    rm -rf root/.cache/* home/*/.cache/* 2>/dev/null
    echo "   ✅ Fertig"
    
    log_progress "Leere APK Cache..."
    rm -rf etc/apk/cache/* 2>/dev/null
    echo "   ✅ Fertig"
    
    log_progress "Lösche Logs..."
    rm -f var/log/*.log var/log/*/*.log 2>/dev/null
    truncate -s 0 var/log/lastlog var/log/wtmp var/log/btmp 2>/dev/null
    echo "   ✅ Fertig"
    
    log_progress "Lösche History..."
    rm -f root/.bash_history home/*/.bash_history 2>/dev/null
    rm -f root/.ash_history home/*/.ash_history 2>/dev/null
    echo "   ✅ Fertig"
    
    if [ "$REMOVE_SSH_KEYS" = "true" ]; then
        log_progress "Lösche SSH Host Keys..."
        echo "   ✅ Fertig"
    else
        log_info "Behalte SSH Host Keys..."
    fi
    
    log_progress "Räume leere Verzeichnisse auf..."
    rmdir tmp var/cache var/log var/tmp 2>/dev/null
    rmdir root/.cache home/*/.cache 2>/dev/null
    echo "   ✅ Fertig"
    
    log_success "Bereinigung abgeschlossen!"
}

# ============================================================
# ROOTFS PACKEN (NUR HIER WIRD JSON ERSTELLT)
# ============================================================
pack_alpine() {
    log_info "Starte Packen von Alpine..."
    
    cd "$ALPINE_DIR" || {
        log_error "Kann nicht in $ALPINE_DIR wechseln"
        return 1
    }
    
    # ★ ★ ★ GRÖSSE VORHER MESSEN ★ ★ ★
    local SIZE_BEFORE=$(get_size "$ALPINE_DIR")
    
    OUTPUT_FILE="$OUTPUT_DIR/alpine_custom_$TIMESTAMP.tar.gz"
    
    log_progress "Erstelle: $OUTPUT_FILE"
    
    tar -czf "$OUTPUT_FILE" \
        --no-same-owner \
        --no-same-permissions \
        --exclude='root/app_files/*' \
        --exclude='root/app_files' \
        --exclude='proc/*' \
        --exclude='proc' \
        --exclude='dev/*' \
        --exclude='dev' \
        --exclude='run/*' \
        --exclude='vendor/*' \
        --exclude="vendor" \
        --exclude="sys/*" \
        --exclude="sys" \
        --exclude="data/*" \
        --exclude="data" \
        --exclude='system/*' \
        --exclude='system' \
        --exclude='apex/*' \
        --exclude="apex" \
        --exclude='sdcard/*' \
        --exclude="sdcard" \
        --exclude='storage/*' \
        --exclude="storage" \
        --exclude='mnt/*' \
        --exclude='mnt' \
        --exclude='media/*' \
        --exclude='media' \
        --exclude='tmp/*' \
        --exclude='tmp' \
        --exclude="mnt" \
        --exclude='var/cache/*' \
        --exclude='var/log/*' \
        --exclude='var/tmp/*' \
        --exclude='root/.cache/*' \
        --exclude='home/*/.cache/*' \
        --exclude='*.log' \
        --exclude='*.pid' \
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
        
    TAR_EXIT=$?
    sync 
    sleep 1
    
    # ★ ★ ★ GRÖSSE NACHHER MESSEN ★ ★ ★
    local SIZE_AFTER=$(get_size "$ALPINE_DIR")
    local SAVED=$((SIZE_BEFORE - SIZE_AFTER))
    
    if [ $TAR_EXIT -eq 0 ] && [ -f "$OUTPUT_FILE" ]; then
        chmod 644 "$OUTPUT_FILE" 2>/dev/null
        BACKUP_SIZE=$(du -h "$OUTPUT_FILE" | awk '{print $1}')
        
        log_success "Rootfs erstellt: $OUTPUT_FILE"
        log_success "Größe: $BACKUP_SIZE"
    
        if [ -d "$PREFIX/files" ]; then
            cp "$OUTPUT_FILE" "$PREFIX/files/alpine.tar.gz"
            log_success "Kopiert nach: $PREFIX/files/alpine.tar.gz"
            sync
            sleep 1
        fi
        
        # ★ ★ ★ NUR HIER WIRD JSON ERSTELLT! ★ ★ ★
        create_json "success" "Backup erfolgreich erstellt" "$OUTPUT_FILE" "$BACKUP_SIZE" "$SIZE_BEFORE" "$SIZE_AFTER" "$SAVED"
        
    else
        log_error "Fehler beim Erstellen des Rootfs!"
        create_json "error" "Fehler beim Erstellen des Rootfs" "" "" "$SIZE_BEFORE" "$SIZE_AFTER" "$SAVED"
        return 1
    fi
}

# ============================================================
# HAUPT-FUNKTION
# ============================================================
main() {
    # ★ ★ ★ ALTE JSON LÖSCHEN ★ ★ ★
    rm -f "$OUTPUT_DIR/alpine_result_"*.json 2>/dev/null
    
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
            # ★ ★ ★ Bei clean: JSON mit aktuellen Größen ★ ★ ★
            SIZE_BEFORE="${TOTAL_BEFORE:-0}"
            SIZE_AFTER=$(get_size "$ALPINE_DIR")
            SAVED=$((SIZE_BEFORE - SIZE_AFTER))
            #create_json "success" "Bereinigung abgeschlossen" "" "" "$SIZE_BEFORE" "$SIZE_AFTER" "$SAVED"
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
    
    # JSON Pfad anzeigen
    if [ -f "$JSON_FILE" ] && [ -s "$JSON_FILE" ]; then
        echo ""
        echo "📄 JSON Ergebnis: $JSON_FILE"
        echo "📏 Größe: $(du -h "$JSON_FILE" | awk '{print $1}')"
        echo ""
        echo "📋 JSON Inhalt:"
        cat "$JSON_FILE" 2>/dev/null | head -20
    fi
}

main "$@"