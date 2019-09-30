# This will be deployed as dspace/dspace-postgres-pgcrpyto:loadsql
FROM postgres

ENV POSTGRES_DB dspace
ENV POSTGRES_USER dspace
ENV POSTGRES_PASSWORD dspace

# Load a SQL dump.  Set LOADSQL to a URL for the sql dump file.
RUN apt-get update && apt-get install -y curl

COPY install-pgcrypto.sh /docker-entrypoint-initdb.d/
