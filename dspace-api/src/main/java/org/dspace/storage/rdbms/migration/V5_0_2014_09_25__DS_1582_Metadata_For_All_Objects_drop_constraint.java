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
        // Drop the constraint associated with "item_id" column of "metadatavalue"
        checksum = MigrationUtils.dropDBConstraint(connection, "metadatavalue", "item_id", "fkey");
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
