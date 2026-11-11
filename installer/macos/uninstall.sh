#!/usr/bin/env bash
# Desinstalador oficial de Prism en macOS
set -e

MODE="${1:-auto}"

remove_system() {
    if [ "$EUID" -ne 0 ]; then
        echo "Se requieren permisos de administrador para desinstalar /Library/Prism." >&2
        exit 1
    fi

    if [ -L /usr/local/bin/prc ]; then
        TARGET="$(readlink /usr/local/bin/prc || true)"
        case "$TARGET" in
            /Library/Prism/*|/opt/prism/*)
                rm -f /usr/local/bin/prc
                ;;
        esac
    elif [ -f /usr/local/bin/prc ]; then
        if grep -q 'Prism' /usr/local/bin/prc 2>/dev/null; then
            rm -f /usr/local/bin/prc
        fi
    fi

    rm -f /etc/paths.d/prism
    rm -rf /Library/Prism

    if command -v pkgutil >/dev/null 2>&1; then
        pkgutil --forget com.absoftwaregroup.prism.core >/dev/null 2>&1 || true
        pkgutil --forget com.absoftwaregroup.prism.examples >/dev/null 2>&1 || true
    fi

    echo "[OK] Prism eliminado de /Library/Prism."
}

remove_user() {
    rm -rf "$HOME/.prism"
    rm -f "$HOME/.local/bin/prc"
    if [ -L /usr/local/bin/prc ] && [ -w /usr/local/bin ]; then
        TARGET="$(readlink /usr/local/bin/prc || true)"
        case "$TARGET" in
            "$HOME/.prism/"*)
                rm -f /usr/local/bin/prc
                ;;
        esac
    fi
    echo "[OK] Prism eliminado de $HOME/.prism."
}

if [ "$MODE" = "--user" ]; then
    remove_user
elif [ "$MODE" = "--system" ]; then
    remove_system
else
    REMOVED=0
    if [ -d /Library/Prism ] || [ -f /etc/paths.d/prism ]; then
        remove_system
        REMOVED=1
    fi
    if [ -d "$HOME/.prism" ]; then
        remove_user
        REMOVED=1
    fi
    if [ "$REMOVED" -eq 0 ]; then
        echo "No se encontro una instalacion previa de Prism."
    fi
fi

