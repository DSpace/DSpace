# H2 Flyway Database Migrations (i.e. Upgrades)

**WARNING:** DSpace does NOT support the use of the [H2 Database](http://www.h2database.com/)
in Production. Instead, DSpace uses the H2 Database to perform Unit Testing
during development.

By default, the DSpace Unit Testing environment configures H2 to run in memory
and initializes the H2 database using the scripts in this directory. See
`[src]/dspace-api/src/test/data/dspaceFolder/config/local.cfg`.

These database migrations are automatically called by [Flyway](http://flywaydb.org/)
in `DatabaseUtils`.

The H2 migrations in this directory all use H2's grammar/syntax.
For additional info see the [H2 SQL Grammar](https://www.h2database.com/html/grammar.html).


## More Information on Flyway

The SQL scripts in this directory are H2-specific database migrations. They are
used to automatically upgrade your DSpace database using [Flyway](http://flywaydb.org/).
As such, these scripts are automatically called by Flyway when the DSpace
`DatabaseUtils` initializes.

During that process, Flyway determines which version of DSpace your database is using
and then executes the appropriate upgrade script(s) to bring it up to the latest 
version. 

If any failures occur, Flyway will "rollback" the upgrade script which resulted
in an error and log the issue in the DSpace log file at `[dspace]/log/dspace.log.[date]`

**WARNING:** IT IS NOT RECOMMENDED TO RUN THESE SCRIPTS MANUALLY. If you do so,
Flyway will may throw failures the next time you startup DSpace, as Flyway will
not realize you manually ran one or more scripts.

Please see the Flyway Documentation for more information: http://flywaydb.org/




