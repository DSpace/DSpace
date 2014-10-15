# DSpace Database Now Upgrades Automatically

AS OF DSPACE 5.0, the DSpace database now upgrades itself AUTOMATICALLY.

Therefore, all `database_schema*.sql` files have been removed. Starting
with DSpace 4.x -> 5.0 upgrade, you will no longer need to manually run any 
SQL scripts to upgrade your database.

However, if you have not yet upgraded to DSpace 4.x, YOU MUST MANUALLY DO SO.
Those manual `database_schema*.sql` scripts can still be found in the 
DSpace 4.x source code at:

https://github.com/DSpace/DSpace/tree/dspace-4_x/dspace/etc/postgres/

## More info on automatic database upgrades

As of DSpace 5.0, we now use [Flyway DB](http://flywaydb.org/) along with 
the scripts under `[dspace]/etc/migrations/oracle` to automatically keep your
DSpace database up-to-date.

As Flyway automates the upgrade process, you should NEVER run these scripts
manually. For more information, please see the `README` in the scripts directory.

## Using the update-sequences.sql script

The `update-sequences.sql` script in this directory may still be used to update
your internal database counts if you feel they have gotten out of "sync". This
may sometimes occur after large restores of content (e.g. when using the DSpace
[AIP Backup and Restore](https://wiki.duraspace.org/display/DSDOC5x/AIP+Backup+and+Restore) 
feature).

This `update-sequences.sql` script can be run manually. It will not harm your 
database (or its contents) in any way. It just ensures all database counts (i.e.
sequences) are properly set to the next available value.
