call ..\envs\__basic.bat

rem stopping the tomcat
call tomcat\stop.bat

rem rebuild the oai package (dspace-oai)
cd %dspace_source%\dspace-oai\
call mvn clean package

rem copy created jar into tomcat/webapps/server
xcopy /e /h /i /q /y %dspace_source%\dspace-oai\target\dspace-oai-7.2.1.jar %tomcat%\webapps\server\WEB-INF\lib\

rem reindex oai-pmh indexes to show delete cache (sometimes the changes cannot be seen)
cd %dspace_application%\bin
call dspace oai import -c

cd %dspace_source%\scripts\fast-build\