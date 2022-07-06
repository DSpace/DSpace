rem set those to your local paths in .gitignored file /envs/__basic.bat.
rem you HAVE TO CREATE /envs/__basic.bat with following variables in the same directory

rem start of variables expected in /envs/__basic.bat
set dspace_source=
set tomcat=
set dspace_application=
rem end of variables expected in /envs/__basic.bat

call envs\__basic.bat

set dspace_installer=%dspace_source%\dspace\target\dspace-installer\
set tomcat_webapps=%tomcat%\webapps\
set server=%tomcat_webapps%\server
set dspace_webapps=%dspace_application%\webapps\
set tomcat_bin=%tomcat%\bin\
call build.dspace.bat
