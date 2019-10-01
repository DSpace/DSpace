# Docker images supporting DSpace

## dspace-postgres-pgcrypto/Dockerfile

_dspace/dspace-postgres-pgcrypto:latest_

This is a postgres docker image containing the pgcrypto extension used in DSpace 6 and DSpace 7.

## dspace-postgres-pgcrypto-curl/Dockerfile

_dspace/dspace-postgres-pgcrypto:loadsql_

This is a postgres docker image containing the pgcrypto extension used in DSpace 6 and DSpace 7.
This image also contains curl.  The image is pre-configured to load a postgres database dump on initialization.

## solr

_dspace/dspace-solr_

This is a standalone solr image containing DSpace solr schemas used in DSpace 7.

## local.cfg and test/ folder

These resources are bundled into the _dspace/dspace_ image at build time.
