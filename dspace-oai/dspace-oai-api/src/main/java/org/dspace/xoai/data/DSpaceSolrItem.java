package org.dspace.xoai.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import com.lyncode.xoai.common.dataprovider.core.ItemMetadata;
import com.lyncode.xoai.common.dataprovider.core.ReferenceSet;

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
        unparsedMD = (String) doc.getFieldValue("item.compile");
        handle = (String) doc.getFieldValue("item.handle");
        lastMod = (Date) doc.getFieldValue("item.lastmodified");
        sets = new ArrayList<ReferenceSet>();
        for (Object obj : doc.getFieldValues("item.communities"))
            sets.add(new ReferenceSet((String) obj));
        for (Object obj : doc.getFieldValues("item.collections"))
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
