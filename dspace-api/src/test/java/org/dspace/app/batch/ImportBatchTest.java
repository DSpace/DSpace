/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.batch.ImpMetadatavalue;
import org.dspace.batch.ImpRecord;
import org.dspace.batch.ImpRecordToItem;
import org.dspace.batch.service.ImpBitstreamMetadatavalueService;
import org.dspace.batch.service.ImpBitstreamService;
import org.dspace.batch.service.ImpMetadatavalueService;
import org.dspace.batch.service.ImpRecordService;
import org.dspace.batch.service.ImpRecordToItemService;
import org.dspace.batch.service.ImpServiceFactory;
import org.dspace.batch.service.ImpWorkflowNStateService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.WorkflowService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ImportBatchTest extends AbstractUnitTest {
    /**
     * log4j category
     */
    private static final Logger log = LogManager
        .getLogger(ImportBatchTest.class);
    
    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private ImpBitstreamService impBitstreamService = ImpServiceFactory.getInstance().getImpBitstreamService();
    private ImpBitstreamMetadatavalueService impBitstreamMetadatavalueService = ImpServiceFactory.getInstance()
            .getImpBitstreamMetadatavalueService();
    private ImpMetadatavalueService impMetadatavalueService = ImpServiceFactory.getInstance()
            .getImpMetadatavalueService();
    private ImpRecordService impRecordService = ImpServiceFactory.getInstance().getImpRecordService();
    private ImpRecordToItemService impRecordToItemService = ImpServiceFactory.getInstance().getImpRecordToItemService();
    private ImpWorkflowNStateService impWorkflowNStateService = ImpServiceFactory.getInstance()
            .getImpWorkflowNStateService();
    

    private EPerson admin;
    private final String password = "s3cr3t";
    
    public ImportBatchTest() {
    }

    /***
     * Using francesco@sample.ue as context user.
     * 
     * @throws IOException
     */
    @Test
    public void createNewWorkspaceItem() throws IOException {
        try {
            context.turnOffAuthorisationSystem();

            // use ePerson as submitter
            EPerson eperson = ePersonService.create(context);
            eperson.setEmail("francesco@sample.ue");
            eperson.setFirstName(context, "Francesco");
            eperson.setLastName(context, "Cadili");
            ePersonService.setPassword(eperson, "test");
            ePersonService.update(context, eperson);
            context.setCurrentUser(eperson);

            // create the community and a collection
            Community owningCommunity = communityService.create(null, context);
            communityService.setMetadataSingleValue(context, owningCommunity, MetadataSchemaEnum.DC.getName(), "title",
                    null, null, "Main Community");
            communityService.update(context, owningCommunity);

            Collection collection = collectionService.create(context, owningCommunity);
            collectionService.setMetadataSingleValue(context, collection, MetadataSchemaEnum.DC.getName(), "title",
                    null, null, "My Collection");
            collectionService.update(context, collection);

            context.restoreAuthSystemState();

            // create imp_record records
            int impRecordKey = 1;
            String sourceRecordId = UUID.randomUUID().toString();
            String sourceRef = "TEST";
            ImpRecord impRecord = new ImpRecord();
            impRecord.setImpId(impRecordKey);
            impRecordService.setImpCollection(impRecord, collection);
            impRecordService.setImpEperson(impRecord, eperson);
            impRecord.setImpRecordId(sourceRecordId);
            impRecord.setImpSourceref(sourceRef);
            impRecordService.setStatus(impRecord, 'p');
            impRecordService.setOperation(impRecord, "update");
            impRecordService.create(context, impRecord);

            // create imp_metadatavalue records
            int impMetadatavalueKey = 1;
            int impMetadatavalueOrder = 1;
            ImpMetadatavalue impMetadatavalue = new ImpMetadatavalue();
            impMetadatavalue.setMetadatavalueId(impMetadatavalueKey);
            impMetadatavalue.setImpRecord(impRecord);
            impMetadatavalueService.setMetadata(impMetadatavalue, MetadataSchemaEnum.DC.getName(), "title", null, null,
                    "Sample Item");
            impMetadatavalue.setMetadataOrder(impMetadatavalueOrder);
            impMetadatavalueService.create(context, impMetadatavalue);

            // Create a new item
            String argv[] = new String[] { "-E", eperson.getEmail() };

            ItemImportMainOA.main(argv);

            int nItem = workspaceItemService.countByEPerson(context, eperson);
            assertEquals("Workspace Item found 1 for " + eperson.getID(), 1, nItem);

            List<WorkspaceItem> wis = workspaceItemService.findByEPerson(context, eperson);
            assertEquals("Workspace Item found 1 for " + eperson.getID(), 1, wis.size());

            WorkspaceItem wi = wis.get(0);
            Item item = wi.getItem();

            List<MetadataValue> metadata = item.getMetadata();
            assertEquals("Only one metadata", 1, metadata.size());

            String defLanguage = ConfigurationManager.getProperty("default.language");
            metadata = itemService.getMetadata(item, MetadataSchemaEnum.DC.getName(), "title", null, defLanguage);
            assertEquals("Only one metadata is assigned to the item", 1, metadata.size());
            assertEquals("The new metadata value is the right one", metadata.get(0).getValue(), "Sample Item");
            
            // cleanup
            impMetadatavalueService.delete(context, impMetadatavalue);
            impRecordService.delete(context, impRecord);
            context.setCurrentUser(admin);
        } catch (SQLException | AuthorizeException ex) {
            throw new RuntimeException(ex);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /***
     * Using francesco@sample.ue as context user.
     * 
     * @throws IOException
     */
    @Test
    public void deleteItem() {
        try {
            context.turnOffAuthorisationSystem();

            // use ePerson as submitter
            EPerson eperson = ePersonService.create(context);
            eperson.setEmail("francesco@sample.ue");
            eperson.setFirstName(context, "Francesco");
            eperson.setLastName(context, "Cadili");
            ePersonService.setPassword(eperson, "test");
            ePersonService.update(context, eperson);
            context.setCurrentUser(eperson);

            // create the community and a collection
            Community owningCommunity = communityService.create(null, context);
            communityService.setMetadataSingleValue(context, owningCommunity, MetadataSchemaEnum.DC.getName(), "title",
                    null, null, "Main Community");
            communityService.update(context, owningCommunity);

            Collection collection = collectionService.create(context, owningCommunity);
            collectionService.setMetadataSingleValue(context, collection, MetadataSchemaEnum.DC.getName(), "title",
                    null, null, "My Collection");
            collectionService.update(context, collection);

            // create an item
            WorkspaceItem wi = workspaceItemService.create(context, collection, false);
            Item item = wi.getItem();
            itemService.setMetadataSingleValue(context, item, MetadataSchemaEnum.DC.getName(), "title", null, null,
                    "sample item");
            itemService.update(context, item);
            context.restoreAuthSystemState();

            // create imp_record records
            int impRecordKey = 1;
            String sourceRecordId = UUID.randomUUID().toString();
            String sourceRef = "TEST";
            ImpRecord impRecord = new ImpRecord();
            impRecord.setImpId(impRecordKey);
            impRecordService.setImpCollection(impRecord, collection);
            impRecordService.setImpEperson(impRecord, eperson);
            impRecord.setImpRecordId(sourceRecordId);
            impRecord.setImpSourceref(sourceRef);
            impRecordService.setStatus(impRecord, 'p');
            impRecordService.setOperation(impRecord, "delete");
            impRecordService.create(context, impRecord);

            // create imp_record_to_item records
            ImpRecordToItem impRecordToItem = new ImpRecordToItem();
            impRecordToItem.setImpItemId(item.getID());
            impRecordToItem.setImpRecordId(impRecord.getImpRecordId());
            impRecordToItem.setImpSourceref(impRecord.getImpSourceref());
            impRecordToItemService.create(context, impRecordToItem);

            // Create a new item
            String argv[] = new String[] { "-E", eperson.getEmail() };

            ItemImportMainOA.main(argv);

            int nItem = workspaceItemService.countByEPerson(context, eperson);
            assertEquals("Workspace Item found 0 for " + eperson.getID(), 0, nItem);

            assertNull("Record impRecordToItem is deleted.",
                    impRecordToItemService.findByPK(context, impRecord.getImpRecordId()));
            
            // cleanup
            impRecordToItemService.delete(context, impRecordToItem);
            impRecordService.delete(context, impRecord);
        } catch (SQLException | AuthorizeException ex) {
            throw new RuntimeException(ex);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /***
     * Using francesco@sample.ue as context user.
     * 
     * @throws IOException
     */
    @Test
    public void updateItem() throws IOException {
        try {
            context.turnOffAuthorisationSystem();

            // use ePerson as submitter
            EPerson eperson = ePersonService.create(context);
            eperson.setEmail("francesco@sample.ue");
            eperson.setFirstName(context, "Francesco");
            eperson.setLastName(context, "Cadili");
            ePersonService.setPassword(eperson, "test");
            ePersonService.update(context, eperson);
            context.setCurrentUser(eperson);

            // create the community and a collection
            Community owningCommunity = communityService.create(null, context);
            communityService.setMetadataSingleValue(context, owningCommunity, MetadataSchemaEnum.DC.getName(), "title",
                    null, null, "Main Community");
            communityService.update(context, owningCommunity);

            Collection collection = collectionService.create(context, owningCommunity);
            collectionService.setMetadataSingleValue(context, collection, MetadataSchemaEnum.DC.getName(), "title",
                    null, null, "My Collection");
            collectionService.update(context, collection);

            // create an item
            WorkspaceItem wi = workspaceItemService.create(context, collection, false);
            Item item = wi.getItem();
            itemService.setMetadataSingleValue(context, item, MetadataSchemaEnum.DC.getName(), "title", null, null,
                    "sample item");
            itemService.update(context, item);
            context.restoreAuthSystemState();

            // create imp_record records
            int impRecordKey = 1;
            String sourceRecordId = UUID.randomUUID().toString();
            String sourceRef = "TEST";
            ImpRecord impRecord = new ImpRecord();
            impRecord.setImpId(impRecordKey);
            impRecordService.setImpCollection(impRecord, collection);
            impRecordService.setImpEperson(impRecord, eperson);
            impRecord.setImpRecordId(sourceRecordId);
            impRecord.setImpSourceref(sourceRef);
            impRecordService.setStatus(impRecord, 'p');
            impRecordService.setOperation(impRecord, "update");
            impRecordService.create(context, impRecord);

            // create imp_metadatavalue records
            int impMetadatavalueKey = 1;
            int impMetadatavalueOrder = 1;
            ImpMetadatavalue impMetadatavalue = new ImpMetadatavalue();
            impMetadatavalue.setMetadatavalueId(impMetadatavalueKey);
            impMetadatavalue.setImpRecord(impRecord);
            impMetadatavalueService.setMetadata(impMetadatavalue, MetadataSchemaEnum.DC.getName(), "contributor",
                    "author", null, "Francesco Cadili");
            impMetadatavalue.setMetadataOrder(impMetadatavalueOrder);
            impMetadatavalueService.create(context, impMetadatavalue);

            // create imp_record_to_item records
            ImpRecordToItem impRecordToItem = new ImpRecordToItem();
            impRecordToItem.setImpItemId(item.getID());
            impRecordToItem.setImpRecordId(impRecord.getImpRecordId());
            impRecordToItem.setImpSourceref(impRecord.getImpSourceref());
            impRecordToItemService.create(context, impRecordToItem);

            // Create a new item
            String argv[] = new String[] { "-E", eperson.getEmail() };

            ItemImportMainOA.main(argv);

            int nItem = workspaceItemService.countByEPerson(context, eperson);
            assertEquals("One Workspace Item found for " + eperson.getID(), 1, nItem);

            List<WorkspaceItem> wis = workspaceItemService.findByEPerson(context, eperson);
            assertEquals("One Workspace Item found for " + eperson.getID(), 1, wis.size());

            assertEquals("The workspace is the same", wi.getID(), wis.get(0).getID());
            assertEquals("The item is the same", item.getID(), wis.get(0).getItem().getID());

            List<MetadataValue> metadata = item.getMetadata();
            assertEquals("Only one metadata", 1, metadata.size());
            assertEquals("The value is the right one", metadata.get(0).getValue(), "Francesco Cadili");

            String defLanguage = ConfigurationManager.getProperty("default.language");
            metadata = itemService.getMetadata(item, MetadataSchemaEnum.DC.getName(), "contributor", "author",
                    defLanguage);
            assertEquals("Only one metadata is assigned to the item", 1, metadata.size());
            assertEquals("The new metadata value is the right one", metadata.get(0).getValue(), "Francesco Cadili");
            
            // cleanup
            impRecordToItemService.delete(context, impRecordToItem);
            impMetadatavalueService.delete(context, impMetadatavalue);
            impRecordService.delete(context, impRecord);
        } catch (SQLException | AuthorizeException ex) {
            throw new RuntimeException(ex);
        } finally {
            context.restoreAuthSystemState();
        }
    }
}
