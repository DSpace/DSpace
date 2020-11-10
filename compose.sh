#!/bin/bash

# Use BuildKit, which uses more aggressive parallelism and
# caching of intermediate stages
export COMPOSE_DOCKER_CLI_BUILD=1
export DOCKER_BUILDKIT=1

if test -w /var/run/docker.sock; then
    COMPOSE="docker-compose"
else
    COMPOSE="sudo docker-compose"
fi

${COMPOSE} -p d6 -f docker-compose.yml "$@"

if [ $? -ne 0 ]; then
    echo
    echo To pull the latest DSpace docker container, please run ./compose.sh pull
    echo To build DSpace, please run ./compose.sh build
    echo To run DSpace and make it available on http://127.0.0.1:8080/xmlui please run: ./compose.sh up
    echo For any other docker-compose command, please run ./compose.sh COMMAND
    echo
fi
