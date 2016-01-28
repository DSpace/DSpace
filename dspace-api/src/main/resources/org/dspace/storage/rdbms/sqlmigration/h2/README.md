# H2 Flyway Database Migrations (i.e. Upgrades)

**WARNING:** DSpace does NOT support the use of the [H2 Database](http://www.h2database.com/)
in Production. Instead, DSpace uses the H2 Database to perform Unit Testing
during development.

By default, the DSpace Unit Testing environment configures H2 to run in
"Oracle Mode" and initializes the H2 database using the scripts in this directory.
These database migrations are automatically called by [Flyway](http://flywaydb.org/)
when the `DatabaseManager` initializes itself (see `initializeDatabase()` method).

The H2 migrations in this directory are *based on* the Oracle Migrations, but
with some modifications in order to be valid in H2.

## Oracle vs H2 script differences

One of the primary differences between the Oracle scripts and these H2 ones
is in the syntax of the `ALTER TABLE` command. Unfortunately, H2's syntax for
that command differs greatly from Oracle (and PostgreSQL as well).

Most of the remainder of the scripts contain the exact Oracle syntax (which is 
usually valid in H2). But, to you can always `diff` scripts of the same name
for further syntax differences.

For additional info see the [H2 SQL Grammar](http://www.h2database.com/html/grammar.html).

## More Information on Flyway

The SQL scripts in this directory are H2-specific database migrations. They are
used to automatically upgrade your DSpace database using [Flyway](http://flywaydb.org/).
As such, these scripts are automatically called by Flyway when the DSpace
`DatabaseManager` initializes itself (see `initializeDatabase()` method). During
that process, Flyway determines which version of DSpace your database is using
and then executes the appropriate upgrade script(s) to bring it up to the latest 
version. 

If any failures occur, Flyway will "rollback" the upgrade script which resulted
in an error and log the issue in the DSpace log file at `[dspace]/log/dspace.log.[date]`

**WARNING:** IT IS NOT RECOMMENDED TO RUN THESE SCRIPTS MANUALLY. If you do so,
Flyway will may throw failures the next time you startup DSpace, as Flyway will
not realize you manually ran one or more scripts.

Please see the Flyway Documentation for more information: http://flywaydb.org/




