@echo off
REM Prism Compiler & Runtime Launcher (prc) v1.0.0
setlocal
set "SCRIPT_DIR=%~dp0"
set "PRISM_HOME=%SCRIPT_DIR%.."

set "JAR_PATH=%PRISM_HOME%\prism.jar"
if not exist "%JAR_PATH%" set "JAR_PATH=%PRISM_HOME%\lib\prism.jar"

if not exist "%JAR_PATH%" (
    echo Error: No se encontro 'prism.jar' en %PRISM_HOME% >&2
    exit /b 1
)

java -jar "%JAR_PATH%" %*

