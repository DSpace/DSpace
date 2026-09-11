/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.migration;

import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.ReloadableEntity;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for {@link RelationshipToAuthorityMigrationScript}.
 *
 * Tests that the script correctly converts entity relationships into
 * authority-based metadata values on the publication item.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class RelationshipToAuthorityMigrationScriptIT extends AbstractIntegrationTestWithDatabase {

    private static ConfigurationService configurationService;


    private static PluginService pluginService = CoreServiceFactory
        .getInstance()
        .getPluginService();

    private static MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory
        .getInstance()
        .getMetadataAuthorityService();

    private static ChoiceAuthorityService choiceAuthorityService = ContentAuthorityServiceFactory
        .getInstance()
        .getChoiceAuthorityService();

    private static String[] savedAuthorityPlugins;
    private static String savedChoicesPlugin;
    private static String savedChoicesPresentation;
    private static String savedAuthorityControlled;
    private static boolean savedVirtualMetadata;

    private ItemService itemService;
    private RelationshipService relationshipService;

    private Collection personCollection;
    private Collection publicationCollection;
    private Collection projectCollection;

    /**
     * Save current configuration values before any tests run.
     */
    @BeforeClass
    public static void saveConfiguration() {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        savedAuthorityPlugins = configurationService.getArrayProperty(
            "plugin.named.org.dspace.content.authority.ChoiceAuthority");
        savedChoicesPlugin = configurationService.getProperty(
            "choices.plugin.dc.contributor.author");
        savedChoicesPresentation = configurationService.getProperty(
            "choices.presentation.dc.contributor.author");
        savedAuthorityControlled = configurationService.getProperty(
            "authority.controlled.dc.contributor.author");
        savedVirtualMetadata = configurationService.getBooleanProperty(
            "item.enable-virtual-metadata", false);
    }

    /**
     * Restore configuration values after all tests.
     */
    @AfterClass
    public static void restoreConfiguration() throws Exception {
        configurationService.setProperty(
            "plugin.named.org.dspace.content.authority.ChoiceAuthority", savedAuthorityPlugins);
        configurationService.setProperty(
            "choices.plugin.dc.contributor.author", savedChoicesPlugin);
        configurationService.setProperty(
            "choices.presentation.dc.contributor.author", savedChoicesPresentation);
        configurationService.setProperty(
            "authority.controlled.dc.contributor.author", savedAuthorityControlled);
        configurationService.setProperty(
            "item.enable-virtual-metadata", savedVirtualMetadata);
        clearAllAuthorityCaches();

    }

    /**
     * Restore default authority configuration after each test so failures
     * in one test do not leave virtual-metadata/authority settings for the next.
     */
    @After
    public void tearDown() throws Exception {
        configurationService.setProperty("item.enable-virtual-metadata", savedVirtualMetadata);
        restoreAuthorAuthorityConfig();
        clearAllAuthorityCaches();
    }

    @Before
    public void setup() {
        choiceAuthorityService.getChoiceAuthoritiesNames(); // initialize the ChoiceAuthorityService
        itemService = ContentServiceFactory.getInstance().getItemService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
    }

    /**
     * Test that the migration script converts author relationships to
     * dc.contributor.author metadata with proper authority and place.
     */
    @Test
    public void testMigrateAuthorRelationshipToAuthority() throws Exception {
        disableAuthorAuthorityConfig();
        clearAllAuthorityCaches();

        TestScenario scenario = createAuthorScenario();

        Item author1 = ItemBuilder.createItem(context, scenario.personCollection)
            .withTitle("Walter White")
            .build();

        Item author2 = ItemBuilder.createItem(context, scenario.personCollection)
            .withTitle("Jesse Pinkman")
            .build();

        Item publication = ItemBuilder.createItem(context, scenario.publicationCollection)
            .withTitle("Test Publication")
            .withAuthor("Saul Goodman")
            .withAuthor("Jesse Pinkman")
            .build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, author1, scenario.relationshipType, 1, -1)
            .withLeftwardValue("Walter White")
            .build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, author2, scenario.relationshipType, 2, -1)
            .withLeftwardValue("Jesse Pinkman")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        publication = reload(publication);

        // Verify no authority metadata before migration
        List<MetadataValue> authorsBefore = itemService.getMetadataByMetadataString(
            publication, "dc.contributor.author");
        for (MetadataValue mv : authorsBefore) {
            assertThat("Authority should be null before migration", mv.getAuthority(), nullValue());
        }

        enableAuthorAuthorityConfig();
        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = runMigration(
            "relationship-to-authority-migrate", "-t",
            String.valueOf(scenario.relationshipType.getID()));

        // Verify no errors
        assertThat("Migration should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        // Verify success message in info logs
        boolean foundComplete = runnableHandler.getInfoMessages().stream()
            .anyMatch(msg -> msg.contains("=== Migration Complete ==="));
        assertThat("Should contain completion message", foundComplete, is(true));

        // Reload publication and verify metadata
        publication = reload(publication);
        List<MetadataValue> authorsAfter = itemService.getMetadataByMetadataString(
            publication, "dc.contributor.author");
        assertThat("Should have 3 author metadata values", authorsAfter, hasSize(3));

        // Sort by place to ensure order
        authorsAfter.sort(Comparator.comparingInt(MetadataValue::getPlace));

        // Verify first author
        MetadataValue firstAuthor = authorsAfter.get(0);
        assertEquals("Saul Goodman", firstAuthor.getValue());
        assertEquals(null, firstAuthor.getAuthority());
        assertEquals(0, firstAuthor.getPlace());

        // Verify second author
        MetadataValue secondAuthor = authorsAfter.get(1);
        assertEquals("Walter White", secondAuthor.getValue());
        assertEquals(author1.getID().toString(), secondAuthor.getAuthority());
        assertEquals(CF_ACCEPTED, secondAuthor.getConfidence());
        assertEquals(1, secondAuthor.getPlace());

        // Verify third author

        MetadataValue thirdAuthor = authorsAfter.get(2);
        assertEquals("Jesse Pinkman", thirdAuthor.getValue());
        assertEquals(author2.getID().toString(), thirdAuthor.getAuthority());
        assertEquals(CF_ACCEPTED, thirdAuthor.getConfidence());
        assertEquals(2, thirdAuthor.getPlace());

        // Verify relationships still exist (no -d flag)
        List<Relationship> remaining = relationshipService.findByRelationshipType(
            context, scenario.relationshipType);
        assertThat("Relationships should still exist without -d flag", remaining, hasSize(2));
    }

    /**
     * Test that the migration script deletes relationships when -d flag is used.
     */
    @Test
    public void testMigrateWithDelete() throws Exception {
        disableAuthorAuthorityConfig();
        clearAllAuthorityCaches();

        TestScenario scenario = createAuthorScenario();

        Item author1 = ItemBuilder.createItem(context, scenario.personCollection)
            .withTitle("Heisenberg")
            .build();

        Item publication = ItemBuilder.createItem(context, scenario.publicationCollection)
            .withTitle("Blue Sky Paper")
            .build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, author1, scenario.relationshipType, 0, -1)
            .withLeftwardValue("Heisenberg")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        enableAuthorAuthorityConfig();
        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = runMigration(
            "relationship-to-authority-migrate", "-t",
            String.valueOf(scenario.relationshipType.getID()), "-d");

        // Verify no errors
        assertThat("Migration should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        // Reload and verify metadata was added
        publication = reload(publication);
        List<MetadataValue> authors = itemService.getMetadataByMetadataString(
            publication, "dc.contributor.author");
        assertThat("Should have 1 author metadata value", authors, hasSize(1));
        assertEquals("Heisenberg", authors.get(0).getValue());
        assertEquals(author1.getID().toString(), authors.get(0).getAuthority());
        assertEquals(CF_ACCEPTED, authors.get(0).getConfidence());

        // Verify relationships are deleted
        List<Relationship> remaining = relationshipService.findByRelationshipType(
            context, scenario.relationshipType);
        assertThat("Relationships should be deleted with -d flag", remaining, empty());
    }

    /**
     * Test that dry-run mode does not commit any changes.
     */
    @Test
    public void testMigrateDryRun() throws Exception {
        // Disable authority plugins and enable virtual metadata for relationship creation
        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty(
            "plugin.named.org.dspace.content.authority.ChoiceAuthority", null);
        configurationService.setProperty("choices.plugin.dc.contributor.author", null);
        configurationService.setProperty("choices.presentation.dc.contributor.author", null);
        configurationService.setProperty("authority.controlled.dc.contributor.author", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        // Create entity types
        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType personEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        // Create community and collections
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        personCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Persons")
            .withEntityType("Person")
            .build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications")
            .withEntityType("Publication")
            .build();

        // Create relationship type
        RelationshipType isAuthorOfPublication = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, personEntityType,
                "isAuthorOfPublication", "isPublicationOfAuthor", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        // Create person item
        Item author1 = ItemBuilder.createItem(context, personCollection)
            .withTitle("Gustavo Fring")
            .build();

        // Create publication item
        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Los Pollos Hermanos")
            .build();

        // Create relationship
        RelationshipBuilder.createRelationshipBuilder(
                context, publication, author1, isAuthorOfPublication, 0, -1)
            .withLeftwardValue("Gustavo Fring")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        // Disable virtual metadata before running the migration
        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = PersonAuthority" });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "PersonAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        clearAllAuthorityCaches();

        // Run the migration with -n (dry-run) flag
        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t",
            String.valueOf(isAuthorOfPublication.getID()), "-n"
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        // Verify no errors
        assertThat("Dry-run should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        // Verify dry-run message
        boolean foundDryRun = runnableHandler.getInfoMessages().stream()
            .anyMatch(msg -> msg.contains("DRY RUN: no changes committed"));
        assertThat("Should contain dry-run message", foundDryRun, is(true));

        // Reload and verify NO metadata was added (changes were aborted)
        publication = reload(publication);
        List<MetadataValue> authors = itemService.getMetadataByMetadataString(
            publication, "dc.contributor.author");
        for (MetadataValue mv : authors) {
            assertThat("No authority should be set in dry-run", mv.getAuthority(), nullValue());
        }

        // Verify relationships still exist
        List<Relationship> remaining = relationshipService.findByRelationshipType(
            context, isAuthorOfPublication);
        assertThat("Relationships should still exist in dry-run", remaining, hasSize(1));
    }

    /**
     * Test that migration falls back to dc.title when leftwardValue is not set.
     */
    @Test
    public void testMigrateWithoutLeftwardValue() throws Exception {
        // Disable authority plugins and enable virtual metadata for relationship creation
        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty(
            "plugin.named.org.dspace.content.authority.ChoiceAuthority", null);
        configurationService.setProperty("choices.plugin.dc.contributor.author", null);
        configurationService.setProperty("choices.presentation.dc.contributor.author", null);
        configurationService.setProperty("authority.controlled.dc.contributor.author", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        // Create entity types
        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType personEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        // Create community and collections
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        personCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Persons")
            .withEntityType("Person")
            .build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications")
            .withEntityType("Publication")
            .build();

        // Create relationship type
        RelationshipType isAuthorOfPublication = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, personEntityType,
                "isAuthorOfPublication", "isPublicationOfAuthor", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        // Create person item with a dc.title but no leftwardValue will be set on relationship
        Item author1 = ItemBuilder.createItem(context, personCollection)
            .withTitle("Mike Ehrmantraut")
            .build();

        // Create publication item
        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Half Measures Paper")
            .build();

        // Create relationship WITHOUT leftwardValue
        RelationshipBuilder.createRelationshipBuilder(
                context, publication, author1, isAuthorOfPublication, 0, -1)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        // Disable virtual metadata before running the migration
        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = PersonAuthority" });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "PersonAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        clearAllAuthorityCaches();

        // Run the migration
        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t",
            String.valueOf(isAuthorOfPublication.getID())
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        // Verify no errors
        assertThat("Migration should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        // Reload and verify metadata uses dc.title as fallback value
        publication = reload(publication);
        List<MetadataValue> authors = itemService.getMetadataByMetadataString(
            publication, "dc.contributor.author");
        assertThat("Should have 1 author metadata value", authors, hasSize(1));

        MetadataValue authorMv = authors.get(0);
        assertEquals("Mike Ehrmantraut", authorMv.getValue());
        assertEquals(author1.getID().toString(), authorMv.getAuthority());
        assertEquals(CF_ACCEPTED, authorMv.getConfidence());
        assertEquals(0, authorMv.getPlace());
    }



    // ============= Project Migration Tests =============
    // The isProjectOfPublication/isPublicationOfProject type definition maps to
    // dc.relation.project (leftward, null qualifier). The migration script matches the
    // definition by the relationship type's leftward/rightward labels, so no
    // specific database ID is required.

    /**
     * Test that the migration script converts project relationships to
     * dc.relation.project metadata with proper authority and place.
     */
    @Test
    public void testMigrateProjectRelationshipToAuthority() throws Exception {
        // Save current dc.relation.project authority configuration
        String savedRelationPlugin = configurationService.getProperty("choices.plugin.dc.relation.project");
        String savedRelationPresentation = configurationService.getProperty("choices.presentation.dc.relation.project");
        String savedRelationControlled = configurationService.getProperty("authority.controlled.dc.relation.project");

        // Disable dc.relation.project authority for item creation
        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty("choices.plugin.dc.relation.project", null);
        configurationService.setProperty("choices.presentation.dc.relation.project", null);
        configurationService.setProperty("authority.controlled.dc.relation.project", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType projectEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        projectCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Projects").withEntityType("Project").build();

        RelationshipType isProjectOfPublication = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, projectEntityType,
                "isProjectOfPublication", "isPublicationOfProject", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item project = ItemBuilder.createItem(context, projectCollection)
            .withTitle("Project Alpha").build();

        Item project2 = ItemBuilder.createItem(context, projectCollection)
            .withTitle("Project Beta").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Test Publication").build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, project, isProjectOfPublication, 0, -1)
            .withLeftwardValue("Project Alpha").build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, project2, isProjectOfPublication, 1, -1)
            .withLeftwardValue("Project Beta").build();

        context.commit();
        context.restoreAuthSystemState();

        publication = reload(publication);

        // Verify no dc.relation.project metadata before migration
        List<MetadataValue> relationBefore = itemService.getMetadataByMetadataString(
            publication, "dc.relation.project");
        assertThat("Should have no dc.relation.project metadata before migration",
            relationBefore, empty());

        // Set dc.relation.project authority for migration
        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.relation.project", "ProjectAuthority");
        configurationService.setProperty("choices.presentation.dc.relation.project", "suggest");
        configurationService.setProperty("authority.controlled.dc.relation.project", "true");

        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t",
            String.valueOf(isProjectOfPublication.getID())
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        assertThat("Migration should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        publication = reload(publication);
        List<MetadataValue> relationAfter = itemService.getMetadataByMetadataString(
            publication, "dc.relation.project");
        assertThat("Should have 2 dc.relation.project metadata values", relationAfter, hasSize(2));

        relationAfter.sort(Comparator.comparingInt(MetadataValue::getPlace));

        MetadataValue firstRelation = relationAfter.get(0);
        assertEquals("Project Alpha", firstRelation.getValue());
        assertEquals(project.getID().toString(), firstRelation.getAuthority());
        assertEquals(CF_ACCEPTED, firstRelation.getConfidence());
        assertEquals(0, firstRelation.getPlace());

        MetadataValue secondRelation = relationAfter.get(1);
        assertEquals("Project Beta", secondRelation.getValue());
        assertEquals(project2.getID().toString(), secondRelation.getAuthority());
        assertEquals(CF_ACCEPTED, secondRelation.getConfidence());
        assertEquals(1, secondRelation.getPlace());

        List<Relationship> remaining = relationshipService.findByRelationshipType(
            context, isProjectOfPublication);
        assertThat("Relationships should still exist without -d flag", remaining, hasSize(2));

        // Restore saved dc.relation.project authority configuration
        configurationService.setProperty("choices.plugin.dc.relation.project", savedRelationPlugin);
        configurationService.setProperty("choices.presentation.dc.relation.project", savedRelationPresentation);
        configurationService.setProperty("authority.controlled.dc.relation.project", savedRelationControlled);
    }

    /**
     * Test that the migration script deletes project relationships when -d flag is used.
     */
    @Test
    public void testMigrateProjectWithDelete() throws Exception {
        // Save current dc.relation.project authority configuration
        String savedRelationPlugin = configurationService.getProperty("choices.plugin.dc.relation.project");
        String savedRelationPresentation = configurationService.getProperty("choices.presentation.dc.relation.project");
        String savedRelationControlled = configurationService.getProperty("authority.controlled.dc.relation.project");

        // Disable dc.relation.project authority for item creation
        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty("choices.plugin.dc.relation.project", null);
        configurationService.setProperty("choices.presentation.dc.relation.project", null);
        configurationService.setProperty("authority.controlled.dc.relation.project", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType projectEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        projectCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Projects").withEntityType("Project").build();

        RelationshipType isProjectOfPublication = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, projectEntityType,
                "isProjectOfPublication", "isPublicationOfProject", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item project = ItemBuilder.createItem(context, projectCollection)
            .withTitle("Secret Project").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Confidential Report").build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, project, isProjectOfPublication, 0, -1)
            .withLeftwardValue("Secret Project").build();

        context.commit();
        context.restoreAuthSystemState();

        // Set dc.relation.project authority for migration
        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.relation.project", "ProjectAuthority");
        configurationService.setProperty("choices.presentation.dc.relation.project", "suggest");
        configurationService.setProperty("authority.controlled.dc.relation.project", "true");

        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t",
            String.valueOf(isProjectOfPublication.getID()), "-d"
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        assertThat("Migration should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        publication = reload(publication);
        List<MetadataValue> relations = itemService.getMetadataByMetadataString(
            publication, "dc.relation.project");
        assertThat("Should have 1 dc.relation.project metadata value", relations, hasSize(1));
        assertEquals("Secret Project", relations.get(0).getValue());
        assertEquals(project.getID().toString(), relations.get(0).getAuthority());
        assertEquals(CF_ACCEPTED, relations.get(0).getConfidence());

        List<Relationship> remaining = relationshipService.findByRelationshipType(
            context, isProjectOfPublication);
        assertThat("Relationships should be deleted with -d flag", remaining, empty());

        // Restore saved dc.relation.project authority configuration
        configurationService.setProperty("choices.plugin.dc.relation.project", savedRelationPlugin);
        configurationService.setProperty("choices.presentation.dc.relation.project", savedRelationPresentation);
        configurationService.setProperty("authority.controlled.dc.relation.project", savedRelationControlled);
    }

    /**
     * Test that dry-run mode does not commit any changes for project migration.
     */
    @Test
    public void testMigrateProjectDryRun() throws Exception {
        // Save current dc.relation.project authority configuration
        String savedRelationPlugin = configurationService.getProperty("choices.plugin.dc.relation.project");
        String savedRelationPresentation = configurationService.getProperty("choices.presentation.dc.relation.project");
        String savedRelationControlled = configurationService.getProperty("authority.controlled.dc.relation.project");

        // Disable dc.relation.project authority for item creation
        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty("choices.plugin.dc.relation.project", null);
        configurationService.setProperty("choices.presentation.dc.relation.project", null);
        configurationService.setProperty("authority.controlled.dc.relation.project", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType projectEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        projectCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Projects").withEntityType("Project").build();

        RelationshipType isProjectOfPublication = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, projectEntityType,
                "isProjectOfPublication", "isPublicationOfProject", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item project = ItemBuilder.createItem(context, projectCollection)
            .withTitle("Dry Run Project").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Dry Run Publication").build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, project, isProjectOfPublication, 0, -1)
            .withLeftwardValue("Dry Run Project").build();

        context.commit();
        context.restoreAuthSystemState();

        // Set dc.relation.project authority for migration
        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.relation.project", "ProjectAuthority");
        configurationService.setProperty("choices.presentation.dc.relation.project", "suggest");
        configurationService.setProperty("authority.controlled.dc.relation.project", "true");

        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t",
            String.valueOf(isProjectOfPublication.getID()), "-n"
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        assertThat("Dry-run should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        boolean foundDryRun = runnableHandler.getInfoMessages().stream()
            .anyMatch(msg -> msg.contains("DRY RUN: no changes committed"));
        assertThat("Should contain dry-run message", foundDryRun, is(true));

        publication = reload(publication);
        List<MetadataValue> relations = itemService.getMetadataByMetadataString(
            publication, "dc.relation.project");
        assertThat("Should have no dc.relation.project metadata in dry-run",
            relations, empty());

        List<Relationship> remaining = relationshipService.findByRelationshipType(
            context, isProjectOfPublication);
        assertThat("Relationships should still exist in dry-run", remaining, hasSize(1));

        // Restore saved dc.relation.project authority configuration
        configurationService.setProperty("choices.plugin.dc.relation.project", savedRelationPlugin);
        configurationService.setProperty("choices.presentation.dc.relation.project", savedRelationPresentation);
        configurationService.setProperty("authority.controlled.dc.relation.project", savedRelationControlled);
    }

    /**
     * Test that project migration falls back to dc.title when leftwardValue is not set.
     */
    @Test
    public void testMigrateProjectWithoutLeftwardValue() throws Exception {
        // Save current dc.relation.project authority configuration
        String savedRelationPlugin = configurationService.getProperty("choices.plugin.dc.relation.project");
        String savedRelationPresentation = configurationService.getProperty("choices.presentation.dc.relation.project");
        String savedRelationControlled = configurationService.getProperty("authority.controlled.dc.relation.project");

        // Disable dc.relation.project authority for item creation
        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty("choices.plugin.dc.relation.project", null);
        configurationService.setProperty("choices.presentation.dc.relation.project", null);
        configurationService.setProperty("authority.controlled.dc.relation.project", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType projectEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        projectCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Projects").withEntityType("Project").build();

        RelationshipType isProjectOfPublication = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, projectEntityType,
                "isProjectOfPublication", "isPublicationOfProject", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item project = ItemBuilder.createItem(context, projectCollection)
            .withTitle("Fallback Project").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Fallback Publication").build();

        // Relationship WITHOUT leftwardValue — should fallback to project's dc.title
        RelationshipBuilder.createRelationshipBuilder(
                context, publication, project, isProjectOfPublication, 0, -1)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        // Set dc.relation.project authority for migration
        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.relation.project", "ProjectAuthority");
        configurationService.setProperty("choices.presentation.dc.relation.project", "suggest");
        configurationService.setProperty("authority.controlled.dc.relation.project", "true");

        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t",
            String.valueOf(isProjectOfPublication.getID())
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        assertThat("Migration should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        publication = reload(publication);
        List<MetadataValue> relations = itemService.getMetadataByMetadataString(
            publication, "dc.relation.project");
        assertThat("Should have 1 dc.relation.project metadata value", relations, hasSize(1));

        MetadataValue relationMv = relations.get(0);
        assertEquals("Fallback Project", relationMv.getValue());
        assertEquals(project.getID().toString(), relationMv.getAuthority());
        assertEquals(CF_ACCEPTED, relationMv.getConfidence());
        assertEquals(0, relationMv.getPlace());

        // Restore saved dc.relation.project authority configuration
        configurationService.setProperty("choices.plugin.dc.relation.project", savedRelationPlugin);
        configurationService.setProperty("choices.presentation.dc.relation.project", savedRelationPresentation);
        configurationService.setProperty("authority.controlled.dc.relation.project", savedRelationControlled);
    }

    // ============= Dual-Direction Migration Tests =============
    // The isAuthorOfPublicationDual/isPublicationOfAuthorDual type definition is
    // configured with both leftward (dc.contributor.author on leftItem) and
    // rightward (dc.description on rightItem) directional definitions. Dedicated
    // labels are used so this does not clash with the leftward-only author type.

    /**
     * Test that a dual-direction type definition writes metadata to both
     * the left and right items in a single migration run.
     */
    @Test
    public void testMigrateDualDirection() throws Exception {
        // Save authority configs
        String savedAuthorPlugin = configurationService.getProperty("choices.plugin.dc.contributor.author");
        String savedAuthorPresentation = configurationService.getProperty("choices.presentation.dc.contributor.author");
        String savedAuthorControlled = configurationService.getProperty("authority.controlled.dc.contributor.author");
        String savedDescPlugin = configurationService.getProperty("choices.plugin.dc.description");
        String savedDescPresentation = configurationService.getProperty("choices.presentation.dc.description");
        String savedDescControlled = configurationService.getProperty("authority.controlled.dc.description");

        // Disable authority plugins for item creation
        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty("choices.plugin.dc.contributor.author", null);
        configurationService.setProperty("choices.presentation.dc.contributor.author", null);
        configurationService.setProperty("authority.controlled.dc.contributor.author", null);
        configurationService.setProperty("choices.plugin.dc.description", null);
        configurationService.setProperty("choices.presentation.dc.description", null);
        configurationService.setProperty("authority.controlled.dc.description", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType personEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        personCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Persons").withEntityType("Person").build();

        RelationshipType dualDirectionType = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, personEntityType,
                "isAuthorOfPublicationDual", "isPublicationOfAuthorDual", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item person = ItemBuilder.createItem(context, personCollection)
            .withTitle("Jane Doe").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Dual Direction Paper").build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, person, dualDirectionType, 0, 0)
            .withLeftwardValue("Jane Doe")
            .withRightwardValue("Dual Direction Paper")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        publication = reload(publication);
        person = reload(person);

        // Verify no related metadata before migration
        List<MetadataValue> authorsBefore = itemService.getMetadataByMetadataString(
            publication, "dc.contributor.author");
        assertThat("Should have no author metadata before migration", authorsBefore, empty());
        List<MetadataValue> descBefore = itemService.getMetadataByMetadataString(
            person, "dc.description");
        assertThat("Should have no description metadata before migration", descBefore, empty());

        // Restore authority configs for migration
        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.contributor.author", "PersonAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("choices.plugin.dc.description", "PublicationAuthority");
        configurationService.setProperty("choices.presentation.dc.description", "suggest");
        configurationService.setProperty("authority.controlled.dc.description", "true");

        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t",
            String.valueOf(dualDirectionType.getID())
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        assertThat("Migration should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        // Verify leftward: dc.contributor.author on publication
        publication = reload(publication);
        List<MetadataValue> authors = itemService.getMetadataByMetadataString(
            publication, "dc.contributor.author");
        assertThat("Should have 1 author metadata value on publication", authors, hasSize(1));
        assertEquals("Jane Doe", authors.get(0).getValue());
        assertEquals(person.getID().toString(), authors.get(0).getAuthority());
        assertEquals(CF_ACCEPTED, authors.get(0).getConfidence());
        assertEquals(0, authors.get(0).getPlace());

        // Verify rightward: dc.description on person
        person = reload(person);
        List<MetadataValue> descriptions = itemService.getMetadataByMetadataString(
            person, "dc.description");
        assertThat("Should have 1 description metadata value on person", descriptions, hasSize(1));
        assertEquals("Dual Direction Paper", descriptions.get(0).getValue());
        assertEquals(publication.getID().toString(), descriptions.get(0).getAuthority());
        assertEquals(CF_ACCEPTED, descriptions.get(0).getConfidence());
        assertEquals(0, descriptions.get(0).getPlace());

        // Restore authority configs
        configurationService.setProperty("choices.plugin.dc.contributor.author", savedAuthorPlugin);
        configurationService.setProperty("choices.presentation.dc.contributor.author", savedAuthorPresentation);
        configurationService.setProperty("authority.controlled.dc.contributor.author", savedAuthorControlled);
        configurationService.setProperty("choices.plugin.dc.description", savedDescPlugin);
        configurationService.setProperty("choices.presentation.dc.description", savedDescPresentation);
        configurationService.setProperty("authority.controlled.dc.description", savedDescControlled);
    }

    /**
     * Test that rerunning the migration does not create duplicate metadata
     * values (place-based upsert idempotency).
     */
    @Test
    public void testMigrateIdempotentUpsert() throws Exception {
        String savedAuthorPlugin = configurationService.getProperty("choices.plugin.dc.contributor.author");
        String savedAuthorPresentation = configurationService.getProperty("choices.presentation.dc.contributor.author");
        String savedAuthorControlled = configurationService.getProperty("authority.controlled.dc.contributor.author");

        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty("choices.plugin.dc.contributor.author", null);
        configurationService.setProperty("choices.presentation.dc.contributor.author", null);
        configurationService.setProperty("authority.controlled.dc.contributor.author", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType personEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        personCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Persons").withEntityType("Person").build();

        // Uses the isAuthorOfPublication/isPublicationOfAuthor type (author leftward)
        RelationshipType isAuthorOfPublication = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, personEntityType,
                "isAuthorOfPublication", "isPublicationOfAuthor", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item author = ItemBuilder.createItem(context, personCollection)
            .withTitle("Repeat Author").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Repeat Paper").build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, author, isAuthorOfPublication, 0, -1)
            .withLeftwardValue("Repeat Author").build();

        context.commit();
        context.restoreAuthSystemState();

        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.contributor.author", "PersonAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");

        clearAllAuthorityCaches();

        // First migration run
        TestDSpaceRunnableHandler handler1 = new TestDSpaceRunnableHandler();
        String[] args1 = new String[] {
            "relationship-to-authority-migrate", "-t",
            String.valueOf(isAuthorOfPublication.getID())
        };
        ScriptLauncher.handleScript(args1, ScriptLauncher.getConfig(kernelImpl), handler1, kernelImpl);
        assertThat("First migration should complete without errors",
            handler1.getErrorMessages(), empty());

        publication = reload(publication);
        List<MetadataValue> afterFirst = itemService.getMetadataByMetadataString(
            publication, "dc.contributor.author");
        assertThat("Should have 1 author after first migration", afterFirst, hasSize(1));

        // Second migration run (idempotent)
        TestDSpaceRunnableHandler handler2 = new TestDSpaceRunnableHandler();
        String[] args2 = new String[] {
            "relationship-to-authority-migrate", "-t",
            String.valueOf(isAuthorOfPublication.getID())
        };
        ScriptLauncher.handleScript(args2, ScriptLauncher.getConfig(kernelImpl), handler2, kernelImpl);
        assertThat("Second migration should complete without errors",
            handler2.getErrorMessages(), empty());

        publication = reload(publication);
        List<MetadataValue> afterSecond = itemService.getMetadataByMetadataString(
            publication, "dc.contributor.author");
        assertThat("Should still have exactly 1 author after second migration (no duplicates)",
            afterSecond, hasSize(1));
        assertEquals("Repeat Author", afterSecond.get(0).getValue());
        assertEquals(author.getID().toString(), afterSecond.get(0).getAuthority());

        configurationService.setProperty("choices.plugin.dc.contributor.author", savedAuthorPlugin);
        configurationService.setProperty("choices.presentation.dc.contributor.author", savedAuthorPresentation);
        configurationService.setProperty("authority.controlled.dc.contributor.author", savedAuthorControlled);
    }

    // ============= Rightward-Only Migration Tests =============
    // The isRelatedToProject/isProjectRelatedTo type definition is configured
    // with a single rightward directional definition (dc.relation on rightItem).

    /**
     * Test a rightward-only migration where metadata is written to the
     * right item using rightwardValue from the relationship.
     */
    @Test
    public void testMigrateRightwardOnly() throws Exception {
        String savedRelationPlugin = configurationService.getProperty("choices.plugin.dc.relation");
        String savedRelationPresentation = configurationService.getProperty("choices.presentation.dc.relation");
        String savedRelationControlled = configurationService.getProperty("authority.controlled.dc.relation");

        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty("choices.plugin.dc.relation", null);
        configurationService.setProperty("choices.presentation.dc.relation", null);
        configurationService.setProperty("authority.controlled.dc.relation", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType projectEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        projectCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Projects").withEntityType("Project").build();

        RelationshipType type = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, projectEntityType,
                "isRelatedToProject", "isProjectRelatedTo", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Source Publication").build();

        Item project = ItemBuilder.createItem(context, projectCollection)
            .withTitle("Target Project").build();

        // Relationship: rightwardValue set, leftwardValue empty
        RelationshipBuilder.createRelationshipBuilder(
                context, publication, project, type, -1, 0)
            .withRightwardValue("Source Publication")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        publication = reload(publication);
        project = reload(project);

        // Verify no dc.relation on either item before migration
        List<MetadataValue> leftRelationsBefore = itemService.getMetadataByMetadataString(
            publication, "dc.relation");
        assertThat("Publication should have no dc.relation before migration",
            leftRelationsBefore, empty());
        List<MetadataValue> rightRelationsBefore = itemService.getMetadataByMetadataString(
            project, "dc.relation");
        assertThat("Project should have no dc.relation before migration",
            rightRelationsBefore, empty());

        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.relation", "PublicationAuthority");
        configurationService.setProperty("choices.presentation.dc.relation", "suggest");
        configurationService.setProperty("authority.controlled.dc.relation", "true");

        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t",
            String.valueOf(type.getID())
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        assertThat("Migration should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        // Verify no metadata on leftItem (publication)
        publication = reload(publication);
        List<MetadataValue> leftRelations = itemService.getMetadataByMetadataString(
            publication, "dc.relation");
        assertThat("Publication should still have no dc.relation (rightward migrates to rightItem)",
            leftRelations, empty());

        // Verify metadata on rightItem (project)
        project = reload(project);
        List<MetadataValue> rightRelations = itemService.getMetadataByMetadataString(
            project, "dc.relation");
        assertThat("Project should have 1 dc.relation metadata value", rightRelations, hasSize(1));
        assertEquals("Source Publication", rightRelations.get(0).getValue());
        assertEquals(publication.getID().toString(), rightRelations.get(0).getAuthority());
        assertEquals(CF_ACCEPTED, rightRelations.get(0).getConfidence());
        assertEquals(0, rightRelations.get(0).getPlace());

        configurationService.setProperty("choices.plugin.dc.relation", savedRelationPlugin);
        configurationService.setProperty("choices.presentation.dc.relation", savedRelationPresentation);
        configurationService.setProperty("authority.controlled.dc.relation", savedRelationControlled);
    }

    /**
     * Test rightward-only migration with dc.title fallback when
     * rightwardValue is not set.
     */
    @Test
    public void testMigrateRightwardOnlyWithoutValue() throws Exception {
        String savedRelationPlugin = configurationService.getProperty("choices.plugin.dc.relation");
        String savedRelationPresentation = configurationService.getProperty("choices.presentation.dc.relation");
        String savedRelationControlled = configurationService.getProperty("authority.controlled.dc.relation");

        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty("choices.plugin.dc.relation", null);
        configurationService.setProperty("choices.presentation.dc.relation", null);
        configurationService.setProperty("authority.controlled.dc.relation", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType projectEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        projectCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Projects").withEntityType("Project").build();

        RelationshipType type = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, projectEntityType,
                "isRelatedToProject", "isProjectRelatedTo", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Fallback Publication Title").build();

        Item project = ItemBuilder.createItem(context, projectCollection)
            .withTitle("Fallback Project").build();

        // Relationship WITHOUT rightwardValue — should fallback to publication's dc.title
        RelationshipBuilder.createRelationshipBuilder(
                context, publication, project, type, -1, 0)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.relation", "PublicationAuthority");
        configurationService.setProperty("choices.presentation.dc.relation", "suggest");
        configurationService.setProperty("authority.controlled.dc.relation", "true");

        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t",
            String.valueOf(type.getID())
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        assertThat("Migration should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        project = reload(project);
        List<MetadataValue> relations = itemService.getMetadataByMetadataString(
            project, "dc.relation");
        assertThat("Should have 1 dc.relation metadata value", relations, hasSize(1));
        assertEquals("Fallback Publication Title", relations.get(0).getValue());
        assertEquals(publication.getID().toString(), relations.get(0).getAuthority());
        assertEquals(CF_ACCEPTED, relations.get(0).getConfidence());

        configurationService.setProperty("choices.plugin.dc.relation", savedRelationPlugin);
        configurationService.setProperty("choices.presentation.dc.relation", savedRelationPresentation);
        configurationService.setProperty("authority.controlled.dc.relation", savedRelationControlled);
    }

    // ============= Migrate-All & Batch-Size Tests =============

    /**
     * Test that omitting the -t option migrates every relationship type that
     * has a configured migration definition.
     */
    @Test
    public void testMigrateAllWhenTypeIdOmitted() throws Exception {
        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty(
            "plugin.named.org.dspace.content.authority.ChoiceAuthority", null);
        configurationService.setProperty("choices.plugin.dc.contributor.author", null);
        configurationService.setProperty("choices.presentation.dc.contributor.author", null);
        configurationService.setProperty("authority.controlled.dc.contributor.author", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType personEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        personCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Persons").withEntityType("Person").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        RelationshipType isAuthorOfPublication = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, personEntityType,
                "isAuthorOfPublication", "isPublicationOfAuthor", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item author = ItemBuilder.createItem(context, personCollection)
            .withTitle("Migrate All Author").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Migrate All Paper").build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, author, isAuthorOfPublication, 0, -1)
            .withLeftwardValue("Migrate All Author").build();

        context.commit();
        context.restoreAuthSystemState();

        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.contributor.author", "PersonAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        clearAllAuthorityCaches();

        // Run WITHOUT -t : every configured relationship type should be migrated
        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] { "relationship-to-authority-migrate" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        assertThat("Migration should complete without errors",
            runnableHandler.getErrorMessages(), empty());
        boolean foundAllMode = runnableHandler.getInfoMessages().stream()
            .anyMatch(msg -> msg.contains("Mode: ALL configured relationship types"));
        assertThat("Should run in migrate-all mode", foundAllMode, is(true));

        publication = reload(publication);
        List<MetadataValue> authors = itemService.getMetadataByMetadataString(
            publication, "dc.contributor.author");
        assertThat("Should have 1 author metadata value", authors, hasSize(1));
        assertEquals("Migrate All Author", authors.get(0).getValue());
        assertEquals(author.getID().toString(), authors.get(0).getAuthority());
        assertEquals(CF_ACCEPTED, authors.get(0).getConfidence());

        configurationService.setProperty("choices.plugin.dc.contributor.author", savedChoicesPlugin);
        configurationService.setProperty("choices.presentation.dc.contributor.author", savedChoicesPresentation);
        configurationService.setProperty("authority.controlled.dc.contributor.author", savedAuthorityControlled);
    }

    /**
     * Test that the relationship paging batch size can be set with the -b option.
     */
    @Test
    public void testBatchSizeOption() throws Exception {
        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty(
            "plugin.named.org.dspace.content.authority.ChoiceAuthority", null);
        configurationService.setProperty("choices.plugin.dc.contributor.author", null);
        configurationService.setProperty("choices.presentation.dc.contributor.author", null);
        configurationService.setProperty("authority.controlled.dc.contributor.author", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType personEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        personCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Persons").withEntityType("Person").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        RelationshipType isAuthorOfPublication = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, personEntityType,
                "isAuthorOfPublication", "isPublicationOfAuthor", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item author1 = ItemBuilder.createItem(context, personCollection).withTitle("Author One").build();
        Item author2 = ItemBuilder.createItem(context, personCollection).withTitle("Author Two").build();
        Item author3 = ItemBuilder.createItem(context, personCollection).withTitle("Author Three").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Batched Paper").build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, author1, isAuthorOfPublication, 0, -1).withLeftwardValue("Author One").build();
        RelationshipBuilder.createRelationshipBuilder(
                context, publication, author2, isAuthorOfPublication, 1, -1).withLeftwardValue("Author Two").build();
        RelationshipBuilder.createRelationshipBuilder(
                context, publication, author3, isAuthorOfPublication, 2, -1).withLeftwardValue("Author Three").build();

        context.commit();
        context.restoreAuthSystemState();

        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.contributor.author", "PersonAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t", String.valueOf(isAuthorOfPublication.getID()), "-b", "1"
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        assertThat("Migration should complete without errors",
            runnableHandler.getErrorMessages(), empty());
        boolean foundBatch = runnableHandler.getInfoMessages().stream()
            .anyMatch(msg -> msg.contains("Batch size: 1"));
        assertThat("Should log the configured batch size of 1", foundBatch, is(true));

        publication = reload(publication);
        List<MetadataValue> authors = itemService.getMetadataByMetadataString(
            publication, "dc.contributor.author");
        assertThat("All 3 authors should be migrated across batches", authors, hasSize(3));

        configurationService.setProperty("choices.plugin.dc.contributor.author", savedChoicesPlugin);
        configurationService.setProperty("choices.presentation.dc.contributor.author", savedChoicesPresentation);
        configurationService.setProperty("authority.controlled.dc.contributor.author", savedAuthorityControlled);
    }

    // ============= Project RIGHTWARD Migration Tests =============
    // The dual isProjectOfPublication/isPublicationOfProject type also writes
    // dc.relation.publication on the right (Project) item.

    @Test
    public void testMigratePublicationOfProjectRightward() throws Exception {
        String savedPlugin = configurationService.getProperty("choices.plugin.dc.relation.publication");
        String savedPres = configurationService.getProperty("choices.presentation.dc.relation.publication");
        String savedCtrl = configurationService.getProperty("authority.controlled.dc.relation.publication");

        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty("choices.plugin.dc.relation.publication", null);
        configurationService.setProperty("choices.presentation.dc.relation.publication", null);
        configurationService.setProperty("authority.controlled.dc.relation.publication", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType projectEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        projectCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Projects").withEntityType("Project").build();

        RelationshipType type = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, projectEntityType,
                "isProjectOfPublication", "isPublicationOfProject", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Source Publication").build();

        Item project = ItemBuilder.createItem(context, projectCollection)
            .withTitle("Target Project").build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, project, type, -1, 0)
            .withRightwardValue("Source Publication")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.relation.publication", "PublicationAuthority");
        configurationService.setProperty("choices.presentation.dc.relation.publication", "suggest");
        configurationService.setProperty("authority.controlled.dc.relation.publication", "true");

        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t", String.valueOf(type.getID())
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        assertThat("Migration should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        project = reload(project);
        List<MetadataValue> relations = itemService.getMetadataByMetadataString(
            project, "dc.relation.publication");
        assertThat("Project should have 1 dc.relation.publication value", relations, hasSize(1));
        assertEquals("Source Publication", relations.get(0).getValue());
        assertEquals(publication.getID().toString(), relations.get(0).getAuthority());
        assertEquals(CF_ACCEPTED, relations.get(0).getConfidence());
        assertEquals(0, relations.get(0).getPlace());

        configurationService.setProperty("choices.plugin.dc.relation.publication", savedPlugin);
        configurationService.setProperty("choices.presentation.dc.relation.publication", savedPres);
        configurationService.setProperty("authority.controlled.dc.relation.publication", savedCtrl);
    }

    @Test
    public void testMigratePublicationOfProjectRightwardWithDelete() throws Exception {
        String savedPlugin = configurationService.getProperty("choices.plugin.dc.relation.publication");
        String savedPres = configurationService.getProperty("choices.presentation.dc.relation.publication");
        String savedCtrl = configurationService.getProperty("authority.controlled.dc.relation.publication");

        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty("choices.plugin.dc.relation.publication", null);
        configurationService.setProperty("choices.presentation.dc.relation.publication", null);
        configurationService.setProperty("authority.controlled.dc.relation.publication", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType projectEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        projectCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Projects").withEntityType("Project").build();

        RelationshipType type = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, projectEntityType,
                "isProjectOfPublication", "isPublicationOfProject", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Delete Publication").build();

        Item project = ItemBuilder.createItem(context, projectCollection)
            .withTitle("Delete Project").build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, project, type, -1, 0)
            .withRightwardValue("Delete Publication")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.relation.publication", "PublicationAuthority");
        configurationService.setProperty("choices.presentation.dc.relation.publication", "suggest");
        configurationService.setProperty("authority.controlled.dc.relation.publication", "true");

        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t", String.valueOf(type.getID()), "-d"
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        assertThat("Migration should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        project = reload(project);
        List<MetadataValue> relations = itemService.getMetadataByMetadataString(
            project, "dc.relation.publication");
        assertThat("Project should have 1 dc.relation.publication value", relations, hasSize(1));
        assertEquals(publication.getID().toString(), relations.get(0).getAuthority());

        List<Relationship> remaining = relationshipService.findByRelationshipType(context, type);
        assertThat("Relationships should be deleted with -d flag", remaining, empty());

        configurationService.setProperty("choices.plugin.dc.relation.publication", savedPlugin);
        configurationService.setProperty("choices.presentation.dc.relation.publication", savedPres);
        configurationService.setProperty("authority.controlled.dc.relation.publication", savedCtrl);
    }

    @Test
    public void testMigratePublicationOfProjectRightwardDryRun() throws Exception {
        String savedPlugin = configurationService.getProperty("choices.plugin.dc.relation.publication");
        String savedPres = configurationService.getProperty("choices.presentation.dc.relation.publication");
        String savedCtrl = configurationService.getProperty("authority.controlled.dc.relation.publication");

        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty("choices.plugin.dc.relation.publication", null);
        configurationService.setProperty("choices.presentation.dc.relation.publication", null);
        configurationService.setProperty("authority.controlled.dc.relation.publication", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType projectEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        projectCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Projects").withEntityType("Project").build();

        RelationshipType type = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, projectEntityType,
                "isProjectOfPublication", "isPublicationOfProject", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("DryRun Publication").build();

        Item project = ItemBuilder.createItem(context, projectCollection)
            .withTitle("DryRun Project").build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, project, type, -1, 0)
            .withRightwardValue("DryRun Publication")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.relation.publication", "PublicationAuthority");
        configurationService.setProperty("choices.presentation.dc.relation.publication", "suggest");
        configurationService.setProperty("authority.controlled.dc.relation.publication", "true");

        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t", String.valueOf(type.getID()), "-n"
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        assertThat("Dry-run should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        project = reload(project);
        List<MetadataValue> relations = itemService.getMetadataByMetadataString(
            project, "dc.relation.publication");
        assertThat("Project should have no dc.relation.publication in dry-run", relations, empty());

        List<Relationship> remaining = relationshipService.findByRelationshipType(context, type);
        assertThat("Relationships should still exist in dry-run", remaining, hasSize(1));

        configurationService.setProperty("choices.plugin.dc.relation.publication", savedPlugin);
        configurationService.setProperty("choices.presentation.dc.relation.publication", savedPres);
        configurationService.setProperty("authority.controlled.dc.relation.publication", savedCtrl);
    }

    // ============= JournalIssue RIGHTWARD Migration Tests =============
    // isPublicationOfJournalIssue/isJournalIssueOfPublication (rightward only)
    // writes dc.relation.ispartof on the right (JournalIssue) item.

    @Test
    public void testMigrateJournalIssueOfPublicationRightward() throws Exception {
        String savedPlugin = configurationService.getProperty("choices.plugin.dc.relation.ispartof");
        String savedPres = configurationService.getProperty("choices.presentation.dc.relation.ispartof");
        String savedCtrl = configurationService.getProperty("authority.controlled.dc.relation.ispartof");

        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty("choices.plugin.dc.relation.ispartof", null);
        configurationService.setProperty("choices.presentation.dc.relation.ispartof", null);
        configurationService.setProperty("authority.controlled.dc.relation.ispartof", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType journalIssueEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "JournalIssue").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        Collection journalIssueCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Journal Issues").withEntityType("JournalIssue").build();

        RelationshipType type = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, journalIssueEntityType,
                "isPublicationOfJournalIssue", "isJournalIssueOfPublication", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Hosted Publication").build();

        Item journalIssue = ItemBuilder.createItem(context, journalIssueCollection)
            .withTitle("Journal Issue 2026").build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, journalIssue, type, -1, 0)
            .withRightwardValue("Hosted Publication")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.relation.ispartof", "PublicationAuthority");
        configurationService.setProperty("choices.presentation.dc.relation.ispartof", "suggest");
        configurationService.setProperty("authority.controlled.dc.relation.ispartof", "true");

        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t", String.valueOf(type.getID())
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        assertThat("Migration should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        journalIssue = reload(journalIssue);
        List<MetadataValue> relations = itemService.getMetadataByMetadataString(
            journalIssue, "dc.relation.ispartof");
        assertThat("JournalIssue should have 1 dc.relation.ispartof value", relations, hasSize(1));
        assertEquals("Hosted Publication", relations.get(0).getValue());
        assertEquals(publication.getID().toString(), relations.get(0).getAuthority());
        assertEquals(CF_ACCEPTED, relations.get(0).getConfidence());
        assertEquals(0, relations.get(0).getPlace());

        configurationService.setProperty("choices.plugin.dc.relation.ispartof", savedPlugin);
        configurationService.setProperty("choices.presentation.dc.relation.ispartof", savedPres);
        configurationService.setProperty("authority.controlled.dc.relation.ispartof", savedCtrl);
    }

    @Test
    public void testMigrateJournalIssueOfPublicationRightwardWithDelete() throws Exception {
        String savedPlugin = configurationService.getProperty("choices.plugin.dc.relation.ispartof");
        String savedPres = configurationService.getProperty("choices.presentation.dc.relation.ispartof");
        String savedCtrl = configurationService.getProperty("authority.controlled.dc.relation.ispartof");

        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty("choices.plugin.dc.relation.ispartof", null);
        configurationService.setProperty("choices.presentation.dc.relation.ispartof", null);
        configurationService.setProperty("authority.controlled.dc.relation.ispartof", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType journalIssueEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "JournalIssue").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        Collection journalIssueCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Journal Issues").withEntityType("JournalIssue").build();

        RelationshipType type = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, journalIssueEntityType,
                "isPublicationOfJournalIssue", "isJournalIssueOfPublication", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Delete Hosted Publication").build();

        Item journalIssue = ItemBuilder.createItem(context, journalIssueCollection)
            .withTitle("Delete Journal Issue").build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, journalIssue, type, -1, 0)
            .withRightwardValue("Delete Hosted Publication")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.relation.ispartof", "PublicationAuthority");
        configurationService.setProperty("choices.presentation.dc.relation.ispartof", "suggest");
        configurationService.setProperty("authority.controlled.dc.relation.ispartof", "true");

        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t", String.valueOf(type.getID()), "-d"
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        assertThat("Migration should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        journalIssue = reload(journalIssue);
        List<MetadataValue> relations = itemService.getMetadataByMetadataString(
            journalIssue, "dc.relation.ispartof");
        assertThat("JournalIssue should have 1 dc.relation.ispartof value", relations, hasSize(1));
        assertEquals(publication.getID().toString(), relations.get(0).getAuthority());

        List<Relationship> remaining = relationshipService.findByRelationshipType(context, type);
        assertThat("Relationships should be deleted with -d flag", remaining, empty());

        configurationService.setProperty("choices.plugin.dc.relation.ispartof", savedPlugin);
        configurationService.setProperty("choices.presentation.dc.relation.ispartof", savedPres);
        configurationService.setProperty("authority.controlled.dc.relation.ispartof", savedCtrl);
    }

    @Test
    public void testMigrateJournalIssueOfPublicationRightwardDryRun() throws Exception {
        String savedPlugin = configurationService.getProperty("choices.plugin.dc.relation.ispartof");
        String savedPres = configurationService.getProperty("choices.presentation.dc.relation.ispartof");
        String savedCtrl = configurationService.getProperty("authority.controlled.dc.relation.ispartof");

        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty("choices.plugin.dc.relation.ispartof", null);
        configurationService.setProperty("choices.presentation.dc.relation.ispartof", null);
        configurationService.setProperty("authority.controlled.dc.relation.ispartof", null);

        clearAllAuthorityCaches();

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType journalIssueEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "JournalIssue").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType("Publication").build();

        Collection journalIssueCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Journal Issues").withEntityType("JournalIssue").build();

        RelationshipType type = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, journalIssueEntityType,
                "isPublicationOfJournalIssue", "isJournalIssueOfPublication", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("DryRun Hosted Publication").build();

        Item journalIssue = ItemBuilder.createItem(context, journalIssueCollection)
            .withTitle("DryRun Journal Issue").build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, journalIssue, type, -1, 0)
            .withRightwardValue("DryRun Hosted Publication")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("choices.plugin.dc.relation.ispartof", "PublicationAuthority");
        configurationService.setProperty("choices.presentation.dc.relation.ispartof", "suggest");
        configurationService.setProperty("authority.controlled.dc.relation.ispartof", "true");

        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "relationship-to-authority-migrate", "-t", String.valueOf(type.getID()), "-n"
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);

        assertThat("Dry-run should complete without errors",
            runnableHandler.getErrorMessages(), empty());

        journalIssue = reload(journalIssue);
        List<MetadataValue> relations = itemService.getMetadataByMetadataString(
            journalIssue, "dc.relation.ispartof");
        assertThat("JournalIssue should have no dc.relation.ispartof in dry-run", relations, empty());

        List<Relationship> remaining = relationshipService.findByRelationshipType(context, type);
        assertThat("Relationships should still exist in dry-run", remaining, hasSize(1));

        configurationService.setProperty("choices.plugin.dc.relation.ispartof", savedPlugin);
        configurationService.setProperty("choices.presentation.dc.relation.ispartof", savedPres);
        configurationService.setProperty("authority.controlled.dc.relation.ispartof", savedCtrl);
    }

    /**
     * Test that an unknown relationship type id is reported cleanly.
     */
    @Test
    public void testMigrateWithInvalidTypeId() throws Exception {
        enableAuthorAuthorityConfig();
        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler handler = runMigration(
            "relationship-to-authority-migrate", "-t", "999999");

        assertThat("Should fail with IllegalArgumentException for unknown type id",
            handler.getException(), instanceOf(IllegalArgumentException.class));
        assertThat("Error message should mention missing relationship type",
            handler.getErrorMessages().stream()
                .anyMatch(m -> m.contains("not found")), is(true));
    }

    /**
     * Test that a relationship type with no registered migration strategy is rejected.
     */
    @Test
    public void testMigrateTypeWithNoStrategy() throws Exception {
        disableAuthorAuthorityConfig();
        clearAllAuthorityCaches();

        TestScenario scenario = createAuthorScenario();

        Item author = ItemBuilder.createItem(context, scenario.personCollection)
            .withTitle("Mike Ehrmantraut")
            .build();

        Item publication = ItemBuilder.createItem(context, scenario.publicationCollection)
            .withTitle("Security Consulting")
            .build();

        RelationshipType unconfiguredType = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, scenario.publicationEntityType, scenario.personEntityType,
                "isEditorOfPublication", "isPublicationOfEditor", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, author, unconfiguredType, 0, -1)
            .withLeftwardValue("Mike Ehrmantraut")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        enableAuthorAuthorityConfig();
        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler handler = runMigration(
            "relationship-to-authority-migrate", "-t",
            String.valueOf(unconfiguredType.getID()));

        assertThat("Should fail with IllegalArgumentException for unconfigured type",
            handler.getException(), instanceOf(IllegalArgumentException.class));
        assertThat("Error message should mention no migration definition",
            handler.getErrorMessages().stream()
                .anyMatch(m -> m.contains("No migration definition")), is(true));

        // Relationships must remain untouched.
        List<Relationship> remaining = relationshipService.findByRelationshipType(
            context, unconfiguredType);
        assertThat("Relationships should not be changed", remaining, hasSize(1));
    }

    /**
     * Test that combining delete ({@code -d}) and dry-run ({@code -n}) does not
     * delete relationships.
     */
    @Test
    public void testMigrateDryRunWithDeleteDoesNotDelete() throws Exception {
        disableAuthorAuthorityConfig();
        clearAllAuthorityCaches();

        TestScenario scenario = createAuthorScenario();

        Item author = ItemBuilder.createItem(context, scenario.personCollection)
            .withTitle("Hank Schrader")
            .build();

        Item publication = ItemBuilder.createItem(context, scenario.publicationCollection)
            .withTitle("Minerals")
            .build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, author, scenario.relationshipType, 0, -1)
            .withLeftwardValue("Hank Schrader")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        enableAuthorAuthorityConfig();
        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler handler = runMigration(
            "relationship-to-authority-migrate", "-t",
            String.valueOf(scenario.relationshipType.getID()), "-d", "-n");

        assertThat("Dry-run with delete should complete without errors",
            handler.getErrorMessages(), empty());

        publication = reload(publication);
        List<MetadataValue> authors = itemService.getMetadataByMetadataString(
            publication, "dc.contributor.author");
        assertThat("No authority metadata should be added in dry-run", authors, empty());

        List<Relationship> remaining = relationshipService.findByRelationshipType(
            context, scenario.relationshipType);
        assertThat("Relationships should still exist in dry-run", remaining, hasSize(1));
    }

    /**
     * Test that a withdrawn target item is processed without error.
     */
    @Test
    public void testMigrateWithdrawnItem() throws Exception {
        disableAuthorAuthorityConfig();
        clearAllAuthorityCaches();

        TestScenario scenario = createAuthorScenario();

        Item author = ItemBuilder.createItem(context, scenario.personCollection)
            .withTitle("Skyler White")
            .build();

        Item publication = ItemBuilder.createItem(context, scenario.publicationCollection)
            .withTitle("Beneke Accounts")
            .build();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication, author, scenario.relationshipType, 0, -1)
            .withLeftwardValue("Skyler White")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        context.turnOffAuthorisationSystem();
        itemService.withdraw(context, publication);
        context.commit();
        context.restoreAuthSystemState();

        enableAuthorAuthorityConfig();
        clearAllAuthorityCaches();

        TestDSpaceRunnableHandler handler = runMigration(
            "relationship-to-authority-migrate", "-t",
            String.valueOf(scenario.relationshipType.getID()));

        assertThat("Migration should complete without errors for withdrawn item",
            handler.getErrorMessages(), empty());

        publication = reload(publication);
        List<MetadataValue> authors = itemService.getMetadataByMetadataString(
            publication, "dc.contributor.author");
        assertThat("Should have 1 author metadata value", authors, hasSize(1));
        assertEquals("Skyler White", authors.get(0).getValue());
        assertEquals(author.getID().toString(), authors.get(0).getAuthority());
        assertEquals(CF_ACCEPTED, authors.get(0).getConfidence());
    }

    /**
     * Holder for the immutable data created by {@link #createAuthorScenario()}.
     */
    private static final class TestScenario {
        final EntityType publicationEntityType;
        final EntityType personEntityType;
        final Collection personCollection;
        final Collection publicationCollection;
        final RelationshipType relationshipType;

        TestScenario(EntityType publicationEntityType, EntityType personEntityType,
                     Collection personCollection, Collection publicationCollection,
                     RelationshipType relationshipType) {
            this.publicationEntityType = publicationEntityType;
            this.personEntityType = personEntityType;
            this.personCollection = personCollection;
            this.publicationCollection = publicationCollection;
            this.relationshipType = relationshipType;
        }
    }

    /**
     * Immutable snapshot of an authority configuration for a single metadata field.
     */
    private static final class SavedAuthorityConfig {
        final String plugin;
        final String presentation;
        final String controlled;

        SavedAuthorityConfig(String plugin, String presentation, String controlled) {
            this.plugin = plugin;
            this.presentation = presentation;
            this.controlled = controlled;
        }
    }

    /**
     * Read the current authority configuration for a single metadata field.
     *
     * @param pluginProperty the choices.plugin property name
     * @param presentationProperty the choices.presentation property name
     * @param controlledProperty the authority.controlled property name
     * @return the saved configuration snapshot
     */
    private SavedAuthorityConfig saveAuthorityConfig(String pluginProperty,
                                                     String presentationProperty,
                                                     String controlledProperty) {
        return new SavedAuthorityConfig(
            configurationService.getProperty(pluginProperty),
            configurationService.getProperty(presentationProperty),
            configurationService.getProperty(controlledProperty));
    }

    /**
     * Restore the authority configuration for a single metadata field.
     *
     * @param pluginProperty the choices.plugin property name
     * @param presentationProperty the choices.presentation property name
     * @param controlledProperty the authority.controlled property name
     * @param saved the saved configuration snapshot
     */
    private void restoreRelationAuthorityConfig(String pluginProperty,
                                                String presentationProperty,
                                                String controlledProperty,
                                                SavedAuthorityConfig saved) {
        configurationService.setProperty(pluginProperty, saved.plugin);
        configurationService.setProperty(presentationProperty, saved.presentation);
        configurationService.setProperty(controlledProperty, saved.controlled);
    }

    /**
     * Disable authority plugins for {@code dc.contributor.author} and enable
     * virtual metadata so relationships can be created without authority
     * interference.
     */
    private void disableAuthorAuthorityConfig() {
        configurationService.setProperty("item.enable-virtual-metadata", true);
        configurationService.setProperty(
            "plugin.named.org.dspace.content.authority.ChoiceAuthority", (String[]) null);
        configurationService.setProperty("choices.plugin.dc.contributor.author", null);
        configurationService.setProperty("choices.presentation.dc.contributor.author", null);
        configurationService.setProperty("authority.controlled.dc.contributor.author", null);
    }

    /**
     * Restore the {@code dc.contributor.author} authority configuration to
     * the values captured in {@link #saveConfiguration()}.
     */
    private void restoreAuthorAuthorityConfig() {
        configurationService.setProperty(
            "plugin.named.org.dspace.content.authority.ChoiceAuthority", savedAuthorityPlugins);
        configurationService.setProperty("choices.plugin.dc.contributor.author", savedChoicesPlugin);
        configurationService.setProperty("choices.presentation.dc.contributor.author", savedChoicesPresentation);
        configurationService.setProperty("authority.controlled.dc.contributor.author", savedAuthorityControlled);
    }

    /**
     * Enable the {@code dc.contributor.author} authority configuration for
     * migration (PersonAuthority / suggest / true).
     */
    private void enableAuthorAuthorityConfig() {
        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = PersonAuthority" });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "PersonAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
    }

    /**
     * Create the common entity types, collections and relationship type used
     * by the author-migration tests.
     *
     * @return the populated test scenario
     * @throws SQLException if a database error occurs
     */
    private TestScenario createAuthorScenario() throws SQLException {
        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType personEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection personCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Persons")
            .withEntityType("Person")
            .build();

        Collection publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications")
            .withEntityType("Publication")
            .build();

        RelationshipType isAuthorOfPublication = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationEntityType, personEntityType,
                "isAuthorOfPublication", "isPublicationOfAuthor", 0, null, 0, null)
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        return new TestScenario(publicationEntityType, personEntityType,
            personCollection, publicationCollection, isAuthorOfPublication);
    }

    /**
     * Run the migration script with the supplied CLI arguments and return the
     * captured handler for assertions.
     *
     * @param args the script arguments (including the script name)
     * @return the handler capturing logs, errors and exceptions
     * @throws Exception if script launch fails
     */
    private TestDSpaceRunnableHandler runMigration(String... args) throws Exception {
        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);
        return runnableHandler;
    }

    @SuppressWarnings("rawtypes")
    private <T extends ReloadableEntity> T reload(T entity) throws SQLException {
        return context.reloadEntity(entity);
    }

    /**
     * Clears all authority-related caches to ensure configuration changes take effect.
     * CRITICAL: This must be called after any authority configuration changes.
     */
    private static void clearAllAuthorityCaches() throws Exception {
        try {
            pluginService.clearNamedPluginClasses();  // Can throw SubmissionConfigReaderException
            choiceAuthorityService.clearCache();
            metadataAuthorityService.clearCache();  // Critical for authority config reload
        } catch (Exception e) {
            throw new Exception("Failed to clear authority caches: " + e.getMessage(), e);
        }
    }
}
