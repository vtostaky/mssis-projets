@echo off

call .\setenv.bat

rem opencard.core.*
set CLASSES=%CLASSES%;%OCF_HOME%\lib\base-core.jar

rem opencard.opt.util
set CLASSES=%CLASSES%;%OCF_HOME%\lib\base-opt.jar

rem bouncy castle (crypto provider)
set CLASSES=%CLASSES%;%MISC%\bcprov-jdk15on-150.jar

IF NOT EXIST %OUT%\%PROJECT% MD %OUT%\%PROJECT% 

echo Compilation...
%JAVA_HOME%\bin\javac.exe -classpath %CLASSES% -g -d %OUT%\%PROJECT% %SRC%\%PROJECT%\%PKGSERVER%\%SERVICE%.java
if errorlevel 1 goto error
echo %SERVICE%.class compiled: OK
echo .

set CLASSES=%CLASSES%;%OUT%\%PROJECT%\

echo Compilation...
%JAVA_HOME%\bin\javac.exe -classpath %CLASSES% -g -d %OUT%\%PROJECT% %SRC%\%PROJECT%\%PKGSERVER%\%SERVER%.java
if errorlevel 1 goto error
echo %SERVER%.class compiled: OK
echo .

goto end

:error
echo ***************
echo    ERROR !
echo ***************
pause
goto end

:end
cls
