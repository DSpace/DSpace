/*
 * EditItemServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.webui.servlet.admin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
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
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.search.DSIndexer;
import org.dspace.license.CreativeCommons;

/**
 * Servlet for editing and deleting (expunging) items
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class EditItemServlet extends DSpaceServlet
{
    /** User wants to delete (expunge) an item */
    public static final int START_DELETE = 1;

    /** User confirms delete (expunge) of item */
    public static final int CONFIRM_DELETE = 2;

    /** User updates item */
    public static final int UPDATE_ITEM = 3;

    /** User starts withdrawal of item */
    public static final int START_WITHDRAW = 4;

    /** User confirms withdrawal of item */
    public static final int CONFIRM_WITHDRAW = 5;

    /** User reinstates a withdrawn item */
    public static final int REINSTATE = 6;

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
        int internalID = UIUtil.getIntParameter(request, "item_id");
        String handle = request.getParameter("handle");
        boolean showError = false;

        // See if an item ID or Handle was passed in
        Item itemToEdit = null;

        if (internalID > 0)
        {
            itemToEdit = Item.find(context, internalID);

            showError = (itemToEdit == null);
        }
        else if ((handle != null) && !handle.equals(""))
        {
            // resolve handle
            DSpaceObject dso = HandleManager.resolveToObject(context, handle.trim());

            // make sure it's an ITEM
            if ((dso != null) && (dso.getType() == Constants.ITEM))
            {
                itemToEdit = (Item) dso;
                showError = false;
            }
            else
            {
                showError = true;
            }
        }

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
                request.setAttribute("invalid.id", new Boolean(true));
            }

            JSPManager.showJSP(request, response, "/tools/get-item-id.jsp");
        }
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // First, see if we have a multipart request (uploading a new bitstream)
        String contentType = request.getContentType();

        if ((contentType != null)
                && (contentType.indexOf("multipart/form-data") != -1))
        {
            // This is a multipart request, so it's a file upload
            processUploadBitstream(context, request, response);

            return;
        }

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

        Item item = Item.find(context, UIUtil.getIntParameter(request,
                "item_id"));
 
        String handle = HandleManager.findHandle(context, item);

        // now check to see if person can edit item
        checkEditAuthorization(context, item);

        request.setAttribute("item", item);
        request.setAttribute("handle", handle);

        switch (action)
        {
        case START_DELETE:

            // Show "delete item" confirmation page
            JSPManager.showJSP(request, response,
                    "/tools/confirm-delete-item.jsp");

            break;

        case CONFIRM_DELETE:

            // Delete the item - if "cancel" was pressed this would be
            // picked up above
            // FIXME: Don't know if this does all it should - remove Handle?
            Collection[] collections = item.getCollections();

            // Remove item from all the collections it's in
            for (int i = 0; i < collections.length; i++)
            {
                collections[i].removeItem(item);
            }

            JSPManager.showJSP(request, response, "/tools/get-item-id.jsp");
            context.complete();

            break;

        case UPDATE_ITEM:
            processUpdateItem(context, request, response, item);

            break;

        case START_WITHDRAW:

            // Show "withdraw item" confirmation page
            JSPManager.showJSP(request, response,
                    "/tools/confirm-withdraw-item.jsp");

            break;

        case CONFIRM_WITHDRAW:

            // Withdraw the item
            item.withdraw();
            JSPManager.showJSP(request, response, "/tools/get-item-id.jsp");
            context.complete();

            break;

        case REINSTATE:
            item.reinstate();
            JSPManager.showJSP(request, response, "/tools/get-item-id.jsp");
            context.complete();

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
     * @param context
     * @param item
     */
    private void checkEditAuthorization(Context c, Item item)
            throws AuthorizeException, java.sql.SQLException
    {
        if (!item.canEdit())
        {
            int userID = 0;

            // first, check if userid is set
            if (c.getCurrentUser() != null)
            {
                userID = c.getCurrentUser().getID();
            }

            // show an error or throw an authorization exception
            throw new AuthorizeException("EditItemServlet: User " + userID
                    + " not authorized to edit item " + item.getID());
        }
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
            HttpServletResponse response, Item item) throws ServletException,
            IOException, SQLException, AuthorizeException
    {
        if ( request.getParameter("cc_license_url") != null )
        {
        	// set or replace existing CC license
        	CreativeCommons.setLicense( context, item,
                   request.getParameter("cc_license_url") );
        	context.commit();
        }
  
        // Get the handle, if any
        String handle = HandleManager.findHandle(context, item);

        // Collections
        Collection[] collections = item.getCollections();

        // All DC types in the registry
        MetadataField[] types = MetadataField.findAll(context);
        
        // Get a HashMap of metadata field ids and a field name to display
        HashMap metadataFields = new HashMap();
        
        // Get all existing Schemas
        MetadataSchema[] schemas = MetadataSchema.findAll(context);
        for (int i = 0; i < schemas.length; i++)
        {
            String schemaName = schemas[i].getName();
            // Get all fields for the given schema
            MetadataField[] fields = MetadataField.findAllInSchema(context, schemas[i].getSchemaID());
            for (int j = 0; j < fields.length; j++)
            {
                Integer fieldID = new Integer(fields[j].getFieldID());
                String displayName = "";
                displayName = schemaName + "." + fields[j].getElement() + (fields[j].getQualifier() == null ? "" : "." + fields[j].getQualifier());
                metadataFields.put(fieldID, displayName);
            }
        }

        request.setAttribute("item", item);
        request.setAttribute("handle", handle);
        request.setAttribute("collections", collections);
        request.setAttribute("dc.types", types);
        request.setAttribute("metadataFields", metadataFields);

        JSPManager.showJSP(request, response, "/tools/edit-item-form.jsp");
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
            HttpServletResponse response, Item item) throws ServletException,
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
        List sortedParamNames = new LinkedList();

        while (unsortedParamNames.hasMoreElements())
        {
            sortedParamNames.add(unsortedParamNames.nextElement());
        }

        // Sort the list
        Collections.sort(sortedParamNames);

        Iterator iterator = sortedParamNames.iterator();

        while (iterator.hasNext())
        {
            String p = (String) iterator.next();

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

                // Empty string language = null
                if ((language != null) && language.equals(""))
                {
                    language = null;
                }

                // Get the value
                String value = request.getParameter(p).trim();

                // If remove button pressed for this value, we don't add it
                // back to the item. We also don't add empty values.
                if (!(value.equals("") || button.equals("submit_remove_" + key
                        + "_" + sequenceNumber)))
                {
                    // Value is empty, or remove button for this wasn't pressed
                    item.addMetadata(schema, element, qualifier, language, value);
                }
            }
            // only process bitstreams if admin
            else if (p.startsWith("bitstream_name")
                    && AuthorizeManager.isAdmin(context))
            {
                // We have bitstream metadata
                // First, get the bundle and bitstream ID
                // Parameter name is bitstream_name_(bundleID)_(bitstreamID)
                StringTokenizer st = new StringTokenizer(p, "_");

                // Ignore "bitstream" and "name"
                st.nextToken();
                st.nextToken();

                // Bundle ID and bitstream ID next
                int bundleID = Integer.parseInt(st.nextToken());
                int bitstreamID = Integer.parseInt(st.nextToken());

                Bundle bundle = Bundle.find(context, bundleID);
                Bitstream bitstream = Bitstream.find(context, bitstreamID);

                // Get the string "(bundleID)_(bitstreamID)" for finding other
                // parameters related to this bitstream
                String key = String.valueOf(bundleID) + "_" + bitstreamID;

                // Update bitstream metadata, or delete?
                if (button.equals("submit_delete_bitstream_" + key))
                {
                    // "delete" button pressed
                    bundle.removeBitstream(bitstream);

                    // Delete bundle too, if empty
                    if (bundle.getBitstreams().length == 0)
                    {
                        item.removeBundle(bundle);
                    }
                }
                else
                {
                    // Update the bitstream metadata
                    String name = request.getParameter(p);
                    String source = request.getParameter("bitstream_source_"
                            + key);
                    String desc = request.getParameter("bitstream_description_"
                            + key);
                    int formatID = UIUtil.getIntParameter(request,
                            "bitstream_format_id_" + key);
                    String userFormatDesc = request
                            .getParameter("bitstream_user_format_description_"
                                    + key);
                    int primaryBitstreamID = UIUtil.getIntParameter(request,
                            bundleID + "_primary_bitstream_id");

                    // Empty strings become non-null
                    if (source.equals(""))
                    {
                        source = null;
                    }

                    if (desc.equals(""))
                    {
                        desc = null;
                    }

                    if (userFormatDesc.equals(""))
                    {
                        userFormatDesc = null;
                    }

                    bitstream.setName(name);
                    bitstream.setSource(source);
                    bitstream.setDescription(desc);
                    bitstream
                            .setFormat(BitstreamFormat.find(context, formatID));

                    if (primaryBitstreamID > 0)
                    {
                        bundle.setPrimaryBitstreamID(primaryBitstreamID);
                    }

                    if (userFormatDesc != null)
                    {
                        bitstream.setUserFormatDescription(userFormatDesc);
                    }

                    bitstream.update();
                    bundle.update();
                }
            }
        }

        item.update();

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

            // Empty language = null
            if (lang.equals(""))
            {
                lang = null;
            }

            MetadataField field = MetadataField.find(context, dcTypeID);
            MetadataSchema schema = MetadataSchema.find(context, field
                    .getSchemaID());
            item.addMetadata(schema.getName(), field.getElement(), field
                    .getQualifier(), lang, value);
            item.update();
        }

        if (button.equals("submit_addcc"))
        {
            // Show cc-edit page 
            request.setAttribute("item", item);
            JSPManager
                    .showJSP(request, response, "/tools/creative-commons-edit.jsp");
        }
        
        if (button.equals("submit_addbitstream"))
        {
            // Show upload bitstream page
            request.setAttribute("item", item);
            JSPManager
                    .showJSP(request, response, "/tools/upload-bitstream.jsp");
        }
        else
        {
            // Show edit page again
            showEditForm(context, request, response, item);
        }
        
        // update the item index
        DSIndexer.reIndexContent(context, item);

        // Complete transaction
        context.complete();
    }

    /**
     * Process the input from the upload bitstream page
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     */
    private void processUploadBitstream(Context context,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // Wrap multipart request to get the submission info
        FileUploadRequest wrapper = new FileUploadRequest(request);
        Bitstream b = null;

        Item item = Item.find(context, UIUtil.getIntParameter(wrapper,
                "item_id"));

        File temp = wrapper.getFile("file");

        // Read the temp file as logo
        InputStream is = new BufferedInputStream(new FileInputStream(temp));

        // now check to see if person can edit item
        checkEditAuthorization(context, item);

        // do we already have an ORIGINAL bundle?
        Bundle[] bundles = item.getBundles("ORIGINAL");

        if (bundles.length < 1)
        {
            // set bundle's name to ORIGINAL
            b = item.createSingleBitstream(is, "ORIGINAL");
        }
        else
        {
            // we have a bundle already, just add bitstream
            b = bundles[0].createBitstream(is);
        }

        // Strip all but the last filename. It would be nice
        // to know which OS the file came from.
        String noPath = wrapper.getFilesystemName("file");

        while (noPath.indexOf('/') > -1)
        {
            noPath = noPath.substring(noPath.indexOf('/') + 1);
        }

        while (noPath.indexOf('\\') > -1)
        {
            noPath = noPath.substring(noPath.indexOf('\\') + 1);
        }

        b.setName(noPath);
        b.setSource(wrapper.getFilesystemName("file"));

        // Identify the format
        BitstreamFormat bf = FormatIdentifier.guessFormat(context, b);
        b.setFormat(bf);
        b.update();

        item.update();

        // Back to edit form
        showEditForm(context, request, response, item);

        // Remove temp file
        temp.delete();

        // Update DB
        context.complete();
    }
}