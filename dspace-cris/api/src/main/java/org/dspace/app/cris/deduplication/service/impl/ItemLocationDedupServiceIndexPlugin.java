/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.deduplication.service.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.deduplication.service.SolrDedupServiceIndexPlugin;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SolrServiceImpl;

public class ItemLocationDedupServiceIndexPlugin
        implements SolrDedupServiceIndexPlugin
{

    private static final Logger log = Logger
            .getLogger(ItemLocationDedupServiceIndexPlugin.class);

    @Override
    public void additionalIndex(Context context, Integer firstId,
            Integer secondId, Integer type, SolrInputDocument document)
    {

        if (type == Constants.ITEM)
        {
            internal(context, firstId, document);
            if(firstId!=secondId) {
                internal(context, secondId, document);
            }
        }
    }

    private void internal(Context context, Integer itemId,
            SolrInputDocument document)
    {
        try
        {

            Item item = Item.find(context, itemId);

            // build list of community ids
            Community[] communities;
            // build list of collection ids
            Collection[] collections;

            communities = item.getCommunities();
            collections = item.getCollections();

            // now put those into strings
            int i = 0;

            for (i = 0; i < communities.length; i++)
            {
                document.addField("parentlocation_s", communities[i].getName());
            }

            for (i = 0; i < collections.length; i++)
            {
                document.addField("parentlocation_s", collections[i].getName());
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
    }

}
