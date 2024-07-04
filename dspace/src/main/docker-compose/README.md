# Docker Compose files for DSpace Backend

***
:warning: **THESE IMAGES ARE NOT PRODUCTION READY**  The below Docker Compose images/resources were built for development/testing only.  Therefore, they may not be fully secured or up-to-date, and should not be used in production.

If you wish to run DSpace on Docker in production, we recommend building your own Docker images. You are welcome to borrow ideas/concepts from the below images in doing so. But, the below images should not be used "as is" in any production scenario.
***


## Overview
The scripts in this directory can be used to start the DSpace REST API (backend) in Docker.
Optionally, the DSpace User Interface (frontend) may also be started in Docker.

For additional options/settings in starting the User Interface (frontend) in Docker, see the Docker Compose
documentation for the frontend: https://github.com/DSpace/dspace-angular/blob/main/docker/README.md

## Primary Docker Compose Scripts (in root directory)
The root directory of this project contains the primary Dockerfiles & Docker Compose scripts
which are used to start the backend.

- docker-compose.yml
    - Docker compose file to orchestrate DSpace REST API (backend) components.
    - Uses the `Dockerfile` in the same directory.
- docker-compose-cli.yml
    - Docker compose file to run DSpace CLI (Command Line Interface) tasks within a running DSpace instance in Docker. See instructions below.
    - Uses the `Dockerfile.cli` in the same directory.

Documentation for all Dockerfiles used by these compose scripts can be found in the ["docker" folder README](../docker/README.md)

## Additional Docker Compose tools (in ./dspace/src/main/docker-compose)

