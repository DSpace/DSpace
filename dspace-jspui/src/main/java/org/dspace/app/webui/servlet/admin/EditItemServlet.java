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
import java.util.UUID;

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
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.authority.Choices;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.license.CCLicense;
import org.dspace.license.CCLookup;
import org.dspace.license.LicenseMetadataValue;
import org.dspace.license.factory.LicenseServiceFactory;
import org.dspace.license.service.CreativeCommonsService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

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

    /** User starts the movement of an item */
    public static final int START_MOVE_ITEM = 7;

    /** User confirms the movement of the item */
    public static final int CONFIRM_MOVE_ITEM = 8;

    /** User starts withdrawal of item */
    public static final int START_PRIVATING = 9;

    /** User confirms withdrawal of item */
    public static final int CONFIRM_PRIVATING = 10;

    /** User confirms withdrawal of item */
    public static final int PUBLICIZE = 11;

    /** User updates Creative Commons License */
    public static final int UPDATE_CC = 12;
    
    /** JSP to upload bitstream */
    protected static final String UPLOAD_BITSTREAM_JSP = "/tools/upload-bitstream.jsp";

    /** Logger */
    private static final Logger log = Logger.getLogger(EditCommunitiesServlet.class);

    private final transient CollectionService collectionService
             = ContentServiceFactory.getInstance().getCollectionService();
    
    private final transient ItemService itemService
             = ContentServiceFactory.getInstance().getItemService();
    
    private final transient BitstreamFormatService bitstreamFormatService
             = ContentServiceFactory.getInstance().getBitstreamFormatService();
    
    private final transient BitstreamService bitstreamService
             = ContentServiceFactory.getInstance().getBitstreamService();
    
    private final transient BundleService bundleService
             = ContentServiceFactory.getInstance().getBundleService();
    
    private final transient HandleService handleService
             = HandleServiceFactory.getInstance().getHandleService();
    
    private final transient MetadataFieldService metadataFieldService
             = ContentServiceFactory.getInstance().getMetadataFieldService();
    
    private final transient MetadataSchemaService metadataSchemaService
             = ContentServiceFactory.getInstance().getMetadataSchemaService();
    
    private final transient CreativeCommonsService creativeCommonsService
             = LicenseServiceFactory.getInstance().getCreativeCommonsService();
    
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    
    @Override
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
        UUID internalID = UIUtil.getUUIDParameter(request, "item_id");
        String handle = request.getParameter("handle");
        boolean showError = false;

        // See if an item ID or Handle was passed in
        Item itemToEdit = null;

        if (internalID != null)
        {
            itemToEdit = itemService.find(context, internalID);

            showError = (itemToEdit == null);
        }
        else if ((handle != null) && !handle.equals(""))
        {
            // resolve handle
            DSpaceObject dso = handleService.resolveToObject(context, handle.trim());

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
                request.setAttribute("invalid.id", Boolean.TRUE);
            }

            JSPManager.showJSP(request, response, "/tools/get-item-id.jsp");
        }
    }

    @Override
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

        Item item = itemService.find(context, UIUtil.getUUIDParameter(request,
                "item_id"));
 
        if (request.getParameter("submit_cancel_cc") != null)
        {
            showEditForm(context, request, response, item);

            return;
        }
        
        String handle = handleService.findHandle(context, item);

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
            itemService.delete(context, item);
            
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
            itemService.withdraw(context, item);
            JSPManager.showJSP(request, response, "/tools/get-item-id.jsp");
            context.complete();

            break;

        case REINSTATE:
            itemService.reinstate(context, item);
            JSPManager.showJSP(request, response, "/tools/get-item-id.jsp");
            context.complete();

            break;

        case START_MOVE_ITEM:
                if (authorizeService.isAdmin(context, item))
                {
                        // Display move collection page with fields of collections and communities
                        List<Collection> allNotLinkedCollections = itemService.getCollectionsNotLinked(context, item);
                        List<Collection> allLinkedCollections = item.getCollections();
                    
                        // get only the collection where the current user has the right permission
                        List<Collection> authNotLinkedCollections = new ArrayList<>();
                        for (Collection c : allNotLinkedCollections)
                        {
                            if (authorizeService.authorizeActionBoolean(context, c, Constants.ADD))
                            {
                                authNotLinkedCollections.add(c);
                            }
                        }

                List<Collection> authLinkedCollections = new ArrayList<>();
                for (Collection c : allLinkedCollections)
                {
                    if (authorizeService.authorizeActionBoolean(context, c, Constants.REMOVE))
                    {
                        authLinkedCollections.add(c);
                    }
                }
                        
                        request.setAttribute("linkedCollections", authLinkedCollections);
                        request.setAttribute("notLinkedCollections", authNotLinkedCollections);
                                    
                        JSPManager.showJSP(request, response, "/tools/move-item.jsp");
                } else
                {
                        throw new ServletException("You must be an administrator to move an item");
                }
                
                break;
                        
        case CONFIRM_MOVE_ITEM:
                if (authorizeService.isAdmin(context, item))
                {
                        Collection fromCollection = collectionService.find(context, UIUtil.getUUIDParameter(request, "collection_from_id"));
                        Collection toCollection = collectionService.find(context, UIUtil.getUUIDParameter(request, "collection_to_id"));

                        Boolean inheritPolicies = false;
                        if (request.getParameter("inheritpolicies") != null)
                        {
                            inheritPolicies = true;
                        }

                        if (fromCollection == null || toCollection == null)
                        {
                                throw new ServletException("Missing or incorrect collection IDs for moving item");
                        }
                                    
                        itemService.move(context, item, fromCollection, toCollection, inheritPolicies);
                    
                    showEditForm(context, request, response, item);
        
                    context.complete();
                } else
                {
                        throw new ServletException("You must be an administrator to move an item");
                }
                
                break;

        case START_PRIVATING:

            // Show "withdraw item" confirmation page
            JSPManager.showJSP(request, response,
                    "/tools/confirm-privating-item.jsp");

            break;

        case CONFIRM_PRIVATING:

            // Withdraw the item
            item.setDiscoverable(false);
            itemService.update(context, item);
            JSPManager.showJSP(request, response, "/tools/get-item-id.jsp");
            context.complete();

            break;

        case PUBLICIZE:
            item.setDiscoverable(true);
            itemService.update(context, item);
            JSPManager.showJSP(request, response, "/tools/get-item-id.jsp");
            context.complete();

            break;
                
        case UPDATE_CC:

           	Map<String, String> map = new HashMap<String, String>();
        	String licenseclass = (request.getParameter("licenseclass_chooser") != null) ? request.getParameter("licenseclass_chooser") : "";
        	String jurisdiction = (configurationService.getProperty("cc.license.jurisdiction") != null) ? configurationService.getProperty("cc.license.jurisdiction") : "";
        	if (licenseclass.equals("standard")) {
        		map.put("commercial", request.getParameter("commercial_chooser"));
        		map.put("derivatives", request.getParameter("derivatives_chooser"));
        	} else if (licenseclass.equals("recombo")) {
        		map.put("sampling", request.getParameter("sampling_chooser"));
        	}
        	map.put("jurisdiction", jurisdiction);
        	
        	LicenseMetadataValue uriField = creativeCommonsService.getCCField("uri");
        	LicenseMetadataValue nameField = creativeCommonsService.getCCField("name");
        	
        	boolean exit = false;
			if (licenseclass.equals("webui.Submission.submit.CCLicenseStep.no_license")) 
        	{
				creativeCommonsService.removeLicense(context, uriField, nameField, item);
				
				itemService.update(context, item);
	            context.dispatchEvents();
    			exit = true;
        	}
        	else if (licenseclass.equals("webui.Submission.submit.CCLicenseStep.select_change")) {
        		//none
        		exit = true;
			}
        	
			if (!exit) {
				CCLookup ccLookup = new CCLookup();
				ccLookup.issue(licenseclass, map, configurationService.getProperty("cc.license.locale"));
				if (ccLookup.isSuccess()) {
					creativeCommonsService.removeLicense(context, uriField, nameField, item);

					uriField.addItemValue(context, item, ccLookup.getLicenseUrl());
					if (configurationService.getBooleanProperty("cc.submit.addbitstream")) {
						creativeCommonsService.setLicenseRDF(context, item, ccLookup.getRdf());
					}
					if (configurationService.getBooleanProperty("cc.submit.setname")) {
						nameField.addItemValue(context, item, ccLookup.getLicenseName());
					}

					itemService.update(context, item);
					context.dispatchEvents();

				}
			}
            showEditForm(context, request, response, item);
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
     * @param c
     * @param item
     */
    private void checkEditAuthorization(Context c, Item item)
            throws AuthorizeException, java.sql.SQLException
    {
        if (!itemService.canEdit(c, item))
        {
            UUID userID = null;

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
  
        // Get the handle, if any
        String handle = handleService.findHandle(context, item);

        // Collections
        List<Collection> collections = item.getCollections();

        // All DC types in the registry
        List<MetadataField> types = metadataFieldService.findAll(context);
        
        // Get a HashMap of metadata field ids and a field name to display
        Map<Integer, String> metadataFields = new HashMap<>();
        
        // Get all existing Schemas
        List<MetadataSchema> schemas = metadataSchemaService.findAll(context);
        for (MetadataSchema s : schemas)
        {
            String schemaName = s.getName();
            // Get all fields for the given schema
            List<MetadataField> fields = metadataFieldService.findAllInSchema(context, s);
            for (MetadataField f : fields)
            {
                String displayName = "";
                displayName = schemaName + "." + f.getElement() + (f.getQualifier() == null ? "" : "." + f.getQualifier());
                metadataFields.put(f.getID(), displayName);
            }
        }

        request.setAttribute("admin_button", authorizeService.authorizeActionBoolean(context, item, Constants.ADMIN));
        try
        {
            AuthorizeUtil.authorizeManageItemPolicy(context, item);
            request.setAttribute("policy_button", Boolean.TRUE);
        }
        catch (AuthorizeException authex)
        {
            request.setAttribute("policy_button", Boolean.FALSE);
        }
        
        if (authorizeService.authorizeActionBoolean(context, itemService
                .getParentObject(context, item), Constants.REMOVE))
        {
            request.setAttribute("delete_button", Boolean.TRUE);
        }
        else
        {
            request.setAttribute("delete_button", Boolean.FALSE);
        }
        
        try
        {
            authorizeService.authorizeAction(context, item, Constants.ADD);
            request.setAttribute("create_bitstream_button", Boolean.TRUE);
        }
        catch (AuthorizeException authex)
        {
            request.setAttribute("create_bitstream_button", Boolean.FALSE);
        }
        
        try
        {
            authorizeService.authorizeAction(context, item, Constants.REMOVE);
            request.setAttribute("remove_bitstream_button", Boolean.TRUE);
        }
        catch (AuthorizeException authex)
        {
            request.setAttribute("remove_bitstream_button", Boolean.FALSE);
        }
        
        try
        {
            AuthorizeUtil.authorizeManageCCLicense(context, item);
            request.setAttribute("cclicense_button", Boolean.TRUE);
        }
        catch (AuthorizeException authex)
        {
            request.setAttribute("cclicense_button", Boolean.FALSE);
        }
        
        try
        {
            if( 0 < itemService.getBundles(item, "ORIGINAL").size()){
                AuthorizeUtil.authorizeManageBundlePolicy(context, itemService.getBundles(item, "ORIGINAL").get(0));
                request.setAttribute("reorder_bitstreams_button", Boolean.TRUE);
            }
        }
        catch (AuthorizeException authex)
        {
            request.setAttribute("reorder_bitstreams_button", Boolean.FALSE);
        }

        if (!item.isWithdrawn())
        {
            try
            {
                AuthorizeUtil.authorizeWithdrawItem(context, item);
                request.setAttribute("withdraw_button", Boolean.TRUE);
            }
            catch (AuthorizeException authex)
            {
                request.setAttribute("withdraw_button", Boolean.FALSE);
            }
        }
        else
        {
            try
            {
                AuthorizeUtil.authorizeReinstateItem(context, item);
                request.setAttribute("reinstate_button", Boolean.TRUE);
            }
            catch (AuthorizeException authex)
            {
                request.setAttribute("reinstate_button", Boolean.FALSE);
            }
        }

		if (item.isDiscoverable()) 
		{
			request.setAttribute("privating_button", authorizeService
					.authorizeActionBoolean(context, item, Constants.WRITE));
		} 
		else 
		{
			request.setAttribute("publicize_button", authorizeService
					.authorizeActionBoolean(context, item, Constants.WRITE));
		}
        
        request.setAttribute("item", item);
        request.setAttribute("handle", handle);
        request.setAttribute("collections", collections);
        request.setAttribute("dc.types", types);
        request.setAttribute("metadataFields", metadataFields);

        if(response.isCommitted()) {
        	return;
        }
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
        itemService.clearMetadata(context, item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        // We'll sort the parameters by name. This ensures that DC fields
        // of the same element/qualifier are added in the correct sequence.
        // Get the parameters names
        Enumeration unsortedParamNames = request.getParameterNames();

        // Put them in a list
        List<String> sortedParamNames = new LinkedList<>();

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
                String key = metadataFieldService.findByElement(context, 
                		schema,element,qualifier).toString();

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
                    itemService.addMetadata(context, item, schema, element, qualifier, language, value,
                            authority, confidence);
                }
            }
            else if (p.startsWith("bitstream_name"))
            {
                // We have bitstream metadata
                // First, get the bundle and bitstream ID
                // Parameter name is bitstream_name_(bundleID)_(bitstreamID)
                StringTokenizer st = new StringTokenizer(p, "_");

                // Ignore "bitstream" and "name"
                st.nextToken();
                st.nextToken();

                // Bundle ID and bitstream ID next
                UUID bundleID = UUID.fromString(st.nextToken());
                UUID bitstreamID = UUID.fromString(st.nextToken());

                Bundle bundle = bundleService.find(context, bundleID);
                Bitstream bitstream = bitstreamService.find(context, bitstreamID);

                // Get the string "(bundleID)_(bitstreamID)" for finding other
                // parameters related to this bitstream
                String key = String.valueOf(bundleID) + "_" + bitstreamID;

                // Update bitstream metadata, or delete?
                if (button.equals("submit_delete_bitstream_" + key))
                {
                    // "delete" button pressed
                    bundleService.removeBitstream(context, bundle, bitstream);

                    // Delete bundle too, if empty
                    if (bundle.getBitstreams().size() == 0)
                    {
                        itemService.removeBundle(context, item, bundle);
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
                    UUID primaryBitstreamID = UIUtil.getUUIDParameter(request,
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

                    bitstream.setName(context, name);
                    bitstream.setSource(context, source);
                    bitstream.setDescription(context, desc);
                    bitstream
                            .setFormat(context, bitstreamFormatService.find(context, formatID));

                    if (primaryBitstreamID != null)
                    {
                        bundle.setPrimaryBitstreamID(bitstreamService.find(context, primaryBitstreamID));
                    }

                    if (userFormatDesc != null)
                    {
                        bitstream.setUserFormatDescription(context, userFormatDesc);
                    }

                    bitstreamService.update(context, bitstream);
                    bundleService.update(context, bundle);
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

            MetadataField field = metadataFieldService.find(context, dcTypeID);
            MetadataSchema schema = field.getMetadataSchema();
            itemService.addMetadata(context, item, schema.getName(), field.getElement(), field
                    .getQualifier(), lang, value);
        }

        itemService.update(context, item);

        if (button.equals("submit_addcc"))
        {
            // Show cc-edit page
            request.setAttribute("item", item);
            
            boolean exists = creativeCommonsService.hasLicense(context, item);
            request.setAttribute("cclicense.exists", Boolean.valueOf(exists));

            String ccLocale = configurationService.getProperty("cc.license.locale");
            /** Default locale to 'en' */
            ccLocale = (StringUtils.isNotBlank(ccLocale)) ? ccLocale : "en";
            request.setAttribute("cclicense.locale", ccLocale);
            
            CCLookup cclookup = new CCLookup();
            java.util.Collection<CCLicense> collectionLicenses = cclookup.getLicenses(ccLocale);
            request.setAttribute("cclicense.licenses", collectionLicenses);
            
            JSPManager
                    .showJSP(request, response, "/tools/creative-commons-edit.jsp");
        }
        
        if (button.equals("submit_addbitstream"))
        {
            // Show upload bitstream page
            request.setAttribute("item", item);
            JSPManager
                    .showJSP(request, response, UPLOAD_BITSTREAM_JSP);
        }else
        if(button.equals("submit_update_order") || button.startsWith("submit_order_"))
        {
            List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
            for (Bundle bundle : bundles) {
                List<Bitstream> bitstreams = bundle.getBitstreams();
                UUID[] newBitstreamOrder = new UUID[bitstreams.size()];
                if (button.equals("submit_update_order")) {
                    for (Bitstream bitstream : bitstreams) {
                        //The order is determined by javascript
                        //For each of our bitstream retrieve the order value
                        int order = Util.getIntParameter(request, "order_" + bitstream.getID());
                        //-1 the order since the order needed to start from one
                        order--;
                        //Place the bitstream identifier in the correct order
                        newBitstreamOrder[order] = bitstream.getID();
                    }
                }else{
                    //Javascript isn't operational retrieve the value from the hidden field
                    //Retrieve the button key
                    String inputKey = button.replace("submit_order_", "") + "_value";
                    if(inputKey.startsWith(bundle.getID() + "_")){
                        List<UUID> vals = Util.getUUIDParameters(request, inputKey);
                        int idx = 0;
                        for (UUID v : vals) {
                            newBitstreamOrder[idx] = v;
                            idx++;
                        }
                    }else{
                        newBitstreamOrder = null;
                    }

                }

                if(newBitstreamOrder != null){
                    //Set the new order in our bundle !
                    bundleService.setOrder(context, bundle, newBitstreamOrder);
                    bundleService.update(context, bundle);
                }
            }

            // Show edit page again
            showEditForm(context, request, response, item);
        }
        else
        {
            // Show edit page again
            showEditForm(context, request, response, item);
        }
        
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
        try {
            // Wrap multipart request to get the submission info
            FileUploadRequest wrapper = new FileUploadRequest(request);
            Bitstream b = null;
            Item item = itemService.find(context, UIUtil.getUUIDParameter(wrapper, "item_id"));
            File temp = wrapper.getFile("file");
            
            if(temp == null)
            {
                boolean noFileSelected = true;
                
                // Show upload bitstream page
                request.setAttribute("noFileSelected", noFileSelected);
                request.setAttribute("item", item);
                JSPManager
                        .showJSP(request, response, UPLOAD_BITSTREAM_JSP);
                return;
            }
            // Read the temp file as logo
            InputStream is = new BufferedInputStream(new FileInputStream(temp));

            // now check to see if person can edit item
            checkEditAuthorization(context, item);

            // do we already have an ORIGINAL bundle?
            List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");

            if (bundles == null || bundles.size() == 0)
            {
                // set bundle's name to ORIGINAL
                b = itemService.createSingleBitstream(context, is, item, "ORIGINAL");

                // set the permission as defined in the owning collection
                Collection owningCollection = item.getOwningCollection();
                if (owningCollection != null)
                {
                    Bundle bnd = b.getBundles().get(0);
                    bundleService.inheritCollectionDefaultPolicies(context, bnd,
                    		owningCollection);
                }
            } 
            else
            {
                // we have a bundle already, just add bitstream
                b = bitstreamService.create(context, bundles.get(0), is);
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

            b.setName(context, noPath);
            b.setSource(context, wrapper.getFilesystemName("file"));

            // Identify the format
            BitstreamFormat bf = bitstreamFormatService.guessFormat(context, b);
            b.setFormat(context, bf);
            bitstreamService.update(context, b);

            itemService.update(context, item);

            // Back to edit form
            showEditForm(context, request, response, item);

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
