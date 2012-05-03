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
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.core.Context;

/**
 * Servlet for editing the Dublin Core registry
 * 
 * @author Robert Tansley
 * @author Martin Hald
 * @version $Revision$
 */
public class MetadataFieldRegistryServlet extends DSpaceServlet
{
    /** Logger */
    private static Logger log = Logger.getLogger(MetadataFieldRegistryServlet.class);
    private String clazz = "org.dspace.app.webui.servlet.admin.MetadataFieldRegistryServlet";

    /**
     * @see org.dspace.app.webui.servlet.DSpaceServlet#doDSGet(org.dspace.core.Context,
     *      javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // GET just displays the list of type
        int schemaID = getSchemaID(request);
        showTypes(context, request, response, schemaID);
    }

    /**
     * @see org.dspace.app.webui.servlet.DSpaceServlet#doDSPost(org.dspace.core.Context,
     *      javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String button = UIUtil.getSubmitButton(request, "submit");
        int schemaID = getSchemaID(request);

        // Get access to the localized resource bundle
        Locale locale = context.getCurrentLocale();
        ResourceBundle labels = ResourceBundle.getBundle("Messages", locale);

        if (button.equals("submit_update"))
        {
            // The sanity check will update the request error string if needed
            if (!sanityCheck(request, labels))
            {
                showTypes(context, request, response, schemaID);
                context.abort();
                return;
            }

            try
            {
                // Update the metadata for a DC type
                MetadataField dc = MetadataField.find(context, UIUtil
                        .getIntParameter(request, "dc_type_id"));
            dc.setElement(request.getParameter("element"));

            String qual = request.getParameter("qualifier");
            if (qual.equals(""))
            {
                qual = null;
            }

            dc.setQualifier(qual);
            dc.setScopeNote(request.getParameter("scope_note"));
                dc.update(context);
                showTypes(context, request, response, schemaID);
            context.complete();
        }
            catch (NonUniqueMetadataException e)
            {
                context.abort();
                log.error(e);
            }
        }
        else if (button.equals("submit_add"))
        {

            // The sanity check will update the request error string if needed
            if (!sanityCheck(request, labels))
            {
                showTypes(context, request, response, schemaID);
                context.abort();
                return;
            }

            // Add a new DC type - simply add to the list, and let the user
            // edit with the main form
            try
            {
                MetadataField dc = new MetadataField();
                dc.setSchemaID(schemaID);
                dc.setElement(request.getParameter("element"));

                String qual = request.getParameter("qualifier");
                if (qual.equals(""))
                {
                    qual = null;
                }

                dc.setQualifier(qual);
                dc.setScopeNote(request.getParameter("scope_note"));
                dc.create(context);
                showTypes(context, request, response, schemaID);
            context.complete();
        }
            catch (NonUniqueMetadataException e)
            {
                // Record the exception as a warning
                log.warn(e);

                // Show the page again but with an error message to inform the
                // user that the metadata field was not created and why
                request.setAttribute("error", labels.getString(clazz
                        + ".createfailed"));
                showTypes(context, request, response, schemaID);
                context.abort();
            }
        }
        else if (button.equals("submit_delete"))
        {
            // Start delete process - go through verification step
            MetadataField dc = MetadataField.find(context, UIUtil
                    .getIntParameter(request, "dc_type_id"));
            request.setAttribute("type", dc);
            JSPManager.showJSP(request, response,
                    "/dspace-admin/confirm-delete-mdfield.jsp");
        }
        else if (button.equals("submit_confirm_delete"))
        {
            // User confirms deletion of type
            MetadataField dc = MetadataField.find(context, UIUtil
                    .getIntParameter(request, "dc_type_id"));
            try
            {
                dc.delete(context);
                request.setAttribute("failed", Boolean.FALSE);
                showTypes(context, request, response, schemaID);
            } catch (Exception e)
            {
                request.setAttribute("type", dc);
                request.setAttribute("failed", true);
                JSPManager.showJSP(request, response,
                        "/dspace-admin/confirm-delete-mdfield.jsp");
            }
            context.complete();
        }
        else if (button.equals("submit_move"))
        {
            // User requests that one or more metadata elements be moved to a
            // new metadata schema. Note that we change the default schema ID to
            // be the destination schema.
            try
            {
                schemaID = Integer.parseInt(request
                        .getParameter("dc_dest_schema_id"));
                String[] param = request.getParameterValues("dc_field_id");
                if (schemaID == 0 || param == null)
                {
                    request.setAttribute("error", labels.getString(clazz
                            + ".movearguments"));
                    showTypes(context, request, response, schemaID);
                    context.abort();
                }
                else
                {
                    for (int ii = 0; ii < param.length; ii++)
                    {
                        int fieldID = Integer.parseInt(param[ii]);
                        MetadataField field = MetadataField.find(context,
                                fieldID);
                        field.setSchemaID(schemaID);
                        field.update(context);

                    }
            context.complete();
                     
                    // Send the user to the metadata schema in which they just moved
                    // the metadata fields
                    response.sendRedirect(request.getContextPath()
                            + "/dspace-admin/metadata-schema-registry?dc_schema_id=" + schemaID);
                }
            }
            catch (NonUniqueMetadataException e)
            {
                // Record the exception as a warning
                log.warn(e);

                // Show the page again but with an error message to inform the
                // user that the metadata field could not be moved
                request.setAttribute("error", labels.getString(clazz
                        + ".movefailed"));
                showTypes(context, request, response, schemaID);
                context.abort();
            }
        }
        else
        {
            // Cancel etc. pressed - show list again
            showTypes(context, request, response, schemaID);
        }
    }

    /**
     * Get the schema that we are currently working in from the HTTP request. If
     * not present then default to the DSpace Dublin Core schema (schemaID 1).
     *
     * @param request
     * @return the current schema ID
     */
    private int getSchemaID(HttpServletRequest request)
    {
        int schemaID = MetadataSchema.DC_SCHEMA_ID;
        if (request.getParameter("dc_schema_id") != null)
        {
            schemaID = Integer.parseInt(request.getParameter("dc_schema_id"));
        }
        return schemaID;
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
     * @param schemaID
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void showTypes(Context context, HttpServletRequest request,
            HttpServletResponse response, int schemaID)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // Find matching metadata fields
        MetadataField[] types = MetadataField
                .findAllInSchema(context, schemaID);
        request.setAttribute("types", types);

        // Pull the metadata schema object as well
        MetadataSchema schema = MetadataSchema.find(context, schemaID);
        request.setAttribute("schema", schema);

        // Pull all metadata schemas for the pulldown
        MetadataSchema[] schemas = MetadataSchema.findAll(context);
        request.setAttribute("schemas", schemas);

        JSPManager
                .showJSP(request, response, "/dspace-admin/list-metadata-fields.jsp");
    }

