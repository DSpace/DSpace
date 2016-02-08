#!/bin/bash

gitHome=..
cd $gitHome
if [ $# = 1 ] ; then
 git apply --reject --whitespace=fix  upgrade/$1
 exit 0
fi

for patch in `ls $gitHome/upgrade/patches/*.patch`
do
 git apply --reject --whitespace=fix $patch
done
