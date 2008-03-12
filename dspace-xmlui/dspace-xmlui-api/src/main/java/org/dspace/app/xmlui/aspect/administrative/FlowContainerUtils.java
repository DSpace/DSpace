/*
 * FlowContainerUtils.java
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
 */package org.dspace.app.xmlui.aspect.administrative;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import org.apache.cocoon.environment.Request;
import org.apache.cocoon.servlet.multipart.Part;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Utility methods to processes actions on Communities and Collections. 
 * 
 * @author scott phillips
 */
public class FlowContainerUtils 
{

	/** Possible Collection roles */
	public static final String ROLE_ADMIN 	 	 = "ADMIN";
	public static final String ROLE_WF_STEP1 	 = "WF_STEP1";
	public static final String ROLE_WF_STEP2 	 = "WF_STEP2";
	public static final String ROLE_WF_STEP3 	 = "WF_STEP3";
	public static final String ROLE_SUBMIT   	 = "SUBMIT";
	public static final String ROLE_DEFAULT_READ = "DEFAULT_READ";
	
	
	// Collection related functions

	/**
	 * Process the collection metadata edit form.
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @param deleteLogo Determines if the logo should be deleted along with the metadata editing action.
	 * @param request the Cocoon request object
	 * @return A process result's object.
	 */
	public static FlowResult processEditCollection(Context context, int collectionID, boolean deleteLogo, Request request) throws SQLException, IOException, AuthorizeException
	{
		FlowResult result = new FlowResult();
		
		Collection collection = Collection.find(context, collectionID);
		
		// Get the metadata
		String name = request.getParameter("name");
		String shortDescription = request.getParameter("short_description");
		String introductoryText = request.getParameter("introductory_text");
		String copyrightText = request.getParameter("copyright_text");
		String sideBarText = request.getParameter("side_bar_text");
		String license = request.getParameter("license");
		String provenanceDescription = request.getParameter("provenance_description");
		
		// If they don't have a name then make it untitled.
		if (name == null || name.length() == 0)
			name = "Untitled";

		// If empty, make it null.
		if (shortDescription != null && shortDescription.length() == 0)
			shortDescription = null;
		if (introductoryText != null && introductoryText.length() == 0)
			introductoryText = null;
		if (copyrightText != null && copyrightText.length() == 0)
			copyrightText = null;
		if (sideBarText != null && sideBarText.length() == 0)
			sideBarText = null;
		if (license != null && license.length() == 0)
			license = null;
		if (provenanceDescription != null && provenanceDescription.length() == 0)
			provenanceDescription = null;
		
		// Save the metadata
		collection.setMetadata("name", name);
		collection.setMetadata("short_description", shortDescription);
		collection.setMetadata("introductory_text", introductoryText);
		collection.setMetadata("copyright_text", copyrightText);
		collection.setMetadata("side_bar_text", sideBarText);
		collection.setMetadata("license", license);
		collection.setMetadata("provenance_description", provenanceDescription);
		
        
		// Change or delete the logo
        if (deleteLogo)
        {
        	// Remove the logo
        	collection.setLogo(null);
        }
        else
        {
        	// Update the logo
    		Object object = request.get("logo");
    		Part filePart = null;
    		if (object instanceof Part)
    			filePart = (Part) object;

    		if (filePart != null && filePart.getSize() > 0)
    		{
    			InputStream is = filePart.getInputStream();
    			
    			collection.setLogo(is);
    		}
        }
        
        // Save everything
        collection.update();
        context.commit();
        
        
        // No notice...
        result.setContinue(true);
		
		return result;
	}
	
	
	/**
	 * Look up the id of the template item for a given collection.
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @return The id of the template item.
	 * @throws IOException 
	 */
	public static int getTemplateItemID(Context context, int collectionID) throws SQLException, AuthorizeException, IOException
	{
		Collection collection = Collection.find(context, collectionID);
		Item template = collection.getTemplateItem();
		
		if (template == null)
		{
			collection.createTemplateItem();
			template = collection.getTemplateItem();
			
			collection.update();
			template.update();
			context.commit();
		}
		
		return template.getID();
	}

	
	
	
	/**
	 * Look up the id of a group authorized for one of the given roles. If no group is currently 
	 * authorized to preform this role then a new group will be created and assigned the role.
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @param roleName ADMIN, WF_STEP1,	WF_STEP2, WF_STEP3,	SUBMIT, DEFAULT_READ.
	 * @return The id of the group associated with that particular role, or -1 if the role was not found.
	 */
	public static int getCollectionRole(Context context, int collectionID, String roleName) throws SQLException, AuthorizeException, IOException
	{
		Collection collection = Collection.find(context, collectionID);
		
		// Determine the group based upon wich role we are looking for.
		Group role = null;
		if (ROLE_ADMIN.equals(roleName))
		{
			role = collection.getAdministrators();
			if (role == null)
				role = collection.createAdministrators();
		} 
		else if (ROLE_SUBMIT.equals(roleName))
		{
			role = collection.getSubmitters();
			if (role == null)
				role = collection.createSubmitters();
		}
		else if (ROLE_WF_STEP1.equals(roleName))
		{	
			role = collection.getWorkflowGroup(1);
			if (role == null)
				role = collection.createWorkflowGroup(1);
			
		}
		else if (ROLE_WF_STEP2.equals(roleName))
		{
			role = collection.getWorkflowGroup(2);
			if (role == null)
				role = collection.createWorkflowGroup(2);
		}
		else if (ROLE_WF_STEP3.equals(roleName))
		{
			role = collection.getWorkflowGroup(3);
			if (role == null)
				role = collection.createWorkflowGroup(3);
			
		}
		
		// In case we needed to create a group, save our changes
		collection.update();
		context.commit();
		
		// If the role name was valid then role should be non null,
		if (role != null)
			return role.getID();
		
		return -1;
	}
	
	
	/**
	 * Delete one of collection's roles
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @param roleName ADMIN, WF_STEP1,	WF_STEP2, WF_STEP3,	SUBMIT, DEFAULT_READ.
	 * @param groupID The id of the group associated with this role.
	 * @return A process result's object.
	 */
	public static FlowResult processDeleteCollectionRole(Context context, int collectionID, String roleName, int groupID) throws SQLException, UIException, IOException, AuthorizeException
	{
		FlowResult result = new FlowResult();
		
		Collection collection = Collection.find(context,collectionID);
		Group role = Group.find(context, groupID);
		
		// First, Unregister the role
		if (ROLE_ADMIN.equals(roleName))
		{
			collection.removeAdministrators();
		} 
		else if (ROLE_SUBMIT.equals(roleName))
		{
			collection.removeSubmitters();
		}
		else if (ROLE_WF_STEP1.equals(roleName))
		{	
			collection.setWorkflowGroup(1, null);
		}
		else if (ROLE_WF_STEP2.equals(roleName))
		{
			collection.setWorkflowGroup(2, null);
		}
		else if (ROLE_WF_STEP3.equals(roleName))
		{
			collection.setWorkflowGroup(3, null);
			
		}
		
		// Second, remove all outhorizations for this role by searching for all policies that this 
		// group has on the collection and remove them otherwise the delete will fail because 
		// there are dependencies.
		@SuppressWarnings("unchecked") // the cast is correct
		List<ResourcePolicy> policies = AuthorizeManager.getPolicies(context,collection);
		for (ResourcePolicy policy : policies)
		{
			if (policy.getGroupID() == groupID)
				policy.delete();
		}
		
		// Finally, Delete the role's actual group.
		collection.update();
		role.delete();
		context.commit();
	
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(new Message("default","The role was successfully deleted."));
		return result;
	}
	
	
	/**
	 * Look up the id of a group authorized for one of the given roles. If no group is currently 
	 * authorized to preform this role then a new group will be created and assigned the role.
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @param roleName ADMIN, WF_STEP1,	WF_STEP2, WF_STEP3,	SUBMIT, DEFAULT_READ.
	 * @return The id of the group associated with that particular role.
	 */
	public static int getCollectionDefaultRead(Context context, int collectionID) throws SQLException, AuthorizeException
	{
		Collection collection = Collection.find(context,collectionID);
		
		Group[] itemGroups = AuthorizeManager.getAuthorizedGroups(context, collection, Constants.DEFAULT_ITEM_READ);
		Group[] bitstreamGroups = AuthorizeManager.getAuthorizedGroups(context, collection, Constants.DEFAULT_BITSTREAM_READ);
		
		if (itemGroups.length != 1 && bitstreamGroups.length != 1)
			// If there are more than one groups assigned either of these privleges then this role based method will not work.
			// The user will need to go to the authorization section to manualy straight this out.
			return -1;
		
		Group itemGroup = itemGroups[0];
		Group bitstreamGroup = bitstreamGroups[0];
		
		if (itemGroup.getID() != bitstreamGroup.getID())
			// If the same group is not assigned both of these priveleges then this role based method will not work. The user 
			// will need to go to the authorization section to manualy straighten this out.
			return -1;
		
		
		
		return itemGroup.getID();
	}
	
