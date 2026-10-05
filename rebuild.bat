@echo off
echo Initializing Visual Studio Environment...
call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" x64

echo Compiling UTAR WiFi Helper...
cl /EHsc /W3 /O2 utarwifihelper.cpp /Fe:utarwifihelper.exe user32.lib gdi32.lib wininet.lib shlwapi.lib gdiplus.lib shell32.lib ole32.lib oleaut32.lib wlanapi.lib advapi32.lib /link /SUBSYSTEM:WINDOWS /ENTRY:WinMainCRTStartup

if %ERRORLEVEL% EQU 0 (
    echo [OK] Rebuild Successful!
) else (
    echo [FAIL] Rebuild Failed with error %ERRORLEVEL%
)
pause
