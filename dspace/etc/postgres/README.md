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

## Using the update-sequences.sql script

The `update-sequences.sql` script in this directory may still be used to update
your internal database counts if you feel they have gotten out of "sync". This
may sometimes occur after large restores of content (e.g. when using the DSpace
[AIP Backup and Restore](https://wiki.duraspace.org/display/DSDOC5x/AIP+Backup+and+Restore) 
feature).

This `update-sequences.sql` script can be run manually. It will not harm your 
database (or its contents) in any way. It just ensures all database counts (i.e.
sequences) are properly set to the next available value.

## Using the ex- and import-c-c.sql script
The ex- and import-c-c.sql contains SQL scripts for export and import of the tree of communities and collections into a new fresh DSpace installation.

This can preferably be used to create a basic data set for test and development systems, containing the same community-collection-tree as in the production system but without any individual-related data.  
  
### Preconditions
* There exists (on the source as well as on the target server) a directory `/tmp/dspace-export`. On the source server must the user postgres be allowed to write to this directory.
* There exists a fresh DSpace target database (yes, completely fresh, without any preexisting contents or users!)

### Execution steps
#### On the source database (tubIT)
`psql -d <db-name> -a -f "export-c-c.sql"`

#### On the target database
1. Copy the exported files to the target server
  1. `psql -U postgres -d <db-name> -a -f "import-c-c.sql"`
  1. `psql -U postgres -d <db-name> -a -f "[dspace-install]/etc/postgres/update-sequences.sql"`
  1. `[dspace-install]/bin/dspace create-administrator`

#### Harvesting Items 
The result of the import is a DSpace data set with the same communities, collections, epersongroups and metadatafields as in the source system but without users or items.

Items can now be harvested from the production system using OAI-ORE.
 
1. Activate the xmlui in the target system (if you are not already using it)
  1. Follow the [harvesting instructions in the DSpace documentation](https://wiki.duraspace.org/display/DSDOC6x/XMLUI+Configuration+and+Customization#XMLUIConfigurationandCustomization-HarvestingItemsfromXMLUIviaOAI-OREorOAI-PMH). Note: xmlui is required only in the target system, not in the source system. 
  1. Use DIM (DSpace Intermediate Metadata) as Metadata Format to get the original metadata fields.  
