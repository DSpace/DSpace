/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import com.google.common.collect.Iterators;
import org.apache.commons.cli.*;

import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;

import java.util.ArrayList;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Metadata exporter to allow the batch export of metadata into a file
 *
 * @author Stuart Lewis
 */
public class MetadataExport
{
    /** The items to export */
    protected Iterator<Item> toExport;

    protected ItemService itemService;

    protected Context context;

    /** Whether to export all metadata, or just normally edited metadata */
    protected boolean exportAll;

    protected MetadataExport() {
        itemService = ContentServiceFactory.getInstance().getItemService();
    }

    /**
     * Set up a new metadata export
     *
     * @param c The Context
     * @param toExport The ItemIterator of items to export
     * @param exportAll whether to export all metadata or not (include handle, provenance etc)
     */
    public MetadataExport(Context c, Iterator<Item> toExport, boolean exportAll)
    {
        itemService = ContentServiceFactory.getInstance().getItemService();

        // Store the export settings
        this.toExport = toExport;
        this.exportAll = exportAll;
        this.context = c;
    }

    /**
     * Method to export a community (and sub-communities and collections)
     *
     * @param c The Context
     * @param toExport The Community to export
     * @param exportAll whether to export all metadata or not (include handle, provenance etc)
     */
    public MetadataExport(Context c, Community toExport, boolean exportAll)
    {
        itemService = ContentServiceFactory.getInstance().getItemService();

        try
        {
            // Try to export the community
            this.toExport = buildFromCommunity(c, toExport, 0);
            this.exportAll = exportAll;
            this.context = c;
        }
        catch (SQLException sqle)
        {
            // Something went wrong...
            System.err.println("Error running exporter:");
            sqle.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /**
     * Build an array list of item ids that are in a community (include sub-communities and collections)
     *
     * @param context DSpace context
     * @param community The community to build from
     * @param indent How many spaces to use when writing out the names of items added
     * @return The list of item ids
     * @throws SQLException if database error
     */
    protected Iterator<Item> buildFromCommunity(Context context, Community community, int indent)
                                                                               throws SQLException
    {
        // Add all the collections
        List<Collection> collections = community.getCollections();
        Iterator<Item> result = null;
        for (Collection collection : collections)
        {
            for (int i = 0; i < indent; i++)
            {
                System.out.print(" ");
            }

            Iterator<Item> items = itemService.findByCollection(context, collection);
            result = addItemsToResult(result,items);

        }
        // Add all the sub-communities
        List<Community> communities = community.getSubcommunities();
        for (Community subCommunity : communities)
        {
            for (int i = 0; i < indent; i++)
            {
                System.out.print(" ");
            }
            Iterator<Item> items = buildFromCommunity(context, subCommunity, indent + 1);
            result = addItemsToResult(result,items);
        }

        return result;
    }

    private Iterator<Item> addItemsToResult(Iterator<Item> result, Iterator<Item> items) {
        if(result == null)
        {
            result = items;
        }else{
            result = Iterators.concat(result, items);
        }

        return result;
    }

    /**
     * Run the export
     *
     * @return the exported CSV lines
     */
    public DSpaceCSV export()
    {
        try
        {
            Context.Mode originalMode = context.getCurrentMode();
            context.setMode(Context.Mode.READ_ONLY);

            // Process each item
            DSpaceCSV csv = new DSpaceCSV(exportAll);
            while (toExport.hasNext())
            {
                Item item = toExport.next();
                csv.addItem(item);
                context.uncacheEntity(item);
            }

            context.setMode(originalMode);
            // Return the results
            return csv;
        }
        catch (Exception e)
        {
            // Something went wrong...
            System.err.println("Error exporting to CSV:");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Print the help message
     *
     * @param options The command line options the user gave
     * @param exitCode the system exit code to use
     */
    private static void printHelp(Options options, int exitCode)
    {
        // print the help message
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("MetadataExport\n", options);
        System.out.println("\nfull export: metadataexport -f filename");
        System.out.println("partial export: metadataexport -i handle -f filename");
        System.exit(exitCode);
    }

    /**
	 * main method to run the metadata exporter
	 *
	 * @param argv the command line arguments given
         * @throws Exception if error occurs
	 */
    public static void main(String[] argv) throws Exception
    {
        // Create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("i", "id", true, "ID or handle of thing to export (item, collection, or community)");
        options.addOption("f", "file", true, "destination where you want file written");
        options.addOption("a", "all", false, "include all metadata fields that are not normally changed (e.g. provenance)");
        options.addOption("h", "help", false, "help");

        CommandLine line = null;

        try
        {
            line = parser.parse(options, argv);
        }
        catch (ParseException pe)
        {
            System.err.println("Error with commands.");
            printHelp(options, 1);
            System.exit(0);
        }

        if (line.hasOption('h'))
        {
            printHelp(options, 0);
        }

        // Check a filename is given
        if (!line.hasOption('f'))
        {
            System.err.println("Required parameter -f missing!");
            printHelp(options, 1);
        }
        String filename = line.getOptionValue('f');

        // Create a context
        Context c = new Context(Context.Mode.READ_ONLY);
        c.turnOffAuthorisationSystem();

        // The things we'll export
        Iterator<Item> toExport = null;
        MetadataExport exporter = null;

        // Export everything?
        boolean exportAll = line.hasOption('a');

        ContentServiceFactory contentServiceFactory = ContentServiceFactory.getInstance();
        // Check we have an item OK
        ItemService itemService = contentServiceFactory.getItemService();
        if (!line.hasOption('i'))
        {
            System.out.println("Exporting whole repository WARNING: May take some time!");
            exporter = new MetadataExport(c, itemService.findAll(c), exportAll);
        }
        else
        {
            String handle = line.getOptionValue('i');
            DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(c, handle);
            if (dso == null)
            {
                System.err.println("Item '" + handle + "' does not resolve to an item in your repository!");
                printHelp(options, 1);
            }

            if (dso.getType() == Constants.ITEM)
            {
                System.out.println("Exporting item '" + dso.getName() + "' (" + handle + ")");
                List<Item> item = new ArrayList<>();
                item.add((Item) dso);
                exporter = new MetadataExport(c, item.iterator(), exportAll);
            }
            else if (dso.getType() == Constants.COLLECTION)
            {
                System.out.println("Exporting collection '" + dso.getName() + "' (" + handle + ")");
                Collection collection = (Collection)dso;
                toExport = itemService.findByCollection(c, collection);
                exporter = new MetadataExport(c, toExport, exportAll);
            }
            else if (dso.getType() == Constants.COMMUNITY)
            {
                System.out.println("Exporting community '" + dso.getName() + "' (" + handle + ")");
                exporter = new MetadataExport(c, (Community)dso, exportAll);
            }
            else
            {
                System.err.println("Error identifying '" + handle + "'");
                System.exit(1);
            }
        }

        // Perform the export
        DSpaceCSV csv = exporter.export();

        // Save the files to the file
        csv.save(filename);        

        // Finish off and tidy up
        c.restoreAuthSystemState();
        c.complete();
    }
}
