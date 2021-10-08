/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.harvest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
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
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.HarvestingException;
import org.dspace.harvest.OAIHarvester;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * Test class for harvested collections.
 *
 * @author Alexey Maslov
 */
public class Harvest extends DSpaceRunnable<HarvestScriptConfiguration> {

    private HarvestedCollectionService harvestedCollectionService;
    protected EPersonService ePersonService;
    private CollectionService collectionService;

    private boolean help;
    private String command = null;
    private String collection = null;
    private String oaiSource = null;
    private String oaiSetID = null;
    private String metadataKey = null;
    private int harvestType = 0;

    protected Context context;


    public HarvestScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
                           .getServiceByName("harvest", HarvestScriptConfiguration.class);
    }

    public void setup() throws ParseException {
        harvestedCollectionService =
                HarvestServiceFactory.getInstance().getHarvestedCollectionService();
        ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        collectionService =
                ContentServiceFactory.getInstance().getCollectionService();

        assignCurrentUserInContext();

        help = commandLine.hasOption('h');


        if (commandLine.hasOption('s')) {
            command = "config";
        }
        if (commandLine.hasOption('p')) {
            command = "purge";
        }
        if (commandLine.hasOption('r')) {
            command = "run";
        }
        if (commandLine.hasOption('g')) {
            command = "ping";
        }
        if (commandLine.hasOption('S')) {
            command = "start";
        }
        if (commandLine.hasOption('R')) {
            command = "reset";
        }
        if (commandLine.hasOption('P')) {
            command = "purgeAll";
        }
        if (commandLine.hasOption('o')) {
            command = "reimport";
        }
        if (commandLine.hasOption('c')) {
            collection = commandLine.getOptionValue('c');
        }
        if (commandLine.hasOption('t')) {
            harvestType = Integer.parseInt(commandLine.getOptionValue('t'));
        } else {
            harvestType = 0;
        }
        if (commandLine.hasOption('a')) {
            oaiSource = commandLine.getOptionValue('a');
        }
        if (commandLine.hasOption('i')) {
            oaiSetID = commandLine.getOptionValue('i');
        }
        if (commandLine.hasOption('m')) {
            metadataKey = commandLine.getOptionValue('m');
        }
    }

    /**
     * This method will assign the currentUser to the {@link Context} variable which is also created in this method.
     * The instance of the method in this class will fetch the EPersonIdentifier from this class, this identifier
     * was given to this class upon instantiation, it'll then be used to find the {@link EPerson} associated with it
     * and this {@link EPerson} will be set as the currentUser of the created {@link Context}
     * @throws ParseException If something went wrong with the retrieval of the EPerson Identifier
     */
    protected void assignCurrentUserInContext() throws ParseException {
        UUID currentUserUuid = this.getEpersonIdentifier();
        try {
            this.context = new Context(Context.Mode.BATCH_EDIT);
            EPerson eperson = ePersonService.find(context, currentUserUuid);
            if (eperson == null) {
                super.handler.logError("EPerson not found: " + currentUserUuid);
                throw new IllegalArgumentException("Unable to find a user with uuid: " + currentUserUuid);
            }
            this.context.setCurrentUser(eperson);
        } catch (SQLException e) {
            handler.handleException("Something went wrong trying to fetch eperson for uuid: " + currentUserUuid, e);
        }
    }

    public void internalRun() throws Exception {
        if (help) {
            printHelp();
            handler.logInfo("PING OAI server: Harvest -g -a oai_source -i oai_set_id");
            handler.logInfo(
                    "SETUP a collection for harvesting: Harvest -s -c collection -t harvest_type -a oai_source -i " +
                            "oai_set_id -m metadata_format");
            handler.logInfo("RUN harvest once: Harvest -r -e eperson -c collection");
            handler.logInfo("START harvest scheduler: Harvest -S");
            handler.logInfo("RESET all harvest status: Harvest -R");
            handler.logInfo("PURGE a collection of items and settings: Harvest -p -e eperson -c collection");
            handler.logInfo("PURGE all harvestable collections: Harvest -P -e eperson");

            return;
        }

        if (StringUtils.isBlank(command)) {
            handler.logError("No parameters specified (run with -h flag for details)");
            throw new UnsupportedOperationException("No command specified");
        } else if ("run".equals(command)) {
            // Run a single harvest cycle on a collection using saved settings.
            if (collection == null || context.getCurrentUser() == null) {
                handler.logError("A target collection and eperson must be provided (run with -h flag for details)");
                throw new UnsupportedOperationException("A target collection and eperson must be provided");
            }
            runHarvest(context, collection);
        } else if ("start".equals(command)) {
            // start the harvest loop
            startHarvester();
        } else if ("reset".equals(command)) {
            // reset harvesting status
            resetHarvesting(context);
        } else if ("purgeAll".equals(command)) {
            // purge all collections that are set up for harvesting (obviously for testing purposes only)
            if (context.getCurrentUser() == null) {
                handler.logError("An eperson must be provided (run with -h flag for details)");
                throw new UnsupportedOperationException("An eperson must be provided");
            }

            List<HarvestedCollection> harvestedCollections = harvestedCollectionService.findAll(context);
            for (HarvestedCollection harvestedCollection : harvestedCollections) {
                handler.logInfo(
                        "Purging the following collections (deleting items and resetting harvest status): " +
                                harvestedCollection
                                        .getCollection().getID().toString());
                purgeCollection(context, harvestedCollection.getCollection().getID().toString());
            }
            context.complete();
        } else if ("purge".equals(command)) {
            // Delete all items in a collection. Useful for testing fresh harvests.
            if (collection == null || context.getCurrentUser() == null) {
                handler.logError("A target collection and eperson must be provided (run with -h flag for details)");
                throw new UnsupportedOperationException("A target collection and eperson must be provided");
            }

            purgeCollection(context, collection);
            context.complete();

        } else if ("reimport".equals(command)) {
            // Delete all items in a collection. Useful for testing fresh harvests.
            if (collection == null || context.getCurrentUser() == null) {
                handler.logError("A target collection and eperson must be provided (run with -h flag for details)");
                throw new UnsupportedOperationException("A target collection and eperson must be provided");
            }
            purgeCollection(context, collection);
            runHarvest(context, collection);
            context.complete();

        } else if ("config".equals(command)) {
            // Configure a collection with the three main settings
            if (collection == null) {
                handler.logError("A target collection must be provided (run with -h flag for details)");
                throw new UnsupportedOperationException("A target collection must be provided");
            }
            if (oaiSource == null || oaiSetID == null) {
                handler.logError(
                        "Both the OAI server address and OAI set id must be specified (run with -h flag for details)");
                throw new UnsupportedOperationException("Both the OAI server address and OAI set id must be specified");
            }
            if (metadataKey == null) {
                handler.logError(
                        "A metadata key (commonly the prefix) must be specified for this collection (run with -h flag" +
                                " for details)");
                throw new UnsupportedOperationException(
                        "A metadata key (commonly the prefix) must be specified for this collection");
            }

            configureCollection(context, collection, harvestType, oaiSource, oaiSetID, metadataKey);
        } else if ("ping".equals(command)) {
            if (oaiSource == null || oaiSetID == null) {
                handler.logError(
                        "Both the OAI server address and OAI set id must be specified  (run with -h flag for details)");
                throw new UnsupportedOperationException("Both the OAI server address and OAI set id must be specified");
            }

            pingResponder(oaiSource, oaiSetID, metadataKey);
        } else {
            handler.logError(
                    "Your command '" + command + "' was not recognized properly (run with -h flag for details)");
            throw new UnsupportedOperationException("Your command '" + command + "' was not recognized properly");
        }


    }

    /*
     * Resolve the ID into a collection and check to see if its harvesting options are set. If so, return
     * the collection, if not, bail out.
     */
    private Collection resolveCollection(Context context, String collectionID) {

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
                    handler.logInfo("Looking up by UUID: " + collectionID + ", " + "in context: " + context);
                    targetCollection = collectionService.find(context, UUID.fromString(collectionID));
                }
            }
            // was the collection valid?
            if (targetCollection == null) {
                handler.logError("Cannot resolve " + collectionID + " to collection");
                throw new UnsupportedOperationException("Cannot resolve " + collectionID + " to collection");
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }

        return targetCollection;
    }


    private void configureCollection(Context context, String collectionID, int type, String oaiSource, String oaiSetId,
                                     String mdConfigId) {
        handler.logInfo("Running: configure collection");

        Collection collection = resolveCollection(context, collectionID);
        handler.logInfo(String.valueOf(collection.getID()));

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
            handler.logError("Changes could not be committed");
            handler.handleException(e);
        } finally {
            if (context != null) {
                context.restoreAuthSystemState();
            }
        }
    }


    /**
     * Purges a collection of all harvest-related data and settings. All items in the collection will be deleted.
     *  @param collectionID
     *
     */
    private void purgeCollection(Context context, String collectionID) {
        handler.logInfo(
                "Purging collection of all items and resetting last_harvested and harvest_message: " + collectionID);
        Collection collection = resolveCollection(context, collectionID);

        try {
            context.turnOffAuthorisationSystem();

            ItemService itemService = ContentServiceFactory.getInstance().getItemService();
            Iterator<Item> it = itemService.findByCollection(context, collection);
            int i = 0;
            while (it.hasNext()) {
                i++;
                Item item = it.next();
                handler.logInfo("Deleting: " + item.getHandle());
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
            handler.logError("Changes could not be committed");
            handler.handleException(e);
        } finally {
            context.restoreAuthSystemState();
        }
    }


    /**
     * Run a single harvest cycle on the specified collection under the authorization of the supplied EPerson
     */
    private void runHarvest(Context context, String collectionID) {
        handler.logInfo("Running: a harvest cycle on " + collectionID);

        handler.logInfo("Initializing the harvester... ");
        OAIHarvester harvester = null;
        try {
            Collection collection = resolveCollection(context, collectionID);
            HarvestedCollection hc = harvestedCollectionService.find(context, collection);
            harvester = new OAIHarvester(context, collection, hc);
            handler.logInfo("Initialized the harvester successfully");
        } catch (HarvestingException hex) {
            handler.logError("Initializing the harvester failed.");
            throw new IllegalStateException("Unable to harvest", hex);
        } catch (SQLException se) {
            handler.logError("Initializing the harvester failed.");
            throw new IllegalStateException("Unable to access database", se);
        }

        try {
            // Harvest will not work for an anonymous user
            handler.logInfo("Harvest started... ");
            harvester.runHarvest();
            context.complete();
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new IllegalStateException("Failed to run harvester", e);
        }

        handler.logInfo("Harvest complete. ");
    }

    /**
     * Resets harvest_status and harvest_start_time flags for all collections that have a row in the
     * harvested_collections table
     */
    private void resetHarvesting(Context context) {
        handler.logInfo("Resetting harvest status flag on all collections... ");

        try {
            List<HarvestedCollection> harvestedCollections = harvestedCollectionService.findAll(context);
            for (HarvestedCollection harvestedCollection : harvestedCollections) {
                //hc.setHarvestResult(null,"");
                harvestedCollection.setHarvestStartTime(null);
                harvestedCollection.setHarvestStatus(HarvestedCollection.STATUS_READY);
                harvestedCollectionService.update(context, harvestedCollection);
            }
            handler.logInfo("Reset harvest status flag successfully");
        } catch (Exception ex) {
            handler.logError("Resetting harvest status flag failed");
            handler.handleException(ex);
        }
    }

    /**
     * Starts up the harvest scheduler. Terminating this process will stop the scheduler.
     */
    private void startHarvester() {
        try {
            handler.logInfo("Starting harvest loop... ");
            HarvestServiceFactory.getInstance().getHarvestSchedulingService().startNewScheduler();
            handler.logInfo("running. ");
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
    private void pingResponder(String server, String set, String metadataFormat) {
        List<String> errors;

        handler.logInfo("Testing basic PMH access:  ");
        errors = harvestedCollectionService.verifyOAIharvester(server, set,
                                                               (null != metadataFormat) ? metadataFormat : "dc", false);
        if (errors.isEmpty()) {
            handler.logInfo("OK");
        } else {
            for (String error : errors) {
                handler.logError(error);
            }
        }

        handler.logInfo("Testing ORE support:  ");
        errors = harvestedCollectionService.verifyOAIharvester(server, set,
                                                               (null != metadataFormat) ? metadataFormat : "dc", true);
        if (errors.isEmpty()) {
            handler.logInfo("OK");
        } else {
            for (String error : errors) {
                handler.logError(error);
            }
        }
    }


}
