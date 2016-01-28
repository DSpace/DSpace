/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.commons.lang.StringUtils;

/**
 * This Utility class offers utility methods which may be of use to perform
 * common Java migration task(s).
 *
 * @author Tim Donohue
 */
public class MigrationUtils
{
    /**
     * Drop a given Database Column Constraint (based on the current database type).
     * Returns a "checksum" for this migration which can be used as part of
     * a Flyway Java migration
     *
     * @param connection the current Database connection
     * @param tableName the name of the table the constraint applies to
     * @param columnName the name of the column the constraint applies to
     * @return migration checksum as an Integer
     * @throws SQLException if a database error occurs
     */
    public static Integer dropDBConstraint(Connection connection, String tableName, String columnName)
            throws SQLException
    {
        Integer checksum = -1;

        // First, in order to drop the appropriate Database constraint, we
        // must determine the unique name of the constraint. As constraint
        // naming is DB specific, this is dependent on our DB Type
        DatabaseMetaData meta = connection.getMetaData();
        // NOTE: We use "findDbKeyword()" here as it won't cause
        // DatabaseManager.initialize() to be called (which in turn re-calls Flyway)
        String dbtype = DatabaseManager.findDbKeyword(meta);
        String constraintName = null;
        String constraintNameSQL = null;
        String schemaName = null;
        switch(dbtype)
        {
            case DatabaseManager.DBMS_POSTGRES:
                // In Postgres, column constraints are listed in the "information_schema.key_column_usage" view
                // See: http://www.postgresql.org/docs/9.4/static/infoschema-key-column-usage.html
                constraintNameSQL = "SELECT DISTINCT CONSTRAINT_NAME " +
                                    "FROM information_schema.key_column_usage " +
                                    "WHERE TABLE_NAME = ? AND COLUMN_NAME = ? AND TABLE_SCHEMA = ?";
                // For Postgres, we need to limit by the schema as well
                schemaName = DatabaseUtils.getSchemaName(connection);
                break;
            case DatabaseManager.DBMS_ORACLE:
                // In Oracle, constraints are listed in the USER_CONS_COLUMNS table
                constraintNameSQL = "SELECT CONSTRAINT_NAME " +
                                    "FROM USER_CONS_COLUMNS " +
                                    "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
                break;
            case DatabaseManager.DBMS_H2:
                // In H2, constraints are listed in the "information_schema.constraints" table
                constraintNameSQL = "SELECT DISTINCT CONSTRAINT_NAME " +
                                    "FROM information_schema.constraints " +
                                    "WHERE table_name = ? AND column_list = ?";
                break;
            default:
                throw new SQLException("DBMS " + dbtype + " is unsupported in this migration.");
        }

        // Run the query to obtain the constraint name, passing it the parameters
        PreparedStatement statement = connection.prepareStatement(constraintNameSQL);
        statement.setString(1, DatabaseUtils.canonicalize(connection, tableName));
        statement.setString(2, DatabaseUtils.canonicalize(connection, columnName));
        // Also limit by database schema, if a schemaName has been set (only needed for PostgreSQL)
        if(schemaName!=null && !schemaName.isEmpty())
        {
            statement.setString(3, DatabaseUtils.canonicalize(connection, schemaName));
        }
        try
        {
            ResultSet results = statement.executeQuery();
            if(results.next())
            {
                constraintName = results.getString("CONSTRAINT_NAME");
            }
            results.close();
        }
        finally
        {
            statement.close();
        }

        // As long as we have a constraint name, drop it
        if (constraintName!=null && !constraintName.isEmpty())
        {
            // Canonicalize the constraintName
            constraintName = DatabaseUtils.canonicalize(connection, constraintName);
            // If constraintName starts with a $, surround with double quotes
            // (This is mostly for PostgreSQL, which sometimes names constraints $1, $2, etc)
            if(constraintName.startsWith("$"))
            {
                constraintName = "\"" + constraintName + "\"";
            }

            // This drop constaint SQL should be the same in all databases
            String dropConstraintSQL = "ALTER TABLE " + DatabaseUtils.canonicalize(connection, tableName) +
                                       " DROP CONSTRAINT " + constraintName;

            statement = connection.prepareStatement(dropConstraintSQL);
            try
            {
                statement.execute();
            }
            finally
            {
                statement.close();
            }
            // Return the size of the query we just ran
            // This will be our "checksum" for this Flyway migration (see getChecksum())
            checksum = dropConstraintSQL.length();
        }

        return checksum;
    }
}
