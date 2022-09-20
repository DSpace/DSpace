/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
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
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

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
public class ItemImport extends DSpaceRunnable<ItemImportScriptConfiguration> {

    protected boolean template = false;
    protected String command = null;
    protected String sourcedir = null;
    protected String mapfile = null;
    protected String eperson = null;
    protected String[] collections = null;
    protected boolean isTest = false;
    protected boolean isResume = false;
    protected boolean useWorkflow = false;
    protected boolean useWorkflowSendEmail = false;
    protected boolean isQuiet = false;
    protected boolean commandLineCollections = false;
    protected boolean zip = false;
    protected String zipfilename = null;
    protected boolean help = false;

    protected static final CollectionService collectionService =
            ContentServiceFactory.getInstance().getCollectionService();
    protected static final EPersonService epersonService =
            EPersonServiceFactory.getInstance().getEPersonService();
    protected static final HandleService handleService =
            HandleServiceFactory.getInstance().getHandleService();

    @Override
    public ItemImportScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
                .getServiceByName("import", ItemImportScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        help = commandLine.hasOption('h');

        if (commandLine.hasOption('a')) {
            command = "add";
        }

        if (commandLine.hasOption('r')) {
            command = "replace";
        }

        if (commandLine.hasOption('d')) {
            command = "delete";
        }

        if (commandLine.hasOption('w')) {
            useWorkflow = true;
            if (commandLine.hasOption('n')) {
                useWorkflowSendEmail = true;
            }
        }

        if (commandLine.hasOption('t')) {
            isTest = true;
            handler.logInfo("**Test Run** - not actually importing items.");
        }

        if (commandLine.hasOption('p')) {
            template = true;
        }

        if (commandLine.hasOption('s')) { // source
            sourcedir = commandLine.getOptionValue('s');
        }

        if (commandLine.hasOption('m')) { // mapfile
            mapfile = commandLine.getOptionValue('m');
        }

        if (commandLine.hasOption('e')) { // eperson
            eperson = commandLine.getOptionValue('e');
        }

        if (commandLine.hasOption('c')) { // collections
            collections = commandLine.getOptionValues('c');
            commandLineCollections = true;
        } else {
            handler.logInfo("No collections given. Assuming 'collections' file inside item directory");
        }

        if (commandLine.hasOption('R')) {
            isResume = true;
            handler.logInfo("**Resume import** - attempting to import items not already imported");
        }

        if (commandLine.hasOption('q')) {
            isQuiet = true;
        }

        if (commandLine.hasOption('z')) {
            zip = true;
            zipfilename = commandLine.getOptionValue('z');
        }
    }

