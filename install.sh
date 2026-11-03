#!/usr/bin/env bash
# Script de instalación oficial de Prism (prc) v1.0.0
set -e

echo "=== Instalador oficial de Prism (prc) v1.0.0 ==="

# 1. Verificar Java
if ! command -v java >/dev/null 2>&1; then
    echo "Error: Java no está instalado en el sistema. Por favor instala OpenJDK 17 o superior." >&2
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1)
echo "Java detectado: $JAVA_VERSION"

# 2. Rutas de instalación
PRISM_DIR="$HOME/.prism"
LIB_DIR="$PRISM_DIR/lib"
BIN_DIR="$PRISM_DIR/bin"

mkdir -p "$LIB_DIR" "$BIN_DIR"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_JAR="$SCRIPT_DIR/prism.jar"
if [ ! -f "$SOURCE_JAR" ]; then
    SOURCE_JAR="$SCRIPT_DIR/lib/prism.jar"
fi

if [ ! -f "$SOURCE_JAR" ]; then
    echo "Error: No se encontró 'prism.jar' en el instalador." >&2
    exit 1
fi

echo "Copiando runtime a $LIB_DIR/prism.jar..."
cp "$SOURCE_JAR" "$LIB_DIR/prism.jar"

# 3. Crear ejecutable prc en ~/.prism/bin/prc
cat << 'EOF' > "$BIN_DIR/prc"
#!/usr/bin/env bash
PRISM_JAR="$HOME/.prism/lib/prism.jar"
if [ ! -f "$PRISM_JAR" ]; then
    echo "Error: No se encontró $PRISM_JAR" >&2
    exit 1
fi
exec java -jar "$PRISM_JAR" "$@"
EOF
chmod +x "$BIN_DIR/prc"

# 4. Enlazar en ~/.local/bin si existe o en /usr/local/bin si hay permisos
LINK_TARGET="$HOME/.local/bin"
if [ -d "$LINK_TARGET" ] && [[ ":$PATH:" == *":$LINK_TARGET:"* ]]; then
    ln -sf "$BIN_DIR/prc" "$LINK_TARGET/prc"
    echo "Comando 'prc' enlazado en $LINK_TARGET/prc"
elif [ -w "/usr/local/bin" ]; then
    ln -sf "$BIN_DIR/prc" "/usr/local/bin/prc"
    echo "Comando 'prc' enlazado en /usr/local/bin/prc"
else
    mkdir -p "$LINK_TARGET"
    ln -sf "$BIN_DIR/prc" "$LINK_TARGET/prc"
    echo "Comando 'prc' instalado en $BIN_DIR/prc"
    echo "Aviso: Agrega export PATH=\"\$HOME/.prism/bin:\$PATH\" a tu ~/.bashrc o ~/.zshrc"
fi

echo ""
echo "=== Instalación de Prism completada con éxito ==="
echo "Comandos disponibles:"
echo "  prc run <archivo.pr>              Ejecuta un programa .pr"
echo "  prc compile <archivo.pr> [-d out] Compila a bytecode JVM (.class)"
echo "  prc check <archivo.pr>            Valida sintaxis y referencias"
echo "  prc --version                     Muestra la versión instalada"
echo ""
echo "Para instalar la extensión en VS Code:"
echo "  code --install-extension prism-0.3.2.vsix"

