#Drum 1.7 Installation 

**[Prerequisite Software](#prerequisite-software)**

**[DSpace Instance Installation](#dspace-installation)**

**[Validation procedure](#validate-installation)**


##<a name="prerequisite-software"></a>Prerequisite Software

The third-party components and tools to run a Drum 1.7.

### Oracle Java JDK 6


Oracle's Java can be downloaded from the following location: [Java Downloads](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

Please, follow the download instructions and set your JAVA_HOME enviroment variable to a Java 6 JDK.

If you are using Mac OS you might want to check the installed Java machines at the following location:

/Library/Java/JavaVirtualMachines.

If you have Java JDK listed you may skip the installation step and simply to setup the JAVA_HOME according to the example below:

```
$ cd ~ 
$ vi .profile
$ export JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
  PATH=${JAVA_HOME}/bin:${PATH}
$ java -version

java version "1.6.0_21"

```

### Apache Maven 2.2 (Java build tool)

Please, unpack all build tools into /apps/tools directory

```
$ mkdir /apps/tools

```

Maven is necessary in the first stage of the build process to assemble the installation package for the DSpace instance.

Maven can be downloaded from the following location: [Maven Downloads](http://maven.apache.org/download.html).

Check your current maven version:

```
$ mvn --version
$ cd /apps/tools
$ curl -O http://archive.apache.org/dist/maven/binaries/apache-maven-2.2.1-bin.tar.gz
$ tar -xzvf apache-maven-2.2.1-bin.tar.gz
$ rm apache-maven-2.2.1-bin.tar.gz

```

Set the Maven environment variable to the Maven 2.2.x.

```
$ vi .profile
$ export M2_HOME=/apps/tools/apache-maven-2.2.1
$ export M2=$M2_HOME/bin
$ export PATH=$M2:$PATH
$ mvn --version

```

### Apache Ant 1.8 or later (Java build tool)

Apache Ant is required for the second stage of the build process.

Ant can be downloaded from the following location: http://ant.apache.org.


####Create directory for ant and download its archive.

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
$ mkdir /apps/servers/drum/
$ cd /apps/servers/drum/
$ curl -O http://archive.apache.org/dist/tomcat/tomcat-7/v7.0.52/bin/apache-tomcat-7.0.52.tar.gz
$ tar -xzvf apache-tomcat-7.0.52.tar.gz
$ ls
$ mv apache-tomcat-7.0.52 tomcat17
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
export CATALINA_HOME=/apps/servers/drum/tomcat17
export CATALINA_BASE=/apps/servers/drum/tomcat17

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
[DSpace 1.7 Tomcat Instance](http:localhost:8080)


### Relational Database: PostgreSQL 8.4

PostgreSQL 8.4 to 9.1 PostgreSQL can be downloaded from: [Postgres Downloads](http://www.postgresql.org/).

Additional Content to be added:


##<a name="dspace-installation"></a>DSpace 1.7 Instance Installation

### Download the DSpace 1.7 from GitHub.

```
$ cd /apps/git
$ git clone git@github.com:umd-lib/drum.git
$ git br

  develop
  master
  
$ git co develop

```

####Database Setup 


* Download current drum database from the server.
* Place database dump into: /apps/tmp/ directory:

 ```
 $ cd /apps/tmp/
 $ ls
 $ dump.tar.0
 ```

* Log as a superuser

```
$ sudo su
```

* Create a database user named 'root', you're logged in as 'root' unix user.

```
$ createuser -U postgres -d -A -P root

```

You will be prompted for the password of the PostgreSQL superuser (postgres). Then you'll be prompted (twice) for a password for the new root user. 

Please enter password the root database user.

```
Enter password for new role:
Enter it again:
Shall the new role be allowed to create more new roles? (y/n) y

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

*  	Create a dspace database, owned by the dspace PostgreSQL user, logged in as 'root'.


```
$ createdb -U root --encoding=UNICODE --owner=dspace dspace

```

* 	Unpack a dspace database from the archive.

```
$ pg_restore -U root -d dspace /apps/tmp/dump.tar.0

```

####Drum Directory

* Checkout drum server environment from GitHub.

```
$ cd /apps/git
$ git clone https://github.com/umd-lib/drum-env
$ mv /apps/git/drum-env /apps/drum
```

####Initial Configuration


* Obtain the dspace-local.cfg file first, and copy it to /apps/git/drum/dspace/config directory

```
$ cd /apps/git/drum/dspace/config

```


####Build the Installation Package


```
$ cd /apps/git/drum/dspace
$ mvn package

```

####Install DSpace and Initialize Database

```
$ cd /apps/git/drum/dspace/target/dspace-1.7.2.2-SNAPSHOT-build.dir
$ ant fresh_install

```

####Configure local properties of the Drum Instance

```
$ cd /apps/drum/config/
$ vi dspace-local.cfg

```

* You might want to check/change the following properties:

```
db.url = jdbc:postgresql://localhost:5432/dspace
db.username = dspace
db.password = 

```


####Deploy Web Applications

##### Create a soft link for the Drum jspui web-application

```
$ ln -s /apps/drum/webapps/jspui /apps/servers/drum/tomcat17/webapps/jspui

```


###Create administrator account

*	Run command
	* Provide administrator login and password	
	
```
$ /apps/git/drum/dspace/bin/dspace create-administrator
```



###Start Drum 1.7 Tomcat Instance

```
$ cd /apps/servers/drum/tomcat17
$ ./control start

```

##<a name="validate-installation"></a>Check Drum 1.7 Installation


* Check webapps

	* [jspui](http://localhost:8080/jspui/)
		
* Login as Administrator and create community, collection, submit an item to the collection.
	


