/*
 * CollectionWizardServlet.java
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
import java.util.List;

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
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.CommunityDAOFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Collection creation wizard UI
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class CollectionWizardServlet extends DSpaceServlet
{
    /** Initial questions page */
    public final static int INITIAL_QUESTIONS = 1;

    /** Basic information page */
    public final static int BASIC_INFO = 2;

    /** Permissions pages */
    public final static int PERMISSIONS = 3;

    /** Default item page */
    public final static int DEFAULT_ITEM = 4;

    /** Summary page */
    public final static int SUMMARY = 5;

    /** Permissions page for who gets read permissions on new items */
    public final static int PERM_READ = 10;

    /** Permissions page for submitters */
    public final static int PERM_SUBMIT = 11;

    /** Permissions page for workflow step 1 */
    public final static int PERM_WF1 = 12;

    /** Permissions page for workflow step 2 */
    public final static int PERM_WF2 = 13;

    /** Permissions page for workflow step 3 */
    public final static int PERM_WF3 = 14;

    /** Permissions page for collection administrators */
    public final static int PERM_ADMIN = 15;

    /** Logger */
    private static Logger log = Logger.getLogger(CollectionWizardServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        /*
         * For GET, all we should really get is a community_id parameter (DB ID
         * of community to add collection to). doDSPost handles this
         */
        doDSPost(context, request, response);
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        /*
         * For POST, we expect from the form:
         * 
         * community_id DB ID if it was a 'create a new collection' button press
         * 
         * OR
         * 
         * collection_id DB ID of collection we're dealing with stage Stage
         * we're at (from constants above)
         */

        // First, see if we have a multipart request
        // (the 'basic info' page which might include uploading a logo)
        String contentType = request.getContentType();

        if ((contentType != null)
                && (contentType.indexOf("multipart/form-data") != -1))
        {
            // This is a multipart request, so it's a file upload
            processBasicInfo(context, request, response);

            return;
        }

        int communityID = UIUtil.getIntParameter(request, "community_id");

        if (communityID > -1)
        {
            // We have a community ID, "create new collection" button pressed
            Community c =
                CommunityDAOFactory.getInstance(context).retrieve(communityID);

            if (c == null)
            {
                log.warn(LogManager.getHeader(context, "integrity_error",
                        UIUtil.getRequestLogInfo(request)));
                JSPManager.showIntegrityError(request, response);

                return;
            }

            // Create the collection
            Collection newCollection = c.createCollection();
            request.setAttribute("collection", newCollection);
            if (AuthorizeManager.isAdmin(context))
            {
                // set a variable to show all buttons
                request.setAttribute("admin_button", new Boolean(true));
            }
            JSPManager.showJSP(request, response,
                    "/dspace-admin/wizard-questions.jsp");
            context.complete();
        }
        else
        {
            // Collection already created, dealing with one of the wizard pages
            int collectionID = UIUtil.getIntParameter(request, "collection_id");
            int stage = UIUtil.getIntParameter(request, "stage");

            // Get the collection
            Collection collection =
                CollectionDAOFactory.getInstance(context).retrieve(collectionID);

            // Put it in request attributes, as most JSPs will need it
            request.setAttribute("collection", collection);

            if (collection == null)
            {
                log.warn(LogManager.getHeader(context, "integrity_error",
                        UIUtil.getRequestLogInfo(request)));
                JSPManager.showIntegrityError(request, response);

                return;
            }

            // All pages will need this attribute
            request.setAttribute("collection.id", String.valueOf(collection
                    .getID()));

            switch (stage)
            {
            case INITIAL_QUESTIONS:
                processInitialQuestions(context, request, response, collection);

                break;

            case PERMISSIONS:
                processPermissions(context, request, response, collection);

                break;

            case DEFAULT_ITEM:
                processDefaultItem(context, request, response, collection);

                break;

            default:
                log.warn(LogManager.getHeader(context, "integrity_error",
                        UIUtil.getRequestLogInfo(request)));
                JSPManager.showIntegrityError(request, response);
            }
        }
    }

    /**
     * Process input from initial questions page
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     * @param response
     *            HTTP response
     * @param collection
     *            Collection we're editing
     */
    private void processInitialQuestions(Context context,
            HttpServletRequest request, HttpServletResponse response,
            Collection collection) throws SQLException, ServletException,
            IOException, AuthorizeException
    {
        Group anonymousGroup = Group.find(context, 0);

        // "Public read" checkbox. Only need to do anything
        // if it's not checked.
        if (!UIUtil.getBoolParameter(request, "public_read"))
        {
            // Remove anonymous default policies for new items
            AuthorizeManager.removePoliciesActionFilter(context, collection,
                    Constants.DEFAULT_ITEM_READ);
            AuthorizeManager.removePoliciesActionFilter(context, collection,
                    Constants.DEFAULT_BITSTREAM_READ);
        }

        // Some people authorised to submit
        if (UIUtil.getBoolParameter(request, "submitters"))
        {
            // Create submitters group
            Group g = collection.createSubmitters();
        }

        // Check for the workflow steps
        for (int i = 1; i <= 3; i++)
        {
            if (UIUtil.getBoolParameter(request, "workflow" + i))
            {
                // should have workflow step i
                Group g = collection.createWorkflowGroup(i);
            }
        }

        // Check for collection administrators
        if (UIUtil.getBoolParameter(request, "admins"))
        {
            // Create administrators group
            Group g = collection.createAdministrators();
        }

        // Default item stuff?
        if (UIUtil.getBoolParameter(request, "default.item"))
        {
            collection.createTemplateItem();
        }

        // Need to set a name so that the indexer won't throw an exception
        collection.setMetadata("name", "");
        collection.update();

        // Now display "basic info" screen
        JSPManager.showJSP(request, response,
                "/dspace-admin/wizard-basicinfo.jsp");
        context.complete();
    }

    /**
     * Process input from one of the permissions pages
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     * @param response
     *            HTTP response
     * @param collection
     *            Collection we're editing
     */
    private void processPermissions(Context context,
            HttpServletRequest request, HttpServletResponse response,
            Collection collection) throws SQLException, ServletException,
            IOException, AuthorizeException
    {
        // Which permission are we dealing with?
        int permission = UIUtil.getIntParameter(request, "permission");

        // First, we deal with the special case of the MIT group...
        if (UIUtil.getBoolParameter(request, "mitgroup"))
        {
            Group mitGroup = Group.findByName(context, "MIT Users");
            int action;

            if (permission == PERM_READ)
            {
                // assign default item and bitstream read to mitGroup
                AuthorizeManager.addPolicy(context, collection,
                        Constants.DEFAULT_ITEM_READ, mitGroup);
                AuthorizeManager.addPolicy(context, collection,
                        Constants.DEFAULT_BITSTREAM_READ, mitGroup);
            }
            else
            {
                // Must be submit
                AuthorizeManager.addPolicy(context, collection, Constants.ADD,
                        mitGroup);
            }
        }

        //We need to add the selected people to the group.
        // First, get the relevant group
        Group g = null;

        switch (permission)
        {
        case PERM_READ:

            // Actually need to create a group for this.
            g = Group.create(context);

            // Name it according to our conventions
            g
                    .setName("COLLECTION_" + collection.getID()
                            + "_DEFAULT_ITEM_READ");

            // Give it the needed permission
            AuthorizeManager.addPolicy(context, collection,
                    Constants.DEFAULT_ITEM_READ, g);
            AuthorizeManager.addPolicy(context, collection,
                    Constants.DEFAULT_BITSTREAM_READ, g);

            break;

        case PERM_SUBMIT:
            g = collection.getSubmitters();

            break;

        case PERM_WF1:
            g = collection.getWorkflowGroup(1);

            break;

        case PERM_WF2:
            g = collection.getWorkflowGroup(2);

            break;

        case PERM_WF3:
            g = collection.getWorkflowGroup(3);

            break;

        case PERM_ADMIN:
            g = collection.getAdministrators();

            break;
        }

        // Add people and groups from the form to the group
        int[] eperson_ids = UIUtil.getIntParameters(request, "eperson_id");
        int[] group_ids = UIUtil.getIntParameters(request, "group_ids");
        
        if (eperson_ids != null)
        {
            for (int i = 0; i < eperson_ids.length; i++)
            {
                EPerson eperson = EPerson.find(context, eperson_ids[i]);

                if (eperson != null)
                {
                    g.addMember(eperson);
                }
            }
        }
        
        if (group_ids != null)
        {
            for (int i = 0; i < group_ids.length; i++)
            {
                Group group = Group.find(context, group_ids[i]);
            
                if (group != null)
                {
                    g.addMember(group);
                }
            }
        }
        

        // Update group
        g.update();

        showNextPage(context, request, response, collection, permission);

        context.complete();
    }

    /**
     * process input from basic info page
     * 
     * @param context
     * @param request
     * @param response
     * @param collection
     * @throws SQLException
     * @throws ServletException
     * @throws IOException
     * @throws AuthorizeException
     */
    private void processBasicInfo(Context context, HttpServletRequest request,
            HttpServletResponse response) throws SQLException,
            ServletException, IOException, AuthorizeException
    {
        // Wrap multipart request to get the submission info
        FileUploadRequest wrapper = new FileUploadRequest(request);

        Collection collection =
            CollectionDAOFactory.getInstance(context).retrieve(
                    UIUtil.getIntParameter(wrapper, "collection_id"));

        if (collection == null)
        {
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(wrapper)));
            JSPManager.showIntegrityError(request, response);

            return;
        }

        // Get metadata
        collection.setMetadata("name", wrapper.getParameter("name"));
        collection.setMetadata("short_description", wrapper
                .getParameter("short_description"));
        collection.setMetadata("introductory_text", wrapper
                .getParameter("introductory_text"));
        collection.setMetadata("copyright_text", wrapper
                .getParameter("copyright_text"));
        collection.setMetadata("side_bar_text", wrapper
                .getParameter("side_bar_text"));
        collection.setMetadata("provenance_description", wrapper
                .getParameter("provenance_description"));

        // Need to be more careful about license -- make sure it's null if
        // nothing was entered
        String license = wrapper.getParameter("license");

        if ((license != null) || "".equals(license))
        {
            collection.setLicense(license);
        }

        File temp = wrapper.getFile("file");

        if (temp != null)
        {
            // Read the temp file as logo
            InputStream is = new BufferedInputStream(new FileInputStream(temp));
            Bitstream logoBS = collection.setLogo(is);

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

            logoBS.setName(noPath);
            logoBS.setSource(wrapper.getFilesystemName("file"));

            // Identify the format
            BitstreamFormat bf = FormatIdentifier.guessFormat(context, logoBS);
            logoBS.setFormat(bf);
            logoBS.update();

            // Remove temp file
            temp.delete();
        }

        collection.update();

        // Now work out what next page is
        showNextPage(context, request, response, collection, BASIC_INFO);

        context.complete();
    }

    /**
     * Process input from default item page
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     * @param response
     *            HTTP response
     * @param collection
     *            Collection we're editing
     */
    private void processDefaultItem(Context context,
            HttpServletRequest request, HttpServletResponse response,
            Collection collection) throws SQLException, ServletException,
            IOException, AuthorizeException
    {
        Item item = collection.getTemplateItem();

        for (int i = 0; i < 10; i++)
        {
            int dcTypeID = UIUtil.getIntParameter(request, "dctype_" + i);
            String value = request.getParameter("value_" + i);
            String lang = request.getParameter("lang_" + i);

            if ((dcTypeID != -1) && (value != null) && !value.equals(""))
            {
                MetadataField field = MetadataField.find(context,dcTypeID);
                MetadataSchema schema = MetadataSchema.find(context,field.getSchemaID());
                item.addMetadata(schema.getName(),field.getElement(), field.getQualifier(), lang, value);
            }
        }

        item.update();

        // Now work out what next page is
        showNextPage(context, request, response, collection, DEFAULT_ITEM);

        context.complete();
    }

    /**
     * Work out which page to show next, and show it
     * 
     * @param context
     * @param request
     * @param response
     * @param collection
     * @param stage
     *            the stage the user just finished, or if PERMISSIONS, the
     *            particular permissions page
     * @throws SQLException
     * @throws ServletException
     * @throws IOException
     * @throws AuthorizeException
     */
    private void showNextPage(Context context, HttpServletRequest request,
            HttpServletResponse response, Collection collection, int stage)
            throws SQLException, ServletException, IOException,
            AuthorizeException
    {
        // Put collection in request attributes, as most JSPs will need it
        request.setAttribute("collection", collection);

        // FIXME: Not a nice hack -- do we show the MIT users checkbox?
        if (Group.findByName(context, "MIT Users") != null)
        {
            request.setAttribute("mitgroup", new Boolean(true));
        }

        log.debug(LogManager.getHeader(context, "nextpage", "stage=" + stage));

        switch (stage)
        {
        case BASIC_INFO:

            // Next page is 'permission to read' page iff ITEM_DEFAULT_READ
            // for anonymous group is NOT there
            List anonReadPols = AuthorizeManager.getPoliciesActionFilter(
                    context, collection, Constants.DEFAULT_ITEM_READ);

            // At this stage, if there's any ITEM_DEFAULT_READ, it can only
            // be an anonymous one.
            if (anonReadPols.size() == 0)
            {
                request.setAttribute("permission", new Integer(PERM_READ));
                JSPManager.showJSP(request, response,
                        "/dspace-admin/wizard-permissions.jsp");

                break;
            }

        case PERM_READ:

            // Next page is 'permission to submit' iff there's a submit group
            // defined
            if (collection.getSubmitters() != null)
            {
                request.setAttribute("permission", new Integer(PERM_SUBMIT));
                JSPManager.showJSP(request, response,
                        "/dspace-admin/wizard-permissions.jsp");

                break;
            }

        case PERM_SUBMIT:

            // Next page is 'workflow step 1' iff there's a wf step 1 group
            // defined
            if (collection.getWorkflowGroup(1) != null)
            {
                request.setAttribute("permission", new Integer(PERM_WF1));
                JSPManager.showJSP(request, response,
                        "/dspace-admin/wizard-permissions.jsp");

                break;
            }

        case PERM_WF1:

            // Next page is 'workflow step 2' iff there's a wf step 2 group
            // defined
            if (collection.getWorkflowGroup(2) != null)
            {
                request.setAttribute("permission", new Integer(PERM_WF2));
                JSPManager.showJSP(request, response,
                        "/dspace-admin/wizard-permissions.jsp");

                break;
            }

        case PERM_WF2:

            // Next page is 'workflow step 3' iff there's a wf step 2 group
            // defined
            if (collection.getWorkflowGroup(3) != null)
            {
                request.setAttribute("permission", new Integer(PERM_WF3));
                JSPManager.showJSP(request, response,
                        "/dspace-admin/wizard-permissions.jsp");

                break;
            }

        case PERM_WF3:

            // Next page is 'collection administrator' iff there's a collection
            // administrator group
            if (collection.getAdministrators() != null)
            {
                request.setAttribute("permission", new Integer(PERM_ADMIN));
                JSPManager.showJSP(request, response,
                        "/dspace-admin/wizard-permissions.jsp");

                break;
            }

        case PERM_ADMIN:

            // Next page is 'default item' iff there's a default item
            if (collection.getTemplateItem() != null)
            {
                MetadataField[] types = MetadataField.findAll(context);
                request.setAttribute("dctypes", types);
                JSPManager.showJSP(request, response,
                        "/dspace-admin/wizard-default-item.jsp");

                break;
            }

        case DEFAULT_ITEM:

            // Next page is 'summary page (the last page)
            // sort of a hack to pass the community ID to the edit collection
            // page,
            // which needs it in other contexts
            if (collection != null)
            {
                Community[] communities = collection.getCommunities();
                request.setAttribute("community", communities[0]);

                if (AuthorizeManager.isAdmin(context))
                {
                    // set a variable to show all buttons
                    request.setAttribute("admin_button", new Boolean(true));
                }
            }

            JSPManager.showJSP(request, response, "/tools/edit-collection.jsp");

            break;
        }
    }
}
