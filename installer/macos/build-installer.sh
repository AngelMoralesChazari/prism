#!/usr/bin/env bash
# Script para compilar el instalador grafico nativo de Prism para macOS (.pkg y .dmg + jlink)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

SKIP_BUILD_JAR=0
SKIP_RUNTIME=0
UNIVERSAL=0
SKIP_DMG=0
SKIP_TAR=0
STAGE_ONLY=0
CUSTOM_VERSION=""
SIGN_IDENTITY=""

while [ "$#" -gt 0 ]; do
    case "$1" in
        --skip-build-jar)
            SKIP_BUILD_JAR=1
            shift
            ;;
        --skip-runtime)
            SKIP_RUNTIME=1
            shift
            ;;
        --universal)
            UNIVERSAL=1
            SKIP_RUNTIME=1
            shift
            ;;
        --skip-dmg)
            SKIP_DMG=1
            shift
            ;;
        --skip-tar)
            SKIP_TAR=1
            shift
            ;;
        --stage-only)
            STAGE_ONLY=1
            shift
            ;;
        --version)
            CUSTOM_VERSION="${2:-}"
            shift 2
            ;;
        --sign)
            SIGN_IDENTITY="${2:-}"
            shift 2
            ;;
        -h|--help)
            cat <<'EOF'
Uso: ./installer/macos/build-installer.sh [opciones]

Opciones:
  --skip-build-jar   Usa el prism.jar existente sin recompilar
  --skip-runtime     Omite jlink (requiere OpenJDK 17+ en la Mac destino)
  --universal        Genera instalador Universal sin runtime (x86_64 + arm64)
  --skip-dmg         No genera la imagen .dmg
  --skip-tar         No genera el paquete portatil .tar.gz
  --stage-only       Solo prepara el staging sin invocar pkgbuild/productbuild
  --version X.Y.Z    Sobrescribe la version detectada automaticamente
  --sign "ID"        Firma el paquete con Developer ID Installer en macOS
EOF
            exit 0
            ;;
        *)
            echo "Opcion desconocida: $1" >&2
            exit 1
            ;;
    esac
done

