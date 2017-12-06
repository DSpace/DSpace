# PostgreSQL Flyway Database Migrations (i.e. Upgrades)

The SQL scripts in this directory are PostgreSQL-specific database migrations. They are
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


