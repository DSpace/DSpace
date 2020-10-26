/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Migration class that will drop the public key for the dspace objects, the integer based key will be moved to a UUID
 *
 * @author kevinvandevelde at atmire.com
 */
public class V6_0_2015_03_06__DS_2701_Dso_Uuid_Migration extends BaseJavaMigration {

    private int checksum = -1;


    @Override
    public void migrate(Context context) throws Exception {
        checksum += MigrationUtils.dropDBConstraint(context.getConnection(), "eperson", "eperson_id", "pkey");
        checksum += MigrationUtils.dropDBConstraint(context.getConnection(), "epersongroup",
                                                    "eperson_group_id", "pkey");
        checksum += MigrationUtils.dropDBConstraint(context.getConnection(), "community", "community_id", "pkey");
        checksum += MigrationUtils.dropDBConstraint(context.getConnection(), "collection", "collection_id", "pkey");
        checksum += MigrationUtils.dropDBConstraint(context.getConnection(), "item", "item_id", "pkey");
        checksum += MigrationUtils.dropDBConstraint(context.getConnection(), "bundle", "bundle_id", "pkey");
        checksum += MigrationUtils.dropDBConstraint(context.getConnection(), "bitstream", "bitstream_id", "pkey");
    }

    @Override
    public Integer getChecksum() {
        return checksum;
    }
}
