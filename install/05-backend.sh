#!/bin/bash

git clone git clone https://github.com/LACCEI/DSpace-Backend.git ~/DSpace-Backend
sudo chown -R $USER:$USER ~/DSpace-Backend
sudo su postgres -c "cd /etc/postgresql/14/main && createuser --username=postgres --no-superuser --pwprompt dspace"
sudo su postgres -c "cd /etc/postgresql/14/main && createdb --username=postgres --owner=dspace --encoding=UNICODE dspace"
sudo su postgres -c "cd /etc/postgresql/14/main && psql --username=postgres dspace -c \"CREATE EXTENSION pgcrypto;\""
cp ~/DSpace-Backend/dspace/config/local.cfg.EXAMPLE ~/DSpace-Backend/dspace/config/local.cfg
sudo sed -i 's/dspace\.name\ \=\ DSpace\ at\ My University/dspace\.name\ \=\ LACCEI/g'  ~/DSpace-Backend/dspace/config/local.cfg
sudo sed -i 's/\#solr\.server/solr\.server/g'  ~/DSpace-Backend/dspace/config/local.cfg
sudo sed -i 's/db\.username\ \=\ dspace/db\.username\ \=\ dspace/g'  ~/DSpace-Backend/dspace/config/local.cfg
sudo sed -i 's/db\.password\ \=\ dspace/db\.password\ \=\ Ad46b36ff/g'  ~/DSpace-Backend/dspace/config/local.cfg
cd ~/DSpace-Backend && mvn package
cd ~/DSpace-Backend/dspace/target/dspace-installer && sudo ant fresh_install
cd /dspace/bin && ./dspace database migrate
sudo cp -R /dspace/webapps/* /var/lib/tomcat9/webapps*
cp -R /dspace/solr/* /opt/solr-8.11.3/server/solr/configsets
cd /opt/solr-8.11.3/bin && ./solr restart

/dspace/bin/dspace create-administrator
sudo chown -R tomcat:tomcat /dspace
sudo systemctl restart tomcat9.service