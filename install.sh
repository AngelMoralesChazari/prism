#!/usr/bin/env bash
# Script de instalacion universal de Prism (prc) v1.0.0
# Disenado para integrar prc en el PATH del sistema para cualquier IDE o terminal.
set -e

echo "======================================================="
echo "       INSTALADOR OFICIAL DE PRISM (prc) v1.0.0        "
echo "======================================================="
echo ""

# 1. Resolver ejecutable de Java
JAVA_BIN=""
if command -v java >/dev/null 2>&1; then
    JAVA_BIN="java"
elif [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_BIN="$JAVA_HOME/bin/java"
else
    for cand in /usr/lib/jvm/*/bin/java /usr/java/*/bin/java "$HOME"/.sdkman/candidates/java/current/bin/java "$HOME"/.asdf/shims/java; do
        if [ -x "$cand" ]; then
            JAVA_BIN="$cand"
            break
        fi
    done
fi

if [ -z "$JAVA_BIN" ]; then
    echo "[ERROR] No se encontro Java en el sistema." >&2
    echo "Prism requiere Java (OpenJDK 17 o superior)." >&2
    echo "En Linux puedes instalarlo con: sudo apt install default-jdk  (o sudo dnf install java-latest-openjdk)" >&2
    exit 1
fi

JAVA_VERSION=$("$JAVA_BIN" -version 2>&1 | head -n 1)
echo "[OK] Java detectado: $JAVA_VERSION"

# 2. Localizar archivo prism.jar del paquete
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_JAR="$SCRIPT_DIR/prism.jar"
if [ ! -f "$SOURCE_JAR" ]; then
    SOURCE_JAR="$SCRIPT_DIR/lib/prism.jar"
fi

if [ ! -f "$SOURCE_JAR" ]; then
    echo "[ERROR] No se encontro 'prism.jar' en el paquete instalador." >&2
    exit 1
fi

# 3. Determinar instalacion: a nivel de sistema (/opt y /usr/local/bin) o de usuario (~/.prism)
if [ "$EUID" -eq 0 ]; then
    INSTALL_ROOT="/opt/prism"
    BIN_DIR="/usr/local/bin"
    LIB_DIR="$INSTALL_ROOT/lib"
    IS_SYSTEM=1
else
    INSTALL_ROOT="$HOME/.prism"
    BIN_DIR="$INSTALL_ROOT/bin"
    LIB_DIR="$INSTALL_ROOT/lib"
    IS_SYSTEM=0
fi

mkdir -p "$LIB_DIR" "$BIN_DIR"
cp "$SOURCE_JAR" "$LIB_DIR/prism.jar"
echo "[OK] Runtime instalado en $LIB_DIR/prism.jar"

# 4. Crear ejecutable lanzador prc
cat << 'EOF' > "$BIN_DIR/prc"
#!/usr/bin/env bash
# Lanzador oficial del lenguaje Prism

# Resolver ubicacion del runtime
if [ -f "/opt/prism/lib/prism.jar" ]; then
    PRISM_JAR="/opt/prism/lib/prism.jar"
elif [ -f "$HOME/.prism/lib/prism.jar" ]; then
    PRISM_JAR="$HOME/.prism/lib/prism.jar"
else
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    PRISM_JAR="$SCRIPT_DIR/../lib/prism.jar"
fi

if [ ! -f "$PRISM_JAR" ]; then
    echo "[ERROR] No se encontro el runtime de Prism en $PRISM_JAR" >&2
    exit 1
fi

# Resolver comando java
JAVA_CMD="java"
if ! command -v java >/dev/null 2>&1; then
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        JAVA_CMD="$JAVA_HOME/bin/java"
    else
        for cand in /usr/lib/jvm/*/bin/java /usr/java/*/bin/java "$HOME"/.sdkman/candidates/java/current/bin/java; do
            if [ -x "$cand" ]; then
                JAVA_CMD="$cand"
                break
            fi
        done
    fi
fi

exec "$JAVA_CMD" -jar "$PRISM_JAR" "$@"
EOF
chmod +x "$BIN_DIR/prc"
echo "[OK] Lanzador 'prc' configurado con permisos de ejecucion."

# 5. Integracion global al PATH del sistema operativo (para todos los IDEs y terminales)
if [ "$IS_SYSTEM" -eq 1 ]; then
    # Sistema global
    echo 'export PATH="/usr/local/bin:$PATH"' > /etc/profile.d/prism.sh
    chmod +x /etc/profile.d/prism.sh
    echo "[OK] Agregado a /etc/profile.d/prism.sh para todas las terminales e IDEs del sistema."
else
    # Intento en /usr/local/bin (el directorio estandar que todo IDE y terminal lee siempre)
    if [ -w "/usr/local/bin" ]; then
        ln -sf "$BIN_DIR/prc" "/usr/local/bin/prc"
        echo "[OK] Enlazado en /usr/local/bin/prc (accesible globalmente)."
    elif command -v sudo >/dev/null 2>&1; then
        echo "Solicitando acceso para enlazar en /usr/local/bin/prc (permite que cualquier IDE lo detecte sin configurar nada)..."
        sudo ln -sf "$BIN_DIR/prc" "/usr/local/bin/prc" 2>/dev/null && echo "[OK] Enlazado en /usr/local/bin/prc globalmente." || true
    fi

    # Enlace en ~/.local/bin
    mkdir -p "$HOME/.local/bin"
    ln -sf "$BIN_DIR/prc" "$HOME/.local/bin/prc"

    # Integracion con systemd environment.d (para que entornos graficos e IDEs como IntelliJ/NetBeans/VS Code lean el PATH)
    mkdir -p "$HOME/.config/environment.d" 2>/dev/null || true
    if [ -d "$HOME/.config/environment.d" ]; then
        echo 'PATH=$HOME/.prism/bin:$HOME/.local/bin:$PATH' > "$HOME/.config/environment.d/10-prism.conf"
        echo "[OK] PATH configurado para el entorno grafico (systemd / IDEs)."
    fi

    # Integracion en shells (.zshrc, .bashrc, .profile)
    for rc in "$HOME/.zshrc" "$HOME/.bashrc" "$HOME/.profile"; do
        if [ -f "$rc" ]; then
            if ! grep -q '\.prism/bin' "$rc"; then
                echo '' >> "$rc"
                echo '# Prism Language CLI' >> "$rc"
                echo 'export PATH="$HOME/.prism/bin:$HOME/.local/bin:/usr/local/bin:$PATH"' >> "$rc"
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
echo "Para verificar en tu terminal actual ejecuta:"
echo "  prc --version"
echo ""
echo "Comandos disponibles:"
echo "  prc run <archivo.pr>              Ejecuta un programa .pr"
echo "  prc compile <archivo.pr> [-d out] Compila a bytecode JVM (.class)"
echo "  prc check <archivo.pr>            Valida sintaxis y referencias"
echo ""
