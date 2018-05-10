# Digital Repository at the University of Maryland (DRUM)

Home: http://drum.lib.umd.edu/

See http://drum.lib.umd.edu/help/about_drum.jsp for more information.

## Documentation

The original DSpace documentation:

* [README-DSPACE.md](README-DSPACE.md)
* [DSpace Manual](dspace/docs/pdf/DSpace-Manual.pdf)

### Installation

Instructions for installing in UMD Libraries development environments (Mac OS X):

*Important:* Until the upcoming DSpace 7 upgrade, we will be using Java 7 for both the maven build (local) and ant deployment (server). Set your JAVA_HOME and PATH environment variables to run maven build using Java 7.

```
# Build the base modules and overlay modules (Slower)
# Full build is only required after a version change (checking out a different version
# or a local project version change)
cd /apps/git/drum
mvn clean install

# Build only the overlay modules (Faster)
# Can be run only after a full build is done at least once after a version change.
cd /apps/git/drum/dspace
mvn install
```

### Deployment

The `dspace-installer` directory that contains all the artifacts and the ant script to perform the deployment. The `installer-dist` maven profile creates a tar file of the installer directory which can be pushed to the UMD nexus by using the `deploy-release` or `deploy-snapshot` profile.

```
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
