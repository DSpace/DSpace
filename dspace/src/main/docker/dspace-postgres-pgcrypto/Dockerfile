# This will be deployed as dspace/dspace-postgres-pgcrpyto:latest
FROM postgres

ENV POSTGRES_DB dspace
ENV POSTGRES_USER dspace
ENV POSTGRES_PASSWORD dspace

RUN apt-get update

COPY install-pgcrypto.sh /docker-entrypoint-initdb.d/
