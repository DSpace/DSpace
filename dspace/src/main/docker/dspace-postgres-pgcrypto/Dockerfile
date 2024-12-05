#
# The contents of this file are subject to the license and copyright
# detailed in the LICENSE and NOTICE files at the root of the source
# tree and available online at
#
# http://www.dspace.org/license/
#

# To build for example use:
# docker build --build-arg POSTGRES_VERSION=13 --build-arg POSTGRES_PASSWORD=mypass ./dspace/src/main/docker/dspace-postgres-pgcrypto/
# This will be published as dspace/dspace-postgres-pgcrypto:$DSPACE_VERSION

ARG POSTGRES_VERSION=15
ARG POSTGRES_PASSWORD=dspace

FROM docker.io/postgres:${POSTGRES_VERSION}
LABEL org.opencontainers.image.source="https://github.com/DSpace/DSpace"
LABEL org.opencontainers.image.description="DSpace PostgreSQL image. This is a custom version of the official \
'postgres' image, which includes the pgcrypto extension which is required by DSpace."
LABEL org.opencontainers.image.licenses="BSD-3-Clause"

ENV POSTGRES_DB=dspace
ENV POSTGRES_USER=dspace
ENV POSTGRES_PASSWORD=${POSTGRES_PASSWORD}

# Copy over script which will initialize database and install pgcrypto extension
COPY install-pgcrypto.sh /docker-entrypoint-initdb.d/
