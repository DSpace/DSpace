#!/usr/bin/env bash
#==============================================================================
#
#         FILE:  import-setup.sh
#        USAGE:  import-setup.sh [file] [handle]
#  DESCRIPTION:  Given a filename and DSpace handle, setup the directories 
#                and files needed for adding the file to the object denoted
#                by the handle using the dspace itemupdate command.
#       AUTHOR:  Joshua A. Westgard
#      CREATED:  2016.02.03
#      VERSION:  1
#
#==============================================================================
FILE=$1
HANDLE=$2
OUTDIR="load$(date +%Y%m%d)/item1"
DC="$OUTDIR/dublin_core.xml"

#----------------------------------------------------------------------
# create directories and files, and move content file into place
#----------------------------------------------------------------------
echo "Setting up DSpace file import..."
if [ ! -f $FILE ]; then
  echo "Cannot locate specified file. Exiting."
  exit 1
fi
mkdir -p "$OUTDIR"
mv $FILE "$OUTDIR/$FILE"
echo "$FILE" > "$OUTDIR/contents" && echo "  - created contents file;"
echo "$HANDLE" > "$OUTDIR/handle" && echo "  - created handle file;"

#----------------------------------------------------------------------
# create minimal dublin_core.xml containing URL of obj to be updated
#----------------------------------------------------------------------
echo '<?xml version="1.0" encoding="utf-8" standalone="no"?>' > "$DC"
echo '<dublin_core schema="dc">' >> "$DC"
echo "<dcvalue element=\"identifier\" qualifier=\"uri\">$HANDLE</dcvalue>" >> "$DC"
echo '</dublin_core>' >> "$DC"
echo "  - created dublin_core.xml;"
echo "Done."
