/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.dspace.content.authority.ItemAuthorityExtraMetadataGenerator;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Unit tests for {@link RelationshipTypeConfiguration}.
 */
public class RelationshipTypeConfigurationTest {

    private RelationshipTypeConfiguration configuration;

    @Before
    public void setUp() {
        configuration = new RelationshipTypeConfiguration();
        configuration.setId("publication-author");
    }

    @Test
    public void testSetId() {
        assertEquals("publication-author", configuration.getId());
    }

    @Test
    public void testSetIdRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> configuration.setId(null));
    }

    @Test
    public void testSetIdRejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> configuration.setId("   "));
    }

    @Test
    public void testSetIdRejectsTooLong() {
        assertThrows(IllegalArgumentException.class, () -> configuration.setId("a".repeat(256)));
    }

    @Test
    public void testSetIdAcceptsMaximumLength() {
        String id = "a".repeat(255);

        configuration.setId(id);

        assertEquals(id, configuration.getId());
    }

    @Test
    public void testPrimaryMetadataFields() {
        configuration.setLeftPrimaryMetadataField("dc.contributor.author");
        configuration.setRightPrimaryMetadataField("person.identifier.orcid");

        assertEquals("dc.contributor.author",
                     configuration.getLeftPrimaryMetadataField().orElse(null));
        assertEquals("person.identifier.orcid",
                     configuration.getRightPrimaryMetadataField().orElse(null));
    }

    @Test
    public void testPrimaryMetadataFieldsAreOptional() {
        assertTrue(configuration.getLeftPrimaryMetadataField().isEmpty());
        assertTrue(configuration.getRightPrimaryMetadataField().isEmpty());

        configuration.setLeftPrimaryMetadataField("dc.contributor.author");
        configuration.setLeftPrimaryMetadataField(null);

        assertTrue(configuration.getLeftPrimaryMetadataField().isEmpty());
    }

    @Test
    public void testEntityTypes() {
        configuration.setLeftEntityTypes(Set.of("Publication", "Book"));
        configuration.setRightEntityTypes(Set.of("Person"));

        assertEquals(Set.of("Publication", "Book"), configuration.getLeftEntityTypes());
        assertEquals(Set.of("Person"), configuration.getRightEntityTypes());
    }

    @Test
    public void testEntityTypesAreDefensivelyCopied() {
        Set<String> types = new HashSet<>(Set.of("Publication"));
        configuration.setLeftEntityTypes(types);

        // Changing the original set must not affect the configuration.
        types.add("Book");

        assertEquals(Set.of("Publication"), configuration.getLeftEntityTypes());

        // The returned set must not be modifiable.
        assertThrows(UnsupportedOperationException.class,
                     () -> configuration.getLeftEntityTypes().add("Book"));
    }

    @Test
    public void testMetadataGenerators() {
        ItemAuthorityExtraMetadataGenerator leftGenerator =
            mock(ItemAuthorityExtraMetadataGenerator.class);
        ItemAuthorityExtraMetadataGenerator rightGenerator =
            mock(ItemAuthorityExtraMetadataGenerator.class);

        configuration.setLeftMetadataGenerators(
            Map.of("person.identifier.orcid", leftGenerator));
        configuration.setRightMetadataGenerators(
            Map.of("dc.description", rightGenerator));

        assertSame(leftGenerator,
                   configuration.getLeftMetadataGenerators().get("person.identifier.orcid"));
        assertSame(rightGenerator,
                   configuration.getRightMetadataGenerators().get("dc.description"));
    }

    @Test
    public void testAdditionalMetadataFieldsAreDerivedFromGenerators() {
        ItemAuthorityExtraMetadataGenerator generator =
            mock(ItemAuthorityExtraMetadataGenerator.class);

        configuration.setLeftMetadataGenerators(Map.of(
            "person.identifier.orcid", generator,
            "dc.description", generator
        ));

        assertEquals(Set.of("person.identifier.orcid", "dc.description"),
                     configuration.getLeftAdditionalMetadataFields());
        assertTrue(configuration.getRightAdditionalMetadataFields().isEmpty());
    }

    @Test
    public void testMetadataGeneratorsAreDefensivelyCopied() {
        ItemAuthorityExtraMetadataGenerator generator =
            mock(ItemAuthorityExtraMetadataGenerator.class);

        Map<String, ItemAuthorityExtraMetadataGenerator> generators = new HashMap<>();
        generators.put("person.identifier.orcid", generator);

        configuration.setLeftMetadataGenerators(generators);

        // Changing the original map must not affect the configuration.
        generators.clear();

        assertEquals(Set.of("person.identifier.orcid"),
                     configuration.getLeftAdditionalMetadataFields());

        // The returned map must not be modifiable.
        assertThrows(UnsupportedOperationException.class,
                     () -> configuration.getLeftMetadataGenerators().clear());
    }

    @Test
    public void testNullMetadataGeneratorsAreRejected() {
        Map<String, ItemAuthorityExtraMetadataGenerator> generators = new HashMap<>();
        generators.put("person.identifier.orcid", null);

        assertThrows(IllegalArgumentException.class,
                     () -> configuration.setLeftMetadataGenerators(generators));
        assertThrows(IllegalArgumentException.class,
                     () -> configuration.setRightMetadataGenerators(generators));
    }

    @Test
    public void testIsOwnerOnLeft() {
        configuration.setLeftPrimaryMetadataField("dc.contributor.author");
        configuration.setRightPrimaryMetadataField("person.identifier.orcid");

        assertTrue(configuration.isOwnerOnLeft("dc.contributor.author"));
        assertFalse(configuration.isOwnerOnLeft("person.identifier.orcid"));
    }

    @Test
    public void testIsOwnerOnLeftRejectsUnknownField() {
        configuration.setLeftPrimaryMetadataField("dc.contributor.author");

        assertThrows(IllegalArgumentException.class,
                     () -> configuration.isOwnerOnLeft("dc.title"));
    }

    @Test
    public void testIsOwnerOnLeftRejectsAmbiguousField() {
        configuration.setLeftPrimaryMetadataField("dc.contributor.author");
        configuration.setRightPrimaryMetadataField("dc.contributor.author");

        assertThrows(IllegalArgumentException.class,
                     () -> configuration.isOwnerOnLeft("dc.contributor.author"));
    }

    @Test
    public void testMapsFieldMatchesPrimaryMetadataOnly() {
        ItemAuthorityExtraMetadataGenerator generator =
            mock(ItemAuthorityExtraMetadataGenerator.class);

        configuration.setLeftPrimaryMetadataField("dc.contributor.author");
        configuration.setRightPrimaryMetadataField("dc.relation.isAuthorOfPublication");
        configuration.setLeftMetadataGenerators(
            Map.of("person.identifier.orcid", generator));

        assertTrue(configuration.mapsField("dc.contributor.author"));
        assertTrue(configuration.mapsField("dc.relation.isAuthorOfPublication"));

        // Additional metadata must not be treated as a primary relationship field.
        assertFalse(configuration.mapsField("person.identifier.orcid"));
        assertFalse(configuration.mapsField("dc.title"));
    }

    @Test
    public void testMapsFieldWithNoPrimaryMetadata() {
        assertFalse(configuration.mapsField("dc.contributor.author"));
    }

    @Test
    public void testExistingMetadataGeneratorBeanCanBeReused() {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
            "org/dspace/content/relationship-configuration-test.xml")) {

            RelationshipTypeConfiguration configuration =
                context.getBean("publicationAuthorRelationship", RelationshipTypeConfiguration.class);

            ItemAuthorityExtraMetadataGenerator generator =
                context.getBean("authorOrcidMetadataGenerator", ItemAuthorityExtraMetadataGenerator.class);

            assertEquals("publication-author", configuration.getId());
            assertSame(generator, configuration.getLeftMetadataGenerators().get("person.identifier.orcid"));
            assertSame(generator, configuration.getRightMetadataGenerators().get("person.identifier.orcid"));
        }
    }
}
