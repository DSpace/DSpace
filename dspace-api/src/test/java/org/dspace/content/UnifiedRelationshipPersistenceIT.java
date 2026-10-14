/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.authority.AuthorityBackedRelationshipServiceImpl;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipService;
import org.dspace.utils.DSpace;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Small manually migrated DSpace-7 fixture. This verifies the storage association;
 * it is deliberately not a general migration tool or a claim of complete migration coverage.
 *
 * @author Ben Bosman (ben . bosman at atmire.com)
 */
public class UnifiedRelationshipPersistenceIT extends AbstractIntegrationTestWithDatabase {
    @Test
    public void testLegacyRelationshipIdSurvivesMetadataBackfill() throws Exception {
        context.turnOffAuthorisationSystem();
        DSpace dspace = new DSpace();
        RelationshipService relationships = ContentServiceFactory.getInstance().getRelationshipService();
        MetadataValueService metadata = ContentServiceFactory.getInstance().getMetadataValueService();
        AuthorityBackedRelationshipService service =
                dspace.getServiceManager().getServiceByName(
                        AuthorityBackedRelationshipServiceImpl.class.getCanonicalName(),
                        AuthorityBackedRelationshipService.class
                );
        Community community = CommunityBuilder.createCommunity(context).withName("migration").build();
        Collection collection = CollectionBuilder.createCollection(context, community).withName("migration").build();
        Item publication = ItemBuilder.createItem(context, collection).withTitle("publication")
            .withEntityType("Publication").build();
        Item person = ItemBuilder.createItem(context, collection).withTitle("Jane Anwall")
            .withEntityType("Person").build();
        EntityType publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType personType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        RelationshipType legacyType = RelationshipTypeBuilder.createRelationshipTypeBuilder(context,
            publicationType, personType, "isAuthorOfPublication", "isPublicationOfAuthor", 0, null, 0, null).build();
        Relationship legacy = RelationshipBuilder.createRelationshipBuilder(context, publication, person, legacyType)
            .withLeftPlace(1).withRightwardValue("Smith, Jane").build();
        Integer legacyId = legacy.getID();

        RelationshipTypeConfiguration definition = new RelationshipTypeConfiguration();
        definition.setId("poc-publication-author");
        definition.setLeftMetadataField("dc.contributor.author");
        definition.setLeftEntityTypes(java.util.List.of("Publication"));
        definition.setRightEntityTypes(java.util.List.of("Person", "OrgUnit"));
        RelationshipConfigurationServiceImpl configurations =
                dspace.getServiceManager().getServiceByName(
                        RelationshipConfigurationServiceImpl.class.getCanonicalName(),
                        RelationshipConfigurationServiceImpl.class
                );
        Object previous = ReflectionTestUtils.getField(configurations, "configurations");
        ReflectionTestUtils.setField(configurations, "configurations", java.util.List.of(definition));
        try {
            MetadataField field = ContentServiceFactory.getInstance().getMetadataFieldService()
                .findByElement(context, "dc", "contributor", "author");
            MetadataValue value = metadata.create(context, publication, field);
            value.setValue(legacy.getRightwardValue());
            value.setPlace(legacy.getLeftPlace());
            value.setAuthority(person.getID().toString());
            legacy.setRelationshipConfigKey(definition.getId());
            // Keep the old type table available, but this row has migrated off it.
            legacy.setRelationshipType(null);
            service.attachMetadataToRelationship(context, value, legacy);
            relationships.update(context, legacy);
            context.commit();

            publication = context.reloadEntity(publication);
            person = context.reloadEntity(person);
            legacy = context.reloadEntity(legacy);
            value = context.reloadEntity(value);
            assertThat(legacy.getID(), equalTo(legacyId));
            assertThat(legacy.getRelationshipType(), nullValue());
            assertThat(legacy.getRelationshipConfigKey(), equalTo(definition.getId()));
            assertThat(value.getRelationship().getID(), equalTo(legacyId));
            assertThat(value.getValue(), equalTo("Smith, Jane"));
            assertThat(value.getPlace(), equalTo(0));
            assertThat(metadata.findByRelationship(context, legacy), hasSize(1));
            assertThat(relationships.findByItem(context, person), hasSize(1));
            assertThat(ContentServiceFactory.getInstance().getRelationshipTypeService()
                .find(context, legacyType.getID()), notNullValue());
            // Demonstrate that migrated rows do not also generate virtual author values.
            assertThat(ContentServiceFactory.getInstance().getRelationshipMetadataService()
                .findRelationshipMetadataValueForItemRelationship(context, publication,
                    "Publication", legacy, true), hasSize(0));
        } finally {
            // Remove this fixture's projections/link before removing its temporary definition.
            Relationship current = relationships.find(context, legacyId);
            if (current != null) {
                if (current.isConfigurationBacked()) {
                    service.removeRelationshipAndMetadata(context, current);
                } else {
                    relationships.delete(context, current);
                }
                context.commit();
            }
            ReflectionTestUtils.setField(configurations, "configurations", previous);
        }
    }
}
