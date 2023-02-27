# Docker Compose files

***
:warning: **THESE IMAGES ARE NOT PRODUCTION READY**  The below Docker Compose images/resources were built for development/testing only.  Therefore, they may not be fully secured or up-to-date, and should not be used in production.

If you wish to run DSpace on Docker in production, we recommend building your own Docker images. You are welcome to borrow ideas/concepts from the below images in doing so. But, the below images should not be used "as is" in any production scenario.
***

## 'Dockerfile' in root directory 
This Dockerfile is used to build a *development* DSpace 7 Angular UI image, published as 'dspace/dspace-angular'

```
docker build -t dspace/dspace-angular:dspace-7_x .
```

This image is built *automatically* after each commit is made to the `main` branch.

Admins to our DockerHub repo can manually publish with the following command.
```
docker push dspace/dspace-angular:dspace-7_x
```

## docker directory
- docker-compose.yml
  - Starts DSpace Angular with Docker Compose from the current branch.  This file assumes that a DSpace 7 REST instance will also be started in Docker.
- docker-compose-rest.yml
  - Runs a published instance of the DSpace 7 REST API - persists data in Docker volumes
- docker-compose-ci.yml
  - Runs a published instance of the DSpace 7 REST API for CI testing.  The database is re-populated from a SQL dump on each startup.
- cli.yml
  - Docker compose file that provides a DSpace CLI container to work with a running DSpace REST container.
- cli.assetstore.yml
  - Docker compose file that will download and install data into a DSpace REST assetstore.  This script points to a default dataset that will be utilized for CI testing.


## To refresh / pull DSpace images from Dockerhub
```
docker-compose -f docker/docker-compose.yml pull
```

## To build DSpace images using code in your branch
```
docker-compose -f docker/docker-compose.yml build
```

## To start DSpace (REST and Angular) from your branch

```
docker-compose -p d7 -f docker/docker-compose.yml -f docker/docker-compose-rest.yml up -d
```

## Run DSpace REST and DSpace Angular from local branches.
_The system will be started in 2 steps. Each step shares the same docker network._

From DSpace/DSpace (build as needed)
```
docker-compose -p d7 up -d
```

From DSpace/DSpace-angular
```
docker-compose -p d7 -f docker/docker-compose.yml up -d
```

## Ingest test data from AIPDIR

Create an administrator
```
docker-compose -p d7 -f docker/cli.yml run --rm dspace-cli create-administrator -e test@test.edu -f admin -l user -p admin -c en
```

Load content from AIP files
```
docker-compose -p d7 -f docker/cli.yml -f ./docker/cli.ingest.yml run --rm dspace-cli
```

## Alternative Ingest - Use Entities dataset
_Delete your docker volumes or use a unique project (-p) name_

Start DSpace with Database Content from a database dump
```
docker-compose -p d7 -f docker/docker-compose.yml -f docker/docker-compose-rest.yml -f docker/db.entities.yml up -d
```

Load assetstore content and trigger a re-index of the repository
```
docker-compose -p d7 -f docker/cli.yml -f docker/cli.assetstore.yml run --rm dspace-cli
```

## End to end testing of the rest api (runs in travis).
_In this instance, only the REST api runs in Docker using the Entities dataset. Travis will perform CI testing of Angular using Node to drive the tests._

```
docker-compose -p d7ci -f docker/docker-compose-travis.yml up -d
```
