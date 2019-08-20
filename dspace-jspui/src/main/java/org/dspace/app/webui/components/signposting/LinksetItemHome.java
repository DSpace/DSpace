/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components.signposting;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.json.JSONRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.plugin.signposting.ItemSignPostingProcessor;
import org.dspace.utils.DSpace;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * 
 * 
 * @author Pascarelli Luigi Andrea
 *
 */
public class LinksetItemHome extends JSONRequest
{

    @Override
    public void doJSONRequest(Context context, HttpServletRequest request,
            HttpServletResponse response) throws AuthorizeException, IOException
    {

        Gson gson = new Gson();

        String reqPath = request.getPathInfo();
        // remove the first slash if present
        if (reqPath.startsWith("/"))
        {
            reqPath = reqPath.substring(1);
        }

        String[] pathParts = reqPath.substring(getSubPath().length() + 1)
                .split("/");

        if (pathParts.length != 2)
        {
            // unknown action or malformed URL
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        Item item;
        try
        {
            item = (Item) HandleManager.resolveToObject(context, pathParts[0] + "/" + pathParts[1]);
        }
        catch (IllegalStateException | SQLException e)
        {
            // bad request
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Map<String, Object> results = new HashMap<>();   
        try
        {
            List<ItemSignPostingProcessor> spp = new DSpace().getServiceManager()
                    .getServicesByType(ItemSignPostingProcessor.class);
                  
            for (ItemSignPostingProcessor sp : spp)
            {
                Map<String, Object> result = sp.buildLinkset(context, request, response, item);
                results.putAll(result);
            }
            results.put("anchor", HandleManager.resolveToURL(context, item.getHandle()));
        }
        catch (Exception e)
        {
            // error
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        
        JsonArray jarray = new JsonArray();
        jarray.add(gson.toJsonTree(results));
        JsonObject anchorLinkset = new JsonObject();
        anchorLinkset.addProperty("anchor", ConfigurationManager.getProperty("dspace.url") + "/json/links");
        jarray.add(anchorLinkset);
        
        JsonObject linkset = new JsonObject();
        linkset.add("linkset", jarray);
        
        response.setContentType("application/linkset+json");
        String jsonString = gson.toJson(linkset);
        response.getWriter().write(jsonString);
    }

}
