#!/usr/bin/env bash
#
# These DSpace jars/wars must be available to Maven during a 
# local (module-only) build of the journal-landing module.
#
# This script must be run from the dryad-repo/ directory.
#

if [[ "$LOGNAME" != "vagrant" ]]; then
    echo "This script is meant to be run on vagrant. Exiting.";
    exit 1;
fi

DIR="$(basename $(pwd))"
if [[ "$DIR" != "dryad-repo" ]]; then 
    echo "Run this script from the dryad-repo/ directory";
    exit 1;
fi

mvn install:install-file -DgroupId=com.atmire -DartifactId=atmire-workflow-api -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar -Dfile=dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/lib/atmire-workflow-api-1.7.3-SNAPSHOT.jar
mvn install:install-file -DgroupId=com.atmire -DartifactId=atmire-workflow-xmlui-api -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar -Dfile=dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/lib/atmire-workflow-xmlui-api-1.7.3-SNAPSHOT.jar
mvn install:install-file -DgroupId=org.dspace -DartifactId=dspace-api -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar -Dfile=dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/lib/dspace-api-1.7.3-SNAPSHOT.jar
mvn install:install-file -DgroupId=org.dspace -DartifactId=dspace-stats -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar -Dfile=dspace-stats/target/dspace-stats-1.7.3-SNAPSHOT.jar
mvn install:install-file -DgroupId=org.dspace -DartifactId=dspace-xmlui-api -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar -Dfile=dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/lib/dspace-xmlui-api-1.7.3-SNAPSHOT.jar
mvn install:install-file -DgroupId=org.dspace -DartifactId=dspace-xmlui-webapp -Dversion=1.7.3-SNAPSHOT -Dpackaging=war -Dfile=dspace-xmlui/dspace-xmlui-webapp/target/dspace-xmlui-webapp-1.7.3-SNAPSHOT.war
mvn install:install-file -DgroupId=org.dspace -DartifactId=dspace-xmlui-wing -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar -Dfile=dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/lib/dspace-xmlui-wing-1.7.3-SNAPSHOT.jar
mvn install:install-file -DgroupId=org.dspace.modules -DartifactId=api -Dversion=1.7.3-SNAPSHOT -Dclassifier=tests -Dpackaging=test-jar -Dfile=dspace/modules/api/target/api-1.7.3-SNAPSHOT-tests.jar
mvn install:install-file -DgroupId=org.dspace.modules -DartifactId=api -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar -Dfile=dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/lib/api-1.7.3-SNAPSHOT.jar
mvn install:install-file -DgroupId=org.dspace.modules -DartifactId=api-stats -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar -Dfile=dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/lib/api-stats-1.7.3-SNAPSHOT.jar
mvn install:install-file -DgroupId=org.dspace.modules -DartifactId=bagit-api -Dversion=0.0.1 -Dpackaging=jar -Dfile=dspace/modules/bagit/dspace-bagit-api/target/bagit-api-0.0.1.jar
mvn install:install-file -DgroupId=org.dspace.modules -DartifactId=doi-service -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar -Dfile=dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/lib/doi-service-1.7.3-SNAPSHOT.jar
mvn install:install-file -DgroupId=org.dspace.modules -DartifactId=identifier-services -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar -Dfile=dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/lib/identifier-services-1.7.3-SNAPSHOT.jar
mvn install:install-file -DgroupId=org.dspace.modules -DartifactId=payment-api -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar -Dfile=dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/lib/payment-api-1.7.3-SNAPSHOT.jar
mvn install:install-file -DgroupId=org.dspace.modules -DartifactId=payment-webapp -Dversion=1.7.3-SNAPSHOT -Dclassifier=classes -Dpackaging=jar -Dfile=dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/webapps/xmlui/WEB-INF/lib/payment-webapp-1.7.3-SNAPSHOT-classes.jar
mvn install:install-file -DgroupId=org.dspace.modules -DartifactId=payment-webapp -Dversion=1.7.3-SNAPSHOT -Dpackaging=war -Dfile=dspace/modules/payment-system/payment-webapp/target/payment-webapp-1.7.3-SNAPSHOT.war
mvn install:install-file -DgroupId=org.dspace.modules -DartifactId=versioning-api -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar -Dfile=dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/lib/versioning-api-1.7.3-SNAPSHOT.jar
mvn install:install-file -DgroupId=org.dspace.modules -DartifactId=versioning-webapp -Dversion=1.7.3-SNAPSHOT -Dclassifier=classes -Dpackaging=jar -Dfile=dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/webapps/xmlui/WEB-INF/lib/versioning-webapp-1.7.3-SNAPSHOT-classes.jar
mvn install:install-file -DgroupId=org.dspace.modules -DartifactId=versioning-webapp -Dversion=1.7.3-SNAPSHOT -Dpackaging=war -Dfile=dspace/modules/versioning/versioning-webapp/target/versioning-webapp-1.7.3-SNAPSHOT.war

