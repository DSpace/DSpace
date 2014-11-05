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
     * Drop a given Database Constraint (based on the current database type).
     * Returns a "checksum" for this migration which can be used as part of
     * a Flyway Java migration
     *
     * @param connection the current Database connection
     * @param tableName the name of the table the constraint applies to
     * @param columnName the name of the column the constraint applies to
     * @param constraintSuffix Only used for PostgreSQL, whose constraint naming convention depends on a suffix (key, fkey, etc)
     * @return migration checksum as an Integer
     * @throws SQLException if a database error occurs
     */
    public static Integer dropDBConstraint(Connection connection, String tableName, String columnName, String constraintSuffix)
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
        switch(dbtype)
        {
            case DatabaseManager.DBMS_POSTGRES:
                // In Postgres, constraints are always named:
                // {tablename}_{columnname(s)}_{suffix}
                // see: http://stackoverflow.com/a/4108266/3750035
                constraintName = StringUtils.lowerCase(tableName) + "_" + StringUtils.lowerCase(columnName) + "_" + StringUtils.lowerCase(constraintSuffix);
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

        // If we have a SQL query to run for the constraint name, then run it
        if (constraintNameSQL!=null)
        {
            // Run the query to obtain the constraint name, passing it the parameters
            PreparedStatement statement = connection.prepareStatement(constraintNameSQL);
            statement.setString(1, StringUtils.upperCase(tableName));
            statement.setString(2, StringUtils.upperCase(columnName));
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
        }

        // As long as we have a constraint name, drop it
        if (constraintName!=null && !constraintName.isEmpty())
        {
            // This drop constaint SQL should be the same in all databases
            String dropConstraintSQL = "ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName;

            PreparedStatement statement = connection.prepareStatement(dropConstraintSQL);
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
