#!/system/bin/sh
# disk_usage.sh - Alpine Rootfs Speicheranalyse (BUSYBOX KOMPATIBEL)

# ============================================================
# KONFIGURATION
# ============================================================
ALPINE_DIR="/data/user/0/com.meinname.loginapp8.debug/local/alpine"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_FILE="/sdcard/disk_usage_${TIMESTAMP}.txt"
JSON_FILE="/sdcard/disk_usage_${TIMESTAMP}.json"

# Farben (BusyBox kompatibel)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ============================================================
# HELPER: Größe formatieren (BusyBox kompatibel)
# ============================================================
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
# HELPER: In Alpine ausführen
# ============================================================
run_in_alpine() {
    local cmd="$1"
    chroot "$ALPINE_DIR" /bin/sh -c "$cmd" 2>/dev/null
}

# ============================================================
# 1. APK PAKETE ANALYSIEREN
# ============================================================
analyze_apk() {
    echo ""
    echo "============================================================"
    echo "📦 APK PAKETE (Alpine)"
    echo "============================================================"
    echo ""
    echo "📋 Installierte APK Pakete:"
    echo "------------------------------------------------------------"
    
    if [ -x "$ALPINE_DIR/sbin/apk" ]; then
        echo "✅ APK gefunden: /sbin/apk"
        echo ""
        
        APK_COUNT=0
        APK_TOTAL_SIZE=0
        
        # ★ ★ ★ BUSYBOX KOMPATIBEL: Kein $(), sondern Backticks ★ ★ ★
        run_in_alpine "/sbin/apk list --installed 2>/dev/null" | while read -r line; do
            if [ -n "$line" ]; then
                # Mit cut und sed (BusyBox kompatibel)
                pkg=`echo "$line" | cut -d' ' -f1`
                version=`echo "$line" | cut -d' ' -f2 | sed 's/-[0-9]*$//'`
                
                # Größe über apk info
                pkg_size=`run_in_alpine "/sbin/apk info -s $pkg 2>/dev/null | grep 'size:' | awk '{print \$2}'"`
                
                if [ -n "$pkg_size" ] && [ "$pkg_size" -gt 0 ]; then
                    printf "  %-40s %-20s %10s\n" "$pkg" "$version" "$(format_size $pkg_size)"
                    APK_TOTAL_SIZE=$((APK_TOTAL_SIZE + pkg_size))
                else
                    printf "  %-40s %-20s %10s\n" "$pkg" "$version" "(keine Größe)"
                fi
                APK_COUNT=$((APK_COUNT + 1))
            fi
        done
        
        echo "------------------------------------------------------------"
        printf "${GREEN}%-40s ${CYAN}%10s${NC}\n" "GESAMT: $APK_COUNT Pakete" "$(format_size $APK_TOTAL_SIZE)"
        
    else
        echo "⚠️ APK nicht gefunden"
    fi
    
    echo "============================================================"
}

# ============================================================
# 2. PIP PAKETE ANALYSIEREN
# ============================================================
analyze_pip() {
    echo ""
    echo "============================================================"
    echo "🐍 PIP PAKETE (Python)"
    echo "============================================================"
    echo ""
    echo "📋 Installierte PIP Pakete:"
    echo "------------------------------------------------------------"
    
    PIP_COUNT=0
    PIP_TOTAL_SIZE=0
    
    if [ -x "$ALPINE_DIR/usr/bin/pip3" ] || [ -x "$ALPINE_DIR/usr/local/bin/pip3" ]; then
        run_in_alpine "pip3 list --format=freeze 2>/dev/null" | while read -r line; do
            if [ -n "$line" ]; then
                # ★ ★ ★ BUSYBOX KOMPATIBEL ★ ★ ★
                pkg=`echo "$line" | cut -d'=' -f1`
                version=`echo "$line" | cut -d'=' -f3`
                
                # PIP Paket Größe
                pkg_path=`run_in_alpine "python3 -c \"import $pkg; print($pkg.__file__)\" 2>/dev/null" | sed 's/__init__.py//' | sed 's/\.pyc//'`
                
                if [ -n "$pkg_path" ]; then
                    pkg_size=`run_in_alpine "du -sb $pkg_path 2>/dev/null | cut -f1"`
                    if [ -n "$pkg_size" ] && [ "$pkg_size" -gt 0 ]; then
                        printf "  %-40s %-20s %10s\n" "$pkg" "$version" "$(format_size $pkg_size)"
                        PIP_TOTAL_SIZE=$((PIP_TOTAL_SIZE + pkg_size))
                    else
                        printf "  %-40s %-20s %10s\n" "$pkg" "$version" "(keine Größe)"
                    fi
                else
                    printf "  %-40s %-20s %10s\n" "$pkg" "$version" "(keine Größe)"
                fi
                PIP_COUNT=$((PIP_COUNT + 1))
            fi
        done
        
        echo "------------------------------------------------------------"
        printf "${GREEN}%-40s ${CYAN}%10s${NC}\n" "GESAMT: $PIP_COUNT Pakete" "$(format_size $PIP_TOTAL_SIZE)"
    else
        echo "⚠️ Python3/pip3 nicht verfügbar"
    fi
    
    echo "============================================================"
}