detect_version() {
    if [ -n "$CUSTOM_VERSION" ]; then
        printf '%s\n' "$CUSTOM_VERSION"
        return 0
    fi
    local cli_file="$REPO_ROOT/src/main/java/prism/pr/PrcCli.java"
    if [ -f "$cli_file" ]; then
        local v
        v="$(sed -n 's/.*VERSION = "\([^"]*\)".*/\1/p' "$cli_file" | head -n 1)"
        if [ -n "$v" ]; then
            printf '%s\n' "$v"
            return 0
        fi
    fi
    printf '1.1.0\n'
}

VERSION="$(detect_version)"
RAW_ARCH="$(uname -m)"

if [ "$UNIVERSAL" -eq 1 ]; then
    ARCH_TAG="Universal"
    ARCH_LABEL="Universal (Apple Silicon + Intel)"
    HOST_ARCHS="x86_64,arm64"
elif [ "$RAW_ARCH" = "arm64" ] || [ "$RAW_ARCH" = "aarch64" ]; then
    ARCH_TAG="arm64"
    ARCH_LABEL="Apple Silicon (arm64)"
    HOST_ARCHS="arm64"
else
    ARCH_TAG="x64"
    ARCH_LABEL="Intel (x86_64)"
    HOST_ARCHS="x86_64"
fi

if [ "$SKIP_RUNTIME" -eq 0 ]; then
    RUNTIME_NOTE="Runtime Java 17+ mínimo integrado (no requiere instalar Java aparte)."
    RUNTIME_DESC="runtime Java integrado"
else
    RUNTIME_NOTE="Usa el Java OpenJDK 17+ del sistema (paquete ligero ${ARCH_LABEL})."
    RUNTIME_DESC="lanzador sobre OpenJDK 17+"
fi

echo "======================================================="
echo "   Compilando instalador nativo de Prism v${VERSION} (macOS)   "
echo "======================================================="

cd "$REPO_ROOT"

# [1/4] Compilar prism.jar si no se omitio
if [ "$SKIP_BUILD_JAR" -eq 0 ]; then
    if ! command -v javac >/dev/null 2>&1 || ! command -v jar >/dev/null 2>&1; then
        echo "Error: Se requiere Java JDK 17 o superior en el PATH (javac y jar)." >&2
        exit 1
    fi
    echo "[1/4] Compilando prism.jar (objetivo Java 17)..."
    CLASSES_DIR="$REPO_ROOT/target/classes"
    SOURCES_FILE="$REPO_ROOT/target/sources.txt"
    rm -rf "$CLASSES_DIR"
    mkdir -p "$CLASSES_DIR" "$REPO_ROOT/target"
    find "$REPO_ROOT/src/main/java" -name '*.java' | sort > "$SOURCES_FILE"
    javac --release 17 -encoding UTF-8 -d "$CLASSES_DIR" "@$SOURCES_FILE"
    if [ -d "$REPO_ROOT/src/main/resources" ]; then
        cp -R "$REPO_ROOT/src/main/resources/"* "$CLASSES_DIR/"
    fi
    jar --create --file "$REPO_ROOT/prism.jar" --main-class prism.pr.PrcCli -C "$CLASSES_DIR" .
    if [ -d "$REPO_ROOT/extensions/prism/bin" ]; then
        cp "$REPO_ROOT/prism.jar" "$REPO_ROOT/extensions/prism/bin/prism.jar"
    fi
else
    if [ ! -f "$REPO_ROOT/prism.jar" ]; then
        echo "Error: No existe prism.jar y se especifico --skip-build-jar." >&2
        exit 1
    fi
    echo "[1/4] Reutilizando prism.jar existente (--skip-build-jar)."
fi

# [2/4] Generar runtime JVM minimo via jlink
RUNTIME_DIR="$REPO_ROOT/build/macos-runtime"
if [ "$SKIP_RUNTIME" -eq 0 ]; then
    JLINK_BIN=""
    if command -v jlink >/dev/null 2>&1; then
        JLINK_BIN="$(command -v jlink)"
    elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/jlink" ]; then
        JLINK_BIN="$JAVA_HOME/bin/jlink"
    fi
    if [ -z "$JLINK_BIN" ]; then
        echo "Error: No se encontro jlink. Instala un JDK 17+ completo o usa --skip-runtime." >&2
        exit 1
    fi
    echo "[2/4] Generando runtime JVM minimo (jlink)..."
    rm -rf "$RUNTIME_DIR"
    mkdir -p "$(dirname "$RUNTIME_DIR")"
    COMPRESS_OPT="--compress=2"
    if "$JLINK_BIN" --help 2>&1 | grep "zip-" >/dev/null; then
        COMPRESS_OPT="--compress=zip-6"
    fi
    JLINK_HOME="$(cd "$(dirname "$JLINK_BIN")/.." && pwd)"
    JLINK_ARGS=(
        --add-modules 'java.base,java.compiler,jdk.compiler,jdk.zipfs,jdk.charsets'
        --strip-debug
        --no-man-pages
        --no-header-files
        "$COMPRESS_OPT"
        --output "$RUNTIME_DIR"
    )
    if [ -d "$JLINK_HOME/jmods" ]; then
        JLINK_ARGS=(--module-path "$JLINK_HOME/jmods" "${JLINK_ARGS[@]}")
    fi
    if ! "$JLINK_BIN" "${JLINK_ARGS[@]}"; then
        echo "Error: jlink fallo al construir el runtime. En JDKs sin carpeta jmods (JEP 493), usa Temurin JDK 17/21 o agrega --skip-runtime / --universal." >&2
        exit 1
    fi
else
    echo "[2/4] Omitiendo runtime integrado (--skip-runtime / --universal)."
fi

# [3/4] Preparar staging de paquetes y recursos graficos
echo "[3/4] Preparando estructura del instalador grafico..."
WORK_DIR="$REPO_ROOT/build/macos-installer-work"
rm -rf "$WORK_DIR"
CORE_STAGE="$WORK_DIR/core-root"
EXAMPLES_STAGE="$WORK_DIR/examples-root"
RES_OUT="$WORK_DIR/resources"
PKGS_OUT="$WORK_DIR/pkgs"

mkdir -p \
    "$CORE_STAGE/bin" \
    "$CORE_STAGE/lib" \
    "$CORE_STAGE/resources" \
    "$EXAMPLES_STAGE" \
    "$RES_OUT/es.lproj" \
    "$PKGS_OUT" \
    "$REPO_ROOT/dist"

cp "$REPO_ROOT/bin/prc" "$CORE_STAGE/bin/prc"
cp "$REPO_ROOT/prism.jar" "$CORE_STAGE/lib/prism.jar"
cp "$REPO_ROOT/LICENSE" "$CORE_STAGE/LICENSE"
cp "$REPO_ROOT/README.md" "$CORE_STAGE/README.md"
cp "$SCRIPT_DIR/LEEME_INSTALACION_MACOS.txt" "$CORE_STAGE/LEEME_INSTALACION_MACOS.txt"
cp "$SCRIPT_DIR/uninstall.sh" "$CORE_STAGE/uninstall.sh"
cp "$SCRIPT_DIR/Desinstalar-Prism.command" "$CORE_STAGE/Desinstalar Prism.command"
cp "$SCRIPT_DIR/resources/prism-icon.png" "$CORE_STAGE/resources/prism-icon.png"

chmod 755 "$CORE_STAGE/bin/prc" "$CORE_STAGE/uninstall.sh" "$CORE_STAGE/Desinstalar Prism.command"
chmod 644 "$CORE_STAGE/lib/prism.jar"

if [ "$SKIP_RUNTIME" -eq 0 ] && [ -d "$RUNTIME_DIR" ]; then
    cp -R "$RUNTIME_DIR" "$CORE_STAGE/runtime"
    chmod 755 "$CORE_STAGE/runtime/bin/"* 2>/dev/null || true
    [ -f "$CORE_STAGE/runtime/lib/jspawnhelper" ] && chmod 755 "$CORE_STAGE/runtime/lib/jspawnhelper"
fi

if ls "$REPO_ROOT/examples/"*.pr >/dev/null 2>&1; then
    cp "$REPO_ROOT/examples/"*.pr "$EXAMPLES_STAGE/"
fi

# Renderizar plantillas XML y HTML
render_templates() {
    python3 - "$SCRIPT_DIR" "$WORK_DIR" "$REPO_ROOT/LICENSE" "$VERSION" "$ARCH_LABEL" "$HOST_ARCHS" "$RUNTIME_NOTE" "$RUNTIME_DESC" <<'PY'
import pathlib, shutil, sys

script_dir = pathlib.Path(sys.argv[1])
work_dir = pathlib.Path(sys.argv[2])
license_path = pathlib.Path(sys.argv[3])
repls = {
    "${VERSION}": sys.argv[4],
    "${ARCH_LABEL}": sys.argv[5],
    "${HOST_ARCHS}": sys.argv[6],
    "${RUNTIME_NOTE}": sys.argv[7],
    "${RUNTIME_DESC}": sys.argv[8],
}

def render(src: pathlib.Path, dst: pathlib.Path):
    text = src.read_text(encoding="utf-8")
    for k, v in repls.items():
        text = text.replace(k, v)
    dst.write_text(text, encoding="utf-8")

res_in = script_dir / "resources"
res_out = work_dir / "resources"
es_out = res_out / "es.lproj"

render(script_dir / "distribution.xml.in", work_dir / "distribution.xml")
for name in ("welcome.html", "conclusion.html"):
    render(res_in / f"{name}.in", res_out / name)
    render(res_in / f"{name}.in", es_out / name)

shutil.copy2(license_path, res_out / "LICENSE.txt")
shutil.copy2(license_path, es_out / "LICENSE.txt")
for img in ("background.png", "background-dark.png", "prism-icon.png"):
    shutil.copy2(res_in / img, res_out / img)
PY
}

render_templates

chmod 755 "$SCRIPT_DIR/scripts/core/postinstall" "$SCRIPT_DIR/scripts/examples/postinstall"

# Generar paquete portatil .tar.gz con asistente grafico osascript
if [ "$SKIP_TAR" -eq 0 ]; then
    TAR_STAGE="$WORK_DIR/tar-stage"
    mkdir -p "$TAR_STAGE/bin" "$TAR_STAGE/examples" "$TAR_STAGE/installer/macos/resources"
    cp "$REPO_ROOT/install.sh" "$REPO_ROOT/prism.jar" "$REPO_ROOT/README.md" "$REPO_ROOT/LICENSE" "$TAR_STAGE/"
    cp "$SCRIPT_DIR/LEEME_INSTALACION_MACOS.txt" "$TAR_STAGE/"
    cp "$SCRIPT_DIR/PrismSetupGUI.sh" "$TAR_STAGE/Instalar-Prism-Grafico.command"
    cp "$SCRIPT_DIR/Desinstalar-Prism.command" "$TAR_STAGE/Desinstalar-Prism.command"
    cp "$SCRIPT_DIR/uninstall.sh" "$TAR_STAGE/installer/macos/uninstall.sh"
    cp "$SCRIPT_DIR/resources/prism-icon.png" "$TAR_STAGE/installer/macos/resources/prism-icon.png"
    cp "$REPO_ROOT/bin/prc" "$TAR_STAGE/bin/prc"
    chmod 755 "$TAR_STAGE/install.sh" "$TAR_STAGE/bin/prc" \
        "$TAR_STAGE/Instalar-Prism-Grafico.command" \
        "$TAR_STAGE/Desinstalar-Prism.command" \
        "$TAR_STAGE/installer/macos/uninstall.sh"
    if ls "$REPO_ROOT/examples/"*.pr >/dev/null 2>&1; then
        cp "$REPO_ROOT/examples/"*.pr "$TAR_STAGE/examples/"
    fi
    if ls "$REPO_ROOT/extensions/prism/"*.vsix >/dev/null 2>&1; then
        mkdir -p "$TAR_STAGE/extensions"
        cp "$REPO_ROOT/extensions/prism/"*.vsix "$TAR_STAGE/extensions/"
    fi
    TAR_OUT="$REPO_ROOT/dist/prism-v${VERSION}-macos.tar.gz"
    tar -czf "$TAR_OUT" -C "$TAR_STAGE" .
    echo "[OK] Paquete portatil macOS: $TAR_OUT"
fi

if [ "$STAGE_ONLY" -eq 1 ]; then
    echo "[OK] Staging preparado (--stage-only) en $WORK_DIR"
    exit 0
fi

# [4/4] Empaquetar .pkg y .dmg usando herramientas nativas de macOS
if [ "$(uname -s)" != "Darwin" ]; then
    echo "Error: pkgbuild y productbuild solo existen en macOS (Darwin)." >&2
    echo "Ejecuta este script en una Mac o mediante GitHub Actions (macos-latest)." >&2
    echo "Puedes validar la preparacion previa pasando --stage-only." >&2
    exit 1
fi

if ! command -v pkgbuild >/dev/null 2>&1 || ! command -v productbuild >/dev/null 2>&1; then
    echo "Error: No se encontraron pkgbuild/productbuild en este sistema macOS." >&2
    exit 1
fi

echo "[4/4] Construyendo instalador grafico nativo (.pkg)..."

pkgbuild \
    --root "$CORE_STAGE" \
    --identifier "com.absoftwaregroup.prism.core" \
    --version "$VERSION" \
    --install-location "/Library/Prism" \
    --scripts "$SCRIPT_DIR/scripts/core" \
    --ownership recommended \
    "$PKGS_OUT/prism-core.pkg"

pkgbuild \
    --root "$EXAMPLES_STAGE" \
    --identifier "com.absoftwaregroup.prism.examples" \
    --version "$VERSION" \
    --install-location "/Library/Prism/examples" \
    --scripts "$SCRIPT_DIR/scripts/examples" \
    --ownership recommended \
    "$PKGS_OUT/prism-examples.pkg"

PKG_OUT="$REPO_ROOT/dist/Prism-${VERSION}-macOS-${ARCH_TAG}.pkg"
PRODUCT_ARGS=(
    --distribution "$WORK_DIR/distribution.xml"
    --resources "$RES_OUT"
    --package-path "$PKGS_OUT"
)
if [ -n "$SIGN_IDENTITY" ]; then
    PRODUCT_ARGS+=(--sign "$SIGN_IDENTITY")
fi
productbuild "${PRODUCT_ARGS[@]}" "$PKG_OUT"
echo "[OK] Instalador .pkg generado: $PKG_OUT"

# Generar imagen .dmg opcional con acceso directo al .pkg, desinstalador y ejemplos
if [ "$SKIP_DMG" -eq 0 ] && command -v hdiutil >/dev/null 2>&1; then
    DMG_STAGE="$WORK_DIR/dmg-stage"
    mkdir -p "$DMG_STAGE/Ejemplos" "$DMG_STAGE/.prism-resources"
    cp "$PKG_OUT" "$DMG_STAGE/Instalar Prism.pkg"
    cp "$SCRIPT_DIR/Desinstalar-Prism.command" "$DMG_STAGE/Desinstalar Prism.command"
    cp "$SCRIPT_DIR/uninstall.sh" "$DMG_STAGE/.prism-uninstall.sh"
    cp "$SCRIPT_DIR/resources/prism-icon.png" "$DMG_STAGE/.prism-resources/prism-icon.png"
    chmod 755 "$DMG_STAGE/Desinstalar Prism.command" "$DMG_STAGE/.prism-uninstall.sh"
    cp "$SCRIPT_DIR/LEEME_INSTALACION_MACOS.txt" "$DMG_STAGE/LEEME_INSTALACION_MACOS.txt"
    if ls "$REPO_ROOT/examples/"*.pr >/dev/null 2>&1; then
        cp "$REPO_ROOT/examples/"*.pr "$DMG_STAGE/Ejemplos/"
    fi

    # Generar .VolumeIcon.icns si sips + iconutil estan disponibles
    if command -v sips >/dev/null 2>&1 && command -v iconutil >/dev/null 2>&1; then
        ICONSET_DIR="$WORK_DIR/Prism.iconset"
        SRC_ICON="$SCRIPT_DIR/resources/prism-icon.png"
        mkdir -p "$ICONSET_DIR"
        for sz in 16 32 128 256; do
            sips -z "$sz" "$sz" "$SRC_ICON" --out "$ICONSET_DIR/icon_${sz}x${sz}.png" >/dev/null 2>&1 || true
            d=$((sz * 2))
            sips -z "$d" "$d" "$SRC_ICON" --out "$ICONSET_DIR/icon_${sz}x${sz}@2x.png" >/dev/null 2>&1 || true
        done
        sips -z 512 512 "$SRC_ICON" --out "$ICONSET_DIR/icon_512x512.png" >/dev/null 2>&1 || true
        if iconutil -c icns "$ICONSET_DIR" -o "$DMG_STAGE/.VolumeIcon.icns" >/dev/null 2>&1; then
            if command -v SetFile >/dev/null 2>&1; then
                SetFile -a C "$DMG_STAGE" >/dev/null 2>&1 || true
            fi
        fi
    fi

    DMG_OUT="$REPO_ROOT/dist/Prism-${VERSION}-macOS-${ARCH_TAG}.dmg"
    rm -f "$DMG_OUT"
    hdiutil create -volname "Prism ${VERSION}" -srcfolder "$DMG_STAGE" -ov -format UDZO "$DMG_OUT" >/dev/null
    echo "[OK] Imagen .dmg generada: $DMG_OUT"
fi

echo "[OK] Proceso de empaquetado macOS finalizado."
