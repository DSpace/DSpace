# Digital Repository at the University of Maryland (DRUM)

Home: <http://drum.lib.umd.edu/>

See <http://drum.lib.umd.edu/help/about_drum.jsp> for more information.

## Documentation

The original DSpace documentation:

* [README-DSPACE.md](README-DSPACE.md)
* [DSpace Manual](dspace/docs/pdf/DSpace-Manual.pdf)

### Development Environment

Instructions for building and running drum locally can be found at:
[Drum7DockerDevelopmentEnvironment.md](/dspace/docs/Drum7DockerDevelopmentEnvironment.md)

### Building Images for K8s Deployment

#### DSpace Image

Dockerfile.dependencies is used to pre-cache maven downloads that will be used
in subsequent DSpace docker builds.

```bash
docker build -t docker.lib.umd.edu/drum-dependencies-6_x:latest -f Dockerfile.dependencies .
```

This Dockerfile builds a drum tomcat image.

```bash
docker build -t docker.lib.umd.edu/drum:<VERSION> .
```

The version would follow the drum project version. For example, a release
version could be `6.3/drum-4.2`, and we can suffix the version number
with `-rcX` or use `latest` as the version for non-production images.

#### Postgres Image

To build postgres image with pgcrypto module.

```bash
cd dspace/src/main/docker/dspace-postgres-pgcrypto
docker build -t docker.lib.umd.edu/dspace-postgres:<VERSION> .
```

We could follow the same versioning scheme as the main drum image, but we don't
necessarily have create new image versions for postgres for every patch or
hotfix version increments. The postgres image can be built when there is a
relevant change.

#### Solr Image

To build postgres image with pgcrypto module.

```bash
cd dspace/solr
docker build -t docker.lib.umd.edu/drum-solr:<VERSION> .
```

We could follow the same versioning scheme as the main drum image, but we don't
necessariliy have create new image versions for solr for every patch or hotfix
version increments. The solr image can be built when there is a relevant change.

### Deployment

The `dspace-installer` directory that contains all the artifacts and the ant
script to perform the deployment. The `installer-dist` maven profile creates a
tar file of the installer directory which can be pushed to the UMD nexus by
using the `deploy-release` or `deploy-snapshot` profile.

```bash
# Switch to the dspace directory
cd /apps/git/drum/dspace

# Deploy a snapshot version to nexus
# (use this profile if the current project version is a SNAPSHOT version)
mvn -P installer-dist,deploy-snapshot

# Deploy a release version to nexus
mvn -P installer-dist,deploy-release
```

*NOTE:* For the Nexus deployment to succeed, the nexus server, username and
password needs to be configured in the `.m2/setting.xml` and a prior successful
`mvn install`.

### Features

* [DrumFeatures](dspace/docs/DrumFeatures.md) - Summary of DRUM enhancements to
  base DSpace functionality
* [DrumFeaturesandCode](dspace/docs/DrumFeaturesandCode.md) - DRUM enhancements
  with implementation details
* [DrumConfigurationCustomization](dspace/docs/DrumConfigurationCustomization.md) -
  Information about customizing DSpace for DRUM.
* [docs](dspace/docs) - additional documentation

## License

See the [DRUM-LICENSE](DRUM-LICENSE.md) file for license rights and limitations
(Apache 2.0). This lincense only governs the part of code base developed at UMD.
The DSpace license can be found at <https://github.com/DSpace/DSpace>
