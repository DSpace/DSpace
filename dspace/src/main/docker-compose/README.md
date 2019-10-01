# Docker Compose Resources

- cli.assetstore.yml
  - Docker compose file that will download and install a default assetstore.
- cli.ingest.yml
  - Docker compose file that will run an AIP ingest into DSpace 7.
- db.entities.yml
  - Docker compose file that pre-populate a database instance using a SQL dump.  The default dataset is the configurable entities test dataset.
- local.cfg
  - Sets the environment used across containers run with docker-compose
- docker-compose-angular.yml
  - Docker compose file that will start a published DSpace angular container that interacts with the branch.
- environment.dev.js
  - Default angular environment when testing DSpace-angular from this repo

## To run a published DSpace image from your branch

## Option 1: Start DSpace with an empty repository

Start with empty repository.
```
docker-compose -p d7 up --build -d
```

## Option 2: Ingesting test content from AIP

Start with empty repository.
```
docker-compose -p d7 up --build -d
```

Compile your local changes for the DSpace CLI container
```
docker-compose -p d7 -f docker-compose-cli.yml build
```

Create an admin account.  By default, the dspace-cli container runs the dspace command.
```
docker-compose -p d7 -f docker-compose-cli.yml run dspace-cli create-administrator -e test@test.edu -f admin -l user -p admin -c en
```

Download a Zip file of AIP content and ingest test data
```
docker-compose -p d7 -f docker-compose-cli.yml -f dspace/src/main/docker-compose/cli.ingest.yml run dspace-cli run dspace-cli
```

## Option 3: Ingest Entities Test Data

Start with a postgres database dump downloaded from the internet.
```
docker-compose -p d7 -f dspace/src/main/docker-compose/db.entities.yml up --build -d
```

Download an assetstore from a tar file on the internet.
```
docker-compose -p d7 -f docker-compose-cli.yml -f dspace/src/main/docker-compose/cli.assetstore.yml run dspace-cli
```
