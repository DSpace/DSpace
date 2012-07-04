/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.xoai.data.DSpaceIdentify;
import org.dspace.xoai.data.DSpaceItemDatabaseRepository;
import org.dspace.xoai.data.DSpaceItemRepository;
import org.dspace.xoai.data.DSpaceItemSolrRepository;
import org.dspace.xoai.data.DSpaceSetRepository;
import org.dspace.xoai.filter.DSpaceFilter;
import org.dspace.xoai.util.XOAICacheManager;

import com.lyncode.xoai.common.dataprovider.OAIDataProvider;
import com.lyncode.xoai.common.dataprovider.OAIRequestParameters;
import com.lyncode.xoai.common.dataprovider.core.XOAIManager;
import com.lyncode.xoai.common.dataprovider.exceptions.InvalidContextException;
import com.lyncode.xoai.common.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.common.dataprovider.filter.AbstractFilter;
import com.lyncode.xoai.common.dataprovider.util.Base64Utils;

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
                    .getProperty("dspace.dir") + "/config/modules/xoai");
            log.info("[XOAI] Initialized");
        }
        catch (com.lyncode.xoai.common.dataprovider.exceptions.ConfigurationException e)
        {
            log.error(e.getMessage(), e);
        }
    }

    public void destroy()
    {
        log.info("[XOAI] Destroyed");
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        request.setCharacterEncoding("UTF-8");
        Context context = null;

        try
        {
            log.debug("XOAI request received");
            context = new Context();

            // Filters require database connection -> dependency injection?
            for (AbstractFilter filter : XOAIManager.getManager()
                    .getFilterManager().getFilters())
                if (filter instanceof DSpaceFilter)
                    ((DSpaceFilter) filter).initialize(context);

            DSpaceItemRepository repository;
            String storage = ConfigurationManager
                    .getProperty("xoai", "storage");
            if (storage == null
                    || !storage.trim().toLowerCase().equals("database"))
            {
                log.debug("Using Solr for querying");
                repository = new DSpaceItemSolrRepository(context);
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
                    "Context \""
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
