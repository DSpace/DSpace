/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.app.util.Util;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.FileUploadRequest;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.license.CCLicense;
import org.dspace.license.CCLookup;
import org.dspace.license.CreativeCommons;

/**
 * Servlet for metadata editing of any DSpace Object
 *
 * @author Andrea Bollini
 * @version $Revision$
 */
public class EditDSpaceObjectServlet extends DSpaceServlet
{
    /** User updates item */
    public static final int UPDATE_ITEM = 3;

    /** Logger */
    private static Logger log = Logger.getLogger(EditCommunitiesServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        /*
         * GET with no parameters displays "find by handle/id" form parameter
         * item_id -> find and edit item with internal ID item_id parameter
         * handle -> find and edit corresponding item if internal ID or Handle
         * are invalid, "find by handle/id" form is displayed again with error
         * message
         */
        int internalID = UIUtil.getIntParameter(request, "resource_id");
        int resourceTypeID = UIUtil.getIntParameter(request, "resource_type");

        boolean showError = false;

        // See if an item ID or Handle was passed in
        DSpaceObject itemToEdit = null;
        context.turnOffItemWrapper();
        if (internalID > -1 && resourceTypeID > -1)
        {
            itemToEdit = DSpaceObject.find(context, resourceTypeID, internalID);
            showError = (itemToEdit == null);
        }
        context.restoreItemWrapperState();
        
        // Show edit form if appropriate
        if (itemToEdit != null)
        {
            // now check to see if person can edit item
            checkEditAuthorization(context, itemToEdit);
            showEditForm(context, request, response, itemToEdit);
        }
        else
        {
            if (showError)
            {
                request.setAttribute("invalid.id", Boolean.TRUE);
            }

            JSPManager.showJSP(request, response, "/tools/get-dso-id.jsp");
        }
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        /*
         * Then we check for a "cancel" button - if it's been pressed, we simply
         * return to the "find by handle/id" page
         */
        if (request.getParameter("submit_cancel") != null)
        {
            JSPManager.showJSP(request, response, "/tools/get-item-id.jsp");

            return;
        }
        
        /*
         * Respond to submitted forms. Each form includes an "action" parameter
         * indicating what needs to be done (from the constants above.)
         */
        int action = UIUtil.getIntParameter(request, "action");
        context.turnOffItemWrapper();
		DSpaceObject item = DSpaceObject.find(context, UIUtil.getIntParameter(request, "resource_type"),
				UIUtil.getIntParameter(request, "resource_id"));
        context.restoreItemWrapperState();

        String handle = HandleManager.findHandle(context, item);

        // now check to see if person can edit item
        checkEditAuthorization(context, item);

        request.setAttribute("item", item);
        request.setAttribute("handle", handle);

        switch (action)
        {
        case UPDATE_ITEM:
            processUpdateItem(context, request, response, item);

            break;

        default:

            // Erm... weird action value received.
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
        }
    }

    /**
     * Throw an exception if user isn't authorized to edit this item
     *
     * @param c
     * @param item
     */
    private void checkEditAuthorization(Context c, DSpaceObject item)
            throws AuthorizeException, java.sql.SQLException
    {
        AuthorizeManager.authorizeAction(c, item, Constants.WRITE);
    }

    /**
     * Show the item edit form for a particular item
     *
     * @param context
     *            DSpace context
     * @param request
     *            the HTTP request containing posted info
     * @param response
     *            the HTTP response
     * @param item
     *            the item
     */
    private void showEditForm(Context context, HttpServletRequest request,
            HttpServletResponse response, DSpaceObject item) throws ServletException,
            IOException, SQLException, AuthorizeException
    {
  
        // Get the handle, if any
        String handle = HandleManager.findHandle(context, item);

        DSpaceObject parent = item.getParentObject();

        // All DC types in the registry
        MetadataField[] types = MetadataField.findAll(context);
        
        // Get a HashMap of metadata field ids and a field name to display
        Map<Integer, String> metadataFields = new HashMap<Integer, String>();
        
        // Get all existing Schemas
        MetadataSchema[] schemas = MetadataSchema.findAll(context);
        for (int i = 0; i < schemas.length; i++)
        {
            String schemaName = schemas[i].getName();
            // Get all fields for the given schema
            MetadataField[] fields = MetadataField.findAllInSchema(context, schemas[i].getSchemaID());
            for (int j = 0; j < fields.length; j++)
            {
                Integer fieldID = Integer.valueOf(fields[j].getFieldID());
                String displayName = "";
                displayName = schemaName + "." + fields[j].getElement() + (fields[j].getQualifier() == null ? "" : "." + fields[j].getQualifier());
                metadataFields.put(fieldID, displayName);
            }
        }

        request.setAttribute("item", item);
        request.setAttribute("handle", handle);
        request.setAttribute("parent", parent);
        request.setAttribute("dc.types", types);
        request.setAttribute("metadataFields", metadataFields);
        
        if(response.isCommitted()) {
        	return;
        }
        JSPManager.showJSP(request, response, "/tools/edit-dso-form.jsp");
    }

