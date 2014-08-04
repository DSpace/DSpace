#DSpace 4.1 Installation 

**[Prerequisite Software](#prerequisite-software)**

**[DSpace Instance Installation](#dspace-installation)**

**[Validation procedure](#validate-installation)**


##<a name="prerequisite-software"></a>Prerequisite Software

The third-party components and tools to run a DSpace 4.1 server.

### Oracle Java JDK 7


Oracle's Java can be downloaded from the following location: [Java Downloads](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

Please, follow the download instructions and set your JAVA_HOME enviroment variable to a Java 7 JDK.

If you are using Mac OS you might want to check the installed Java machines at the following location:

/Library/Java/JavaVirtualMachines.

If you have Java JDK listed you may skip the installation step and simply to setup the JAVA_HOME according to the example below:

```
$ cd ~ 
$ vi .profile
$ export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.7.0_40.jdk/Contents/Home
$ java -version

java version "1.7.0_40"
Java(TM) SE Runtime Environment (build 1.7.0_40-b43)
Java HotSpot(TM) 64-Bit Server VM (build 24.0-b56, mixed mode)

```

### Apache Maven 3.x (Java build tool)

Maven is necessary in the first stage of the build process to assemble the installation package for the DSpace instance.

Maven can be downloaded from the following location: [Maven Downloads](http://maven.apache.org/download.html).

Check your current maven version:

```
$ mvn --version

Apache Maven 3.0.3 (r1075438; 2011-02-28 12:31:09-0500)
Maven home: /usr/share/maven
Java version: 1.7.0_40, vendor: Oracle Corporation
Java home: /Library/Java/JavaVirtualMachines/jdk1.7.0_40.jdk/Contents/Home/jre
Default locale: en_US, platform encoding: UTF-8
OS name: "mac os x", version: "10.8.2", arch: "x86_64", family: "mac"

```

Set the Maven environment variable to the Maven 3.

```
$ vi .profile
$ export M2_HOME=/usr/share/maven
$ export M2=$M2_HOME/bin
$ export PATH=$M2:$PATH

```

### Apache Ant 1.8 or later (Java build tool)

Apache Ant is required for the second stage of the build process.

Ant can be downloaded from the following location: http://ant.apache.org.

Please, unpack all build tools into /apps/tools directory

```
$ mkdir /apps/tools

```
####Create directory for ant download its archive.

```
$ cd /apps/tools
$ mkdir ant
$ cd ant
$ curl -O http://apache.osuosl.org/ant/binaries/apache-ant-1.9.3-bin.tar.gz
$ tar -xzvf apache-ant-1.9.3-bin.tar.gz
$ rm apache-ant-1.9.3-bin.tar.gz
```

####Add the Ant PATH variables to the environment file.

```
$ cd ~ 
$ vi .profile
$ export PATH=/apps/tools/ant/apache-ant-1.9.3/bin:"$PATH"
```

####Testing the Ant Installation


```
$ cd ~ 
$ vi .profile
$ export PATH=/apps/tools/ant/apache-ant-1.9.3/bin:"$PATH"
$ source ~/.profile
$ ant --version

It should return the installed ant version.
Apache Ant(TM) version 1.8.2 compiled on June 16 2012

```

### Tomcat 7

* Create installation directory

```
$ mkdir /apps/servers/dspace/
$ cd /apps/servers/dspace/
$ curl -O http://archive.apache.org/dist/tomcat/tomcat-7/v7.0.52/bin/apache-tomcat-7.0.52.tar.gz
$ tar -xzvf apache-tomcat-7.0.52.tar.gz
$ ls
$ mv apache-tomcat-7.0.52 tomcat41
$ ls

```

* Create control script

```
$ cd /apps/servers/dspace/tomcat41
$ vi control

```

* Content of the control script

Please, add bash script directives at the beginning of the file.



```
export CATALINA_HOME=/apps/servers/dspace/tomcat41
export CATALINA_BASE=/apps/servers/dspace/tomcat41

export SOLR_HOME=/apps/dspace/solr
export CATALINA_OPTS="-Dsolr.solr.home=${SOLR_HOME}"

export JAVA_OPTS="-Dsolr.solr.home=/apps/dspace/solr"
export JAVA_OPTS="-Xmx1024M -Xms1024M -XX:PermSize=256M -XX:MaxPermSize=256M -Dfile.encoding=UTF-8"

start() {

echo "Starting Tomcat"

$CATALINA_HOME/bin/startup.sh

return 0
}


stop() {
$CATALINA_HOME/bin/shutdown.sh
echo -n "Stopping Tomcat"

return 0
}

case $1 in

start)
start
;;

stop)
stop
;;

esac

exit 0

```

* Grant permissions

```
$ chown +x+o control

```
* Start and validate Tomcat 

```
$ ./control start

```
[DSpace 4.1 Tomcat Instance](http:localhost:8080)


### Relational Database: PostgreSQL 8.4

PostgreSQL 8.4 to 9.1 PostgreSQL can be downloaded from: [Postgres Downloads](http://www.postgresql.org/).

Additional Content to be added:


##<a name="dspace-installation"></a>DSpace Instance Installation

### Download the DSpace 4.1 from GitHub.

```
$ cd /apps/git
$ git clone https://github.com/DSpace/DSpace.git
$ mv DSpace dspace
$ git br

  dspace-4_x
  master
  
$ git co dspace-4_x
$ git tag
$ git tags/dspace-4.1

```

####Database Setup 

* Log as a superuser

```
$ sudo su
```

* Create a dspace database user, logged in as 'root'.

```
$ createuser -U postgres -d -A -P dspace

```

You will be prompted for the password of the PostgreSQL superuser (postgres). Then you'll be prompted (twice) for a password for the new dspace user. 

Please, just press Enter for the dspace user (we set empty password for now).

```
Enter password for new role:
Enter it again:
Shall the new role be allowed to create more new roles? (y/n) y

```

* 	Create a dspace database, owned by the dspace PostgreSQL user, logged in as 'root'.


```
$ createdb -U dspace -E UNICODE dspace41

```

####DSpace Directory

```
$ mkdir /apps/dspace
```

####Initial Configuration

Copy /apps/git/drum/local.properties.template to local.properties.  This properties file contains the basic settings necessary to actually build/install DRUM for the first time.

The properties with value LOCAL in the local.properties file needs to updated with environment specific values.
Please, pay attention to the value of the database password for the dspace user. You should replace it with the password you have given to the dspace user in the previous steps.


* Configure initial properties

```
$ cd /apps/git/drum
$ cp local.properties.template local.properties
$ vi local.properties

```
* Set the following in local.properties file

```
# DSpace installation directory

dspace.install.dir=/apps/drum
dspace.hostname = <HOSTNAME>
dspace.baseUrl = <APPLICATION-BASE-URL>

# Solr Server location

solr.server = <SOLR-SERVER-URL>

# CAS LDAP CONFIGURATION 
ldap.bind.auth = <ENTER-UMD-LDAP-AUTH-INFO-HERE>
ldap.bind.password = <ENTER-UMD-LDAP-PASSWORD-HERE>

# Database properties

db.url=<ENTER-YOUR-DSPACE-DATABASE-URL>
db.username=<ENTER-YOUR-DSPACE-DATABASE-USERNAME-HERE>
db.password=<ENTER-YOUR-DSPACE-DATABASE-PASSWORD-HERE>

# EMAIL CONFIGURATION 

mail.server = <SMTP-SERVER-IP>

mail.server.username=<SMTP-USERNAME-IF-REQUIRED-BY-SMTP-SERVER>
mail.server.password=<SMTP-PASSWORD-IF-REQUIRED-BY-SMTP-SERVER>

mail.from.address = <ENTER-AN-EMAIL-ADDRESS-HERE>
mail.feedback.recipient = <ENTER-AN-EMAIL-ADDRESS-HERE>
mail.admin = <ENTER-AN-EMAIL-ADDRESS-HERE>
mail.alert.recipient = <ENTER-AN-EMAIL-ADDRESS-HERE>
mail.registration.notify = <ENTER-AN-EMAIL-ADDRESS-HERE>


## ETD Loader Email settings (optional)

mail.etdmarc.recipient = <ENTER-AN-EMAIL-ADDRESS-HERE>
mail.etd.recipient = <ENTER-AN-EMAIL-ADDRESS-HERE>

# HANDLE CONFIGURATION 
handle.prefix = <HANDLE-PREFIX>

### ADDITIONAL CONFIGURATION 

#### Used by daily subscription mailer
eperson.subscription.limiteperson =

#### Used by daily groovy/update_stats_nightly (stats from apache logs) cron job
update.stats.apachelog.dir =

#### Used by etd loader cron job
etdloader.transfermarc =

### Identifier providers.
#### Following are configuration values for the EZID DOI provider, with appropriate
#### values for testing.  Replace the values with your assigned "shoulder" and
#### credentials.
identifier.doi.ezid.shoulder = 10.5072/FK2
identifier.doi.ezid.user = apitest
identifier.doi.ezid.password = apitest
#### A default publisher, for Items not previously published.
#### If generateDataciteXML bean property is enabled. Set default publisher in the
#### XSL file configured by: crosswalk.dissemination.DataCite.stylesheet file.
identifier.doi.ezid.publisher = a publisher


```

####Build the Installation Package

The ```-Denv=local``` arguement will specify maven to use local.properties file instead of the default build.properties file while building dspace.

```
$ cd /apps/git/dspace
$ mvn -U -Denv=local clean package

```

####Install DSpace and Initialize Database

```
$ cd /apps/git/dspace/dspace/target/dspace-4.1-build
$ ant fresh_install

```

####Deploy Web Applications

##### Create a context entry for each DSpace web-application

```
$ cd /apps/servers/dspace/tomcat41/conf/Catalina/localhost

```

* Create jspui context

```
$ vi jspui.xml

<?xml version="1.0" encoding="utf-8"?>
<Context>
    docBase="/apps/dspace/webapps/webapps/jspui" debug="0" reloadable="true" cachingAllowed="false" allowLinking="true"
</Context>

```

* Create xmlui context

```
$ vi xmlui.xml

<?xml version="1.0" encoding="utf-8"?>
<Context>
    docBase="/apps/dspace/webapps/xmlui" debug="0" reloadable="true" cachingAllowed="false" allowLinking="true"
</Context>

```

* Create oai context

```
$ vi oai.xml

<?xml version="1.0" encoding="utf-8"?>
<Context>
    docBase="/apps/dspace/webapps/oai" debug="0" reloadable="true" cachingAllowed="false" allowLinking="true"
</Context>

```
* Create lni context

```
$ vi lni.xml

<?xml version="1.0" encoding="utf-8"?>
<Context>
    docBase="/apps/dspace/webapps/lni" debug="0" reloadable="true" cachingAllowed="false" allowLinking="true"
</Context>

```

* Create rest context

```
$ vi rest.xml

<?xml version="1.0" encoding="utf-8"?>
<Context>
    docBase="/apps/dspace/webapps/rest" debug="0" reloadable="true" cachingAllowed="false" allowLinking="true"
</Context>

```

* Create sword context

```
$ vi sword.xml

<?xml version="1.0" encoding="utf-8"?>
<Context>
    docBase="/apps/dspace/webapps/sword" debug="0" reloadable="true" cachingAllowed="false" allowLinking="true"
</Context>

```

* Create swordv2 context

```
$ vi swordv2.xml

<?xml version="1.0" encoding="utf-8"?>
<Context>
    docBase="/apps/dspace/webapps/swordv2" debug="0" reloadable="true" cachingAllowed="false" allowLinking="true"
</Context>

```

* Create solr context

```
$ vi solr.xml

<?xml version="1.0" encoding="utf-8"?>
<Context>
    docBase="/apps/dspace/webapps/solr" debug="0" reloadable="true" cachingAllowed="false" allowLinking="true"
</Context>

```

#### Download and Configure DSpace Solr Instance

You would need to copy solr.war and solr libs to the dspace directory, and configure dspace solr cores. 

* Copy solr libraries to the dspace solr instance

```
$ cd /apps/tools/
$ mkdir solr
$ cd solr
$ curl -O https://archive.apache.org/dist/lucene/solr/4.4.0/solr-4.4.0.tgz
$ tar -xzvf solr-4.4.0.tgz
$ mv solr-4.4.0 solr44
$ cd solr44/
$ cd dist
$ ls
$ cp solr-4.4.0.war /apps/dspace/solr/solr.war
$ cp -R /apps/tools/solr/solr44/dist /apps/dspace/solr/
$ cp -R /apps/tools/solr/solr44/contrib /apps/dspace/solr/

```

* Configure dspace solr core

	* Search core
	
	```
	$ /apps/dspace/solr/search/conf
	$ vi solrconfig.xml
	
	```
	* Replace lib block (point to the correct location of solr libs)
	
	
	```
	 <lib dir="/apps/dspace/contrib/extraction/lib" regex=".*\.jar" />
     <lib dir="/apps/dspace/dist/" regex="solr-cell-\d.*\.jar" />

     <lib dir="/apps/dspace/contrib/clustering/lib/" regex=".*\.jar" />
     <lib dir="/apps/dspace/dist" regex="solr-clustering-\d.*\.jar" />

     <lib dir="/apps/dspace/contrib/langid/lib/" regex=".*\.jar" />
     <lib dir="/apps/dspace/dist" regex="solr-langid-\d.*\.jar" />

     <lib dir="/apps/dspace/contrib/velocity/lib" regex=".*\.jar" />
     <lib dir="/apps/dspace/dist" regex="solr-velocity-\d.*\.jar" />
  
	```
	

* Oai core
	
	```
	$ /apps/dspace/solr/oai/conf
	$ vi solrconfig.xml
	
	```
	* Replace lib block (point to the correct location of solr libs)
	
	
	```
	<lib dir="/apps/dspace/contrib/extraction/lib" regex=".*\.jar" />
     <lib dir="/apps/dspace/dist/" regex="solr-cell-\d.*\.jar" />

     <lib dir="/apps/dspace/contrib/clustering/lib/" regex=".*\.jar" />
     <lib dir="/apps/dspace/dist" regex="solr-clustering-\d.*\.jar" />

     <lib dir="/apps/dspace/contrib/langid/lib/" regex=".*\.jar" />
     <lib dir="/apps/dspace/dist" regex="solr-langid-\d.*\.jar" />

     <lib dir="/apps/dspace/contrib/velocity/lib" regex=".*\.jar" />
     <lib dir="/apps/dspace/dist" regex="solr-velocity-\d.*\.jar" />
	```
	
* Statistics core
	
	```
	$ /apps/dspace/solr/statistics/conf
	$ vi solrconfig.xml
	
	```
	* Replace lib block (point to the correct location of solr libs)
	
	
	```
	<lib dir="/apps/dspace/contrib/extraction/lib" regex=".*\.jar" />
     <lib dir="/apps/dspace/dist/" regex="solr-cell-\d.*\.jar" />

     <lib dir="/apps/dspace/contrib/clustering/lib/" regex=".*\.jar" />
     <lib dir="/apps/dspace/dist" regex="solr-clustering-\d.*\.jar" />

     <lib dir="/apps/dspace/contrib/langid/lib/" regex=".*\.jar" />
     <lib dir="/apps/dspace/dist" regex="solr-langid-\d.*\.jar" />

     <lib dir="/apps/dspace/contrib/velocity/lib" regex=".*\.jar" />
     <lib dir="/apps/dspace/dist" regex="solr-velocity-\d.*\.jar" />
	```
* Copy solr libraries to the DSpace 4.1 Tomcat instance

```
$ cp /apps/tools/solr/solr44/example/lib/ext /apps/servers/dspace/tomcat41/lib
```


###Create administrator account

*	Run command
	* Provide administrator login and password	
	
```
$ /apps/git/dspace/dspace/bin/dspace create-administrator
```



###Start DSpace 4.1 Tomcat Instance

```
$ cd /apps/servers/dspace/tomcat41
$ ./control start

```

##<a name="validate-installation"></a>Check DSpace 4.1 Installation


* Check webapps

	* [jspui](http://localhost:8080/jspui/)
	* [xmlui](http://localhost:8080/xmlui/)
	* [oai](http://localhost:8080/oai/request?verb=Identify)
	
* Check DSpace Solr Instance
	* [DSpace 4.1 Solr](http://localhost:8080/solr/#/)
	* Look at the Solr Core Tab (ensure all cores: search, statistics, oai are running)
	* Consult Tomcat/Solr logs if needed
* Login as Administrator and create community, collection, submit an item to the collection.
	


