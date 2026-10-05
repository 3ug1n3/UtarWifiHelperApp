@echo off
setlocal
:: Find vcvarsall.bat location
set "VS_PATH="
if exist "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" set "VS_PATH=C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat"
if exist "C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvarsall.bat" set "VS_PATH=C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvarsall.bat"
:: Fallback for VS 2026 based on user log
if exist "C:\Program Files\Microsoft Visual Studio\2026\Community\VC\Auxiliary\Build\vcvarsall.bat" set "VS_PATH=C:\Program Files\Microsoft Visual Studio\2026\Community\VC\Auxiliary\Build\vcvarsall.bat"

if "%VS_PATH%"=="" (
    echo [ERROR] Could not find vcvarsall.bat. Please compile manually or update the script path.
    exit /b 1
)

echo Loading Environment from %VS_PATH%...
call "%VS_PATH%" x64

echo Compiling utarwifihelper.cpp...
cl /EHsc /W3 /O2 utarwifihelper.cpp /Fe:utarwifihelper.exe user32.lib gdi32.lib wininet.lib shlwapi.lib gdiplus.lib shell32.lib ole32.lib oleaut32.lib wlanapi.lib advapi32.lib /link /SUBSYSTEM:WINDOWS /ENTRY:WinMainCRTStartup

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] utarwifihelper.exe has been updated.
) else (
    echo [FAILURE] Compilation failed.
)
endlocal
