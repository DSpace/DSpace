/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.database;

import com.lyncode.xoai.dataprovider.filter.Scope;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import com.lyncode.xoai.dataprovider.filter.conditions.Condition;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.xoai.services.api.config.ConfigurationService;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.context.ContextServiceException;
import org.dspace.xoai.services.api.database.DatabaseQuery;
import org.dspace.xoai.services.api.database.DatabaseQueryException;
import org.dspace.xoai.services.api.database.DatabaseQueryResolver;
import org.dspace.xoai.services.api.xoai.DSpaceFilterResolver;
import org.springframework.beans.factory.annotation.Autowired;

public class DSpaceDatabaseQueryResolver implements DatabaseQueryResolver {
    private static final Logger log = LogManager.getLogger(DSpaceDatabaseQueryResolver.class);

    @Autowired
    DSpaceFilterResolver filterResolver;

    @Autowired
    ContextService contextService;

    @Autowired
    ConfigurationService configurationService;

    @Override
    public DatabaseQuery buildQuery(List<ScopedFilter> filters, int offset, int length) throws DatabaseQueryException {
        List<Object> parameters = new ArrayList<Object>();
        List<Object> countParameters = new ArrayList<Object>();
        String query = "SELECT i.* FROM item i ";
        String countQuery = "SELECT COUNT(*) as count FROM item i";

        String where = null;
        try {
            where = this.buildCondition(filters, parameters);
        } catch (ContextServiceException e) {
            throw new DatabaseQueryException(e);
        }
        countParameters.addAll(parameters);

        String whereInArchive = "WHERE i.in_archive=true";
        if(DatabaseManager.isOracle())
        {
            whereInArchive = "WHERE i.in_archive=1";
        }

        if (!where.equals("")) {
            query += " " + whereInArchive + " AND " + where;
            countQuery += " " + whereInArchive + " AND " + where;
        } else {
            query += " " + whereInArchive;
            countQuery += " " + whereInArchive;
        }

        query += " ORDER BY i.item_id";
        boolean postgres = ! DatabaseManager.isOracle();
        if (postgres)
        {
            query += " OFFSET ? LIMIT ?";
        }
        else
        {
            // Oracle
            query = "SELECT * FROM (" + query
                    + ") WHERE ROWNUM BETWEEN ? AND ?";
            length = length + offset;
        }
        parameters.add(offset);
        parameters.add(length);

        try {
            return new DatabaseQuery(contextService.getContext())
                    .withCountQuery(countQuery, countParameters)
                    .withQuery(query)
                    .withParameters(parameters);
        } catch (ContextServiceException e) {
            throw new DatabaseQueryException(e);
        }
    }


    private String buildQuery (Condition condition, Scope scope, List<Object> parameters) throws ContextServiceException {
        return filterResolver.buildDatabaseQuery(condition, parameters, scope);
    }

    private String buildCondition (List<ScopedFilter> filters, List<Object> parameters) throws ContextServiceException {
        List<String> whereCond = new ArrayList<String>();
        for (ScopedFilter filter : filters)
            whereCond.add(this.buildQuery(filter.getCondition(), filter.getScope(), parameters));

        return StringUtils.join(whereCond.iterator(), " AND ");
    }
}
