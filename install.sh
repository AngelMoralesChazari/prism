#!/usr/bin/env bash
# Script de instalacion universal de Prism (prc) v1.1.0 (Linux y macOS)
# Disenado para integrar prc en el PATH del sistema para cualquier IDE o terminal.
set -e

OS_NAME="$(uname -s)"

echo "======================================================="
echo "       INSTALADOR OFICIAL DE PRISM (prc) v1.1.0        "
echo "======================================================="
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 1. Resolver ejecutable de Java
resolve_java_bin() {
    if [ -x "$SCRIPT_DIR/runtime/bin/java" ]; then
        printf '%s\n' "$SCRIPT_DIR/runtime/bin/java"
        return 0
    fi
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        printf '%s\n' "$JAVA_HOME/bin/java"
        return 0
    fi
    if [ "$OS_NAME" = "Darwin" ] && [ -x "/usr/libexec/java_home" ]; then
        local mac_jh=""
        mac_jh="$(/usr/libexec/java_home -v 17+ 2>/dev/null || /usr/libexec/java_home 2>/dev/null || true)"
        if [ -n "$mac_jh" ] && [ -x "$mac_jh/bin/java" ]; then
            printf '%s\n' "$mac_jh/bin/java"
            return 0
        fi
    fi
    if command -v java >/dev/null 2>&1; then
        local cmd_java
        cmd_java="$(command -v java)"
        if [ "$OS_NAME" != "Darwin" ] || "$cmd_java" -version >/dev/null 2>&1; then
            printf '%s\n' "$cmd_java"
            return 0
        fi
    fi
    local cand
    for cand in \
        /Library/Java/JavaVirtualMachines/*/Contents/Home/bin/java \
        /opt/homebrew/opt/openjdk*/bin/java \
        /usr/local/opt/openjdk*/bin/java \
        /usr/lib/jvm/*/bin/java \
        /usr/java/*/bin/java \
        "$HOME"/.sdkman/candidates/java/current/bin/java \
        "$HOME"/.asdf/shims/java; do
        if [ -x "$cand" ]; then
            printf '%s\n' "$cand"
            return 0
        fi
    done
    return 1
}

JAVA_BIN="$(resolve_java_bin || true)"
if [ -z "$JAVA_BIN" ]; then
    echo "[ERROR] No se encontro Java en el sistema." >&2
    echo "Prism requiere Java (OpenJDK 17 o superior)." >&2
    if [ "$OS_NAME" = "Darwin" ]; then
        echo "En macOS puedes instalarlo desde https://adoptium.net o con: brew install openjdk@17" >&2
    else
        echo "En Linux puedes instalarlo con: sudo apt install default-jdk  (o sudo dnf install java-latest-openjdk)" >&2
    fi
    exit 1
fi

JAVA_VERSION=$("$JAVA_BIN" -version 2>&1 | head -n 1)
echo "[OK] Java detectado: $JAVA_VERSION"

# 2. Localizar archivo prism.jar del paquete
SOURCE_JAR="$SCRIPT_DIR/prism.jar"
if [ ! -f "$SOURCE_JAR" ]; then
    SOURCE_JAR="$SCRIPT_DIR/lib/prism.jar"
fi

if [ ! -f "$SOURCE_JAR" ]; then
    echo "[ERROR] No se encontro 'prism.jar' en el paquete instalador." >&2
    exit 1
fi

# 3. Determinar instalacion: a nivel de sistema o de usuario (~/.prism)
if [ "$EUID" -eq 0 ]; then
    if [ "$OS_NAME" = "Darwin" ]; then
        INSTALL_ROOT="/Library/Prism"
    else
        INSTALL_ROOT="/opt/prism"
    fi
    BIN_DIR="/usr/local/bin"
    LIB_DIR="$INSTALL_ROOT/lib"
    LAUNCHER_DIR="$INSTALL_ROOT/bin"
    IS_SYSTEM=1
else
    INSTALL_ROOT="$HOME/.prism"
    BIN_DIR="$INSTALL_ROOT/bin"
    LIB_DIR="$INSTALL_ROOT/lib"
    LAUNCHER_DIR="$BIN_DIR"
    IS_SYSTEM=0
fi

