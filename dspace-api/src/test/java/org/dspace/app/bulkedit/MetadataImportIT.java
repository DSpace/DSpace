/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
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
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ScriptService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

public class MetadataImportIT extends AbstractIntegrationTestWithDatabase {

    private final ItemService itemService
            = ContentServiceFactory.getInstance().getItemService();
    private final EPersonService ePersonService
            = EPersonServiceFactory.getInstance().getEPersonService();
    private final RelationshipService relationshipService
            = ContentServiceFactory.getInstance().getRelationshipService();
    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();
    private final MetadataAuthorityService metadataAuthorityService
            = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
    private final ChoiceAuthorityService choiceAuthorityService
            = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();

    private Collection collection;
    private Collection publicationCollection;
    private Collection personCollection;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        this.collection = CollectionBuilder.createCollection(context, community).build();
        this.publicationCollection = CollectionBuilder.createCollection(context, community)
                                           .withEntityType("Publication")
                                           .build();
        this.personCollection = CollectionBuilder.createCollection(context, community)
                                                 .withEntityType("Person")
                                                 .build();

        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType person = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publication, person, "isAuthorOfPublication",
            "isPublicationOfAuthor", 0, 10, 0, 10);
        context.commit();

        context.restoreAuthSystemState();
    }

    @Test
    public void metadataImportTest() throws Exception {
        String[] csv = {"id,collection,dc.title,dc.contributor.author",
            "+," + collection.getHandle() + ",\"Test Import 1\"," + "\"Donald, SmithImported\""};
        performImportScript(csv);
        Item importedItem = findItemByName("Test Import 1");
        assertTrue(itemHasMetadata(importedItem, "dc", "contributor", "author", Item.ANY,
            "Donald, SmithImported", null));
        eperson = ePersonService.findByEmail(context, eperson.getEmail());
        assertEquals(importedItem.getSubmitter(), eperson);

        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, importedItem.getID()));
        context.restoreAuthSystemState();
    }

    @Test
    public void metadataImportIntoCollectionWithEntityTypeWithTemplateEnabledTest() throws Exception {
        String[] csv = {"id,collection,dc.title,dc.contributor.author",
            "+," + publicationCollection.getHandle() + ",\"Test Import 1\"," + "\"Donald, SmithImported\""};
        performImportScript(csv, true);
        Item importedItem = findItemByName("Test Import 1");
        assertTrue(itemHasMetadata(importedItem, "dc", "contributor", "author", Item.ANY,
            "Donald, SmithImported", null));
        assertTrue(itemHasMetadata(importedItem, "dspace", "entity", "type", Item.ANY,
            "Publication", null));
        eperson = ePersonService.findByEmail(context, eperson.getEmail());
        assertEquals(importedItem.getSubmitter(), eperson);

        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, importedItem.getID()));
        context.restoreAuthSystemState();
    }

    @Test
    public void metadataImportIntoCollectionWithEntityTypeWithTemplateDisabledTest() throws Exception {
        String[] csv = {"id,collection,dc.title,dc.contributor.author",
            "+," + publicationCollection.getHandle() + ",\"Test Import 1\"," + "\"Donald, SmithImported\""};
        performImportScript(csv, false);
        Item importedItem = findItemByName("Test Import 1");
        assertTrue(itemHasMetadata(importedItem, "dc", "contributor", "author", Item.ANY,
            "Donald, SmithImported", null));
        assertEquals(0, itemService.getMetadata(importedItem, "dspace", "entity", "type", Item.ANY)
            .size());
        eperson = ePersonService.findByEmail(context, eperson.getEmail());
        assertEquals(importedItem.getSubmitter(), eperson);

        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, importedItem.getID()));
        context.restoreAuthSystemState();
    }

    @Test(expected = ParseException.class)
    public void metadataImportWithoutEPersonParameterTest()
        throws IllegalAccessException, InstantiationException, ParseException {
        String fileLocation = new File(testProps.get("test.importcsv").toString()).getAbsolutePath();
        String[] args = new String[] {"metadata-import", "-f", fileLocation, "-s"};
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
    }

    @Test
    public void metadataImportNewItemRelationshipUUIDReferenceTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, publicationCollection)
                               .withTitle("Publication1").build();
        context.restoreAuthSystemState();

        String[] csv = {"id,collection,dc.title,relation.isPublicationOfAuthor,dspace.entity.type",
            "+," + personCollection.getHandle() + ",\"Test Import 1\"," + item.getID() + ",Person"};
        performImportScript(csv);
        Item importedItem = findItemByName("Test Import 1");

        List<Relationship> relationshipList = relationshipService.findByItem(context, importedItem);
        assertEquals(1, relationshipList.size());
        assertEquals(item.getID(), relationshipList.get(0).getLeftItem().getID());
        assertEquals(importedItem.getID(), relationshipList.get(0).getRightItem().getID());
    }

    @Test
    public void metadataImportExistingItemRelationshipUUIDReferenceTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item personItem = ItemBuilder.createItem(context, personCollection)
            .withPersonIdentifierFirstName("John")
            .withPersonIdentifierLastName("Doe")
            .build();
        Item publicationItem = ItemBuilder.createItem(context, publicationCollection)
                                          .withTitle("Publication1").build();
        context.restoreAuthSystemState();

        String[] csv = {"id,collection,relation.isPublicationOfAuthor",
            personItem.getID() + "," + personCollection.getHandle() + "," + publicationItem.getID()};
        performImportScript(csv);

        List<Relationship> relationshipList = relationshipService.findByItem(context, personItem);
        assertEquals(1, relationshipList.size());
        assertEquals(publicationItem.getID(), relationshipList.get(0).getLeftItem().getID());
        assertEquals(personItem.getID(), relationshipList.get(0).getRightItem().getID());
    }

    @Test
    public void metadataImportExistingItemRelationshipMetadataReferenceTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item personItem = ItemBuilder.createItem(context, personCollection)
            .withPersonIdentifierFirstName("John")
            .withPersonIdentifierLastName("Doe")
            .build();
        Item publicationItem = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Publication1").build();
        context.restoreAuthSystemState();

        String[] csv = {"id,collection,relation.isAuthorOfPublication",
            publicationItem.getID() + "," + publicationCollection.getHandle() + ",person.givenName:John"};
        performImportScript(csv);

        List<Relationship> relationshipList = relationshipService.findByItem(context, personItem);
        assertEquals(1, relationshipList.size());
        assertEquals(publicationItem.getID(), relationshipList.get(0).getLeftItem().getID());
        assertEquals(personItem.getID(), relationshipList.get(0).getRightItem().getID());
    }

    @Test
    public void metadataImportNewItemsRelationshipMetadataReferenceTest() throws Exception {
        String[] csv = {"id,collection,dc.title,relation.isAuthorOfPublication,dspace.entity.type",
            "+," + personCollection.getHandle() + ",\"Person Import 1\",,Person",
            "+," + publicationCollection.getHandle() + ",\"Publication Import 1\",dc.title:Person Import 1,Publication"
        };
        performImportScript(csv);
        Item importedPerson = findItemByName("Person Import 1");
        Item importedPublication = findItemByName("Publication Import 1");

        List<Relationship> relationshipList = relationshipService.findByItem(context, importedPerson);
        assertEquals(1, relationshipList.size());
        assertEquals(importedPublication.getID(), relationshipList.get(0).getLeftItem().getID());
        assertEquals(importedPerson.getID(), relationshipList.get(0).getRightItem().getID());
    }

    @Test
    public void metadataImportNewItemsRelationshipRowNameReferenceTest() throws Exception {
        String[] csv = {"id,collection,dc.title,relation.isAuthorOfPublication,dspace.entity.type,rowName",
            "+," + personCollection.getHandle() + ",\"Person Import 1\",,Person,PersonA",
            "+," + publicationCollection.getHandle() + ",\"Publication Import 1\",rowName:PersonA,Publication," +
                "PublicationA"
        };
        performImportScript(csv);
        Item importedPerson = findItemByName("Person Import 1");
        Item importedPublication = findItemByName("Publication Import 1");

        List<Relationship> relationshipList = relationshipService.findByItem(context, importedPerson);
        assertEquals(1, relationshipList.size());
        assertEquals(importedPublication.getID(), relationshipList.get(0).getLeftItem().getID());
        assertEquals(importedPerson.getID(), relationshipList.get(0).getRightItem().getID());
    }

    @Test
    public void personMetadataImportTest() throws Exception {

        String[] csv = {"id,collection,dc.title,person.birthDate",
            "+," + publicationCollection.getHandle() + ",\"Test Import 2\"," + "2000"};
        performImportScript(csv);
        Item importedItem = findItemByName("Test Import 2");
        assertTrue(
            StringUtils.equals(
                itemService.getMetadata(importedItem, "person", "birthDate", null, Item.ANY)
                           .get(0).getValue(), "2000"));
        context.turnOffAuthorisationSystem();
        itemService.delete(context, importedItem);
        context.restoreAuthSystemState();
    }

    @Test
    public void metadataImportRemovingValueTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context,publicationCollection).withAuthor("TestAuthorToRemove")
            .withTitle("title").build();
        context.restoreAuthSystemState();

        assertTrue(itemHasMetadata(item, "dc", "contributor", "author", Item.ANY,
                "TestAuthorToRemove", null));

        context.commit();

        String[] csv = {"id,collection,dc.title,dc.contributor.author[*]",
            item.getID().toString() + "," + publicationCollection.getHandle() + "," + item.getName() + ","};
        performImportScript(csv);
        item = findItemByName("title");
        assertEquals(0, itemService.getMetadata(item, "dc", "contributor", "author", Item.ANY).size());
    }

    @Test
    public void metadataImportDeleteItemNotAllowedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context,publicationCollection).withTitle("title").build();
        context.restoreAuthSystemState();

        configurationService.setProperty("bulkedit.allowexpunge", false);

        String[] csv = {"id,action", item.getID().toString() + ",expunge"};
        performImportScript(csv);
        item = findItemByName("title");
        assertNotNull(item);
    }

    @Test
    public void metadataImportDeleteItemAllowedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context,publicationCollection).withTitle("title").build();
        context.restoreAuthSystemState();

        configurationService.setProperty("bulkedit.allowexpunge", true);

        String[] csv = {"id,action", item.getID().toString() + ",expunge"};
        performImportScript(csv);
        item = findItemByName("title");
        assertNull(item);
    }

    @Test
    public void metadataImportWithdrawItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context,publicationCollection).withTitle("title").build();
        context.commit();
        context.restoreAuthSystemState();

        assertFalse(item.isWithdrawn());

        String[] csv = {"id,action", item.getID().toString() + ",withdraw"};
        performImportScript(csv);
        item = findItemByName("title", true);
        assertTrue(item.isWithdrawn());
    }

    @Test
    public void metadataImportReinstateItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context,publicationCollection).withTitle("title").build();
        context.commit();
        itemService.withdraw(context, context.reloadEntity(item));
        context.commit();
        context.restoreAuthSystemState();

        assertTrue(context.reloadEntity(item).isWithdrawn());

        String[] csv = {"id,action", item.getID().toString() + ",reinstate"};
        performImportScript(csv);
        item = findItemByName("title", true);
        assertFalse(item.isWithdrawn());
    }

    @Test
    public void metadataImportAuthorityTest() throws Exception {
        enableAuthorAuthorityControl();

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context,publicationCollection)
            .withAuthor("author 1", "authorityKeyToBeChanged", Choices.CF_ACCEPTED)
            .withAuthor("author 2", "authorityKeyToStay", Choices.CF_ACCEPTED)
            .withAuthor("author 3", "authorityKeyUnchanged", Choices.CF_ACCEPTED)
            .withTitle("title").build();
        context.commit();
        context.restoreAuthSystemState();

        String[] csv = {"id,dc.title,dc.contributor.author", item.getID().toString() +
            ",title,\"author 1::authorityKeyChanged||author 2 edited::authorityKeyToStay||" +
            "author 3::authorityKeyUnchanged||author 4::newAuthorityKey\""};
        performImportScript(csv);

        item = findItemByName("title");
        assertTrue(itemHasMetadata(item, "dc", "contributor", "author", Item.ANY,
            "author 1", "authorityKeyChanged"));
        assertTrue(itemHasMetadata(item, "dc", "contributor", "author", Item.ANY,
            "author 2 edited", "authorityKeyToStay"));
        assertTrue(itemHasMetadata(item, "dc", "contributor", "author", Item.ANY,
            "author 3", "authorityKeyUnchanged"));
        assertTrue(itemHasMetadata(item, "dc", "contributor", "author", Item.ANY,
            "author 4", "newAuthorityKey"));
        assertEquals(4, itemService.getMetadata(item, "dc", "contributor", "author", Item.ANY).size());
    }

    private boolean itemHasMetadata(Item item, String schema, String element, String qualifier, String language,
                                    String value, String authority) {
        List<MetadataValue> mdValues = itemService.getMetadata(item, schema, element, qualifier, language);
        for (MetadataValue mdValue : mdValues) {
            String mdValueValue = mdValue.getValue();
            String mdValueAuthority = mdValue.getAuthority();
            if (StringUtils.equals(mdValueValue, value) && StringUtils.equals(mdValueAuthority, authority)) {
                return true;
            }
        }
        return false;
    }

    private Item findItemByName(String name) throws SQLException {
        return findItemByName(name, false);
    }

    private Item findItemByName(String name, boolean unfiltered) throws SQLException {
        Item importedItem = null;
        List<Item> allItems = IteratorUtils.toList(unfiltered ?
            itemService.findAllRegularItems(context) : itemService.findAll(context));
        for (Item item : allItems) {
            if (item.getName().equals(name)) {
                importedItem = item;
            }
        }
        return importedItem;
    }

    public void performImportScript(String[] csv) throws Exception {
        performImportScript(csv, false);
    }

    /**
     * Import mocked CSVs to test item creation behavior, deleting temporary file afterward.
     * @param csv content for test file.
     * @throws java.lang.Exception passed through.
     */
    public void performImportScript(String[] csv, boolean useTemplate) throws Exception {
        File csvFile = File.createTempFile("dspace-test-import", "csv");
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile), "UTF-8"));
        for (String csvLine : csv) {
            out.write(csvLine + "\n");
        }
        out.flush();
        out.close();
        String fileLocation = csvFile.getAbsolutePath();
        try {
            String[] args = new String[] {"metadata-import", "-f", fileLocation, "-e", eperson.getEmail(), "-s"};
            if (useTemplate) {
                args = ArrayUtils.add(args, "-t");
            }
            TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
            ScriptLauncher
                .handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);
        } finally {
            csvFile.delete();
        }
    }

    private void enableAuthorAuthorityControl() {
        configurationService.setProperty("choices.plugin.dc.contributor.author", "SolrAuthorAuthority");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");

        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();
    }

    @Override
    public void destroy() throws Exception {
        // Ensure authority control is removed for authors
        configurationService.setProperty("choices.plugin.dc.contributor.author", null);
        configurationService.setProperty("authority.controlled.dc.contributor.author", "false");
        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();

        super.destroy();
    }
}
