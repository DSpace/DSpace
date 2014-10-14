/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.migration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.dspace.storage.rdbms.DatabaseManager;
import org.flywaydb.core.api.migration.MigrationChecksumProvider;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is in support of the DS-1582 Metadata for All Objects feature.
 * It simply drops the database constraint associated with the "item_id" column
 * of the "metadatavalue" table. This is necessary because to support DS-1582
 * this column must be renamed to "resource_id".
 * <P>
 * This class was created because the names of database constraints differs based
 * on the type of database (Postgres vs. Oracle vs. H2). As such, it becomes difficult
 * to write simple SQL which will work for multiple database types (especially
 * since unit tests require H2 and the syntax for H2 is different from either
 * Oracle or Postgres).
 * <P>
 * NOTE: This migration class is very simple because it is meant to be used
 * in conjuction with the corresponding SQL script:
 * ./etc/migrations/[db-type]/V5.0_2014_09_26__DS-1582_Metadata_For_All_Objects.sql
 * <P>
 * Also note that this migration is dated as 2014_09_25 so that it will run
 * just PRIOR to the corresponding SQL script listed above.
 * <P>
 * This class represents a Flyway DB Java Migration
 * http://flywaydb.org/documentation/migration/java.html
 * 
 * @author Tim Donohue
 */
public class V5_0_2014_09_25__DS_1582_Metadata_For_All_Objects_drop_constraint
    implements JdbcMigration, MigrationChecksumProvider
{
    /** logging category */
    private static final Logger log = LoggerFactory.getLogger(V5_0_2014_09_25__DS_1582_Metadata_For_All_Objects_drop_constraint.class);
    
    /* Size of the DROP Constraint SQL query. Used as the "checksum" of this change by getChecksum() */
    private int drop_constraint_sql_size = -1;
    
    /**
     * Actually migrate the existing database
     * @param connection 
     */
    @Override
    public void migrate(Connection connection)
            throws IOException, SQLException
    {
        // First, in order to drop the appropriate Database constraint, we
        // must determine the unique name of the constraint. As constraint
        // naming is DB specific, this is dependent on our DB Type
        String dbtype = DatabaseManager.getDbKeyword();
        String constraintName = null;
        String constraintNameSQL = null;
        switch(dbtype)
        {
            case DatabaseManager.DBMS_POSTGRES:
                // In Postgres, constraints are always named:
                // {tablename}_{columnname(s)}_{suffix}
                // see: http://stackoverflow.com/a/4108266/3750035
                constraintName = "metadatavalue_item_id_fkey";
                break;
            case DatabaseManager.DBMS_ORACLE:
                // In Oracle, constraints are listed in the USER_CONS_COLUMNS table
                constraintNameSQL = "SELECT CONSTRAINT_NAME " +
                                    "FROM USER_CONS_COLUMNS " +
                                    "WHERE table_name='METADATAVALUE' AND COLUMN_NAME='ITEM_ID'";
                break;
            case DatabaseManager.DBMS_H2:
                // In H2, constraints are listed in the "information_schema.constraints" table
                constraintNameSQL = "SELECT DISTINCT CONSTRAINT_NAME " +
                                    "FROM information_schema.constraints " +
                                    "WHERE table_name='METADATAVALUE' AND column_list='ITEM_ID'";
                break;
            default:
                throw new SQLException("DBMS " + dbtype + " is unsupported in this migration.");
        }

        // If we have a SQL query to run for the constraint name, then run it
        if (constraintNameSQL!=null)
        {
            // Run the query to obtain the constraint name
            PreparedStatement statement = connection.prepareStatement(constraintNameSQL);
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
            String dropConstraintSQL = "ALTER TABLE METADATAVALUE DROP CONSTRAINT " + constraintName;

            PreparedStatement statement = connection.prepareStatement(dropConstraintSQL);
            try
            {
                statement.execute();
            }
            finally
            {
                statement.close();
            }
            // Record the size of the query we just ran
            // This will be our "checksum" for this Flyway migration (see getChecksum())
            drop_constraint_sql_size = dropConstraintSQL.length();
        }
    }
    
    /**
     * Return the checksum to be associated with this Migration
     * in the Flyway database table (schema_version).
     * @return checksum as an Integer
     */
    @Override
    public Integer getChecksum()
    {
        return drop_constraint_sql_size;
    }
}
