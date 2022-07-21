#!/bin/sh

# Substitute values in dspace.cfg for DSpace 5 from environment variables
VARS=`env | egrep "__.*="|cut -f1 -d=` | sed -e "s/__P__/\./g" | sed -e "s/__D__/-/g"
for v in $VARS
do
  grep -v "^${v}=" /dspace/config/dspace.cfg > /dspace/config/dspace.cfg
done
env | egrep "__.*=" | egrep -v "=.*__" | sed -e "s/__P__/./g" | sed -e "s/__D__/-/g" >> /dspace/config/dspace.cfg