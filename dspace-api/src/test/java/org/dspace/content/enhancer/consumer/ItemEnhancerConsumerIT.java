/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.consumer;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.sql.SQLException;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.matcher.MetadataValueMatcher;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.MetadataFieldBuilder;
import org.dspace.builder.MetadataSchemaBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.AuthorityBackedRelationshipServiceImpl;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.ReloadableEntity;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the {@link ItemEnhancerConsumer}.
 *
 * <p>The consumer forces the enhancement of the archived items touched by the dispatched events.
 * The legacy virtual metadata was replaced by the relationship additional metadata derived by the
 * related entity enhancer, so the tests assert on the {@code additional.identifier.orcid} values
 * produced for the relationship-backed authors.</p>
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ItemEnhancerConsumerIT extends AbstractIntegrationTestWithDatabase {

    private static final String ADDITIONAL_FIELD = "additional.identifier.orcid";
    private static final String AUTHOR_FIELD = "dc.contributor.author";

    private ItemService itemService;

    private Collection collection;

    private AuthorityBackedRelationshipService authorityBackedRelationshipService;

    private MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory
        .getInstance().getMetadataAuthorityService();

    private ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    @Before
    public void setup() throws Exception {

        itemService = ContentServiceFactory.getInstance().getItemService();
        authorityBackedRelationshipService = new DSpace().getServiceManager().getServiceByName(
            AuthorityBackedRelationshipServiceImpl.class.getCanonicalName(),
            AuthorityBackedRelationshipService.class);

        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("authority.controlled.additional.identifier.orcid", "true");
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();
        registerAdditionalOrcidField();

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

        Item person = createPerson("Walter White", "0000-0001-0000-0001");
        Item publication = createPublication("Test publication", person);
        promoteAuthors(publication, person);

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> metadataValues = getMetadataValues(publication, ADDITIONAL_FIELD);
        assertThat(metadataValues, hasSize(1));

        MetadataValue additional = metadataValues.get(0);
        assertThat(additional.getValue(), equalTo("0000-0001-0000-0001"));
        assertThat(additional.getRelationship(), notNullValue());
        assertThat(additional.getRelationship().getID(), equalTo(primaryRelationship(publication).getID()));
        assertThat(additional.getAuthority(), equalTo(person.getID().toString()));

        Integer additionalId = additional.getID();

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, publication, "dc", "subject", null, null, "Test");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        metadataValues = getMetadataValues(publication, ADDITIONAL_FIELD);
        assertThat(metadataValues, hasSize(1));
        assertThat(metadataValues.get(0).getID(), equalTo(additionalId));
        assertThat(metadataValues.get(0).getValue(), equalTo("0000-0001-0000-0001"));

    }

    @Test
    public void testManyMetadataValuesEnhancement() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person1 = createPerson("Walter White", "0000-0001-0000-0001");
        Item person2 = createPerson("John Smith", null);
        Item person3 = createPerson("Jesse Pinkman", "0000-0001-0000-0003");

        Item publication = createPublication("Test publication", person1, person2, person3);
        promoteAuthors(publication, person1, person2, person3);

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> values = publication.getMetadata();
        assertThat(values, hasItem(with(AUTHOR_FIELD, "Walter White", person1.getID().toString(), 0, 600)));
        assertThat(values, hasItem(with(AUTHOR_FIELD, "John Smith", person2.getID().toString(), 1, 600)));
        assertThat(values, hasItem(with(AUTHOR_FIELD, "Jesse Pinkman", person3.getID().toString(), 2, 600)));

        // additional metadata is not required to respect the order of the source metadata
        assertThat(values, hasItem(additionalValue("0000-0001-0000-0001", person1)));
        assertThat(values, hasItem(additionalValue("0000-0001-0000-0003", person3)));
        assertThat(values, hasItem(additionalValue(PLACEHOLDER_PARENT_METADATA_VALUE, person2)));
        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), hasSize(3));

    }

    @Test
    public void testEnhancementAfterMetadataAddition() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = createPerson("Walter White", "0000-0001-0000-0001");

        Item publication = createPublication("Test publication");

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), empty());

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, publication, "dc", "contributor", "author",
            null, titleOf(person), person.getID().toString(), 600);
        itemService.update(context, publication);
        MetadataValue author = getMetadataValues(publication, AUTHOR_FIELD).get(0);
        authorityBackedRelationshipService.promoteResolvedAuthority(context, publication, author, person);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> metadataValues = getMetadataValues(publication, ADDITIONAL_FIELD);
        assertThat(metadataValues, hasSize(1));
        assertThat(metadataValues.get(0).getValue(), equalTo("0000-0001-0000-0001"));
        assertThat(metadataValues.get(0).getRelationship(), notNullValue());

    }

    @Test
    public void testEnhancementWithMetadataRemoval() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person1 = createPerson("Walter White", "0000-0001-0000-0001");
        Item person2 = createPerson("John Smith", "0000-0001-0000-0002");
        Item person3 = createPerson("Jesse Pinkman", "0000-0001-0000-0003");

        Item publication = createPublication("Test publication", person1, person2, person3);
        promoteAuthors(publication, person1, person2, person3);

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), hasSize(3));

        MetadataValue authorToRemove = getMetadataValues(publication, AUTHOR_FIELD).get(1);

        context.turnOffAuthorisationSystem();
        itemService.removeMetadataValues(context, publication, List.of(authorToRemove));
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> values = publication.getMetadata();
        assertThat(values, hasItem(with(AUTHOR_FIELD, "Walter White", person1.getID().toString(), 0, 600)));
        assertThat(values, hasItem(with(AUTHOR_FIELD, "Jesse Pinkman", person3.getID().toString(), 1, 600)));

        List<MetadataValue> additionals = getMetadataValues(publication, ADDITIONAL_FIELD);
        assertThat(additionals, hasSize(2));
        assertThat(values, hasItem(additionalValue("0000-0001-0000-0001", person1)));
        assertThat(values, hasItem(additionalValue("0000-0001-0000-0003", person3)));

    }

    @Test
    public void testWithWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = createPerson("Walter White", "0000-0001-0000-0001");

        WorkspaceItem publication = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Walter White", person.getID().toString())
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), empty());

    }

    @Test
    public void testEnhancementAfterItemUpdate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = createPerson("Walter White", "0000-0000-1111-2222");

        Item publication = createPublication("Test publication", person);
        promoteAuthors(publication, person);

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        Integer additionalId = getMetadataValues(publication, ADDITIONAL_FIELD).get(0).getID();
        Integer relationshipId = primaryRelationship(publication).getID();

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, publication, "dc", "title", "alternative", null, "Other name");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> additionals = getMetadataValues(publication, ADDITIONAL_FIELD);
        assertThat(additionals, hasSize(1));
        assertThat(additionals.get(0).getID(), equalTo(additionalId));
        assertThat(additionals.get(0).getValue(), equalTo("0000-0000-1111-2222"));
        assertThat(additionals.get(0).getRelationship().getID(), equalTo(relationshipId));

    }

    @Test
    public void testMultipleRelatedItemValuesEnhancement() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = createPerson("Walter White", "0000-0001-0000-0001");
        itemService.addMetadata(context, person, "person", "identifier", "orcid", null, "0000-0001-0000-0002");
        itemService.update(context, person);

        Item publication = createPublication("Test publication", person);
        promoteAuthors(publication, person);

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> values = publication.getMetadata();
        assertThat(values, hasItem(additionalValue("0000-0001-0000-0001", person)));
        assertThat(values, hasItem(additionalValue("0000-0001-0000-0002", person)));
        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), hasSize(2));

    }

    /**
     * Register the target metadata field used by the production relationship configuration.
     */
    private void registerAdditionalOrcidField() throws Exception {
        MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        MetadataField existing = metadataFieldService.findByElement(context, "additional", "identifier", "orcid");
        if (existing != null) {
            return;
        }

        MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
        MetadataSchema schema = metadataSchemaService.find(context, "additional");
        if (schema == null) {
            schema = MetadataSchemaBuilder
                .createMetadataSchema(context, "additional", "http://dspace.org/additional")
                .build();
        }
        MetadataFieldBuilder.createMetadataField(context, schema, "identifier", "orcid", null).build();
    }

    private Item createPerson(String title, String orcid) throws Exception {
        ItemBuilder builder = ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withEntityType("Person");
        if (orcid != null) {
            builder.withOrcidIdentifier(orcid);
        }
        return builder.build();
    }

    private Item createPublication(String title, Item... persons) throws Exception {
        ItemBuilder builder = ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withEntityType("Publication");
        for (Item person : persons) {
            builder.withAuthor(titleOf(person), person.getID().toString());
        }
        return builder.build();
    }

    private void promoteAuthors(Item publication, Item... persons) throws Exception {
        List<MetadataValue> authors = getMetadataValues(publication, AUTHOR_FIELD);
        assertThat(authors, hasSize(persons.length));
        for (int i = 0; i < persons.length; i++) {
            Relationship relationship = authorityBackedRelationshipService
                .promoteResolvedAuthority(context, publication, authors.get(i), persons[i]);
            assertThat(relationship, notNullValue());
        }
    }

    private Relationship primaryRelationship(Item publication) {
        return getMetadataValues(publication, AUTHOR_FIELD).get(0).getRelationship();
    }

    private MetadataValueMatcher additionalValue(String value, Item person) {
        return with(ADDITIONAL_FIELD, value, null, person.getID().toString(), null, -1);
    }

    private String titleOf(Item item) {
        return itemService.getMetadata(item, "dc", "title", null, Item.ANY).get(0).getValue();
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
