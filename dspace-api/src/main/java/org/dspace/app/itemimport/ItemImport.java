/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.itemimport.factory.ItemImportServiceFactory;
import org.dspace.app.itemimport.service.ItemImportService;
import org.dspace.authorize.AuthorizeException;
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

    public static String TEMP_DIR = "importSAF";
    public static String MAPFILE_FILENAME = "mapfile";
    public static String MAPFILE_BITSTREAM_TYPE = "importSAFMapfile";

    protected boolean template = false;
    protected String command = null;
    protected String sourcedir = null;
    protected String mapfile = null;
    protected String eperson = null;
    protected String[] collections = null;
    protected boolean isTest = false;
    protected boolean isExcludeContent = false;
    protected boolean isResume = false;
    protected boolean useWorkflow = false;
    protected boolean useWorkflowSendEmail = false;
    protected boolean isQuiet = false;
    protected boolean commandLineCollections = false;
    protected boolean zip = false;
    protected String zipfilename = null;
    protected boolean help = false;
    protected File workDir = null;
    private File workFile = null;

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

        if (commandLine.hasOption('v')) {
            isTest = true;
            handler.logInfo("**Test Run** - not actually importing items.");
        }

        isExcludeContent = commandLine.hasOption('x');

        if (commandLine.hasOption('p')) {
            template = true;
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

        setZip();
    }

    @Override
    public void internalRun() throws Exception {
        if (help) {
            printHelp();
            return;
        }

        Date startTime = new Date();
        Context context = new Context(Context.Mode.BATCH_EDIT);

        setMapFile();

        validate(context);

        setEPerson(context);

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
            itemImportService.setExcludeContent(isExcludeContent);
            itemImportService.setResume(isResume);
            itemImportService.setUseWorkflow(useWorkflow);
            itemImportService.setUseWorkflowSendEmail(useWorkflowSendEmail);
            itemImportService.setQuiet(isQuiet);
            itemImportService.setHandler(handler);

            try {
                context.turnOffAuthorisationSystem();

                readZip(context, itemImportService);

                process(context, itemImportService, mycollections);

                // complete all transactions
                context.complete();
            } catch (Exception e) {
                context.abort();
                throw new Exception(
                    "Error committing changes to database: " + e.getMessage() + ", aborting most recent changes", e);
            }

            if (isTest) {
                handler.logInfo("***End of Test Run***");
            }
        } finally {
            // clean work dir
            if (zip) {
                FileUtils.deleteDirectory(new File(sourcedir));
                FileUtils.deleteDirectory(workDir);
            }

            Date endTime = new Date();
            handler.logInfo("Started: " + startTime.getTime());
            handler.logInfo("Ended: " + endTime.getTime());
            handler.logInfo(
                "Elapsed time: " + ((endTime.getTime() - startTime.getTime()) / 1000) + " secs (" + (endTime
                    .getTime() - startTime.getTime()) + " msecs)");
        }
    }

    /**
     * Validate the options
     * @param context
     */
    protected void validate(Context context) {
        if (command == null) {
            handler.logError("Must run with either add, replace, or remove (run with -h flag for details)");
            throw new UnsupportedOperationException("Must run with either add, replace, or remove");
        }

        // can only resume for adds
        if (isResume && !"add".equals(command)) {
            handler.logError("Resume option only works with the --add command (run with -h flag for details)");
            throw new UnsupportedOperationException("Resume option only works with the --add command");
        }

        if (isResume && StringUtils.isBlank(mapfile)) {
            handler.logError("The mapfile does not exist. ");
            throw new UnsupportedOperationException("The mapfile does not exist");
        }
    }

    /**
     * Process the import
     * @param context
     * @param itemImportService
     * @param collections
     * @throws Exception
     */
    protected void process(Context context, ItemImportService itemImportService,
            List<Collection> collections) throws Exception {
        readMapfile(context);

        if ("add".equals(command)) {
            itemImportService.addItems(context, collections, sourcedir, mapfile, template);
        } else if ("replace".equals(command)) {
            itemImportService.replaceItems(context, collections, sourcedir, mapfile, template);
        } else if ("delete".equals(command)) {
            itemImportService.deleteItems(context, mapfile);
        }

        // write input stream on handler
        File mapFile = new File(mapfile);
        try (InputStream mapfileInputStream = new FileInputStream(mapFile)) {
            handler.writeFilestream(context, MAPFILE_FILENAME, mapfileInputStream, MAPFILE_BITSTREAM_TYPE);
        } finally {
            mapFile.delete();
            workFile.delete();
        }
    }

    /**
     * Read the ZIP archive in SAF format
     * @param context
     * @param itemImportService
     * @throws Exception
     */
    protected void readZip(Context context, ItemImportService itemImportService) throws Exception {
        Optional<InputStream> optionalFileStream = handler.getFileStream(context, zipfilename);
        if (optionalFileStream.isPresent()) {
            workFile = new File(itemImportService.getTempWorkDir() + File.separator
                    + zipfilename + "-" + context.getCurrentUser().getID());
            FileUtils.copyInputStreamToFile(optionalFileStream.get(), workFile);
            workDir = new File(itemImportService.getTempWorkDir() + File.separator + TEMP_DIR);
            sourcedir = itemImportService.unzip(workFile, workDir.getAbsolutePath());
        } else {
            throw new IllegalArgumentException(
                    "Error reading file, the file couldn't be found for filename: " + zipfilename);
        }
    }

    /**
     * Read the mapfile
     * @param context
     */
    protected void readMapfile(Context context) {
        if (isResume) {
            try {
                Optional<InputStream> optionalFileStream = handler.getFileStream(context, mapfile);
                if (optionalFileStream.isPresent()) {
                    File tempFile = File.createTempFile(mapfile, "temp");
                    tempFile.deleteOnExit();
                    FileUtils.copyInputStreamToFile(optionalFileStream.get(), tempFile);
                    mapfile = tempFile.getAbsolutePath();
                }
            } catch (IOException | AuthorizeException e) {
                throw new UnsupportedOperationException("The mapfile does not exist");
            }
        }
    }

    /**
     * Set the mapfile option
     * @throws IOException
     */
    protected void setMapFile() throws IOException {
        if (isResume && commandLine.hasOption('m')) {
            mapfile = commandLine.getOptionValue('m');
        } else {
            mapfile = Files.createTempFile(MAPFILE_FILENAME, "temp").toString();
        }
    }

    /**
     * Set the zip option
     */
    protected void setZip() {
        zip = true;
        zipfilename = commandLine.getOptionValue('z');
    }

    /**
     * Set the eperson in the context
     * @param context
     * @throws SQLException
     */
    protected void setEPerson(Context context) throws SQLException {
        EPerson myEPerson = epersonService.find(context, this.getEpersonIdentifier());

        // check eperson
        if (myEPerson == null) {
            handler.logError("EPerson cannot be found: " + this.getEpersonIdentifier());
            throw new UnsupportedOperationException("EPerson cannot be found: " + this.getEpersonIdentifier());
        }

        context.setCurrentUser(myEPerson);
    }
}
