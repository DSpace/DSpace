/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.xmlui.aspect.administrative.mapper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.handle.HandleServiceImpl;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.search.DSQuery;
import org.dspace.search.QueryArgs;
import org.dspace.search.QueryResults;

/**
 * Search using built-in Lucene index provider.
 *
 * @author mwood
 */
public class LuceneSearchRequestProcessor
        implements SearchRequestProcessor
{
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    @Override
    public List<DSpaceObject> doItemMapSearch(Context context, String query, Collection collection)
            throws IOException, SQLException
    {
        QueryArgs queryArgs = new QueryArgs();
        queryArgs.setQuery(query);
        queryArgs.setPageSize(Integer.MAX_VALUE);
        QueryResults results = DSQuery.doQuery(context, queryArgs);

        results.getHitHandles();
        List<DSpaceObject> dsos = new ArrayList<DSpaceObject>();
        for (String handle : results.getHitHandles())
            dsos.add(handleService.resolveToObject(context, handle));

        return dsos;
    }
}
