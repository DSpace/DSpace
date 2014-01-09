#! /bin/bash
echo Running selenium tests....
set -e
mvn package -DseleniumTestURL="$1"
