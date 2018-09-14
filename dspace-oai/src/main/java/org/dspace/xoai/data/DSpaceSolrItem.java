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
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DSpaceSolrItem extends DSpaceItem
{
    private static Logger log = LogManager
            .getLogger(DSpaceSolrItem.class);
    
    private String unparsedMD;
    private ItemMetadata metadata;
    private String handle;
    private Date lastMod;
    private List<ReferenceSet> sets;
    private boolean deleted;
    
    public DSpaceSolrItem (SolrDocument doc) {
    	log.debug("Creating OAI Item from Solr source");
        unparsedMD = (String) doc.getFieldValue("item.compile");
        handle = (String) doc.getFieldValue("item.handle");
        lastMod = (Date) doc.getFieldValue("item.lastmodified");
        sets = new ArrayList<ReferenceSet>();

        Collection<Object> communities = doc.getFieldValues("item.communities");
        if (null != communities)
            for (Object obj : communities)
                sets.add(new ReferenceSet((String) obj));
        else
            log.warn(String.format("Record for item %s has no communities.", handle));

        Collection<Object> collections = doc.getFieldValues("item.collections");
        if (null != collections)
            for (Object obj : collections)
                sets.add(new ReferenceSet((String) obj));
        else
            log.warn(String.format("Record for item %s has no collections.", handle));

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
