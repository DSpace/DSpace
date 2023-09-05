echo starting at
date
echo going to index handle $1 with "-f"
./dspace filter-media -f -i $1 > $1idx.out 2> $1idx.err
echo finished at
date
