# Docker Compose Resources

## Root directory
- docker-compose.yml
  - Docker compose file to orchestrate DSpace 6 components
- docker-compose-cli
  - Docker compose file to run DSpace CLI tasks within a running DSpace instance in Docker

## dspace/src/main/docker-compose

- cli.ingest.yml
  - Docker compose file that will run an AIP ingest into DSpace 6.
- local.cfg
  - Sets the environment used across containers run with docker-compose
- xmlui.xconf
  - Brings up the XMLUI Mirage2 theme by default.

## To run a published DSpace image from your branch

```
docker-compose -p d6 up -d
```

## To build start DSpace from your branch
```
docker-compose -p d6 up --build -d
```

## Ingesting test content

Compile your local changes for the DSpace CLI container
```
docker-compose -p d6 -f docker-compose-cli.yml build
```

Create an admin account.  By default, the dspace-cli container runs the dspace command.
```
docker-compose -p d6 -f docker-compose-cli.yml run dspace-cli create-administrator -e test@test.edu -f admin -l user -p admin -c en
```

Download a Zip file of AIP content and ingest test data
```
docker-compose -p d6 -f docker-compose-cli.yml -f dspace/src/main/docker-compose/cli.ingest.yml run dspace-cli
```
