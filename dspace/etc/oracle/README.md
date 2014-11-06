# DSpace Database Now Upgrades Automatically

AS OF DSPACE 5, the DSpace database now upgrades itself AUTOMATICALLY.

Therefore, all `database_schema*.sql` files have been removed. Starting
with DSpace 4.x -> 5.0 upgrade, you will no longer need to manually run any
SQL scripts to upgrade your database.

Please see the [5.0 Upgrade Instructions](https://wiki.duraspace.org/display/DSDOC5x/Upgrading+to+5.x)
for more information on upgrading to DSpace 5.


## More info on automatic database upgrades

As of DSpace 5.0, we now use [Flyway DB](http://flywaydb.org/) along with the
SQL scripts embedded in the `dspace-api.jar` to automatically keep your DSpace
database up-to-date. These scripts are now located in the source code at:
`[dspace-src]/dspace-api/src/main/java/resources/org/dspace/storage/rdbms/sqlmigration/oracle`

As Flyway automates the upgrade process, you should NEVER run these SQL scripts
manually. For more information, please see the `README.md` in the scripts directory.

## Using the update-sequences.sql script

The `update-sequences.sql` script in this directory may still be used to update
your internal database counts if you feel they have gotten out of "sync". This
may sometimes occur after large restores of content (e.g. when using the DSpace
[AIP Backup and Restore](https://wiki.duraspace.org/display/DSDOC5x/AIP+Backup+and+Restore) 
feature).

This `update-sequences.sql` script can be run manually. It will not harm your 
database (or its contents) in any way. It just ensures all database counts (i.e.
sequences) are properly set to the next available value.