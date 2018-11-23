/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.xmlworkflow;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.dspace.core.Constants;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.flywaydb.core.api.migration.MigrationChecksumProvider;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.flywaydb.core.internal.util.scanner.classpath.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class will fix the collection tables in your DSpace Database when you
 *
 * This class automatically migrates your DSpace Database to use the
 * XML-based Configurable Workflow system whenever it is enabled.
 * (i.e. workflow.framework=xmlworkflow in workflow.cfg)
 * <P>
 * Because XML-based Configurable Workflow existed prior to our migration, this
 * class first checks for the existence of the "cwf_workflowitem" table before
 * running any migrations.
 * <P>
 * This class represents a Flyway DB Java Migration
 * http://flywaydb.org/documentation/migration/java.html
 * <P>
 * It can upgrade a 5.0 (or above) version of DSpace to use the XMLWorkflow.
 *
 * @author Yana De Pauw (yana at atmire dot com)
 */
public class V5_7_2018_11_20__Remove_Old_Workflow_Steps_From_Collections
        implements JdbcMigration, MigrationChecksumProvider {
    /**
     * logging category
     */
    private static final Logger log = LoggerFactory.getLogger(
            V5_7_2018_11_20__Remove_Old_Workflow_Steps_From_Collections.class);

    // Size of migration script run
    Integer migration_file_size = -1;


    @Override
    public void migrate(Connection connection)
            throws IOException, SQLException {
        // Make sure XML Workflow is enabled, shouldn't even be needed since this class is only loaded if the service
        // is enabled.
        if (WorkflowServiceFactory.getInstance().getWorkflowService() instanceof XmlWorkflowService
                && DatabaseUtils.getCurrentFlywayDSpaceState(connection) < 6) {

            String dbtype = connection.getMetaData().getDatabaseProductName();
            String dbFileLocation = null;
            if (dbtype.toLowerCase().contains("postgres")) {
                dbFileLocation = "postgres";
            } else if (dbtype.toLowerCase().contains("oracle")) {
                dbFileLocation = "oracle";
            }


            // Determine path of this migration class (as the SQL scripts
            // we will run are based on this path under /src/main/resources)
            String packagePath = V5_7_2018_11_20__Remove_Old_Workflow_Steps_From_Collections.class
                    .getPackage().getName().replace(".", "/");

            // Get the contents of our DB Schema migration script, based on path & DB type
            // (e.g. /src/main/resources/[path-to-this-class]/postgres/xml_workflow_migration.sql)
            String dbMigrateSQL = new ClassPathResource(packagePath + "/" +
                                                                dbFileLocation +
                                                                "/remove_old_collection_steps.sql",
                                                        getClass().getClassLoader())
                    .loadAsString(Constants.DEFAULT_ENCODING);

            // Actually execute the Database schema migration SQL
            // This will create the necessary tables for the XMLWorkflow feature
            DatabaseUtils.executeSql(connection, dbMigrateSQL);

            // Assuming both succeeded, save the size of the scripts for getChecksum() below
            migration_file_size = dbMigrateSQL.length();
        }
    }


    /**
     * Return the checksum to be associated with this Migration
     * in the Flyway database table (schema_version).
     *
     * @return checksum as an Integer
     */
    @Override
    public Integer getChecksum() {
        return migration_file_size;
    }
}
