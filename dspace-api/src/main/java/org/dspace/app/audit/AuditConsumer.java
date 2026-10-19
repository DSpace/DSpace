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

import org.dspace.app.audit.factory.AuditServiceFactory;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Class to store all received events in the audit system, if auditing is enabled.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */

public class AuditConsumer implements Consumer {
    private AuditService auditService;
    private ConfigurationService configurationService;
    private List<Integer> meaningfulEvents;


    public void initialize() throws Exception {
        auditService = AuditServiceFactory.getInstance().getAuditService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        meaningfulEvents = List.of(Event.MODIFY_METADATA, Event.CREATE, Event.DELETE,
            Event.REMOVE);
    }

    /**
     * Consume a content event
     * 
     * @param ctx   DSpace context
     * @param event Content event
     */
    @Override
    public void consume(Context ctx, Event event) throws Exception {
        if (configurationService.getBooleanProperty("audit.enabled", false)
            && isEventMeaningful(event)) {
            auditService.store(ctx, event); // AuditService also handles detailed event logging
        }
    }

    /**
     * Checks if the given event is meaningful for audit purposes.
     * An event is considered meaningful if its type is present in the meaningfulEvents list,
     * or if it has a non-null related object ID.
     * Some events, may not be in the meaningfulEvents list, eighter because they contain
     * duplicated information or because they are not relevant for auditing.
     *
     * @param event the event to check
     * @return true if the event is meaningful, false otherwise
     */
    private boolean isEventMeaningful(Event event) {
        if (meaningfulEvents.contains(event.getEventType())) {
            return true;
        }
        UUID relatedObjectId = event.getObjectID();
        return relatedObjectId != null;
    }

    @Override
    public void end(Context ctx) throws Exception {
        // no-op
    }

    @Override
    public void finish(Context ctx) throws Exception {
        // No-op
    }

}
