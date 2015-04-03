/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.app;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.DSpaceCSVLine;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/* Created for LINDAT/CLARIN */
/**
 * Action to move items from one collection to another
 * 
 * @author Michal Josífko
 */
public class MoveItems
{
    /** The Context */
    Context c;

    /** The DSpaceCSV object we're processing */
    DSpaceCSV csv;

    /** The lines to import */
    List<DSpaceCSVLine> toImport;

    /** Logger */
    private static final Logger log = Logger.getLogger(MoveItems.class);

    /**
     * Print the help message
     * 
     * @param options
     *            The command line options the user gave
     * @param exitCode
     *            the system exit code to use
     */
    private static void printHelp(Options options, int exitCode)
    {
        // print the help message
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp(
                "MoveItems (-h | -l | -s <source collection ID> -t <target collection ID>)\n",
                options);
        System.exit(exitCode);
    }

    /**
     * main method to run the MoveItems action
     * 
     * @param argv
     *            the command line arguments given
     * @throws SQLException
     */
    public static void main(String[] argv) throws SQLException
    {
        // Create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("l", "list", false, "list collections");
        options.addOption("s", "source", true, "source collection ID");
        options.addOption("t", "target", true, "target collection ID");
        options.addOption("i", "inherit", false,
                "inherit target collection privileges (default false)");
        options.addOption("h", "help", false, "help");

        // Parse the command line arguments
        CommandLine line;
        try
        {
            line = parser.parse(options, argv);
        }
        catch (ParseException pe)
        {
            System.err.println("Error parsing command line arguments: "
                    + pe.getMessage());
            System.exit(1);    
            return;
        }

        if (line.hasOption('h') || line.getOptions().length == 0)
        {
            printHelp(options, 0);
        }

        // Create a context
        Context context;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();
        }
        catch (Exception e)
        {
            System.err.println("Unable to create a new DSpace Context: "
                    + e.getMessage());
            System.exit(1);     
            return;
        }                

        if (line.hasOption('l'))
        {
            listCollections(context);
            System.exit(0);            
        }

        // Check a filename is given
        if (!line.hasOption('s'))
        {
            System.err.println("Required parameter -s missing!");
            printHelp(options, 1);
        }

        Boolean inherit;

        // Check a filename is given
        if (line.hasOption('i'))
        {
            inherit = true;
        }
        else
        {
            inherit = false;
        }

        String sourceCollectionIdParam = line.getOptionValue('s');
        Integer sourceCollectionId = null;

        if (sourceCollectionIdParam.matches("\\d+"))
        {
            sourceCollectionId = Integer.valueOf(sourceCollectionIdParam);
        }
        else
        {
            System.err.println("Invalid argument for parameter -s: "
                    + sourceCollectionIdParam);
            printHelp(options, 1);
        }

        // Check source collection ID is given
        if (!line.hasOption('t'))
        {
            System.err.println("Required parameter -t missing!");
            printHelp(options, 1);
        }
        String targetCollectionIdParam = line.getOptionValue('t');
        Integer targetCollectionId = null;

        if (targetCollectionIdParam.matches("\\d+"))
        {
            targetCollectionId = Integer.valueOf(targetCollectionIdParam);
        }
        else
        {
            System.err.println("Invalid argument for parameter -t: "
                    + targetCollectionIdParam);
            printHelp(options, 1);
        }

        // Check target collection ID is given
        if (targetCollectionIdParam.equals(sourceCollectionIdParam))
        {
            System.err
                    .println("Source collection id and target collection id must differ");
            printHelp(options, 1);
        }

        try
        {

            moveItems(context, sourceCollectionId, targetCollectionId, inherit);

            // Finish off and tidy up
            context.restoreAuthSystemState();
            context.complete();
        }
        catch (Exception e)
        {
            context.abort();
            System.err.println("Error committing changes to database: "
                    + e.getMessage());
            System.err.println("Aborting most recent changes.");
            System.exit(1);
        }
    }

    /**
     * Moves items from one collection to another
     * 
     * @param sourceCollectionId
     *            ID of the original collection
     * @param targetCollectionId
     *            ID of the target collection
     */

    private static void moveItems(Context context, Integer sourceCollectionId,
            Integer targetCollectionId, Boolean inherit) throws SQLException,
            AuthorizeException, IOException
    {
        int count = 0;

        Collection sourceCollection = Collection.find(context,
                sourceCollectionId);
        Collection targetCollection = Collection.find(context,
                targetCollectionId);

        if (sourceCollection == null)
        {
            System.err.println("Source collection not found.");
            System.exit(1);
        }

        if (targetCollection == null)
        {
            System.err.println("Target collection not found.");
            System.exit(1);
        }

        ItemIterator it = sourceCollection.getAllItems();
        while (it.hasNext())
        {
            Item item = it.next();
            if (item.isOwningCollection(sourceCollection))
            {
                moveItem(context, item, targetCollection, inherit);
                count++;
            }
        }

        System.out.println(count + " items succesfully moved from collection "
                + sourceCollectionId + " to collection " + targetCollectionId);
    }

    /**
     * Lists IDs and names of all collections in DSpace instance
     * 
     * @param context
     *            The context
     * @throws SQLException
     */

    private static void listCollections(Context context) throws SQLException
    {
        Community[] communities = Community.findAll(context);
        for (Community community : communities)
        {
            System.out.println(community.getName());

            Collection[] collections = community.getCollections();
            for (Collection collection : collections)
            {
                ItemIterator it = collection.getAllItems();
                int count = 0;
                while (it.hasNext())
                {
                    count++;
                    it.next();
                }
                System.out.println("\t" + collection.getID() + " - "
                        + collection.getName() + " [" + count + " items]");
            }

        }
    }

    /**
     * Moves single item from current collection to another
     * 
     * @param context
     *            The context
     * @param item
     *            The item
     * @param targetCollection
     *            The target collection
     * @param inherit
     *            Should privileges be inherited from targetCollection?
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */

    private static void moveItem(Context context, Item item,
            Collection targetCollection, Boolean inherit) throws SQLException,
            AuthorizeException, IOException
    {

        if (AuthorizeManager.isAdmin(context, item))
        {
            // Add an action giving this user *explicit* admin permissions on
            // the item itself.
            // This ensures that the user will be able to call item.update()
            // even if he/she
            // moves it to a Collection that he/she doesn't administer.
            if (item.canEdit())
            {
                AuthorizeManager
                        .authorizeAction(context, item, Constants.WRITE);
            }

            Collection owningCollection = item.getOwningCollection();
            if (targetCollection.equals(owningCollection))
            {
                return;
            }

            // note: an item.move() method exists, but does not handle several
            // cases:
            // - no preexisting owning collection (first arg is null)
            // - item already in collection, but not an owning collection
            // (works, but puts item in collection twice)

            // Don't re-add the item to a collection it's already in.
            boolean alreadyInCollection = false;
            for (Collection collection : item.getCollections())
            {
                if (collection.equals(targetCollection))
                {
                    alreadyInCollection = true;
                    break;
                }
            }

            // Remove item from its owning collection and add to the destination
            if (!alreadyInCollection)
            {
                targetCollection.addItem(item);
            }

            if (owningCollection != null)
            {
                owningCollection.removeItem(item);
            }

            item.setOwningCollection(targetCollection);

            // Inherit policies of destination collection if required
            if (inherit)
            {
                item.inheritCollectionDefaultPolicies(targetCollection);
            }

            item.update();

        }
    }
}
