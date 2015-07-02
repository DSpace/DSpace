/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 *
 */

package edu.umd.lib.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dspace.app.xmlui.aspect.administrative.FlowResult;
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

        Unit unit = Unit.find(context, unitID);

        if (unit == null)
        {
            return "New Unit";
        }

        return unit.getName();
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
    public static String[] getUnitMembers(Context context, int unitID)
            throws SQLException
    {
        if (unitID < 0)
        {
            return new String[0];
        }

        Unit unit = Unit.find(context, unitID);

        if (unit == null)
        {
            return new String[0];
        }

        Group[] groups = unit.getGroups();

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
}
