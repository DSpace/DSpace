# DRUM 7 Docker Development Environment

This document contains instructions for building DRUM 7 dspace environment
using docker.

## Development Setup

1. Clone the Git repository and switch to the directory:

    ```bash
    git clone -b drum-develop git@github.com:umd-lib/DSpace.git drum
    cd drum
    ```

2. Optional: Build the dependent images.

    ```bash
    docker build -f Dockerfile.dependencies -t docker.lib.umd.edu/drum-dependencies-7_x:latest .
    docker build -f Dockerfile.ant -t docker.lib.umd.edu/drum-ant:latest .
    cd dspace/src/main/docker/dspace-postgres-pgcrypto
    docker build -t docker.lib.umd.edu/dspace-postgres:latest .
    cd -
    ```

3. Create the local configuration file

    ```bash
    cp dspace/config/local.cfg.TEMPLATE dspace/config/local.cfg
    ```

4. Build the application and client docker images:

    ```bash
    # Build both images
    docker compose -f docker-compose.yml -f docker-compose-cli.yml build

    # Build application image only
    docker compose -f docker-compose.yml build

    # Build client image only
    docker compose -f docker-compose-cli.yml build
    ```

5. Follow the instructions at [DRUM7DBMigration.md](./DRUM7DBMigration.md) to migrate DRUM's DSpace 6 DB dump to DSpace 7.

6. Start all the containers

    ```bash
    docker compose -p d7 up
    ```

    Once the REST API starts, it should be accessible at [http://localhost:8080/server]

## Useful commands

```bash
# To stop all the containers
docker compose -p d7 stop

# To stop just the dspace container
docker compose -p d7 stop dspace

# To restart just the dspace container
docker compose -p d7 restart dspace

# To attach to the dspace container
docker exec -it dspace bash
```

## Create an adminstrator user

```bash
$ docker compose -p d7 -f docker-compose-cli.yml run dspace-cli create-administrator
Creating d7_dspace-cli_run ... done
Creating an initial administrator account
E-mail address: <EMAIL_ADDRESS>
First name: <FIRST_NAME>
Last name: <LAST_NAME>
Password will not display on screen.
Password:
Again to confirm:
Is the above data correct? (y or n): y
Administrator account created
```

## Populate the Solr search index

```bash
$ docker compose -p d7 -f docker-compose-cli.yml run dspace-cli index-discovery
The script has started
Updating Index
Done with indexing
The script has completed
```
