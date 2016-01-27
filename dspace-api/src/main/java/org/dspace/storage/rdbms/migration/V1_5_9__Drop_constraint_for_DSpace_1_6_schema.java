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
import java.sql.SQLException;
import org.flywaydb.core.api.migration.MigrationChecksumProvider;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

/**
 * This class is in support of the "V1.6__Upgrade_to_DSpace_1.6_schema.sql"
 * It simply drops a few database constraints. This is necessary for the upgrade
 * from 1.5 -> 1.6
 * <P>
 * This class was created because the names of database constraints differs based
 * on the type of database (Postgres vs. Oracle vs. H2). As such, it becomes difficult
 * to write simple SQL which will work for multiple database types (especially
 * since unit tests require H2 and the syntax for H2 is different from either
 * Oracle or Postgres).
 * <P>
 * NOTE: This migration class is very simple because it is meant to be used
 * in conjuction with the corresponding SQL script:
 * ./etc/migrations/[db-type]/V1.6__Upgrade_to_DSpace_1.6_schema.sql
 * <P>
 * Also note that this migration is "hackingly" versioned "1.5.9" as it needs to
 * run just PRIOR to the 1.6 migration script.
 * <P>
 * This class represents a Flyway DB Java Migration
 * http://flywaydb.org/documentation/migration/java.html
 *
 * @author Tim Donohue
 */
public class V1_5_9__Drop_constraint_for_DSpace_1_6_schema
    implements JdbcMigration, MigrationChecksumProvider
{
    /* The checksum to report for this migration (when successful) */
    private int checksum = -1;

    /**
     * Actually migrate the existing database
     * @param connection
     */
    @Override
    public void migrate(Connection connection)
            throws IOException, SQLException
    {
        // Drop the constraint associated with "collection_id" column of "community2collection" table
        int return1 = MigrationUtils.dropDBConstraint(connection, "community2collection", "collection_id", "pkey");
        // Drop the constraint associated with "child_comm_id" column of "community2community" table
        int return2 = MigrationUtils.dropDBConstraint(connection, "community2community", "child_comm_id", "pkey");
        // Drop the constraint associated with "item_id" column of "collection2item" table
        int return3 = MigrationUtils.dropDBConstraint(connection, "collection2item", "item_id", "pkey");

	// Checksum will just be the sum of those three return values
	checksum = return1 + return2 + return3;
    }

    /**
     * Return the checksum to be associated with this Migration
     * in the Flyway database table (schema_version).
     * @return checksum as an Integer
     */
    @Override
    public Integer getChecksum()
    {
        return checksum;
    }
}
