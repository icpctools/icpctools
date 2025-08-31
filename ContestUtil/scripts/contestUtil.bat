@echo off

set LIBDIR=%~dp0\lib

java -cp "%LIBDIR%\contestUtil.jar" org.icpc.tools.contest.SWTLauncher org.icpc.tools.contest.util.Launcher %1 %2 %3 %4 %5
