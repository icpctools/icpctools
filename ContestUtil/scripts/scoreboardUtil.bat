@echo off

set LIBDIR=%~dp0\lib

java -cp "%LIBDIR%\*" org.icpc.tools.contest.util.ScoreboardUtil %1 %2 %3 %4
