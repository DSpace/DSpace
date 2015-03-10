#!/usr/bin/env bash
#

set -e

if [[ "$LOGNAME" != "vagrant" ]]; then
    echo "This script is meant to be run on vagrant. Exiting.";
    exit 1;
fi

JOURNAL_LANDING="$(dirname "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )")"
JAR="journal-landing-webapp-1.7.3-SNAPSHOT-classes.jar"
XMLUI="/opt/dryad/webapps/xmlui/WEB-INF/lib"

pushd `pwd`
cd $JOURNAL_LANDING
mvn clean package -P dev
rm -f "$XMLUI/$JAR"
cp "$JOURNAL_LANDING/journal-landing-webapp/target/$JAR" "$XMLUI/$JAR"
popd

echo
echo "!!!RESTART DRYAD-TOMCAT TO AVOID ERRORS!!!"
echo

