/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.matcher.BundleMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
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
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test to test the /api/core/bitstreams/[id]/content endpoint
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 */
public class BitstreamControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    BundleService bundleService;

    @Autowired
    BitstreamService bitstreamService;

    private static final Logger log = LogManager.getLogger(BitstreamControllerIT.class);

    @Test
    public void getBundle() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";

        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("ORIGINAL")
                                      .build();

        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                                BundleMatcher.matchBundle(bundle1.getName(),
                                                          bundle1.getID(),
                                                          bundle1.getHandle(),
                                                          bundle1.getType(),
                                                          bundle1.getBitstreams())

                        )));

    }

    @Test
    public void getFirstBundleWhenMultipleBundles() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";

        List<Bundle> bundles = new ArrayList();
        bundles.add(BundleBuilder.createBundle(context, publicItem1)
                                      .withName("TEST FIRST BUNDLE")
                                      .build());

        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, bundles.get(0), is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        bundles.add(BundleBuilder.createBundle(context, publicItem1)
                                      .withName("TEST SECOND BUNDLE")
                                      .withBitstream(bitstream)
                                      .build());


        context.restoreAuthSystemState();

        // While in DSpace code, Bundles are *unordered*, in Hibernate v5 + H2 v2.x, they are returned sorted by UUID.
        // So, we reorder this list of created Bundles by UUID to get their expected return order.
        // NOTE: Once on Hibernate v6, this might need "toString()" removed as it may sort UUIDs based on RFC 4412.
        Comparator<Bundle> compareByUUID = Comparator.comparing(b -> b.getID().toString());
        bundles.sort(compareByUUID);

        String token = getAuthToken(admin.getEmail(), password);

        // Expect only the first Bundle to be returned
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                           BundleMatcher.matchBundle(bundles.get(0).getName(),
                                                     bundles.get(0).getID(),
                                                     bundles.get(0).getHandle(),
                                                     bundles.get(0).getType(),
                                                     bundles.get(0).getBitstreams())
        )));


    }

    @Test
    public void getBundleWhenNoBundle() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";

        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("TEST FIRST BUNDLE")
                                      .build();

        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, bundle1, is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        bundleService.removeBitstream(context, bundle1, bitstream);
        bundleService.update(context, bundle1);
        bitstreamService.update(context, bitstream);

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/bundle"))
                        .andExpect(status().isNoContent());

    }

    @Test
    public void putOnBitstreamInOneBundle() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item targetItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Test")
                                     .withIssueDate("2010-10-17")
                                     .withAuthor("Smith, Donald")
                                     .withSubject("ExtraEntry")
                                     .build();


        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("TEST FIRST BUNDLE")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, bundle1, is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        Bundle targetBundle = BundleBuilder.createBundle(context, targetItem)
                                           .withName("TARGET BUNDLE")
                                           .build();


        EPerson putBundlePerson = EPersonBuilder.createEPerson(context).withEmail("bundle@pput.org")
                                                .withPassword("test")
                                                .withNameInMetadata("Bundle", "Put").build();

        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.REMOVE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.ADD)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bitstream).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(publicItem1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetItem).build();


        context.restoreAuthSystemState();
        String token = getAuthToken(putBundlePerson.getEmail(), "test");

        getClient(token)
                .perform(put("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                                 .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                 .content(
                                         "https://localhost:8080/spring-rest/api/core/bundles/" + targetBundle.getID()
                                 ))
                .andExpect(status().isOk())
        ;

        targetBundle = bundleService.find(context, targetBundle.getID());
        String name = targetBundle.getName();
        String handle = targetBundle.getHandle();
        List<Bitstream> bitstreams = targetBundle.getBitstreams();
        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                                BundleMatcher.matchBundle(name,
                                                          targetBundle.getID(),
                                                          handle,
                                                          targetBundle.getType(),
                                                          bitstreams)
                        )));


    }

    @Test
    public void putOnBitstreamInOneBundleForbiddenTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2016-11-11")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item targetItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Test")
                                     .withIssueDate("2016-11-11")
                                     .withAuthor("Smith, Donald")
                                     .withSubject("ExtraEntry")
                                     .build();


        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("TEST FIRST BUNDLE")
                                      .build();

        Bundle targetBundle = BundleBuilder.createBundle(context, targetItem)
                                           .withName("TARGET BUNDLE")
                                           .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, bundle1, is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(put("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content("https://localhost:8080/spring-rest/api/core/bundles/" + targetBundle.getID()))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void putOnBitstreamInMultipleBundles() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item targetItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Test")
                                     .withIssueDate("2010-10-17")
                                     .withAuthor("Smith, Donald")
                                     .withSubject("ExtraEntry")
                                     .build();


        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("TEST FIRST BUNDLE")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, bundle1, is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        Bundle bundle2 = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("TEST SECOND BUNDLE")
                                      .withBitstream(bitstream)
                                      .build();

        Bundle targetBundle = BundleBuilder.createBundle(context, targetItem)
                                           .withName("TARGET BUNDLE")
                                           .build();


        EPerson putBundlePerson = EPersonBuilder.createEPerson(context).withEmail("bundle@pput.org")
                                                .withPassword("test")
                                                .withNameInMetadata("Bundle", "Put").build();

        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.REMOVE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.REMOVE)
                             .withDspaceObject(bundle2).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bundle2).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.ADD)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bitstream).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(publicItem1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetItem).build();


        context.restoreAuthSystemState();
        String token = getAuthToken(putBundlePerson.getEmail(), "test");

        getClient(token)
                .perform(put("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                                 .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                 .content(
                                         "https://localhost:8080/spring-rest/api/core/bundles/" + targetBundle.getID()
                                 ))
                .andExpect(status().isOk())
        ;

        targetBundle = bundleService.find(context, targetBundle.getID());
        String name = targetBundle.getName();
        String handle = targetBundle.getHandle();
        List<Bitstream> bitstreams = targetBundle.getBitstreams();

        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                                BundleMatcher.matchBundle(name,
                                                          targetBundle.getID(),
                                                          handle,
                                                          targetBundle.getType(),
                                                          bitstreams)
                        )));


    }

    @Test
    public void putOnBitstreamInNoBundle() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item targetItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Test")
                                     .withIssueDate("2010-10-17")
                                     .withAuthor("Smith, Donald")
                                     .withSubject("ExtraEntry")
                                     .build();


        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("TEST FIRST BUNDLE")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, bundle1, is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        // NOTE: this will set deleted = true for the bitstream
        bundleService.removeBitstream(context, bundle1, bitstream);
        bundleService.update(context, bundle1);
        bitstreamService.update(context, bitstream);

        Bundle targetBundle = BundleBuilder.createBundle(context, targetItem)
                                           .withName("TARGET BUNDLE")
                                           .build();


        EPerson putBundlePerson = EPersonBuilder.createEPerson(context).withEmail("bundle@pput.org")
                                                .withPassword("test")
                                                .withNameInMetadata("Bundle", "Put").build();

        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.REMOVE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.ADD)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bitstream).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(publicItem1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetItem).build();


        context.restoreAuthSystemState();
        String token = getAuthToken(putBundlePerson.getEmail(), "test");

        // at this moment the bitstream is still marked as deleted because it is not attached to any bundle
        assertTrue(bitstream.isDeleted());

        // add the bitstream to target bundle
        getClient(token)
                .perform(put("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                                 .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                 .content(
                                         "https://localhost:8080/spring-rest/api/core/bundles/" + targetBundle.getID()
                                 )).andExpect(status().isOk());

        // at this moment the bitstream should NOT be marked as deleted, because it has been attached to target bundle
        assertFalse(context.reloadEntity(bitstream).isDeleted());

        targetBundle = bundleService.find(context, targetBundle.getID());
        String name = targetBundle.getName();
        String handle = targetBundle.getHandle();
        List<Bitstream> bitstreams = targetBundle.getBitstreams();


        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                                BundleMatcher.matchBundle(name,
                                                          targetBundle.getID(),
                                                          handle,
                                                          targetBundle.getType(),
                                                          bitstreams)
                        )));


    }

    @Test
    public void putOnBitstreamInOneBundleWithNoRemoveRights() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item targetItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Test")
                                     .withIssueDate("2010-10-17")
                                     .withAuthor("Smith, Donald")
                                     .withSubject("ExtraEntry")
                                     .build();


        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("TEST FIRST BUNDLE")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, bundle1, is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        Bundle targetBundle = BundleBuilder.createBundle(context, targetItem)
                                           .withName("TARGET BUNDLE")
                                           .build();


        EPerson putBundlePerson = EPersonBuilder.createEPerson(context).withEmail("bundle@pput.org")
                                                .withPassword("test")
                                                .withNameInMetadata("Bundle", "Put").build();

        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.ADD)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bitstream).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(publicItem1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetItem).build();


        context.restoreAuthSystemState();
        String token = getAuthToken(putBundlePerson.getEmail(), "test");

        getClient(token)
                .perform(put("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                                 .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                 .content(
                                         "https://localhost:8080/spring-rest/api/core/bundles/" + targetBundle.getID()
                                 ))
                .andExpect(status().isForbidden());

        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                                BundleMatcher.matchBundle(bundle1.getName(),
                                                          bundle1.getID(),
                                                          bundle1.getHandle(),
                                                          bundle1.getType(),
                                                          bundle1.getBitstreams())

                        )));


    }

    @Test
    public void putOnBitstreamInOneBundleWithNoWriteOnCurrentBundleRights() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item targetItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Test")
                                     .withIssueDate("2010-10-17")
                                     .withAuthor("Smith, Donald")
                                     .withSubject("ExtraEntry")
                                     .build();


        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("TEST FIRST BUNDLE")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, bundle1, is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        Bundle targetBundle = BundleBuilder.createBundle(context, targetItem)
                                           .withName("TARGET BUNDLE")
                                           .build();


        EPerson putBundlePerson = EPersonBuilder.createEPerson(context).withEmail("bundle@pput.org")
                                                .withPassword("test")
                                                .withNameInMetadata("Bundle", "Put").build();

        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.REMOVE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.ADD)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bitstream).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(publicItem1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetItem).build();


        context.restoreAuthSystemState();
        String token = getAuthToken(putBundlePerson.getEmail(), "test");

        getClient(token)
                .perform(put("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                                 .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                 .content(
                                         "https://localhost:8080/spring-rest/api/core/bundles/" + targetBundle.getID()
                                 ))
                .andExpect(status().isForbidden());


        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                .param("projection", "full"))
                .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                                BundleMatcher.matchBundle(bundle1.getName(),
                                                          bundle1.getID(),
                                                          bundle1.getHandle(),
                                                          bundle1.getType(),
                                                          bundle1.getBitstreams())

                        )));


    }

    @Test
    public void putOnBitstreamInOneBundleWithNoWriteRightsOnTargetBundle() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item targetItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Test")
                                     .withIssueDate("2010-10-17")
                                     .withAuthor("Smith, Donald")
                                     .withSubject("ExtraEntry")
                                     .build();


        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("TEST FIRST BUNDLE")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, bundle1, is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        Bundle targetBundle = BundleBuilder.createBundle(context, targetItem)
                                           .withName("TARGET BUNDLE")
                                           .build();


        EPerson putBundlePerson = EPersonBuilder.createEPerson(context).withEmail("bundle@pput.org")
                                                .withPassword("test")
                                                .withNameInMetadata("Bundle", "Put").build();

        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.REMOVE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.ADD)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bitstream).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(publicItem1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetItem).build();


        context.restoreAuthSystemState();
        String token = getAuthToken(putBundlePerson.getEmail(), "test");

        getClient(token)
                .perform(put("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                                 .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                 .content(
                                         "https://localhost:8080/spring-rest/api/core/bundles/" + targetBundle.getID()
                                 ))
                .andExpect(status().isForbidden());

        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                                BundleMatcher.matchBundle(bundle1.getName(),
                                                          bundle1.getID(),
                                                          bundle1.getHandle(),
                                                          bundle1.getType(),
                                                          bundle1.getBitstreams())

                        )));


    }

    @Test
    public void putOnBitstreamInOneBundleWithNoAddRightsOnTargetBundle() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item targetItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Test")
                                     .withIssueDate("2010-10-17")
                                     .withAuthor("Smith, Donald")
                                     .withSubject("ExtraEntry")
                                     .build();


        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("TEST FIRST BUNDLE")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, bundle1, is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        Bundle targetBundle = BundleBuilder.createBundle(context, targetItem)
                                           .withName("TARGET BUNDLE")
                                           .build();


        EPerson putBundlePerson = EPersonBuilder.createEPerson(context).withEmail("bundle@pput.org")
                                                .withPassword("test")
                                                .withNameInMetadata("Bundle", "Put").build();

        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.REMOVE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bitstream).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(publicItem1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetItem).build();


        context.restoreAuthSystemState();
        String token = getAuthToken(putBundlePerson.getEmail(), "test");

        getClient(token)
                .perform(put("/api/core/bitstreams/" + bitstream.getID() + "/bundle").param("projection", "full")
                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                 .content(
                                         "https://localhost:8080/spring-rest/api/core/bundles/" + targetBundle.getID()
                                 ))
                .andExpect(status().isForbidden());

        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                                BundleMatcher.matchBundle(bundle1.getName(),
                                                          bundle1.getID(),
                                                          bundle1.getHandle(),
                                                          bundle1.getType(),
                                                          bundle1.getBitstreams())

                        )));


    }

    @Test
    public void putOnBitstreamInOneBundleWithNoWriteRightsOnBitstream() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item targetItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Test")
                                     .withIssueDate("2010-10-17")
                                     .withAuthor("Smith, Donald")
                                     .withSubject("ExtraEntry")
                                     .build();


        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("TEST FIRST BUNDLE")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, bundle1, is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        Bundle targetBundle = BundleBuilder.createBundle(context, targetItem)
                                           .withName("TARGET BUNDLE")
                                           .build();


        EPerson putBundlePerson = EPersonBuilder.createEPerson(context).withEmail("bundle@pput.org")
                                                .withPassword("test")
                                                .withNameInMetadata("Bundle", "Put").build();

        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.REMOVE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.ADD)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(publicItem1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetItem).build();


        context.restoreAuthSystemState();
        String token = getAuthToken(putBundlePerson.getEmail(), "test");

        getClient(token)
                .perform(put("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                                 .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                 .content(
                                         "https://localhost:8080/spring-rest/api/core/bundles/" + targetBundle.getID()
                                 ))
                .andExpect(status().isForbidden());


        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                                BundleMatcher.matchBundle(bundle1.getName(),
                                                          bundle1.getID(),
                                                          bundle1.getHandle(),
                                                          bundle1.getType(),
                                                          bundle1.getBitstreams())

                        )));


    }

    @Test
    public void putOnBitstreamInOneBundleWithNoWriteRightsOnCurrentItem() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item targetItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Test")
                                     .withIssueDate("2010-10-17")
                                     .withAuthor("Smith, Donald")
                                     .withSubject("ExtraEntry")
                                     .build();


        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("TEST FIRST BUNDLE")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, bundle1, is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        Bundle targetBundle = BundleBuilder.createBundle(context, targetItem)
                                           .withName("TARGET BUNDLE")
                                           .build();


        EPerson putBundlePerson = EPersonBuilder.createEPerson(context).withEmail("bundle@pput.org")
                                                .withPassword("test")
                                                .withNameInMetadata("Bundle", "Put").build();

        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.REMOVE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.ADD)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bitstream).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetItem).build();


        context.restoreAuthSystemState();
        String token = getAuthToken(putBundlePerson.getEmail(), "test");

        getClient(token)
                .perform(put("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                                 .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                 .content(
                                         "https://localhost:8080/spring-rest/api/core/bundles/" + targetBundle.getID()
                                 ))
                .andExpect(status().isForbidden());

        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                                BundleMatcher.matchBundle(bundle1.getName(),
                                                          bundle1.getID(),
                                                          bundle1.getHandle(),
                                                          bundle1.getType(),
                                                          bundle1.getBitstreams())

                        )));


    }

    @Test
    public void putOnBitstreamInOneBundleWithNoWriteRightsOnTargetItem() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item targetItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Test")
                                     .withIssueDate("2010-10-17")
                                     .withAuthor("Smith, Donald")
                                     .withSubject("ExtraEntry")
                                     .build();


        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("TEST FIRST BUNDLE")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, bundle1, is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        Bundle targetBundle = BundleBuilder.createBundle(context, targetItem)
                                           .withName("TARGET BUNDLE")
                                           .build();


        EPerson putBundlePerson = EPersonBuilder.createEPerson(context).withEmail("bundle@pput.org")
                                                .withPassword("test")
                                                .withNameInMetadata("Bundle", "Put").build();

        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.REMOVE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bundle1).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.ADD)
                             .withDspaceObject(targetBundle).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(bitstream).build();
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(putBundlePerson)
                             .withAction(Constants.WRITE)
                             .withDspaceObject(publicItem1).build();


        context.restoreAuthSystemState();
        String token = getAuthToken(putBundlePerson.getEmail(), "test");

        getClient(token)
                .perform(put("/api/core/bitstreams/" + bitstream.getID() + "/bundle").param("projection", "full")
                                 .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                 .content(
                                         "https://localhost:8080/spring-rest/api/core/bundles/" + targetBundle.getID()
                                 ))
                .andExpect(status().isForbidden());

        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/bundle")
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                                BundleMatcher.matchBundle(bundle1.getName(),
                                                          bundle1.getID(),
                                                          bundle1.getHandle(),
                                                          bundle1.getType(),
                                                          bundle1.getBitstreams())

                        )));


    }

}
