/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.harvest;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.HarvestingException;
import org.dspace.harvest.OAIHarvester;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.model.OAIHarvesterOptions;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for harvested collections.
 *
 * @author Alexey Maslov
 */
public class Harvest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Harvest.class);

    public static final String LOG_PREFIX = "PROCESSINGDATA ";

    public static final String LOG_DELIMITER = "|";

    private static Context context;

    private static final HarvestedCollectionService harvestedCollectionService =
        HarvestServiceFactory.getInstance().getHarvestedCollectionService();
    private static final EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private static final CollectionService collectionService =
        ContentServiceFactory.getInstance().getCollectionService();
    private static final OAIHarvester harvester = HarvestServiceFactory.getInstance().getOAIHarvester();
    private static final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

    public static void main(String[] argv) throws Exception {
        // create an options object and populate it
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();

        options.addOption("p", "purge", false, "delete all items in the collection");
        options.addOption("r", "run", false, "run the standard harvest procedure");
        options.addOption("g", "ping", false, "test the OAI server and set");
        options.addOption("o", "once", false, "run the harvest procedure with specified parameters");
        options.addOption("s", "setup", false, "Set the collection up for harvesting");
        options.addOption("S", "start", false, "start the harvest loop");
        options.addOption("R", "reset", false, "reset harvest status on all collections");
        options.addOption("P", "purge", false, "purge all harvestable collections");

        options.addOption("F", "force synchronization", false, "force the synchronization");
        options.addOption("V", "validate", false, "to enable the item validation");
        options.addOption("W", "workflow", false, "to start the item workflow after its creation");


        options.addOption("e", "eperson", true,
                          "eperson");
        options.addOption("c", "collection", true,
                          "harvesting collection (handle or id)");
        options.addOption("t", "type", true,
                          "type of harvesting (0 for none)");
        options.addOption("a", "address", true,
                          "address of the OAI-PMH server");
        options.addOption("i", "oai_set_id", true,
                          "id of the PMH set representing the harvested collection");
        options.addOption("m", "metadata_format", true,
                          "the name of the desired metadata format for harvesting, resolved to namespace and " +
                              "crosswalk in dspace.cfg");

        options.addOption("h", "help", false, "help");

        CommandLine line = parser.parse(options, argv);

        String command = null;
        String eperson = null;
        String collection = null;
        String oaiSource = null;
        String oaiSetID = null;
        String metadataKey = null;
        int harvestType = 0;
        boolean forceSynch = false;
        boolean validation = false;
        boolean submitEnabled = true;

        if (line.hasOption('h')) {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("Harvest\n", options);
            System.out.println("\nPING OAI server: Harvest -g -a oai_source -i oai_set_id");
            System.out.println(
                "RUNONCE harvest with arbitrary options: Harvest -o -e eperson -c collection -t harvest_type -a " +
                    "oai_source -i oai_set_id -m metadata_format");
            System.out.println(
                "SETUP a collection for harvesting: Harvest -s -c collection -t harvest_type -a oai_source -i " +
                    "oai_set_id -m metadata_format");
            System.out.println("RUN harvest once: Harvest -r -e eperson -c collection");
            System.out.println("START harvest scheduler: Harvest -S");
            System.out.println("RESET all harvest status: Harvest -R");
            System.out.println("PURGE a collection of items and settings: Harvest -p -e eperson -c collection");
            System.out.println("PURGE all harvestable collections: Harvest -P -e eperson");


            System.exit(0);
        }

        if (line.hasOption('s')) {
            command = "config";
        }
        if (line.hasOption('p')) {
            command = "purge";
        }
        if (line.hasOption('r')) {
            command = "run";
        }
        if (line.hasOption('g')) {
            command = "ping";
        }
        if (line.hasOption('o')) {
            command = "runOnce";
        }
        if (line.hasOption('S')) {
            command = "start";
        }
        if (line.hasOption('R')) {
            command = "reset";
        }
        if (line.hasOption('P')) {
            command = "purgeAll";
        }


        if (line.hasOption('e')) {
            eperson = line.getOptionValue('e');
        }
        if (line.hasOption('c')) {
            collection = line.getOptionValue('c');
        }
        if (line.hasOption('t')) {
            harvestType = Integer.parseInt(line.getOptionValue('t'));
        } else {
            harvestType = 0;
        }
        if (line.hasOption('a')) {
            oaiSource = line.getOptionValue('a');
        }
        if (line.hasOption('i')) {
            oaiSetID = line.getOptionValue('i');
        }
        if (line.hasOption('m')) {
            metadataKey = line.getOptionValue('m');
        }
        if (line.hasOption('F')) {
            forceSynch = true;
        }
        if (line.hasOption('V')) {
            validation = true;
        }
        if (line.hasOption('W')) {
            submitEnabled = false;
        }


        // Instantiate our class
        Harvest harvester = new Harvest();
        Harvest.context = new Context(Context.Mode.BATCH_EDIT);


        // Check our options
        if (command == null) {
            System.out
                .println("Error - no parameters specified (run with -h flag for details)");
            System.exit(1);
        } else if ("run".equals(command)) {
            // Run a single harvest cycle on a collection using saved settings.
            if (collection == null || eperson == null) {
                System.out
                    .println("Error - a target collection and eperson must be provided");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }

            harvester.runHarvest(collection, eperson, new OAIHarvesterOptions(forceSynch, validation, submitEnabled));

        } else if ("start".equals(command)) {
            // start the harvest loop
            startHarvester();
        } else if ("reset".equals(command)) {
            // reset harvesting status
            resetHarvesting();
        } else if ("purgeAll".equals(command)) {
            // purge all collections that are set up for harvesting (obviously for testing purposes only)
            if (eperson == null) {
                System.out
                    .println("Error - an eperson must be provided");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }

            List<HarvestedCollection> harvestedCollections = harvestedCollectionService.findAll(context);
            for (HarvestedCollection harvestedCollection : harvestedCollections) {
                System.out.println(
                    "Purging the following collections (deleting items and resetting harvest status): " +
                        harvestedCollection
                            .getCollection().getID().toString());
                harvester.purgeCollection(harvestedCollection.getCollection().getID().toString(), eperson);
            }
            context.complete();
        } else if ("purge".equals(command)) {
            // Delete all items in a collection. Useful for testing fresh harvests.
            if (collection == null || eperson == null) {
                System.out
                    .println("Error - a target collection and eperson must be provided");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }

            harvester.purgeCollection(collection, eperson);
            context.complete();

            //TODO: implement this... remove all items and remember to unset "last-harvested" settings
        } else if ("config".equals(command)) {
            // Configure a collection with the three main settings
            if (collection == null) {
                System.out.println("Error -  a target collection must be provided");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }
            if (oaiSource == null || oaiSetID == null) {
                System.out.println("Error - both the OAI server address and OAI set id must be specified");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }
            if (metadataKey == null) {
                System.out
                    .println("Error - a metadata key (commonly the prefix) must be specified for this collection");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }

            harvester.configureCollection(collection, harvestType, oaiSource, oaiSetID, metadataKey);
        } else if ("ping".equals(command)) {
            if (oaiSource == null || oaiSetID == null) {
                System.out.println("Error - both the OAI server address and OAI set id must be specified");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }

            pingResponder(oaiSource, oaiSetID, metadataKey);
        }
    }

    /*
     * Resolve the ID into a collection and check to see if its harvesting options are set. If so, return
     * the collection, if not, bail out.
     */
    private Collection resolveCollection(String collectionID) {

        DSpaceObject dso;
        Collection targetCollection = null;

        try {
            // is the ID a handle?
            if (collectionID != null) {
                if (collectionID.indexOf('/') != -1) {
                    // string has a / so it must be a handle - try and resolve it
                    dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, collectionID);

                    // resolved, now make sure it's a collection
                    if (dso == null || dso.getType() != Constants.COLLECTION) {
                        targetCollection = null;
                    } else {
                        targetCollection = (Collection) dso;
                    }
                } else {
                    // not a handle, try and treat it as an collection database UUID
                    System.out.println("Looking up by UUID: " + collectionID + ", " + "in context: " + context);
                    targetCollection = collectionService.find(context, UUID.fromString(collectionID));
                }
            }
            // was the collection valid?
            if (targetCollection == null) {
                System.out.println("Cannot resolve " + collectionID + " to collection");
                System.exit(1);
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }

        return targetCollection;
    }


    private void configureCollection(String collectionID, int type, String oaiSource, String oaiSetId,
                                     String mdConfigId) {
        System.out.println("Running: configure collection");

        Collection collection = resolveCollection(collectionID);
        System.out.println(collection.getID());

        try {
            HarvestedCollection hc = harvestedCollectionService.find(context, collection);
            if (hc == null) {
                hc = harvestedCollectionService.create(context, collection);
            }

            context.turnOffAuthorisationSystem();
            hc.setHarvestParams(type, oaiSource, oaiSetId, mdConfigId);
            hc.setHarvestStatus(HarvestedCollection.STATUS_READY);
            harvestedCollectionService.update(context, hc);
            context.restoreAuthSystemState();
            context.complete();
        } catch (Exception e) {
            System.out.println("Changes could not be committed");
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (context != null) {
                context.restoreAuthSystemState();
            }
        }
    }


    /**
     * Purges a collection of all harvest-related data and settings. All items in the collection will be deleted.
     *
     * @param collectionID
     * @param email
     */
    private void purgeCollection(String collectionID, String email) {
        System.out.println(
            "Purging collection of all items and resetting last_harvested and harvest_message: " + collectionID);
        Collection collection = resolveCollection(collectionID);

        try {
            EPerson eperson = ePersonService.findByEmail(context, email);
            context.setCurrentUser(eperson);
            context.turnOffAuthorisationSystem();

            ItemService itemService = ContentServiceFactory.getInstance().getItemService();
            Iterator<Item> it = itemService.findByCollection(context, collection);
            int i = 0;
            while (it.hasNext()) {
                i++;
                Item item = it.next();
                System.out.println("Deleting: " + item.getHandle());
                collectionService.removeItem(context, collection, item);
                context.uncacheEntity(item);// Dispatch events every 50 items
                if (i % 50 == 0) {
                    context.dispatchEvents();
                    i = 0;
                }
            }

            HarvestedCollection hc = harvestedCollectionService.find(context, collection);
            if (hc != null) {
                hc.setLastHarvested(null);
                hc.setHarvestMessage("");
                hc.setHarvestStatus(HarvestedCollection.STATUS_READY);
                hc.setHarvestStartTime(null);
                harvestedCollectionService.update(context, hc);
            }
            context.restoreAuthSystemState();
            context.dispatchEvents();
        } catch (Exception e) {
            System.out.println("Changes could not be committed");
            e.printStackTrace();
            System.exit(1);
        } finally {
            context.restoreAuthSystemState();
        }
    }


    /**
     * Run a single harvest cycle on the specified collection under the authorization of the supplied EPerson
     */
    private void runHarvest(String collectionID, String email, OAIHarvesterOptions options) {
        System.out.println("Running: a harvest cycle on " + collectionID);

        System.out.print("Initializing the harvester... ");
        try {

            Collection collection = resolveCollection(collectionID);
            HarvestedCollection hc = harvestedCollectionService.find(context, collection);

            if (hc == null) {
                throw new HarvestingException("Provided collection is not set up for harvesting");
            }

            // Harvest will not work for an anonymous user
            EPerson eperson = ePersonService.findByEmail(context, email);

            System.out.println("Harvest started... ");
            context.setCurrentUser(eperson);

            long startTimestamp = System.currentTimeMillis();

            logProcess(options.getProcessId(), hc, true, startTimestamp);

            harvester.runHarvest(context, hc, options);
            context.complete();

            logProcess(options.getProcessId(), hc, false, startTimestamp);

            System.out.println("success. ");

        } catch (SQLException se) {
            System.out.print("failed. ");
            System.out.println(se.getMessage());
            throw new IllegalStateException("Unable to access database", se);
        }

        System.out.println("Harvest complete. ");
    }

    /**
     * Resets harvest_status and harvest_start_time flags for all collections that have a row in the
     * harvested_collections table
     */
    private static void resetHarvesting() {
        System.out.print("Resetting harvest status flag on all collections... ");

        try {
            List<HarvestedCollection> harvestedCollections = harvestedCollectionService.findAll(context);
            for (HarvestedCollection harvestedCollection : harvestedCollections) {
                //hc.setHarvestResult(null,"");
                harvestedCollection.setHarvestStartTime(null);
                harvestedCollection.setHarvestStatus(HarvestedCollection.STATUS_READY);
                harvestedCollectionService.update(context, harvestedCollection);
            }
            System.out.println("success. ");
        } catch (Exception ex) {
            System.out.println("failed. ");
            ex.printStackTrace();
        }
    }

    /**
     * Starts up the harvest scheduler. Terminating this process will stop the scheduler.
     */
    private static void startHarvester() {
        try {
            System.out.print("Starting harvest loop... ");
            HarvestServiceFactory.getInstance().getHarvestSchedulingService().startNewScheduler();
            System.out.println("running. ");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * See if the responder is alive and working.
     *
     * @param server         address of the responder's host.
     * @param set            name of an item set.
     * @param metadataFormat local prefix name, or null for "dc".
     */
    private static void pingResponder(String server, String set, String metadataFormat) {
        List<String> errors;

        System.out.print("Testing basic PMH access:  ");
        errors = harvestedCollectionService.verifyOAIharvester(server, set,
                                                 (null != metadataFormat) ? metadataFormat : "dc", false);
        if (errors.isEmpty()) {
            System.out.println("OK");
        } else {
            for (String error : errors) {
                System.err.println(error);
            }
        }

        System.out.print("Testing ORE support:  ");
        errors = harvestedCollectionService.verifyOAIharvester(server, set,
                                                 (null != metadataFormat) ? metadataFormat : "dc", true);
        if (errors.isEmpty()) {
            System.out.println("OK");
        } else {
            for (String error : errors) {
                System.err.println(error);
            }
        }
    }

    private static void logProcess(UUID processId, HarvestedCollection harvestRow, boolean start, long startTimestamp)
        throws SQLException {

        Collection collection = harvestRow.getCollection();
        Community parentCommunity = (Community) collectionService.getParentObject(context, collection);

        String logMessage = new StringBuilder(LOG_PREFIX)
            .append(processId).append(LOG_DELIMITER)
            .append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date())).append(LOG_DELIMITER)
            .append(harvestRow.getOaiSource()).append(LOG_DELIMITER)
            .append(harvestRow.getOaiSetId() != null ? harvestRow.getOaiSetId() : "").append(LOG_DELIMITER)
            .append(communityService.getName(parentCommunity)).append(LOG_DELIMITER)
            .append(collection.getID()).append(LOG_DELIMITER)
            .append(collectionService.getName(collection)).append(LOG_DELIMITER)
            .append(start ? "START" : "FINISH").append(LOG_DELIMITER)
            .append(start ? 0 : System.currentTimeMillis() - startTimestamp)
            .toString();

        LOGGER.trace(logMessage);
    }
}
