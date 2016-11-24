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
     * @param argv the command line arguments given
     */
    public static void main(String[] argv)
    {
        try
        {
            log.info("Cleaning up asset store");
            
            // set up command line parser
            CommandLineParser parser = new PosixParser();
            CommandLine line = null;

            // create an options object and populate it
            Options options = new Options();

            options.addOption("l", "leave", false, "Leave database records but delete file from assetstore");
            options.addOption("v", "verbose", false, "Provide verbose output");
            options.addOption("h", "help", false, "Help");
            
            try
            {
                line = parser.parse(options, argv);
            }
            catch (ParseException e)
            {
                log.fatal(e);
                System.exit(1);
            }
            
            // user asks for help
            if (line.hasOption('h'))
            {
                printHelp(options);
                System.exit(0);
            }

            boolean deleteDbRecords = true;
            // Prune stage
            if (line.hasOption('l'))
            {
                log.debug("option l used setting flag to leave db records");
                deleteDbRecords = false;    
            }
            log.debug("leave db records = " + deleteDbRecords);
            StorageServiceFactory.getInstance().getBitstreamStorageService().cleanup(deleteDbRecords, line.hasOption('v'));
            
            System.exit(0);
        }
        catch (Exception e)
        {
            log.fatal("Caught exception:", e);
            System.exit(1);
        }
    }
    
    private static void printHelp(Options options)
    {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("Cleanup\n", options);
    }

}
