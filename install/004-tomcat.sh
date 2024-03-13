#!/bin/bash

sudo sed -i "/Security/a ReadWritePaths\=\/dspace" /lib/systemd/system/tomcat9.service
sudo sed -i '/\<Connector port=\"8080\" protocol=\"HTTP\/1.1\"/a minSpareThreads="25"\n\tenableLookups=\"false\"\n\tdisableUploadTimeout=\"true\"\n\tURIEncoding=\"UTF-8\"' /etc/tomcat9/server.xml
sudo systemctl daemon-reload
sudo systemctl restart tomcat9.service
sudo systemctl enable tomcat9.service
sudo systemctl start tomcat9.service
