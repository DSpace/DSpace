/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.migration;

import java.sql.Connection;

import org.dspace.core.Constants;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.flywaydb.core.api.migration.MigrationChecksumProvider;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.flywaydb.core.internal.util.scanner.classpath.ClassPathResource;

/**
 * This class automatically adding rptype to the resource policy created with a migration into XML-based Configurable
 * Workflow system
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class V7_0_2018_04_03__Upgrade_Workflow_Policy implements JdbcMigration, MigrationChecksumProvider {
    // Size of migration script run
    protected Integer migration_file_size = -1;


    @Override
    public void migrate(Connection connection) throws Exception {
        // Make sure XML Workflow is enabled, shouldn't even be needed since this class is only loaded if the service
        // is enabled.
        if (WorkflowServiceFactory.getInstance().getWorkflowService() instanceof XmlWorkflowService) {
            // Now, check if the XMLWorkflow table (cwf_workflowitem) already exists in this database
            if (DatabaseUtils.tableExists(connection, "cwf_workflowitem")) {
                String dbtype = DatabaseUtils.getDbType(connection);

                String sqlMigrationPath = "org/dspace/storage/rdbms/sqlmigration/workflow/" + dbtype + "/";
                String dataMigrateSQL = new ClassPathResource(sqlMigrationPath +
                                                                  "xmlworkflow" +
                                                                  "/V7.0_2018.04.03__upgrade_workflow_policy.sql",
                                                              getClass().getClassLoader())
                    .loadAsString(Constants.DEFAULT_ENCODING);

                // Actually execute the Data migration SQL
                // This will migrate all existing traditional workflows to the new XMLWorkflow system & tables
                DatabaseUtils.executeSql(connection, dataMigrateSQL);

                // Assuming both succeeded, save the size of the scripts for getChecksum() below
                migration_file_size = dataMigrateSQL.length();
            }
        }
    }

    @Override
    public Integer getChecksum() {
        return migration_file_size;
    }
}
