/*
 * FlowGroupUtils.java
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Utility methods to processes actions on Groups. These methods are used
 * exclusivly from the administrative flow scripts.
 * 
 * @author scott phillips
 */
public class FlowGroupUtils {

	/** Language Strings */
	private static final Message T_edit_group_success_notice =
		new Message("default","xmlui.administrative.FlowGroupUtils.edit_group_success_notice");
	
	private static final Message T_delete_group_success_notice =
		new Message("default","xmlui.administrative.FlowGroupUtils.delete_group_success_notice");

	
	/**
	 * Return the current name for the given group ID.
	 * @param context The current DSpace context.
	 * @param groupID The group id.
	 * @return The group's name.
	 */
	public static String getName(Context context, int groupID) throws SQLException
	{
		if (groupID < 0)
			return "New Group";	
		
		Group group = Group.find(context,groupID);
		
		if (group == null)
			return "New Group";
		
		return group.getName();
	}
	
	/**
	 * Return the list of current epeople ID's that are a member of this group.
	 * 
	 * @param context The current DSpace context
	 * @param groupID The group's id.
	 * @return An array of ids.
	 */
	public static String[] getEPeopleMembers(Context context, int groupID) throws SQLException
	{
		// New group, just return an empty list
		if (groupID < 0)
			return new String[0];
		
		Group group = Group.find(context,groupID);
		
		if (group == null)
			return new String[0];
		
		EPerson[] epeople = group.getMembers();
		
		String[] epeopleIDs = new String[epeople.length];
		for (int i=0; i < epeople.length; i++)
			epeopleIDs[i] = String.valueOf(epeople[i].getID());
		
		return epeopleIDs;
	}
	
