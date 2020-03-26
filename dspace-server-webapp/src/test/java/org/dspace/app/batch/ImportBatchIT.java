/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.batch.ImpBitstream;
import org.dspace.batch.ImpBitstreamMetadatavalue;
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
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.xmlworkflow.Role;
import org.dspace.xmlworkflow.WorkflowUtils;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.junit.Before;
import org.junit.Test;

public class ImportBatchIT extends AbstractControllerIntegrationTest {
    /**
     * log4j category
     */
    private static final Logger log = LogManager.getLogger(ImportBatchIT.class);

    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    private XmlWorkflowItemService workflowItemService = XmlWorkflowServiceFactory.getInstance()
            .getXmlWorkflowItemService();
    private XmlWorkflowService workflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();

    private ImpBitstreamService impBitstreamService = ImpServiceFactory.getInstance().getImpBitstreamService();
    private ImpBitstreamMetadatavalueService impBitstreamMetadatavalueService = ImpServiceFactory.getInstance()
            .getImpBitstreamMetadatavalueService();
    private ImpMetadatavalueService impMetadatavalueService = ImpServiceFactory.getInstance()
            .getImpMetadatavalueService();
    private ImpRecordService impRecordService = ImpServiceFactory.getInstance().getImpRecordService();
    private ImpRecordToItemService impRecordToItemService = ImpServiceFactory.getInstance().getImpRecordToItemService();
    private ImpWorkflowNStateService impWorkflowNStateService = ImpServiceFactory.getInstance()
            .getImpWorkflowNStateService();

    private static UUID epersonUUID = null;
    private final String epersonPassword = "s3cr3t";
    private static UUID owningCommunityUUID = null;
    private static UUID collectionUUID = null;

    @Before
    @Override
    public void setUp() throws Exception {
        try {
            super.setUp();

            context.turnOffAuthorisationSystem();

            EPerson eperson;
            if (epersonUUID == null) {
                eperson = ePersonService.create(context);
                eperson.setEmail("francesco@sample.ue");
                eperson.setFirstName(context, "Francesco");
                eperson.setLastName(context, "Cadili");
                ePersonService.setPassword(eperson, epersonPassword);
                ePersonService.update(context, eperson);
                epersonUUID = eperson.getID();

                Group g = groupService.findByName(context, Group.ADMIN);
                groupService.addMember(context, g, eperson);
                groupService.update(context, g);
            } else {
                eperson = ePersonService.find(context, epersonUUID);
            }
            context.setCurrentUser(eperson);

            if (collectionUUID == null) {
                Community owningCommunity;

                context.setCurrentUser(eperson);
                owningCommunity = communityService.create(null, context);
                communityService.setMetadataSingleValue(context, owningCommunity, MetadataSchemaEnum.DC.getName(),
                        "title", null, null, "Main Community");
                communityService.update(context, owningCommunity);
                owningCommunityUUID = owningCommunity.getID();

                Collection collection = collectionService.create(context, owningCommunity);
                collectionService.setMetadataSingleValue(context, collection, MetadataSchemaEnum.DC.getName(), "title",
                        null, null, "My Collection");
                collectionService.update(context, collection);
                collectionUUID = collection.getID();

                // add eperson to the required groups
                String[] roleNames = { "Reviewer", "Editor", "Final Editor" };
                for (String roleName : roleNames) {
                    Role role = WorkflowUtils.getCollectionAndRepositoryRoles(collection).get(roleName);
                    if (role.getScope() == Role.Scope.COLLECTION) {
                        Group roleGroup = WorkflowUtils.getRoleGroup(context, collection, role);
                        if (roleGroup == null) {
                            roleGroup = groupService.create(context);
                            if (role.getScope() == Role.Scope.COLLECTION) {
                                groupService.setName(roleGroup,
                                        "COLLECTION_" + collection.getID().toString() + "_WORKFLOW_ROLE_" + roleName);
                            } else {
                                groupService.setName(roleGroup, role.getName());
                            }
                            groupService.update(context, roleGroup);
                            if (role.getScope() == Role.Scope.COLLECTION) {
                                WorkflowUtils.createCollectionWorkflowRole(context, collection,
                                        roleName.replaceAll("\\s+", "").toLowerCase(), roleGroup);
                            }
                        }
                        groupService.addMember(context, roleGroup, eperson);
                        groupService.update(context, roleGroup);
                    }
                }

                // add authorization on delete item from collection
                authorizeService.addPolicy(context, collection, Constants.REMOVE, eperson);
            }
            // cleanup all workspace items
            for (WorkspaceItem w : workspaceItemService.findByEPerson(context, eperson)) {
                workspaceItemService.deleteAll(context, w);
            }
            context.restoreAuthSystemState();

            impRecordService.cleanupTables(context);
        } catch (Exception ex) {
            log.error("Error during test initialization", ex);
        }
    }

