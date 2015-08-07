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

function dmvn() {
    (cd /apps/git/drum; pwd; mvn "$@")
}

function dant() {
  (/apps/servers/drum/tomcat411/control stop;
   cd /apps/git/drum/dspace/target/dspace-*.*-build/;
   ant "$@";
   sleep 20;
   rm -rf /apps/servers/drum/tomcat411/work/Catalina/localhost;
   /apps/servers/drum/tomcat411/control start)
}

alias dup="dmvn -Denv=local clean package && dant update"
alias dp="dmvn -Denv=local clean package"


```


* Switch to the Drum 4.1 development environment:

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

* Checkout DRUM 4.1 Maven projects into the drum workspace:

	Workspace location: /apps/cms/drum
	
	* File > Import > Maven > Existing Maven Projects
	
	When you import maven projects, please point to the root drum directory where the  parent pom is located (/apps/git/drum). 

* Set Spring Source settings
* SpringSource Tool > Preferences

	* General:
		* Workspace > New text file delimeter > Other > Windows
		* Workspace > Text file encoding > Other UTF-8
		* Workspace 
			* Selected - Build Automatically 
			* Selected - Refresh using native hooks or polling
	* Maven 
		 * Unselected - Do not automatically update dependencies from remote repositories
         * Selected - Download Artifact Sources
         * Selected - Download Artifact JavaDoc
    * Java
    	* Code Style > Formatter
        	* Import [DSpace formatter](https://wiki.duraspace.org/display/DSPACE/Code+Contribution+Guidelines#CodeContributionGuidelines-CodingConventions) and set as Active profile.
        * Editor
        	* Save Actions
        	* Selected - Perform the selected actions on save
            	* Format source code > Format all lines
            * Organize imports
            * Additional actions (Press configure)
                * Code Organizing
                 * Remove trailing whitespace > All lines
                 * Correct indentation
    * XML
        * XML Files > Editor
            * Indent using spaces
            * Indentation size: 4
 
 We skip the Web setup for Drum as we usually for the other projects.






