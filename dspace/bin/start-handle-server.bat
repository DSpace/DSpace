@REM ###########################################################################
@REM # The contents of this file are subject to the license and copyright
@REM # detailed in the LICENSE and NOTICE files at the root of the source
@REM # tree and available online at
@REM # 
@REM # http://www.dspace.org/license/
@REM ###########################################################################
@REM # 'start-handle-server.bat' script
@REM # Windows bash script for starting Handle server.  WARNING this assumes any
@REM # previously running Handle servers have been terminated.
@echo off

set CURRENT_DIR=%cd%

REM Determine DSpace 'bin' directory. CD to directory script is in
chdir /D "%~p0"
set BINDIR=%cd%

REM Read 'dspace.dir' parameter from DSpace config (using dspace.bat script)
REM (Note: see http://stackoverflow.com/a/12069559/3750035 for this 'for' syntax)
set DSPACEDIR=
for /f "delims=" %%a in ('%BINDIR%\dspace dsprop --property dspace.dir') do @set DSPACEDIR=%%a
echo Using DSpace installation in: %DSPACEDIR%

REM Read 'handle.dir' parameter from DSpace config (using dspace.bat script)
set HANDLEDIR=
for /f "delims=" %%a in ('%BINDIR%\dspace dsprop --property handle.dir') do @set HANDLEDIR=%%a
echo Using Handle server directory: %HANDLEDIR%

REM Assume log directory is a subdirectory of DSPACEDIR.
REM If you want your handle server logs stored elsewhere, change this value
set LOGDIR=%DSPACEDIR%/log

REM Build a CLASSPATH including all classes in oai webapp, all libraries in [dspace]/lib and the config folder.
set DSPACE_CLASSPATH=%CLASSPATH%;%DSPACEDIR%\config;%DSPACEDIR%\webapps\oai\WEB-INF\classes\
for %%f in (%DSPACEDIR%\lib\*.jar) DO CALL %BINDIR%\buildpath.bat %%f

REM If JAVA_OPTS specified, use those options
REM Otherwise, default Java to using 256MB of memory
if "%JAVA_OPTS%"=="" set "JAVA_OPTS=-Xmx256m -Dfile.encoding=UTF-8"

REM Remove (forcibly) lock file, in case the old Handle server did not shut down properly
if exist "%HANDLEDIR%\txns\lock" del /F "%HANDLEDIR%\txns\lock"

REM Execute Java
REM Start the Handle server, with a special log4j properties file.
REM We cannot simply write to the same logs, since log4j
REM does not support more than one JVM writing to the same rolling log.
echo.
echo Starting Handle Server (check logs for details)...
echo.
echo NOTE: If you want to run the Handle Server as a backend process, re-execute this script
echo using the Windows "start" command. For example, "start /B start-handle-server.bat"
echo.
java %JAVA_OPTS% -cp "%DSPACE_CLASSPATH%" -Ddspace.log.init.disable=true -Dlog4j.configuration=log4j-handle-plugin.properties net.handle.server.Main %HANDLEDIR%  >> "%LOGDIR%/handle-server.log"

REM Clean up DSPACE_CLASSPATH variable
set DSPACE_CLASSPATH=

REM Back to original dir
chdir /D %CURRENT_DIR%