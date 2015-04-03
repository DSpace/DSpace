#!/bin/bash
#Put stop commands and cleanup here
### Postgres ###
echo "Stopping postgres"
/etc/init.d/postgresql-9.4 stop
### Tomcat ###
echo "Stopping tomcat"
/etc/init.d/tomcat8 stop
### Handle server ###
HANDLE_SERVER=/etc/init.d/handle-server
if [[ -r $HANDLE_SERVER ]]; then
    echo "Stopping handle server";
    $HANDLE_SERVER stop;
else
    echo "Handle server not present - ignoring stop command";
fi
### nginx ###
#echo "Stopping nginx"
#/etc/init.d/nginx stop
### supervisor (shibboleth + fastcgi) ###
#echo "Stopping all supervised programs"
#supervisorctl stop all
