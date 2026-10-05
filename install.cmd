@echo off
setlocal
set APPDIR=%~dp0
mkdir "%LOCALAPPDATA%\Programs\UTAR WiFi Helper" 2>nul
copy "%APPDIR%utarwifihelper.exe" "%LOCALAPPDATA%\Programs\UTAR WiFi Helper\" >nul
copy "%APPDIR%utarwifihelper.exe.manifest" "%LOCALAPPDATA%\Programs\UTAR WiFi Helper\" >nul
copy "%APPDIR%logo.ico" "%LOCALAPPDATA%\Programs\UTAR WiFi Helper\" >nul
copy "%APPDIR%README.md" "%LOCALAPPDATA%\Programs\UTAR WiFi Helper\" >nul
mklink "%USERPROFILE%\Desktop\UTAR WiFi Helper.lnk" "%LOCALAPPDATA%\Programs\UTAR WiFi Helper\utarwifihelper.exe" >nul 2>&1
echo Installation complete.
echo Program installed to %LOCALAPPDATA%\Programs\UTAR WiFi Helper
pause
