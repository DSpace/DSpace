#! /bin/bash
echo Running selenium tests....
set -e
mvn package -DseleniumTestURL="$1"

# ===============================
# currently test/DescribePublicationAJAXTest checks flags for
# which browser to test, invoked as below. Testing Chrome requires
# a driver binary.
#URL="http://localhost:9999"
#BROWSER="Chrome"
#BROWSER="Firefox"
#BROWSER="Safari"
#DRIVER="bin/chromedriver-osx-2.10"
#
#mvn package -DseleniumTestURL=$URL -DseleniumTestBrowser=$BROWSER -Dtest=DescribePublicationAJAXTest -Dwebdriver.chrome.driver=$DRIVER

