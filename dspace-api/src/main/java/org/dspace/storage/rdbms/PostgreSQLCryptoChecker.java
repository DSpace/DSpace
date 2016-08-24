/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.log4j.Logger;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.FlywayCallback;

/**
 * This is a FlywayCallback class which automatically verifies that "pgcrypto"
 * is at the proper version before running any database migrations.
 * <P>
 * When running PostgreSQL, pgcrypto is REQUIRED by DSpace as it allows UUIDs
 * to be generated.
 * <P>
 * During a database "clean", this also de-registers "pgcrypto" proir to the
 * full database clean.
 *
 * @author Tim Donohue
 */
public class PostgreSQLCryptoChecker implements FlywayCallback
{
    private Logger log = Logger.getLogger(PostgreSQLCryptoChecker.class);

    /**
     * Check for pgcrypto (if needed). Throws an exception if pgcrypto is
     * not installed or needs an upgrade.
     * @param connection database connection
     */
    public void checkPgCrypto(Connection connection)
    {
        String dbType;
        try
        {
            dbType = DatabaseUtils.getDbType(connection);
        }
        catch(SQLException se)
        {
            throw new FlywayException("Unable to determine database type.", se);
        }

        // ONLY Check if this is a PostgreSQL database
        if(dbType!=null && dbType.equals(DatabaseUtils.DBMS_POSTGRES))
        {
            // If this is a PostgreSQL database, then a supported version
            // of the 'pgcrypto' extension MUST be installed to continue.
            
            // Check if pgcrypto is both installed & a supported version
            if(!PostgresUtils.isPgcryptoUpToDate())
            {
                throw new FlywayException("This PostgreSQL Database is INCOMPATIBLE with DSpace. The upgrade will NOT proceed. " +
                        "A supported version (>=" + PostgresUtils.PGCRYPTO_VERSION + ") of the '" + PostgresUtils.PGCRYPTO + "' extension must be installed! " +
                        "Please run 'dspace database info' for additional info/tips.");
            }
        }
    }

    /**
     * Remove pgcrypto (if necessary).
     * <P>
     * The pgcrypto extension MUST be removed before a clean or else errors occur.
     * This method checks if it needs to be removed and, if so, removes it.
     * @param connection database connection
     */
    public void removePgCrypto(Connection connection)
    {
        try
        {
            String dbType = DatabaseUtils.getDbType(connection);

            // ONLY remove if this is a PostgreSQL database
            if(dbType!=null && dbType.equals(DatabaseUtils.DBMS_POSTGRES))
            {
                // Get current schema
                String schema = DatabaseUtils.getSchemaName(connection);

                // Check if pgcrypto is in this schema
                // If so, it MUST be removed before a 'clean'
                if(PostgresUtils.isPgcryptoInSchema(schema))
                {
                    // remove the extension
                    try(Statement statement = connection.createStatement())
                    {
                        // WARNING: ONLY superusers can remove pgcrypto. However, at this point,
                        // we have already verified user acct permissions via PostgresUtils.checkCleanPermissions()
                        // (which is called prior to a 'clean' being triggered).
                        statement.execute("DROP EXTENSION " + PostgresUtils.PGCRYPTO + " CASCADE");
                    }
                }
            }
        }
        catch(SQLException e)
        {
            throw new FlywayException("Failed to check for and/or remove '" + PostgresUtils.PGCRYPTO + "' extension", e);
        }
    }

    @Override
    public void beforeClean(Connection connection) {
        // If pgcrypto is installed, remove it
        removePgCrypto(connection);
    }

    @Override
    public void afterClean(Connection connection) {

    }

    @Override
    public void beforeMigrate(Connection connection) {
        // Before migrating database, check for pgcrypto
        checkPgCrypto(connection);
    }

    @Override
    public void afterMigrate(Connection connection) {

    }

    @Override
    public void beforeEachMigrate(Connection connection, MigrationInfo migrationInfo) {

    }

    @Override
    public void afterEachMigrate(Connection connection, MigrationInfo migrationInfo) {

    }

    @Override
    public void beforeValidate(Connection connection) {

    }

    @Override
    public void afterValidate(Connection connection) {

    }

    @Override
    public void beforeBaseline(Connection connection) {
        // Before initializing database, check for pgcrypto
        checkPgCrypto(connection);
    }

    @Override
    public void afterBaseline(Connection connection) {

    }

    @Override
    public void beforeRepair(Connection connection) {

    }

    @Override
    public void afterRepair(Connection connection) {

    }

    @Override
    public void beforeInfo(Connection connection) {

    }

    @Override
    public void afterInfo(Connection connection) {

    }
}
