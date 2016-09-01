/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.versioning.service.VersionHistoryService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.apache.log4j.Logger;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.versioning.service.VersioningService;
import org.dspace.workflow.WorkflowItemService;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class DefaultItemVersionProvider extends AbstractVersionProvider implements ItemVersionProvider
{
    
    Logger log = Logger.getLogger(DefaultItemVersionProvider.class);

    @Autowired(required = true)
    protected WorkspaceItemService workspaceItemService;
    @Autowired(required = true)
    protected WorkflowItemService workflowItemService;
    @Autowired(required = true)
    protected VersionHistoryService versionHistoryService;
    @Autowired(required = true)
    protected VersioningService versioningService;
    @Autowired(required = true)
    protected IdentifierService identifierService;

    @Override
    public Item createNewItemAndAddItInWorkspace(Context context, Item nativeItem) {
        try
        {
            WorkspaceItem workspaceItem = workspaceItemService.create(context, nativeItem.getOwningCollection(), false);
            Item itemNew = workspaceItem.getItem();
            itemService.update(context, itemNew);
            return itemNew;
        }catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteVersionedItem(Context c, Version versionToDelete, VersionHistory history)
            throws SQLException
    {
        try
        {
            // if versionToDelete is the current version we have to reinstate the previous version
            // and reset canonical
            if(versionHistoryService.isLastVersion(c, history, versionToDelete)
                    && versioningService.getVersionsByHistory(c, history).size() > 1)
            {
                // if a new version gets archived, the old one is set to false.
                // we need to do the oposite now, if the old version was previously
                // unarchived. If the old version is still archived, the new
                // version is a WorkspaceItem or WorkflowItem we should skip this,
                // as unarchiving of previous versions is done only when a newer
                // version gets archived.
                Item item = versionHistoryService.getPrevious(c, history, versionToDelete).getItem();
                if (!item.isArchived()
                        || workspaceItemService.findByItem(c, versionToDelete.getItem()) != null
                        || workflowItemService.findByItem(c, versionToDelete.getItem()) != null)
                {
                    item.setArchived(true);
                    itemService.update(c, item);
                }
            }

            // assign tombstone to the Identifier and reset canonical to the previous version only if there is a previous version
            Item itemToDelete=versionToDelete.getItem();
            identifierService.delete(c, itemToDelete);
        } catch (SQLException | AuthorizeException | IdentifierException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Item updateItemState(Context c, Item itemNew, Item previousItem)
    {
        try
        {
            copyMetadata(c, itemNew, previousItem);
            createBundlesAndAddBitstreams(c, itemNew, previousItem);
            try
            {
                identifierService.reserve(c, itemNew);
            } catch (IdentifierException e) {
                throw new RuntimeException("Can't create Identifier!", e);
            }
            // DSpace knows several types of resource policies (see the class
            // org.dspace.authorize.ResourcePolicy): Submission, Workflow, Custom
            // and inherited. Submission, Workflow and Inherited policies will be
            // set automatically as neccessary. We need to copy the custom policies
            // only to preserve customly set policies and embargos (which are
            // realized by custom policies with a start date).
            List<ResourcePolicy> policies = 
                    authorizeService.findPoliciesByDSOAndType(c, previousItem, ResourcePolicy.TYPE_CUSTOM);
            authorizeService.addPolicies(c, policies, itemNew);
            itemService.update(c, itemNew);
            return itemNew;
        }catch (IOException | SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
