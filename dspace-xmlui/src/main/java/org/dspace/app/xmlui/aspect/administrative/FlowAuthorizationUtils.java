/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeServiceImpl;
import org.dspace.authorize.PolicySet;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.HandleServiceImpl;

import org.dspace.core.Constants;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

/**
 * FIXME: add documentation
 * 
 * @author Alexey maslov
 */
public class FlowAuthorizationUtils {

	protected static final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
	protected static final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected static final BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
	protected static final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
	protected static final HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
	protected static final AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
	protected static final ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
	protected static final GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

	/** Language Strings */
	//example language string
//	private static final Message T_add_eperson_success_notice =
//		new Message("default","xmlui.administrative.FlowUtils.add-eperson-success-notice");

    /**
	 * Resolve an identifier submitted into the item lookup box. If it contains a slash, it's assumed to be a
	 * handle and is resolved by that mechanism into an item, collection or community. Otherwise, it's assumed
	 * to be an item and looked up by ID. 
	 * 
	 * @param context The current DSpace context.
	 * @param identifier The identifier that is to be resolved.
	 * @return A process result's object.
     * @throws java.sql.SQLException passed through.
	 */
	public static FlowResult resolveItemIdentifier(Context context, String identifier) throws SQLException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		//Check whether it's a handle or internal id (by check ing if it has a slash in the string)
		if (identifier.contains("/")) {
			DSpaceObject dso = handleService.resolveToObject(context, identifier);
			
			if (dso != null && dso.getType() == Constants.ITEM) { 
				result.setParameter("itemID", dso.getID());
				result.setParameter("type", Constants.ITEM);
				result.setContinue(true);
				return result;
			}
			else if (dso != null && dso.getType() == Constants.COLLECTION) { 
				result.setParameter("collectionID", dso.getID());
				result.setParameter("type", Constants.COLLECTION);
				result.setContinue(true);
				return result;
			}
			else if (dso != null && dso.getType() == Constants.COMMUNITY) { 
				result.setParameter("communityID", dso.getID());
				result.setParameter("type", Constants.COMMUNITY);
				result.setContinue(true);
				return result;
			}
		}
		// Otherwise, it's assumed to be a DSpace Item
		else {
			Item item = null;
			try {
				item = itemService.find(context, UUID.fromString(identifier));
			} catch (Exception e) {
				// ignoring the exception in case of a malformed input string
			}
			
			if (item != null) {
				result.setParameter("itemID", item.getID());
				result.setParameter("type", Constants.ITEM);
				result.setContinue(true);
				return result;
			}
		}
		
