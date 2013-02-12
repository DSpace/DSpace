#! /bin/bash

# Clean up after branch. First, tag it with "archive/<branchname".
# Then, remove the branch from both the local system and the origin.

echo Cleaning branch $1
set -e
git tag -f archive/$1 $1
git push origin archive/$1
git branch -d $1
git push origin --delete $1
echo
echo     ====  CLEAN of $1 SUCCEEDED ====
echo
