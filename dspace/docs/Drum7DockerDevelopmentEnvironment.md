# DRUM 7 Docker Development Environment

This document contains instructions for building a local development instance
of a DSpace 7-based DRUM using Docker.

## Development Prerequisites

The following steps need to be run *once* on the local workstation to prepare
it to use the hostnames and certificates needed for running DRUM in the local
development environment.

See the "CAS and the Local Development Environment" section in
[docs/dspace/CASAuthentication.md](CASAuthentication.md) for more information
about these steps.

Preq. 1) Install the "mkcert" and "nss" utilities:

```zsh
$ brew install mkcert nss
```

Note: The "nss" utility is necessary to enable the certificate authority
to be added to Mozilla Firefox.

Preq. 2) Generate and install the "mkcert" certificate authority:

```zsh
$ mkcert -install
```

You will be prompted for the "sudo" password. After entering the "sudo"
password, a MacOS security dialog  will be shown indicating that changes are
being made to the "System Certificate Trust Settings" and requesting your
account password. Enter your password and left-click the "Update Settings"
button.

Preq. 3) Restart your web browser to pick up the changes to the system settings.

Preq. 4) Edit the "/etc/host" file:

```zsh
$ sudo vi /etc/hosts
```

and add the following lines:

```text
127.0.0.1       api.drum-local.lib.umd.edu
127.0.0.1       drum-local.lib.umd.edu
```

## Development Setup

This repository uses the "GitHub Flow" branching model, with "drum-main" as the
main branch for DRUM development.

1) Clone the Git repository and switch to the directory:

    ```zsh
    $ git clone -b drum-main git@github.com:umd-lib/DSpace.git drum
    $ cd drum
    ```

2) Optional: Build the dependent images.

    ```zsh
    $ docker build -f Dockerfile.dependencies -t docker.lib.umd.edu/drum-dependencies-7_x:latest .
    $ docker build -f Dockerfile.ant -t docker.lib.umd.edu/drum-ant:latest .
    $ cd dspace/src/main/docker/dspace-postgres-pgcrypto
    $ docker build -t docker.lib.umd.edu/dspace-postgres:latest .
    $ cd -
    ```

3) Create the local configuration file

    ```zsh
    $ cp dspace/config/local.cfg.EXAMPLE dspace/config/local.cfg
    ```

4) Edit the local configuration file:

    ```zsh
    $ vi dspace/config/local.cfg
    ```

   and enter values for the following properties:

   * drum.ldap.bind.auth
   * drum.ldap.bind.password

   The appropriate values can be found in LastPass.

   **Note:** The "drum.ldap.bind.auth" value typically contains commas (for
   example "uid=foo,cn=bar,ou=baz,dc=quuz,dc=zot"), which must be escaped. So
   the actual value added to the file would be similar to
   `uid=foo\,cn=bar\,ou=baz\,dc=quuz\,dc=zot`.

5) Follow the instructions at [dspace/docs/DrumDBRestore.md](DrumDBRestore.md)
   to populate the Postgres database with a DSpace 7 database dump from
   Kubernetes.

   **Note:** If populating the Postgres database from a DSpace 6 database
   snapshot, use [dspace/docs/DrumDBRestoreFromDSpace6.md](DrumDBRestoreFromDSpace6.md)

6) Generate the HTTPS certificate for the back-end:

   ```zsh
   $ mkcert -cert-file dspace/src/main/docker/nginx/certs/api.drum-local.pem \
            -key-file dspace/src/main/docker/nginx/certs/api.drum-local-key.pem \
            api.drum-local.lib.umd.edu
   ```

7) Build the application and client Docker images:

   **Note:** If building for development, use the instructions in the
   "Quick Builds for development" section below, in place of the following
   steps.

    ```zsh
    # Build the dspace image
    $ docker compose -f docker-compose.yml build

    ```

8) Start all the containers

    ```zsh
    $ docker compose -p d7 up
    ```

    Once the REST API starts, it should be accessible at
    <https://api.drum-local.lib.umd.edu/server>

## Quick Builds for development

To shortcut the long build time that is needed to build the entire project, we
can do a two stage build where the base build does a full Maven build, and for
subsequent changes, only build the "overlays" modules that contain our
customized Java classes.

```zsh
# Base build
$ docker build -f Dockerfile.dev-base -t docker.lib.umd.edu/drum:7_x-dev-base .

# Overlay modules build
$ docker build -f Dockerfile.dev-additions -t docker.lib.umd.edu/drum:7_x-dev .
```

Also, we can start the "dspace" container and the dependencies ("dspacedb"
and "dspacesolr") in separate commands. This allows the "dspace"
container to be started/stopped individually.

```zsh
# Start the Postgres, Solr, and Nginx containers in detached mode
$ docker compose -p d7 up -d dspacedb dspacesolr nginx

# Start the dspace container
$ docker compose -p d7 up dspace
```

## Visual Studio Code IDE Setup

The following is the suggested setup for Visual Studio Code for DSpace
development:

* Install the "Extension Pack for Java" (vscjava.vscode-java-pack) extension
* Install the "Checkstyle for Java" (shengchen.vscode-checkstyle) extension
  * Follow the instructions in the Lyrasis
    ["Code Style Guide"](https://wiki.lyrasis.org/display/DSPACE/Code+Style+Guide#CodeStyleGuide-VSCode)
    to configure the Checkstyle plugin and formatting options.
* The debug configuration necessary for the VS Code to attach to the Tomcat
  running on the Docker is maintained within in ".vscode" directory.

## Debugging

The `JAVA_TOOL_OPTIONS` configuration included in the Docker compose starts the
jpda debugger for Tomcat. The [.vscode/launch.json](/.vscode/launch.json)
file contains the VS Code debug configuration needed to attach to Tomcat. See
the "Visual Studio Code IDE Setup" section for the extensions neeeded for
debugging.

To start debugging,

1) Ensure that the dspace Docker container is up and running.

