# Docker images supporting DSpace

## Dockerfile.dependencies

This Dockerfile is used to pre-cache Maven dependency downloads that will be used in subsequent DSpace docker builds.
```
docker build -t dspace/dspace-dependencies:dspace-7_x -f Dockerfile.dependencies .
```

**This image is built manually.**  It should be rebuilt each year or after each major release in order to refresh the cache of jars.

A corresponding image exists for DSpace 4-6.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace-dependencies:dspace-7_x
```

## Dockerfile.test

This Dockerfile builds a DSpace 7 Tomcat image (for testing/development).
This image deploys two DSpace webapps:
1. The DSpace 7 REST API (at `http://localhost:8080/server`)
2. The legacy (v6) REST API (at `http://localhost:8080//rest`), deployed without requiring HTTPS access.

```
docker build -t dspace/dspace:dspace-7_x-test -f Dockerfile.test .
```

This image is built *automatically* after each commit is made to the `master` branch.

A corresponding image exists for DSpace 4-6.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace:dspace-7_x-test
```

## Dockerfile

This Dockerfile builds a DSpace 7 tomcat image.
This image deploys two DSpace webapps:
1. The DSpace 7 REST API (at `http://localhost:8080/server`)
2. The legacy (v6) REST API (at `http://localhost:8080//rest`), deployed *requiring* HTTPS access.
```
docker build -t dspace/dspace:dspace-7_x -f Dockerfile .
```

This image is built *automatically* after each commit is made to the `master` branch.

A corresponding image exists for DSpace 4-6.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace:dspace-7_x
```

## Dockefile.cli

This Dockerfile builds a DSpace 7 CLI image, which can be used to run commandline tools via Docker.
```
docker build -t dspace/dspace-cli:dspace-7_x -f Dockerfile.cli .
```

This image is built *automatically* after each commit is made to the master branch.

A corresponding image exists for DSpace 6.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace-cli:dspace-7_x
```

## dspace/src/main/docker/dspace-postgres-pgcrypto/Dockerfile

This is a PostgreSQL Docker image containing the `pgcrypto` extension required by DSpace 6+.
```
cd dspace/src/main/docker/dspace-postgres-pgcrypto
docker build -t dspace/dspace-postgres-pgcrypto .
```

**This image is built manually.**  It should be rebuilt as needed.

A copy of this file exists in the DSpace 6 branch.  A specialized version of this file exists for DSpace 4 in DSpace-Docker-Images.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace-postgres-pgcrypto
```

## dspace/src/main/docker/dspace-postgres-pgcrypto-curl/Dockerfile

This is a PostgreSQL Docker image containing the `pgcrypto` extension required by DSpace 6+.
This image also contains `curl`.  The image is pre-configured to load a Postgres database dump on initialization.
```
cd dspace/src/main/docker/dspace-postgres-pgcrypto-curl
docker build -t dspace/dspace-postgres-pgcrypto:loadsql .
```

**This image is built manually.**   It should be rebuilt as needed.

A copy of this file exists in the DSpace 6 branch.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace-postgres-pgcrypto:loadsql
```

## dspace/src/main/docker/solr/Dockerfile

This is a standalone Solr image containing DSpace Solr cores & schemas required by DSpace 7.

**WARNING:** Rebuilding this image first **requires** rebuilding `dspace-7_x` (i.e. `Dockerfile` listed above),
as this Solr image copies the latest DSpace-specific Solr schemas & settings from that other image.

```
# First, rebuild dspace-7_x to grab the latest Solr configs
cd [src]
docker build -t dspace/dspace:dspace-7_x -f Dockerfile .

# Then, rebuild dspace-solr based on that build of DSpace 7.
cd dspace/src/main/docker/solr
docker build -t dspace/dspace-solr .
```

**This image is built manually.**  It should be rebuilt when Solr schemas change or as new releases of Solr are incorporated.

This file was introduced for DSpace 7.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace-solr
```

## local.cfg and test/ folder

These resources are bundled into the `dspace/dspace` image at build time.
