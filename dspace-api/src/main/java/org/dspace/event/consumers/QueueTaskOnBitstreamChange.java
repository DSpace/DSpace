/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event.consumers;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Event;

/**
 * Event consumer that queues curation tasks when an item's bitstreams are changed in any way.
 *
 * Event consumers cannot make changes to their subject. Consequently, whenever a change to the item is desired on
 * installation, this needs to be performed by a curation task.
 *
 * This consumer queues up the curation tasks given in the configuration file via queue.bitstreamchange.tasks
 * (comma separated list) into the task queue named in the configuration file via queue.bitstreamchange.name
 * (default: continually). This task queue should then be run regularly and quite often, eg using a cronjob that runs
 * every minute or two.
 *
 * @author Andrea Schweer schweer@waikato.ac.nz for the LCoNZ IRRs
 * @author Kim Shepherd kim@shepherd.nz update and refactor for DSpace 6.x generic usage
 *
 */
public class QueueTaskOnBitstreamChange extends QueueTaskOnEvent {

    private static final Logger log = LogManager.getLogger(QueueTaskOnBitstreamChange.class);

    /**
     * Return the current object if it is already an item, or the owning item of the current subject.
     *
     * @param ctx DSpace context
     * @param event Dispatched event
     * @return subject itself or its parent item
     * @throws SQLException
     */
    @Override
    Item findItem(Context ctx, Event event) throws SQLException {
        if (event.getSubject(ctx) == null) {
            return null;
        }

        if (event.getSubjectType() == Constants.ITEM) {
            return (Item) event.getSubject(ctx);
        }

        Item result = null;

        // Get parent object and return it as item, if appropriate
        DSpaceObject parent = ContentServiceFactory.getInstance()
                .getDSpaceObjectService(event.getSubject(ctx)).getParentObject(ctx, event.getSubject(ctx));

        if (parent instanceof Item) {
            log.debug("Found item parent");
            result = (Item) parent;
        }
        if (result != null && result.isArchived()) {
            return result;
        }

        log.debug("End of method, returning null");
        return null;
    }

    /**
     * Return an indicator as to whether this event applies to the intended usage by the consumer
     * @param ctx DSpace context
     * @param event Dispatched event
     * @return
     * @throws SQLException
     */
    @Override
    boolean isApplicableEvent(Context ctx, Event event) throws SQLException {
        // Get bundles to ignore
        String[] ignoreBundlesProp = configurationService.getArrayProperty(
                "custom-events.queue.bitstreamchange.ignore_bundles", new String[]{"TEXT", "THUMBNAIL"});
        List<String> ignoreBundles = Arrays.asList(ignoreBundlesProp);

        // Return eligibility. If this is an ADD or REMOVE to a bundle, and the bundle name isn't in the 'ignore' list
        // or the event is REMOVE and subject is an Item, and we can resolve the current subject to a parent item,
        // or itself, then this is eligible.
        int eventType = event.getEventType();
        boolean eligible =
                (event.getSubjectType() == Constants.BUNDLE && (eventType == Event.ADD || eventType == Event.REMOVE))
                        || (event.getSubjectType() == Constants.ITEM && eventType == Event.REMOVE)
                        && (findItem(ctx, event) != null);
        if (!eligible || event.getSubjectType() != Constants.BUNDLE) {
            return eligible;
        }
        Bundle subject = (Bundle) event.getSubject(ctx);
        return !ignoreBundles.contains(subject.getName());
    }

    String getTasksProperty() {
        return "custom-events.queue.bitstreamchange.tasks";
    }

    String getQueueProperty() {
        return "custom-events.queue.bitstreamchange.name";
    }
}
