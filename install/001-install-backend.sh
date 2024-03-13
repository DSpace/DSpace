#!/bin/bash

# Install basic programs
sudo apt update && sudo apt upgrade -y 
sudo apt install git -y
sudo apt install default-jdk  -y
sudo apt install maven -y
sudo apt install ant -y
sudo apt install postgresql postgresql-client postgresql-contrib -y
sudo apt install unzip -y
sudo apt install tomcat9 -y


# Config Java
sudo touch /etc/environment 
echo "JAVA_HOME=\"/usr/lib/jvm/java-11-openjdk-amd64\"" | sudo tee -a /etc/environment
echo "JAVA_OPTS=\"-Xmx512M -Xms64M -Dfile.encoding=UTF-8\"" | sudo tee -a /etc/environment
source /etc/environment


# Report status
java -version
echo $JAVA_HOME
echo $JAVA_OPTS
mvn -v
ant -version
git --version
psql -V psql