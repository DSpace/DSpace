#!/bin/bash
#
# The contents of this file are subject to the license and copyright
# detailed in the LICENSE and NOTICE files at the root of the source
# tree and available online at
#
# http://www.dspace.org/license/
#

set -e

CHECKFILE=/pgdata/ingest.hasrun.flag

# If $LOADSQL environment variable set, use 'curl' to download that SQL and run it in PostgreSQL
# This can be used to initialize a database based on test data available on the web.
if [ ! -f $CHECKFILE -a ! -z ${LOADSQL} ]
then
  # Download SQL file to /tmp/dspace-db-init.sql
  curl ${LOADSQL} -L -s --output /tmp/dspace-db-init.sql
  # Load into PostgreSQL
  psql -U $POSTGRES_USER < /tmp/dspace-db-init.sql
  # Remove downloaded file
  rm /tmp/dspace-db-init.sql

  touch $CHECKFILE
fi

# If $LOCALSQL environment variable set, then simply run it in PostgreSQL
# This can be used to restore data from a pg_dump or similar.
if [ ! -f $CHECKFILE -a ! -z ${LOCALSQL} ]
then
  # Load into PostgreSQL
  psql -U $POSTGRES_USER < ${LOCALSQL}

  touch $CHECKFILE
fi
