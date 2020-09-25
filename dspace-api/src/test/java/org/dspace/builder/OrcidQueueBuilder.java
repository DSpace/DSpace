/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Builder to construct OrcidQueue objects
 *
 * @author Mykhaylo Boychuk (4science)
 */
public class OrcidQueueBuilder extends  AbstractBuilder<OrcidQueue, OrcidQueueService> {

    private static final Logger log = Logger.getLogger(OrcidQueueBuilder.class);

    private OrcidQueue orcidQueue;

    protected OrcidQueueBuilder(Context context) {
        super(context);
    }

    @Override
    protected OrcidQueueService getService() {
        return orcidQueueService;
    }

    @Override
    public void cleanup() throws Exception {
        delete(orcidQueue);
    }

    public static OrcidQueueBuilder createOrcidQueue(Context context, Item owner, Item entity) {
        OrcidQueueBuilder builder = new OrcidQueueBuilder(context);
        return builder.create(context, owner, entity);
    }

    private OrcidQueueBuilder create(Context context, Item owner, Item entity) {
        try {
            this.context = context;
            this.orcidQueue = getService().create(context, owner, entity);
        } catch (Exception e) {
            log.error("Error in OrcidQueueBuilder.create(..), error: ", e);
        }
        return this;
    }

    @Override
    public OrcidQueue build() throws SQLException, AuthorizeException {
        try {
            getService().update(context, orcidQueue);
            context.dispatchEvents();

            indexingService.commit();
        } catch (Exception e) {
            log.error("Error in OrcidQueueBuilder.build(), error: ", e);
        }
        return orcidQueue;
    }

    @Override
    public void delete(Context c, OrcidQueue orcidQueue) throws Exception {
        if (orcidQueue != null) {
            getService().delete(c, orcidQueue);
        }
    }

    public void delete(OrcidQueue orcidQueue) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            OrcidQueue attachedTab = c.reloadEntity(orcidQueue);
            if (attachedTab != null) {
                getService().delete(c, attachedTab);
            }
            c.complete();
        }
        indexingService.commit();
    }

}
