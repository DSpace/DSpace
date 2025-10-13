/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.audit;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/**
 * Class to store all received events in an audit log with batch commit optimization
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */

public class AuditConsumer implements Consumer {
    private AuditSolrServiceImpl auditSolrService;
    private ConfigurationService configurationService;
    private List<Integer> meaningfulEvents;

    /**
     * Thread-safe counter for pending operations before commit
     */
    private final AtomicInteger pendingOperations = new AtomicInteger(0);

    /**
     * Batch size for commits, configurable via audit.commit.batch.size property
     */
    private int commitBatchSize;

    public void initialize() throws Exception {
        DSpace dSpace = new DSpace();
        auditSolrService = dSpace.getSingletonService(AuditSolrServiceImpl.class);
        configurationService = dSpace.getConfigurationService();
        meaningfulEvents = List.of(Event.MODIFY_METADATA, Event.CREATE, Event.DELETE,
            Event.REMOVE);

        // Initialize batch size from configuration, default to 50 if not specified
        commitBatchSize = configurationService.getIntProperty("audit.commit.batch.size", 50);
    }

    /**
     * Consume a content event
     * 
     * @param ctx   DSpace context
     * @param event Content event
     */
    public void consume(Context ctx, Event event) throws Exception {
        if (configurationService.getBooleanProperty("audit.enabled", false)
            && isEventMeaningful(event)) {
            auditSolrService.store(ctx, event); // AuditSolrService also handles detailed event logging

            // Increment counter and check if we need to commit
            int currentCount = pendingOperations.incrementAndGet();
            if (currentCount >= commitBatchSize) {
                // Use compareAndSet to atomically reset counter only if it's still >= batchSize
                if (pendingOperations.compareAndSet(currentCount, 0)) {
                    auditSolrService.commit();
                }
            }
        }
    }

    private boolean isEventMeaningful(Event event) {
        if (meaningfulEvents.contains(event.getEventType())) {
            return true;
        }
        UUID relatedObjectId = event.getObjectID();
        return relatedObjectId != null;
    }

    public void end(Context ctx) throws Exception {
        if (configurationService.getBooleanProperty("audit.enabled", false)) {
            // Always commit any pending operations at the end
            int remaining = pendingOperations.getAndSet(0);
            if (remaining > 0) {
                auditSolrService.commit();
            }
        }
    }

    public void finish(Context ctx) throws Exception {
        // No-op
    }

}
