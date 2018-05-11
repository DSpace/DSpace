/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemexport;

import org.apache.commons.cli.*;
import org.dspace.app.itemexport.factory.ItemExportServiceFactory;
import org.dspace.app.itemexport.service.ItemExportService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

import java.util.*;

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
public class ItemExportCLITool {

    protected static ItemExportService itemExportService = ItemExportServiceFactory.getInstance().getItemExportService();
    protected static HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected static CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();


    /*
     *
     */
    public static void main(String[] argv) throws Exception
    {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("t", "type", true, "type: COLLECTION or ITEM");
        options.addOption("i", "id", true, "ID or handle of thing to export");
        options.addOption("d", "dest", true,
                "destination where you want items to go");
        options.addOption("m", "migrate", false, "export for migration (remove handle and metadata that will be re-created in new system)");
        options.addOption("n", "number", true,
                "sequence number to begin exporting items with");
        options.addOption("z", "zip", true, "export as zip file (specify filename e.g. export.zip)");
        options.addOption("h", "help", false, "help");

        // as pointed out by Peter Dietz this provides similar functionality to export metadata
        // but it is needed since it directly exports to Simple Archive Format (SAF)
        options.addOption("x", "exclude-bitstreams", false, "do not export bitstreams");

        CommandLine line = parser.parse(options, argv);

        String typeString = null;
        String destDirName = null;
        String myIDString = null;
        int seqStart = -1;
        int myType = -1;

        Item myItem = null;
        Collection mycollection = null;

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ItemExport\n", options);
            System.out
                    .println("\nfull collection: ItemExport -t COLLECTION -i ID -d dest -n number");
            System.out
                    .println("singleitem:       ItemExport -t ITEM -i ID -d dest -n number");

            System.exit(0);
        }

        if (line.hasOption('t')) // type
        {
            typeString = line.getOptionValue('t');

            if ("ITEM".equals(typeString))
            {
                myType = Constants.ITEM;
            }
            else if ("COLLECTION".equals(typeString))
            {
                myType = Constants.COLLECTION;
            }
        }

        if (line.hasOption('i')) // id
        {
            myIDString = line.getOptionValue('i');
        }

        if (line.hasOption('d')) // dest
        {
            destDirName = line.getOptionValue('d');
        }

        if (line.hasOption('n')) // number
        {
            seqStart = Integer.parseInt(line.getOptionValue('n'));
        }

        boolean migrate = false;
        if (line.hasOption('m')) // number
        {
            migrate = true;
        }

        boolean zip = false;
        String zipFileName = "";
        if (line.hasOption('z'))
        {
            zip = true;
            zipFileName = line.getOptionValue('z');
        }

        boolean excludeBitstreams = false;
        if (line.hasOption('x'))
        {
        	excludeBitstreams = true;
        }

        // now validate the args
        if (myType == -1)
        {
            System.out
                    .println("type must be either COLLECTION or ITEM (-h for help)");
            System.exit(1);
        }

        if (destDirName == null)
        {
            System.out
                    .println("destination directory must be set (-h for help)");
            System.exit(1);
        }

        if (seqStart == -1)
        {
            System.out
                    .println("sequence start number must be set (-h for help)");
            System.exit(1);
        }

        if (myIDString == null)
        {
            System.out
                    .println("ID must be set to either a database ID or a handle (-h for help)");
            System.exit(1);
        }

        Context c = new Context(Context.Mode.READ_ONLY);
        c.turnOffAuthorisationSystem();

        if (myType == Constants.ITEM)
        {
            // first, is myIDString a handle?
            if (myIDString.indexOf('/') != -1)
            {
                myItem = (Item) handleService.resolveToObject(c, myIDString);

                if ((myItem == null) || (myItem.getType() != Constants.ITEM))
                {
                    myItem = null;
                }
            }
            else
            {
                myItem = itemService.find(c, UUID.fromString(myIDString));
            }

            if (myItem == null)
            {
                System.out
                        .println("Error, item cannot be found: " + myIDString);
            }
        }
        else
        {
            if (myIDString.indexOf('/') != -1)
            {
                // has a / must be a handle
                mycollection = (Collection) handleService.resolveToObject(c,
                        myIDString);

                // ensure it's a collection
                if ((mycollection == null)
                        || (mycollection.getType() != Constants.COLLECTION))
                {
                    mycollection = null;
                }
            }
            else if (myIDString != null)
            {
                mycollection = collectionService.find(c, UUID.fromString(myIDString));
            }

            if (mycollection == null)
            {
                System.out.println("Error, collection cannot be found: "
                        + myIDString);
                System.exit(1);
            }
        }

        if (zip)
        {
            Iterator<Item> items;
            if (myItem != null)
            {
                List<Item> myItems = new ArrayList<>();
                myItems.add(myItem);
                items = myItems.iterator();
            }
            else
            {
                System.out.println("Exporting from collection: " + myIDString);
                items = itemService.findByCollection(c, mycollection);
            }
            itemExportService.exportAsZip(c, items, destDirName, zipFileName, seqStart, migrate, excludeBitstreams);
        }
        else
        {
            if (myItem != null)
            {
                // it's only a single item
                itemExportService.exportItem(c, Collections.singletonList(myItem).iterator(), destDirName, seqStart, migrate, excludeBitstreams);
            }
            else
            {
                System.out.println("Exporting from collection: " + myIDString);

                // it's a collection, so do a bunch of items
                Iterator<Item> i = itemService.findByCollection(c, mycollection);
                itemExportService.exportItem(c, i, destDirName, seqStart, migrate, excludeBitstreams);
            }
        }

        c.complete();
    }
}
