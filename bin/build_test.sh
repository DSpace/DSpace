#!/usr/bin/env bash
#

#set -x  # verbose bash
set -e  # return fail on a failed command, for travis-ci

TEST_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BASE_DIR="$TEST_DIR/.."

# non-dspace, standalone pom.xml file for local 
# builds and travis-ci
POM="$BASE_DIR/pom.standalone.xml"
MVN_BASE="mvn -f \"$POM\""
MVN="$MVN_BASE"
#MVN="$MVN_BASE -e"
#MVN="$MVN_BASE --quiet"

# run always on exit, wherever failure
function on_exit {
    # Travis-CI clenup
    if [ "$travis" ]; then
        sh -e /etc/init.d/xvfb stop
    fi
}
trap on_exit EXIT

# build and run junit tests
cd "$BASE_DIR"
$MVN clean package -DskipTests=true
$MVN test

# start virtual frame buffer if in Travis-CI env 
if [ "$travis" ]; then
    echo "Starting xvfb"
    export DISPLAY=:99.0
    sh -e /etc/init.d/xvfb start
    # TODO: ping framebuffer for start instead of sleeping
    sleep 3
fi 

# run selenium/integration tests
$MVN failsafe:integration-test failsafe:verify

