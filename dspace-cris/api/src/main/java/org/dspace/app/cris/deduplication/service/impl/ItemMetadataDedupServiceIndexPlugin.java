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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.deduplication.service.SolrDedupServiceIndexPlugin;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.core.Context;

public class ItemMetadataDedupServiceIndexPlugin
        implements SolrDedupServiceIndexPlugin
{

    private static final Logger log = Logger
            .getLogger(ItemMetadataDedupServiceIndexPlugin.class);

    private List<String> metadata;

    private String field;

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

            for (String meta : metadata)
            {
                for (Metadatum mm : item.getMetadataByMetadataString(meta))
                {
                    if (StringUtils.isNotEmpty(field))
                    {
                        document.addField(field, mm.value);
                    }
                    else
                    {
                        document.addField(mm.getField() + "_s", mm.value);
                    }
                }
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
    }

    public List<String> getMetadata()
    {
        return metadata;
    }

    public void setMetadata(List<String> metadata)
    {
        this.metadata = metadata;
    }

    public String getField()
    {
        return field;
    }

    public void setField(String field)
    {
        this.field = field;
    }

}
