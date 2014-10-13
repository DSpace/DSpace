#!/bin/bash
#
#set -euo pipefail
set -e

VERSION="1.7.3-SNAPSHOT";
BIN_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_DIR="$( readlink -e $BIN_DIR/../../)";

echo BIN_DIR: $BIN_DIR
echo REPO_DIR: $REPO_DIR

function install_file_path() {
    FILE="$1"
    TYPE="$2"
    ARTIFACT=`basename $FILE | sed "s/-${VERSION}.${TYPE}//"`;
    GROUP=$(jar tf $FILE | grep "META-INF/maven/" | grep -v "$ARTIFACT" | grep -v pom  | sed 's/META-INF\/maven//g' | perl -pe 's:[/\s]+::g');
    install "$FILE" "$ARTIFACT" "$GROUP" "$TYPE"
}

function install() {
    FILE="$1"
    ARTIFACT="$2"
    GROUP="$3"
    TYPE="$4"
    echo FILE: $FILE
    echo Installing $GROUP $ARTIFACT
    mvn install:install-file            \
        -DgroupId="$GROUP"              \
        -DartifactId="$ARTIFACT"        \
        -Dversion="$VERSION"            \
        -Dpackaging="$TYPE"             \
        -Dfile="$FILE"
    echo Success for mvn: $?
}

# odds and ends
# api-1.7.3-SNAPSHOT-tests.jar
install "$REPO_DIR/dspace/modules/api/target/api-1.7.3-SNAPSHOT-tests.jar" "api" "org.dspace.modules" "test-jar"

# install from dryad-repo dirs starting dspace-*
for f in `ls "$REPO_DIR" | grep "dspace-"`;
do
    for g in `find "$f" -name "*${VERSION}.jar" | grep -v lib`;
    do
        install_file_path "$g" "jar";
    done;
    for g in `find "$f" -name "*${VERSION}.war" | grep -v lib`;
    do
        install_file_path "$g" "war";
    done;
done;

# install from dryad-repo/dspace/modules dirs
for f in `find "$REPO_DIR/dspace/modules" -name "*${VERSION}.jar" | grep -v lib`;
do
    install_file_path "$f" "jar";
done;

