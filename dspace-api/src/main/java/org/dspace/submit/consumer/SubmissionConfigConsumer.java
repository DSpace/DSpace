/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.consumer;

import org.apache.logging.log4j.Logger;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.submit.factory.SubmissionServiceFactory;

/**
 * Consumer implementation to be used for Item Submission Configuration
 *
 * @author paulo.graca at fccn.pt
 */
public class SubmissionConfigConsumer implements Consumer {
    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(SubmissionConfigConsumer.class);

    @Override
    public void initialize() throws Exception {
        // No-op
    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {
        int st = event.getSubjectType();
        if (!( st == Constants.COLLECTION )) {
            log
                .warn("SubmissionConfigConsumer should not have been given this kind of Subject in an event, skipping: "
                          + event.toString());
            return;
        }

        int et = event.getEventType();
        switch (et) {
            case Event.CREATE:
            case Event.DELETE:
            case Event.MODIFY:
            case Event.MODIFY_METADATA:
            default:
                // reload submission configurations
                SubmissionServiceFactory.getInstance().getSubmissionConfigService().reload();
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
