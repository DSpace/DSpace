#!/usr/bin/env bash
set -e
#set -x

TEST_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SERVER="99"
SCREEN="0"
XVFBPID="$TEST_DIR/xvfb.pid"
XVFBARGS=":$SERVER -screen $SCREEN 1024x768x24 -ac +extension GLX +render -noreset"
XVFB="/usr/bin/Xvfb"
export DISPLAY=":${SERVER}.${SCREEN}"

if [ "$LOGNAME" != "vagrant" ]; 
then
    echo Script must be run on vagrant. Exiting.
    exit
fi

if [[ "$1" = "" ]];
then
	echo No test class provided. Exiting. 
	exit
fi
echo Running test class \'$1\'

# run always on exit, wherever failure
function on_exit {
    echo "Stopping Vagrant Xvfb"
    start-stop-daemon --stop --quiet --pidfile $XVFBPID
}
trap on_exit EXIT

# start virtual frame buffer 
echo "Starting xvfb with display $DISPLAY"
start-stop-daemon --start --quiet --pidfile $XVFBPID --make-pidfile --background --exec $XVFB -- $XVFBARGS
echo "Sleeping for xvfb server start"
sleep 3

mvn package -DseleniumTestURL="http://localhost:9999" -Dfirefox_binary="/usr/bin/firefox" -Dfirefox_display="$DISPLAY" -Dtest="$1"

