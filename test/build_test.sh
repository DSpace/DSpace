#!/usr/bin/env bash
#

set -x  # verbose bash
set -e  # return fail on a failed command, for travis-ci

TEST_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BASE_DIR="$TEST_DIR/.."

MVN="mvn -e"
#MVN="mvn --quiet"

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
$MVN failsafe:integration-test -DskipTests=true

# Travis-CI clenup
if [ "$travis" ]; then
    sh -e /etc/init.d/xvfb stop
fi

