/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.api.solr;

import java.util.List;

import com.lyncode.xoai.dataprovider.filter.ScopedFilter;

public interface SolrQueryResolver {
    String buildQuery(List<ScopedFilter> filters);
}
