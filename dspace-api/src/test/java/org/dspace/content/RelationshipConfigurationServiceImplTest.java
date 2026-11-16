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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.enhancer.generator.RelationshipMetadataGenerator;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Constants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link RelationshipConfigurationServiceImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RelationshipConfigurationServiceImplTest extends AbstractIntegrationTestWithDatabase {

    @Mock
    private ChoiceAuthorityService choiceAuthorityService;

    @Mock
    private ItemService itemService;

    @Mock
    private MetadataFieldService metadataFieldService;

    @InjectMocks
    private RelationshipConfigurationServiceImpl service;

    private RelationshipTypeConfiguration configuration;
    private Relationship relationship;
    private Item leftItem;
    private Item rightItem;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent community")
                                          .build();
        Collection publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                            .withName("Collection")
                                                            .withEntityType("Publication")
                                                            .build();
        Collection personCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                       .withName("Collection")
                                                       .withEntityType("Person")
                                                       .build();

        leftItem = ItemBuilder.createItem(context, publicationCollection).withTitle("Left Item Publication").build();
        rightItem = ItemBuilder.createItem(context, personCollection).withTitle("Right Item Person").build();

        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType person = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        RelationshipType type = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publication, person,
                                           "isAuthorOfPublication", "isPublicationOfAuthor",
                                           0, 10, 0, 10).build();

        relationship = RelationshipBuilder.createRelationshipBuilder(context, leftItem, rightItem, type).build();
        relationship.setRelationshipConfigKey("authority:dc.contributor.author");

        configuration = configuration("authority:dc.contributor.author", "dc.contributor.author",
                                      "dc.relation.isAuthorOfPublication");
        setConfigurations(configuration);

        context.restoreAuthSystemState();
    }

    private void mockOwnerEntityTypes() {
        when(itemService.getEntityTypeLabel(leftItem)).thenReturn("Publication");
        when(itemService.getEntityTypeLabel(rightItem)).thenReturn("Person");
    }

    /**
     * Creates a relationship configuration between Publication and Person entities.
     */
    private RelationshipTypeConfiguration configuration(String id, String leftField, String rightField) {
        RelationshipTypeConfiguration result = new RelationshipTypeConfiguration();
        result.setId(id);
        result.setLeftEntityTypes(Set.of("Publication"));
        result.setRightEntityTypes(Set.of("Person"));
        result.setLeftPrimaryMetadataField(leftField);
        result.setRightPrimaryMetadataField(rightField);
        return result;
    }

    /**
     * Sets the relationship configurations used by the service in tests.
     */
    private void setConfigurations(RelationshipTypeConfiguration... configurations) {
        ReflectionTestUtils.setField(service, "configurations", Arrays.asList(configurations));
    }

    /**
     * Creates a mocked metadata value with the specified field components.
     */
    private MetadataValue metadataValue(String schema, String element, String qualifier, DSpaceObject dso) {
        MetadataSchema metadataSchema = new MetadataSchema();
        metadataSchema.setName(schema);

        MetadataField metadataField = new MetadataField();
        metadataField.setMetadataSchema(metadataSchema);
        metadataField.setElement(element);
        metadataField.setQualifier(qualifier);

        MetadataValue mdv = new MetadataValue();
        mdv.setDSpaceObject(dso);
        mdv.setMetadataField(metadataField);
        return mdv;
    }

    /**
     * Mocks metadata field lookup to treat all fields as registered.
     */
    private void registerMetadataFields() throws SQLException {
        when(metadataFieldService.findByString(eq(context), any(String.class), eq('.')))
            .thenReturn(mock(MetadataField.class));
    }

    /**
     * Mocks a linked-entity authority for the given field and target entity types.
     */
    private void enableAuthority(String field, String... entityTypes) {
        String authorityKey = field.replace('.', '_');

        when(choiceAuthorityService.isChoicesConfigured(
            eq(authorityKey), eq(Constants.ITEM), isNull(Collection.class)
        )).thenReturn(true);

        when(choiceAuthorityService.getLinkedEntityTypes(authorityKey)).thenReturn(entityTypes);
    }

    /**
     * Verifies that duplicate configuration IDs are rejected at startup.
     */
    @Test
    public void testDuplicateIdsRejectedAtStartup() {
        setConfigurations(configuration, configuration("authority:dc.contributor.author", "dc.title", null));
        assertThrows(IllegalArgumentException.class, () -> service.validateConfigurations());
    }

    /**
     * Verifies that endpoint entity types are required.
     */
    @Test
    public void testMissingLeftEntityTypesRejectedAtStartup() {
        configuration.setLeftEntityTypes(Set.of());
        assertThrows(IllegalArgumentException.class, () -> service.validateConfigurations());
    }

    /**
     * Verifies that a configuration can omit both primary metadata fields.
     */
    @Test
    public void testConfigurationWithoutPrimaryFieldsIsAllowed() {
        configuration.setLeftPrimaryMetadataField(null);
        configuration.setRightPrimaryMetadataField(null);
        service.validateConfigurations();
    }

    /**
     * Verifies that invalid primary metadata field syntax is rejected with a configuration exception.
     */
    @Test
    public void testInvalidPrimaryMetadataFieldRejectedAtStartup() {
        configuration.setLeftPrimaryMetadataField("dc..author");

        RelationshipConfigurationException exception =
            assertThrows(RelationshipConfigurationException.class,
                         () -> service.validateConfigurations());

        assertTrue(exception.getMessage().contains("authority:dc.contributor.author"));
        assertTrue(exception.getMessage().contains("dc..author"));
    }

    /**
     * Verifies that a primary field cannot also be configured as additional metadata.
     */
    @Test
    public void testPrimaryFieldCannotAlsoBeAdditionalOnLeft() {
        RelationshipMetadataGenerator generator = mock(RelationshipMetadataGenerator.class);
        configuration.setLeftMetadataGenerators(Map.of("dc.contributor.author", generator));
        assertThrows(IllegalArgumentException.class, () -> service.validateConfigurations());
    }

    /**
     * Verifies that explicit relationship configurations can be resolved by ID.
     */
    @Test
    public void testGetRelationshipConfigResolvesExplicitConfiguration() throws SQLException {
        registerMetadataFields();
        assertSame(configuration, service.getRelationshipConfig(context, relationship));
    }

    /**
     * Verifies that either endpoint's primary metadata field resolves the configuration.
     */
    @Test
    public void testFindForMetadataValueMatchesEitherPrimaryField() throws SQLException {
        registerMetadataFields();
        mockOwnerEntityTypes();
        assertSame(configuration,
                   service.findForMetadataValue(context, metadataValue("dc", "contributor", "author", leftItem)));
        assertSame(configuration,
                   service.findForMetadataValue(context,
                                                metadataValue("dc", "relation", "isAuthorOfPublication", rightItem)));
    }

    /**
     * Verifies that relationships can define primary metadata on only one endpoint.
     */
    @Test
    public void testOneSidedPrimaryMetadataConfiguration() throws SQLException {
        configuration.setRightPrimaryMetadataField(null);
        registerMetadataFields();
        when(itemService.getEntityTypeLabel(leftItem)).thenReturn("Publication");

        assertSame(configuration,
                   service.findForMetadataValue(context, metadataValue("dc", "contributor", "author", leftItem)));

        assertTrue(service.getPrimaryMetadataField(context, relationship, rightItem).isEmpty());
    }

    /**
     * Verifies that additional metadata fields are not used to resolve relationships.
     */
    @Test
    public void testFindForMetadataValueDoesNotMatchAdditionalFields() throws SQLException {
        when(itemService.getEntityTypeLabel(leftItem)).thenReturn("Publication");
        RelationshipMetadataGenerator generator = mock(RelationshipMetadataGenerator.class);
        configuration.setLeftMetadataGenerators(Map.of("person.identifier.orcid", generator));
        assertNull(service.findForMetadataValue(context, metadataValue("person", "identifier", "orcid", leftItem)));
    }

    /**
     * Verifies that ambiguous primary metadata mappings are rejected.
     */
    @Test
    public void testFindForMetadataValueRejectsAmbiguousPrimaryField() {
        when(itemService.getEntityTypeLabel(leftItem)).thenReturn("Publication");

        setConfigurations(configuration, configuration("another", "dc.contributor.author", null));
        assertThrows(IllegalArgumentException.class,
                     () -> service.findForMetadataValue(context,
                                                        metadataValue("dc", "contributor", "author", leftItem)));
    }

    /**
     * Verifies that primary metadata fields must exist in the metadata registry.
     */
    @Test
    public void testGetRelationshipConfigRejectsNonexistentPrimaryMetadataField() {
        RelationshipConfigurationException exception =
            assertThrows(RelationshipConfigurationException.class,
                         () -> service.getRelationshipConfig(context, relationship));

        assertTrue(exception.getMessage().contains("authority:dc.contributor.author"));
        assertTrue(exception.getMessage().contains("primary metadata field"));
    }

    /**
     * Verifies that unregistered additional metadata fields are ignored.
     */
    @Test
    public void testNonexistentAdditionalMetadataFieldIsIgnored() throws SQLException {
        RelationshipMetadataGenerator generator = mock(RelationshipMetadataGenerator.class);
        configuration.setLeftMetadataGenerators(Map.of("person.identifier.orcid", generator));

        when(metadataFieldService.findByString(eq(context), any(String.class), eq('.')))
            .thenAnswer(invocation -> {
                String field = invocation.getArgument(1);
                return field.startsWith("person.") ? null : mock(MetadataField.class);
            });

        RelationshipTypeConfiguration resolved = service.getRelationshipConfig(context, relationship);

        assertTrue(resolved.getLeftAdditionalMetadataFields().isEmpty());
        assertTrue(resolved.getLeftMetadataGenerators().isEmpty());

        // The original Spring configuration remains unchanged.
        assertEquals(Set.of("person.identifier.orcid"), configuration.getLeftAdditionalMetadataFields());
    }

    /**
     * Verifies that invalid additional metadata field syntax is rejected at startup.
     */
    @Test
    public void testInvalidAdditionalMetadataFieldRejectedAtStartup() {
        RelationshipMetadataGenerator generator = mock(RelationshipMetadataGenerator.class);
        configuration.setLeftMetadataGenerators(Map.of("dc..author", generator));

        RelationshipConfigurationException exception =
            assertThrows(RelationshipConfigurationException.class,
                         () -> service.validateConfigurations());

        assertTrue(exception.getMessage().contains("authority:dc.contributor.author"));
        assertTrue(exception.getMessage().contains("dc..author"));
    }

    /**
     * Verifies that a generator whose source metadata field is not registered is dropped, following the
     * same severity rule as a missing additional field.
     */
    @Test
    public void testNonexistentSourceMetadataFieldDropsGenerator() throws SQLException {
        RelationshipMetadataGenerator generator = mock(RelationshipMetadataGenerator.class);
        when(generator.getSourceField()).thenReturn("person.identifier.orcid");
        configuration.setLeftMetadataGenerators(Map.of("dc.description", generator));

        when(metadataFieldService.findByString(eq(context), any(String.class), eq('.')))
            .thenAnswer(invocation -> "person.identifier.orcid".equals(invocation.getArgument(1))
                ? null
                : mock(MetadataField.class));
        mockOwnerEntityTypes();

        RelationshipTypeConfiguration resolved = service.getRelationshipConfig(context, relationship);

        assertTrue(resolved.getLeftMetadataGenerators().isEmpty());
        assertTrue(resolved.getLeftAdditionalMetadataFields().isEmpty());
    }

    /**
     * Verifies that a generator with a missing target field is dropped without validating its source field.
     */
    @Test
    public void testMissingAdditionalFieldDropsGeneratorWithoutValidatingSource() throws SQLException {
        RelationshipMetadataGenerator generator = mock(RelationshipMetadataGenerator.class);
        configuration.setLeftMetadataGenerators(Map.of("person.identifier.orcid", generator));

        when(metadataFieldService.findByString(eq(context), any(String.class), eq('.')))
            .thenAnswer(invocation -> ((String) invocation.getArgument(1)).startsWith("person.")
                ? null
                : mock(MetadataField.class));
        mockOwnerEntityTypes();

        RelationshipTypeConfiguration resolved = service.getRelationshipConfig(context, relationship);

        assertTrue(resolved.getLeftMetadataGenerators().isEmpty());
        assertTrue(resolved.getLeftAdditionalMetadataFields().isEmpty());
    }

    /**
     * Verifies that a generator without a usable source metadata field is rejected at startup.
     */
    @Test
    public void testMissingSourceMetadataFieldRejectedAtStartup() {
        RelationshipMetadataGenerator generator = mock(RelationshipMetadataGenerator.class);
        configuration.setLeftMetadataGenerators(Map.of("person.identifier.orcid", generator));

        RelationshipConfigurationException exception =
            assertThrows(RelationshipConfigurationException.class,
                         () -> service.validateConfigurations());

        assertTrue(exception.getMessage().contains("authority:dc.contributor.author"));
        assertTrue(exception.getMessage().contains("source metadata field"));
    }

    /**
     * Verifies that linked-entity authority settings are used when no explicit relationship configuration exists.
     */
    @Test
    public void testAuthorityFallbackResolvesValidField() throws SQLException {
        setConfigurations();
        enableAuthority("dc.contributor.author", "Person");
        registerMetadataFields();

        RelationshipTypeConfiguration result =
            service.findForMetadataValue(context, metadataValue("dc", "contributor", "author", leftItem));

        assertEquals("authority:dc.contributor.author", result.getId());
        assertEquals("dc.contributor.author", result.getLeftPrimaryMetadataField().orElse(null));
        assertEquals(Set.of("Person"), result.getRightEntityTypes());
        assertTrue(result.getLeftEntityTypes().isEmpty());
    }

    /**
     * Verifies that dynamically resolved authority fields are validated for correct syntax.
     */
    @Test
    public void testAuthorityFallbackRejectsMalformedMetadataField() {
        setConfigurations();
        enableAuthority("dc..author", "Person");

        relationship.setRelationshipConfigKey("authority:dc..author");
        RelationshipConfigurationException exception =
            assertThrows(RelationshipConfigurationException.class,
                         () -> service.getRelationshipConfig(context, relationship));

        assertTrue(exception.getMessage().contains("dc..author"));
        assertTrue(exception.getMessage().contains("left endpoint"));
    }

    /**
     * Verifies that unconfigured linked-entity authorities do not resolve.
     */
    @Test
    public void testUnconfiguredAuthorityDoesNotResolve() throws SQLException {
        setConfigurations();
        assertNull(service.findForMetadataValue(context, metadataValue("dc", "contributor", "author", leftItem)));
        assertThrows(IllegalArgumentException.class,
                     () -> service.getRelationshipConfig(context, relationship));
    }

    /**
     * Verifies primary metadata, additional metadata, and generator lookup for both endpoints.
     */
    @Test
    public void testMetadataHelpersForBothEndpoints() throws SQLException {
        RelationshipMetadataGenerator leftGenerator = mock(RelationshipMetadataGenerator.class);
        RelationshipMetadataGenerator rightGenerator = mock(RelationshipMetadataGenerator.class);
        when(leftGenerator.getSourceField()).thenReturn("person.identifier.orcid");
        when(rightGenerator.getSourceField()).thenReturn("dc.description");
        configuration.setLeftMetadataGenerators(Map.of("person.identifier.orcid", leftGenerator));
        configuration.setRightMetadataGenerators(Map.of("dc.description", rightGenerator));
        registerMetadataFields();

        assertEquals("dc.contributor.author",
                     service.getPrimaryMetadataField(context, relationship, leftItem).orElse(null));
        assertEquals("dc.relation.isAuthorOfPublication",
                     service.getPrimaryMetadataField(context, relationship, rightItem).orElse(null));

        MetadataValue leftPrimary = metadataValue("dc", "contributor", "author", leftItem);
        MetadataValue rightPrimary = metadataValue("dc", "relation", "isAuthorOfPublication", rightItem);

        assertTrue(service.isPrimaryMetadata(context, relationship, leftPrimary));
        assertTrue(service.isPrimaryMetadata(context, relationship, rightPrimary));

        assertEquals(Set.of("person.identifier.orcid"),
                     service.getAdditionalMetadataFields(context, relationship, leftItem));
        assertEquals(Set.of("dc.description"),
                     service.getAdditionalMetadataFields(context, relationship, rightItem));

        MetadataValue leftAdditional = metadataValue("person", "identifier", "orcid", leftItem);
        MetadataValue rightAdditional = metadataValue("dc", "description", null, rightItem);

        assertTrue(service.isAdditionalMetadata(context, relationship, leftAdditional));
        assertTrue(service.isAdditionalMetadata(context, relationship, rightAdditional));

        assertFalse(service.isAdditionalMetadata(context, relationship, leftPrimary));
        assertFalse(service.isAdditionalMetadata(context, relationship, rightPrimary));

        assertSame(leftGenerator,
                   service.getMetadataGenerator(context, relationship, leftItem, "person.identifier.orcid")
                          .orElse(null));
        assertSame(rightGenerator,
                   service.getMetadataGenerator(context, relationship, rightItem, "dc.description").orElse(null));

        assertTrue(service.getMetadataGenerator(context, relationship, leftItem, "dc.title").isEmpty());
    }

    /**
     * Verifies that concrete relationships require archived items.
     */
    @Test
    public void testValidateRequiresArchivedItems() {
        Item left = mock(Item.class);
        Item right = mock(Item.class);
        assertThrows(IllegalArgumentException.class,
                     () -> service.validate(context, configuration, left, right));
    }

    /**
     * Verifies that relationship endpoints must have an allowed entity type.
     */
    @Test
    public void testValidateRejectsIncorrectEntityType() {
        Item left = mock(Item.class);
        Item right = mock(Item.class);
        when(left.isArchived()).thenReturn(true);
        when(right.isArchived()).thenReturn(true);
        when(itemService.getEntityTypeLabel(left)).thenReturn("Collection");

        assertThrows(IllegalArgumentException.class,
                     () -> service.validate(context, configuration, left, right));
    }

    /**
     * Verifies that relationship endpoints are accepted in their configured orientation.
     */
    @Test
    public void testValidateAcceptsMatchingEndpointTypes() throws SQLException {
        Item left = mock(Item.class);
        Item right = mock(Item.class);

        when(left.isArchived()).thenReturn(true);
        when(right.isArchived()).thenReturn(true);
        when(itemService.getEntityTypeLabel(left)).thenReturn("Publication");
        when(itemService.getEntityTypeLabel(right)).thenReturn("Person");

        service.validate(context, configuration, left, right);
    }

    /**
     * Verifies that unarchived items are accepted when explicitly allowed.
     */
    @Test
    public void testValidateCanAllowUnarchivedItems() throws SQLException {
        Item left = mock(Item.class);
        Item right = mock(Item.class);
        when(itemService.getEntityTypeLabel(left)).thenReturn("Publication");
        when(itemService.getEntityTypeLabel(right)).thenReturn("Person");

        service.validate(context, configuration, left, right, true);
    }

    @Test
    public void testMetadataHelpersRejectUnrelatedEndpoint() throws SQLException {
        Item unrelatedItem = mock(Item.class);
        when(unrelatedItem.getID()).thenReturn(UUID.randomUUID());
        registerMetadataFields();

        assertThrows(IllegalArgumentException.class,
                     () -> service.getPrimaryMetadataField(context, relationship, unrelatedItem));
    }

    @Test
    public void testSamePrimaryMetadataFieldOnBothEndpoints() throws SQLException {
        configuration.setLeftPrimaryMetadataField("dc.title");
        configuration.setRightPrimaryMetadataField("dc.title");
        registerMetadataFields();

        MetadataValue leftMetadata = metadataValue("dc", "title", null, leftItem);
        MetadataValue rightMetadata = metadataValue("dc", "title", null, rightItem);

        mockOwnerEntityTypes();
        service.validateConfigurations();

        assertSame(configuration, service.findForMetadataValue(context, leftMetadata));
        assertSame(configuration, service.findForMetadataValue(context, rightMetadata));

        assertEquals("dc.title",
                     service.getPrimaryMetadataField(context, relationship, leftItem).orElse(null));
        assertEquals("dc.title",
                     service.getPrimaryMetadataField(context, relationship, rightItem).orElse(null));

        assertTrue(service.isPrimaryMetadata(context, relationship, leftMetadata));
        assertTrue(service.isPrimaryMetadata(context, relationship, rightMetadata));
    }

    @Test
    public void testValidAdditionalMetadataFieldsRemainAvailable() throws SQLException {
        RelationshipMetadataGenerator validGenerator = mock(RelationshipMetadataGenerator.class);
        RelationshipMetadataGenerator invalidGenerator = mock(RelationshipMetadataGenerator.class);
        when(validGenerator.getSourceField()).thenReturn("dc.title");

        configuration.setLeftMetadataGenerators(Map.of(
            "dc.description", validGenerator,
            "person.identifier.orcid", invalidGenerator
        ));

        when(metadataFieldService.findByString(eq(context), any(String.class), eq('.')))
            .thenAnswer(invocation -> {
                String field = invocation.getArgument(1);
                return field.startsWith("person.") ? null : mock(MetadataField.class);
            });

        RelationshipTypeConfiguration resolved = service.getRelationshipConfig(context, relationship);

        assertEquals(Set.of("dc.description"), resolved.getLeftAdditionalMetadataFields());
        assertSame(validGenerator, resolved.getLeftMetadataGenerators().get("dc.description"));
        assertFalse(resolved.getLeftMetadataGenerators().containsKey("person.identifier.orcid"));
    }

    /**
     * Verifies that resolving a metadata value rejects configurations with nonexistent primary fields.
     */
    @Test
    public void testFindForMetadataValueRejectsNonexistentPrimaryMetadataField() {
        when(itemService.getEntityTypeLabel(leftItem)).thenReturn("Publication");
        RelationshipConfigurationException exception =
            assertThrows(RelationshipConfigurationException.class,
                         () -> service.findForMetadataValue(
                             context, metadataValue("dc", "contributor", "author", leftItem)));

        assertTrue(exception.getMessage().contains("authority:dc.contributor.author"));
        assertTrue(exception.getMessage().contains("dc.contributor.author"));
    }

    @Test
    public void testOverlappingPrimaryMetadataMappingsRejectedAtStartup() {
        RelationshipTypeConfiguration another = configuration("another", "dc.contributor.author", null);
        another.setLeftEntityTypes(Set.of("Publication", "Book"));

        setConfigurations(configuration, another);

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> service.validateConfigurations());

        assertTrue(exception.getMessage().contains("dc.contributor.author"));
        assertTrue(exception.getMessage().contains(configuration.getId()));
        assertTrue(exception.getMessage().contains("another"));
        assertTrue(exception.getMessage().contains("Publication"));
    }

    @Test
    public void testDisjointPrimaryMetadataMappingsAllowedAtStartup() {
        RelationshipTypeConfiguration another = configuration("dataset-person", "dc.contributor.author", null);
        another.setLeftEntityTypes(Set.of("DataSet"));

        setConfigurations(configuration, another);

        service.validateConfigurations();
    }

    @Test
    public void testCrossSidePrimaryMetadataConflictRejectedAtStartup() {
        RelationshipTypeConfiguration another = configuration("another", null, "dc.contributor.author");
        another.setRightEntityTypes(Set.of("Publication", "Person"));

        setConfigurations(configuration, another);

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> service.validateConfigurations());

        assertTrue(exception.getMessage().contains("dc.contributor.author"));
        assertTrue(exception.getMessage().contains("Publication"));
        assertTrue(exception.getMessage().contains("left"));
        assertTrue(exception.getMessage().contains("right"));
    }

    @Test
    public void testSamePrimaryFieldOnDisjointEndpointsAllowedAtStartup() {
        configuration.setLeftPrimaryMetadataField("dc.title");
        configuration.setRightPrimaryMetadataField("dc.title");

        service.validateConfigurations();
    }

    @Test
    public void testSamePrimaryFieldOnOverlappingEndpointsRejectedAtStartup() {
        configuration.setLeftPrimaryMetadataField("dc.title");
        configuration.setRightPrimaryMetadataField("dc.title");
        configuration.setRightEntityTypes(Set.of("Publication", "Person"));

        assertThrows(IllegalArgumentException.class, () -> service.validateConfigurations());
    }

    @Test
    public void testFindForMetadataValueSelectsConfigurationByOwnerEntityType() throws SQLException {
        RelationshipTypeConfiguration datasetConfiguration =
            configuration("dataset-person", "dc.contributor.author", null);
        datasetConfiguration.setLeftEntityTypes(Set.of("DataSet"));

        setConfigurations(configuration, datasetConfiguration);
        registerMetadataFields();

        Item dataset = mock(Item.class);

        when(itemService.getEntityTypeLabel(leftItem)).thenReturn("Publication");
        when(itemService.getEntityTypeLabel(dataset)).thenReturn("DataSet");

        assertSame(configuration,
                   service.findForMetadataValue(context,
                                                metadataValue("dc", "contributor", "author", leftItem)));

        assertSame(datasetConfiguration,
                   service.findForMetadataValue(context,
                                                metadataValue("dc", "contributor", "author", dataset)));
    }

    @Test
    public void testAuthorityFallbackWithSharedKeyUsesEndpointTypes() throws SQLException {
        enableAuthority("dc.contributor.author", "Person");
        registerMetadataFields();
        Item dataset = mock(Item.class);
        when(itemService.getEntityTypeLabel(dataset)).thenReturn("DataSet");
        when(itemService.getEntityTypeLabel(rightItem)).thenReturn("Person");

        // A Publication-specific explicit bean may share its key with a generic authority fallback.
        // A DataSet must select the fallback, and the persisted Relationship must resolve to that fallback.
        RelationshipTypeConfiguration fallback = service.findForMetadataValue(context,
            metadataValue("dc", "contributor", "author", dataset));
        assertEquals("authority:dc.contributor.author", fallback.getId());
        assertTrue(fallback.getLeftEntityTypes().isEmpty());
        assertEquals(Set.of("Person"), fallback.getRightEntityTypes());

        service.validateForCreation(context, fallback, dataset, rightItem, true);
        Relationship datasetRelationship = mock(Relationship.class);
        when(datasetRelationship.getRelationshipConfigKey()).thenReturn("authority:dc.contributor.author");
        when(datasetRelationship.getLeftItem()).thenReturn(dataset);
        when(datasetRelationship.getRightItem()).thenReturn(rightItem);
        when(dataset.getID()).thenReturn(UUID.randomUUID());
        assertEquals(fallback.getLeftEntityTypes(),
                     service.getRelationshipConfig(context, datasetRelationship).getLeftEntityTypes());
        assertTrue(service.isPrimaryMetadata(context, datasetRelationship,
            metadataValue("dc", "contributor", "author", dataset)));
    }

    @Test
    public void testExplicitAuthorityConfigurationStillWinsForMatchingEndpointTypes() throws SQLException {
        enableAuthority("dc.contributor.author", "Person");
        registerMetadataFields();
        when(itemService.getEntityTypeLabel(leftItem)).thenReturn("Publication");
        when(itemService.getEntityTypeLabel(rightItem)).thenReturn("Person");

        assertSame(configuration, service.findForMetadataValue(context,
            metadataValue("dc", "contributor", "author", leftItem)));
        assertSame(configuration, service.getRelationshipConfig(context, relationship));

        RelationshipTypeConfiguration fallback = new RelationshipTypeConfiguration();
        fallback.setId("authority:dc.contributor.author");
        fallback.setLeftPrimaryMetadataField("dc.contributor.author");
        fallback.setRightEntityTypes(Set.of("Person"));
        assertThrows(IllegalArgumentException.class,
            () -> service.validateForCreation(context, fallback, leftItem, rightItem, true));
    }

    @Test
    public void testValidateForCreationSupportsAuthorityWithoutSpringConfiguration() throws SQLException {
        setConfigurations();
        enableAuthority("dc.contributor.author", "Person");
        registerMetadataFields();
        when(itemService.getEntityTypeLabel(rightItem)).thenReturn("Person");

        RelationshipTypeConfiguration authorityOnly = service.findForMetadataValue(context,
            metadataValue("dc", "contributor", "author", leftItem));
        assertEquals("authority:dc.contributor.author", authorityOnly.getId());
        service.validateForCreation(context, authorityOnly, leftItem, rightItem, true);
        assertEquals(authorityOnly.getLeftPrimaryMetadataField(),
                     service.getRelationshipConfig(context, relationship).getLeftPrimaryMetadataField());
    }

    @Test
    public void testValidateForCreationRejectsUnknownKey() {
        RelationshipTypeConfiguration unknown = configuration("unknown-key", "dc.contributor.author", null);
        assertThrows(IllegalArgumentException.class,
            () -> service.validateForCreation(context, unknown, leftItem, rightItem, false));
    }

    @Test
    public void testValidateForCreationRejectsMismatchedSemantics() throws SQLException {
        registerMetadataFields();
        RelationshipTypeConfiguration incompatible =
            configuration("authority:dc.contributor.author", "dc.contributor.author", null);
        incompatible.setLeftEntityTypes(Set.of("DataSet"));
        assertThrows(IllegalArgumentException.class,
            () -> service.validateForCreation(context, incompatible, leftItem, rightItem, false));
    }

    @Test
    public void testValidateForCreationChecksRegisteredPrimaryFields() {
        // The supplied configuration is known, but its primary metadata field is not in the registry.
        assertThrows(RelationshipConfigurationException.class,
            () -> service.validateForCreation(context, configuration, leftItem, rightItem, false));
    }

    @Test
    public void testGetRelationshipConfigResolvesAuthorityDerivedConfiguration() throws SQLException {
        setConfigurations();
        enableAuthority("dc.contributor.author", "Person");
        registerMetadataFields();

        RelationshipTypeConfiguration result = service.getRelationshipConfig(context, relationship);

        assertEquals("authority:dc.contributor.author", result.getId());
        assertEquals("dc.contributor.author", result.getLeftPrimaryMetadataField().orElse(null));
        assertEquals(Set.of("Person"), result.getRightEntityTypes());
    }

    @Test
    public void testGetRelationshipConfigRejectsMissingConfigurationKey() {
        relationship.setRelationshipConfigKey(null);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.getRelationshipConfig(context, relationship));

        assertTrue(exception.getMessage().contains("Missing relationship configuration key"));
    }

    @Test
    public void testGetRelationshipConfigRejectsUnknownConfigurationKey() {
        relationship.setRelationshipConfigKey("unknown");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.getRelationshipConfig(context, relationship));

        assertTrue(exception.getMessage().contains("Unknown relationship configuration"));
    }
}
