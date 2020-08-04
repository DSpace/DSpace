#
# The contents of this file are subject to the license and copyright
# detailed in the LICENSE and NOTICE files at the root of the source
# tree and available online at
#
# http://www.dspace.org/license/
#

# This will be deployed as dspace/dspace-postgres-pgcrpyto:loadsql
FROM postgres

ENV POSTGRES_DB dspace
ENV POSTGRES_USER dspace
ENV POSTGRES_PASSWORD dspace

# Load a SQL dump.  Set LOADSQL to a URL for the sql dump file.
RUN apt-get update && apt-get install -y curl

COPY install-pgcrypto.sh /docker-entrypoint-initdb.d/
