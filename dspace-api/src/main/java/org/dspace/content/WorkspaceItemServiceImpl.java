/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.event.Event;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the WorkspaceItem object.
 * This class is responsible for all business logic calls for the WorkspaceItem object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class WorkspaceItemServiceImpl implements WorkspaceItemService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(WorkspaceItemServiceImpl.class);

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


    protected WorkspaceItemServiceImpl() {

    }

    @Override
    public WorkspaceItem find(Context context, int id) throws SQLException {
        WorkspaceItem workspaceItem = workspaceItemDAO.findByID(context, WorkspaceItem.class, id);

        if (workspaceItem == null) {
            if (log.isDebugEnabled()) {
                log.debug(LogHelper.getHeader(context, "find_workspace_item",
                                               "not_found,workspace_item_id=" + id));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(LogHelper.getHeader(context, "find_workspace_item",
                                               "workspace_item_id=" + id));
            }
        }
        return workspaceItem;
    }

    @Override
    public WorkspaceItem create(Context context, Collection collection, boolean template)
            throws AuthorizeException, SQLException {
        return create(context, collection, null, template);
    }

    @Override
    public WorkspaceItem create(Context context, Collection collection, UUID uuid, boolean template)
        throws AuthorizeException, SQLException {
        // Check the user has permission to ADD to the collection
        authorizeService.authorizeAction(context, collection, Constants.ADD);

        WorkspaceItem workspaceItem = workspaceItemDAO.create(context, new WorkspaceItem());
        workspaceItem.setCollection(collection);


        // Create an item
        Item item;
        if (uuid != null) {
            item = itemService.create(context, workspaceItem, uuid);
        } else {
            item = itemService.create(context, workspaceItem);
        }
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
        authorizeService
            .addPolicy(context, item, Constants.REMOVE, item.getSubmitter(), ResourcePolicy.TYPE_SUBMISSION);
        // delete permission
        authorizeService
            .addPolicy(context, item, Constants.DELETE, item.getSubmitter(), ResourcePolicy.TYPE_SUBMISSION);

        // Copy template if appropriate
        Item templateItem = collection.getTemplateItem();

        Optional<MetadataValue> colEntityType = getDSpaceEntityType(collection);
        Optional<MetadataValue> templateItemEntityType = getDSpaceEntityType(templateItem);

        if (template && colEntityType.isPresent() && templateItemEntityType.isPresent() &&
                !StringUtils.equals(colEntityType.get().getValue(), templateItemEntityType.get().getValue())) {
            throw new IllegalStateException("The template item has entity type : (" +
                      templateItemEntityType.get().getValue() + ") different than collection entity type : " +
                      colEntityType.get().getValue());
        }

        if (template && colEntityType.isPresent() && templateItemEntityType.isEmpty()) {
            MetadataValue original = colEntityType.get();
            MetadataField metadataField = original.getMetadataField();
            MetadataSchema metadataSchema = metadataField.getMetadataSchema();
            // NOTE: dspace.entity.type = <blank> does not make sense
            //       the collection entity type is by default blank when a collection is first created
            if (StringUtils.isNotBlank(original.getValue())) {
                itemService.addMetadata(context, item, metadataSchema.getName(), metadataField.getElement(),
                                        metadataField.getQualifier(), original.getLanguage(), original.getValue());
            }
        }

        if (template && (templateItem != null)) {
            List<MetadataValue> md = itemService.getMetadata(templateItem, Item.ANY, Item.ANY, Item.ANY, Item.ANY);

            for (MetadataValue aMd : md) {
                MetadataField metadataField = aMd.getMetadataField();
                MetadataSchema metadataSchema = metadataField.getMetadataSchema();
                itemService.addMetadata(context, item, metadataSchema.getName(), metadataField.getElement(),
                                        metadataField.getQualifier(), aMd.getLanguage(),
                                        aMd.getValue());
            }
        }

        itemService.update(context, item);
        workspaceItem.setItem(item);

        log.info(LogHelper.getHeader(context, "create_workspace_item",
                                      "workspace_item_id=" + workspaceItem.getID()
                                          + "item_id=" + item.getID() + "collection_id="
                                          + collection.getID()));

        context.addEvent(new Event(Event.MODIFY, Constants.ITEM, item.getID(), null,
                itemService.getIdentifiers(context, item)));

        return workspaceItem;
    }

    private Optional<MetadataValue> getDSpaceEntityType(DSpaceObject dSpaceObject) {
        return Objects.nonNull(dSpaceObject) ? dSpaceObject.getMetadata()
                                                           .stream()
                                                           .filter(x -> x.getMetadataField().toString('.')
                                                                         .equalsIgnoreCase("dspace.entity.type"))
                                                           .findFirst()
                                             : Optional.empty();
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
    public List<WorkspaceItem> findByEPerson(Context context, EPerson ep, Integer limit, Integer offset)
        throws SQLException {
        return workspaceItemDAO.findByEPerson(context, ep, limit, offset);
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
    public List<WorkspaceItem> findAll(Context context, Integer limit, Integer offset) throws SQLException {
        return workspaceItemDAO.findAll(context, limit, offset);
    }

    @Override
    public void update(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException {
        // Authorisation is checked by the item.update() method below

        log.info(LogHelper.getHeader(context, "update_workspace_item",
                                      "workspace_item_id=" + workspaceItem.getID()));

        // Update the item
        itemService.update(context, workspaceItem.getItem());

        // Update ourselves
        workspaceItemDAO.save(context, workspaceItem);
    }

    @Override
    public void deleteAll(Context context, WorkspaceItem workspaceItem)
        throws SQLException, AuthorizeException, IOException {
        /*
         * Authorisation is a special case. The submitter won't have REMOVE
         * permission on the collection, so our policy is this: Only the
         * original submitter or an administrator can delete a workspace item.

         */
        Item item = workspaceItem.getItem();
        if (!authorizeService.isAdmin(context)
            && (item.getSubmitter() == null || (context.getCurrentUser() == null)
                || (context.getCurrentUser().getID() != item.getSubmitter().getID()))) {
            // Not an admit, not the submitter
            throw new AuthorizeException("Must be an administrator or the "
                                             + "original submitter to delete a workspace item");
        }

        log.info(LogHelper.getHeader(context, "delete_workspace_item",
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
    public int countByEPerson(Context context, EPerson ep) throws SQLException {
        return workspaceItemDAO.countRows(context, ep);
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

        log.info(LogHelper.getHeader(context, "delete_workspace_item",
                                      "workspace_item_id=" + workspaceItem.getID() + "item_id=" + item.getID()
                                          + "collection_id=" + workspaceItem.getCollection().getID()));

        //        deleteSubmitPermissions();

        // Need to delete the workspaceitem row first since it refers
        // to item ID
        try {
            workspaceItem.getSupervisorGroups().clear();
        } catch (Exception e) {
            log.error("failed to clear supervisor group", e);
        }

        workspaceItemDAO.delete(context, workspaceItem);

    }

    @Override
    public void move(Context context, WorkspaceItem source, Collection fromCollection, Collection toCollection)
        throws DCInputsReaderException {
        source.setCollection(toCollection);

        List<MetadataValue> remove = new ArrayList<>();
        List<String> diff = Util.differenceInSubmissionFields(fromCollection, toCollection);
        for (String toRemove : diff) {
            for (MetadataValue value : source.getItem().getMetadata()) {
                if (value.getMetadataField().toString('.').equals(toRemove)) {
                    remove.add(value);
                }
            }
        }

        source.getItem().removeMetadata(remove);

    }

}
