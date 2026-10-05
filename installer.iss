[Setup]
AppName=UTAR WiFi Helper
AppVersion=1.0.2
DefaultDirName={localappdata}\Programs\UTAR WiFi Helper
DefaultGroupName=UTAR WiFi Helper
OutputBaseFilename=utarwifihelper-installer
Compression=lzma2
SolidCompression=yes
PrivilegesRequired=lowest
SetupIconFile=logo.ico
UninstallDisplayIcon={app}\utarwifihelper.exe
OutputDir=.

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "utarwifihelper.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "utarwifihelper.exe.manifest"; DestDir: "{app}"; Flags: ignoreversion
Source: "logo.ico"; DestDir: "{app}"; Flags: ignoreversion
Source: "README.md"; DestDir: "{app}"; Flags: ignoreversion
Source: "packed_release\LICENSE.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "packed_release\README.txt"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\UTAR WiFi Helper"; Filename: "{app}\utarwifihelper.exe"
Name: "{group}\Uninstall UTAR WiFi Helper"; Filename: "{uninstallexe}"
Name: "{autodesktop}\UTAR WiFi Helper"; Filename: "{app}\utarwifihelper.exe"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop icon"; GroupDescription: "Additional icons:"; Flags: unchecked

[Run]
Filename: "{app}\utarwifihelper.exe"; Description: "Launch UTAR WiFi Helper"; Flags: nowait postinstall skipifsilent
