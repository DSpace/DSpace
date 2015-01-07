#!/usr/bin/env bash
#

# do a check for whether we're in a travis-ci build before setting -o
# as part of bash strict mode
if [ "$TRAVIS" == ""  ]; then
    TRAVIS="";
fi

#set -x  # verbose bash
set -euo pipefail # return fail on a failed command, for travis-ci

ITPORT=2341
TEST_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BASE_DIR="$TEST_DIR/.."
SERVER="99"
SCREEN="0"
XVFBPID="$BASE_DIR/xvfb.pid"
XVFBARGS=":$SERVER -screen $SCREEN 1024x768x24 -ac +extension GLX +render -noreset"
XVFB="/usr/bin/Xvfb"

# non-dspace, standalone pom.xml file for local 
# builds and travis-ci
POM="$BASE_DIR/pom.standalone.xml"
MVN_BASE="mvn -f $POM"
MVN="$MVN_BASE"
#MVN="$MVN_BASE -e"
#MVN="$MVN_BASE --quiet"

# run always on exit, wherever failure
function on_exit {
    # Travis-CI clenup
    if [ "$TRAVIS" ]; then
        echo "Stopping Travis xvfb"
        sh -e /etc/init.d/xvfb stop
    elif [ "$LOGNAME" == "vagrant" ]; then
        echo "Stopping Vagrant Xvfb"
        start-stop-daemon --stop --quiet --pidfile $XVFBPID
    fi
    echo Killing SimpleHTTPServer
    pkill -f SimpleHTTPServer
}
trap on_exit EXIT

# build and run junit tests
cd "$BASE_DIR"
$MVN clean package -DskipTests=true
$MVN test

# start virtual frame buffer if in Travis-CI env 
if [ "$TRAVIS" ] || [ "$LOGNAME" == "vagrant" ]; then
    export DISPLAY=":${SERVER}.${SCREEN}"
    echo "Starting xvfb with display $DISPLAY"
    if [ "$TRAVIS" ]; then
        echo Starting Travis-CI xvfb
        sh -e /etc/init.d/xvfb start
    elif [ "$LOGNAME" == "vagrant" ]; then
        echo Starting Vagrant Xvfb
        start-stop-daemon --start --quiet --pidfile $XVFBPID --make-pidfile --background --exec $XVFB -- $XVFBARGS
    fi
    # TODO: ping framebuffer for start instead of sleeping
    echo "Sleeping for xvfb server start"
    sleep 3
fi 

# run selenium/integration tests
echo "Starting SimpleHTTPServer"
python -m SimpleHTTPServer $ITPORT > /dev/null 2>&1 &
sleep 5 
if [ "$?" -eq "0"  ]; then
    echo Running integration tests
    $MVN failsafe:integration-test failsafe:verify -DseleniumTestURL="http://localhost:$ITPORT/dryad-widgets-webapp/src/test/java/it"
    echo Integration tests complete
else
    echo "Failed to start IT test server";
    exit 1;
fi

