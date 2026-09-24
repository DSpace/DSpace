/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test for the persistence shape of authority-backed relationships.
 *
 * <p>This test deliberately checks both the Hibernate model and the underlying
 * {@code metadatavalue.relationship_id} database column. A Publication is created
 * with several {@code dc.contributor.author} values whose authority is the UUID of
 * an archived Person item. The CRIS consumer is expected to mint one durable
 * Relationship per author and link the corresponding MetadataValue to it.</p>
 */
public class AuthorityBackedRelationshipPersistenceIT extends AbstractControllerIntegrationTest {

    private EPerson submitter;

    private Collection publicationCollection;

    private Collection personCollection;

    private ItemService itemService;

    private RelationshipService relationshipService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Autowired
    private MetadataAuthorityService metadataAuthorityService;

    @Autowired
    private PluginService pluginService;

    @Before
    public void setup() throws Exception {
        // Initialise the authority services before changing their runtime configuration.
        choiceAuthorityService.getChoiceAuthoritiesNames();

        itemService = ContentServiceFactory.getInstance().getItemService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();

        configurationService.setProperty("cris-consumer.skip-empty-authority", false);
        configurationService.setProperty(
                "plugin.named.org.dspace.content.authority.ChoiceAuthority",
                new String[] { "org.dspace.content.authority.ItemAuthority = AuthorAuthority" });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");

        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        submitter = EPersonBuilder.createEPerson(context)
                .withEmail("relationship-persistence-submitter@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Relationship Persistence Test Community")
                .build();

        publicationCollection = createCollection("Collection of publications", "Publication");
        personCollection = createCollection("Collection of persons", "Person");

        context.setCurrentUser(submitter);
        context.restoreAuthSystemState();
    }

    @Test
    public void testAuthorityBackedAuthorMetadataStoresRelationshipIdAndCorrectEndpoints() throws Exception {
        context.turnOffAuthorisationSystem();

        Item personA = ItemBuilder.createItem(context, personCollection)
                .withTitle("Adams, Alice")
                .build();
        Item personB = ItemBuilder.createItem(context, personCollection)
                .withTitle("Brown, Bob")
                .build();
        Item personC = ItemBuilder.createItem(context, personCollection)
                .withTitle("Clark, Carol")
                .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                .withTitle("Relationship persistence publication")
                .withAuthor("Adams, Alice", personA.getID().toString(), CF_ACCEPTED)
                .withAuthor("Brown, Bob", personB.getID().toString(), CF_ACCEPTED)
                .withAuthor("Clark, Carol", personC.getID().toString(), CF_ACCEPTED)
                .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        personA = context.reloadEntity(personA);
        personB = context.reloadEntity(personB);
        personC = context.reloadEntity(personC);

        List<MetadataValue> authors =
                itemService.getMetadataByMetadataString(publication, "dc.contributor.author");
        assertThat(authors, hasSize(3));

        List<Relationship> relationships = relationshipService.findByItem(context, publication);
        assertThat(relationships, hasSize(3));

        assertAuthorRelationship(publication, personA, authors.get(0), "Adams, Alice");
        assertAuthorRelationship(publication, personB, authors.get(1), "Brown, Bob");
        assertAuthorRelationship(publication, personC, authors.get(2), "Clark, Carol");
    }

    private void assertAuthorRelationship(Item publication, Item expectedPerson, MetadataValue author,
                                          String expectedDisplayValue) throws SQLException {
        assertThat(author.getValue(), equalTo(expectedDisplayValue));
        assertThat(author.getAuthority(), equalTo(expectedPerson.getID().toString()));

        // Hibernate/JPA mapping: MetadataValue.relationship is populated.
        Relationship relationship = author.getRelationship();
        assertThat("Expected the author MetadataValue to reference a Relationship",
                relationship, notNullValue());
        assertThat("Expected the referenced Relationship to have a persisted ID",
                relationship.getID(), notNullValue());

        // Verify the physical metadatavalue.relationship_id column contains that same ID.
        assertThat("metadatavalue.relationship_id should contain the Relationship ID",
                findPersistedRelationshipId(author), equalTo(relationship.getID()));

        // Verify the durable relationship connects this Publication to the correct Person.
        assertThat(relationship.getLeftItem().getID(), equalTo(publication.getID()));
        assertThat(relationship.getRightItem().getID(), equalTo(expectedPerson.getID()));
    }

    private Integer findPersistedRelationshipId(MetadataValue metadataValue) throws SQLException {
        DataSource dataSource = DSpaceServicesFactory.getInstance()
                .getServiceManager()
                .getServiceByName("dataSource", DataSource.class);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT relationship_id FROM metadatavalue WHERE metadata_value_id = ?")) {

            statement.setInt(1, metadataValue.getID());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new AssertionError("No metadatavalue row found for ID " + metadataValue.getID());
                }

                int relationshipId = resultSet.getInt(1);
                if (resultSet.wasNull()) {
                    throw new AssertionError(
                            "metadatavalue.relationship_id is NULL for metadata value " + metadataValue.getID());
                }

                return relationshipId;
            }
        }
    }

    private Collection createCollection(String name, String entityType) throws Exception {
        return CollectionBuilder.createCollection(context, parentCommunity)
                .withName(name)
                .withEntityType(entityType)
                .withSubmitterGroup(submitter)
                .build();
    }
}
