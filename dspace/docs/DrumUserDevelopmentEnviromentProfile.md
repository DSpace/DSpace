#User Profile Configuration - Development Environment  

**[Drum 1.7 Environment](#drum17)**

**[Drum 4.1 Environment](#drum41)**

To facilitate development, please update a local *.profile*
with the functions for each development environment.

These functions set the JAVA_HOME, and Maven HOME environment variables.

##<a name="drum17"></a>Drum 1.7 Environment

*	Requires:
	*	Java 6
	*	Maven 2.2


* Update local profile with Drum 1.7 settings:

```
$ cd ~
$ vi .profile

function drum17() {
  echo "Drum 1.7 profile ... "
  export JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
  PATH=${JAVA_HOME}/bin:${PATH}
  export M2_HOME=/apps/apache-maven-2.2.1
  export M2=$M2_HOME/bin
  export PATH=$M2:$PATH
  mvn --version
}

function dmvn() {
  (cd /apps/git/drum/dspace; pwd; mvn "$@")
}

function dant() {
  (cd /apps/git/drum/dspace/target/dspace-*-build.dir/; ant -Dconfig=/apps/drum/config/dspace.cfg "$@")
}

alias dup="dmvn package && dant update"
```

* Switch to the Drum 1.7 development environment

```
$ drum17
```
* Build Drum package

```
$ dmvn package
```

* Install Drum in the update mode

```
$ dant update
```

* Build and Install Drum in the update mode

```
$ dup
```

##<a name="drum41"></a>Drum 4.1 Environment

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

* Checkout DRUM 4.1 Maven projects into the drum-new workspace:

	Workspace location: /apps/cms/drum-new
	
* [Enable DSpace specific formatting settings](https://wiki.duraspace.org/display/DSPACE/Code+Contribution+Guidelines#CodeContributionGuidelines-CodingConventions)

Download DSpace Eclipse formatter from the link above.

* Import formatter

	* Open Spring Preferences and import DSpace formatter from **Java/Code Style/Formatter**.
	Click Import.
	
* Configure Editor settings: Spring Preferences>Java>Editor>Save Action

Make sure the following items checked:

* Perform the following actions on Save:
	* Format Source Code > Format All Lines
	* Organize imports
			

	






