DSpace does NOT support the use of the H2 Database (http://www.h2database.com/)
in Production. Instead, DSpace uses the H2 Database to perform Unit Testing
during development.

By default, the DSpace Unit Testing environment configures H2 to run in
"Oracle Mode" and initializes the H2 database using the scripts in this directory.

The H2 migrations in this directory are *based on* the Oracle Migrations
(/migrations/oracle/*.sql), but with some modifications in order to be valid
in H2. For reference see the H2 SQL Grammar:
http://www.h2database.com/html/grammar.html