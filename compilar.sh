#!/bin/bash
mvn package -Dmirage2.on=true
sudo rm -R /opt/dspace/*
sudo chown -R dspace:dspace /home/dspace/dspace-src-git/
cd /home/dspace/dspace-src-git/dspace/target/dspace-installer
ant fresh_install
cp -r /opt/dspace/webapps* /opt/apache-tomcat-9.0.22/webapps*
/opt/apache-tomcat-9.0.22/bin/shutdown.sh
/opt/apache-tomcat-9.0.22/bin/startup.sh
