#!/bin/bash
FILE=do_debug.txt
if [ -f "$FILE" ]; then
  export JPDA_ADDRESS="*:8000"
  ./catalina.sh jpda run
else
  export JPDA_ADDRESS="localhost:8000"
  ./catalina.sh run
fi
