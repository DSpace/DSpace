# Solr Dockerfile for DSpace 7

## Build Instructions

From DSPACE-SRC dir
```
docker build -t dspace/dspace-solr -f dspace/src/main/docker/solr/Dockerfile .
```

## Test Instructions

From DSPACE-SRC dir
```
docker-compose -f dspace/src/main/docker/solr/docker-compose.yml up
```

Open http://localhost:8983/solr

## Push Instructions

The following command requires push access to dspace on dockerhub.
```
docker push dspace/dspace-solr
```

## Testing with DSpace

_The following PR will be needed to test https://github.com/DSpace-Labs/DSpace-Docker-Images/pull/79_

A new solr volume will be needed for this test. Please delete any pre-existing DSpace 7 solr volumes.

```
docker-compose -p d7 -f docker-compose.yml -f d7.override.yml up -d
```
