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

import java.util.ArrayList;
import java.util.List;

public class OrFilter extends DSpaceFilter {
    private DSpaceFilter left;
    private DSpaceFilter right;

    public OrFilter(DSpaceFilter left, DSpaceFilter right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public DatabaseFilterResult buildDatabaseQuery(Context context) {
        DatabaseFilterResult leftResult = left.buildDatabaseQuery(context);
        DatabaseFilterResult rightResult = right.buildDatabaseQuery(context);
        List<Object> param = new ArrayList<Object>();
        param.addAll(leftResult.getParameters());
        param.addAll(rightResult.getParameters());
        return new DatabaseFilterResult("("+leftResult.getQuery()+") OR ("+ rightResult.getQuery() +")", param);
    }

    @Override
    public SolrFilterResult buildSolrQuery() {
        return new SolrFilterResult("("+left.buildSolrQuery().getQuery()+") OR ("+right.buildSolrQuery().getQuery()+")");
    }

    @Override
    public boolean isShown(DSpaceItem item) {
        return left.isShown(item) || right.isShown(item);
    }
}
