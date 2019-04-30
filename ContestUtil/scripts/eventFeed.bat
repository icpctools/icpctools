@echo off

set LIBDIR=%~dp0\lib

java -cp "%LIBDIR%\contestUtil.jar" org.icpc.tools.contest.util.EventFeedUtil %1 %2 %3 %4

