/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import org.apache.cocoon.environment.Request;
import org.apache.cocoon.servlet.multipart.Part;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.browse.BrowseException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.Curator;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.harvest.HarvestScheduler;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.OAIHarvester;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.WorkflowUtils;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;


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

	protected static final AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
	protected static final ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
	protected static final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
	protected static final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
	protected static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();


	protected static final GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
	protected static final HarvestedCollectionService harvestedCollectionService = HarvestServiceFactory.getInstance().getHarvestedCollectionService();
	protected static final WorkflowService workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();

	
	// Collection related functions

	/**
	 * Process the collection metadata edit form.
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @param deleteLogo Determines if the logo should be deleted along with the metadata editing action.
	 * @param request the Cocoon request object
	 * @return A process result's object.
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
	 */
	public static FlowResult processEditCollection(Context context, UUID collectionID, boolean deleteLogo, Request request) throws SQLException, IOException, AuthorizeException
	{
		FlowResult result = new FlowResult();
		
		Collection collection = collectionService.find(context, collectionID);
		
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
		collectionService.setMetadata(context, collection, "name", name);
		collectionService.setMetadata(context, collection, "short_description", shortDescription);
		collectionService.setMetadata(context, collection, "introductory_text", introductoryText);
		collectionService.setMetadata(context, collection, "copyright_text", copyrightText);
		collectionService.setMetadata(context, collection, "side_bar_text", sideBarText);
		collectionService.setMetadata(context, collection, "license", license);
		collectionService.setMetadata(context, collection, "provenance_description", provenanceDescription);
		
        
		// Change or delete the logo
        if (deleteLogo)
        {
        	// Remove the logo
			collectionService.setLogo(context, collection, null);
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
    			
				collectionService.setLogo(context, collection, is);
    		}
        }
        
        // Save everything
        collectionService.update(context, collection);

        
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
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
	 */
	public static FlowResult processSetupCollectionHarvesting(Context context, UUID collectionID, Request request) throws SQLException, IOException, AuthorizeException
	{
		FlowResult result = new FlowResult();
		Collection collection = collectionService.find(context, collectionID);
		HarvestedCollection hc = harvestedCollectionService.find(context, collection);

		String contentSource = request.getParameter("source");

		// First, if this is not a harvested collection (anymore), set the harvest type to 0; possibly also wipe harvest settings  
		if (contentSource.equals("source_normal")) 
		{
			if (hc != null)
            {
				harvestedCollectionService.delete(context, hc);
            }
			
			result.setContinue(true);
		}
		else 
		{
			FlowResult subResult = testOAISettings(context, request);
			
			// create a new harvest instance if all the settings check out
			if (hc == null) {
				hc = harvestedCollectionService.create(context, collection);
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
			
			harvestedCollectionService.update(context, hc);
		}
		        
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
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
	 * @throws TransformerException passed through.
	 * @throws SAXException passed through.
	 * @throws ParserConfigurationException passed through.
	 * @throws CrosswalkException passed through.
	 */
	public static FlowResult processRunCollectionHarvest(Context context, UUID collectionID, Request request)
            throws SQLException, IOException, AuthorizeException, CrosswalkException, ParserConfigurationException, SAXException, TransformerException
	{
		FlowResult result = new FlowResult();
		OAIHarvester harvester;
		List<String> testErrors = new ArrayList<String>();
		Collection collection = collectionService.find(context, collectionID);
		HarvestedCollection hc = harvestedCollectionService.find(context, collection);

		//TODO: is there a cleaner way to do this?
		try
		{
			if (!HarvestScheduler.hasStatus(HarvestScheduler.HARVESTER_STATUS_STOPPED)) {
				synchronized(HarvestScheduler.lock) {
					HarvestScheduler.setInterrupt(HarvestScheduler.HARVESTER_INTERRUPT_INSERT_THREAD, collectionID);
					HarvestScheduler.lock.notify();
				}
			}
			else {
				// Harvester should return some errors in my opinion..
				harvester = new OAIHarvester(context, collection, hc);
				harvester.runHarvest(); // this throws an exception when fetching bitstreams.
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
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
	 * @throws TransformerException passed through.
	 * @throws SAXException passed through.
	 * @throws ParserConfigurationException passed through.
	 * @throws CrosswalkException passed through.
	 * @throws BrowseException passed through.
	 */
	public static FlowResult processReimportCollection(Context context, UUID collectionID, Request request) throws SQLException, IOException, AuthorizeException, CrosswalkException, ParserConfigurationException, SAXException, TransformerException, BrowseException
	{
		boolean originalMode = context.isBatchModeEnabled();
		context.enableBatchMode(true);

		Collection collection = collectionService.find(context, collectionID);
		HarvestedCollection hc = harvestedCollectionService.find(context, collection);
		
		Iterator<Item> it = itemService.findByCollection(context, collection);
		//IndexBrowse ib = new IndexBrowse(context);
		while (it.hasNext()) {
			Item item = it.next();
			//System.out.println("Deleting: " + item.getHandle());
			//ib.itemRemoved(item);
			collectionService.removeItem(context, collection, item);

		}

		hc.setLastHarvested(null);
		hc.setHarvestMessage("");
		harvestedCollectionService.update(context, hc);
		collectionService.update(context, collection);
        // update the context?
		//context.dispatchEvent() // not sure if this is required yet.ts();

		context.enableBatchMode(originalMode);

		return processRunCollectionHarvest(context, collectionID, request);
	}

	/**
	 * Test the supplied OAI settings. 
	 * 
	 * @param context session context.
	 * @param request user's request.
     * @return result of testing.
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
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
	 * @throws IOException passed through.
	 */
	public static UUID getTemplateItemID(Context context, UUID collectionID) throws SQLException, AuthorizeException, IOException
	{
		Collection collection = collectionService.find(context, collectionID);
		Item template = collection.getTemplateItem();
		
		if (template == null)
		{
			collectionService.createTemplateItem(context, collection);
			template = collection.getTemplateItem();
			
			collectionService.update(context, collection);
			itemService.update(context, template);
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
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     * @throws javax.xml.transform.TransformerException passed through.
     * @throws org.xml.sax.SAXException passed through.
     * @throws org.dspace.xmlworkflow.WorkflowConfigurationException passed through.
     * @throws javax.xml.parsers.ParserConfigurationException passed through.
     * @throws org.dspace.workflow.WorkflowException passed through.
	 */
	public static UUID getCollectionRole(Context context, UUID collectionID, String roleName)
            throws SQLException, AuthorizeException, IOException,
            TransformerException, SAXException, WorkflowConfigurationException,
            ParserConfigurationException, WorkflowException {
		Collection collection = collectionService.find(context, collectionID);

		// Determine the group based upon wich role we are looking for.
		Group roleGroup = null;
		if (ROLE_ADMIN.equals(roleName))
		{
			roleGroup = collection.getAdministrators();
			if (roleGroup == null){
				roleGroup = collectionService.createAdministrators(context, collection);
            }
		} 
		else if (ROLE_SUBMIT.equals(roleName))
		{
			roleGroup = collection.getSubmitters();
			if (roleGroup == null)
				roleGroup = collectionService.createSubmitters(context, collection);
		}else{
			roleGroup = workflowService.getWorkflowRoleGroup(context, collection, roleName, roleGroup);
		}

		// In case we needed to create a group, save our changes
		collectionService.update(context, collection);

		// If the role name was valid then role should be non null,
		if (roleGroup != null)
			return roleGroup.getID();

		return null;
    }

	/**
	 * Delete one of collection's roles
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @param roleName ADMIN, WF_STEP1,	WF_STEP2, WF_STEP3,	SUBMIT, DEFAULT_READ.
	 * @param groupID The id of the group associated with this role.
	 * @return A process result's object.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.xmlworkflow.WorkflowConfigurationException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
	 */
	public static FlowResult processDeleteCollectionRole(Context context, UUID collectionID, String roleName, UUID groupID)
            throws SQLException, UIException, IOException, AuthorizeException, WorkflowConfigurationException
    {
		FlowResult result = new FlowResult();
		
		Collection collection = collectionService.find(context,collectionID);
		Group role = groupService.find(context, groupID);
		
		// First, Unregister the role
		if (ROLE_ADMIN.equals(roleName))
		{
			collectionService.removeAdministrators(context, collection);
		} 
		else if (ROLE_SUBMIT.equals(roleName))
		{
			collectionService.removeSubmitters(context, collection);
		}
        else{
            if(WorkflowServiceFactory.getInstance().getWorkflowService() instanceof XmlWorkflowService)
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
		List<ResourcePolicy> policies = authorizeService.getPolicies(context, collection);
		for (ResourcePolicy policy : policies)
		{
			if (policy.getGroup() != null && policy.getGroup().getID().equals(groupID))
            {
				resourcePolicyService.delete(context, policy);
            }
		}
		
		// Finally, Delete the role's actual group.
		collectionService.update(context, collection);
		groupService.delete(context, role);

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
	 * @param collection The collection.
	 * @return The id of the group associated with that particular role or -1
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
	 */
	public static Group getCollectionDefaultRead(Context context, Collection collection) throws SQLException, AuthorizeException
	{

		List<Group> itemGroups = authorizeService.getAuthorizedGroups(context, collection, Constants.DEFAULT_ITEM_READ);
		List<Group> bitstreamGroups = authorizeService.getAuthorizedGroups(context, collection, Constants.DEFAULT_BITSTREAM_READ);
		
       Group itemGroup = null;

		// If there are more than one groups assigned either of these privileges then this role based method will not work.
        // The user will need to go to the authorization section to manually straighten this out.		
		if (itemGroups.size() != 1 || bitstreamGroups.size() != 1)
		{
            itemGroup = null;
		}
		else
		{
	        itemGroup = itemGroups.get(0);
	        Group bitstreamGroup = bitstreamGroups.get(0);
	        
            // If the same group is not assigned both of these privileges then this role based method will not work. The user 
            // will need to go to the authorization section to manually straighten this out.
	        if (!itemGroup.getID().equals(bitstreamGroup.getID()))
	        {
                itemGroup = null;
	        }
		}

		return itemGroup;
	}

	/**
	 * @see #getCollectionDefaultRead(Context, Collection)
     */
	public static UUID getCollectionDefaultRead(final Context context, final UUID collectionID) throws SQLException, AuthorizeException {
		return getCollectionDefaultRead(context, collectionService.find(context,collectionID)).getID();
	}

	/**
	 * Change default privileges from the anonymous group to a new group that will be created and
	 * appropriate privileges assigned. The id of this new group will be returned.
	 * 
	 * @param context The current DSpace context.
	 * @param collectionID The collection id.
	 * @return The group ID of the new group.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
	 */
	public static UUID createCollectionDefaultReadGroup(Context context, UUID collectionID) throws SQLException, AuthorizeException, UIException
	{
		Collection collection = collectionService.find(context,collectionID);
		Group defaultRead = getCollectionDefaultRead(context, collection);
		
		if (defaultRead != null && !defaultRead.getName().equals(Group.ANONYMOUS))
        {
            throw new UIException("Unable to create a new default read group because either the group already exists or multiple groups are assigned the default privileges.");
        }
		
		Group role = groupService.create(context);
        groupService.setName(role, "COLLECTION_"+collection.getID().toString() +"_DEFAULT_READ");
		
		// Remove existing privileges from the anonymous group.
		authorizeService.removePoliciesActionFilter(context, collection, Constants.DEFAULT_ITEM_READ);
		authorizeService.removePoliciesActionFilter(context, collection, Constants.DEFAULT_BITSTREAM_READ);
		
		// Grant our new role the default privileges.
		authorizeService.addPolicy(context, collection, Constants.DEFAULT_ITEM_READ, role);
		authorizeService.addPolicy(context, collection, Constants.DEFAULT_BITSTREAM_READ, role);
		
		// Commit the changes
		groupService.update(context, role);

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
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
     * @throws java.io.IOException passed through.
	 */
	public static FlowResult changeCollectionDefaultReadToAnonymous(Context context, UUID collectionID)
            throws SQLException, AuthorizeException, UIException, IOException {
		FlowResult result = new FlowResult();
		
		Collection collection = collectionService.find(context,collectionID);
		Group defaultRead = getCollectionDefaultRead(context, collection);
		
		if (defaultRead == null || defaultRead.getName().equals(Group.ANONYMOUS))
		{
			throw new UIException("Unable to delete the default read role because the role is either already assigned to the anonymous group or multiple groups are assigned the default privileges.");
		}
		
		Group anonymous = groupService.findByName(context, Group.ANONYMOUS);
		
		// Delete the old role, this will remove the default privileges.
		groupService.delete(context, defaultRead);
		
		// Set anonymous as the default read group.
		authorizeService.addPolicy(context, collection, Constants.DEFAULT_ITEM_READ, anonymous);
		authorizeService.addPolicy(context, collection, Constants.DEFAULT_BITSTREAM_READ, anonymous);
		
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
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
	 */
	public static FlowResult processDeleteCollection(Context context, UUID collectionID) throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();
		
		Collection collection = collectionService.find(context, collectionID);
		collectionService.delete(context, collection);
		
		
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
     * @param request user's request.
	 * @return A process result's object.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
	 */
	public static FlowResult processCreateCollection(Context context, UUID communityID, Request request)
            throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();
		
		Community parent = communityService.find(context, communityID);
		Collection newCollection = collectionService.create(context, parent);
		
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
		collectionService.setMetadata(context, newCollection, "name", name);
		collectionService.setMetadata(context, newCollection, "short_description", shortDescription);
		collectionService.setMetadata(context, newCollection, "introductory_text", introductoryText);
		collectionService.setMetadata(context, newCollection, "copyright_text", copyrightText);
		collectionService.setMetadata(context, newCollection, "side_bar_text", sideBarText);
		collectionService.setMetadata(context, newCollection, "license", license);
		collectionService.setMetadata(context, newCollection, "provenance_description", provenanceDescription);

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
			
			collectionService.setLogo(context, newCollection, is);
		}
        
        // Save everything
		collectionService.update(context, newCollection);
        // success
        result.setContinue(true);
        result.setOutcome(true);
        result.setMessage(new Message("default","The collection was successfully created."));
        result.setParameter("collectionID", newCollection.getID());
		
		return result;
	}

	// Community related functions

	/**
	 * Create a new community.
	 * 
	 * @param context The current DSpace context.
	 * @param communityID The id of the parent community (-1 for a top-level community).
     * @param request user's request.
	 * @return A process result's object.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     * @throws java.sql.SQLException passed through.
	 */
	public static FlowResult processCreateCommunity(Context context, UUID communityID, Request request)
            throws AuthorizeException, IOException, SQLException
	{
		FlowResult result = new FlowResult();

		Community newCommunity;

		if (communityID != null)
        {
            newCommunity = communityService.createSubcommunity(context, communityService.find(context, communityID));
        }
		else
        {
            newCommunity = communityService.create(null, context);
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
		
		communityService.setMetadata(context, newCommunity, "name", name);
		communityService.setMetadata(context, newCommunity, "short_description", shortDescription);
		communityService.setMetadata(context, newCommunity, "introductory_text", introductoryText);
		communityService.setMetadata(context, newCommunity, "copyright_text", copyrightText);
		communityService.setMetadata(context, newCommunity, "side_bar_text", sideBarText);
        
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
			
			communityService.setLogo(context, newCommunity, is);
		}
        
		// Save everything
		communityService.update(context, newCommunity);
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
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     * @throws java.sql.SQLException passed through.
	 */
	public static FlowResult processEditCommunity(Context context, UUID communityID, boolean deleteLogo, Request request)
            throws AuthorizeException, IOException, SQLException
	{
		FlowResult result = new FlowResult();

		Community community = communityService.find(context, communityID);
		
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
		communityService.setMetadata(context, community, "name", name);
		communityService.setMetadata(context, community, "short_description", shortDescription);
        communityService.setMetadata(context, community, "introductory_text", introductoryText);
        communityService.setMetadata(context, community, "copyright_text", copyrightText);
        communityService.setMetadata(context, community, "side_bar_text", sideBarText);
        
        if (deleteLogo)
        {
        	// Remove the logo
        	communityService.setLogo(context, community, null);
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
    			
				communityService.setLogo(context, community, is);
    		}
        }
        
        // Save everything
        communityService.update(context, community);

        // No notice...
        result.setContinue(true);
		return result;
	}

	/**
	 * Delete community itself.
	 * 
	 * @param context The current DSpace context.
	 * @param communityID The community id.
	 * @return A process result's object.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
	 */
	public static FlowResult processDeleteCommunity(Context context, UUID communityID) throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();
		
		Community community = communityService.find(context, communityID);
		
		communityService.delete(context, community);

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
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     */
    public static UUID getCommunityRole(Context context, UUID communityID, String roleName) throws SQLException, AuthorizeException, IOException
    {
        Community community = communityService.find(context, communityID);
	
        // Determine the group based upon which role we are looking for.
        Group role = null;
        if (ROLE_ADMIN.equals(roleName))
        {
            role = community.getAdministrators();
            if (role == null)
            {
                role = communityService.createAdministrators(context, community);
            }
        } 
	
        // In case we needed to create a group, save our changes
		communityService.update(context, community);

        // If the role name was valid then role should be non null,
        if (role != null)
        {
            return role.getID();
        }
        
        return null;
    }

	/**
     * Delete one of a community's roles
     * 
     * @param context The current DSpace context.
     * @param communityID The community id.
     * @param roleName ADMIN.
     * @param groupID The id of the group associated with this role.
     * @return A process result's object.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    public static FlowResult processDeleteCommunityRole(Context context, UUID communityID, String roleName, UUID groupID)
            throws SQLException, UIException, IOException, AuthorizeException
    {
        FlowResult result = new FlowResult();
        
        Community community = communityService.find(context, communityID);
        Group role = groupService.find(context, groupID);
        
        // First, unregister the role
        if (ROLE_ADMIN.equals(roleName))
        {
			communityService.removeAdministrators(context, community);
        }
        
        // Second, remove all authorizations for this role by searching for all policies that this 
        // group has on the collection and remove them otherwise the delete will fail because 
        // there are dependencies.
        @SuppressWarnings("unchecked") // the cast is correct
        List<ResourcePolicy> policies = authorizeService.getPolicies(context, community);
        for (ResourcePolicy policy : policies)
        {
            if (policy.getGroup() != null && policy.getGroup().getID().equals(groupID))
            {
                resourcePolicyService.delete(context, policy);
            }
        }
        
        // Finally, delete the role's actual group.
        communityService.update(context, community);
		groupService.delete(context, role);

        result.setContinue(true);
        result.setOutcome(true);
        result.setMessage(new Message("default","The role was successfully deleted."));
        return result;
    }
    
    /**
     * Delete a collection's template item (which is not a member of the collection).
     * 
     * @param context session context.
     * @param collectionID the collection.
     * @return continuation result.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     * @throws IOException passed through.
     */
    public static FlowResult processDeleteTemplateItem(Context context, UUID collectionID) throws SQLException, AuthorizeException, IOException
    {
        FlowResult result = new FlowResult();
        
        Collection collection = collectionService.find(context, collectionID);
        
        collectionService.removeTemplateItem(context, collection);

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
     * @param context session context.
     * @param dsoID the object to be curated.
     * @param request user's request.
     * @return flow result.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     * @throws java.sql.SQLException passed through.
     */
    public static FlowResult processCurateCollection(Context context, UUID dsoID, Request request)
        throws AuthorizeException, IOException, SQLException, Exception
	{
                String task = request.getParameter("curate_task");
                Curator curator = FlowCurationUtils.getCurator(task);
               
                try
                {
                    Collection collection = collectionService.find(context, dsoID);
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
     * Queues curation tasks.
     * @param context session context.
     * @param dsoID the object to be curated.
     * @param request user's request.
     * @return flow result.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     * @throws java.sql.SQLException passed through.
     */
    public static FlowResult processQueueCollection(Context context, UUID dsoID, Request request)
            throws AuthorizeException, IOException, SQLException, Exception
	{
                String task = request.getParameter("curate_task");
                Curator curator = FlowCurationUtils.getCurator(task);
                String objId = String.valueOf(dsoID);
                String taskQueueName = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("curate.ui.queuename");
                boolean status = false;
                Collection collection = collectionService.find(context, dsoID);
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
     * Utility method to process curation tasks submitted via the DSpace GUI.
     *
     * @param context session context.
     * @param dsoID object to be curated.
     * @param request user's request
     * @return flow result.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     * @throws java.sql.SQLException passed through.
     */
    public static FlowResult processCurateCommunity(Context context, UUID dsoID, Request request)
            throws AuthorizeException, IOException, SQLException, Exception
	{
        String task = request.getParameter("curate_task");
		Curator curator = FlowCurationUtils.getCurator(task);
        try
        {
            Community community = communityService.find(context, dsoID);
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
     * queues curation tasks.
     *
     * @param context session context.
     * @param dsoID object to be curated.
     * @param request user's request.
     * @return flow result.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     * @throws java.sql.SQLException passed through.
     */
    public static FlowResult processQueueCommunity(Context context, UUID dsoID, Request request)
            throws AuthorizeException, IOException, SQLException, Exception
	{
                String task = request.getParameter("curate_task");
                Curator curator = FlowCurationUtils.getCurator(task);
                String objId = String.valueOf(dsoID);
                String taskQueueName = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("curate.ui.queuename");
                boolean status = false;
                Community community = communityService.find(context, dsoID);
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
