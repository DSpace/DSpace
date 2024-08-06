/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.qaevent;

import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.utils.DSpace;

/**
 * Consumer to delete qaevents from solr due to the target item deletion
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class QAEventsDeleteCascadeConsumer implements Consumer {

    private QAEventService qaEventService;

    @Override
    public void initialize() throws Exception {
        qaEventService = new DSpace().getSingletonService(QAEventService.class);
    }

    @Override
    public void finish(Context context) throws Exception {

    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        if (event.getEventType() == Event.DELETE) {
            if (event.getSubjectType() == Constants.ITEM && event.getSubjectID() != null) {
                qaEventService.deleteEventsByTargetId(event.getSubjectID());
            }
        }
    }

    public void end(Context context) throws Exception {
    }

}