	/**
	 * Return the list of current group id's that are a member of this group.
	 * 
	 * @param context The current DSpace context
	 * @param groupID The group's id.
	 * @return An array of ids.
	 */
	public static String[] getGroupMembers(Context context, int groupID) throws SQLException
	{
		if (groupID < 0)
			return new String[0];
		
		Group group = Group.find(context,groupID);
		
		if (group == null)
			return new String[0];
		
		Group[] groups = group.getMemberGroups();
		
		String[] groupIDs = new String[groups.length];
		for (int i=0; i < groups.length; i++)
			groupIDs[i] = String.valueOf(groups[i].getID());
		
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
		// FIXME: this is terribly ineffecient.
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
		// FIXME: this is terribly ineffecient.
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
	 */
	public static FlowResult processSaveGroup(Context context, int groupID, String newName, String[] newEPeopleIDsArray, String[] newGroupIDsArray) throws SQLException, AuthorizeException, UIException
	{
		FlowResult result = new FlowResult();
		
		// Decode the name incase it uses non-ascii characters.
		try
        {
            newName = URLDecoder.decode(newName, Constants.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new UIException(uee);
        }
		
		Group group = null;
		if (groupID == -1)
		{
			// First check if the name is blank.
			if (newName == null || newName.length() == 0)
			{
				// Group's can not have blank names.
				result.setContinue(false);
				result.addError("group_name");
				result.setOutcome(false);
				result.setMessage(new Message("default","The group name may not be blank."));
				
				return result;
			}
			
			// Create a new group, check if the newName is allready in use.
			Group potentialDuplicate = Group.findByName(context,newName);
			
			if (potentialDuplicate == null)
	    	{
				// All good, create the new group.
				group = Group.create(context);
				group.setName(newName);
	    	}
			else
			{
				// The name is allready in use, return in error.
    			result.setContinue(false);
    			result.addError("group_name");
    			result.addError("group_name_duplicate");
    			result.setOutcome(false);
    			result.setMessage(new Message("default","The group name is allready in use"));
    			
    			return result;
			}
		}
		else
		{
			group = Group.find(context,groupID);
			String name = group.getName();
			
			// Only update the name if there has been a change.
			if (newName != null && newName.length() > 0 && !name.equals(newName))
			{
				// The group name is to be updated, check if the newName is allready in use.
				Group potentialDuplicate = Group.findByName(context,newName);
				
				if (potentialDuplicate == null)
		    	{
					// All good, update the name
					group.setName(newName);
		    	}
				else
				{
					// The name is allready in use, return in error.
	    			result.setContinue(false);
	    			result.addError("group_name");
	    			result.addError("group_name_duplicate");
	    			result.setOutcome(false);
	    			result.setMessage(new Message("default","The group name is allready in use"));
	    			
	    			return result;
				}
			}
		}
		
		// Second, Prepare to check members by turning arrays into lists
		List<Integer> newEPeopleIDs = new ArrayList<Integer>();
		for (String epeopleID : newEPeopleIDsArray)
			newEPeopleIDs.add(Integer.valueOf(epeopleID));
		List<Integer> newGroupIDs = new ArrayList<Integer>();
		for (String _groupID : newGroupIDsArray)
			newGroupIDs.add(Integer.valueOf(_groupID));
		
		
		// Third, check if there are any members to remove
		// i.e. scan the list on the group against the ids.
		for (EPerson epersonMember : group.getMembers())
		{
			if (!newEPeopleIDs.contains(epersonMember.getID()))
			{
				// The current eperson is not contained in the new list.
				group.removeMember(epersonMember);
			}
			else
			{
				// If they are still in the list then remove them
				// from the list of people to add.
				newEPeopleIDs.remove((Object)epersonMember.getID());
			}
		}
		for (Group groupMember : group.getMemberGroups())
		{
			if (!newGroupIDs.contains(groupMember.getID()))
			{
				// The current group is not contained in the new list.
				group.removeMember(groupMember);
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
		for (Integer epersonID : newEPeopleIDs)
		{
			EPerson eperson = EPerson.find(context, epersonID);
			
			group.addMember(eperson);
		}
		
		for (Integer _groupID : newGroupIDs)
		{
			Group _group = Group.find(context, _groupID);
			
			group.addMember(_group);
		}
		
		// Last, create the result flow
		group.update();
		context.commit();
		
		// Let's record our group id incase we created a new one.
		result.setParameter("groupID", group.getID());
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(T_edit_group_success_notice);
		
		return result;
	}
	
	/**
	 * Remove the specified groups. It is assumed that the user has allready confirm this selection.
	 * 
	 * @param context The current DSpace context
	 * @param groupIDs A list of groups to be removed.
	 * @return A results object.
	 */
	public static FlowResult processDeleteGroups(Context context, String[] groupIDs) throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(true);
		
    	for (String id : groupIDs) 
    	{
    		Group groupDeleted = Group.find(context, Integer.valueOf(id));
    		
    		// If this group is related to a collection, then un-link it.
    		int collectionId = getCollectionId(groupDeleted.getName());
    		Role role = getCollectionRole(groupDeleted.getName());
    		if (collectionId != -1 && role != Role.none)
    		{
	    		Collection collection = Collection.find(context, collectionId);
	    		
	    		if (collection != null)
	    		{
		    		if (role == Role.Administrators)
		    		{
		    			collection.removeAdministrators();
		    			collection.update();
		    		} 
		    		else if (role == Role.Submitters)
		    		{
		    			collection.removeSubmitters();
		    			collection.update();
		    		}
		    		else if (role == Role.WorkflowStep1)
		    		{
		    			collection.setWorkflowGroup(1, null);
		    			collection.update();
		    		}
		    		else if (role == Role.WorkflowStep2)
		    		{
		    			collection.setWorkflowGroup(2, null);
		    			collection.update();
		    		}
		    		else if (role == Role.WorkflowStep3)
		    		{
		    			collection.setWorkflowGroup(3, null);
		    			collection.update();
		    		}
		    		else if (role == Role.DefaultRead)
		    		{
		    			// Nothing special needs to happen.
		    		}
	    		}
    		}

			groupDeleted.delete();
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
	 * id should be inbetween the prefix and the suffix.
	 * 
	 * Note: the order of these suffixes are important, see getCollectionRole()
	 */
	private static final String[] COLLECTION_SUFFIXES = {"_SUBMIT","_ADMIN","_WFSTEP_1","_WORKFLOW_STEP_1","_WFSTEP_2","_WORKFLOW_STEP_2","_WFSTEP_3","_WORKFLOW_STEP_3","_DEFAULT_ITEM_READ"};
	
	
	/**
	 * Extracts the collection id that may be immbedded in the given group name.
	 * 
	 * @param groupName - the name of a group (ie group.getName())
	 * @return the integer collection id or -1 if the group is not that of a collection
	 */
	public static int getCollectionId(String groupName)
	{
		if (groupName != null && groupName.startsWith(COLLECTION_PREFIX))
		{
			for (String suffix : COLLECTION_SUFFIXES)
			{
				if (groupName.endsWith(suffix))
				{
					String idString = groupName.substring(COLLECTION_PREFIX.length());
					idString = idString.substring(0, idString.length() - suffix.length());

					int collectionID = -1;
					try {
						collectionID = Integer.valueOf(idString); 
						
						return collectionID;
						// All good, we were able to ah 
					}
					catch (NumberFormatException nfe)
					{
						// Somethnig went wrong, just ignore the exception and
						// continue searching for a collection id
					} // try & catch
				} // if it ends with a proper suffix.
			} // for each possible suffix
		} // if it starts with COLLECTION_
		
		return -1;
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
						return Role.Submitters;
					else if (COLLECTION_SUFFIXES[1].equals(suffix))
						return Role.Administrators;
					else if (COLLECTION_SUFFIXES[2].equals(suffix))
						return Role.WorkflowStep1;
					else if (COLLECTION_SUFFIXES[3].equals(suffix))
						return Role.WorkflowStep1;
					else if (COLLECTION_SUFFIXES[4].equals(suffix))
						return Role.WorkflowStep2;
					else if (COLLECTION_SUFFIXES[5].equals(suffix))
						return Role.WorkflowStep2;
					else if (COLLECTION_SUFFIXES[6].equals(suffix))
						return Role.WorkflowStep3;
					else if (COLLECTION_SUFFIXES[7].equals(suffix))
						return Role.WorkflowStep3;
					else if (COLLECTION_SUFFIXES[8].equals(suffix))
						return Role.DefaultRead;
					
				} // if it ends with a proper suffix.
			} // for each possible suffix
		} // if it starts with COLLECTION_
		
		return Role.none;
	}
	
	
	
}
