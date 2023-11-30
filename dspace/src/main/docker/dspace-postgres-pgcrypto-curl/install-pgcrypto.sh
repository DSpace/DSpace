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
  exit
fi

# If $LOCALSQL environment variable set, then simply run it in PostgreSQL
# This can be used to restore data from a pg_dump or similar.
if [ ! -f $CHECKFILE -a ! -z ${LOCALSQL} ]
then
  # Load into PostgreSQL
  psql -U $POSTGRES_USER < ${LOCALSQL}

  touch $CHECKFILE
  exit
fi

# Then, setup pgcrypto on this database
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
  -- Create a new schema in this database named "extensions" (or whatever you want to name it)
  CREATE SCHEMA extensions;
  -- Enable this extension in this new schema
  CREATE EXTENSION pgcrypto SCHEMA extensions;
  -- Update your database's "search_path" to also search the new "extensions" schema.
  -- You are just appending it on the end of the existing comma-separated list.
  ALTER DATABASE dspace SET search_path TO "\$user",public,extensions;
  -- Grant rights to call functions in the extensions schema to your dspace user
  GRANT USAGE ON SCHEMA extensions TO $POSTGRES_USER;
EOSQL
