#!/bin/bash
set -e

# Retrieve the directory where the script resides
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# Switch to the script directory
cd $SCRIPT_DIR

# Run pg_restore against any file in the directory with a ".dump" extension
find . -name '*.dump' -exec pg_restore --username="$POSTGRES_USER" --no-owner --dbname="$POSTGRES_DB" --verbose {} \;
