/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.deduplication;

import org.apache.log4j.Logger;
import org.dspace.app.cris.deduplication.service.DedupService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.core.Context;

import it.cilea.osd.common.listener.NativePostDeleteEventListener;
import it.cilea.osd.common.listener.NativePostUpdateEventListener;
import it.cilea.osd.common.model.Identifiable;

public class CrisDedupListenerSolrIndexer implements NativePostUpdateEventListener,
    NativePostDeleteEventListener
{

    private static Logger log = Logger
            .getLogger(CrisDedupListenerSolrIndexer.class);

    private DedupService dedupService;

    public DedupService getDedupService()
    {
        return dedupService;
    }

    public void setDedupService(DedupService dedupService)
    {
        this.dedupService = dedupService;
    }

    @Override
    public <T extends Identifiable> void onPostUpdate(T entity)
    {
        Object object = entity;
        if (!(object instanceof ACrisObject))
        {
            // nothing to do
            return;
        }

        log.debug("Call onPostUpdate " + CrisDedupListenerSolrIndexer.class);

        ACrisObject crisObject = (ACrisObject)object;
        Context ctx = null;
        try
        {
            ctx = new Context();
            dedupService.indexContent(ctx, crisObject, false);
        }
        catch (Exception ex)
        {
            log.error("Failed to update CRIS metadata in deduplication index for cris-"
                    + crisObject.getPublicPath() + " uuid:" + crisObject.getUuid());
        }
        finally
        {
            if (ctx != null && ctx.isValid())
            {
                ctx.abort();
            }
        }
    }

    @Override
    public <P> void onPostDelete(P entity)
    {
        Object object = entity;
        if (!(object instanceof ACrisObject))
        {
            // nothing to do
            return;
        }

        log.debug("Call onPostUpdate " + CrisDedupListenerSolrIndexer.class);

        ACrisObject crisObject = (ACrisObject)object;
        Context ctx = null;
        try
        {
            ctx = new Context();
            dedupService.unIndexContent(ctx, crisObject);
        }
        catch (Exception ex)
        {
            log.error("Failed to delete CRIS metadata in deduplication index for cris-"
                    + crisObject.getPublicPath() + " uuid:" + crisObject.getUuid());
        }
        finally
        {
            if (ctx != null && ctx.isValid())
            {
                ctx.abort();
            }
        }
    }

}
