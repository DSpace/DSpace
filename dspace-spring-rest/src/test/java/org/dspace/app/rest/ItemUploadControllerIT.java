/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.MetadataMatcher;
import org.dspace.app.rest.model.BitstreamPropertiesRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class ItemUploadControllerIT extends AbstractEntityIntegrationTest {

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
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();

        String token = getAuthToken(admin.getEmail(), password);
        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());

        BitstreamPropertiesRest bitstreamPropertiesRest = new BitstreamPropertiesRest();
        bitstreamPropertiesRest.setBundleName("ORIGINAL");
        bitstreamPropertiesRest.setName("testing");
        bitstreamPropertiesRest.setSequenceId(123456);

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

        bitstreamPropertiesRest.setMetadata(metadataRest);
        ObjectMapper mapper = new ObjectMapper();

        context.restoreAuthSystemState();
        getClient(token).perform(MockMvcRequestBuilders.fileUpload("/api/core/items/" + item.getID() + "/bitstreams")
                                                       .file(file)
                                                       .param("properties", mapper
                                                           .writeValueAsString(bitstreamPropertiesRest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.name", Matchers.is("testing")))
                        .andExpect(jsonPath("$.bundleName", Matchers.is("ORIGINAL")))
                        .andExpect(jsonPath("$.sequenceId", Matchers.is(Integer.parseInt("123456"))))
                        .andExpect(jsonPath("$", Matchers.allOf(
                            hasJsonPath("$.metadata", Matchers.allOf(
                                MetadataMatcher.matchMetadata("dc.description",
                                                              "description"),
                                MetadataMatcher.matchMetadata("dc.description.tableofcontents",
                                                              "News"),
                                MetadataMatcher.matchMetadata("dc.rights",
                                                              "Custom Copyright Text"),
                                MetadataMatcher.matchMetadata("dc.title",
                                                              "testing")
                            )))));
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
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();

        String token = getAuthToken(admin.getEmail(), password);
        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());
        context.restoreAuthSystemState();
        getClient(token).perform(MockMvcRequestBuilders.fileUpload("/api/core/items/" + item.getID() + "/bitstreams")
                                                       .file(file))
                        .andExpect(status().isOk());
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
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();
        context.setCurrentUser(eperson);
        authorizeService.addPolicy(context, item, Constants.ADD, eperson);
        authorizeService.addPolicy(context, item, Constants.WRITE, eperson);
        String token = getAuthToken(eperson.getEmail(), password);

        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());

        context.restoreAuthSystemState();
        getClient(token).perform(MockMvcRequestBuilders.fileUpload("/api/core/items/" + item.getID() + "/bitstreams")
                                                       .file(file))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", notNullValue()));
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
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();
        context.setCurrentUser(eperson);
        String token = getAuthToken(eperson.getEmail(), password);

        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());

        context.restoreAuthSystemState();
        getClient(token).perform(MockMvcRequestBuilders.fileUpload("/api/core/items/" + item.getID() + "/bitstreams")
                                                       .file(file))
                        .andExpect(status().isForbidden());
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
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();
        context.setCurrentUser(eperson);
        String token = getAuthToken(eperson.getEmail(), password);

        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());

        context.restoreAuthSystemState();
        getClient().perform(MockMvcRequestBuilders.fileUpload("/api/core/items/" + item.getID() + "/bitstreams")
                                                  .file(file))
                   .andExpect(status().isUnauthorized());
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
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();
        context.setCurrentUser(eperson);
        authorizeService.addPolicy(context, item, Constants.ADD, eperson);
        authorizeService.addPolicy(context, item, Constants.WRITE, eperson);
        String token = getAuthToken(eperson.getEmail(), password);

        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());

        BitstreamPropertiesRest bitstreamPropertiesRest = new BitstreamPropertiesRest();
        String originalBundle = "ORIGINAL";
        bitstreamPropertiesRest.setBundleName(originalBundle);

        ObjectMapper mapper = new ObjectMapper();


        context.restoreAuthSystemState();
        getClient(token).perform(MockMvcRequestBuilders.fileUpload("/api/core/items/" + item.getID() + "/bitstreams")
                                                       .file(file)
                                                       .param("properties", mapper
                                                           .writeValueAsString(bitstreamPropertiesRest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.bundleName", Matchers.is(originalBundle)))
                        .andExpect(jsonPath("$.uuid", notNullValue()));
    }

    //TODO This test just fails to run entirely because we cannot pass 'null' to a file upload
    // Should we support this test case differently and if so, how?
    @Test
    @Ignore
    public void uploadBitstreamNoFileUnprocessableEntityException() throws Exception {
        context.turnOffAuthorisationSystem();


        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();

        String token = getAuthToken(admin.getEmail(), password);

        context.restoreAuthSystemState();
        getClient(token).perform(MockMvcRequestBuilders.fileUpload("/api/core/items/" + item.getID() + "/bitstreams"))
                        .andExpect(status().isUnprocessableEntity());
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
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();

        String token = getAuthToken(admin.getEmail(), password);
        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello1.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "hello2.txt", MediaType.TEXT_PLAIN_VALUE,
                                                        input.getBytes());
        context.restoreAuthSystemState();
        getClient(token).perform(MockMvcRequestBuilders.fileUpload("/api/core/items/" + item.getID() + "/bitstreams")
                                                       .file(file).file(file2))
                        .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void uploadBitstreamNoBundleNameInPropertiesUnprocessableException() throws Exception {
        context.turnOffAuthorisationSystem();


        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();
        context.setCurrentUser(eperson);
        authorizeService.addPolicy(context, item, Constants.ADD, eperson);
        authorizeService.addPolicy(context, item, Constants.WRITE, eperson);
        String token = getAuthToken(eperson.getEmail(), password);

        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());

        BitstreamPropertiesRest bitstreamPropertiesRest = new BitstreamPropertiesRest();

        ObjectMapper mapper = new ObjectMapper();


        context.restoreAuthSystemState();
        getClient(token).perform(MockMvcRequestBuilders.fileUpload("/api/core/items/" + item.getID() + "/bitstreams")
                                                       .file(file)
                                                       .param("properties", mapper
                                                           .writeValueAsString(bitstreamPropertiesRest)))
                        .andExpect(status().isUnprocessableEntity());
    }


    //TODO This check on sequence ID doesn't happen
    @Test
    public void uploadBitstreamSameSequenceIdTwiceUnprocessableEntityException() throws Exception {
        context.turnOffAuthorisationSystem();


        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Author1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald")
                               .build();

        String token = getAuthToken(admin.getEmail(), password);
        String input = "Hello, World!";

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE,
                                                       input.getBytes());

        BitstreamPropertiesRest bitstreamPropertiesRest = new BitstreamPropertiesRest();
        bitstreamPropertiesRest.setBundleName("ORIGINAL");
        bitstreamPropertiesRest.setName("testing");
        bitstreamPropertiesRest.setSequenceId(123456);

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

        bitstreamPropertiesRest.setMetadata(metadataRest);
        ObjectMapper mapper = new ObjectMapper();

        context.restoreAuthSystemState();
        getClient(token).perform(MockMvcRequestBuilders.fileUpload("/api/core/items/" + item.getID() + "/bitstreams")
                                                       .file(file)
                                                       .param("properties", mapper
                                                           .writeValueAsString(bitstreamPropertiesRest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.name", Matchers.is("testing")))
                        .andExpect(jsonPath("$.bundleName", Matchers.is("ORIGINAL")))
                        .andExpect(jsonPath("$.sequenceId", Matchers.is(Integer.parseInt("123456"))))
                        .andExpect(jsonPath("$", Matchers.allOf(
                            hasJsonPath("$.metadata", Matchers.allOf(
                                MetadataMatcher.matchMetadata("dc.description",
                                                              "description"),
                                MetadataMatcher.matchMetadata("dc.description.tableofcontents",
                                                              "News"),
                                MetadataMatcher.matchMetadata("dc.rights",
                                                              "Custom Copyright Text"),
                                MetadataMatcher.matchMetadata("dc.title",
                                                              "testing")
                            )))));

        getClient(token).perform(MockMvcRequestBuilders.fileUpload("/api/core/items/" + item.getID() + "/bitstreams")
                                                       .file(file)
                                                       .param("properties", mapper
                                                           .writeValueAsString(bitstreamPropertiesRest)))
                        .andExpect(status().isUnprocessableEntity());

    }

}
