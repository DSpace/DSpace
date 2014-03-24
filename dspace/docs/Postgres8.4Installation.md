#Postgres 8.4 Installation 

**[Prerequisites](#prerequisites)**

**[PostgreSQL Installation](#postgres-installation)**

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
$ export PGHOST=localhost
$ export PGDATA=opt/local/var/db/postgresql84/defaultdb
```

* Create .pgpass file

```
$ cd ~ 
$ vi .pgpass
save file & disallow permissions for the group or world
$ chmod 0600 ~/.pgpass
```

* Create a database instance

```
$ sudo chown postgres:postgres /opt/local/var/db/postgresql84/defaultdb
$ sudo su postgres -c '/opt/local/lib/postgresql84/bin/initdb -D /opt/local/var/db/postgresql84/defaultdb'
```

* Give the postgres user a shell (make sure that the postgres user account can be executed using the su command)

```
sudo dscl . -create /Users/postgres UserShell /bin/sh
```
* Merge the Sys V Shared Memory Kernel parameters from the /etc/sysctl.conf.pg file with the existing /etc/sysctl.conf file (optional ?)


```
$ sudo sh -c "cat /etc/sysctl.conf.pg >>/etc/sysctl.conf"
```

* Content of /etc/sysctl.conf

```
$ vi /etc/sysctl.conf
```

```
kern.sysv.shmmax=33554432
kern.sysv.shmmin=1
kern.sysv.shmmni=256
kern.sysv.shmseg=64
kern.sysv.shmall=8192
```


* Reboot the OS

* Create the log file directory and make it writable by the postgres user

```
sudo mkdir -p /opt/local/var/log/postgresql84
sudo chown postgres:postgres /opt/local/var/log/postgresql84
```

* Create a postgres database user account for yourself and make yourself superuser

```
$ createuser `whoami`
$ Shall the new role be a superuser? (y/n) y
```

* Make the database server accessible via a TCP socket and allow local connections to the localhost

```
$ sudo su
$ vi /opt/local/var/db/postgresql84/defaultdb/pg_hba.conf
```
* Edit the pg_hba.conf

```
TYPE  DATABASE    USER        CIDR-ADDRESS          METHOD
local is for Unix domain socket connections only
host dspace dspace 127.0.0.1 255.255.255.255 md5
local   all         all                               trust

IPv4 local connections:
host    all         all         127.0.0.1/32          trust
IPv6 local connections:
host    all         all         ::1/128               trust
host    all         all         0.0.0.0/0             trust

```

* Edit the postgresql.conf and add line **listen_addresses = '*'**


```
$ vi /opt/local/var/db/postgresql84/defaultdb/postgresql.conf
#------------------------------------------------------------------------------
# CUSTOMIZED OPTIONS
#------------------------------------------------------------------------------
$ listen_addresses = '*'
```


* Run PostreSQL in background on system startup

```
$ sudo launchctl load -w /Library/LaunchDaemons/org.macports.postgresql84-server.plist
```

* Reboot the OS

* Connect to the database

```
$ psql template1
psql (8.4.17)
Type "help" for help.
```

###References
http://damosworld.wordpress.com/2011/04/09/installation-of-postgresql-on-macos-x-using-macports/