#!/bin/sh

USAGE="$0 [-native2ascii] cvs-tag version"

# Just in case you need to 'socksify' etc
CVS_COMMAND="cvs"

if [ "$1" = "-native2ascii" ]; then
	NATIVE2ASCII="1"
	shift
fi

# Check we have required command-line arguments
if [ "$#" != "2" ]; then
        echo $USAGE
        exit 1
fi

FILENAME="dspace-language-pack-$2"

mkdir tmp
cd tmp

echo "Checking out language packs with tag $1..."
$CVS_COMMAND -Q export -r $1 language-packs

if [ -n "$NATIVE2ASCII" ]; then
	echo "Convert the UTF-8 encoded files to ASCII (DSpace 1.3.x only)" 
	for i in `find . -name '*.UTF-8'`; do
		destination=`echo $i | sed 's/\.UTF-8//'`
		native2ascii -encoding UTF-8 $i $destination
		rm $i
	done
fi

echo "Creating tarball..."
mv language-packs $FILENAME

tar -cf - $FILENAME | gzip -c > $FILENAME.tar.gz

echo "Cleaning up..."

cd ..
mv tmp/$FILENAME.tar.gz .
rm -r tmp

echo "Package created as $FILENAME.tar.gz"
