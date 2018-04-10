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
import java.util.UUID;

import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.EtdUnit;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EtdUnitService;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * Utility methods to processes actions on ETDDepartments. These methods are
 * used exclusively from the administrative flow scripts.
 *
 * @author Scott Phillips
 */
public class FlowETDDepartmentUtils
{

    /** Language Strings */
    private static final Message T_edit_etd_department_success_notice = new Message(
            "default",
            "xmlui.administrative.FlowETDDepartmentUtils.edit_etd_department_success_notice");

    private static final Message T_delete_etd_department_success_notice = new Message(
            "default",
            "xmlui.administrative.FlowETDDepartmentUtils.delete_etd_department_success_notice");
    
    private static EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();
    
    private static CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    /**
     * Return the current name for the given etd_department ID.
     *
     * @param context
     *            The current DSpace context.
     * @param etd_departmentID
     *            The etd_department id.
     * @return The etd_department's name.
     */
    public static String getName(Context context, UUID etd_departmentID)
            throws SQLException
    {
        if (etd_departmentID == null)
        {
            return "New ETD Department";
        }

        EtdUnit etd_department = etdunitService.find(context, etd_departmentID);

        if (etd_department == null)
        {
            return "New ETD Department";
        }

        return etd_department.getName();
    }

    /**
     * Return the list of current collection ID's that are a member of this
     * etd_department.
     *
     * @param context
     *            The current DSpace context
     * @param etd_departmentID
     *            The etd_department's id.
     * @return An array of ids.
     */
    public static String[] getCollectionMembers(Context context,
            UUID etd_departmentID) throws SQLException
    {
        // New etd_department, just return an empty list
        if (etd_departmentID != null)
        {
            return new String[0];
        }

        EtdUnit etd_department = etdunitService.find(context,etd_departmentID);

        if (etd_department == null)
        {
            return new String[0];
        }

        List<Collection> collections = etd_department.getCollections();

        String[] collectionIDs = new String[collections.size()];
        for (int i = 0; i < collections.size(); i++)
        {
            collectionIDs[i] = String.valueOf(collections.get(0).getID());
        }

        return collectionIDs;
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
     * Save the etd_department. If the name has been changed then it will be
     * updated, if any members have been added or removed then they are updated.
     *
     * If the etd_departmentID is -1 then a new etd_department is created.
     *
     * @param context
     *            The current dspace context
     * @param etd_departmentID
     *            The etd_department id, or -1 for a new etd_department.
     * @param newName
     *            The etd_department's new name.
     * @param newCollectionIDsArray
     *            All collection members
     * @return A result
     */
    public static FlowResult processSaveETDDepartment(Context context,
            UUID etd_departmentID, String newName, String[] newCollectionIDsArray)
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

        EtdUnit etd_department = null;
        if (etd_departmentID == null)
        {
            // First, check if the name is blank.
            if (newName == null || newName.length() == 0)
            {
                // Group's can not have blank names.
                result.setContinue(false);
                result.addError("etd_department_name");
                result.setOutcome(false);
                result.setMessage(new Message("default",
                        "The ETD Department name may not be blank."));

                return result;
            }

            // Create a new etd_department, check if the newName is already in
            // use.
            EtdUnit potentialDuplicate = etdunitService.findByName(context, newName);

            if (potentialDuplicate == null)
            {
                // All good, create the new etd_department.
                etd_department = etdunitService.create(context);
                etd_department.setName(newName);
            }
            else
            {
                // The name is already in use, return an error.
                result.setContinue(false);
                result.addError("etd_department_name");
                result.addError("etd_department_name_duplicate");
                result.setOutcome(false);
                result.setMessage(new Message("default",
                        "The ETD Department name is already in use"));

                return result;
            }
        }
        else
        {
            etd_department = etdunitService.find(context, etd_departmentID);
            String name = etd_department.getName();

            // Only update the name if there has been a change.
            if (newName != null && newName.length() > 0
                    && !name.equals(newName))
            {
                // The etd_department name is to be updated, check if the
                // newName is already in use.
                EtdUnit potentialDuplicate = etdunitService.findByName(context,
                        newName);

                if (potentialDuplicate == null)
                {
                    // All good, update the name
                    etd_department.setName(newName);
                }
                else
                {
                    // The name is already in use, return an error.
                    result.setContinue(false);
                    result.addError("etd_department_name");
                    result.addError("etd_department_name_duplicate");
                    result.setOutcome(false);
                    result.setMessage(new Message("default",
                            "The ETD Department name is already in use"));

                    return result;
                }
            }
        }

        // Second, prepare to check members by turning arrays into lists
        List<String> newCollectionIDs = new ArrayList<String>();
        for (String collectionID : newCollectionIDsArray)
        {
            newCollectionIDs.add(collectionID);
        }

        // Third, check if there are any members to remove
        // i.e. scan the list on the etd_department against the ids.
        for (Collection collectionMember : etd_department.getCollections())
        {
            if (!newCollectionIDs.contains(collectionMember.getID()))
            {
                // The current collection is not contained in the new list.
                etdunitService.removeCollection(context, etd_department, collectionMember);
            }
            else
            {
                // If they are still in the list then remove them
                // from the list of collections to add.
                newCollectionIDs.remove((Object) collectionMember.getID());
            }
        }

        // Fourth, check if there are any members to add
        // i.e. scan the list of ids against the etd_department.
        for (String collectionID : newCollectionIDs)
        {
            Collection collection = collectionService.find(context, UUID.fromString(collectionID));

            etdunitService.addCollection(context, etd_department, collection);
        }

        // Last, create the result flow
        etdunitService.update(context, etd_department);
        context.commit();

        // Let's record our etd_department id in case we created a new one.
        result.setParameter("etd_departmentID", etd_department.getID());
        result.setContinue(true);
        result.setOutcome(true);
        result.setMessage(T_edit_etd_department_success_notice);

        return result;
    }

    /**
     * Remove the specified etd_departments. It is assumed that the user has
     * already confirmed this selection.
     *
     * @param context
     *            The current DSpace context
     * @param etd_departmentIDs
     *            A list of etd_departments to be removed.
     * @return A results object.
     */
    public static FlowResult processDeleteETDDepartments(Context context,
            String[] etd_departmentIDs) throws SQLException,
            AuthorizeException, IOException
    {
        FlowResult result = new FlowResult();
        result.setContinue(true);

        for (String id : etd_departmentIDs)
        {
            EtdUnit etd_DepartmentDeleted = etdunitService.find(context, UUID.fromString(id));
            etdunitService.delete(context, etd_DepartmentDeleted);
        }

        result.setOutcome(true);
        result.setMessage(T_delete_etd_department_success_notice);

        return result;
    }
}
