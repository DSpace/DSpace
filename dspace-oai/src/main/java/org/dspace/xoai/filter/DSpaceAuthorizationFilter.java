/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.filter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.filter.results.DatabaseFilterResult;
import org.dspace.xoai.filter.results.SolrFilterResult;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DSpaceAuthorizationFilter extends DSpaceFilter
{
    private static Logger log = LogManager.getLogger(DSpaceAuthorizationFilter.class);
    private Context context;

    public DSpaceAuthorizationFilter (Context context) {
        this.context = context;
    }

    @Override
    public DatabaseFilterResult buildDatabaseQuery(Context context)
    {
        List<Object> params = new ArrayList<Object>();
        return new DatabaseFilterResult("EXISTS (SELECT p.action_id FROM "
                + "resourcepolicy p, " + "bundle2bitstream b, " + "bundle bu, "
                + "item2bundle ib " + "WHERE " + "p.resource_type_id=0 AND "
                + "p.resource_id=b.bitstream_id AND "
                + "p.epersongroup_id=0 AND " + "b.bundle_id=ib.bundle_id AND "
                + "bu.bundle_id=b.bundle_id AND " + "bu.name='ORIGINAL' AND "
                + "ib.item_id=i.item_id)", params);
    }

    @Override
    public boolean isShown(DSpaceItem item)
    {
        try
        {
            String handle = DSpaceItem.parseHandle(item.getIdentifier());
            if (handle == null) return false;
            Item dspaceItem = (Item) HandleManager.resolveToObject(context, handle);
            AuthorizeManager.authorizeAction(context, dspaceItem, Constants.READ);
            for (Bundle b : dspaceItem.getBundles())
                AuthorizeManager.authorizeAction(context, b, Constants.READ);
            return true;
        }
        catch (AuthorizeException ex)
        {
            log.error(ex.getMessage(), ex);
        }
        catch (SQLException ex)
        {
            log.error(ex.getMessage(), ex);
        }
        catch (Exception ex)
        {
            log.error(ex.getMessage(), ex);
        }
        return false;
    }

    @Override
    public SolrFilterResult buildSolrQuery()
    {
        return new SolrFilterResult("item.public:true");
    }

}
