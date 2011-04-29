/*
 * MetadataExport.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.bulkedit;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import java.util.ArrayList;
import java.sql.SQLException;

/**
 * Metadata exporter to allow the batch export of metadata into a file
 *
 * @author Stuart Lewis
 */
public class MetadataExport
{
    /** The Context */
    private Context c;

    /** The items to export */
    private ItemIterator toExport;

    /** Whether to export all metadata, or just normally edited metadata */
    private boolean exportAll;

    /** log4j logger */
    private static Logger log = Logger.getLogger(MetadataExport.class);

    /**
     * Set up a new metadata export
     *
     * @param c The Context
     * @param toExport The ItemIterator of items to export
     * @param exportAll whether to export all metadata or not (include handle, provenance etc)
     */
    public MetadataExport(Context c, ItemIterator toExport, boolean exportAll)
    {
        // Store the export settings
        this.c = c;
        this.toExport = toExport;
        this.exportAll = exportAll;
    }

    /**
     * Method to export a community (and sub-communites and collections)
     *
     * @param c The Context
     * @param toExport The Community to export
     * @param exportAll whether to export all metadata or not (include handle, provenance etc)
     */
    public MetadataExport(Context c, Community toExport, boolean exportAll)
    {
        try
        {
            // Try to export the community
            this.c = c;
            this.toExport = new ItemIterator(c, buildFromCommunity(toExport, new ArrayList(), 0));
            this.exportAll = exportAll;
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
     * @param community The community to build from
     * @param itemIDs The itemID (used for recuriosn - use an empty ArrayList)
     * @param indent How many spaces to use when writing out the names of items added
     * @return The list of item ids
     * @throws SQLException
     */
    private ArrayList buildFromCommunity(Community community, ArrayList itemIDs, int indent)
                                                                               throws SQLException
    {
        // Add all the collections
        Collection[] collections = community.getCollections();
        for (Collection collection : collections)
        {
            for (int i = 0; i < indent; i++) System.out.print(" ");
            ItemIterator items = collection.getAllItems();
            while (items.hasNext())
            {
                int id = items.next().getID();
                // Only add if not already included (so mapped items only appear once)
                if (!itemIDs.contains(id))
                {
                    itemIDs.add(id);
                }
            }
        }

        // Add all the sub-communities
        Community[] communities = community.getSubcommunities();
        for (Community subCommunity : communities)
        {
            for (int i = 0; i < indent; i++) System.out.print(" ");
            buildFromCommunity(subCommunity, itemIDs, indent + 1);
        }

        return itemIDs;
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
            // Process each item
            DSpaceCSV csv = new DSpaceCSV(exportAll);
            while (toExport.hasNext())
            {
                csv.addItem(toExport.next());
            }

            // Return the results
            return csv;
        }
        catch (Exception e)
        {
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
        Context c = new Context();
        c.turnOffAuthorisationSystem();

        // The things we'll export
        ItemIterator toExport = null;
        MetadataExport exporter = null;

        // Export everything?
        boolean exportAll = line.hasOption('a');

        // Check we have an item OK
        if (!line.hasOption('i'))
        {
            System.out.println("Exporting whole repository WARNING: May take some time!");
            exporter = new MetadataExport(c, Item.findAll(c), exportAll);
        }
        else
        {
            String handle = line.getOptionValue('i');
            DSpaceObject dso = HandleManager.resolveToObject(c, handle);
            if (dso == null)
            {
                System.err.println("Item '" + handle + "' does not resolve to an item in your repository!");
                printHelp(options, 1);
            }

            if (dso.getType() == Constants.ITEM)
            {
                System.out.println("Exporting item '" + dso.getName() + "' (" + handle + ")");
                ArrayList item = new ArrayList();
                item.add(dso.getID());
                exporter = new MetadataExport(c, new ItemIterator(c, item), exportAll);
            }
            else if (dso.getType() == Constants.COLLECTION)
            {
                System.out.println("Exporting collection '" + dso.getName() + "' (" + handle + ")");
                Collection collection = (Collection)dso;
                toExport = collection.getAllItems();
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

        // Finsh off and tidy up
        c.restoreAuthSystemState();
        c.complete();
    }
}