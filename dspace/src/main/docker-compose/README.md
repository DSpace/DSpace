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

## To refresh / pull DSpace images from Dockerhub
```
docker-compose -f docker-compose.yml -f docker-compose-cli.yml pull
```

## To build DSpace images using code in your branch
```
docker-compose -f docker-compose.yml -f docker-compose-cli.yml build
```

## To run DSpace from your branch

```
docker-compose -p d6 up -d
```

## Ingesting test content

Create an admin account.  By default, the dspace-cli container runs the dspace command.
```
docker-compose -p d6 -f docker-compose-cli.yml run dspace-cli create-administrator -e test@test.edu -f admin -l user -p admin -c en
```

Download a Zip file of AIP content and ingest test data
```
docker-compose -p d6 -f docker-compose-cli.yml -f dspace/src/main/docker-compose/cli.ingest.yml run dspace-cli
```
