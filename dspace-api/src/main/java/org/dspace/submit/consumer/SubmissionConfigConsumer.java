/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.consumer;

import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.factory.DSpaceServicesFactory;
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

    IndexingService indexer = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName(IndexingService.class.getName(),
                              IndexingService.class);

    @Override
    public void initialize() throws Exception {
        // No-op
    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {
        int st = event.getSubjectType();
        int et = event.getEventType();


        if ( st == Constants.COLLECTION ) {
            switch (et) {
                case Event.MODIFY_METADATA:
                    // Submission configuration it's based on solr
                    // for collection's entity type but, at this point
                    // that info isn't indexed yet, we need to force it
                    DSpaceObject subject = event.getSubject(ctx);
                    Collection collectionFromDSOSubject = (Collection) subject;
                    indexer.indexContent(ctx, new IndexableCollection (collectionFromDSOSubject), true, false, false);
                    indexer.commit();

                    log.debug("SubmissionConfigConsumer occured: " + event.toString());
                    // reload submission configurations
                    SubmissionServiceFactory.getInstance().getSubmissionConfigService().reload();
                    break;

                default:
                    log.debug("SubmissionConfigConsumer occured: " + event.toString());
                    // reload submission configurations
                    SubmissionServiceFactory.getInstance().getSubmissionConfigService().reload();
                    break;
            }
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
