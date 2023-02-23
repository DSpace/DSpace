#!/bin/bash
cd /usr/local/tomcat/bin
rm catalina.sh
cp catalina_debug.sh catalina.sh
./shutdown.sh