    /***
     * Create a new workspace item.
     * 
     * @throws IOException
     */
    @Test
    public void createNewWorkspaceItem() throws IOException {
        try {
            EPerson eperson = ePersonService.find(context, epersonUUID);
            Collection collection = collectionService.find(context, collectionUUID);

            // create imp_record records
            int impRecordKey = 1;
            ImpRecord impRecord = createImpRecord(context, impRecordKey, ImpRecordService.SEND_BACK_TO_WORKSPACE_STATUS,
                    ImpRecordService.INSERT_OR_UPDATE_OPERATION, eperson, collection);

            // create imp_metadatavalue records
            int impMetadatavalueKey = 1;
            createImpMetadatavalue(context, impRecordKey, impMetadatavalueKey, MetadataSchemaEnum.DC.getName(), "title",
                    null, null, "Sample Item");

            // Create a new item
            String argv[] = new String[] { "-E", eperson.getEmail() };

            ItemImportMainOA.main(argv);

            int nItem = workspaceItemService.countByEPerson(context, eperson);
            assertEquals("One workspace item found for " + eperson.getID(), 1, nItem);

            List<WorkspaceItem> wis = workspaceItemService.findByEPerson(context, eperson);
            assertEquals("One workspace item found for " + eperson.getID(), 1, wis.size());

            WorkspaceItem wi = wis.get(0);
            Item item = wi.getItem();

            List<MetadataValue> metadata = item.getMetadata();
            assertEquals("Only one metadata found", 1, metadata.size());

            String defLanguage = ConfigurationManager.getProperty("default.language");
            metadata = itemService.getMetadata(item, MetadataSchemaEnum.DC.getName(), "title", null, defLanguage);
            assertEquals("Only one metadata is assigned to the item", 1, metadata.size());
            assertEquals("Is the new metadata value the right one?", metadata.get(0).getValue(), "Sample Item");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /***
     * Remove an item.
     * 
     * @throws IOException
     */
    @Test
    public void deleteItem() {
        try {
            context.turnOffAuthorisationSystem();
            EPerson eperson = ePersonService.find(context, epersonUUID);
            Collection collection = collectionService.find(context, collectionUUID);

            // create an item
            WorkspaceItem wi = workspaceItemService.create(context, collection, false);
            Item item = wi.getItem();
            itemService.setMetadataSingleValue(context, item, MetadataSchemaEnum.DC.getName(), "title", null, null,
                    "sample item");
            itemService.update(context, item);
            context.restoreAuthSystemState();

            // create imp_record records
            int impRecordKey = 1;
            ImpRecord impRecord = createImpRecord(context, impRecordKey, ImpRecordService.SEND_BACK_TO_WORKSPACE_STATUS,
                    ImpRecordService.DELETE_OPERATION, eperson, collection);

            // create imp_record_to_item records
            ImpRecordToItem impRecordToItem = createImpRecordToItem(context, impRecord, item);

            // Create a new item
            String argv[] = new String[] { "-E", eperson.getEmail() };

            ItemImportMainOA.main(argv);

            wi = workspaceItemService.find(context, wi.getID());
            assertNull("Is the workspace item null?", wi);

            item = itemService.find(context, item.getID());
            assertNull("Is the item null?", item);

            int nItem = workspaceItemService.countByEPerson(context, eperson);
            assertEquals("No workspace item found for " + eperson.getID(), 0, nItem);
        } catch (SQLException | AuthorizeException ex) {
            throw new RuntimeException(ex);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /***
     * Add some metadata to an existing item. Old metadata are cleared.
     * 
     * @throws IOException
     */
    @Test
    public void updateItemAndClean() throws IOException {
        try {
            context.turnOffAuthorisationSystem();
            EPerson eperson = ePersonService.find(context, epersonUUID);
            Collection collection = collectionService.find(context, collectionUUID);

            // create an item
            WorkspaceItem wi = workspaceItemService.create(context, collection, false);
            Item item = wi.getItem();
            itemService.setMetadataSingleValue(context, item, MetadataSchemaEnum.DC.getName(), "title", null, null,
                    "sample item");
            itemService.update(context, item);
            context.restoreAuthSystemState();

            // create imp_record records
            int impRecordKey = 1;
            ImpRecord impRecord = createImpRecord(context, impRecordKey, ImpRecordService.SEND_BACK_TO_WORKSPACE_STATUS,
                    ImpRecordService.INSERT_OR_UPDATE_OPERATION, eperson, collection);

            // create imp_metadatavalue records
            int impMetadatavalueKey = 1;
            createImpMetadatavalue(context, impRecordKey, impMetadatavalueKey, MetadataSchemaEnum.DC.getName(),
                    "contributor", "author", null, "Francesco Cadili");

            // create imp_record_to_item records
            ImpRecordToItem impRecordToItem = createImpRecordToItem(context, impRecord, item);

            // Create a new item
            String argv[] = new String[] { "-E", eperson.getEmail() };

            ItemImportMainOA.main(argv);

            assertEquals("Does the workspace item exist?", workspaceItemService.find(context, wi.getID()), wi);
            assertEquals("Does the item exist?", itemService.find(context, item.getID()), item);

            int nItem = workspaceItemService.countByEPerson(context, eperson);
            assertEquals("One workspace item found for " + eperson.getID(), 1, nItem);

            List<MetadataValue> metadata = item.getMetadata();
            assertEquals("Only one metadata found", 1, metadata.size());
            assertEquals("Is the value the right one?", metadata.get(0).getValue(), "Francesco Cadili");

            String defLanguage = ConfigurationManager.getProperty("default.language");
            metadata = itemService.getMetadata(item, MetadataSchemaEnum.DC.getName(), "contributor", "author",
                    defLanguage);
            assertEquals("Only one metadata is assigned to the item", 1, metadata.size());
            assertEquals("Is the new metadata value the right one?", metadata.get(0).getValue(), "Francesco Cadili");
        } catch (SQLException | AuthorizeException ex) {
            throw new RuntimeException(ex);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /***
     * Add some metadata to an existing item. Old metadata are kept.
     * 
     * @throws IOException
     */
    @Test
    public void updateItemAndKeep() throws IOException {
        try {
            context.turnOffAuthorisationSystem();
            EPerson eperson = ePersonService.find(context, epersonUUID);
            Collection collection = collectionService.find(context, collectionUUID);

            // create an item
            WorkspaceItem wi = workspaceItemService.create(context, collection, false);
            Item item = wi.getItem();
            itemService.setMetadataSingleValue(context, item, MetadataSchemaEnum.DC.getName(), "title", null, null,
                    "sample item");
            itemService.update(context, item);
            context.restoreAuthSystemState();

            // create imp_record records
            int impRecordKey = 1;
            ImpRecord impRecord = createImpRecord(context, impRecordKey, ImpRecordService.SEND_BACK_TO_WORKSPACE_STATUS,
                    ImpRecordService.INSERT_OR_UPDATE_OPERATION, eperson, collection);

            // create imp_metadatavalue records
            int impMetadatavalueKey = 1;
            createImpMetadatavalue(context, impRecordKey, impMetadatavalueKey, MetadataSchemaEnum.DC.getName(),
                    "contributor", "author", null, "Francesco Cadili");

            // create imp_record_to_item records
            ImpRecordToItem impRecordToItem = createImpRecordToItem(context, impRecord, item);

            // Create a new item
            String argv[] = new String[] { "-E", eperson.getEmail(), "-m", "dc.title", "-s" };

            ItemImportMainOA.main(argv);

            assertEquals("Does the workspace item exist?", workspaceItemService.find(context, wi.getID()), wi);
            assertEquals("Does the item exist?", itemService.find(context, item.getID()), item);

            int nItem = workspaceItemService.countByEPerson(context, eperson);
            assertEquals("One workspace item found for " + eperson.getID(), 1, nItem);

            List<MetadataValue> metadata = item.getMetadata();
            assertEquals("Two metadata found", 2, metadata.size());
            for (MetadataValue m : metadata) {
                if ("contributor".equals(m.getElement())) {
                    assertEquals("dc.contibutor.autor is the right one", m.getValue(), "Francesco Cadili");
                } else if ("title".equals(m.getElement())) {
                    assertEquals("dc.title value is the right one", m.getValue(), "sample item");
                } else {
                    assertTrue("Metadata is not valid.", m == null);
                }
            }
        } catch (SQLException | AuthorizeException ex) {
            throw new RuntimeException(ex);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /***
     * Create some new workspace items.
     * 
     * @throws IOException
     */
    @Test
    public void createSomeNewWorkspaceItem() throws IOException {
        try {
            EPerson eperson = ePersonService.find(context, epersonUUID);
            Collection collection = collectionService.find(context, collectionUUID);

            // create imp_record records
            for (int impRecordKey = 1; impRecordKey < 15; impRecordKey++) {
                createImpRecord(context, impRecordKey, ImpRecordService.SEND_BACK_TO_WORKSPACE_STATUS,
                        ImpRecordService.INSERT_OR_UPDATE_OPERATION, eperson, collection);
            }

            // create imp_metadatavalue records
            int impMetadatavalueKey = 1;
            for (int impRecordKey = 1; impRecordKey < 15; impRecordKey++) {
                createImpMetadatavalue(context, impRecordKey, impMetadatavalueKey, MetadataSchemaEnum.DC.getName(),
                        "title", null, null, "Sample Item (" + impMetadatavalueKey + ")");
                impMetadatavalueKey++;
            }

            // Create a new item
            String argv[] = new String[] { "-E", eperson.getEmail() };

            ItemImportMainOA.main(argv);

            int nItem = workspaceItemService.countByEPerson(context, eperson);
            assertEquals("14 workspace Items found for " + eperson.getID(), 14, nItem);

            List<WorkspaceItem> wis = workspaceItemService.findByEPerson(context, eperson);
            assertEquals("14 workspace items found for " + eperson.getID(), 14, wis.size());

            for (WorkspaceItem wi : wis) {
                Item item = wi.getItem();

                List<MetadataValue> metadata = item.getMetadata();
                assertEquals("Only one metadata found", 1, metadata.size());

                String defLanguage = ConfigurationManager.getProperty("default.language");
                metadata = itemService.getMetadata(item, MetadataSchemaEnum.DC.getName(), "title", null, defLanguage);
                assertEquals("Only one metadata is assigned to the item", 1, metadata.size());
                assertTrue("Is the new metadata value the right one?",
                        metadata.get(0).getValue().indexOf("Sample Item") == 0);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /***
     * Complex example: some insert, followed by some deletion and updates.
     * 
     * @throws IOException
     */
    @Test
    public void runComplexExample() throws IOException {
        try {
            context.turnOffAuthorisationSystem();
            EPerson eperson = ePersonService.find(context, epersonUUID);
            Collection collection = collectionService.find(context, collectionUUID);

            // create imp_record records
            for (int impRecordKey = 1; impRecordKey < 15; impRecordKey++) {
                createImpRecord(context, impRecordKey, ImpRecordService.SEND_BACK_TO_WORKSPACE_STATUS,
                        ImpRecordService.INSERT_OR_UPDATE_OPERATION, eperson, collection);
            }

            // create imp_metadatavalue records
            int impMetadatavalueKey = 1;
            for (int impRecordKey = 1; impRecordKey < 15; impRecordKey++) {
                createImpMetadatavalue(context, impRecordKey, impMetadatavalueKey, MetadataSchemaEnum.DC.getName(),
                        "title", null, null, "Sample Object (" + impRecordKey + ")");
                createImpMetadatavalue(context, impRecordKey, impMetadatavalueKey + 1, MetadataSchemaEnum.DC.getName(),
                        "contributor", "author", null, "Francesco Cadili");
                impMetadatavalueKey += 2;
            }

            // Create 14 new items
            String argv[] = new String[] { "-E", eperson.getEmail() };
            ItemImportMainOA.main(argv);
            impRecordService.cleanupTables(context);

            List<WorkspaceItem> wis = workspaceItemService.findByEPerson(context, eperson);
            assertEquals("14 workspace items found for " + eperson.getID(), 14, wis.size());

            WorkspaceItem del01 = null;
            WorkspaceItem upd02 = null;
            WorkspaceItem upd10 = null;
            WorkspaceItem del11 = null;
            for (WorkspaceItem wi : wis) {
                Item item = wi.getItem();
                List<MetadataValue> metadata = wi.getItem().getMetadata();

                assertEquals("Only two metadata found", 2, metadata.size());

                for (MetadataValue m : metadata) {
                    if ("title".equals(m.getElement())) {
                        assertTrue("the title is: ", m.getValue().indexOf("Sample Object (") == 0);

                        int start = "Sample Object (".length();
                        int end = m.getValue().indexOf(")");
                        int recordKey = Integer.parseInt(m.getValue().substring(start, end));

                        switch (recordKey) {
                            case 1: {
                                del01 = wi;
                                break;
                            }
                            case 2: {
                                upd02 = wi;
                                break;
                            }
                            case 10: {
                                upd10 = wi;
                                break;
                            }
                            case 11: {
                                del11 = wi;
                                break;
                            }
                            default: {
                                assertTrue(recordKey != 1 && recordKey != 2 && recordKey != 11);
                                break;
                            }
                        }
                    } else if ("contributor".equals(m.getElement())) {
                        assertEquals("The contributor.author is: ", "Francesco Cadili", m.getValue());
                    } else {
                        assertEquals("Invalid Metadata", null, m.getValue());
                    }
                }
            }

            assertTrue("Selected workspace exists (del01)", del01 != null);
            assertTrue("Selected workspace exists (upd02)", upd02 != null);
            assertTrue("Selected workspace exists (del11)", del11 != null);

            // remove del01 and del11
            int impRecordKey2 = 1;
            ImpRecord impRecord = createImpRecord(context, impRecordKey2,
                    ImpRecordService.SEND_BACK_TO_WORKSPACE_STATUS, ImpRecordService.DELETE_OPERATION, eperson,
                    collection);
            ImpRecordToItem impRecordToItem = createImpRecordToItem(context, impRecord, del01.getItem());

            impRecord = createImpRecord(context, impRecordKey2 + 1, ImpRecordService.SEND_BACK_TO_WORKSPACE_STATUS,
                    ImpRecordService.DELETE_OPERATION, eperson, collection);
            impRecordToItem = createImpRecordToItem(context, impRecord, del11.getItem());

            // Delete two items
            argv = new String[] { "-E", eperson.getEmail() };
            ItemImportMainOA.main(argv);
            impRecordService.cleanupTables(context);

            assertNull("Is the workspace item null?", workspaceItemService.find(context, del01.getID()));
            assertNull("Is the workspace item null?", workspaceItemService.find(context, del11.getID()));

            assertNull("Is the item null?", itemService.find(context, del01.getItem().getID()));
            assertNull("Is the item null?", itemService.find(context, del11.getItem().getID()));

            wis = workspaceItemService.findByEPerson(context, eperson);
            assertEquals("12 workspace items found for " + eperson.getID(), 12, wis.size());

            for (WorkspaceItem wi : wis) {
                Item item = wi.getItem();
                List<MetadataValue> metadata = wi.getItem().getMetadata();

                assertEquals("Only two metadata found", 2, metadata.size());

                for (MetadataValue m : metadata) {
                    if ("title".equals(m.getElement())) {
                        assertTrue("The title is: ", m.getValue().indexOf("Sample Object (") == 0);

                        int start = "Sample Object (".length();
                        int end = m.getValue().indexOf(")");
                        int recordKey = Integer.parseInt(m.getValue().substring(start, end));

                        switch (recordKey) {
                            case 1: {
                                assertTrue("Failed to remove ", recordKey != 1);
                                break;
                            }
                            case 11: {
                                assertTrue("Failed to remove ", recordKey != 11);
                                break;
                            }
                            default: {
                                assertTrue(recordKey != 1 && recordKey != 11);
                                break;
                            }
                        }
                    } else if ("contributor".equals(m.getElement())) {
                        assertEquals("The contributor.author is: ", "Francesco Cadili", m.getValue());
                    } else {
                        assertEquals("Invalid Metadata", null, m.getValue());
                    }
                }
            }

            // update (using two records instead of one)
            // create imp_record records
            for (int impRecordKey = 1; impRecordKey < 3; impRecordKey++) {
                impRecord = createImpRecord(context, impRecordKey, ImpRecordService.SEND_BACK_TO_WORKSPACE_STATUS,
                        ImpRecordService.INSERT_OR_UPDATE_OPERATION, eperson, collection);

                // create imp_record_to_item records
                if (impRecordKey == 1) {
                    impRecordToItem = createImpRecordToItem(context, impRecord, upd02.getItem());
                } else {
                    impRecordToItem = createImpRecordToItem(context, impRecord, upd10.getItem());
                }
            }

            // create imp_metadatavalue records
            impMetadatavalueKey = 1;
            for (int impRecordKey = 1; impRecordKey < 3; impRecordKey++) {
                if (impRecordKey == 1) {
                    createImpMetadatavalue(context, impRecordKey, impMetadatavalueKey, MetadataSchemaEnum.DC.getName(),
                            "contributor", "editor", null, "Matteo Perelli");
                } else {
                    createImpMetadatavalue(context, impRecordKey, impMetadatavalueKey, MetadataSchemaEnum.DC.getName(),
                            "date", null, null, "2020/03/23");
                }
                impMetadatavalueKey++;
            }

            // Create a new item
            argv = new String[] { "-E", eperson.getEmail(), "-m", "dc.title", "-m", "dc.contributor.author", "-s" };
            ItemImportMainOA.main(argv);
            impRecordService.cleanupTables(context);

            wis = workspaceItemService.findByEPerson(context, eperson);
            assertEquals("Workspace Item found 12 for " + eperson.getID(), 12, wis.size());

            for (WorkspaceItem wi : wis) {
                Item item = wi.getItem();
                List<MetadataValue> metadata = wi.getItem().getMetadata();

                if (wi.getID() != upd02.getID() && wi.getID() != upd10.getID()) {
                    assertEquals("Only two metadata found", 2, metadata.size());
                } else {
                    assertEquals("Only three metadata found", 3, metadata.size());
                }

                for (MetadataValue m : metadata) {
                    if ("title".equals(m.getElement())) {
                        assertTrue("The title is: ", m.getValue().indexOf("Sample Object (") == 0);

                        int start = "Sample Object (".length();
                        int end = m.getValue().indexOf(")");
                        int recordKey = Integer.parseInt(m.getValue().substring(start, end));

                        switch (recordKey) {
                            case 1: {
                                assertTrue("Failed to remove ", recordKey != 1);
                                break;
                            }
                            case 11: {
                                assertTrue("Failed to remove ", recordKey != 11);
                                break;
                            }
                            default: {
                                assertTrue(recordKey != 1 && recordKey != 11);
                                break;
                            }
                        }
                    } else if ("contributor".equals(m.getElement()) && "author".equals(m.getQualifier())) {
                        assertEquals("The dc.contributor.author is: ", "Francesco Cadili", m.getValue());
                    } else if ("contributor".equals(m.getElement()) && "editor".equals(m.getQualifier())) {
                        assertEquals("The dc.contributor.author is: ", "Matteo Perelli", m.getValue());
                        assertEquals("The workspace item is: ", upd02.getID(), wi.getID());
                    } else if ("date".equals(m.getElement())) {
                        assertEquals("The dc.date is: ", "2020/03/23", m.getValue());
                        assertEquals("The workspace item is: ", upd10.getID(), wi.getID());
                    } else {
                        assertEquals("Invalid Metadata", null, m.getValue());
                    }
                }
            }

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /***
     * Test bitstream creation with one metadata. Embargo group is not set.
     * 
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void addBitstream() throws IOException, URISyntaxException {
        try {
            context.turnOffAuthorisationSystem();
            EPerson eperson = ePersonService.find(context, epersonUUID);
            Collection collection = collectionService.find(context, collectionUUID);

            // create an item
            WorkspaceItem wi = workspaceItemService.create(context, collection, false);
            Item item = wi.getItem();
            itemService.setMetadataSingleValue(context, item, MetadataSchemaEnum.DC.getName(), "title", null, null,
                    "sample item");
            itemService.update(context, item);

            // create groups
            context.restoreAuthSystemState();

            // create imp_record records
            int impRecordKey = 1;
            ImpRecord impRecord = createImpRecord(context, impRecordKey, ImpRecordService.SEND_BACK_TO_WORKSPACE_STATUS,
                    ImpRecordService.INSERT_OR_UPDATE_OPERATION, eperson, collection);

            // create imp_metadatavalue records
            int impMetadatavalueKey = 1;
            createImpMetadatavalue(context, impRecordKey, impMetadatavalueKey, MetadataSchemaEnum.DC.getName(),
                    "contributor", "author", null, "Francesco Cadili");

            // create imp_record_to_item records
            ImpRecordToItem impRecordToItem = createImpRecordToItem(context, impRecord, item);

            int impBitstreamId = 1;
            ImpBitstream impBitstream = createImpBitstream(context, impRecordKey, impBitstreamId,
                    getClass().getResource("/org/dspace/license/CreativeCommons.xsl"), "CreativeCommons.xsl",
                    "License file", null, null);

            int impBitstreamMetadatavalueKey = 1;
            createImpBitstreamMetadatavalue(context, impBitstreamId, impBitstreamMetadatavalueKey,
                    MetadataSchemaEnum.DC.getName(), "description", null, null, "License file");

            // Create a new item
            String argv[] = new String[] { "-E", eperson.getEmail() };

            ItemImportMainOA.main(argv);

            assertEquals("Does workspace item exist?", workspaceItemService.find(context, wi.getID()), wi);
            assertEquals("Does item exist?", itemService.find(context, item.getID()), item);

            int nItem = workspaceItemService.countByEPerson(context, eperson);
            assertEquals("Workspace Item found 1 for " + eperson.getID(), 1, nItem);

            List<MetadataValue> metadata = item.getMetadata();
            assertEquals("Only one metadata", 1, metadata.size());
            assertEquals("Is the value the right one?", metadata.get(0).getValue(), "Francesco Cadili");

            String defLanguage = ConfigurationManager.getProperty("default.language");
            metadata = itemService.getMetadata(item, MetadataSchemaEnum.DC.getName(), "contributor", "author",
                    defLanguage);
            assertEquals("Only one metadata is assigned to the item", 1, metadata.size());
            assertEquals("Is the new metadata value the right one?", metadata.get(0).getValue(), "Francesco Cadili");

            int nBits = 0;
            Iterator<Bitstream> iter = bitstreamService.getItemBitstreams(context, item);
            while (iter.hasNext()) {
                Bitstream b = iter.next();

                // check metadata
                String m = bitstreamService.getMetadata(b, "dc.description");
                assertEquals("check metadata", "License file", m);

                // check policy
                List<ResourcePolicy> p = authorizeService.getPolicies(context, b);
                assertEquals("Only one embrago policy", 1, p.size());

                Group g = groupService.findByName(context, Group.ANONYMOUS);
                assertEquals("Use anonymous group in emmargo policy", g.getName(), p.get(0).getGroup().getName());

                context.turnOffAuthorisationSystem();
                bitstreamService.delete(context, b);
                context.restoreAuthSystemState();

                nBits++;
                assertEquals("Only one bitstream", false, iter.hasNext());
            }
        } catch (SQLException | AuthorizeException ex) {
            throw new RuntimeException(ex);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /***
     * Test bitstream creation with one metadata. Embargo group is set to
     * administrator.
     * 
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void addBitstreamWithEmbargoGroup() throws IOException, URISyntaxException {
        try {
            context.turnOffAuthorisationSystem();
            EPerson eperson = ePersonService.find(context, epersonUUID);
            Collection collection = collectionService.find(context, collectionUUID);

            Group adminGroup = groupService.findByName(context, Group.ADMIN);

            // create an item
            WorkspaceItem wi = workspaceItemService.create(context, collection, false);
            Item item = wi.getItem();
            itemService.setMetadataSingleValue(context, item, MetadataSchemaEnum.DC.getName(), "title", null, null,
                    "sample item");
            itemService.update(context, item);

            // create groups
            context.restoreAuthSystemState();

            // create imp_record records
            int impRecordKey = 1;
            ImpRecord impRecord = createImpRecord(context, impRecordKey, ImpRecordService.SEND_BACK_TO_WORKSPACE_STATUS,
                    ImpRecordService.INSERT_OR_UPDATE_OPERATION, eperson, collection);

            // create imp_metadatavalue records
            int impMetadatavalueKey = 1;
            createImpMetadatavalue(context, impRecordKey, impMetadatavalueKey, MetadataSchemaEnum.DC.getName(),
                    "contributor", "author", null, "Francesco Cadili");

            // create imp_record_to_item records
            ImpRecordToItem impRecordToItem = createImpRecordToItem(context, impRecord, item);

            int impBitstreamId = 1;
            ImpBitstream impBitstream = createImpBitstream(context, impRecordKey, impBitstreamId,
                    getClass().getResource("/org/dspace/license/CreativeCommons.xsl"), "CreativeCommons.xsl",
                    "License file", adminGroup.getID(), null);

            int impBitstreamMetadatavalueKey = 1;
            createImpBitstreamMetadatavalue(context, impBitstreamId, impBitstreamMetadatavalueKey,
                    MetadataSchemaEnum.DC.getName(), "description", null, null, "License file");

            // Create a new item
            String argv[] = new String[] { "-E", eperson.getEmail() };

            ItemImportMainOA.main(argv);

            assertEquals("Does the workspace item exist?", workspaceItemService.find(context, wi.getID()), wi);
            assertEquals("Does theh item exist?", itemService.find(context, item.getID()), item);

            int nItem = workspaceItemService.countByEPerson(context, eperson);
            assertEquals("One workspace item found for " + eperson.getID(), 1, nItem);

            List<MetadataValue> metadata = item.getMetadata();
            assertEquals("Only one metadata found", 1, metadata.size());
            assertEquals("Is the value the right one?", metadata.get(0).getValue(), "Francesco Cadili");

            String defLanguage = ConfigurationManager.getProperty("default.language");
            metadata = itemService.getMetadata(item, MetadataSchemaEnum.DC.getName(), "contributor", "author",
                    defLanguage);
            assertEquals("Only one metadata is assigned to the item", 1, metadata.size());
            assertEquals("Is the new metadata value the right one?", metadata.get(0).getValue(), "Francesco Cadili");

            int nBits = 0;
            Iterator<Bitstream> iter = bitstreamService.getItemBitstreams(context, item);
            while (iter.hasNext()) {
                Bitstream b = iter.next();

                // check metadata
                String m = bitstreamService.getMetadata(b, "dc.description");
                assertEquals("Check metadata", "License file", m);

                // check policy
                List<ResourcePolicy> p = authorizeService.getPolicies(context, b);
                assertEquals("Only one embrago policy", 1, p.size());

                assertEquals("Use administrator group in emmargo policy", adminGroup.getName(),
                        p.get(0).getGroup().getName());

                context.turnOffAuthorisationSystem();
                bitstreamService.delete(context, b);
                context.restoreAuthSystemState();

                nBits++;
                assertEquals("Only one bitstream", false, iter.hasNext());
            }
        } catch (SQLException | AuthorizeException ex) {
            throw new RuntimeException(ex);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /***
     * Test bitstream creation with one metadata. Embargo group is set to
     * administrator. Embargo start date is set to a valid data in format
     * dd/MM/yyyy.
     * 
     * @See {@link SimpleDateFormat}
     * 
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void addBitstreamWithEmbargoGroupAndDate() throws IOException, URISyntaxException {
        try {
            context.turnOffAuthorisationSystem();
            EPerson eperson = ePersonService.find(context, epersonUUID);
            Collection collection = collectionService.find(context, collectionUUID);

            Group adminGroup = groupService.findByName(context, Group.ADMIN);

            // create an item
            WorkspaceItem wi = workspaceItemService.create(context, collection, false);
            Item item = wi.getItem();
            itemService.setMetadataSingleValue(context, item, MetadataSchemaEnum.DC.getName(), "title", null, null,
                    "sample item");
            itemService.update(context, item);

            // create groups
            context.restoreAuthSystemState();

            // create imp_record records
            int impRecordKey = 1;
            ImpRecord impRecord = createImpRecord(context, impRecordKey, ImpRecordService.SEND_BACK_TO_WORKSPACE_STATUS,
                    ImpRecordService.INSERT_OR_UPDATE_OPERATION, eperson, collection);

            // create imp_metadatavalue records
            int impMetadatavalueKey = 1;
            createImpMetadatavalue(context, impRecordKey, impMetadatavalueKey, MetadataSchemaEnum.DC.getName(),
                    "contributor", "author", null, "Francesco Cadili");

            // create imp_record_to_item records
            ImpRecordToItem impRecordToItem = createImpRecordToItem(context, impRecord, item);

            int impBitstreamId = 1;
            ImpBitstream impBitstream = createImpBitstream(context, impRecordKey, impBitstreamId,
                    getClass().getResource("/org/dspace/license/CreativeCommons.xsl"), "CreativeCommons.xsl",
                    "License file", adminGroup.getID(), "01/02/2020");

            int impBitstreamMetadatavalueKey = 1;
            createImpBitstreamMetadatavalue(context, impBitstreamId, impBitstreamMetadatavalueKey,
                    MetadataSchemaEnum.DC.getName(), "description", null, null, "License file");

            // Create a new item
            String argv[] = new String[] { "-E", eperson.getEmail() };

            ItemImportMainOA.main(argv);

            assertEquals("Does the workspace item exist?", workspaceItemService.find(context, wi.getID()), wi);
            assertEquals("Does theh item exist?", itemService.find(context, item.getID()), item);

            int nItem = workspaceItemService.countByEPerson(context, eperson);
            assertEquals("One workspace item found for " + eperson.getID(), 1, nItem);

            List<MetadataValue> metadata = item.getMetadata();
            assertEquals("Only one metadata found", 1, metadata.size());
            assertEquals("Is the value the right one?", metadata.get(0).getValue(), "Francesco Cadili");

            String defLanguage = ConfigurationManager.getProperty("default.language");
            metadata = itemService.getMetadata(item, MetadataSchemaEnum.DC.getName(), "contributor", "author",
                    defLanguage);
            assertEquals("Only one metadata is assigned to the item", 1, metadata.size());
            assertEquals("Is the new metadata value the right one?", metadata.get(0).getValue(), "Francesco Cadili");

            int nBits = 0;
            Iterator<Bitstream> iter = bitstreamService.getItemBitstreams(context, item);
            while (iter.hasNext()) {
                Bitstream b = iter.next();

                // check metadata
                String m = bitstreamService.getMetadata(b, "dc.description");
                assertEquals("Check metadata", "License file", m);

                // check policy
                List<ResourcePolicy> p = authorizeService.getPolicies(context, b);
                assertEquals("Only one embrago policy", 1, p.size());

                assertEquals("Use administrator group in emmargo policy", adminGroup.getName(),
                        p.get(0).getGroup().getName());

                Calendar cal = Calendar.getInstance();
                cal.clear();
                // The month starts from 0
                cal.set(2020, 01, 01, 0, 0, 0);
                assertEquals("Embargo end date is set", cal.getTimeInMillis(), p.get(0).getStartDate().getTime());

                context.turnOffAuthorisationSystem();
                bitstreamService.delete(context, b);
                context.restoreAuthSystemState();

                nBits++;
                assertEquals("Only one bitstream", false, iter.hasNext());
            }
        } catch (SQLException | AuthorizeException ex) {
            throw new RuntimeException(ex);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /***
     * Start workflow.
     * 
     * @throws IOException
     */
    @Test
    public void startWorkflow() throws IOException {
        try {
            context.turnOffAuthorisationSystem();
            EPerson eperson = ePersonService.find(context, epersonUUID);
            Collection collection = collectionService.find(context, collectionUUID);

            // create an item
            WorkspaceItem wi = workspaceItemService.create(context, collection, false);
            Item item = wi.getItem();
            itemService.setMetadataSingleValue(context, item, MetadataSchemaEnum.DC.getName(), "title", null, null,
                    "sample item");
            itemService.update(context, item);
            context.restoreAuthSystemState();

            // create imp_record records
            int impRecordKey = 1;
            ImpRecord impRecord = createImpRecord(context, impRecordKey, ImpRecordService.SEND_THROUGH_WORKFLOW_STATUS,
                    ImpRecordService.INSERT_OR_UPDATE_OPERATION, eperson, collection);

            // create imp_metadatavalue records
            int impMetadatavalueKey = 1;
            createImpMetadatavalue(context, impRecordKey, impMetadatavalueKey, MetadataSchemaEnum.DC.getName(),
                    "contributor", "author", null, "Francesco Cadili");

            // create imp_record_to_item records
            ImpRecordToItem impRecordToItem = createImpRecordToItem(context, impRecord, item);

            // Create a new item
            String argv[] = new String[] { "-E", eperson.getEmail(), "-m", "dc.title", "-s" };

            ItemImportMainOA.main(argv);

            assertNull("Does the workspace item exist?", workspaceItemService.find(context, wi.getID()));
            // assertEquals("Does item exist?", item, itemService.find(context,
            // item.getID()));

            List<XmlWorkflowItem> xwil = workflowItemService.findByCollection(context, collection);
            assertEquals("Ony one workflow item in the collection", 1, xwil.size());
            assertEquals("Is the workflow item the right one?", item, xwil.get(0).getItem());

            XmlWorkflowItem xwi = workflowItemService.findByItem(context, item);
            assertEquals("Is the workflow item the right one?", item, xwi.getItem());

            xwil = workflowItemService.findBySubmitter(context, eperson);
            assertEquals("Ony one workflow item in the collection", 1, xwil.size());
            assertEquals("Is the workflow item the right one?", item, xwil.get(0).getItem());

            List<MetadataValue> metadata = item.getMetadata();
            assertEquals("Two metadata found", 3, metadata.size());
            for (MetadataValue m : metadata) {
                if ("contributor".equals(m.getElement())) {
                    assertEquals("The dc.contibutor.autor is the right one!", m.getValue(), "Francesco Cadili");
                } else if ("title".equals(m.getElement())) {
                    assertEquals("The dc.title value is the right one!", m.getValue(), "sample item");
                } else if ("description".equals(m.getElement()) && "provenance".equals(m.getQualifier())) {
                    assertTrue("The dc.description.provenance value is the right one!",
                            m.getValue().indexOf("Submitted by Francesco Cadili (francesco@sample.ue) on ") == 0);
                    assertTrue("The dc.description.provenance value is the right one!",
                            m.getValue().indexOf("workflow start=Step: reviewstep - action:claimaction") > 0);
                } else {
                    assertTrue("Metadata is not valid.", m == null);
                }
            }
        } catch (SQLException | AuthorizeException ex) {
            throw new RuntimeException(ex);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /***
     * Reinstate an item.
     * 
     * @throws IOException
     */
    @Test
    public void reinstateItem() throws IOException {
        try {
            context.turnOffAuthorisationSystem();
            EPerson eperson = ePersonService.find(context, epersonUUID);
            Collection collection = collectionService.find(context, collectionUUID);

            // create an item
            WorkspaceItem wi = workspaceItemService.create(context, collection, false);
            Item item = wi.getItem();
            item.setOwningCollection(collection);
            itemService.setMetadataSingleValue(context, item, MetadataSchemaEnum.DC.getName(), "title", null, null,
                    "sample item");
            itemService.update(context, item);
            // withdraw the item
            itemService.withdraw(context, item);

            context.restoreAuthSystemState();

            // create imp_record records
            int impRecordKey = 1;
            ImpRecord impRecord = createImpRecord(context, impRecordKey,
                    ImpRecordService.REINSTATE_WITHDRAW_ITEM_STATUS, ImpRecordService.INSERT_OR_UPDATE_OPERATION,
                    eperson, collection);

            // create imp_metadatavalue records
            int impMetadatavalueKey = 1;
            createImpMetadatavalue(context, impRecordKey, impMetadatavalueKey, MetadataSchemaEnum.DC.getName(),
                    "contributor", "author", null, "Francesco Cadili");

            // create imp_record_to_item records
            ImpRecordToItem impRecordToItem = createImpRecordToItem(context, impRecord, item);

            // Create a new item
            String argv[] = new String[] { "-E", eperson.getEmail(), "-m", "dc.title", "-s" };

            ItemImportMainOA.main(argv);

            assertNotNull("Does the workspace item exist?", workspaceItemService.find(context, wi.getID()));
            assertEquals("Does item exist?", item, itemService.find(context, item.getID()));

            assertEquals("Is item withdraw ?", false, item.isWithdrawn());
            assertEquals("Is item archived ?", true, item.isArchived());

            List<MetadataValue> metadata = item.getMetadata();
            assertEquals("Three metadata found", 3, metadata.size());
            for (MetadataValue m : metadata) {
                if ("contributor".equals(m.getElement())) {
                    assertEquals("The dc.contibutor.autor is the right one!", m.getValue(), "Francesco Cadili");
                } else if ("title".equals(m.getElement())) {
                    assertEquals("The dc.title value is the right one!", m.getValue(), "sample item");
                } else if ("description".equals(m.getElement()) && "provenance".equals(m.getQualifier())) {
                    assertTrue("The dc.description.provenance value is the right one!",
                            m.getValue().indexOf("Item reinstated by Francesco Cadili (francesco@sample.ue) on ") == 0);
                } else {
                    assertTrue("Metadata is not valid.", m == null);
                }
            }
        } catch (SQLException | AuthorizeException ex) {
            throw new RuntimeException(ex);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /***
     * Create an ImpRecord.
     * 
     * @param context      The context
     * @param impRecordKey The key
     * @param eperson      The submitter
     * @param collection   The collection
     * @return
     * @throws SQLException
     */
    private ImpRecord createImpRecord(Context context, int impRecordKey, Character status, String operation,
            EPerson eperson, Collection collection) throws SQLException {
        // create imp_record records
        String sourceRecordId = UUID.randomUUID().toString();
        String sourceRef = "TEST";
        ImpRecord impRecord = new ImpRecord();
        impRecord.setImpId(impRecordKey);
        impRecordService.setImpCollection(impRecord, collection);
        impRecordService.setImpEperson(impRecord, eperson);
        impRecord.setImpRecordId(sourceRecordId);
        impRecord.setImpSourceref(sourceRef);
        impRecordService.setStatus(impRecord, status);
        impRecordService.setOperation(impRecord, operation);

        return impRecordService.create(context, impRecord);
    }

    /***
     * Create a Metadata of ImpRecord
     * 
     * @param context             The context
     * @param impRecordKey        The ImpRecord key
     * @param impMetadatavalueKey The impMetadatavalue key
     * @param schema              The schema
     * @param qualifier           The qualifier
     * @param language            The language
     * @param value               The metadata value
     * @return
     * @throws SQLException
     */
    private ImpMetadatavalue createImpMetadatavalue(Context context, int impRecordKey, int impMetadatavalueKey,
            String schema, String element, String qualifier, String language, String value) throws SQLException {
        ImpRecord impRecord = impRecordService.findByID(context, impRecordKey);
        ImpMetadatavalue impMetadatavalue = new ImpMetadatavalue();
        impMetadatavalue.setMetadatavalueId(impMetadatavalueKey);
        impMetadatavalue.setImpRecord(impRecord);
        List<ImpMetadatavalue> metadata = impMetadatavalueService.searchByImpRecordId(context, impRecord);
        impMetadatavalueService.setMetadata(impMetadatavalue, schema, element, qualifier, language, value);
        impMetadatavalue.setMetadataOrder(metadata.size() + 1);

        return impMetadatavalueService.create(context, impMetadatavalue);
    }

    /***
     * Create a bind between a impRecor and an item.
     * 
     * @param context   The context
     * @param impRecord The impRecord
     * @param item      The item
     * @return
     * @throws SQLException
     */
    private ImpRecordToItem createImpRecordToItem(Context context, ImpRecord impRecord, Item item) throws SQLException {
        ImpRecordToItem impRecordToItem = new ImpRecordToItem();
        impRecordToItem.setImpItemId(item.getID());
        impRecordToItem.setImpRecordId(impRecord.getImpRecordId());
        impRecordToItem.setImpSourceref(impRecord.getImpSourceref());
        return impRecordToItemService.create(context, impRecordToItem);
    }

    /***
     * Create a Metadata of ImpRecord
     * 
     * @param context          The context
     * @param impRecordKey     The ImpRecord key
     * @param impBitstreamId   The impBitstream key
     * @param resouce          The resource to upload
     * @param name             The resource name
     * @param description      The resource description
     * @param embargoGroup     The embargo group (or null)
     * @param embargoStartDate The embargo data (or null)
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws URISyntaxException
     */
    private ImpBitstream createImpBitstream(Context context, int impRecordKey, int impBitstreamId, URL resource,
            String name, String description, UUID embargoGroup, String embargoStartDate)
            throws SQLException, IOException, URISyntaxException {
        ImpRecord impRecord = impRecordService.findByID(context, impRecordKey);
        File f = new File(resource.toURI());
        ImpBitstream impBitstream = new ImpBitstream();
        impBitstream.setImpBitstreamId(impBitstreamId);
        impBitstream.setImpRecord(impRecord);
        impBitstream.setDescription(description);
        impBitstream.setBitstreamOrder(1);
        impBitstream.setName(name);
        impBitstream.setFilepath(f.getAbsolutePath());
        try (InputStream is = Files.newInputStream(Paths.get(f.getAbsolutePath()))) {
            String md5 = DigestUtils.md5Hex(is);
            impBitstream.setMd5value(md5);
        }

        if (embargoGroup != null) {
            impBitstream.setEmbargoPolicy(ImpBitstream.USE_GROUP);
            impBitstream.setEmbargoGroup(embargoGroup);
        }
        if (embargoStartDate != null) {
            impBitstream.setEmbargoStartDate(embargoStartDate);
        }
        return impBitstreamService.create(context, impBitstream);
    }

    /***
     * Create a Metadata of ImpRecord
     * 
     * @param context                      The context
     * @param impBitstreamId               The ImpBitstreamRecord key
     * @param impBitstreamMetadatavalueKey The impbitstreamMetadatavalue key
     * @param schema                       The schema
     * @param qualifier                    The qualifier
     * @param language                     The language
     * @param value                        The metadata value
     * @return
     * @throws SQLException
     */
    private ImpBitstreamMetadatavalue createImpBitstreamMetadatavalue(Context context, int impBitstreamId,
            int impBitstreamMetadatavalueKey, String schema, String element, String qualifier, String language,
            String value) throws SQLException {
        ImpBitstream impBitstream = impBitstreamService.findByID(context, impBitstreamId);
        ImpBitstreamMetadatavalue impBitstreamMetadatavalue = new ImpBitstreamMetadatavalue();
        impBitstreamMetadatavalue.setImpBitstreamMetadatavalueId(impBitstreamMetadatavalueKey);
        impBitstreamMetadatavalue.setImpBitstream(impBitstream);
        List<ImpBitstreamMetadatavalue> metadata = impBitstreamMetadatavalueService.searchByImpBitstream(context,
                impBitstream);
        impBitstreamMetadatavalueService.setMetadata(impBitstreamMetadatavalue, schema, element, qualifier, language,
                value);
        impBitstreamMetadatavalue.setMetadataOrder(metadata.size() + 1);

        return impBitstreamMetadatavalueService.create(context, impBitstreamMetadatavalue);
    }
}
