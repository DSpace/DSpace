/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.discovery.DiscoverySearchRequestProcessor;
import org.dspace.app.webui.search.SearchProcessorException;
import org.dspace.app.webui.search.SearchRequestProcessor;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.PluginConfigurationError;
import org.dspace.core.factory.CoreServiceFactory;

/**
 * Servlet for producing OpenSearch-compliant search results, and the OpenSearch
 * description document.
 * <p>
 * OpenSearch is a specification for describing and advertising search-engines
 * and their result formats. Commonly, RSS and Atom formats are used, which the
 * current implementation supports, as is HTML (used directly in browsers). NB:
 * this is baseline OpenSearch, no extensions currently supported.
 * </p>
 * 
 */
public class OpenSearchServlet extends DSpaceServlet
{
    private static final long serialVersionUID = 1L;
    
    private transient SearchRequestProcessor internalLogic;

    /** log4j category */
    private static final Logger log = Logger.getLogger(OpenSearchServlet.class);

    public OpenSearchServlet()
    {
        super();

        try
        {
            internalLogic = (SearchRequestProcessor) CoreServiceFactory.getInstance().getPluginService()
                    .getSinglePlugin(SearchRequestProcessor.class);
        }
        catch (PluginConfigurationError e)
        {
            log.warn(
                    "OpenSearchServlet not properly configurated, please configure the SearchRequestProcessor plugin",
                    e);
        }
        if (internalLogic == null)
        {   // Discovery is the default search provider since DSpace 4.0
            internalLogic = new DiscoverySearchRequestProcessor();
        }
    }

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        try
        {
            internalLogic.doOpenSearch(context, request, response);
        }
        catch (SearchProcessorException e)
        {
            throw new ServletException(e.getMessage(), e);
        }
    }

}
