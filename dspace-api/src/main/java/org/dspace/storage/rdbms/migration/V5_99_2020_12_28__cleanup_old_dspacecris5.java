/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.migration;

import org.dspace.storage.rdbms.DatabaseUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * This migration will prefix all the cris related tables from a DSpace-CRIS 5.x
 * version with old_ so that once the data have been transformed to match the new
 * structure and features of version 7 can be removed. Sequences, indexes and
 * constraints that would conflict with the names in use for version 7 are
 * removed as well. The script has a guard to prevent damage if accidently run
 * out-of-order
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class V5_99_2020_12_28__cleanup_old_dspacecris5
    extends BaseJavaMigration {

    // Size of migration script run
    int migration_file_size = -1;

    @Override
    public void migrate(Context context) throws Exception {
        // Based on type of DB, get path to SQL migration script
        String dbtype = DatabaseUtils.getDbType(context.getConnection());

        String dataMigrateSQL;
        String sqlMigrationPath = "org/dspace/storage/rdbms/sqlmigration/cris/" + dbtype + "/";
        // check that the old cris_rpage table is here and there is not the dspaceobject table that would mean v6+
        if (!DatabaseUtils.tableExists(context.getConnection(), "cris_rpage") ||
                DatabaseUtils.tableExists(context.getConnection(), "dspaceobject")) {
            return;
        } else {
            // we come from a previous dspace-cris 5.x version, run the cleanup
            dataMigrateSQL = MigrationUtils.getResourceAsString(
                sqlMigrationPath + "V5.99_2020.12.28__cleanup_old_dspacecris5.sql");
        }

        // Actually execute the Data migration SQL
        // This will prepare all the dspace-cris data for cleanup so that the base
        // dspace can be migrated to dspace-cris 7
        DatabaseUtils.executeSql(context.getConnection(), dataMigrateSQL);
        migration_file_size = dataMigrateSQL.length();
    }

    @Override
    public Integer getChecksum() {
        return migration_file_size;
    }
}
