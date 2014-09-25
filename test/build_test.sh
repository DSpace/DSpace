#!/usr/bin/env bash
#

TEST_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BASE_DIR="$TEST_DIR/.."

# build and run junit tests
cd "$BASE_DIR"
mvn --quiet package -DskipTests=false

# run Selenium tests
export DISPLAY=:99.0
sh -e /etc/init.d/xvfb start
sleep 3
echo
sleep 3


