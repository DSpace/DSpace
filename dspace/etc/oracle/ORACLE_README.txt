DSpace on Oracle
Revision: 11-sep-04 dstuve


1   Introduction
2   Installing DSpace on Oracle
3   Porting notes for the curious


    DSpace is now tested to work with Oracle 10x, and should work with
version 9 as well.  The installation process is quite simple, involving
changinge a few configuration settings in dspace.cfg, and replacing a few
Postgres-specific sql files in etc/ with Oracle-specific ones.  Everything
is fully functional, although Oracle limits you to 4k of text in text fields
such as item metadata or collection descriptions.
    For people interested in switching from Postgres to Oracle, I know of
no tools that would do this automatically.  You will need to recreate the
community, collection, and eperson structure in the Oracle system, and then
use the item export and import tools to move your content over.

How to Install DSpace on Oracle

Things you need

    Oracle 9 or 10
    Oracle JDBC driver
    sequence update script from Akadia consulting:
       currently incseq.sql at: http://akadia.com/services/scripts/incseq.sql
       (put in your dspace source /etc directory)
         
Installation procedure 

 1. Create adatabase for DSpace. Make sure that the character set is one
of the Unicode character sets.  DSpace uses UTF8 natively, and I would
suggest that the Oracle database use the same character set.  Create
a user account for DSpace (we use 'dspace',) and ensure that it has
permissions to add and remove tables in the database.

 2. Edit the config/dspace.cfg file in your source directory for the
  following settings:

        db.name   = oracle
        db.url    = jdbc.oracle.thin:@//host:port/dspace
        db.driver = oracle.jdbc.OracleDriver

 3. Go to etc/oracle in your source directory and copy the contents
  to their parent directory, overwriting the versions in the parent:

        cd dspace_source/etc/oracle
        cp * ..
   
  You now have Oracle-specific .sql files in your etc directory, and
  your dspace.cfg is modified to point to your Oracle database and
  are ready to continue with a normal DSpace install, skipping the
  Postgres setup steps.

 NOTE**** DSpace uses sequences to generate unique object IDs - beware
  Oracle sequences, which are said to lose their values when doing
  a database export/import, say restoring from a backup.  Be sure
  to run the script etc/udpate-sequences.sql.


Oracle Porting Notes for the Curious

Oracle is missing quite a number of cool features found in Postgres, so
workarounds had to be found, most of which are hidden behind tests of
the db.name configuration parameter in dspace.cfg.  If the db.name is
set to Oracle the workarounds are activated:

Oracle doesn't like ';' characters in JDBC SQL - they have all been removed
from the DSpace source, including code in the .sql file reader to strip ;'s.

browse code - LIMIT and OFFSET is used to limit browse results, and an
Oracle-hack is used to limit the result set to a given size

Oracle has no boolean data type, so a new schema file was created that
uses INTEGERs and code is inserted everywhere to use 0 for false
and 1 for true if the db.name is Oracle

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

DatabaseManager had to have some of the type checking changed, because Oracle's
JDBC driver is reporting INTEGERS as type DECIMAL.

Oracle doesn't like it when you reference table names in lower case when
getting JDBC metadata for the tables, so they are converted in TableRow
to upper case.
