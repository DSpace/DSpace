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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.app.bulkedit.MetadataImportInvalidHeadingException;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ScriptService;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by: Andrew Wood
 * Date: 26 Jul 2019
 */
public class CSVMetadataImportReferenceIT extends AbstractIntegrationTestWithDatabase {

    //Common collection to utilize for test
    private Collection col;
    private Collection col1;
    private Collection col2;

    private final RelationshipService relationshipService
            = ContentServiceFactory.getInstance().getRelationshipService();
    private final ItemService itemService
            = ContentServiceFactory.getInstance().getItemService();

    Community parentCommunity;

    /**
     * Setup testing environment.
     * @throws java.sql.SQLException passed through.
     */
    @Before
    public void setup() throws SQLException {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        col = CollectionBuilder.createCollection(context, parentCommunity)
                               .withName("Collection")
                               .build();

        col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                .withEntityType("Person")
                                .withName("Collection 1")
                                .build();

        col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                .withEntityType("Publication")
                                .withName("Collection 2")
                                .build();

        context.turnOffAuthorisationSystem();

        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType person = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        EntityType project = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();

        RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publication, person, "isAuthorOfPublication",
                                           "isPublicationOfAuthor", 0, null, 0,
                                           null).withCopyToLeft(false).withCopyToRight(true).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publication, project, "isProjectOfPublication",
                                                              "isPublicationOfProject", 0, null, 0,
                                                              null).withCopyToRight(true).build();

        context.restoreAuthSystemState();
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
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,dc.identifier.other",
            "+,Person,," + col1.getHandle() + ",0",
            "+,Publication,dc.identifier.other:0," + col2.getHandle() + ",1"};
        Item[] items = runImport(csv);
        assertRelationship(items[1], items[0], 1, "left", 0);

        // remove created items
        cleanupImportItems(items);
    }

    @Test
    public void testSingleMdRefIntoCollectionWithoutEntityTypeTest() throws Exception {
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,dc.identifier.other",
            "+,Person,," + col.getHandle() + ",0",
            "+,Publication,dc.identifier.other:0," + col.getHandle() + ",1"};
        Item[] items = runImport(csv);
        assertRelationship(items[1], items[0], 1, "left", 0);

        // remove created items
        cleanupImportItems(items);
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
     * Delete the Items in the given array. This method is used for cleanup after using "runImport"
     * @param items items array
     * @throws SQLException
     * @throws IOException
     */
    private void cleanupImportItems(Item[] items) throws SQLException, IOException {
        context.turnOffAuthorisationSystem();
        for (Item item: items) {
            ItemBuilder.deleteItem(item.getID());
        }
        context.restoreAuthSystemState();
    }

    /**
     * Test existence of newly created item with proper relationships defined in the item's metadata via
     * a rowName reference
     */
    @Test
    public void testSingleRowNameRef() throws Exception {
        String[] csv = {"id,dc.title,dspace.entity.type,relation.isAuthorOfPublication,collection,rowName," +
            "dc.identifier.other",
            "+,Test Item 1,Person,," + col1.getHandle() + ",idVal,0",
            "+,Test Item 2,Publication,rowName:idVal," + col2.getHandle() + ",anything,1"};
        Item[] items = runImport(csv);
        assertRelationship(items[1], items[0], 1, "left", 0);
        // remove created items
        cleanupImportItems(items);
    }

    /**
     * Test existence of newly created items with proper relationships defined in the item's metadata via
     * a metadata field and value reference with multiable references
     */
    @Test
    public void testMultiMdRef() throws Exception {
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,dc.identifier.other",
            "+,Person,," + col1.getHandle() + ",0",
            "+,Person,," + col1.getHandle() + ",1",
            "+,Publication,dc.identifier.other:0||dc.identifier.other:1," + col2.getHandle() + ",2"};
        Item[] items = runImport(csv);
        assertRelationship(items[2], items[0], 1, "left", 0);
        assertRelationship(items[2], items[1], 1, "left", 1);
        // remove created items
        cleanupImportItems(items);
    }

    /**
     * Test existence of newly created items with proper relationships defined in the item's metadata via
     * a metadata field and value reference with multiable references
     */
    @Test
    public void testMultiRowNameRef() throws Exception {
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,dc.identifier.other,rowName",
            "+,Person,," + col1.getHandle() + ",0,val1",
            "+,Person,," + col1.getHandle() + ",1,val2",
            "+,Publication,rowName:val1||rowName:val2," + col2.getHandle() + ",2,val3"};
        Item[] items = runImport(csv);
        assertRelationship(items[2], items[0], 1, "left", 0);
        assertRelationship(items[2], items[1], 1, "left", 1);
        // remove created items
        cleanupImportItems(items);
    }

    /**
     * Test existence of newly created items with proper relationships defined in the item's metadata via
     * a UUID reference
     */
    @Test
    public void testSingleUUIDReference() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = ItemBuilder.createItem(context, col1)
                             .withTitle("Author1")
                             .withIssueDate("2017-10-17")
                             .withAuthor("Smith, Donald")
                             .withPersonIdentifierLastName("Smith")
                             .withPersonIdentifierFirstName("Donald")
                             .build();
        context.restoreAuthSystemState();
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,rowName,dc.identifier.other",
            "+,Publication," + person.getID().toString() + "," + col2.getHandle() + ",anything,0"};
        Item[] items = runImport(csv);
        assertRelationship(items[0], person, 1, "left", 0);
        // remove created items
        cleanupImportItems(items);
    }

    /**
     * Test existence of newly created items with proper relationships defined in the item's metadata via
     * a UUID reference with multiable references
     */
    @Test
    public void testMultiUUIDReference() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = ItemBuilder.createItem(context, col1)
                                 .withTitle("Author1")
                                 .withIssueDate("2017-10-17")
                                 .withAuthor("Smith, Donald")
                                 .withPersonIdentifierLastName("Smith")
                                 .withPersonIdentifierFirstName("Donald")
                                 .build();
        Item person2 = ItemBuilder.createItem(context, col1)
                                 .withTitle("Author2")
                                 .withIssueDate("2017-10-17")
                                 .withAuthor("Smith, John")
                                 .withPersonIdentifierLastName("Smith")
                                 .withPersonIdentifierFirstName("John")
                                 .build();
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,rowName,dc.identifier.other",
            "+,Publication," + person.getID().toString() + "||" + person2.getID().toString() + "," +
                col2.getHandle() + ",anything,0"};
        Item[] items = runImport(csv);
        assertRelationship(items[0], person, 1, "left", 0);
        assertRelationship(items[0], person2, 1, "left", 1);
        // remove created items
        cleanupImportItems(items);
    }

    /**
     * Test existence of newly created items with proper relationships defined in the item's metadata via
     * multi metadata references. One archived item and one in the csv.
     */
    @Test
    public void testMultiRefArchivedCsv() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = ItemBuilder.createItem(context, col1)
                                 .withTitle("Person")
                                 .withIssueDate("2017-10-17")
                                 .withAuthor("Smith, Donald")
                                 .withPersonIdentifierLastName("Smith")
                                 .withPersonIdentifierFirstName("Donald")
                                 .build();
        String[] csv = {"id,dc.title,dspace.entity.type,relation.isAuthorOfPublication,collection,rowName," +
            "dc.identifier.other",
            "+,Person2,Person,," + col1.getHandle() + ",idVal,0",
            "+,Pub1,Publication,dc.title:Person||dc.title:Person2," + col2.getHandle() + ",anything,1"};
        context.restoreAuthSystemState();
        Item[] items = runImport(csv);
        assertRelationship(items[1], person, 1, "left", 0);
        assertRelationship(items[1], items[0], 1, "left", 1);
        // remove created items
        cleanupImportItems(items);
    }

    /**
     * Test existence of newly created items with proper relationships defined in the item's metadata via
     * multi mixed references. One archived item, one by metadata reference in the CSV, and one by a rowName reference
     * in the CSV
     */
    @Test
    public void testMultiMixedRefArchivedCsv() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = ItemBuilder.createItem(context, col1)
                                 .withTitle("Person")
                                 .withIssueDate("2017-10-17")
                                 .withAuthor("Smith, Donald")
                                 .withPersonIdentifierLastName("Smith")
                                 .withPersonIdentifierFirstName("Donald")
                                 .build();
        Item person2 = ItemBuilder.createItem(context, col1)
                                 .withTitle("Person2")
                                 .withIssueDate("2017-10-17")
                                 .withAuthor("Smith, John")
                                 .withPersonIdentifierLastName("Smith")
                                 .withPersonIdentifierFirstName("John")
                                 .build();

        context.restoreAuthSystemState();
        String[] csv = {"id,dc.title,dspace.entity.type,relation.isAuthorOfPublication,collection,rowName," +
            "dc.identifier.other",
            "+,Person3,Person,," + col1.getHandle() + ",idVal,0",
            "+,Pub1,Publication," + person.getID() + "||dc.title:Person2||rowName:idVal," +
                col2.getHandle() + ",anything,1"};
        Item[] items = runImport(csv);
        assertRelationship(items[1], person, 1, "left", 0);
        assertRelationship(items[1], person2, 1, "left", 1);
        assertRelationship(items[1], items[0], 1, "left", 2);
        // remove created items
        cleanupImportItems(items);
    }

    /**
     * Test existence of newly created items with proper relationships defined in the item's metadata via
     * a metadata reference with a special char utilized in the metadata import script
     */
    @Test
    public void testRefWithSpecialChar() throws Exception {
        String[] csv = {"id,dc.title,dspace.entity.type,relation.isAuthorOfPublication,collection,rowName," +
            "dc.identifier.other",
            "+,Person:,Person,," + col1.getHandle() + ",idVal,0",
            "+,Pub1,Publication,dc.title:Person:," + col2.getHandle() + ",anything,1"};
        Item[] items = runImport(csv);
        assertRelationship(items[1], items[0], 1, "left", 0);
        // remove created items
        cleanupImportItems(items);
    }

    /**
     * Test failure when referring to item by non unique metadata in the csv file.
     */
    @Test(expected = MetadataImportException.class)
    public void testNonUniqueMDRefInCsv() throws Exception {
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,dc.identifier.other",
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
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,dc.identifier.other,rowName",
            "+,Person,," + col1.getHandle() + ",1,value",
            "+,Person,," + col1.getHandle() + ",1,value",
            "+,Publication,rowName:value," + col1.getHandle() + ",2"};
        performImportScript(csv, true);
    }

    /**
     * Test failure when referring to item by non unique metadata in the database.
     * @throws java.lang.Exception passed through.
     */
    @Test(expected = MetadataImportException.class)
    public void testNonUniqueMDRefInDb() throws Exception {
        context.turnOffAuthorisationSystem();
        ItemBuilder.createItem(context, col1)
                                 .withTitle("Person")
                                 .withIssueDate("2017-10-17")
                                 .withAuthor("Smith, Donald")
                                 .withPersonIdentifierLastName("Smith")
                                 .withPersonIdentifierFirstName("Donald")
                                 .withIdentifierOther("1")
                                 .build();
        ItemBuilder.createItem(context, col1)
                                 .withTitle("Person2")
                                 .withIssueDate("2017-10-17")
                                 .withAuthor("Smith, John")
                                 .withPersonIdentifierLastName("Smith")
                                 .withPersonIdentifierFirstName("John")
                                 .withIdentifierOther("1")
                                 .build();

        context.restoreAuthSystemState();
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,dc.identifier.other",
            "+,Publication,dc.identifier.other:1," + col1.getHandle() + ",2"};
        performImportScript(csv, true);
    }

    /**
     * Test failure when referring to item by non unique metadata in the csv and database.
     */
    @Test(expected = MetadataImportException.class)
    public void testNonUniqueMDRefInBoth() throws Exception {
        context.turnOffAuthorisationSystem();
        ItemBuilder.createItem(context, col1)
                                 .withTitle("Person")
                                 .withIssueDate("2017-10-17")
                                 .withAuthor("Smith, Donald")
                                 .withPersonIdentifierLastName("Smith")
                                 .withPersonIdentifierFirstName("Donald")
                                 .withIdentifierOther("1")
                                 .build();
        context.restoreAuthSystemState();
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,dc.identifier.other",
            "+,Person,," + col1.getHandle() + ",1",
            "+,Publication,dc.identifier.other:1," + col1.getHandle() + ",2"};
        performImportScript(csv, true);
    }

    /**
     * Test failure when referring to item by metadata that does not exist in the relation column
     */
    @Test(expected = Exception.class)
    public void testNonExistMdRef() throws Exception {
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,dc.identifier.other",
            "+,Person,," + col1.getHandle() + ",1",
            "+,Publication,dc.identifier.other:8675309," + col1.getHandle() + ",2"};
        performImportScript(csv, false);
    }

    /**
     * Test failure when referring to an item in the CSV that hasn't been created yet due to it's order in the CSV
     */
    @Test(expected = Exception.class)
    public void testCSVImportWrongOrder() throws Exception {
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,dc.identifier.other",
            "+,Publication,dc.identifier.other:8675309," + col1.getHandle() + ",2",
            "+,Person,," + col1.getHandle() + ",8675309",};
        performImportScript(csv, false);
    }

    /**
     * Test failure when referring to an item in the CSV that hasn't been created yet due to it's order in the CSV
     */
    @Test(expected = Exception.class)
    public void testCSVImportWrongOrderRowName() throws Exception {
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,dc.identifier.other,rowName",
            "+,Publication,rowName:row2," + col1.getHandle() + ",2,row1",
            "+,Person,," + col1.getHandle() + ",8675309,row2",};
        performImportScript(csv, false);
    }

    /**
     * Test relationship validation with invalid relationship definition
     */
    @Test(expected = MetadataImportException.class)
    public void testCSVImportInvalidRelationship() throws Exception {
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,rowName",
            "+,Publication,," + col1.getHandle() + ",row1",
            "+,Unit,rowName:row1," + col1.getHandle() + ",row2",};
        performImportScript(csv, true);
    }

    /**
     * Test relationship validation with invalid relationship definition and with an archived origin referrer.
     */
    @Test(expected = MetadataImportInvalidHeadingException.class)
    public void testInvalidRelationshipArchivedOrigin() throws Exception {
        context.turnOffAuthorisationSystem();

        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("Parent Community")
                                                  .build();

        Collection orgUnitCollection = CollectionBuilder.createCollection(context, rootCommunity)
                                                        .withEntityType("OrgUnit")
                                                        .withName("Collection 1")
                                                        .build();

        Item testItem = ItemBuilder.createItem(context, orgUnitCollection)
                                 .withTitle("OrgUnit")
                                 .withIssueDate("2017-10-17")
                                 .build();

        context.restoreAuthSystemState();
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,rowName",
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

        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("Parent Community")
                                                  .build();

        Collection orgUnitCollection = CollectionBuilder.createCollection(context, rootCommunity)
                                                        .withEntityType("OrgUnit")
                                                        .withName("Collection 1")
                                                        .build();

        Item testItem = ItemBuilder.createItem(context, orgUnitCollection)
                                   .withTitle("OrgUnit")
                                   .withIssueDate("2017-10-17")
                                   .build();

        context.restoreAuthSystemState();
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,rowName",
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

        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("Parent Community")
                                                  .build();

        Collection publicationCollection = CollectionBuilder.createCollection(context, rootCommunity)
                                                            .withEntityType("Publication")
                                                            .withName("Collection 1")
                                                            .build();

        Collection projectCollection = CollectionBuilder.createCollection(context, rootCommunity)
                                                        .withEntityType("Project")
                                                        .withName("Collection 1")
                                                        .build();

        Item testItem = ItemBuilder.createItem(context, col1)
                                 .withTitle("Person")
                                 .withIssueDate("2017-10-17")
                                 .withAuthor("Smith, Donald")
                                 .withPersonIdentifierLastName("Smith")
                                 .withPersonIdentifierFirstName("Donald")
                                 .withIdentifierOther("testItemOne")
                                 .build();


        Item testItem2 = ItemBuilder.createItem(context, publicationCollection)
                                 .withTitle("Publication")
                                 .withIssueDate("2017-10-17")
                                 .withIdentifierOther("testItemTwo")
                                 .build();


        Item testItem3 = ItemBuilder.createItem(context, projectCollection)
                                 .withTitle("Project")
                                 .withIssueDate("2017-10-17")
                                 .withIdentifierOther("testItemThree")
                                 .build();


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
        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,dc.identifier.other,rowName",
            "+,Person,," + col1.getHandle() + ",0,value",
            "+,Publication,rowName:value," + col2.getHandle() + ",1,1",
            "+,Publication,rowName:value," + col2.getHandle() + ",2,2"};
        Item[] items = runImport(csv);
        assertRelationship(items[1], items[0], 1, "left", 0);
        assertRelationship(items[2], items[0], 1, "left", 0);
        // remove created items
        cleanupImportItems(items);
    }

    @Test
    public void testRelationToVirtualDataInReferences() throws Exception {

        Item testItem = ItemBuilder.createItem(context, col1)
                                   .withTitle("Person")
                                   .withIssueDate("2017-10-17")
                                   .withAuthor("Smith, Donald")
                                   .withPersonIdentifierLastName("Smith")
                                   .withPersonIdentifierFirstName("Donald")
                                   .withIdentifierOther("testItemOne")
                                   .build();


        String[] csv = {"id,dspace.entity.type,relation.isAuthorOfPublication,collection,dc.identifier.other,rowName",
            "+,Publication," + testItem.getID() + "::virtual::4::600," + col2.getHandle() + ",0,1"};
        Item[] items = runImport(csv);
        assertRelationship(items[0], testItem, 1, "left", 0);
    }

    /**
     * Test relationship validation with invalid relationship definition by incorrect typeName usage
     */
    @Test(expected = MetadataImportException.class)
    public void testInvalidTypeNameDefined() throws Exception {
        context.turnOffAuthorisationSystem();

        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("Parent Community")
                                                  .build();

        Collection publicationCollection = CollectionBuilder.createCollection(context, rootCommunity)
                                                            .withEntityType("Publication")
                                                            .withName("Collection 1")
                                                            .build();

        Item testItem = ItemBuilder.createItem(context, publicationCollection)
                                 .withTitle("Publication")
                                 .withIssueDate("2017-10-17")
                                 .build();

        context.restoreAuthSystemState();
        String[] csv = {"id,collection,dspace.entity.type,dc.title," +
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
                if (DSpaceRunnable.StepResult.Continue
                        .equals(script.initialize(args, testDSpaceRunnableHandler, null))) {
                    script.run();
                }
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
