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
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.log4j.Logger;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.FileUploadRequest;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.service.HarvestedCollectionService;

/**
 * Servlet for editing communities and collections, including deletion,
 * creation, and metadata editing
 * 
 * @author Robert Tansley
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

    /** User committed community edit or creation */
    public static final int CONFIRM_EDIT_COMMUNITY = 7;

    /** User confirmed community deletion */
    public static final int CONFIRM_DELETE_COMMUNITY = 8;

    /** User committed collection edit or creation */
    public static final int CONFIRM_EDIT_COLLECTION = 9;

    /** User wants to delete a collection */
    public static final int CONFIRM_DELETE_COLLECTION = 10;

    /** Logger */
    private static final Logger log = Logger.getLogger(EditCommunitiesServlet.class);

    private final transient CommunityService communityService
             = ContentServiceFactory.getInstance().getCommunityService();
    
    private static final transient CollectionService collectionService
             = ContentServiceFactory.getInstance().getCollectionService();
    
    private final transient BitstreamFormatService bitstreamFormatService
             = ContentServiceFactory.getInstance().getBitstreamFormatService();
    
    private final transient BitstreamService bitstreamService
             = ContentServiceFactory.getInstance().getBitstreamService();
    
    private final transient HarvestedCollectionService harvestedCollectionService
             = HarvestServiceFactory.getInstance().getHarvestedCollectionService();

    private static final transient AuthorizeService myAuthorizeService
            = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    
    private final transient GroupService groupService
             = EPersonServiceFactory.getInstance().getGroupService();
    
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // GET just displays the list of communities and collections
        showControls(context, request, response);
    }

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // First, see if we have a multipart request (uploading a logo)
        String contentType = request.getContentType();

        if ((contentType != null)
                && (contentType.indexOf("multipart/form-data") != -1))
        {
            // This is a multipart request, so it's a file upload
            processUploadLogo(context, request, response);

            return;
        }

        /*
         * Respond to submitted forms. Each form includes an "action" parameter
         * indicating what needs to be done (from the constants above.)
         */
        int action = UIUtil.getIntParameter(request, "action");

        /*
         * Most of the forms supply one or more of these values. Since we just
         * get null if we try and find something with ID -1, we'll just try and
         * find both here to save hassle later on
         */
        Community community = communityService.find(context, UIUtil.getUUIDParameter(
                request, "community_id"));
        Community parentCommunity = communityService.find(context, UIUtil
                .getUUIDParameter(request, "parent_community_id"));
        Collection collection = collectionService.find(context, UIUtil
                .getUUIDParameter(request, "collection_id"));

        // Just about every JSP will need the values we received
        request.setAttribute("community", community);
        request.setAttribute("parent", parentCommunity);
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
            storeAuthorizeAttributeCommunityEdit(context, request, community);
            
            // Display the relevant "edit community" page
            JSPManager.showJSP(request, response, "/tools/edit-community.jsp");

            break;

        case START_DELETE_COMMUNITY:

            // Show "confirm delete" page
            JSPManager.showJSP(request, response,
                    "/tools/confirm-delete-community.jsp");

            break;

        case START_CREATE_COMMUNITY:
            // no authorize attribute will be given to the jsp so a "clean" creation form
            // will be always supplied, advanced setting on policies and admin group creation
            // will be possible after to have completed the community creation

            // Display edit community page with empty fields + create button
            JSPManager.showJSP(request, response, "/tools/edit-community.jsp");

            break;

        case START_EDIT_COLLECTION:
        	HarvestedCollection hc = harvestedCollectionService.find(context, collection);
        	request.setAttribute("harvestInstance", hc);
        	
        	storeAuthorizeAttributeCollectionEdit(context, request, collection);
        	
            // Display the relevant "edit collection" page
            JSPManager.showJSP(request, response, "/tools/edit-collection.jsp");

            break;

        case START_DELETE_COLLECTION:

            // Show "confirm delete" page
            JSPManager.showJSP(request, response,
                    "/tools/confirm-delete-collection.jsp");

            break;

        case START_CREATE_COLLECTION:

            // Forward to collection creation wizard
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/tools/collection-wizard?community_id="
                    + community.getID()));

            break;

        case CONFIRM_EDIT_COMMUNITY:

            // Edit or creation of a community confirmed
            processConfirmEditCommunity(context, request, response, community);

            break;

        case CONFIRM_DELETE_COMMUNITY:

            // remember the parent community, if any
            Community parent = (Community) communityService.getParentObject(context, community);

            // Delete the community
            communityService.delete(context, community);

            // if community was top-level, redirect to community-list page
            if (parent == null)
            {
                response.sendRedirect(response.encodeRedirectURL(request
                        .getContextPath()
                        + "/community-list"));
            }
            else
            // redirect to parent community page
            {
                response.sendRedirect(response.encodeRedirectURL(request
                        .getContextPath()
                        + "/handle/" + parent.getHandle()));
            }

            // Show main control page
            //showControls(context, request, response);
            // Commit changes to DB
            context.complete();

            break;

        case CONFIRM_EDIT_COLLECTION:

            // Edit or creation of a collection confirmed
            processConfirmEditCollection(context, request, response, community,
                    collection);

            break;

        case CONFIRM_DELETE_COLLECTION:

            // Delete the collection
            communityService.removeCollection(context, community, collection);
            // remove the collection object from the request, so that the user
            // will be redirected on the community home page
            request.removeAttribute("collection");
            // Show main control page
            showControls(context, request, response);

            // Commit changes to DB
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
     * Store in the request attribute to teach to the jsp which button are
     * needed/allowed for the community edit form
     * 
     * @param context
     * @param request
     * @param community
     * @throws SQLException
     */
    private void storeAuthorizeAttributeCommunityEdit(Context context,
            HttpServletRequest request, Community community) throws SQLException
    {
        try 
        {
            AuthorizeUtil.authorizeManageAdminGroup(context, community);                
            request.setAttribute("admin_create_button", Boolean.TRUE);
        }
        catch (AuthorizeException authex) {
            request.setAttribute("admin_create_button", Boolean.FALSE);
        }
        
        try 
        {
            AuthorizeUtil.authorizeRemoveAdminGroup(context, community);                
            request.setAttribute("admin_remove_button", Boolean.TRUE);
        }
        catch (AuthorizeException authex) {
            request.setAttribute("admin_remove_button", Boolean.FALSE);
        }
        
        if (myAuthorizeService.authorizeActionBoolean(context, community, Constants.DELETE))
        {
            request.setAttribute("delete_button", Boolean.TRUE);
        }
        else
        {
            request.setAttribute("delete_button", Boolean.FALSE);
        }
        
        try 
        {
            AuthorizeUtil.authorizeManageCommunityPolicy(context, community);                
            request.setAttribute("policy_button", Boolean.TRUE);
        }
        catch (AuthorizeException authex) {
            request.setAttribute("policy_button", Boolean.FALSE);
        }
        if (myAuthorizeService.isAdmin(context, community))
        {
            request.setAttribute("admin_community", Boolean.TRUE);
        }
        else
        {
            request.setAttribute("admin_community", Boolean.FALSE);
        }

    }
    
    /**
     * Store in the request attribute to teach to the jsp which button are
     * needed/allowed for the collection edit form
     * 
     * @param context
     * @param request
     * @param community
     * @throws SQLException
     */
    static void storeAuthorizeAttributeCollectionEdit(Context context,
            HttpServletRequest request, Collection collection) throws SQLException
    {
        if (myAuthorizeService.isAdmin(context, collection))
        {
            request.setAttribute("admin_collection", Boolean.TRUE);
        }
        else
        {
            request.setAttribute("admin_collection", Boolean.FALSE);
        }
        
        try 
        {
            AuthorizeUtil.authorizeManageAdminGroup(context, collection);                
            request.setAttribute("admin_create_button", Boolean.TRUE);
        }
        catch (AuthorizeException authex) {
            request.setAttribute("admin_create_button", Boolean.FALSE);
        }
        
        try 
        {
            AuthorizeUtil.authorizeRemoveAdminGroup(context, collection);                
            request.setAttribute("admin_remove_button", Boolean.TRUE);
        }
        catch (AuthorizeException authex) {
            request.setAttribute("admin_remove_button", Boolean.FALSE);
        }
        
        try 
        {
            AuthorizeUtil.authorizeManageSubmittersGroup(context, collection);                
            request.setAttribute("submitters_button", Boolean.TRUE);
        }
        catch (AuthorizeException authex) {
            request.setAttribute("submitters_button", Boolean.FALSE);
        }
        
        try 
        {
            AuthorizeUtil.authorizeManageWorkflowsGroup(context, collection);                
            request.setAttribute("workflows_button", Boolean.TRUE);
        }
        catch (AuthorizeException authex) {
            request.setAttribute("workflows_button", Boolean.FALSE);
        }
        
        try 
        {
            AuthorizeUtil.authorizeManageTemplateItem(context, collection);                
            request.setAttribute("template_button", Boolean.TRUE);
        }
        catch (AuthorizeException authex) {
            request.setAttribute("template_button", Boolean.FALSE);
        }
        
        if (myAuthorizeService.authorizeActionBoolean(context, collectionService.getParentObject(context, collection), Constants.REMOVE))
        {
            request.setAttribute("delete_button", Boolean.TRUE);
        }
        else
        {
            request.setAttribute("delete_button", Boolean.FALSE);
        }
        
        try 
        {
            AuthorizeUtil.authorizeManageCollectionPolicy(context, collection);                
            request.setAttribute("policy_button", Boolean.TRUE);
        }
        catch (AuthorizeException authex) {
            request.setAttribute("policy_button", Boolean.FALSE);
        }        
    }

    /**
     * Show community home page with admin controls
     * 
     * @param context
     *            Current DSpace context
     * @param request
     *            Current HTTP request
     * @param response
     *            Current HTTP response
     */
    private void showControls(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // new approach - eliminate the 'list-communities' page in favor of the
        // community home page, enhanced with admin controls. If no community,
        // or no parent community, just fall back to the community-list page
        Community community = (Community) request.getAttribute("community");
        Collection collection = (Collection) request.getAttribute("collection");
        
        if (collection != null)
        {
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/handle/" + collection.getHandle()));
        }
        else if (community != null)
        {
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/handle/" + community.getHandle()));
        }
        else
        {
            // see if a parent community was specified
            Community parent = (Community) request.getAttribute("parent");

            if (parent != null)
            {
                response.sendRedirect(response.encodeRedirectURL(request
                        .getContextPath()
                        + "/handle/" + parent.getHandle()));
            }
            else
            {
                // fall back on community-list page
                response.sendRedirect(response.encodeRedirectURL(request
                        .getContextPath()
                        + "/community-list"));
            }
        }
    }

    /**
     * Create/update community metadata from a posted form
     * 
     * @param context
     *            DSpace context
     * @param request
     *            the HTTP request containing posted info
     * @param response
     *            the HTTP response
     * @param community
     *            the community to update (or null for creation)
     */
    private void processConfirmEditCommunity(Context context,
            HttpServletRequest request, HttpServletResponse response,
            Community community) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        if (request.getParameter("create").equals("true"))
        {
            // if there is a parent community id specified, create community
            // as its child; otherwise, create it as a top-level community
            UUID parentCommunityID = UIUtil.getUUIDParameter(request,
                    "parent_community_id");

            if (parentCommunityID != null)
            {
                Community parent = communityService.find(context, parentCommunityID);

                if (parent != null)
                {
                    community = communityService.createSubcommunity(context, parent);
                }
            }
            else
            {
                community = communityService.create(null, context);
            }

            // Set attribute
            request.setAttribute("community", community);
        }

        storeAuthorizeAttributeCommunityEdit(context, request, community);
        
        communityService.setMetadata(context, community, "name", request.getParameter("name"));
        communityService.setMetadata(context, community, "short_description", request
                .getParameter("short_description"));

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

        communityService.setMetadata(context, community, "introductory_text", intro);
        communityService.setMetadata(context, community,"copyright_text", copy);
        communityService.setMetadata(context, community,"side_bar_text", side);
        communityService.update(context, community);

        // Which button was pressed?
        String button = UIUtil.getSubmitButton(request, "submit");

        if (button.equals("submit_set_logo"))
        {
            // Change the logo - delete any that might be there first
            communityService.setLogo(context, community, null);
            communityService.update(context, community);

            // Display "upload logo" page. Necessary attributes already set by
            // doDSPost()
            JSPManager.showJSP(request, response,
                    "/dspace-admin/upload-logo.jsp");
        }
        else if (button.equals("submit_delete_logo"))
        {
            // Simply delete logo
            communityService.setLogo(context, community, null);
            communityService.update(context, community);

            // Show edit page again - attributes set in doDSPost()
            JSPManager.showJSP(request, response, "/tools/edit-community.jsp");
        }
        else if (button.equals("submit_authorization_edit"))
        {
            // Forward to policy edit page
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/tools/authorize?community_id="
                    + community.getID() + "&submit_community_select=1"));
        }
        else if (button.equals("submit_curate_community"))
        {
            // Forward to policy edit page
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/tools/curate?community_id="
                    + community.getID() + "&submit_community_select=1"));
        }
        else if (button.equals("submit_admins_create"))
        {
            // Create new group
            Group newGroup = communityService.createAdministrators(context, community);
            communityService.update(context, community);

            // Forward to group edit page
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/tools/group-edit?group_id=" + newGroup.getID()));
        }
        else if (button.equals("submit_admins_remove"))
        {
            Group g = community.getAdministrators(); 
            communityService.removeAdministrators(context, community);
            communityService.update(context, community);
            groupService.delete(context, g);
            // Show edit page again - attributes set in doDSPost()
            JSPManager.showJSP(request, response, "/tools/edit-community.jsp");
        }   
        else if (button.equals("submit_admins_edit"))
        {
            // Edit 'community administrators' group
            Group g = community.getAdministrators();
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/tools/group-edit?group_id=" + g.getID()));
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
     * @param context
     *            DSpace context
     * @param request
     *            the HTTP request containing posted info
     * @param response
     *            the HTTP response
     * @param community
     *            the community the collection is in
     * @param collection
     *            the collection to update (or null for creation)
     */
    private void processConfirmEditCollection(Context context,
            HttpServletRequest request, HttpServletResponse response,
            Community community, Collection collection)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        if (request.getParameter("create").equals("true"))
        {
            // We need to create a new community
            collection = collectionService.create(context, community);
            request.setAttribute("collection", collection);
        }
        
        storeAuthorizeAttributeCollectionEdit(context, request, collection);

        // Update the basic metadata
        collectionService.setMetadata(context, collection, "name", request.getParameter("name"));
        collectionService.setMetadata(context, collection, "short_description", request
                .getParameter("short_description"));

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

        collectionService.setMetadata(context, collection, "introductory_text", intro);
        collectionService.setMetadata(context, collection, "copyright_text", copy);
        collectionService.setMetadata(context, collection, "side_bar_text", side);
        collectionService.setMetadata(context, collection, "license", license);
        collectionService.setMetadata(context, collection, "provenance_description", provenance);
        
        
        
        
        // Set the harvesting settings
        
        HarvestedCollection hc = harvestedCollectionService.find(context, collection);
		String contentSource = request.getParameter("source");

		// First, if this is not a harvested collection (anymore), set the harvest type to 0; wipe harvest settings  
		if (contentSource.equals("source_normal")) 
		{
			if (hc != null)
            {
                harvestedCollectionService.delete(context, hc);
            }
		}
		else 
		{
			// create a new harvest instance if all the settings check out
			if (hc == null) {
				hc = harvestedCollectionService.create(context, collection);
			}
			
			String oaiProvider = request.getParameter("oai_provider");
			String oaiSetId = request.getParameter("oai_setid");
			String metadataKey = request.getParameter("metadata_format");
			String harvestType = request.getParameter("harvest_level");

			hc.setHarvestParams(Integer.parseInt(harvestType), oaiProvider, oaiSetId, metadataKey);
			hc.setHarvestStatus(HarvestedCollection.STATUS_READY);
			
			harvestedCollectionService.update(context, hc);
		}
        
        

        // Which button was pressed?
        String button = UIUtil.getSubmitButton(request, "submit");

        if (button.equals("submit_set_logo"))
        {
            // Change the logo - delete any that might be there first
            collectionService.setLogo(context, collection, null);

            // Display "upload logo" page. Necessary attributes already set by
            // doDSPost()
            JSPManager.showJSP(request, response,
                    "/dspace-admin/upload-logo.jsp");
        }
        else if (button.equals("submit_delete_logo"))
        {
            // Simply delete logo
            collectionService.setLogo(context, collection, null);

            // Show edit page again - attributes set in doDSPost()
            JSPManager.showJSP(request, response, "/tools/edit-collection.jsp");
        }
        else if (button.startsWith("submit_wf_create_"))
        {
            int step = Integer.parseInt(button.substring(17));

            // Create new group
            Group newGroup = collectionService.createWorkflowGroup(context, collection, step);
            collectionService.update(context, collection);

            // Forward to group edit page
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/tools/group-edit?group_id=" + newGroup.getID()));
        }
        else if (button.equals("submit_admins_create"))
        {
            // Create new group
            Group newGroup = collectionService.createAdministrators(context, collection);
            collectionService.update(context, collection);
            
            // Forward to group edit page
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/tools/group-edit?group_id=" + newGroup.getID()));
        }
        else if (button.equals("submit_admins_delete"))
        {
        	// Remove the administrators group.
        	Group g = collection.getAdministrators();
        	collectionService.removeAdministrators(context, collection);
            collectionService.update(context, collection);
            groupService.delete(context, g);

            // Show edit page again - attributes set in doDSPost()
            JSPManager.showJSP(request, response, "/tools/edit-collection.jsp");
        }
        else if (button.equals("submit_submitters_create"))
        {
            // Create new group
            Group newGroup = collectionService.createSubmitters(context, collection);
            collectionService.update(context, collection);
            
            // Forward to group edit page
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/tools/group-edit?group_id=" + newGroup.getID()));
        }
        else if (button.equals("submit_submitters_delete"))
        {
        	// Remove the administrators group.
        	Group g = collection.getSubmitters();
        	collectionService.removeSubmitters(context, collection);
            collectionService.update(context, collection);
            groupService.delete(context, g);

            // Show edit page again - attributes set in doDSPost()
            JSPManager.showJSP(request, response, "/tools/edit-collection.jsp");
        }
        else if (button.equals("submit_authorization_edit"))
        {
            // Forward to policy edit page
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/tools/authorize?collection_id="
                    + collection.getID() + "&submit_collection_select=1"));
        }
        else if (button.equals("submit_curate_collection"))
        {
            // Forward to policy edit page
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/tools/curate?collection_id="
                    + collection.getID() + "&submit_collection_select=1"));
        }
        else if (button.startsWith("submit_wf_edit_"))
        {
            int step = Integer.parseInt(button.substring(15));

            // Edit workflow group
            Group g = collectionService.getWorkflowGroup(collection, step);
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/tools/group-edit?group_id=" + g.getID()));
        }
        else if (button.equals("submit_submitters_edit"))
        {
            // Edit submitters group
            Group g = collection.getSubmitters();
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/tools/group-edit?group_id=" + g.getID()));
        }
        else if (button.equals("submit_admins_edit"))
        {
            // Edit 'collection administrators' group
            Group g = collection.getAdministrators();
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/tools/group-edit?group_id=" + g.getID()));
        }
        else if (button.startsWith("submit_wf_delete_"))
        {
            // Delete workflow group
            int step = Integer.parseInt(button.substring(17));

            Group g = collectionService.getWorkflowGroup(collection, step);
            collection.setWorkflowGroup(step, null);

            // Have to update to avoid ref. integrity error
            collectionService.update(context, collection);
            groupService.delete(context, g);

            // Show edit page again - attributes set in doDSPost()
            JSPManager.showJSP(request, response, "/tools/edit-collection.jsp");
        }
        else if (button.equals("submit_create_template"))
        {
            // Create a template item
            collectionService.createTemplateItem(context, collection);

            // Forward to edit page for new template item
            Item i = collection.getTemplateItem();            

            // save the changes
            collectionService.update(context, collection);
            context.complete();
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/tools/edit-item?item_id=" + i.getID()));

            return;
        }
        else if (button.equals("submit_edit_template"))
        {
            // Forward to edit page for template item
            Item i = collection.getTemplateItem();
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/tools/edit-item?item_id=" + i.getID()));
        }
        else if (button.equals("submit_delete_template"))
        {
            collectionService.removeTemplateItem(context, collection);

            // Show edit page again - attributes set in doDSPost()
            JSPManager.showJSP(request, response, "/tools/edit-collection.jsp");
        }
        else
        {
            // Plain old "create/update" button pressed - go back to main page
            showControls(context, request, response);
        }

        // Commit changes to DB
        collectionService.update(context, collection);
        context.complete();
    }

    /**
     * Process the input from the upload logo page
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     */
    private void processUploadLogo(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        try {
            // Wrap multipart request to get the submission info
            FileUploadRequest wrapper = new FileUploadRequest(request);
            Community community = communityService.find(context, UIUtil.getUUIDParameter(wrapper, "community_id"));
            Collection collection = collectionService.find(context, UIUtil.getUUIDParameter(wrapper, "collection_id"));
            File temp = wrapper.getFile("file");

            // Read the temp file as logo
            InputStream is = new BufferedInputStream(new FileInputStream(temp));
            Bitstream logoBS;

            if (collection == null)
            {
                logoBS = communityService.setLogo(context, community, is);
            }
            else
            {
                logoBS = collectionService.setLogo(context, collection, is);
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

            logoBS.setName(context, noPath);
            logoBS.setSource(context, wrapper.getFilesystemName("file"));

            // Identify the format
            BitstreamFormat bf = bitstreamFormatService.guessFormat(context, logoBS);
            logoBS.setFormat(context, bf);
            myAuthorizeService.addPolicy(context, logoBS, Constants.WRITE, context.getCurrentUser());
            bitstreamService.update(context, logoBS);

            String jsp;
            DSpaceObject dso;
            if (collection == null)
            {
                communityService.update(context, community);

                // Show community edit page
                request.setAttribute("community", community);
                storeAuthorizeAttributeCommunityEdit(context, request, community);
                dso = community;
                jsp = "/tools/edit-community.jsp";
            } 
            else
            {
                collectionService.update(context, collection);

                // Show collection edit page
                request.setAttribute("collection", collection);
                request.setAttribute("community", community);
                storeAuthorizeAttributeCollectionEdit(context, request, collection);
                dso = collection;
                jsp = "/tools/edit-collection.jsp";
            }
            
            if (myAuthorizeService.isAdmin(context, dso))
            {
                // set a variable to show all buttons
                request.setAttribute("admin_button", Boolean.TRUE);
            }

            JSPManager.showJSP(request, response, jsp);

            // Remove temp file
            if (!temp.delete())
            {
                log.error("Unable to delete temporary file");
            }

            // Update DB
            context.complete();
        } catch (FileSizeLimitExceededException ex)
        {
            log.warn("Upload exceeded upload.max");
            JSPManager.showFileSizeLimitExceededError(request, response, ex.getMessage(), ex.getActualSize(), ex.getPermittedSize());
        }
    }
}
