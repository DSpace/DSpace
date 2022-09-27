/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemexport;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.file.PathUtils;
import org.dspace.app.itemexport.factory.ItemExportServiceFactory;
import org.dspace.app.itemexport.service.ItemExportService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
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
 * Item exporter to create simple AIPs for DSpace content. Currently exports
 * individual items, or entire collections. For instructions on use, see
 * printUsage() method.
 * <P>
 * ItemExport creates the simple AIP package that the importer also uses. It
 * consists of:
 * <P>
 * /exportdir/42/ (one directory per item) / dublin_core.xml - qualified dublin
 * core in RDF schema / contents - text file, listing one file per line / file1
 * - files contained in the item / file2 / ...
 * <P>
 * issues -doesn't handle special characters in metadata (needs to turn {@code &'s} into
 * {@code &amp;}, etc.)
 * <P>
 * Modified by David Little, UCSD Libraries 12/21/04 to allow the registration
 * of files (bitstreams) into DSpace.
 *
 * @author David Little
 * @author Jay Paz
 */
public class ItemExport extends DSpaceRunnable<ItemExportScriptConfiguration> {

    public static final String TEMP_DIR = "exportSAF";
    public static final String ZIP_NAME = "exportSAFZip";
    public static final String ZIP_FILENAME = "saf-export";
    public static final String ZIP_EXT = "zip";

    protected String typeString = null;
    protected String destDirName = null;
    protected String idString = null;
    protected int seqStart = -1;
    protected int type = -1;
    protected Item item = null;
    protected Collection collection = null;
    protected boolean migrate = false;
    protected boolean zip = false;
    protected String zipFileName = "";
    protected boolean excludeBitstreams = false;
    protected boolean help = false;

    protected static HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected static CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected static final EPersonService epersonService =
            EPersonServiceFactory.getInstance().getEPersonService();

    @Override
    public ItemExportScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
                .getServiceByName("export", ItemExportScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        help = commandLine.hasOption('h');

        if (commandLine.hasOption('t')) { // type
            typeString = commandLine.getOptionValue('t');

            if ("ITEM".equals(typeString)) {
                type = Constants.ITEM;
            } else if ("COLLECTION".equals(typeString)) {
                type = Constants.COLLECTION;
            }
        }

        if (commandLine.hasOption('i')) { // id
            idString = commandLine.getOptionValue('i');
        }

        setNumber();

        if (commandLine.hasOption('m')) { // number
            migrate = true;
        }

        if (commandLine.hasOption('x')) {
            excludeBitstreams = true;
        }
    }

    @Override
    public void internalRun() throws Exception {
        if (help) {
            printHelp();
            return;
        }

        validate();

        Context context = new Context();
        context.turnOffAuthorisationSystem();

        if (type == Constants.ITEM) {
            // first, is myIDString a handle?
            if (idString.indexOf('/') != -1) {
                item = (Item) handleService.resolveToObject(context, idString);

                if ((item == null) || (item.getType() != Constants.ITEM)) {
                    item = null;
                }
            } else {
                item = itemService.find(context, UUID.fromString(idString));
            }

            if (item == null) {
                handler.logError("The item cannot be found: " + idString + " (run with -h flag for details)");
                throw new UnsupportedOperationException("The item cannot be found: " + idString);
            }
        } else {
            if (idString.indexOf('/') != -1) {
                // has a / must be a handle
                collection = (Collection) handleService.resolveToObject(context,
                                                                          idString);

                // ensure it's a collection
                if ((collection == null)
                    || (collection.getType() != Constants.COLLECTION)) {
                    collection = null;
                }
            } else {
                collection = collectionService.find(context, UUID.fromString(idString));
            }

            if (collection == null) {
                handler.logError("The collection cannot be found: " + idString + " (run with -h flag for details)");
                throw new UnsupportedOperationException("The collection cannot be found: " + idString);
            }
        }

        ItemExportService itemExportService = ItemExportServiceFactory.getInstance()
                .getItemExportService();
        try {
            itemExportService.setHandler(handler);
            process(context, itemExportService);
            context.complete();
        } catch (Exception e) {
            context.abort();
            throw new Exception(e);
        }
    }

    /**
     * Validate the options
     */
    protected void validate() {
        if (type == -1) {
            handler.logError("The type must be either COLLECTION or ITEM (run with -h flag for details)");
            throw new UnsupportedOperationException("The type must be either COLLECTION or ITEM");
        }

        if (idString == null) {
            handler.logError("The ID must be set to either a database ID or a handle (run with -h flag for details)");
            throw new UnsupportedOperationException("The ID must be set to either a database ID or a handle");
        }
    }

    /**
     * Process the export
     * @param context
     * @throws Exception
     */
    protected void process(Context context, ItemExportService itemExportService) throws Exception {
        setEPerson(context);
        setDestDirName(context, itemExportService);
        setZip(context);

        Iterator<Item> items;
        if (item != null) {
            List<Item> myItems = new ArrayList<>();
            myItems.add(item);
            items = myItems.iterator();
        } else {
            handler.logInfo("Exporting from collection: " + idString);
            items = itemService.findByCollection(context, collection);
        }
        itemExportService.exportAsZip(context, items, destDirName, zipFileName,
                seqStart, migrate, excludeBitstreams);

        File zip = new File(destDirName + System.getProperty("file.separator") + zipFileName);
        try (InputStream is = new FileInputStream(zip)) {
            // write input stream on handler
            handler.writeFilestream(context, ZIP_FILENAME + "." + ZIP_EXT, is, ZIP_NAME);
        } finally {
            PathUtils.deleteDirectory(Path.of(destDirName));
        }
    }

    /**
     * Set the destination directory option
     */
    protected void setDestDirName(Context context, ItemExportService itemExportService) throws Exception {
        destDirName = itemExportService.getExportWorkDirectory() + File.separator + TEMP_DIR;
    }

    /**
     * Set the zip option
     */
    protected void setZip(Context context) {
        zip = true;
        zipFileName = ZIP_FILENAME + "-" + context.getCurrentUser().getID() + "." + ZIP_EXT;
    }

    /**
     * Set the number option
     */
    protected void setNumber() {
        seqStart = 1;
        if (commandLine.hasOption('n')) { // number
            seqStart = Integer.parseInt(commandLine.getOptionValue('n'));
        }
    }

    private void setEPerson(Context context) throws SQLException {
        EPerson myEPerson = epersonService.find(context, this.getEpersonIdentifier());

        // check eperson
        if (myEPerson == null) {
            handler.logError("EPerson cannot be found: " + this.getEpersonIdentifier());
            throw new UnsupportedOperationException("EPerson cannot be found: " + this.getEpersonIdentifier());
        }

        context.setCurrentUser(myEPerson);
    }
}
