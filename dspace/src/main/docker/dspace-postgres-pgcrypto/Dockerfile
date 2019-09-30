#
# The contents of this file are subject to the license and copyright
# detailed in the LICENSE and NOTICE files at the root of the source
# tree and available online at
#
# http://www.dspace.org/license/
#

# This will be deployed as dspace/dspace-postgres-pgcrpyto:latest
FROM postgres

ENV POSTGRES_DB dspace
ENV POSTGRES_USER dspace
ENV POSTGRES_PASSWORD dspace

RUN apt-get update

COPY install-pgcrypto.sh /docker-entrypoint-initdb.d/
