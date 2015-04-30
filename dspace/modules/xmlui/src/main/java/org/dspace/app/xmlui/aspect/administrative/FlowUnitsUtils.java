/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

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
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;

/**
 * Utility methods to processes actions on Units. These methods are used
 * exclusively from the administrative flow scripts.
 *
 * @author Scott Phillips
 */
public class FlowUnitsUtils
{

    /** Language Strings */
    private static final Message T_edit_unit_success_notice = new Message(
            "default",
            "xmlui.administrative.FlowUnitsUtils.edit_unit_success_notice");

    private static final Message T_delete_unit_success_notice = new Message(
            "default",
            "xmlui.administrative.FlowUnitsUtils.delete_unit_success_notice");

    /**
     * Return the current name for the given group ID.
     * 
     * @param context
     *            The current DSpace context.
     * @param groupID
     *            The group id.
     * @return The group's name.
     */
    public static String getName(Context context, int unitID)
            throws SQLException
    {
        if (unitID < 0)
        {
            return "New Unit";
        }

        Group group = Group.find(context, unitID);

        if (group == null)
        {
            return "New Unit";
        }

        return group.getName();
    }

    /**
     * Return the list of current group id's that are a member of this unit.
     * 
     * @param context
     *            The current DSpace context
     * @param groupID
     *            The group's id.
     * @return An array of ids.
     */
    public static String[] getGroupMembers(Context context, int groupID)
            throws SQLException
    {
        if (groupID < 0)
        {
            return new String[0];
        }

        Group group = Group.find(context, groupID);

        if (group == null)
        {
            return new String[0];
        }

        Group[] groups = group.getMemberGroups();

        String[] groupIDs = new String[groups.length];
        for (int i = 0; i < groups.length; i++)
        {
            groupIDs[i] = String.valueOf(groups[i].getID());
        }

        return groupIDs;
    }

    /**
     * Add the given id to the list and return a new list.
     * 
     * @param list
     *            The current array
     * @param id
     *            The new element
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
     * @param list
     *            The current array
     * @param id
     *            The id to remove
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
     * Save the group. If the name has been changed then it will be updated, if
     * any members have been added or removed then they are updated.
     * 
     * If the groupID is -1 then a new group is created.
     * 
     * @param context
     *            The current dspace context
     * @param groupID
     *            The group id, or -1 for a new group.
     * @param newName
     *            The group's new name.
     * @param newEPeopleIDsArray
     *            All epeople members
     * @param newGroupIDsArray
     *            All group members.
     * @return A result
     */
    public static FlowResult processSaveUnits(Context context, int unitID,
            String newName, String[] newGroupIDsArray) throws SQLException,
            AuthorizeException, UIException
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

        Unit unit = null;
        if (unitID == -1)
        {
            // First, check if the name is blank.
            if (newName == null || newName.length() == 0)
            {
                // Unit's can not have blank names.
                result.setContinue(false);
                result.addError("unit_name");
                result.setOutcome(false);
                result.setMessage(new Message("default",
                        "The unit name may not be blank."));

                return result;
            }

            // Create a new unit, check if the newName is already in use.
            Unit potentialDuplicate = Unit.findByName(context, newName);

            if (potentialDuplicate == null)
            {
                // All good, create the new unit.
                unit = Unit.create(context);
                unit.setName(newName);
            }
            else
            {
                // The name is already in use, return an error.
                result.setContinue(false);
                result.addError("unit_name");
                result.addError("unit_name_duplicate");
                result.setOutcome(false);
                result.setMessage(new Message("default",
                        "The unit name is already in use"));

                return result;
            }
        }
        else
        {
            unit = Unit.find(context, unitID);
            String name = unit.getName();

            // Only update the name if there has been a change.
            if (newName != null && newName.length() > 0
                    && !name.equals(newName))
            {
                // The unit name is to be updated, check if the newName is
                // already in use.
                Unit potentialDuplicate = Unit.findByName(context, newName);

                if (potentialDuplicate == null)
                {
                    // All good, update the name
                    unit.setName(newName);
                }
                else
                {
                    // The name is already in use, return an error.
                    result.setContinue(false);
                    result.addError("unit_name");
                    result.addError("unit_name_duplicate");
                    result.setOutcome(false);
                    result.setMessage(new Message("default",
                            "The unit name is already in use"));

                    return result;
                }
            }
        }

        // Second, prepare to check members by turning arrays into lists
        List<Integer> newGroupIDs = new ArrayList<Integer>();
        for (String _groupID : newGroupIDsArray)
        {
            newGroupIDs.add(Integer.valueOf(_groupID));
        }

        // Third, check if there are any members to remove
        // i.e. scan the list on the unit against the ids.
        for (Group groupMember : unit.getGroups())
        {
            if (!newGroupIDs.contains(groupMember.getID()))
            {
                // The current group is not contained in the new list.
                unit.removeGroup(groupMember);
            }
            else
            {
                // If they are still in the list then remove them
                // from the list of collections to add.
                newGroupIDs.remove((Object) groupMember.getID());
            }
        }

