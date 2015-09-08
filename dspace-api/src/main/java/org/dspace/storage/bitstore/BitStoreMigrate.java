package org.dspace.storage.bitstore;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.dspace.core.Context;

/**
 * Created by peterdietz on 9/8/15.
 */
public class BitStoreMigrate {

    /** log4j log */
    private static Logger log = Logger.getLogger(BitStoreMigrate.class);

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

            options.addOption("a", "source", true, "Source assetstore (to lose content)");
            options.addOption("b", "destination", true, "Destination assetstore (to gain content)");
            options.addOption("k", "keep", false, "Keep assets in source assetstore (don't delete it).");
            options.addOption("p", "print", false, "Print out current assetstore information");
            options.addOption("s", "size", true, "Batch commit size");
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

            Context context = new Context();
            context.turnOffAuthorisationSystem();

            if(line.hasOption('p')) {
                BitstreamStorageManager.printStores(context);
                System.exit(0);
            }

            boolean deleteOld = true;
            if (line.hasOption('k'))
            {
                log.debug("option k used setting flag to keep old assetstore files alone");
                deleteOld = false;
            }
            log.debug("deleteOldAssets = " + deleteOld);


            if(line.hasOption('a') && line.hasOption('b')) {
                Integer sourceAssetstore = Integer.valueOf(line.getOptionValue('a'));
                Integer destinationAssetstore = Integer.valueOf(line.getOptionValue('b'));

                Integer batchCommitSize = 100;
                if(line.hasOption('s')) {
                    batchCommitSize = Integer.parseInt(line.getOptionValue('s'));
                }

                BitstreamStorageManager.migrate(context, sourceAssetstore, destinationAssetstore, deleteOld, batchCommitSize);
            }

            context.complete();

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
        myhelp.printHelp("Migrate\n", options);
    }
}
