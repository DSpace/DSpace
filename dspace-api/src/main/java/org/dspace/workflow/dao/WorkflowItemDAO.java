/*
 * WorkflowItemDAO.java
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
package org.dspace.workflow.dao;

import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.content.Collection;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.eperson.EPerson;
import org.dspace.storage.dao.CRUD;
import org.dspace.workflow.TaskListItem;
import org.dspace.workflow.WorkflowItem;

/**
 * @author James Rutherford
 */
public abstract class WorkflowItemDAO implements CRUD<WorkflowItem>
{
    protected Logger log = Logger.getLogger(WorkflowItemDAO.class);

    protected Context context;
    protected ItemDAO itemDAO;
    protected WorkspaceItemDAO wsiDAO;

    public abstract WorkflowItem create() throws AuthorizeException;

    public abstract WorkflowItem create(WorkspaceItem wsi)
        throws AuthorizeException;

    // FIXME: This should be called something else, but I can't think of
    // anything suitable. The reason this can't go in create() is because we
    // need access to the item that was created, but we can't reach into the
    // subclass to get it (storing it as a protected member variable would be
    // even more filthy).
    protected final WorkflowItem create(WorkflowItem wfi, WorkspaceItem wsi)
        throws AuthorizeException
    {
        wfi.setItem(wsi.getItem());
        wfi.setCollection(wsi.getCollection());
        wfi.setMultipleFiles(wsi.hasMultipleFiles());
        wfi.setMultipleTitles(wsi.hasMultipleTitles());
        wfi.setPublishedBefore(wsi.isPublishedBefore());
        
        wsiDAO.delete(wsi.getID());
        
        update(wfi);

        return wfi;
    }

    public WorkflowItem retrieve(int id)
    {
        return (WorkflowItem) context.fromCache(WorkflowItem.class, id);
    }

    public WorkflowItem retrieve(UUID uuid)
    {
        return null;
    }

    /**
     * Update the workflow item, including the unarchived item.
     */
    public void update(WorkflowItem wfi) throws AuthorizeException
    {
        ItemDAOFactory.getInstance(context).update(wfi.getItem());

        log.info(LogManager.getHeader(context, "update_workflow_item",
                "workflow_item_id=" + wfi.getID()));
    }

    /**
     * Delete the WorkflowItem, retaining the Item
     */
    public void delete(int id) throws AuthorizeException
    {
        WorkflowItem wfi = retrieve(id);
        update(wfi); // Sync in-memory object before removal

        context.removeCached(wfi, id);

        // delete any pending tasks
        deleteTasks(wfi);
    }
    
    public abstract TaskListItem createTask(WorkflowItem wfi, EPerson eperson);
    public abstract void deleteTasks(WorkflowItem wfi);

    public abstract List<WorkflowItem> getWorkflowItems();

    /**
     * Get all workflow items that were original submissions by a particular
     * e-person. These are ordered by workflow ID, since this should likely keep
     * them in the order in which they were created.
     */
    public abstract List<WorkflowItem> getWorkflowItemsBySubmitter(EPerson eperson);

    public abstract List<WorkflowItem> getWorkflowItemsByOwner(EPerson eperson);

    public abstract List<WorkflowItem> getWorkflowItems(Collection collection);

    public abstract List<TaskListItem> getTaskListItems(EPerson eperson);
}
