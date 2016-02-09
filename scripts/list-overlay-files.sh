#! /bin/bash

# This script creates a list all overlay files that needs to be patched during dspace version upgrade.
#
# The list includes all files except:
#  - pom.xml (overlay module pom files are different from their corresponding base modules - i.e dspace-xmlui/pom.xml is differnt from dspace/modules/xmlui/pom.xml)
#  - paths that contain /edu/umd (these umd only files will not exist in base modules)
#  - paths that contain /. (files/directories that start with '.' - usually system/tool generated files) 
#  - paths that contain /target/
#
# The paths in the list are updated from the overlay to their corresponding files in the base modules which can be used by the create-patch.sh script to generate the patches.

if [ ! -d work ] ; then
  mkdir work;
fi

DRUM_SOURCE_DIR=/apps/git/drum

# Go to root
cd $DRUM_SOURCE_DIR

outFile=scripts/work/additions.list
find dspace/modules/additions -type f | grep -v pom.xml | grep -v "\/edu\/umd\/"  | grep -v "\/bin\/src\/"| grep -v "/\." | grep -v "/target/" | sed 's/^dspace\/modules\/additions/dspace-api/g' > $outFile

modules="lni oai rdf rest xmlui xmlui-mirage2"
for module in $modules
do
  outFile=scripts/work/$module.list
  find dspace/modules/$module -type f | grep -v pom.xml | grep -v "\/edu\/umd\/"  | grep -v "\/bin\/src\/" | grep -v "/\." | grep -v "/target/" | sed 's/^dspace\/modules\//dspace-/g' | sed 's/webapp\/themes\/Mirage2/webapp/g'> $outFile

done

has_non_empty_list=false
# Delete empty lists
for list in `ls $DRUM_SOURCE_DIR/scripts/work/*.list`
do
  if [ `wc -l < $list | tr -d ' '` = 0 ] ; then
    rm $list
  else
    echo "Created file list: $list"
    has_non_empty_list=true
  fi
done

if $has_non_empty_list ; then
  echo "Completed generating files list"
  echo 
  echo "Run the scripts/create-patch.sh script to create patches for the generated files"
else
  echo "No file list was generated"
fi
