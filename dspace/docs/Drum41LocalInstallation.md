#DRUM 4.1 Installation 

**[Prerequisite Software](#prerequisite-software)**

**[DRUM Instance Installation](#drum-installation)**

**[Validation procedure](#validate-installation)**


##<a name="prerequisite-software"></a>Prerequisite Software

The third-party components and tools to run DRUM 4.1 server instance.

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
$ ant -version

It should return the installed ant version.
Apache Ant(TM) version 1.9.3 compiled on December 23 2013

```

### Tomcat 7

* Create installation directory

```
$ mkdir /apps/servers/drum/
$ cd /apps/servers/drum/
$ curl -O http://archive.apache.org/dist/tomcat/tomcat-7/v7.0.52/bin/apache-tomcat-7.0.52.tar.gz
$ tar -xzvf apache-tomcat-7.0.52.tar.gz
$ ls
$ mv apache-tomcat-7.0.52 tomcat411
$ ls

```

* Create control script

```
$ cd /apps/servers/drum/tomcat411
$ vi control

```

* Content of the control script

Please, add bash script directives at the beginning of the file.



```
export CATALINA_HOME=/apps/servers/drum/tomcat411
export CATALINA_BASE=/apps/servers/drum/tomcat411

export SOLR_HOME=/apps/drum-new/solr
export CATALINA_OPTS="-Dsolr.solr.home=${SOLR_HOME}"

export JAVA_OPTS="-Dsolr.solr.home=${SOLR_HOME}"
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
$ chmod +x+o control

```
* Start and validate Tomcat 

```
$ ./control start

```
[DSpace 4.1 Tomcat Instance](http:localhost:8080)

### Enable DRUM 4.1 Local Profile

*	Requires:
	*	Java 7
	*	Maven 3


* Update local profile with Drum 4.1 settings:

```
$ cd ~
$ vi .profile

function drum41() {
  echo "Drum 4.1 profile ... "
  export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.7.0_40.jdk/Contents/Home
  PATH=${JAVA_HOME}/bin:${PATH}
  export M2_HOME=/usr/share/maven
  export M2=$M2_HOME/bin
  export PATH=$M2:$PATH
  mvn --version
}
```


* Switch to the Drum 4.1 development environment:

```
$ drum41
```
[Drum User Development Profile - Configuration Reference](https://github.com/umd-lib/drum-new/blob/drum-develop/dspace/docs/DrumUserDevelopmentEnviromentProfile.md)

### Relational Database: PostgreSQL 8.4

Please, install [PostgeSQL 8.4 according to the instructions](Postgres8.4Installation.md).


##<a name="drum-installation"></a>DRUM Instance Installation

### Download the DRUM 4.1 from GitHub.

```
$ cd /apps/git
$ git clone https://github.com/umd-lib/drum-new.git

```
Checkout drumn-develop branch

```
$ git br

  drum-develop
  drum-master
  master
  
$ git co drum-develop

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
* Create a root database user, logged in as 'root'.

```
$ createuser -U postgres -d -A -P -s root

```

You will be prompted for the password of the PostgreSQL superuser (postgres). Then you'll be prompted (twice) for a password for the new dspace user. 

Please, just press Enter for the dspace user (we set empty password for now).

```
Enter password for new role:
Enter it again:
Shall the new role be allowed to create more new roles? (y/n) y

```


* Create a dspace411 database, owned by the dspace PostgreSQL user, logged in as 'root'.


```
$ createdb -U root --encoding=UNICODE --owner=dspace dspace411

```

* Obtain the current production database dump & run database upgrade scripts 
	
	OR
	
* Obtain the upgraded version of the dump from your project team
	
	* 	Unpack a dspace database from the archive to the target database dspace411

Assume the database dump was copied to

/apps/tmp/dump.tar.0


```
$ cd /apps/tmp
$ ls
dump.tar.0
$ pwd 
```

Run pg_restore command

```
$ pg_restore -U root -d dspace411 /apps/tmp/dump.tar.0
```
Login into psql console, and note the unpacked dspace411 database in the list

```
$ psql -U dspace -d dspace411
===>
\list

```

####DSpace Directory

```
$ mkdir /apps/drum-new
```

####Initial Configuration

Edit /apps/git/drum-new/build.properties.  This properties file contains the basic settings necessary to actually build/install DRUM for the first time.

By the time, you checkout the build.properties it will contain the properties setup.
Please, pay attention to the value of the database password for the dspace user. You should replace it with the password you have given to the dspace user in the previous steps.


* Configure initial properties

```
$ /apps/git/drum-new
$ vi build.properties

```
* Set the following properties

```
DSpace installation directory

dspace.install.dir=/apps/drum-new

Solr Server location

solr.server=http://localhost:8080/solr
Database properties

db.url=jdbc:postgresql://localhost:5432/dspace411
db.username=dspace
db.password=

```

####Build the Installation Package


```
$ cd /apps/git/drum-new/dspace
$ mvn -U clean package
```

####Install DRUM code

* Do a fresh install of the code, preserving any data

```
$ cd /apps/git/drum-new/dspace/target/dspace-4.1-build
$ ant install_code
```

Regular code update
```
$ ant update
```


* Project help

```
$ cd /apps/git/drum-new/dspace/target/dspace-4.1-build
$ ant -projecthelp
```

#### Download and Configure DRUM 4.1 Solr Instance

You would need to copy solr.war and solr libs to the drum installation directory, and configure drum solr cores. 

* Copy solr libraries to the drum solr instance

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
$ cp solr-4.4.0.war /apps/drum-new/solr/solr.war
$ mkdir /apps/drum-new/solrlib
$ cp -R /apps/tools/solr/solr44/dist /apps/drum-new/solrlib
$ cp -R /apps/tools/solr/solr44/contrib /apps/drum-new/solrlib

```

* Configure drum solr cores

	* Search core
	
	```
	$ cd /apps/drum-new/solr/search/conf
	$ vi solrconfig.xml
	
	```
	* Replace lib block (point to the correct location of solr libs)
	
	
	```
	 <lib dir="/apps/drum-new/solrlib/contrib/extraction/lib" regex=".*\.jar" />
     <lib dir="/apps/drum-new/solrlib/dist/" regex="solr-cell-\d.*\.jar" />

     <lib dir="/apps/drum-new/solrlib/contrib/clustering/lib/" regex=".*\.jar" />
     <lib dir="/apps/drum-new/solrlib/dist" regex="solr-clustering-\d.*\.jar" />

     <lib dir="/apps/drum-new/solrlib/contrib/langid/lib/" regex=".*\.jar" />
     <lib dir="/apps/drum-new/solrlib/dist" regex="solr-langid-\d.*\.jar" />

     <lib dir="/apps/drum-new/solrlib/contrib/velocity/lib" regex=".*\.jar" />
     <lib dir="/apps/drum-new/solrlib/dist" regex="solr-velocity-\d.*\.jar" />
  
	```
	

* Oai core
	
	```
	$ cd /apps/drum-new/solr/oai/conf
	$ vi solrconfig.xml
	
	```
	* Replace lib block (point to the correct location of solr libs)
	
	
	```
	 <lib dir="/apps/drum-new/solrlib/contrib/extraction/lib" regex=".*\.jar" />
     <lib dir="/apps/drum-new/solrlib/dist/" regex="solr-cell-\d.*\.jar" />

     <lib dir="/apps/drum-new/solrlib/contrib/clustering/lib/" regex=".*\.jar" />
     <lib dir="/apps/drum-new/solrlib/dist" regex="solr-clustering-\d.*\.jar" />

     <lib dir="/apps/drum-new/solrlib/contrib/langid/lib/" regex=".*\.jar" />
     <lib dir="/apps/drum-new/solrlib/dist" regex="solr-langid-\d.*\.jar" />

     <lib dir="/apps/drum-new/solrlib/contrib/velocity/lib" regex=".*\.jar" />
     <lib dir="/apps/drum-new/solrlib/dist" regex="solr-velocity-\d.*\.jar" />
  	```
	
* Statistics core
	
	```
	$ cd /apps/drum-new/solr/statistics/conf
	$ vi solrconfig.xml
	
	```
	* Replace lib block (point to the correct location of solr libs)
	
	
	```
	 <lib dir="/apps/drum-new/solrlib/contrib/extraction/lib" regex=".*\.jar" />
     <lib dir="/apps/drum-new/solrlib/dist/" regex="solr-cell-\d.*\.jar" />

     <lib dir="/apps/drum-new/solrlib/contrib/clustering/lib/" regex=".*\.jar" />
     <lib dir="/apps/drum-new/solrlib/dist" regex="solr-clustering-\d.*\.jar" />

     <lib dir="/apps/drum-new/solrlib/contrib/langid/lib/" regex=".*\.jar" />
     <lib dir="/apps/drum-new/solrlib/dist" regex="solr-langid-\d.*\.jar" />

     <lib dir="/apps/drum-new/solrlib/contrib/velocity/lib" regex=".*\.jar" />
     <lib dir="/apps/drum-new/solrlib/dist" regex="solr-velocity-\d.*\.jar" />
  
	```
	
* Copy solr libraries to the DRUM 4.1 Tomcat instance

```
$ cp -r /apps/tools/solr/solr44/example/lib/ext /apps/servers/drum/tomcat411/lib
```

####Deploy Web Applications

##### Create soft links for the Drum 4.1 web-applications

```
$ ln -s /apps/drum-new/webapps/jspui /apps/servers/drum/tomcat411/webapps/jspui
$ ln -s /apps/drum-new/webapps/xmlui /apps/servers/drum/tomcat411/webapps/xmlui
$ ln -s /apps/drum-new/webapps/solr /apps/servers/drum/tomcat411/webapps/solr
$ ln -s /apps/drum-new/webapps/oai /apps/servers/drum/tomcat411/webapps/oai
$ ln -s /apps/drum-new/webapps/lni /apps/servers/drum/tomcat411/webapps/lni
$ ln -s /apps/drum-new/webapps/rest /apps/servers/drum/tomcat411/webapps/rest
$ ln -s /apps/drum-new/webapps/sword /apps/servers/drum/tomcat411/webapps/sword
$ ln -s /apps/drum-new/webapps/swordv2 /apps/servers/drum/tomcat411/webapps/swordv2
```

###Start DRUM 4.1 Tomcat Instance

```
$ cd /apps/servers/drum/tomcat411
$ ./control start

```

* Refresh Browse and Search Indexes
 
Run command from the Drum 4.1 installation directory (Tomcat & Solr should be up and running)

```
$ cd /apps/drum-new 
$ bin/dspace index-discovery -f
```

* Create administrator account (optional, unlless you have UMD CAS account in the current  DSpace with Administrator permissions). Please, note the user account is identified by email id.

```
$ cd /apps/drum-new 
$ bin/dspace create-administrator
```

##<a name="validate-installation"></a>Check DRUM 4.1 Installation


* Check webapps

	* [jspui](http://localhost:8080/jspui/)
	* [xmlui](http://localhost:8080/xmlui/)
	* [oai](http://localhost:8080/oai/request?verb=Identify)
	
* Check DRUM Solr Instance
	* [DRUM 4.1 Solr](http://localhost:8080/solr/#/)
	* Look at the Solr Core Tab (ensure all cores: search, statistics, oai are running)
	* Consult Tomcat/Solr/Dspace logs if needed
	
	    * DSpace and Solr logs
	    
		```
		$ cd /apps/drum-new/log 
		$ tail -f solr.log
		$
		``` 

* Login as Administrator and create community, collection, submit an item to the collection.