        // Fourth, check if there are any members to add
        // i.e. scan the list of ids against the unit.

        for (Integer groupID : newGroupIDs)
        {
            Group group = Group.find(context, groupID);

            unit.addGroup(group);
        }

        // Last, create the result flow
        unit.update();
        context.commit();

        // Let's record our group id in case we created a new one.
        result.setParameter("unitID", unit.getID());
        result.setContinue(true);
        result.setOutcome(true);
        result.setMessage(T_edit_unit_success_notice);

        return result;
    }

    /**
     * Remove the specified units. It is assumed that the user has already
     * confirmed this selection.
     * 
     * @param context
     *            The current DSpace context
     * @param unitIDs
     *            A list of units to be removed.
     * @return A results object.
     */
    public static FlowResult processDeleteUnits(Context context,
            String[] unitIDs) throws SQLException, AuthorizeException,
            IOException
    {
        FlowResult result = new FlowResult();
        result.setContinue(true);

        for (String id : unitIDs)
        {
            Unit unitDeleted = Unit.find(context, Integer.valueOf(id));
            unitDeleted.delete();
        }

        result.setOutcome(true);
        result.setMessage(T_delete_unit_success_notice);

        return result;
    }

    /**
     * The collection prefix, all groups which are specific to a collection
     * start with this.
     */
    private static final String COLLECTION_PREFIX = "COLLECTION_";

    /**
     * These are the possible collection suffixes, all groups which are specific
     * to a collection will end with one of these. The collection id should be
     * in between the prefix and the suffix.
     * 
     * Note: the order of these suffixes are important, see getCollectionRole()
     */
    private static final String[] COLLECTION_SUFFIXES = { "_SUBMIT", "_ADMIN",
            "_WFSTEP_1", "_WORKFLOW_STEP_1", "_WFSTEP_2", "_WORKFLOW_STEP_2",
            "_WFSTEP_3", "_WORKFLOW_STEP_3", "_DEFAULT_ITEM_READ" };

    /**
     * Extracts the collection id that may be immbedded in the given group name.
     * 
     * @param groupName
     *            - the name of a group (ie group.getName())
     * @return the integer collection id or -1 if the group is not that of a
     *         collection
     */
    public static int getCollectionId(String groupName)
    {
        if (groupName != null && groupName.startsWith(COLLECTION_PREFIX))
        {
            for (String suffix : COLLECTION_SUFFIXES)
            {
                if (groupName.endsWith(suffix))
                {
                    String idString = groupName.substring(COLLECTION_PREFIX
                            .length());
                    idString = idString.substring(0,
                            idString.length() - suffix.length());

                    int collectionID = -1;
                    try
                    {
                        collectionID = Integer.valueOf(idString);

                        return collectionID;
                        // All good, we were able to ah
                    }
                    catch (NumberFormatException nfe)
                    {
                        // Something went wrong, just ignore the exception and
                        // continue searching for a collection id
                    } // try & catch
                } // if it ends with a proper suffix.
            } // for each possible suffix
        } // if it starts with COLLECTION_

        return -1;
    }

    public enum Role {
        Administrators, Submitters, WorkflowStep1, WorkflowStep2, WorkflowStep3, DefaultRead, none
    };

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
     * The community prefix: all groups which are specific to a community start
     * with this.
     */
    private static final String COMMUNITY_PREFIX = "COMMUNITY_";

    /**
     * These are the possible community suffixes. All groups which are specific
     * to a collection will end with one of these. The collection id should be
     * between the prefix and the suffix.
     *
     * Note: the order of these suffixes are important, see getCollectionRole()
     */
    private static final String[] COMMUNITY_SUFFIXES = { "_ADMIN" };

    /**
     * Extracts the community id that may be embedded in the given group name.
     *
     * @param groupName
     *            - the name of a group (ie group.getName())
     * @return the integer community id or -1 if the group is not that of a
     *         community
     */
    public static int getCommunityId(String groupName)
    {
        if (groupName != null && groupName.startsWith(COMMUNITY_PREFIX))
        {
            for (String suffix : COMMUNITY_SUFFIXES)
            {
                if (groupName.endsWith(suffix))
                {
                    String idString = groupName.substring(COMMUNITY_PREFIX
                            .length());
                    idString = idString.substring(0,
                            idString.length() - suffix.length());

                    int communityID = -1;
                    try
                    {
                        communityID = Integer.valueOf(idString);

                        return communityID;
                    }
                    catch (NumberFormatException nfe)
                    {
                    }
                } // if it ends with a proper suffix.
            } // for each possible suffix
        } // if it starts with COLLECTION_

        return -1;
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
