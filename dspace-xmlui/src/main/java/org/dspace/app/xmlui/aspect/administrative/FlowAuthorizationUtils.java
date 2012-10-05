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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.PolicySet;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;

import org.dspace.core.Constants;



/**
 * 
 * FIXME: add documentation
 * 
 * @author Alexey maslov
 */
public class FlowAuthorizationUtils {

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
	 */
	public static FlowResult resolveItemIdentifier(Context context, String identifier) throws SQLException 
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		//Check whether it's a handle or internal id (by check ing if it has a slash in the string)
		if (identifier.contains("/")) {
			DSpaceObject dso = HandleManager.resolveToObject(context, identifier);
			
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
				item = Item.find(context, Integer.valueOf(identifier));
			} catch (NumberFormatException e) {
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
	 * @return A process result's object.
	 */
	public static FlowResult processEditPolicy(Context context, int objectType, int objectID, int policyID, int groupID, int actionID, 
                                                    String name, String description, String startDateParam, String endDateParam) throws SQLException, AuthorizeException
	{
		FlowResult result = new FlowResult();
		boolean added = false;
	
		ResourcePolicy policy = ResourcePolicy.find(context, policyID);
		
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
		if (groupID == -1) {
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

        // check if a similar policy is already in place
        if(policy==null){
            if(AuthorizeManager.isAnIdenticalPolicyAlreadyInPlace(context, objectType, objectID, groupID, actionID, -1)){
                result.setContinue(false);
                result.addError("duplicatedPolicy");
                return result;
            }
        }
        else{
            if(AuthorizeManager.isAnIdenticalPolicyAlreadyInPlace(context, objectType, objectID, groupID, actionID, policy.getID())){
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
			        policyParent = Community.find(context, objectID); 
			        AuthorizeUtil.authorizeManageCommunityPolicy(context, (Community)policyParent);
			        break;
			    }
			case Constants.COLLECTION:
		        {
			        policyParent = Collection.find(context, objectID); 
			        AuthorizeUtil.authorizeManageCollectionPolicy(context, (Collection)policyParent);        
			        break;
		        }
			case Constants.ITEM:
		        {
			        policyParent = Item.find(context, objectID); 
			        AuthorizeUtil.authorizeManageItemPolicy(context, (Item) policyParent);
			        break;
		        }
			case Constants.BUNDLE:
		        {
			        policyParent = Bundle.find(context, objectID); 
			        AuthorizeUtil.authorizeManageItemPolicy(context, (Item) (policyParent.getParentObject()));
			        break;
		        }
			case Constants.BITSTREAM: 
		        {
			        policyParent = Bitstream.find(context, objectID); 
			        AuthorizeUtil
                        .authorizeManageItemPolicy(context, (Item) (policyParent
                                .getParentObject()));
			        break;
		        }
			}
			policy = ResourcePolicy.create(context);
			policy.setResource(policyParent);
            policy.setRpType(ResourcePolicy.TYPE_CUSTOM);
			added = true;
		}
		
	    Group group = Group.find(context, groupID);
	    
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
	    	logoContainer = Collection.find(context, objectID);
	        logo = ((Collection)logoContainer).getLogo();
	    }
	    else if (objectType == Constants.COMMUNITY)
	    {
	    	logoContainer = Community.find(context, objectID);
	        logo = ((Community)logoContainer).getLogo();
	    }
	    
	    if (logo != null)
	    {
	        List policySet = AuthorizeManager.getPolicies(context, logoContainer);
	        AuthorizeManager.removeAllPolicies(context, logo);
	        AuthorizeManager.addPolicies(context, policySet, logo);
	    }
	    
	    // Perform the update action
	    policy.update();
	    context.commit();
	    
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
	 */
	public static FlowResult processDeletePolicies(Context context, String[] policyIDs) throws NumberFormatException, SQLException, AuthorizeException
	{
		FlowResult result = new FlowResult();
	
		for (String id : policyIDs) 
		{
			ResourcePolicy policyDeleted = ResourcePolicy.find(context, Integer.valueOf(id));
			// check authorization
			AuthorizeUtil.authorizeManagePolicy(context, policyDeleted);
			policyDeleted.delete();
	    }
	
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(new Message("default","The policies were deleted successfully"));
		
		return result;
	}
	
	
	/**
	 * Process addition of a several authorizations at once, as entered in the wildcard/advanced authorizations tool 
	 * 
	 * @param context The current DSpace context.
	 * @param groupIDs The IDs of the groups to be associated with the newly created policies
	 * @param actionID The ID of the action to be associated with the policies
	 * @param resourceID Whether the policies will apply to Items or Bitstreams
	 * @param collectionIDs The IDs of the collections that the policies will be applied to 
	 * @return A process result's object.
	 */
	public static FlowResult processAdvancedPolicyAdd(Context context, String[] groupIDs, int actionID,
			int resourceID, String [] collectionIDs, String name, String description, String startDateParam, String endDateParam) throws NumberFormatException, SQLException, AuthorizeException
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
				PolicySet.setPolicies(context, Constants.COLLECTION, Integer.valueOf(collectionID),
			            resourceID, actionID, Integer.valueOf(groupID), false, false, name, description, startDate, endDate);
				
				// if it's a bitstream, do it to the bundle too
			    if (resourceID == Constants.BITSTREAM)
			    {
			    	PolicySet.setPolicies(context, Constants.COLLECTION, Integer.valueOf(collectionID),
			    			Constants.BUNDLE, actionID, Integer.valueOf(groupID), false, false, name, description, startDate, endDate);
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
	 */
	public static FlowResult processAdvancedPolicyDelete(Context context, int resourceID, String [] collectionIDs) 
			throws NumberFormatException, SQLException, AuthorizeException
	{
	    AuthorizeUtil.requireAdminRole(context);
		FlowResult result = new FlowResult();
		
		for (String collectionID : collectionIDs) 
		{
			PolicySet.setPolicies(context, Constants.COLLECTION, Integer.valueOf(collectionID),
		            resourceID, 0, 0, false, true);
			
			// if it's a bitstream, do it to the bundle too
		    if (resourceID == Constants.BITSTREAM)
		    {
		    	PolicySet.setPolicies(context, Constants.COLLECTION, Integer.valueOf(collectionID),
		    			Constants.BUNDLE, 0, 0, false, true);
		    }
		}
		
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(new Message("default","The policies for the selected collections were cleared."));
		
		return result;
	}
}