/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.BundleBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.builder.ResourcePolicyBuilder;
import org.dspace.app.rest.matcher.BitstreamMatcher;
import org.dspace.app.rest.matcher.BundleMatcher;
import org.dspace.app.rest.matcher.MetadataMatcher;
import org.dspace.app.rest.matcher.ProjectionsMatcher;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.MoveOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

public class BundleRestRepositoryIT extends AbstractControllerIntegrationTest {

    private Collection collection;
    private Item item;
    private Bundle bundle1;
    private Bundle bundle2;
    private Bitstream bitstream1;
    private Bitstream bitstream2;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        item = ItemBuilder.createItem(context, collection)
                          .withTitle("Public item 1")
                          .withIssueDate("2017-10-17")
                          .withAuthor("Smith, Donald").withAuthor("Doe, John")
                          .withSubject("ExtraEntry")
                          .build();


        context.restoreAuthSystemState();

    }

    @Test
    public void GetSingleBundle() throws Exception {
        context.turnOffAuthorisationSystem();
        ProjectionsMatcher projectionsMatcher = new ProjectionsMatcher();

        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, item, is)
                                         .withName("Bitstream")
                                         .withMimeType("text/plain")
                                         .build();
        }

        bundle1 = BundleBuilder.createBundle(context, item)
                               .withName("testname")
                               .withBitstream(bitstream1)
                               .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/bundles/" + bundle1.getID())
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", projectionsMatcher.matchBundleEmbeds()))
                   .andExpect(jsonPath("$", projectionsMatcher.matchBundleLinks()))
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", BundleMatcher.matchBundle(bundle1.getName(),
                                                                      bundle1.getID(),
                                                                      bundle1.getHandle(),
                                                                      bundle1.getType(),
                                                                      bundle1.getBitstreams())
                   ))
                   .andExpect(jsonPath("$._embedded.bitstreams._embedded.bitstreams", containsInAnyOrder(
                           BitstreamMatcher.matchBitstreamEntry(bitstream1.getID(), bitstream1.getSizeBytes())))
                   )
        ;

        getClient().perform(get("/api/core/bundles/" + bundle1.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", projectionsMatcher.matchNoEmbeds()))
        ;

    }


    @Test
    public void getItemBundles() throws Exception {

        context.turnOffAuthorisationSystem();

        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, item, is)
                                         .withName("Bitstream")
                                         .withMimeType("text/plain")
                                         .build();
        }

        bundle1 = BundleBuilder.createBundle(context, item)
                               .withName("testname")
                               .withBitstream(bitstream1)
                               .build();
        bundle2 = BundleBuilder.createBundle(context, item).withName("test2").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/" + item.getID() + "/bundles")
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.bundles", Matchers.hasItems(
                           BundleMatcher.matchBundle(bundle1.getName(),
                                                     bundle1.getID(),
                                                     bundle1.getHandle(),
                                                     bundle1.getType(),
                                                     bundle1.getBitstreams())
                           ,
                           BundleMatcher.matchBundle(bundle2.getName(),
                                                     bundle2.getID(),
                                                     bundle2.getHandle(),
                                                     bundle2.getType(),
                                                     bundle2.getBitstreams())

                   )))
                   .andExpect(jsonPath("$._links.self.href",
                           Matchers.containsString("/api/core/items/" + item.getID() + "/bundles")))
        ;
    }

    @Test
    public void createBundleWithoutMetadata() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        BundleRest bundleRest = new BundleRest();
        bundleRest.setName("Create Bundle Without Metadata");

        String token = getAuthToken(admin.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/items/" + item.getID() + "/bundles")
                                                               .content(mapper.writeValueAsBytes(bundleRest))
                                                               .contentType(contentType))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        UUID bundleUuid = UUID.fromString(String.valueOf(map.get("uuid")));


        getClient().perform(get("/api/core/bundles/" + bundleUuid)
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", BundleMatcher.matchBundle(
                           "Create Bundle Without Metadata",
                           bundleUuid, null, Constants.BUNDLE, new ArrayList<>())));
    }

    @Test
    public void createBundleWithMetadata() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        BundleRest bundleRest = new BundleRest();
        bundleRest.setName("Create Bundle Without Metadata");
        bundleRest.setMetadata(new MetadataRest()
                                       .put("dc.description",
                                            new MetadataValueRest("A description"))
                                       .put("dc.relation",
                                            new MetadataValueRest("A relation")));


        String token = getAuthToken(admin.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/items/" + item.getID() + "/bundles")
                                                               .content(mapper.writeValueAsBytes(bundleRest))
                                                               .contentType(contentType))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        UUID bundleUuid = UUID.fromString(String.valueOf(map.get("uuid")));


        getClient().perform(get("/api/core/bundles/" + bundleUuid)
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", BundleMatcher.matchBundle(
                           "Create Bundle Without Metadata",
                           bundleUuid, null, Constants.BUNDLE, new ArrayList<>())))
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.metadata", Matchers.allOf(
                                   MetadataMatcher.matchMetadata("dc.description",
                                                                 "A description"),
                                   MetadataMatcher.matchMetadata("dc.relation",
                                                                 "A relation"))))));
    }

    @Test
    public void createBundleAsAnonymous() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        BundleRest bundleRest = new BundleRest();
        bundleRest.setName("Create Bundle Without Metadata");

        getClient().perform(post("/api/core/items/" + item.getID() + "/bundles")
                                    .content(mapper.writeValueAsBytes(bundleRest)).contentType(contentType))
                   .andExpect(status().isUnauthorized());

        getClient().perform(get("/api/core/items/" + item.getID() + "/bundles"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", is(0)));

    }

    @Test
    public void createBundleWithInsufficientPermissions() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        BundleRest bundleRest = new BundleRest();
        bundleRest.setName("Create Bundle Without Metadata");

        String token = getAuthToken(eperson.getEmail(), password);


        getClient(token).perform(post("/api/core/items/" + item.getID() + "/bundles")
                                         .content(mapper.writeValueAsBytes(bundleRest)).contentType(contentType))
                        .andExpect(status().isForbidden());

        getClient().perform(get("/api/core/items/" + item.getID() + "/bundles"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", is(0)));

    }

    @Test
    public void createBundleWithSufficientPermissions() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        context.turnOffAuthorisationSystem();

        EPerson createBundleEperson = EPersonBuilder.createEPerson(context).withEmail("createm@bundle.org")
                                                    .withPassword("test")
                                                    .withNameInMetadata("Create", "Bundle").build();

        ResourcePolicy rp1 = ResourcePolicyBuilder.createResourcePolicy(context).withUser(createBundleEperson)
                                                  .withAction(Constants.ADD)
                                                  .withDspaceObject(item).build();
        context.restoreAuthSystemState();


        BundleRest bundleRest = new BundleRest();
        bundleRest.setName("Create Bundle Without Metadata");


        String token = getAuthToken(createBundleEperson.getEmail(), "test");


        MvcResult mvcResult = getClient(token).perform(post("/api/core/items/" + item.getID() + "/bundles")
                                                               .content(mapper.writeValueAsBytes(bundleRest))
                                                               .contentType(contentType))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        UUID bundleUuid = UUID.fromString(String.valueOf(map.get("uuid")));


        getClient().perform(get("/api/core/bundles/" + bundleUuid)
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", BundleMatcher.matchBundle(
                           "Create Bundle Without Metadata",
                           bundleUuid, null, Constants.BUNDLE, new ArrayList<>())));

    }

    @Test
    public void createBundleOnNonExistingItem() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        BundleRest bundleRest = new BundleRest();
        bundleRest.setName("Create Bundle Without Metadata");

        String token = getAuthToken(eperson.getEmail(), password);


        getClient(token).perform(post("/api/core/items/" + UUID.randomUUID() + "/bundles")
                                         .content(mapper.writeValueAsBytes(bundleRest)).contentType(contentType))
                        .andExpect(status().isNotFound());

    }

    @Test
    public void getBitstreamsForBundle() throws Exception {
        context.turnOffAuthorisationSystem();

        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, item, is)
                                         .withName("Bitstream")
                                         .withDescription("Description")
                                         .withMimeType("text/plain")
                                         .build();
            bitstream2 = BitstreamBuilder.createBitstream(context, item, is)
                                         .withName("Bitstream2")
                                         .withDescription("Description2")
                                         .withMimeType("text/plain")
                                         .build();
        }

        bundle1 = BundleBuilder.createBundle(context, item)
                               .withName("testname")
                               .withBitstream(bitstream1)
                               .withBitstream(bitstream2)
                               .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/bundles/" + bundle1.getID() + "/bitstreams")
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.bitstreams", Matchers.hasItems(
                           BitstreamMatcher.matchBitstreamEntry(bitstream1),
                           BitstreamMatcher.matchBitstreamEntry(bitstream2)
                   )));
    }

    @Test
    public void patchMoveBitstreams() throws Exception {
        context.turnOffAuthorisationSystem();

        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream")
                    .withDescription("Description")
                    .withMimeType("text/plain")
                    .build();
            bitstream2 = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream2")
                    .withDescription("Description2")
                    .withMimeType("text/plain")
                    .build();
        }

        bundle1 = BundleBuilder.createBundle(context, item)
                .withName("testname")
                .withBitstream(bitstream1)
                .withBitstream(bitstream2)
                .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/bundles/" + bundle1.getID() + "/bitstreams")
                .param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.bitstreams", Matchers.contains(
                        BitstreamMatcher.matchBitstreamEntry(bitstream1),
                        BitstreamMatcher.matchBitstreamEntry(bitstream2)
                )));

        List<Operation> ops = new ArrayList<Operation>();
        MoveOperation moveOperation = new MoveOperation("/_links/bitstreams/0/href",
                "/_links/bitstreams/1/href");
        ops.add(moveOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/core/bundles/" + bundle1.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                    .andExpect(status().isOk());

        getClient().perform(get("/api/core/bundles/" + bundle1.getID() + "/bitstreams")
                .param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.bitstreams", Matchers.contains(
                        BitstreamMatcher.matchBitstreamEntry(bitstream2),
                        BitstreamMatcher.matchBitstreamEntry(bitstream1)
                )));
    }

    @Test
    public void deleteBundle() throws Exception {
        context.turnOffAuthorisationSystem();

        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream")
                    .withDescription("Description")
                    .withMimeType("text/plain")
                    .build();
            bitstream2 = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream2")
                    .withDescription("Description2")
                    .withMimeType("text/plain")
                    .build();
        }

        bundle1 = BundleBuilder.createBundle(context, item)
                .withName("testname")
                .withBitstream(bitstream1)
                .withBitstream(bitstream2)
                .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        // Check if bundle is present
        getClient().perform(get("/api/core/bundles/" + bundle1.getID() + "/bitstreams")
                .param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.bitstreams", Matchers.hasItems(
                        BitstreamMatcher.matchBitstreamEntry(bitstream1),
                        BitstreamMatcher.matchBitstreamEntry(bitstream2)
                )));

        // Delete bundle with admin auth token
        getClient(token).perform(delete("/api/core/bundles/" + bundle1.getID()))
                .andExpect(status().is(204));

        // Verify 404 after delete for bundle AND its bitstreams
        getClient(token).perform(get("/api/core/bundles/" + bundle1.getID()))
                .andExpect(status().isNotFound());
        getClient(token).perform(get("/api/core/bitstreams/" + bitstream1.getID()))
                .andExpect(status().isNotFound());
        getClient(token).perform(get("/api/core/bitstreams/" + bitstream2.getID()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleteBundle_Forbidden() throws Exception {
        context.turnOffAuthorisationSystem();

        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream")
                    .withDescription("Description")
                    .withMimeType("text/plain")
                    .build();
            bitstream2 = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream2")
                    .withDescription("Description2")
                    .withMimeType("text/plain")
                    .build();
        }

        bundle1 = BundleBuilder.createBundle(context, item)
                .withName("testname")
                .withBitstream(bitstream1)
                .withBitstream(bitstream2)
                .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Try to delete bundle with eperson auth token
        getClient(token).perform(delete("/api/core/bundles/" + bundle1.getID()))
                .andExpect(status().isForbidden());

        // Verify the bundle is still here
        getClient(token).perform(get("/api/core/bundles/" + bundle1.getID()))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteBundle_NoAuthToken() throws Exception {
        context.turnOffAuthorisationSystem();

        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream")
                    .withDescription("Description")
                    .withMimeType("text/plain")
                    .build();
            bitstream2 = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream2")
                    .withDescription("Description2")
                    .withMimeType("text/plain")
                    .build();
        }

        bundle1 = BundleBuilder.createBundle(context, item)
                .withName("testname")
                .withBitstream(bitstream1)
                .withBitstream(bitstream2)
                .build();

        context.restoreAuthSystemState();

        // Try to delete bundle without auth token
        getClient().perform(delete("/api/core/bundles/" + bundle1.getID()))
                .andExpect(status().isUnauthorized());

        // Verify the bundle is still here
        getClient().perform(get("/api/core/bundles/" + bundle1.getID()))
                .andExpect(status().isOk());
    }

}
