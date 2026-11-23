/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.dspace.content.enhancer.generator.impl.SourceFieldMetadataGenerator;
import org.dspace.discovery.indexobject.ItemIndexFactoryImpl;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for {@link ItemSimpleAuthorityMetadataGenerator}.
 *
 * <p>These tests build a {@link SolrDocument} by hand, in the format written by the item indexer,
 * and check the extras and the aggregate choices produced by the generator.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class ItemSimpleAuthorityMetadataGeneratorTest {

    private static final String SOURCE_FIELD = "person.affiliation.name";

    private static final String STORED_FIELD = SOURCE_FIELD + "_stored";

    private static final String KEY_ID = "oairecerif_author_affiliation";

    private static final String DATA_KEY_ID = "data-" + KEY_ID;

    private static final String TITLE = "Author 1";

    private static final String RESOURCE_ID = "resource-id";

    private SolrDocument document;

    @Before
    public void setUp() {
        document = new SolrDocument();
        document.addField("dc.title", TITLE);
        document.addField("search.resourceid", RESOURCE_ID);
    }

    @Test
    public void testBuildExtractsValueWithAuthorityFromSolrDocument() {
        document.addField(STORED_FIELD, stored("University of Rome", "null", "null", "uuid-1", "en"));

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection(SOURCE_FIELD)) {
            Map<String, String> extras = generator(SOURCE_FIELD).build("AuthorAuthority", document);

            assertEquals("University of Rome::uuid-1", extras.get(KEY_ID));
            assertEquals("University of Rome::uuid-1", extras.get(DATA_KEY_ID));
        }
    }

    @Test
    public void testBuildExtractsValueWithoutAuthorityFromSolrDocument() {
        document.addField(STORED_FIELD, stored("University of Rome", "null", "null", "null", "en"));

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection(SOURCE_FIELD)) {
            Map<String, String> extras = generator(SOURCE_FIELD).build("AuthorAuthority", document);

            assertEquals("University of Rome", extras.get(KEY_ID));
        }
    }

    @Test
    public void testBuildJoinsMultipleValuesWithSeparator() {
        document.addField(STORED_FIELD, stored("University of Rome", "null", "null", "uuid-1", "en"));
        document.addField(STORED_FIELD, stored("University of Milan", "null", "null", "uuid-2", "it"));

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection(SOURCE_FIELD)) {
            Map<String, String> extras = generator(SOURCE_FIELD).build("AuthorAuthority", document);

            assertEquals("University of Rome::uuid-1|||University of Milan::uuid-2", extras.get(KEY_ID));
        }
    }

    @Test
    public void testBuildSuppressesDuplicatedValues() {
        document.addField(STORED_FIELD, stored("University of Rome", "null", "null", "uuid-1", "en"));
        document.addField(STORED_FIELD, stored("University of Rome", "null", "null", "uuid-1", "en"));

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection(SOURCE_FIELD)) {
            Map<String, String> extras = generator(SOURCE_FIELD).build("AuthorAuthority", document);

            assertEquals("University of Rome::uuid-1", extras.get(KEY_ID));
        }
    }

    @Test
    public void testBuildAddsOnlyDataExtraWhenUseForDisplayIsFalse() {
        document.addField(STORED_FIELD, stored("University of Rome", "null", "null", "uuid-1", "en"));

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection(SOURCE_FIELD)) {
            ItemSimpleAuthorityMetadataGenerator generator = generator(SOURCE_FIELD);
            generator.setUseForDisplay(false);

            Map<String, String> extras = generator.build("AuthorAuthority", document);

            assertEquals("University of Rome::uuid-1", extras.get(DATA_KEY_ID));
            assertFalse(extras.containsKey(KEY_ID));
        }
    }

    @Test
    public void testBuildAddsOnlyDisplayExtraWhenUseAsDataIsFalse() {
        document.addField(STORED_FIELD, stored("University of Rome", "null", "null", "uuid-1", "en"));

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection(SOURCE_FIELD)) {
            ItemSimpleAuthorityMetadataGenerator generator = generator(SOURCE_FIELD);
            generator.setUseAsData(false);

            Map<String, String> extras = generator.build("AuthorAuthority", document);

            assertEquals("University of Rome::uuid-1", extras.get(KEY_ID));
            assertFalse(extras.containsKey(DATA_KEY_ID));
        }
    }

    @Test
    public void testBuildSkipsExtractionWhenAuthorityNameDoesNotMatch() {
        document.addField(STORED_FIELD, stored("University of Rome", "null", "null", "uuid-1", "en"));

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection(SOURCE_FIELD)) {
            ItemSimpleAuthorityMetadataGenerator generator = generator(SOURCE_FIELD);
            generator.setAuthorityName("AuthorAuthority");

            assertTrue(generator.build("EditorAuthority", document).isEmpty());
        }
    }

    @Test
    public void testBuildMatchesConfiguredAuthorityName() {
        document.addField(STORED_FIELD, stored("University of Rome", "null", "null", "uuid-1", "en"));

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection(SOURCE_FIELD)) {
            ItemSimpleAuthorityMetadataGenerator generator = generator(SOURCE_FIELD);
            generator.setAuthorityName("AuthorAuthority");

            assertEquals("University of Rome::uuid-1", generator.build("AuthorAuthority", document).get(KEY_ID));
        }
    }

    @Test
    public void testBuildReturnsNoExtrasWhenFieldIsNotInProjection() {
        document.addField(STORED_FIELD, stored("University of Rome", "null", "null", "uuid-1", "en"));

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection("dc.contributor.author")) {
            assertTrue(generator(SOURCE_FIELD).build("AuthorAuthority", document).isEmpty());
        }
    }

    @Test
    public void testBuildReturnsNoExtrasWhenProjectedFieldIsAbsent() {
        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection(SOURCE_FIELD)) {
            assertTrue(generator(SOURCE_FIELD).build("AuthorAuthority", document).isEmpty());
        }
    }

    @Test
    public void testBuildAcceptsWildcardProjectionForFieldWithoutQualifier() {
        String sourceField = "person.affiliation";
        document.addField(sourceField + "_stored", stored("University of Rome", "null", "null", "uuid-1", "en"));

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection("person.affiliation.*")) {
            assertEquals("University of Rome::uuid-1", generator(sourceField).build("AuthorAuthority", document)
                                                                             .get(KEY_ID));
        }
    }

    @Test
    public void testBuildAcceptsWildcardProjection() {
        document.addField(STORED_FIELD, stored("University of Rome", "null", "null", "uuid-1", "en"));

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection("person.affiliation.*")) {
            assertEquals("University of Rome::uuid-1", generator(SOURCE_FIELD).build("AuthorAuthority", document)
                                                                              .get(KEY_ID));
        }
    }

    @Test
    public void testBuildSkipsLiteralNullValuesAndToleratesShortEntries() {
        document.addField(STORED_FIELD, stored("null", "null", "null", "null", "en"));
        document.addField(STORED_FIELD, "Short entry");

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection(SOURCE_FIELD)) {
            Map<String, String> extras = generator(SOURCE_FIELD).build("AuthorAuthority", document);

            assertEquals("Short entry", extras.get(KEY_ID));
        }
    }

    @Test
    public void testBuildAggregateCreatesSingleChoiceWhenSingleResultOnAggregate() {
        document.addField(STORED_FIELD, stored("University of Rome", "null", "null", "uuid-1", "en"));
        document.addField(STORED_FIELD, stored("University of Milan", "null", "null", "uuid-2", "it"));

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection(SOURCE_FIELD)) {
            List<Choice> choices = generator(SOURCE_FIELD).buildAggregate("AuthorAuthority", document);

            assertEquals(1, choices.size());
            assertEquals(TITLE, choices.get(0).label);
            assertEquals("University of Rome::uuid-1|||University of Milan::uuid-2",
                         choices.get(0).extras.get(KEY_ID));
        }
    }

    @Test
    public void testBuildAggregateSupportsMultiValuedTitleAndResourceIdFields() {
        document.removeFields("dc.title");
        document.removeFields("search.resourceid");
        document.addField("dc.title", List.of(TITLE));
        document.addField("search.resourceid", List.of(RESOURCE_ID));

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection(SOURCE_FIELD)) {
            List<Choice> choices = generator(SOURCE_FIELD).buildAggregate("AuthorAuthority", document);

            assertEquals(1, choices.size());
            assertEquals(RESOURCE_ID, choices.get(0).authority);
            assertEquals(TITLE, choices.get(0).label);
        }
    }

    @Test
    public void testBuildAggregateCreatesOneChoicePerValue() {
        document.addField(STORED_FIELD, stored("University of Rome", "null", "null", "uuid-1", "en"));
        document.addField(STORED_FIELD, stored("University of Milan", "null", "null", "uuid-2", "it"));

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection(SOURCE_FIELD)) {
            ItemSimpleAuthorityMetadataGenerator generator = generator(SOURCE_FIELD);
            generator.setSingleResultOnAggregate(false);

            List<Choice> choices = generator.buildAggregate("AuthorAuthority", document);

            assertEquals(2, choices.size());
            assertEquals(TITLE + "(University of Rome)", choices.get(0).label);
            assertEquals("University of Rome::uuid-1", choices.get(0).extras.get(KEY_ID));
            assertEquals(TITLE + "(University of Milan)", choices.get(1).label);
            assertEquals("University of Milan::uuid-2", choices.get(1).extras.get(KEY_ID));
        }
    }

    @Test
    public void testBuildAggregateCreatesPlainChoiceWhenThereAreNoValues() {
        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection(SOURCE_FIELD)) {
            ItemSimpleAuthorityMetadataGenerator generator = generator(SOURCE_FIELD);
            generator.setSingleResultOnAggregate(false);

            List<Choice> choices = generator.buildAggregate("AuthorAuthority", document);

            assertEquals(1, choices.size());
            assertEquals(TITLE, choices.get(0).label);
            assertTrue(choices.get(0).extras.isEmpty());
        }
    }

    @Test
    public void testBuildAggregateCreatesPlainChoiceWhenAuthorityNameDoesNotMatch() {
        document.addField(STORED_FIELD, stored("University of Rome", "null", "null", "uuid-1", "en"));

        try (MockedStatic<DSpaceServicesFactory> ignored = mockProjection(SOURCE_FIELD)) {
            ItemSimpleAuthorityMetadataGenerator generator = generator(SOURCE_FIELD);
            generator.setAuthorityName("AuthorAuthority");
            generator.setSingleResultOnAggregate(false);

            List<Choice> choices = generator.buildAggregate("EditorAuthority", document);

            assertEquals(1, choices.size());
            assertEquals(TITLE, choices.get(0).label);
            assertTrue(choices.get(0).extras.isEmpty());
        }
    }

    @Test
    public void testAfterPropertiesSetFailsWithoutDelegate() {
        ItemSimpleAuthorityMetadataGenerator generator = new ItemSimpleAuthorityMetadataGenerator();
        generator.setKeyId(KEY_ID);

        IllegalStateException exception = assertThrows(IllegalStateException.class, generator::afterPropertiesSet);

        assertTrue(exception.getMessage().contains(KEY_ID));
    }

    @Test
    public void testAfterPropertiesSetSucceedsWithDelegate() {
        generator(SOURCE_FIELD).afterPropertiesSet();
    }

    private ItemSimpleAuthorityMetadataGenerator generator(String sourceField) {
        SourceFieldMetadataGenerator delegate = new SourceFieldMetadataGenerator();
        delegate.setSourceField(sourceField);
        ItemSimpleAuthorityMetadataGenerator generator = new ItemSimpleAuthorityMetadataGenerator();
        generator.setKeyId(KEY_ID);
        generator.setDelegate(delegate);
        return generator;
    }

    private MockedStatic<DSpaceServicesFactory> mockProjection(String... projectionFields) {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.getArrayProperty("discovery.index.projection")).thenReturn(projectionFields);
        DSpaceServicesFactory servicesFactory = mock(DSpaceServicesFactory.class);
        when(servicesFactory.getConfigurationService()).thenReturn(configurationService);
        MockedStatic<DSpaceServicesFactory> mockedStatic = Mockito.mockStatic(DSpaceServicesFactory.class);
        mockedStatic.when(DSpaceServicesFactory::getInstance).thenReturn(servicesFactory);
        return mockedStatic;
    }

    private static String stored(String value, String label, String variants, String authority, String language) {
        String separator = ItemIndexFactoryImpl.STORE_SEPARATOR;
        return value + separator + label + separator + variants + separator + authority + separator + language;
    }

}
