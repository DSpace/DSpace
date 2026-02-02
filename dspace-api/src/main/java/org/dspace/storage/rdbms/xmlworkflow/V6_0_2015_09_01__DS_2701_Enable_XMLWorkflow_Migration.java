/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.xmlworkflow;


import org.dspace.storage.rdbms.DatabaseUtils;
import org.dspace.storage.rdbms.migration.MigrationUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * This class automatically migrates your DSpace Database to use the
 * XML-based Configurable Workflow system whenever it is enabled.
 * <P>
 * Because XML-based Configurable Workflow existed prior to our migration, this
 * class first checks for the existence of the "cwf_workflowitem" table before
 * running any migrations.
 * <P>
 * This class represents a Flyway DB Java Migration
 * http://flywaydb.org/documentation/migration/java.html
 * <P>
 * It can upgrade a 6.0 version of DSpace to use the XMLWorkflow.
 *
 * User: kevin (kevin at atmire.com)
 * Date: 1/09/15
 * Time: 11:34
 */
public class V6_0_2015_09_01__DS_2701_Enable_XMLWorkflow_Migration extends BaseJavaMigration {
    // Size of migration script run
    protected Integer migration_file_size = -1;


    @Override
    public void migrate(Context context) throws Exception {
        // Check if the XMLWorkflow table (cwf_workflowitem) already exists in this database
        // If XMLWorkflow Table does NOT exist in this database, then lets do the migration!
        // If XMLWorkflow Table ALREADY exists, then this migration is a noop, we assume you manually ran the sql
        // scripts
        if (!DatabaseUtils.tableExists(context.getConnection(), "cwf_workflowitem")) {
            String dbtype = context.getConnection().getMetaData().getDatabaseProductName();
            String dbFileLocation = null;
            if (dbtype.toLowerCase().contains("postgres")) {
                dbFileLocation = "postgres";
            } else if (dbtype.toLowerCase().contains("h2")) {
                dbFileLocation = "h2";
            }


            // Determine path of this migration class (as the SQL scripts
            // we will run are based on this path under /src/main/resources)
            String packagePath = V6_0_2015_09_01__DS_2701_Enable_XMLWorkflow_Migration.class.getPackage().getName()
                    .replace(".", "/");

            // Get the contents of our DB Schema migration script, based on path & DB type
            // (e.g. /src/main/resources/[path-to-this-class]/postgres/xml_workflow_migration.sql)
            String dbMigrateSQL = MigrationUtils.getResourceAsString(packagePath + "/" + dbFileLocation +
                    "/v6.0__DS-2701_xml_workflow_migration.sql");

            // Actually execute the Database schema migration SQL
            // This will create the necessary tables for the XMLWorkflow feature
            DatabaseUtils.executeSql(context.getConnection(), dbMigrateSQL);

            // Get the contents of our data migration script, based on path & DB type
            // (e.g. /src/main/resources/[path-to-this-class]/postgres/data_workflow_migration.sql)
            String dataMigrateSQL = MigrationUtils.getResourceAsString(packagePath + "/" + dbFileLocation +
                    "/v6.0__DS-2701_data_workflow_migration.sql");

            // Actually execute the Data migration SQL
            // This will migrate all existing traditional workflows to the new XMLWorkflow system & tables
            DatabaseUtils.executeSql(context.getConnection(), dataMigrateSQL);

            // Assuming both succeeded, save the size of the scripts for getChecksum() below
            migration_file_size = dbMigrateSQL.length() + dataMigrateSQL.length();
        }
    }

    @Override
    public Integer getChecksum() {
        return migration_file_size;
    }
}
