# Digital Repository at the University of Maryland (DRUM)

Home: <https://drum.lib.umd.edu/>

## Documentation

The original Dspace documentation is in the "README.md" file.

## Development Environment

Instructions for building and running drum locally can be found in
[dspace/docs/Drum7DockerDevelopmentEnvironment.md](/dspace/docs/Drum7DockerDevelopmentEnvironment.md)

## Building Images for K8s Deployment

As of May 2023,  MacBooks utilizing Apple Silicon (the "arm64" architecture)
are unable to directly generate the "amd64" Docker images used by Kubernetes.

The following procedure uses the Docker "buildx" functionality and the
Kubernetes "build" namespace to build the Docker images. This procedure should
work on both "arm64" and "amd64" MacBooks.

All images will be automatically pushed to the Nexus.

### Local Machine Setup

See <https://confluence.umd.edu/display/LIB/Docker+Builds+in+Kubernetes> in
Confluence for information about setting up a MacBook to use the Kubernetes
"build" namespace.

### Creating the Docker images

1) In an empty directory, checkout the Git repository and switch into the
   directory:

    ```bash
    $ git clone git@github.com:umd-lib/DSpace.git drum
    $ cd drum
    ```

2) Checkout the appropriate Git tag, branch, or commit for the Docker images.

3) Set up a "DRUM_TAG" environment variable:

    ```bash
    $ export DRUM_TAG=<DOCKER_IMAGE_TAG>
    ```

   where \<DOCKER_IMAGE_TAG> is the Docker image tag to associate with the
   Docker images. This will typically be the Git tag for the DRUM version,
   or some other identifier, such as a Git commit hash. For example, using the
   Git tag of "7.4/drum-0":

    ```bash
    $ export DRUM_TAG=7.4/drum-0
    ```

4) Set up a "DRUM_DIR" environment variable referring to the current
   directory:

    ```bash
    $ export DRUM_DIR=`pwd`
    ```

5) Switch to the Kubernetes "build" namespace:

    ```bash
    $ kubectl config use-context build
    ```

6) Create the "docker.lib.umd.edu/drum-dependencies-7_x" Docker image. This
   image is used to pre-cache Maven downloads that will be used in subsequent
   DSpace docker builds:

    ```bash
    $ docker buildx build --platform linux/amd64 --builder=kube --push --no-cache -t docker.lib.umd.edu/drum-dependencies-7_x:latest -f Dockerfile.dependencies .
    ```

7) Create the "docker.lib.umd.edu/drum" Docker image:

    ```bash
    $ docker buildx build --platform linux/amd64 --builder=kube --push --no-cache -f Dockerfile -t docker.lib.umd.edu/drum:$DRUM_TAG .
    ```

8) Create the "docker.lib.umd.edu/dspace-postgres", which is a Postgres image
   with "pgcrypto" module:

    **Note:** The "Dockerfile" for the "dspace-postgres" image specifies
    only the major Postgres version as the base image. This allows Postgres
    minor version updates to be retrieved automatically. It may not be
    necessary to create new "dspace-postgres" image versions for every DRUM
    patch or hotfix version increment.

    ```bash
    $ cd $DRUM_DIR/dspace/src/main/docker/dspace-postgres-pgcrypto

    $ docker buildx build --platform linux/amd64 --builder=kube --push --no-cache -f Dockerfile -t docker.lib.umd.edu/dspace-postgres:$DRUM_TAG .
    ```

9) Create the "docker.lib.umd.edu/drum-solr":

    **Note:** The "Dockerfile" for the "drum-solr" image specifies only the
    major Solr version as the base image. This allows Solr minor version updates
    to be retrieved automatically. It may not be necessary to create new
    "drum-solr" image versions for every DRUM patch or hotfix version increment.

    ```bash
    $ cd $DRUM_DIR/dspace/solr

    $ docker buildx build --platform linux/amd64 --builder=kube --push --no-cache -f Dockerfile -t docker.lib.umd.edu/drum-solr:$DRUM_TAG .
    ```

### Features

* [DrumFeatures](dspace/docs/DrumFeatures.md) - Summary of DRUM enhancements to
  base DSpace functionality
* [DrumConfigurationCustomization](dspace/docs/DrumConfigurationCustomization.md) -
  Information about customizing DSpace for DRUM.
* [docs](dspace/docs) - additional documentation

## License

See the [DRUM-LICENSE](DRUM-LICENSE.md) file for license rights and limitations
(Apache 2.0). This lincense only governs the part of code base developed at UMD.
The DSpace license can be found at <https://github.com/DSpace/DSpace>
