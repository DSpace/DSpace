#! /bin/bash

if [ ! -d work ] ; then
  mkdir work;
fi

# Go to root
cd ..

outFile=upgrade/work/additions.list
find dspace/modules/additions -type f | grep -v pom.xml | grep -v "\/edu\/umd\/"  | grep -v "\/bin\/src\/"| grep -v "/\." | grep -v "/target/" | sed 's/^dspace\/modules\/additions/dspace-api/g' > $outFile

modules="lni oai rdf rest xmlui xmlui-mirage2"
for module in $modules
do
  outFile=upgrade/work/$module.list
  find dspace/modules/$module -type f | grep -v pom.xml | grep -v "\/edu\/umd\/"  | grep -v "\/bin\/src\/" | grep -v "/\." | grep -v "/target/" | sed 's/^dspace\/modules\//dspace-/g' | sed 's/webapp\/themes\/Mirage2/webapp/g'> $outFile

done


cd - > /dev/null
