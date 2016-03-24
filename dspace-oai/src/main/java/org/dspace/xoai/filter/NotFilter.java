/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import org.dspace.core.Context;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.filter.results.DatabaseFilterResult;
import org.dspace.xoai.filter.results.SolrFilterResult;

public class NotFilter extends DSpaceFilter {
    private DSpaceFilter inFilter;

    public NotFilter(DSpaceFilter inFilter) {
        this.inFilter = inFilter;
    }


    @Override
    public DatabaseFilterResult buildDatabaseQuery(Context context) {
        DatabaseFilterResult result = inFilter.buildDatabaseQuery(context);
        return new DatabaseFilterResult("NOT ("+result.getQuery()+")", result.getParameters());
    }

    @Override
    public SolrFilterResult buildSolrQuery() {
        return new SolrFilterResult("*:* AND NOT(" + inFilter.buildSolrQuery().getQuery() + ")");
    }

    @Override
    public boolean isShown(DSpaceItem item) {
        return !inFilter.isShown(item);
    }
}
