package org.dspace.submit;

import org.apache.logging.log4j.Logger;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.submit.factory.SubmissionServiceFactory;

public class ItemSubmissionConsumer implements Consumer {
    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemSubmissionConsumer.class);
    
    @Override
    public void initialize() throws Exception {
        // No-op
    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {
        int st = event.getSubjectType();
        if (!( st == Constants.COLLECTION )) {
            log
                .warn("ItemSubmissionConsumer should not have been given this kind of Subject in an event, skipping: "
                          + event.toString());
            return;
        }
                
        int et = event.getEventType();
        switch (et) {
            case Event.CREATE:
            case Event.MODIFY:
            case Event.MODIFY_METADATA:
                SubmissionServiceFactory.getInstance().getSubmissionConfigReaderService().reload();
                break;
        }
        
    }

    @Override
    public void end(Context ctx) throws Exception {
        // No-op
    }

    @Override
    public void finish(Context ctx) throws Exception {
        // No-op
    }

}
