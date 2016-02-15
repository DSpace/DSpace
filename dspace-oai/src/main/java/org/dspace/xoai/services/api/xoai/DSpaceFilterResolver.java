/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.api.xoai;


import com.lyncode.xoai.dataprovider.filter.Scope;
import com.lyncode.xoai.dataprovider.filter.conditions.Condition;
import com.lyncode.xoai.dataprovider.services.api.FilterResolver;
import org.dspace.xoai.services.api.context.ContextServiceException;

import java.util.List;

public interface DSpaceFilterResolver extends FilterResolver {
    String buildDatabaseQuery(Condition condition, List<Object> parameters, Scope scope) throws ContextServiceException;

    String buildSolrQuery(Scope scope, Condition condition);
}