    @Override
    public void internalRun() throws Exception {
        if (help) {
            printHelp();
            handler.logInfo("adding items: ItemImport -a -e eperson -c collection -s sourcedir -m mapfile");
            handler.logInfo("adding items from zip file: ItemImport -a -e eperson -c collection -s sourcedir "
                    + "-z filename.zip -m mapfile");
            handler.logInfo("replacing items: ItemImport -r -e eperson -c collection -s sourcedir -m mapfile");
            handler.logInfo("deleting items: ItemImport -d -e eperson -m mapfile");
            handler.logInfo("If multiple collections are specified, the first collection will be the one "
                    + "that owns the item.");
            return;
        }

        // validation
        // can only resume for adds
        if (isResume && !"add".equals(command)) {
            handler.logError("Resume option only works with the --add command (run with -h flag for details)");
            throw new UnsupportedOperationException("Resume option only works with the --add command");
        }

        File myFile = new File(mapfile);

        if (!isResume && "add".equals(command) && myFile.exists()) {
            handler.logError("The mapfile " + mapfile + " already exists. "
                    + "Either delete it or use --resume if attempting to resume an aborted import. "
                    + "(run with -h flag for details)");
            throw new UnsupportedOperationException("The mapfile " + mapfile + " already exists");
        }

        if (command == null) {
            handler.logError("Must run with either add, replace, or remove (run with -h flag for details)");
            throw new UnsupportedOperationException("Must run with either add, replace, or remove");
        } else if ("add".equals(command) || "replace".equals(command)) {
            if (sourcedir == null) {
                handler.logError("A source directory containing items must be set (run with -h flag for details)");
                throw new UnsupportedOperationException("A source directory containing items must be set");
            }

            if (mapfile == null) {
                handler.logError(
                        "A map file to hold importing results must be specified (run with -h flag for details)");
                throw new UnsupportedOperationException("A map file to hold importing results must be specified");
            }
        } else if ("delete".equals(command)) {
            if (mapfile == null) {
                handler.logError("A map file must be specified (run with -h flag for details)");
                throw new UnsupportedOperationException("A map file must be specified");
            }
        }

        if (eperson == null) {
            handler.logError("An eperson to do the importing must be specified (run with -h flag for details)");
            throw new UnsupportedOperationException("An eperson to do the importing must be specified");
        }

        Date startTime = new Date();
        Context context = new Context(Context.Mode.BATCH_EDIT);

        // check eperson
        EPerson myEPerson = null;
        if (StringUtils.contains(eperson, '@')) {
            // @ sign, must be an email
            myEPerson = epersonService.findByEmail(context, eperson);
        } else {
            myEPerson = epersonService.find(context, UUID.fromString(eperson));
        }

        if (myEPerson == null) {
            handler.logError("EPerson cannot be found: " + eperson + " (run with -h flag for details)");
            throw new UnsupportedOperationException("EPerson cannot be found: " + eperson);
        }
        context.setCurrentUser(myEPerson);

        // check collection
        List<Collection> mycollections = null;
        // don't need to validate collections set if command is "delete"
        // also if no collections are given in the command line
        if (!"delete".equals(command) && commandLineCollections) {
            handler.logInfo("Destination collections:");

            mycollections = new ArrayList<>();

            // validate each collection arg to see if it's a real collection
            for (int i = 0; i < collections.length; i++) {
                Collection collection = null;
                if (collections[i] != null) {
                    // is the ID a handle?
                    if (collections[i].indexOf('/') != -1) {
                        // string has a / so it must be a handle - try and resolve
                        // it
                        collection = ((Collection) handleService
                            .resolveToObject(context, collections[i]));
                    } else {
                        // not a handle, try and treat it as an integer collection database ID
                        collection = collectionService.find(context, UUID.fromString(collections[i]));
                    }
                }

                // was the collection valid?
                if (collection == null
                        || collection.getType() != Constants.COLLECTION) {
                    throw new IllegalArgumentException("Cannot resolve "
                            + collections[i] + " to collection");
                }

                // add resolved collection to list
                mycollections.add(collection);

                // print progress info
                handler.logInfo((i == 0 ? "Owning " : "") + "Collection: " + collection.getName());
            }
        }
        // end validation

        // start
        ItemImportService itemImportService = ItemImportServiceFactory.getInstance()
                .getItemImportService();
        try {
            itemImportService.setTest(isTest);
            itemImportService.setResume(isResume);
            itemImportService.setUseWorkflow(useWorkflow);
            itemImportService.setUseWorkflowSendEmail(useWorkflowSendEmail);
            itemImportService.setQuiet(isQuiet);

            try {
                // If this is a zip archive, unzip it first
                if (zip) {
                    sourcedir = itemImportService.unzip(sourcedir, zipfilename);
                }

                context.turnOffAuthorisationSystem();

                if ("add".equals(command)) {
                    itemImportService.addItems(context, mycollections, sourcedir, mapfile, template);
                } else if ("replace".equals(command)) {
                    itemImportService.replaceItems(context, mycollections, sourcedir, mapfile, template);
                } else if ("delete".equals(command)) {
                    itemImportService.deleteItems(context, mapfile);
                }

                // complete all transactions
                context.complete();
            } catch (Exception e) {
                context.abort();
                throw new Exception(
                    "Error committing changes to database: " + e.getMessage() + ", aborting most recent changes", e);
            }

            // Delete the unzipped file
            try {
                if (zip) {
                    System.gc();
                    handler.logInfo(
                        "Deleting temporary zip directory: "
                        + itemImportService.getTempWorkDirFile().getAbsolutePath());
                    itemImportService.cleanupZipTemp();
                }
            } catch (IOException ex) {
                handler.logError("Unable to delete temporary zip archive location: "
                        + itemImportService.getTempWorkDirFile().getAbsolutePath());
                throw new IOException("Unable to delete temporary zip archive location: "
                        + itemImportService.getTempWorkDirFile().getAbsolutePath(), ex);
            }

            if (isTest) {
                handler.logInfo("***End of Test Run***");
            }
        } finally {
            Date endTime = new Date();
            handler.logInfo("Started: " + startTime.getTime());
            handler.logInfo("Ended: " + endTime.getTime());
            handler.logInfo(
                "Elapsed time: " + ((endTime.getTime() - startTime.getTime()) / 1000) + " secs (" + (endTime
                    .getTime() - startTime.getTime()) + " msecs)");
        }
    }
}
