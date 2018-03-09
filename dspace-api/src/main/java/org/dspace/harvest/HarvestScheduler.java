/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.service.HarvestedCollectionService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * The class responsible for scheduling harvesting cycles are regular intervals.
 * @author alexey
 */
public class HarvestScheduler implements Runnable
{
    protected static Logger log = Logger.getLogger(HarvestScheduler.class);


    protected static EPerson harvestAdmin;

    protected Context mainContext;

    public static final Object lock = new Object();

    protected static Stack<HarvestThread> harvestThreads;

    protected static Integer maxActiveThreads;

    protected static volatile Integer activeThreads = 0;

    public static final int HARVESTER_STATUS_RUNNING = 1;

    public static final int HARVESTER_STATUS_SLEEPING = 2;

    public static final int HARVESTER_STATUS_PAUSED = 3;

    public static final int HARVESTER_STATUS_STOPPED = 4;

    public static final int HARVESTER_INTERRUPT_NONE = 0;

    public static final int HARVESTER_INTERRUPT_PAUSE = 1;

    public static final int HARVESTER_INTERRUPT_STOP = 2;

    public static final int HARVESTER_INTERRUPT_RESUME = 3;

    public static final int HARVESTER_INTERRUPT_INSERT_THREAD = 4;

    public static final int HARVESTER_INTERRUPT_KILL_THREAD = 5;

    protected static int status = HARVESTER_STATUS_STOPPED;

    private static int interrupt = HARVESTER_INTERRUPT_NONE;

    protected static UUID interruptValue = null;

    protected static long minHeartbeat;

    protected static long maxHeartbeat;

    private static final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private static final HarvestedCollectionService harvestedCollectionService = HarvestServiceFactory.getInstance().getHarvestedCollectionService();

    public static boolean hasStatus(int statusToCheck) {
        return status == statusToCheck;
    }

    public static synchronized void setInterrupt(int newInterrupt) {
        interrupt = newInterrupt;
    }

    public static synchronized void setInterrupt(int newInterrupt, UUID newInterruptValue) {
        interrupt = newInterrupt;
        interruptValue = newInterruptValue;
    }

    public static int getInterrupt() {
        return interrupt;
    }

    public static String getStatus() {
        switch(status) {
        case HARVESTER_STATUS_RUNNING:
            switch(interrupt) {
            case HARVESTER_INTERRUPT_PAUSE: return("The scheduler is finishing active harvests before pausing. ");
            case HARVESTER_INTERRUPT_STOP: return("The scheduler is shutting down. ");
            }
            return("The scheduler is actively harvesting collections. ");
        case HARVESTER_STATUS_SLEEPING: return("The scheduler is waiting for collections to harvest. ");
        case HARVESTER_STATUS_PAUSED: return("The scheduler is paused. ");
        default: return("Automatic harvesting is not active. ");
        }
    }

    public HarvestScheduler() throws SQLException, AuthorizeException {
        mainContext = new Context();
        String harvestAdminParam = ConfigurationManager.getProperty("oai", "harvester.eperson");
        harvestAdmin = null;
        if (harvestAdminParam != null && harvestAdminParam.length() > 0)
        {
            harvestAdmin = EPersonServiceFactory.getInstance().getEPersonService().findByEmail(mainContext, harvestAdminParam);
        }

        harvestThreads = new Stack<HarvestThread>();

        maxActiveThreads = ConfigurationManager.getIntProperty("oai", "harvester.maxThreads");
        if (maxActiveThreads == 0)
        {
            maxActiveThreads = 3;
        }
        minHeartbeat = ConfigurationManager.getIntProperty("oai", "harvester.minHeartbeat") * 1000;
        if (minHeartbeat == 0)
        {
            minHeartbeat = 30000;
        }
        maxHeartbeat = ConfigurationManager.getIntProperty("oai", "harvester.maxHeartbeat") * 1000;
        if (maxHeartbeat == 0)
        {
            maxHeartbeat = 3600000;
        }
    }

    @Override
    public void run() {
        scheduleLoop();
    }

