# Oracle Flyway Database Migrations (i.e. Upgrades)

---
WARNING: Oracle Support is deprecated.
See https://github.com/DSpace/DSpace/issues/8214
---

The SQL scripts in this directory are Oracle-specific database migrations. They are
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

## Oracle Porting Notes for the Curious

Oracle is missing quite a number of cool features found in Postgres, so
workarounds had to be found, most of which are hidden behind tests in 
DatabaseManager.  If Oracle is your DBMS, the workarounds are activated:

Oracle doesn't like ';' characters in JDBC SQL - they have all been removed
from the DSpace source, including code in the .sql file reader to strip ;'s.

browse code - LIMIT and OFFSET is used to limit browse results, and an
Oracle-hack is used to limit the result set to a given size

Oracle has no boolean data type, so a new schema file was created that
uses NUMBER(1) (AKA 'integers') and code is inserted everywhere to use 0 for
false and 1 for true if DSpace is using Oracle.

Oracle doesn't have a TEXT data type either, so TEXT columns are defined
as VARCHAR2 in the Oracle-specific schema.

Oracle doesn't allow dynamic naming for objects, so our cute trick to
derive the name of the sequence by appending _seq to the table name
in a function doesn't work in Oracle - workaround is to insert Oracle
code to generate the name of the sequence and then place that into
our SQL calls to generate a new ID.

Oracle doesn't let you directly set the value of sequences, so
update-sequences.sql is forced to use a special script sequpdate.sql
to update the sequences.

Bitstream had a column 'size' which is a reserved word in Oracle,
so this had to be changed to 'size_bytes' with corresponding code changes.

VARCHAR2 has a limit of 4000 characters, so DSpace text data is limited to 4k. 
Going to the CLOB data type can get around that, but seemed like too much effort
for now.  Note that with UTF-8 encoding that 4k could translate to 1300
characters worst-case (every character taking up 3 bytes is the worst case
scenario.)

### UPDATE 5 April 2007

CLOBs are now used as follows:
MetadataValue:text_value
Community:introductory_text
Community:copyright_text
Collection:introductory_text
Collection:license
Collection:copyright_text

DatabaseManager had to have some of the type checking changed, because Oracle's
JDBC driver is reporting INTEGERS as type DECIMAL.

Oracle doesn't like it when you reference table names in lower case when
getting JDBC metadata for the tables, so they are converted in TableRow
to upper case.

### UPDATE 27 November 2012

Oracle complains with ORA-01408 if you attempt to create an index on a column which
has already had the UNIQUE contraint added (such an index is implicit in maintaining the uniqueness
of the column). See [DS-1370](https://jira.duraspace.org/browse/DS-1370) for details.
