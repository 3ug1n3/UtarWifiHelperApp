@echo off
setlocal
echo Loading Visual Studio Environment...
if exist "C:\Program Files\Microsoft Visual Studio\2026\Community\VC\Auxiliary\Build\vcvarsall.bat" (
    call "C:\Program Files\Microsoft Visual Studio\2026\Community\VC\Auxiliary\Build\vcvarsall.bat" x64
) else if exist "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" (
    call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" x64
) else (
    :: Try finding any vcvarsall.bat
    for /f "usebackq tokens=*" %%i in (`where /r "C:\Program Files" vcvarsall.bat 2^>nul`) do (
        call "%%i" x64
        goto :found
    )
)
:found
echo Building utarwifihelper.exe...
cl /EHsc /O2 utarwifihelper.cpp /Fe:utarwifihelper.exe user32.lib gdi32.lib wininet.lib shlwapi.lib gdiplus.lib shell32.lib ole32.lib oleaut32.lib wlanapi.lib advapi32.lib /link /SUBSYSTEM:WINDOWS /ENTRY:WinMainCRTStartup

if %ERRORLEVEL% EQU 0 (
    echo [OK] Build Successful!
) else (
    echo [FAIL] Build Failed!
)
endlocal
