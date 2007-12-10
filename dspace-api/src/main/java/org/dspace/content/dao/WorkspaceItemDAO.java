package org.dspace.content.dao;

import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.storage.dao.CRUD;
import org.dspace.workflow.WorkflowItem;

public abstract class WorkspaceItemDAO extends ContentDAO<WorkspaceItemDAO>
        implements CRUD<WorkspaceItem>
{
    protected Logger log = Logger.getLogger(WorkspaceItemDAO.class);

    protected Context context;
    protected ItemDAO itemDAO;

    protected WorkspaceItemDAO childDAO;

    public WorkspaceItemDAO(Context context)
    {
        this.context = context;
    }

    public WorkspaceItemDAO getChild()
    {
        return childDAO;
    }

    public void setChild(WorkspaceItemDAO childDAO)
    {
        this.childDAO = childDAO;
    }

    public abstract WorkspaceItem create() throws AuthorizeException;

    /**
     * Create a new workspace item, with a new ID. An Item is also created. The
     * submitter is the current user in the context.
     */
    public abstract WorkspaceItem create(Collection collection,
            boolean template) throws AuthorizeException;

    /**
     * Create a WorkspaceItem from a WorkflowItem. This is for returning Items
     * to a user without submitting it to the archive.
     */
    public abstract WorkspaceItem create(WorkflowItem wfi)
        throws AuthorizeException;

    public abstract WorkspaceItem create(WorkspaceItem wsi, WorkflowItem wfi)
        throws AuthorizeException;

    public abstract WorkspaceItem retrieve(int id);

    public abstract WorkspaceItem retrieve(UUID uuid);

    /**
     * Update the workspace item, including the unarchived item.
     */
    public abstract void update(WorkspaceItem wsi) throws AuthorizeException;

    public abstract void delete(int id) throws AuthorizeException;

    /**
     * Delete the workspace item. The entry in workspaceitem, the unarchived
     * item and its contents are all removed (multiple inclusion
     * notwithstanding.)
     */
    public abstract void deleteAll(int id) throws AuthorizeException;

    public abstract List<WorkspaceItem> getWorkspaceItems();

    public abstract List<WorkspaceItem> getWorkspaceItems(EPerson eperson);

    public abstract List<WorkspaceItem> getWorkspaceItems(Collection collection);

    /**
     * FIXME: I don't like doing this, but it's the least filthy way I can
     * think of achieving what I want.
     */
    public abstract <T extends WorkspaceItem> void populate(T t);
}
