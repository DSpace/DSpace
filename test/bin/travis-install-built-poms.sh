#!/bin/bash
#
#set -euo pipefail
set -e

VERSION="1.7.3-SNAPSHOT";
BIN_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_DIR="$( readlink -e $BIN_DIR/../../)";

echo BIN_DIR: $BIN_DIR
echo REPO_DIR: $REPO_DIR

function install_file() {
    FILE="$1"
    ARTIFACT=`basename $g | sed "s/-${VERSION}.jar//"`;
    GROUP=$(jar tf $FILE | grep "META-INF/maven/" | grep -v "$ARTIFACT" | grep -v pom  | sed 's/META-INF\/maven//g' | perl -pe 's:[/\s]+::g');
    echo Installing $GROUP $ARTIFACT
    echo FILE: $FILE
    mvn install:install-file            \
        -DgroupId="$GROUP"              \
        -DartifactId="$ARTIFACT"        \
        -Dversion="$VERSION"            \
        -Dpackaging=jar                 \
        -Dfile="$FILE"
    echo Success for mvn: $?
}

# install from dryad-repo dirs starting dspace-*
for f in `ls "$REPO_DIR" | grep "dspace-"`;
do
    for g in `find "$f" -name "*${VERSION}.jar" | grep -v lib`;
    do
        install_file "$g";
    done;
done;

# install from dryad-repo/dspace/modules dirs
for f in `find "$REPO_DIR/dspace/modules" -name "*${VERSION}.jar" | grep -v lib`;
do
    install_file "$f";
done;

