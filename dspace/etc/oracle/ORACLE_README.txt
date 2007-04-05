DSpace on Oracle
Revision: 11-sep-04 dstuve

(Installation notes moved to main DSpace documentation)


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

==UPDATE 5 April 2007==
CLOBs are now used as follows:
MetadataValue:text_value
Community:introductory_text
Community:copyright_text
Collection:introductory_text
Collection:license
Collection:copyright_text
==                   ==

DatabaseManager had to have some of the type checking changed, because Oracle's
JDBC driver is reporting INTEGERS as type DECIMAL.

Oracle doesn't like it when you reference table names in lower case when
getting JDBC metadata for the tables, so they are converted in TableRow
to upper case.
