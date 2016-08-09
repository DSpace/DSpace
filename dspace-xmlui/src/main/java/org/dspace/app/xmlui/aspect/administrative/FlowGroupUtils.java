/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.*;

/**
 * Utility methods to processes actions on Groups. These methods are used
 * exclusively from the administrative flow scripts.
 *
 * @author Scott Phillips
 */
public class FlowGroupUtils {

	/** Language Strings */
	private static final Message T_edit_group_success_notice =
		new Message("default","xmlui.administrative.FlowGroupUtils.edit_group_success_notice");
	
	private static final Message T_delete_group_success_notice =
		new Message("default","xmlui.administrative.FlowGroupUtils.delete_group_success_notice");

	protected static final EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
	protected static final GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
	protected static final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
	protected static final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

	/**
	 * Return the current name for the given group ID.
	 * @param context The current DSpace context.
	 * @param groupID The group id.
	 * @return The group's name.
     * @throws java.sql.SQLException passed through.
	 */
	public static String getName(Context context, UUID groupID) throws SQLException
	{
		if (groupID == null)
        {
            return "New Group";
        }
		
		Group group = groupService.find(context,groupID);
		
		if (group == null)
        {
            return "New Group";
        }
		
		return group.getName();
	}
	
	/**
	 * Return the list of current epeople ID's that are a member of this group.
	 * 
	 * @param context The current DSpace context
	 * @param groupID The group's id.
	 * @return An array of ids.
     * @throws java.sql.SQLException passed through.
	 */
	public static String[] getEPeopleMembers(Context context, UUID groupID) throws SQLException
	{
		// New group, just return an empty list
		if (groupID == null)
        {
            return new String[0];
        }
		
		Group group = groupService.find(context,groupID);
		
		if (group == null)
        {
            return new String[0];
        }
		
		List<EPerson> epeople = group.getMembers();
		
		String[] epeopleIDs = new String[epeople.size()];
		for (int i=0; i < epeople.size(); i++)
        {
			epeopleIDs[i] = String.valueOf(epeople.get(i).getID());
        }
		
		return epeopleIDs;
	}
	
	/**
	 * Return the list of current group id's that are a member of this group.
	 * 
	 * @param context The current DSpace context
	 * @param groupID The group's id.
	 * @return An array of ids.
     * @throws java.sql.SQLException passed through.
	 */
	public static String[] getGroupMembers(Context context, UUID groupID) throws SQLException
	{
		if (groupID == null)
        {
            return new String[0];
        }
		
		Group group = groupService.find(context,groupID);
		
		if (group == null)
        {
            return new String[0];
        }
		
		List<Group> groups = group.getMemberGroups();
		
		String[] groupIDs = new String[groups.size()];
		for (int i=0; i < groups.size(); i++)
        {
			groupIDs[i] = String.valueOf(groups.get(i).getID());
        }
		
		return groupIDs;
	}

	/**
	 * Add the given id to the list and return a new list.
	 * 
	 * @param list The current array
	 * @param id The new element
	 * @return A new combined array.
	 */
	public static String[] addMember(String[] list, String id)
	{
		// FIXME: this is terribly inefficient.
		List<String> newList = new ArrayList<String>(Arrays.asList(list));
		newList.add(id);
		return newList.toArray(new String[newList.size()]);
	}
	
	/**
	 * Remove all instances of the given id from the member list.
	 * 
	 * @param list The current array
	 * @param id The id to remove
	 * @return A new combined array.
	 */
	public static String[] removeMember(String[] list, String id)
	{
		// FIXME: this is terribly inefficient.
		List<String> newList = new ArrayList<String>(Arrays.asList(list));
		newList.remove(id);
		return newList.toArray(new String[newList.size()]);
	}
	
