/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.filter;

import java.sql.SQLException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.filter.results.SolrFilterResult;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DSpaceAuthorizationFilter extends DSpaceFilter
{
    private static final Logger log = LogManager.getLogger(DSpaceAuthorizationFilter.class);

    private static final AuthorizeService authorizeService
            = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    private static final HandleService handleService
            = HandleServiceFactory.getInstance().getHandleService();

    @Override
    public boolean isShown(DSpaceItem item)
    {
        boolean pub = false;
        try
        {
            // If Handle or Item are not found, return false
            String handle = DSpaceItem.parseHandle(item.getIdentifier());
            if (handle == null)
                return false;
            Item dspaceItem = (Item) handleService.resolveToObject(context, handle);
            if (dspaceItem == null)
                return false;

            // Check if READ access allowed on Item
            pub = authorizeService.authorizeActionBoolean(context, dspaceItem, Constants.READ);
        }
        catch (SQLException ex)
        {
            log.error(ex.getMessage(), ex);
        }
        return pub;
    }

    @Override
    public SolrFilterResult buildSolrQuery()
    {
        return new SolrFilterResult("item.public:true");
    }

}
