#Postgres 8.4 Installation 

**[Prerequisites](#prerequisites)**

**[PostgreSQL Installation](#postgres-installation)**

**[Validation procedure](#validate-installation)**


##<a name="prerequisite-software"></a>Prerequisites

Before starting, make sure you have installed the prerequisites. 

### Mac OS Developer's Tools

Mac OS Developer's Tools such as XCode can be downloaded from [here](https://developer.apple.com/xcode/). 

[Install Xcode and the Xcode Command Line Tools](http://guide.macports.org/#installing.xcode).

You can verify the XCode current setting by opening the *XCode/System Preferences/Components/Command Line Tools*.

### MacPorts

MacPorts is necessary to install the Postgres instance.

[MacPorts Installation Guide](http://www.macports.org/install.php).


##<a name="postgres-installation"></a>PostgreSQL Installation


* Install database server from the command-line

```
$ sudo port install postgresql84 postgresql84-server
```


* Add the bin folder of the PostgreSQL installation to the system PATH

```
$ sudo mkdir -p /opt/local/var/db/postgresql84/defaultdb
$ vi .profile
$ export PATH=/opt/local/lib/postgresql84/bin:$PATH
```

* Create a database instance

```
$ sudo mkdir -p /opt/local/var/db/postgresql84/defaultdb
$ sudo chown postgres:postgres /opt/local/var/db/postgresql84/defaultdb
$ sudo su postgres -c '/opt/local/lib/postgresql84/bin/initdb -D /opt/local/var/db/postgresql84/defaultdb'
```
 
* Run PostreSQL in background

```
$ sudo launchctl load -w /Library/LaunchDaemons/org.macports.postgresql84-server.plist
```

