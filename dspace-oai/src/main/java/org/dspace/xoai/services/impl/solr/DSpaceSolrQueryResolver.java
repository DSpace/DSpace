/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.solr;

import com.lyncode.xoai.dataprovider.filter.Scope;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import com.lyncode.xoai.dataprovider.filter.conditions.Condition;
import org.apache.commons.lang.StringUtils;
import org.dspace.xoai.services.api.solr.SolrQueryResolver;
import org.dspace.xoai.services.api.xoai.DSpaceFilterResolver;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class DSpaceSolrQueryResolver implements SolrQueryResolver {
    @Autowired
    DSpaceFilterResolver filterResolver;

    @Override
    public String buildQuery(List<ScopedFilter> filters) {
        List<String> whereCond = new ArrayList<String>();
        for (ScopedFilter filter : filters)
            whereCond.add(buildQuery(filter.getScope(), filter.getCondition()));

        if (whereCond.isEmpty())
            whereCond.add("*:*");
        String where = "(" + StringUtils.join(whereCond.iterator(), ") AND (") + ")";

        return where;
    }


    private String buildQuery (Scope scope, Condition condition) {
        return filterResolver.buildSolrQuery(scope, condition);
    }
}
