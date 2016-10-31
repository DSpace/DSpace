/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import static org.dspace.storage.rdbms.DatabaseUtils.getSchemaName;
import org.flywaydb.core.api.FlywayException;

/**
 * Database utility class specific to Postgres.
 * This class contains tools and methods which are useful in determining
 * the status of a PostgreSQL database backend.  It's a companion class
 * to DatabaseUtils, but PostgreSQL specific.
 *
 * @author Tim Donohue
 */
public class PostgresUtils
{
    // PostgreSQL pgcrypto extention name, and required versions of Postgres & pgcrypto
    public static final String PGCRYPTO="pgcrypto";
    public static final Double PGCRYPTO_VERSION=1.1;
    public static final Double POSTGRES_VERSION=9.4;

    /**
     * Get version of pgcrypto extension available. The extension is "available"
     * if it's been installed via operating system tools/packages. It also
     * MUST be installed in the DSpace database (see getPgcryptoInstalled()).
     * <P>
     * The pgcrypto extension is required for Postgres databases
     * @param connection database connection
     * @return version number or null if not available
     */
    protected static Double getPgcryptoAvailableVersion(Connection connection)
    {
        Double version = null;

        String checkPgCryptoAvailable = "SELECT default_version AS version FROM pg_available_extensions WHERE name=?";

        // Run the query to obtain the version of 'pgcrypto' available
        try (PreparedStatement statement = connection.prepareStatement(checkPgCryptoAvailable))
        {
            statement.setString(1,PGCRYPTO);
            try(ResultSet results = statement.executeQuery())
            {
                if(results.next())
                {
                    version = results.getDouble("version");
                }
            }
        }
        catch(SQLException e)
        {
            throw new FlywayException("Unable to determine whether 'pgcrypto' extension is available.", e);
        }

        return version;
    }

    /**
     * Get version of pgcrypto extension installed in the DSpace database.
     * <P>
     * The pgcrypto extension is required for Postgres databases to support
     * UUIDs.
     * @param connection database connection
     * @return version number or null if not installed
     */
    protected static Double getPgcryptoInstalledVersion(Connection connection)
    {
        Double version = null;

        String checkPgCryptoInstalled = "SELECT extversion AS version FROM pg_extension WHERE extname=?";

        // Run the query to obtain the version of 'pgcrypto' installed on this database
        try (PreparedStatement statement = connection.prepareStatement(checkPgCryptoInstalled))
        {
            statement.setString(1,PGCRYPTO);
            try(ResultSet results = statement.executeQuery())
            {
                if(results.next())
                {
                    version = results.getDouble("version");
                }
            }
        }
        catch(SQLException e)
        {
            throw new FlywayException("Unable to determine whether 'pgcrypto' extension is installed.", e);
        }

        return version;
    }

    /**
     * Check if the pgcrypto extension is BOTH installed AND up-to-date.
     * <P>
     * This requirement is only needed for PostgreSQL databases.
     * It doesn't matter what schema pgcrypto is installed in, as long as it exists.
     * @return true if everything is installed and up-to-date. False otherwise.
     */
    public static boolean isPgcryptoUpToDate()
    {
        // Get our configured dataSource
        DataSource dataSource = DatabaseUtils.getDataSource();

        try(Connection connection = dataSource.getConnection())
        {
            Double pgcryptoInstalled = getPgcryptoInstalledVersion(connection);

            // Check if installed & up-to-date in this DSpace database
            if(pgcryptoInstalled!=null && pgcryptoInstalled.compareTo(PGCRYPTO_VERSION)>=0)
            {
                return true;
            }

            return false;
        }
        catch(SQLException e)
        {
            throw new FlywayException("Unable to determine whether 'pgcrypto' extension is up-to-date.", e);
        }
    }

    /**
     * Check if the pgcrypto extension is installed into a particular schema
     * <P>
     * This allows us to check if pgcrypto needs to be REMOVED prior to running
     * a 'clean' on this database. If pgcrypto is in the same schema as the
     * dspace database, a 'clean' will require removing pgcrypto FIRST.
     *
     * @param schema name of schema
     * @return true if pgcrypto is in this schema. False otherwise.
     */
    public static boolean isPgcryptoInSchema(String schema)
    {
        // Get our configured dataSource
        DataSource dataSource = DatabaseUtils.getDataSource();

        try(Connection connection = dataSource.getConnection())
        {
            // Check if pgcrypto is installed in the current database schema.
            String pgcryptoInstalledInSchema = "SELECT extversion FROM pg_extension,pg_namespace " +
                                                 "WHERE pg_extension.extnamespace=pg_namespace.oid " +
                                                 "AND extname=? " +
                                                 "AND nspname=?;";
            Double pgcryptoVersion = null;
            try (PreparedStatement statement = connection.prepareStatement(pgcryptoInstalledInSchema))
            {
                statement.setString(1,PGCRYPTO);
                statement.setString(2, schema);
                try(ResultSet results = statement.executeQuery())
                {
                    if(results.next())
                    {
                        pgcryptoVersion = results.getDouble("extversion");
                    }
                }
            }

            // If a pgcrypto version returns, it's installed in this schema
            if(pgcryptoVersion!=null)
                return true;
            else
                return false;
        }
        catch(SQLException e)
        {
            throw new FlywayException("Unable to determine whether 'pgcrypto' extension is installed in schema '" + schema + "'.", e);
        }
    }


    /**
     * Check if the current user has permissions to run a clean on existing
     * database.
     * <P>
     * Mostly this just checks if you need to remove pgcrypto, and if so,
     * whether you have permissions to do so.
     *
     * @param connection database connection
     * @return true if permissions valid, false otherwise
     */
    protected static boolean checkCleanPermissions(Connection connection)
    {
        try
        {
            // get username of our db user
            String username = connection.getMetaData().getUserName();

            // Check their permissions. Are they a 'superuser'?
            String checkSuperuser = "SELECT rolsuper FROM pg_roles WHERE rolname=?;";
            boolean superuser = false;
            try (PreparedStatement statement = connection.prepareStatement(checkSuperuser))
            {
                statement.setString(1,username);
                try(ResultSet results = statement.executeQuery())
                {
                    if(results.next())
                    {
                        superuser = results.getBoolean("rolsuper");
                    }
                }
            }
            catch(SQLException e)
            {
                throw new FlywayException("Unable to determine if user '" + username + "' is a superuser.", e);
            }

            // If user is a superuser, then "clean" can be run successfully
            if(superuser)
            {
                return true;
            }
            else // Otherwise, we'll need to see which schema 'pgcrypto' is installed in
            {
                // Get current schema name
                String schema = getSchemaName(connection);

                // If pgcrypto is installed in this schema, then superuser privileges are needed to remove it
                if(isPgcryptoInSchema(schema))
                    return false;
                else // otherwise, a 'clean' can be run by anyone
                    return true;
            }
        }
        catch(SQLException e)
        {
            throw new FlywayException("Unable to determine if DB user has 'clean' privileges.", e);
        }
    }
}
