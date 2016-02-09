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
  for file in `cat $listFile`
  do
    git diff drum-develop..$upgradeBranch $file >> upgrade/patches/$listName
  done
  
  sed -i .patch 's/\/dspace-api\//\/dspace\/modules\/additions\//g' upgrade/patches/$listName

  modules="lni oai rdf rest swordv2 xmlui xmlui-mirage2"
  for module in $modules
  do
    if [ "$listName" != "xmlui-mirage2.list" ]  ; then
      echo "111111111111: $listName"
      sed 's/\/dspace-'$module'\//\/dspace\/modules\/'$module'\//g' < upgrade/patches/$listName | cat > upgrade/patches/$listName.patch
    fi
    if [ "$listName" = "xmlui-mirage2.list" ] ; then
      echo "22222222: $listName"
      sed 's/\/dspace-'$module'\//\/dspace\/modules\/'$module'\//g' < upgrade/patches/$listName | sed 's/main\/webapp\//main\/webapp\/themes\/Mirage2\//g' | cat > upgrade/patches/$listName.patch
      #sed -i .tmp 's/\/dspace-'$module'\//\/dspace\/modules\/'$module'\//g' upgrade/patches/$listName
      #sed -i .patch 's/main\/webapp\//main\/webapp\/themes\/Mirage2\//g' upgrade/patches/$listName.tmp
      #rm upgrade/patches/$listName.tmp
    fi
  done

  rm upgrade/patches/$listName
  
done
cd - > /dev/null
