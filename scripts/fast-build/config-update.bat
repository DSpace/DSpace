call ..\envs\__basic.bat

call tomcat\stop.bat

rem copy all config files
xcopy /e /h /i /q /y %dspace_source%\dspace\config\ %dspace_application%\config\

cd %dspace_source%\scripts\fast-build\
