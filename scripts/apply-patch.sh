#!/bin/bash

EXEC_DIR=`pwd`

DRUM_SOURCE_DIR=/apps/git/drum
cd $DRUM_SOURCE_DIR

# If a patch file is specified, apply only that patch
if [ $# = 1 ] ; then
 PATCH_FILE=$1
 if [[ ! ($PATCH_FILE == \/*) ]] ; then
   PATCH_FILE=$EXEC_DIR/$PATCH_FILE
 fi
 git apply --reject --whitespace=fix $PATCH_FILE
 exit 0
fi

# If not patch file is specified, apply all patches under scripts/patches
for patch in `ls $DRUM_SOURCE_DIR/scripts/patches/*.patch`
do
 git apply --reject --whitespace=fix $patch
done
