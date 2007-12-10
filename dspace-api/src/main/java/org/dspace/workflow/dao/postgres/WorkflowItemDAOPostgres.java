/*
 * WorkflowItemDAOPostgres.java
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
package org.dspace.workflow.dao.postgres;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.dao.WorkspaceItemDAOFactory;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.dao.EPersonDAOFactory;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.workflow.TaskListItem;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.dao.WorkflowItemDAO;

/**
 * @author James Rutherford
 */
public class WorkflowItemDAOPostgres extends WorkflowItemDAO
{
    public WorkflowItemDAOPostgres(Context context)
    {
        this.context = context;

        itemDAO = ItemDAOFactory.getInstance(context);
        wsiDAO = WorkspaceItemDAOFactory.getInstance(context);
    }

    @Override
    public WorkflowItem create() throws AuthorizeException
    {
        UUID uuid = UUID.randomUUID();

        try
        {
            TableRow row = DatabaseManager.create(context, "workflowitem");
            row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("workflow_id");
            WorkflowItem wfi = new WorkflowItem(context, id);
            wfi.setIdentifier(new ObjectIdentifier(uuid));

            return wfi;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public WorkflowItem create(WorkspaceItem wsi) throws AuthorizeException
    {
        WorkflowItem wfi = create();
        return super.create(wfi, wsi);
    }

    @Override
    public WorkflowItem retrieve(int id)
    {
        WorkflowItem wfi = super.retrieve(id);

        if (wfi != null)
        {
            return wfi;
        }

        try
        {
            TableRow row = DatabaseManager.find(context, "workflowitem", id);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public WorkflowItem retrieve(UUID uuid)
    {
        WorkflowItem wfi = super.retrieve(uuid);

        if (wfi != null)
        {
            return wfi;
        }

        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "workflowitem", "uuid", uuid.toString());

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void update(WorkflowItem wfi) throws AuthorizeException
    {
        super.update(wfi);

        try
        {
            TableRow row =
                DatabaseManager.find(context, "workflowitem", wfi.getID());

            if (row != null)
            {
                populateTableRowFromWorkflowItem(wfi, row);
                DatabaseManager.update(context, row);
            }
            else
            {
                throw new RuntimeException("Didn't find workflow item " +
                        wfi.getID());
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void delete(int id) throws AuthorizeException
    {
        super.delete(id);

        try
        {
            DatabaseManager.delete(context, "workflowitem", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public TaskListItem createTask(WorkflowItem wfi, EPerson eperson)
    {
        try
        {
            TableRow row = DatabaseManager.create(context, "tasklistitem");
            row.setColumn("eperson_id", eperson.getID());
            row.setColumn("workflow_id", wfi.getID());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("tasklist_id");
            TaskListItem tli = new TaskListItem(id);
            tli.setEPersonID(eperson.getID());
            tli.setWorkflowItemID(wfi.getID());

            return tli;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void deleteTasks(WorkflowItem wfi)
    {
        try
        {
            DatabaseManager.deleteByValue(context, "tasklistitem",
                    "workflow_id", wfi.getID() + "");
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<WorkflowItem> getWorkflowItems()
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT workflow_id FROM workflowitem " +
                    "ORDER BY workflow_id");

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<WorkflowItem> getWorkflowItemsByOwner(EPerson eperson)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT workflow_id FROM workflowitem " +
                    "WHERE owner = ? ORDER BY workflow_id",
                    eperson.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<WorkflowItem> getWorkflowItemsBySubmitter(EPerson eperson)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT wfi.workflow_id FROM workflowitem wfi, item i " +
                    "WHERE wfi.item_id = i.item_id " +
                    "AND i.submitter_id = ? " + 
                    "ORDER BY wfi.workflow_id",
                    eperson.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<WorkflowItem> getWorkflowItems(Collection collection)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT workflow_id FROM workflowitem " +
                    "WHERE collection_id = ? ",
                    collection.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<TaskListItem> getTaskListItems(EPerson eperson)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT tli.* " +
                    "FROM workflowitem wfi, tasklistitem tli " +
                    "WHERE tli.eperson_id = ? " +
                    "AND tli.workflow_id = wfi.workflow_id",
                    eperson.getID());

            List<TaskListItem> tlItems = new ArrayList<TaskListItem>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("tasklist_id");
                int epersonID = row.getIntColumn("eperson_id");
                int workflowItemID = row.getIntColumn("workflow_id");

                TaskListItem tli = new TaskListItem(id);
                tli.setEPersonID(epersonID);
                tli.setWorkflowItemID(workflowItemID);

                tlItems.add(tli);
            }

            return tlItems;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private WorkflowItem retrieve(TableRow row)
    {
        if (row == null)
        {
            return null;
        }

        int id = row.getIntColumn("workflow_id");
        WorkflowItem wfi = new WorkflowItem(context, id);
        populateWorkflowItemFromTableRow(wfi, row);

        return wfi;
    }

    private List<WorkflowItem> returnAsList(TableRowIterator tri)
            throws SQLException
    {
        List<WorkflowItem> wfItems = new ArrayList<WorkflowItem>();

        for (TableRow row : tri.toList())
        {
            int id = row.getIntColumn("workflow_id");
            wfItems.add(retrieve(id));
        }

        return wfItems;
    }

    private void populateWorkflowItemFromTableRow(WorkflowItem wfi,
            TableRow row)
    {
        itemDAO = ItemDAOFactory.getInstance(context);
        CollectionDAO collectionDAO =
            CollectionDAOFactory.getInstance(context);
        EPersonDAO epersonDAO = EPersonDAOFactory.getInstance(context);

        Item item = itemDAO.retrieve(row.getIntColumn("item_id"));
        Collection collection =
            collectionDAO.retrieve(row.getIntColumn("collection_id"));
        EPerson owner = epersonDAO.retrieve(row.getIntColumn("owner"));

        wfi.setItem(item);
        wfi.setCollection(collection);
        wfi.setOwner(owner);
        wfi.setMultipleFiles(row.getBooleanColumn("multiple_files"));
        wfi.setMultipleTitles(row.getBooleanColumn("multiple_titles"));
        wfi.setPublishedBefore(row.getBooleanColumn("published_before"));
        wfi.setState(row.getIntColumn("state"));
    }

    private void populateTableRowFromWorkflowItem(WorkflowItem wfi,
            TableRow row)
    {
        row.setColumn("item_id", wfi.getItem().getID());
        row.setColumn("collection_id", wfi.getCollection().getID());
        EPerson owner = wfi.getOwner();
        if (owner != null)
        {
            row.setColumn("owner", owner.getID());
        }
        else
        {
            row.setColumnNull("owner");
        }
        row.setColumn("multiple_titles", wfi.hasMultipleTitles());
        row.setColumn("multiple_files", wfi.hasMultipleFiles());
        row.setColumn("published_before", wfi.isPublishedBefore());
        row.setColumn("state", wfi.getState());
    }
}
