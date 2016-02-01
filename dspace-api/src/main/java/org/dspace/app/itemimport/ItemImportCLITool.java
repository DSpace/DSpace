/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import org.apache.commons.cli.*;
import org.dspace.app.itemimport.factory.ItemImportServiceFactory;
import org.dspace.app.itemimport.service.ItemImportService;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Import items into DSpace. The conventional use is upload files by copying
 * them. DSpace writes the item's bitstreams into its assetstore. Metadata is
 * also loaded to the DSpace database.
 * <P>
 * A second use assumes the bitstream files already exist in a storage
 * resource accessible to DSpace. In this case the bitstreams are 'registered'.
 * That is, the metadata is loaded to the DSpace database and DSpace is given
 * the location of the file which is subsumed into DSpace.
 * <P>
 * The distinction is controlled by the format of lines in the 'contents' file.
 * See comments in processContentsFile() below.
 * <P>
 * Modified by David Little, UCSD Libraries 12/21/04 to
 * allow the registration of files (bitstreams) into DSpace.
 */
public class ItemImportCLITool {

    private static boolean template = false;

    private static final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private static final EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private static final HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    public static void main(String[] argv) throws Exception
    {
        Date startTime = new Date();
        int status = 0;

        try {
            // create an options object and populate it
            CommandLineParser parser = new PosixParser();

            Options options = new Options();

            options.addOption("a", "add", false, "add items to DSpace");
            options.addOption("b", "add-bte", false, "add items to DSpace via Biblio-Transformation-Engine (BTE)");
            options.addOption("r", "replace", false, "replace items in mapfile");
            options.addOption("d", "delete", false,
                    "delete items listed in mapfile");
            options.addOption("i", "inputtype", true, "input type in case of BTE import");
            options.addOption("s", "source", true, "source of items (directory)");
            options.addOption("z", "zip", true, "name of zip file");
            options.addOption("c", "collection", true,
                    "destination collection(s) Handle or database ID");
            options.addOption("m", "mapfile", true, "mapfile items in mapfile");
            options.addOption("e", "eperson", true,
                    "email of eperson doing importing");
            options.addOption("w", "workflow", false,
                    "send submission through collection's workflow");
            options.addOption("n", "notify", false,
                    "if sending submissions through the workflow, send notification emails");
            options.addOption("t", "test", false,
                    "test run - do not actually import items");
            options.addOption("p", "template", false, "apply template");
            options.addOption("R", "resume", false,
                    "resume a failed import (add only)");
            options.addOption("q", "quiet", false, "don't display metadata");

            options.addOption("h", "help", false, "help");

            CommandLine line = parser.parse(options, argv);

            String command = null; // add replace remove, etc
            String bteInputType = null; //ris, endnote, tsv, csv, bibtex
            String sourcedir = null;
            String mapfile = null;
            String eperson = null; // db ID or email
            String[] collections = null; // db ID or handles
            boolean isTest = false;
            boolean isResume = false;
            boolean useWorkflow = false;
            boolean useWorkflowSendEmail = false;
            boolean isQuiet = false;

            if (line.hasOption('h')) {
                HelpFormatter myhelp = new HelpFormatter();
                myhelp.printHelp("ItemImport\n", options);
                System.out
                        .println("\nadding items:    ItemImport -a -e eperson -c collection -s sourcedir -m mapfile");
                System.out
                        .println("\nadding items from zip file:    ItemImport -a -e eperson -c collection -s sourcedir -z filename.zip -m mapfile");
                System.out
                        .println("replacing items: ItemImport -r -e eperson -c collection -s sourcedir -m mapfile");
                System.out
                        .println("deleting items:  ItemImport -d -e eperson -m mapfile");
                System.out
                        .println("If multiple collections are specified, the first collection will be the one that owns the item.");

                System.exit(0);
            }

            if (line.hasOption('a')) {
                command = "add";
            }

            if (line.hasOption('r')) {
                command = "replace";
            }

            if (line.hasOption('d')) {
                command = "delete";
            }

            if (line.hasOption('b')) {
                command = "add-bte";
            }

            if (line.hasOption('i')) {
                bteInputType = line.getOptionValue('i');
            }

            if (line.hasOption('w')) {
                useWorkflow = true;
                if (line.hasOption('n')) {
                    useWorkflowSendEmail = true;
                }
            }

            if (line.hasOption('t')) {
                isTest = true;
                System.out.println("**Test Run** - not actually importing items.");
            }

            if (line.hasOption('p')) {
                template = true;
            }

            if (line.hasOption('s')) // source
            {
                sourcedir = line.getOptionValue('s');
            }

            if (line.hasOption('m')) // mapfile
            {
                mapfile = line.getOptionValue('m');
            }

            if (line.hasOption('e')) // eperson
            {
                eperson = line.getOptionValue('e');
            }

            if (line.hasOption('c')) // collections
            {
                collections = line.getOptionValues('c');
            }

            if (line.hasOption('R')) {
                isResume = true;
                System.out
                        .println("**Resume import** - attempting to import items not already imported");
            }

            if (line.hasOption('q')) {
                isQuiet = true;
            }

            boolean zip = false;
            String zipfilename = "";
            if (line.hasOption('z')) {
                zip = true;
                zipfilename = sourcedir + System.getProperty("file.separator") + line.getOptionValue('z');
            }

            //By default assume collections will be given on the command line
            boolean commandLineCollections = true;
            // now validate
            // must have a command set
            if (command == null) {
                System.out
                        .println("Error - must run with either add, replace, or remove (run with -h flag for details)");
                System.exit(1);
            } else if ("add".equals(command) || "replace".equals(command)) {
                if (sourcedir == null) {
                    System.out
                            .println("Error - a source directory containing items must be set");
                    System.out.println(" (run with -h flag for details)");
                    System.exit(1);
                }

                if (mapfile == null) {
                    System.out
                            .println("Error - a map file to hold importing results must be specified");
                    System.out.println(" (run with -h flag for details)");
                    System.exit(1);
                }

                if (eperson == null) {
                    System.out
                            .println("Error - an eperson to do the importing must be specified");
                    System.out.println(" (run with -h flag for details)");
                    System.exit(1);
                }

                if (collections == null) {
                    System.out.println("No collections given. Assuming 'collections' file inside item directory");
                    commandLineCollections = false;
                }
            } else if ("add-bte".equals(command)) {
                //Source dir can be null, the user can specify the parameters for his loader in the Spring XML configuration file

                if (mapfile == null) {
                    System.out
                            .println("Error - a map file to hold importing results must be specified");
                    System.out.println(" (run with -h flag for details)");
                    System.exit(1);
                }

                if (eperson == null) {
                    System.out
                            .println("Error - an eperson to do the importing must be specified");
                    System.out.println(" (run with -h flag for details)");
                    System.exit(1);
                }

                if (collections == null) {
                    System.out.println("No collections given. Assuming 'collections' file inside item directory");
                    commandLineCollections = false;
                }

                if (bteInputType == null) {
                    System.out
                            .println("Error - an input type (tsv, csv, ris, endnote, bibtex or any other type you have specified in BTE Spring XML configuration file) must be specified");
                    System.out.println(" (run with -h flag for details)");
                    System.exit(1);
                }
            } else if ("delete".equals(command)) {
                if (eperson == null) {
                    System.out
                            .println("Error - an eperson to do the importing must be specified");
                    System.exit(1);
                }

                if (mapfile == null) {
                    System.out.println("Error - a map file must be specified");
                    System.exit(1);
                }
            }

            // can only resume for adds
            if (isResume && !"add".equals(command) && !"add-bte".equals(command)) {
                System.out
                        .println("Error - resume option only works with the --add or the --add-bte commands");
                System.exit(1);
            }

            // do checks around mapfile - if mapfile exists and 'add' is selected,
            // resume must be chosen
            File myFile = new File(mapfile);

            if (!isResume && "add".equals(command) && myFile.exists()) {
                System.out.println("Error - the mapfile " + mapfile
                        + " already exists.");
                System.out
                        .println("Either delete it or use --resume if attempting to resume an aborted import.");
                System.exit(1);
            }

            ItemImportService myloader = ItemImportServiceFactory.getInstance().getItemImportService();
            myloader.setTest(isTest);
            myloader.setResume(isResume);
            myloader.setUseWorkflow(useWorkflow);
            myloader.setUseWorkflowSendEmail(useWorkflowSendEmail);
            myloader.setQuiet(isQuiet);

            // create a context
            Context c = new Context();

            // find the EPerson, assign to context
            EPerson myEPerson = null;

            if (eperson.indexOf('@') != -1) {
                // @ sign, must be an email
                myEPerson = epersonService.findByEmail(c, eperson);
            } else {
                myEPerson = epersonService.find(c, UUID.fromString(eperson));
            }

            if (myEPerson == null) {
                System.out.println("Error, eperson cannot be found: " + eperson);
                System.exit(1);
            }

            c.setCurrentUser(myEPerson);

            // find collections
            List<Collection> mycollections = null;

            // don't need to validate collections set if command is "delete"
            // also if no collections are given in the command line
            if (!"delete".equals(command) && commandLineCollections) {
                System.out.println("Destination collections:");

                mycollections = new ArrayList<>();

                // validate each collection arg to see if it's a real collection
                for (int i = 0; i < collections.length; i++) {
                    // is the ID a handle?
                    if (collections[i].indexOf('/') != -1) {
                        // string has a / so it must be a handle - try and resolve
                        // it
                        mycollections.add((Collection) handleService
                                .resolveToObject(c, collections[i]));

                        // resolved, now make sure it's a collection
                        if ((mycollections.get(i) == null)
                                || (mycollections.get(i).getType() != Constants.COLLECTION)) {
                            mycollections.set(i, null);
                        }
                    }
                    // not a handle, try and treat it as an integer collection
                    // database ID
                    else if (collections[i] != null) {
                        mycollections.set(i, collectionService.find(c, UUID.fromString(collections[i])));
                    }

                    // was the collection valid?
                    if (mycollections.get(i) == null) {
                        throw new IllegalArgumentException("Cannot resolve "
                                + collections[i] + " to collection");
                    }

                    // print progress info
                    String owningPrefix = "";

                    if (i == 0) {
                        owningPrefix = "Owning ";
                    }

                    System.out.println(owningPrefix + " Collection: "
                            + mycollections.get(i).getName());
                }
            } // end of validating collections

            try {
                // If this is a zip archive, unzip it first
                if (zip) {
                    sourcedir = myloader.unzip(sourcedir, zipfilename);
                }


                c.turnOffAuthorisationSystem();

                if ("add".equals(command)) {
                    myloader.addItems(c, mycollections, sourcedir, mapfile, template);
                } else if ("replace".equals(command)) {
                    myloader.replaceItems(c, mycollections, sourcedir, mapfile, template);
                } else if ("delete".equals(command)) {
                    myloader.deleteItems(c, mapfile);
                } else if ("add-bte".equals(command)) {
                    myloader.addBTEItems(c, mycollections, sourcedir, mapfile, template, bteInputType, null);
                }

                // complete all transactions
                c.complete();
            } catch (Exception e) {
                c.abort();
                e.printStackTrace();
                System.out.println(e);
                status = 1;
            }

            // Delete the unzipped file
            try {
                if (zip) {
                    System.gc();
                    System.out.println("Deleting temporary zip directory: " + myloader.getTempWorkDirFile().getAbsolutePath());
                    myloader.cleanupZipTemp();
                }
            } catch (Exception ex) {
                System.out.println("Unable to delete temporary zip archive location: " + myloader.getTempWorkDirFile().getAbsolutePath());
            }


            if (isTest) {
                System.out.println("***End of Test Run***");
            }
        } finally {
            Date endTime = new Date();
            System.out.println("Started: " + startTime.getTime());
            System.out.println("Ended: " + endTime.getTime());
            System.out.println("Elapsed time: " + ((endTime.getTime() - startTime.getTime()) / 1000) + " secs (" + (endTime.getTime() - startTime.getTime()) + " msecs)");
        }

        System.exit(status);
    }
}
