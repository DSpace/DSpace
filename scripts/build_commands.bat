cd %dspace_source%
call mvn clean package
cd %dspace_installer% || echo 'failure'
call ant fresh_install 
rd /s /q %server%
xcopy /e /h /i /q %dspace_webapps% %tomcat_webapps% 
cd %tomcat_bin% || echo 'failure'