#!/bin/bash

cd /opt
sudo wget -P /opt  https://downloads.apache.org/lucene/solr/8.11.3/solr-8.11.3.zip

sudo unzip /opt/solr-8.11.3.zip
sudo rm /opt/solr-8.11.3.zip
sudo chown -R $USER:$USER /opt/solr-8.11.3

sudo mkdir /dspace
sudo chown -R $USER:$USER /dspace


cd

echo "/opt/solr-8.11.3/bin/solr start" | sudo tee -a .profile
/opt/solr-8.11.3/bin/solr start
