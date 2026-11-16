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
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.dspace.content.enhancer.generator.RelationshipMetadataGenerator;
import org.dspace.content.enhancer.generator.impl.SourceFieldMetadataGenerator;
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
        configuration.setId("authority:dc.contributor.author");
    }

    @Test
    public void testSetId() {
        assertEquals("authority:dc.contributor.author", configuration.getId());
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
        RelationshipMetadataGenerator leftGenerator =
            mock(RelationshipMetadataGenerator.class);
        RelationshipMetadataGenerator rightGenerator =
            mock(RelationshipMetadataGenerator.class);

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
        RelationshipMetadataGenerator generator =
            mock(RelationshipMetadataGenerator.class);

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
        RelationshipMetadataGenerator generator =
            mock(RelationshipMetadataGenerator.class);

        Map<String, RelationshipMetadataGenerator> generators = new HashMap<>();
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
        Map<String, RelationshipMetadataGenerator> generators = new HashMap<>();
        generators.put("person.identifier.orcid", null);

        assertThrows(IllegalArgumentException.class,
                     () -> configuration.setLeftMetadataGenerators(generators));
        assertThrows(IllegalArgumentException.class,
                     () -> configuration.setRightMetadataGenerators(generators));
    }

    @Test
    public void testNullMetadataGeneratorMessagesIdentifyEndpoint() {
        Map<String, RelationshipMetadataGenerator> generators = new HashMap<>();
        generators.put("person.identifier.orcid", null);

        IllegalArgumentException left = assertThrows(IllegalArgumentException.class,
                                                    () -> configuration.setLeftMetadataGenerators(generators));
        IllegalArgumentException right = assertThrows(IllegalArgumentException.class,
                                                     () -> configuration.setRightMetadataGenerators(generators));

        assertEquals("Metadata generators cannot be null on the left endpoint", left.getMessage());
        assertEquals("Metadata generators cannot be null on the right endpoint", right.getMessage());
    }

    @Test
    public void testMetadataGeneratorSourceFieldIsAvailableThroughConfiguration() {
        RelationshipMetadataGenerator generator = mock(RelationshipMetadataGenerator.class);
        when(generator.getSourceField()).thenReturn("person.identifier.orcid");

        configuration.setLeftMetadataGenerators(Map.of("additional.identifier.orcid", generator));

        RelationshipMetadataGenerator configured =
            configuration.getLeftMetadataGenerators().get("additional.identifier.orcid");

        assertSame(generator, configured);
        assertEquals("person.identifier.orcid", configured.getSourceField());
        assertEquals(Set.of("additional.identifier.orcid"), configuration.getLeftAdditionalMetadataFields());
    }

    @Test
    public void testIsOwnerOnLeft() {
        configuration.setLeftPrimaryMetadataField("dc.contributor.author");
        configuration.setRightPrimaryMetadataField("person.identifier.orcid");

        assertTrue(configuration.isOwnerOnLeft("dc.contributor.author", "Publication"));
        assertFalse(configuration.isOwnerOnLeft("person.identifier.orcid", "Person"));
    }

    @Test
    public void testIsOwnerOnLeftRejectsUnknownField() {
        configuration.setLeftPrimaryMetadataField("dc.contributor.author");

        assertThrows(IllegalArgumentException.class,
                     () -> configuration.isOwnerOnLeft("dc.title", "Publication"));
    }

    @Test
    public void testIsOwnerOnLeftRejectsAmbiguousField() {
        configuration.setLeftPrimaryMetadataField("dc.contributor.author");
        configuration.setRightPrimaryMetadataField("dc.contributor.author");

        assertThrows(IllegalArgumentException.class,
                     () -> configuration.isOwnerOnLeft("dc.contributor.author", "Publication"));
    }

    @Test
    public void testMapsFieldMatchesPrimaryMetadataOnly() {
        RelationshipMetadataGenerator generator =
            mock(RelationshipMetadataGenerator.class);

        configuration.setLeftPrimaryMetadataField("dc.contributor.author");
        configuration.setRightPrimaryMetadataField("dc.relation.isAuthorOfPublication");
        configuration.setLeftMetadataGenerators(
            Map.of("person.identifier.orcid", generator));

        assertTrue(configuration.mapsField("dc.contributor.author", "Publication"));
        assertTrue(configuration.mapsField("dc.relation.isAuthorOfPublication", "Person"));

        // Additional metadata must not be treated as a primary relationship field.
        assertFalse(configuration.mapsField("person.identifier.orcid", "Person"));
        assertFalse(configuration.mapsField("dc.title", "Publication"));
    }

    @Test
    public void testMapsFieldWithNoPrimaryMetadata() {
        assertFalse(configuration.mapsField("dc.contributor.author", "Publication"));
    }

    @Test
    public void testInlineMetadataGeneratorLoadedFromSpringXml() {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
            "org/dspace/content/relationship-configuration-test.xml")) {

            RelationshipTypeConfiguration configuration =
                context.getBean("testPublicationAuthorRelationship", RelationshipTypeConfiguration.class);

            assertEquals("authority:ds-rel-test.contributor.author", configuration.getId());

            RelationshipMetadataGenerator generator =
                configuration.getLeftMetadataGenerators().get("additional.test.orcid");

            assertTrue(generator instanceof SourceFieldMetadataGenerator);
            assertEquals("person.identifier.orcid", generator.getSourceField());
            assertTrue(configuration.getRightMetadataGenerators().isEmpty());
        }
    }

    @Test
    public void testIsOwnerOnLeftWithSameFieldAndDifferentEntityTypes() {
        configuration.setLeftEntityTypes(Set.of("Publication"));
        configuration.setRightEntityTypes(Set.of("DataSet"));
        configuration.setLeftPrimaryMetadataField("dc.title");
        configuration.setRightPrimaryMetadataField("dc.title");

        assertTrue(configuration.isOwnerOnLeft("dc.title", "Publication"));
        assertFalse(configuration.isOwnerOnLeft("dc.title", "DataSet"));

        assertTrue(configuration.mapsField("dc.title", "Publication"));
        assertTrue(configuration.mapsField("dc.title", "DataSet"));
        assertFalse(configuration.mapsField("dc.title", "Person"));
    }

    @Test
    public void testIsOwnerOnLeftRejectsUnsupportedEntityType() {
        configuration.setLeftEntityTypes(Set.of("Publication"));
        configuration.setRightEntityTypes(Set.of("Person"));
        configuration.setLeftPrimaryMetadataField("dc.title");

        assertThrows(IllegalArgumentException.class,
                     () -> configuration.isOwnerOnLeft("dc.title", "DataSet"));
    }
}
