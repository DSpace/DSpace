/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.service.HarvestedCollectionService;

import java.sql.SQLException;
import java.util.UUID;

/**
 * A harvester thread used to execute a single harvest cycle on a collection
 * @author alexey
 */
public class HarvestThread extends Thread {

    private static final Logger log = Logger.getLogger(HarvestThread.class);
    protected UUID collectionId;
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected HarvestedCollectionService harvestedCollectionService = HarvestServiceFactory.getInstance().getHarvestedCollectionService();


    protected HarvestThread(UUID collectionId) throws SQLException {
        this.collectionId = collectionId;
    }

    @Override
    public void run()
    {
        log.info("Thread for collection " + collectionId + " starts.");
        runHarvest();
    }

    private void runHarvest()
    {
        Context context;
        Collection dso;
        HarvestedCollection hc = null;
        try {
            context = new Context();
            dso = collectionService.find(context, collectionId);
            hc = harvestedCollectionService.find(context, dso);
            try {

                dso = hc.getCollection();
                OAIHarvester harvester = new OAIHarvester(context, dso, hc);
                harvester.runHarvest();
            } catch (RuntimeException e) {
                log.error("Runtime exception in thread: " + this.toString());
                log.error(e.getMessage() + " " + e.getCause());
                hc.setHarvestMessage("Runtime error occured while generating an OAI response");
                hc.setHarvestStatus(HarvestedCollection.STATUS_UNKNOWN_ERROR);
            } catch (Exception ex) {
                log.error("General exception in thread: " + this.toString());
                log.error(ex.getMessage() + " " + ex.getCause());
                hc.setHarvestMessage("Error occured while generating an OAI response");
                hc.setHarvestStatus(HarvestedCollection.STATUS_UNKNOWN_ERROR);
            } finally {
                try {
                    harvestedCollectionService.update(context, hc);
                    context.restoreAuthSystemState();
                    context.complete();
                } catch (RuntimeException e) {
                    log.error("Unexpected exception while recovering from a harvesting error: " + e.getMessage(), e);
                    context.abort();
                } catch (Exception e) {
                    log.error("Unexpected exception while recovering from a harvesting error: " + e.getMessage(), e);
                    context.abort();
                }

                synchronized (HarvestScheduler.class) {
                    HarvestScheduler.activeThreads--;
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        log.info("Thread for collection " + hc.getCollection().getID() + " completes.");
    }
}