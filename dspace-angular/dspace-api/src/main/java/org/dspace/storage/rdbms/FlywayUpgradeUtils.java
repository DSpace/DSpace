/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import static org.dspace.storage.rdbms.DatabaseUtils.FLYWAY_TABLE;
import static org.dspace.storage.rdbms.DatabaseUtils.executeSql;
import static org.dspace.storage.rdbms.DatabaseUtils.getCurrentFlywayState;
import static org.dspace.storage.rdbms.DatabaseUtils.getDbType;
import static org.dspace.storage.rdbms.DatabaseUtils.getSchemaName;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.Logger;
import org.dspace.storage.rdbms.migration.MigrationUtils;
import org.flywaydb.core.Flyway;

/**
 * Utility class used to detect issues with the Flyway migration history table and attempt to correct/fix them.
 * These issues can occur when attempting to upgrade your database across multiple versions/releases of Flyway.
 * <p>
 * As documented in this issue ticket, Flyway does not normally support skipping over any
 * major release (for example going from v3 to v5 is unsuppored): https://github.com/flyway/flyway/issues/2126
 * <p>
 * This class allows us to do a migration (where needed) through multiple major versions of Flyway.
 *
 * @author Tim Donohue
 */
public class FlywayUpgradeUtils {
    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(FlywayUpgradeUtils.class);

    // Resource path of all Flyway upgrade scripts
    private static final String UPGRADE_SCRIPT_PATH = "org/dspace/storage/rdbms/flywayupgrade/";


    /**
     * Default constructor
     */
    private FlywayUpgradeUtils() { }

    /**
     * Ensures the Flyway migration history table (FLYWAY_TABLE) is upgraded to the latest version of Flyway safely.
     * <P>
     * Unfortunately, Flyway does not always support skipping major versions (e.g. upgrading directly from Flyway
     * v3.x to 5.x is not possible, see https://github.com/flyway/flyway/issues/2126).
     * <P>
     * While sometimes it's possible to do so, other times you MUST upgrade through each major version. This method
     * ensures we upgrade the Flyway history table through each version of Flyway where deemed necessary.
     *
     * @param flyway initialized/configured Flyway object
     * @param connection current database connection
     */
    protected static synchronized void upgradeFlywayTable(Flyway flyway, Connection connection)
        throws SQLException {
        // Whether the Flyway table needs updating or not
        boolean needsUpgrade = false;

        // Determine if Flyway needs updating by running a simple info() command.
        // This command will not run any pending migrations, but it will throw an exception
        // if the Flyway migration history table is NOT valid for the current version of Flyway
        try {
            flyway.info();
        } catch (Exception e) {
            // ignore error, but log info statement to say we will try to upgrade to fix problem
            log.info("Flyway table '{}' appears to be outdated. Will attempt to upgrade it automatically. " +
                         "Flyway Exception was '{}'", FLYWAY_TABLE, e.toString());
            needsUpgrade = true;
        }

        if (needsUpgrade) {
            // Get the DSpace version info from the LAST migration run.
            String lastMigration = getCurrentFlywayState(connection);
            // If this is an older DSpace 5.x compatible database, then it used Flyway 3.x.
            // Because we cannot upgrade directly from Flyway 3.x -> 6.x, we need to FIRST update this
            // database to be compatible with Flyway 4.2.0 (which can be upgraded directly to Flyway 6.x)
            if (lastMigration.startsWith("5.")) {
                // Based on type of DB, get path to our Flyway 4.x upgrade script
                String dbtype = getDbType(connection);
                String scriptPath = UPGRADE_SCRIPT_PATH + dbtype + "/upgradeToFlyway4x.sql";

                log.info("Attempting to upgrade Flyway table '{}' using script at '{}'",
                         FLYWAY_TABLE, scriptPath);
                // Load the Flyway v4.2.0 upgrade SQL script as a String
                String flywayUpgradeSQL = MigrationUtils.getResourceAsString(scriptPath);

                // As this Flyway upgrade SQL was borrowed from Flyway v4.2.0 directly, it contains some inline
                // variables which need replacing, namely ${schema} and ${table} variables.
                // We'll use the StringSubstitutor to replace those variables with their proper values.
                Map<String, String> valuesMap = new HashMap<>();
                valuesMap.put("schema", getSchemaName(connection));
                valuesMap.put("table", FLYWAY_TABLE);
                StringSubstitutor sub = new StringSubstitutor(valuesMap);
                flywayUpgradeSQL = sub.replace(flywayUpgradeSQL);

                // Run the script to update the Flyway table to be compatible with FLyway v4.x
                executeSql(connection, flywayUpgradeSQL);
            }
            // NOTE: no other DSpace versions require a specialized Flyway upgrade script at this time.
            // DSpace 4 didn't use Flyway. DSpace 6 used Flyway v4, which Flyway v6 can update automatically.

            // After any Flyway table upgrade, we MUST run a Flyway repair() to cleanup migration checksums if needed
            log.info("Repairing Flyway table '{}' after upgrade...", FLYWAY_TABLE);
            flyway.repair();
        }
    }
}
