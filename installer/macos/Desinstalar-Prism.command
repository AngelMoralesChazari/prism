#!/usr/bin/env bash
# Desinstalador grafico nativo para macOS (doble clic desde Finder)
set -e

if [ "$(uname -s)" != "Darwin" ]; then
    echo "Este asistente grafico es exclusivo para macOS."
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
UNINSTALL_SH=""
for cand in \
    "$SCRIPT_DIR/uninstall.sh" \
    "$SCRIPT_DIR/.prism-uninstall.sh" \
    "$SCRIPT_DIR/installer/macos/uninstall.sh" \
    "/Library/Prism/uninstall.sh"; do
    if [ -f "$cand" ]; then
        UNINSTALL_SH="$cand"
        break
    fi
done

ICON_PATH=""
for cand in \
    "$SCRIPT_DIR/resources/prism-icon.png" \
    "$SCRIPT_DIR/.prism-resources/prism-icon.png" \
    "$SCRIPT_DIR/installer/macos/resources/prism-icon.png" \
    "/Library/Prism/resources/prism-icon.png"; do
    if [ -f "$cand" ]; then
        ICON_PATH="$cand"
        break
    fi
done

run_osascript() {
    osascript - "$@"
}

if ! command -v osascript >/dev/null 2>&1; then
    exec bash "$UNINSTALL_SH"
fi

HAS_SYSTEM=0
HAS_USER=0
if [ -d /Library/Prism ] || [ -f /etc/paths.d/prism ]; then
    HAS_SYSTEM=1
fi
if [ -d "$HOME/.prism" ]; then
    HAS_USER=1
fi

if [ "$HAS_SYSTEM" -eq 0 ] && [ "$HAS_USER" -eq 0 ]; then
    osascript <<EOF
display dialog "No se encontró ninguna instalación activa de Prism en este equipo." with title "Desinstalador de Prism" buttons {"Aceptar"} default button "Aceptar"
EOF
    exit 0
fi

MSG="¿Deseas desinstalar Prism y eliminar el comando prc del equipo?"
[ "$HAS_SYSTEM" -eq 1 ] && MSG="${MSG}"$'\n\n'"- Se eliminará /Library/Prism, /usr/local/bin/prc y /etc/paths.d/prism."
[ "$HAS_USER" -eq 1 ] && MSG="${MSG}"$'\n'"- Se eliminará $HOME/.prism."

CONFIRM_RES=$(osascript - "$ICON_PATH" "$MSG" <<'EOF' 2>/dev/null || true
on run argv
    set iconFile to item 1 of argv
    set msgText to item 2 of argv
    try
        if iconFile is not "" then
            set dlg to display dialog msgText with title "Desinstalador de Prism" buttons {"Cancelar", "Desinstalar"} default button "Cancelar" with icon POSIX file iconFile
        else
            set dlg to display dialog msgText with title "Desinstalador de Prism" buttons {"Cancelar", "Desinstalar"} default button "Cancelar"
        end if
        return button returned of dlg
    on error
        return "Cancelar"
    end try
end run
EOF
)

if [ "$CONFIRM_RES" != "Desinstalar" ]; then
    exit 0
fi

if [ "$HAS_USER" -eq 1 ]; then
    bash "$UNINSTALL_SH" --user >/dev/null
fi

if [ "$HAS_SYSTEM" -eq 1 ]; then
    osascript - "$UNINSTALL_SH" <<'EOF'
on run argv
    set scriptPath to item 1 of argv
    do shell script "/bin/bash " & quoted form of scriptPath & " --system" with administrator privileges
end run
EOF
fi

osascript - "$ICON_PATH" <<'EOF'
on run argv
    set iconFile to item 1 of argv
    if iconFile is not "" then
        display dialog "Prism se desinstaló correctamente." with title "Desinstalador de Prism" buttons {"Aceptar"} default button "Aceptar" with icon POSIX file iconFile
    else
        display dialog "Prism se desinstaló correctamente." with title "Desinstalador de Prism" buttons {"Aceptar"} default button "Aceptar"
    end if
end run
EOF
