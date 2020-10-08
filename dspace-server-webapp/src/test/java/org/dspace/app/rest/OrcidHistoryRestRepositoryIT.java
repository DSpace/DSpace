/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;

import com.amazonaws.util.StringInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.dspace.app.orcid.OrcidHistory;
import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.service.impl.OrcidHistoryServiceImpl;
import org.dspace.app.rest.matcher.HttpEntityRequestMatcher;
import org.dspace.app.rest.matcher.OrcidHistoryMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.OrcidHistoryBuilder;
import org.dspace.builder.OrcidQueueBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.http.MediaType;

/**
 * Integration test class for the orcid history endpoint
 *
 * @author Mykhaylo Boychuk - 4Science
 */
public class OrcidHistoryRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrcidHistoryServiceImpl orcidHistoryService;

    @Test
    public void findAllTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/cris/orcidhistories"))
                            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson researcher = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Josiah", "Carberry")
                                           .withEmail("josiah.Carberry@example.com")
                                           .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 2").build();

        Item itemPerson = ItemBuilder.createItem(context, col1)
                                     .withPersonIdentifierFirstName("Josiah")
                                     .withPersonIdentifierLastName("Carberry")
                                     .withCrisOwner(researcher.getFullName(), researcher.getID().toString())
                                     .build();

        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03").build();

        OrcidHistory orcidHistory = OrcidHistoryBuilder.createOrcidHistory(context, itemPerson, itemPublication)
                                                       .withResponseMessage("<xml><work>...</work>")
                                                       .withPutCode("123456")
                                                       .withStatus(201).build();

        context.restoreAuthSystemState();

        String tokenResearcher = getAuthToken(researcher.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenResearcher).perform(get("/api/cris/orcidhistories/" + orcidHistory.getID().toString()))
                                  .andExpect(status().isOk())
                                  .andExpect(jsonPath("$", is(OrcidHistoryMatcher.matchOrcidHistory(
                                             orcidHistory, 201, "123456", "<xml><work>...</work>"))))
                                  .andExpect(jsonPath("$._links.self.href", Matchers
                                             .containsString("/api/cris/orcidhistories/" + orcidHistory.getID())));

        getClient(tokenAdmin).perform(get("/api/cris/orcidhistories/" + orcidHistory.getID().toString()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", is(OrcidHistoryMatcher.matchOrcidHistory(
                                        orcidHistory, 201, "123456", "<xml><work>...</work>"))))
                             .andExpect(jsonPath("$._links.self.href", Matchers
                                        .containsString("/api/cris/orcidhistories/" + orcidHistory.getID())));
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson researcher = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Josiah", "Carberry")
                                           .withEmail("josiah.Carberry@example.com")
                                           .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 2").build();

        Item itemPerson = ItemBuilder.createItem(context, col1)
                                     .withPersonIdentifierFirstName("Josiah")
                                     .withPersonIdentifierLastName("Carberry")
                                     .withCrisOwner(researcher.getFullName(), researcher.getID().toString())
                                     .build();

        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03").build();

        OrcidHistory orcidHistory = OrcidHistoryBuilder.createOrcidHistory(context, itemPerson, itemPublication)
                                                       .withResponseMessage("<xml><work>...</work>")
                                                       .withPutCode("123456")
                                                       .withStatus(201).build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        getClient(tokenEperson).perform(get("/api/cris/orcidhistories/" + orcidHistory.getID().toString()))
                               .andExpect(status().isForbidden());
    }

    @Test
    public void findOneisUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson researcher = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Josiah", "Carberry")
                                           .withEmail("josiah.Carberry@example.com")
                                           .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 2").build();

        Item itemPerson = ItemBuilder.createItem(context, col1)
                                     .withPersonIdentifierFirstName("Josiah")
                                     .withPersonIdentifierLastName("Carberry")
                                     .withCrisOwner(researcher.getFullName(), researcher.getID().toString())
                                     .build();

        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03").build();

        OrcidHistory orcidHistory = OrcidHistoryBuilder.createOrcidHistory(context, itemPerson, itemPublication)
                                                       .withResponseMessage("<xml><work>...</work>")
                                                       .withPutCode("123456")
                                                       .withStatus(201).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/cris/orcidhistories/" + orcidHistory.getID().toString()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneNotFoundTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/cris/orcidhistories/" + Integer.MAX_VALUE))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void createTest() throws Exception {
        context.turnOffAuthorisationSystem();

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {
            EPerson researcher = EPersonBuilder.createEPerson(context)
                                               .withNameInMetadata("Josiah", "Carberry")
                                               .withEmail("josiah.Carberry@example.com")
                                               .withPassword(password).build();

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Person")
                                               .withName("Collection 1").build();

            Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Publication")
                                               .withName("Collection 2").build();

            Item itemPerson = ItemBuilder.createItem(context, col1)
                                         .withPersonIdentifierFirstName("Josiah")
                                         .withPersonIdentifierLastName("Carberry")
                                         .withCrisOwner(researcher.getFullName(),
                                                        researcher.getID().toString()).build();

            itemService.addMetadata(context, itemPerson, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");
            itemService.addMetadata(context, itemPerson, "cris", "orcid", "access-token", "en",
                                    "f5af9f51-07e6-4332-8f1a-c0c11c1e3728");

            Item itemPublication = ItemBuilder.createItem(context, col2)
                                              .withAuthor("Josiah, Carberry")
                                              .withTitle("A Methodology for the Emulation of Architecture")
                                              .withIssueDate("2013-08-03").build();

            OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson, itemPublication).build();

            context.restoreAuthSystemState();

            String authToken = getAuthToken(researcher.getEmail(), password);
            getClient(authToken).perform(post("/api/cris/orcidhistories")
                                .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                                .content("/api/cris/orcidqueues/" + orcidQueue.getID()))
                                .andExpect(status().isCreated())
                                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/cris/orcidhistories/" + idRef.get()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", Matchers.allOf(
                                        hasJsonPath("$.id", is(idRef.get())),
                                        hasJsonPath("$.ownerId", is(itemPerson.getID().toString())),
                                        hasJsonPath("$.entityId", is(itemPublication.getID().toString())),
                                        hasJsonPath("$.status", is(401)),
                                        hasJsonPath("$.putCode", nullValue()),
                                        hasJsonPath("$.type", is("orcidhistory")))));
        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
        }
    }

    @Test
    public void createUsingMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        HttpClient originalHttpClient = orcidHistoryService.getHttpClient();
        HttpClient httpClient = Mockito.mock(HttpClient.class);

        orcidHistoryService.setHttpClient(httpClient);
        BasicHttpResponse basicHttpResponse = new BasicHttpResponse(new ProtocolVersion("http", 1, 1), 200, "OK");
        basicHttpResponse.setHeader("Location", "ABCDE");
        basicHttpResponse.setEntity(new BasicHttpEntity());
        InputStream is = new StringInputStream("<xml><work>...</work>");
        BasicHttpEntity bhe = new BasicHttpEntity();
        basicHttpResponse.setEntity(bhe);
        bhe.setChunked(true);
        bhe.setContent(is);

        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(basicHttpResponse);
        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(basicHttpResponse);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {
            EPerson researcher = EPersonBuilder.createEPerson(context)
                                               .withNameInMetadata("Josiah", "Carberry")
                                               .withEmail("josiah.Carberry@example.com")
                                               .withPassword(password).build();

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Person")
                                               .withName("Collection 1").build();

            Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Publication")
                                               .withName("Collection 2").build();

            Item itemPerson = ItemBuilder.createItem(context, col1)
                                         .withPersonIdentifierFirstName("Josiah")
                                         .withPersonIdentifierLastName("Carberry")
                                         .withCrisOwner(researcher.getFullName(),
                                                        researcher.getID().toString()).build();

            itemService.addMetadata(context, itemPerson, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");
            itemService.addMetadata(context, itemPerson, "cris", "orcid", "access-token", "en",
                                    "f5af9f51-07e6-4332-8f1a-c0c11c1e3728");

            Item itemPublication = ItemBuilder.createItem(context, col2)
                                              .withAuthor("Josiah, Carberry")
                                              .withTitle("A Methodology for the Emulation of Architecture")
                                              .withIssueDate("2013-08-03").build();

            OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson, itemPublication).build();

            OrcidHistory orcidHistory = OrcidHistoryBuilder.createOrcidHistory(context, itemPerson, itemPublication)
                                                           .withResponseMessage("<xml><work>...</work>")
                                                           .withPutCode("123456")
                                                           .withStatus(201).build();

            context.restoreAuthSystemState();

            String authToken = getAuthToken(researcher.getEmail(), password);
            getClient(authToken).perform(post("/api/cris/orcidhistories")
                                .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                                .param("forceAddition", "true")
                                .content("/api/cris/orcidqueues/" + orcidQueue.getID()))
                                .andExpect(status().isCreated())
                                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/cris/orcidhistories/" + idRef.get()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", Matchers.allOf(
                                           hasJsonPath("$.id", is(idRef.get())),
                                           hasJsonPath("$.ownerId", is(itemPerson.getID().toString())),
                                           hasJsonPath("$.entityId", is(itemPublication.getID().toString())),
                                           hasJsonPath("$.status", is(200)),
                                           hasJsonPath("$.putCode", is("ABCDE")),
                                           hasJsonPath("$.responseMessage", is("<xml><work>...</work>")),
                                           hasJsonPath("$.type", is("orcidhistory")))))
                                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/cris/orcidhistories/" + orcidHistory.getId()))
                                .andExpect(status().isNotFound());
        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
            orcidHistoryService.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void createForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson researcher = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Josiah", "Carberry")
                                           .withEmail("josiah.Carberry@example.com")
                                           .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 2").build();

        Item itemPerson = ItemBuilder.createItem(context, col1)
                                     .withPersonIdentifierFirstName("Josiah")
                                     .withPersonIdentifierLastName("Carberry")
                                     .withCrisOwner(researcher.getFullName(), researcher.getID().toString()).build();

        itemService.addMetadata(context, itemPerson, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");
        itemService.addMetadata(context, itemPerson, "cris", "orcid", "access-token", "en",
                                                     "f5af9f51-07e6-4332-8f1a-c0c11c1e3728");

        Item itemPublication = ItemBuilder.createItem(context, col2).withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03").build();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson, itemPublication).build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(post("/api/cris/orcidhistories")
                               .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                               .content("/api/cris/orcidqueues/" + orcidQueue.getID()))
                               .andExpect(status().isForbidden());
    }

    @Test
    public void createUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson researcher = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Josiah", "Carberry")
                                           .withEmail("josiah.Carberry@example.com")
                                           .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 2").build();

        Item itemPerson = ItemBuilder.createItem(context, col1)
                                     .withPersonIdentifierFirstName("Josiah")
                                     .withPersonIdentifierLastName("Carberry")
                                     .withCrisOwner(researcher.getFullName(), researcher.getID().toString()).build();

        itemService.addMetadata(context, itemPerson, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");
        itemService.addMetadata(context, itemPerson, "cris", "orcid", "access-token", "en",
                                                     "f5af9f51-07e6-4332-8f1a-c0c11c1e3728");

        Item itemPublication = ItemBuilder.createItem(context, col2).withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03").build();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson, itemPublication).build();

        context.restoreAuthSystemState();

        getClient().perform(post("/api/cris/orcidhistories")
                   .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                   .content("/api/cris/orcidqueues/" + orcidQueue.getID()))
                   .andExpect(status().isUnauthorized());

    }

    @Test
    public void sendToOrcidWithoutPutCodeMatchWorkXmlFileMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        HttpClient originalHttpClient = orcidHistoryService.getHttpClient();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try (FileInputStream fis = new FileInputStream(testProps.get("test.orcidWorkXML").toString())) {

            String xmlWorkExample = IOUtils.toString(fis, Charset.defaultCharset());
            orcidHistoryService.setHttpClient(httpClient);

            BasicHttpResponse basicHttpResponse = new BasicHttpResponse(new ProtocolVersion("http", 1, 1), 200, "OK");
            basicHttpResponse.setHeader("Location", "ABCDE");
            basicHttpResponse.setEntity(new BasicHttpEntity());
            InputStream inputStream = new StringInputStream(xmlWorkExample);
            BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
            basicHttpResponse.setEntity(basicHttpEntity);
            basicHttpEntity.setChunked(true);
            basicHttpEntity.setContent(inputStream);

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(basicHttpResponse);

            EPerson researcher = EPersonBuilder.createEPerson(context)
                                               .withNameInMetadata("Josiah", "Carberry")
                                               .withEmail("josiah.Carberry@example.com")
                                               .withPassword(password).build();

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Person")
                                               .withName("Collection 1").build();

            Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Publication")
                                               .withName("Collection 2").build();

            Item itemPerson = ItemBuilder.createItem(context, col1)
                                         .withPersonIdentifierFirstName("Josiah")
                                         .withPersonIdentifierLastName("Carberry")
                                         .withCrisOwner(researcher.getFullName(),
                                                        researcher.getID().toString()).build();

            itemService.addMetadata(context, itemPerson, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");
            itemService.addMetadata(context, itemPerson, "cris", "orcid", "access-token", "en",
                                    "f5af9f51-07e6-4332-8f1a-c0c11c1e3728");

            Item itemPublication = ItemBuilder.createItem(context, col2)
                                              .withAuthor("Josiah, Carberry")
                                              .withTitle("A Methodology for the Emulation of Architecture")
                                              .withHandle("123456789/yyy")
                                              .withDoiIdentifier("10.1000/182")
                                              .withIssueDate("2013-08-03").build();

            OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson, itemPublication).build();

            context.restoreAuthSystemState();

            String authToken = getAuthToken(researcher.getEmail(), password);
            getClient(authToken).perform(post("/api/cris/orcidhistories")
                                .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                                .content("/api/cris/orcidqueues/" + orcidQueue.getID()))
                                .andExpect(status().isCreated())
                                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            verify(httpClient).execute(ArgumentMatchers.argThat(new HttpEntityRequestMatcher(xmlWorkExample, "POST" )));

            getClient(authToken).perform(get("/api/cris/orcidhistories/" + idRef.get()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", Matchers.allOf(
                                        hasJsonPath("$.id", is(idRef.get())),
                                        hasJsonPath("$.ownerId", is(itemPerson.getID().toString())),
                                        hasJsonPath("$.entityId", is(itemPublication.getID().toString())),
                                        hasJsonPath("$.status", is(200)),
                                        hasJsonPath("$.putCode", is("ABCDE")),
                                        hasJsonPath("$.responseMessage", is(xmlWorkExample)),
                                        hasJsonPath("$.type", is("orcidhistory")))))
                                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
            orcidHistoryService.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void sendToOrcidWithPutCodeMatchWorkXmlFileMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        HttpClient originalHttpClient = orcidHistoryService.getHttpClient();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try (FileInputStream fis = new FileInputStream(testProps.get("test.orcidWorkWithPutCodeXML").toString())) {

            String xmlWorkWithPutCodeExample = IOUtils.toString(fis, Charset.defaultCharset());
            orcidHistoryService.setHttpClient(httpClient);

            BasicHttpResponse basicHttpResponse = new BasicHttpResponse(new ProtocolVersion("http", 1, 1), 200, "OK");
            basicHttpResponse.setHeader("Location", "ABCDE");
            basicHttpResponse.setEntity(new BasicHttpEntity());
            InputStream inputStream = new StringInputStream(xmlWorkWithPutCodeExample);
            BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
            basicHttpResponse.setEntity(basicHttpEntity);
            basicHttpEntity.setChunked(true);
            basicHttpEntity.setContent(inputStream);

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(basicHttpResponse);

            EPerson researcher = EPersonBuilder.createEPerson(context)
                                               .withNameInMetadata("Josiah", "Carberry")
                                               .withEmail("josiah.Carberry@example.com")
                                               .withPassword(password).build();

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Person")
                                               .withName("Collection 1").build();

            Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Publication")
                                               .withName("Collection 2").build();

            Item itemPerson = ItemBuilder.createItem(context, col1)
                                         .withPersonIdentifierFirstName("Josiah")
                                         .withPersonIdentifierLastName("Carberry")
                                         .withCrisOwner(researcher.getFullName(),
                                                        researcher.getID().toString()).build();

            itemService.addMetadata(context, itemPerson, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");
            itemService.addMetadata(context, itemPerson, "cris", "orcid", "access-token", "en",
                                    "f5af9f51-07e6-4332-8f1a-c0c11c1e3728");

            Item itemPublication = ItemBuilder.createItem(context, col2)
                                              .withAuthor("Josiah, Carberry")
                                              .withTitle("A Methodology for the Emulation of Architecture")
                                              .withHandle("123456789/xxx")
                                              .withIssueDate("2013-08-03").build();

            OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson, itemPublication).build();

            OrcidHistoryBuilder.createOrcidHistory(context, itemPerson, itemPublication)
                               .withResponseMessage("<xml><work>...</work>")
                               .withPutCode("12345")
                               .withStatus(201).build();

            context.restoreAuthSystemState();

            String authToken = getAuthToken(researcher.getEmail(), password);
            getClient(authToken).perform(post("/api/cris/orcidhistories")
                                .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                                .content("/api/cris/orcidqueues/" + orcidQueue.getID()))
                                .andExpect(status().isCreated())
                                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            verify(httpClient).execute(ArgumentMatchers
                              .argThat(new HttpEntityRequestMatcher(xmlWorkWithPutCodeExample, "PUT" )));

            getClient(authToken).perform(get("/api/cris/orcidhistories/" + idRef.get()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", Matchers.allOf(
                                        hasJsonPath("$.id", is(idRef.get())),
                                        hasJsonPath("$.ownerId", is(itemPerson.getID().toString())),
                                        hasJsonPath("$.entityId", is(itemPublication.getID().toString())),
                                        hasJsonPath("$.status", is(200)),
                                        hasJsonPath("$.putCode", is("ABCDE")),
                                        hasJsonPath("$.responseMessage", is(xmlWorkWithPutCodeExample)),
                                        hasJsonPath("$.type", is("orcidhistory")))))
                                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        } finally {
            OrcidHistoryBuilder.deleteOrcidHistory(idRef.get());
            orcidHistoryService.setHttpClient(originalHttpClient);
        }
    }
}
