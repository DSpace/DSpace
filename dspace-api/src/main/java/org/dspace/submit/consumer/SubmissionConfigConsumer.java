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
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SubmissionConfigConsumer.class);

    private boolean reloadNeeded = false;

    @Override
    public void initialize() throws Exception {
        // No-op
    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {
        int st = event.getSubjectType();

        if (st == Constants.COLLECTION) {
            // NOTE: IndexEventConsumer ("discovery") should be declared before this consumer
            // We don't reindex the collection because it will normally be reindexed by IndexEventConsumer
            // before the submission configurations are reloaded

            log.debug("SubmissionConfigConsumer occurred: " + event);
            // submission configurations should be reloaded
            reloadNeeded = true;
        }
    }

    @Override
    public void end(Context ctx) throws Exception {
        if (reloadNeeded) {
            // reload submission configurations
            SubmissionServiceFactory.getInstance().getSubmissionConfigService().reload();

            // Reset the boolean used
            reloadNeeded = false;
        }
    }

    @Override
    public void finish(Context ctx) throws Exception {
        // No-op
    }

}
