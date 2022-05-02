/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.matcher.BitstreamMatcher;
import org.dspace.app.rest.matcher.MetadataMatcher;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class BundleUploadBitstreamControllerIT extends AbstractEntityIntegrationTest {

    @Autowired
    private AuthorizeService authorizeService;

    @Test
    public void uploadBitstreamAllPossibleFieldsProperties() throws Exception {
        context.turnOffAuthorisationSystem();


        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();

        Bundle bundle = BundleBuilder.createBundle(context, item)
                                     .withName("TESTINGBUNDLE")
                                     .build();

        String token = getAuthToken(admin.getEmail(), password);
        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());

        BitstreamRest bitstreamRest = new BitstreamRest();
        bitstreamRest.setName("testing");

        MetadataRest metadataRest = new MetadataRest();

        MetadataValueRest description = new MetadataValueRest();
        description.setValue("description");
        metadataRest.put("dc.description", description);

        MetadataValueRest contents = new MetadataValueRest();
        contents.setValue("News");
        metadataRest.put("dc.description.tableofcontents", contents);

        MetadataValueRest copyright = new MetadataValueRest();
        copyright.setValue("Custom Copyright Text");
        metadataRest.put("dc.rights", copyright);

        MetadataValueRest title = new MetadataValueRest();
        title.setValue("Title Text");
        metadataRest.put("dc.title", title);

        bitstreamRest.setMetadata(metadataRest);
        ObjectMapper mapper = new ObjectMapper();

        context.restoreAuthSystemState();
        MvcResult mvcResult = getClient(token).perform(
                MockMvcRequestBuilders.multipart("/api/core/bundles/" + bundle.getID() + "/bitstreams")
                                      .file(file)
                                      .param("properties", mapper
                                              .writeValueAsString(bitstreamRest)))
                                              .andExpect(status().isCreated())
                                              .andExpect(jsonPath("$.name", is("testing")))
                                              .andExpect(jsonPath("$.bundleName", is("TESTINGBUNDLE")))
                                              .andExpect(jsonPath("$", Matchers.allOf(
                                                      hasJsonPath("$.metadata", Matchers.allOf(
                                                              MetadataMatcher.matchMetadata("dc.description",
                                                                                            "description"),
                                                              MetadataMatcher
                                                                      .matchMetadata("dc.description.tableofcontents",
                                                                                     "News"),
                                                              MetadataMatcher.matchMetadata("dc.rights",
                                                                                            "Custom Copyright Text"),
                                                              MetadataMatcher.matchMetadata("dc.title",
                                                                                            "testing")
                                                      ))))).andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String bitstreamId = String.valueOf(map.get("id"));


        getClient(token).perform(get("/api/core/bundles/" + bundle.getID() + "/bitstreams")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("_embedded.bitstreams", Matchers.hasItem(
                                BitstreamMatcher.matchBitstreamEntry(UUID.fromString(bitstreamId), file.getSize(),
                                                                     bitstreamRest.getName(), "description"))));

    }

    @Test
    public void uploadBitstreamNoProperties() throws Exception {
        context.turnOffAuthorisationSystem();


        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();

        Bundle bundle = BundleBuilder.createBundle(context, item)
                                     .withName("TESTINGBUNDLE")
                                     .build();

        String token = getAuthToken(admin.getEmail(), password);
        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());
        context.restoreAuthSystemState();
        MvcResult mvcResult = getClient(token)
                .perform(MockMvcRequestBuilders.multipart("/api/core/bundles/" + bundle.getID() + "/bitstreams")
                                               .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bundleName", is("TESTINGBUNDLE")))
                .andExpect(jsonPath("$.name", is("hello.txt")))
                .andExpect(jsonPath("$.sequenceId", is(1)))
                .andReturn();

        ObjectMapper mapper = new ObjectMapper();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String bitstreamId = String.valueOf(map.get("id"));

        getClient(token).perform(get("/api/core/bundles/" + bundle.getID() + "/bitstreams")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("_embedded.bitstreams", Matchers.hasItem(
                                BitstreamMatcher.matchBitstreamEntry(UUID.fromString(bitstreamId), file.getSize()))));
    }

    @Test
    public void uploadBitstreamNoPropertiesUserWithItemAddAndWriteRights() throws Exception {
        context.turnOffAuthorisationSystem();


        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();
        Bundle bundle = BundleBuilder.createBundle(context, item)
                                     .withName("TESTINGBUNDLE")
                                     .build();
        context.setCurrentUser(eperson);
        authorizeService.addPolicy(context, bundle, Constants.ADD, eperson);
        authorizeService.addPolicy(context, bundle, Constants.WRITE, eperson);
        authorizeService.addPolicy(context, item, Constants.ADD, eperson);
        authorizeService.addPolicy(context, item, Constants.WRITE, eperson);
        String token = getAuthToken(eperson.getEmail(), password);

        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());

        context.restoreAuthSystemState();
        MvcResult mvcResult = getClient(token)
                .perform(MockMvcRequestBuilders.multipart("/api/core/bundles/" + bundle.getID() + "/bitstreams")
                                               .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid", notNullValue())).andReturn();

        ObjectMapper mapper = new ObjectMapper();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String bitstreamId = String.valueOf(map.get("id"));

        getClient(token).perform(get("/api/core/bundles/" + bundle.getID() + "/bitstreams")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("_embedded.bitstreams", Matchers.hasItem(
                                BitstreamMatcher.matchBitstreamEntry(UUID.fromString(bitstreamId), file.getSize()))));
    }

    @Test
    public void uploadBitstreamNoRights() throws Exception {
        context.turnOffAuthorisationSystem();


        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();
        Bundle bundle = BundleBuilder.createBundle(context, item)
                                     .withName("TESTINGBUNDLE")
                                     .build();

        context.setCurrentUser(eperson);
        String token = getAuthToken(eperson.getEmail(), password);

        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());

        context.restoreAuthSystemState();
        getClient(token).perform(MockMvcRequestBuilders
                                         .multipart("/api/core/bundles/" + bundle.getID() + "/bitstreams")
                                         .file(file))
                        .andExpect(status().isForbidden());

        getClient(token).perform(get("/api/core/bundles/" + bundle.getID() + "/bitstreams")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("_embedded.bitstreams").isEmpty());
    }

    @Test
    public void uploadBitstreamAnonymous() throws Exception {
        context.turnOffAuthorisationSystem();


        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();

        Bundle bundle = BundleBuilder.createBundle(context, item)
                                     .withName("TESTINGBUNDLE")
                                     .build();

        context.setCurrentUser(eperson);
        String token = getAuthToken(eperson.getEmail(), password);

        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());

        context.restoreAuthSystemState();
        getClient().perform(MockMvcRequestBuilders.multipart("/api/core/bundles/" + bundle.getID() + "/bitstreams")
                                                  .file(file))
                   .andExpect(status().isUnauthorized());

        getClient(token).perform(get("/api/core/bundles/" + bundle.getID() + "/bitstreams")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("_embedded.bitstreams").isEmpty());
    }

    @Test
    public void uploadBitstreamMinimalProperties() throws Exception {
        context.turnOffAuthorisationSystem();


        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();

        Bundle bundle = BundleBuilder.createBundle(context, item)
                                     .withName("TESTINGBUNDLE")
                                     .build();

        context.setCurrentUser(eperson);
        authorizeService.addPolicy(context, bundle, Constants.ADD, eperson);
        authorizeService.addPolicy(context, bundle, Constants.WRITE, eperson);
        authorizeService.addPolicy(context, item, Constants.ADD, eperson);
        authorizeService.addPolicy(context, item, Constants.WRITE, eperson);
        String token = getAuthToken(eperson.getEmail(), password);

        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());

        BitstreamRest bitstreamRest = new BitstreamRest();

        ObjectMapper mapper = new ObjectMapper();


        context.restoreAuthSystemState();
        MvcResult mvcResult = getClient(token)
                .perform(MockMvcRequestBuilders.multipart("/api/core/bundles/" + bundle.getID() + "/bitstreams")
                                               .file(file)
                                               .param("properties", mapper
                                                       .writeValueAsString(bitstreamRest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bundleName", is("TESTINGBUNDLE")))
                .andExpect(jsonPath("$.uuid", notNullValue())).andReturn();


        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String bitstreamId = String.valueOf(map.get("id"));

        getClient(token).perform(get("/api/core/bundles/" + bundle.getID() + "/bitstreams")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("_embedded.bitstreams", Matchers.hasItem(
                                BitstreamMatcher.matchBitstreamEntry(UUID.fromString(bitstreamId), file.getSize()))));
    }

    // TODO This test doesn't work either as it seems that only the first file is ever transfered into the request
    // Thus we cannot check for this and we have no way of knowing how many files we gave to the request
    @Test
    @Ignore
    public void uploadBitstreamMultipleFiles() throws Exception {
        context.turnOffAuthorisationSystem();


        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();

        Bundle bundle = BundleBuilder.createBundle(context, item)
                                     .withName("TESTINGBUNDLE")
                                     .build();

        String token = getAuthToken(admin.getEmail(), password);
        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello1.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "hello2.txt", MediaType.TEXT_PLAIN_VALUE,
                                                        input.getBytes());
        context.restoreAuthSystemState();
        getClient(token)
                .perform(MockMvcRequestBuilders.multipart("/api/core/bundles/" + bundle.getID() + "/bitstreams")
                                               .file(file).file(file2))
                .andExpect(status().isUnprocessableEntity());
    }

}