		result.addError("identifier");
		return result;	
	}

	/**
	 * Process the editing of an existing or a newly created policy. 
	 * 
	 * @param context The current DSpace context.
	 * @param objectType The type of the policy's parent object (ITEM, COLLECTION, COMMUNITY)
	 * @param objectID The ID of the policy's parent object
	 * @param policyID The ID of the policy being edited (-1 if a new policy is being created)
	 * @param groupID The ID of the group to be associated with this policy
	 * @param actionID The ID of the action (dependent on the objectType) to be associated with this policy
     * @param name name the policy.
     * @param description describe the policy.
     * @param startDateParam when the policy starts to apply.
     * @param endDateParam when the policy no longer applies.
	 * @return A process result's object.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
	 */
	public static FlowResult processEditPolicy(Context context, int objectType, UUID objectID, int policyID, UUID groupID, int actionID,
                                                    String name, String description, String startDateParam, String endDateParam)
            throws SQLException, AuthorizeException
	{
		FlowResult result = new FlowResult();
		boolean added = false;
	
		ResourcePolicy policy = resourcePolicyService.find(context, policyID);
		
		// check authorization to edit an existent policy
		if (policy != null)
		{
		    AuthorizeUtil.authorizeManagePolicy(context, policy);
		}
		
		/* First and foremost, if no group or action was selected, throw an error back to the user */
		if (actionID == -1) {
			result.setContinue(false);
			result.addError("action_id");
			return result;
		}
		if (groupID == null) {
			result.setContinue(false);
			result.addError("group_id");
			return result;
		}


        // check dates
        Date startDate = null;
        Date endDate = null;

        // 05/01/2012 valid date
        if(StringUtils.isNotBlank(startDateParam)){

            try {
                startDate = DateUtils.parseDate(startDateParam, new String[]{"yyyy-MM-dd", "yyyy-MM", "yyyy"});
            } catch (ParseException e) {
                startDate = null;
            }
            if(startDate==null){
                result.setContinue(false);
                result.addError("startDate");
                return result;
            }
        }

        if(StringUtils.isNotBlank(endDateParam)){
            try {
                endDate = DateUtils.parseDate(endDateParam, new String[]{"yyyy-MM-dd", "yyyy-MM", "yyyy"});
            } catch (ParseException e) {
                endDate = null;
            }

            if(endDate==null){
                result.setContinue(false);
                result.addError("endDate");
                return result;
            }
        }

        if(endDate!=null && startDate!=null){
            if(startDate.after(endDate)){
                result.setContinue(false);
                result.addError("startDateGreaterThenEndDate");
                return result;
            }
        }
        // end check dates

		DSpaceObject dso = ContentServiceFactory.getInstance().getDSpaceObjectService(objectType).find(context, objectID);
		// check if a similar policy is already in place
        if(policy==null){
            if(authorizeService.isAnIdenticalPolicyAlreadyInPlace(context, dso, groupService.find(context, groupID), actionID, -1)){
                result.setContinue(false);
                result.addError("duplicatedPolicy");
                return result;
            }
        }
        else{
            if(authorizeService.isAnIdenticalPolicyAlreadyInPlace(context, dso, groupService.find(context, groupID), actionID, policy.getID())){
                result.setContinue(false);
                result.addError("duplicatedPolicy");
                return result;
            }
        }

        /* If the policy doesn't exist, create a new one and set its parent resource */
		DSpaceObject policyParent = null;
		if (policy == null) 
		{
			switch (objectType) {
			case Constants.COMMUNITY: 
			    {
			        policyParent = communityService.find(context, objectID);
			        AuthorizeUtil.authorizeManageCommunityPolicy(context, (Community)policyParent);
			        break;
			    }
			case Constants.COLLECTION:
		        {
			        policyParent = collectionService.find(context, objectID);
			        AuthorizeUtil.authorizeManageCollectionPolicy(context, (Collection)policyParent);        
			        break;
		        }
			case Constants.ITEM:
		        {
			        policyParent = itemService.find(context, objectID);
			        AuthorizeUtil.authorizeManageItemPolicy(context, (Item) policyParent);
			        break;
		        }
			case Constants.BUNDLE:
		        {
			        policyParent = bundleService.find(context, objectID);
			        AuthorizeUtil.authorizeManageItemPolicy(context, (Item) (bundleService.getParentObject(context, (Bundle) policyParent)));
			        break;
		        }
			case Constants.BITSTREAM: 
		        {
			        policyParent = bitstreamService.find(context, objectID);
			        AuthorizeUtil
                        .authorizeManageItemPolicy(context, (Item) (bitstreamService.getParentObject(context, (Bitstream) policyParent)));
			        break;
				}
			default:
				//If we can't find a parent the policy will receive a NULL dspace object, this is not something we want.
				throw new IllegalArgumentException("Invalid DSpaceObject type provided");
			}
			policy = resourcePolicyService.create(context);
			policy.setdSpaceObject(policyParent);
            policy.setRpType(ResourcePolicy.TYPE_CUSTOM);
			added = true;
		}
		
	    Group group = groupService.find(context, groupID);
	    
	    //  modify the policy
	    policy.setAction(actionID);
	    policy.setGroup(group);

        policy.setRpName(name);
        policy.setRpDescription(description);

        if(endDate!=null) policy.setEndDate(endDate);
        else policy.setEndDate(null);


        if(startDate!=null) policy.setStartDate(startDate);
        else policy.setStartDate(null);

	      
	    // propagate the changes to the logo, which is treated on the same level as the parent object
	    Bitstream logo = null;
	    DSpaceObject logoContainer = null;
	    if (objectType == Constants.COLLECTION)
	    {
	    	logoContainer = collectionService.find(context, objectID);
	        logo = ((Collection)logoContainer).getLogo();
	    }
	    else if (objectType == Constants.COMMUNITY)
	    {
	    	logoContainer = communityService.find(context, objectID);
	        logo = ((Community)logoContainer).getLogo();
	    }
	    
	    if (logo != null)
	    {
	        List policySet = authorizeService.getPolicies(context, logoContainer);
			authorizeService.removeAllPolicies(context, logo);
			authorizeService.addPolicies(context, policySet, logo);
	    }
	    
	    // Perform the update action
	    resourcePolicyService.update(context, policy);

	    result.setContinue(true);
	    result.setOutcome(true);
	    if (added)
        {
            result.setMessage(new Message("default", "A new policy was created successfully"));
        }
	    else
        {
            result.setMessage(new Message("default", "The policy was edited successfully"));
        }
	    
	    result.setParameter("policyID", policy.getID());
	    
		return result;
	}

    /**
	 * Delete the policies specified by the policyIDs parameter. This assumes that the
	 * deletion has been confirmed.
	 * 
	 * @param context The current DSpace context
	 * @param policyIDs The unique ids of the policies being deleted.
	 * @return A process result's object.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
	 */
	public static FlowResult processDeletePolicies(Context context, String[] policyIDs)
            throws NumberFormatException, SQLException, AuthorizeException
	{
		FlowResult result = new FlowResult();
	
		for (String id : policyIDs) 
		{
			ResourcePolicy policyDeleted = resourcePolicyService.find(context, Integer.valueOf(id));
			// check authorization
			AuthorizeUtil.authorizeManagePolicy(context, policyDeleted);
			resourcePolicyService.delete(context, policyDeleted);
	    }
	
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(new Message("default","The policies were deleted successfully"));
		
		return result;
	}

	/**
	 * Process addition of a several authorizations at once, as entered in the wildcard/advanced authorizations tool.
	 *
	 * @param context The current DSpace context.
	 * @param groupIDs The IDs of the groups to be associated with the newly created policies
	 * @param actionID The ID of the action to be associated with the policies
	 * @param resourceID Whether the policies will apply to Items or Bitstreams
	 * @param collectionIDs The IDs of the collections that the policies will be applied to 
     * @param name name the policy.
     * @param description describe the policy.
     * @param startDateParam start enforcing the policy now.
     * @param endDateParam stop enforcing the policy now.
	 * @return A process result's object.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
	 */
	public static FlowResult processAdvancedPolicyAdd(Context context, String[] groupIDs, int actionID,
			int resourceID, String [] collectionIDs, String name, String description, String startDateParam, String endDateParam)
            throws NumberFormatException, SQLException, AuthorizeException
	{
	    AuthorizeUtil.requireAdminRole(context);
		FlowResult result = new FlowResult();

        if(groupIDs==null){
            result.setContinue(false);
            result.addError("groupIDs");
            return result;
        }

        if(collectionIDs==null){
            result.setContinue(false);
            result.addError("collectionIDs");
            return result;
        }

         // check dates
        Date startDate = null;
        Date endDate = null;

        // 05/01/2012 valid date
        if(StringUtils.isNotBlank(startDateParam)){
            try {
                startDate = DateUtils.parseDate(startDateParam, new String[]{"yyyy-MM-dd", "yyyy-MM", "yyyy"});
            } catch (ParseException e) {
                startDate = null;
            }
            if(startDate==null){
                result.setContinue(false);
                result.addError("startDate");
                return result;
            }
        }

        if(StringUtils.isNotBlank(endDateParam)){
            try {
                endDate = DateUtils.parseDate(endDateParam, new String[]{"yyyy-MM-dd", "yyyy-MM", "yyyy"});
            } catch (ParseException e) {
                endDate = null;
            }
            if(endDate==null){
                result.setContinue(false);
                result.addError("endDate");
                return result;
            }
        }

        if(endDate!=null && startDate!=null){
            if(startDate.after(endDate)){
                result.setContinue(false);
                result.addError("startDateGreaterThenEndDate");
                return result;
            }
        }
        // end check dates

		for (String groupID : groupIDs) 
		{
			for (String collectionID : collectionIDs) 
			{
				PolicySet.setPolicies(context, Constants.COLLECTION, UUID.fromString(collectionID),
			            resourceID, actionID, UUID.fromString(groupID), false, false, name, description, startDate, endDate);
				
				// if it's a bitstream, do it to the bundle too
			    if (resourceID == Constants.BITSTREAM)
			    {
			    	PolicySet.setPolicies(context, Constants.COLLECTION, UUID.fromString(collectionID),
			    			Constants.BUNDLE, actionID, UUID.fromString(groupID), false, false, name, description, startDate, endDate);
			    }
			}
	    }	
		
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(new Message("default","The policies were added successfully!"));
		
		return result;
	}
	
	/**
	 * Process the deletion of all authorizations across a set of collections, regardless of associated 
	 * actions or groups. This functionality should probably not be used, ever, unless the goal to 
	 * completely reset a collection for one reason or another. 
	 * 
	 * @param context The current DSpace context.
	 * @param resourceID Whether the policies will apply to Items or Bitstreams
	 * @param collectionIDs The IDs of the collections that the policy wipe will be applied to 
	 * @return A process result's object.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
	 */
	public static FlowResult processAdvancedPolicyDelete(Context context, int resourceID, String [] collectionIDs) 
			throws NumberFormatException, SQLException, AuthorizeException
	{
	    AuthorizeUtil.requireAdminRole(context);
		FlowResult result = new FlowResult();
		
		for (String collectionID : collectionIDs) 
		{
			PolicySet.setPolicies(context, Constants.COLLECTION, UUID.fromString(collectionID),
		            resourceID, 0, null, false, true);
			
			// if it's a bitstream, do it to the bundle too
		    if (resourceID == Constants.BITSTREAM)
		    {
		    	PolicySet.setPolicies(context, Constants.COLLECTION, UUID.fromString(collectionID),
		    			Constants.BUNDLE, 0, null, false, true);
		    }
		}
		
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(new Message("default","The policies for the selected collections were cleared."));
		
		return result;
	}
}
