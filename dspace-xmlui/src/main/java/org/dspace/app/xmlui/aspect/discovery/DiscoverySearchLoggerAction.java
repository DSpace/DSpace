/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.SearchLoggerAction;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class DiscoverySearchLoggerAction extends SearchLoggerAction {

    @Override
    protected List<String> getQueries(Request request) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        List<String> queries = new ArrayList<String>();
        if(request.getParameter("query") != null){
            queries.add(request.getParameter("query"));
        }

        queries.addAll(Arrays.asList(DiscoveryUIUtils.getFilterQueries(request, context)));

        return queries;
    }
}
