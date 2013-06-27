/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.util.XOAIDatabaseManager;

import com.lyncode.xoai.dataprovider.core.ReferenceSet;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DspaceSetSpecFilter extends DSpaceFilter
{
    private static Logger log = LogManager.getLogger(DspaceSetSpecFilter.class);

    private String _setSpec;

    public DspaceSetSpecFilter(String spec)
    {
        _setSpec = spec;
    }

    @Override
    public DatabaseFilterResult getWhere(Context context)
    {
        if (_setSpec.startsWith("col_"))
        {
            try
            {
                DSpaceObject dso = HandleManager.resolveToObject(context,
                        _setSpec.replace("col_", ""));
                return new DatabaseFilterResult(
                        "EXISTS (SELECT tmp.* FROM collection2item tmp WHERE tmp.item_id=i.item_id AND collection_id = ?)",
                        dso.getID());
            }
            catch (Exception ex)
            {
                log.error(ex.getMessage(), ex);
            }
        }
        else if (_setSpec.startsWith("com_"))
        {
            try
            {
                DSpaceObject dso = HandleManager.resolveToObject(context,
                        _setSpec.replace("com_", ""));
                List<Integer> list = XOAIDatabaseManager.getAllSubCollections(
                        context, dso.getID());
                String subCollections = StringUtils.join(list.iterator(), ",");
                return new DatabaseFilterResult(
                        "EXISTS (SELECT tmp.* FROM collection2item tmp WHERE tmp.item_id=i.item_id AND collection_id IN ("
                                + subCollections + "))");
            }
            catch (Exception e)
            {
                log.error(e.getMessage(), e);
            }
        }
        return new DatabaseFilterResult();
    }

    @Override
    public boolean isShown(DSpaceItem item)
    {
        for (ReferenceSet s : item.getSets())
            if (s.getSetSpec().equals(_setSpec))
                return true;
        return false;
    }

    @Override
    public SolrFilterResult getQuery()
    {
        if (_setSpec.startsWith("col_"))
        {
            try
            {
                return new SolrFilterResult("item.collections:"
                        + ClientUtils.escapeQueryChars(_setSpec));
            }
            catch (Exception ex)
            {
                log.error(ex.getMessage(), ex);
            }
        }
        else if (_setSpec.startsWith("com_"))
        {
            try
            {
                return new SolrFilterResult("item.communities:"
                        + ClientUtils.escapeQueryChars(_setSpec));
            }
            catch (Exception e)
            {
                log.error(e.getMessage(), e);
            }
        }
        return new SolrFilterResult();
    }

}
