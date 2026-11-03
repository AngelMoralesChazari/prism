@echo off
setlocal
title Instalador oficial de Prism v1.0.0
color 0A
cls

echo =======================================================
echo          INSTALADOR OFICIAL DE PRISM v1.0.0
echo =======================================================
echo.

REM 1. Verificar si Java esta instalado en Windows
where java >nul 2>nul
if %errorlevel% neq 0 goto :no_java

echo [OK] Java detectado en el sistema.
echo.
goto :install

:no_java
color 0C
echo [ERROR] No se encontro Java instalado en este equipo Windows.
echo.
echo Prism requiere Java (OpenJDK 17 o superior) para funcionar.
echo Descargalo e instalalo gratis desde:
echo   https://adoptium.net (Eclipse Temurin OpenJDK 17 o 21)
echo.
echo Presiona cualquier tecla para salir...
pause >nul
exit /b 1

:install
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
echo [OK] Archivos del runtime instalados en %LIB_DIR%

REM 4. Crear lanzador prc.cmd en %BIN_DIR%
echo @echo off > "%BIN_DIR%\prc.cmd"
echo setlocal >> "%BIN_DIR%\prc.cmd"
echo set "JAR_PATH=%%USERPROFILE%%\.prism\lib\prism.jar" >> "%BIN_DIR%\prc.cmd"
echo java -jar "%%JAR_PATH%%" %%* >> "%BIN_DIR%\prc.cmd"
echo [OK] Comando prc configurado en %BIN_DIR%

REM 5. Agregar %BIN_DIR% al PATH del usuario de Windows de forma permanente
echo.
echo Configurando variable de entorno PATH...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$b=[System.IO.Path]::GetFullPath('%BIN_DIR%'); $p=[Environment]::GetEnvironmentVariable('PATH','User'); if($p -notlike \"*$b*\"){ [Environment]::SetEnvironmentVariable('PATH', ($p+';'+$b).Trim(';'), 'User'); Write-Host '[OK] Directorio agregado al PATH global de Windows.' } else { Write-Host '[OK] El directorio ya estaba en el PATH.' }"

echo.
echo =======================================================
echo     INSTALACION COMPLETADA EXITOSAMENTE!
echo =======================================================
echo.
echo Pasos finales:
echo 1. Abre una nueva ventana de terminal (CMD o PowerShell).
echo 2. Escribe 'prc --version' para comprobar.
echo 3. Los plugins para editores estan listos en la carpeta 'extensions/'.
echo.
echo Presiona cualquier tecla para finalizar.
pause >nul
