/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.migration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Constants;
import org.springframework.util.FileCopyUtils;

/**
 * This Utility class offers utility methods which may be of use to perform
 * common Java database migration task(s) (via Flyway).
 * <P>
 * NOTE: This class specifically CANNOT utilize Hibernate, because it is
 * used for database migrations which take place PRIOR to Hibernate loading.
 * However, as you'll see below, all methods are protected to ensure the rest
 * of the API cannot bypass Hibernate.
 *
 * @author Tim Donohue
 */
public class MigrationUtils {

    /**
     * Default constructor
     */
    private MigrationUtils() { }

    /**
     * Drop a given Database Constraint (based on the current database type).
     * Returns a "checksum" for this migration which can be used as part of
     * a Flyway Java migration
     *
     * @param connection       the current Database connection
     * @param tableName        the name of the table the constraint applies to
     * @param columnName       the name of the column the constraint applies to
     * @param constraintSuffix Only used for PostgreSQL, whose constraint naming convention depends on a suffix (key,
     *                         fkey, etc)
     * @return migration checksum as an Integer
     * @throws SQLException if a database error occurs
     */
    protected static Integer dropDBConstraint(Connection connection, String tableName, String columnName,
                                              String constraintSuffix)
        throws SQLException {
        Integer checksum = -1;

        // First, in order to drop the appropriate Database constraint, we
        // must determine the unique name of the constraint. As constraint
        // naming is DB specific, this is dependent on our DB Type
        String dbtype = connection.getMetaData().getDatabaseProductName();
        String constraintName = null;
        String constraintNameSQL = null;
        boolean cascade = false;
        switch (dbtype.toLowerCase()) {
            case "postgres":
            case "postgresql":
                // In Postgres, constraints are always named:
                // {tablename}_{columnname(s)}_{suffix}
                // see: http://stackoverflow.com/a/4108266/3750035
                constraintName = StringUtils.lowerCase(tableName);
                if (!StringUtils.equals(constraintSuffix, "pkey")) {
                    constraintName += "_" + StringUtils.lowerCase(columnName);
                }

                constraintName += "_" + StringUtils.lowerCase(constraintSuffix);
                cascade = true;
                break;
            case "oracle":
                // In Oracle, constraints are listed in the USER_CONS_COLUMNS table
                constraintNameSQL = "SELECT CONSTRAINT_NAME " +
                    "FROM USER_CONS_COLUMNS " +
                    "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
                cascade = true;
                break;
            case "h2":
                // In H2, column constraints are listed in the "INFORMATION_SCHEMA.KEY_COLUMN_USAGE" table
                constraintNameSQL = "SELECT DISTINCT CONSTRAINT_NAME " +
                    "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                    "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
                cascade = true;
                break;
            default:
                throw new SQLException("DBMS " + dbtype + " is unsupported in this migration.");
        }

        // If we have a SQL query to run for the constraint name, then run it
        if (constraintNameSQL != null) {
            // Run the query to obtain the constraint name, passing it the parameters
            PreparedStatement statement = connection.prepareStatement(constraintNameSQL);
            statement.setString(1, StringUtils.upperCase(tableName));
            statement.setString(2, StringUtils.upperCase(columnName));
            try {
                ResultSet results = statement.executeQuery();
                if (results.next()) {
                    constraintName = results.getString("CONSTRAINT_NAME");
                }
                results.close();
            } finally {
                statement.close();
            }
        }

        // As long as we have a constraint name, drop it
        if (constraintName != null && !constraintName.isEmpty()) {
            // This drop constaint SQL should be the same in all databases
            String dropConstraintSQL = "ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName;
            if (cascade) {
                dropConstraintSQL += " CASCADE";
            }

            try (PreparedStatement statement = connection.prepareStatement(dropConstraintSQL)) {
                statement.execute();
            }
            // Return the size of the query we just ran
            // This will be our "checksum" for this Flyway migration (see getChecksum())
            checksum = dropConstraintSQL.length();
        }

        return checksum;
    }


    /**
     * Drop a given Database Table (based on the current database type).
     * Returns a "checksum" for this migration which can be used as part of
     * a Flyway Java migration
     * <P>
     * NOTE: Ideally, if you need to do a DROP TABLE, you should just create
     * a Flyway SQL migration. This method should ONLY be used if the table name
     * needs to be dynamically determined via Java.
     *
     * @param connection the current Database connection
     * @param tableName  the name of the table to drop
     * @return migration checksum as an Integer
     * @throws SQLException if a database error occurs
     */
    protected static Integer dropDBTable(Connection connection, String tableName)
        throws SQLException {
        String dropTableSQL = null;
        Integer checksum = -1;

        // First, in order to drop the appropriate Database table, we must
        // determine the query based on DB type
        String dbtype = connection.getMetaData().getDatabaseProductName();
        switch (dbtype.toLowerCase()) {
            case "postgres":
            case "postgresql":
                dropTableSQL = "DROP TABLE IF EXISTS " + tableName + " CASCADE";
                break;
            case "oracle":
                dropTableSQL = "DROP TABLE " + tableName + " CASCADE CONSTRAINTS";
                break;
            case "h2":
                dropTableSQL = "DROP TABLE IF EXISTS " + tableName + " CASCADE";
                break;
            default:
                throw new SQLException("DBMS " + dbtype + " is unsupported in this migration.");
        }

        // If we have a SQL query to run, then run it
        if (dropTableSQL != null) {
            try (PreparedStatement statement = connection.prepareStatement(dropTableSQL)) {
                statement.execute();
            }
            // Return the size of the query we just ran
            // This will be our "checksum" for this Flyway migration (see getChecksum())
            checksum = dropTableSQL.length();
        }

        return checksum;
    }

