@echo off
rem
rem Purpose: display presentations in standalone mode
rem

set LIBDIR=%~dp0\lib

set params=

:loop
if %1. == . goto :continue
set params=%params% %1
shift
goto :loop

:continue

java -Xmx4096m -cp "%LIBDIR%\*" org.icpc.tools.presentation.contest.internal.standalone.StandaloneLauncher %params% 
