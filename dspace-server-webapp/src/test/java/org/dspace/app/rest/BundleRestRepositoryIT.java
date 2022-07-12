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
import org.dspace.app.rest.matcher.BitstreamMatcher;
import org.dspace.app.rest.matcher.BundleMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.matcher.MetadataMatcher;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.MoveOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

public class BundleRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    ResourcePolicyService resourcePolicyService;

    @Autowired
    ItemService itemService;

    private Collection collection;
    private Item item;
    private Bundle bundle1;
    private Bundle bundle2;
    private Bitstream bitstream1;
    private Bitstream bitstream2;

    @Before
    @Override
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
    public void findOneTest() throws Exception {
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

        context.restoreAuthSystemState();

        // When full projection is requested, response should include expected properties, links, and embeds.
        getClient().perform(get("/api/core/bundles/" + bundle1.getID())
                .param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", BundleMatcher.matchFullEmbeds()))
                .andExpect(jsonPath("$", BundleMatcher.matchBundle(bundle1.getName(),
                        bundle1.getID(),
                        bundle1.getHandle(),
                        bundle1.getType(),
                        bundle1.getBitstreams())
                ))
        ;

        // When no projection is requested, response should include expected properties, links, and no embeds.
        getClient().perform(get("/api/core/bundles/" + bundle1.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
                .andExpect(jsonPath("$", BundleMatcher.matchLinks(bundle1.getID())))
                .andExpect(jsonPath("$", BundleMatcher.matchProperties(bundle1.getName(),
                        bundle1.getID(),
                        bundle1.getHandle(),
                        bundle1.getType())
                ))
        ;
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
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

        resourcePolicyService.removePolicies(context, bundle1, Constants.READ);
        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/core/bundles/" + bundle1.getID()))
                .andExpect(status().isForbidden());
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
        UUID bundleUuid = null;
        String token = getAuthToken(admin.getEmail(), password);
        try {
        MvcResult mvcResult = getClient(token).perform(post("/api/core/items/" + item.getID() + "/bundles")
                                                               .content(mapper.writeValueAsBytes(bundleRest))
                                                               .contentType(contentType))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        bundleUuid = UUID.fromString(String.valueOf(map.get("uuid")));


        getClient().perform(get("/api/core/bundles/" + bundleUuid)
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", BundleMatcher.matchBundle(
                           "Create Bundle Without Metadata",
                           bundleUuid, null, Constants.BUNDLE, new ArrayList<>())));
        } finally {
            BundleBuilder.deleteBundle(bundleUuid);
        }
    }

    @Test
    public void createBundleWithMetadata() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UUID bundleUuid = null;
        try {
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
        bundleUuid = UUID.fromString(String.valueOf(map.get("uuid")));


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
        } finally {
            BundleBuilder.deleteBundle(bundleUuid);
        }
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
        UUID bundleUuid = null;
        try {
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
        bundleUuid = UUID.fromString(String.valueOf(map.get("uuid")));


        getClient().perform(get("/api/core/bundles/" + bundleUuid)
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", BundleMatcher.matchBundle(
                           "Create Bundle Without Metadata",
                           bundleUuid, null, Constants.BUNDLE, new ArrayList<>())));
        } finally {
            BundleBuilder.deleteBundle(bundleUuid);
        }
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

        bundle1 = BundleBuilder.createBundle(context, item)
                               .withName("testname")
                               .build();

        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, item, is, bundle1.getName())
                                         .withName("Bitstream")
                                         .withDescription("Description")
                                         .withMimeType("text/plain")
                                         .build();
            bitstream2 = BitstreamBuilder.createBitstream(context, item, is, bundle1.getName())
                                         .withName("Bitstream2")
                                         .withDescription("Description2")
                                         .withMimeType("text/plain")
                                         .build();
        }

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
    public void getBitstreamsForBundleForbiddenTest() throws Exception {
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

        resourcePolicyService.removePolicies(context, bundle1, Constants.READ);
        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/core/bundles/" + bundle1.getID() + "/bitstreams"))
                   .andExpect(status().isForbidden());
    }

    @Test
    public void patchMoveBitstreams() throws Exception {
        context.turnOffAuthorisationSystem();

        bundle1 = BundleBuilder.createBundle(context, item)
                               .withName("testname")
                               .build();

        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, item, is, bundle1.getName())
                    .withName("Bitstream")
                    .withDescription("Description")
                    .withMimeType("text/plain")
                    .build();
            bitstream2 = BitstreamBuilder.createBitstream(context, item, is, bundle1.getName())
                    .withName("Bitstream2")
                    .withDescription("Description2")
                    .withMimeType("text/plain")
                    .build();
        }

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

        bundle1 = item.getBundles("ORIGINAL").get(0);

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

    @Test
    public void getEmbeddedItemForBundle() throws Exception {
        context.turnOffAuthorisationSystem();

        bundle1 = BundleBuilder.createBundle(context, item)
                .withName("testname")
                .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/bundles/" + bundle1.getID() + "?embed=item"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.item",
                        ItemMatcher.matchItemWithTitleAndDateIssued(item, "Public item 1", "2017-10-17")
                ));
    }

    @Test
    /**
     * This test proves that, if a bundle is linked to multiple items, we only ever return the first item.
     * **NOTE: DSpace does NOT support or expect to have a bundle linked to multiple items**.
     * But, because the database does allow for it, this test simply proves the REST API will respond without an error
     */
    public void linksToFirstItemWhenMultipleItems() throws Exception {
        context.turnOffAuthorisationSystem();

        bundle1 = BundleBuilder.createBundle(context, item)
                .withName("testname")
                .build();

        Item item2 = ItemBuilder.createItem(context, collection)
                .withTitle("Public item 2")
                .withIssueDate("2020-07-08")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("SecondEntry")
                .build();

        itemService.addBundle(context, item2, bundle1);

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/bundles/" + bundle1.getID() + "/item"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$",
                        ItemMatcher.matchItemWithTitleAndDateIssued(item, "Public item 1", "2017-10-17")
                ));
    }

}
