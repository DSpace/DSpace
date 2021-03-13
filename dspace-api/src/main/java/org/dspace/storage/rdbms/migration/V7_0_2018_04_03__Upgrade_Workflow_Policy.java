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
 * This class automatically adding rptype to the resource policy created with a migration into XML-based Configurable
 * Workflow system
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class V7_0_2018_04_03__Upgrade_Workflow_Policy extends BaseJavaMigration {
    // Size of migration script run
    protected Integer migration_file_size = -1;


    @Override
    public void migrate(Context context) throws Exception {
        // Check if the XMLWorkflow table (cwf_workflowitem) already exists in this database
        if (DatabaseUtils.tableExists(context.getConnection(), "cwf_workflowitem")) {
            String dbtype = DatabaseUtils.getDbType(context.getConnection());

            String sqlMigrationPath = "org/dspace/storage/rdbms/sqlmigration/workflow/" + dbtype + "/";
            String dataMigrateSQL = MigrationUtils.getResourceAsString(
                    sqlMigrationPath + "xmlworkflow/V7.0_2018.04.03__upgrade_workflow_policy.sql");

            // Actually execute the Data migration SQL
            // This will migrate all existing traditional workflows to the new XMLWorkflow system & tables
            DatabaseUtils.executeSql(context.getConnection(), dataMigrateSQL);

            // Assuming both succeeded, save the size of the scripts for getChecksum() below
            migration_file_size = dataMigrateSQL.length();
        }
    }

    @Override
    public Integer getChecksum() {
        return migration_file_size;
    }
}
