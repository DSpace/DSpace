/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.handle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.webui.json.JSONRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

import com.google.gson.Gson;

public class HandleJSONResolver extends JSONRequest
{
    private static final Logger log = Logger
            .getLogger(HandleJSONResolver.class);

	private HandleService handleService;
        private ConfigurationService configurationService;

	public HandleJSONResolver() {
		handleService = HandleServiceFactory.getInstance().getHandleService();
                configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
	}

    public void doJSONRequest(Context context, HttpServletRequest request,
            HttpServletResponse resp) throws AuthorizeException, IOException
    {
        String reqPath = request.getPathInfo().replaceFirst(getSubPath()+"/", "");
        // remove the first slash if present
        if (reqPath.startsWith("/"))
        {
            reqPath = reqPath.substring(1);
        }

        Gson gson = new Gson();
        String jsonString = "";

        try
        {
            if (reqPath.startsWith("resolve/"))
            {
                String handle = reqPath.substring("resolve/".length());
                if (StringUtils.isBlank(handle))
                {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                String url = handleService.resolveToURL(context, handle);
                // Only an array or an abject is valid JSON. A simple string
                // isn't. An object always uses key value pairs, so we use an
                // array.
                if (url != null)
                {
                    jsonString = gson.toJson(new String[] {url});
                }
                else
                {
                    jsonString = gson.toJson(null);
                }
            }
            else if (reqPath.equals("listprefixes"))
            {
                List<String> prefixes = new ArrayList<String>();
                prefixes.add(handleService.getPrefix());
                String[] additionalPrefixes = configurationService
                        .getArrayProperty("handle.additional.prefixes");
                if (additionalPrefixes!=null)
                {
                    for (String apref : additionalPrefixes)
                    {
                        prefixes.add(apref.trim());
                    }
                }
                jsonString = gson.toJson(prefixes);
            }
            else if (reqPath.startsWith("listhandles/"))
            {
                if (configurationService.getBooleanProperty(
                        "handle.hide.listhandles", true))
                {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                String prefix = reqPath.substring("listhandles/".length());
                if (StringUtils.isBlank(prefix))
                {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                List<String> handlelist = handleService.getHandlesForPrefix(
                        context, prefix);
                jsonString = gson.toJson(handlelist);
            }
            else
            {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            resp.getWriter().print(jsonString);
            return;
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
            return;
        }
    }
}
