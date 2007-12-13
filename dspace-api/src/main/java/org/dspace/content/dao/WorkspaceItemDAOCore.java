/*
 * WorkspaceItemDAO.java
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
package org.dspace.content.dao;

import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.dao.GroupDAOFactory;
import org.dspace.workflow.WorkflowItem;

/**
 * @author James Rutherford
 */
public class WorkspaceItemDAOCore extends WorkspaceItemDAO
{
    public WorkspaceItemDAOCore(Context context)
    {
        super(context);

        itemDAO = ItemDAOFactory.getInstance(context);
    }

    @Override
    public WorkspaceItem create() throws AuthorizeException
    {
        return childDAO.create();
    }

    @Override
    public WorkspaceItem create(Collection collection, boolean template)
            throws AuthorizeException
    {
        WorkspaceItem wsi = create();

        // Check the user has permission to ADD to the collection
        AuthorizeManager.authorizeAction(context, collection, Constants.ADD);

        EPerson currentUser = context.getCurrentUser();

        // Create an item
        ItemDAO itemDAO = ItemDAOFactory.getInstance(context);
        Item item = itemDAO.create();
        item.setSubmitter(currentUser);

        // Now create the policies for the submitter and workflow users to
        // modify item and contents (contents = bitstreams, bundles)
        // FIXME: hardcoded workflow steps
        Group stepGroups[] = {
                collection.getWorkflowGroup(1),
                collection.getWorkflowGroup(2),
                collection.getWorkflowGroup(3)
        };

        int actions[] = {
                Constants.READ,
                Constants.WRITE,
                Constants.ADD,
                Constants.REMOVE
        };

        // Give read, write, add, and remove privileges to the current user
        for (int action : actions)
        {
            AuthorizeManager.addPolicy(context, item, action, currentUser);
        }

        // Give read, write, add, and remove privileges to the various
        // workflow groups (if any).
        for (Group stepGroup : stepGroups)
        {
            if (stepGroup != null)
            {
                for (int action : actions)
                {
                    AuthorizeManager.addPolicy(context, item, action,
                            stepGroup);
                }
            }
        }

        // Copy template if appropriate
        Item templateItem = collection.getTemplateItem();

        if (template && (templateItem != null))
        {
            DCValue[] md = templateItem.getMetadata(
                    Item.ANY, Item.ANY, Item.ANY, Item.ANY);

            for (int n = 0; n < md.length; n++)
            {
                item.addMetadata(md[n].schema, md[n].element, md[n].qualifier,
                        md[n].language, md[n].value);
            }
        }

        itemDAO.update(item);

        wsi.setItem(item);
        wsi.setCollection(collection);
        update(wsi);

        log.info(LogManager.getHeader(context, "create_workspace_item",
                "workspace_item_id=" + wsi.getID() +
                        "item_id=" + item.getID() +
                        "collection_id=" + collection.getID()));

        return wsi;
    }

    @Override
    public WorkspaceItem create(WorkflowItem wfi) throws AuthorizeException
    {
        return create(create(), wfi);
    }


    public WorkspaceItem create(WorkspaceItem wsi, WorkflowItem wfi)
        throws AuthorizeException
    {
        wsi.setItem(wfi.getItem());
        wsi.setCollection(wfi.getCollection());
        wsi.setMultipleFiles(wfi.hasMultipleFiles());
        wsi.setMultipleTitles(wfi.hasMultipleTitles());
        wsi.setPublishedBefore(wfi.isPublishedBefore());
        update(wsi);

        return wsi;
    }

    public WorkspaceItem retrieve(int id)
    {
        WorkspaceItem wsi =
                (WorkspaceItem) context.fromCache(WorkspaceItem.class, id);

        if (wsi == null)
        {
            wsi = childDAO.retrieve(id);
        }

        return wsi;
    }

    public WorkspaceItem retrieve(UUID uuid)
    {
        return childDAO.retrieve(uuid);
    }

    public void update(WorkspaceItem wsi) throws AuthorizeException
    {
        // Authorisation is checked by the item update
        log.info(LogManager.getHeader(context, "update_workspace_item",
                "workspace_item_id=" + wsi.getID()));

        itemDAO.update(wsi.getItem());

        childDAO.update(wsi);
    }

    public void delete(int id) throws AuthorizeException
    {
        WorkspaceItem wsi = retrieve(id);

        context.removeCached(wsi, id);

        // Check authorisation. We check permissions on the enclosed item.
        AuthorizeManager.authorizeAction(context, wsi.getItem(),
                Constants.WRITE);

        // Collection might be null
        Collection collection = wsi.getCollection();
        int collectionID = -1;

        if (collection != null)
        {
            collectionID = collection.getID();
        }

        log.info(LogManager.getHeader(context, "delete_workspace_item",
                    "workspace_item_id=" + id +
                    "item_id=" + wsi.getItem().getID() +
                    "collection_id=" + collectionID));

        childDAO.delete(id);
    }

    public void deleteAll(int id) throws AuthorizeException
    {
        WorkspaceItem wsi = retrieve(id);
        Item item = wsi.getItem();
        Collection collection = wsi.getCollection();

        /*
         * Authorisation is a special case. The submitter won't have REMOVE
         * permission on the collection, so our policy is this: Only the
         * original submitter or an administrator can delete a workspace item.
         */
        if (!AuthorizeManager.isAdmin(context) &&
                ((context.getCurrentUser() == null) ||
                 (context.getCurrentUser().getID() !=
                  item.getSubmitter().getID())))
        {
            // Not an admit, not the submitter
            throw new AuthorizeException("Must be an administrator or the "
                    + "original submitter to delete a workspace item");
        }

        int collectionID = -1;
        if (collection != null)
        {
            collectionID = collection.getID();
        }

        log.info(LogManager.getHeader(context, "delete_workspace_item",
                "workspace_item_id=" + wsi.getID() +
                "item_id=" + item.getID() +
                "collection_id=" + collectionID));

        // Remove any Group <-> WorkspaceItem mappings
        GroupDAO groupDAO = GroupDAOFactory.getInstance(context);
        for (Group group : groupDAO.getSupervisorGroups(wsi))
        {
            groupDAO.unlink(group, wsi);
        }

        delete(id);
        itemDAO.delete(wsi.getItem().getID());
    }

    public List<WorkspaceItem> getWorkspaceItems()
    {
        return childDAO.getWorkspaceItems();
    }

    public List<WorkspaceItem> getWorkspaceItems(EPerson eperson)
    {
        return childDAO.getWorkspaceItems(eperson);
    }

    public List<WorkspaceItem> getWorkspaceItems(Collection collection)
    {
        return childDAO.getWorkspaceItems(collection);
    }

    public <T extends WorkspaceItem> void populate(T t)
    {
        childDAO.populate(t);
    }
}