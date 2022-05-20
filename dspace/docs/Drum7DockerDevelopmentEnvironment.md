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
    # Build the dspace image
    docker compose -f docker-compose.yml build

    ```

5. Follow the instructions at [DRUM7DBRestore.md](./DRUM7DBRestore.md) to restore DRUM's DSpace 6 DB dump to DSpace 7.

6. Start all the containers

    ```bash
    docker compose -p d7 up
    ```

    Once the REST API starts, it should be accessible at [http://localhost:8080/server]

## Quick Builds for development

The cut-short the long build time that is needed to build the entire project, we can do a two stage build where the base build does a full maven build, and for subsequent changes, we can do a build only builds the overlays modules that contain our overriding customization Java classes.

```bash
# Base build
docker build -f Dockerfile.dev-base -t docker.lib.umd.edu/drum:7_x-dev-base .

# Overlay modules build
docker build -f Dockerfile.dev-additions -t docker.lib.umd.edu/drum:7_x-dev .
```

Also, we can start the dspace container and the dependencies (db & solr) in separate commands. This would allow us to individually stop and start the dspace container.

```bash
# Start the db and solr container in detached mode
docker compose -p d7 up -d dspacedb dspacesolr

# Start the dspace container
docker compose -p d7 up dspace
```

## Debugging

The `JAVA_TOOL_OPTIONS` configuration included in the docker compose starts the jpda debugger for tomcat. The [.vscode/launch.json](.vscode/launch.json) contains the VS Code debug configuration needed to attach to the tomcat. Follow the instructions at <https://confluence.umd.edu/display/LIB/DSpace+Development+IDE+Setup> to install the necessary extensions to be able to debug.

To start debugging,

1. Ensure that the dspace docker container is up and running.
2. Open to the "Run and Debug" panel (CMD + SHIFT + D) on VS Code.
3. Click the green triange (Play)  "Debug (Attach to Tomcat)" button on top of the debug panel.

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
$ docker exec -it dspace /dspace/bin/dspace create-administrator
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
$ docker exec -it dspace /dspace/bin/dspace index-discovery
The script has started
Updating Index
Done with indexing
The script has completed
```
## Running the tests

By default the unit and integration tests are not run when building the project.

To run both the unit and integration tests:

```bash
mvn install -DskipUnitTests=false -DskipIntegrationTests=false
```

### Test Environments

Typically, the unit and integration tests will require a test environment
consisting of the DSpace and Spring configurations files.

The stock DSpace uses the Maven "assembly" plugin to generate a Zip file
(named "dspace-parent-<VERSION>-testEnvironment.zip", where <VERSION> is the
project version), storing it as an artifact in the local Maven repository. This
Zip file contains the necessary configuration for the tests, based on the
standard configuration files, overlaid with files from the
"src/test/data/dspaceFolder" folder of each module (see
<https://wiki.lyrasis.org/display/DSPACE/Code+Testing+Guide> and
[src/main/assembly/testEnvironment.xml][testenv]).

This "testEnvironment.zip" file is not suitable, however, for the "additions"
module, which modifies some of the DSpace and Spring configuration files to
support the database entities and services added by the module. Therefore a
second Maven assembly file
[src/main/assembly/testEnvironment-additions.xml][testenv-add] has
been created, which generates a
"dspace-parent-<VERSION>-testEnvironment-additions.zip" artifact.

Both of these "testEnvironment" artifacts are generated by running `mvn install`
in the project root directory. Therefore, after making any changes to the
standard DSpace or Spring configuration files, or any changes in a module's
"src/test/data/dspaceFolder" folder, run the following command in the project
root directory:

```
mvn install
```

---
[testenv]: <../../src/main/assembly/testEnvironment.xml>
[testenv-add]: <../../src/main/assembly/testEnvironment-additions.xml>
