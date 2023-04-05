/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.xoai;

import static com.lyncode.xoai.dataprovider.filter.Scope.MetadataFormat;

import com.lyncode.xoai.dataprovider.data.Filter;
import com.lyncode.xoai.dataprovider.filter.Scope;
import com.lyncode.xoai.dataprovider.filter.conditions.AndCondition;
import com.lyncode.xoai.dataprovider.filter.conditions.Condition;
import com.lyncode.xoai.dataprovider.filter.conditions.CustomCondition;
import com.lyncode.xoai.dataprovider.filter.conditions.NotCondition;
import com.lyncode.xoai.dataprovider.filter.conditions.OrCondition;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.ParameterMap;
import org.apache.logging.log4j.Logger;
import org.dspace.xoai.filter.AndFilter;
import org.dspace.xoai.filter.DSpaceFilter;
import org.dspace.xoai.filter.NotFilter;
import org.dspace.xoai.filter.OrFilter;
import org.dspace.xoai.filter.results.SolrFilterResult;
import org.dspace.xoai.services.api.FieldResolver;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.context.ContextServiceException;
import org.dspace.xoai.services.api.xoai.DSpaceFilterResolver;
import org.springframework.beans.factory.annotation.Autowired;

public class BaseDSpaceFilterResolver implements DSpaceFilterResolver {
    private static final Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(BaseDSpaceFilterResolver.class);

    @Autowired
    FieldResolver fieldResolver;

    @Autowired
    ContextService contextService;

    public DSpaceFilter getFilter(Condition condition) {
        if (condition instanceof AndCondition) {
            return (DSpaceFilter) getFilter((AndCondition) condition);
        } else if (condition instanceof OrCondition) {
            return (DSpaceFilter) getFilter((OrCondition) condition);
        } else if (condition instanceof NotCondition) {
            return (DSpaceFilter) getFilter((NotCondition) condition);
        } else if (condition instanceof CustomCondition) {
            CustomCondition customCondition = (CustomCondition) condition;
            return (DSpaceFilter) customCondition.getFilter();
        } else {
            return (DSpaceFilter) condition.getFilter();
        }
    }

    @Override
    public String buildSolrQuery(Scope scope, Condition condition) {
        DSpaceFilter filter = getFilter(condition);
        SolrFilterResult result = filter.buildSolrQuery();
        if (result.hasResult()) {
            if (scope == MetadataFormat) {
                return "(item.deleted:true OR ("
                    + result.getQuery() + "))";
            } else {
                return "(" + result.getQuery() + ")";
            }
        }
        return "true";
    }

    @Override
    public Filter getFilter(Class<? extends Filter> filterClass, ParameterMap configuration) {
        Filter result = null;
        try {
            result = filterClass.newInstance();
            if (result instanceof DSpaceFilter) {
                // add the DSpace filter specific objects
                ((DSpaceFilter) result).setConfiguration(configuration);
                ((DSpaceFilter) result).setContext(contextService.getContext());
                ((DSpaceFilter) result).setFieldResolver(fieldResolver);
            }
        } catch (InstantiationException | IllegalAccessException
            | ContextServiceException e) {
            LOGGER.error("Filter " + filterClass.getName()
                             + " could not be instantiated", e);
        }
        return result;
    }

    @Override
    public Filter getFilter(AndCondition andCondition) {
        DSpaceFilter leftFilter = this.getFilter(andCondition.getLeft());
        DSpaceFilter rightFilter = this.getFilter(andCondition.getRight());
        return new AndFilter(leftFilter, rightFilter);
    }

    @Override
    public Filter getFilter(OrCondition orCondition) {
        DSpaceFilter leftFilter = this.getFilter(orCondition.getLeft());
        DSpaceFilter rightFilter = this.getFilter(orCondition.getRight());
        return new OrFilter(leftFilter, rightFilter);
    }

    @Override
    public Filter getFilter(NotCondition notCondition) {
        DSpaceFilter filter = this.getFilter(notCondition.getCondition());
        return new NotFilter(filter);
    }
}