	/**
	 * Change default privleges from the anonymous group to a new group that will be created and
	 * approrpate privleges assigned. The id of this new group will be returned.
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @return The group ID of the new group.
	 */
	public static int createCollectionDefaultReadGroup(Context context, int collectionID) throws SQLException, AuthorizeException, UIException
	{
		int roleID = getCollectionDefaultRead(context, collectionID);
		
		if (roleID != 0)
			throw new UIException("Unable to create a new default read group because either the group allready exists or multiple groups are assigned the default privleges.");
		
		Collection collection = Collection.find(context,collectionID);
		Group role = Group.create(context);
		role.setName("COLLECTION_"+collection.getID() +"_DEFAULT_READ");
		
		// Remove existing privleges from the anynomous group.
		AuthorizeManager.removePoliciesActionFilter(context, collection, Constants.DEFAULT_ITEM_READ);
		AuthorizeManager.removePoliciesActionFilter(context, collection, Constants.DEFAULT_BITSTREAM_READ);
		
		// Grant our new role the default privleges.
		AuthorizeManager.addPolicy(context, collection, Constants.DEFAULT_ITEM_READ,      role);
		AuthorizeManager.addPolicy(context, collection, Constants.DEFAULT_BITSTREAM_READ, role);
		
		// Committ the changes
		role.update();
		context.commit();
		
		return role.getID();
	}
	
