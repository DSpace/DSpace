#
# The contents of this file are subject to the license and copyright
# detailed in the LICENSE and NOTICE files at the root of the source
# tree and available online at
#
# http://www.dspace.org/license/
#

# To build for example use:
# docker build --build-arg POSTGRES_VERSION=13 --build-arg POSTGRES_PASSWORD=mypass ./dspace/src/main/docker/dspace-postgres-pgcrypto-curl/
# This will be published as dspace/dspace-postgres-pgcrypto:$DSPACE_VERSION-loadsql

ARG POSTGRES_VERSION=13
ARG POSTGRES_PASSWORD=dspace

FROM postgres:${POSTGRES_VERSION}

ENV POSTGRES_DB dspace
ENV POSTGRES_USER dspace
ENV POSTGRES_PASSWORD ${POSTGRES_PASSWORD}

# Install curl which is necessary to load SQL file
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Load a SQL dump.  Set LOADSQL to a URL for the sql dump file.
COPY install-pgcrypto.sh /docker-entrypoint-initdb.d/
