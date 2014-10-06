DSpace Database Now Upgrades Automatically
##############################################

AS OF DSPACE 5.0, the DSpace database now upgrades AUTOMATICALLY.

Therefore, all "database_schema*.sql" files have been removed. Starting
with DSpace 4.0 -> 5.0 upgrade, you will no longer need to manually run any 
SQL scripts.

However, if you have not yet upgraded to DSpace 4.0, YOU MUST MANUALLY DO SO.
Those manual "database_schema*.sql" scripts can still be found in the 
DSpace 4.0 source code at:

https://github.com/DSpace/DSpace/tree/dspace-4_x/dspace/etc/oracle/


MORE INFO (for Developers):
---------------------------

As of DSpace 5.0, we now use Flyway DB (http://flywaydb.org/) along with 
the scripts under [dspace]/etc/migrations/ to automatically keep your DSpace
database up-to-date.
