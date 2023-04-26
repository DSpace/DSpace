call ..\..\envs\__basic.bat

set tomcat_bin=%tomcat%\bin\

cd %tomcat_bin%\
call catalina stop -force

cd %dspace_source%\scripts\fast-build\tomcat\
