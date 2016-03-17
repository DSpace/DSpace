/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.apache.log4j.Logger;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;

/**
 * Cleans up asset store.
 * 
 * @author Peter Breton
 * @version $Revision$
 */
public class Cleanup
{
    /** log4j log */
    private static Logger log = Logger.getLogger(Cleanup.class);

    /**
     * Cleans up asset store.
     * 
     * @param argv -
     *            Command-line arguments
     */
    public static void main(String[] argv)
    {
        log.info("Cleaning up asset store");

        // set up command line parser
        CommandLineParser parser = new PosixParser();
        CommandLine line = null;

        // create an options object and populate it
        Options options = new Options();

        options.addOption("l", "leave", false, "Leave database records but delete file from assetstore");
        options.addOption("b", "batchSize", true, "commitBatches, default 100");
        options.addOption("v", "verbose", false, "Provide verbose output");
        options.addOption("h", "help", false, "Help");

        try
        {
            line = parser.parse(options, argv);
            // user asks for help
            if (line.hasOption('h'))
            {
                printHelp(options);
            }
            else  // actually do the work
            {
                boolean deleteDbRecords = !line.hasOption('l');
                boolean verbose = line.hasOption('v');
                int commitBatch = 100;
                if (line.hasOption('b')) {
                    try {
                        commitBatch = Integer.parseInt(line.getOptionValue('b'));
                    } catch (NumberFormatException ne) {
                        throw new ParseException("batchSize must be greater equal 1");
                    }
                    if (commitBatch < 1) {
                        throw new ParseException("batchSize must be greater equal 1");
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("leave db records = " + deleteDbRecords);
                    log.debug("batchSize = " + commitBatch);
                }
                if (verbose) {
                    System.out.println("leave db records = " + deleteDbRecords);
                    System.out.println("batchSize = " + commitBatch);
                }
                StorageServiceFactory.getInstance().getBitstreamStorageService().cleanup(deleteDbRecords, commitBatch, verbose);
            }
        }
        catch (Exception e)
        {
            log.fatal("Caught exception:", e);
            printHelp(options);
            System.exit(1);
        }
    }
    
    private static void printHelp(Options options)
    {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("Cleanup\n", options);
    }

}
