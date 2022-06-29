rem set those to your local paths in .gitignored file paths.bat.
rem you HAVE TO CREATE paths.bat with following variables in the same directory

rem start of variables expected in paths.bat
set dspace_source=
set tomcat=
set dspace_application=
rem end of variables expected in paths.bat

call paths.bat

set dspace_installer=%dspace_source%\dspace\target\dspace-installer\
set tomcat_webapps=%tomcat%\webapps\
set server=%tomcat_webapps%\server
set dspace_webapps=%dspace_application%\webapps\
set tomcat_bin=%tomcat%\bin\
call build_commands.bat
