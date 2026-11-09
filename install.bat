@echo off
setlocal EnableExtensions
title Instalador oficial de Prism v1.0.0
color 0A
cls

echo =======================================================
echo          INSTALADOR OFICIAL DE PRISM v1.0.0
echo =======================================================
echo.

REM 1. Localizar Java en PATH, JAVA_HOME o rutas estandar de Windows
set "JAVA_FOUND="

where java >nul 2>nul
if %errorlevel% equ 0 (
    set "JAVA_FOUND=java"
    goto :java_ok
)

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_FOUND=%JAVA_HOME%\bin\java.exe"
        goto :java_ok
    )
)

for %%B in (
    "C:\Program Files\Eclipse Adoptium"
    "C:\Program Files\Java"
    "C:\Program Files\Microsoft"
    "C:\Program Files\Amazon Corretto"
    "C:\Program Files\Zulu"
    "%LocalAppData%\Programs\Eclipse Adoptium"
) do (
    if exist "%%~B" (
        for /d %%D in ("%%~B\jdk*") do (
            if exist "%%~D\bin\java.exe" (
                set "JAVA_FOUND=%%~D\bin\java.exe"
                goto :java_ok
            )
        )
        for /d %%D in ("%%~B\zulu*") do (
            if exist "%%~D\bin\java.exe" (
                set "JAVA_FOUND=%%~D\bin\java.exe"
                goto :java_ok
            )
        )
    )
)

:no_java
color 0C
echo [ERROR] No se encontro Java instalado en este equipo Windows.
echo.
echo Prism requiere Java OpenJDK 17 o superior para ejecutarse.
echo Descargalo e instalalo gratis desde:
echo   https://adoptium.net
echo.
echo O bien usa el instalador grafico completo Prism-1.0.0-Setup.exe
echo que ya incluye su propio runtime integrado.
echo.
echo Presiona cualquier tecla para salir...
pause >nul
exit /b 1

:java_ok
echo [OK] Java detectado: %JAVA_FOUND%
echo.

REM 2. Definir rutas de instalacion estandar (%LocalAppData%\Programs\Prism)
set "PRISM_DIR=%LocalAppData%\Programs\Prism"
if not defined LocalAppData set "PRISM_DIR=%USERPROFILE%\.prism"
set "BIN_DIR=%PRISM_DIR%\bin"
set "LIB_DIR=%PRISM_DIR%\lib"
set "EX_DIR=%PRISM_DIR%\examples"

if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"
if not exist "%LIB_DIR%" mkdir "%LIB_DIR%"
if not exist "%EX_DIR%" mkdir "%EX_DIR%"

REM 3. Copiar prism.jar
set "SCRIPT_DIR=%~dp0"
if exist "%SCRIPT_DIR%prism.jar" (
    copy /y "%SCRIPT_DIR%prism.jar" "%LIB_DIR%\prism.jar" >nul
    goto :copy_ok
)
if exist "%SCRIPT_DIR%lib\prism.jar" (
    copy /y "%SCRIPT_DIR%lib\prism.jar" "%LIB_DIR%\prism.jar" >nul
    goto :copy_ok
)

color 0C
echo [ERROR] No se encontro prism.jar en el paquete instalador.
pause
exit /b 1

:copy_ok
echo [OK] Runtime instalado en %LIB_DIR%\prism.jar

REM 4. Copiar ejemplos si existen
if exist "%SCRIPT_DIR%examples\*.pr" (
    copy /y "%SCRIPT_DIR%examples\*.pr" "%EX_DIR%\" >nul 2>nul
)

REM 5. Instalar lanzador prc.cmd en %BIN_DIR%
if exist "%SCRIPT_DIR%bin\prc.cmd" (
    copy /y "%SCRIPT_DIR%bin\prc.cmd" "%BIN_DIR%\prc.cmd" >nul
    goto :launcher_ok
)

(
    echo @echo off
    echo setlocal EnableExtensions
    echo set "JAR_PATH=%%~dp0..\lib\prism.jar"
    echo java -Dfile.encoding=UTF-8 -jar "%%JAR_PATH%%" %%*
) > "%BIN_DIR%\prc.cmd"

:launcher_ok
echo [OK] Comando prc instalado en %BIN_DIR%\prc.cmd

REM 6. Actualizar PATH del usuario (eliminando rutas antiguas de Prism) y notificar a Windows
echo.
echo Configurando variable de entorno PATH...
set "PRISM_BIN_DIR=%BIN_DIR%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$bin = $env:PRISM_BIN_DIR.TrimEnd('\'); $key = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey('Environment', $true); if (-not $key) { $key = [Microsoft.Win32.Registry]::CurrentUser.CreateSubKey('Environment') }; $cur = [string]$key.GetValue('Path', '', 'DoNotExpandEnvironmentNames'); $parts = @($cur -split ';' | Where-Object { $p = $_.Trim().TrimEnd('\'); $p -and ($p -ine $bin) -and (-not ($p -like '*\.prism\bin')) -and (-not ($p -like '*\Programs\Prism\bin')) }); $newPath = ($parts + $bin) -join ';'; $key.SetValue('Path', $newPath, 'ExpandString'); $key.Close(); try { $q = [char]34; $sig = '[DllImport(' + $q + 'user32.dll' + $q + ', SetLastError=true, CharSet=CharSet.Auto)] public static extern IntPtr SendMessageTimeout(IntPtr hWnd, uint Msg, UIntPtr wParam, string lParam, uint fuFlags, uint uTimeout, out UIntPtr lpdwResult);'; Add-Type -Namespace Win32 -Name NativeMethods -MemberDefinition $sig; $r = [UIntPtr]::Zero; [void][Win32.NativeMethods]::SendMessageTimeout([IntPtr]0xffff, 0x001A, [UIntPtr]::Zero, 'Environment', 2, 3000, [ref]$r) } catch {}; Write-Host '[OK] Directorio agregado al PATH del usuario:' $bin"

if %errorlevel% neq 0 (
    echo [AVISO] No se pudo actualizar el PATH automaticamente. Agrega manualmente: %BIN_DIR%
)

echo.
echo Verificando ejecucion directa:
call "%BIN_DIR%\prc.cmd" --version
if %errorlevel% neq 0 (
    color 0E
    echo.
    echo [AVISO] prc no pudo ejecutarse. Verifica que Java instalado sea version 17 o superior.
    echo Descarga Java 17 gratis en: https://adoptium.net
)

echo.
echo =======================================================
echo     INSTALACION COMPLETADA EXITOSAMENTE
echo =======================================================
echo.
echo Pasos finales:
echo 1. Cierra esta ventana y abre una terminal NUEVA de PowerShell o CMD.
echo 2. Escribe: prc --version
echo 3. Para ejecutar un programa: prc run "%EX_DIR%\prueba_nl.pr"
echo.
echo Presiona cualquier tecla para finalizar...
pause >nul
