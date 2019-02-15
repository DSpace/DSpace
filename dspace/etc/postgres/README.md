# DSpace Database Now Upgrades Automatically

AS OF DSPACE 5, the DSpace database now upgrades itself AUTOMATICALLY.

Therefore, all `database_schema*.sql` files have been removed. Starting
with DSpace 4.x -> 5.0 upgrade, you will no longer need to manually run any
SQL scripts to upgrade your database.

Please see the [5.0 Upgrade Instructions](https://wiki.duraspace.org/display/DSDOC5x/Upgrading+DSpace)
for more information on upgrading to DSpace 5.


## More info on automatic database upgrades

As of DSpace 5.0, we now use [Flyway DB](http://flywaydb.org/) along with the
SQL scripts embedded in the `dspace-api.jar` to automatically keep your DSpace
database up-to-date. These scripts are now located in the source code at:
`[dspace-src]/dspace-api/src/main/resources/org/dspace/storage/rdbms/sqlmigration/postgres`

As Flyway automates the upgrade process, you should NEVER run these SQL scripts
manually. For more information, please see the `README.md` in the scripts directory.
