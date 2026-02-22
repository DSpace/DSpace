/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.consumer;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.app.matcher.MetadataValueMatcher.withNoPlace;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.StringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.MetadataFieldBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ReloadableEntity;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

public class ItemEnhancerConsumerIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService;

    private Collection collection;

    private PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

    private ChoiceAuthorityService choiceAuthorityService = ContentAuthorityServiceFactory
        .getInstance().getChoiceAuthorityService();

    private MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory
        .getInstance().getMetadataAuthorityService();

    private ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    @Before
    public void setup() throws Exception {

        itemService = ContentServiceFactory.getInstance().getItemService();

        // Common authority configuration for most tests
        choiceAuthorityService.getChoiceAuthoritiesNames();
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .build();
        context.restoreAuthSystemState();

    }

    @Test
    public void testSingleMetadataValueEnhancement() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .build();

        String personId = person.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Walter White", personId)
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(10));
        assertThat(metadataValues, hasItem(with("cris.virtual.department", "4Science")));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.department", personId)));
        assertThat(metadataValues, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.orcid", personId)));


        MetadataValue virtualField = getFirstMetadataValue(publication, "cris.virtual.department");
        MetadataValue virtualSourceField = getFirstMetadataValue(publication, "cris.virtualsource.department");

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, publication, "dc", "subject", null, null, "Test");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(11));
        assertThat(metadataValues, hasItem(with("dc.contributor.author", "Walter White", personId, 600)));
        assertThat(metadataValues, hasItem(with("cris.virtual.department", "4Science")));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.department", personId)));
        assertThat(metadataValues, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.orcid", personId)));

        assertThat(virtualField, equalTo(getFirstMetadataValue(publication, "cris.virtual.department")));
        assertThat(virtualSourceField, equalTo(getFirstMetadataValue(publication, "cris.virtualsource.department")));

    }

    @Test
    public void testManyMetadataValuesEnhancement() throws Exception {
        // This test needs additional editor authority control
        configurationService.setProperty("authority.controlled.dc.contributor.editor", "true");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        Item person1 = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .build();

        Item person2 = ItemBuilder.createItem(context, collection)
            .withTitle("John Smith")
            .build();

        Item person3 = ItemBuilder.createItem(context, collection)
            .withTitle("Jesse Pinkman")
            .withPersonMainAffiliation("University of Rome")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Red Smith")
            .withAuthor("Walter White", person1.getID().toString())
            .withAuthor("John Smith", person2.getID().toString())
            .withEditor("Jesse Pinkman", person3.getID().toString())
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> values = publication.getMetadata();
        assertThat(values, hasSize(21));
        assertThat(values, hasItem(with("dc.contributor.author", "Red Smith")));
        assertThat(values, hasItem(with("dc.contributor.author", "Walter White", person1.getID().toString(), 1, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "John Smith", person2.getID().toString(), 2, 600)));
        assertThat(values, hasItem(with("dc.contributor.editor", "Jesse Pinkman", person3.getID().toString(), 0, 600)));
        // virtual source and virtual metadata are not required to respect the order of the source metadata
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person1.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person2.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person3.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", "4Science")));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", "University of Rome")));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid", person1.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid", person2.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid", person3.getID().toString())));
        // we can check with the position as all the values are expected to be placeholder
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 0)));
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 1)));
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 2)));
        assertThat(getMetadataValues(publication, "cris.virtual.department"), hasSize(3));
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), hasSize(3));
        assertThat(getMetadataValues(publication, "cris.virtual.orcid"), hasSize(3));
        assertThat(getMetadataValues(publication, "cris.virtualsource.orcid"), hasSize(3));

    }

    @Test
    public void testEnhancementAfterMetadataAddition() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .build();

        String personId = person.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(5));

        assertThat(getMetadataValues(publication, "cris.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), empty());

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, publication, "dc", "contributor", "author",
            null, "Walter White", personId, 600);
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(10));
        assertThat(metadataValues, hasItem(with("dc.contributor.author", "Walter White", personId, 600)));
        assertThat(metadataValues, hasItem(with("cris.virtual.department", "4Science")));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.department", personId)));

    }

    @Test
    public void testEnhancementWithMetadataRemoval() throws Exception {

        choiceAuthorityService.getChoiceAuthoritiesNames();
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        Item person1 = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .build();

        Item person2 = ItemBuilder.createItem(context, collection)
            .withTitle("John Smith")
            .withPersonMainAffiliation("Company")
            .build();

        Item person3 = ItemBuilder.createItem(context, collection)
            .withTitle("Jesse Pinkman")
            .withPersonMainAffiliation("University of Rome")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Walter White", person1.getID().toString())
            .withAuthor("John Smith", person2.getID().toString())
            .withAuthor("Jesse Pinkman", person3.getID().toString())
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> values = publication.getMetadata();

        List<MetadataValue> depts = getMetadataValues(publication, "cris.virtualsource.department");
        depts.forEach(mv -> {
            System.out.println("Field: " + mv.getMetadataField().getQualifier());
            System.out.println("Value: '" + mv.getValue() + "' (" + mv.getValue().getClass() + ")");
            System.out.println("Conf: " + mv.getConfidence() + " (" + mv.getConfidence() + ")");
            System.out.println("Auth: " + mv.getAuthority());
        });
        System.out.println("Expected value: '" + person3.getID().toString() + "'");


        assertThat(values, hasSize(20));
        assertThat(values, hasItem(with("dc.contributor.author", "Walter White", person1.getID().toString(), 0, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "John Smith", person2.getID().toString(), 1, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "Jesse Pinkman", person3.getID().toString(), 2, 600)));

        // virtual source and virtual metadata are not required to respect the order of the source metadata
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person1.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person2.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person3.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", "4Science")));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", "Company")));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", "University of Rome")));
        assertThat(getMetadataValues(publication, "cris.virtual.department"), hasSize(3));
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), hasSize(3));

        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid",  person1.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid", person2.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid", person3.getID().toString())));
        // we can check with the position as all the values are expected to be placeholder
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 0)));
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 1)));
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 2)));
        assertThat(getMetadataValues(publication, "cris.virtual.orcid"), hasSize(3));
        assertThat(getMetadataValues(publication, "cris.virtualsource.orcid"), hasSize(3));


        MetadataValue authorToRemove = getMetadataValues(publication, "dc.contributor.author").get(1);

        context.turnOffAuthorisationSystem();
        itemService.removeMetadataValues(context, publication, List.of(authorToRemove));
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        values = publication.getMetadata();
        assertThat(values, hasSize(15));
        assertThat(values, hasItem(with("dc.contributor.author", "Walter White", person1.getID().toString(), 0, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "Jesse Pinkman", person3.getID().toString(), 1, 600)));
        // virtual source and virtual metadata are not required to respect the order of the source metadata
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person1.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person3.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", "4Science")));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", "University of Rome")));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid",  person1.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid", person3.getID().toString())));
        // we can check with the position as all the values are expected to be placeholder
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 0)));
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 1)));
        assertThat(getMetadataValues(publication, "cris.virtual.department"), hasSize(2));
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), hasSize(2));
        assertThat(getMetadataValues(publication, "cris.virtual.orcid"), hasSize(2));
        assertThat(getMetadataValues(publication, "cris.virtualsource.orcid"), hasSize(2));

    }

    @Test
    public void testWithWorkspaceItem() throws Exception {
        // Configure authority settings for this specific test
        choiceAuthorityService.getChoiceAuthoritiesNames();
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .build();

        String personId = person.getID().toString();

        WorkspaceItem publication = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Walter White", personId)
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> metadataValues = publication.getItem().getMetadata();
        assertThat(metadataValues, hasSize(3));
        assertThat(getMetadataValues(publication, "cris.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), empty());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEnhancementAfterItemUpdate() throws Exception {

        choiceAuthorityService.getChoiceAuthoritiesNames();
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withOrcidIdentifier("0000-0000-1111-2222")
            .build();

        String personId = person.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Saul Goodman")
            .withAuthor("Walter White", person.getID().toString())
            .withAuthor("Gus Fring")
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        assertThat(getMetadataValues(publication, "dc.contributor.author"), contains(
            with("dc.contributor.author", "Jesse Pinkman"),
            with("dc.contributor.author", "Saul Goodman", 1),
            with("dc.contributor.author", "Walter White", personId, 2, 600),
            with("dc.contributor.author", "Gus Fring", 3)));

        assertThat(getMetadataValues(publication, "cris.virtual.orcid"), contains(
            with("cris.virtual.orcid", "0000-0000-1111-2222")));

        assertThat(getMetadataValues(publication, "cris.virtualsource.orcid"), contains(
            with("cris.virtualsource.orcid", personId)));

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, publication, "dc", "title", "alternative", null, "Other name");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        assertThat(getMetadataValues(publication, "dc.contributor.author"), contains(
            with("dc.contributor.author", "Jesse Pinkman"),
            with("dc.contributor.author", "Saul Goodman", 1),
            with("dc.contributor.author", "Walter White", personId, 2, 600),
            with("dc.contributor.author", "Gus Fring", 3)));

        assertThat(getMetadataValues(publication, "cris.virtual.orcid"), contains(
                with("cris.virtual.orcid", "0000-0000-1111-2222")));

        assertThat(getMetadataValues(publication, "cris.virtualsource.orcid"), contains(
            with("cris.virtualsource.orcid", personId)));

    }

    @Test
    public void testMultipleRelatedItemValuesEnhancement() throws Exception {

        choiceAuthorityService.getChoiceAuthoritiesNames();
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("authority.controlled.dc.contributor.editor", "true");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();
        MetadataSchema schema = ContentServiceFactory.getInstance()
                .getMetadataSchemaService().find(context, "cris");
        MetadataFieldBuilder.createMetadataField(context, schema, "virtual", "testmultival", null);
        MetadataFieldBuilder.createMetadataField(context, schema, "virtualsource", "testmultival", null);

        Item person1 = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .withPersonMainAffiliation("DSpace")
            .withOrcidIdentifier("orcid1")
            .build();

        Item person2 = ItemBuilder.createItem(context, collection)
            .withTitle("John Smith")
            .build();

        Item person3 = ItemBuilder.createItem(context, collection)
            .withTitle("Jesse Pinkman")
            .withPersonMainAffiliation("University of Rome")
            .build();

        Item testEntity = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            // let's use our custom entity for test purpose, see extra-metadata-enhancers-for-test.xml
            .withEntityType("TestEntity")
            .withAuthor("Red Smith")
            .withAuthor("Walter White", person1.getID().toString())
            .withAuthor("John Smith", person2.getID().toString())
            .withEditor("Jesse Pinkman", person3.getID().toString())
            .build();

        context.restoreAuthSystemState();
        testEntity = commitAndReload(testEntity);

        List<MetadataValue> values = testEntity.getMetadata();
        assertThat(values, hasItem(with("dc.contributor.author", "Red Smith")));
        assertThat(values, hasItem(with("dc.contributor.author", "Walter White", person1.getID().toString(), 1, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "John Smith", person2.getID().toString(), 2, 600)));
        assertThat(values, hasItem(with("dc.contributor.editor", "Jesse Pinkman", person3.getID().toString(), 0, 600)));
        // virtual source and virtual metadata are not required to respect the order of the source metadata
        List<Integer> posPerson1 = getPlacesAsVirtualSource(person1, testEntity, "cris.virtualsource.testmultival");
        List<Integer> posPerson2 = getPlacesAsVirtualSource(person2, testEntity, "cris.virtualsource.testmultival");
        List<Integer> posPerson3 = getPlacesAsVirtualSource(person3, testEntity, "cris.virtualsource.testmultival");
        assertThat(values,
                hasItem(with("cris.virtualsource.testmultival", person1.getID().toString(), posPerson1.get(0))));
        assertThat(values, hasItem(with("cris.virtual.testmultival", "4Science", posPerson1.get(0))));
        assertThat(values,
                hasItem(with("cris.virtualsource.testmultival", person1.getID().toString(), posPerson1.get(1))));
        assertThat(values, hasItem(with("cris.virtual.testmultival", "DSpace", posPerson1.get(1))));
        assertThat(values,
                hasItem(with("cris.virtualsource.testmultival", person1.getID().toString(), posPerson1.get(2))));
        assertThat(values, hasItem(with("cris.virtual.testmultival", "orcid1", posPerson1.get(2))));

        assertThat(values,
                hasItem(with("cris.virtualsource.testmultival", person2.getID().toString(), posPerson2.get(0))));
        assertThat(values,
                hasItem(with("cris.virtual.testmultival", PLACEHOLDER_PARENT_METADATA_VALUE, posPerson2.get(0))));

        assertThat(values,
                hasItem(with("cris.virtualsource.testmultival", person3.getID().toString(), posPerson3.get(0))));
        assertThat(values, hasItem(with("cris.virtual.testmultival", "University of Rome", posPerson3.get(0))));

        assertThat(getMetadataValues(testEntity, "cris.virtualsource.testmultival"), hasSize(5));
        assertThat(getMetadataValues(testEntity, "cris.virtual.testmultival"), hasSize(5));

    }

    @Test
    public void testSingleMetadataJournalAnceEnhancement() throws Exception {
        // Configure authority settings for this specific test
        choiceAuthorityService.getChoiceAuthoritiesNames();
        configurationService.setProperty("authority.controlled.dc.relation.journal", "true");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        Item journalItem = ItemBuilder.createItem(context, collection)
                .withTitle("Test journal")
                .withEntityType("Journal")
                .withJournalAnce("AA110022")
                .build();

        Item publication = ItemBuilder.createItem(context, collection)
                .withTitle("Test publication")
                .withEntityType("Publication")
                .withRelationJournal(journalItem.getName(), journalItem.getID().toString())
                .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(8));
        assertThat(metadataValues, hasItem(with("cris.virtual.journalance", "AA110022")));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.journalance", journalItem.getID().toString())));
    }

    private List<Integer> getPlacesAsVirtualSource(Item person1, Item publication, String metadata) {
        return getMetadataValues(publication, metadata).stream()
                .filter(mv -> StringUtils.equals(mv.getValue(), person1.getID().toString())).map(mv -> mv.getPlace())
                .collect(Collectors.toList());
    }

    private MetadataValue getFirstMetadataValue(Item item, String metadataField) {
        return getMetadataValues(item, metadataField).get(0);
    }

    private List<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    private List<MetadataValue> getMetadataValues(WorkspaceItem item, String metadataField) {
        return itemService.getMetadataByMetadataString(item.getItem(), metadataField);
    }

    @SuppressWarnings("rawtypes")
    private <T extends ReloadableEntity> T commitAndReload(T entity) throws SQLException, AuthorizeException {
        context.commit();
        return context.reloadEntity(entity);
    }

}
