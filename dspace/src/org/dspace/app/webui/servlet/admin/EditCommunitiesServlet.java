/*
 * EditCommunities.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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
import java.io.InputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.oreilly.servlet.MultipartWrapper;

import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;


/**
 * Servlet for editing communities and collections, including deletion,
 * creation, and metadata editing
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class EditCommunitiesServlet extends DSpaceServlet
{
    /** User wants to edit a community */
    public static final int START_EDIT_COMMUNITY = 1;

    /** User wants to delete a community */
    public static final int START_DELETE_COMMUNITY = 2;

    /** User wants to create a community */
    public static final int START_CREATE_COMMUNITY = 3;

    /** User wants to edit a collection */
    public static final int START_EDIT_COLLECTION = 4;

    /** User wants to delete a collection */
    public static final int START_DELETE_COLLECTION = 5;

    /** User wants to create a collection */
    public static final int START_CREATE_COLLECTION = 6;

    /** User commited community edit or creation */
    public static final int CONFIRM_EDIT_COMMUNITY = 7;

    /** User confirmed community deletion*/
    public static final int CONFIRM_DELETE_COMMUNITY = 8;

    /** User commited collection edit or creation */
    public static final int CONFIRM_EDIT_COLLECTION = 9;

    /** User wants to delete a collection */
    public static final int CONFIRM_DELETE_COLLECTION = 10;


    /** Logger */
    private static Logger log = Logger.getLogger(EditCommunitiesServlet.class);


    protected void doDSGet(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // GET just displays the list of communities and collections
        showControls(context, request, response);
    }


    protected void doDSPost(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // First, see if we have a multipart request (uploading a logo)
        String contentType = request.getContentType();

        if (contentType != null &&
            contentType.indexOf("multipart/form-data") != -1)
        {
            // This is a multipart request, so it's a file upload
            processUploadLogo(context, request, response);
            return;
        }

        /*
         * Respond to submitted forms.  Each form includes an "action" parameter
         * indicating what needs to be done (from the constants above.)
         */
        int action = UIUtil.getIntParameter(request, "action");

        /*
         * Most of the forms supply one or both of these values.  Since we just
         * get null if we try and find something with ID -1, we'll just try
         * and find both here to save hassle later on
         */
        Community community = Community.find(context,
            UIUtil.getIntParameter(request, "community_id"));
        Collection collection = Collection.find(context,
            UIUtil.getIntParameter(request, "collection_id"));

        // Just about every JSP will need the values we received
        request.setAttribute("community", community);
        request.setAttribute("collection", collection);

        /*
         * First we check for a "cancel" button - if it's been pressed, we
         * simply return to the main control page
         */
        if (request.getParameter("submit_cancel") != null)
        {
            showControls(context, request, response);
            return;
        }

        // Now proceed according to "action" parameter
        switch (action)
        {
        case START_EDIT_COMMUNITY:
            // Display the relevant "edit community" page
            JSPManager.showJSP(request, response, "/admin/edit-community.jsp");
            break;

        case START_DELETE_COMMUNITY:
            // Show "confirm delete" page
            JSPManager.showJSP(request, response,
                "/admin/confirm-delete-community.jsp");
            break;

        case START_CREATE_COMMUNITY:
            // Display edit community page with empty fields + create button
            JSPManager.showJSP(request, response, "/admin/edit-community.jsp");
            break;

        case START_EDIT_COLLECTION:
            // Display the relevant "edit collection" page
            JSPManager.showJSP(request, response, "/admin/edit-collection.jsp");
            break;

        case START_DELETE_COLLECTION:
            // Show "confirm delete" page
            JSPManager.showJSP(request, response,
                "/admin/confirm-delete-collection.jsp");
            break;

        case START_CREATE_COLLECTION:
            // Display edit collection page with empty fields + create button
            JSPManager.showJSP(request, response, "/admin/edit-collection.jsp");
            break;

        case CONFIRM_EDIT_COMMUNITY:
            // Edit or creation of a community confirmed
            processConfirmEditCommunity(context, request, response, community);
            break;

        case CONFIRM_DELETE_COMMUNITY:
            // Delete the community
            community.delete();

             // Show main control page
            showControls(context, request, response);

            // Commit changes to DB
            context.complete();
            break;

        case CONFIRM_EDIT_COLLECTION:
            // Edit or creation of a collection confirmed
            processConfirmEditCollection(
                context, request, response, community, collection);
            break;

        case CONFIRM_DELETE_COLLECTION:
            // Delete the collection
            community.removeCollection(collection);

             // Show main control page
            showControls(context, request, response);

            // Commit changes to DB
            context.complete();
            break;

        default:
            // Erm... weird action value received.
            log.warn(LogManager.getHeader(context,
                "integrity_error",
                UIUtil.getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
        }
    }


    /**
     * Show list of communities and collections with controls
     *
     * @param context   Current DSpace context
     * @param request   Current HTTP request
     * @param response  Current HTTP response
     */
    private void showControls(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        Community[] communities = Community.findAll(context);

        // Build up a map in which the keys are community IDs and the
        // values are collections - this is so the JSP doesn't have to do
        // the work
        Map communityIDToCollection = new HashMap();

        for (int i = 0; i < communities.length; i++)
        {
            communityIDToCollection.put(new Integer(communities[i].getID()),
                communities[i].getCollections());
        }

        log.info(LogManager.getHeader(context,
            "view_editcommunities",
            ""));

        // Set attributes for JSP
        request.setAttribute("communities", communities);
        request.setAttribute("collections.map", communityIDToCollection);

        JSPManager.showJSP(request, response, "/admin/list-communities.jsp");
    }


    /**
     * Create/update community metadata from a posted form
     *
     * @param context     DSpace context
     * @param request     the HTTP request containing posted info
     * @param response    the HTTP response
     * @param community   the community to update (or null for creation)
     */
    private void processConfirmEditCommunity(Context context,
        HttpServletRequest request,
        HttpServletResponse response,
        Community community)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        if (request.getParameter("create").equals("true"))
        {
            // We need to create a new community
            community = Community.create(context);
        }

        community.setMetadata("name", request.getParameter("name"));
        community.setMetadata("short_description",
            request.getParameter("short_description"));

        String intro = request.getParameter("introductory_text");
        if (intro.equals(""))
        {
            intro = null;
        }

        String copy = request.getParameter("copyright_text");
        if (copy.equals(""))
        {
            copy = null;
        }

        String side = request.getParameter("side_bar_text");
        if (side.equals(""))
        {
            side = null;
        }

        community.setMetadata("introductory_text", intro);
        community.setMetadata("copyright_text", copy);
        community.setMetadata("side_bar_text", side);
        community.update();

        // Which button was pressed?
        String button = UIUtil.getSubmitButton(request, "submit");

        if (button.equals("submit_set_logo"))
        {
            // Change the logo - delete any that might be there first
            community.setLogo(null);
            community.update();

            // Display "upload logo" page.  Necessary attributes already set by
            // doDSPost()
            JSPManager.showJSP(request, response, "/admin/upload-logo.jsp");
        }
        else if(button.equals("submit_delete_logo"))
        {
            // Simply delete logo
            community.setLogo(null);
            community.update();

            // Show edit page again - attributes set in doDSPost()
            JSPManager.showJSP(request, response, "/admin/edit-community.jsp");
        }
        else
        {
            // Button at bottom clicked - show main control page
            showControls(context, request, response);
        }

        // Commit changes to DB
        context.complete();
    }


    /**
     * Create/update collection metadata from a posted form
     *
     * @param context     DSpace context
     * @param request     the HTTP request containing posted info
     * @param response    the HTTP response
     * @param community   the community the collection is in
     * @param collection  the collection to update (or null for creation)
     */
    private void processConfirmEditCollection(Context context,
        HttpServletRequest request,
        HttpServletResponse response,
        Community community,
        Collection collection)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        if (request.getParameter("create").equals("true"))
        {
            // We need to create a new community
            collection = community.createCollection();
            request.setAttribute("collection", collection);
        }

        // Update the basic metadata

        collection.setMetadata("name", request.getParameter("name"));
        collection.setMetadata("short_description",
            request.getParameter("short_description"));

        String intro = request.getParameter("introductory_text");
        if (intro.equals(""))
        {
            intro = null;
        }

        String copy = request.getParameter("copyright_text");
        if (copy.equals(""))
        {
            copy = null;
        }

        String side = request.getParameter("side_bar_text");
        if (side.equals(""))
        {
            side = null;
        }

        String license = request.getParameter("license");
        if (license.equals(""))
        {
            license = null;
        }

        String provenance = request.getParameter("provenance_description");
        if (provenance.equals(""))
        {
            provenance = null;
        }

        collection.setMetadata("introductory_text", intro);
        collection.setMetadata("copyright_text", copy);
        collection.setMetadata("side_bar_text", side);
        collection.setMetadata("license", license);
        collection.setMetadata("provenance_description", license);

        // Which button was pressed?
        String button = UIUtil.getSubmitButton(request, "submit");

        if (button.equals("submit_set_logo"))
        {
            // Change the logo - delete any that might be there first
            collection.setLogo(null);

            // Display "upload logo" page.  Necessary attributes already set by
            // doDSPost()
            JSPManager.showJSP(request, response, "/admin/upload-logo.jsp");
        }
        else if(button.equals("submit_delete_logo"))
        {
            // Simply delete logo
            collection.setLogo(null);

            // Show edit page again - attributes set in doDSPost()
            JSPManager.showJSP(request, response, "/admin/edit-collection.jsp");
        }
        else if(button.startsWith("submit_wf_create_"))
        {
            int step = Integer.parseInt(button.substring(17));

            // Create new group
            Group newGroup = Group.create(context);
            newGroup.setName("COLLECTION_" + collection.getID() + "_WFSTEP_" +
                step);
            newGroup.update();
            collection.setWorkflowGroup(step, newGroup);

            // Forward to group edit page
            response.sendRedirect(response.encodeRedirectURL(
                request.getContextPath() + "/admin/groups?group=" +
                    newGroup.getID()));
        }
        else if(button.startsWith("submit_wf_edit_"))
        {
            int step = Integer.parseInt(button.substring(15));

            // Edit workflow group
            Group g = collection.getWorkflowGroup(step);
            response.sendRedirect(response.encodeRedirectURL(
                request.getContextPath() + "/admin/groups?group=" +
                    g.getID()));
        }
        else if(button.startsWith("submit_wf_delete_"))
        {
            // Delete workflow group
            int step = Integer.parseInt(button.substring(17));

            Group g = collection.getWorkflowGroup(step);
            collection.setWorkflowGroup(step, null);
            // Have to update to avoid ref. integrity error
            collection.update();
            g.delete();

            // Show edit page again - attributes set in doDSPost()
            JSPManager.showJSP(request, response, "/admin/edit-collection.jsp");
        }
        else if(button.equals("submit_create_template"))
        {
            // Create a template item
            collection.createTemplateItem();

            // Forward to edit page for new template item
            Item i = collection.getTemplateItem();
            response.sendRedirect(response.encodeRedirectURL(
                request.getContextPath() + "/admin/edit_item?item_id=" +
                    i.getID()));
        }
        else if(button.equals("submit_edit_template"))
        {
            // Forward to edit page for template item
            Item i = collection.getTemplateItem();
            response.sendRedirect(response.encodeRedirectURL(
                request.getContextPath() + "/admin/edit_item?item_id=" +
                    i.getID()));
        }
        else if(button.equals("submit_delete_template"))
        {
            collection.removeTemplateItem();

            // Show edit page again - attributes set in doDSPost()
            JSPManager.showJSP(request, response, "/admin/edit-collection.jsp");
        }
        else
        {
            // Plain old "create/update" button pressed - go back to main page
            showControls(context, request, response);
        }

        // Commit changes to DB
        collection.update();
        context.complete();
    }


    /**
     * Process the input from the upload logo page
     *
     * @param context   current DSpace context
     * @param request   current servlet request object
     * @param response  current servlet response object
     */
    private void processUploadLogo(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // Wrap multipart request to get the submission info
		String tempDir = ConfigurationManager.getProperty("upload.temp.dir");

        MultipartWrapper wrapper = new MultipartWrapper(request, tempDir);

        Community community = Community.find(context,
            UIUtil.getIntParameter(wrapper, "community_id"));
        Collection collection = Collection.find(context,
            UIUtil.getIntParameter(wrapper, "collection_id"));

        File temp = wrapper.getFile("file");

        // Read the temp file as logo
        InputStream is = new BufferedInputStream(new FileInputStream(
            temp));
        Bitstream logoBS;

        if (community != null)
        {
            logoBS = community.setLogo(is);
        }
        else
        {
            logoBS = collection.setLogo(is);
        }

        // Strip all but the last filename.  It would be nice
        // to know which OS the file came from.
        String noPath = wrapper.getFilesystemName("file");
        while (noPath.indexOf('/') > -1)
        {
            noPath = noPath.substring(
                        noPath.indexOf('/') + 1);
        }
        while (noPath.indexOf('\\') > -1)
        {
            noPath = noPath.substring(
                        noPath.indexOf('\\') + 1);
        }

        logoBS.setName(noPath);
        logoBS.setSource(wrapper.getFilesystemName("file"));

        // Identify the format
        BitstreamFormat bf = FormatIdentifier.guessFormat(context, logoBS);
        logoBS.setFormat(bf);
        logoBS.update();

        if (community != null)
        {
            community.update();

            // Show community edit page
            request.setAttribute("community", community);
            JSPManager.showJSP(request, response, "/admin/edit-community.jsp");
        }
        else
        {
            collection.update();
            // Show collection edit page
            request.setAttribute("collection", collection);
            JSPManager.showJSP(request, response, "/admin/edit-collection.jsp");
        }

		// Remove temp file
		temp.delete();

        // Update DB
        context.complete();
    }
}
