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
import java.util.ArrayList;
import javax.sql.DataSource;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchServiceException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.internal.dbsupport.DbSupport;
import org.flywaydb.core.internal.dbsupport.DbSupportFactory;
import org.flywaydb.core.internal.dbsupport.SqlScript;
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
        if (argv.length < 1)
        {
            System.out.println("\nDatabase action argument is missing.");
            System.out.println("Valid actions: 'test', 'info', 'migrate', 'repair' or 'clean'");
            System.out.println("\nOr, type 'database help' for more information.\n");
            System.exit(1);
        }

        try
        {
            // Call initDataSource to JUST initialize the dataSource WITHOUT fully
            // initializing the DatabaseManager itself. This ensures we do NOT
            // immediately run our Flyway DB migrations on this database
            DataSource dataSource = DatabaseManager.initDataSource();

            // Get configured DB URL for reporting below
            String url = ConfigurationManager.getProperty("db.url");

            // Point Flyway API to our database
            Flyway flyway = setupFlyway(dataSource);

            // "test" = Test Database Connection
            if(argv[0].equalsIgnoreCase("test"))
            {
                // Try to connect to the database
                System.out.println("\nAttempting to connect to database using these configurations: ");
                System.out.println(" - URL: " + url);
                System.out.println(" - Driver: " + ConfigurationManager.getProperty("db.driver"));
                System.out.println(" - Username: " + ConfigurationManager.getProperty("db.username"));
                System.out.println(" - Password: [hidden]");
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
                System.out.println("\nDatabase URL: " + url);
                System.out.println("Database Schema: " + getSchemaName(connection));
                System.out.println("Database Software: " + meta.getDatabaseProductName() + " version " + meta.getDatabaseProductVersion());
                System.out.println("Database Driver: " + meta.getDriverName() + " version " + meta.getDriverVersion());

                // Get info table from Flyway
                System.out.println("\n" + MigrationInfoDumper.dumpToAsciiTable(flyway.info().all()));

                // If Flyway is NOT yet initialized, also print the determined version information
                // NOTE: search is case sensitive, as flyway table name is ALWAYS lowercase,
                // See: http://flywaydb.org/documentation/faq.html#case-sensitive
                if(!tableExists(connection, flyway.getTable(), true))
                {
                    System.out.println("\nNOTE: This database is NOT yet initialized for auto-migrations (via Flyway).");
                    // Determine which version of DSpace this looks like
                    String dbVersion = determineDBVersion(connection);
                    if (dbVersion!=null)
                    {
                        System.out.println("\nYour database looks to be compatible with DSpace version " + dbVersion);
                        System.out.println("All upgrades *after* version " + dbVersion + " will be run during the next migration.");
                        System.out.println("\nIf you'd like to upgrade now, simply run 'dspace database migrate'.");
                    }
                }
                connection.close();
            }
            // "migrate" = Manually run any outstanding Database migrations (if any)
            else if(argv[0].equalsIgnoreCase("migrate"))
            {
                System.out.println("\nDatabase URL: " + url);

                // "migrate" allows for an OPTIONAL second argument:
                //    - "ignored" = Also run any previously "ignored" migrations during the migration
                //    - [version] = ONLY run migrations up to a specific DSpace version (ONLY FOR TESTING)
                if(argv.length==2)
                {
                    if(argv[1].equalsIgnoreCase("ignored"))
                    {
                        System.out.println("Migrating database to latest version AND running previously \"Ignored\" migrations... (Check logs for details)");
                        Connection connection = dataSource.getConnection();
                        // Update the database to latest version, but set "outOfOrder=true"
                        // This will ensure any old migrations in the "ignored" state are now run
                        updateDatabase(dataSource, connection, null, true);
                        connection.close();
                    }
                    else
                    {
                        // Otherwise, we assume "argv[1]" is a valid migration version number
                        // This is only for testing! Never specify for Production!
                        System.out.println("Migrating database ONLY to version " + argv[1] + " ... (Check logs for details)");
                        System.out.println("\nWARNING: It is highly likely you will see errors in your logs when the Metadata");
                        System.out.println("or Bitstream Format Registry auto-update. This is because you are attempting to");
                        System.out.println("use an OLD version " + argv[1] + " Database with a newer DSpace API. NEVER do this in a");
                        System.out.println("PRODUCTION scenario. The resulting old DB is only useful for migration testing.\n");
                        Connection connection = dataSource.getConnection();
                        // Update the database, to the version specified.
                        updateDatabase(dataSource, connection, argv[1], false);
                        connection.close();
                    }
                }
                else
                {
                    System.out.println("Migrating database to latest version... (Check logs for details)");
                    // NOTE: This looks odd, but all we really need to do is ensure the
                    // DatabaseManager auto-initializes. It'll take care of the migration itself.
                    // Asking for our DB Name will ensure DatabaseManager.initialize() is called.
                    DatabaseManager.getDbName();
                }
                System.out.println("Done.");
            }
            // "repair" = Run Flyway repair script
            else if(argv[0].equalsIgnoreCase("repair"))
            {
                System.out.println("\nDatabase URL: " + url);
                System.out.println("Attempting to repair any previously failed migrations via FlywayDB... (Check logs for details)");
                flyway.repair();
                System.out.println("Done.");
            }
            // "clean" = Run Flyway clean script
            else if(argv[0].equalsIgnoreCase("clean"))
            {
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("\nDatabase URL: " + url);
                System.out.println("\nWARNING: ALL DATA AND TABLES IN YOUR DATABASE WILL BE PERMANENTLY DELETED.\n");
                System.out.println("There is NO turning back from this action. Backup your DB before continuing.");
                System.out.println("If you are using Oracle, your RECYCLEBIN will also be PURGED.\n");
                System.out.print("Do you want to PERMANENTLY DELETE everything from your database? [y/n]: ");
                String choiceString = input.readLine();
                input.close();

                if (choiceString.equalsIgnoreCase("y"))
                {
                    System.out.println("Scrubbing database clean... (Check logs for details)");
                    cleanDatabase(flyway, dataSource);
                    System.out.println("Done.");
                }
            }
            else
            {
                System.out.println("\nUsage: database [action]");
                System.out.println("Valid actions: 'test', 'info', 'migrate', 'repair' or 'clean'");
                System.out.println(" - test    = Test database connection is OK");
                System.out.println(" - info    = Describe basic info about database, including migrations run");
                System.out.println(" - migrate = Migrate the Database to the latest version");
                System.out.println("             Optionally, specify \"ignored\" to also run \"Ignored\" migrations");
                System.out.println(" - repair  = Attempt to repair any previously failed database migrations");
                System.out.println(" - clean   = DESTROY all data and tables in Database (WARNING there is no going back!)");
                System.out.println("");
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
                String dbType = DatabaseManager.findDbKeyword(meta);
                connection.close();

                // Determine location(s) where Flyway will load all DB migrations
                ArrayList<String> scriptLocations = new ArrayList<String>();

                // First, add location for custom SQL migrations, if any (based on DB Type)
                // e.g. [dspace.dir]/etc/[dbtype]/
                // (We skip this for H2 as it's only used for unit testing)
                if(!dbType.equals(DatabaseManager.DBMS_H2))
                {
                    scriptLocations.add("filesystem:" + ConfigurationManager.getProperty("dspace.dir") +
                                        "/etc/" + dbType);
                }

                // Also add the Java package where Flyway will load SQL migrations from (based on DB Type)
                scriptLocations.add("classpath:org.dspace.storage.rdbms.sqlmigration." + dbType);

                // Also add the Java package where Flyway will load Java migrations from
                // NOTE: this also loads migrations from any sub-package
                scriptLocations.add("classpath:org.dspace.storage.rdbms.migration");

                // Special scenario: If XMLWorkflows are enabled, we need to run its migration(s)
                // as it REQUIRES database schema changes. XMLWorkflow uses Java migrations
                // which first check whether the XMLWorkflow tables already exist
                if (ConfigurationManager.getProperty("workflow", "workflow.framework").equals("xmlworkflow"))
                {
                    scriptLocations.add("classpath:org.dspace.storage.rdbms.xmlworkflow");
                }

                // Now tell Flyway which locations to load SQL / Java migrations from
                log.info("Loading Flyway DB migrations from: " + StringUtils.join(scriptLocations, ", "));
                flywaydb.setLocations(scriptLocations.toArray(new String[scriptLocations.size()]));

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
        // By default, upgrade to the *latest* version and never run migrations out-of-order
        updateDatabase(datasource, connection, null, false);
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
     * @param targetVersion
     *      If specified, only migrate the database to a particular *version* of DSpace. This is mostly just useful for testing.
     *      If null, the database is migrated to the latest version.
     * @param outOfOrder
     *      If true, Flyway will run any lower version migrations that were previously "ignored".
     *      If false, Flyway will only run new migrations with a higher version number.
     * @throws SQLException
     *      If database cannot be upgraded.
     */
    protected static synchronized void updateDatabase(DataSource datasource, Connection connection, String targetVersion, boolean outOfOrder)
            throws SQLException
    {
        try
        {
            // Setup Flyway API against our database
            Flyway flyway = setupFlyway(datasource);

            // Set whethe Flyway will run migrations "out of order". By default, this is false,
            // and Flyway ONLY runs migrations that have a higher version number.
            flyway.setOutOfOrder(outOfOrder);

            // If a target version was specified, tell Flyway to ONLY migrate to that version
            // (i.e. all later migrations are left as "pending"). By default we always migrate to latest version.
            if(!StringUtils.isBlank(targetVersion))
            {
                flyway.setTarget(targetVersion);
            }

            // Does the necessary Flyway table ("schema_version") exist in this database?
            // If not, then this is the first time Flyway has run, and we need to initialize
            // NOTE: search is case sensitive, as flyway table name is ALWAYS lowercase,
            // See: http://flywaydb.org/documentation/faq.html#case-sensitive
            if(!tableExists(connection, flyway.getTable(), true))
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
        catch(FlywayException fe)
        {
            // If any FlywayException (Runtime) is thrown, change it to a SQLException
            throw new SQLException("Flyway migration error occurred", fe);
        }
    }

    /**
     * Clean the existing database, permanently removing all data and tables
     * <P>
     * FlywayDB (http://flywaydb.org/) is used to clean the database
     *
     * @param flyway
     *      Initialized Flyway object
     * @param dataSource
     *      Initialized DataSource
     * @throws SQLException
     *      If database cannot be cleaned.
     */
    private static synchronized void cleanDatabase(Flyway flyway, DataSource dataSource)
            throws SQLException
    {
        try
        {
            // First, run Flyway's clean command on database.
            // For MOST database types, this takes care of everything
            flyway.clean();

            Connection connection = null;
            try
            {
                // Get info about which database type we are using
                connection = dataSource.getConnection();
                DatabaseMetaData meta = connection.getMetaData();
                String dbKeyword = DatabaseManager.findDbKeyword(meta);

                // If this is Oracle, the only way to entirely clean the database
                // is to also purge the "Recyclebin". See:
                // http://docs.oracle.com/cd/B19306_01/server.102/b14200/statements_9018.htm
                if(dbKeyword.equals(DatabaseManager.DBMS_ORACLE))
                {
                    PreparedStatement statement = null;
                    try
                    {
                        statement = connection.prepareStatement("PURGE RECYCLEBIN");
                        statement.executeQuery();
                    }
                    finally
                    {
                        if(statement!=null && !statement.isClosed())
                            statement.close();
                    }
                }
            }
            finally
            {
                if(connection!=null && !connection.isClosed())
                    connection.close();
            }
        }
        catch(FlywayException fe)
        {
            // If any FlywayException (Runtime) is thrown, change it to a SQLException
            throw new SQLException("Flyway clean error occurred", fe);
        }
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

        // Is this pre-DSpace 5.0 (with Metadata 4 All changes)? Look for the "resource_id" column in the "metadatavalue" table
        if(tableColumnExists(connection, "metadatavalue", "resource_id"))
        {
            return "5.0.2014.09.26"; // This version matches the version in the SQL migration for this feature
        }

        // Is this pre-DSpace 5.0 (with Helpdesk plugin)? Look for the "request_message" column in the "requestitem" table
        if(tableColumnExists(connection, "requestitem", "request_message"))
        {
            return "5.0.2014.08.08"; // This version matches the version in the SQL migration for this feature
        }

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
        //By default, do a case-insensitive search
        return tableExists(connection, tableName, false);
    }

    /**
     * Determine if a particular database table exists in our database
     *
     * @param connection
     *          Current Database Connection
     * @param tableName
     *          The name of the table
     * @param caseSensitive
     *          When "true", the case of the tableName will not be changed.
     *          When "false, the name may be uppercased or lowercased based on DB type.
     * @return true if table of that name exists, false otherwise
     */
    public static boolean tableExists(Connection connection, String tableName, boolean caseSensitive)
    {
        boolean exists = false;
        ResultSet results = null;

        try
        {
            // Get the name of the Schema that the DSpace Database is using
            // (That way we can search the right schema)
            String schema = getSchemaName(connection);

            // Get information about our database.
            DatabaseMetaData meta = connection.getMetaData();

            // If this is not a case sensitive search
            if(!caseSensitive)
            {
                // Canonicalize everything to the proper case based on DB type
                schema = canonicalize(connection, schema);
                tableName = canonicalize(connection, tableName);
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
        boolean exists = false;
        ResultSet results = null;

        try
        {
            // Get the name of the Schema that the DSpace Database is using
            // (That way we can search the right schema)
            String schema = getSchemaName(connection);

            // Canonicalize everything to the proper case based on DB type
            schema = canonicalize(connection, schema);
            tableName = canonicalize(connection, tableName);
            columnName = canonicalize(connection, columnName);

            // Get information about our database.
            DatabaseMetaData meta = connection.getMetaData();

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
        boolean exists = false;
        PreparedStatement statement = null;
        ResultSet results = null;
        // Whether or not to filter query based on schema (this is DB Type specific)
        boolean schemaFilter = false;

        try
        {
            // Get the name of the Schema that the DSpace Database is using
            // (That way we can search the right schema)
            String schema = getSchemaName(connection);

            // Canonicalize everything to the proper case based on DB type
            schema = canonicalize(connection, schema);
            sequenceName = canonicalize(connection, sequenceName);

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
                statement.setString(1, sequenceName);
                if(schemaFilter)
                {
                    statement.setString(2, schema);
                }
                results = statement.executeQuery();

                // If results are non-zero, then this sequence exists!
                if(results!=null && results.next() && results.getInt(1)>0)
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
     * Execute a block of SQL against the current database connection.
     * <P>
     * The SQL is executed using the Flyway SQL parser.
     *
     * @param connection
     *            Current Database Connection
     * @param sqlToExecute
     *            The actual SQL to execute as a String
     * @throws SQLException
     *            If a database error occurs
     */
    public static void executeSql(Connection connection, String sqlToExecute) throws SQLException
    {
        try
        {
            // Create a Flyway DbSupport object (based on our connection)
            // This is how Flyway determines the database *type* (e.g. Postgres vs Oracle)
            DbSupport dbSupport = DbSupportFactory.createDbSupport(connection, false);

            // Load our SQL string & execute via Flyway's SQL parser
            SqlScript script = new SqlScript(sqlToExecute, dbSupport);
            script.execute(dbSupport.getJdbcTemplate());
        }
        catch(FlywayException fe)
        {
            // If any FlywayException (Runtime) is thrown, change it to a SQLException
            throw new SQLException("Flyway executeSql() error occurred", fe);
        }
    }

    /**
     * Get the Database Schema Name in use by this Connection, so that it can
     * be used to limit queries in other methods (e.g. tableExists()).
     * <P>
     * NOTE: Once we upgrade to using Apache Commons DBCP / Pool version 2.0,
     * this method WILL BE REMOVED in favor of java.sql.Connection's new
     * "getSchema()" method.
     * http://docs.oracle.com/javase/7/docs/api/java/sql/Connection.html#getSchema()
     * 
     * @param connection 
     *            Current Database Connection
     * @return Schema name as a string, or "null" if cannot be determined or unspecified
     */
    public static String getSchemaName(Connection connection)
            throws SQLException
    {
        String schema = null;
        DatabaseMetaData meta = connection.getMetaData();
        
        // Check the configured "db.schema" FIRST for the value configured there
        schema = DatabaseManager.canonicalize(ConfigurationManager.getProperty("db.schema"));
        
        // If unspecified, determine "sane" defaults based on DB type
        if(StringUtils.isBlank(schema))
        {
            String dbType = DatabaseManager.findDbKeyword(meta);
        
            if(dbType.equals(DatabaseManager.DBMS_POSTGRES))
            {
                // For PostgreSQL, the default schema is named "public"
                // See: http://www.postgresql.org/docs/9.0/static/ddl-schemas.html
                schema = "public";
            }
            else if (dbType.equals(DatabaseManager.DBMS_ORACLE))
            {
                // For Oracle, default schema is actually the user account
                // See: http://stackoverflow.com/a/13341390
                schema = meta.getUserName();
            }
            else
                schema = null;
        }
        
        return schema;
    }
    
    /**
     * Return the canonical name for a database identifier based on whether this
     * database defaults to storing identifiers in uppercase or lowercase.
     *
     * @param connection 
     *            Current Database Connection
     * @param dbIdentifier 
     *            Identifier to canonicalize (may be a table name, column name, etc)
     * @return The canonical name of the identifier.
     */
    public static String canonicalize(Connection connection, String dbIdentifier)
            throws SQLException
    {
        // Avoid any null pointers
        if(dbIdentifier==null)
            return null;
        
        DatabaseMetaData meta = connection.getMetaData();

        // Check how this database stores its identifiers, etc.
        // i.e. lowercase vs uppercase (by default we assume mixed case)
        if(meta.storesLowerCaseIdentifiers())
        {
            return StringUtils.lowerCase(dbIdentifier);
            
        }
        else if(meta.storesUpperCaseIdentifiers())
        {
            return StringUtils.upperCase(dbIdentifier);
        }
        else // Otherwise DB doesn't care about case
        {    
            return dbIdentifier;
        }
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

                        // Reindex Discovery completely
                        // Force clean all content
                        this.indexer.cleanIndex(true);
                        // Recreate the entire index (overwriting existing one)
                        this.indexer.createIndex(context);
                        // Rebuild spell checker (which is based on index)
                        this.indexer.buildSpellCheck();

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
                        // Reset our indexing flag. Indexing is done or it threw an error,
                        // Either way, we shouldn't try again.
                        DatabaseUtils.setReindexDiscovery(false);

                        // Clean up our context, if it still exists
                        if(context!=null && context.isValid())
                            context.abort();
                    }
                }
            }
        }
    }
}
