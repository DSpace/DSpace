# DRUM Configuration Customization

This document describes how to override the DSpace configuration to support
DRUM extensions.

## Useful Resources

* <https://wiki.lyrasis.org/display/DSDOC7x/Advanced+Customisation>
* https://wiki.lyrasis.org/display/DSDOC7x/Configuration+Reference
* https://wiki.lyrasis.org/display/DSDOC7x/Storage+Layer

## dspace/modules/additions

All source code and resources for DRUM customizations should be placed in the
"dspace/modules/additions" directory.

## dspace/config/dspace.cfg

The "dspace/config/dspace.cfg" file contains the default DSpace configuration.
In general, changes *should not* be made to this file.

DRUM customization should be added to the "dspace/config/local.cfg.TEMPLATE"
file, which, when deployed, should be copied to "dspace/config/local.cfg" (see
the [DRUM 7 Docker Development Environment](Drum7DockerDevelopmentEnvironment.md).

## XML File Changes

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

* dspace-api/test/data/dspaceFolder/config/hibernate.cfg.xml
* dspace-api/test/data/dspaceFolder/spring/api/core-services.xml
* dspace-api/test/data/dspaceFolder/config/spring/api/core-dao-services.xml

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
Moveover, if you attempt to drop the primary key before changing it, i.e.:

```sql
ALTER TABLE etdunit DROP PRIMARY KEY;
```

this will fail with an error message similar:

```bash
[ERROR] Failures:
[ERROR]   UnitTest>AbstractUnitTest.initDatabase:80 Error initializing database: Flyway migration error occurred:
Migration V6.2_2018.04.05__drum-0_LIBDRUM-511_hibernate_migration_of_customization.sql failed
---------------------------------------------------------------------------------------------
SQL State  : 90085
Error Code : 90085
Message    : Index "PRIMARY_KEY_FF" belongs to constraint "CONSTRAINT_F267"; SQL statement:
ALTER TABLE etdunit DROP PRIMARY KEY [90085-187]
...
```

indicating that there is an "implicit" constraint (named "CONSTRAINT_F267" in
the example).

This implicit constraint must be removed, so to actually change the primary key
in the example, the following commands are necessary:

```sql
ALTER TABLE etdunit DROP CONSTRAINT CONSTRAINT_F267;
ALTER TABLE etdunit DROP PRIMARY KEY;
ALTER TABLE etdunit ADD PRIMARY KEY (uuid);
```

The implicit contraint names are believed to be stable, so while kludgy, this
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

would be convertd to:

```sql
ALTER TABLE etdunit ADD COLUMN uuid UUID DEFAULT random_uuid();
ALTER TABLE etdunit ADD CONSTRAINT etdunit_uuid_unique UNIQUE(uuid);
```

The "etdunit_uuid_unique" constraint name is arbitrary, but useful if it needs
to be changed in a later migration (otherwise H2 simply assigns an opaque name).
