/*
 * SupervisedItem.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
 */

package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
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
    
    /** log4j logger */
    private static Logger log = Logger.getLogger(SupervisedItem.class);
    
    /** The item this workspace object pertains to */
    private Item item;
    
    /** Our context */
    private Context ourContext;

    /** The table row corresponding to this workspace item */
    private TableRow siRow;

    /** The collection the item is being submitted to */
    private Collection collection;
    
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
        List sItems = new ArrayList();
        
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
        
        while (tri.hasNext())
        {
            TableRow row = tri.next();
            SupervisedItem si = new SupervisedItem(context, row);
            
            sItems.add(si);
        }
        
        tri.close();
        
        SupervisedItem[] siArray = new SupervisedItem[sItems.size()];
        siArray = (SupervisedItem[]) sItems.toArray(siArray);

        return siArray;
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
        List groupList = new ArrayList();
        String query = "SELECT epersongroup.* " +
                       "FROM epersongroup, epersongroup2workspaceitem " +
                       "WHERE epersongroup2workspaceitem.workspace_item_id" +
                       " = ? " +
                       " AND epersongroup2workspaceitem.eperson_group_id =" +
                       " epersongroup.eperson_group_id " +
                       "ORDER BY epersongroup.name";
        
        TableRowIterator tri = DatabaseManager.queryTable(c,"epersongroup",query, wi);
        
        while (tri.hasNext())
        {
            TableRow row = tri.next();
            Group group = Group.find(c,row.getIntColumn("eperson_group_id"));
            
            groupList.add(group);
        }
        
        tri.close();
        
        Group[] groupArray = new Group[groupList.size()];
        groupArray = (Group[]) groupList.toArray(groupArray);

        return groupArray;
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
        
        List groupList = new ArrayList();
        String query = "SELECT epersongroup.* " +
                       "FROM epersongroup, epersongroup2workspaceitem " +
                       "WHERE epersongroup2workspaceitem.workspace_item_id" +
                       " = ? " + 
                       " AND epersongroup2workspaceitem.eperson_group_id =" +
                       " epersongroup.eperson_group_id " +
                       "ORDER BY epersongroup.name";
        
        TableRowIterator tri = DatabaseManager.queryTable(ourContext,
                                    "epersongroup",
                                    query, this.getID());
        
        while (tri.hasNext())
        {
            TableRow row = tri.next();
            Group group = Group.find(ourContext,
                                row.getIntColumn("eperson_group_id"));
            
            groupList.add(group);
        }
        
        tri.close();
        
        Group[] groupArray = new Group[groupList.size()];
        groupArray = (Group[]) groupList.toArray(groupArray);

        return groupArray;
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
        List sItems = new ArrayList();
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
        
        while (tri.hasNext())
        {
            TableRow row = tri.next();
            SupervisedItem si = new SupervisedItem(context, row);
            sItems.add(si);
        }
        
        tri.close();
        
        SupervisedItem[] siArray = new SupervisedItem[sItems.size()];
        siArray = (SupervisedItem[]) sItems.toArray(siArray);

        return siArray;
        
    }
    
}
