/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import com.lyncode.xoai.dataprovider.core.ReferenceSet;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.filter.results.DatabaseFilterResult;
import org.dspace.xoai.filter.results.SolrFilterResult;
import org.dspace.xoai.services.api.database.CollectionsService;
import org.dspace.xoai.services.api.database.HandleResolver;

import java.util.List;

/**
 * based on class by Lyncode Development Team <dspace@lyncode.com>
 * modified for LINDAT/CLARIN
 */
public class DSpaceSetSpecFilter extends DSpaceFilter
{
    private static Logger log = LogManager.getLogger(DSpaceSetSpecFilter.class);

    private String setSpec;
    private HandleResolver handleResolver;
    private CollectionsService collectionsService;

    public DSpaceSetSpecFilter(CollectionsService collectionsService, HandleResolver handleResolver, String spec)
    {
        this.collectionsService = collectionsService;
        this.handleResolver = handleResolver;
        this.setSpec = spec;
    }

    @Override
    public DatabaseFilterResult buildDatabaseQuery(Context context)
    {
		try {
			DSpaceObject dso = handleResolver.resolve(setSpec.replace("hdl_",
					"").replace("_", "/"));
			if (dso != null) {
				if (dso.getType() == Constants.COLLECTION) {
					return new DatabaseFilterResult(
							"EXISTS (SELECT tmp.* FROM collection2item tmp WHERE tmp.resource_id=i.item_id AND collection_id = ?)",
							dso.getID());
				} else if (dso.getType() == Constants.COMMUNITY) {
					List<Integer> list = collectionsService
							.getAllSubCollections(dso.getID());
					String subCollections = StringUtils.join(list.iterator(),
							",");
					return new DatabaseFilterResult(
							"EXISTS (SELECT tmp.* FROM collection2item tmp WHERE tmp.resource_id=i.item_id AND collection_id IN ("
									+ subCollections + "))");
				}
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		return new DatabaseFilterResult();
    }

    @Override
    public boolean isShown(DSpaceItem item)
    {
        for (ReferenceSet s : item.getSets())
            if (s.getSetSpec().equals(setSpec))
                return true;
        return false;
    }

    @Override
    public SolrFilterResult buildSolrQuery()
    {
		try {
			DSpaceObject dso = handleResolver.resolve(setSpec.replace("hdl_",
					"").replace("_", "/"));
			if (dso != null) {
				if (dso.getType() == Constants.COLLECTION) {
					return new SolrFilterResult("item.collections:"
							+ ClientUtils.escapeQueryChars(setSpec));
				} else if (dso.getType() == Constants.COMMUNITY) {
					return new SolrFilterResult("item.communities:"
							+ ClientUtils.escapeQueryChars(setSpec));
				}
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		return new SolrFilterResult();
	}

}
