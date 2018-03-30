/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.itemexport.factory.ItemExportServiceFactory;
import org.dspace.app.itemexport.service.ItemExportService;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.xml.sax.SAXException;

/**
 * 
 * Create the navigation options for everything in the administrative aspects. This includes 
 * Epeople, group, item, access control, and registry management.
 * 
 * @author Scott Phillips
 * @author Afonso Araujo Neto (internationalization)
 * @author Alexey Maslov
 * @author Jay Paz
 */
public class Navigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private static final Message T_context_head 				= message("xmlui.administrative.Navigation.context_head");
    private static final Message T_context_edit_item 			= message("xmlui.administrative.Navigation.context_edit_item");
    private static final Message T_context_edit_collection 		= message("xmlui.administrative.Navigation.context_edit_collection");
    private static final Message T_context_item_mapper 			= message("xmlui.administrative.Navigation.context_item_mapper");
    private static final Message T_context_edit_community 		= message("xmlui.administrative.Navigation.context_edit_community");
    private static final Message T_context_create_collection 	= message("xmlui.administrative.Navigation.context_create_collection");
    private static final Message T_context_create_subcommunity 	= message("xmlui.administrative.Navigation.context_create_subcommunity");
    private static final Message T_context_create_community 	= message("xmlui.administrative.Navigation.context_create_community");
    private static final Message T_context_export_metadata      = message("xmlui.administrative.Navigation.context_export_metadata");
    private static final Message T_administrative_batch_import      = message("xmlui.administrative.Navigation.administrative_batch_import");
    private static final Message T_administrative_import_metadata       = message("xmlui.administrative.Navigation.administrative_import_metadata");
    private static final Message T_administrative_head 				= message("xmlui.administrative.Navigation.administrative_head");
    private static final Message T_administrative_access_control 	= message("xmlui.administrative.Navigation.administrative_access_control");
    private static final Message T_administrative_people 			= message("xmlui.administrative.Navigation.administrative_people");
    private static final Message T_administrative_groups 			= message("xmlui.administrative.Navigation.administrative_groups");
    private static final Message T_administrative_authorizations 	= message("xmlui.administrative.Navigation.administrative_authorizations");
    private static final Message T_administrative_registries 		= message("xmlui.administrative.Navigation.administrative_registries");
    private static final Message T_administrative_metadata 			= message("xmlui.administrative.Navigation.administrative_metadata");
    private static final Message T_administrative_format 			= message("xmlui.administrative.Navigation.administrative_format");
    private static final Message T_administrative_content           = message("xmlui.administrative.Navigation.administrative_content");
    private static final Message T_administrative_items 			= message("xmlui.administrative.Navigation.administrative_items");
    private static final Message T_administrative_withdrawn  		= message("xmlui.administrative.Navigation.administrative_withdrawn");
    private static final Message T_administrative_private  		= message("xmlui.administrative.Navigation.administrative_private");
    private static final Message T_administrative_control_panel 	= message("xmlui.administrative.Navigation.administrative_control_panel");
    private static final Message T_administrative_curation              = message("xmlui.administrative.Navigation.administrative_curation");
    
    private static final Message T_statistics            	        = message("xmlui.administrative.Navigation.statistics");

    private static final Message T_context_export_item 				= message("xmlui.administrative.Navigation.context_export_item");
    private static final Message T_context_export_collection 		= message("xmlui.administrative.Navigation.context_export_collection");
    private static final Message T_context_export_community 		= message("xmlui.administrative.Navigation.context_export_community");
    private static final Message T_account_export			 		= message("xmlui.administrative.Navigation.account_export");

    private static final Message T_my_account                       = message("xmlui.EPerson.Navigation.my_account");

    /** Cached validity object */
	private SourceValidity validity;
	
	/** exports available for download */
	java.util.List<String> availableExports = null;

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
   	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected ItemExportService itemExportService = ItemExportServiceFactory.getInstance().getItemExportService();

	 /**
     * Generate the unique cache key.
     *
     * @return The generated key hashes the src
     */
    public Serializable getKey() 
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        
        // Special case, don't cache anything if the user is logging 
        // in. The problem occures because of timming, this cache key
        // is generated before we know whether the operation has 
        // succeeded or failed. So we don't know whether to cache this 
        // under the user's specific cache or under the anonymous user.
        if (request.getParameter("login_email")    != null ||
            request.getParameter("login_password") != null ||
            request.getParameter("login_realm")    != null )
        {
            return "0";
        }

        if (context.getCurrentUser() == null)
        {
            return HashUtil.hash("anonymous");
        }

        if (availableExports != null && availableExports.size()>0) {
            StringBuilder key = new StringBuilder(context.getCurrentUser().getEmail());
            for(String fileName : availableExports){
                key.append(":").append(fileName);
            }

            return HashUtil.hash(key.toString());
        }

        return HashUtil.hash(context.getCurrentUser().getEmail());
    }
	
    /**
     * Generate the validity object.
     *
     * @return The generated validity object or <code>null</code> if the
     *         component is currently not cacheable.
     */
    public SourceValidity getValidity() 
    {
    	if (this.validity == null)
    	{
    		// Only use the DSpaceValidity object is someone is logged in.
    		if (context.getCurrentUser() != null)
    		{
		        try {
		            DSpaceValidity validity = new DSpaceValidity();
		            
		            validity.add(context, eperson);
		            
		            java.util.Set<Group> groups = groupService.allMemberGroupsSet(context, eperson);
		            for (Group group : groups)
		            {
		            	validity.add(context, group);
		            }
		            
		            this.validity = validity.complete();
		        } 
		        catch (SQLException sqle)
		        {
		            // Just ignore it and return invalid.
		        }
    		}
    		else
    		{
    			this.validity = NOPValidity.SHARED_INSTANCE;
    		}
    	}
    	return this.validity;
    }
	
    
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
    	super.setup(resolver, objectModel, src, parameters);
        availableExports = null;
        if (context.getCurrentUser() != null)
        {
            try
            {
                availableExports = itemExportService.getExportsAvailable(context.getCurrentUser());
            }
            catch (Exception e)
            {
                throw new ProcessingException("Error getting available exports", e);
            }
        }
    }
    
    
    
    
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	/* Create skeleton menu structure to ensure consistent order between aspects,
    	 * even if they are never used 
    	 */
        options.addList("browse");
        List account = options.addList("account");
        List context = options.addList("context");
        List admin = options.addList("administrative");
        account.setHead(T_my_account);	        
        
        // My Account options
        if(availableExports!=null && availableExports.size()>0){
            account.addItem().addXref(contextPath+"/admin/export", T_account_export);
        }

        //Check if a system administrator
        boolean isSystemAdmin = authorizeService.isAdmin(this.context);

        // Context Administrative options
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
    	if (dso instanceof Item)
    	{
    		Item item = (Item) dso;
    		if (itemService.canEdit(this.context, item))
    		{
                    context.setHead(T_context_head);
                    context.addItem().addXref(contextPath+"/admin/item?itemID="+item.getID(), T_context_edit_item);
                    if (authorizeService.isAdmin(this.context, dso))
                    {
                        context.addItem().addXref(contextPath+"/admin/export?itemID="+item.getID(), T_context_export_item );
                        context.addItem().addXref(contextPath+ "/csv/handle/"+dso.getHandle(),T_context_export_metadata );
                    }
                }
    	}
    	else if (dso instanceof Collection)
    	{
    		Collection collection = (Collection) dso;
    		
    		// can they admin this collection?
            if (collectionService.canEditBoolean(this.context, collection, true))
            {
            	context.setHead(T_context_head);
            	context.addItemXref(contextPath+"/admin/collection?collectionID=" + collection.getID(), T_context_edit_collection);            	
            	context.addItemXref(contextPath+"/admin/mapper?collectionID="+collection.getID(), T_context_item_mapper); 
            	if (authorizeService.isAdmin(this.context, dso))
                {
                    context.addItem().addXref(contextPath+"/admin/export?collectionID="+collection.getID(), T_context_export_collection );
                    context.addItem().addXref(contextPath+ "/csv/handle/"+dso.getHandle(),T_context_export_metadata );
                }
            }
    	}
    	else if (dso instanceof Community)
    	{
    		Community community = (Community) dso;
    		
    		// can they admin this collection?
            if (communityService.canEditBoolean(this.context, community))
            {
            	context.setHead(T_context_head);
            	context.addItemXref(contextPath+"/admin/community?communityID=" + community.getID(), T_context_edit_community); 
            	if (authorizeService.isAdmin(this.context, dso))
                {
                    context.addItem().addXref(contextPath + "/admin/export?communityID=" + community.getID(), T_context_export_community);
                }
                context.addItem().addXref(contextPath+ "/csv/handle/"+dso.getHandle(),T_context_export_metadata );
            }
            
            // can they add to this community?
            if (authorizeService.authorizeActionBoolean(this.context, community, Constants.ADD))
            {
            	context.setHead(T_context_head);
            	context.addItemXref(contextPath+"/admin/collection?createNew&communityID=" + community.getID(), T_context_create_collection);
                context.addItemXref(contextPath+"/admin/community?createNew&communityID=" + community.getID(), T_context_create_subcommunity);      
            }
    	}
    	
    	if (isSystemAdmin && ("community-list".equals(this.sitemapURI) || "".equals(this.sitemapURI)))
    	{
            // Only System administrators can create top-level communities
            context.setHead(T_context_head);
            context.addItemXref(contextPath+"/admin/community?createNew", T_context_create_community);
    	}
        
        
        // System Administrator options!
        if (isSystemAdmin)
        {
            admin.setHead(T_administrative_head);

            // Control panel
            admin.addItemXref(contextPath+"/admin/panel", T_administrative_control_panel);

            // Access Controls
            List epeople = admin.addList("epeople");
            epeople.setHead(T_administrative_access_control);
            epeople.addItemXref(contextPath+"/admin/epeople", T_administrative_people);
            epeople.addItemXref(contextPath+"/admin/groups", T_administrative_groups);
            epeople.addItemXref(contextPath+"/admin/authorize", T_administrative_authorizations);

            // Content Admin
            List content = admin.addList("content");
            content.setHead(T_administrative_content);
            content.addItemXref(contextPath+"/admin/item", T_administrative_items);
            content.addItemXref(contextPath+"/admin/withdrawn", T_administrative_withdrawn);
            content.addItemXref(contextPath+"/admin/private", T_administrative_private);
            content.addItemXref(contextPath+"/admin/metadataimport", T_administrative_import_metadata);
            content.addItemXref(contextPath+"/admin/batchimport", T_administrative_batch_import);

            // Registries
            List registries = admin.addList("registries");
            registries.setHead(T_administrative_registries);
            registries.addItemXref(contextPath+"/admin/metadata-registry",T_administrative_metadata);
            registries.addItemXref(contextPath+"/admin/format-registry",T_administrative_format);

            admin.addItemXref(contextPath+"/statistics", T_statistics);
            admin.addItemXref(contextPath+ "/admin/curate", T_administrative_curation);
        }
    }
    
    
    public int addContextualOptions(List context) throws SQLException, WingException
    {
    	// How many options were added.
    	int options = 0;
    	
    	DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
    	
    	if (dso instanceof Item)
    	{
    		Item item = (Item) dso;
    		if (itemService.canEdit(this.context, item))
    		{   			
    			context.addItem().addXref(contextPath+"/admin/item?itemID="+item.getID(), T_context_edit_item);
    			options++;
    		}
    	}
    	else if (dso instanceof Collection)
    	{
    		Collection collection = (Collection) dso;
    		
    		
    		// can they admin this collection?
            if (authorizeService.authorizeActionBoolean(this.context, collection, Constants.ADMIN))
            {            	
            	context.addItemXref(contextPath+"/admin/collection?collectionID=" + collection.getID(), T_context_edit_collection);
            	context.addItemXref(contextPath+"/admin/mapper?collectionID="+collection.getID(), T_context_item_mapper);            	
            	options++;
            }
    	}
    	else if (dso instanceof Community)
    	{
    		Community community = (Community) dso;
    		
    		// can they admin this collection?
            if (communityService.canEditBoolean(this.context, community))
            {           	
            	context.addItemXref(contextPath+"/admin/community?communityID=" + community.getID(), T_context_edit_community);
            	options++;
            }
            
            // can they add to this community?
            if (authorizeService.authorizeActionBoolean(this.context, community, Constants.ADD))
            {        	
            	context.addItemXref(contextPath+"/admin/collection?createNew&communityID=" + community.getID(), T_context_create_collection);         	
            	context.addItemXref(contextPath+"/admin/community?createNew&communityID=" + community.getID(), T_context_create_subcommunity);
            	options++;
            }
    	}
    	
    	if (("community-list".equals(this.sitemapURI) || "".equals(this.sitemapURI)) && authorizeService.isAdmin(this.context))
    	{
            context.addItemXref(contextPath+"/admin/community?createNew", T_context_create_community);
            options++;
    	}
    	
    	return options;
    }
    
    
    /**
     * recycle
     */
    public void recycle()
    {
        this.validity = null;
        this.availableExports = null;
        super.recycle();
    }
    
}