    /**
     * Drop a given Database Sequence (based on the current database type).
     * Returns a "checksum" for this migration which can be used as part of
     * a Flyway Java migration
     * <P>
     * NOTE: Ideally, if you need to do a DROP SEQUENCE, you should just create
     * a Flyway SQL migration. This method should ONLY be used if the sequence name
     * needs to be dynamically determined via Java.
     *
     * @param connection   the current Database connection
     * @param sequenceName the name of the sequence to drop
     * @return migration checksum as an Integer
     * @throws SQLException if a database error occurs
     */
    protected static Integer dropDBSequence(Connection connection, String sequenceName)
        throws SQLException {
        String dropSequenceSQL = null;
        Integer checksum = -1;

        String dbtype = connection.getMetaData().getDatabaseProductName();
        switch (dbtype.toLowerCase()) {
            case "postgres":
            case "postgresql":
                dropSequenceSQL = "DROP SEQUENCE IF EXISTS " + sequenceName;
                break;
            case "oracle":
                dropSequenceSQL = "DROP SEQUENCE " + sequenceName;
                break;
            case "h2":
                dropSequenceSQL = "DROP SEQUENCE IF EXISTS " + sequenceName;
                break;
            default:
                throw new SQLException("DBMS " + dbtype + " is unsupported in this migration.");
        }

        // If we have a SQL query to run, then run it
        if (dropSequenceSQL != null) {
            try (PreparedStatement statement = connection.prepareStatement(dropSequenceSQL)) {
                statement.execute();
            }
            // Return the size of the query we just ran
            // This will be our "checksum" for this Flyway migration (see getChecksum())
            checksum = dropSequenceSQL.length();
        }

        return checksum;
    }

    /**
     * Drop a given Database View (based on the current database type).
     * Returns a "checksum" for this migration which can be used as part of
     * a Flyway Java migration
     * <P>
     * NOTE: Ideally, if you need to do a DROP VIEW, you should just create
     * a Flyway SQL migration. This method should ONLY be used if the view name
     * needs to be dynamically determined via Java.
     *
     * @param connection the current Database connection
     * @param viewName   the name of the view to drop
     * @return migration checksum as an Integer
     * @throws SQLException if a database error occurs
     */
    protected static Integer dropDBView(Connection connection, String viewName)
        throws SQLException {
        String dropViewSQL = null;
        Integer checksum = -1;

        String dbtype = connection.getMetaData().getDatabaseProductName();
        switch (dbtype.toLowerCase()) {
            case "postgres":
            case "postgresql":
                dropViewSQL = "DROP VIEW IF EXISTS " + viewName + " CASCADE";
                break;
            case "oracle":
                dropViewSQL = "DROP VIEW " + viewName + " CASCADE CONSTRAINTS";
                break;
            case "h2":
                dropViewSQL = "DROP VIEW IF EXISTS " + viewName + " CASCADE";
                break;
            default:
                throw new SQLException("DBMS " + dbtype + " is unsupported in this migration.");
        }

        // If we have a SQL query to run, then run it
        if (dropViewSQL != null) {
            try (PreparedStatement statement = connection.prepareStatement(dropViewSQL)) {
                statement.execute();
            }
            // Return the size of the query we just ran
            // This will be our "checksum" for this Flyway migration (see getChecksum())
            checksum = dropViewSQL.length();
        }

        return checksum;
    }

    /**
     * Read a given Resource, converting to a String. This is used by several Java-based
     * migrations to read a SQL migration into a string, so that it can be executed under
     * specific scenarios.
     * @param resourcePath relative path of resource to read
     * @return String contents of Resource
     */
    public static String getResourceAsString(String resourcePath) {
        // Read the resource, copying to a string
        try (Reader reader =
                 new InputStreamReader(
                     Objects.requireNonNull(MigrationUtils.class.getClassLoader().getResourceAsStream(resourcePath)),
                     Constants.DEFAULT_ENCODING)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NullPointerException e) {
            throw new IllegalStateException("Resource at " + resourcePath + " was not found", e);
        }
    }
}
