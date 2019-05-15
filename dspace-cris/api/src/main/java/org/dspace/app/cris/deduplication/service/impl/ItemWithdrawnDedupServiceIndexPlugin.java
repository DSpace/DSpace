/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.deduplication.service.impl;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.deduplication.service.SolrDedupServiceIndexPlugin;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.util.ItemUtils;

public class ItemWithdrawnDedupServiceIndexPlugin
        implements SolrDedupServiceIndexPlugin
{

    private static final Logger log = Logger
            .getLogger(ItemWithdrawnDedupServiceIndexPlugin.class);

    @Override
    public void additionalIndex(Context context, Integer firstId,
            Integer secondId, Integer type, SolrInputDocument document)
    {

        if (type == Constants.ITEM)
        {
            boolean isWithdrawn = internal(context, firstId, document);
            if(!isWithdrawn && (firstId != secondId)) {
                internal(context, secondId, document);
            }
        }

    }

    private boolean internal(Context context, Integer itemId,
            SolrInputDocument document)
    {
        try
        {
            Item item = Item.find(context, itemId);

            Integer status = ItemUtils.getItemStatus(context, item);
            if (status == 3)
            {
                document.addField(SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD, true);
                return true;
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return false;
    }

}