- cli.assetstore.yml
  - Docker compose file that will download and install a default assetstore.
  - The default assetstore is the configurable entities test dataset. Useful for [testing/demos of Entities](#Ingest Option 2 Ingest Entities Test Data).
- cli.ingest.yml
  - Docker compose file that will run an AIP ingest into DSpace. Useful for testing/demos with basic Items.
- db.entities.yml
  - Docker compose file that pre-populate a database instance using a downloaded SQL dump.
  - The default dataset is the configurable entities test dataset. Useful for [testing/demos of Entities](#Ingest Option 2 Ingest Entities Test Data).
- db.restore.yml
  - Docker compose file that pre-populate a database instance using a *local* SQL dump (hardcoded to `./pgdump.sql`)
  - Useful for restoring data from a local backup, or [Upgrading PostgreSQL in Docker](#Upgrading PostgreSQL in Docker)
- docker-compose-angular.yml
  - Docker compose file that will start a published DSpace User Interface container that interacts with the branch.
- docker-compose-shibboleth.yml
  - Docker compose file that will start a *test/demo* Shibboleth SP container (in Apache) that proxies requests to the DSpace container
  - ONLY useful for testing/development. NOT production ready.
- docker-compose-iiif.yml
    - Docker compose file that will start a *test/demo* Cantaloupe image server container required for enabling IIIF support.
    - ONLY useful for testing/development. NOT production ready.

Documentation for all Dockerfiles used by these compose scripts can be found in the ["docker" folder README](../docker/README.md)


## To refresh / pull DSpace images from Dockerhub
```
docker compose -f docker-compose.yml -f docker-compose-cli.yml pull
```

## To build DSpace images using code in your branch
```
docker compose -f docker-compose.yml -f docker-compose-cli.yml build
```

OPTIONALLY, you can build DSpace images using a different JDK_VERSION like this:
```
docker compose -f docker-compose.yml -f docker-compose-cli.yml build --build-arg JDK_VERSION=17
```
Default is Java 11, but other LTS releases (e.g. 17) are also supported.

## Run DSpace 8 REST from your current branch
```
docker compose -p d8 up -d
```

## Run DSpace 8 REST and Angular from your branch

```
docker compose -p d8 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-angular.yml up -d
```
NOTE: This starts the UI in development mode. It will take a few minutes to see the UI as the Angular code needs to be compiled.

## Run DSpace REST and DSpace Angular from local branches

*Allows you to run the backend from the "DSpace/DSpace" codebase while also running the frontend from the "DSpace/dspace-angular" codebase.*

See documentation in [DSpace User Interface Docker instructions](https://github.com/DSpace/dspace-angular/blob/main/docker/README.md#run-dspace-rest-and-dspace-angular-from-local-branches).

## Run DSpace 8 REST with a IIIF Image Server from your branch
*Only useful for testing IIIF support in a development environment*

This command starts our `dspace-iiif` container alongside the REST API.
That container provides a [Cantaloupe image server](https://cantaloupe-project.github.io/),
which can be used when IIIF support is enabled in DSpace (`iiif.enabled=true`).

```
docker compose -p d8 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-iiif.yml up -d
```

## Run DSpace 8 REST and Shibboleth SP (in Apache) from your branch
*Only useful for testing Shibboleth in a development environment*

This Shibboleth container uses https://samltest.id/ as an IdP (see `../docker/dspace-shibboleth/`).
Therefore, for Shibboleth login to work properly, you MUST make your DSpace site available to the external web.

One option is to use a development proxy service like https://ngrok.com/, which creates a temporary public proxy for your localhost.
The remainder of these instructions assume you are using ngrok (though other proxies may be used).

1. If you use ngrok, start it first (in order to obtain a random URL that looks like https://a6eb2e55ad17.ngrok.io):
   ```
   ./ngrok http 443
   ```

2. Then, update `local.cfg` in this directory to use that ngrok URL & configure Shibboleth:
   ```
   # NOTE: dspace.server.url MUST be available externally to use with https://samltest.id/.
   # In this example we are assuming you are using ngrok.
   dspace.server.url=https://[subdomain].ngrok.io/server

   # Enable both Password auth & Shibboleth
   plugin.sequence.org.dspace.authenticate.AuthenticationMethod = org.dspace.authenticate.PasswordAuthentication
   plugin.sequence.org.dspace.authenticate.AuthenticationMethod = org.dspace.authenticate.ShibAuthentication
   
   # Settings for https://samltest.id/
   authentication-shibboleth.netid-header = uid
   authentication-shibboleth.email-header = mail
   authentication-shibboleth.firstname-header = givenName
   authentication-shibboleth.lastname-header = sn
   authentication-shibboleth.role-header = role
   ```

3. Build the Shibboleth container (if you haven't built or pulled it before):
   ```
   cd [dspace-src]
   docker compose -p d8 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-shibboleth.yml build
   ```

4. Start all containers, passing your public hostname as the `DSPACE_HOSTNAME` environment variable:
   ```
   DSPACE_HOSTNAME=[subdomain].ngrok.io docker compose -p d8 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-shibboleth.yml up -d
   ```
   NOTE: For Windows you MUST either set the environment variable separately, or use the 'env' command provided with Git/Cygwin
   (you may already have this command if you are running Git for Windows). See https://superuser.com/a/1079563
   ```
   env DSPACE_HOSTNAME=[subdomain].ngrok.io docker compose -p d8 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-shibboleth.yml up -d
   ```

5. Finally, for https://samltest.id/, you need to upload your Shibboleth Metadata for the site to "trust" you.
   Using the form at https://samltest.id/upload.php, enter in
   `https://[subdomain].ngrok.io/Shibboleth.sso/Metadata` and click "Fetch!"
      * Note: If samltest.id still says you are untrusted, restart your Shibboleth daemon! (This may be necessary to download the IdP Metadata from samltest.id)
        ```
        docker exec -it dspace-shibboleth /bin/bash
        service shibd stop
        service shibd start
        ```

6. At this point, if all went well, your site should work!  Try it at
   https://[subdomain].ngrok.io/server/
7. If you want to include Angular UI as well, then you'll need a few extra steps:
      * Update `environment.dev.ts` in this directory as follows:
        ```
        rest: {
          ssl: true,
          host: '[subdomain].ngrok.io',
          port: 443,
          // NOTE: Space is capitalized because 'namespace' is a reserved string in TypeScript
          nameSpace: '/server'
        }
        ```
      * Spin up the `dspace-angular` container alongside the others, e.g.
        ```
        DSPACE_HOSTNAME=[subdomain].ngrok.io docker compose -p d8 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-angular.yml -f dspace/src/main/docker-compose/docker-compose-shibboleth.yml up -d
        ```

## Sample Test Data

### Ingesting test content from AIP files

*Allows you to ingest a set of AIPs into your DSpace instance for testing/demo purposes.* These AIPs represent basic Communities, Collections and Items.

Prerequisites
- Start DSpace using one of the options listed above
- Build the DSpace CLI image if needed.  See the instructions above.

Create an admin account.  By default, the dspace-cli container runs the dspace command.
```
docker compose -p d8 -f docker-compose-cli.yml run --rm dspace-cli create-administrator -e test@test.edu -f admin -l user -p admin -c en
```

Download a Zip file of AIP content and ingest test data
```
docker compose -p d8 -f docker-compose-cli.yml -f dspace/src/main/docker-compose/cli.ingest.yml run --rm dspace-cli
```

### Ingest Entities Test Data

*Allows you to load Configurable Entities test data for testing/demo purposes.*

Prerequisites
- Start DSpace using one of the options listed above
- Build the DSpace CLI image if needed.  See the instructions above.
- _Remove your volumes if you already ingested content into your docker volumes_

Start DSpace REST with a postgres database dump downloaded from the internet.
```
docker compose -p d8 -f docker-compose.yml -f dspace/src/main/docker-compose/db.entities.yml up -d
```

Download an assetstore from a tar file on the internet.
```
docker compose -p d8 -f docker-compose-cli.yml -f dspace/src/main/docker-compose/cli.assetstore.yml run dspace-cli
```

## Modify DSpace Configuration in Docker
While your Docker containers are running, you may directly modify any configurations under
`[dspace-src]/dspace/config/`. Those config changes will be synced to the container.
(This works because our `docker-compose.yml` mounts the `[src]/dspace/config` directory from the host into the running Docker instance.)

Many DSpace configuration settings will reload automatically (after a few seconds).  However, configurations which are cached by DSpace (or by Spring Boot) may require you to quickly reboot the Docker containers by running `docker compose -p d7 down` followed by `docker compose -p d7 up -d`.

## Running DSpace CLI scripts in Docker
While the Docker containers are running, you can use the DSpace CLI image to run any DSpace commandline script (i.e. any command that normally can be run by `[dspace]/bin/dspace`).  The general format is:

```
docker compose -p d8 -f docker-compose-cli.yml run --rm dspace-cli [command] [parameters]
```

So, for example, to reindex all content in Discovery, normally you'd run `./dspace index-discovery -b` from commandline.  Using our DSpace CLI image, that command becomes:

```
docker compose -p d8 -f docker-compose-cli.yml run --rm dspace-cli index-discovery -b
```

Similarly, you can see the value of any DSpace configuration (in local.cfg or dspace.cfg) by running:

```
# Output the value of `dspace.ui.url` from running Docker instance
docker compose -p d8 -f docker-compose-cli.yml run --rm dspace-cli dsprop -p dspace.ui.url
```

NOTE: It is also possible to run CLI scripts directly on the "dspace" container (where the backend runs)
This can be useful if you want to pass environment variables which override DSpace configs.
```
# Run the "./dspace database clean" command from the "dspace" container
# Before doing so, it sets "db.cleanDisabled=false".
# WARNING: This will delete all your data. It's just an example of how to do so.
docker compose -p d8 exec -e "db__P__cleanDisabled=false" dspace /dspace/bin/dspace database clean
```

## Upgrading PostgreSQL in Docker

Occasionally, we update our `dspace-postgres-*` images to use a new version of PostgreSQL.
Simply using the new image will likely throw errors as the pgdata (postgres data) directory is incompatible
with the new version of PostgreSQL. These errors look like:
```
FATAL:  database files are incompatible with server
DETAIL:  The data directory was initialized by PostgreSQL version 11, which is not compatible with this version 13.10
```

Here's how to fix those issues by migrating your old Postgres data to the new version of Postgres

1. First, you must start up the older PostgreSQL image (to dump your existing data to a `*.sql` file)
    ```
    # This command assumes you are using the process described above to start all your containers
    docker compose -p d8 up -d
    ```
    * If you've already accidentally updated to the new PostgreSQL image, you have a few options:
        * Pull down an older version of the image from Dockerhub (using a tag)
        * Or, temporarily rebuild your local image with the old version of Postgres. For example:
          ```
          # This command will rebuild using PostgreSQL v11 & tag it locally as "latest"
          docker build --build-arg POSTGRES_VERSION=11 -t dspace/dspace-postgres-pgcrypto:latest ./dspace/src/main/docker/dspace-postgres-pgcrypto/
          # Then restart container with that image
          docker compose -p d8 up -d
          ```
2. Dump your entire "dspace" database out of the old "dspacedb" container to a local file named `pgdump.sql`
    ```
    # NOTE: WE HIGHLY RECOMMEND LOGGING INTO THE CONTAINER and doing the pg_dump within the container.
    # If you attempt to run pg_dump from your local machine via docker "exec" (or similar), sometimes
    # UTF-8 characters can be corrupted in the export file. This may result in data loss.

    # First login to the "dspacedb" container
    docker exec -it dspacedb /bin/bash

    # Dump the "dspace" database to a file named "/tmp/pgdump.sql" within the container
    pg_dump -U dspace dspace > /tmp/pgdump.sql

    # Exit the container
    exit

    # Download (copy) that /tmp/pgdump.sql backup file from container to your local machine
    docker cp dspacedb:/tmp/pgdump.sql .
    ```
3. Now, stop all existing containers. This shuts down the old version of PostgreSQL
    ```
    # This command assumes you are using the process described above to start/stop all your containers
    docker compose -p d8 down
    ```
4. Delete the `pgdata` volume. WARNING: This deletes all your old PostgreSQL data. Make sure you have that `pgdump.sql` file FIRST!
    ```
    # Assumes you are using `-p d8` which prefixes all volumes with `d8_`
    docker volume rm d8_pgdata
    ```
5. Now, pull down the latest PostgreSQL image with the NEW version of PostgreSQL.
    ```
    docker compose -f docker-compose.yml -f docker-compose-cli.yml pull
    ```
6. Start everything up using our `db.restore.yml` script.  This script will recreate the database
using the local `./pgdump.sql` file. IMPORTANT: If you renamed that "pgdump.sql" file or stored it elsewhere,
then you MUST change the name/directory in the `db.restore.yml` script.
    ```
    # Restore database from "./pgdump.sql" (this path is hardcoded in db.restore.yml)
    docker compose -p d8 -f docker-compose.yml -f dspace/src/main/docker-compose/db.restore.yml up -d
    ```
7. Finally, reindex all database contents into Solr (just to be sure Solr indexes are current).
    ```
    # Run "./dspace index-discovery -b" using our CLI image
    docker compose -p d8 -f docker-compose-cli.yml run --rm dspace-cli index-discovery -b
    ```
At this point in time, all your old database data should be migrated to the new Postgres
and running at http://localhost:8080/server/
