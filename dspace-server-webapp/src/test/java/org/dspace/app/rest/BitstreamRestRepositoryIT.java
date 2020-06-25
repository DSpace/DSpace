/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataDoesNotExist;
import static org.dspace.core.Constants.WRITE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.builder.ResourcePolicyBuilder;
import org.dspace.app.rest.matcher.BitstreamFormatMatcher;
import org.dspace.app.rest.matcher.BitstreamMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.test.MetadataPatchSuite;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BitstreamRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private ResourcePolicyService resourcePolicyService;

    @Test
    public void findAllTest() throws Exception {
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

        //Add a bitstream to an item
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                             createBitstream(context, publicItem1, is)
                                         .withName("Bitstream1")
                                         .withDescription("description123")
                                         .withMimeType("text/plain")
                                         .build();
        }

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/bitstreams/"))
                   .andExpect(status().isMethodNotAllowed());
    }

    //TODO Re-enable test after https://jira.duraspace.org/browse/DS-3774 is fixed
    @Ignore
    @Test
    public void findAllWithDeletedTest() throws Exception {
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
                                      .build();

        String bitstreamContent = "This is an archived bitstream";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withMimeType("text/plain")
                                        .build();
        }

        //Add a bitstream to an item
        bitstreamContent = "This is a deleted bitstream";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                             createBitstream(context, publicItem1, is)
                                         .withName("Bitstream1")
                                         .withMimeType("text/plain")
                                         .build();
        }

        //Delete the last bitstream
        bitstreamService.delete(context, bitstream1);
        context.commit();

        getClient().perform(get("/api/core/bitstreams/"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.bitstreams", contains(
                       BitstreamMatcher.matchBitstreamEntry(bitstream)
                   )))

        ;
    }

    @Test
    public void findOneBitstreamTest() throws Exception {

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
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        //Add a bitstream to an item
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                             createBitstream(context, publicItem1, is)
                                         .withName("Bitstream1")
                                         .withDescription("Description1")
                                         .withMimeType("text/plain")
                                         .build();
        }

        context.restoreAuthSystemState();

        // When full projection is requested, response should include expected properties, links, and embeds.
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID())
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchFullEmbeds()))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchBitstreamEntry(bitstream)))
                   .andExpect(jsonPath("$", not(BitstreamMatcher.matchBitstreamEntry(bitstream1))))
        ;

        // When no projection is requested, response should include expected properties, links, and no embeds.
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
        ;

    }

    @Test
    public void findOneBitstreamTest_EmbargoedBitstream_Anon() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        // a public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        Item publicItem1;
        Bitstream bitstream;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, org.apache.commons.lang3.CharEncoding.UTF_8)) {

            publicItem1 = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item 1")
                                     .withIssueDate("2017-10-17")
                                     .withAuthor("Smith, Donald")
                                     .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod("3 months")
                .build();
        }
        context.restoreAuthSystemState();

        // Bitstream metadata should still be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", BitstreamMatcher.matchProperties(bitstream)))
                   .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchLinks(bitstream.getID())))
        ;

        // Also accessible as embedded object by anonymous request
        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "?embed=bundles/bitstreams"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.bundles._embedded.bundles[0]._embedded.bitstreams._embedded" +
                                       ".bitstreams[0]", BitstreamMatcher.matchProperties(bitstream)))
        ;
    }

    @Test
    public void findOneBitstreamTest_NoReadPolicyOnBitstream_Anon() throws Exception {
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

        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        // Remove all READ policies on bitstream
        resourcePolicyService.removePolicies(context, bitstream, Constants.READ);

        context.restoreAuthSystemState();

        // Bitstream metadata should still be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", BitstreamMatcher.matchProperties(bitstream)))
                   .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchLinks(bitstream.getID())))
        ;

        // Also accessible as embedded object by anonymous request
        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "?embed=bundles/bitstreams"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.bundles._embedded.bundles[0]._embedded.bitstreams._embedded" +
                                       ".bitstreams[0]", BitstreamMatcher.matchProperties(bitstream)))
        ;
    }

    @Test
    public void findOneBitstreamTest_EmbargoedBitstream_NoREADRightsOnBundle() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(eperson);

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        // a public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        Item publicItem1;
        Bitstream bitstream;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, org.apache.commons.lang3.CharEncoding.UTF_8)) {

            publicItem1 = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item 1")
                                     .withIssueDate("2017-10-17")
                                     .withAuthor("Smith, Donald")
                                     .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod("3 months")
                .build();
        }

        // Remove read policies on bundle of bitstream
        resourcePolicyService.removePolicies(context, bitstream.getBundles().get(0), Constants.READ);

        context.restoreAuthSystemState();

        // Bitstream metadata should not be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                   .andExpect(status().isUnauthorized())
        ;

        // Bitstream metadata should not be accessible by submitter
        String submitterToken = getAuthToken(context.getCurrentUser().getEmail(), password);
        getClient(submitterToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                                 .andExpect(status().isForbidden())
        ;

        // Bitstream metadata should be accessible by admin
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                             .andExpect(status().isOk())
        ;
    }

    @Test
    public void findOneBitstreamTest_EmbargoedBitstream_ePersonREADRightsOnBundle() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(eperson);

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        // a public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        Item publicItem1;
        Bitstream bitstream;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, org.apache.commons.lang3.CharEncoding.UTF_8)) {

            publicItem1 = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item 1")
                                     .withIssueDate("2017-10-17")
                                     .withAuthor("Smith, Donald")
                                     .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod("3 months")
                .build();
        }

        // Replace anon read policy on bundle of bitstream with ePerson READ policy
        resourcePolicyService.removePolicies(context, bitstream.getBundles().get(0), Constants.READ);
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(eperson)
                             .withAction(Constants.READ)
                             .withDspaceObject(bitstream.getBundles().get(0)).build();

        context.restoreAuthSystemState();

        // Bitstream metadata should not be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                   .andExpect(status().isUnauthorized())
        ;

        // Bitstream metadata should be accessible by eperson
        String submitterToken = getAuthToken(context.getCurrentUser().getEmail(), password);
        getClient(submitterToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                                 .andExpect(status().isOk())
        ;

        // Bitstream metadata should be accessible by admin
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                             .andExpect(status().isOk())
        ;
    }

    @Test
    public void findOneBitstreamTest_EmbargoedBitstream_NoREADRightsOnItem() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(eperson);

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        // a public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        Item publicItem1;
        Bitstream bitstream;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, org.apache.commons.lang3.CharEncoding.UTF_8)) {

            publicItem1 = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item 1")
                                     .withIssueDate("2017-10-17")
                                     .withAuthor("Smith, Donald")
                                     .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod("3 months")
                .build();
        }

        // Remove read policies on item of bitstream
        resourcePolicyService.removePolicies(context, publicItem1, Constants.READ);

        context.restoreAuthSystemState();

        // Bitstream metadata should not be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                   .andExpect(status().isUnauthorized())
        ;

        // Bitstream metadata should not be accessible by submitter
        String submitterToken = getAuthToken(context.getCurrentUser().getEmail(), password);
        getClient(submitterToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                                 .andExpect(status().isForbidden())
        ;

        // Bitstream metadata should be accessible by admin
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                             .andExpect(status().isOk())
        ;
    }

    @Test
    public void findOneBitstreamTest_EmbargoedBitstream_ePersonREADRightsOnItem() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(eperson);

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        // a public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        Item publicItem1;
        Bitstream bitstream;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, org.apache.commons.lang3.CharEncoding.UTF_8)) {

            publicItem1 = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item 1")
                                     .withIssueDate("2017-10-17")
                                     .withAuthor("Smith, Donald")
                                     .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod("3 months")
                .build();
        }

        // Replace anon read policy on item of bitstream with ePerson READ policy
        resourcePolicyService.removePolicies(context, publicItem1, Constants.READ);
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(eperson)
                             .withAction(Constants.READ)
                             .withDspaceObject(publicItem1).build();

        context.restoreAuthSystemState();

        // Bitstream metadata should not be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                   .andExpect(status().isUnauthorized())
        ;

        // Bitstream metadata should be accessible by eperson
        String submitterToken = getAuthToken(context.getCurrentUser().getEmail(), password);
        getClient(submitterToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                                 .andExpect(status().isOk())
        ;

        // Bitstream metadata should be accessible by admin
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                             .andExpect(status().isOk())
        ;
    }

    @Test
    public void findOneBitstreamRelsTest() throws Exception {

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
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        //Add a bitstream to an item
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                             createBitstream(context, publicItem1, is)
                                         .withName("Bitstream1")
                                         .withDescription("Description1234")
                                         .withMimeType("text/plain")
                                         .build();
        }

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/format"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", BitstreamFormatMatcher.matchBitstreamFormatMimeType("text/plain")))
        ;

        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                   .andExpect(status().isOk())
                   .andExpect(content().string("ThisIsSomeDummyText"))
        ;

    }

    @Test
    public void findOneLogoBitstreamTest() throws Exception {

        // We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // ** GIVEN **
        // 1. A community with a logo
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").withLogo("logo_community")
                                          .build();

        // 2. A collection with a logo
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection")
                                          .withLogo("logo_collection").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/bitstreams/" + parentCommunity.getLogo().getID()))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + col.getLogo().getID())).andExpect(status().isOk());

    }

    @Test
    public void findOneLogoBitstreamRelsTest() throws Exception {

        // We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // ** GIVEN **
        // 1. A community with a logo
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").withLogo("logo_community")
                                          .build();

        // 2. A collection with a logo
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection")
                                          .withLogo("logo_collection").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/bitstreams/" + parentCommunity.getLogo().getID() + "/content"))
                   .andExpect(status().isOk()).andExpect(content().string("logo_community"));

        getClient().perform(get("/api/core/bitstreams/" + col.getLogo().getID() + "/content"))
                   .andExpect(status().isOk()).andExpect(content().string("logo_collection"));
    }

    @Test
    public void findOneWrongUUID() throws Exception {

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

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/bitstreams/" + UUID.randomUUID()))
                   .andExpect(status().isNotFound())
        ;

    }

    @Test
    public void deleteOne() throws Exception {

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
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        // Delete
        getClient(token).perform(delete("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().is(204));

        // Verify 404 after delete
        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleteForbidden() throws Exception {

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
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Delete using an unauthorized user
        getClient(token).perform(delete("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isForbidden());

        // Verify the bitstream is still here
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteUnauthorized() throws Exception {

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
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        // Delete as anonymous
        getClient().perform(delete("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isUnauthorized());

        // Verify the bitstream is still here
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteLogo() throws Exception {
        // We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // ** GIVEN **
        // 1. A community with a logo
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").withLogo("logo_community")
                                          .build();

        // 2. A collection with a logo
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection")
                                          .withLogo("logo_collection").build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        // trying to DELETE parentCommunity logo should work
        getClient(token).perform(delete("/api/core/bitstreams/" + parentCommunity.getLogo().getID()))
                   .andExpect(status().is(204));

        // trying to DELETE collection logo should work
        getClient(token).perform(delete("/api/core/bitstreams/" + col.getLogo().getID()))
                   .andExpect(status().is(204));
    }

    @Test
    public void deleteMissing() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        // Delete
        getClient(token).perform(delete("/api/core/bitstreams/1c11f3f1-ba1f-4f36-908a-3f1ea9a557eb"))
                .andExpect(status().isNotFound());

        // Verify 404 after failed delete
        getClient(token).perform(delete("/api/core/bitstreams/1c11f3f1-ba1f-4f36-908a-3f1ea9a557eb"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleteDeleted() throws Exception {
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
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .withDescription("Description")
                    .withMimeType("text/plain")
                    .build();
        }

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        // Delete
        getClient(token).perform(delete("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().is(204));

        // Verify 404 when trying to delete a non-existing bitstream
        getClient(token).perform(delete("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void patchBitstreamMetadataAuthorized() throws Exception {
        runPatchMetadataTests(admin, 200);
    }

    @Test
    public void patchBitstreamMetadataUnauthorized() throws Exception {
        runPatchMetadataTests(eperson, 403);
    }

    private void runPatchMetadataTests(EPerson asUser, int expectedStatus) throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").withLogo("logo_community")
                .build();
        context.restoreAuthSystemState();
        String token = getAuthToken(asUser.getEmail(), password);

        new MetadataPatchSuite().runWith(getClient(token), "/api/core/bitstreams/"
                + parentCommunity.getLogo().getID(), expectedStatus);
    }


    @Test
    public void testHiddenMetadataForAnonymousUser() throws Exception {

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
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withProvenance("Provenance Data")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        // When full projection is requested, response should include expected properties, links, and embeds.
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID())
                                    .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchFullEmbeds()))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchBitstreamEntry(bitstream)))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Bitstream")))
                   .andExpect(jsonPath("$.metadata", matchMetadataDoesNotExist("dc.description.provenance")))
        ;

    }

    @Test
    public void testHiddenMetadataForAdminUser() throws Exception {

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
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withProvenance("Provenance Data")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        // When full projection is requested, response should include expected properties, links, and embeds.
        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID())
                                    .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchFullEmbeds()))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchBitstreamEntry(bitstream)))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Bitstream")))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.description.provenance", "Provenance Data")))
        ;

    }


    @Test
    public void testHiddenMetadataForUserWithWriteRights() throws Exception {

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
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withProvenance("Provenance Data")
                                        .withMimeType("text/plain")
                                        .build();
        }

        ResourcePolicyBuilder.createResourcePolicy(context)
                             .withUser(eperson)
                             .withAction(WRITE)
                             .withDspaceObject(col1)
                             .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);


        // When full projection is requested, response should include expected properties, links, and embeds.
        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID())
                                         .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", BitstreamMatcher.matchFullEmbeds()))
                        .andExpect(jsonPath("$", BitstreamMatcher.matchBitstreamEntry(bitstream)))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Bitstream")))
                        .andExpect(jsonPath("$.metadata", matchMetadataDoesNotExist("dc.description.provenance")))
        ;

    }


}
