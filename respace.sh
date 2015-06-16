#!/bin/bash

set -e

echo "Begin DSpace recompile+redeploy"
cd ~/Projects/DSpace

##Ensure git is up to date?
git pull

mvn package -DskipTests=true

cd dspace/target/dspace-installer/
ant update

catalina stop -force
catalina start

echo "DSpace redeployed..."

##OSX 10.9/Mavericks Notification Center
osascript -e 'display notification "Code recompile + redeploy complete, back to work!" with title "DSpace"'