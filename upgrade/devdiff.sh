#!/bin/bash

gitHome=..

upgradeBranch=feature/LIBDRUM-331

# Go to root
cd $gitHome

if [ ! -d upgrade/patches ] ; then
  mkdir upgrade/patches
fi

for listFile in `ls upgrade/work/*.list`
do
  echo Processing $listFile
  listName=`echo $listFile | rev | cut -d"/" -f1 | rev`
  echo > upgrade/patches/$listName.patch
  for file in `cat $listFile`
  do
    git diff drum-develop..$upgradeBranch $file >> upgrade/patches/$listName.patch
  done
  
  sed -i -- 's/a\/dspace-api\//a\/dspace\/modules\/additions\//g' upgrade/patches/$listName.patch
  sed -i -- 's/b\/dspace-api\//b\/dspace\/modules\/additions\//g' upgrade/patches/$listName.patch

  modules="jspui lni oai rdf rest solr sword swordv2 xmlui"
  for module in $modules
  do
    sed -i -- 's/a\/dspace-'$module'\//a\/dspace\/modules\/'$module'\//g' upgrade/patches/$listName.patch
    sed -i -- 's/b\/dspace-'$module'\//b\/dspace\/modules\/'$module'\//g' upgrade/patches/$listName.patch
  done
  
done
cd -
