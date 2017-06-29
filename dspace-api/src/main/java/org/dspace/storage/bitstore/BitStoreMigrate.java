/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.dspace.storage.bitstore.service.BitstreamStorageService;

/**
 * Command Line Utility to migrate bitstreams from one assetstore to another
 */
public class BitStoreMigrate {

    /** log4j log */
    private static Logger log = Logger.getLogger(BitStoreMigrate.class);

    private static final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private static final BitstreamStorageService bitstreamStorageService = StorageServiceFactory.getInstance().getBitstreamStorageService();

    /**
     * Migrates asset store.
     *
     * @param argv -
     *            Command-line arguments
     */
    public static void main(String[] argv)
    {
        try
        {
            log.info("Migrate Assetstore");

            // set up command line parser
            CommandLineParser parser = new PosixParser();
            CommandLine line = null;

            // create an options object and populate it
            Options options = new Options();

            options.addOption("a", "source", true, "Source assetstore store_number (to lose content). This is a number such as 0 or 1");
            options.addOption("b", "destination", true, "Destination assetstore store_number (to gain content). This is a number such as 0 or 1.");
            options.addOption("d", "delete", false, "Delete file from losing assetstore. (Default: Keep bitstream in old assetstore)");
            options.addOption("p", "print", false, "Print out current assetstore information");
            options.addOption("s", "size", true, "Batch commit size. (Default: 1, commit after each file transfer)");
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

            Context context = new Context(Context.Mode.BATCH_EDIT);
            context.turnOffAuthorisationSystem();

            if(line.hasOption('p')) {
                bitstreamStorageService.printStores(context);
                System.exit(0);
            }

            boolean deleteOld = false;
            if (line.hasOption('d'))
            {
                log.debug("DELETE flag set to remove bitstream from old assetstore");
                deleteOld = true;
            }
            log.debug("deleteOldAssets = " + deleteOld);


            if(line.hasOption('a') && line.hasOption('b')) {
                Integer sourceAssetstore = Integer.valueOf(line.getOptionValue('a'));
                Integer destinationAssetstore = Integer.valueOf(line.getOptionValue('b'));

                //Safe default, commit every time. TODO Performance Profile
                Integer batchCommitSize = 1;
                if(line.hasOption('s')) {
                    batchCommitSize = Integer.parseInt(line.getOptionValue('s'));
                }

                bitstreamStorageService.migrate(context, sourceAssetstore, destinationAssetstore, deleteOld, batchCommitSize);
            } else {
                printHelp(options);
                System.exit(0);
            }

            context.complete();

            System.exit(0);
        }
        catch (Exception e)
        {
            log.fatal("Caught exception:", e);
            System.out.println("Exception during BitStoreMigrate: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printHelp(Options options)
    {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("BitstoreMigrate\n", options);
    }
}
