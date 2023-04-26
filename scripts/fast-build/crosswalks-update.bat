call ..\envs\__basic.bat

call tomcat\stop.bat

xcopy /e /h /i /q /y %dspace_source%\dspace\config\crosswalks\oai\metadataFormats\ %dspace_application%\config\crosswalks\oai\metadataFormats\

rem reindex oai-pmh indexes to show delete cache (sometimes the changes cannot be seen)
cd %dspace_application%\bin
call dspace oai import -c

cd %dspace_source%\scripts\fast-build\
