#!/bin/sh

sudo pg_ctlcluster 14 main start
echo "Postgres started"
echo "Change password for user postgres"
sudo passwd postgres

echo "Validando UTF-8"

sudo su postgres -c "cd /etc/postgresql/14/main && psql -c \"SHOW SERVER_encoding\""

sudo sed -i 's/\#listen/listen/g' /etc/postgresql/14/main/postgresql.conf 
sudo sed -i "/Database administrative/a host\t$USER\t$USER\t127.0.0.1\t255.255.255.255\tmd5" /etc/postgresql/14/main/pg_hba.conf
# sudo sed -i '/Database administrative/a host\tdspace\tdspace\t127.0.0.1\t255.255.255.255\tmd5' /etc/postgresql/14/main/pg_hba.conf


sudo systemctl restart postgresql
sudo systemctl enable postgresql
sudo systemctl start postgresql
