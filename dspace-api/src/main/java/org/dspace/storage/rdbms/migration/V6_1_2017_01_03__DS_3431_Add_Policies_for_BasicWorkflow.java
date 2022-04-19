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

public class V6_1_2017_01_03__DS_3431_Add_Policies_for_BasicWorkflow
    extends BaseJavaMigration {

    // Size of migration script run
    Integer migration_file_size = -1;


    @Override
    public void migrate(Context context) throws Exception {
        // Based on type of DB, get path to SQL migration script
        String dbtype = DatabaseUtils.getDbType(context.getConnection());

        String dataMigrateSQL;
        String sqlMigrationPath = "org/dspace/storage/rdbms/sqlmigration/workflow/" + dbtype + "/";
        // Now, check if the XMLWorkflow table (cwf_workflowitem) already exists in this database
        // If XMLWorkflow Table does NOT exist in this database, then lets do the migration!
        // If XMLWorkflow Table ALREADY exists, then this migration is a noop, we assume you manually ran the sql
        // scripts
        if (DatabaseUtils.tableExists(context.getConnection(), "cwf_workflowitem")) {
            return;
        } else {
            //Migrate the basic workflow
            // Get the contents of our data migration script, based on path & DB type
            dataMigrateSQL = MigrationUtils.getResourceAsString(sqlMigrationPath + "basicWorkflow" +
                                                                    "/V6.1_2017.01.03__DS-3431.sql");
        }

        // Actually execute the Data migration SQL
        // This will migrate all existing traditional workflows to the new XMLWorkflow system & tables
        DatabaseUtils.executeSql(context.getConnection(), dataMigrateSQL);
        migration_file_size = dataMigrateSQL.length();

    }

    @Override
    public Integer getChecksum() {
        return migration_file_size;
    }
}
