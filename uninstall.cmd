@echo off
setlocal
set APPDIR=%LOCALAPPDATA%\Programs\UTAR WiFi Helper
if exist "%APPDIR%" rmdir /s /q "%APPDIR%"
del "%USERPROFILE%\Desktop\UTAR WiFi Helper.lnk" 2>nul
echo Uninstall complete.
pause
