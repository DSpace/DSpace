#!/bin/bash
function usage {
	echo "call with directory name that will contain the created links"
	exit 1
}

if [ "$#" -ne 1 ]; then
	usage
fi

working_dir="$1"

if [ ! -d "$working_dir" ]; then
	mkdir -p $working_dir
fi

dirs="bits config logs scripts sources"

for dir in $dirs; do
	ln -s $(pwd)/$dir $working_dir/$dir
done
