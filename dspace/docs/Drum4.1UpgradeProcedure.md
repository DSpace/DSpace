# Drum 4.1 Upgrade Procedure

* Switch to source directory on the drum server

       ```
cd /apps/drum/src
```
* Pull the latest changes from github

       ```
git fetch
git pull <branch_name>
```
* Manually update {dev,stage,prod}.properties, if necessary.

       ```
vim prod.properties
```
* Build the project using maven

       ```
mvn clean install -Denv=prod -Dlicense.skip=true
```
* Stop the tomcat server

       ```
cd /apps/drum/tomcat
./control stop
```
* Deploy the build using ANT

       ```
cd /apps/drum/src/dspace/target/dspace-<VERSION>-build/
ant update
```
* Start the tomcat server

       ```
cd /apps/drum/tomcat
./control start
```

### Solr Upgrade Procedure
If the upgrade includes changes to solr configuration, upgrade the solr configuration in corresponding (dev,stage,production) solr server.

* Switch to the solr cores directory in th server

       ```
cd /apps/solr/cores
```
* Copy the updated configuration from the drum server.

       ```
scp -r <USERNAME>@drum.lib.umd.edu:/apps/drum/solr/search/conf/* drum-search/conf/
scp -r <USERNAME>@drum.lib.umd.edu:/apps/drum/solr/statistics/conf/* drum-statistics/conf/
scp -r <USERNAME>@drum.lib.umd.edu:/apps/drum/solr/oai/conf/* drum-oai/conf/
```
* Restart Solr OR Reload the drum solr cores.

       ```
cd /apps/solr
./control restart
```


