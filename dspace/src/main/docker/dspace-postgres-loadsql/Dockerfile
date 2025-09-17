#
# The contents of this file are subject to the license and copyright
# detailed in the LICENSE and NOTICE files at the root of the source
# tree and available online at
#
# http://www.dspace.org/license/
#

# Example build:
# docker build --build-arg POSTGRES_VERSION=17 --build-arg POSTGRES_PASSWORD=mypass ./dspace/src/main/docker/dspace-postgres-loadsql/
# This will be published as dspace/dspace-postgres-loadsql:$DSPACE_VERSION

ARG POSTGRES_VERSION=15
ARG POSTGRES_DB=dspace
ARG POSTGRES_USER=dspace
ARG POSTGRES_PASSWORD=dspace

FROM docker.io/postgres:${POSTGRES_VERSION}

ENV POSTGRES_DB=${POSTGRES_DB}
ENV POSTGRES_USER=${POSTGRES_USER}
ENV POSTGRES_PASSWORD=${POSTGRES_PASSWORD}

# Install curl which is necessary to load SQL file
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Load a SQL dump.  Set LOADSQL to a URL for the sql dump file.
COPY loadsql.sh /docker-entrypoint-initdb.d/
