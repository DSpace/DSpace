# Docker Compose Resources

## root directory Resources
- docker-compose.yml
  - Docker compose file to orchestrate DSpace 7 REST components
- docker-compose-cli
  - Docker compose file to run DSpace CLI tasks within a running DSpace instance in Docker

## dspace/src/main/docker-compose resources

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
- environment.dev.ts
  - Default angular environment when testing DSpace-angular from this repo

## To refresh / pull DSpace images from Dockerhub
```
docker-compose -f docker-compose.yml -f docker-compose-cli.yml pull
```

## To build DSpace images using code in your branch
```
docker-compose -f docker-compose.yml -f docker-compose-cli.yml build
```

## Run DSpace 7 REST from your current branch
```
docker-compose -p d7 up -d
```

## Run DSpace 7 REST and Angular from your branch

```
docker-compose -p d7 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-angular.yml up -d
```

## Run DSpace 7 REST and Angular from local branches

_The system will be started in 2 steps. Each step shares the same docker network._

From DSpace/DSpace
```
docker-compose -p d7 up -d
```

From DSpace/DSpace-angular (build as needed)
```
docker-compose -p d7 -f docker/docker-compose.yml up -d
```

## Ingest Option 1: Ingesting test content from AIP files into a running DSpace 7 instance

Prerequisites
- Start DSpace 7 using one of the options listed above
- Build the DSpace CLI image if needed.  See the instructions above.

Create an admin account.  By default, the dspace-cli container runs the dspace command.
```
docker-compose -p d7 -f docker-compose-cli.yml run --rm dspace-cli create-administrator -e test@test.edu -f admin -l user -p admin -c en
```

Download a Zip file of AIP content and ingest test data
```
docker-compose -p d7 -f docker-compose-cli.yml -f dspace/src/main/docker-compose/cli.ingest.yml run --rm dspace-cli
```

## Ingest Option 2: Ingest Entities Test Data
_Remove your d7 volumes if you already ingested content into your docker volumes_

Start DSpace REST with a postgres database dump downloaded from the internet.
```
docker-compose -p d7 -f docker-compose.yml -f dspace/src/main/docker-compose/db.entities.yml up -d
```

Download an assetstore from a tar file on the internet.
```
docker-compose -p d7 -f docker-compose-cli.yml -f dspace/src/main/docker-compose/cli.assetstore.yml run dspace-cli
```

## Modify DSpace Configuration in Docker
While your Docker containers are running, you may directly modify the `local.cfg` in this directory which will change the DSpace configuration for the running Docker container. (Keep in mind, this works because our `docker-compose.yml` mounts this `[src]/dspace/src/main/docker-compose/local.cfg` from the host into the running Docker instance.)

Many DSpace configuration settings will reload automatically (after a few seconds).  However, configurations which are cached by DSpace (or by Spring Boot) may require you to quickly reboot the Docker containers by running `docker-compose -p d7 down` followed by `docker-compose -p d7 up -d`.

## Running DSpace CLI scripts in Docker
While the Docker containers are running, you can use the DSpace CLI image to run any DSpace commandline script (i.e. any command that normally can be run by `[dspace]/bin/dspace`).  The general format is:

```
docker-compose -p d7 -f docker-compose-cli.yml run --rm dspace-cli [command] [parameters]
```

So, for example, to reindex all content in Discovery, normally you'd run `./dspace index-discovery -b` from commandline.  Using our DSpace CLI image, that command becomes:

```
docker-compose -p d7 -f docker-compose-cli.yml run --rm dspace-cli index-discovery -b
```

Similarly, you can see the value of any DSpace configuration (in local.cfg or dspace.cfg) by running:

```
# Output the value of `dspace.ui.url` from running Docker instance
docker-compose -p d7 -f docker-compose-cli.yml run --rm dspace-cli dsprop -p dspace.ui.url
```
