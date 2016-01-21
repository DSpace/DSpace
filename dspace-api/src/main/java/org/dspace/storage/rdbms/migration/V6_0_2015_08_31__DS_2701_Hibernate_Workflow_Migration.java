/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.migration;

import org.dspace.core.Constants;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.flywaydb.core.api.migration.MigrationChecksumProvider;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.flywaydb.core.internal.util.scanner.classpath.ClassPathResource;

import java.sql.Connection;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 1/09/15
 * Time: 12:08
 */
public class V6_0_2015_08_31__DS_2701_Hibernate_Workflow_Migration implements JdbcMigration, MigrationChecksumProvider
{

    // Size of migration script run
    Integer migration_file_size = -1;


    @Override
    public void migrate(Connection connection) throws Exception
    {
        // Based on type of DB, get path to SQL migration script
        String dbtype = DatabaseUtils.getDbType(connection);

        String dataMigrateSQL;
        String sqlMigrationPath = "org/dspace/storage/rdbms/sqlmigration/workflow/" + dbtype +"/";
        // Now, check if the XMLWorkflow table (cwf_workflowitem) already exists in this database
        // If XMLWorkflow Table does NOT exist in this database, then lets do the migration!
        // If XMLWorkflow Table ALREADY exists, then this migration is a noop, we assume you manually ran the sql scripts
        if (DatabaseUtils.tableExists(connection, "cwf_workflowitem"))
        {
            // Get the contents of our data migration script, based on path & DB type
            dataMigrateSQL = new ClassPathResource(sqlMigrationPath + "xmlworkflow"+
                                                      "/V6.0_2015.08.11__DS-2701_Xml_Workflow_Migration.sql", getClass().getClassLoader()).loadAsString(Constants.DEFAULT_ENCODING);
        }else{
            //Migrate the basic workflow
                        // Get the contents of our data migration script, based on path & DB type
            dataMigrateSQL = new ClassPathResource(sqlMigrationPath + "basicWorkflow"+
                                                      "/V6.0_2015.08.11__DS-2701_Basic_Workflow_Migration.sql", getClass().getClassLoader()).loadAsString(Constants.DEFAULT_ENCODING);
        }

        // Actually execute the Data migration SQL
        // This will migrate all existing traditional workflows to the new XMLWorkflow system & tables
        DatabaseUtils.executeSql(connection, dataMigrateSQL);
        migration_file_size = dataMigrateSQL.length();

    }

    @Override
    public Integer getChecksum() {
        return migration_file_size;
    }
}
