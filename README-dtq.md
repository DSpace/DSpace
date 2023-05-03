
# Dataquest wiki
When installing, please checkout wiki on this repo

## [Wiki](https://github.com/dataquest-dev/DSpace/wiki)


## Issue Tracker

DSpace uses GitHub to track issues:
* Backend (REST API) issues: https://github.com/DSpace/DSpace/issues
* Frontend (User Interface) issues: https://github.com/DSpace/dspace-angular/issues


# Missing or unfinished features - migration issues
- License labels are missing icons, so you won't see license icons in the Item. Issue: https://github.com/dataquest-dev/DSpace/issues/262
- Item View is missing history table, that means you won't see other versions of the Item. Issue: https://github.com/dataquest-dev/DSpace/issues/256
- Item's metadata has wrong separator in the metadata field, it should be `;`, but it is `@@`. Issue: https://github.com/dataquest-dev/DSpace/issues/261
- Item has imported only one type. Issue: https://github.com/dataquest-dev/DSpace/issues/255
- Publisher metadata value is imported into wrong metadata field, it is `creativework.publisher` instead of `dc.publisher`. Issue: https://github.com/dataquest-dev/DSpace/issues/254
- Language is not properly showed in the Item View, it is `ces, zxx` instead of `Czech, Nolinguistic content`. Issue: https://github.com/dataquest-dev/DSpace/issues/253

### Tables which are not migrated yet:
- resurcepolicy, 
- schema_version, 
- subscription, 
- tasklistitem, 
- webapp, 
- license_ressource_user_allowance, 
- user_metadata, 
- verification_token

### Tables which are missing in the DSpace7.*., but they are in the CLARIN-DSpace5.* (they are not planned to migrate)
- userconnection, 
- license_file_download_statistic, 
- piwik_reposr, 
- shibboleth_attribute_mapping


## Testing

### Running Tests

By default, in DSpace, Unit Tests and Integration Tests are disabled. However, they are
run automatically by [GitHub Actions](https://github.com/DSpace/DSpace/actions?query=workflow%3ABuild) for all Pull Requests and code commits.

* Necessary parameters for running every Unit Test command to pass JVM memory flags: `test.argLine`, `surefireJacoco`. Example:
  ```
  mvn <ARGS> -Dtest.argLine=-Xmx1024m -DsurefireJacoco=-XX:MaxPermSize=256m
  ```
* Necessary parameters for running every Integration Test command to pass JVM memory flags: `test.argLine`, `failsafeJacoco`. Example:
  ```
  mvn <ARGS> -Dtest.argLine=-Xmx1024m -DfailsafeJacoco=-XX:MaxPermSize=256m
  ```
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
  
  # Example: mvn test -DskipUnitTests=false -Dtest=org.dspace.content.ItemTest.java -DfailIfNoTests=false -Dtest.argLine=-Xmx1024m -DsurefireJacoco=-XX:MaxPermSize=256m
  # Debug: -Dmaven.surefire.debug

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
  
  # Example:
  mvn install -DskipIntegrationTests=false -Dit.test=org.dspace.content.ItemIT.java#dtqExampleTest -Dtest.argLine=-Xmx1024m -DfailsafeJacoco=-XX:MaxPermSize=256m -DfailIfNoTests=false -Dcheckstyle.skip -Dmaven.failsafe.debug -Dlicense.skip
  # Debug: -Dmaven.failsafe.debug
  # Skip checking of licensing headers: -Dlicense.skip
  # Skip checkstyle: -Dcheckstyle.skip

  ```
  How to turn off checkstyle in tests: `-Dcheckstyle.skip`
  ```
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

DSpace source code is freely available under a standard [BSD 3-Clause license](https://opensource.org/licenses/BSD-3-Clause).
The full license is available in the [LICENSE](LICENSE) file or online at http://www.dspace.org/license/

DSpace uses third-party libraries which may be distributed under different licenses. Those licenses are listed
in the [LICENSES_THIRD_PARTY](LICENSES_THIRD_PARTY) file.

License check command: `mvn license:check`

