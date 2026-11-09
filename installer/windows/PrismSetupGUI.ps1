# Instalador grafico de Prism v1.0.0 para Windows (PowerShell + WinForms)
$ErrorActionPreference = 'Continue'

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

if (-not ([System.Management.Automation.PSTypeName]'PrismWin32.NativeMethods').Type) {
    Add-Type -Namespace PrismWin32 -Name NativeMethods -MemberDefinition @"
[DllImport("user32.dll")]
public static extern bool SetProcessDPIAware();

[DllImport("user32.dll", SetLastError=true, CharSet=CharSet.Auto)]
public static extern IntPtr SendMessageTimeout(
    IntPtr hWnd, uint Msg, UIntPtr wParam, string lParam,
    uint fuFlags, uint uTimeout, out UIntPtr lpdwResult);
"@
}

try { [void][PrismWin32.NativeMethods]::SetProcessDPIAware() } catch {}
[System.Windows.Forms.Application]::EnableVisualStyles()

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PackageRoot = [System.IO.Path]::GetFullPath((Join-Path $ScriptDir '..\..'))
$DefaultInstallDir = if ($env:LOCALAPPDATA) {
    Join-Path $env:LOCALAPPDATA 'Programs\Prism'
} else {
    Join-Path $env:USERPROFILE '.prism'
}

function Find-SourceJar {
    $candidates = @(
        (Join-Path $PackageRoot 'prism.jar'),
        (Join-Path $PackageRoot 'lib\prism.jar')
    )
    foreach ($c in $candidates) {
        if (Test-Path -LiteralPath $c) { return $c }
    }
    return $null
}

function Get-JavaInfo {
    param([string]$TargetInstallDir)

    $candidates = New-Object System.Collections.Generic.List[string]
    if ($TargetInstallDir) {
        $bundled = Join-Path $TargetInstallDir 'runtime\bin\java.exe'
        if (Test-Path -LiteralPath $bundled) { $candidates.Add($bundled) }
    }
    $pkgRuntime = Join-Path $PackageRoot 'build\win-runtime\bin\java.exe'
    if (Test-Path -LiteralPath $pkgRuntime) { $candidates.Add($pkgRuntime) }

    $cmdJava = Get-Command 'java.exe' -ErrorAction SilentlyContinue
    if ($cmdJava -and $cmdJava.Source) { $candidates.Add($cmdJava.Source) }

    if ($env:JAVA_HOME) {
        $jhJava = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path -LiteralPath $jhJava) { $candidates.Add($jhJava) }
    }

    $roots = @(
        'C:\Program Files\Eclipse Adoptium',
        'C:\Program Files\Java',
        'C:\Program Files\Microsoft',
        'C:\Program Files\Amazon Corretto',
        'C:\Program Files\Zulu'
    )
    if ($env:LOCALAPPDATA) {
        $roots += (Join-Path $env:LOCALAPPDATA 'Programs\Eclipse Adoptium')
    }
    foreach ($r in $roots) {
        if (Test-Path -LiteralPath $r) {
            Get-ChildItem -LiteralPath $r -Directory -ErrorAction SilentlyContinue |
                Sort-Object Name -Descending |
                ForEach-Object {
                    $j = Join-Path $_.FullName 'bin\java.exe'
                    if (Test-Path -LiteralPath $j) { $candidates.Add($j) }
                }
        }
    }

    $seen = @{}
    $bestFallback = $null
    foreach ($exe in $candidates) {
        if (-not $exe -or $seen.ContainsKey($exe.ToLowerInvariant())) { continue }
        $seen[$exe.ToLowerInvariant()] = $true
        try {
            $psi = New-Object System.Diagnostics.ProcessStartInfo
            $psi.FileName = $exe
            $psi.Arguments = '-version'
            $psi.RedirectStandardError = $true
            $psi.RedirectStandardOutput = $true
            $psi.UseShellExecute = $false
            $psi.CreateNoWindow = $true
            $proc = [System.Diagnostics.Process]::Start($psi)
            $stderr = $proc.StandardError.ReadToEnd()
            $stdout = $proc.StandardOutput.ReadToEnd()
            $proc.WaitForExit(4000) | Out-Null
            $raw = ($stderr + "`n" + $stdout).Trim()
            $firstLine = ($raw -split "`r?`n")[0]
            $major = 0
            if ($raw -match 'version\s+"(\d+)(?:\.(\d+))?') {
                $v1 = [int]$Matches[1]
                $v2 = if ($Matches[2]) { [int]$Matches[2] } else { 0 }
                $major = if ($v1 -eq 1 -and $v2 -gt 0) { $v2 } else { $v1 }
            }
            $info = [PSCustomObject]@{
                Found = $true
                Executable = $exe
                Major = $major
                Ok = ($major -ge 17)
                VersionText = $firstLine
            }
            if ($info.Ok) { return $info }
            if (-not $bestFallback) { $bestFallback = $info }
        } catch {}
    }

    if ($bestFallback) { return $bestFallback }
    return [PSCustomObject]@{
        Found = $false
        Executable = $null
        Major = 0
        Ok = $false
        VersionText = 'No instalado'
    }
}

