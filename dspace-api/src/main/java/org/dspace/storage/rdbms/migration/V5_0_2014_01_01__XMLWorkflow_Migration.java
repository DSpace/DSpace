/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.migration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.commons.lang.StringUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.flywaydb.core.api.migration.MigrationChecksumProvider;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class automatically migrates your DSpace Database to use the
 * XML-based Configurable Workflow system whenever it is enabled.
 * (i.e. workflow.framework=xmlworkflow in workflow.cfg)
 * <P>
 * This class represents a Flyway DB Java Migration
 * http://flywaydb.org/documentation/migration/java.html
 * <P>
 * It can upgrade a 4.0 (or above) version of DSpace to use the XMLWorkflow.
 * 
 * @author Tim Donohue
 */
public class V5_0_2014_01_01__XMLWorkflow_Migration
    implements JdbcMigration, MigrationChecksumProvider
{
    /** logging category */
    private static final Logger log = LoggerFactory.getLogger(V5_0_2014_01_01__XMLWorkflow_Migration.class);
    
    // Size of migration script run
    Integer migration_file_size = -1;
    
    /**
     * Actually migrate the existing database
     * @param connection 
     */
    @Override
    public void migrate(Connection connection)
            throws IOException, SQLException
    {
        // Get the name of the Schema that the DSpace Database is using
        String schema = ConfigurationManager.getProperty("db.schema");
        if(StringUtils.isBlank(schema)){
            schema = null;
        }
    
        // Check if XML Workflow is enabled in workflow.cfg
        if (ConfigurationManager.getProperty("workflow", "workflow.framework").equals("xmlworkflow"))
        {
            //First, ensure both Migration Scripts exist (in src/main/resources)
            String dbMigrationScript = V5_0_2014_01_01__XMLWorkflow_Migration.class.getPackage().getName() +
                    ".xmlworkflow." + DatabaseManager.getDbKeyword() + "xml_workflow_migration.sql";
            String dataMigrationScript = V5_0_2014_01_01__XMLWorkflow_Migration.class.getPackage().getName() +
                    ".xmlworkflow." + DatabaseManager.getDbKeyword() + "data_workflow_migration.sql";
            
            File dbMigration = new File(dbMigrationScript);
            File dataMigration = new File(dataMigrationScript);
            
            if(!dbMigration.exists() || !dataMigration.exists())
            {
                throw new IOException("Cannot locate XMLWorkflow Database Migration scripts in 'src/main/resources'. " +
                        "UNABLE TO ENABLE XML WORKFLOW IN DATABASE.");
            }
            
            // Now, check if the XMLWorkflow table (cwf_workflowitem) already exists in this database
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet tables = meta.getTables(null, schema, "cwf_workflowitem", null);
        
            // If XMLWorkflow Table does NOT exist in this database, then lets do the migration!
            if (!tables.next()) 
            {
                // Run the DB Migration first!
                DatabaseManager.loadSql(new FileReader(dbMigration));
                // Then migrate any existing data (i.e. workflows)
                DatabaseManager.loadSql(new FileReader(dataMigration));
                
                // Assuming both succeeded, save the size of the dbMigration script for getChecksum() below
                migration_file_size = (int) dbMigration.length();
            }
        }//end if XML Workflow enabled
    }
    
    /**
     * Return the checksum to be associated with this Migration
     * in the Flyway database table (schema_version).
     * @return checksum as an Integer
     */
    @Override
    public Integer getChecksum()
    {
        if (ConfigurationManager.getProperty("workflow", "workflow.framework").equals("xmlworkflow"))
        {
            return migration_file_size;
        }
        else
        {
            // This migration script wasn't actually run. Just return -1
            return -1;
        }
    }
}
