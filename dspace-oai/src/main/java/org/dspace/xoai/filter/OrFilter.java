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


public class OrFilter extends DSpaceFilter {
    private final DSpaceFilter left;
    private final DSpaceFilter right;

    public OrFilter(DSpaceFilter left, DSpaceFilter right) {
        this.left = left;
        this.right = right;
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
