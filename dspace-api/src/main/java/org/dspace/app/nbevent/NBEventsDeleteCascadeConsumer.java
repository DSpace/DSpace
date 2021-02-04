/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.nbevent;

import org.dspace.app.nbevent.service.NBEventService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;

/**
 * Consumer to delete nbevents once the target item is deleted
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class NBEventsDeleteCascadeConsumer implements Consumer {

    private NBEventService nbEventService;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize() throws Exception {
        nbEventService = new DSpace().getSingletonService(NBEventService.class);
    }

    @Override
    public void finish(Context context) throws Exception {

    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        if (event.getEventType() == Event.DELETE) {
            if (event.getSubjectType() == Constants.ITEM && event.getSubjectID() != null) {
                nbEventService.deleteEventsByTargetId(context, event.getSubjectID());
            }
        }
    }

    public void end(Context context) throws Exception {
    }

}
