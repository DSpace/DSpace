#!/bin/sh

# Per http://wiki.datadryad.org/How_To_Install_Dryad#Installation_issues_with_maven
mvn install:install-file -Dfile=$TRAVIS_BUILD_DIR/dspace/etc/discoverySnapshot/discovery-solr-provider-0.9.4-SNAPSHOT.jar  -DgroupId=org.dspace.discovery -DartifactId=discovery-solr-provider -Dversion=0.9.4-SNAPSHOT -Dpackaging=jar
mvn install:install-file -Dfile=$TRAVIS_BUILD_DIR/dspace/etc/discoverySnapshot/dspace-solr-solrj-1.4.0.1-SNAPSHOT.jar  -DgroupId=org.dspace.dependencies.solr -DartifactId=dspace-solr-solrj -Dversion=1.4.0.1-SNAPSHOT -Dpackaging=jar
mvn install:install-file -Dfile=$TRAVIS_BUILD_DIR/dspace/etc/discoverySnapshot/discovery-xmlui-block-0.9.4-SNAPSHOT.jar  -DgroupId=org.dspace.discovery -DartifactId=discovery-xmlui-block -Dversion=0.9.4-SNAPSHOT -Dpackaging=jar

# Carrot2 is also missing
mvn install:install-file -Dfile=$TRAVIS_BUILD_DIR/dspace/etc/discoverySnapshot/carrot2-mini-3.1.0.jar -DgroupId=org.carrot2 -DartifactId=carrot2-mini -Dversion=3.1.0 -Dpackaging=jar 
