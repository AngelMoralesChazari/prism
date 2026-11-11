#!/usr/bin/env bash
# Asistente grafico de instalacion de Prism para macOS (osascript Cocoa nativo)
set -e

if [ "$(uname -s)" != "Darwin" ]; then
    echo "Este asistente grafico es exclusivo para macOS. En Linux usa ./install.sh"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/prism.jar" ] || [ -f "$SCRIPT_DIR/lib/prism.jar" ]; then
    PACKAGE_ROOT="$SCRIPT_DIR"
else
    PACKAGE_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
fi

INSTALL_SH="$PACKAGE_ROOT/install.sh"
ICON_PATH=""
for cand in \
    "$PACKAGE_ROOT/installer/macos/resources/prism-icon.png" \
    "$SCRIPT_DIR/resources/prism-icon.png" \
    "$PACKAGE_ROOT/prism-icon.png"; do
    if [ -f "$cand" ]; then
        ICON_PATH="$cand"
        break
    fi
done

if [ ! -f "$INSTALL_SH" ]; then
    osascript -e 'display dialog "No se encontró el archivo install.sh dentro del paquete de Prism." with title "Instalador de Prism" buttons {"Aceptar"} default button "Aceptar" with icon stop'
    exit 1
fi

# 1. Pantalla de bienvenida
WELCOME_MSG=$'Bienvenido al asistente de instalación de Prism v1.1.0 para macOS.\n\nEste asistente instalará el compilador y runtime (prc) y lo integrará automáticamente con Terminal y Visual Studio Code / Cursor.\n\n¿Deseas continuar?'
WELCOME_BTN=$(osascript - "$ICON_PATH" "$WELCOME_MSG" <<'EOF' 2>/dev/null || true
on run argv
    set iconFile to item 1 of argv
    set msgText to item 2 of argv
    try
        if iconFile is not "" then
            set dlg to display dialog msgText with title "Instalador de Prism v1.1.0" buttons {"Cancelar", "Continuar >"} default button "Continuar >" with icon POSIX file iconFile
        else
            set dlg to display dialog msgText with title "Instalador de Prism v1.1.0" buttons {"Cancelar", "Continuar >"} default button "Continuar >"
        end if
        return button returned of dlg
    on error
        return "Cancelar"
    end try
end run
EOF
)

if [ "$WELCOME_BTN" != "Continuar >" ]; then
    exit 0
fi

# 2. Verificar Java 17+ o runtime integrado
find_java() {
    if [ -x "$PACKAGE_ROOT/runtime/bin/java" ]; then
        printf '%s\n' "$PACKAGE_ROOT/runtime/bin/java"
        return 0
    fi
    if [ -x "/usr/libexec/java_home" ]; then
        local jh
        jh="$(/usr/libexec/java_home -v 17+ 2>/dev/null || true)"
        if [ -n "$jh" ] && [ -x "$jh/bin/java" ]; then
            printf '%s\n' "$jh/bin/java"
            return 0
        fi
    fi
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        printf '%s\n' "$JAVA_HOME/bin/java"
        return 0
    fi
    local cand
    for cand in \
        /Library/Java/JavaVirtualMachines/*/Contents/Home/bin/java \
        /opt/homebrew/opt/openjdk*/bin/java \
        /usr/local/opt/openjdk*/bin/java; do
        if [ -x "$cand" ]; then
            printf '%s\n' "$cand"
            return 0
        fi
    done
    return 1
}

JAVA_BIN="$(find_java || true)"
if [ -z "$JAVA_BIN" ]; then
    JAVA_MSG=$'No se detectó Java (OpenJDK 17 o superior) en este Mac.\n\nSi descargaste el paquete portátil sin Java integrado, instala OpenJDK 17+ (desde Adoptium o Homebrew) o descarga el instalador .pkg completo para macOS.'
    JAVA_BTN=$(osascript - "$ICON_PATH" "$JAVA_MSG" <<'EOF' 2>/dev/null || true
on run argv
    set iconFile to item 1 of argv
    set msgText to item 2 of argv
    try
        if iconFile is not "" then
            set dlg to display dialog msgText with title "Java 17+ requerido" buttons {"Cerrar", "Abrir adoptium.net"} default button "Abrir adoptium.net" with icon POSIX file iconFile
        else
            set dlg to display dialog msgText with title "Java 17+ requerido" buttons {"Cerrar", "Abrir adoptium.net"} default button "Abrir adoptium.net"
        end if
        return button returned of dlg
    on error
        return "Cerrar"
    end try
end run
EOF
)
    if [ "$JAVA_BTN" = "Abrir adoptium.net" ]; then
        open "https://adoptium.net"
    fi
    exit 1
fi

# 3. Elegir ambito de instalacion
CHOICE=$(osascript <<'EOF' 2>/dev/null || true
set opciones to {"Usuario actual (~/.prism) - Sin contraseña", "Todo el equipo (/Library/Prism) - Recomendado"}
set elegido to choose from list opciones with title "Tipo de instalación" with prompt "Elige dónde instalar Prism (prc):" default items {"Usuario actual (~/.prism) - Sin contraseña"} OK button name "Instalar ahora" cancel button name "Cancelar"
if elegido is false then
    return ""
end if
return item 1 of elegido
EOF
)

if [ -z "$CHOICE" ]; then
    exit 0
fi

if [[ "$CHOICE" == *"Todo el equipo"* ]]; then
    EXAMPLES_DEST="/Library/Prism/examples"
    osascript - "$INSTALL_SH" "$PACKAGE_ROOT" <<'EOF'
on run argv
    set installSh to item 1 of argv
    set pkgRoot to item 2 of argv
    do shell script "/bin/bash " & quoted form of installSh & " && (mkdir -p /Library/Prism/examples && cp -f " & quoted form of pkgRoot & "/examples/*.pr /Library/Prism/examples/ 2>/dev/null || true)" with administrator privileges
end run
EOF
    SCOPE_LABEL="/Library/Prism (global para todas las terminales e IDEs)"
else
    bash "$INSTALL_SH" </dev/null
    EXAMPLES_DEST="$HOME/.prism/examples"
    if [ -d "$PACKAGE_ROOT/examples" ]; then
        mkdir -p "$EXAMPLES_DEST"
        cp -f "$PACKAGE_ROOT/examples/"*.pr "$EXAMPLES_DEST/" 2>/dev/null || true
    fi
    SCOPE_LABEL="$HOME/.prism (usuario actual)"
fi

# 4. Resumen final
DONE_MSG="Prism se instaló correctamente en:"$'\n'"${SCOPE_LABEL}"$'\n\n'"Abre una nueva Terminal y prueba:"$'\n'"  prc --version"$'\n'"  prc run \"${EXAMPLES_DEST}/prueba_nl.pr\""

DONE_BTN=$(osascript - "$ICON_PATH" "$DONE_MSG" <<'EOF' 2>/dev/null || true
on run argv
    set iconFile to item 1 of argv
    set msgText to item 2 of argv
    try
        if iconFile is not "" then
            set dlg to display dialog msgText with title "Instalación completada" buttons {"Abrir ejemplos", "Finalizar"} default button "Finalizar" with icon POSIX file iconFile
        else
            set dlg to display dialog msgText with title "Instalación completada" buttons {"Abrir ejemplos", "Finalizar"} default button "Finalizar"
        end if
        return button returned of dlg
    on error
        return "Finalizar"
    end try
end run
EOF
)

if [ "$DONE_BTN" = "Abrir ejemplos" ] && [ -d "$EXAMPLES_DEST" ]; then
    open "$EXAMPLES_DEST"
fi
