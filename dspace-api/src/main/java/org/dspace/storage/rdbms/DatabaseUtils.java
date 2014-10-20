/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.internal.info.MigrationInfoDumper;

/**
 * Utility class used to manage the Database. This class is used by the
 * DatabaseManager to initialize/upgrade/migrate the Database. It can also
 * be called via the commandline as necessary to get information about
 * the database.
 * <p>
 * Currently, we use Flyway DB (http://flywaydb.org/) for database management.
 * 
 * @see org.dspace.storage.rdbms.DatabaseManager
 * @author Tim Donohue
 */
public class DatabaseUtils 
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(DatabaseUtils.class);
    
    // Our Flyway DB object (initialized by setupFlyway())
    private static Flyway flywaydb;
    
    /** Database Status Flags for Flyway setup. Fresh Install vs. pre-4.0 vs */
    private static final int STATUS_PRE_4_0 = -1;
    private static final int STATUS_FRESH_INSTALL = 0;
    private static final int STATUS_NO_FLYWAY = 1;
    private static final int STATUS_FLYWAY = 2;
    
    /**
     * Commandline tools for managing database changes, etc.
     * @param argv 
     */
    public static void main(String[] argv)
    {  
        // Usage checks
        if (argv.length != 1)
        {
            System.out.println("\nDatabase action argument is missing.");
            System.out.println("Valid actions include: 'info', 'migrate', 'repair' or 'clean'");
            System.exit(1);
        }

        try
        {
            // This is just to ensure DatabaseManager.initialize() gets called!
            String dbName = DatabaseManager.getDbName();
            System.out.println("Initialized connection to " + dbName + " database");

            // Setup Flyway against our database
            Flyway flyway = setupFlyway(DatabaseManager.getDataSource());
            
            if(argv[0].equalsIgnoreCase("info"))
            {
                // Get basic Database info
                Connection connection = DatabaseManager.getConnection();
                DatabaseMetaData meta = connection.getMetaData();
                System.out.println("\nDatabase: " + meta.getDatabaseProductName() + " version " + meta.getDatabaseProductVersion());
                System.out.println("Database Driver: " + meta.getDriverName() + " version " + meta.getDriverVersion());
                
                // Get info table from Flyway
                System.out.println("\n" + MigrationInfoDumper.dumpToAsciiTable(flyway.info().all()));
            }
            else if(argv[0].equalsIgnoreCase("migrate"))
            {
                System.out.println("Migrating database to latest version... (Check logs for details)");
                flyway.migrate();
            }
            else if(argv[0].equalsIgnoreCase("repair"))
            {
                System.out.println("Attempting to repair database via FlywayDB... (Check logs for details)");
                flyway.repair();
            }
            else if(argv[0].equalsIgnoreCase("clean"))
            {
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("If you continue, ALL DATA IN YOUR DATABASE WILL BE DELETED. \n");
                System.out.println("There is no turning back from this action. You should backup your database before continuing. \n");
                System.out.println("Are you ready to destroy your entire database? [y/n]: ");
                String choiceString = input.readLine();
                input.close();
                
                if (choiceString.equalsIgnoreCase("y"))
                {
                    System.out.println("Scrubbing database clean... (Check logs for details)");
                    flyway.clean();
                }
            }
            else
            {
                System.out.println("\nDatabase action " + argv[0] + " is not valid.");
                System.out.println("Valid actions include: 'info', 'migrate', 'repair' or 'clean'");
            }

            System.exit(0);
        }
        catch (Exception e)
        {
            System.err.println("Caught exception:");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    
    
    /**
     * Setup/Initialize the Flyway API to run against our DSpace database
     * and point at our migration scripts.
     * 
     * @param datasource
     *      DataSource object initialized by DatabaseManager
     * @return initialized Flyway object
     */
    private static Flyway setupFlyway(DataSource datasource)
    {
        if (flywaydb==null)
        {
            // Initialize Flyway DB API (http://flywaydb.org/), used to perform DB migrations
            flywaydb = new Flyway();
            flywaydb.setDataSource(datasource);
            flywaydb.setEncoding("UTF-8");
    
            // Migration scripts are based on DBMS Keyword (see full path below)
            String scriptFolder = DatabaseManager.getDbKeyword();

            // Set location where Flyway will load DB scripts from (based on DB Type)
            // e.g. [dspace.dir]/etc/[dbtype]/
            String scriptPath = ConfigurationManager.getProperty("dspace.dir") +
                                System.getProperty("file.separator") + "etc" +
                                System.getProperty("file.separator") + "migrations" +
                                System.getProperty("file.separator") + scriptFolder;

            // Flyway will look in "scriptPath" for SQL migrations AND
            // in 'org.dspace.storage.rdbms.migration.*' for Java migrations
            log.info("Loading Flyway DB migrations from " + scriptPath + " and Package 'org.dspace.storage.rdbms.migration.*'");
            flywaydb.setLocations("filesystem:" + scriptPath, "classpath:org.dspace.storage.rdbms.migration");
            
            // Set flyway callbacks (i.e. classes which are called post-DB migration and similar)
            // In this situation, we have a Registry Updater that runs PRE-migration
            // and a Reindexer that runs POST-migration
            flywaydb.setCallbacks(new DatabaseRegistryUpdater(), new DatabaseReindexer());
        }
        
        return flywaydb;
    }
    
    /**
     * Ensures the current database is up-to-date with regards
     * to the latest DSpace DB schema. If the scheme is not up-to-date,
     * then any necessary database migrations are performed.
     * <P>
     * FlywayDB (http://flywaydb.org/) is used to perform database migrations.
     * If a Flyway DB migration fails it will be rolled back to the last
     * successful migration, and any errors will be logged.
     * 
     * @param datasource
     *      DataSource object (retrieved from DatabaseManager())
     * @param connection
     *      Database connection
     * @throws SQLException
     *      If database cannot be upgraded.
     */
    protected static synchronized void updateDatabase(DataSource datasource, Connection connection) 
            throws SQLException
    {
        // Setup Flyway against our database
        Flyway flyway = setupFlyway(datasource);
        
        // Get our Database migration status, so we know what to tell Flyway to do
        int status = getDbMigrationStatus(connection, flyway);
        
        // If we have a pre-4.0 Database, we need to exit immediately. There's nothing we can do here
        if(status==STATUS_PRE_4_0)
            throw new SQLException("CANNOT AUTOUPGRADE DSPACE DATABASE, AS IT DOES NOT LOOK TO BE A VALID DSPACE 4.0 DATABASE. " +
                        "Please manually upgrade your database to DSpace 4.0 compatibility.");
        // If this is a fresh install
        else if (status==STATUS_FRESH_INSTALL)
        {
            // Initialize the Flyway database table
            flyway.init();
        }
        // If we have a valid 4.0 database, but haven't initialized Flyway on it
        else if (status == STATUS_NO_FLYWAY)
        {
            // Initialize the Flyway database table.
            // We are hardcoding the schema version to 4.0 because this should ONLY
            // be encountered on a 4.0 database. After 4.0, all databases should 
            // already have Flyway initialized.
            // (NOTE: Flyway will also create the db.schema, if it doesn't exist)
            flyway.setInitVersion("4.0");
            flyway.setInitDescription("Initial DSpace 4.0 database schema");
            flyway.init();
        }
           
        // Determine pending Database migrations
        MigrationInfo[] pending = flyway.info().pending();
        
        // Log info about pending migrations
        if (pending!=null && pending.length>0) 
        {   
            log.info("Pending DSpace database schema migrations:");
            for (MigrationInfo info : pending)
            {
                log.info("\t" + info.getVersion() + " " + info.getDescription() + " " + info.getType() + " " + info.getState());
            }
        }
        else
            log.info("DSpace database schema is up to date.");

        // Run all pending Flyway migrations to ensure the DSpace Database is up to date
        flyway.migrate();
    }
    
    /**
     * Determine the migration status of our Database
     * so that we are able to properly migrate it to the latest schema
     * via Flyway
     * 
     * @param connection
     *          Current Database Connection
     * @param flyway
     *          Our Flyway settings
     * @throws SQLException if DB status cannot be determined
     * @return status flag 
     */
    private static int getDbMigrationStatus(Connection connection, Flyway flyway)
            throws SQLException
    {
        // Get information about our database. We'll use this to determine DB status.
        DatabaseMetaData meta = connection.getMetaData();
       
        // First, is this a "fresh_install"?  Check for an "item" table.
        if(!tableExists(connection, "item"))
        {
            // No "item" table, this is a fresh install of DSpace
            return STATUS_FRESH_INSTALL;
        }
  
        // Second, is this DSpace DB Schema compatible with 4.0?  Check for a "Webapp" table (which was added in 4.0)
        // TODO: If the "Webapp" table is ever removed, then WE NEED TO CHANGE THIS CHECK.
        if(!tableExists(connection, "Webapp"))
        {
            // No "Webapp" table, so this must be a pre-4.0 database
            return STATUS_PRE_4_0;
        }
        
        // Finally, Check if the necessary Flyway table ("schema_version") exists in this database
        if(!tableExists(connection, flyway.getTable()))
        {
            // No Flyway table, so we need to get Flyway initialized in this database
            return STATUS_NO_FLYWAY;
        }
        
        // IF we get here, we have 4.0 or above compatible database and Flyway is already installed
        return STATUS_FLYWAY;
    }
    
    
    /**
     * Determine if a particular database table exists in our database
     * 
     * @param connection
     *          Current Database Connection
     * @param tableName
     *          The name of the table
     * @return true if table of that name exists, false otherwise
     */
    public static boolean tableExists(Connection connection, String tableName)
    {
        // Get the name of the Schema that the DSpace Database is using
        // (That way we can search the right schema for this table)
        String schema = ConfigurationManager.getProperty("db.schema");
        if(StringUtils.isBlank(schema)){
            schema = null;
        }
        
        boolean exists = false;        
        ResultSet results = null;
        
        try
        {
            // Get information about our database.
            DatabaseMetaData meta = connection.getMetaData();
            
            // Search for a table of the given name in our current schema
            results = meta.getTables(null, schema, tableName, null);
            if (results!=null && results.next()) 
            {
                exists = true;
            }   
        }
        catch(SQLException e)
        {
            log.error("Error attempting to determine if table " + tableName + " exists", e);
        }
        finally
        {
            try
            {
                // ensure the ResultSet gets closed
                if(results!=null && !results.isClosed())
                    results.close();
            }
            catch(SQLException e)
            {
                // ignore it
            }
        }
        
        return exists;
    }
}
