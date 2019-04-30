@echo off

set LIBDIR=%~dp0\lib

java -jar "%LIBDIR%\contestUtil.jar" %1 %2 %3 %4 %5
