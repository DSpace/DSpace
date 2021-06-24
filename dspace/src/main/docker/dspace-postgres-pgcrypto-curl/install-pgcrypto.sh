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

if [ ! -f $CHECKFILE -a ! -z ${LOADSQL} ]
then
  curl ${LOADSQL} -L -s --output /tmp/dspace.sql
  psql -U $POSTGRES_USER < /tmp/dspace.sql

  touch $CHECKFILE
  exit
fi

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
