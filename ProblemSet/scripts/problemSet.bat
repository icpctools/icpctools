@echo off
rem
rem Purpose: to edit problemset.yaml
rem

set LIBDIR=%~dp0\lib

set params=

:loop
if %1. == . goto :continue
set params=%params% %1
shift
goto :loop

:continue

java -cp "%LIBDIR%\swtLauncher.jar" org.icpc.tools.contest.SWTLauncher org.icpc.tools.contest.util.problemset.ProblemSetEditor %params% 
