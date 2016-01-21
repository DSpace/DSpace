/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;

/**
 * Servlet for editing the Dublin Core schema registry.
 *
 * @author Martin Hald
 * @version $Revision$
 */
public class MetadataSchemaRegistryServlet extends DSpaceServlet
{
    /** Logger */
    private static final Logger log = Logger.getLogger(MetadataSchemaRegistryServlet.class);

    private static final String clazz = "org.dspace.app.webui.servlet.admin.MetadataSchemaRegistryServlet";

    private final transient MetadataSchemaService schemaService
             = ContentServiceFactory.getInstance().getMetadataSchemaService();
    
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // GET just displays the list of type
        showSchemas(context, request, response);
    }

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String button = UIUtil.getSubmitButton(request, "submit");

        if (button.equals("submit_add"))
        {
            // We are either going to create a new dc schema or update and
            // existing one depending on if a schema_id was passed in
            String id = request.getParameter("dc_schema_id");

            // The sanity check will update the request error string if needed
            if (!sanityCheck(request))
            {
                showSchemas(context, request, response);
                context.abort();
                return;
            }

            try
            {
                String namespace = request.getParameter("namespace");
				String name = request.getParameter("short_name");
				if (id.equals(""))
                {
                    // Create a new metadata schema
                    schemaService.create(context, name, namespace);
                    showSchemas(context, request, response);
                    context.complete();
                }
                else
                {
                    // Update an existing schema
                    MetadataSchema schema = schemaService.find(context,
                            UIUtil.getIntParameter(request, "dc_schema_id"));
                    schema.setNamespace(namespace);
                    schema.setName(name);
                    schemaService.update(context, schema);
                    showSchemas(context, request, response);
                    context.complete();
                }
            }
            catch (NonUniqueMetadataException e)
            {
                request.setAttribute("error",
                        "Please make the namespace and short name unique.");
                showSchemas(context, request, response);
                context.abort();
                return;
            }
        }
        else if (button.equals("submit_delete"))
        {
            // Start delete process - go through verification step
            MetadataSchema schema = schemaService.find(context, UIUtil
                    .getIntParameter(request, "dc_schema_id"));
            request.setAttribute("schema", schema);
            JSPManager.showJSP(request, response,
                    "/dspace-admin/confirm-delete-mdschema.jsp");
        }
        else if (button.equals("submit_confirm_delete"))
        {
            // User confirms deletion of type
            MetadataSchema dc = schemaService.find(context, UIUtil
                    .getIntParameter(request, "dc_schema_id"));
            schemaService.delete(context, dc);
            showSchemas(context, request, response);
            context.complete();
        }
        else
        {
            // Cancel etc. pressed - show list again
            showSchemas(context, request, response);
        }
    }

    /**
     * Return false if the schema arguments fail to pass the constraints. If
     * there is an error the request error String will be updated with an error
     * description.
     *
     * @param request
     * @return true of false
     */
    private boolean sanityCheck(HttpServletRequest request)
    {
        Locale locale = request.getLocale();
        ResourceBundle labels =
            ResourceBundle.getBundle("Messages", locale);
        
        // TODO: add more namespace checks
        String namespace = request.getParameter("namespace");
        if (namespace.length() == 0)
        {
            return error(request, labels.getString(clazz  + ".emptynamespace"));
        }

        String name = request.getParameter("short_name");
        if (name.length() == 0)
        {
            return error(request, labels.getString(clazz  + ".emptyname"));
        }
        if (name.length() > 32)
        {
            return error(request, labels.getString(clazz  + ".nametolong"));
        }
        for (int ii = 0; ii < name.length(); ii++)
        {
            if (name.charAt(ii) == ' ' || name.charAt(ii) == '_'
                    || name.charAt(ii) == '.')
            {
                return error(request,
                        labels.getString(clazz  + ".illegalchar"));
            }
        }

        return true;
    }

    /**
     * Bind the error text to the request object.
     *
     * @param request
     * @param text
     * @return false
     */
    private boolean error(HttpServletRequest request, String text)
    {
        request.setAttribute("error", text);
        return false;
    }

    /**
     * Show list of DC type
     *
     * @param context
     *            Current DSpace context
     * @param request
     *            Current HTTP request
     * @param response
     *            Current HTTP response
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws IOException
     */
    private void showSchemas(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException,
            SQLException, IOException
    {
        List<MetadataSchema> schemas = schemaService.findAll(context);
        request.setAttribute("schemas", schemas);
        log.info("Showing Schemas");
        JSPManager.showJSP(request, response,
                "/dspace-admin/list-metadata-schemas.jsp");
    }
}
