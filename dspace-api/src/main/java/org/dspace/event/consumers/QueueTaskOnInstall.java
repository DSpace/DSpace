/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event.consumers;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Event;

/**
 *
 * Event consumer that queues curation tasks when an item is installed into the archive.
 *
 * Event consumers cannot make changes to their subject. Consequently, whenever a change to the item is desired on
 * installation, this needs to be performed by a curation task.
 *
 * This consumer queues up the curation tasks given in the configuration file via queue.install.tasks
 * (comma separated list) into the task queue named in the configuration file via queue.install.name
 * (default: continually). This task queue should then be run regularly and quite often, eg using a cronjob that runs
 * every minute or two.
 *
 * @author Andrea Schweer schweer@waikato.ac.nz for the LCoNZ IRRs
 * @author Kim Shepherd kim@shepherd.nz update and refactor for DSpace 6.x generic usage
 */
public class QueueTaskOnInstall extends QueueTaskOnEvent {

    @Override
    String getTasksProperty() {
        return "custom-events.queue.install.tasks";
    }

    @Override
    String getQueueProperty() {
        return "custom-events.queue.install.name";
    }

    @Override
    boolean isApplicableEvent(Context ctx, Event event) {
        return event.getSubjectType() == Constants.ITEM && event.getEventType() == Event.INSTALL;
    }

    Item findItem(Context ctx, Event event) throws SQLException {
        return (Item) event.getSubject(ctx);
    }
}
