/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.csv;

import static junit.framework.TestCase.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.dspace.AbstractIntegrationTest;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.app.bulkedit.MetadataImportInvalidHeadingException;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ScriptService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by: Andrew Wood
 * Date: 26 Jul 2019
 */
public class CSVMetadataImportReferenceIT extends AbstractIntegrationTest {

    //Common collection to utilize for test
    private Collection col1;

    private RelationshipService relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    private InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    private EntityTypeService entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
    private RelationshipTypeService relationshipTypeService = ContentServiceFactory.getInstance()
                                                                                   .getRelationshipTypeService();


    Community parentCommunity;

    /**
     * Setup testing enviorment
     */
    @Before
    public void setup() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        parentCommunity = communityService.create(null, context);
        communityService.addMetadata(context, parentCommunity, "dc", "title", null, null, "Parent Community");

        col1 = collectionService.create(context, parentCommunity);
        collectionService.addMetadata(context, col1, "dc", "title", null, null, "Collection 1");

        if (entityTypeService.findAll(context).size() > 0) {
            //Don't initialize the setup more than once
            return;
        }

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType = entityTypeService.create(context, "Publication");
        EntityType personEntityType = entityTypeService.create(context, "Person");
        EntityType orgUnitType = entityTypeService.create(context, "OrgUnit");
        EntityType projectType = entityTypeService.create(context, "Project");
        relationshipTypeService
            .create(context, publicationEntityType, personEntityType, "isAuthorOfPublication", "isPublicationOfAuthor",
                    null, null, null, null);
        relationshipTypeService
            .create(context, publicationEntityType, projectType, "isProjectOfPublication", "isPublicationOfProject", 0,
                    null, 0, null, false, true);

        context.restoreAuthSystemState();

