/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchServiceException;
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

     // When this temp file exists, the "checkReindexDiscovery()" method will auto-reindex Discovery
    // Reindex flag file is at [dspace]/solr/search/conf/reindex.flag
    // See also setReindexDiscovery()/getReindexDiscover()
    private static final String reindexDiscoveryFilePath = ConfigurationManager.getProperty("dspace.dir") +
                            File.separator + "solr" +
                            File.separator + "search" +
                            File.separator + "conf" +
                            File.separator + "reindex.flag";

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
            System.out.println("Valid actions include: 'test', 'info', 'migrate', 'repair' or 'clean'");
            System.out.println("Or, type 'database help' for more information.");
            System.exit(1);
        }

        try
        {
            // Call initDataSource to JUST initialize the dataSource WITHOUT fully
            // initializing the DatabaseManager itself. This ensures we do NOT
            // immediately run our Flyway DB migrations on this database
            DataSource dataSource = DatabaseManager.initDataSource();
            
            // Point Flyway API to our database
            Flyway flyway = setupFlyway(dataSource);

            // "test" = Test Database Connection
            if(argv[0].equalsIgnoreCase("test"))
            {
                // Get our DB url info
                String url = ConfigurationManager.getProperty("db.url");

                // Try to connect to the database
                System.out.println("\nAttempting to connect to database: ");
                System.out.println(" - URL: " + url);
                System.out.println(" - Driver: " + ConfigurationManager.getProperty("db.driver"));
                System.out.println(" - Username: " + ConfigurationManager.getProperty("db.username"));
                System.out.println(" - Password: " + ConfigurationManager.getProperty("db.password"));
                System.out.println(" - Schema: " + ConfigurationManager.getProperty("db.schema"));
                System.out.println("\nTesting connection...");
                try
                {
                    // Just do a high level test by getting our configured DataSource and attempting to connect to it
                    // NOTE: We specifically do NOT call DatabaseManager.getConnection() because that will attempt
                    // a full initialization of DatabaseManager & also cause database migrations/upgrades to occur
                    Connection connection = dataSource.getConnection();
                    connection.close();
                }
                catch (SQLException sqle)
                {
                    System.err.println("\nError: ");
                    System.err.println(" - " + sqle);
                    System.err.println("\nPlease see the DSpace documentation for assistance.\n");
                    System.exit(1);
                }

                System.out.println("Connected successfully!\n");
            }
            // "info" = Basic Database Information
            else if(argv[0].equalsIgnoreCase("info"))
            {
                // Get basic Database info
                Connection connection = dataSource.getConnection();
                DatabaseMetaData meta = connection.getMetaData();
                System.out.println("\nDatabase: " + meta.getDatabaseProductName() + " version " + meta.getDatabaseProductVersion());
                System.out.println("Database Driver: " + meta.getDriverName() + " version " + meta.getDriverVersion());
                connection.close();

                // Get info table from Flyway
                System.out.println("\n" + MigrationInfoDumper.dumpToAsciiTable(flyway.info().all()));
            }
            // "migrate" = Manually run any outstanding Database migrations (if any)
            else if(argv[0].equalsIgnoreCase("migrate"))
            {
                System.out.println("Migrating database to latest version... (Check logs for details)");
                // NOTE: This looks odd, but all we really need to do is ensure the
                // DatabaseManager auto-initializes. It'll take care of the migration itself.
                // Asking for our DB Name will ensure DatabaseManager.initialize() is called.
                DatabaseManager.getDbName();
            }
            // "repair" = Run Flyway repair script
            else if(argv[0].equalsIgnoreCase("repair"))
            {
                System.out.println("Attempting to repair any previously failed migrations via FlywayDB... (Check logs for details)");
                flyway.repair();
            }
            // "clean" = Run Flyway clean script
            else if(argv[0].equalsIgnoreCase("clean"))
            {
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("If you continue, ALL DATA AND TABLES IN YOUR DATABASE WILL BE PERMANENTLY DELETED. \n");
                System.out.println("There is no turning back from this action. You should backup your database before continuing. \n");
                System.out.print("Are you sure you want to clear your entire database? [y/n]: ");
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
                System.out.println("\nUsage: database [action]");
                System.out.println("Valid actions include: 'test', info', 'migrate', 'repair' or 'clean'");
                System.out.println(" - test    = Test database connection is OK");
                System.out.println(" - info    = Describe basic info about Database (type, version, driver)");
                System.out.println(" - migrate = Migrate the Database to the latest version");
                System.out.println(" - repair  = Attempt to repair any previously failed database migrations (see also Flyway repair command)");
                System.out.println(" - clean   = Destroy all data (Warning there is no going back!)");
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
            try(Connection connection = datasource.getConnection())
            {
                // Initialize Flyway DB API (http://flywaydb.org/), used to perform DB migrations
                flywaydb = new Flyway();
                flywaydb.setDataSource(datasource);
                flywaydb.setEncoding("UTF-8");

                // Migration scripts are based on DBMS Keyword (see full path below)
                DatabaseMetaData meta = connection.getMetaData();
                // NOTE: we use "findDbKeyword()" here as it won't cause
                // DatabaseManager.initialize() to be called (which in turn auto-calls Flyway)
                String scriptFolder = DatabaseManager.findDbKeyword(meta);
                connection.close();

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
                // NOTE: DatabaseLegacyReindexer only indexes in Legacy Lucene & RDBMS indexes. It can be removed once those are obsolete.
                flywaydb.setCallbacks(new DatabaseRegistryUpdater(), new DatabaseLegacyReindexer());
            }
            catch(SQLException e)
            {
                log.error("Unable to setup Flyway against DSpace database", e);
            }
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
        // Setup Flyway API against our database
        Flyway flyway = setupFlyway(datasource);

        // Does the necessary Flyway table ("schema_version") exist in this database?
        // If not, then this is the first time Flyway has run, and we need to initialize
        if(!tableExists(connection, flyway.getTable()))
        {
            // Try to determine our DSpace database version, so we know what to tell Flyway to do
            String dbVersion = determineDBVersion(connection);

            // If this is a fresh install, dbVersion will be null
            if (dbVersion==null)
            {
                // Initialize the Flyway database table with defaults (version=1)
                flyway.init();
            }
            else
            {
                // Otherwise, pass our determined DB version to Flyway to initialize database table
                flyway.setInitVersion(dbVersion);
                flyway.setInitDescription("Initializing from DSpace " + dbVersion + " database schema");
                flyway.init();
            }
        }

        // Determine pending Database migrations
        MigrationInfo[] pending = flyway.info().pending();

        // As long as there are pending migrations, log them and run migrate()
        if (pending!=null && pending.length>0)
        {
            log.info("Pending DSpace database schema migrations:");
            for (MigrationInfo info : pending)
            {
                log.info("\t" + info.getVersion() + " " + info.getDescription() + " " + info.getType() + " " + info.getState());
            }

            // Run all pending Flyway migrations to ensure the DSpace Database is up to date
            flyway.migrate();

            // Flag that Discovery will need reindexing, since database was updated
            setReindexDiscovery(true);
        }
        else
            log.info("DSpace database schema is up to date");
    }

    /**
     * Attempt to determine the version of our DSpace database,
     * so that we are able to properly migrate it to the latest schema
     * via Flyway
     * <P>
     * This determination is performed by checking which table(s) exist in
     * your database and matching them up with known tables that existed in
     * different versions of DSpace.
     *
     * @param connection
     *          Current Database Connection
     * @param flyway
     *          Our Flyway settings
     * @throws SQLException if DB status cannot be determined
     * @return DSpace version as a String (e.g. "4.0"), or null if database is empty
     */
    private static String determineDBVersion(Connection connection)
            throws SQLException
    {
        // First, is this a "fresh_install"?  Check for an "item" table.
        if(!tableExists(connection, "Item"))
        {
            // Item table doesn't exist. This database must be a fresh install
            return null;
        }

        // We will now check prior versions in reverse chronological order, looking
        // for specific tables or columns that were newly created in each version.

        // Is this DSpace 4.x? Look for the "Webapp" table created in that version.
        if(tableExists(connection, "Webapp"))
        {
            return "4.0";
        }

        // Is this DSpace 3.x? Look for the "versionitem" table created in that version.
        if(tableExists(connection, "versionitem"))
        {
            return "3.0";
        }

        // Is this DSpace 1.8.x? Look for the "bitstream_order" column in the "bundle2bitstream" table
        if(tableColumnExists(connection, "bundle2bitstream", "bitstream_order"))
        {
            return "1.8";
        }

        // Is this DSpace 1.7.x? Look for the "dctyperegistry_seq" to NOT exist (it was deleted in 1.7)
        // NOTE: DSPACE 1.7.x only differs from 1.6 in a deleted sequence.
        if(!sequenceExists(connection, "dctyperegistry_seq"))
        {
            return "1.7";
        }

        // Is this DSpace 1.6.x? Look for the "harvested_collection" table created in that version.
        if(tableExists(connection, "harvested_collection"))
        {
            return "1.6";
        }

        // Is this DSpace 1.5.x? Look for the "collection_item_count" table created in that version.
        if(tableExists(connection, "collection_item_count"))
        {
            return "1.5";
        }

        // Is this DSpace 1.4.x? Look for the "Group2Group" table created in that version.
        if(tableExists(connection, "Group2Group"))
        {
            return "1.4";
        }

        // Is this DSpace 1.3.x? Look for the "epersongroup2workspaceitem" table created in that version.
        if(tableExists(connection, "epersongroup2workspaceitem"))
        {
            return "1.3";
        }

        // Is this DSpace 1.2.x? Look for the "Community2Community" table created in that version.
        if(tableExists(connection, "Community2Community"))
        {
            return "1.2";
        }

        // Is this DSpace 1.1.x? Look for the "Community" table created in that version.
        if(tableExists(connection, "Community"))
        {
            return "1.1";
        }

        // IF we get here, something went wrong! This database is missing a LOT of DSpace tables
        throw new SQLException("CANNOT AUTOUPGRADE DSPACE DATABASE, AS IT DOES NOT LOOK TO BE A VALID DSPACE DATABASE.");
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

            // Check how this database stores its table names, etc.
            // i.e. lowercase vs uppercase (by default we assume mixed case)
            if(meta.storesLowerCaseIdentifiers())
            {
                schema = (schema == null) ? null : StringUtils.lowerCase(schema);
                tableName = StringUtils.lowerCase(tableName);
            }
            else if(meta.storesUpperCaseIdentifiers())
            {
                schema = (schema == null) ? null : StringUtils.upperCase(schema);
                tableName = StringUtils.upperCase(tableName);
            }

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

    /**
     * Determine if a particular database column exists in our database
     *
     * @param connection
     *          Current Database Connection
     * @param tableName
     *          The name of the table
     * @param columnName
     *          The name of the column in the table
     * @return true if column of that name exists, false otherwise
     */
    public static boolean tableColumnExists(Connection connection, String tableName, String columnName)
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

            // Check how this database stores its table names, etc.
            // i.e. lowercase vs uppercase (by default we assume mixed case)
            if(meta.storesLowerCaseIdentifiers())
            {
                schema = (schema == null) ? null : StringUtils.lowerCase(schema);
                tableName = StringUtils.lowerCase(tableName);
                columnName = StringUtils.lowerCase(columnName);
            }
            else if(meta.storesUpperCaseIdentifiers())
            {
                schema = (schema == null) ? null : StringUtils.upperCase(schema);
                tableName = StringUtils.upperCase(tableName);
                columnName = StringUtils.upperCase(columnName);
            }

            // Search for a column of that name in the specified table & schema
            results = meta.getColumns(null, schema, tableName, columnName);
            if (results!=null && results.next())
            {
                exists = true;
            }
        }
        catch(SQLException e)
        {
            log.error("Error attempting to determine if column " + columnName + " exists", e);
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

    /*
     * Determine if a particular database sequence exists in our database
     *
     * @param connection
     *          Current Database Connection
     * @param sequenceName
     *          The name of the table
     * @return true if sequence of that name exists, false otherwise
     */
    public static boolean sequenceExists(Connection connection, String sequenceName)
    {
        // Get the name of the Schema that the DSpace Database is using
        // (That way we can search the right schema)
        String schema = ConfigurationManager.getProperty("db.schema");
        if(StringUtils.isBlank(schema)){
            schema = null;
        }

        boolean exists = false;
        PreparedStatement statement = null;
        ResultSet results = null;
        // Whether or not to filter query based on schema (this is DB Type specific)
        boolean schemaFilter = false;

        try
        {
            // Different database types store sequence information in different tables
            String dbtype = DatabaseManager.findDbKeyword(connection.getMetaData());
            String sequenceSQL = null;
            switch(dbtype)
            {
                case DatabaseManager.DBMS_POSTGRES:
                    // Default schema in PostgreSQL is "public"
                    if(schema == null)
                    {
                        schema = "public";
                    }
                    // PostgreSQL specific query for a sequence in a particular schema
                    sequenceSQL = "SELECT COUNT(1) FROM pg_class, pg_namespace " +
                                    "WHERE pg_class.relnamespace=pg_namespace.oid " +
                                    "AND pg_class.relkind='S' " +
                                    "AND pg_class.relname=? " +
                                    "AND pg_namespace.nspname=?";
                    // We need to filter by schema in PostgreSQL
                    schemaFilter = true;
                    break;
                case DatabaseManager.DBMS_ORACLE:
                    // Oracle specific query for a sequence owned by our current DSpace user
                    // NOTE: No need to filter by schema for Oracle, as Schema = User
                    sequenceSQL = "SELECT COUNT(1) FROM user_sequences WHERE sequence_name=?";
                    break;
                case DatabaseManager.DBMS_H2:
                    // In H2, sequences are listed in the "information_schema.sequences" table
                    // SEE: http://www.h2database.com/html/grammar.html#information_schema
                    sequenceSQL = "SELECT COUNT(1) " +
                                    "FROM INFORMATION_SCHEMA.SEQUENCES " +
                                    "WHERE SEQUENCE_NAME = ?";
                    break;
                default:
                    throw new SQLException("DBMS " + dbtype + " is unsupported.");
            }

            // If we have a SQL query to run for the sequence, then run it
            if (sequenceSQL!=null)
            {
                // Run the query, passing it our parameters
                statement = connection.prepareStatement(sequenceSQL);
                statement.setString(1, StringUtils.upperCase(sequenceName));
                if(schemaFilter)
                {
                    statement.setString(2, StringUtils.upperCase(schema));
                }
                results = statement.executeQuery();

                // If results are non-zero, then this sequence exists!
                if(results!=null && results.next())
                {
                   exists = true;
                }
            }
        }
        catch(SQLException e)
        {
            log.error("Error attempting to determine if sequence " + sequenceName + " exists", e);
        }
        finally
        {
            try
            {
                // Ensure statement gets closed
                if(statement!=null && !statement.isClosed())
                    statement.close();
                // Ensure ResultSet gets closed
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

    /**
     * Whether or not to tell Discovery to reindex itself based on the updated
     * database.
     * <P>
     * Whenever a DB migration occurs this is set to "true" to ensure the
     * Discovery index is updated. When Discovery initializes it calls
     * checkReindexDiscovery() to reindex if this flag is true.
     * <P>
     * Because the DB migration may be initialized by commandline or any one of
     * the many DSpace webapps, setting this to "true" actually writes a temporary
     * file which lets Solr know when reindex is needed.
     * @param reindex true or false
     */
    public static synchronized void setReindexDiscovery(boolean reindex)
    {
        File reindexFlag = new File(reindexDiscoveryFilePath);

        // If we need to flag Discovery to reindex, we'll create a temporary file to do so.
        if(reindex)
        {
            try
            {
                //If our flag file doesn't exist, create it as writeable to all
                if(!reindexFlag.exists())
                {
                    reindexFlag.createNewFile();
                    reindexFlag.setWritable(true, false);
                }
            }
            catch(IOException io)
            {
                log.error("Unable to create Discovery reindex flag file " + reindexFlag.getAbsolutePath() + ". You may need to reindex manually.", io);
            }
        }
        else // Otherwise, Discovery doesn't need to reindex. Delete the temporary file if it exists
        {
            //If our flag file exists, delete it
            if(reindexFlag.exists())
            {
                boolean deleted = reindexFlag.delete();
                if(!deleted)
                    log.error("Unable to delete Discovery reindex flag file " + reindexFlag.getAbsolutePath() + ". You may need to delete it manually.");
            }
        }
    }

    /**
     * Whether or not reindexing is required in Discovery.
     * <P>
     * Because the DB migration may be initialized by commandline or any one of
     * the many DSpace webapps, this checks for the existence of a temporary
     * file to know when Discovery/Solr needs reindexing.
     * @return whether reindex flag is true/false
     */
    public static boolean getReindexDiscovery()
    {
        // Simply check if the flag file exists
        File reindexFlag = new File(reindexDiscoveryFilePath);
        return reindexFlag.exists();
    }

    /**
     * Method to check whether we need to reindex in Discovery (i.e. Solr). If
     * reindexing is necessary, it is performed. If not, nothing happens.
     * <P>
     * This method is called by Discovery whenever it initializes a connection
     * to Solr.
     *
     * @param indexer
     *          The actual indexer to use to reindex Discovery, if needed
     * @see org.dspace.discovery.SolrServiceImpl
     */
    public static synchronized void checkReindexDiscovery(IndexingService indexer)
    {
        // We only do something if the reindexDiscovery flag has been triggered
        if(getReindexDiscovery())
        {
            // Kick off a custom thread to perform the reindexing in Discovery
            // (See ReindexerThread nested class below)
            ReindexerThread go = new ReindexerThread(indexer);
            go.start();
        }
    }

    /**
     * Internal class to actually perform re-indexing in a separate thread.
     * (See checkReindexDiscovery() method)>
     */
    private static class ReindexerThread extends Thread
    {
        private final IndexingService indexer;

        /**
         * Constructor. Pass it an existing IndexingService
         * @param indexer
         */
        ReindexerThread(IndexingService is)
        {
            this.indexer = is;
        }

        /**
         * Actually perform Reindexing in Discovery/Solr.
         * This is synchronized so that only one thread can get in at a time.
         */
        @Override
        public void run()
        {
            synchronized(this.indexer)
            {
                // Make sure reindexDiscovery flag is still true
                // If multiple threads get here we only want to reindex ONCE
                if(DatabaseUtils.getReindexDiscovery())
                {
                    Context context = null;
                    try
                    {
                        context = new Context();

                        log.info("Post database migration, reindexing all content in Discovery search and browse engine");

                        // Reindex Discovery (just clean & update index)
                        this.indexer.cleanIndex(true);
                        this.indexer.updateIndex(context, true);

                        // Reset our indexing flag. Indexing is done.
                        DatabaseUtils.setReindexDiscovery(false);
                        log.info("Reindexing is complete");
                    }
                    catch(SearchServiceException sse)
                    {
                        log.warn("Unable to reindex content in Discovery search and browse engine. You may need to reindex manually.", sse);
                    }
                    catch(SQLException | IOException e)
                    {
                        log.error("Error attempting to reindex all contents for search/browse", e);
                    }
                    finally
                    {
                        // Clean up our context, if it still exists
                        if(context!=null && context.isValid())
                            context.abort();
                    }
                }
            }
        }
    }
}