# ============================================================
# 3. GESAMTSPEICHER
# ============================================================
analyze_total() {
    echo ""
    echo "============================================================"
    echo "📊 GESAMTSPEICHER (Alpine Rootfs)"
    echo "============================================================"
    
    cd "$ALPINE_DIR" || return
    
    TOTAL_SIZE=`du -sb --exclude=proc --exclude=sys --exclude=dev --exclude=tmp . 2>/dev/null | cut -f1`
    TOTAL_SIZE_HUMAN=`format_size $TOTAL_SIZE`
    
    echo ""
    echo "📋 Speicherübersicht:"
    echo "------------------------------------------------------------"
    
    for folder in bin etc lib root sbin usr var home; do
        if [ -d "$folder" ]; then
            size=`du -sb "$folder" 2>/dev/null | cut -f1`
            if [ -n "$size" ] && [ "$size" -gt 0 ]; then
                printf "%-30s %15s\n" "$folder/" "$(format_size $size)"
            fi
        fi
    done
    
    echo "------------------------------------------------------------"
    printf "${GREEN}%-30s ${CYAN}%15s${NC}\n" "GESAMT" "$TOTAL_SIZE_HUMAN"
    echo "============================================================"
}

# ============================================================
# 4. JSON AUSGABE
# ============================================================
generate_json() {
    echo ""
    echo "============================================================"
    echo "📄 JSON AUSGABE"
    echo "============================================================"
    
    # APK Pakete sammeln
    APK_JSON=""
    if [ -x "$ALPINE_DIR/sbin/apk" ]; then
        run_in_alpine "/sbin/apk list --installed 2>/dev/null" | while read -r line; do
            pkg=`echo "$line" | cut -d' ' -f1`
            version=`echo "$line" | cut -d' ' -f2 | sed 's/-[0-9]*$//'`
            echo "    {\"name\": \"$pkg\", \"version\": \"$version\"},"
        done
    fi
    
    # PIP Pakete sammeln
    PIP_JSON=""
    if [ -x "$ALPINE_DIR/usr/bin/pip3" ] || [ -x "$ALPINE_DIR/usr/local/bin/pip3" ]; then
        run_in_alpine "pip3 list --format=freeze 2>/dev/null" | while read -r line; do
            if [ -n "$line" ]; then
                pkg=`echo "$line" | cut -d'=' -f1`
                version=`echo "$line" | cut -d'=' -f3`
                echo "    {\"name\": \"$pkg\", \"version\": \"$version\"},"
            fi
        done
    fi
    
    # Ordner-Größen
    FOLDER_JSON=""
    cd "$ALPINE_DIR" || return
    for folder in bin etc lib root sbin usr var home; do
        if [ -d "$folder" ]; then
            size=`du -sb "$folder" 2>/dev/null | cut -f1`
            if [ -n "$size" ]; then
                size_human=`format_size $size`
                FOLDER_JSON="$FOLDER_JSON    {\"name\": \"$folder\", \"size_bytes\": $size, \"size_human\": \"$size_human\"},"
            fi
        fi
    done
    
    # JSON schreiben
    cat > "$JSON_FILE" << EOF
{
  "timestamp": "$(date '+%Y-%m-%d %H:%M:%S')",
  "rootfs": "$ALPINE_DIR",
  "total_size_bytes": $TOTAL_SIZE,
  "total_size_human": "$TOTAL_SIZE_HUMAN",
  "apk_packages": [
${APK_JSON}
  ],
  "pip_packages": [
${PIP_JSON}
  ],
  "folders": [
${FOLDER_JSON}
  ]
}
EOF
    
    # Letztes Komma entfernen (BusyBox kompatibel)
    sed -i 's/},$/}/g' "$JSON_FILE"
    sed -i 's/,$//g' "$JSON_FILE"
    
    echo "✅ JSON gespeichert: $JSON_FILE"
    echo "============================================================"
}

# ============================================================
# HAUPT-FUNKTION
# ============================================================
main() {
    echo "============================================================"
    echo "  🔍 Alpine Rootfs - Speicheranalyse (BUSYBOX KOMPATIBEL)"
    echo "============================================================"
    echo ""
    echo "📁 Rootfs: $ALPINE_DIR"
    echo ""
    
    if [ ! -d "$ALPINE_DIR" ]; then
        echo "❌ Alpine Verzeichnis nicht gefunden: $ALPINE_DIR"
        exit 1
    fi
    
    analyze_apk
    analyze_pip
    analyze_total
    generate_json
    
    echo ""
    echo "============================================================"
    echo "✅ Analyse abgeschlossen!"
    echo "📁 Text: $OUTPUT_FILE"
    echo "📁 JSON: $JSON_FILE"
    echo "============================================================"
}

main "$@"