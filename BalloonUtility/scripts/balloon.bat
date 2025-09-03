@echo off
rem
rem Purpose: run the balloon utility
rem

set LIBDIR=%~dp0\lib
set ROOTDIR=%~dp0

cd %ROOTDIR%
set params=

:loop
if %1. == . goto :restart
set params=%params% %1
shift
goto :loop

:update
echo "Update downloaded, applying"
rmdir "%LIBDIR%" /s /q
robocopy "%ROOTDIR%\update" "%ROOTDIR%\" /e /move
goto :restart

:cache
echo "Clearing cache"
powershell.exe -Command "& {rm -force -r $env:TEMP/org.icpc.*}"
goto :restart

:restart

java -cp "%LIBDIR%\swtLauncher.jar" org.icpc.tools.contest.SWTLauncher org.icpc.tools.balloon.BalloonUtility %params%

if errorlevel 255 goto :restart
if errorlevel 254 goto :update
if errorlevel 253 goto :cache