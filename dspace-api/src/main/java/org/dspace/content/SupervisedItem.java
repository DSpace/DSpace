/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.storage.rdbms.TableRow;

/**
 * Class to handle WorkspaceItems which are being supervised.  It extends the
 * WorkspaceItem class and adds the methods required to be a Supervised Item.
 *
 * @author Richard Jones
 * @version  $Revision$
 */
public class SupervisedItem extends WorkspaceItem
{
    /**
     * Construct a supervised item out of the given row
     *
     * @param context  the context this object exists in
     * @param row      the database row
     */
    SupervisedItem(Context context, TableRow row)
        throws SQLException
    {
        // construct a new workspace item
        super(context, row);
    }

    /**
     * Get all workspace items which are being supervised
     *
     * @param context the context this object exists in
     *
     * @return array of SupervisedItems
     */
    public static SupervisedItem[] getAll(Context context)
        throws SQLException
    {
        List<SupervisedItem> sItems = new ArrayList<SupervisedItem>();
        
        // The following query pulls out distinct workspace items which have 
        // entries in the supervisory linking database.  We use DISTINCT to
        // prevent multiple instances of the item in the case that it is 
        // supervised by more than one group
        String query = "SELECT DISTINCT workspaceitem.* " +
                       "FROM workspaceitem, epersongroup2workspaceitem " +
                       "WHERE workspaceitem.workspace_item_id = " +
                       "epersongroup2workspaceitem.workspace_item_id " +
                       "ORDER BY workspaceitem.workspace_item_id";
        
        TableRowIterator tri = DatabaseManager.queryTable(context,
                                    "workspaceitem",
                                    query);

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();
                SupervisedItem si = new SupervisedItem(context, row);

                sItems.add(si);
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }
        
        return sItems.toArray(new SupervisedItem[sItems.size()]);
    }
    
    /**
     * Gets all the groups that are supervising a particular workspace item
     * 
     * @param c the context this object exists in
     * @param wi the ID of the workspace item
     *
     * @return the supervising groups in an array
     */
    public Group[] getSupervisorGroups(Context c, int wi)
        throws SQLException
    {
        List<Group> groupList = new ArrayList<Group>();
        String query = "SELECT epersongroup.* " +
                       "FROM epersongroup, epersongroup2workspaceitem " +
                       "WHERE epersongroup2workspaceitem.workspace_item_id" +
                       " = ? " +
                       " AND epersongroup2workspaceitem.eperson_group_id =" +
                       " epersongroup.eperson_group_id ";
        
        TableRowIterator tri = DatabaseManager.queryTable(c,"epersongroup",query, wi);

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();
                Group group = Group.find(c,row.getIntColumn("eperson_group_id"));

                groupList.add(group);
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }
        
        return groupList.toArray(new Group[groupList.size()]);
    }
    
    /**
     * Gets all the groups that are supervising a this workspace item
     * 
     *
     * @return the supervising groups in an array
     */
    // FIXME: We should arrange this code to use the above getSupervisorGroups 
    // method by building the relevant info before passing the request.
    public Group[] getSupervisorGroups()
        throws SQLException
    {
        Context ourContext = new Context();
        
        List<Group> groupList = new ArrayList<Group>();
        String query = "SELECT epersongroup.* " +
                       "FROM epersongroup, epersongroup2workspaceitem " +
                       "WHERE epersongroup2workspaceitem.workspace_item_id" +
                       " = ? " + 
                       " AND epersongroup2workspaceitem.eperson_group_id =" +
                       " epersongroup.eperson_group_id ";
        
        TableRowIterator tri = DatabaseManager.queryTable(ourContext,
                                    "epersongroup",
                                    query, this.getID());

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();
                Group group = Group.find(ourContext,
                                    row.getIntColumn("eperson_group_id"));

                groupList.add(group);
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }
        
        return groupList.toArray(new Group[groupList.size()]);
    }
    
    /**
     * Get items being supervised by given EPerson
     *
     * @param   ep          the eperson who's items to supervise we want
     * @param   context     the dspace context
     *
     * @return the items eperson is supervising in an array
     */
    public static SupervisedItem[] findbyEPerson(Context context, EPerson ep)
        throws SQLException
    {
        List<SupervisedItem> sItems = new ArrayList<SupervisedItem>();
        String query = "SELECT DISTINCT workspaceitem.* " +
                        "FROM workspaceitem, epersongroup2workspaceitem, " +
                        "epersongroup2eperson " +
                        "WHERE workspaceitem.workspace_item_id = " +
                        "epersongroup2workspaceitem.workspace_item_id " +
                        "AND epersongroup2workspaceitem.eperson_group_id =" +
                        " epersongroup2eperson.eperson_group_id " +
                        "AND epersongroup2eperson.eperson_id= ? " + 
                        " ORDER BY workspaceitem.workspace_item_id";

        TableRowIterator tri = DatabaseManager.queryTable(context,
                                    "workspaceitem",
                                    query,ep.getID());

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();
                SupervisedItem si = new SupervisedItem(context, row);
                sItems.add(si);
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }
        
        return sItems.toArray(new SupervisedItem[sItems.size()]);
    }
    
}
