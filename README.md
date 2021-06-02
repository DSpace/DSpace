
# DSpace-CRIS

[![Build Status](https://github.com/4Science/DSpace/workflows/Build/badge.svg)](https://github.com/4Science/DSpace/actions?query=workflow%3ABuild)

[DSpace-CRIS Documentation](https://wiki.lyrasis.org/display/DSPACECRIS/Technical+and+User+documentation) |
[DSpace-CRIS Wiki](https://wiki.duraspace.org/display/DSPACECRIS/DSpace-CRIS+Home) |
[Support](https://wiki.lyrasis.org/display/DSPACE/Support)

DSpace-CRIS is an open source extension of DSpace (http://www.dspace.org) providing out of box support for the CRIS / RIMS and moder Institution Repository use cases with advanced features and optimized configurations
For more information, visit https://wiki.duraspace.org/display/DSPACECRIS/DSpace-CRIS+Home

**If you would like to get involved in our DSpace-CRIS 7 development effort, we welcome new contributors.** Just join one of our meetings or get in touch via Slack. See the [DSpace 7 Working Group](https://wiki.lyrasis.org/display/DSPACE/DSpace+7+Working+Group) wiki page for more info and join the #dspace-cris channel.

**If you are looking for the ongoing maintenance work for DSpace-CRIS 6 (or prior releases)**, you can find that work on the corresponding maintenance branch (e.g. [`dspace-6_x_x-cris`](https://github.com/4Science/DSpace/tree/dspace-6_x_x-cris)) in this repository.
***

## Downloads

The latest release of DSpace-CRIS can be downloaded from the [DSpace-CRIS Wiki](https://wiki.duraspace.org/display/DSPACECRIS/DSpace-CRIS+Home) or from [GitHub](https://github.com/4Science/DSpace/releases).

Past releases and future releases are documented in the [RoadMap page](https://wiki.lyrasis.org/display/DSPACECRIS/Product+RoadMap)

## Documentation / Installation

Documentation is available on our [Documentation Wiki](https://wiki.lyrasis.org/display/DSPACECRIS/Technical+and+User+documentation) please check also the documentation from the parent DSpace project as basic features and principle are common and only described in the [DSpace documentation](https://wiki.lyrasis.org/display/DSDOC/).

The latest DSpace Installation instructions are available at:
https://wiki.lyrasis.org/display/DSDOC7x/Installing+DSpace

some extra step to initialize te DSpace-CRIS with proper default can be found in our documentation (see above)

Please be aware that, as a Java web application, DSpace-CRIS requires a database (PostgreSQL or Oracle), a servlet container (usually Tomcat) and a SOLR instance in order to function.

More information about these and all other prerequisites can be found in the Installation instructions above.

## Getting Help
DSpace-CRIS has a [dedicated slack channel](https://dspace-org.slack.com/messages/dspace-cris/) in the DSpace.org workspace.

Support can be received also via the DSpace support channels

DSpace provides public mailing lists where you can post questions or raise topics for discussion.
We welcome everyone to participate in these lists:

* [dspace-community@googlegroups.com](https://groups.google.com/d/forum/dspace-community) : General discussion about DSpace platform, announcements, sharing of best practices
* [dspace-tech@googlegroups.com](https://groups.google.com/d/forum/dspace-tech) : Technical support mailing list. See also our guide for [How to troubleshoot an error](https://wiki.lyrasis.org/display/DSPACE/Troubleshoot+an+error).
* [dspace-devel@googlegroups.com](https://groups.google.com/d/forum/dspace-devel) : Developers / Development mailing list

Great Q&A is also available under the [DSpace tag on Stackoverflow](http://stackoverflow.com/questions/tagged/dspace)

Additional support options are at https://wiki.lyrasis.org/display/DSPACE/Support

DSpace also has an active service provider network. If you'd rather hire a service provider to
install, upgrade, customize or host DSpace, then we recommend getting in touch with one of our
[Registered Service Providers](http://www.dspace.org/service-providers).

## Issue Tracker

DSpace-CRIS uses GitHub to track public reported issues:
* Backend (REST API) issues: https://github.com/4Science/DSpace/issues
* Frontend (User Interface) issues: https://github.com/4Science/dspace-angular/issues

## Testing

### Running Tests

By default, in DSpace, Unit Tests and Integration Tests are disabled. However, they are
run automatically by [GitHub Actions](https://github.com/4Science/DSpace/actions?query=workflow%3ABuild) for all Pull Requests and code commits.

* How to run both Unit Tests (via `maven-surefire-plugin`) and Integration Tests (via `maven-failsafe-plugin`):
  ```
  mvn install -DskipUnitTests=false -DskipIntegrationTests=false
  ```
* How to run _only_ Unit Tests:
  ```
  mvn test -DskipUnitTests=false
  ```
* How to run a *single* Unit Test
  ```
  # Run all tests in a specific test class
  # NOTE: failIfNoTests=false is required to skip tests in other modules
  mvn test -DskipUnitTests=false -Dtest=[full.package.testClassName] -DfailIfNoTests=false

  # Run one test method in a specific test class
  mvn test -DskipUnitTests=false -Dtest=[full.package.testClassName]#[testMethodName] -DfailIfNoTests=false
  ```
* How to run _only_ Integration Tests
  ```
  mvn install -DskipIntegrationTests=false
  ```
* How to run a *single* Integration Test
  ```
  # Run all integration tests in a specific test class
  # NOTE: failIfNoTests=false is required to skip tests in other modules
  mvn install -DskipIntegrationTests=false -Dit.test=[full.package.testClassName] -DfailIfNoTests=false

  # Run one test method in a specific test class
  mvn install -DskipIntegrationTests=false -Dit.test=[full.package.testClassName]#[testMethodName] -DfailIfNoTests=false
  ```
* How to run only tests of a specific DSpace module
  ```
  # Before you can run only one module's tests, other modules may need installing into your ~/.m2
  cd [dspace-src]
  mvn clean install

  # Then, move into a module subdirectory, and run the test command
  cd [dspace-src]/dspace-server-webapp
  # Choose your test command from the lists above
  ```

## License

DSpace-CRIS source code is freely available under a standard [BSD 3-Clause license](https://opensource.org/licenses/BSD-3-Clause).
The full license is available in the [LICENSE](LICENSE) file or online at http://www.dspace.org/license/
