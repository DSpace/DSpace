/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.script;

import static org.dspace.content.Item.ANY;
import static org.dspace.content.enhancer.consumer.ItemEnhancerConsumer.ITEMENHANCER_ENABLED;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.matcher.MetadataValueMatcher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
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
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the {@link ItemEnhancerScript}.
 *
 * <p>The script enhances every archived item and supports a forced (deep) mode. The tests assert on
 * the {@code additional.identifier.orcid} values produced for the relationship-backed authors.</p>
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ItemEnhancerScriptIT extends AbstractIntegrationTestWithDatabase {

    private static final String ADDITIONAL_FIELD = "additional.identifier.orcid";
    private static final String AUTHOR_FIELD = "dc.contributor.author";

    private static ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private static final EventService eventService = EventServiceFactory.getInstance().getEventService();

    private MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory
        .getInstance().getMetadataAuthorityService();

    private static String[] consumers;
    private ItemService itemService;

    private Collection collection;

    private AuthorityBackedRelationshipService authorityBackedRelationshipService;

    /**
     * This method will be run before the first test as per @BeforeClass. It will
     * configure the event.dispatcher.default.consumers property to add the
     * ItemEnhancerConsumer.
     */
    @BeforeClass
    public static void initConsumers() {
        consumers = configService.getArrayProperty("event.dispatcher.default.consumers");
        Set<String> consumersSet = new HashSet<String>(Arrays.asList(consumers));
        if (!consumersSet.contains("itemenhancer")) {
            consumersSet.add("itemenhancer");
            configService.setProperty("event.dispatcher.default.consumers", consumersSet.toArray());
            eventService.reloadConfiguration();
        }
    }

    /**
     * Reset the event.dispatcher.default.consumers property value.
     */
    @AfterClass
    public static void resetDefaultConsumers() {
        configService.setProperty("event.dispatcher.default.consumers", consumers);
        eventService.reloadConfiguration();
    }

    @Before
    public void setup() throws Exception {
        configService.setProperty("authority.controlled.dc.contributor.author", "true");
        configService.setProperty("authority.controlled.additional.identifier.orcid", "true");
        metadataAuthorityService.clearCache();
        configService.setProperty(ITEMENHANCER_ENABLED, false);

        itemService = ContentServiceFactory.getInstance().getItemService();
        authorityBackedRelationshipService = new DSpace().getServiceManager().getServiceByName(
            AuthorityBackedRelationshipServiceImpl.class.getCanonicalName(),
            AuthorityBackedRelationshipService.class);

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
    public void testItemsEnhancement() throws Exception {
        context.turnOffAuthorisationSystem();

        Item firstAuthor = createPerson("Walter White", "0000-0001-0000-0001");
        String firstAuthorId = firstAuthor.getID().toString();

        Item secondAuthor = createPerson("Jesse Pinkman", "0000-0001-0000-0002");
        String secondAuthorId = secondAuthor.getID().toString();

        Item firstPublication = createPublication("Test publication", firstAuthor);
        Item secondPublication = createPublication("Test publication 2", firstAuthor, secondAuthor);
        Item thirdPublication = createPublication("Test publication 3", firstAuthor, firstAuthor, secondAuthor);

        WorkspaceItem wsPublication = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("Test workspace publication")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman", secondAuthorId)
            .build();

        promoteAuthors(firstPublication, firstAuthor);
        promoteAuthors(secondPublication, firstAuthor, secondAuthor);
        promoteAuthors(thirdPublication, firstAuthor, firstAuthor, secondAuthor);

        context.commit();

        firstPublication = reload(firstPublication);
        secondPublication = reload(secondPublication);
        wsPublication = reload(wsPublication);

        assertThat(getMetadataValues(firstPublication, ADDITIONAL_FIELD), empty());
        assertThat(getMetadataValues(secondPublication, ADDITIONAL_FIELD), empty());
        assertThat(getMetadataValues(wsPublication, ADDITIONAL_FIELD), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), contains("Enhancement completed with success"));

        firstPublication = reload(firstPublication);
        secondPublication = reload(secondPublication);
        thirdPublication = reload(thirdPublication);
        wsPublication = reload(wsPublication);

        assertThat(getMetadataValues(firstPublication, ADDITIONAL_FIELD), hasSize(1));
        assertThat(firstPublication.getMetadata(), hasItem(additionalValue("0000-0001-0000-0001", firstAuthor)));
        assertThat(getMetadataValues(firstPublication, ADDITIONAL_FIELD).get(0).getAuthority(),
            is(firstAuthorId));

        assertThat(getMetadataValues(secondPublication, ADDITIONAL_FIELD), hasSize(2));
        assertThat(getMetadataValues(secondPublication, ADDITIONAL_FIELD),
                containsInAnyOrder(
                    additionalValue("0000-0001-0000-0001", firstAuthor),
                    additionalValue("0000-0001-0000-0002", secondAuthor)));

        assertThat(getMetadataValues(thirdPublication, ADDITIONAL_FIELD), hasSize(3));
        assertThat(getMetadataValues(thirdPublication, ADDITIONAL_FIELD),
                containsInAnyOrder(
                    additionalValue("0000-0001-0000-0001", firstAuthor),
                    additionalValue("0000-0001-0000-0001", firstAuthor),
                    additionalValue("0000-0001-0000-0002", secondAuthor)));

        assertThat(getMetadataValues(wsPublication, ADDITIONAL_FIELD), empty());

    }

    @Test
    public void testItemEnhancementWithoutForce() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstAuthor = createPerson("Walter White", "0000-0001-0000-0001");
        Item secondAuthor = createPerson("Jesse Pinkman", "0000-0001-0000-0002");

        Item publication = createPublication("Test publication 2", firstAuthor, secondAuthor);
        promoteAuthors(publication, firstAuthor, secondAuthor);

        context.commit();
        publication = reload(publication);

        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), contains("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), hasSize(2));
        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), containsInAnyOrder(
                additionalValue("0000-0001-0000-0001", firstAuthor),
                additionalValue("0000-0001-0000-0002", secondAuthor)));

        context.turnOffAuthorisationSystem();

        MetadataValue authorToRemove = getMetadataValues(publication, AUTHOR_FIELD).get(1);
        itemService.removeMetadataValues(context, publication, List.of(authorToRemove));

        replaceMetadata(firstAuthor, "person", "identifier", "orcid", "0000-0001-0000-0003");

        context.restoreAuthSystemState();

        runnableHandler = runScript(false);
        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), contains("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), hasSize(1));
        assertThat(publication.getMetadata(), hasItem(additionalValue("0000-0001-0000-0003", firstAuthor)));

    }

    @Test
    public void testItemEnhancementWithForce() throws Exception {
        context.turnOffAuthorisationSystem();

        Item firstAuthor = createPerson("Walter White", "0000-0001-0000-0001");
        itemService.addMetadata(context, firstAuthor, "person", "identifier", "orcid", null,
            "0000-0001-0000-0002");
        itemService.update(context, firstAuthor);

        Item secondAuthor = createPerson("Jesse Pinkman", "0000-0001-0000-0003");

        Item publication = createPublication("Test publication 2", firstAuthor, secondAuthor);
        promoteAuthors(publication, firstAuthor, secondAuthor);

        context.commit();
        publication = reload(publication);

        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), contains("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), hasSize(3));
        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), containsInAnyOrder(
                additionalValue("0000-0001-0000-0001", firstAuthor),
                additionalValue("0000-0001-0000-0002", firstAuthor),
                additionalValue("0000-0001-0000-0003", secondAuthor)));

        List<Integer> additionalIds = getMetadataValues(publication, ADDITIONAL_FIELD).stream()
            .map(MetadataValue::getID)
            .collect(Collectors.toList());

        runnableHandler = runScript(true);
        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), contains("Enhancement completed with success"));

        publication = reload(publication);

        List<MetadataValue> additionals = getMetadataValues(publication, ADDITIONAL_FIELD);
        assertThat(additionals, hasSize(3));

        List<Integer> forcedIds = additionals.stream()
            .map(MetadataValue::getID)
            .collect(Collectors.toList());

        // the forced (deep) enhancement rebuilds every derived value from scratch
        assertThat(Collections.disjoint(additionalIds, forcedIds), is(true));

    }

    @Test
    public void testItemEnhancementMetadataPositions() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstAuthor = createPerson("John Doe", null);
        Item secondAuthor = createPerson("Walter White", "0000-0001-0000-0001");
        Item thirdAuthor = createPerson("Jesse Pinkman", "0000-0001-0000-0002");
        Item fourthAuthor = createPerson("Jesse Smith", null);

        Item publication = createPublication("Test publication 2", firstAuthor, secondAuthor,
            thirdAuthor, fourthAuthor);
        promoteAuthors(publication, firstAuthor, secondAuthor, thirdAuthor, fourthAuthor);

        context.commit();
        publication = reload(publication);

        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), contains("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), hasSize(4));
        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), containsInAnyOrder(
                additionalAt(PLACEHOLDER_PARENT_METADATA_VALUE, firstAuthor, 0),
                additionalAt("0000-0001-0000-0001", secondAuthor, 1),
                additionalAt("0000-0001-0000-0002", thirdAuthor, 2),
                additionalAt(PLACEHOLDER_PARENT_METADATA_VALUE, fourthAuthor, 3)));
    }

    @Test
    public void testItemEnhancementWithoutAuthorities() throws Exception {

        context.turnOffAuthorisationSystem();

        Item publication = createPublication("Test publication 2");
        itemService.addMetadata(context, publication, "dc", "contributor", "author", null, "Jesse Pinkman");
        itemService.addMetadata(context, publication, "dc", "contributor", "author", null, "Jesse Smith");
        itemService.update(context, publication);

        context.commit();
        publication = reload(publication);

        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), contains("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, ADDITIONAL_FIELD), empty());

    }

    private TestDSpaceRunnableHandler runScript(boolean force) throws InstantiationException, IllegalAccessException {
        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = force ? new String[] { "item-enhancer", "-f" } : new String[] { "item-enhancer" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);
        return runnableHandler;
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

    private String titleOf(Item item) {
        return itemService.getMetadata(item, "dc", "title", null, Item.ANY).get(0).getValue();
    }

    private MetadataValueMatcher additionalValue(String value, Item person) {
        return additionalAt(value, person, null);
    }

    private MetadataValueMatcher additionalAt(String value, Item person, Integer place) {
        return MetadataValueMatcher.with(ADDITIONAL_FIELD, value, null, person.getID().toString(), place, -1);
    }

    @SuppressWarnings("rawtypes")
    private <T extends ReloadableEntity> T reload(T entity) throws SQLException, AuthorizeException {
        return context.reloadEntity(entity);
    }

    private void replaceMetadata(Item item, String schema, String element, String qualifier, String newValue)
        throws SQLException, AuthorizeException {
        itemService.replaceMetadata(context, reload(item), schema, element, qualifier, ANY, newValue, null, -1, 0);
    }

    private List<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    private List<MetadataValue> getMetadataValues(WorkspaceItem item, String metadataField) {
        return itemService.getMetadataByMetadataString(item.getItem(), metadataField);
    }

}
