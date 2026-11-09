@echo off
setlocal EnableExtensions
title Instalador Grafico de Prism v1.0.0

set "SCRIPT_DIR=%~dp0"
set "GUI_SCRIPT=%SCRIPT_DIR%installer\windows\PrismSetupGUI.ps1"

if not exist "%GUI_SCRIPT%" (
    echo [ERROR] No se encontro el asistente grafico: %GUI_SCRIPT%
    echo Ejecutando instalador de consola...
    call "%SCRIPT_DIR%install.bat"
    exit /b %errorlevel%
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -STA -File "%GUI_SCRIPT%"
if %errorlevel% neq 0 (
    echo.
    echo [AVISO] No se pudo iniciar la ventana grafica. Puedes usar install.bat en consola.
    pause
    exit /b %errorlevel%
)
exit /b 0

