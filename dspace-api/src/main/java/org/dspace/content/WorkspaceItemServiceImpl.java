/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for the WorkspaceItem object.
 * This class is responsible for all business logic calls for the WorkspaceItem object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class WorkspaceItemServiceImpl implements WorkspaceItemService {

    private static final Logger log = Logger.getLogger(WorkspaceItemServiceImpl.class);

    @Autowired(required = true)
    protected WorkspaceItemDAO workspaceItemDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected CollectionService collectionService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected WorkflowService workflowService;


    protected WorkspaceItemServiceImpl()
    {

    }

    @Override
    public WorkspaceItem find(Context context, int id) throws SQLException {
        WorkspaceItem workspaceItem = workspaceItemDAO.findByID(context, WorkspaceItem.class, id);

        if (workspaceItem == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_workspace_item",
                        "not_found,workspace_item_id=" + id));
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_workspace_item",
                        "workspace_item_id=" + id));
            }
        }
        return workspaceItem;
    }

    @Override
    public WorkspaceItem create(Context context, Collection collection, boolean template) throws AuthorizeException, SQLException {
        // Check the user has permission to ADD to the collection
        authorizeService.authorizeAction(context, collection, Constants.ADD);

        WorkspaceItem workspaceItem = workspaceItemDAO.create(context, new WorkspaceItem());
        workspaceItem.setCollection(collection);


        // Create an item
        Item item = itemService.create(context, workspaceItem);
        item.setSubmitter(context.getCurrentUser());

        // Now create the policies for the submitter to modify item and contents
        // contents = bitstreams, bundles
        // read permission
        authorizeService.addPolicy(context, item, Constants.READ, item.getSubmitter(), ResourcePolicy.TYPE_SUBMISSION);
        // write permission
        authorizeService.addPolicy(context, item, Constants.WRITE, item.getSubmitter(), ResourcePolicy.TYPE_SUBMISSION);
        // add permission
        authorizeService.addPolicy(context, item, Constants.ADD, item.getSubmitter(), ResourcePolicy.TYPE_SUBMISSION);
        // remove contents permission
        authorizeService.addPolicy(context, item, Constants.REMOVE, item.getSubmitter(), ResourcePolicy.TYPE_SUBMISSION);
        // delete permission
        authorizeService.addPolicy(context, item, Constants.DELETE, item.getSubmitter(), ResourcePolicy.TYPE_SUBMISSION);


        // Copy template if appropriate
        Item templateItem = collection.getTemplateItem();

        if (template && (templateItem != null))
        {
            List<MetadataValue> md = itemService.getMetadata(templateItem, Item.ANY, Item.ANY, Item.ANY, Item.ANY);

            for (MetadataValue aMd : md) {
                MetadataField metadataField = aMd.getMetadataField();
                MetadataSchema metadataSchema = metadataField.getMetadataSchema();
                itemService.addMetadata(context, item, metadataSchema.getName(), metadataField.getElement(), metadataField.getQualifier(), aMd.getLanguage(),
                        aMd.getValue());
            }
        }

        itemService.update(context, item);
        workspaceItem.setItem(item);

        log.info(LogManager.getHeader(context, "create_workspace_item",
                "workspace_item_id=" + workspaceItem.getID()
                        + "item_id=" + item.getID() + "collection_id="
                        + collection.getID()));

        return workspaceItem;
    }

    @Override
    public WorkspaceItem create(Context c, WorkflowItem workflowItem) throws SQLException, AuthorizeException {
        WorkspaceItem workspaceItem = workspaceItemDAO.create(c, new WorkspaceItem());
        workspaceItem.setItem(workflowItem.getItem());
        workspaceItem.setCollection(workflowItem.getCollection());
        update(c, workspaceItem);
        return workspaceItem;
    }

    @Override
    public List<WorkspaceItem> findByEPerson(Context context, EPerson ep) throws SQLException {
        return workspaceItemDAO.findByEPerson(context, ep);
    }

    @Override
    public List<WorkspaceItem> findByCollection(Context context, Collection collection) throws SQLException {
        return workspaceItemDAO.findByCollection(context, collection);
    }

    @Override
    public WorkspaceItem findByItem(Context context, Item item) throws SQLException {
        return workspaceItemDAO.findByItem(context, item);
    }

    @Override
    public List<WorkspaceItem> findAllSupervisedItems(Context context) throws SQLException {
        return workspaceItemDAO.findWithSupervisedGroup(context);
    }

    @Override
    public List<WorkspaceItem> findSupervisedItemsByEPerson(Context context, EPerson ePerson) throws SQLException {
        return workspaceItemDAO.findBySupervisedGroupMember(context, ePerson);
    }

    @Override
    public List<WorkspaceItem> findAll(Context context) throws SQLException {
        return workspaceItemDAO.findAll(context);
    }

    @Override
    public void update(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException {
                // Authorisation is checked by the item.update() method below

        log.info(LogManager.getHeader(context, "update_workspace_item",
                "workspace_item_id=" + workspaceItem.getID()));

        // Update the item
        itemService.update(context, workspaceItem.getItem());

        // Update ourselves
        workspaceItemDAO.save(context, workspaceItem);
    }

    @Override
    public void deleteAll(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException, IOException {
        /*
         * Authorisation is a special case. The submitter won't have REMOVE
         * permission on the collection, so our policy is this: Only the
         * original submitter or an administrator can delete a workspace item.

         */
        Item item = workspaceItem.getItem();
        if (!authorizeService.isAdmin(context)
                && ((context.getCurrentUser() == null) || (context
                        .getCurrentUser().getID() != item.getSubmitter()
                        .getID())))
        {
            // Not an admit, not the submitter
            throw new AuthorizeException("Must be an administrator or the "
                    + "original submitter to delete a workspace item");
        }

        log.info(LogManager.getHeader(context, "delete_workspace_item",
                "workspace_item_id=" + workspaceItem.getID() + "item_id=" + item.getID()
                        + "collection_id=" + workspaceItem.getCollection().getID()));

        // Need to delete the epersongroup2workspaceitem row first since it refers
        // to workspaceitem ID
        workspaceItem.getSupervisorGroups().clear();

        // Need to delete the workspaceitem row first since it refers
        // to item ID
        workspaceItemDAO.delete(context, workspaceItem);

        // Delete item
        itemService.delete(context, item);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return workspaceItemDAO.countRows(context);
    }

    @Override
    public List<Map.Entry<Integer, Long>> getStageReachedCounts(Context context) throws SQLException {
        return workspaceItemDAO.getStageReachedCounts(context);
    }

    @Override
    public void deleteWrapper(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException {
        // Check authorisation. We check permissions on the enclosed item.
        Item item = workspaceItem.getItem();
        authorizeService.authorizeAction(context, item, Constants.WRITE);

        log.info(LogManager.getHeader(context, "delete_workspace_item",
                "workspace_item_id=" + workspaceItem.getID() + "item_id=" + item.getID()
                        + "collection_id=" + workspaceItem.getCollection().getID()));

        //        deleteSubmitPermissions();

        // Need to delete the workspaceitem row first since it refers
        // to item ID
        workspaceItem.getSupervisorGroups().clear();
        workspaceItemDAO.delete(context, workspaceItem);

    }
}