mkdir -p "$LIB_DIR" "$BIN_DIR" "$LAUNCHER_DIR"
cp "$SOURCE_JAR" "$LIB_DIR/prism.jar"
echo "[OK] Runtime instalado en $LIB_DIR/prism.jar"

# Si viene un runtime jlink integrado junto al paquete, copiarlo tambien
if [ -d "$SCRIPT_DIR/runtime" ]; then
    rm -rf "$INSTALL_ROOT/runtime"
    cp -R "$SCRIPT_DIR/runtime" "$INSTALL_ROOT/runtime"
    chmod +x "$INSTALL_ROOT/runtime/bin/"* 2>/dev/null || true
    [ -f "$INSTALL_ROOT/runtime/lib/jspawnhelper" ] && chmod +x "$INSTALL_ROOT/runtime/lib/jspawnhelper"
    echo "[OK] Runtime Java integrado copiado en $INSTALL_ROOT/runtime"
fi

# 4. Crear ejecutable lanzador prc
cat << 'EOF' > "$LAUNCHER_DIR/prc"
#!/usr/bin/env bash
# Lanzador oficial del lenguaje Prism
set -e

resolve_script_dir() {
    local source="${BASH_SOURCE[0]}"
    while [ -L "$source" ]; do
        local dir
        dir="$(cd -P "$(dirname "$source")" >/dev/null 2>&1 && pwd)"
        local target
        target="$(readlink "$source")"
        if [[ "$target" != /* ]]; then
            source="$dir/$target"
        else
            source="$target"
        fi
    done
    cd -P "$(dirname "$source")" >/dev/null 2>&1 && pwd
}

SCRIPT_DIR="$(resolve_script_dir)"
PRISM_HOME="$(cd -P "$SCRIPT_DIR/.." && pwd)"

if [ -f "$PRISM_HOME/lib/prism.jar" ]; then
    PRISM_JAR="$PRISM_HOME/lib/prism.jar"
elif [ -f "/Library/Prism/lib/prism.jar" ]; then
    PRISM_HOME="/Library/Prism"
    PRISM_JAR="/Library/Prism/lib/prism.jar"
elif [ -f "/opt/prism/lib/prism.jar" ]; then
    PRISM_HOME="/opt/prism"
    PRISM_JAR="/opt/prism/lib/prism.jar"
elif [ -f "$HOME/.prism/lib/prism.jar" ]; then
    PRISM_HOME="$HOME/.prism"
    PRISM_JAR="$HOME/.prism/lib/prism.jar"
else
    echo "[ERROR] No se encontro el runtime de Prism (prism.jar)." >&2
    exit 1
fi

JAVA_CMD=""
if [ -x "$PRISM_HOME/runtime/bin/java" ]; then
    JAVA_CMD="$PRISM_HOME/runtime/bin/java"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
elif [ "$(uname -s)" = "Darwin" ] && [ -x "/usr/libexec/java_home" ]; then
    MAC_JH="$(/usr/libexec/java_home -v 17+ 2>/dev/null || /usr/libexec/java_home 2>/dev/null || true)"
    [ -n "$MAC_JH" ] && [ -x "$MAC_JH/bin/java" ] && JAVA_CMD="$MAC_JH/bin/java"
fi

if [ -z "$JAVA_CMD" ] && command -v java >/dev/null 2>&1; then
    CMD_JAVA="$(command -v java)"
    if [ "$(uname -s)" != "Darwin" ] || "$CMD_JAVA" -version >/dev/null 2>&1; then
        JAVA_CMD="$CMD_JAVA"
    fi
fi

if [ -z "$JAVA_CMD" ]; then
    for cand in \
        /Library/Java/JavaVirtualMachines/*/Contents/Home/bin/java \
        /opt/homebrew/opt/openjdk*/bin/java \
        /usr/local/opt/openjdk*/bin/java \
        /usr/lib/jvm/*/bin/java \
        /usr/java/*/bin/java \
        "$HOME"/.sdkman/candidates/java/current/bin/java \
        "$HOME"/.asdf/shims/java; do
        if [ -x "$cand" ]; then
            JAVA_CMD="$cand"
            break
        fi
    done
fi

if [ -z "$JAVA_CMD" ]; then
    echo "[ERROR] No se encontro Java OpenJDK 17 o superior." >&2
    exit 1
fi

exec "$JAVA_CMD" -Dfile.encoding=UTF-8 -jar "$PRISM_JAR" "$@"
EOF
chmod +x "$LAUNCHER_DIR/prc"
if [ "$LAUNCHER_DIR" != "$BIN_DIR" ]; then
    ln -sf "$LAUNCHER_DIR/prc" "$BIN_DIR/prc"
fi
echo "[OK] Lanzador 'prc' configurado con permisos de ejecucion."

# 5. Integracion global al PATH del sistema operativo (para todos los IDEs y terminales)
if [ "$IS_SYSTEM" -eq 1 ]; then
    if [ "$OS_NAME" = "Darwin" ]; then
        mkdir -p /etc/paths.d
        printf '%s\n' "$INSTALL_ROOT/bin" "/usr/local/bin" > /etc/paths.d/prism
        chmod 644 /etc/paths.d/prism
        echo "[OK] Agregado a /etc/paths.d/prism para todas las terminales e IDEs de macOS."
    else
        mkdir -p /etc/profile.d
        echo 'export PATH="/usr/local/bin:$PATH"' > /etc/profile.d/prism.sh
        chmod +x /etc/profile.d/prism.sh
        echo "[OK] Agregado a /etc/profile.d/prism.sh para todas las terminales e IDEs del sistema."
    fi
else
    # Intento en /usr/local/bin (cuando ya existe o tiene permisos de escritura)
    if [ -d "/usr/local/bin" ] && [ -w "/usr/local/bin" ]; then
        ln -sf "$LAUNCHER_DIR/prc" "/usr/local/bin/prc"
        echo "[OK] Enlazado en /usr/local/bin/prc (accesible globalmente)."
    elif [ -t 0 ] && command -v sudo >/dev/null 2>&1 && [ -d "/usr/local/bin" ]; then
        echo "Solicitando acceso para enlazar en /usr/local/bin/prc (opcional)..."
        sudo ln -sf "$LAUNCHER_DIR/prc" "/usr/local/bin/prc" 2>/dev/null && echo "[OK] Enlazado en /usr/local/bin/prc globalmente." || true
    fi

    # Enlace en ~/.local/bin
    mkdir -p "$HOME/.local/bin"
    ln -sf "$LAUNCHER_DIR/prc" "$HOME/.local/bin/prc"

    # En macOS (Apple Silicon / zsh por defecto), crear ~/.zshrc si no existe
    if [ "$OS_NAME" = "Darwin" ] && [ ! -f "$HOME/.zshrc" ]; then
        touch "$HOME/.zshrc"
    fi

    if [ "$OS_NAME" != "Darwin" ]; then
        mkdir -p "$HOME/.config/environment.d" 2>/dev/null || true
        if [ -d "$HOME/.config/environment.d" ]; then
            echo 'PATH=$HOME/.prism/bin:$HOME/.local/bin:$PATH' > "$HOME/.config/environment.d/10-prism.conf"
            echo "[OK] PATH configurado para el entorno grafico (systemd / IDEs)."
        fi
    fi

    # Integracion en shells (.zshrc, .zprofile, .bashrc, .bash_profile, .profile)
    for rc in "$HOME/.zshrc" "$HOME/.zprofile" "$HOME/.bashrc" "$HOME/.bash_profile" "$HOME/.profile"; do
        if [ -f "$rc" ]; then
            if ! grep -q '\.prism/bin' "$rc"; then
                echo '' >> "$rc"
                echo '# Prism Language CLI' >> "$rc"
                printf 'export PATH="$HOME/.prism/bin:$HOME/.local/bin:/usr/local/bin:$PATH"\n' >> "$rc"
                echo "[OK] PATH registrado en $(basename "$rc")."
            fi
        fi
    done
fi

echo ""
echo "======================================================="
echo "       INSTALACION COMPLETADA EXITOSAMENTE             "
echo "======================================================="
echo ""
echo "El compilador y runtime de Prism ya esta instalado."
echo "Para verificar en una nueva terminal ejecuta:"
echo "  prc --version"
echo ""
echo "Comandos disponibles:"
echo "  prc run <archivo.pr>              Ejecuta un programa .pr"
echo "  prc compile <archivo.pr> [-d out] Compila a bytecode JVM (.class)"
echo "  prc check <archivo.pr>            Valida sintaxis y referencias"
echo ""