function Broadcast-EnvironmentChange {
    try {
        $result = [UIntPtr]::Zero
        [void][PrismWin32.NativeMethods]::SendMessageTimeout(
            [IntPtr]0xffff, 0x001A, [UIntPtr]::Zero, 'Environment', 2, 3000, [ref]$result
        )
    } catch {}
}

function Update-UserPath {
    param(
        [string]$BinDir,
        [switch]$RemoveOnly
    )
    $target = $BinDir.TrimEnd('\')
    $key = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey('Environment', $true)
    if (-not $key) {
        $key = [Microsoft.Win32.Registry]::CurrentUser.CreateSubKey('Environment')
    }
    $current = [string]$key.GetValue('Path', '', 'DoNotExpandEnvironmentNames')
    $parts = New-Object System.Collections.Generic.List[string]
    foreach ($entry in ($current -split ';')) {
        $clean = $entry.Trim()
        if (-not $clean) { continue }
        $norm = $clean.TrimEnd('\')
        if ($norm -ieq $target) { continue }
        if ($norm -like '*\.prism\bin') { continue }
        if ($norm -like '*\Programs\Prism\bin') { continue }
        $parts.Add($clean)
    }
    if (-not $RemoveOnly) {
        $parts.Add($target)
    }
    $newPath = ($parts.ToArray()) -join ';'
    $key.SetValue('Path', $newPath, [Microsoft.Win32.RegistryValueKind]::ExpandString)
    $key.Close()
    Broadcast-EnvironmentChange
}

function Register-PrAssociation {
    param([string]$IconPath)
    $classes = [Microsoft.Win32.Registry]::CurrentUser.CreateSubKey('Software\Classes\.pr')
    $classes.SetValue('', 'Prism.SourceFile')
    $classes.Close()

    $prog = [Microsoft.Win32.Registry]::CurrentUser.CreateSubKey('Software\Classes\Prism.SourceFile')
    $prog.SetValue('', 'Codigo fuente Prism (.pr)')
    $prog.Close()

    if (Test-Path -LiteralPath $IconPath) {
        $iconKey = [Microsoft.Win32.Registry]::CurrentUser.CreateSubKey('Software\Classes\Prism.SourceFile\DefaultIcon')
        $iconKey.SetValue('', "$IconPath,0")
        $iconKey.Close()
    }
}

function Unregister-PrAssociation {
    try {
        [Microsoft.Win32.Registry]::CurrentUser.DeleteSubKeyTree('Software\Classes\Prism.SourceFile', $false)
        $extKey = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey('Software\Classes\.pr', $true)
        if ($extKey) {
            if ([string]$extKey.GetValue('') -eq 'Prism.SourceFile') {
                $extKey.DeleteValue('', $false)
            }
            $extKey.Close()
        }
    } catch {}
}

function New-StartMenuShortcuts {
    param(
        [string]$InstallDir,
        [string]$ExamplesDir,
        [string]$IconPath,
        [bool]$IncludeExamples
    )
    try {
        $startMenu = [Environment]::GetFolderPath('Programs')
        $prismGroup = Join-Path $startMenu 'Prism'
        New-Item -ItemType Directory -Path $prismGroup -Force | Out-Null

        $wsh = New-Object -ComObject WScript.Shell
        $binDir = Join-Path $InstallDir 'bin'
        $prcCmd = Join-Path $binDir 'prc.cmd'

        $scTerm = $wsh.CreateShortcut((Join-Path $prismGroup 'Abrir terminal de Prism.lnk'))
        $scTerm.TargetPath = $env:ComSpec
        $scTerm.Arguments = '/k "set PATH=' + $binDir + ';%PATH%& cd /d "%USERPROFILE%" & echo Prism v1.0.0 listo. & "' + $prcCmd + '" --version"'
        $scTerm.WorkingDirectory = $env:USERPROFILE
        if (Test-Path -LiteralPath $IconPath) { $scTerm.IconLocation = "$IconPath,0" }
        $scTerm.Save()

        if ($IncludeExamples -and (Test-Path -LiteralPath $ExamplesDir)) {
            $scEx = $wsh.CreateShortcut((Join-Path $prismGroup 'Ejemplos de Prism.lnk'))
            $scEx.TargetPath = $ExamplesDir
            $scEx.WorkingDirectory = $ExamplesDir
            $scEx.Save()
        }
    } catch {}
}

function Remove-StartMenuShortcuts {
    try {
        $startMenu = [Environment]::GetFolderPath('Programs')
        $prismGroup = Join-Path $startMenu 'Prism'
        if (Test-Path -LiteralPath $prismGroup) {
            Remove-Item -LiteralPath $prismGroup -Recurse -Force -ErrorAction SilentlyContinue
        }
    } catch {}
}

# ---------------- Interfaz Grafica (Estilo Python Setup) ----------------

$fontTitle    = New-Object System.Drawing.Font('Segoe UI', 15, [System.Drawing.FontStyle]::Bold)
$fontSection  = New-Object System.Drawing.Font('Segoe UI', 10, [System.Drawing.FontStyle]::Bold)
$fontBody     = New-Object System.Drawing.Font('Segoe UI', 9.25)
$fontSmall    = New-Object System.Drawing.Font('Segoe UI', 8.5)
$fontMono     = New-Object System.Drawing.Font('Consolas', 8.5)

$colorBg      = [System.Drawing.Color]::White
$colorSidebar = [System.Drawing.Color]::FromArgb(24, 29, 43)
$colorAccent  = [System.Drawing.Color]::FromArgb(46, 125, 225)
$colorBorder  = [System.Drawing.Color]::FromArgb(218, 224, 232)
$colorMuted   = [System.Drawing.Color]::FromArgb(92, 102, 118)
$colorOk      = [System.Drawing.Color]::FromArgb(21, 128, 61)
$colorWarn    = [System.Drawing.Color]::FromArgb(180, 83, 9)

$form = New-Object System.Windows.Forms.Form
$form.Text = 'Prism v1.0.0 (64-bit) - Instalador'
$form.ClientSize = New-Object System.Drawing.Size(680, 445)
$form.FormBorderStyle = [System.Windows.Forms.FormBorderStyle]::FixedDialog
$form.MaximizeBox = $false
$form.StartPosition = [System.Windows.Forms.FormStartPosition]::CenterScreen
$form.BackColor = $colorBg
$form.Font = $fontBody

$icoFile = Join-Path $ScriptDir 'prism.ico'
if (Test-Path -LiteralPath $icoFile) {
    try { $form.Icon = New-Object System.Drawing.Icon($icoFile) } catch {}
}

# Barra lateral izquierda
$sidebar = New-Object System.Windows.Forms.Panel
$sidebar.Location = New-Object System.Drawing.Point(0, 0)
$sidebar.Size = New-Object System.Drawing.Size(185, 445)
$sidebar.BackColor = $colorSidebar
$form.Controls.Add($sidebar)

$logoFile = Join-Path $ScriptDir 'prism-logo.png'
if (Test-Path -LiteralPath $logoFile) {
    $pic = New-Object System.Windows.Forms.PictureBox
    $pic.Location = New-Object System.Drawing.Point(36, 48)
    $pic.Size = New-Object System.Drawing.Size(112, 112)
    $pic.SizeMode = [System.Windows.Forms.PictureBoxSizeMode]::Zoom
    $pic.Image = [System.Drawing.Image]::FromFile($logoFile)
    $sidebar.Controls.Add($pic)
}

$lblSideTitle = New-Object System.Windows.Forms.Label
$lblSideTitle.Text = 'Prism'
$lblSideTitle.Font = New-Object System.Drawing.Font('Segoe UI', 16, [System.Drawing.FontStyle]::Bold)
$lblSideTitle.ForeColor = [System.Drawing.Color]::White
$lblSideTitle.TextAlign = [System.Drawing.ContentAlignment]::MiddleCenter
$lblSideTitle.Location = New-Object System.Drawing.Point(10, 174)
$lblSideTitle.Size = New-Object System.Drawing.Size(165, 34)
$sidebar.Controls.Add($lblSideTitle)

$lblSideSub = New-Object System.Windows.Forms.Label
$lblSideSub.Text = "v1.0.0`npara Windows"
$lblSideSub.Font = New-Object System.Drawing.Font('Segoe UI', 9.5)
$lblSideSub.ForeColor = [System.Drawing.Color]::FromArgb(182, 196, 224)
$lblSideSub.TextAlign = [System.Drawing.ContentAlignment]::TopCenter
$lblSideSub.Location = New-Object System.Drawing.Point(10, 210)
$lblSideSub.Size = New-Object System.Drawing.Size(165, 46)
$sidebar.Controls.Add($lblSideSub)

$lblSideFooter = New-Object System.Windows.Forms.Label
$lblSideFooter.Text = "AB Software Group`nLicencia Apache 2.0"
$lblSideFooter.Font = $fontSmall
$lblSideFooter.ForeColor = [System.Drawing.Color]::FromArgb(130, 145, 170)
$lblSideFooter.TextAlign = [System.Drawing.ContentAlignment]::MiddleCenter
$lblSideFooter.Location = New-Object System.Drawing.Point(10, 385)
$lblSideFooter.Size = New-Object System.Drawing.Size(165, 40)
$sidebar.Controls.Add($lblSideFooter)

# Contenedor derecho (paginas)
$rightX = 205
$rightW = 455

# ---------------- PAGINA 1: CONFIGURACION ----------------
$pageSetup = New-Object System.Windows.Forms.Panel
$pageSetup.Location = New-Object System.Drawing.Point($rightX, 16)
$pageSetup.Size = New-Object System.Drawing.Size($rightW, 415)
$pageSetup.BackColor = $colorBg
$form.Controls.Add($pageSetup)

$lblWelcome = New-Object System.Windows.Forms.Label
$lblWelcome.Text = 'Instalar Prism v1.0.0 (64-bit)'
$lblWelcome.Font = $fontTitle
$lblWelcome.ForeColor = [System.Drawing.Color]::FromArgb(20, 25, 35)
$lblWelcome.Location = New-Object System.Drawing.Point(0, 4)
$lblWelcome.Size = New-Object System.Drawing.Size($rightW, 34)
$pageSetup.Controls.Add($lblWelcome)

$lblIntro = New-Object System.Windows.Forms.Label
$lblIntro.Text = 'Configura el compilador prc y su integracion con Windows con un solo clic.'
$lblIntro.Font = $fontBody
$lblIntro.ForeColor = $colorMuted
$lblIntro.Location = New-Object System.Drawing.Point(2, 40)
$lblIntro.Size = New-Object System.Drawing.Size($rightW, 24)
$pageSetup.Controls.Add($lblIntro)

# Caja de instalacion recomendada (estilo "Install Now")
$pnlCard = New-Object System.Windows.Forms.Panel
$pnlCard.Location = New-Object System.Drawing.Point(0, 74)
$pnlCard.Size = New-Object System.Drawing.Size($rightW, 92)
$pnlCard.BackColor = [System.Drawing.Color]::FromArgb(246, 249, 255)
$pnlCard.BorderStyle = [System.Windows.Forms.BorderStyle]::FixedSingle
$pageSetup.Controls.Add($pnlCard)

$lblCardTitle = New-Object System.Windows.Forms.Label
$lblCardTitle.Text = 'Instalacion recomendada (siguiente > instalar)'
$lblCardTitle.Font = $fontSection
$lblCardTitle.ForeColor = $colorAccent
$lblCardTitle.Location = New-Object System.Drawing.Point(12, 10)
$lblCardTitle.Size = New-Object System.Drawing.Size(430, 22)
$pnlCard.Controls.Add($lblCardTitle)

$lblCardPath = New-Object System.Windows.Forms.Label
$lblCardPath.Text = $DefaultInstallDir
$lblCardPath.Font = $fontSmall
$lblCardPath.ForeColor = [System.Drawing.Color]::FromArgb(30, 64, 175)
$lblCardPath.Location = New-Object System.Drawing.Point(12, 34)
$lblCardPath.Size = New-Object System.Drawing.Size(430, 18)
$pnlCard.Controls.Add($lblCardPath)

$lblCardDesc = New-Object System.Windows.Forms.Label
$lblCardDesc.Text = 'Incluye compilador prc, runtime prism.jar, ejemplos .pr y accesos directos.'
$lblCardDesc.Font = $fontSmall
$lblCardDesc.ForeColor = $colorMuted
$lblCardDesc.Location = New-Object System.Drawing.Point(12, 56)
$lblCardDesc.Size = New-Object System.Drawing.Size(430, 24)
$pnlCard.Controls.Add($lblCardDesc)

# Selector de carpeta
$lblDir = New-Object System.Windows.Forms.Label
$lblDir.Text = 'Carpeta de instalacion:'
$lblDir.Font = $fontSmall
$lblDir.ForeColor = [System.Drawing.Color]::FromArgb(40, 48, 60)
$lblDir.Location = New-Object System.Drawing.Point(0, 178)
$lblDir.Size = New-Object System.Drawing.Size(200, 18)
$pageSetup.Controls.Add($lblDir)

$txtDir = New-Object System.Windows.Forms.TextBox
$txtDir.Text = $DefaultInstallDir
$txtDir.Font = $fontSmall
$txtDir.Location = New-Object System.Drawing.Point(0, 198)
$txtDir.Size = New-Object System.Drawing.Size(356, 24)
$txtDir.Add_TextChanged({ $lblCardPath.Text = $txtDir.Text })
$pageSetup.Controls.Add($txtDir)

$btnBrowse = New-Object System.Windows.Forms.Button
$btnBrowse.Text = 'Examinar...'
$btnBrowse.Font = $fontSmall
$btnBrowse.Location = New-Object System.Drawing.Point(364, 197)
$btnBrowse.Size = New-Object System.Drawing.Size(90, 26)
$btnBrowse.Add_Click({
    $dlg = New-Object System.Windows.Forms.FolderBrowserDialog
    $dlg.Description = 'Selecciona la carpeta donde se instalara Prism'
    $dlg.ShowNewFolderButton = $true
    if (Test-Path -LiteralPath $txtDir.Text) { $dlg.SelectedPath = $txtDir.Text }
    if ($dlg.ShowDialog($form) -eq [System.Windows.Forms.DialogResult]::OK) {
        $chosen = $dlg.SelectedPath.TrimEnd('\')
        if (-not ($chosen -match '(?i)\\Prism$') -and -not (Test-Path (Join-Path $chosen 'lib\prism.jar'))) {
            $chosen = Join-Path $chosen 'Prism'
        }
        $txtDir.Text = $chosen
    }
})
$pageSetup.Controls.Add($btnBrowse)

# Opciones tipo Python (checkboxes)
$chkPath = New-Object System.Windows.Forms.CheckBox
$chkPath.Text = 'Agregar Prism (prc) al PATH del sistema (Recomendado)'
$chkPath.Checked = $true
$chkPath.Font = $fontBody
$chkPath.Location = New-Object System.Drawing.Point(2, 236)
$chkPath.Size = New-Object System.Drawing.Size(445, 24)
$pageSetup.Controls.Add($chkPath)

$chkAssoc = New-Object System.Windows.Forms.CheckBox
$chkAssoc.Text = 'Asociar archivos .pr con Prism en el Explorador de Windows'
$chkAssoc.Checked = $true
$chkAssoc.Font = $fontBody
$chkAssoc.Location = New-Object System.Drawing.Point(2, 262)
$chkAssoc.Size = New-Object System.Drawing.Size(445, 24)
$pageSetup.Controls.Add($chkAssoc)

$chkExamples = New-Object System.Windows.Forms.CheckBox
$chkExamples.Text = 'Instalar ejemplos oficiales (.pr) y accesos en el Menu Inicio'
$chkExamples.Checked = $true
$chkExamples.Font = $fontBody
$chkExamples.Location = New-Object System.Drawing.Point(2, 288)
$chkExamples.Size = New-Object System.Drawing.Size(445, 24)
$pageSetup.Controls.Add($chkExamples)

# Estado de Java
$pnlJava = New-Object System.Windows.Forms.Panel
$pnlJava.Location = New-Object System.Drawing.Point(0, 320)
$pnlJava.Size = New-Object System.Drawing.Size($rightW, 46)
$pnlJava.BackColor = [System.Drawing.Color]::FromArgb(248, 250, 252)
$pnlJava.BorderStyle = [System.Windows.Forms.BorderStyle]::FixedSingle
$pageSetup.Controls.Add($pnlJava)

$lblJavaStatus = New-Object System.Windows.Forms.Label
$lblJavaStatus.Font = $fontSmall
$lblJavaStatus.Location = New-Object System.Drawing.Point(10, 7)
$lblJavaStatus.Size = New-Object System.Drawing.Size(310, 32)
$pnlJava.Controls.Add($lblJavaStatus)

$btnGetJava = New-Object System.Windows.Forms.Button
$btnGetJava.Text = 'Descargar Java 17'
$btnGetJava.Font = $fontSmall
$btnGetJava.Location = New-Object System.Drawing.Point(325, 9)
$btnGetJava.Size = New-Object System.Drawing.Size(120, 28)
$btnGetJava.Visible = $false
$btnGetJava.Add_Click({
    Start-Process 'https://adoptium.net/es/temurin/releases/?version=17'
})
$pnlJava.Controls.Add($btnGetJava)

$javaInfo = Get-JavaInfo -TargetInstallDir $DefaultInstallDir
if ($javaInfo.Ok) {
    $lblJavaStatus.Text = "[OK] Java compatible detectado:`n$($javaInfo.VersionText)"
    $lblJavaStatus.ForeColor = $colorOk
} elseif ($javaInfo.Found) {
    $lblJavaStatus.Text = "[AVISO] Java antiguo detectado ($($javaInfo.VersionText)).`nPrism requiere OpenJDK 17 o superior."
    $lblJavaStatus.ForeColor = $colorWarn
    $btnGetJava.Visible = $true
} else {
    $lblJavaStatus.Text = "[AVISO] No se detecto Java OpenJDK 17+ en este equipo.`nInstalalo para poder ejecutar prc."
    $lblJavaStatus.ForeColor = $colorWarn
    $btnGetJava.Visible = $true
}

# Botones inferiores de la pagina 1
$btnUninstall = New-Object System.Windows.Forms.Button
$btnUninstall.Text = 'Desinstalar'
$btnUninstall.Font = $fontBody
$btnUninstall.Location = New-Object System.Drawing.Point(0, 380)
$btnUninstall.Size = New-Object System.Drawing.Size(105, 32)
$btnUninstall.Visible = (Test-Path -LiteralPath (Join-Path $DefaultInstallDir 'bin\prc.cmd'))
$pageSetup.Controls.Add($btnUninstall)

$btnInstall = New-Object System.Windows.Forms.Button
$btnInstall.Text = 'Instalar ahora >'
$btnInstall.Font = $fontSection
$btnInstall.BackColor = $colorAccent
$btnInstall.ForeColor = [System.Drawing.Color]::White
$btnInstall.FlatStyle = [System.Windows.Forms.FlatStyle]::Flat
$btnInstall.FlatAppearance.BorderSize = 0
$btnInstall.Location = New-Object System.Drawing.Point(222, 380)
$btnInstall.Size = New-Object System.Drawing.Size(136, 32)
$pageSetup.Controls.Add($btnInstall)

$btnCancel = New-Object System.Windows.Forms.Button
$btnCancel.Text = 'Cancelar'
$btnCancel.Font = $fontBody
$btnCancel.Location = New-Object System.Drawing.Point(366, 380)
$btnCancel.Size = New-Object System.Drawing.Size(88, 32)
$btnCancel.Add_Click({ $form.Close() })
$pageSetup.Controls.Add($btnCancel)

# ---------------- PAGINA 2: PROGRESO / FINALIZADO ----------------
$pageProgress = New-Object System.Windows.Forms.Panel
$pageProgress.Location = New-Object System.Drawing.Point($rightX, 16)
$pageProgress.Size = New-Object System.Drawing.Size($rightW, 415)
$pageProgress.BackColor = $colorBg
$pageProgress.Visible = $false
$form.Controls.Add($pageProgress)

$lblProgTitle = New-Object System.Windows.Forms.Label
$lblProgTitle.Text = 'Instalando Prism v1.0.0...'
$lblProgTitle.Font = $fontTitle
$lblProgTitle.ForeColor = [System.Drawing.Color]::FromArgb(20, 25, 35)
$lblProgTitle.Location = New-Object System.Drawing.Point(0, 4)
$lblProgTitle.Size = New-Object System.Drawing.Size($rightW, 34)
$pageProgress.Controls.Add($lblProgTitle)

$lblProgStep = New-Object System.Windows.Forms.Label
$lblProgStep.Text = 'Preparando archivos de instalacion...'
$lblProgStep.Font = $fontBody
$lblProgStep.ForeColor = $colorMuted
$lblProgStep.Location = New-Object System.Drawing.Point(2, 46)
$lblProgStep.Size = New-Object System.Drawing.Size($rightW, 24)
$pageProgress.Controls.Add($lblProgStep)

$progressBar = New-Object System.Windows.Forms.ProgressBar
$progressBar.Minimum = 0
$progressBar.Maximum = 100
$progressBar.Value = 0
$progressBar.Location = New-Object System.Drawing.Point(0, 78)
$progressBar.Size = New-Object System.Drawing.Size($rightW, 22)
$pageProgress.Controls.Add($progressBar)

$txtLog = New-Object System.Windows.Forms.TextBox
$txtLog.Multiline = $true
$txtLog.ReadOnly = $true
$txtLog.ScrollBars = [System.Windows.Forms.ScrollBars]::Vertical
$txtLog.BackColor = [System.Drawing.Color]::FromArgb(248, 250, 252)
$txtLog.Font = $fontMono
$txtLog.Location = New-Object System.Drawing.Point(0, 114)
$txtLog.Size = New-Object System.Drawing.Size($rightW, 248)
$pageProgress.Controls.Add($txtLog)

$btnOpenTerm = New-Object System.Windows.Forms.Button
$btnOpenTerm.Text = 'Abrir terminal con Prism'
$btnOpenTerm.Font = $fontBody
$btnOpenTerm.Location = New-Object System.Drawing.Point(0, 380)
$btnOpenTerm.Size = New-Object System.Drawing.Size(172, 32)
$btnOpenTerm.Visible = $false
$pageProgress.Controls.Add($btnOpenTerm)

$btnOpenExamples = New-Object System.Windows.Forms.Button
$btnOpenExamples.Text = 'Ver ejemplos'
$btnOpenExamples.Font = $fontBody
$btnOpenExamples.Location = New-Object System.Drawing.Point(180, 380)
$btnOpenExamples.Size = New-Object System.Drawing.Size(110, 32)
$btnOpenExamples.Visible = $false
$pageProgress.Controls.Add($btnOpenExamples)

$btnFinish = New-Object System.Windows.Forms.Button
$btnFinish.Text = 'Finalizar'
$btnFinish.Font = $fontSection
$btnFinish.BackColor = $colorAccent
$btnFinish.ForeColor = [System.Drawing.Color]::White
$btnFinish.FlatStyle = [System.Windows.Forms.FlatStyle]::Flat
$btnFinish.FlatAppearance.BorderSize = 0
$btnFinish.Location = New-Object System.Drawing.Point(344, 380)
$btnFinish.Size = New-Object System.Drawing.Size(110, 32)
$btnFinish.Enabled = $false
$btnFinish.Add_Click({ $form.Close() })
$pageProgress.Controls.Add($btnFinish)

function Add-InstallLog {
    param([string]$Message, [int]$Percent)
    if ($Percent -ge 0) { $progressBar.Value = [Math]::Min(100, $Percent) }
    $lblProgStep.Text = $Message
    $txtLog.AppendText($Message + "`r`n")
    [System.Windows.Forms.Application]::DoEvents()
}

$script:InstalledBinDir = ''
$script:InstalledExamplesDir = ''

$btnOpenTerm.Add_Click({
    if ($script:InstalledBinDir) {
        $prc = Join-Path $script:InstalledBinDir 'prc.cmd'
        $workDir = if ($script:InstalledExamplesDir -and (Test-Path -LiteralPath $script:InstalledExamplesDir)) {
            $script:InstalledExamplesDir
        } else {
            $env:USERPROFILE
        }
        $args = '/k "set PATH=' + $script:InstalledBinDir + ';%PATH%& cd /d "' + $workDir + '" & "' + $prc + '" --version & echo. & echo Prueba: prc run prueba_nl.pr"'
        Start-Process -FilePath $env:ComSpec -ArgumentList $args -WorkingDirectory $workDir
    }
})

$btnOpenExamples.Add_Click({
    if ($script:InstalledExamplesDir -and (Test-Path -LiteralPath $script:InstalledExamplesDir)) {
        Start-Process -FilePath 'explorer.exe' -ArgumentList $script:InstalledExamplesDir
    }
})

$btnInstall.Add_Click({
    $targetDir = $txtDir.Text.Trim()
    if (-not $targetDir) {
        [System.Windows.Forms.MessageBox]::Show($form, 'Especifica una carpeta de instalacion valida.', 'Prism Setup', 'OK', 'Warning') | Out-Null
        return
    }

    $sourceJar = Find-SourceJar
    if (-not $sourceJar) {
        [System.Windows.Forms.MessageBox]::Show($form, "No se encontro 'prism.jar' dentro de la carpeta del instalador ($PackageRoot).", 'Error de paquete', 'OK', 'Error') | Out-Null
        return
    }

    $pageSetup.Visible = $false
    $pageProgress.Visible = $true
    [System.Windows.Forms.Application]::DoEvents()

    try {
        $binDir = Join-Path $targetDir 'bin'
        $libDir = Join-Path $targetDir 'lib'
        $exDir  = Join-Path $targetDir 'examples'
        $script:InstalledBinDir = $binDir
        $script:InstalledExamplesDir = $exDir

        Add-InstallLog "Creando estructura en $targetDir..." 15
        New-Item -ItemType Directory -Path $binDir, $libDir -Force | Out-Null

        Add-InstallLog 'Copiando runtime prism.jar...' 35
        Copy-Item -LiteralPath $sourceJar -Destination (Join-Path $libDir 'prism.jar') -Force

        $srcPrc = Join-Path $PackageRoot 'bin\prc.cmd'
        $dstPrc = Join-Path $binDir 'prc.cmd'
        if (Test-Path -LiteralPath $srcPrc) {
            Copy-Item -LiteralPath $srcPrc -Destination $dstPrc -Force
        } else {
            @(
                '@echo off',
                'setlocal EnableExtensions',
                'set "JAR_PATH=%~dp0..\lib\prism.jar"',
                'java -Dfile.encoding=UTF-8 -jar "%JAR_PATH%" %*'
            ) | Set-Content -LiteralPath $dstPrc -Encoding Ascii
        }

        foreach ($extra in @('LICENSE', 'README.md', 'LEEME_INSTALACION.txt')) {
            $p = Join-Path $PackageRoot $extra
            if (Test-Path -LiteralPath $p) {
                Copy-Item -LiteralPath $p -Destination (Join-Path $targetDir $extra) -Force
            }
        }
        if (Test-Path -LiteralPath $icoFile) {
            Copy-Item -LiteralPath $icoFile -Destination (Join-Path $targetDir 'prism.ico') -Force
        }

        foreach ($rtCand in @((Join-Path $PackageRoot 'runtime'), (Join-Path $PackageRoot 'build\win-runtime'))) {
            if (Test-Path -LiteralPath (Join-Path $rtCand 'bin\java.exe')) {
                Add-InstallLog 'Copiando runtime Java integrado...' 45
                Copy-Item -LiteralPath $rtCand -Destination (Join-Path $targetDir 'runtime') -Recurse -Force
                break
            }
        }

        if ($chkExamples.Checked) {
            $srcEx = Join-Path $PackageRoot 'examples'
            if (Test-Path -LiteralPath $srcEx) {
                Add-InstallLog 'Instalando ejemplos oficiales (.pr)...' 55
                New-Item -ItemType Directory -Path $exDir -Force | Out-Null
                Get-ChildItem -LiteralPath $srcEx -Filter '*.pr' -ErrorAction SilentlyContinue |
                    ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination $exDir -Force }
            }
        }

        if ($chkPath.Checked) {
            Add-InstallLog 'Agregando prc al PATH del usuario en Windows...' 70
            Update-UserPath -BinDir $binDir
        }

        if ($chkAssoc.Checked) {
            Add-InstallLog 'Registrando asociacion de archivos .pr...' 82
            Register-PrAssociation -IconPath (Join-Path $targetDir 'prism.ico')
        }

        if ($chkExamples.Checked) {
            Add-InstallLog 'Creando accesos directos en el Menu Inicio...' 90
            New-StartMenuShortcuts -InstallDir $targetDir -ExamplesDir $exDir -IconPath (Join-Path $targetDir 'prism.ico') -IncludeExamples $true
        }

        Add-InstallLog 'Verificando compilador prc...' 95
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = $env:ComSpec
        $psi.Arguments = '/c ""' + $dstPrc + '" --version"'
        $psi.RedirectStandardOutput = $true
        $psi.RedirectStandardError = $true
        $psi.UseShellExecute = $false
        $psi.CreateNoWindow = $true
        $pCheck = [System.Diagnostics.Process]::Start($psi)
        $outVer = ($pCheck.StandardOutput.ReadToEnd() + $pCheck.StandardError.ReadToEnd()).Trim()
        $pCheck.WaitForExit(6000) | Out-Null

        if ($pCheck.ExitCode -eq 0 -and $outVer) {
            Add-InstallLog "[OK] Verificacion correcta: $outVer" 100
            Add-InstallLog "Instalado en: $targetDir" 100
            Add-InstallLog 'Abre una terminal nueva y ejecuta: prc --version' 100
            $lblProgTitle.Text = 'Instalacion completada correctamente'
            $lblProgTitle.ForeColor = $colorOk
            $btnOpenTerm.Visible = $true
        } else {
            Add-InstallLog '[AVISO] Archivos instalados, pero falta Java 17+ para ejecutar prc.' 100
            Add-InstallLog 'Descarga Java 17 gratis en: https://adoptium.net' 100
            $lblProgTitle.Text = 'Instalado (requiere Java 17+)'
            $lblProgTitle.ForeColor = $colorWarn
        }

        if (Test-Path -LiteralPath $exDir) { $btnOpenExamples.Visible = $true }
    } catch {
        Add-InstallLog "[ERROR] $($_.Exception.Message)" 100
        $lblProgTitle.Text = 'Ocurrio un error durante la instalacion'
        $lblProgTitle.ForeColor = [System.Drawing.Color]::Firebrick
    }

    $btnFinish.Enabled = $true
})

$btnUninstall.Add_Click({
    $targetDir = $txtDir.Text.Trim()
    $res = [System.Windows.Forms.MessageBox]::Show(
        $form,
        "Deseas desinstalar Prism de:`n$targetDir?",
        'Desinstalar Prism',
        [System.Windows.Forms.MessageBoxButtons]::YesNo,
        [System.Windows.Forms.MessageBoxIcon]::Question
    )
    if ($res -ne [System.Windows.Forms.DialogResult]::Yes) { return }

    $pageSetup.Visible = $false
    $pageProgress.Visible = $true
    $lblProgTitle.Text = 'Desinstalando Prism...'
    [System.Windows.Forms.Application]::DoEvents()

    try {
        Add-InstallLog 'Limpiando variable PATH del usuario...' 35
        Update-UserPath -BinDir (Join-Path $targetDir 'bin') -RemoveOnly
        Add-InstallLog 'Eliminando asociacion de archivos .pr...' 60
        Unregister-PrAssociation
        Add-InstallLog 'Eliminando accesos del Menu Inicio...' 80
        Remove-StartMenuShortcuts
        if (Test-Path -LiteralPath $targetDir) {
            Add-InstallLog "Eliminando archivos de $targetDir..." 92
            Remove-Item -LiteralPath $targetDir -Recurse -Force -ErrorAction SilentlyContinue
        }
        Add-InstallLog '[OK] Prism desinstalado correctamente.' 100
        $lblProgTitle.Text = 'Desinstalacion completada'
        $lblProgTitle.ForeColor = $colorOk
    } catch {
        Add-InstallLog "[ERROR] $($_.Exception.Message)" 100
    }
    $btnFinish.Enabled = $true
})

$form.AcceptButton = $btnInstall
$form.CancelButton = $btnCancel
[void]$form.ShowDialog()
