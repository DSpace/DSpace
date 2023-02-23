#!/bin/bash
cd /usr/local/tomcat/bin
rm catalina.sh
cp catalina_orig.sh catalina.sh
./shutdown.sh