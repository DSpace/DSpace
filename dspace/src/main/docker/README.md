# Docker images supporting DSpace

## Dockerfile.dependencies

This dockerfile is used to pre-cache maven downloads that will be used in subsequent DSpace docker builds.
```
docker build -t dspace/dspace-dependencies:dspace-7_x -f Dockerfile.dependencies .
```

This image is built manually.  It should be rebuilt each year or after each major release in order to refresh the cache of jars.  

A corresponding image exists for DSpace 4-6.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace-dependencies:dspace-7_x
```

## Dockerfile.jdk8-test

This dockefile builds a DSpace 7 tomcat image.  The legacy REST api will be deployed without requiring https access.

```
docker build -t dspace/dspace:dspace-7_x-jdk8-test -f Dockerfile.jdk8-test .
```

This image is built automatically after each commit is made to the master branch.

A corresponding image exists for DSpace 4-6.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace:dspace-7_x-jdk8-test
```

## Dockerfile.jdk8

This dockefile builds a DSpace 7 tomcat image.
```
docker build -t dspace/dspace:dspace-7_x-jdk8 -f Dockerfile.jdk8 .
```

This image is built automatically after each commit is made to the master branch.

A corresponding image exists for DSpace 4-6.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace:dspace-7_x-jdk8
```

## Dockefile.cli.jdk8

This dockerfile builds a DSpace 7 CLI image.
```
docker build -t dspace/dspace-cli:dspace-7_x -f Dockerfile.cli.jdk8 .
```

This image is built automatically after each commit is made to the master branch.

A corresponding image exists for DSpace 6.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace-cli:dspace-7_x
```

## dspace/src/main/docker/dspace-postgres-pgcrypto/Dockerfile

This is a postgres docker image containing the pgcrypto extension used in DSpace 6 and DSpace 7.
```
docker build -t dspace/dspace-postgres-pgcrypto -f dspace/src/main/docker/dspace-postgres-pgcrypto/Dockerfile .
```

This image is built manually.  It should be rebuilt as needed.

A copy of this file exists in the DSpace 6 branch.  A specialized version of this file exists for DSpace 4 in DSpace-Docker-Images.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace-postgres-pgcrypto
```

## dspace/src/main/docker/dspace-postgres-pgcrypto-curl/Dockerfile

This is a postgres docker image containing the pgcrypto extension used in DSpace 6 and DSpace 7.
This image also contains curl.  The image is pre-configured to load a postgres database dump on initialization.
```
docker build -t dspace/dspace-postgres-pgcrypto:loadsql -f dspace/src/main/docker/dspace-postgres-pgcrypto-curl/Dockerfile .
```

This image is built manually.  It should be rebuilt as needed.

A copy of this file exists in the DSpace 6 branch.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace-postgres-pgcrypto:loadsql
```

## dspace/src/main/docker/solr/Dockerfile

This is a standalone solr image containing DSpace solr schemas used in DSpace 7.
```
docker build -t dspace/dspace-solr -f dspace/src/main/docker/solr/Dockerfile .
```

This image is built manually.  It should be rebuilt as solr schemas change or as new releases of solr are incorporated.

This file was introduced for DSpace 7.

Admins to our DockerHub repo can publish with the following command.
```
docker push dspace/dspace-solr
```

## local.cfg and test/ folder

These resources are bundled into the _dspace/dspace_ image at build time.
