#!/bin/sh
wget https://github.com/kermitt2/grobid/archive/0.5.5.zip -P ../
unzip ../0.5.5.zip -d ../ 1>/dev/null
cd ../grobid-0.5.5/
./gradlew clean install
nohup ./gradlew run &
cd -
