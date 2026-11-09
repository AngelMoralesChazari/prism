# Script para compilar el instalador nativo Prism-1.0.0-Setup.exe en Windows (Inno Setup 6 + jlink)
param(
    [switch]$SkipRuntime,
    [switch]$SkipBuildJar
)

$ErrorActionPreference = 'Stop'

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot  = [System.IO.Path]::GetFullPath((Join-Path $ScriptDir '..\..'))
Push-Location $RepoRoot

try {
    Write-Host '======================================================='
    Write-Host '   Compilando instalador nativo de Prism para Windows   '
    Write-Host '======================================================='

    if (-not $SkipBuildJar) {
        $javacCmd = Get-Command 'javac.exe' -ErrorAction SilentlyContinue
        $jarCmd   = Get-Command 'jar.exe' -ErrorAction SilentlyContinue
        if (-not $javacCmd -or -not $jarCmd) {
            throw 'Se requiere Java JDK 17 o superior en el PATH (javac.exe y jar.exe).'
        }

        Write-Host '[1/3] Compilando prism.jar (objetivo Java 17)...'
        $classesDir = Join-Path $RepoRoot 'target\classes'
        if (Test-Path -LiteralPath $classesDir) {
            Remove-Item -LiteralPath $classesDir -Recurse -Force
        }
        New-Item -ItemType Directory -Path $classesDir -Force | Out-Null

        $sourcesFile = Join-Path $RepoRoot 'target\sources.txt'
        $sourceLines = Get-ChildItem -Path (Join-Path $RepoRoot 'src\main\java') -Filter '*.java' -Recurse |
            ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' }
        [System.IO.File]::WriteAllLines($sourcesFile, $sourceLines, (New-Object System.Text.UTF8Encoding($false)))

        & $javacCmd.Source --release 17 -encoding UTF-8 -d $classesDir "@$sourcesFile"
        if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion con javac.' }

        $resDir = Join-Path $RepoRoot 'src\main\resources'
        if (Test-Path -LiteralPath $resDir) {
            Copy-Item -Path (Join-Path $resDir '*') -Destination $classesDir -Recurse -Force
        }

        & $jarCmd.Source --create --file (Join-Path $RepoRoot 'prism.jar') --main-class prism.pr.PrcCli -C $classesDir .
        if ($LASTEXITCODE -ne 0) { throw 'Fallo la creacion de prism.jar.' }

        $extBin = Join-Path $RepoRoot 'extensions\prism\bin'
        if (Test-Path -LiteralPath $extBin) {
            Copy-Item -LiteralPath (Join-Path $RepoRoot 'prism.jar') -Destination (Join-Path $extBin 'prism.jar') -Force
        }
    } else {
        if (-not (Test-Path -LiteralPath (Join-Path $RepoRoot 'prism.jar'))) {
            throw 'No existe prism.jar y se especifico -SkipBuildJar.'
        }
    }

    $runtimeDir = Join-Path $RepoRoot 'build\win-runtime'
    if (Test-Path -LiteralPath $runtimeDir) {
        Remove-Item -LiteralPath $runtimeDir -Recurse -Force
    }

    if (-not $SkipRuntime) {
        $jlinkCmd = Get-Command 'jlink.exe' -ErrorAction SilentlyContinue
        if (-not $jlinkCmd -and $env:JAVA_HOME) {
            $cand = Join-Path $env:JAVA_HOME 'bin\jlink.exe'
            if (Test-Path -LiteralPath $cand) { $jlinkCmd = Get-Command $cand }
        }
        if (-not $jlinkCmd) {
            throw 'No se encontro jlink.exe. Instala un JDK completo (Temurin 17+) o usa -SkipRuntime.'
        }

        Write-Host '[2/3] Generando runtime JVM minimo (jlink)...'
        New-Item -ItemType Directory -Path (Split-Path -Parent $runtimeDir) -Force | Out-Null
        & $jlinkCmd.Source `
            --add-modules 'java.base,java.compiler,jdk.compiler' `
            --strip-debug `
            --no-man-pages `
            --no-header-files `
            --compress=2 `
            --output $runtimeDir
        if ($LASTEXITCODE -ne 0) { throw 'Fallo jlink al construir build\win-runtime.' }
    } else {
        Write-Host '[2/3] Omitiendo runtime integrado (-SkipRuntime).'
    }

    Write-Host '[3/3] Compilando Prism-1.0.0-Setup.exe con Inno Setup 6...'
    $isccFromPath = Get-Command 'ISCC.exe' -ErrorAction SilentlyContinue
    $isccCandidates = @(
        $(if ($isccFromPath) { $isccFromPath.Source }),
        'C:\Program Files (x86)\Inno Setup 6\ISCC.exe',
        'C:\Program Files\Inno Setup 6\ISCC.exe',
        $(if ($env:LOCALAPPDATA) { Join-Path $env:LOCALAPPDATA 'Programs\Inno Setup 6\ISCC.exe' })
    ) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

    $iscc = $isccCandidates | Select-Object -First 1
    if (-not $iscc) {
        throw 'No se encontro Inno Setup 6 (ISCC.exe). Instalalo desde https://jrsoftware.org/isinfo.php o via: choco install innosetup -y'
    }

    New-Item -ItemType Directory -Path (Join-Path $RepoRoot 'dist') -Force | Out-Null
    & $iscc (Join-Path $ScriptDir 'prism-setup.iss')
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion del instalador Inno Setup.' }

    $outExe = Join-Path $RepoRoot 'dist\Prism-1.0.0-Setup.exe'
    Write-Host "[OK] Instalador generado exitosamente: $outExe"
} finally {
    Pop-Location
}
