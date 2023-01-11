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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.internal.info.MigrationInfoDumper;
import org.flywaydb.core.internal.license.VersionPrinter;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Utility class used to manage the Database. This class is used by the
 * DatabaseManager to initialize/upgrade/migrate the Database. It can also
 * be called via the commandline as necessary to get information about
 * the database.
 * <p>
 * Currently, we use Flyway DB (http://flywaydb.org/) for database management.
 *
 * @author Tim Donohue
 */
public class DatabaseUtils {
    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(DatabaseUtils.class);

    // When this temp file exists, the "checkReindexDiscovery()" method will auto-reindex Discovery
    // Reindex flag file is at [dspace]/solr/search/conf/reindex.flag
    // See also setReindexDiscovery()/getReindexDiscover()
    private static final String reindexDiscoveryFilePath = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                                                .getProperty("dspace.dir") +
        File.separator + "solr" +
        File.separator + "search" +
        File.separator + "conf" +
        File.separator + "reindex.flag";

    // Types of databases supported by DSpace. See getDbType()
    public static final String DBMS_POSTGRES = "postgres";
    public static final String DBMS_ORACLE = "oracle";
    public static final String DBMS_H2 = "h2";

    // Name of the table that Flyway uses for its migration history
    public static final String FLYWAY_TABLE = "schema_version";

    /**
     * Default constructor
     */
    private DatabaseUtils() { }

    /**
     * Commandline tools for managing database changes, etc.
     *
     * @param argv the command line arguments given
     */
    public static void main(String[] argv) {
        // Usage checks
        if (argv.length < 1) {
            System.out.println("\nDatabase action argument is missing.");
            System.out.println("Valid actions: 'test', 'info', 'migrate', 'repair', 'skip', 'validate', " +
                    "'update-sequences' or 'clean'");
            System.out.println("\nOr, type 'database help' for more information.\n");
            System.exit(1);
        }

        try {
            // Get a reference to our configured DataSource
            DataSource dataSource = getDataSource();

            // Initialize Flyway against our database
            FluentConfiguration flywayConfiguration = setupFlyway(dataSource);
            Flyway flyway = flywayConfiguration.load();

            // Now, check our Flyway database table to see if it needs upgrading
            // *before* any other Flyway commands can be run. This is a safety check.
            FlywayUpgradeUtils.upgradeFlywayTable(flyway, dataSource.getConnection());

            // Determine action param passed to "./dspace database"
            switch (argv[0].toLowerCase(Locale.ENGLISH)) {
                // "test" = Test Database Connection
                case "test":
                    // Try to connect to the database
                    System.out.println("\nAttempting to connect to database");
                    try (Connection connection = dataSource.getConnection()) {
                        System.out.println("Connected successfully!");

                        // Print basic database connection information
                        printDBInfo(connection);

                        // Print any database warnings/errors found (if any)
                        boolean issueFound = printDBIssues(connection);

                        // If issues found, exit with an error status (even if connection succeeded).
                        if (issueFound) {
                            System.exit(1);
                        } else {
                            System.exit(0);
                        }
                    } catch (SQLException sqle) {
                        System.err.println("\nError running 'test': ");
                        System.err.println(" - " + sqle);
                        System.err.println("\nPlease see the DSpace documentation for assistance.\n");
                        sqle.printStackTrace(System.err);
                        System.exit(1);
                    }
                    break;
                // "info" and "status" are identical and provide database info
                case "info":
                case "status":
                    try (Connection connection = dataSource.getConnection()) {
                        // Print basic Database info
                        printDBInfo(connection);

                        // Get info table from Flyway
                        System.out.println("\n" + MigrationInfoDumper.dumpToAsciiTable(flyway.info().all()));

                        // If Flyway is NOT yet initialized, also print the determined version information
                        // NOTE: search is case sensitive, as flyway table name is ALWAYS lowercase,
                        // See: http://flywaydb.org/documentation/faq.html#case-sensitive
                        if (!tableExists(connection, flyway.getConfiguration().getTable(), true)) {
                            System.out
                                .println("\nNOTE: This database is NOT yet initialized for auto-migrations " +
                                             "(via Flyway).");
                            // Determine which version of DSpace this looks like
                            String dbVersion = determineDBVersion(connection);
                            if (dbVersion != null) {
                                System.out
                                    .println("\nYour database looks to be compatible with DSpace version " + dbVersion);
                                System.out.println(
                                    "All upgrades *after* version " + dbVersion + " will be run during the next " +
                                        "migration.");
                                System.out.println("\nIf you'd like to upgrade now, simply run 'dspace database " +
                                                       "migrate'.");
                            }
                        }

                        // Print any database warnings/errors found (if any)
                        boolean issueFound = printDBIssues(connection);

                        // If issues found, exit with an error status
                        if (issueFound) {
                            System.exit(1);
                        } else {
                            System.exit(0);
                        }
                    } catch (SQLException e) {
                        System.err.println("Info exception:");
                        e.printStackTrace(System.err);
                        System.exit(1);
                    }
                    break;
                // "migrate" = Run all pending database migrations
                case "migrate":
                    try (Connection connection = dataSource.getConnection()) {
                        System.out.println("\nDatabase URL: " + connection.getMetaData().getURL());

                        // "migrate" allows for an OPTIONAL second argument (only one may be specified):
                        //    - "ignored" = Also run any previously "ignored" migrations during the migration
                        //    - "force" = Even if no pending migrations exist, still run migrate to trigger callbacks.
                        //    - [version] = ONLY run migrations up to a specific DSpace version (ONLY FOR TESTING)
                        if (argv.length == 2) {
                            if (argv[1].equalsIgnoreCase("ignored")) {
                                System.out.println(
                                    "Migrating database to latest version AND running previously \"Ignored\" " +
                                        "migrations... (Check logs for details)");
                                // Update the database to latest version, but set "outOfOrder=true"
                                // This will ensure any old migrations in the "ignored" state are now run
                                updateDatabase(dataSource, connection, null, true);
                            } else if (argv[1].equalsIgnoreCase("force")) {
                                updateDatabase(dataSource, connection, null, false, true);
                            } else {
                                // Otherwise, we assume "argv[1]" is a valid migration version number
                                // This is only for testing! Never specify for Production!
                                String migrationVersion = argv[1];
                                BufferedReader input = new BufferedReader(
                                        new InputStreamReader(System.in, StandardCharsets.UTF_8));

                                System.out.println(
                                    "You've specified to migrate your database ONLY to version " + migrationVersion +
                                        " ...");
                                System.out.println(
                                    "\nWARNING: In this mode, we DISABLE all callbacks, which means that you will " +
                                        "need to manually update registries and manually run a reindex. This is " +
                                        "because you are attempting to use an OLD version (" + migrationVersion + ") " +
                                        "Database with a newer DSpace API. NEVER do this in a PRODUCTION scenario. " +
                                        "The resulting database is only useful for migration testing.\n");

                                System.out.print(
                                    "Are you SURE you only want to migrate your database to version " +
                                        migrationVersion + "? [y/n]: ");
                                String choiceString = input.readLine();
                                input.close();

                                if (choiceString.equalsIgnoreCase("y")) {
                                    System.out.println(
                                        "Migrating database ONLY to version " + migrationVersion + " ... " +
                                            "(Check logs for details)");
                                    // Update the database, to the version specified.
                                    updateDatabase(dataSource, connection, migrationVersion, false);
                                } else {
                                    System.out.println("No action performed.");
                                }
                            }
                        } else {
                            System.out.println("Migrating database to latest version... " +
                                                   "(Check dspace logs for details)");
                            updateDatabase(dataSource, connection);
                        }
                        System.out.println("Done.");
                        System.exit(0);
                    } catch (SQLException e) {
                        System.err.println("Migration exception:");
                        e.printStackTrace(System.err);
                        System.exit(1);
                    }
                    break;
                // "repair" = Run Flyway repair script
                case "repair":
                    try (Connection connection = dataSource.getConnection();) {
                        System.out.println("\nDatabase URL: " + connection.getMetaData().getURL());
                        System.out.println(
                            "Attempting to repair any previously failed migrations (or mismatched checksums) via " +
                                "FlywayDB... (Check dspace logs for details)");
                        flyway.repair();
                        System.out.println("Done.");
                        System.exit(0);
                    } catch (SQLException | FlywayException e) {
                        System.err.println("Repair exception:");
                        e.printStackTrace(System.err);
                        System.exit(1);
                    }
                    break;
                // "skip" = Skip a specific Flyway migration (by telling Flyway it succeeded)
                case "skip":
                    try {
                        // "skip" requires a migration version to skip. Only that exact version will be skipped.
                        if (argv.length == 2) {
                            String migrationVersion = argv[1];

                            BufferedReader input = new BufferedReader(
                                new InputStreamReader(System.in, StandardCharsets.UTF_8));
                            System.out.println(
                                "You've specified to SKIP the migration with version='" + migrationVersion + "' " +
                                    "...");
                            System.out.print(
                                "\nWARNING: You should only skip migrations which are no longer required or have " +
                                    "become obsolete. Skipping a REQUIRED migration may result in DSpace failing " +
                                    "to startup or function properly. Are you sure you want to SKIP the " +
                                    "migration with version '" + migrationVersion + "'? [y/n]: ");
                            String choiceString = input.readLine();
                            input.close();

                            if (choiceString.equalsIgnoreCase("y")) {
                                System.out.println(
                                    "Attempting to skip migration with version " + migrationVersion + " " +
                                        "... (Check logs for details)");
                                skipMigration(dataSource, migrationVersion);
                            }
                        } else {
                            System.out.println("The 'skip' command REQUIRES a version to be specified. " +
                                                   "Only that single migration will be skipped. For the list " +
                                                   "of migration versions use the 'info' command.");
                        }
                    } catch (IOException e) {
                        System.err.println("Exception when attempting to skip migration:");
                        e.printStackTrace(System.err);
                        System.exit(1);
                    }
                    break;
                // "validate" = Run Flyway validation to check for database errors/issues
                case "validate":
                    try (Connection connection = dataSource.getConnection();) {
                        System.out.println("\nDatabase URL: " + connection.getMetaData().getURL());
                        System.out
                            .println("Attempting to validate database status (and migration checksums) via " +
                                         "FlywayDB...");
                        flyway.validate();
                        System.out.println("No errors thrown. Validation succeeded. (Check dspace logs for more " +
                                               "details)");
                        System.exit(0);
                    } catch (SQLException | FlywayException e) {
                        System.err.println("Validation exception:");
                        e.printStackTrace(System.err);
                        System.exit(1);
                    }
                    break;
                // "clean" = Run Flyway clean script
                case "clean":
                    // If clean is disabled, return immediately
                    if (flyway.getConfiguration().isCleanDisabled()) {
                        System.out.println(
                            "\nWARNING: 'clean' command is currently disabled, as it is dangerous to run in " +
                                "Production scenarios!");
                        System.out.println(
                            "\nIn order to run a 'clean' you first must enable it in your DSpace config by " +
                                "specifying 'db.cleanDisabled=false'.\n");
                        System.exit(1);
                    }

                    try (Connection connection = dataSource.getConnection()) {
                        String dbType = getDbType(connection);

                        // Not all Postgres user accounts will be able to run a 'clean',
                        // as only 'superuser' accounts can remove the 'pgcrypto' extension.
                        if (dbType.equals(DBMS_POSTGRES)) {
                            // Check if database user has permissions suitable to run a clean
                            if (!PostgresUtils.checkCleanPermissions(connection)) {
                                String username = connection.getMetaData().getUserName();
                                // Exit immediately, providing a descriptive error message
                                System.out.println(
                                    "\nERROR: The database user '" + username + "' does not have sufficient " +
                                        "privileges to run a 'database clean' (via Flyway).");
                                System.out.println(
                                    "\nIn order to run a 'clean', the database user MUST have 'superuser' privileges");
                                System.out.println(
                                    "OR the '" + PostgresUtils.PGCRYPTO + "' extension must be installed in a " +
                                        "separate schema (see documentation).");
                                System.out.println(
                                    "\nOptionally, you could also manually remove the '" + PostgresUtils.PGCRYPTO +
                                        "' extension first (DROP EXTENSION " + PostgresUtils.PGCRYPTO +
                                        " CASCADE;), then rerun the 'clean'");
                                System.exit(1);
                            }
                        }

                        BufferedReader input = new BufferedReader(new InputStreamReader(System.in,
                                                                                        StandardCharsets.UTF_8));

                        System.out.println("\nDatabase URL: " + connection.getMetaData().getURL());
                        System.out
                            .println("\nWARNING: ALL DATA AND TABLES IN YOUR DATABASE WILL BE PERMANENTLY DELETED.\n");
                        System.out.println("There is NO turning back from this action. Backup your DB before " +
                                               "continuing.");
                        if (dbType.equals(DBMS_ORACLE)) {
                            System.out.println("\nORACLE WARNING: your RECYCLEBIN will also be PURGED.\n");
                        } else if (dbType.equals(DBMS_POSTGRES)) {
                            System.out.println(
                                "\nPOSTGRES WARNING: the '" + PostgresUtils.PGCRYPTO + "' extension will be dropped " +
                                "if it is in the same schema as the DSpace database.\n");
                        }
                        System.out.print("Do you want to PERMANENTLY DELETE everything from your database? [y/n]: ");
                        String choiceString = input.readLine();
                        input.close();

                        if (choiceString.equalsIgnoreCase("y")) {
                            System.out.println("Scrubbing database clean... (Check dspace logs for details)");
                            cleanDatabase(flyway, dataSource);
                            System.out.println("Done.");
                            System.exit(0);
                        } else {
                            System.out.println("No action performed.");
                        }
                    } catch (SQLException e) {
                        System.err.println("Clean exception:");
                        e.printStackTrace(System.err);
                        System.exit(1);
                    }
                    break;
                // "update-sequences" = Run DSpace's "update-sequences.sql" script
                case "update-sequences":
                    try (Connection connection = dataSource.getConnection()) {
                        String dbType = getDbType(connection);
                        String sqlfile = "org/dspace/storage/rdbms/sqlmigration/" + dbType +
                                "/update-sequences.sql";
                        InputStream sqlstream = DatabaseUtils.class.getClassLoader().getResourceAsStream(sqlfile);
                        if (sqlstream != null) {
                            String s = IOUtils.toString(sqlstream, StandardCharsets.UTF_8);
                            if (!s.isEmpty()) {
                                System.out.println("Running " + sqlfile);
                                connection.createStatement().execute(s);
                                System.out.println("update-sequences complete");
                            } else {
                                System.err.println(sqlfile + " contains no SQL to execute");
                            }
                        } else {
                            System.err.println(sqlfile + " not found");
                        }
                    }
                    break;
                // default = show help information
                default:
                    System.out.println("\nUsage: database [action]");
                    System.out.println("Valid actions: 'test', 'info', 'migrate', 'repair', 'skip', " +
                        "'validate', 'update-sequences' or 'clean'");
                    System.out.println(
                        " - test             = Performs a test connection to database to " +
                        "validate connection settings");
                    System.out.println(
                        " - info / status    = Describe basic info/status about database, including validating the " +
                        "compatibility of this database");
                    System.out.println(
                        " - migrate          = Migrate the database to the latest version");
                    System.out.println(
                        " - repair           = Attempt to repair any previously failed database " +
                        "migrations or checksum mismatches (via Flyway repair)");
                    System.out.println(
                        " - skip [version]   = Skip a single, pending or ignored migration, " +
                        "ensuring it never runs.");
                    System.out.println(
                        " - validate         = Validate current database's migration status (via Flyway validate), " +
                        "validating all migration checksums.");
                    System.out.println(
                        " - update-sequences = Update database sequences after running AIP ingest.");
                    System.out.println(
                        " - clean            = DESTROY all data and tables in database " +
                        "(WARNING there is no going back!). " +
                        "Requires 'db.cleanDisabled=false' setting in config.");
                    System.out.println("");
                    System.exit(0);
                    break;
            }

        } catch (Exception e) {
            System.err.println("Caught exception:");
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /**
     * Print basic information about the current database to System.out.
     * This is utilized by both the 'test' and 'info' command line options.
     *
     * @param connection current database connection
     * @throws SQLException if database error occurs
     */
    private static void printDBInfo(Connection connection) throws SQLException {
        // Get basic Database info from connection
        DatabaseMetaData meta = connection.getMetaData();
        String dbType = getDbType(connection);
        System.out.println("\nDatabase Type: " + dbType);
        if (dbType.equals(DBMS_ORACLE)) {
            System.out.println("====================================");
            System.out.println("WARNING: Oracle support is deprecated!");
            System.out.println("See https://github.com/DSpace/DSpace/issues/8214");
            System.out.println("=====================================");
        }
        System.out.println("Database URL: " + meta.getURL());
        System.out.println("Database Schema: " + getSchemaName(connection));
        System.out.println("Database Username: " + meta.getUserName());
        System.out.println(
            "Database Software: " + meta.getDatabaseProductName() + " version " + meta.getDatabaseProductVersion());
        System.out.println("Database Driver: " + meta.getDriverName() + " version " + meta.getDriverVersion());

        // For Postgres, report whether pgcrypto is installed
        // (If it isn't, we'll also write out warnings...see below)
        if (dbType.equals(DBMS_POSTGRES)) {
            boolean pgcryptoUpToDate = PostgresUtils.isPgcryptoUpToDate();
            Double pgcryptoVersion = PostgresUtils.getPgcryptoInstalledVersion(connection);
            System.out.println(
                "PostgreSQL '" + PostgresUtils.PGCRYPTO + "' extension installed/up-to-date? " + pgcryptoUpToDate + "" +
                    " " + ((pgcryptoVersion != null) ? "(version=" + pgcryptoVersion + ")" : "(not installed)"));
        }
        // Finally, print out our version of Flyway
        System.out.println("FlywayDB Version: " + VersionPrinter.getVersion());
    }

    /**
     * Print any warnings about current database setup to System.err (if any).
     * This is utilized by both the 'test' and 'info' commandline options.
     *
     * @param connection current database connection
     * @return boolean true if database issues found, false otherwise
     * @throws SQLException if database error occurs
     */
    private static boolean printDBIssues(Connection connection) throws SQLException {
        boolean issueFound = false;

        // Get the DB Type
        String dbType = getDbType(connection);

        // For PostgreSQL databases, we need to check for the 'pgcrypto' extension.
        // If it is NOT properly installed, we'll need to warn the user, as DSpace will be unable to proceed.
        if (dbType.equals(DBMS_POSTGRES)) {
            // Get version of pgcrypto available in this postgres instance
            Double pgcryptoAvailable = PostgresUtils.getPgcryptoAvailableVersion(connection);

            // Generic requirements message
            String requirementsMsg = "\n** DSpace REQUIRES PostgreSQL >= " + PostgresUtils.POSTGRES_VERSION + " AND "
                + PostgresUtils.PGCRYPTO + " extension >= " + PostgresUtils.PGCRYPTO_VERSION + " **\n";

            // Check if installed in PostgreSQL & a supported version
            if (pgcryptoAvailable != null && pgcryptoAvailable.compareTo(PostgresUtils.PGCRYPTO_VERSION) >= 0) {
                // We now know it's available in this Postgres. Let's see if it is installed in this database.
                Double pgcryptoInstalled = PostgresUtils.getPgcryptoInstalledVersion(connection);

                // Check if installed in database, but outdated version
                if (pgcryptoInstalled != null && pgcryptoInstalled.compareTo(PostgresUtils.PGCRYPTO_VERSION) < 0) {
                    System.out.println(
                        "\nWARNING: Required PostgreSQL '" + PostgresUtils.PGCRYPTO + "' extension is OUTDATED " +
                            "(installed version=" + pgcryptoInstalled + ", available version = " + pgcryptoAvailable
                            + ").");
                    System.out.println(requirementsMsg);
                    System.out.println(
                        "To update it, please connect to your DSpace database as a 'superuser' and manually run the " +
                            "following command: ");
                    System.out.println(
                        "\n  ALTER EXTENSION " + PostgresUtils.PGCRYPTO + " UPDATE TO '" + pgcryptoAvailable + "';\n");
                    issueFound = true;
                } else if (pgcryptoInstalled == null) {
                    // If it's not installed in database

                    System.out.println(
                        "\nWARNING: Required PostgreSQL '" + PostgresUtils.PGCRYPTO + "' extension is NOT INSTALLED " +
                            "on this database.");
                    System.out.println(requirementsMsg);
                    System.out.println(
                        "To install it, please connect to your DSpace database as a 'superuser' and manually run the " +
                            "following command: ");
                    System.out.println("\n  CREATE EXTENSION " + PostgresUtils.PGCRYPTO + ";\n");
                    issueFound = true;
                }
            } else if (pgcryptoAvailable != null && pgcryptoAvailable.compareTo(PostgresUtils.PGCRYPTO_VERSION) < 0) {
                // If installed in Postgres, but an unsupported version

                System.out.println(
                    "\nWARNING: UNSUPPORTED version of PostgreSQL '" + PostgresUtils.PGCRYPTO + "' extension found " +
                        "(version=" + pgcryptoAvailable + ").");
                System.out.println(requirementsMsg);
                System.out.println(
                    "Make sure you are running a supported version of PostgreSQL, and then install " + PostgresUtils
                        .PGCRYPTO + " version >= " + PostgresUtils.PGCRYPTO_VERSION);
                System.out.println(
                    "The '" + PostgresUtils.PGCRYPTO + "' extension is often provided in the 'postgresql-contrib' " +
                        "package for your operating system.");
                issueFound = true;
            } else if (pgcryptoAvailable == null) {
                // If it's not installed in Postgres

                System.out.println(
                    "\nWARNING: PostgreSQL '" + PostgresUtils.PGCRYPTO + "' extension is NOT AVAILABLE. Please " +
                        "install it into this PostgreSQL instance.");
                System.out.println(requirementsMsg);
                System.out.println(
                    "The '" + PostgresUtils.PGCRYPTO + "' extension is often provided in the 'postgresql-contrib' " +
                        "package for your operating system.");
                System.out.println(
                    "Once the extension is installed globally, please connect to your DSpace database as a " +
                        "'superuser' and manually run the following command: ");
                System.out.println("\n  CREATE EXTENSION " + PostgresUtils.PGCRYPTO + ";\n");
                issueFound = true;
            }
        }
        return issueFound;
    }

    /**
     * Setup/Initialize the Flyway Configuration to run against our DSpace database
     * and point at our migration scripts.
     *
     * @param datasource DataSource object initialized by DatabaseManager
     * @return initialized FluentConfiguration (Flyway configuration object)
     */
    private synchronized static FluentConfiguration setupFlyway(DataSource datasource) {
        ConfigurationService config = DSpaceServicesFactory.getInstance().getConfigurationService();

        // Initialize Flyway Configuration (http://flywaydb.org/), used to perform DB migrations
        FluentConfiguration flywayConfiguration = Flyway.configure();

        try (Connection connection = datasource.getConnection()) {
            flywayConfiguration.dataSource(datasource);
            flywayConfiguration.encoding("UTF-8");

            // Default cleanDisabled to "true" (which disallows the ability to run 'database clean')
            flywayConfiguration.cleanDisabled(config.getBooleanProperty("db.cleanDisabled", true));

            // Migration scripts are based on DBMS Keyword (see full path below)
            String dbType = getDbType(connection);
            connection.close();

            if (dbType.equals(DBMS_ORACLE)) {
                log.warn("ORACLE SUPPORT IS DEPRECATED! See https://github.com/DSpace/DSpace/issues/8214");
            }

            // Determine location(s) where Flyway will load all DB migrations
            ArrayList<String> scriptLocations = new ArrayList<>();

            // First, add location for custom SQL migrations, if exists (based on DB Type)
            // e.g. [dspace.dir]/etc/[dbtype]/
            // (We skip this for H2 as it's only used for unit testing)
            String etcDirPath = config.getProperty("dspace.dir") + "/etc/" + dbType;
            File etcDir = new File(etcDirPath);
            if (etcDir.exists() && !dbType.equals(DBMS_H2)) {
                scriptLocations.add("filesystem:" + etcDirPath);
            }

            // Also add the Java package where Flyway will load SQL migrations from (based on DB Type)
            scriptLocations.add("classpath:org/dspace/storage/rdbms/sqlmigration/" + dbType);

            // Also add the Java package where Flyway will load Java migrations from
            // NOTE: this also loads migrations from any sub-package
            scriptLocations.add("classpath:org/dspace/storage/rdbms/migration");

            //Add all potential workflow migration paths
            List<String> workflowFlywayMigrationLocations = WorkflowServiceFactory.getInstance()
                                                                                  .getWorkflowService()
                                                                                  .getFlywayMigrationLocations();
            scriptLocations.addAll(workflowFlywayMigrationLocations);

            // Now tell Flyway which locations to load SQL / Java migrations from
            log.info("Loading Flyway DB migrations from: " + StringUtils.join(scriptLocations, ", "));
            flywayConfiguration.locations(scriptLocations.toArray(new String[scriptLocations.size()]));

            // Tell Flyway NOT to throw a validation error if it finds older "Ignored" migrations.
            // For DSpace, we sometimes have to insert "old" migrations in after a major release
            // if further development/bug fixes are needed in older versions. So, "Ignored" migrations are
            // nothing to worry about...you can always trigger them to run using "database migrate ignored" from CLI
            flywayConfiguration.ignoreIgnoredMigrations(true);

            // Set Flyway callbacks (i.e. classes which are called post-DB migration and similar)
            List<Callback> flywayCallbacks = DSpaceServicesFactory.getInstance().getServiceManager()
                                                                        .getServicesByType(Callback.class);

            flywayConfiguration.callbacks(flywayCallbacks.toArray(new Callback[flywayCallbacks.size()]));

            // Tell Flyway to use the "schema_version" table in the database to manage its migration history
            // As of Flyway v5, the default table is named "flyway_schema_history"
            // We are using the older name ("schema_version") for backwards compatibility.
            flywayConfiguration.table(FLYWAY_TABLE);
        } catch (SQLException e) {
            log.error("Unable to setup Flyway against DSpace database", e);
        }

        return flywayConfiguration;
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
     * @throws SQLException if database error
     *                      If database cannot be upgraded.
     */
    public static synchronized void updateDatabase()
        throws SQLException {
        // Get our configured dataSource
        DataSource dataSource = getDataSource();
        if (null == dataSource) {
            throw new SQLException("The DataSource is a null reference -- cannot continue.");
        }

        try (Connection connection = dataSource.getConnection()) {
            // Upgrade database to the latest version of our schema
            updateDatabase(dataSource, connection);
        }
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
     * @param datasource DataSource object (retrieved from DatabaseManager())
     * @param connection Database connection
     * @throws SQLException if database error
     *                      If database cannot be upgraded.
     */
    protected static synchronized void updateDatabase(DataSource datasource, Connection connection)
        throws SQLException {
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
     * @param datasource    DataSource object (retrieved from DatabaseManager())
     * @param connection    Database connection
     * @param targetVersion If specified, only migrate the database to a particular *version* of DSpace. This is
     *                      just useful for testing migrations, and should NOT be used in Production.
     *                      If null, the database is migrated to the latest version.
     * @param outOfOrder    If true, Flyway will run any lower version migrations that were previously "ignored".
     *                      If false, Flyway will only run new migrations with a higher version number.
     * @throws SQLException if database error
     *                      If database cannot be upgraded.
     */
    protected static synchronized void updateDatabase(DataSource datasource,
                                                      Connection connection, String targetVersion, boolean outOfOrder)
        throws SQLException {
        updateDatabase(datasource, connection, targetVersion, outOfOrder, false);
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
     * @param datasource    DataSource object (retrieved from DatabaseManager())
     * @param connection    Database connection
     * @param targetVersion If specified, only migrate the database to a particular *version* of DSpace. This is
     *                      just useful for testing migrations, and should NOT be used in Production.
     *                      If null, the database is migrated to the latest version.
     * @param outOfOrder    If true, Flyway will run any lower version migrations that were previously "ignored".
     *                      If false, Flyway will only run new migrations with a higher version number.
     * @param forceMigrate  If true, always run a Flyway migration, even if no "Pending" migrations exist. This can be
     *                      used to trigger Flyway Callbacks manually.
     *                      If false, only run migration if pending migrations exist, otherwise do nothing.
     * @throws SQLException if database error
     *                      If database cannot be upgraded.
     */
    protected static synchronized void updateDatabase(DataSource datasource, Connection connection,
                                                      String targetVersion, boolean outOfOrder, boolean forceMigrate)
        throws SQLException {
        if (null == datasource) {
            throw new SQLException("The datasource is a null reference -- cannot continue.");
        }

        // Whether to reindex all content in Solr after successfully updating database
        boolean reindexAfterUpdate = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                          .getBooleanProperty("discovery.autoReindex", true);

        try {
            // Setup Flyway API against our database
            FluentConfiguration flywayConfiguration = setupFlyway(datasource);

            // Set whether Flyway will run migrations "out of order". By default, this is false,
            // and Flyway ONLY runs migrations that have a higher version number.
            flywayConfiguration.outOfOrder(outOfOrder);

            // If a target version was specified, tell Flyway to ONLY migrate to that version
            // (i.e. all later migrations are left as "pending"). By default we always migrate to latest version.
            // This mode is only useful for testing migrations & should NEVER be used in Production.
            if (!StringUtils.isBlank(targetVersion)) {
                flywayConfiguration.target(targetVersion);
                // Disable all callbacks. Most callbacks use the Context object which triggers a full database update,
                // bypassing this target version.
                flywayConfiguration.callbacks(new Callback[]{});
                // Also disable reindex after update for this migration mode (as reindex also uses Context object)
                reindexAfterUpdate = false;
            }

            // Initialized Flyway object (will be created by flywayConfiguration.load() below)
            Flyway flyway;

            // Does the necessary Flyway table ("schema_version") exist in this database?
            // If not, then this is the first time Flyway has run, and we need to initialize
            // NOTE: search is case sensitive, as flyway table name is ALWAYS lowercase,
            // See: http://flywaydb.org/documentation/faq.html#case-sensitive
            if (!tableExists(connection, flywayConfiguration.getTable(), true)) {
                // Try to determine our DSpace database version, so we know what to tell Flyway to do
                String dspaceVersion = determineDBVersion(connection);

                // If this is NOT a fresh install (i.e. dspaceVersion is not null)
                if (dspaceVersion != null) {
                    // Pass our determined DSpace version to Flyway to initialize database table
                    flywayConfiguration.baselineVersion(dspaceVersion);
                    flywayConfiguration.baselineDescription(
                        "Initializing from DSpace " + dspaceVersion + " database schema");
                }

                // Initialize Flyway in DB with baseline version (either dspaceVersion or default of 1)
                flyway = flywayConfiguration.load();
                flyway.baseline();
            } else {
                // Otherwise, this database already ran Flyway before
                // So, just load our Flyway configuration, initializing latest Flyway.
                flyway = flywayConfiguration.load();

                // Now, check our Flyway database table to see if it needs upgrading
                // *before* any other Flyway commands can be run.
                FlywayUpgradeUtils.upgradeFlywayTable(flyway, connection);
            }

            // Determine pending Database migrations
            MigrationInfo[] pending = flyway.info().pending();

            // As long as there are pending migrations, log them and run migrate()
            if (pending != null && pending.length > 0) {
                log.info("Pending DSpace database schema migrations:");
                for (MigrationInfo info : pending) {
                    log.info("\t" + info.getVersion() + " " + info.getDescription() + " " + info.getType() + " " + info
                        .getState());
                }

                // Run all pending Flyway migrations to ensure the DSpace Database is up to date
                flyway.migrate();

                // Flag that Discovery will need reindexing, since database was updated
                setReindexDiscovery(reindexAfterUpdate);
            } else if (forceMigrate) {
                log.info("DSpace database schema is up to date, but 'force' was specified. " +
                        "Running migrate command to trigger callbacks.");
                flyway.migrate();
            } else {
                log.info("DSpace database schema is up to date");
            }
        } catch (FlywayException fe) {
            // If any FlywayException (Runtime) is thrown, change it to a SQLException
            throw new SQLException("Flyway migration error occurred", fe);
        }
    }

    /**
     * Skips the given migration by marking it as "successful" in the Flyway table. This ensures
     * the given migration will never be run again.
     * <P>
     * WARNING: Skipping a required migration can result in unexpected errors. Make sure the migration is
     * not required (or obsolete) before skipping it.
     * @param dataSource current DataSource
     * @param skipVersion version of migration to skip
     * @throws SQLException if error occurs
     */
    private static synchronized void skipMigration(DataSource dataSource,
                                                   String skipVersion) throws SQLException {
        if (null == dataSource) {
            throw new SQLException("The datasource is a null reference -- cannot continue.");
        }

        try (Connection connection = dataSource.getConnection()) {
            // Setup Flyway API against our database
            FluentConfiguration flywayConfiguration = setupFlyway(dataSource);

            // In order to allow for skipping "Ignored" migrations, we MUST set "outOfOrder=true".
            // (Otherwise Ignored migrations never appear in the pending list)
            flywayConfiguration.outOfOrder(true);

            // Initialized Flyway object based on this configuration
            Flyway flyway = flywayConfiguration.load();

            // Find the migration we are skipping in the list of pending migrations
            boolean foundMigration = false;
            for (MigrationInfo migration : flyway.info().pending()) {
                // If this migration matches our "skipVersion"
                if (migration.getVersion().equals(MigrationVersion.fromVersion(skipVersion))) {
                    foundMigration = true;
                    System.out.println("Found migration matching version='" + skipVersion + "'. " +
                                           "Changing state to 'Success' in order to skip it.");

                    PreparedStatement statement = null;
                    try {
                        // Create SQL Insert which will log this migration as having already been run.
                        String INSERT_SQL = "INSERT INTO " + FLYWAY_TABLE  + " " +
                            "(" +
                              "installed_rank, version, description, type, script, " +
                              "checksum, installed_by, execution_time, success" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        statement = connection.prepareStatement(INSERT_SQL);
                        // installed_rank
                        statement.setInt(1, getNextFlywayInstalledRank(flyway));
                        // version
                        statement.setString(2, migration.getVersion().getVersion());
                        // description
                        statement.setString(3, migration.getDescription());
                        // type
                        statement.setString(4, migration.getType().toString());
                        // script
                        statement.setString(5, migration.getScript());
                        // checksum
                        statement.setInt(6, migration.getChecksum());
                        // installed_by
                        statement.setString(7, getDBUserName(connection));
                        // execution_time is set to zero as we didn't really execute it
                        statement.setInt(8, 0);
                        // success=true tells Flyway this migration no longer needs to be run.
                        statement.setBoolean(9, true);

                        // Run the INSERT
                        statement.executeUpdate();
                    } finally {
                        if (statement != null && !statement.isClosed()) {
                            statement.close();
                        }
                    }
                }
            }
            if (!foundMigration) {
                System.err.println("Could not find migration to skip! " +
                                       "No 'Pending' or 'Ignored' migrations match version='" + skipVersion + "'");
            }
        } catch (FlywayException fe) {
            // If any FlywayException (Runtime) is thrown, change it to a SQLException
            throw new SQLException("Flyway error occurred", fe);
        }
    }

    /**
     * Clean the existing database, permanently removing all data and tables
     * <P>
     * FlywayDB (http://flywaydb.org/) is used to clean the database
     *
     * @param flyway     Initialized Flyway object
     * @param dataSource Initialized DataSource
     * @throws SQLException if database error
     *                      If database cannot be cleaned.
     */
    private static synchronized void cleanDatabase(Flyway flyway, DataSource dataSource)
        throws SQLException {
        try {
            // First, run Flyway's clean command on database.
            // For MOST database types, this takes care of everything
            flyway.clean();

            try (Connection connection = dataSource.getConnection()) {
                // Get info about which database type we are using
                String dbType = getDbType(connection);

                // If this is Oracle, the only way to entirely clean the database
                // is to also purge the "Recyclebin". See:
                // http://docs.oracle.com/cd/B19306_01/server.102/b14200/statements_9018.htm
                if (dbType.equals(DBMS_ORACLE)) {
                    PreparedStatement statement = null;
                    try {
                        statement = connection.prepareStatement("PURGE RECYCLEBIN");
                        statement.executeQuery();
                    } finally {
                        if (statement != null && !statement.isClosed()) {
                            statement.close();
                        }
                    }
                }
            }
        } catch (FlywayException fe) {
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
     * @param connection Current Database Connection
     * @return DSpace version as a String (e.g. "4.0"), or null if database is empty
     * @throws SQLException if DB status cannot be determined
     */
    private static String determineDBVersion(Connection connection)
        throws SQLException {
        // First, is this a "fresh_install"?  Check for an "item" table.
        if (!tableExists(connection, "Item")) {
            // Item table doesn't exist. This database must be a fresh install
            return null;
        }

        // We will now check prior versions in reverse chronological order, looking
        // for specific tables or columns that were newly created in each version.

        // Is this pre-DSpace 5.0 (with Metadata 4 All changes)? Look for the "resource_id" column in the
        // "metadatavalue" table
        if (tableColumnExists(connection, "metadatavalue", "resource_id")) {
            return "5.0.2014.09.26"; // This version matches the version in the SQL migration for this feature
        }

        // Is this pre-DSpace 5.0 (with Helpdesk plugin)? Look for the "request_message" column in the "requestitem"
        // table
        if (tableColumnExists(connection, "requestitem", "request_message")) {
            return "5.0.2014.08.08"; // This version matches the version in the SQL migration for this feature
        }

        // Is this DSpace 4.x? Look for the "Webapp" table created in that version.
        if (tableExists(connection, "Webapp")) {
            return "4.0";
        }

        // Is this DSpace 3.x? Look for the "versionitem" table created in that version.
        if (tableExists(connection, "versionitem")) {
            return "3.0";
        }

        // Is this DSpace 1.8.x? Look for the "bitstream_order" column in the "bundle2bitstream" table
        if (tableColumnExists(connection, "bundle2bitstream", "bitstream_order")) {
            return "1.8";
        }

        // Is this DSpace 1.7.x? Look for the "dctyperegistry_seq" to NOT exist (it was deleted in 1.7)
        // NOTE: DSPACE 1.7.x only differs from 1.6 in a deleted sequence.
        if (!sequenceExists(connection, "dctyperegistry_seq")) {
            return "1.7";
        }

        // Is this DSpace 1.6.x? Look for the "harvested_collection" table created in that version.
        if (tableExists(connection, "harvested_collection")) {
            return "1.6";
        }

        // Is this DSpace 1.5.x? Look for the "collection_item_count" table created in that version.
        if (tableExists(connection, "collection_item_count")) {
            return "1.5";
        }

        // Is this DSpace 1.4.x? Look for the "Group2Group" table created in that version.
        if (tableExists(connection, "Group2Group")) {
            return "1.4";
        }

        // Is this DSpace 1.3.x? Look for the "epersongroup2workspaceitem" table created in that version.
        if (tableExists(connection, "epersongroup2workspaceitem")) {
            return "1.3";
        }

        // Is this DSpace 1.2.x? Look for the "Community2Community" table created in that version.
        if (tableExists(connection, "Community2Community")) {
            return "1.2";
        }

        // Is this DSpace 1.1.x? Look for the "Community" table created in that version.
        if (tableExists(connection, "Community")) {
            return "1.1";
        }

        // IF we get here, something went wrong! This database is missing a LOT of DSpace tables
        throw new SQLException("CANNOT AUTOUPGRADE DSPACE DATABASE, AS IT DOES NOT LOOK TO BE A VALID DSPACE DATABASE" +
                                   ".");
    }

    /**
     * Determine if a particular database table exists in our database
     *
     * @param connection Current Database Connection
     * @param tableName  The name of the table
     * @return true if table of that name exists, false otherwise
     */
    public static boolean tableExists(Connection connection, String tableName) {
        //By default, do a case-insensitive search
        return tableExists(connection, tableName, false);
    }

    /**
     * Determine if a particular database table exists in our database
     *
     * @param connection    Current Database Connection
     * @param tableName     The name of the table
     * @param caseSensitive When "true", the case of the tableName will not be changed.
     *                      When "false, the name may be uppercased or lowercased based on DB type.
     * @return true if table of that name exists, false otherwise
     */
    public static boolean tableExists(Connection connection, String tableName, boolean caseSensitive) {
        boolean exists = false;
        ResultSet results = null;

        try {
            // Get the name of the Schema that the DSpace Database is using
            // (That way we can search the right schema)
            String schema = getSchemaName(connection);

            // Get information about our database.
            DatabaseMetaData meta = connection.getMetaData();

            // If this is not a case sensitive search
            if (!caseSensitive) {
                // Canonicalize everything to the proper case based on DB type
                schema = canonicalize(connection, schema);
                tableName = canonicalize(connection, tableName);
            }

            // Search for a table of the given name in our current schema
            results = meta.getTables(null, schema, tableName, null);
            if (results != null && results.next()) {
                exists = true;
            }
        } catch (SQLException e) {
            log.error("Error attempting to determine if table " + tableName + " exists", e);
        } finally {
            try {
                // ensure the ResultSet gets closed
                if (results != null && !results.isClosed()) {
                    results.close();
                }
            } catch (SQLException e) {
                // ignore it
            }
        }

        return exists;
    }

    /**
     * Determine if a particular database column exists in our database
     *
     * @param connection Current Database Connection
     * @param tableName  The name of the table
     * @param columnName The name of the column in the table
     * @return true if column of that name exists, false otherwise
     */
    public static boolean tableColumnExists(Connection connection, String tableName, String columnName) {
        boolean exists = false;
        ResultSet results = null;

        try {
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
            if (results != null && results.next()) {
                exists = true;
            }
        } catch (SQLException e) {
            log.error("Error attempting to determine if column " + columnName + " exists", e);
        } finally {
            try {
                // ensure the ResultSet gets closed
                if (results != null && !results.isClosed()) {
                    results.close();
                }
            } catch (SQLException e) {
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
    public static boolean sequenceExists(Connection connection, String sequenceName) {
        boolean exists = false;
        PreparedStatement statement = null;
        ResultSet results = null;
        // Whether or not to filter query based on schema (this is DB Type specific)
        boolean schemaFilter = false;

        try {
            // Get the name of the Schema that the DSpace Database is using
            // (That way we can search the right schema)
            String schema = getSchemaName(connection);

            // Canonicalize everything to the proper case based on DB type
            schema = canonicalize(connection, schema);
            sequenceName = canonicalize(connection, sequenceName);

            // Different database types store sequence information in different tables
            String dbtype = getDbType(connection);
            String sequenceSQL = null;
            switch (dbtype) {
                case DBMS_POSTGRES:
                    // Default schema in PostgreSQL is "public"
                    if (schema == null) {
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
                case DBMS_ORACLE:
                    // Oracle specific query for a sequence owned by our current DSpace user
                    // NOTE: No need to filter by schema for Oracle, as Schema = User
                    sequenceSQL = "SELECT COUNT(1) FROM user_sequences WHERE sequence_name=?";
                    break;
                case DBMS_H2:
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
            if (sequenceSQL != null) {
                // Run the query, passing it our parameters
                statement = connection.prepareStatement(sequenceSQL);
                statement.setString(1, sequenceName);
                if (schemaFilter) {
                    statement.setString(2, schema);
                }
                results = statement.executeQuery();

                // If results are non-zero, then this sequence exists!
                if (results != null && results.next() && results.getInt(1) > 0) {
                    exists = true;
                }
            }
        } catch (SQLException e) {
            log.error("Error attempting to determine if sequence " + sequenceName + " exists", e);
        } finally {
            try {
                // Ensure statement gets closed
                if (statement != null && !statement.isClosed()) {
                    statement.close();
                }
                // Ensure ResultSet gets closed
                if (results != null && !results.isClosed()) {
                    results.close();
                }
            } catch (SQLException e) {
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
     * @param connection   Current Database Connection
     * @param sqlToExecute The actual SQL to execute as a String
     * @throws SQLException if database error
     *                      If a database error occurs
     */
    public static void executeSql(Connection connection, String sqlToExecute) throws SQLException {
        try {
            // Run the SQL using Spring JDBC as documented in Flyway's guide for using Spring JDBC directly
            // https://flywaydb.org/documentation/migrations#spring
            new JdbcTemplate(new SingleConnectionDataSource(connection, true))
                .execute(sqlToExecute);
        } catch (DataAccessException dae) {
            // If any Exception is thrown, change it to a SQLException
            throw new SQLException("Flyway executeSql() error occurred", dae);
        }
    }

    /**
     * Get the Database Schema Name in use by this Connection, so that it can
     * be used to limit queries in other methods (e.g. tableExists()).
     *
     * @param connection Current Database Connection
     * @return Schema name as a string, or "null" if cannot be determined or unspecified
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public static String getSchemaName(Connection connection)
        throws SQLException {
        String schema = null;

        // Try to get the schema from the DB connection itself.
        // As long as the Database driver supports JDBC4.1, there should be a getSchema() method
        // If this method is unimplemented or doesn't exist, it will throw an exception (likely an AbstractMethodError)
        try {
            schema = connection.getSchema();
        } catch (Exception | AbstractMethodError e) {
            // ignore
        }

        // If we don't know our schema, let's try the schema in the DSpace configuration
        if (StringUtils.isBlank(schema)) {
            schema = canonicalize(connection, DSpaceServicesFactory.getInstance().getConfigurationService()
                                                                   .getProperty("db.schema"));
        }

        // Still blank? Ok, we'll find a "sane" default based on the DB type
        if (StringUtils.isBlank(schema)) {
            String dbType = getDbType(connection);

            if (dbType.equals(DBMS_POSTGRES)) {
                // For PostgreSQL, the default schema is named "public"
                // See: http://www.postgresql.org/docs/9.0/static/ddl-schemas.html
                schema = "public";
            } else if (dbType.equals(DBMS_ORACLE)) {
                // For Oracle, default schema is actually the user account
                // See: http://stackoverflow.com/a/13341390
                DatabaseMetaData meta = connection.getMetaData();
                schema = meta.getUserName();
            } else {
                // For H2 (in memory), there is no such thing as a schema
                schema = null;
            }
        }

        return schema;
    }

    /**
     * Get the Database User Name in use by this Connection.
     *
     * @param connection Current Database Connection
     * @return User name as a string, or "null" if cannot be determined or unspecified
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public static String getDBUserName(Connection connection)
        throws SQLException {
        String username = null;

        // Try to get the schema from the DB connection itself.
        // As long as the Database driver supports JDBC4.1, there should be a getSchema() method
        // If this method is unimplemented or doesn't exist, it will throw an exception (likely an AbstractMethodError)
        try {
            username = connection.getMetaData().getUserName();
        } catch (Exception | AbstractMethodError e) {
            // ignore
        }

        // If we don't know our schema, let's try the schema in the DSpace configuration
        if (StringUtils.isBlank(username)) {
            username = canonicalize(connection, DSpaceServicesFactory.getInstance().getConfigurationService()
                                                                     .getProperty("db.username"));
        }
        return username;
    }

    /**
     * Return the canonical name for a database identifier based on whether this
     * database defaults to storing identifiers in uppercase or lowercase.
     *
     * @param connection   Current Database Connection
     * @param dbIdentifier Identifier to canonicalize (may be a table name, column name, etc)
     * @return The canonical name of the identifier.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public static String canonicalize(Connection connection, String dbIdentifier)
        throws SQLException {
        // Avoid any null pointers
        if (dbIdentifier == null) {
            return null;
        }

        DatabaseMetaData meta = connection.getMetaData();

        // Check how this database stores its identifiers, etc.
        // i.e. lowercase vs uppercase (by default we assume mixed case)
        if (meta.storesLowerCaseIdentifiers()) {
            return StringUtils.lowerCase(dbIdentifier);

        } else if (meta.storesUpperCaseIdentifiers()) {
            return StringUtils.upperCase(dbIdentifier);
        } else {
            // Otherwise DB doesn't care about case
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
     *
     * @param reindex true or false
     */
    public static synchronized void setReindexDiscovery(boolean reindex) {
        File reindexFlag = new File(reindexDiscoveryFilePath);

        // If we need to flag Discovery to reindex, we'll create a temporary file to do so.
        if (reindex) {
            try {
                //If our flag file doesn't exist, create it as writeable to all
                if (!reindexFlag.exists()) {
                    reindexFlag.createNewFile();
                    reindexFlag.setWritable(true, false);
                }
            } catch (IOException io) {
                log.error("Unable to create Discovery reindex flag file " + reindexFlag
                    .getAbsolutePath() + ". You may need to reindex manually.", io);
            }
        } else {
            // Otherwise, Discovery doesn't need to reindex. Delete the temporary file if it exists

            //If our flag file exists, delete it
            if (reindexFlag.exists()) {
                boolean deleted = reindexFlag.delete();
                if (!deleted) {
                    log.error("Unable to delete Discovery reindex flag file " + reindexFlag
                        .getAbsolutePath() + ". You may need to delete it manually.");
                }
            }
        }
    }

    /**
     * Whether or not reindexing is required in Discovery.
     * <P>
     * Because the DB migration may be initialized by commandline or any one of
     * the many DSpace webapps, this checks for the existence of a temporary
     * file, and the discovery.autoReindex setting to know when
     * Discovery/Solr needs reindexing.
     * @return whether reindexing should happen.
     */
    public static synchronized boolean getReindexDiscovery() {
        boolean autoReindex = DSpaceServicesFactory.getInstance()
            .getConfigurationService()
            .getBooleanProperty("discovery.autoReindex", true);
        return (autoReindex && new File(reindexDiscoveryFilePath).exists());
    }

    /**
     * Method to check whether we need to reindex in Discovery (i.e. Solr). If
     * reindexing is necessary, it is performed. If not, nothing happens.
     * <P>
     * This method is called by Discovery whenever it initializes a connection
     * to Solr.
     *
     * @param indexer The actual indexer to use to reindex Discovery, if needed
     * @see org.dspace.discovery.SolrServiceImpl
     */
    public static synchronized void checkReindexDiscovery(IndexingService indexer) {
        // We only do something if the reindexDiscovery flag has been triggered
        if (getReindexDiscovery()) {
            // Kick off a custom thread to perform the reindexing in Discovery
            // (See ReindexerThread nested class below)
            ReindexerThread go = new ReindexerThread(indexer);
            go.start();
        }
    }

    /**
     * Internal class to actually perform re-indexing in a separate thread.
     * (See checkReindexDiscovery() method).
     */
    private static class ReindexerThread extends Thread {
        private final IndexingService indexer;

        /**
         * Constructor. Pass it an existing IndexingService
         *
         * @param is
         */
        ReindexerThread(IndexingService is) {
            this.indexer = is;
        }

        /**
         * Actually perform Reindexing in Discovery/Solr.
         * This is synchronized so that only one thread can get in at a time.
         */
        @Override
        public void run() {
            synchronized (this.indexer) {
                // Make sure reindexDiscovery flag is still true
                // If multiple threads get here we only want to reindex ONCE
                if (DatabaseUtils.getReindexDiscovery()) {
                    Context context = null;
                    try {
                        context = new Context();
                        context.turnOffAuthorisationSystem();
                        log.info(
                            "Post database migration, reindexing all content in Discovery search and browse engine");

                        // Reindex Discovery completely
                        // Force clean all content
                        this.indexer.deleteIndex();
                        // Recreate the entire index (overwriting existing one)
                        this.indexer.createIndex(context);
                        // Rebuild spell checker (which is based on index)
                        this.indexer.buildSpellCheck();

                        log.info("Reindexing is complete");
                    } catch (SearchServiceException sse) {
                        log.warn(
                            "Unable to reindex content in Discovery search and browse engine. You may need to reindex" +
                                " manually.",
                            sse);
                    } catch (SQLException | IOException e) {
                        log.error("Error attempting to reindex all contents for search/browse", e);
                    } finally {
                        // Reset our indexing flag. Indexing is done or it threw an error,
                        // Either way, we shouldn't try again.
                        DatabaseUtils.setReindexDiscovery(false);

                        // Clean up our context, if it still exists
                        if (context != null && context.isValid()) {
                            context.abort();
                        }
                    }
                }
            }
        }
    }

    /**
     * Determine the type of Database, based on the DB connection.
     *
     * @param connection current DB Connection
     * @return a DB keyword/type (see DatabaseUtils.DBMS_* constants)
     * @throws SQLException if database error
     */
    public static String getDbType(Connection connection)
        throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        String prodName = meta.getDatabaseProductName();
        String dbms_lc = prodName.toLowerCase(Locale.ROOT);
        if (dbms_lc.contains("postgresql")) {
            return DBMS_POSTGRES;
        } else if (dbms_lc.contains("oracle")) {
            return DBMS_ORACLE;
        } else if (dbms_lc.contains("h2")) {
            // Used for unit testing only
            return DBMS_H2;
        } else {
            return dbms_lc;
        }
    }

    /**
     * Get a reference to the configured DataSource (which can be used to
     * initialize the database using Flyway).
     * The DataSource is configured via our ServiceManager (i.e. via Spring).
     * <P>
     * This is NOT public, as we discourage direct connections to the database
     * which bypass Hibernate. Only Flyway should be allowed a direct connection.
     *
     * @return DataSource
     */
    protected static DataSource getDataSource() {
        DataSource dataSource = DSpaceServicesFactory.getInstance()
                                                     .getServiceManager()
                                                     .getServiceByName("dataSource", DataSource.class);
        if (null == dataSource) {
            log.error("The service manager could not find the DataSource.");
        }
        return dataSource;
    }

    /**
     * Returns the current Flyway schema_version being used by the given database.
     * (i.e. the version of the highest numbered migration that this database has run)
     *
     * @param connection current DB Connection
     * @return version as string
     * @throws SQLException if database error occurs
     */
    public static String getCurrentFlywayState(Connection connection) throws SQLException {
        PreparedStatement statement = connection
            .prepareStatement("SELECT \"version\" FROM \"" + FLYWAY_TABLE + "\" ORDER BY \"version\" desc");
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();
        return resultSet.getString("version");
    }

    /**
     * Return the DSpace version that this Flyway-enabled database reports to be compatible with.
     * The version is retrieved from Flyway, and parsed into a Double to represent an actual
     * DSpace version number (e.g. 5.0, 6.0, etc)
     *
     * @param connection current DB Connection
     * @return reported DSpace version as a Double
     * @throws SQLException if database error occurs
     */
    public static Double getCurrentFlywayDSpaceState(Connection connection) throws SQLException {
        String flywayState = getCurrentFlywayState(connection);
        Matcher matcher = Pattern.compile("^([0-9]*\\.[0-9]*)(\\.)?.*").matcher(flywayState);
        if (matcher.matches()) {
            return Double.parseDouble(matcher.group(1));
        }
        return null;
    }

    /**
     * Determine next valid "installed_rank" value from Flyway, based on the "installed_rank" of the
     * last applied migration.
     * @param flyway currently loaded Flyway
     * @return next installed rank value
     */
    private static int getNextFlywayInstalledRank(Flyway flyway) throws FlywayException {
        // Load all applied migrations
        MigrationInfo[] appliedMigrations = flyway.info().applied();
        // If no applied migrations, throw an error.
        // This should never happen, but this would mean Flyway is not installed or initialized
        if (ArrayUtils.isEmpty(appliedMigrations)) {
            throw new FlywayException("Cannot determine next 'installed_rank' as no applied migrations exist");
        }
        // Find the last migration in the list, and increment its "installed_rank" by one.
        return appliedMigrations[appliedMigrations.length - 1].getInstalledRank() + 1;
    }
}
