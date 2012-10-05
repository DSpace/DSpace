/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.xoai.data.DSpaceIdentify;
import org.dspace.xoai.data.DSpaceItemDatabaseRepository;
import org.dspace.xoai.data.DSpaceItemRepository;
import org.dspace.xoai.data.DSpaceItemSolrRepository;
import org.dspace.xoai.data.DSpaceSetRepository;
import org.dspace.xoai.filter.DSpaceFilter;
import org.dspace.xoai.solr.DSpaceSolrServer;
import org.dspace.xoai.util.XOAICacheManager;

import com.lyncode.xoai.dataprovider.OAIDataProvider;
import com.lyncode.xoai.dataprovider.OAIRequestParameters;
import com.lyncode.xoai.dataprovider.core.XOAIManager;
import com.lyncode.xoai.dataprovider.exceptions.InvalidContextException;
import com.lyncode.xoai.dataprovider.filter.AbstractFilter;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
@SuppressWarnings("serial")
public class DSpaceOAIDataProvider extends HttpServlet
{
    private static Logger log = LogManager
            .getLogger(DSpaceOAIDataProvider.class);

    @Override
    public void init()
    {
        try
        {
            XOAIManager.initialize(ConfigurationManager
                    .getProperty("oai", "config.dir"));
            if (!"database".equals(ConfigurationManager.getProperty("oai", "storage"))) {
                DSpaceSolrServer.getServer();
            }
            System.out.println("[OAI 2.0] Initialized");
        }
        catch (com.lyncode.xoai.dataprovider.exceptions.ConfigurationException e)
        {
            System.out.println("Unable to configure XOAI (OAI 2.0 Core)");
            e.printStackTrace();
        }
        catch (SolrServerException e)
        {
            System.out.println("Unable to configure XOAI (OAI 2.0 Core)");
            e.printStackTrace();
        }
    }

    public void destroy()
    {
        System.out.println("[OAI 2.0] Destroyed");
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        request.setCharacterEncoding("UTF-8");
        Context context = null;

        try
        {
            log.debug("OAI 2.0 request received");
            context = new Context();

            // Filters require database connection -> dependency injection?
            for (AbstractFilter filter : XOAIManager.getManager()
                    .getFilterManager().getFilters())
                if (filter instanceof DSpaceFilter)
                    ((DSpaceFilter) filter).initialize(context);

            DSpaceItemRepository repository;
            String storage = ConfigurationManager
                    .getProperty("oai", "storage");
            if (storage == null
                    || !storage.trim().toLowerCase().equals("database"))
            {
                log.debug("Using Solr for querying");
                repository = new DSpaceItemSolrRepository();
            }
            else
            {
                log.debug("Using Database for querying");
                repository = new DSpaceItemDatabaseRepository(context);
            }

            log.debug("Creating OAI Data Provider Instance");
            OAIDataProvider dataProvider = new OAIDataProvider(request
                    .getPathInfo().replace("/", ""), new DSpaceIdentify(
                    context, request), new DSpaceSetRepository(context),
                    repository);

            log.debug("Reading parameters from request");

            OutputStream out = response.getOutputStream();
            OAIRequestParameters parameters = new OAIRequestParameters();
            parameters.setFrom(request.getParameter("from"));
            parameters.setUntil(request.getParameter("until"));
            parameters.setSet(request.getParameter("set"));
            parameters.setVerb(request.getParameter("verb"));
            parameters
                    .setMetadataPrefix(request.getParameter("metadataPrefix"));
            parameters.setIdentifier(request.getParameter("identifier"));
            parameters.setResumptionToken(request
                    .getParameter("resumptionToken"));

            response.setContentType("application/xml");

            String identification = request.getPathInfo().replace("/", "")
                    + parameters.getVerb() + parameters.getMetadataPrefix()
                    + parameters.getIdentifier()
                    + parameters.getResumptionToken() + parameters.getSet()
                    + parameters.getFrom() + parameters.getUntil();

            log.debug("Handling OAI request");
            XOAICacheManager.handle(identification, dataProvider, parameters, out);
            
            out.flush();
            out.close();
            
            if (context != null)
                context.abort();
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        catch (InvalidContextException e)
        {
            log.error(e.getMessage(), e);
            if (context != null)
                context.abort();
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Requested OAI context \""
                    + request.getPathInfo().replace("/", "")
                    + "\" does not exist");
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        this.doGet(req, resp);
    }

}
