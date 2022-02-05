/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Helper class to deal with curation tasks.
 *
 * @author Andrea Schweer schweer@waikato.ac.nz for the LCoNZ IRRs
 * @author Kim Shepherd kim@shepherd.nz update and refactor for DSpace 6.x generic usage
 */
public class QueueManager {
    private static final Logger log = Logger.getLogger(QueueManager.class);
    private static final ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    private final List<String> taskNames = new ArrayList<String>();
    private String queueName = "continually";
    private ArrayList<Item> toQueue;

    /**
     * Retrieve queue name from configuration, for a given event consumer
     * @param queueProperty the configuration property string returned by getQueueProperty() in the consumer itself
     */
    public void initQueueName(String queueProperty) {
        String queueConfig = configurationService.getProperty(queueProperty);
        if (queueConfig != null && !"".equals(queueConfig)) {
            queueName = queueConfig;
            log.info("Using queue name " + queueName);
        } else {
            log.info("No queue name specified, using default: " + queueName);
        }
    }

    /**
     * Retrieve task names from configuration, for a given event consumer
     * @param tasksProperty the configuration property string returned by getTaskProperty() in the consumer itself
     */
    public void initTaskNames(String tasksProperty) {
        String[] taskConfig = configurationService.getArrayProperty(tasksProperty, new String[]{});
        taskNames.addAll(Arrays.asList(taskConfig));
        log.info("Setting up tasks as " + Arrays.deepToString(taskNames.toArray()));
    }

    /**
     * Add a resolved item to the list of items to queue (once the consumer has finished all other processing)
     * @param item Dspace item to queue
     */
    public void addToQueue(Item item) {
        if (toQueue == null) {
            toQueue = new ArrayList<Item>();
        }
        toQueue.add(item);
        log.info("Adding item " + item.getHandle() + " to list of items to queue");
    }

    /**
     * Take the resolved toQueue items and queue them for curation in the configured queue name
     * with the configured task name(s). This is called at the end of the consumer processing.
     * @param ctx DSpace context
     * @throws IOException
     */
    public void queueForCuration(Context ctx) throws IOException {
        if (toQueue != null && !toQueue.isEmpty()) {
            log.info("Actually queueing " + toQueue.size() + " items for curation");
            for (String taskName : taskNames) {
                Curator curator = new Curator().addTask(taskName);
                for (Item item : toQueue) {
                    String identifier;
                    if (item.getHandle() != null) {
                        identifier = item.getHandle();
                    } else {
                        identifier = item.getID() + "";
                    }
                    log.info("Queued item " + identifier + " for curation in queue "
                            + queueName + ", task " + taskName);
                    curator.queue(ctx, identifier, queueName);
                }
            }
        }
        toQueue = null;
    }

    public boolean hasTaskNames() {
        return !taskNames.isEmpty();
    }
}