    /**
     * Process input from the edit item form
     *
     * @param context
     *            DSpace context
     * @param request
     *            the HTTP request containing posted info
     * @param response
     *            the HTTP response
     * @param item
     *            the item
     */
    private void processUpdateItem(Context context, HttpServletRequest request,
            HttpServletResponse response, DSpaceObject item) throws ServletException,
            IOException, SQLException, AuthorizeException
    {
        String button = UIUtil.getSubmitButton(request, "submit");
        /*
         * "Cancel" handled above, so whatever happens, we need to update the
         * item metadata. First, we remove it all, then build it back up again.
         */
        item.clearMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        // We'll sort the parameters by name. This ensures that DC fields
        // of the same element/qualifier are added in the correct sequence.
        // Get the parameters names
        Enumeration unsortedParamNames = request.getParameterNames();

        // Put them in a list
        List<String> sortedParamNames = new LinkedList<String>();

        while (unsortedParamNames.hasMoreElements())
        {
            sortedParamNames.add((String)unsortedParamNames.nextElement());
        }

        // Sort the list
        Collections.sort(sortedParamNames);

        for (String p : sortedParamNames)
        {
            if (p.startsWith("value"))
            {
                /*
                 * It's a metadata value - it will be of the form
                 * value_element_1 OR value_element_qualifier_2 (the number
                 * being the sequence number) We use a StringTokenizer to
                 * extract these values
                 */
                StringTokenizer st = new StringTokenizer(p, "_");

                st.nextToken(); // Skip "value"

                String schema = st.nextToken();

                String element = st.nextToken();

                String qualifier = null;

                if (st.countTokens() == 2)
                {
                    qualifier = st.nextToken();
                }

                String sequenceNumber = st.nextToken();

                // Get a string with "element" for unqualified or
                // "element_qualifier"
                String key = MetadataField.formKey(schema,element,qualifier);

                // Get the language
                String language = request.getParameter("language_" + key + "_"
                        + sequenceNumber);

                // trim language and set empty string language = null
                if (language != null)
                {
                    language = language.trim();
                    if (language.equals(""))
                    {
                        language = null;
                    }
                }

                // Get the authority key if any
                String authority = request.getParameter("choice_" + key + "_authority_"
                        + sequenceNumber);

                // Empty string authority = null
                if ((authority != null) && authority.equals(""))
                {
                    authority = null;
                }

                // Get the authority confidence value, passed as symbolic name
                String sconfidence = request.getParameter("choice_" + key + "_confidence_" + sequenceNumber);
                int confidence = (sconfidence == null || sconfidence.equals("")) ?
                                 Choices.CF_NOVALUE : Choices.getConfidenceValue(sconfidence);

                // Get the value
                String value = request.getParameter(p).trim();

                // If remove button pressed for this value, we don't add it
                // back to the item. We also don't add empty values
                // (if no authority is specified).
                if (!((value.equals("") && authority == null) || button.equals("submit_remove_" + key
                        + "_" + sequenceNumber)))
                {
                    // Value is empty, or remove button for this wasn't pressed
                    item.addMetadata(schema, element, qualifier, language, value,
                            authority, confidence);
                }
            }
        }

        /*
         * Now respond to button presses, other than "Remove" or "Delete" button
         * presses which were dealt with in the above loop.
         */
        if (button.equals("submit_addfield"))
        {
            // Adding a metadata field
            int dcTypeID = UIUtil.getIntParameter(request, "addfield_dctype");
            String value = request.getParameter("addfield_value").trim();
            String lang = request.getParameter("addfield_language");

            // trim language and set empty string language = null
            if (lang != null)
            {
                lang = lang.trim();
                if (lang.equals(""))
                {
                    lang = null;
                }
            }

            MetadataField field = MetadataField.find(context, dcTypeID);
            MetadataSchema schema = MetadataSchema.find(context, field
                    .getSchemaID());
            item.addMetadata(schema.getName(), field.getElement(), field
                    .getQualifier(), lang, value);
        }

        item.update();

    	// commit now to make available in the edit form changes made by optional consumers
        context.commit();
        // Show edit page again
        showEditForm(context, request, response, item);
        
        // Complete transaction
        context.complete();
    }
}
