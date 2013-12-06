/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.cocoon.SearchLoggerAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Returns the query for a simple search so our SearchLoggerAction can log this
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * 
 * @deprecated Since DSpace 4 the system use an abstraction layer named
 *             Discovery to provide access to different search provider. The
 *             legacy system build upon Apache Lucene is likely to be removed in
 *             a future version. If you are interested in use Lucene as backend
 *             for the DSpace search system please consider to build a Lucene
 *             implementation of the Discovery interfaces
 */
@Deprecated
public class SimpleSearchLoggerAction extends SearchLoggerAction{

    @Override
    protected List<String> getQueries(Request request) {
        String query = request.getParameter("query");
        if(!StringUtils.isBlank(query)){
            return Arrays.asList(request.getParameter("query"));
        }else{
            return new ArrayList<String>();
        }
    }
}
