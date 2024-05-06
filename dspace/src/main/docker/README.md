# Docker images supporting DSpace Backend

***
:warning: **THESE IMAGES ARE NOT PRODUCTION READY**  The below Docker Compose images/resources were built for development/testing only.  Therefore, they may not be fully secured or up-to-date, and should not be used in production.

If you wish to run DSpace on Docker in production, we recommend building your own Docker images. You are welcome to borrow ideas/concepts from the below images in doing so. But, the below images should not be used "as is" in any production scenario.
***

## Overview
The Dockerfiles in this directory (and subdirectories) are used by our [Docker Compose scripts](../docker-compose/README.md).

## Dockerfile.dependencies (in root folder)

This Dockerfile is used to pre-cache Maven dependency downloads that will be used in subsequent DSpace docker builds.
Caching these Maven dependencies provides a speed increase to all later builds by ensuring the dependencies
are only downloaded once.

```
docker build -t dspace/dspace-dependencies:latest -f Dockerfile.dependencies .
```

This image is built *automatically* after each commit is made to the `main` branch.

Admins to our DockerHub repo can manually publish with the following command.
```
docker push dspace/dspace-dependencies:latest
```

## Dockerfile.test (in root folder)

This Dockerfile builds a DSpace REST API backend image (for testing/development).
This image deploys one webapp to Tomcat running in Docker:
1. The DSpace REST API (at `http://localhost:8080/server`)
This image also sets up debugging in Tomcat for development.

```
docker build -t dspace/dspace:latest-test -f Dockerfile.test .
```

This image is built *automatically* after each commit is made to the `main` branch.

Admins to our DockerHub repo can manually publish with the following command.
```
docker push dspace/dspace:latest-test
```

## Dockerfile (in root folder)

This Dockerfile builds a DSpace REST API backend image.
This image deploys one DSpace webapp to Tomcat running in Docker:
1. The DSpace REST API (at `http://localhost:8080/server`)

```
docker build -t dspace/dspace:latest -f Dockerfile .
```

This image is built *automatically* after each commit is made to the `main` branch.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace:latest
```

## Dockerfile.cli (in root folder)

This Dockerfile builds a DSpace CLI (command line interface) image, which can be used to run DSpace's commandline tools via Docker.
```
docker build -t dspace/dspace-cli:latest -f Dockerfile.cli .
```

This image is built *automatically* after each commit is made to the `main` branch.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace-cli:latest
```

## ./dspace-postgres-pgcrypto/Dockerfile

This is a PostgreSQL Docker image containing the `pgcrypto` extension required by DSpace 6+.
This image is built *automatically* after each commit is made to the `main` branch.

How to build manually:
```
cd dspace/src/main/docker/dspace-postgres-pgcrypto
docker build -t dspace/dspace-postgres-pgcrypto:latest .
```

It is also possible to change the version of PostgreSQL or the PostgreSQL user's password during the build:
```
cd dspace/src/main/docker/dspace-postgres-pgcrypto
docker build -t dspace/dspace-postgres-pgcrypto:latest --build-arg POSTGRES_VERSION=11 --build-arg POSTGRES_PASSWORD=mypass .
```

Admins to our DockerHub repo can (manually) publish with the following command.
```
docker push dspace/dspace-postgres-pgcrypto:latest
```

## ./dspace-postgres-pgcrypto-curl/Dockerfile

This is a PostgreSQL Docker image containing the `pgcrypto` extension required by DSpace 6+.
This image also contains `curl`.  The image is pre-configured to load a Postgres database dump on initialization.

This image is built *automatically* after each commit is made to the `main` branch.

How to build manually:
```
cd dspace/src/main/docker/dspace-postgres-pgcrypto-curl
docker build -t dspace/dspace-postgres-pgcrypto:latest-loadsql .
```

Similar to `dspace-postgres-pgcrypto` above, you can also modify the version of PostgreSQL or the PostgreSQL user's password.
See examples above.

Admins to our DockerHub repo can (manually) publish with the following command.
```
docker push dspace/dspace-postgres-pgcrypto:latest-loadsql
```

## ./dspace-shibboleth/Dockerfile

This is a test / demo image which provides an Apache HTTPD proxy (in front of Tomcat)
with `mod_shib` & Shibboleth installed based on the
[DSpace Shibboleth configuration instructions](https://wiki.lyrasis.org/display/DSDOC7x/Authentication+Plugins#AuthenticationPlugins-ShibbolethAuthentication).
It is primarily for usage for testing DSpace's Shibboleth integration.
It uses https://samltest.id/ as the Shibboleth IDP

**This image is built manually.**   It should be rebuilt as needed.

```
cd dspace/src/main/docker/dspace-shibboleth
docker build -t dspace/dspace-shibboleth .

# Test running it manually
docker run -i -t -d -p 80:80 -p 443:443 dspace/dspace-shibboleth
```

This image can also be rebuilt using the `../docker-compose/docker-compose-shibboleth.yml` script.

## ./dspace-solr/Dockerfile

This Dockerfile builds a Solr image with DSpace Solr configsets included. It
can be pulled / built following the [docker compose resources](../docker-compose/README.md)
documentation. Or, to just build and/or run Solr:

```bash
docker-compose build dspacesolr
docker-compose -p d8 up -d dspacesolr
```

If you're making iterative changes to the DSpace Solr configsets you'll need to rebuild /
restart the `dspacesolr` container for the changes to be deployed. From DSpace root:

```bash
docker-compose -p d8 up --detach --build dspacesolr
```

## ./test/ folder

These resources are bundled into the `dspace/dspace:dspace-*-test` image at build time.
See the `Dockerfile.test` section above for more information about the test image.


## Debugging Docker builds

When updating or debugging Docker image builds, it can be useful to briefly
spin up an "intermediate container".  Here's how to do that:
```
# First find the intermediate container/image ID in your commandline logs
docker run -i -t [container-id] /bin/bash
```
