call ..\envs\__basic.bat

rem stopping the tomcat
call tomcat\stop.bat

rem rebuild the oai package (dspace-api)
cd %dspace_source%\dspace-api\
call mvn clean package

rem copy created jar into tomcat/webapps/server
xcopy /e /h /i /q /y %dspace_source%\dspace-api\target\dspace-api-7.2.1.jar %tomcat%\webapps\server\WEB-INF\lib\

cd %dspace_source%\scripts\fast-build\