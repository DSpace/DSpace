/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.audit;

import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/**
 * Class to store all received events in an audit log
 * 
 * @version $Revision$
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

public class AuditConsumer implements Consumer {

    private AuditService auditService;
    private ConfigurationService configurationService;

    public void initialize() throws Exception {
        DSpace dSpace = new DSpace();
        auditService = dSpace.getSingletonService(AuditService.class);
        configurationService = dSpace.getConfigurationService();
    }

    /**
     * Consume a content event
     * 
     * @param ctx   DSpace context
     * @param event Content event
     */
    public void consume(Context ctx, Event event) throws Exception {
        if (configurationService.getBooleanProperty("audit.enabled", false)) {
            auditService.store(ctx, event);
        }
    }

    public void end(Context ctx) throws Exception {
        // No-op
    }

    public void finish(Context ctx) throws Exception {
        // No-op
    }

}
