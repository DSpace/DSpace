#!/bin/bash
#
set -euo pipefail

VERSION="1.7.3-SNAPSHOT";
BIN_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_DIR="$( readlink -e $BIN_DIR/../../)";

echo BIN_DIR: $BIN_DIR
echo REPO_DIR: $REPO_DIR

function install_file() {
    GROUP="$1"
    ARTIFACT="$2"
    VERSION="$3"
    FILE="$4"
    echo Installing $GROUP $ARTIFACT
    mvn install:install-file            \
        -DgroupId="$GROUP"              \
        -DartifactId="$ARTIFACT"        \
        -Dversion="$VERSION"            \
        -Dpackaging=jar                 \
        -Dfile="$FILE"
    echo Success for mvn: $?
}

for f in `ls "$REPO_DIR" | grep "dspace-"`;
do
    for g in `find "$f" -name "*${VERSION}.jar" | grep -v lib`;
    do
        ARTIFACT=`basename $g | sed "s/-${VERSION}.jar//"`;
        install_file "org.dspace" "$ARTIFACT" "$VERSION" "$g";
    done;
done;

for f in `find "$REPO_DIR/dspace/modules" -name "*${VERSION}.jar" | grep -v lib`;
do
    ARTIFACT=`basename $f | sed "s/-${VERSION}.jar//"`;
    install_file "org.dspace.modules" "$ARTIFACT" "$VERSION" "$f";
done;