	/**
	 * Save the group. If the name has been changed then it will be updated, if any 
	 * members have been added or removed then they are updated. 
	 * 
	 * If the groupID is -1 then a new group is created.
	 * 
	 * @param context The current dspace context
	 * @param groupID The group id, or -1 for a new group.
	 * @param newName The group's new name.
	 * @param newEPeopleIDsArray All epeople members
	 * @param newGroupIDsArray All group members.
	 * @return A result
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException on bad encoding.
	 */
	public static FlowResult processSaveGroup(Context context, UUID groupID, String newName, String[] newEPeopleIDsArray, String[] newGroupIDsArray)
            throws SQLException, AuthorizeException, UIException
	{
		FlowResult result = new FlowResult();
		
		// Decode the name in case it uses non-ascii characters.
		try
        {
            newName = URLDecoder.decode(newName, Constants.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new UIException(uee);
        }
		
		Group group = null;
		if (groupID == null)
		{
			// First, check if the name is blank.
			if (newName == null || newName.length() == 0)
			{
				// Group's can not have blank names.
				result.setContinue(false);
				result.addError("group_name");
				result.setOutcome(false);
				result.setMessage(new Message("default","The group name may not be blank."));
				
				return result;
			}
			
			// Create a new group, check if the newName is already in use.
			Group potentialDuplicate = groupService.findByName(context,newName);
			
			if (potentialDuplicate == null)
	    	{
				// All good, create the new group.
				group = groupService.create(context);
				groupService.setName(group, newName);
	    	}
			else
			{
				// The name is already in use, return an error.
    			result.setContinue(false);
    			result.addError("group_name");
    			result.addError("group_name_duplicate");
    			result.setOutcome(false);
    			result.setMessage(new Message("default","The group name is already in use"));
    			
    			return result;
			}
		}
		else
		{
			group = groupService.find(context,groupID);
			String name = group.getName();
			
			// Only update the name if there has been a change.
			if (newName != null && newName.length() > 0 && !name.equals(newName))
			{
				// The group name is to be updated, check if the newName is already in use.
				Group potentialDuplicate = groupService.findByName(context,newName);
				
				if (potentialDuplicate == null)
		    	{
					// All good, update the name
					groupService.setName(group, newName);
		    	}
				else
				{
					// The name is already in use, return an error.
	    			result.setContinue(false);
	    			result.addError("group_name");
	    			result.addError("group_name_duplicate");
	    			result.setOutcome(false);
	    			result.setMessage(new Message("default","The group name is already in use"));
	    			
	    			return result;
				}
			}
		}
		
		// Second, prepare to check members by turning arrays into lists
		List<UUID> newEPeopleIDs = new ArrayList<>();
		for (String epeopleID : newEPeopleIDsArray)
        {
			newEPeopleIDs.add(UUID.fromString(epeopleID));
        }
		List<UUID> newGroupIDs = new ArrayList<>();
		for (String _groupID : newGroupIDsArray)
        {
			newGroupIDs.add(UUID.fromString(_groupID));
        }
		
		
		// Third, check if there are any members to remove
		// i.e. scan the list on the group against the ids.
		for (Iterator<EPerson> it = group.getMembers().iterator(); it.hasNext(); )
		{
            EPerson epersonMember = it.next();
			if (!newEPeopleIDs.contains(epersonMember.getID()))
			{
				// The current eperson is not contained in the new list.
                it.remove();
				groupService.removeMember(context, group, epersonMember);
			}
			else
			{
				// If they are still in the list then remove them
				// from the list of people to add.
				newEPeopleIDs.remove((Object)epersonMember.getID());
			}
		}
		for (Iterator<Group> it = group.getMemberGroups().iterator(); it.hasNext(); )
		{
            Group groupMember = it.next();
			if (!newGroupIDs.contains(groupMember.getID()))
			{
				// The current group is not contained in the new list.
                it.remove();
				groupService.removeMember(context, group, groupMember);
			}
			else
			{
				// If they are still in the list then remove them
				// from the list of groups to add.
				newGroupIDs.remove((Object)group.getID());
			}
		}
		
		// Third, check if there are any members to add
		// i.e. scan the list of ids against the group.
		for (UUID epersonID : newEPeopleIDs)
		{
			EPerson eperson = ePersonService.find(context, epersonID);
			
			groupService.addMember(context, group, eperson);
		}
		
		for (UUID _groupID : newGroupIDs)
		{
			Group _group = groupService.find(context, _groupID);
			
			groupService.addMember(context, group, _group);
		}
		
		// Last, create the result flow
		groupService.update(context, group);

		// Let's record our group id in case we created a new one.
		result.setParameter("groupID", group.getID());
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(T_edit_group_success_notice);
		
		return result;
	}
	
	/**
	 * Remove the specified groups. It is assumed that the user has already confirmed this selection.
	 * 
	 * @param context The current DSpace context
	 * @param groupIDs A list of groups to be removed.
	 * @return A results object.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
	 */
	public static FlowResult processDeleteGroups(Context context, String[] groupIDs) throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(true);
		
    	for (String id : groupIDs) 
    	{
    		Group groupDeleted = groupService.find(context, UUID.fromString(id));
    		
    		// If this group is related to a collection, then un-link it.
    		UUID collectionId = getCollectionId(context, groupDeleted.getName());
    		Role role = getCollectionRole(groupDeleted.getName());
    		if (collectionId != null && role != Role.none)
    		{
	    		Collection collection = collectionService.find(context, collectionId);
	    		
	    		if (collection != null)
	    		{
		    		if (role == Role.Administrators)
		    		{
						collectionService.removeAdministrators(context, collection);
						collectionService.update(context, collection);
		    		} 
		    		else if (role == Role.Submitters)
		    		{
						collectionService.removeSubmitters(context, collection);
						collectionService.update(context, collection);
		    		}
		    		else if (role == Role.WorkflowStep1)
		    		{
		    			collection.setWorkflowGroup(1, null);
						collectionService.update(context, collection);
		    		}
		    		else if (role == Role.WorkflowStep2)
		    		{
		    			collection.setWorkflowGroup(2, null);
						collectionService.update(context, collection);
		    		}
		    		else if (role == Role.WorkflowStep3)
		    		{
		    			collection.setWorkflowGroup(3, null);
						collectionService.update(context, collection);
		    		}
		    		else if (role == Role.DefaultRead)
		    		{
		    			// Nothing special needs to happen.
		    		}
	    		}
    		}

			groupService.delete(context, groupDeleted);
	    }
    	
    	result.setOutcome(true);
		result.setMessage(T_delete_group_success_notice);
    	
    	return result;
	}
	
