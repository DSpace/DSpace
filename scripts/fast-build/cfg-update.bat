call ..\envs\__basic.bat

call tomcat\stop.bat

rem copy specific config files
xcopy /e /h /i /q /y %dspace_source%\dspace\config\clarin-dspace.cfg %dspace_application%\config\
xcopy /e /h /i /q /y %dspace_source%\dspace\config\dspace.cfg %dspace_application%\config\

cd %dspace_source%\scripts\fast-build\
