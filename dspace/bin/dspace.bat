@REM ###########################################################################
@REM # The contents of this file are subject to the license and copyright
@REM # detailed in the LICENSE and NOTICE files at the root of the source
@REM # tree and available online at
@REM # 
@REM # http://www.dspace.org/license/
@REM ###########################################################################
@REM # 'dspace.bat' script
@REM # This is a Windows batch script for running a command-line DSpace tool.
@REM # It sets the CLASSPATH appropriately before invoking Java.
@echo off

set CURRENT_DIR=%cd%

REM Guess DSpace directory: CD to directory script is in; CD to parent
chdir /D "%~p0"
chdir ..

REM Check we can find dspace.cfg.  Quit with an error if not.
if exist "config\dspace.cfg" goto okExec
echo Cannot find %cd%\config\dspace.cfg
goto end

:okExec
echo Using DSpace installation in: %cd%

REM Build a CLASSPATH including all classes in oai webapp, all libraries in [dspace]/lib and the config folder.
set DSPACE_CLASSPATH=%CLASSPATH%;config;webapps\oai\WEB-INF\classes\
for %%f in (lib\*.jar) DO CALL bin\buildpath.bat %%f

REM If the user only wants the CLASSPATH, just give it now.
if not "%1"=="classpath" goto javaOpts
echo %DSPACE_CLASSPATH%
goto end

:javaOpts
REM If JAVA_OPTS specified, use those options
REM Otherwise, default Java to using 256MB of memory
if "%JAVA_OPTS%"=="" set "JAVA_OPTS=-Xmx256m -Dfile.encoding=UTF-8"

REM Execute Java
java %JAVA_OPTS% -classpath "%DSPACE_CLASSPATH%" org.dspace.app.launcher.ScriptLauncher %*

REM Clean up DSPACE_CLASSPATH variable
set DSPACE_CLASSPATH=

:end

REM Back to original dir
chdir /D %CURRENT_DIR%