	/**
	 * The collection prefix, all groups which are specific to
	 * a collection start with this.
	 */
	private static final String COLLECTION_PREFIX = "COLLECTION_";
	
	/**
	 * These are the possible collection suffixes, all groups which are
	 * specific to a collection will end with one of these. The collection
	 * id should be in between the prefix and the suffix.
	 * 
	 * Note: the order of these suffixes are important, see getCollectionRole()
	 */
	private static final String[] COLLECTION_SUFFIXES = {"_SUBMIT","_ADMIN","_WFSTEP_1","_WORKFLOW_STEP_1","_WFSTEP_2","_WORKFLOW_STEP_2","_WFSTEP_3","_WORKFLOW_STEP_3","_DEFAULT_ITEM_READ"};

	/**
	 * Extracts the collection id that may be embedded in the given group name.
	 *
     * @param context session context.
	 * @param groupName - the name of a group (ie group.getName())
	 * @return the integer collection id or -1 if the group is not that of a collection
	 */
	public static UUID getCollectionId(Context context, String groupName)
	{
		if (groupName != null && groupName.startsWith(COLLECTION_PREFIX))
		{
			for (String suffix : COLLECTION_SUFFIXES)
			{
				if (groupName.endsWith(suffix))
				{
					String idString = groupName.substring(COLLECTION_PREFIX.length());
					idString = idString.substring(0, idString.length() - suffix.length());

					try {
						Collection collection = collectionService.findByIdOrLegacyId(context, idString);
						if(collection != null)
						{
							return collection.getID();
						}else{
							return null;
						}
					}
					catch (Exception nfe)
					{
						// Something went wrong, just ignore the exception and
						// continue searching for a collection id
					} // try & catch
				} // if it ends with a proper suffix.
			} // for each possible suffix
		} // if it starts with COLLECTION_
		
		return null;
    }
	
	public enum Role {Administrators, Submitters, WorkflowStep1, WorkflowStep2, WorkflowStep3, DefaultRead, none};
	