    protected void scheduleLoop() {
        long i=0;
        while(true)
        {
            try
            {
                mainContext = new Context();

                synchronized (HarvestScheduler.class) {
                    switch (interrupt) {
                        case HARVESTER_INTERRUPT_NONE:
                            break;
                        case HARVESTER_INTERRUPT_INSERT_THREAD:
                            interrupt = HARVESTER_INTERRUPT_NONE;
                            addThread(mainContext, harvestedCollectionService.find(mainContext, collectionService.find(mainContext, interruptValue)));
                            interruptValue = null;
                            break;
                        case HARVESTER_INTERRUPT_PAUSE:
                            interrupt = HARVESTER_INTERRUPT_NONE;
                            status = HARVESTER_STATUS_PAUSED;
                            break;
                        case HARVESTER_INTERRUPT_STOP:
                            interrupt = HARVESTER_INTERRUPT_NONE;
                            status = HARVESTER_STATUS_STOPPED;
                            return;
                    }
                }

                if (status == HARVESTER_STATUS_PAUSED) {
                    while(interrupt != HARVESTER_INTERRUPT_RESUME && interrupt != HARVESTER_INTERRUPT_STOP) {
                        Thread.sleep(1000);
                    }

                    if (interrupt != HARVESTER_INTERRUPT_STOP) {
                        break;
                    }
                }

                status = HARVESTER_STATUS_RUNNING;

                // Stage #1: if something is ready for harvest, push it onto the ready stack, mark it as "queued"
                List<HarvestedCollection> cids = harvestedCollectionService.findReady(mainContext);
                log.info("Collections ready for immediate harvest: " + cids.toString());

                for (HarvestedCollection harvestedCollection : cids) {
                    addThread(mainContext, harvestedCollection);
                }

                // Stage #2: start up all the threads currently in the queue up to the maximum number
                while (!harvestThreads.isEmpty()) {
                    synchronized(HarvestScheduler.class) {
                        activeThreads++;
                    }
                    Thread activeThread = new Thread(harvestThreads.pop());
                    activeThread.start();
                    log.info("Thread started: " + activeThread.toString());

                    /* Wait while the number of threads running is greater than or equal to max */
                    while (activeThreads >= maxActiveThreads) {
                        /* Wait a second */
                        Thread.sleep(1000);
                    }
                }

                // Finally, wait for the last few remaining threads to finish
                // TODO: this step might be unnecessary. Theoretically a single very long harvest process
                // could then lock out all the other ones from starting on their next iteration.
                // FIXME: also, this might lead to a situation when a single thread getting stuck without
                // throwing an exception would shut down the whole scheduler
                while (activeThreads != 0) {
                        /* Wait a second */
                        Thread.sleep(1000);
                }

                // Commit everything
                try {
                        mainContext.complete();
                        log.info("Done with iteration " + i);
                } catch (SQLException e) {
                        e.printStackTrace();
                        mainContext.abort();
                }

            }
            catch (Exception e) {
                    log.error("Exception on iteration: " + i);
                    e.printStackTrace();
            }

            // Stage #3: figure out how long until the next iteration and wait
            try {
                Context tempContext = new Context();
                HarvestedCollection hc = harvestedCollectionService.findOldestHarvest(tempContext);

                int harvestInterval = ConfigurationManager.getIntProperty("oai", "harvester.harvestFrequency");
                if (harvestInterval == 0)
                {
                    harvestInterval = 720;
                }

                Date nextTime;
                long nextHarvest = 0;
                if (hc != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(hc.getHarvestDate());
                    calendar.add(Calendar.MINUTE, harvestInterval);
                    nextTime = calendar.getTime();
                    nextHarvest = nextTime.getTime() +  - new Date().getTime();
                }

                long upperBound = Math.min(nextHarvest,maxHeartbeat);
                long delay = Math.max(upperBound, minHeartbeat) + 1000;


                tempContext.complete();

                status = HARVESTER_STATUS_SLEEPING;
                synchronized(lock) {
                    lock.wait(delay);
                }
            }
            catch (InterruptedException ie) {
                    log.warn("Interrupt: " + ie.getMessage());
            }
            catch (SQLException e) {
                    e.printStackTrace();
            }

            i++;
        }
    }


    /**
     * Adds a thread to the ready stack. Can also be called externally to queue up a collection
     * for harvesting before it is "due" for another cycle. This allows starting a harvest process
     * from the UI that still "plays nice" with these thread mechanics instead of making an
     * asynchronous call to runHarvest().
     */
    public void addThread(Context context, HarvestedCollection harvestedCollection) throws SQLException, IOException, AuthorizeException {
        log.debug("****** Entered the addThread method. Active threads: " + harvestThreads.toString());
        context.setCurrentUser(harvestAdmin);

        harvestedCollection.setHarvestStatus(HarvestedCollection.STATUS_QUEUED);
        harvestedCollectionService.update(context, harvestedCollection);
        context.dispatchEvents();

        HarvestThread ht = new HarvestThread(harvestedCollection.getCollection().getID());
        harvestThreads.push(ht);

        log.debug("****** Queued up a thread. Active threads: " + harvestThreads.toString());
        log.info("Thread queued up: " + ht.toString());
    }


}
