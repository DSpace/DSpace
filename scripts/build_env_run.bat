rem set those to your local paths
set dspace_source=C:\dspace-be
set tomcat=C:\workspace\apps\apache-tomcat-9.0.53
set dspace_application=C:\dspace

set dspace_installer=%dspace_source%\dspace\target\dspace-installer\
set tomcat_webapps=%tomcat%\webapps\
set server=%tomcat_webapps%\server
set dspace_webapps=%dspace_application%\webapps\
set tomcat_bin=%tomcat%\bin\
call build_commands.bat