/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.cocoon.environment.Request;
import org.apache.cocoon.servlet.multipart.Part;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.browse.BrowseException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.harvest.OAIHarvester;
import org.dspace.harvest.OAIHarvester.HarvestScheduler;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.Curator;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.Role;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.WorkflowUtils;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;

/**
 * Utility methods to processes actions on Communities and Collections.
 *
 * @author Scott Phillips
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
        {
            name = "Untitled";
        }

		// If empty, make it null.
		if (shortDescription != null && shortDescription.length() == 0)
        {
            shortDescription = null;
        }
		if (introductoryText != null && introductoryText.length() == 0)
        {
            introductoryText = null;
        }
		if (copyrightText != null && copyrightText.length() == 0)
        {
            copyrightText = null;
        }
		if (sideBarText != null && sideBarText.length() == 0)
        {
            sideBarText = null;
        }
		if (license != null && license.length() == 0)
        {
            license = null;
        }
		if (provenanceDescription != null && provenanceDescription.length() == 0)
        {
            provenanceDescription = null;
        }
		
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
            {
                filePart = (Part) object;
            }

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
	 * Process the collection harvesting options form.
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @param request the Cocoon request object
	 * @return A process result's object.
	 */
	public static FlowResult processSetupCollectionHarvesting(Context context, int collectionID, Request request) throws SQLException, IOException, AuthorizeException
	{
		FlowResult result = new FlowResult();
		HarvestedCollection hc = HarvestedCollection.find(context, collectionID);

		String contentSource = request.getParameter("source");

		// First, if this is not a harvested collection (anymore), set the harvest type to 0; possibly also wipe harvest settings  
		if (contentSource.equals("source_normal")) 
		{
			if (hc != null)
            {
                hc.delete();
            }
			
			result.setContinue(true);
		}
		else 
		{
			FlowResult subResult = testOAISettings(context, request);
			
			// create a new harvest instance if all the settings check out
			if (hc == null) {
				hc = HarvestedCollection.create(context, collectionID);
			}
			
			// if the supplied options all check out, set the harvesting parameters on the collection
			if (subResult.getErrors().isEmpty()) {
				String oaiProvider = request.getParameter("oai_provider");
				boolean oaiAllSets = "all".equals(request.getParameter("oai-set-setting"));
                String oaiSetId;
                if(oaiAllSets)
                {
                    oaiSetId = "all";
                }
                else
                {
                    oaiSetId = request.getParameter("oai_setid");
                }


				String metadataKey = request.getParameter("metadata_format");
				String harvestType = request.getParameter("harvest_level");
				
				hc.setHarvestParams(Integer.parseInt(harvestType), oaiProvider, oaiSetId, metadataKey);
				hc.setHarvestStatus(HarvestedCollection.STATUS_READY);
			}
			else {
				result.setErrors(subResult.getErrors());
				result.setContinue(false);
				return result;
			}
			
			hc.update();
		}
		        
        // Save everything
        context.commit();
        
        // No notice...
        //result.setMessage(new Message("default","Harvesting options successfully modified."));
        result.setOutcome(true);
        result.setContinue(true);
		
		return result;
	}
	
	
	/**
	 * Use the collection's harvest settings to immediately perform a harvest cycle.
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @param request the Cocoon request object
	 * @return A process result's object.
	 * @throws TransformerException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws CrosswalkException 
	 */
	public static FlowResult processRunCollectionHarvest(Context context, int collectionID, Request request) throws SQLException, IOException, AuthorizeException, CrosswalkException, ParserConfigurationException, SAXException, TransformerException
	{
		FlowResult result = new FlowResult();
		OAIHarvester harvester;
		List<String> testErrors = new ArrayList<String>();
		Collection collection = Collection.find(context, collectionID);
		HarvestedCollection hc = HarvestedCollection.find(context, collectionID);

		//TODO: is there a cleaner way to do this?
		try {
			if (!HarvestScheduler.hasStatus(HarvestScheduler.HARVESTER_STATUS_STOPPED)) {
				synchronized(HarvestScheduler.lock) {
					HarvestScheduler.setInterrupt(HarvestScheduler.HARVESTER_INTERRUPT_INSERT_THREAD, collectionID);
					HarvestScheduler.lock.notify();
				}
			}
			else {
				harvester = new OAIHarvester(context, collection, hc);
				harvester.runHarvest();
			}
		}
		catch (Exception e) {
			testErrors.add(e.getMessage());
			result.setErrors(testErrors);
			result.setContinue(false);
			return result;
		}
		
        result.setContinue(true);
		
		return result;
	}
	
	
	/**
	 * Purge the collection of all items, then run a fresh harvest cycle.
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @param request the Cocoon request object
	 * @return A process result's object.
	 * @throws TransformerException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws CrosswalkException 
	 * @throws BrowseException 
	 */
	public static FlowResult processReimportCollection(Context context, int collectionID, Request request) throws SQLException, IOException, AuthorizeException, CrosswalkException, ParserConfigurationException, SAXException, TransformerException, BrowseException 
	{
		Collection collection = Collection.find(context, collectionID);
		HarvestedCollection hc = HarvestedCollection.find(context, collectionID);
		
		ItemIterator it = collection.getAllItems();
		//IndexBrowse ib = new IndexBrowse(context);
		while (it.hasNext()) {
			Item item = it.next();
			//System.out.println("Deleting: " + item.getHandle());
			//ib.itemRemoved(item);
			collection.removeItem(item);
		}
		hc.setHarvestResult(null,"");
		hc.update();
		collection.update();
		context.commit();
		
		return processRunCollectionHarvest(context, collectionID, request);
	}
	
	
	/**
	 * Test the supplied OAI settings. 
	 * 
	 * @param context
	 * @param request
	 */
	public static FlowResult testOAISettings(Context context, Request request)  
	{
		FlowResult result  = new FlowResult();
		
		String oaiProvider = request.getParameter("oai_provider");
		String oaiSetId = request.getParameter("oai_setid");
        oaiSetId = request.getParameter("oai-set-setting");
        if(!"all".equals(oaiSetId))
        {
            oaiSetId = request.getParameter("oai_setid");
        }
		String metadataKey = request.getParameter("metadata_format");
		String harvestType = request.getParameter("harvest_level");
		int harvestTypeInt = 0;
		
		if (oaiProvider == null || oaiProvider.length() == 0)
        {
            result.addError("oai_provider");
        }
		if (oaiSetId == null || oaiSetId.length() == 0)
        {
            result.addError("oai_setid");
        }
		if (metadataKey == null || metadataKey.length() == 0)
        {
            result.addError("metadata_format");
        }
		if (harvestType == null || harvestType.length() == 0)
        {
            result.addError("harvest_level");
        }
		else
        {
            harvestTypeInt = Integer.parseInt(harvestType);
        }
			
		
		if (result.getErrors() == null) {
			List<String> testErrors = OAIHarvester.verifyOAIharvester(oaiProvider, oaiSetId, metadataKey, (harvestTypeInt>1));
			result.setErrors(testErrors);
		}
		
		if (result.getErrors() == null || result.getErrors().isEmpty()) {
			result.setOutcome(true);
			// On a successful test we still want to stay in the loop, not continue out of it
			//result.setContinue(true);
			result.setMessage(new Message("default","Harvesting settings are valid."));
		}
		else {
			result.setOutcome(false);
			result.setContinue(false);
			// don't really need a message when the errors are highlighted already
			//result.setMessage(new Message("default","Harvesting is not properly configured."));
		}

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
	 * authorized to perform this role then a new group will be created and assigned the role.
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @param roleName ADMIN, WF_STEP1,	WF_STEP2, WF_STEP3,	SUBMIT, DEFAULT_READ.
	 * @return The id of the group associated with that particular role, or -1 if the role was not found.
	 */
	public static int getCollectionRole(Context context, int collectionID, String roleName) throws SQLException, AuthorizeException, IOException, TransformerException, SAXException, WorkflowConfigurationException, ParserConfigurationException {
		Collection collection = Collection.find(context, collectionID);
		
		// Determine the group based upon wich role we are looking for.
		Group roleGroup = null;
		if (ROLE_ADMIN.equals(roleName))
		{
			roleGroup = collection.getAdministrators();
			if (roleGroup == null){
				roleGroup = collection.createAdministrators();
            }
		} 
		else if (ROLE_SUBMIT.equals(roleName))
		{
			roleGroup = collection.getSubmitters();
			if (roleGroup == null)
				roleGroup = collection.createSubmitters();
		}else{
            if(ConfigurationManager.getProperty("workflow","workflow.framework").equals("xmlworkflow")){//Resolve our id to a role
                roleGroup = getXMLWorkflowRole(context, collectionID, roleName, collection, roleGroup);
             }else{
                roleGroup = getOriginalWorkflowRole(roleName, collection, roleGroup);
            }

		}

		// In case we needed to create a group, save our changes
		collection.update();
		context.commit();

		// If the role name was valid then role should be non null,
		if (roleGroup != null)
			return roleGroup.getID();

		return -1;
    }
			
    private static Group getOriginalWorkflowRole(String roleName, Collection collection, Group roleGroup) throws SQLException, AuthorizeException {
        if (ROLE_WF_STEP1.equals(roleName))
        {
            roleGroup = collection.getWorkflowGroup(1);
            if (roleGroup == null)
                roleGroup = collection.createWorkflowGroup(1);

		}
		else if (ROLE_WF_STEP2.equals(roleName))
		{
            roleGroup = collection.getWorkflowGroup(2);
            if (roleGroup == null)
                roleGroup = collection.createWorkflowGroup(2);
        }
		else if (ROLE_WF_STEP3.equals(roleName))
		{
            roleGroup = collection.getWorkflowGroup(3);
            if (roleGroup == null)
                roleGroup = collection.createWorkflowGroup(3);

		}
        return roleGroup;
    }
		
    private static Group getXMLWorkflowRole(Context context, int collectionID, String roleName, Collection collection, Group roleGroup) throws IOException, WorkflowConfigurationException, SQLException, AuthorizeException {
        Role role = WorkflowUtils.getCollectionAndRepositoryRoles(collection).get(roleName);
        if(role.getScope() == Role.Scope.COLLECTION || role.getScope() == Role.Scope.REPOSITORY){
            roleGroup = WorkflowUtils.getRoleGroup(context, collectionID, role);
            if(roleGroup == null){
                AuthorizeManager.authorizeAction(context, collection, Constants.WRITE);
                roleGroup = Group.create(context);
                if(role.getScope() == Role.Scope.COLLECTION){
                    roleGroup.setName("COLLECTION_" + collection.getID() + "_WORKFLOW_ROLE_" + roleName);
                }else{
                    roleGroup.setName(role.getName());
                }
                roleGroup.update();
                AuthorizeManager.addPolicy(context, collection, Constants.ADD, roleGroup);
                if(role.getScope() == Role.Scope.COLLECTION){
                    WorkflowUtils.createCollectionWorkflowRole(context, collectionID, roleName, roleGroup);
                }
           }
        }
        return roleGroup;
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
	public static FlowResult processDeleteCollectionRole(Context context, int collectionID, String roleName, int groupID) throws SQLException, UIException, IOException, AuthorizeException, WorkflowConfigurationException
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
        else{
            if(ConfigurationManager.getProperty("workflow", "workflow.framework").equals("xmlworkflow"))
            {
                WorkflowUtils.deleteRoleGroup(context, collection, roleName);
            }else{
                if (ROLE_WF_STEP1.equals(roleName))
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
            }
		}

		// Second, remove all authorizations for this role by searching for all policies that this
		// group has on the collection and remove them otherwise the delete will fail because 
		// there are dependencies.
		@SuppressWarnings("unchecked") // the cast is correct
		List<ResourcePolicy> policies = AuthorizeManager.getPolicies(context,collection);
		for (ResourcePolicy policy : policies)
		{
			if (policy.getGroupID() == groupID)
            {
                policy.delete();
            }
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
	 * authorized to perform this role then a new group will be created and assigned the role.
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @return The id of the group associated with that particular role or -1
	 */
	public static int getCollectionDefaultRead(Context context, int collectionID) throws SQLException, AuthorizeException
	{
		Collection collection = Collection.find(context,collectionID);
		
		Group[] itemGroups = AuthorizeManager.getAuthorizedGroups(context, collection, Constants.DEFAULT_ITEM_READ);
		Group[] bitstreamGroups = AuthorizeManager.getAuthorizedGroups(context, collection, Constants.DEFAULT_BITSTREAM_READ);
		
       int itemGroupID = -1;
        
		// If there are more than one groups assigned either of these privileges then this role based method will not work.
        // The user will need to go to the authorization section to manually straighten this out.		
		if (itemGroups.length != 1 || bitstreamGroups.length != 1)
		{
		    // do nothing the itemGroupID is already set to -1
		}
		else
		{
	        Group itemGroup = itemGroups[0];
	        Group bitstreamGroup = bitstreamGroups[0];
	        
            // If the same group is not assigned both of these privileges then this role based method will not work. The user 
            // will need to go to the authorization section to manually straighten this out.
	        if (itemGroup.getID() != bitstreamGroup.getID())
	        {
	            // do nothing the itemGroupID is already set to -1
	        }
	        else
	        {
	            itemGroupID = itemGroup.getID();
	        }
		}

		return itemGroupID;
	}
	
	/**
	 * Change default privileges from the anonymous group to a new group that will be created and
	 * appropriate privileges assigned. The id of this new group will be returned.
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @return The group ID of the new group.
	 */
	public static int createCollectionDefaultReadGroup(Context context, int collectionID) throws SQLException, AuthorizeException, UIException
	{
		int roleID = getCollectionDefaultRead(context, collectionID);
		
		if (roleID != 0)
        {
            throw new UIException("Unable to create a new default read group because either the group already exists or multiple groups are assigned the default privileges.");
        }
		
		Collection collection = Collection.find(context,collectionID);
		Group role = Group.create(context);
		role.setName("COLLECTION_"+collection.getID() +"_DEFAULT_READ");
		
		// Remove existing privileges from the anonymous group.
		AuthorizeManager.removePoliciesActionFilter(context, collection, Constants.DEFAULT_ITEM_READ);
		AuthorizeManager.removePoliciesActionFilter(context, collection, Constants.DEFAULT_BITSTREAM_READ);
		
		// Grant our new role the default privileges.
		AuthorizeManager.addPolicy(context, collection, Constants.DEFAULT_ITEM_READ,      role);
		AuthorizeManager.addPolicy(context, collection, Constants.DEFAULT_BITSTREAM_READ, role);
		
		// Commit the changes
		role.update();
		context.commit();
		
		return role.getID();
	}
	
	/**
	 * Change the default read privileges to the anonymous group.
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
			throw new UIException("Unable to delete the default read role because the role is either already assigned to the anonymous group or multiple groups are assigned the default privileges.");
		}
		
		Collection collection = Collection.find(context,collectionID);
		Group role = Group.find(context, roleID);
		Group anonymous = Group.find(context,0);
		
		// Delete the old role, this will remove the default privileges.
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
        {
            name = "Untitled";
        }

		// If empty, make it null.
		if (shortDescription != null && shortDescription.length() == 0)
        {
            shortDescription = null;
        }
		if (introductoryText != null && introductoryText.length() == 0)
        {
            introductoryText = null;
        }
		if (copyrightText != null && copyrightText.length() == 0)
        {
            copyrightText = null;
        }
		if (sideBarText != null && sideBarText.length() == 0)
        {
            sideBarText = null;
        }
		if (license != null && license.length() == 0)
        {
            license = null;
        }
		if (provenanceDescription != null && provenanceDescription.length() == 0)
        {
            provenanceDescription = null;
        }
		
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
        {
            filePart = (Part) object;
        }

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
        {
            newCommunity = parent.createSubcommunity();
        }
		else
        {
            newCommunity = Community.create(null, context);
        }
		
		String name = request.getParameter("name");
		String shortDescription = request.getParameter("short_description");
		String introductoryText = request.getParameter("introductory_text");
		String copyrightText = request.getParameter("copyright_text");
		String sideBarText = request.getParameter("side_bar_text");

		// If they don't have a name then make it untitled.
		if (name == null || name.length() == 0)
        {
            name = "Untitled";
        }

		// If empty, make it null.
		if (shortDescription != null && shortDescription.length() == 0)
        {
            shortDescription = null;
        }
		if (introductoryText != null && introductoryText.length() == 0)
        {
            introductoryText = null;
        }
		if (copyrightText != null && copyrightText.length() == 0)
        {
            copyrightText = null;
        }
		if (sideBarText != null && sideBarText.length() == 0)
        {
            sideBarText = null;
        }
		
		newCommunity.setMetadata("name", name);
		newCommunity.setMetadata("short_description", shortDescription);
		newCommunity.setMetadata("introductory_text", introductoryText);
		newCommunity.setMetadata("copyright_text", copyrightText);
		newCommunity.setMetadata("side_bar_text", sideBarText);
        
    	// Upload the logo
		Object object = request.get("logo");
		Part filePart = null;
		if (object instanceof Part)
        {
            filePart = (Part) object;
        }

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
        {
            name = "Untitled";
        }

		// If empty, make it null.
		if (shortDescription != null && shortDescription.length() == 0)
        {
            shortDescription = null;
        }
		if (introductoryText != null && introductoryText.length() == 0)
        {
            introductoryText = null;
        }
		if (copyrightText != null && copyrightText.length() == 0)
        {
            copyrightText = null;
        }
		if (sideBarText != null && sideBarText.length() == 0)
        {
            sideBarText = null;
        }
		
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
            {
                filePart = (Part) object;
            }

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
     * Look up the id of a group authorized for one of the given roles. If no group is currently 
     * authorized to perform this role then a new group will be created and assigned the role.
     * 
     * @param context The current DSpace context.
     * @param communityID The collection id.
     * @param roleName ADMIN.
     * @return The id of the group associated with that particular role, or -1 if the role was not found.
     */
    public static int getCommunityRole(Context context, int communityID, String roleName) throws SQLException, AuthorizeException, IOException
    {
        Community community = Community.find(context, communityID);
	
        // Determine the group based upon which role we are looking for.
        Group role = null;
        if (ROLE_ADMIN.equals(roleName))
        {
            role = community.getAdministrators();
            if (role == null)
            {
                role = community.createAdministrators();
            }
        } 
	
        // In case we needed to create a group, save our changes
        community.update();
        context.commit();
        
        // If the role name was valid then role should be non null,
        if (role != null)
        {
            return role.getID();
        }
        
        return -1;
    }

	/**
     * Delete one of a community's roles
     * 
     * @param context The current DSpace context.
     * @param communityID The community id.
     * @param roleName ADMIN.
     * @param groupID The id of the group associated with this role.
     * @return A process result's object.
     */
    public static FlowResult processDeleteCommunityRole(Context context, int communityID, String roleName, int groupID) throws SQLException, UIException, IOException, AuthorizeException
    {
        FlowResult result = new FlowResult();
        
        Community community = Community.find(context, communityID);
        Group role = Group.find(context, groupID);
        
        // First, unregister the role
        if (ROLE_ADMIN.equals(roleName))
        {
            community.removeAdministrators();
        }
        
        // Second, remove all authorizations for this role by searching for all policies that this 
        // group has on the collection and remove them otherwise the delete will fail because 
        // there are dependencies.
        @SuppressWarnings("unchecked") // the cast is correct
        List<ResourcePolicy> policies = AuthorizeManager.getPolicies(context, community);
        for (ResourcePolicy policy : policies)
        {
            if (policy.getGroupID() == groupID)
            {
                policy.delete();
            }
        }
        
        // Finally, delete the role's actual group.
        community.update();
        role.delete();
        context.commit();
    
        result.setContinue(true);
        result.setOutcome(true);
        result.setMessage(new Message("default","The role was successfully deleted."));
        return result;
    }
    
    /**
     * Delete a collection's template item (which is not a member of the collection).
     * 
     * @param context
     * @param collectionID
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public static FlowResult processDeleteTemplateItem(Context context, int collectionID) throws SQLException, AuthorizeException, IOException
    {
        FlowResult result = new FlowResult();
        
        Collection collection = Collection.find(context, collectionID);
        
        collection.removeTemplateItem();
        context.commit();
        
        result.setContinue(true);
        result.setOutcome(true);
        return result;
    }
	
    
        /**
     * processCurateCollection
     *
     * Utility method to process curation tasks
     * submitted via the DSpace GUI
     *
     * @param context
     * @param dsoID
     * @param request
     *
     */
        public static FlowResult processCurateCollection(Context context, int dsoID, Request request)
                                                                throws AuthorizeException, IOException, SQLException, Exception
	{
                String task = request.getParameter("curate_task");
                Curator curator = FlowCurationUtils.getCurator(task);
               
                try
                {
                    Collection collection = Collection.find(context, dsoID);
                    if (collection != null)
                    {
                        //Call curate(context,ID) to ensure a Task Performer (Eperson) is set in Curator
                        curator.curate(context, collection.getHandle());
                       
                    }
                    return FlowCurationUtils.getRunFlowResult(task, curator, true);
                }
		catch (Exception e) 
                {
                    curator.setResult(task, e.getMessage());
                    return FlowCurationUtils.getRunFlowResult(task, curator, false);
		}
                
	}

        /**
         * queues curation tasks
         */
        public static FlowResult processQueueCollection(Context context, int dsoID, Request request)
                                                                throws AuthorizeException, IOException, SQLException, Exception
	{
                String task = request.getParameter("curate_task");
                Curator curator = FlowCurationUtils.getCurator(task);
                String objId = String.valueOf(dsoID);
                String taskQueueName = ConfigurationManager.getProperty("curate", "ui.queuename");
                boolean status = false;
                Collection collection = Collection.find(context, dsoID);
                if (collection != null)
                {
                    objId = collection.getHandle();
                    try
                    {
                        curator.queue(context, objId, taskQueueName);
                        status = true;
                    }
                    catch (IOException ioe)
                    {
                        // no-op
                    }
                }
                return FlowCurationUtils.getQueueFlowResult(task, status, objId, taskQueueName);
	}

    /** 
     * processCurateCommunity
     *
     * Utility method to process curation tasks
     * submitted via the DSpace GUI
     *
     * @param context
     * @param dsoID
     * @param request
     *
     */
        public static FlowResult processCurateCommunity(Context context, int dsoID, Request request)
                                                                throws AuthorizeException, IOException, SQLException, Exception
	{
                String task = request.getParameter("curate_task");
		Curator curator = FlowCurationUtils.getCurator(task);
                try
                {
                    Community community = Community.find(context, dsoID);
                    if (community != null)
                    {
                        //Call curate(context,ID) to ensure a Task Performer (Eperson) is set in Curator
                        curator.curate(context, community.getHandle());
                    }
                    return FlowCurationUtils.getRunFlowResult(task, curator, true);
                }
                catch (Exception e) 
                {
                    curator.setResult(task, e.getMessage());
                    return FlowCurationUtils.getRunFlowResult(task, curator, false);
		}
	}

        /**
         * queues curation tasks
         */
        public static FlowResult processQueueCommunity(Context context, int dsoID, Request request)
                                                                throws AuthorizeException, IOException, SQLException, Exception
	{
                String task = request.getParameter("curate_task");
                Curator curator = FlowCurationUtils.getCurator(task);
                String objId = String.valueOf(dsoID);
                String taskQueueName = ConfigurationManager.getProperty("curate", "ui.queuename");
                boolean status = false;
                Community community = Community.find(context, dsoID);
                if (community != null)
                {
                    objId = community.getHandle();
                    try
                    {
                        curator.queue(context, objId, taskQueueName);
                        status = true;
                    }
                    catch (IOException ioe)
                    {
                        // no-op
                    }
                }
                return FlowCurationUtils.getQueueFlowResult(task, status, objId, taskQueueName);
	}
	
    
	/**
	 * Check whether this metadata value is a proper XML fragment. If the value is not 
	 * then an error message will be returned that might (sometimes not) tell the user how
	 * to correct the problem.
	 * 
	 * @param value The metadata value
	 * @return An error string of the problem or null if there is no problem with the metadata value.
	 */
	public static String checkXMLFragment(String value)
	{
		// escape the ampersand correctly;
		value = escapeXMLEntities(value);
		
		// Try and parse the XML into a mini-DOM
    	String xml = "<?xml version='1.0' encoding='UTF-8'?><fragment>"+value+"</fragment>";
 	   
 	   	ByteArrayInputStream inputStream = null;
        try {
            inputStream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            inputStream = new ByteArrayInputStream(xml.getBytes()); //not supposed to happen, but never hurts

        }
 	   
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
			// This shouldn't ever occur because we are parsing
			// an in-memory string, but in case it does we'll just return
			// it as a normal error.
			return ioe.getMessage();
		}

    	return null;
	}
	
    /** 
     * Sanitize any XML that was inputed by the user, this will clean up
     * any unescaped characters so that they can be stored as proper XML.
     * These are errors that in general we want to take care of on behalf
     * of the user.
     * 
     * @param value The unsanitized value
     * @return A sanitized value
     */
	public static String escapeXMLEntities(String value)
	{
		if (value == null)
        {
            return null;
        }
		
		// Escape any XML entities
    	int amp = -1;
    	while ((amp = value.indexOf('&', amp+1)) > -1)
    	{
    		// Is it an xml entity named by number?
    		if (substringCompare(value,amp+1,'#'))
            {
                continue;
            }
    		
    		// &amp;
    		if (substringCompare(value,amp+1,'a','m','p',';'))
            {
                continue;
            }
    		
    		// &apos;
    		if (substringCompare(value,amp+1,'a','p','o','s',';'))
            {
                continue;
            }
    		
    		// &quot;
    		if (substringCompare(value,amp+1,'q','u','o','t',';'))
            {
                continue;
            }
    			
    		// &lt;
    		if (substringCompare(value,amp+1,'l','t',';'))
            {
                continue;
            }
    		
    		// &gt;
    		if (substringCompare(value,amp+1,'g','t',';'))
            {
                continue;
            }
    		
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
        {
            return false;
        }
    	
    	// Do all the characters match?
    	for (char character : characters)
    	{
    		if (string.charAt(index) != character)
            {
                return false;
            }
    		index++;
    	}
    	
    	return false;
    }
	

}
