# DRUM Configuration Customization

This document describes how to override the DSpace configuration to support
DRUM extensions.

## Useful Resources

* <https://wiki.lyrasis.org/display/DSDOC8x/Advanced+Customisation>
* <https://wiki.lyrasis.org/display/DSDOC8x/Configuration+Reference>
* <https://wiki.lyrasis.org/display/DSDOC8x/Storage+Layer>

## Dockerfile configurations

Stock DSpace provides the following Dockerfiles for running DSpace:

* Dockerfile
* Dockerfile.cli
* Dockerfile.dependencies
* Dockerfile.test

The "Dockerfile" is used to generate the Docker images used in production.

The following Dockerfiles were added to support local development:

* Dockerfile.ant - Creates the Docker image for running Apache Ant
* Dockefile.dev - The "local development" equivalent of the "Dockerfile". This
  Docker image fully constructs a DRUM Docker image for use by Docker Compose.
* Dockerfile.dev-base - Used for "Quick Builds", to generate a "base" build
  that encompasses the "dspace-api" package. Used with the
  "Dockefile.dev-additions" file.
* Dockerfile.dev-additions - Used for "Quick Builds", to build the "overlays"
  consisting of the customizations in the the "dspace/modules/additions" and
  "dspace/modules/server" directories.

The intent behind the "Dockerfile.dev-base" and "Dockerfile.dev-additions" files
is that when doing local development, changes in the "dspace/modules/additions"
and "dspace/modules/server" directories only require the
"Dockerfile.dev-additions" image to be regenerated, which is much faster than
running "Dockerfile.dev".

Finally, the "Dockerfile.ci" file supports generating a Docker image for use
by Jenkins for continuous integration setup and testing.

---

