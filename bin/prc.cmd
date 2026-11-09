@echo off
REM Prism Compiler & Runtime Launcher (prc) v1.0.0
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PRISM_HOME=%%~fI"

set "JAR_PATH=%PRISM_HOME%\lib\prism.jar"
if not exist "%JAR_PATH%" set "JAR_PATH=%PRISM_HOME%\prism.jar"

if not exist "%JAR_PATH%" (
    echo [ERROR] No se encontro prism.jar en %PRISM_HOME% >&2
    exit /b 1
)

set "JAVA_EXE="

REM 1. Runtime Java empaquetado con Prism (instalador standalone)
if exist "%PRISM_HOME%\runtime\bin\java.exe" (
    set "JAVA_EXE=%PRISM_HOME%\runtime\bin\java.exe"
    goto :run_prism
)

REM 2. Java en el PATH del sistema
where java >nul 2>nul
if %errorlevel% equ 0 (
    set "JAVA_EXE=java"
    goto :run_prism
)

REM 3. JAVA_HOME configurado
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
        goto :run_prism
    )
)

REM 4. Busqueda automatica en rutas estandar de Windows
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
                set "JAVA_EXE=%%~D\bin\java.exe"
                goto :run_prism
            )
        )
        for /d %%D in ("%%~B\zulu*") do (
            if exist "%%~D\bin\java.exe" (
                set "JAVA_EXE=%%~D\bin\java.exe"
                goto :run_prism
            )
        )
    )
)

echo [ERROR] No se encontro Java OpenJDK 17 o superior en este equipo. >&2
echo Puedes instalarlo desde https://adoptium.net o usar el instalador completo de Prism. >&2
exit /b 1

:run_prism
"%JAVA_EXE%" -Dfile.encoding=UTF-8 -jar "%JAR_PATH%" %*
exit /b %errorlevel%
