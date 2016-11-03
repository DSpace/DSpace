/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import com.lyncode.xoai.dataprovider.core.ItemMetadata;
import com.lyncode.xoai.dataprovider.core.ReferenceSet;
import java.util.Collection;

/**
 * 
 * @author Lyncode Development Team (dspace at lyncode dot com)
 */
public class DSpaceSolrItem extends DSpaceItem
{
    private static final Logger log = LogManager
            .getLogger(DSpaceSolrItem.class);
    
    private final String unparsedMD;
    private ItemMetadata metadata;
    private final String handle;
    private final Date lastMod;
    private final List<ReferenceSet> sets;
    private final boolean deleted;

    public DSpaceSolrItem (SolrDocument doc) {
    	log.debug("Creating OAI Item from Solr source");
        unparsedMD = (String) doc.getFieldValue("item.compile");
        handle = (String) doc.getFieldValue("item.handle");
        lastMod = (Date) doc.getFieldValue("item.lastmodified");
        sets = new ArrayList<>();

        Collection<Object> fieldValues;

        fieldValues = doc.getFieldValues("item.communities");
        if (null != fieldValues)
            for (Object obj : fieldValues)
                sets.add(new ReferenceSet((String) obj));

        fieldValues = doc.getFieldValues("item.collections");
        if (null != fieldValues)
            for (Object obj : fieldValues)
                sets.add(new ReferenceSet((String) obj));

        deleted = (Boolean) doc.getFieldValue("item.deleted");
    }

    @Override
    public ItemMetadata getMetadata()
    {
        if (metadata == null) {
            metadata = new ItemMetadata(unparsedMD);
        }
        return metadata;
    }

    @Override
    public Date getDatestamp()
    {
        return lastMod;
    }

    @Override
    public List<ReferenceSet> getSets()
    {
        return sets;
    }

    @Override
    public boolean isDeleted()
    {
        return deleted;
    }

    @Override
    protected String getHandle()
    {
        return handle;
    }

}