**Note:** Starting in DSpace 8, the stock files use Spring Boot and an
"embedded Tomcat" server to run the back-end server. This turned out to be
incompatible with the DSpace customization mechanism, when customizations are
added to the "dspace/modules/server" directory (see DSpace Issue 9987 -
<https://github.com/DSpace/DSpace/issues/9987)>).

A fix to restore the the "dspace/modules/server" customizations was provided
to DSpace in DSpace Pull 10043 (<https://github.com/DSpace/DSpace/pull/10043>).

These customizations can be removed when upgrading to a DSpace version that
contains the changes.

---

## .github/workflows/build.yml

Commented out the "codecov" task, because UMD does not have an appropriate key
to upload code coverage results to codecov.io, which was causing builds to
be marked as "Failed" in GitHub.

## dspace/modules/[additions|server]

All source code and resources for DRUM customizations should be placed in
either the "dspace/modules/additions" or "dspace/modules/server" directory.

## dspace/config/dspace.cfg

The "dspace/config/dspace.cfg" file contains the default DSpace configuration.
In general, changes *should not* be made to this file.

DRUM customization should be added to the "dspace/config/local.cfg.EXAMPLE"
file, which, when deployed, should be copied to "dspace/config/local.cfg" (see
the [Docker Development Environment](DockerDevelopmentEnvironment.md).

## XML File Changes

### Modifying existing DSpace configuration files

The "dspace/config" directory contains XML files such as:

* dspace/config/hibernate.cfg.xml
* dspace/config/spring/api/core-services.xml
* dspace/config/spring/api/core-dao-services.xml

which may need to be changed to support new database objects and REST services.
There does not appear to be a way to override these files, so changes for DRUM
must be made to the DSpace-provided files.

Changing these files may cause the unit/integration tests in the stock DSpace
modules (such as "dspace-api") to fail, as these modules will not have access
to the Java classes specified in the XML files. To fix this, place the
*stock* version of the files into the "test/data/dspaceFolder/config" directory
of the module. For example, for the "dspace-api" module, the "stock" versions
of the above files would be placed in:

* dspace-api/src/test/data/dspaceFolder/config/hibernate.cfg.xml
* dspace-api/src/test/data/dspaceFolder/config/spring/api/core-services.xml
* dspace-api/src/test/data/dspaceFolder/config/spring/api/core-dao-services.xml

The UMD-customized versions of these files (from the "dspace/config/" directory)
should be placed in:

* dspace/modules/additions/src/test/data/dspaceFolder/config/hibernate.cfg.xml
* dspace/modules/additions/src/test/data/dspaceFolder/config/spring/api/core-services.xml
* dspace/modules/additions/src/test/data/dspaceFolder/config/spring/api/core-dao-services.xml

#### bitstore.xml

The "dspace/config/spring/api/bitstore.xml" file has been customized to
support multiple asset directories. To support the "dspace-api" tests,
the "stock" DSpace "dspace/config/spring/api/bitstore.xml" file should be
placed in

* dspace-api/src/test/data/dspaceFolder/config/spring/api/bitstore.xml

#### discovery.xml

The "dspace/config/spring/api/discovery.xml" file has been modified from the
stock DSpace version. The support the "dspace-server-webapp" tests, the
"stock" DSpace "dspace/config/spring/api/discovery.xml" file should be placed in

* dspace-server-webapp/src/test/data/dspaceFolder/config/spring/api/discovery.xml

### New DRUM-specific XML Files

The addition of DRUM-specific configuration files to the "dspace/config"
directory that reference DRUM-specific Java files will also cause the
unit/integration tests in the stock DSpace modules (such as "dspace-api") to
fail.

New DRUM-specific configuration files added to the "dspace/config" directory
should have a "drum-" prefix, for example:

* config/spring/api/drum-dao-services.xml
* config/spring/api/drum-factory-services.xml
* config/spring/api/drum-services.xml

Using the "drum-" prefix allows the configuration files to be automatically
filtered out by the "testEnvironment" task in the
"src/main/assembly/testEnvironment.xml" assembly file that Maven uses to
build the configuration for the stock unit/integration tests.

## Database Schema Changes

Database schema changes to support new entities are implemented as Flyway
(<https://flywaydb.org/>) migration files, in the
"dspace/modules/additions/src/main/resources/org/dspace/storage/rdbms/sqlmigration/"
directory. There are two subdirectories, corresponding to the database
applications being used:

* h2 - used for unit/integration testing
* postgres - used for development/production

When adding a migration, the migration should be added to *both* subdirectories.
Unfortunately, while the SQL used by the H2 and Postgres databases is largely
the same, there are some differences. See
<http://www.h2database.com/html/commands.html> and associated pages for help
with the "h2" SQL.

The following sections also provide some guidance for specific situations.

### Altering Primary Keys

In the Postgres migration scripts, it appears that changing a table's primary
key requires a single command, i.e.:

```sql
ALTER TABLE etdunit ADD PRIMARY KEY (uuid);
```

In H2, this is not possible, as H2 thinks a second primary key is being created.
Moreover, if you attempt to drop the primary key before changing it, i.e.:

```sql
ALTER TABLE etdunit DROP PRIMARY KEY;
```

this will fail with an error message similar:

```bash
[ERROR] Failures:
[ERROR]   CASAuthenticationTest>AbstractUnitTest.initDatabase:80 Error initializing database: Flyway migration error occurred: Migration V6.2_2018.04.05__drum-0_LIBDRUM-511_hibernate_migration_of_customization.sql failed
---------------------------------------------------------------------------------------------
SQL State  : 90085
Error Code : 90085
Message    : Index "PRIMARY_KEY_FF" belongs to constraint "CONSTRAINT_D"; SQL statement:
...
```

indicating that there is an "implicit" constraint (named "CONSTRAINT_D" in
the example).

This implicit constraint must be removed, so to actually change the primary key
in the example, the following commands are necessary:

```sql
ALTER TABLE etdunit DROP CONSTRAINT CONSTRAINT_D CASCADE;
ALTER TABLE etdunit ADD PRIMARY KEY (uuid);
```

The "CASCADE" option deletes the primary key associated with the constraint.

The implicit constraint names are believed to be stable, so while kludgy, this
should work consistently.

### Renaming Columns

The syntax for renaming columns is slightly different. Given a Postgres command
of:

```sql
ALTER TABLE collection2workflowitem RENAME COLUMN collection_id to collection_legacy_id;
```

the equivalent H2 command is:

```sql
ALTER TABLE collection2workflowitem ALTER COLUMN collection_id RENAME to collection_legacy_id;
```

### Adding Column References

Give a Postgres command to add a column with a foreign key:

```sql
ALTER TABLE collection2workflowitem ADD COLUMN collection_id UUID REFERENCES Collection(uuid);
```

the H2 equivalent is the following two commands (one to add the column, one to
add the foreign key):

```sql
ALTER TABLE collection2workflowitem ADD COLUMN collection_id UUID;
ALTER TABLE collection2workflowitem ADD FOREIGN KEY (collection_id) REFERENCES Collection(uuid);
``

### UPDATE/SET

Given a Postgres command to update a column based on an SQL query:

```sql
UPDATE epersongroup2unit SET eperson_group_id = epersongroup.uuid FROM epersongroup WHERE epersongroup2unit.eperson_group_legacy_id = epersongroup.eperson_group_id;
```

the H2 equivalent is a slight reformatting of the command (wrapping the right
side of the equals in an SQL subselect):

```sql
UPDATE epersongroup2unit SET eperson_group_id = (SELECT epersongroup.uuid FROM epersongroup WHERE epersongroup2unit.eperson_group_legacy_id = epersongroup.eperson_group_id);
```

### gen_random_uuid()

The "gen_random_uuid()" function is provided by the Postgres "pgcrypto"
extension, and so is not available to H2. Simply replaced with "random_uuid()"
function.

When using the "random_uuid()" function, the "UNIQUE" directive (if present)
should not be used, as H2 reports it as a syntax error. Instead a constraint
can be added.

For example,

```sql
ALTER TABLE etdunit ADD COLUMN uuid UUID DEFAULT gen_random_uuid() UNIQUE;
```

would be converted to:

```sql
ALTER TABLE etdunit ADD COLUMN uuid UUID DEFAULT random_uuid();
ALTER TABLE etdunit ADD CONSTRAINT etdunit_uuid_unique UNIQUE(uuid);
```

The "etdunit_uuid_unique" constraint name is arbitrary, but useful if it needs
to be changed in a later migration (otherwise H2 simply assigns an opaque name).
