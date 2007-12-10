/*
 * SupervisedItemDAOPostgres.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
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
package org.dspace.content.dao.postgres;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.content.SupervisedItem;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dao.SupervisedItemDAO;
import org.dspace.content.dao.WorkspaceItemDAOFactory;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class SupervisedItemDAOPostgres extends SupervisedItemDAO
{
    public SupervisedItemDAOPostgres(Context context)
    {
        super(context);
    }

    @Override
    public List<SupervisedItem> getSupervisedItems()
    {
        try
        {
            // The following query pulls out distinct workspace items which have 
            // entries in the supervisory linking database.  We use DISTINCT to
            // prevent multiple instances of the item in the case that it is 
            // supervised by more than one group
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "workspaceitem",
                    "SELECT DISTINCT wsi.workspace_item_id " +
                    "FROM workspaceitem wsi, epersongroup2workspaceitem eg2wsi " +
                    "WHERE wsi.workspace_item_id = eg2wsi.workspace_item_id " +
                    "ORDER BY wsi.workspace_item_id");

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<SupervisedItem> getSupervisedItems(EPerson eperson)
    {
        try
        {
            // The following query pulls out distinct workspace items which have 
            // entries in the supervisory linking database.  We use DISTINCT to
            // prevent multiple instances of the item in the case that it is 
            // supervised by more than one group
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "workspaceitem",
                    "SELECT DISTINCT wsi.workspace_item_id " +
                    "FROM workspaceitem wsi, " +
                    "epersongroup2workspaceitem eg2wsi, " +
                    "epersongroup2eperson eg2e " +
                    "WHERE wsi.workspace_item_id = eg2wsi.workspace_item_id " +
                    "AND eg2wsi.eperson_group_id = eg2e.eperson_group_id " +
                    "AND eg2e.eperson_id = ? " + 
                    "ORDER BY wsi.workspace_item_id",
                    eperson.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private List<SupervisedItem> returnAsList(TableRowIterator tri)
        throws SQLException
    {
        List<SupervisedItem> items = new ArrayList<SupervisedItem>();

        for (TableRow row : tri.toList())
        {
            int id = row.getIntColumn("workspace_item_id");
            SupervisedItem si = new SupervisedItem(context, id);
            dao.populate(si);
            items.add(si);
        }

        return items;
    }
}
