/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.builder.SiteBuilder;
import org.dspace.app.rest.model.ViewEventRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.junit.Test;

public class ViewEventRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAllTestThrowNotImplementedException() throws Exception {

        getClient().perform(get("/api/statistics/viewevents"))
                   .andExpect(status().is(405));
    }
    @Test
    public void findOneTestThrowNotImplementedException() throws Exception {

        getClient().perform(get("/api/statistics/viewevents/" + UUID.randomUUID()))
                   .andExpect(status().is(405));
    }

    @Test
    public void postTestSucces() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();

        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(publicItem1.getID());


        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/viewevents")
                                                .content(mapper.writeValueAsBytes(viewEventRest))
                                                .contentType(contentType))
                   .andExpect(status().isCreated());

    }

    @Test
    public void postTestInvalidUUIDUnprocessableEntityException() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();

        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(UUID.randomUUID());


        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/viewevents")
                                .content(mapper.writeValueAsBytes(viewEventRest))
                                .contentType(contentType))
                   .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void postTestNoUUIDBadRequestException() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();

        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(null);


        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/viewevents")
                                .content(mapper.writeValueAsBytes(viewEventRest))
                                .contentType(contentType))
                   .andExpect(status().isBadRequest());

    }

    @Test
    public void postTestNoTargetTypeBadRequestException() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();

        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType(null);
        viewEventRest.setTargetId(publicItem1.getID());


        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/viewevents")
                                .content(mapper.writeValueAsBytes(viewEventRest))
                                .contentType(contentType))
                   .andExpect(status().isBadRequest());

    }

    @Test
    public void postTestWrongTargetTypeBadRequestException() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();

        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("aezazeaezea");
        viewEventRest.setTargetId(publicItem1.getID());


        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/viewevents")
                                .content(mapper.writeValueAsBytes(viewEventRest))
                                .contentType(contentType))
                   .andExpect(status().isBadRequest());

    }

    @Test
    public void postBitstreamTestSucces() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("descr")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("bitstream");
        viewEventRest.setTargetId(bitstream.getID());


        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/viewevents")
                                .content(mapper.writeValueAsBytes(viewEventRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());
    }

    @Test
    public void postCollectionTestSucces() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("descr")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("collection");
        viewEventRest.setTargetId(col1.getID());


        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/viewevents")
                                .content(mapper.writeValueAsBytes(viewEventRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

    }

    @Test
    public void postCommunityTestSucces() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("descr")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("community");
        viewEventRest.setTargetId(child1.getID());


        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/viewevents")
                                .content(mapper.writeValueAsBytes(viewEventRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

    }

    @Test
    public void postSiteTestSucces() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("descr")
                                        .withMimeType("text/plain")
                                        .build();

        }
        Site site = SiteBuilder.createSite(context).build();

        context.restoreAuthSystemState();

        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("site");
        viewEventRest.setTargetId(site.getID());


        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/viewevents")
                                .content(mapper.writeValueAsBytes(viewEventRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

    }


    @Test
    public void postTestAuthenticatedUserSucces() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();

        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(publicItem1.getID());


        ObjectMapper mapper = new ObjectMapper();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/statistics/viewevents")
                                .content(mapper.writeValueAsBytes(viewEventRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

    }


}
