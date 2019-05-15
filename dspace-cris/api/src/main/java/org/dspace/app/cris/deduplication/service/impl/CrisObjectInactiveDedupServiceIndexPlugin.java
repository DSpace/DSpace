/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.deduplication.service.impl;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.deduplication.service.SolrDedupServiceIndexPlugin;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.Context;

public class CrisObjectInactiveDedupServiceIndexPlugin
        implements SolrDedupServiceIndexPlugin
{

    private static final Logger log = Logger
            .getLogger(CrisObjectInactiveDedupServiceIndexPlugin.class);

    private ApplicationService applicationService;
        
    @Override
    public void additionalIndex(Context context, Integer firstId,
            Integer secondId, Integer type, SolrInputDocument document)
    {
        if (type >= CrisConstants.CRIS_TYPE_ID_START)
        {
            boolean isWithdrawn = internal(context, firstId, type, document);
            if (!isWithdrawn && (firstId != secondId))
            {
                internal(context, secondId, type, document);
            }
        }
    }

    private boolean internal(Context context, Integer id, Integer type,
            SolrInputDocument document)
    {
        if (!applicationService.getEntityById(id, type).getStatus())
        {
            document.addField(SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD, true);
            return true;
        }
        return false;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

}
