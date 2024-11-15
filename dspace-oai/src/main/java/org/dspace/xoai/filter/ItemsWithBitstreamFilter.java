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
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.filter.results.SolrFilterResult;


/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 21/04/15
 * Time: 15:18
 */
public class ItemsWithBitstreamFilter extends DSpaceFilter {

    private static Logger log = LogManager.getLogger(ItemsWithBitstreamFilter.class);

    private static final HandleService handleService
            = HandleServiceFactory.getInstance().getHandleService();

    @Override
    public SolrFilterResult buildSolrQuery() {
        return new SolrFilterResult("item.hasbitstream:true");
    }

    @Override
    public boolean isShown(DSpaceItem item) {
        try {
            String handle = DSpaceItem.parseHandle(item.getIdentifier());
            if (handle == null) {
                return false;
            }
            Item dspaceItem = (Item) handleService.resolveToObject(context, handle);
            for (Bundle b : dspaceItem.getBundles("ORIGINAL")) {
                if (b.getBitstreams().size() > 0) {
                    return true;
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