	public static Role getCollectionRole(String groupName)
	{
		if (groupName != null && groupName.startsWith(COLLECTION_PREFIX))
		{
			for (String suffix : COLLECTION_SUFFIXES)
			{
				if (groupName.endsWith(suffix))
				{
					if (COLLECTION_SUFFIXES[0].equals(suffix))
                    {
                        return Role.Submitters;
                    }
					else if (COLLECTION_SUFFIXES[1].equals(suffix))
                    {
                        return Role.Administrators;
                    }
					else if (COLLECTION_SUFFIXES[2].equals(suffix))
                    {
                        return Role.WorkflowStep1;
                    }
					else if (COLLECTION_SUFFIXES[3].equals(suffix))
                    {
                        return Role.WorkflowStep1;
                    }
					else if (COLLECTION_SUFFIXES[4].equals(suffix))
                    {
                        return Role.WorkflowStep2;
                    }
					else if (COLLECTION_SUFFIXES[5].equals(suffix))
                    {
                        return Role.WorkflowStep2;
                    }
					else if (COLLECTION_SUFFIXES[6].equals(suffix))
                    {
                        return Role.WorkflowStep3;
                    }
					else if (COLLECTION_SUFFIXES[7].equals(suffix))
                    {
                        return Role.WorkflowStep3;
                    }
					else if (COLLECTION_SUFFIXES[8].equals(suffix))
                    {
                        return Role.DefaultRead;
                    }
					
				} // if it ends with a proper suffix.
			} // for each possible suffix
		} // if it starts with COLLECTION_
		
		return Role.none;
	}

    /**
     * The community prefix: all groups which are specific to
     * a community start with this.
     */
    private static final String COMMUNITY_PREFIX = "COMMUNITY_";
    
    /**
     * These are the possible community suffixes. All groups which are
     * specific to a collection will end with one of these. The collection
     * id should be between the prefix and the suffix.
     * 
     * Note: the order of these suffixes are important, see getCollectionRole()
     */
    private static final String[] COMMUNITY_SUFFIXES = {"_ADMIN"};
    
    
    /**
     * Extracts the community id that may be embedded in the given group name.
     * 
     * @param context session context.
     * @param groupName the name of a group (ie group.getName())
     * @return the integer community id or -1 if the group is not that of a community
     */
    public static UUID getCommunityId(Context context, String groupName)
    {
        if (groupName != null && groupName.startsWith(COMMUNITY_PREFIX))
        {
            for (String suffix : COMMUNITY_SUFFIXES)
            {
                if (groupName.endsWith(suffix))
                {
                    String idString = groupName.substring(COMMUNITY_PREFIX.length());
                    idString = idString.substring(0, idString.length() - suffix.length());

					try {
						Community community = communityService.findByIdOrLegacyId(context, idString);
						if(community != null)
						{
							return community.getID();
						}else{
							return null;
						}
					}
					catch (Exception nfe)
					{
						// Something went wrong, just ignore the exception and
						// continue searching for a collection id
					} // try & catch
                } // if it ends with a proper suffix.
            } // for each possible suffix
        } // if it starts with COLLECTION_
        
        return null;
    }
    
    public static Role getCommunityRole(String groupName)
    {
        if (groupName != null && groupName.startsWith(COMMUNITY_PREFIX))
        {
            for (String suffix : COMMUNITY_SUFFIXES)
            {
                if (groupName.endsWith(suffix))
                {
                    if (COLLECTION_SUFFIXES[0].equals(suffix))
                    {
                        return Role.Submitters;
                    }
                    else if (COLLECTION_SUFFIXES[1].equals(suffix))
                    {
                        return Role.Administrators;
                    }
                    else if (COLLECTION_SUFFIXES[2].equals(suffix))
                    {
                        return Role.WorkflowStep1;
                    }
                    else if (COLLECTION_SUFFIXES[3].equals(suffix))
                    {
                        return Role.WorkflowStep1;
                    }
                    else if (COLLECTION_SUFFIXES[4].equals(suffix))
                    {
                        return Role.WorkflowStep2;
                    }
                    else if (COLLECTION_SUFFIXES[5].equals(suffix))
                    {
                        return Role.WorkflowStep2;
                    }
                    else if (COLLECTION_SUFFIXES[6].equals(suffix))
                    {
                        return Role.WorkflowStep3;
                    }
                    else if (COLLECTION_SUFFIXES[7].equals(suffix))
                    {
                        return Role.WorkflowStep3;
                    }
                    else if (COLLECTION_SUFFIXES[8].equals(suffix))
                    {
                        return Role.DefaultRead;
                    }
                    
                } // if it ends with a proper suffix.
            } // for each possible suffix
        } // if it starts with COMMUNITY_
        
        return Role.none;
    }

}
