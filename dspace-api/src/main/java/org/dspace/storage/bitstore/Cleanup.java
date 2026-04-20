/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.dspace.utils.DSpace;

/**
 * Cleans up asset store.
 *
 * @author Peter Breton
 */
public class Cleanup {
    /**
     * log4j log
     */
    private static final Logger log = LogManager.getLogger(Cleanup.class);

    /**
     * Default constructor
     */
    private Cleanup() { }

    /**
     * Cleans up asset store.
     *
     * @param argv the command line arguments given
     */
    public static void main(String[] argv) {
        try {
            log.info("Cleaning up asset store");

            // set up command line parser
            CommandLineParser parser = new DefaultParser();
            CommandLine line = null;

            // create an options object and populate it
            Options options = new Options();

            boolean versioning = new DSpace().getConfigurationService().getBooleanProperty("versioning.enabled", true);
            boolean replaceBitstream = new DSpace().getConfigurationService()
                    .getBooleanProperty("replace-bitstream.enabled", false);
            boolean defaultLeave = versioning || replaceBitstream;
            String defaultSuffix = String.format(" (default due to versioning.enabled=%b or " +
                            "replace-bitstream.enabled=%b)",
                    versioning, replaceBitstream);
            options.addOption("l", "leave", false, "Leave database records but delete file " +
                    "from assetstore" + (defaultLeave ? defaultSuffix : ""));
            options.addOption("d", "delete", false, "Delete database records as well as " +
                    "assetstore files" + (!defaultLeave ? defaultSuffix : ""));
            options.addOption("v", "verbose", false, "Provide verbose output");
            options.addOption("h", "help", false, "Help");

            try {
                line = parser.parse(options, argv);
            } catch (ParseException e) {
                log.fatal(e);
                System.exit(1);
            }

            // user asks for help
            if (line.hasOption('h')) {
                printHelp(options);
                System.exit(0);
            }

            boolean deleteDbRecords = !defaultLeave;
            // Prune stage
            if (line.hasOption('l')) {
                log.debug("option l used setting flag to leave db records");
                deleteDbRecords = false;
            }
            if (line.hasOption('d')) {
                log.debug("option d used setting flag to delete db records");
                deleteDbRecords = true;
            }
            if (line.hasOption('l') && line.hasOption('d')) {
                throw new IllegalArgumentException("Cannot use --leave and --delete together!");
            }
            log.debug("leave db records = " + deleteDbRecords);
            StorageServiceFactory.getInstance().getBitstreamStorageService()
                                 .cleanup(deleteDbRecords, line.hasOption('v'));

            System.exit(0);
        } catch (IOException | SQLException | AuthorizeException e) {
            log.fatal("Caught exception:", e);
            System.exit(1);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("Cleanup\n", options);
    }

}
