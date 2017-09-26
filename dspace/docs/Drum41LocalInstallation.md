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

export SOLR_HOME=/apps/drum/solr
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
[Drum User Development Profile - Configuration Reference](https://github.com/umd-lib/drum/blob/drum-develop/dspace/docs/DrumUserDevelopmentEnviromentProfile.md)

### Relational Database: PostgreSQL 8.4

Please, install [PostgeSQL 8.4 according to the instructions](Postgres8.4Installation.md).


##<a name="drum-installation"></a>DRUM Instance Installation

### Download the DRUM 4.1 from GitHub.

```
$ cd /apps/git
$ git clone https://github.com/umd-lib/drum.git

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
$ mkdir /apps/drum
```

####Initial Configuration

Copy /apps/git/drum/local.properties.template to local.properties.  This properties file contains the basic settings necessary to actually build/install DRUM for the first time.

The properties in local.properties file needs to updated with environment specific values.
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
dspace.hostname = localhost
dspace.baseUrl = http://localhost:8080
my.dspace.url = ${dspace.baseUrl}/jspui

# Solr Server location

solr.server = http://localhost:8983/solr
OR
solr.server = https://localhost:8443/solr


# Stackable Authentication Methods
security.plugins.stack = \
        org.dspace.authenticate.IPAuthentication, \
        org.dspace.authenticate.CASAuthentication, \
        org.dspace.authenticate.PasswordAuthentication

# IP Authentication 
authentication.ip = 

# LDAP Authentication Configuration Settings
enable.ldap = 


# CAS LDAP CONFIGURATION 
ldap.bind.auth = <ENTER-UMD-LDAP-AUTH-INFO-HERE>
ldap.bind.password = <ENTER-UMD-LDAP-PASSWORD-HERE>

# Database properties

db.url=jdbc:postgresql://localhost:5432/dspace411
db.username=dspace
db.password=<ENTER-YOUR-DSPACE-DATABASE-PASSWORD-HERE>

# EMAIL CONFIGURATION (optional in local)


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
handle.prefix = 123456789

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

#####Full build
The first build has be done from the project root. This will build and deploy the dspace artifacts to the local maven repository. A full build would be necessary after every project version change.

```
$ cd /apps/git/drum
$ mvn -U -Denv=local clean install -Dlicense.skip=true
```
Note: The ```-Dlicense.skip=true``` is added to avoid build failure due to missing license texts in the files added by umd customization.

#####Quick build
After the initial build, the project can be rebuilt from the dspace directory. This will rebuild the modules and reapply overlays to the dspace artifacts.

```
$ cd /apps/git/drum/dspace
$ mvn -U -Denv=local clean package
```

Also, to only rebuild specific modules, use the corresponding profile. Example profiles:

```
$ mvn -U -Denv=local clean package -P dspace-xmlui
$ mvn -U -Denv=local clean package -P xmlui-and-mirage2
```
See [dspace/modules/pom.xml](../modules/pom.xml) for all available profiles.

####Install DRUM code

* Do a fresh install of the code, preserving any data

```
$ cd /apps/git/drum/dspace/target/dspace-4.1-build
$ ant install_code
```

Regular code update
```
$ ant update
```


* Project help

```
$ cd /apps/git/drum/dspace/target/dspace-4.1-build
$ ant -projecthelp
```

#### Setup Solr Environment for Drum

* Follow instructions in [solr-env local documentation](https://github.com/umd-lib/solr-env/tree/local) to setup the drum solr cores.
* Start the solr server

Note: Unlike the other dspace webapps, the solr server need not be restarted after every ```ant update```. Solr server restart is necessary only when there is a change in solr configuration.




####Deploy Web Applications

##### Create soft links for the Drum 4.1 web-applications

```
$ ln -s /apps/drum/webapps/jspui /apps/servers/drum/tomcat411/webapps/jspui
$ ln -s /apps/drum/webapps/xmlui /apps/servers/drum/tomcat411/webapps/xmlui
$ ln -s /apps/drum/webapps/oai /apps/servers/drum/tomcat411/webapps/oai
$ ln -s /apps/drum/webapps/lni /apps/servers/drum/tomcat411/webapps/lni
$ ln -s /apps/drum/webapps/rest /apps/servers/drum/tomcat411/webapps/rest
$ ln -s /apps/drum/webapps/sword /apps/servers/drum/tomcat411/webapps/sword
$ ln -s /apps/drum/webapps/swordv2 /apps/servers/drum/tomcat411/webapps/swordv2
```

### Create directories to be accessed by webapps 

```
$ mkdir /apps/drum/upload
```

###Start DRUM 4.1 Tomcat Instance

```
$ cd /apps/servers/drum/tomcat411
$ ./control start
```

* Refresh Browse and Search  and OAI Indexes
 
Run command from the Drum 4.1 installation directory (Tomcat & Solr should be up and running)

```
$ cd /apps/drum 
$ bin/dspace index-discovery -f
$ bin/dspace oai import
```

* Create administrator account (optional, unlless you have UMD CAS account in the current  DSpace with Administrator permissions). Please, note the user account is identified by email id.

```
$ cd /apps/drum 
$ bin/dspace create-administrator
```

### Quick Build/Redeploy

Please, enable drum 4.1 functions in the local profile as described in this [document](DrumUserDevelopmentEnviromentProfile.md), source your local profile, switch to drum41 environment and run shortcut commands.

* Source profile

```
$ source ~/.profile
```

* Switch to the Drum 4.1 environment

```
$ drum41
```

* Build package

```
$ dp
```

* Build package & Redeploy with Tomcat clean-up/restart

```
$ dup
```

##<a name="validate-installation"></a>Check DRUM 4.1 Installation


* Check webapps

	* [jspui](http://localhost:8080/jspui/)
	* [xmlui](http://localhost:8080/xmlui/)
	* [oai](http://localhost:8080/oai/request?verb=Identify)
	
* Check DRUM Solr Instance
	* [DRUM 4.1 Solr](http://localhost:8983/solr/#/)
	* Look at the Solr Core Tab (ensure all cores: search, statistics, oai are running)
	* Consult Tomcat/Solr/Dspace logs if needed
	
	* DSpace and Solr logs
	    
		```
		$ cd /apps/drum/log 
		$ tail -f solr.log
		``` 

   * Tomcat logs
   
  		```
		$ cd /apps/servers/drum/tomcat411/
		$ tail -f -catalina.out
		$
		```  
		   
* Login as Administrator and create community, collection, submit an item to the collection.

