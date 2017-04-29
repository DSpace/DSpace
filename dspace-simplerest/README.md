REQUIREMENTS
------------

1. Maven user settings required for this project:
[C:\Documents and Settings\<username>\.m2\settings.xml]

[/home/<username>/.m2/settings.xml]
```
  <settings ...>
  ...
  <profiles>
    <profile>
    <id>(your-profile)</id>
    <properties>
      ...
      <mylogic.log.file>absolute path to ${project.artifactId}.log</mylogic.log.file>
      <spring.log.file>absolute path to ${project.artifactId}-spring.log</spring.log.file>
      <dspace.log.file>absolute path to ${project.artifactId}-dspace.log</dspace.log.file>
      <dspace.rest.username>...</dspace.rest.username>
      <dspace.rest.password>...</dspace.rest.password>
      <dspace.eperson.id>...</dspace.eperson.id>
      <dspace.config.file>absolute path to DSpace dspace.cfg file</dspace.config.file>
      ...
    </properties>
    </profile>
  </profiles>
  ...
  <settings>
```
, where absolute path examples: 

[D:\...\${project.artifactId}.log] [/var/.../${project.artifactId}.log]

[D:\...\dspace.cfg] [/data/dspace/.../dspace.cfg]


If you've already cloned SimpleREST, you can pull new changes from github git repo with the following command: git pull origin master

USAGE
-----

A. Install the RESTful web service on top of your DSpace
========================================================
ATTENTION! OBS! HUOM!
Check your exec.sh before doing any installation

1. Install git on to your system.

2. Clone updated code from the repo: git clone
https://github.com/anis-moubarik/SimpleREST.git

3. In the console, position in the project base directory, where the pom.xml 
file is:
 
4. mvn clean package -P(your-profile) [from above section on settings.xml]

5. Check in the console that all completed successfully. [BUILD SUCCESS]

6. The <WAR> file is generated in /target/.

7. Copy the <WAR> file to your DSpace webapps folder.
[In our UNIX setups it is: /data/dspace/webapps]

[5.0 You might need to restart the Tomcat]

8. You can test the RESTful web service at:
http://<dspace-hostname>/simplerest/rootcommunities

9. The log file(s) written where you said above.

B. Uninstall the RESTful web service on top of your DSpace
==========================================================

1. Delete the <WAR> file AND the folder with the same name from your DSpace 
webapps folder.
[In our UNIX setups it is: /data/dspace/webapps]

SITE
----

1. In the console, position in the project base directory, where the pom.xml 
file is:
 
2. mvn clean site -P(your-profile) [from above section on settings.xml]

3. The site is generated in /target/site/. [index.html]

SimpleREST and json
--------------------
Add media query to your urls to retrieve data in json format, for example.
www.dspaceinstance.com/simplerest/rootcommunities?media=json
json is supported with communities, users, groups, collections and items

SimpleREST testing philosophy (not srs)
---------------------------------------
![TestingPhilosophy](https://raw.github.com/anis-moubarik/SimpleREST/master/testing.jpg)

AUTOMATIC LICENSING HEADERS
---------------------------

1. To check which of the predefined files[1] have the predefined license
header[2] or not:

mvn com.mycila.maven-license-plugin:maven-license-plugin:check

2. To remove the predefined header[2] from the predefined files[1]:

mvn com.mycila.maven-license-plugin:maven-license-plugin:remove

3. To write/format the predefined files[1] with the header[2].

mvn com.mycila.maven-license-plugin:maven-license-plugin:format

---
[1] see includes, excludes in the pom.xml for 
com.mycila.maven-license-plugin:maven-license-plugin
[2] src/main/config/HEADER.txt
