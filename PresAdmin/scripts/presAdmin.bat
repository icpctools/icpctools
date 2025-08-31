@echo off
rem
rem Purpose: run a Presentation Admin
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

:restart

java -cp "%LIBDIR%\swtLauncher.jar" org.icpc.tools.contest.SWTLauncher org.icpc.tools.presentation.admin.internal.Admin %params%

if errorlevel 255 goto :restart
if errorlevel 254 goto :update
