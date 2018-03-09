/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.migration;

import org.flywaydb.core.api.migration.MigrationChecksumProvider;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;

/**
 * Migration class that will drop the public key for the dspace objects, the integer based key will be moved to a UUID
 *
 * @author kevinvandevelde at atmire.com
 */
public class V6_0_2015_03_06__DS_2701_Dso_Uuid_Migration implements JdbcMigration, MigrationChecksumProvider {

    private int checksum = -1;


    @Override
    public void migrate(Connection connection) throws Exception {
        checksum += MigrationUtils.dropDBConstraint(connection, "eperson", "eperson_id", "pkey");
        checksum += MigrationUtils.dropDBConstraint(connection, "epersongroup", "eperson_group_id", "pkey");
        checksum += MigrationUtils.dropDBConstraint(connection, "community", "community_id", "pkey");
        checksum += MigrationUtils.dropDBConstraint(connection, "collection", "collection_id", "pkey");
        checksum += MigrationUtils.dropDBConstraint(connection, "item", "item_id", "pkey");
        checksum += MigrationUtils.dropDBConstraint(connection, "bundle", "bundle_id", "pkey");
        checksum += MigrationUtils.dropDBConstraint(connection, "bitstream", "bitstream_id", "pkey");
    }

    @Override
    public Integer getChecksum() {
        return checksum;
    }
}
