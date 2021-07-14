# Digital Repository at the University of Maryland (DRUM)

Home: http://drum.lib.umd.edu/

See http://drum.lib.umd.edu/help/about_drum.jsp for more information.

## Documentation

The original DSpace documentation:

* [README-DSPACE.md](README-DSPACE.md)
* [DSpace Manual](dspace/docs/pdf/DSpace-Manual.pdf)

### Installation

### Installation

Instructions for building and running drum locally.
#### Prerequisites

The following images needs to be built once for the Dockerfile.dev to build successfully.

```
docker build -t docker.lib.umd.edu/drum-dependencies-6_x:latest -f Dockerfile.dependencies .
docker build -t docker.lib.umd.edu/drum-ant:latest -f Dockerfile.ant .
docker build -t docker.lib.umd.edu/drum-tomcat:latest -f Dockerfile.tomcat .
```

#### Build

To build the dspace development image used by the docker-compose.yml

```
docker build -t docker.lib.umd.edu/drum:dev -f Dockerfile.dev .
```

#### Run

Start the drum using docker-compose.

```
docker-compose up -d
```

Useful commands
```
# To stop all the containers
docker-compose down

# To stop just the dspace container
docker-compose stop dspace

# To attach to the dspace container
docker exec -it $(docker ps -f name=dspace$ --format {{.ID}}) bash

```

### Building Images for K8s Deployment


#### DSpace Image

Dockerfile.dependencies is used to pre-cache maven downloads that will be used in subsequent DSpace docker builds.

```
docker build -t docker.lib.umd.edu/drum-dependencies-6_x:latest -f Dockerfile.dependencies .
```

This dockefile builds a drum tomcat image.

```
docker build -t docker.lib.umd.edu/drum:<VERSION> .
```

The version would follow the drum project version. For example, a release version could be `6.3/drum-4.2`, and we can suffix the version number with `-rcX` or use `latest` as the version for non-production images.

#### Postgres Image

To build postgres image with pgcrypto module.

```
cd dspace/src/main/docker/dspace-postgres-pgcrypto
docker build -t docker.lib.umd.edu/dspace-postgres:<VERSION> .
```

We could follow the same versioning scheme as the main drum image, but we don't necessariliy have create new image versions for postgres for every patch or hotfix version increments. The postgres image can be built when there is a relevant change.

#### Solr Image

To build postgres image with pgcrypto module.

```
cd dspace/solr
docker build -t docker.lib.umd.edu/drum-solr:<VERSION> .
```

We could follow the same versioning scheme as the main drum image, but we don't necessariliy have create new image versions for solr for every patch or hotfix version increments. The solr image can be built when there is a relevant change.



### Deployment

The `dspace-installer` directory that contains all the artifacts and the ant script to perform the deployment. The `installer-dist` maven profile creates a tar file of the installer directory which can be pushed to the UMD nexus by using the `deploy-release` or `deploy-snapshot` profile.

```
# Switch to the dspace directory
cd /apps/git/drum/dspace

# Deploy a snapshot version to nexus
# (use this profile if the current project version is a SNAPSHOT version)
mvn -P installer-dist,deploy-snapshot

# Deploy a release version to nexus
mvn -P installer-dist,deploy-release
```

*NOTE:* For the Nexus deployment to succeed, the nexus server, username and password needs to be configured in the `.m2/setting.xml` and a prior successful `mvn install`.

### Features

* [DrumFeatures](dspace/docs/DrumFeatures.md) - Summary of DRUM enhancements to base DSpace functionality
* [DrumFeaturesandCode](dspace/docs/DrumFeaturesandCode.md) - DRUM enhancements with implementation details
* [docs](dspace/docs) - additional documentation

## License

See the [DRUM-LICENSE](DRUM-LICENSE.md) file for license rights and limitations (Apache 2.0). This lincense only governs the part of code base developed at UMD. The DSpace license can be found at https://github.com/DSpace/DSpace
