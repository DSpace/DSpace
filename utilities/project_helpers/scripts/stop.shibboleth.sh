#!/bin/bash

countOfMatch=`ps -ejH | grep "shibd" -c` 
while [ $countOfMatch -gt 0 ]; do
   numberOfProcess=`ps -ejH | grep "shibd" | sed 's/ *\([0-9]*\).*/\1/'`
   kill $numberOfProcess
   echo "Shibd process ($numberOfProcess) killed"
   sleep 1;
   countOfMatch=`ps -ejH | grep "shibd" -c` 
done
echo "Shibboleth stopped!"
