@REM
@REM dsrun.bat
@REM
@REM Version: $Revision: 3705 $
@REM
@REM Date: $Date: 2009-04-11 19:02:24 +0200 (Sat, 11 Apr 2009) $
@REM
@REM Copyright (c) 2005, Hewlett-Packard Company and Massachusetts
@REM Institute of Technology.  All rights reserved.
@REM
@REM Redistribution and use in source and binary forms, with or without
@REM modification, are permitted provided that the following conditions are
@REM met:
@REM
@REM - Redistributions of source code must retain the above copyright
@REM notice, this list of conditions and the following disclaimer.
@REM
@REM - Redistributions in binary form must reproduce the above copyright
@REM notice, this list of conditions and the following disclaimer in the
@REM documentation and/or other materials provided with the distribution.
@REM
@REM - Neither the name of the Hewlett-Packard Company nor the name of the
@REM Massachusetts Institute of Technology nor the names of their
@REM contributors may be used to endorse or promote products derived from
@REM this software without specific prior written permission.
@REM
@REM THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
@REM ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
@REM LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
@REM A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
@REM HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
@REM INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
@REM BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
@REM OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
@REM ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
@REM TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
@REM USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
@REM DAMAGE.
@REM

@echo off

REM This is a simple shell script for running a command-line DSpace tool.
REM sets the CLASSPATH appropriately before invoking Java.

REM Remember startup dir

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

REM Build a CLASSPATH

set DSPACE_CLASSPATH=%CLASSPATH%;config
for %%f in (lib\*.jar) DO CALL bin\buildpath.bat %%f

REM If JAVA_OPTS specified, use those options
REM Otherwise, default Java to using 256MB of memory

if "%JAVA_OPTS%"=="" set JAVA_OPTS=-Xmx256m

REM Execute Java

java %JAVA_OPTS% -classpath "%DSPACE_CLASSPATH%" %*


REM Clean up DSPACE_CLASSPATH variable

set DSPACE_CLASSPATH=


:end

REM Back to original dir

chdir /D %CURRENT_DIR%
