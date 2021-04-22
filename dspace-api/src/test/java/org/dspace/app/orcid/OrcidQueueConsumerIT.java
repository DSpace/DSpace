/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid;

import static org.dspace.app.matcher.OrcidQueueMatcher.matches;
import static org.dspace.app.profile.OrcidEntitySyncPreference.ALL;
import static org.dspace.app.profile.OrcidEntitySyncPreference.DISABLED;
import static org.dspace.app.profile.OrcidProfileSyncPreference.AFFILIATION;
import static org.dspace.app.profile.OrcidProfileSyncPreference.EDUCATION;
import static org.dspace.builder.OrcidHistoryBuilder.createOrcidHistory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import java.sql.SQLException;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.orcid.consumer.OrcidQueueConsumer;
import org.dspace.app.orcid.factory.OrcidServiceFactory;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link OrcidQueueConsumer}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidQueueConsumerIT extends AbstractIntegrationTestWithDatabase {

    private OrcidQueueService orcidQueueService = OrcidServiceFactory.getInstance().getOrcidQueueService();

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private Collection profileCollection;

    @Before
    public void setup() {

        context.setDispatcher("cris-default");
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent community")
            .build();

        profileCollection = createCollection("Profiles", "Person");

        context.restoreAuthSystemState();
    }

    @After
    public void after() throws SQLException, AuthorizeException {
        List<OrcidQueue> records = orcidQueueService.findAll(context);
        for (OrcidQueue record : records) {
            orcidQueueService.delete(context, record);
        }
        context.setDispatcher(null);
    }

    @Test
    public void testOrcidQueueRecordCreationForProfile() throws Exception {
        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test User")
            .withCrisOwner(eperson)
            .withOrcidIdentifier("0000-1111-2222-3333")
            .withOrcidAccessToken("ab4d18a0-8d9a-40f1-b601-a417255c8d20")
            .withOrcidSynchronizationProfilePreference(AFFILIATION)
            .withOrcidSynchronizationProfilePreference(EDUCATION)
            .build();

        context.restoreAuthSystemState();
        context.commit();

        List<OrcidQueue> orcidQueueRecords = orcidQueueService.findAll(context);
        assertThat(orcidQueueRecords, hasSize(1));
        assertThat(orcidQueueRecords.get(0), matches(profile, profile, "Person", null));

        addMetadata(profile, "person", "birthDate", null, "1992-06-26", null);
        context.commit();

        List<OrcidQueue> newOrcidQueueRecords = orcidQueueService.findAll(context);
        assertThat(newOrcidQueueRecords, hasSize(1));

        assertThat(orcidQueueRecords.get(0), equalTo(newOrcidQueueRecords.get(0)));
    }

    @Test
    public void testNoOrcidQueueRecordCreationOccursIfProfileSynchronizationIsDisabled() throws SQLException {
        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test User")
            .withCrisOwner(eperson)
            .withOrcidIdentifier("0000-1111-2222-3333")
            .withOrcidAccessToken("ab4d18a0-8d9a-40f1-b601-a417255c8d20")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        assertThat(orcidQueueService.findAll(context), empty());
    }

    @Test
    public void testOrcidQueueRecordCreationForPublication() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test User")
            .withCrisOwner(eperson)
            .withOrcidIdentifier("0000-1111-2222-3333")
            .withOrcidAccessToken("ab4d18a0-8d9a-40f1-b601-a417255c8d20")
            .withOrcidSynchronizationPublicationsPreference(ALL)
            .build();

        Collection publicationCollection = createCollection("Publications", "Publication");

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Test publication")
            .withAuthor("Test User", profile.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        List<OrcidQueue> orcidQueueRecords = orcidQueueService.findAll(context);
        assertThat(orcidQueueRecords, hasSize(1));
        assertThat(orcidQueueRecords.get(0), matches(profile, publication, "Publication", null));

        addMetadata(publication, "dc", "contributor", "editor", "Editor", null);
        context.commit();

        List<OrcidQueue> newOrcidQueueRecords = orcidQueueService.findAll(context);
        assertThat(newOrcidQueueRecords, hasSize(1));

        assertThat(orcidQueueRecords.get(0), equalTo(newOrcidQueueRecords.get(0)));
    }

    @Test
    public void testOrcidQueueRecordCreationToUpdatePublication() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test User")
            .withCrisOwner(eperson)
            .withOrcidIdentifier("0000-1111-2222-3333")
            .withOrcidAccessToken("ab4d18a0-8d9a-40f1-b601-a417255c8d20")
            .withOrcidSynchronizationPublicationsPreference(ALL)
            .build();

        Collection publicationCollection = createCollection("Publications", "Publication");

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Test publication")
            .build();

        createOrcidHistory(context, profile, publication)
            .withPutCode("123456")
            .build();

        addMetadata(publication, "dc", "contributor", "author", "Test User", profile.getID().toString());

        context.restoreAuthSystemState();
        context.commit();

        List<OrcidQueue> orcidQueueRecords = orcidQueueService.findAll(context);
        assertThat(orcidQueueRecords, hasSize(1));
        assertThat(orcidQueueRecords.get(0), matches(profile, publication, "Publication", "123456"));
    }

    @Test
    public void testNoOrcidQueueRecordCreationOccursIfPublicationSynchronizationIsDisabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test User")
            .withCrisOwner(eperson)
            .withOrcidIdentifier("0000-1111-2222-3333")
            .withOrcidAccessToken("ab4d18a0-8d9a-40f1-b601-a417255c8d20")
            .build();

        Collection publicationCollection = createCollection("Publications", "Publication");

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Test publication")
            .withAuthor("Test User", profile.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        assertThat(orcidQueueService.findAll(context), empty());

        addMetadata(profile, "cris", "orcid", "sync-publications", DISABLED.name(), null);
        addMetadata(publication, "dc", "date", "issued", "2021-01-01", null);
        context.commit();

        assertThat(orcidQueueService.findAll(context), empty());
    }

    @Test
    public void testOrcidQueueRecordCreationForProject() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test User")
            .withCrisOwner(eperson)
            .withOrcidIdentifier("0000-1111-2222-3333")
            .withOrcidAccessToken("ab4d18a0-8d9a-40f1-b601-a417255c8d20")
            .withOrcidSynchronizationProjectsPreference(ALL)
            .build();

        Collection projectCollection = createCollection("Projects", "Project");

        Item project = ItemBuilder.createItem(context, projectCollection)
            .withTitle("Test project")
            .withProjectCoordinator("Test User", profile.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        List<OrcidQueue> orcidQueueRecords = orcidQueueService.findAll(context);
        assertThat(orcidQueueRecords, hasSize(1));
        assertThat(orcidQueueRecords.get(0), matches(profile, project, "Project", null));

        addMetadata(project, "dc", "type", null, "Project type", null);
        context.commit();

        List<OrcidQueue> newOrcidQueueRecords = orcidQueueService.findAll(context);
        assertThat(newOrcidQueueRecords, hasSize(1));

        assertThat(orcidQueueRecords.get(0), equalTo(newOrcidQueueRecords.get(0)));
    }

    @Test
    public void testOrcidQueueRecordCreationToUpdateProject() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test User")
            .withCrisOwner(eperson)
            .withOrcidIdentifier("0000-1111-2222-3333")
            .withOrcidAccessToken("ab4d18a0-8d9a-40f1-b601-a417255c8d20")
            .withOrcidSynchronizationProjectsPreference(ALL)
            .build();

        Collection projectCollection = createCollection("Projects", "Project");

        Item project = ItemBuilder.createItem(context, projectCollection)
            .withTitle("Test project")
            .build();

        createOrcidHistory(context, profile, project)
            .withPutCode("123456")
            .build();

        addMetadata(project, "crispj", "coordinator", null, "Test User", profile.getID().toString());

        context.restoreAuthSystemState();
        context.commit();

        List<OrcidQueue> orcidQueueRecords = orcidQueueService.findAll(context);
        assertThat(orcidQueueRecords, hasSize(1));
        assertThat(orcidQueueRecords.get(0), matches(profile, project, "Project", "123456"));
    }

    @Test
    public void testNoOrcidQueueRecordCreationOccursIfProjectSynchronizationIsDisabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test User")
            .withCrisOwner(eperson)
            .withOrcidIdentifier("0000-1111-2222-3333")
            .withOrcidAccessToken("ab4d18a0-8d9a-40f1-b601-a417255c8d20")
            .build();

        Collection projectCollection = createCollection("Projects", "Project");

        Item project = ItemBuilder.createItem(context, projectCollection)
            .withTitle("Test project")
            .withProjectCoordinator("Test User", profile.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        assertThat(orcidQueueService.findAll(context), empty());

        addMetadata(profile, "cris", "orcid", "sync-projects", DISABLED.name(), null);
        addMetadata(project, "crispj", "partnerou", null, "Partner", null);
        context.commit();

        assertThat(orcidQueueService.findAll(context), empty());
    }

    @Test
    public void testNoOrcidQueueRecordCreationOccursIfProfileHasNotOrcidIdentifier() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test User")
            .withCrisOwner(eperson)
            .withOrcidAccessToken("ab4d18a0-8d9a-40f1-b601-a417255c8d20")
            .build();

        Collection projectCollection = createCollection("Projects", "Project");

        ItemBuilder.createItem(context, projectCollection)
            .withTitle("Test project")
            .withOrcidSynchronizationProjectsPreference(ALL)
            .withProjectCoordinator("Test User", profile.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        assertThat(orcidQueueService.findAll(context), empty());
    }

    @Test
    public void testNoOrcidQueueRecordCreationOccursIfProfileHasNotOrcidAccessToken() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test User")
            .withCrisOwner(eperson)
            .withOrcidIdentifier("0000-1111-2222-3333")
            .build();

        Collection projectCollection = createCollection("Projects", "Project");

        ItemBuilder.createItem(context, projectCollection)
            .withTitle("Test project")
            .withOrcidSynchronizationProjectsPreference(ALL)
            .withProjectCoordinator("Test User", profile.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        assertThat(orcidQueueService.findAll(context), empty());
    }

    @Test
    public void testNoOrcidQueueRecordCreationOccursForNotConfiguredEntities() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test User")
            .withCrisOwner(eperson)
            .withOrcidIdentifier("0000-1111-2222-3333")
            .withOrcidAccessToken("ab4d18a0-8d9a-40f1-b601-a417255c8d20")
            .build();

        Collection fundingCollection = createCollection("Fundings", "Funding");

        ItemBuilder.createItem(context, fundingCollection)
            .withTitle("Test funding")
            .withAuthor("Test User", profile.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        assertThat(orcidQueueService.findAll(context), empty());
    }

    @Test
    public void testManyOrcidQueueRecordCreations() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstProfile = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test User")
            .withCrisOwner(eperson)
            .withOrcidIdentifier("0000-1111-2222-3333")
            .withOrcidAccessToken("ab4d18a0-8d9a-40f1-b601-a417255c8d20")
            .withOrcidSynchronizationPublicationsPreference(ALL)
            .build();

        Item secondProfile = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Another Test User")
            .withCrisOwner(admin)
            .withOrcidIdentifier("0000-1111-2222-3333")
            .withOrcidAccessToken("ab4d18a0-8d9a-40f1-b601-a417255c8d20")
            .withOrcidSynchronizationPublicationsPreference(ALL)
            .build();

        Collection publicationCollection = createCollection("Publications", "Publication");

        Item firstPublication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Test publication")
            .withAuthor("Test User", firstProfile.getID().toString())
            .withEditor("Test User", firstProfile.getID().toString())
            .withAuthor("Another Test User", secondProfile.getID().toString())
            .build();

        Item secondPublication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Another Test publication")
            .withAuthor("Another Test User", secondProfile.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        List<OrcidQueue> orcidQueueRecords = orcidQueueService.findAll(context);
        assertThat(orcidQueueRecords, hasSize(3));

        assertThat(orcidQueueRecords, hasItem(matches(firstProfile, firstPublication, "Publication", null)));
        assertThat(orcidQueueRecords, hasItem(matches(secondProfile, firstPublication, "Publication", null)));
        assertThat(orcidQueueRecords, hasItem(matches(secondProfile, secondPublication, "Publication", null)));
    }

    private void addMetadata(Item item, String schema, String element, String qualifier, String value,
        String authority) throws Exception {
        context.turnOffAuthorisationSystem();
        item = context.reloadEntity(item);
        itemService.addMetadata(context, item, schema, element, qualifier, null, value, authority, 600);
        itemService.update(context, item);
        context.restoreAuthSystemState();
    }

    private Collection createCollection(String name, String entityType) {
        return CollectionBuilder.createCollection(context, parentCommunity)
            .withName(name)
            .withEntityType(entityType)
            .build();
    }

}
