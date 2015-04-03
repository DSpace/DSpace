#!/bin/bash
#Put start commands here
### Postgres ###
echo "Starting postgres"
/etc/init.d/postgresql-9.4 start
### Tomcat ###
echo "Starting tomcat"
/etc/init.d/tomcat8 start
### Handle server ###
HANDLE_SERVER=/etc/init.d/handle-server
if [[ -r $HANDLE_SERVER ]]; then
    echo "Starting handle server";
    $HANDLE_SERVER start;
else
    echo "Handle server not present - ignoring start command";
fi
### nginx ###
#echo "Starting nginx"
#/etc/init.d/nginx start
### supervisor (shibboleth + fastcgi) ###
#echo "Starting all supervised programs"
#supervisorctl start all