    /**
     * Return false if the metadata field fail to pass the constraint tests. If
     * there is an error the request error String will be updated with an error
     * description.
     *
     * @param request
     * @param labels
     * @return true of false
     */
    private boolean sanityCheck(HttpServletRequest request,
            ResourceBundle labels)
    {
        String element = request.getParameter("element");
        if (element.length() == 0)
        {
            return error(request, labels.getString(clazz + ".elemempty"));
        }
        for (int ii = 0; ii < element.length(); ii++)
        {
            if (element.charAt(ii) == '.' || element.charAt(ii) == '_'
                    || element.charAt(ii) == ' ')
            {
                return error(request, labels.getString(clazz + ".badelemchar"));
            }
        }
        if (element.length() > 64)
        {
            return error(request, labels.getString(clazz + ".elemtoolong"));
        }

        String qualifier = request.getParameter("qualifier");
        if ("".equals(qualifier))
        {
            qualifier = null;
        }
        if (qualifier != null)
        {
            if (qualifier.length() > 64)
            {
                return error(request, labels.getString(clazz + ".qualtoolong"));
            }
            for (int ii = 0; ii < qualifier.length(); ii++)
            {
                if (qualifier.charAt(ii) == '.' || qualifier.charAt(ii) == '_'
                        || qualifier.charAt(ii) == ' ')
                {
                    return error(request, labels.getString(clazz
                            + ".badqualchar"));
                }
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
}
