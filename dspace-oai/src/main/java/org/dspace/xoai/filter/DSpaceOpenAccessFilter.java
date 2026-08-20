/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.access.status.AccessStatusHelper;
import org.dspace.access.status.factory.AccessStatusServiceFactory;
import org.dspace.access.status.service.AccessStatusService;
import org.dspace.content.Item;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.filter.results.SolrFilterResult;

/**
 * @author Tina Schoenborn (schoenborn at tu-berlin dot de)
 */
public class DSpaceOpenAccessFilter extends DSpaceFilter {

    private static final Logger log = LogManager.getLogger(DSpaceOpenAccessFilter.class);

    private static final AccessStatusService accessStatusService =
        AccessStatusServiceFactory.getInstance().getAccessStatusService();

    private static final HandleService handleService
        = HandleServiceFactory.getInstance().getHandleService();

    @Override
    public boolean isShown(DSpaceItem dSpaceItem) {
        boolean shown = false;
        try {
            // If Handle or Item are not found, return false
            String handle = DSpaceItem.parseHandle(dSpaceItem.getIdentifier());
            if (handle == null) {
                return false;
            }
            Item item = (Item) handleService.resolveToObject(context, handle);
            if (item == null) {
                return false;
            }
            shown = accessStatusService
                .getAnonymousAccessStatus(context, item)
                .getStatus()
                .equals(AccessStatusHelper.OPEN_ACCESS);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return shown;
    }

    @Override
    public SolrFilterResult buildSolrQuery() {
        return new SolrFilterResult("item.isOpenAccess:true");
    }
}
