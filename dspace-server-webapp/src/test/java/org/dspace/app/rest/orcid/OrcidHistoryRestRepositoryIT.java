/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.orcid;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.matcher.LambdaMatcher.matches;
import static org.dspace.builder.OrcidQueueBuilder.createOrcidQueue;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.dspace.app.rest.matcher.OrcidHistoryMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.OrcidHistoryBuilder;
import org.dspace.builder.OrcidQueueBuilder;
import org.dspace.builder.OrcidTokenBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.RelationshipType;
import org.dspace.eperson.EPerson;
import org.dspace.orcid.OrcidHistory;
import org.dspace.orcid.OrcidOperation;
import org.dspace.orcid.OrcidQueue;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.client.OrcidResponse;
import org.dspace.orcid.exception.OrcidClientException;
import org.dspace.orcid.service.impl.OrcidHistoryServiceImpl;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.jaxb.model.v3.release.common.FuzzyDate;
import org.orcid.jaxb.model.v3.release.record.Address;
import org.orcid.jaxb.model.v3.release.record.Funding;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.http.MediaType;

/**
 * Integration test class for the orcid history endpoint.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class OrcidHistoryRestRepositoryIT extends AbstractControllerIntegrationTest {

    private final static String ACCESS_TOKEN = "f5af9f51-07e6-4332-8f1a-c0c11c1e3728";
    private final static String ORCID = "0000-0002-1825-0097";

    private EPerson researcher;

    private Collection persons;

    private Collection publications;

    private Item profile;

    private Item publication;

    @Autowired
    private OrcidHistoryServiceImpl orcidHistoryService;

    private OrcidClient orcidClient;

    private OrcidClient orcidClientMock;

    @Before
    public void setup() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();

        researcher = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Josiah", "Carberry")
            .withEmail("josiah.Carberry@example.com")
            .withPassword(password)
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        persons = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Person")
            .withName("Collection 1")
            .build();

        publications = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 2")
            .build();

        profile = ItemBuilder.createItem(context, persons)
            .withPersonIdentifierFirstName("Josiah")
            .withPersonIdentifierLastName("Carberry")
            .withPersonCountry("IT")
            .withDspaceObjectOwner(researcher.getFullName(), researcher.getID().toString())
            .withOrcidIdentifier(ORCID)
            .build();

        OrcidTokenBuilder.create(context, researcher, ACCESS_TOKEN)
            .withProfileItem(profile)
            .build();

        publication = ItemBuilder.createItem(context, publications)
            .withAuthor("Josiah, Carberry")
            .withTitle("A Methodology for the Emulation of Architecture")
            .withIssueDate("2013-08-03")
            .withType("Controlled Vocabulary for Resource Type Genres::text::book")
            .withDoiIdentifier("10.1000/182")
            .build();

        context.restoreAuthSystemState();

        orcidClientMock = mock(OrcidClient.class);
        orcidClient = orcidHistoryService.getOrcidClient();
        orcidHistoryService.setOrcidClient(orcidClientMock);

    }

    @After
    public void after() {
        orcidHistoryService.setOrcidClient(orcidClient);
    }

    @Test
    public void findAllTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/orcidhistories"))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findOneTest() throws Exception {

        context.turnOffAuthorisationSystem();

        OrcidHistory orcidHistory = OrcidHistoryBuilder.createOrcidHistory(context, profile, publication)
            .withResponseMessage("<xml><work>...</work>")
            .withPutCode("123456")
            .withStatus(201).build();

        context.restoreAuthSystemState();

        String tokenResearcher = getAuthToken(researcher.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenResearcher).perform(get("/api/eperson/orcidhistories/" + orcidHistory.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", is(OrcidHistoryMatcher.matchOrcidHistory(
                orcidHistory, 201, "123456", "<xml><work>...</work>"))))
            .andExpect(jsonPath("$._links.self.href", Matchers
                .containsString("/api/eperson/orcidhistories/" + orcidHistory.getID())));

        getClient(tokenAdmin).perform(get("/api/eperson/orcidhistories/" + orcidHistory.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", is(OrcidHistoryMatcher.matchOrcidHistory(
                orcidHistory, 201, "123456", "<xml><work>...</work>"))))
            .andExpect(jsonPath("$._links.self.href", Matchers
                .containsString("/api/eperson/orcidhistories/" + orcidHistory.getID())));
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        OrcidHistory orcidHistory = OrcidHistoryBuilder.createOrcidHistory(context, profile, publication)
            .withResponseMessage("<xml><work>...</work>")
            .withPutCode("123456")
            .withStatus(201)
            .build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        getClient(tokenEperson).perform(get("/api/eperson/orcidhistories/" + orcidHistory.getID().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findOneisUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        OrcidHistory orcidHistory = OrcidHistoryBuilder.createOrcidHistory(context, profile, publication)
            .withResponseMessage("<xml><work>...</work>")
            .withPutCode("123456")
            .withStatus(201).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/eperson/orcidhistories/" + orcidHistory.getID().toString()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneNotFoundTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/eperson/orcidhistories/" + Integer.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void createForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, profile, publication).build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(post("/api/eperson/orcidhistories")
            .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
            .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
            .andExpect(status().isForbidden());
    }

    @Test
    public void createUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, profile, publication).build();

        context.restoreAuthSystemState();

        getClient().perform(post("/api/eperson/orcidhistories")
            .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
            .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
            .andExpect(status().isUnauthorized());

    }

    @Test
    public void testCreationForPublicationInsert() throws Exception {

        context.turnOffAuthorisationSystem();
        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, profile, publication)
            .withDescription("A Methodology for the Emulation of Architecture")
            .withOperation(OrcidOperation.INSERT)
            .withRecordType("Publication")
            .build();
        context.restoreAuthSystemState();

        when(orcidClientMock.push(eq(ACCESS_TOKEN), eq(ORCID), any())).thenReturn(createdResponse("12345"));

        String authToken = getAuthToken(researcher.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {

            getClient(getAuthToken(researcher.getEmail(), password))
                .perform(post("/api/eperson/orcidhistories")
                    .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                    .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/eperson/orcidhistories/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                    hasJsonPath("$.id", is(idRef.get())),
                    hasJsonPath("$.profileItemId", is(profile.getID().toString())),
                    hasJsonPath("$.entityId", is(publication.getID().toString())),
                    hasJsonPath("$.status", is(201)),
                    hasJsonPath("$.putCode", is("12345")),
                    hasJsonPath("$.type", is("orcidhistory")))));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
        }

        assertThat(context.reloadEntity(orcidQueue), nullValue());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(orcidClientMock).push(eq(ACCESS_TOKEN), eq(ORCID), captor.capture());

        Object sentObject = captor.getValue();
        assertThat(sentObject, instanceOf(Work.class));

        Work work = (Work) sentObject;
        assertThat(work.getPutCode(), nullValue());
        assertThat(work.getWorkTitle().getTitle().getContent(), is("A Methodology for the Emulation of Architecture"));
        assertThat(work.getPublicationDate(), matches(date("2013", "08", "03")));

        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testCreationForPublicationInsertWithOrcidClientException() throws Exception {

        context.turnOffAuthorisationSystem();
        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, profile, publication)
            .withDescription("A Methodology for the Emulation of Architecture")
            .withOperation(OrcidOperation.INSERT)
            .withRecordType("Publication")
            .build();
        context.restoreAuthSystemState();

        when(orcidClientMock.push(eq(ACCESS_TOKEN), eq(ORCID), any()))
            .thenThrow(new OrcidClientException(400, "Invalid resource"));

        String authToken = getAuthToken(researcher.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {

            getClient(getAuthToken(researcher.getEmail(), password))
                .perform(post("/api/eperson/orcidhistories")
                    .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                    .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/eperson/orcidhistories/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                    hasJsonPath("$.id", is(idRef.get())),
                    hasJsonPath("$.profileItemId", is(profile.getID().toString())),
                    hasJsonPath("$.entityId", is(publication.getID().toString())),
                    hasJsonPath("$.responseMessage", is("Invalid resource")),
                    hasJsonPath("$.status", is(400)),
                    hasJsonPath("$.putCode", nullValue()),
                    hasJsonPath("$.type", is("orcidhistory")))));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
        }

        assertThat(context.reloadEntity(orcidQueue), notNullValue());

        verify(orcidClientMock).push(eq(ACCESS_TOKEN), eq(ORCID), any(Work.class));
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testCreationForPublicationInsertWithGenericException() throws Exception {

        context.turnOffAuthorisationSystem();
        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, profile, publication)
            .withDescription("A Methodology for the Emulation of Architecture")
            .withOperation(OrcidOperation.INSERT)
            .withRecordType("Publication")
            .build();
        context.restoreAuthSystemState();

        when(orcidClientMock.push(eq(ACCESS_TOKEN), eq(ORCID), any()))
            .thenThrow(new RuntimeException("GENERIC ERROR"));

        String authToken = getAuthToken(researcher.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {

            getClient(getAuthToken(researcher.getEmail(), password))
                .perform(post("/api/eperson/orcidhistories")
                    .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                    .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/eperson/orcidhistories/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                    hasJsonPath("$.id", is(idRef.get())),
                    hasJsonPath("$.profileItemId", is(profile.getID().toString())),
                    hasJsonPath("$.entityId", is(publication.getID().toString())),
                    hasJsonPath("$.responseMessage", is("GENERIC ERROR")),
                    hasJsonPath("$.status", is(500)),
                    hasJsonPath("$.putCode", nullValue()),
                    hasJsonPath("$.type", is("orcidhistory")))));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
        }

        assertThat(context.reloadEntity(orcidQueue), notNullValue());

        verify(orcidClientMock).push(eq(ACCESS_TOKEN), eq(ORCID), any(Work.class));
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testCreationForPublicationUpdate() throws Exception {

        context.turnOffAuthorisationSystem();
        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, profile, publication)
            .withDescription("A Methodology for the Emulation of Architecture")
            .withOperation(OrcidOperation.UPDATE)
            .withRecordType("Publication")
            .withPutCode("12345")
            .build();
        context.restoreAuthSystemState();

        when(orcidClientMock.update(eq(ACCESS_TOKEN), eq(ORCID), any(), eq("12345")))
            .thenReturn(updatedResponse("12345"));

        String authToken = getAuthToken(researcher.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {

            getClient(getAuthToken(researcher.getEmail(), password))
                .perform(post("/api/eperson/orcidhistories")
                    .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                    .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/eperson/orcidhistories/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                    hasJsonPath("$.id", is(idRef.get())),
                    hasJsonPath("$.profileItemId", is(profile.getID().toString())),
                    hasJsonPath("$.entityId", is(publication.getID().toString())),
                    hasJsonPath("$.status", is(200)),
                    hasJsonPath("$.putCode", is("12345")),
                    hasJsonPath("$.type", is("orcidhistory")))));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
        }

        assertThat(context.reloadEntity(orcidQueue), nullValue());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(orcidClientMock).update(eq(ACCESS_TOKEN), eq(ORCID), captor.capture(), eq("12345"));

        Object sentObject = captor.getValue();
        assertThat(sentObject, instanceOf(Work.class));

        Work work = (Work) sentObject;
        assertThat(work.getPutCode(), is(12345L));
        assertThat(work.getWorkTitle().getTitle().getContent(), is("A Methodology for the Emulation of Architecture"));
        assertThat(work.getPublicationDate(), matches(date("2013", "08", "03")));

        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testCreationForPublicationUpdateWithForceAddition() throws Exception {

        context.turnOffAuthorisationSystem();
        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, profile, publication)
            .withDescription("A Methodology for the Emulation of Architecture")
            .withOperation(OrcidOperation.UPDATE)
            .withRecordType("Publication")
            .withPutCode("12345")
            .build();
        context.restoreAuthSystemState();

        when(orcidClientMock.push(eq(ACCESS_TOKEN), eq(ORCID), any())).thenReturn(createdResponse("12345"));

        String authToken = getAuthToken(researcher.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {

            getClient(getAuthToken(researcher.getEmail(), password))
                .perform(post("/api/eperson/orcidhistories")
                    .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                    .content("/api/eperson/orcidqueues/" + orcidQueue.getID())
                    .param("forceAddition", "true"))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/eperson/orcidhistories/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                    hasJsonPath("$.id", is(idRef.get())),
                    hasJsonPath("$.profileItemId", is(profile.getID().toString())),
                    hasJsonPath("$.entityId", is(publication.getID().toString())),
                    hasJsonPath("$.status", is(201)),
                    hasJsonPath("$.putCode", is("12345")),
                    hasJsonPath("$.type", is("orcidhistory")))));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
        }

        assertThat(context.reloadEntity(orcidQueue), nullValue());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(orcidClientMock).push(eq(ACCESS_TOKEN), eq(ORCID), captor.capture());

        Object sentObject = captor.getValue();
        assertThat(sentObject, instanceOf(Work.class));

        Work work = (Work) sentObject;
        assertThat(work.getPutCode(), nullValue());
        assertThat(work.getWorkTitle().getTitle().getContent(), is("A Methodology for the Emulation of Architecture"));
        assertThat(work.getPublicationDate(), matches(date("2013", "08", "03")));

        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testCreationForPublicationDeletion() throws Exception {

        context.turnOffAuthorisationSystem();
        OrcidQueue orcidQueue = createOrcidQueue(context, profile, "Description", "Publication", "12345").build();
        context.restoreAuthSystemState();

        when(orcidClientMock.deleteByPutCode(ACCESS_TOKEN, ORCID, "12345", "/work")).thenReturn(deletedResponse());

        String authToken = getAuthToken(researcher.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {

            getClient(getAuthToken(researcher.getEmail(), password))
                .perform(post("/api/eperson/orcidhistories")
                    .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                    .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/eperson/orcidhistories/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                    hasJsonPath("$.id", is(idRef.get())),
                    hasJsonPath("$.profileItemId", is(profile.getID().toString())),
                    hasJsonPath("$.entityId", nullValue()),
                    hasJsonPath("$.status", is(204)),
                    hasJsonPath("$.putCode", nullValue()),
                    hasJsonPath("$.type", is("orcidhistory")))));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
        }

        assertThat(context.reloadEntity(orcidQueue), nullValue());

        verify(orcidClientMock).deleteByPutCode(ACCESS_TOKEN, ORCID, "12345", "/work");
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testCreationForPublicationDeletionWithNotFound() throws Exception {

        context.turnOffAuthorisationSystem();
        OrcidQueue orcidQueue = createOrcidQueue(context, profile, "Description", "Publication", "12345").build();
        context.restoreAuthSystemState();

        when(orcidClientMock.deleteByPutCode(ACCESS_TOKEN, ORCID, "12345", "/work")).thenReturn(notFoundResponse());

        String authToken = getAuthToken(researcher.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {

            getClient(getAuthToken(researcher.getEmail(), password))
                .perform(post("/api/eperson/orcidhistories")
                    .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                    .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/eperson/orcidhistories/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                    hasJsonPath("$.id", is(idRef.get())),
                    hasJsonPath("$.profileItemId", is(profile.getID().toString())),
                    hasJsonPath("$.entityId", nullValue()),
                    hasJsonPath("$.status", is(204)),
                    hasJsonPath("$.putCode", nullValue()),
                    hasJsonPath("$.type", is("orcidhistory")))));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
        }

        assertThat(context.reloadEntity(orcidQueue), nullValue());

        verify(orcidClientMock).deleteByPutCode(ACCESS_TOKEN, ORCID, "12345", "/work");
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testCreationForProfileDataInsert() throws Exception {

        context.turnOffAuthorisationSystem();
        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, profile, profile)
            .withDescription("IT")
            .withOperation(OrcidOperation.INSERT)
            .withRecordType("COUNTRY")
            .withMetadata("person.country::IT")
            .build();
        context.restoreAuthSystemState();

        when(orcidClientMock.push(eq(ACCESS_TOKEN), eq(ORCID), any())).thenReturn(createdResponse("12345"));

        String authToken = getAuthToken(researcher.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {

            getClient(getAuthToken(researcher.getEmail(), password))
                .perform(post("/api/eperson/orcidhistories")
                    .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                    .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/eperson/orcidhistories/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                    hasJsonPath("$.id", is(idRef.get())),
                    hasJsonPath("$.profileItemId", is(profile.getID().toString())),
                    hasJsonPath("$.entityId", is(profile.getID().toString())),
                    hasJsonPath("$.status", is(201)),
                    hasJsonPath("$.putCode", is("12345")),
                    hasJsonPath("$.type", is("orcidhistory")))));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
        }

        assertThat(context.reloadEntity(orcidQueue), nullValue());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(orcidClientMock).push(eq(ACCESS_TOKEN), eq(ORCID), captor.capture());

        Object sentObject = captor.getValue();
        assertThat(sentObject, instanceOf(Address.class));

        Address address = (Address) sentObject;
        assertThat(address.getPutCode(), nullValue());
        assertThat(address.getCountry().getValue(), is(Iso3166Country.IT));

        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testCreationForProfileDataDeletion() throws Exception {

        context.turnOffAuthorisationSystem();
        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, profile, profile)
            .withDescription("IT")
            .withOperation(OrcidOperation.DELETE)
            .withRecordType("COUNTRY")
            .withMetadata("person.country::IT")
            .withPutCode("12345")
            .build();
        context.restoreAuthSystemState();

        when(orcidClientMock.deleteByPutCode(ACCESS_TOKEN, ORCID, "12345", "/address"))
            .thenReturn(deletedResponse());

        String authToken = getAuthToken(researcher.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {

            getClient(getAuthToken(researcher.getEmail(), password))
                .perform(post("/api/eperson/orcidhistories")
                    .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                    .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/eperson/orcidhistories/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                    hasJsonPath("$.id", is(idRef.get())),
                    hasJsonPath("$.profileItemId", is(profile.getID().toString())),
                    hasJsonPath("$.entityId", is(profile.getID().toString())),
                    hasJsonPath("$.status", is(204)),
                    hasJsonPath("$.putCode", nullValue()),
                    hasJsonPath("$.type", is("orcidhistory")))));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
        }

        assertThat(context.reloadEntity(orcidQueue), nullValue());

        verify(orcidClientMock).deleteByPutCode(ACCESS_TOKEN, ORCID, "12345", "/address");
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testCreationForFundingInsert() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection fundings = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Project")
            .withName("Collection 3")
            .build();

        Collection orgUnits = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("OrgUnit")
            .withName("Collection 4")
            .build();

        Item orgUnit = ItemBuilder.createItem(context, orgUnits)
            .withOrgUnitLegalName("4Science")
            .withOrgUnitCountry("IT")
            .withOrgUnitLocality("Milan")
            .withOrgUnitCrossrefIdentifier("12345")
            .build();

        Item funding = ItemBuilder.createItem(context, fundings)
            .withTitle("Test funding")
            .withProjectStartDate("2013-08-03")
            .withIdentifier("888-666-444")
            .build();

        EntityType fundingType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        EntityType orgUnitType = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();

        RelationshipType isAuthorOfPublication = createRelationshipTypeBuilder(context, orgUnitType, fundingType,
            "isOrgUnitOfProject", "isProjectOfOrgUnit", 0, null, 0, null).build();

        RelationshipBuilder.createRelationshipBuilder(context, orgUnit, funding, isAuthorOfPublication).build();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, profile, funding)
            .withDescription("Test funding")
            .withOperation(OrcidOperation.INSERT)
            .withRecordType("Project")
            .build();

        context.restoreAuthSystemState();

        when(orcidClientMock.push(eq(ACCESS_TOKEN), eq(ORCID), any())).thenReturn(createdResponse("12345"));

        String authToken = getAuthToken(researcher.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {

            getClient(getAuthToken(researcher.getEmail(), password))
                .perform(post("/api/eperson/orcidhistories")
                    .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                    .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/eperson/orcidhistories/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                    hasJsonPath("$.id", is(idRef.get())),
                    hasJsonPath("$.profileItemId", is(profile.getID().toString())),
                    hasJsonPath("$.entityId", is(funding.getID().toString())),
                    hasJsonPath("$.status", is(201)),
                    hasJsonPath("$.putCode", is("12345")),
                    hasJsonPath("$.type", is("orcidhistory")))));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
        }

        assertThat(context.reloadEntity(orcidQueue), nullValue());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(orcidClientMock).push(eq(ACCESS_TOKEN), eq(ORCID), captor.capture());

        Object sentObject = captor.getValue();
        assertThat(sentObject, instanceOf(Funding.class));

        Funding sentFunding = (Funding) sentObject;
        assertThat(sentFunding.getPutCode(), nullValue());
        assertThat(sentFunding.getTitle().getTitle().getContent(), is("Test funding"));
        assertThat(sentFunding.getStartDate(), matches(date("2013", "08", "03")));

        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testCreationForFundingInsertWithOrcidClientException() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection fundings = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Project")
            .withName("Collection 3")
            .build();

        Collection orgUnits = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("OrgUnit")
            .withName("Collection 4")
            .build();

        Item orgUnit = ItemBuilder.createItem(context, orgUnits)
            .withOrgUnitLegalName("4Science")
            .withOrgUnitCountry("IT")
            .withOrgUnitLocality("Milan")
            .withOrgUnitCrossrefIdentifier("12345")
            .build();

        Item funding = ItemBuilder.createItem(context, fundings)
            .withTitle("Test funding")
            .withProjectStartDate("2013-08-03")
            .withIdentifier("888-666-444")
            .build();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, profile, funding)
            .withDescription("Test funding")
            .withOperation(OrcidOperation.INSERT)
            .withRecordType("Project")
            .build();

        EntityType fundingType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        EntityType orgUnitType = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();

        RelationshipType isAuthorOfPublication = createRelationshipTypeBuilder(context, orgUnitType, fundingType,
            "isOrgUnitOfProject", "isProjectOfOrgUnit", 0, null, 0, null).build();

        RelationshipBuilder.createRelationshipBuilder(context, orgUnit, funding, isAuthorOfPublication).build();

        context.restoreAuthSystemState();

        when(orcidClientMock.push(eq(ACCESS_TOKEN), eq(ORCID), any()))
            .thenThrow(new OrcidClientException(400, "Invalid resource"));

        String authToken = getAuthToken(researcher.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {

            getClient(getAuthToken(researcher.getEmail(), password))
                .perform(post("/api/eperson/orcidhistories")
                    .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                    .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/eperson/orcidhistories/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                    hasJsonPath("$.id", is(idRef.get())),
                    hasJsonPath("$.profileItemId", is(profile.getID().toString())),
                    hasJsonPath("$.entityId", is(funding.getID().toString())),
                    hasJsonPath("$.responseMessage", is("Invalid resource")),
                    hasJsonPath("$.status", is(400)),
                    hasJsonPath("$.putCode", nullValue()),
                    hasJsonPath("$.type", is("orcidhistory")))));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
        }

        assertThat(context.reloadEntity(orcidQueue), notNullValue());

        verify(orcidClientMock).push(eq(ACCESS_TOKEN), eq(ORCID), any(Funding.class));
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testCreationForFundingInsertWithGenericException() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection fundings = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Project")
            .withName("Collection 3")
            .build();

        Collection orgUnits = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("OrgUnit")
            .withName("Collection 4")
            .build();

        Item orgUnit = ItemBuilder.createItem(context, orgUnits)
            .withOrgUnitLegalName("4Science")
            .withOrgUnitCountry("IT")
            .withOrgUnitLocality("Milan")
            .withOrgUnitCrossrefIdentifier("12345")
            .build();

        Item funding = ItemBuilder.createItem(context, fundings)
            .withTitle("Test funding")
            .withProjectStartDate("2013-08-03")
            .withIdentifier("888-666-444")
            .build();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, profile, funding)
            .withDescription("Test funding")
            .withOperation(OrcidOperation.INSERT)
            .withRecordType("Project")
            .build();

        EntityType fundingType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        EntityType orgUnitType = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();

        RelationshipType isAuthorOfPublication = createRelationshipTypeBuilder(context, orgUnitType, fundingType,
            "isOrgUnitOfProject", "isProjectOfOrgUnit", 0, null, 0, null).build();

        RelationshipBuilder.createRelationshipBuilder(context, orgUnit, funding, isAuthorOfPublication).build();

        context.restoreAuthSystemState();

        when(orcidClientMock.push(eq(ACCESS_TOKEN), eq(ORCID), any()))
            .thenThrow(new RuntimeException("GENERIC ERROR"));

        String authToken = getAuthToken(researcher.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {

            getClient(getAuthToken(researcher.getEmail(), password))
                .perform(post("/api/eperson/orcidhistories")
                    .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                    .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/eperson/orcidhistories/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                    hasJsonPath("$.id", is(idRef.get())),
                    hasJsonPath("$.profileItemId", is(profile.getID().toString())),
                    hasJsonPath("$.entityId", is(funding.getID().toString())),
                    hasJsonPath("$.responseMessage", is("GENERIC ERROR")),
                    hasJsonPath("$.status", is(500)),
                    hasJsonPath("$.putCode", nullValue()),
                    hasJsonPath("$.type", is("orcidhistory")))));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
        }

        assertThat(context.reloadEntity(orcidQueue), notNullValue());

        verify(orcidClientMock).push(eq(ACCESS_TOKEN), eq(ORCID), any(Funding.class));
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testCreationForFundingUpdate() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection fundings = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Project")
            .withName("Collection 3")
            .build();

        Collection orgUnits = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("OrgUnit")
            .withName("Collection 4")
            .build();

        Item orgUnit = ItemBuilder.createItem(context, orgUnits)
            .withOrgUnitLegalName("4Science")
            .withOrgUnitCountry("IT")
            .withOrgUnitLocality("Milan")
            .withOrgUnitCrossrefIdentifier("12345")
            .build();

        Item funding = ItemBuilder.createItem(context, fundings)
            .withTitle("Test funding")
            .withProjectStartDate("2013-08-03")
            .withIdentifier("888-666-444")
            .build();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, profile, funding)
            .withDescription("Test funding")
            .withOperation(OrcidOperation.UPDATE)
            .withRecordType("Project")
            .withPutCode("12345")
            .build();

        EntityType fundingType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        EntityType orgUnitType = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();

        RelationshipType isAuthorOfPublication = createRelationshipTypeBuilder(context, orgUnitType, fundingType,
            "isOrgUnitOfProject", "isProjectOfOrgUnit", 0, null, 0, null).build();

        RelationshipBuilder.createRelationshipBuilder(context, orgUnit, funding, isAuthorOfPublication).build();

        context.restoreAuthSystemState();

        when(orcidClientMock.update(eq(ACCESS_TOKEN), eq(ORCID), any(), eq("12345")))
            .thenReturn(updatedResponse("12345"));

        String authToken = getAuthToken(researcher.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {

            getClient(getAuthToken(researcher.getEmail(), password))
                .perform(post("/api/eperson/orcidhistories")
                    .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                    .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/eperson/orcidhistories/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                    hasJsonPath("$.id", is(idRef.get())),
                    hasJsonPath("$.profileItemId", is(profile.getID().toString())),
                    hasJsonPath("$.entityId", is(funding.getID().toString())),
                    hasJsonPath("$.status", is(200)),
                    hasJsonPath("$.putCode", is("12345")),
                    hasJsonPath("$.type", is("orcidhistory")))));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
        }

        assertThat(context.reloadEntity(orcidQueue), nullValue());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(orcidClientMock).update(eq(ACCESS_TOKEN), eq(ORCID), captor.capture(), eq("12345"));

        Object sentObject = captor.getValue();
        assertThat(sentObject, instanceOf(Funding.class));

        Funding sentFunding = (Funding) sentObject;
        assertThat(sentFunding.getPutCode(), is(12345L));
        assertThat(sentFunding.getTitle().getTitle().getContent(), is("Test funding"));
        assertThat(sentFunding.getStartDate(), matches(date("2013", "08", "03")));

        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testCreationForFundingDeletion() throws Exception {

        context.turnOffAuthorisationSystem();
        OrcidQueue orcidQueue = createOrcidQueue(context, profile, "Description", "Project", "12345").build();
        context.restoreAuthSystemState();

        when(orcidClientMock.deleteByPutCode(ACCESS_TOKEN, ORCID, "12345", "/funding")).thenReturn(deletedResponse());

        String authToken = getAuthToken(researcher.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {

            getClient(getAuthToken(researcher.getEmail(), password))
                .perform(post("/api/eperson/orcidhistories")
                    .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                    .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/eperson/orcidhistories/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                    hasJsonPath("$.id", is(idRef.get())),
                    hasJsonPath("$.profileItemId", is(profile.getID().toString())),
                    hasJsonPath("$.entityId", nullValue()),
                    hasJsonPath("$.status", is(204)),
                    hasJsonPath("$.putCode", nullValue()),
                    hasJsonPath("$.type", is("orcidhistory")))));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
        }

        assertThat(context.reloadEntity(orcidQueue), nullValue());

        verify(orcidClientMock).deleteByPutCode(ACCESS_TOKEN, ORCID, "12345", "/funding");
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testWithInvalidFunding() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection fundings = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Project")
            .withName("Collection 3")
            .build();

        Item funding = ItemBuilder.createItem(context, fundings)
            .withTitle("Test funding")
            .withProjectStartDate("2013-08-03")
            .build();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, profile, funding)
            .withDescription("Test funding")
            .withOperation(OrcidOperation.INSERT)
            .withRecordType("Project")
            .build();

        context.restoreAuthSystemState();

        when(orcidClientMock.push(eq(ACCESS_TOKEN), eq(ORCID), any())).thenReturn(createdResponse("12345"));

        getClient(getAuthToken(researcher.getEmail(), password))
            .perform(post("/api/eperson/orcidhistories")
                .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("/api/eperson/orcidqueues/" + orcidQueue.getID()))
            .andExpect(status().isUnprocessableEntity());

        assertThat(context.reloadEntity(orcidQueue), notNullValue());
        verifyNoMoreInteractions(orcidClientMock);

    }

    private Predicate<? super FuzzyDate> date(String year, String month, String days) {
        return date -> date != null
            && year.equals(date.getYear().getValue())
            && month.equals(date.getMonth().getValue())
            && days.equals(date.getDay().getValue());
    }

    private OrcidResponse createdResponse(String putCode) {
        return new OrcidResponse(201, putCode, null);
    }

    private OrcidResponse notFoundResponse() {
        return new OrcidResponse(404, null, null);
    }

    private OrcidResponse updatedResponse(String putCode) {
        return new OrcidResponse(200, putCode, null);
    }

    private OrcidResponse deletedResponse() {
        return new OrcidResponse(204, null, null);
    }

}
