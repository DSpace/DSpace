# Docker images supporting DSpace

<<<<<<< HEAD
## Dockerfile.dependencies

This dockerfile is used to pre-cache maven downloads that will be used in subsequent DSpace docker builds.
```
docker build -t dspace/dspace-dependencies:dspace-6_x -f Dockerfile.dependencies .
```

This image is built manually.  It should be rebuilt each year or after each major release in order to refresh the cache of jars.  

A corresponding image exists for the other DSpace branches.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace-dependencies:dspace-6_x
```

## Dockerfile.jdk8-test

This dockefile builds a DSpace 6 tomcat image.  
- The DSpace 6 REST api will be deployed without requiring https access.
- The localhost restriction for SOLR will be bypassed.

```
docker build -t dspace/dspace:dspace-6_x-jdk8-test -f Dockerfile.jdk8-test .
```

This image is built automatically after each commit is made to the master branch.

A corresponding image exists for the other DSpace branches.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace:dspace-6_x-jdk8-test
```

## Dockerfile.jdk8

This dockefile builds a DSpace 7 tomcat image.
```
docker build -t dspace/dspace:dspace-6_x-jdk8 -f Dockerfile.jdk8 .
```

This image is built automatically after each commit is made to the master branch.

A corresponding image exists for the other DSpace branches.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace:dspace-6_x-jdk8
```

## Dockefile.cli.jdk8

This dockerfile builds a DSpace 6 CLI image.
```
docker build -t dspace/dspace-cli:dspace-6_x -f Dockerfile.cli.jdk8 .
```

This image is built automatically after each commit is made to the master branch.
=======
***
:warning: **NOT PRODUCTION READY**  The below Docker images/resources are not guaranteed "production ready" at this time. They have been built for development/testing only. Therefore, DSpace Docker images may not be fully secured or up-to-date. While you are welcome to base your own images on these DSpace images/resources, these should not be used "as is" in any production scenario.
***

## Dockerfile.dependencies

This Dockerfile is used to pre-cache Maven dependency downloads that will be used in subsequent DSpace docker builds.
```
docker build -t dspace/dspace-dependencies:dspace-7_x -f Dockerfile.dependencies .
```

This image is built *automatically* after each commit is made to the `main` branch.

A corresponding image exists for DSpace 4-6.

Admins to our DockerHub repo can manually publish with the following command.
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

This image is built *automatically* after each commit is made to the `main` branch.

A corresponding image exists for DSpace 4-6.

Admins to our DockerHub repo can manually publish with the following command.
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

This image is built *automatically* after each commit is made to the `main` branch.

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

This image is built *automatically* after each commit is made to the `main` branch.
>>>>>>> dspace-7.2.1

A corresponding image exists for DSpace 6.

Admins to our DockerHub repo can publish with the following command.
```
<<<<<<< HEAD
docker push dspace/dspace-cli:dspace-6_x
=======
docker push dspace/dspace-cli:dspace-7_x
>>>>>>> dspace-7.2.1
```

## dspace/src/main/docker/dspace-postgres-pgcrypto/Dockerfile

<<<<<<< HEAD
This is a postgres docker image containing the pgcrypto extension used in DSpace 6 and DSpace 7.
=======
This is a PostgreSQL Docker image containing the `pgcrypto` extension required by DSpace 6+.
>>>>>>> dspace-7.2.1
```
cd dspace/src/main/docker/dspace-postgres-pgcrypto
docker build -t dspace/dspace-postgres-pgcrypto .
```

<<<<<<< HEAD
This image is built manually.  It should be rebuilt as needed.

A copy of this file exists in the DSpace 7 branch.  A specialized version of this file exists for DSpace 4 in DSpace-Docker-Images.
=======
**This image is built manually.**  It should be rebuilt as needed.

A copy of this file exists in the DSpace 6 branch.  A specialized version of this file exists for DSpace 4 in DSpace-Docker-Images.
>>>>>>> dspace-7.2.1

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace-postgres-pgcrypto
```

## dspace/src/main/docker/dspace-postgres-pgcrypto-curl/Dockerfile

<<<<<<< HEAD
This is a postgres docker image containing the pgcrypto extension used in DSpace 6 and DSpace 7.
This image also contains curl.  The image is pre-configured to load a postgres database dump on initialization.
=======
This is a PostgreSQL Docker image containing the `pgcrypto` extension required by DSpace 6+.
This image also contains `curl`.  The image is pre-configured to load a Postgres database dump on initialization.
>>>>>>> dspace-7.2.1
```
cd dspace/src/main/docker/dspace-postgres-pgcrypto-curl
docker build -t dspace/dspace-postgres-pgcrypto:loadsql .
```

<<<<<<< HEAD
This image is built manually.  It should be rebuilt as needed.

A copy of this file exists in the DSpace 7 branch.
=======
**This image is built manually.**   It should be rebuilt as needed.

A copy of this file exists in the DSpace 6 branch.
>>>>>>> dspace-7.2.1

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace-postgres-pgcrypto:loadsql
```

<<<<<<< HEAD
## local.cfg and test/ folder

These resources are bundled into the _dspace/dspace_ image at build time.

## SFTP Docker Image

The [atmoz/sftp](https://hub.docker.com/r/atmoz/sftp) docker image used for running the sftp
container. The `atmoz/sftp` docker repsitory is not maintaining release versions anymore, and
the updates are pushed to the `latest` tag. In order to avoid using the `latest` tag on the
k8s deployment, we are pulling the `latest` docker image and tagging it with the current date,
and pushing it to our internal docker registry.

```bash
docker tag atmoz/sftp:latest docker.lib.umd.edu/atmoz_sftp_latest:20210715    
=======
## dspace/src/main/docker/dspace-shibboleth/Dockerfile

This is a test / demo image which provides an Apache HTTPD proxy (in front of Tomcat)
with mod_shib & Shibboleth installed.  It is primarily for usage for
testing DSpace's Shibboleth integration. It uses https://samltest.id/ as the Shibboleth IDP

**This image is built manually.**   It should be rebuilt as needed.

```
cd dspace/src/main/docker/dspace-shibboleth
docker build -t dspace/dspace-shibboleth .

# Test running it manually
docker run -i -t -d -p 80:80 -p 443:443 dspace/dspace-shibboleth
```

This image can also be rebuilt using the `../docker-compose/docker-compose-shibboleth.yml` script.


## test/ folder

These resources are bundled into the `dspace/dspace:dspace-*-test` image at build time.


## Debugging Docker builds

When updating or debugging Docker image builds, it can be useful to briefly
spin up an "intermediate container".  Here's how to do that:
```
# First find the intermediate container/image ID in your commandline logs
docker run -i -t [container-id] /bin/bash
>>>>>>> dspace-7.2.1
```