2) Open to the "Run and Debug" panel (CMD + SHIFT + D) on VS Code.

3) Click the green triangle (Play)  "Debug (Attach to Tomcat)" button on top of
   the debug panel.

## Useful commands

```zsh
# To stop all the containers
$ docker compose -p d7 stop

# To stop just the dspace container
$ docker compose -p d7 stop dspace

# To restart just the dspace container
$ docker compose -p d7 restart dspace

# To attach to the dspace container
$ docker exec -it dspace bash
```

## Create an adminstrator user

```zsh
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

```zsh
$ docker exec -it dspace /dspace/bin/dspace index-discovery
The script has started
Updating Index
Done with indexing
The script has completed
```

## Running the tests

By default the unit and integration tests are not run when building the project.

To run both the unit and integration tests:

```zsh
$ mvn install -DskipUnitTests=false -DskipIntegrationTests=false
```

### Test Environments

Typically, the unit and integration tests will require a test environment
consisting of the DSpace and Spring configurations files.

The stock DSpace uses the Maven "assembly" plugin to generate a Zip file
(named "dspace-parent-\<VERSION>-testEnvironment.zip", where \<VERSION> is the
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
"dspace-parent-\<VERSION>-testEnvironment-additions.zip" artifact.

Both of these "testEnvironment" artifacts are generated by running `mvn install`
in the project root directory. Therefore, after making any changes to the
standard DSpace or Spring configuration files, or any changes in a module's
"src/test/data/dspaceFolder" folder, run the following command in the project
root directory:

```zsh
$ mvn install
```

## DSpace Scripts and Email Setup

DSpace scripts (such as [dspace/bin/load-etd](../bin/load-etd)) may send email
as part of their operation. The development Docker images do not, by themselves,
support running the DSpace scripts or sending emails.

The following changes enable the DSpace scripts to be run in the "dspace"
Docker container, with email being captured by the "MailHog" application, which
is accessible at <http://localhost:8025/>.

**Note:** After making the following changes, the "Dockerfile.dev-base" and
"Dockerfile.dev-additions" Docker images need to be rebuilt.

### Dockerfile.dev-additions

Add the following lines to the "Dockerfile.dev-additions" file, just after the
`FROM tomcat:9-jdk${JDK_VERSION}` line, to include the packages needed for the
script and email functionality:

```text
FROM tomcat:9-jdk${JDK_VERSION}

# Dependencies for email functionality
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y \
      csh \
      postfix \
      s-nail \
      libgetopt-complete-perl \
      libconfig-properties-perl \
    && apt-get purge -y --auto-remove \
    && rm -rf /var/lib/apt/lists/* \
    && mkfifo /var/spool/postfix/public/pickup
# End Dependencies for email functionality
```

### docker-compose.yml

Add the following lines to the "docker-compose.yml" file, in the "service"
stanza, to enable the "MailHog" (<https://github.com/mailhog/MailHog>) SMTP
capture tool as part of the Docker Compose stack:

```yaml
service:
  ...
  # MailHog SMTP Capture
  mailhog:
    container_name: mailhog
    image: mailhog/mailhog:v1.0.1
    networks:
      dspacenet:
    logging:
      driver: 'none'  # disable saving logs
    ports:
      - 1025:1025 # smtp server
      - 8025:8025 # web ui
  # End MailHog SMTP Capture
```

### dspace/config/local.cfg

Set the following values in the "dspace/config/local.cfg" file, replacing the
existing values:

```text
mail.server = mailhog
mail.server.port = 1025
```

## Testing File Download Counts

Testing the file download counts, generated by the Solr "statistics" core,
requires the "GeoIP" database to be added to the local development environment.

These steps should be done at Step 4 in the "Development Setup" process,
*before* building the Docker images.

**Note:** These steps use the "lite" version of the GeoIP
database, which does not require account credentials. The version running in
Kubernetes uses the "full" GeoIP database, which requires account
credentials.

1) Download the “IP to City Lite” (in “mmdb” format) from
<https://db-ip.com/db/download/ip-to-city-lite> and put in the “/tmp” directory,
and extract the file, where “YYYY-MM” is the year/month of the download:

```zsh
$ cd /tmp
$ gunzip dbip-city-lite-YYYY-MM.mmdb.gz
```

This will result in a file named “dbip-city-lite-YYYY-MM.mmdb”. For simplicity,
rename the file to “dbip-city-lite.mmdb”:

```zsh
$ mv /tmp/dbip-city-lite-<yyyy-MM>.mmdb /tmp/dbip-city-lite.mmdb
```

2) Copy the "/tmp/dbip-city-lite.mmdb" file into the "dspace/config/" directory:

```zsh
$ cp /tmp/dbip-city-lite.mmdb dspace/config/
```

3) Add the following line to the “dspace/config/local.cfg” file:

```zsh
usage-statistics.dbfile = /dspace/config/dbip-city-lite.mmdb
```

---
[testenv]: <../../src/main/assembly/testEnvironment.xml>
[testenv-add]: <../../src/main/assembly/testEnvironment-additions.xml>