        context.restoreAuthSystemState();
    }

    @After
    @Override
    public void destroy() {
        context.turnOffAuthorisationSystem();
        try {
            List<Relationship> relationships = relationshipService.findAll(context);
            for (Relationship relationship : relationships) {
                relationshipService.delete(context, relationship);
            }
            Iterator<Item> itemIterator = itemService.findAll(context);
            while (itemIterator.hasNext()) {
                Item item = itemIterator.next();
                itemService.delete(context, item);
            }
            List<Collection> collections = collectionService.findAll(context);
            for (Collection collection : collections) {
                collectionService.delete(context, collection);
            }
            List<Community> communities = communityService.findAll(context);
            for (Community community : communities) {
                communityService.delete(context, community);
            }
            context.commit();
        } catch (Exception e) {
            String t = "";
        }

        context.restoreAuthSystemState();
        col1 = null;
        parentCommunity = null;
        try {
            super.destroy();
        } catch (Exception e) {
            String t = "";
        }
    }

    /**
     * Helper method to validate relationships
     * @param leftItem the left item in a known relationship
     * @param rightItem the right item in a known relationship
     * @param expectedCount expected relationship count for a known relationship
     * @param placeDirection direction of subjects relationship(s)
     * @param placeCount Expected place of subject's relationship
     */
    private void assertRelationship(Item leftItem, Item rightItem, int expectedCount,
                                    String placeDirection, int placeCount) throws SQLException {
        List<Relationship> rels = relationshipService.findByItem(context, rightItem);
        Relationship relationship = null;
        int foundCount = 0;
        for (Relationship rel : rels) {
            if (rel.getRightItem().getID().equals(rightItem.getID())
                && rel.getLeftItem().getID().equals(leftItem.getID())) {
                foundCount++;
                relationship = rel;
            }
        }
        if (placeDirection.equalsIgnoreCase("left")) {
            assertEquals(relationship.getLeftPlace(), placeCount);
        } else {
            assertEquals(relationship.getRightPlace(), placeCount);
        }
        assertEquals(expectedCount, foundCount);
    }

    /**
     * Test existence of newly created items with proper relationships defined in mets via
     * a metadata field and value reference
     */
    @Test
    public void testSingleMdRef() throws Exception {
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,dc.identifier.other",
            "+,Person,," + col1.getHandle() + ",0",
            "+,Publication,dc.identifier.other:0," + col1.getHandle() + ",1"};
        Item[] items = runImport(csv);
        assertRelationship(items[1], items[0], 1, "left", 0);
    }

    /**
     * return an array of items given a representation of a CSV as a string array
     *
     * @param csvLines A representation of a CSV as a string array
     *
     * @return an array of items
     */
    private Item[] runImport(String[] csvLines) throws Exception {
        performImportScript(csvLines, false);
        Item[] items = new Item[csvLines.length - 1];
        for (int i = 0; i < items.length; i++) {
            items[i] = itemService.findByIdOrLegacyId(context, getUUIDByIdentifierOther("" + i).toString());
        }
        return items;
    }

    /**
     * Test existence of newly created item with proper relationships defined in the item's metadata via
     * a rowName reference
     */
    @Test
    public void testSingleRowNameRef() throws Exception {
        String[] csv = {"id,dc.title,relationship.type,relation.isAuthorOfPublication,collection,rowName," +
            "dc.identifier.other",
            "+,Test Item 1,Person,," + col1.getHandle() + ",idVal,0",
            "+,Test Item 2,Publication,rowName:idVal," + col1.getHandle() + ",anything,1"};
        Item[] items = runImport(csv);
        assertRelationship(items[1], items[0], 1, "left", 0);
    }

    /**
     * Test existence of newly created items with proper relationships defined in the item's metadata via
     * a metadata field and value reference with multiable references
     */
    @Test
    public void testMultiMdRef() throws Exception {
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,dc.identifier.other",
            "+,Person,," + col1.getHandle() + ",0",
            "+,Person,," + col1.getHandle() + ",1",
            "+,Publication,dc.identifier.other:0||dc.identifier.other:1," + col1.getHandle() + ",2"};
        Item[] items = runImport(csv);
        assertRelationship(items[2], items[0], 1, "left", 0);
        assertRelationship(items[2], items[1], 1, "left", 1);
    }

    /**
     * Test existence of newly created items with proper relationships defined in the item's metadata via
     * a metadata field and value reference with multiable references
     */
    @Test
    public void testMultiRowNameRef() throws Exception {
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,dc.identifier.other,rowName",
            "+,Person,," + col1.getHandle() + ",0,val1",
            "+,Person,," + col1.getHandle() + ",1,val2",
            "+,Publication,rowName:val1||rowName:val2," + col1.getHandle() + ",2,val3"};
        Item[] items = runImport(csv);
        assertRelationship(items[2], items[0], 1, "left", 0);
        assertRelationship(items[2], items[1], 1, "left", 1);
    }

    /**
     * Test existence of newly created items with proper relationships defined in the item's metadata via
     * a UUID reference
     */
    @Test
    public void testSingleUUIDReference() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = workspaceItemService.create(context, col1, false);
        Item person = installItemService.installItem(context, workspaceItem);
        itemService.addMetadata(context, person, "relationship", "type", null, null, "Person");
        itemService.update(context, person);
        context.restoreAuthSystemState();
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,rowName,dc.identifier.other",
            "+,Publication," + person.getID().toString() + "," + col1.getHandle() + ",anything,0"};
        Item[] items = runImport(csv);
        assertRelationship(items[0], person, 1, "left", 0);
    }

    /**
     * Test existence of newly created items with proper relationships defined in the item's metadata via
     * a UUID reference with multiable references
     */
    @Test
    public void testMultiUUIDReference() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = workspaceItemService.create(context, col1, false);
        Item person = installItemService.installItem(context, workspaceItem);
        itemService.addMetadata(context, person, "relationship", "type", null, null, "Person");
        itemService.update(context, person);
        WorkspaceItem workspaceItem2 = workspaceItemService.create(context, col1, false);
        Item person2 = installItemService.installItem(context, workspaceItem2);
        itemService.addMetadata(context, person2, "relationship", "type", null, null, "Person");
        itemService.update(context, person2);
        context.restoreAuthSystemState();
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,rowName,dc.identifier.other",
            "+,Publication," + person.getID().toString() + "||" + person2.getID().toString() + "," +
                col1.getHandle() + ",anything,0"};
        Item[] items = runImport(csv);
        assertRelationship(items[0], person, 1, "left", 0);
        assertRelationship(items[0], person2, 1, "left", 1);
    }

    /**
     * Test existence of newly created items with proper relationships defined in the item's metadata via
     * multi metadata references. One archived item and one in the csv.
     */
    @Test
    public void testMultiRefArchivedCsv() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = workspaceItemService.create(context, col1, false);
        Item person = installItemService.installItem(context, workspaceItem);
        itemService.addMetadata(context, person, "relationship", "type", null, null, "Person");
        itemService.addMetadata(context, person, "dc", "title", null, null, "Person");
        itemService.update(context, person);

        String[] csv = {"id,dc.title,relationship.type,relation.isAuthorOfPublication,collection,rowName," +
            "dc.identifier.other",
            "+,Person2,Person,," + col1.getHandle() + ",idVal,0",
            "+,Pub1,Publication,dc.title:Person||dc.title:Person2," + col1.getHandle() + ",anything,1"};
        context.restoreAuthSystemState();
        Item[] items = runImport(csv);
        assertRelationship(items[1], person, 1, "left", 0);
        assertRelationship(items[1], items[0], 1, "left", 1);
    }

    /**
     * Test existence of newly created items with proper relationships defined in the item's metadata via
     * multi mixed references. One archived item, one by metadata reference in the CSV, and one by a rowName reference
     * in the CSV
     */
    @Test
    public void testMultiMixedRefArchivedCsv() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = workspaceItemService.create(context, col1, false);
        Item person = installItemService.installItem(context, workspaceItem);
        itemService.addMetadata(context, person, "relationship", "type", null, null, "Person");
        itemService.addMetadata(context, person, "dc", "title", null, null, "Person");
        itemService.update(context, person);
        WorkspaceItem workspaceItem2 = workspaceItemService.create(context, col1, false);
        Item person2 = installItemService.installItem(context, workspaceItem2);
        itemService.addMetadata(context, person2, "relationship", "type", null, null, "Person");
        itemService.addMetadata(context, person2, "dc", "title", null, null, "Person2");
        itemService.update(context, person2);

        context.restoreAuthSystemState();
        String[] csv = {"id,dc.title,relationship.type,relation.isAuthorOfPublication,collection,rowName," +
            "dc.identifier.other",
            "+,Person3,Person,," + col1.getHandle() + ",idVal,0",
            "+,Pub1,Publication," + person.getID() + "||dc.title:Person2||rowName:idVal," +
                col1.getHandle() + ",anything,1"};
        Item[] items = runImport(csv);
        assertRelationship(items[1], person, 1, "left", 0);
        assertRelationship(items[1], person2, 1, "left", 1);
        assertRelationship(items[1], items[0], 1, "left", 2);
    }

    /**
     * Test existence of newly created items with proper relationships defined in the item's metadata via
     * a metadata reference with a special char utilized in the metadata import script
     */
    @Test
    public void testRefWithSpecialChar() throws Exception {
        String[] csv = {"id,dc.title,relationship.type,relation.isAuthorOfPublication,collection,rowName," +
            "dc.identifier.other",
            "+,Person:,Person,," + col1.getHandle() + ",idVal,0",
            "+,Pub1,Publication,dc.title:Person:," + col1.getHandle() + ",anything,1"};
        Item[] items = runImport(csv);
        assertRelationship(items[1], items[0], 1, "left", 0);
    }

    /**
     * Test failure when referring to item by non unique metadata in the csv file.
     */
    @Test(expected = MetadataImportException.class)
    public void testNonUniqueMDRefInCsv() throws Exception {
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,dc.identifier.other",
            "+,Person,," + col1.getHandle() + ",1",
            "+,Person,," + col1.getHandle() + ",1",
            "+,Publication,dc.identifier.other:1," + col1.getHandle() + ",2"};
        performImportScript(csv, true);
    }

    /**
     * Test failure when referring to item by non unique metadata in the csv file.
     */
    @Test(expected = MetadataImportException.class)
    public void testNonUniqueRowName() throws Exception {
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,dc.identifier.other,rowName",
            "+,Person,," + col1.getHandle() + ",1,value",
            "+,Person,," + col1.getHandle() + ",1,value",
            "+,Publication,rowName:value," + col1.getHandle() + ",2"};
        performImportScript(csv, true);
    }

    /**
     * Test failure when referring to item by non unique metadata in the database.
     */
    @Test(expected = MetadataImportException.class)
    public void testNonUniqueMDRefInDb() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = workspaceItemService.create(context, col1, false);
        Item person = installItemService.installItem(context, workspaceItem);
        itemService.addMetadata(context, person, "relationship", "type", null, null, "Person");
        itemService.addMetadata(context, person, "dc", "identifier", "other", null, "1");
        itemService.update(context, person);
        WorkspaceItem workspaceItem2 = workspaceItemService.create(context, col1, false);
        Item person2 = installItemService.installItem(context, workspaceItem2);
        itemService.addMetadata(context, person2, "relationship", "type", null, null, "Person");
        itemService.addMetadata(context, person2, "dc", "identifier", "other", null, "1");
        itemService.update(context, person2);

        context.restoreAuthSystemState();
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,dc.identifier.other",
            "+,Publication,dc.identifier.other:1," + col1.getHandle() + ",2"};
        performImportScript(csv, true);
    }

    /**
     * Test failure when referring to item by non unique metadata in the csv and database.
     */
    @Test(expected = MetadataImportException.class)
    public void testNonUniqueMDRefInBoth() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = workspaceItemService.create(context, col1, false);
        Item person = installItemService.installItem(context, workspaceItem);
        itemService.addMetadata(context, person, "relationship", "type", null, null, "Person");
        itemService.addMetadata(context, person, "dc", "identifier", "other", null, "1");
        itemService.update(context, person);
        context.restoreAuthSystemState();
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,dc.identifier.other",
            "+,Person,," + col1.getHandle() + ",1",
            "+,Publication,dc.identifier.other:1," + col1.getHandle() + ",2"};
        performImportScript(csv, true);
    }

    /**
     * Test failure when refering to item by metadata that does not exist in the relation column
     */
    @Test(expected = Exception.class)
    public void testNonExistMdRef() throws Exception {
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,dc.identifier.other",
            "+,Person,," + col1.getHandle() + ",1",
            "+,Publication,dc.identifier.other:8675309," + col1.getHandle() + ",2"};
        performImportScript(csv, false);
    }

    /**
     * Test failure when refering to an item in the CSV that hasn't been created yet due to it's order in the CSV
     */
    @Test(expected = Exception.class)
    public void testCSVImportWrongOrder() throws Exception {
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,dc.identifier.other",
            "+,Publication,dc.identifier.other:8675309," + col1.getHandle() + ",2",
            "+,Person,," + col1.getHandle() + ",8675309",};
        performImportScript(csv, false);
    }

    /**
     * Test failure when refering to an item in the CSV that hasn't been created yet due to it's order in the CSV
     */
    @Test(expected = Exception.class)
    public void testCSVImportWrongOrderRowName() throws Exception {
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,dc.identifier.other,rowName",
            "+,Publication,rowName:row2," + col1.getHandle() + ",2,row1",
            "+,Person,," + col1.getHandle() + ",8675309,row2",};
        performImportScript(csv, false);
    }

    /**
     * Test relationship validation with invalid relationship definition
     */
    @Test(expected = MetadataImportException.class)
    public void testCSVImportInvalidRelationship() throws Exception {
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,rowName",
            "+,Publication,," + col1.getHandle() + ",row1",
            "+,Unit,rowName:row1," + col1.getHandle() + ",row2",};
        performImportScript(csv, true);
    }

    /**
     * Test relationship validation with invalid relationship definition and with an archived origin referer
     */
    @Test(expected = MetadataImportInvalidHeadingException.class)
    public void testInvalidRelationshipArchivedOrigin() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = workspaceItemService.create(context, col1, false);
        Item testItem = installItemService.installItem(context, workspaceItem);
        itemService.addMetadata(context, testItem, "relationship", "type", null, null, "OrgUnit");
        itemService.update(context, testItem);
        context.restoreAuthSystemState();
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,rowName",
            "+,Person,," + col1.getHandle() + ",1" +
                testItem.getID().toString() + ",,rowName:1," + col1.getHandle() + ",2"};
        performImportScript(csv, false);
    }

    /**
     * Test relationship validation with invalid relationship definition and with archived target reference
     */
    @Test(expected = MetadataImportInvalidHeadingException.class)
    public void testInvalidRelationshipArchivedTarget() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = workspaceItemService.create(context, col1, false);
        Item testItem = installItemService.installItem(context, workspaceItem);
        itemService.addMetadata(context, testItem, "relationship", "type", null, null, "OrgUnit");
        itemService.update(context, testItem);
        context.restoreAuthSystemState();
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,rowName",
            testItem.getID().toString() + ",Person,," + col1.getHandle() + ",1" +
                "+,OrgUnit,rowName:1," + col1.getHandle() + ",2"};
        performImportScript(csv, false);
    }

    /**
     * Test relationship validation against archived items with pre-existing relationship types
     */
    @Test
    public void testValidRelationshipNoDefinedTypesInCSV() throws Exception {
        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem = workspaceItemService.create(context, col1, false);
        Item testItem = installItemService.installItem(context, workspaceItem);
        itemService.addMetadata(context, testItem, "relationship", "type", null, null, "Person");
        itemService.addMetadata(context, testItem, "dc", "identifier", "other", null, "testItemOne");
        itemService.update(context, testItem);


        WorkspaceItem workspaceItem2 = workspaceItemService.create(context, col1, false);
        Item testItem2 = installItemService.installItem(context, workspaceItem2);
        itemService.addMetadata(context, testItem2, "relationship", "type", null, null, "Publication");
        itemService.addMetadata(context, testItem2, "dc", "identifier", "other", null, "testItemTwo");
        itemService.update(context, testItem2);


        WorkspaceItem workspaceItem3 = workspaceItemService.create(context, col1, false);
        Item testItem3 = installItemService.installItem(context, workspaceItem3);
        itemService.addMetadata(context, testItem3, "relationship", "type", null, null, "Project");
        itemService.addMetadata(context, testItem3, "dc", "identifier", "other", null, "testItemThree");
        itemService.update(context, testItem3);


        context.restoreAuthSystemState();
        String[] csv = {"id,relation.isAuthorOfPublication,relation.isPublicationOfProject,collection",
            testItem.getID().toString() + ",,," + col1.getHandle(),
            testItem2.getID().toString() + ",dc.identifier.other:testItemOne,," + col1.getHandle(),
            testItem3.getID().toString() + ",,dc.identifier.other:testItemTwo," + col1.getHandle()};
        performImportScript(csv, false);
        assertRelationship(testItem2, testItem, 1, "left", 0);
        assertRelationship(testItem2, testItem3, 1, "left", 0);
    }

    /**
     * Test relationship validation with valid relationship definition using the same rowName more than once
     */
    @Test
    public void testDuplicateRowNameReferences() throws Exception {
        String[] csv = {"id,relationship.type,relation.isAuthorOfPublication,collection,dc.identifier.other,rowName",
            "+,Person,," + col1.getHandle() + ",0,value",
            "+,Publication,rowName:value," + col1.getHandle() + ",1,1",
            "+,Publication,rowName:value," + col1.getHandle() + ",2,2"};
        Item[] items = runImport(csv);
        assertRelationship(items[1], items[0], 1, "left", 0);
        assertRelationship(items[2], items[0], 1, "left", 0);
    }

    /**
     * Test relationship validation with invalid relationship definition by incorrect typeName usage
     */
    @Test(expected = MetadataImportException.class)
    public void testInvalidTypeNameDefined() throws Exception {
        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem = workspaceItemService.create(context, col1, false);
        Item testItem = installItemService.installItem(context, workspaceItem);
        itemService.addMetadata(context, testItem, "relationship", "type", null, null, "Publication");
        itemService.update(context, testItem);

        context.restoreAuthSystemState();
        String[] csv = {"id,collection,relationship.type,dc.title," +
            "relation.isProjectOfPublication,relation.isPublicationOfProject",
            "+," + col1.getHandle() + ",Project,Title," +
                testItem.getID().toString() + "," + testItem.getID().toString()};
        performImportScript(csv, true);
    }

    /**
     * Import mocked CSVs to test item creation behavior, deleting temporary file afterward.
     */
    public int performImportScript(String[] csv, boolean validateOnly) throws Exception {
        File csvFile = File.createTempFile("dspace-test-import", "csv");
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile), "UTF-8"));
        for (String csvLine : csv) {
            out.write(csvLine + "\n");
        }
        out.flush();
        out.close();
        String fileLocation = csvFile.getAbsolutePath();
        try {
            String[] args = null;
            if (validateOnly) {
                args = new String[] {"metadata-import", "-f", fileLocation, "-e", eperson.getEmail(), "-s", "-v"};
            } else {
                args = new String[] {"metadata-import", "-f", fileLocation, "-e", eperson.getEmail(), "-s",};
            }
            TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

            ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();
            ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(args[0]);

            DSpaceRunnable script = null;
            if (scriptConfiguration != null) {
                script = scriptService.createDSpaceRunnableForScriptConfiguration(scriptConfiguration);
            }
            if (script != null) {
                script.initialize(args, testDSpaceRunnableHandler, null);
                script.run();
            }
            if (testDSpaceRunnableHandler.getException() != null) {
                throw testDSpaceRunnableHandler.getException();
            }
        } finally {
            csvFile.delete();
        }
        return 0;
    }

    /**
     * Return UUID given a dc.identifier.other value
     *
     * @param value the value of the dc.identifier.other to query for
     *
     * @return first retrived UUID
     */
    private UUID getUUIDByIdentifierOther(String value) throws Exception {
        ArrayList<UUID> uuidList = new ArrayList<>();
        MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
        MetadataFieldService metadataFieldService =
            ContentServiceFactory.getInstance().getMetadataFieldService();
        MetadataField mfo = metadataFieldService.findByElement(context, "dc", "identifier", "other");
        Iterator<MetadataValue> mdv = metadataValueService.findByFieldAndValue(context, mfo, value);
        while (mdv.hasNext()) {
            MetadataValue mdvVal = mdv.next();
            uuidList.add(mdvVal.getDSpaceObject().getID());
        }
        return uuidList.get(0);
    }


}
