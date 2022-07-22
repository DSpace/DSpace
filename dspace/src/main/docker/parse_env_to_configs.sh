#!/bin/sh
#
# The contents of this file are subject to the license and copyright
# detailed in the LICENSE and NOTICE files at the root of the source
# tree and available online at
#
# http://www.dspace.org/license/
#

#
# This script is used by DSpace 5 to convert/save runtime environment variables used by v6 or 7 into dspace.cfg
# This is necessary because DSpace 5 doesn't support runtime configs like v6 or 7.  All configs are added to dspace.cfg
# during the Maven build process -- local.cfg didn't come about until 6.x
#
# This script passes any params to "[dspace]/bin/dspace", as this is necessary to support "dspace-cli" containers
# as those containers need this script to dynamically update configs and then run a command from commandline.

# Substitute values in dspace.cfg for DSpace 5 from environment variables
# Converts environment variables like "dspace__P__dir" to "dspace.dir" and appends to end of dspace.cfg
VARS=`env | egrep "__.*="|cut -f1 -d=` | sed -e "s/__P__/\./g" | sed -e "s/__D__/-/g"
for v in $VARS
do
  # First write all non-matching lines back to the dspace.cfg
  grep -v "^${v}=" /dspace/config/dspace.cfg > /dspace/config/dspace.cfg
done
# Then write all converted environment variables at the end of dspace.cfg
env | egrep "__.*=" | egrep -v "=.*__" | sed -e "s/__P__/./g" | sed -e "s/__D__/-/g" >> /dspace/config/dspace.cfg

# Optionally call /dspace/bin/dspace with any other params
# This is used by dspace-cli image to run commands after parsing env to config
if [ $# -ne 0 ]
  then
    /dspace/bin/dspace "$@"
fi