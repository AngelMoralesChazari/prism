@echo off
title Instalador oficial de Prism v1.0.0
color 0A
cls

echo =======================================================
echo          INSTALADOR OFICIAL DE PRISM v1.0.0
echo =======================================================
echo.

REM 1. Verificar si Java esta instalado
where java >nul 2>nul
if %errorlevel% neq 0 (
    color 0C
    echo [ERROR] No se encontro Java instalado en el sistema.
    echo.
    echo Prism necesita Java (OpenJDK 17 o superior) para funcionar.
    echo Puedes descargarlo e instalarlo gratis desde:
    echo https://adoptium.net (Eclipse Temurin)
    echo.
    echo Presiona cualquier tecla para salir...
    pause >nul
    exit /b 1
)

echo [OK] Java detectado en el sistema.
echo.

REM 2. Definir rutas de instalacion (%USERPROFILE%\.prism)
set "PRISM_DIR=%USERPROFILE%\.prism"
set "BIN_DIR=%PRISM_DIR%\bin"
set "LIB_DIR=%PRISM_DIR%\lib"

if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"
if not exist "%LIB_DIR%" mkdir "%LIB_DIR%"

REM 3. Copiar prism.jar
set "SCRIPT_DIR=%~dp0"
if exist "%SCRIPT_DIR%prism.jar" (
    copy /y "%SCRIPT_DIR%prism.jar" "%LIB_DIR%\prism.jar" >nul
) else if exist "%SCRIPT_DIR%lib\prism.jar" (
    copy /y "%SCRIPT_DIR%lib\prism.jar" "%LIB_DIR%\prism.jar" >nul
) else (
    color 0C
    echo [ERROR] No se encontro 'prism.jar' en el instalador.
    pause
    exit /b 1
)
echo [OK] Archivos del runtime instalados en %LIB_DIR%

REM 4. Crear lanzador prc.cmd en %BIN_DIR%
(
    echo @echo off
    echo setlocal
    echo set "JAR_PATH=%%USERPROFILE%%\.prism\lib\prism.jar"
    echo if not exist "%%JAR_PATH%%" ^(
    echo     echo Error: No se encontro prism.jar en %%JAR_PATH%% ^>&2
    echo     exit /b 1
    echo ^)
    echo java -jar "%%JAR_PATH%%" %%*
) > "%BIN_DIR%\prc.cmd"
echo [OK] Comando 'prc' creado en %BIN_DIR%

REM 5. Agregar %BIN_DIR% al PATH del usuario de Windows mediante PowerShell
echo.
echo Configurando variables de entorno PATH...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$bin = [System.IO.Path]::GetFullPath('%BIN_DIR%'); " ^
    "$userPath = [Environment]::GetEnvironmentVariable('PATH', 'User'); " ^
    "if ($userPath -notlike \"*$bin*\") { " ^
    "    $newPath = if ($userPath) { \"$userPath;$bin\" } else { $bin }; " ^
    "    [Environment]::SetEnvironmentVariable('PATH', $newPath, 'User'); " ^
    "    Write-Host '[OK] Directorio agregado al PATH del usuario.'; " ^
    "} else { " ^
    "    Write-Host '[OK] El directorio ya estaba en el PATH.'; " ^
    "}"

echo.
echo =======================================================
echo     INSTALACION COMPLETADA EXITOSAMENTE!
echo =======================================================
echo.
echo Que puedes hacer ahora?
echo 1. Abre una nueva ventana de terminal (CMD o PowerShell).
echo 2. Escribe 'prc --version' para verificar la instalacion.
echo 3. Para instalar el soporte visual en VS Code:
echo    - Abre VS Code.
echo    - Ve a Extensiones (Ctrl+Shift+X).
echo    - En el menu de tres puntos (...) selecciona:
echo      'Install from VSIX...'
echo    - Selecciona el archivo 'prism-0.3.2.vsix' incluido.
echo.
echo Presiona cualquier tecla para finalizar.
pause >nul

