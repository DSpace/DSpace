@REM ###########################################################################
@REM # The contents of this file are subject to the license and copyright
@REM # detailed in the LICENSE and NOTICE files at the root of the source
@REM # tree and available online at
@REM # 
@REM # http://www.dspace.org/license/
@REM ###########################################################################
@REM # 'buildpath.bat' script
@REM # A simple Windows batch script to facilitate building a CLASSPATH dynamically
@REM # in 'dspace.bat' (and similar Windows batch scripts)

@echo off

set DSPACE_CLASSPATH=%DSPACE_CLASSPATH%;%~s1
