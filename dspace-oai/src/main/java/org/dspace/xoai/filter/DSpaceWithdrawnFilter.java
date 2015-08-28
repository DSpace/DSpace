/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.filter.results.SolrFilterResult;

/**
 * Filter for Withdrawn items. Enabling this filter allows tombstones for
 * withdrawn items to be accessible via OAI-PMH. This allows us to properly
 * flag withdrawn items with a "deleted" status. For more info on OAI-PMH
 * "deleted" status, see:
 * http://www.openarchives.org/OAI/openarchivesprotocol.html#deletion
 * <P>
 * (Don't worry, a tombstone doesn't display the withdrawn item's metadata or files.)
 * 
 * @author Tim Donohue
 */
public class DSpaceWithdrawnFilter extends DSpaceFilter {

    @Override
    public boolean isShown(DSpaceItem item)
    {
        // For DSpace, if an Item is withdrawn, "isDeleted()" will be true.
        // In this scenario, we want a withdrawn item to be *shown* so that
        // we can properly respond with a "deleted" status via OAI-PMH.
        // Don't worry, this does NOT make the metadata public for withdrawn items,
        // it merely provides an item "tombstone" via OAI-PMH.
        return item.isDeleted();
    }

    @Override
    public SolrFilterResult buildSolrQuery()
    {
        // In Solr, we store withdrawn items as "deleted".
        // See org.dspace.xoai.app.XOAI, index(Item) method.
        return new SolrFilterResult("item.deleted:true");
    }
}
