; Instalador grafico oficial de Prism para Windows (Inno Setup 6)
; Compilar mediante installer\windows\build-installer.ps1 o GitHub Actions

#define MyAppName "Prism"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "AB Software Group"
#define MyAppURL "https://github.com/AngelMoralesChazari/prism"
#define MyAppExeName "prc.cmd"
#define HasRuntime DirExists("..\..\build\win-runtime")

[Setup]
AppId={{9B7E4E2B-8D4A-4B8F-9C2E-7A1E6D2F1001}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={localappdata}\Programs\{#MyAppName}
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
LicenseFile=..\..\LICENSE
OutputDir=..\..\dist
OutputBaseFilename=Prism-{#MyAppVersion}-Setup
SetupIconFile=prism.ico
UninstallDisplayIcon={app}\prism.ico
WizardStyle=modern
WizardImageFile=wizard-sidebar.bmp
WizardSmallImageFile=wizard-small.bmp
Compression=lzma2/max
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
ChangesEnvironment=yes
ChangesAssociations=yes

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Messages]
spanish.WelcomeLabel1=Bienvenido al instalador de Prism {#MyAppVersion}
spanish.WelcomeLabel2=Este asistente instalara el lenguaje de programacion Prism (compilador y runtime prc) en tu equipo.%n%nInspirado en la descomposicion espectral de Newton, Prism transforma enunciados y formulas logicas en bytecode JVM ejecutable.

[Tasks]
Name: "addtopath"; Description: "Agregar Prism (prc) al PATH del sistema (Recomendado)"; GroupDescription: "Opciones del sistema:"
Name: "assocpr"; Description: "Asociar archivos .pr con Prism en el Explorador de Windows"; GroupDescription: "Integracion con Windows:"
Name: "examples"; Description: "Instalar ejemplos oficiales (.pr) y guia rapida"; GroupDescription: "Documentacion y ejemplos:"

[Files]
Source: "..\..\prism.jar"; DestDir: "{app}\lib"; Flags: ignoreversion
Source: "..\..\bin\prc.cmd"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\LICENSE"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\..\README.md"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\..\LEEME_INSTALACION.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "prism.ico"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\..\examples\*.pr"; DestDir: "{app}\examples"; Flags: ignoreversion; Tasks: examples
#if HasRuntime
Source: "..\..\build\win-runtime\*"; DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs createallsubdirs
#endif

[Icons]
Name: "{group}\Abrir terminal de Prism"; Filename: "cmd.exe"; Parameters: "/k ""set PATH={app}\bin;%PATH%& cd /d ""%USERPROFILE%"" & echo Prism {#MyAppVersion} listo. & ""{app}\bin\prc.cmd"" --version"""; WorkingDir: "{app}"; IconFilename: "{app}\prism.ico"
Name: "{group}\Ejemplos de Prism"; Filename: "{app}\examples"; Tasks: examples
Name: "{group}\Desinstalar Prism"; Filename: "{uninstallexe}"

[Registry]
; Asociacion de archivos .pr
Root: HKA; Subkey: "Software\Classes\.pr"; ValueType: string; ValueName: ""; ValueData: "Prism.SourceFile"; Flags: uninsdeletevalue; Tasks: assocpr
Root: HKA; Subkey: "Software\Classes\Prism.SourceFile"; ValueType: string; ValueName: ""; ValueData: "Codigo fuente Prism (.pr)"; Flags: uninsdeletekey; Tasks: assocpr
Root: HKA; Subkey: "Software\Classes\Prism.SourceFile\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\prism.ico,0"; Tasks: assocpr

[Run]
Filename: "cmd.exe"; Parameters: "/c """"{app}\bin\prc.cmd"" --version & echo. & echo Instalacion verificada. Presiona una tecla para cerrar... & pause >nul"""; Description: "Verificar la instalacion de prc ahora"; Flags: postinstall skipifsilent unchecked

[Code]
function GetEnvRootKey: Integer;
begin
  if IsAdminInstallMode then
    Result := HKLM
  else
    Result := HKCU;
end;

function GetEnvSubkey: String;
begin
  if IsAdminInstallMode then
    Result := 'SYSTEM\CurrentControlSet\Control\Session Manager\Environment'
  else
    Result := 'Environment';
end;

procedure EnvAddPath(PathToAdd: String);
var
  Paths: String;
  CheckPaths: String;
  CheckTarget: String;
begin
  if not RegQueryStringValue(GetEnvRootKey(), GetEnvSubkey(), 'Path', Paths) then
    Paths := '';

  CheckPaths := ';' + Uppercase(Paths) + ';';
  CheckTarget := ';' + Uppercase(PathToAdd) + ';';
  if Pos(CheckTarget, CheckPaths) > 0 then
    Exit;

  if (Length(Paths) > 0) and (Paths[Length(Paths)] <> ';') then
    Paths := Paths + ';';
  Paths := Paths + PathToAdd;

  RegWriteExpandStringValue(GetEnvRootKey(), GetEnvSubkey(), 'Path', Paths);
end;

procedure EnvRemovePath(PathToRemove: String);
var
  Paths: String;
  P: Integer;
  TargetUpper: String;
begin
  if not RegQueryStringValue(GetEnvRootKey(), GetEnvSubkey(), 'Path', Paths) then
    Exit;

  TargetUpper := Uppercase(PathToRemove);
  P := Pos(';' + TargetUpper + ';', ';' + Uppercase(Paths) + ';');
  if P = 0 then
    Exit;

  StringChangeEx(Paths, PathToRemove + ';', '', True);
  StringChangeEx(Paths, ';' + PathToRemove, '', True);
  if Uppercase(Paths) = TargetUpper then
    Paths := '';

  RegWriteExpandStringValue(GetEnvRootKey(), GetEnvSubkey(), 'Path', Paths);
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
  begin
    if WizardIsTaskSelected('addtopath') then
      EnvAddPath(ExpandConstant('{app}\bin'));
  end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usPostUninstall then
    EnvRemovePath(ExpandConstant('{app}\bin'));
end;
