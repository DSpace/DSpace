/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.script;

import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.dspace.app.matcher.LambdaMatcher.matches;
import static org.dspace.app.matcher.OrcidQueueMatcher.matches;
import static org.dspace.app.orcid.OrcidOperation.DELETE;
import static org.dspace.app.orcid.OrcidOperation.INSERT;
import static org.dspace.app.orcid.OrcidOperation.UPDATE;
import static org.dspace.app.profile.OrcidSynchronizationMode.BATCH;
import static org.dspace.app.profile.OrcidSynchronizationMode.MANUAL;
import static org.dspace.builder.OrcidQueueBuilder.createOrcidQueue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.ArrayUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.orcid.OrcidHistory;
import org.dspace.app.orcid.OrcidOperation;
import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.orcid.client.OrcidResponse;
import org.dspace.app.orcid.exception.OrcidClientException;
import org.dspace.app.orcid.factory.OrcidServiceFactory;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.app.orcid.service.impl.OrcidHistoryServiceImpl;
import org.dspace.app.profile.OrcidSynchronizationMode;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.OrcidTokenBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link OrcidBulkPush}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidBulkPushIT extends AbstractIntegrationTestWithDatabase {

    private Collection profileCollection;

    private Collection publicationCollection;

    private OrcidHistoryServiceImpl orcidHistoryService;

    private OrcidQueueService orcidQueueService;

    private ConfigurationService configurationService;

    private OrcidClient orcidClient;

    private OrcidClient orcidClientMock;

    @Before
    public void setup() {

        orcidHistoryService = (OrcidHistoryServiceImpl) OrcidServiceFactory.getInstance().getOrcidHistoryService();
        orcidQueueService = OrcidServiceFactory.getInstance().getOrcidQueueService();

        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

        context.setCurrentUser(admin);

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent community")
            .build();

        profileCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Profiles")
            .withEntityType("Person")
            .build();

        publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications")
            .withEntityType("Publication")
            .build();

        orcidClientMock = mock(OrcidClient.class);

        orcidClient = orcidHistoryService.getOrcidClient();
        orcidHistoryService.setOrcidClient(orcidClientMock);

    }

    @After
    public void after() throws SQLException {
        List<OrcidHistory> records = orcidHistoryService.findAll(context);
        for (OrcidHistory record : records) {
            orcidHistoryService.delete(context, record);
        }
        orcidHistoryService.setOrcidClient(orcidClient);
    }

    @Test
    public void testWithoutOrcidQueueRecords() throws Exception {
        TestDSpaceRunnableHandler handler = runBulkSynchronization(false);
        assertThat(handler.getInfoMessages(), hasSize(1));
        assertThat(handler.getInfoMessages().get(0), is("Found 0 queue records to synchronize with ORCID"));
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());
    }

    @Test
    public void testWithManyOrcidQueueRecords() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson owner = EPersonBuilder.createEPerson(context)
            .withEmail("owner@test.it")
            .build();
        context.restoreAuthSystemState();

        Item firstOwner = createOwnerItem("0000-1111-2222-3333", eperson, BATCH);
        Item secondOwner = createOwnerItem("1111-2222-3333-4444", admin, MANUAL);
        Item thirdOwner = createOwnerItem("2222-3333-4444-5555", owner, BATCH);

        Item firstEntity = createPublication("First publication");
        Item secondEntity = createPublication("Second publication");
        Item thirdEntity = createPublication("Third publication");
        Item fourthEntity = createPublication("Fourth publication");
        Item fifthEntity = createPublication("Fifth publication");

        when(orcidClientMock.push(any(), eq("0000-1111-2222-3333"), any()))
            .thenReturn(createdResponse("12345"));

        when(orcidClientMock.update(any(), eq("0000-1111-2222-3333"), any(), eq("98765")))
            .thenReturn(updatedResponse("98765"));

        when(orcidClientMock.deleteByPutCode(any(), eq("0000-1111-2222-3333"), eq("22222"), eq("/work")))
            .thenReturn(deletedResponse());

        when(orcidClientMock.push(any(), eq("2222-3333-4444-5555"), any()))
            .thenReturn(createdResponse("11111"));

        createOrcidQueue(context, firstOwner, firstEntity);
        createOrcidQueue(context, firstOwner, secondEntity, "98765");
        createOrcidQueue(context, firstOwner, "Description", "Publication", "22222");
        createOrcidQueue(context, secondOwner, thirdEntity);
        createOrcidQueue(context, secondOwner, fourthEntity);
        createOrcidQueue(context, thirdOwner, fifthEntity);

        context.commit();

        TestDSpaceRunnableHandler handler = runBulkSynchronization(false);

        String firstOwnerId = firstOwner.getID().toString();
        String thirdOwnerId = thirdOwner.getID().toString();

        assertThat(handler.getInfoMessages(), hasSize(9));
        assertThat(handler.getInfoMessages(), containsInAnyOrder(
            "Found 4 queue records to synchronize with ORCID",
            "Addition of Publication for profile with ID: " + firstOwnerId,
            "History record created with status 201. The operation was completed successfully",
            "Update of Publication for profile with ID: " + firstOwnerId + " by put code 98765",
            "History record created with status 200. The operation was completed successfully",
            "Deletion of Publication for profile with ID: " + firstOwnerId + " by put code 22222",
            "History record created with status 204. The operation was completed successfully",
            "Addition of Publication for profile with ID: " + thirdOwnerId,
            "History record created with status 201. The operation was completed successfully"));

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        verify(orcidClientMock).push(any(), eq("0000-1111-2222-3333"), any());
        verify(orcidClientMock).push(any(), eq("2222-3333-4444-5555"), any());
        verify(orcidClientMock).update(any(), eq("0000-1111-2222-3333"), any(), eq("98765"));
        verify(orcidClientMock).deleteByPutCode(any(), eq("0000-1111-2222-3333"), eq("22222"), eq("/work"));

        verifyNoMoreInteractions(orcidClientMock);

        List<OrcidQueue> queueRecords = orcidQueueService.findAll(context);
        assertThat(queueRecords, hasSize(2));
        assertThat(queueRecords, hasItem(matches(secondOwner, thirdEntity, "Publication", INSERT, 0)));
        assertThat(queueRecords, hasItem(matches(secondOwner, fourthEntity, "Publication", INSERT, 0)));

        List<OrcidHistory> historyRecords = orcidHistoryService.findAll(context);
        assertThat(historyRecords, hasSize(4));
        assertThat(historyRecords, hasItem(matches(history(firstOwner, firstEntity, 201, INSERT))));
        assertThat(historyRecords, hasItem(matches(history(firstOwner, secondEntity, 200, UPDATE))));
        assertThat(historyRecords, hasItem(matches(history(firstOwner, 204, DELETE))));
        assertThat(historyRecords, hasItem(matches(history(thirdOwner, fifthEntity, 201, INSERT))));

    }

    @Test
    public void testWithOneValidationError() throws Exception {

        Item firstOwner = createOwnerItem("0000-1111-2222-3333", eperson, BATCH);
        Item secondOwner = createOwnerItem("1111-2222-3333-4444", admin, BATCH);

        Item firstEntity = createPublication("First publication");
        Item secondEntity = createPublication("");
        Item thirdEntity = createPublication("Third publication");

        when(orcidClientMock.push(any(), eq("0000-1111-2222-3333"), any()))
            .thenReturn(createdResponse("12345"));

        when(orcidClientMock.push(any(), eq("1111-2222-3333-4444"), any()))
            .thenReturn(createdResponse("55555"));

        createOrcidQueue(context, firstOwner, firstEntity);
        createOrcidQueue(context, firstOwner, secondEntity, "98765");
        createOrcidQueue(context, secondOwner, thirdEntity);

        context.commit();

        TestDSpaceRunnableHandler handler = runBulkSynchronization(false);

        assertThat(handler.getInfoMessages(), hasSize(6));
        assertThat(handler.getInfoMessages(), containsInAnyOrder(
            "Found 3 queue records to synchronize with ORCID",
            "Addition of Publication for profile with ID: " + firstOwner.getID().toString(),
            "History record created with status 201. The operation was completed successfully",
            "Update of Publication for profile with ID: " + firstOwner.getID().toString() + " by put code 98765",
            "Addition of Publication for profile with ID: " + secondOwner.getID().toString(),
            "History record created with status 201. The operation was completed successfully"));

        assertThat(handler.getErrorMessages(), hasSize(1));
        assertThat(handler.getErrorMessages(), containsInAnyOrder(
            "Errors occurs during ORCID object validation. Error codes: title.required"));

        assertThat(handler.getWarningMessages(), empty());

        verify(orcidClientMock).push(any(), eq("0000-1111-2222-3333"), any());
        verify(orcidClientMock).push(any(), eq("1111-2222-3333-4444"), any());
        verifyNoMoreInteractions(orcidClientMock);

        List<OrcidQueue> queueRecords = orcidQueueService.findAll(context);
        assertThat(queueRecords, hasSize(1));
        assertThat(queueRecords, hasItem(matches(firstOwner, secondEntity, "Publication", UPDATE, 1)));

        List<OrcidHistory> historyRecords = orcidHistoryService.findAll(context);
        assertThat(historyRecords, hasSize(2));
        assertThat(historyRecords, hasItem(matches(history(firstOwner, firstEntity, 201, INSERT))));
        assertThat(historyRecords, hasItem(matches(history(secondOwner, thirdEntity, 201, INSERT))));

    }

    @Test
    public void testWithUnexpectedErrorForMissingOrcid() throws Exception {

        Item firstOwner = createOwnerItem("0000-1111-2222-3333", eperson, BATCH);
        Item secondOwner = createOwnerItem("", admin, BATCH);

        Item firstEntity = createPublication("First publication");
        Item secondEntity = createPublication("Second publication");

        when(orcidClientMock.push(any(), eq("0000-1111-2222-3333"), any()))
            .thenReturn(createdResponse("12345"));

        createOrcidQueue(context, secondOwner, secondEntity);
        createOrcidQueue(context, firstOwner, firstEntity);

        context.commit();

        TestDSpaceRunnableHandler handler = runBulkSynchronization(false);

        assertThat(handler.getInfoMessages(), hasSize(4));
        assertThat(handler.getInfoMessages(), containsInAnyOrder(
            "Found 2 queue records to synchronize with ORCID",
            "Addition of Publication for profile with ID: " + secondOwner.getID().toString(),
            "Addition of Publication for profile with ID: " + firstOwner.getID().toString(),
            "History record created with status 201. The operation was completed successfully"));

        assertThat(handler.getErrorMessages(), hasSize(1));
        assertThat(handler.getErrorMessages(), contains("An unexpected error occurs during the synchronization: "
            + "The related owner item (id = " + secondOwner.getID() + ") does not have an orcid"));

        assertThat(handler.getWarningMessages(), empty());

        verify(orcidClientMock).push(any(), eq("0000-1111-2222-3333"), any());
        verifyNoMoreInteractions(orcidClientMock);

        List<OrcidQueue> queueRecords = orcidQueueService.findAll(context);
        assertThat(queueRecords, hasSize(1));
        assertThat(queueRecords, hasItem(matches(secondOwner, secondEntity, "Publication", INSERT, 1)));

        List<OrcidHistory> historyRecords = orcidHistoryService.findAll(context);
        assertThat(historyRecords, hasSize(1));
        assertThat(historyRecords, hasItem(matches(history(firstOwner, firstEntity, 201, INSERT))));

    }

    @Test
    public void testWithOrcidClientException() throws Exception {

        Item firstOwner = createOwnerItem("0000-1111-2222-3333", eperson, BATCH);
        Item secondOwner = createOwnerItem("1111-2222-3333-4444", admin, BATCH);

        Item firstEntity = createPublication("First publication");
        Item secondEntity = createPublication("Second publication");

        when(orcidClientMock.push(any(), eq("0000-1111-2222-3333"), any()))
            .thenThrow(new OrcidClientException(400, "Bad request"));

        when(orcidClientMock.push(any(), eq("1111-2222-3333-4444"), any()))
            .thenReturn(createdResponse("55555"));

        createOrcidQueue(context, firstOwner, firstEntity);
        createOrcidQueue(context, secondOwner, secondEntity);

        context.commit();

        TestDSpaceRunnableHandler handler = runBulkSynchronization(false);

        assertThat(handler.getInfoMessages(), hasSize(5));
        assertThat(handler.getInfoMessages(), containsInAnyOrder(
            "Found 2 queue records to synchronize with ORCID",
            "Addition of Publication for profile with ID: " + firstOwner.getID().toString(),
            "History record created with status 400. The resource sent to ORCID registry is not valid",
            "Addition of Publication for profile with ID: " + secondOwner.getID().toString(),
            "History record created with status 201. The operation was completed successfully"));

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        verify(orcidClientMock).push(any(), eq("0000-1111-2222-3333"), any());
        verify(orcidClientMock).push(any(), eq("1111-2222-3333-4444"), any());
        verifyNoMoreInteractions(orcidClientMock);

        List<OrcidQueue> queueRecords = orcidQueueService.findAll(context);
        assertThat(queueRecords, hasSize(1));
        assertThat(queueRecords, hasItem(matches(firstOwner, firstEntity, "Publication", INSERT, 1)));

        List<OrcidHistory> historyRecords = orcidHistoryService.findAll(context);
        assertThat(historyRecords, hasSize(2));
        assertThat(historyRecords, hasItem(matches(history(firstOwner, firstEntity, 400, INSERT))));
        assertThat(historyRecords, hasItem(matches(history(secondOwner, secondEntity, 201, INSERT))));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWithTooManyAttempts() throws Exception {

        configurationService.setProperty("orcid.bulk-synchronization.max-attempts", 2);

        Item owner = createOwnerItem("0000-1111-2222-3333", eperson, BATCH);
        Item entity = createPublication("First publication");

        when(orcidClientMock.push(any(), eq("0000-1111-2222-3333"), any()))
            .thenThrow(new OrcidClientException(400, "Bad request"));

        createOrcidQueue(context, owner, entity);

        // First attempt

        TestDSpaceRunnableHandler handler = runBulkSynchronization(false);
        assertThat(handler.getInfoMessages(), hasItem("Found 1 queue records to synchronize with ORCID"));
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        List<OrcidQueue> queueRecords = orcidQueueService.findAll(context);
        assertThat(queueRecords, hasSize(1));
        assertThat(queueRecords, hasItem(matches(owner, entity, "Publication", INSERT, 1)));

        List<OrcidHistory> historyRecords = orcidHistoryService.findAll(context);
        assertThat(historyRecords, hasSize(1));
        assertThat(historyRecords, hasItem(matches(history(owner, entity, 400, INSERT))));

        // Second attempt

        handler = runBulkSynchronization(false);
        assertThat(handler.getInfoMessages(), hasItem("Found 1 queue records to synchronize with ORCID"));
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        queueRecords = orcidQueueService.findAll(context);
        assertThat(queueRecords, hasSize(1));
        assertThat(queueRecords, hasItem(matches(owner, entity, "Publication", INSERT, 2)));

        historyRecords = orcidHistoryService.findAll(context);
        assertThat(historyRecords, hasSize(2));
        assertThat(historyRecords, contains(matches(history(owner, entity, 400, INSERT)),
            matches(history(owner, entity, 400, INSERT))));

        // Third attempt

        handler = runBulkSynchronization(false);
        assertThat(handler.getInfoMessages(), hasItem("Found 0 queue records to synchronize with ORCID"));
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        queueRecords = orcidQueueService.findAll(context);
        assertThat(queueRecords, hasSize(1));
        assertThat(queueRecords, hasItem(matches(owner, entity, "Publication", INSERT, 2)));

        historyRecords = orcidHistoryService.findAll(context);
        assertThat(historyRecords, hasSize(2));
        assertThat(historyRecords, contains(matches(history(owner, entity, 400, INSERT)),
            matches(history(owner, entity, 400, INSERT))));

        // Fourth attempt forcing synchronization

        handler = runBulkSynchronization(true);
        assertThat(handler.getInfoMessages(), hasItem("Found 1 queue records to synchronize with ORCID"));
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        queueRecords = orcidQueueService.findAll(context);
        assertThat(queueRecords, hasSize(1));
        assertThat(queueRecords, hasItem(matches(owner, entity, "Publication", INSERT, 3)));

        historyRecords = orcidHistoryService.findAll(context);
        assertThat(historyRecords, hasSize(3));
        assertThat(historyRecords, contains(matches(history(owner, entity, 400, INSERT)),
            matches(history(owner, entity, 400, INSERT)),
            matches(history(owner, entity, 400, INSERT))));
    }

    private Predicate<OrcidHistory> history(Item owner, Item entity, int status, OrcidOperation operation) {
        return history -> owner.equals(history.getOwner())
            && entity.equals(history.getEntity())
            && history.getStatus().equals(status)
            && operation == history.getOperation();
    }

    private Predicate<OrcidHistory> history(Item owner, int status, OrcidOperation operation) {
        return history -> owner.equals(history.getOwner())
            && history.getStatus().equals(status)
            && operation == history.getOperation();
    }

    private TestDSpaceRunnableHandler runBulkSynchronization(boolean forceSynchronization) throws Exception {
        String[] args = new String[] { "orcid-bulk-push" };
        args = forceSynchronization ? ArrayUtils.add(args, "-f") : args;
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);
        return handler;
    }

    private Item createOwnerItem(String orcid, EPerson owner, OrcidSynchronizationMode mode) throws Exception {

        Item item = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test user")
            .withOrcidIdentifier(orcid)
            .withOrcidSynchronizationMode(mode)
            .withDspaceObjectOwner(owner.getFullName(), owner.getID().toString())
            .build();

        OrcidTokenBuilder.create(context, owner, "9c913f57-961e-48af-9223-cfad6562c925")
            .withProfileItem(item)
            .build();

        return item;
    }

    private Item createPublication(String title) {
        return ItemBuilder.createItem(context, publicationCollection)
            .withTitle(title)
            .withType("Controlled Vocabulary for Resource Type Genres::dataset")
            .build();
    }

    private OrcidResponse createdResponse(String putCode) {
        return new OrcidResponse(201, putCode, null);
    }

    private OrcidResponse updatedResponse(String putCode) {
        return new OrcidResponse(200, putCode, null);
    }

    private OrcidResponse deletedResponse() {
        return new OrcidResponse(204, null, null);
    }
}
