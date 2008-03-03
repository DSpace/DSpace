/*
 * Navigation.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/07/13 23:20:54 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.xml.sax.SAXException;

/**
 * 
 * Create the navigation options for everything in the administrative aspects. This includes 
 * Epeople, group, item, access control, and registry management.
 * 
 * @author Scott Phillips
 * @author Afonso Araujo Neto (internationalization)
 * @author Alexey Maslov
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

    private static final Message T_administrative_head 				= message("xmlui.administrative.Navigation.administrative_head");
    private static final Message T_administrative_access_control 	= message("xmlui.administrative.Navigation.administrative_access_control");
    private static final Message T_administrative_people 			= message("xmlui.administrative.Navigation.administrative_people");
    private static final Message T_administrative_groups 			= message("xmlui.administrative.Navigation.administrative_groups");
    private static final Message T_administrative_authorizations 	= message("xmlui.administrative.Navigation.administrative_authorizations");
    private static final Message T_administrative_registries 		= message("xmlui.administrative.Navigation.administrative_registries");
    private static final Message T_administrative_metadata 			= message("xmlui.administrative.Navigation.administrative_metadata");
    private static final Message T_administrative_format 			= message("xmlui.administrative.Navigation.administrative_format");
    private static final Message T_administrative_items 			= message("xmlui.administrative.Navigation.administrative_items");
    private static final Message T_administrative_withdrawn  		= message("xmlui.administrative.Navigation.administrative_withdrawn");
    private static final Message T_administrative_control_panel 	= message("xmlui.administrative.Navigation.administrative_control_panel");


   
	

    /** Cached validity object */
	private SourceValidity validity;
	
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
        // succeded or failed. So we don't know whether to cache this 
        // under the user's specific cache or under the anonymous user.
        if (request.getParameter("login_email")    != null ||
            request.getParameter("login_password") != null ||
            request.getParameter("login_realm")    != null )
        {
            return "0";
        }
                
    	String key;
        if (context.getCurrentUser() != null)
            key = context.getCurrentUser().getEmail();
        else
        	key = "anonymous";
        
        return HashUtil.hash(key);
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
		            
		            validity.add(eperson);
		            
		            Group[] groups = Group.allMemberGroups(context, eperson);
		            for (Group group : groups)
		            {
		            	validity.add(group);
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
	
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	/* Create skeleton menu structure to ensure consistent order between aspects,
    	 * even if they are never used 
    	 */
        options.addList("browse");
        options.addList("account");
        List context = options.addList("context");
        List admin = options.addList("administrative");
        
        // Context Administrative options
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
    	if (dso instanceof Item)
    	{
    		
    		Item item = (Item) dso;
    		if (item.canEdit())
    		{
            	context.setHead(T_context_head);
            	context.addItem().addXref(contextPath+"/admin/item?itemID="+item.getID(), T_context_edit_item);
    		}
    	}
    	else if (dso instanceof Collection)
    	{
    		Collection collection = (Collection) dso;
    		
    		
    		// can they admin this collection?
            if (AuthorizeManager.authorizeActionBoolean(this.context, collection, Constants.COLLECTION_ADMIN))
            {
            	context.setHead(T_context_head);
            	context.addItemXref(contextPath+"/admin/collection?collectionID=" + collection.getID(), T_context_edit_collection);            	
            	context.addItemXref(contextPath+"/admin/mapper?collectionID="+collection.getID(), T_context_item_mapper);            	
            }
    	}
    	else if (dso instanceof Community)
    	{
    		Community community = (Community) dso;
    		
    		// can they admin this collection?
            if (community.canEditBoolean())
            {
            	context.setHead(T_context_head);
            	context.addItemXref(contextPath+"/admin/community?communityID=" + community.getID(), T_context_edit_community);            	
            }
            
            // can they add to this community?
            if (AuthorizeManager.authorizeActionBoolean(this.context, community,Constants.ADD))
            {
            	context.setHead(T_context_head);
            	context.addItemXref(contextPath+"/admin/collection?createNew&communityID=" + community.getID(), T_context_create_collection);         	
            }
            
            // Only administrators can create communities
            if (AuthorizeManager.isAdmin(this.context))
            {
            	context.setHead(T_context_head);
            	context.addItemXref(contextPath+"/admin/community?createNew&communityID=" + community.getID(), T_context_create_subcommunity);  	
            }
    	}
    	
    	if ("community-list".equals(this.sitemapURI))
    	{
    		if (AuthorizeManager.isAdmin(this.context))
            {
            	context.setHead(T_context_head);
    			context.addItemXref(contextPath+"/admin/community?createNew", T_context_create_community);    			
            }
    	}
        
        
        // System Administrator options!
        if (AuthorizeManager.isAdmin(this.context))
        {
	        admin.setHead(T_administrative_head);
	                
	        List epeople = admin.addList("epeople");
	        List registries = admin.addList("registries");
	        
	        epeople.setHead(T_administrative_access_control);	        
	        epeople.addItemXref(contextPath+"/admin/epeople", T_administrative_people);	        
	        epeople.addItemXref(contextPath+"/admin/groups", T_administrative_groups);	        
	        epeople.addItemXref(contextPath+"/admin/authorize", T_administrative_authorizations);	        
	        
	        registries.setHead(T_administrative_registries);	        
	        registries.addItemXref(contextPath+"/admin/metadata-registry",T_administrative_metadata);	        
	        registries.addItemXref(contextPath+"/admin/format-registry",T_administrative_format);	        
	        
	        admin.addItemXref(contextPath+"/admin/item", T_administrative_items);	        
            admin.addItemXref(contextPath+"/admin/withdrawn", T_administrative_withdrawn);	        
	        admin.addItemXref(contextPath+"/admin/panel", T_administrative_control_panel);
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
    		if (item.canEdit())
    		{   			
    			context.addItem().addXref(contextPath+"/admin/item?itemID="+item.getID(), T_context_edit_item);
    			options++;
    		}
    	}
    	else if (dso instanceof Collection)
    	{
    		Collection collection = (Collection) dso;
    		
    		
    		// can they admin this collection?
            if (AuthorizeManager.authorizeActionBoolean(this.context, collection, Constants.COLLECTION_ADMIN))
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
            if (community.canEditBoolean())
            {           	
            	context.addItemXref(contextPath+"/admin/community?communityID=" + community.getID(), T_context_edit_community);
            	options++;
            }
            
            // can they add to this community?
            if (AuthorizeManager.authorizeActionBoolean(this.context, community,Constants.ADD))
            {        	
            	context.addItemXref(contextPath+"/admin/collection?createNew&communityID=" + community.getID(), T_context_create_collection);         	
            	context.addItemXref(contextPath+"/admin/community?createNew&communityID=" + community.getID(), T_context_create_subcommunity);
            	options++;
            }
    	}
    	
    	if ("community-list".equals(this.sitemapURI))
    	{
    		if (AuthorizeManager.isAdmin(this.context))
            {
    			context.addItemXref(contextPath+"/admin/community?createNew", T_context_create_community);
    			options++;
            }
    	}
    	
    	return options;
    }
    
    
    /**
     * recycle
     */
    public void recycle()
    {
        this.validity = null;
        super.recycle();
    }
    
}