	/**
	 * Change the default read priveleges to the anonymous group.
	 * 
	 * If getCollectionDefaultRead() returns -1 or the anonymous group then nothing 
	 * is done. 
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @return A process result's object.
	 */
	public static FlowResult changeCollectionDefaultReadToAnonymous(Context context, int collectionID) throws SQLException, AuthorizeException, UIException
	{
		FlowResult result = new FlowResult();
		
		int roleID = getCollectionDefaultRead(context, collectionID);
		
		if (roleID < 1)
		{
			throw new UIException("Unable to delete the default read role because the role is either allready assigned to the anonymous group or multiple groups are assigned the default priveleges.");
		}
		
		Collection collection = Collection.find(context,collectionID);
		Group role = Group.find(context, roleID);
		Group anonymous = Group.find(context,0);
		
		// Delete the old role, this will remove the default privleges.
		role.delete();
		
		// Set anonymous as the default read group.
		AuthorizeManager.addPolicy(context, collection, Constants.DEFAULT_ITEM_READ,      anonymous);
		AuthorizeManager.addPolicy(context, collection, Constants.DEFAULT_BITSTREAM_READ, anonymous);
		
		// Commit the changes
		context.commit();
		
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(new Message("default","All new items submitted to this collection will default to anonymous read."));
		return result;
	}
	
	/**
	 * Delete collection itself
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @return A process result's object.
	 */
	public static FlowResult processDeleteCollection(Context context, int collectionID) throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();
		
		Collection collection = Collection.find(context, collectionID);
		
		Community[] parents = collection.getCommunities();
		
		for (Community parent: parents)
		{
			parent.removeCollection(collection);
			parent.update();
		}
		
		context.commit();
		
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(new Message("default","The collection was successfully deleted."));
		
		return result;
	}
	
	
	/**
	 * Create a new collection 
	 * 
	 * @param context The current DSpace context.
	 * @param communityID The id of the parent community.
	 * @return A process result's object.
	 */
	public static FlowResult processCreateCollection(Context context, int communityID, Request request) throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();
		
		Community parent = Community.find(context, communityID);
		Collection newCollection = parent.createCollection();
		
		// Get the metadata
		String name = request.getParameter("name");
		String shortDescription = request.getParameter("short_description");
		String introductoryText = request.getParameter("introductory_text");
		String copyrightText = request.getParameter("copyright_text");
		String sideBarText = request.getParameter("side_bar_text");
		String license = request.getParameter("license");
		String provenanceDescription = request.getParameter("provenance_description");
		
		// If they don't have a name then make it untitled.
		if (name == null || name.length() == 0)
			name = "Untitled";

		// If empty, make it null.
		if (shortDescription != null && shortDescription.length() == 0)
			shortDescription = null;
		if (introductoryText != null && introductoryText.length() == 0)
			introductoryText = null;
		if (copyrightText != null && copyrightText.length() == 0)
			copyrightText = null;
		if (sideBarText != null && sideBarText.length() == 0)
			sideBarText = null;
		if (license != null && license.length() == 0)
			license = null;
		if (provenanceDescription != null && provenanceDescription.length() == 0)
			provenanceDescription = null;
		
		// Save the metadata
		newCollection.setMetadata("name", name);
		newCollection.setMetadata("short_description", shortDescription);
		newCollection.setMetadata("introductory_text", introductoryText);
		newCollection.setMetadata("copyright_text", copyrightText);
		newCollection.setMetadata("side_bar_text", sideBarText);
		newCollection.setMetadata("license", license);
		newCollection.setMetadata("provenance_description", provenanceDescription);
		
        
        // Set the logo
    	Object object = request.get("logo");
    	Part filePart = null;
		if (object instanceof Part)
			filePart = (Part) object;

		if (filePart != null && filePart.getSize() > 0)
		{
			InputStream is = filePart.getInputStream();
			
			newCollection.setLogo(is);
		}
        
        // Save everything
		newCollection.update();
        context.commit();
        // success
        result.setContinue(true);
        result.setOutcome(true);
        result.setMessage(new Message("default","The collection was successfully created."));
        result.setParameter("collectionID", newCollection.getID());
		
		return result;
	}
	
		
	
	
	
	// Community related functions

	/**
	 * Create a new community 
	 * 
	 * @param context The current DSpace context.
	 * @param communityID The id of the parent community (-1 for a top-level community).
	 * @return A process result's object.
	 */
	public static FlowResult processCreateCommunity(Context context, int communityID, Request request) throws AuthorizeException, IOException, SQLException
	{
		FlowResult result = new FlowResult();

		Community parent = Community.find(context, communityID);
		Community newCommunity;
		
		if (parent != null)
			newCommunity = parent.createSubcommunity();
		else
			newCommunity = Community.create(null, context);
		
		String name = request.getParameter("name");
		String shortDescription = request.getParameter("short_description");
		String introductoryText = request.getParameter("introductory_text");
		String copyrightText = request.getParameter("copyright_text");
		String sideBarText = request.getParameter("side_bar_text");

		// If they don't have a name then make it untitled.
		if (name == null || name.length() == 0)
			name = "Untitled";

		// If empty, make it null.
		if (shortDescription != null && shortDescription.length() == 0)
			shortDescription = null;
		if (introductoryText != null && introductoryText.length() == 0)
			introductoryText = null;
		if (copyrightText != null && copyrightText.length() == 0)
			copyrightText = null;
		if (sideBarText != null && sideBarText.length() == 0)
			sideBarText = null;
		
		newCommunity.setMetadata("name", name);
		newCommunity.setMetadata("short_description", shortDescription);
		newCommunity.setMetadata("introductory_text", introductoryText);
		newCommunity.setMetadata("copyright_text", copyrightText);
		newCommunity.setMetadata("side_bar_text", sideBarText);
        
    	// Upload the logo
		Object object = request.get("logo");
		Part filePart = null;
		if (object instanceof Part)
			filePart = (Part) object;

		if (filePart != null && filePart.getSize() > 0)
		{
			InputStream is = filePart.getInputStream();
			
			newCommunity.setLogo(is);
		}
        
		// Save everything
		newCommunity.update();
        context.commit();
        // success
        result.setContinue(true);
        result.setOutcome(true);
        result.setMessage(new Message("default","The community was successfully created."));
        result.setParameter("communityID", newCommunity.getID());
		
		return result;
	}
	
	
	/**
	 * Process the community metadata edit form.
	 * 
	 * @param context The current DSpace context.
	 * @param communityID The community id.
	 * @param deleteLogo Determines if the logo should be deleted along with the metadata editing action.
	 * @param request the Cocoon request object
	 * @return A process result's object.
	 */
	public static FlowResult processEditCommunity(Context context, int communityID, boolean deleteLogo, Request request) throws AuthorizeException, IOException, SQLException
	{
		FlowResult result = new FlowResult();

		Community community = Community.find(context, communityID);
		
		String name = request.getParameter("name");
		String shortDescription = request.getParameter("short_description");
		String introductoryText = request.getParameter("introductory_text");
		String copyrightText = request.getParameter("copyright_text");
		String sideBarText = request.getParameter("side_bar_text");

		// If they don't have a name then make it untitled.
		if (name == null || name.length() == 0)
			name = "Untitled";

		// If empty, make it null.
		if (shortDescription != null && shortDescription.length() == 0)
			shortDescription = null;
		if (introductoryText != null && introductoryText.length() == 0)
			introductoryText = null;
		if (copyrightText != null && copyrightText.length() == 0)
			copyrightText = null;
		if (sideBarText != null && sideBarText.length() == 0)
			sideBarText = null;
		
		// Save the data
		community.setMetadata("name", name);
		community.setMetadata("short_description", shortDescription);
        community.setMetadata("introductory_text", introductoryText);
        community.setMetadata("copyright_text", copyrightText);
        community.setMetadata("side_bar_text", sideBarText);
        
        if (deleteLogo)
        {
        	// Remove the logo
        	community.setLogo(null);
        }
        else
        {
        	// Update the logo
    		Object object = request.get("logo");
    		Part filePart = null;
    		if (object instanceof Part)
    			filePart = (Part) object;

    		if (filePart != null && filePart.getSize() > 0)
    		{
    			InputStream is = filePart.getInputStream();
    			
    			community.setLogo(is);
    		}
        }
        
        // Save everything
        community.update();
        context.commit();
        
        // No notice...
        result.setContinue(true);
		return result;
	}
	
	
	
	/**
	 * Delete community itself
	 * 
	 * @param context The current DSpace context.
	 * @param communityID The community id.
	 * @return A process result's object.
	 */
	public static FlowResult processDeleteCommunity(Context context, int communityID) throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();
		
		Community community = Community.find(context, communityID);
		
		community.delete();
		context.commit();
		
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(new Message("default","The community was successfully deleted."));
		
		return result;
	}
	
	
	
	/**
	 * Check whether this metadata value is a proper XML fragment. If the value is not 
	 * then an error message will be returned that might (sometimes not) tell the user how
	 * to correct the problem.
	 * 
	 * @param value The metadat's value
	 * @return An error string of the problem or null if there is no problem with the metadata's value.
	 */
	public static String checkXMLFragment(String value)
	{
		// escape the ampersand correctly;
		value = escapeXMLEntities(value);
		
		// Try and parse the XML into a mini-dom
    	String xml = "<fragment>"+value+"</fragment>";
 	   
 	   	ByteArrayInputStream inputStream = new ByteArrayInputStream(xml.getBytes());
 	   
 	    SAXBuilder builder = new SAXBuilder();
		try 
		{
			// This will generate an error if not valid XML.
			builder.build(inputStream);
		} 
		catch (JDOMException jdome) 
		{
			// It's not XML
			return jdome.getMessage();
		} 
		catch (IOException ioe) 
		{
			// This shouldn't ever occure because we are parsing
			// an in-memory string, but in case it does we'll just return
			// it as a normal error.
			return ioe.getMessage();
		}

    	return null;
	}
	
    /** 
     * Sanatize any XML that was inputed by the user, this will clean up
     * any unescaped characters so that they can be stored as proper XML.
     * These are errors that in general we want to take care of on behalf
     * of the user.
     * 
     * @param value The unsantized value
     * @return A sanatized value
     */
	public static String escapeXMLEntities(String value)
	{
		if (value == null)
			return null;
		
		// Escape any XML entities
    	int amp = -1;
    	while ((amp = value.indexOf('&', amp+1)) > -1)
    	{
    		// Is it an xml entity named by number?
    		if (substringCompare(value,amp+1,'#'))
    			continue;
    		
    		// &amp;
    		if (substringCompare(value,amp+1,'a','m','p',';'))
    			continue;
    		
    		// &apos;
    		if (substringCompare(value,amp+1,'a','p','o','s',';'))
    			continue;
    		
    		// &quot;
    		if (substringCompare(value,amp+1,'q','u','o','t',';'))
    			continue;
    			
    		// &lt;
    		if (substringCompare(value,amp+1,'l','t',';'))
    			continue;
    		
    		// &gt;
    		if (substringCompare(value,amp+1,'g','t',';'))
    			continue;
    		
    		// Replace the ampersand with an XML entity.
    		value = value.substring(0,amp) + "&amp;" + value.substring(amp+1);
    	}
    	
    	return value;
	}
	
	 /**
     * Check if the given character sequence is located in the given
     * string at the specified index. If it is then return true, otherwise false.
     * 
     * @param string The string to test against
     * @param index The location within the string
     * @param characters The character sequence to look for.
     * @return true if the character sequence was found, otherwise false.
     */
    private static boolean substringCompare(String string, int index, char ... characters)
    {
    	// Is the string long enough?
    	if (string.length() <= index + characters.length)
    		return false;
    	
    	// Do all the characters match?
    	for (char character : characters)
    	{
    		if (string.charAt(index) != character)
    			return false;
    		index++;
    	}
    	
    	return false;
    }
	

}
