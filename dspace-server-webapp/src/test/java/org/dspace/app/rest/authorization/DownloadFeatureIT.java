/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.time.Period;
import java.util.UUID;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.authorization.impl.DownloadFeature;
import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Constants;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DownloadFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private ResourcePolicyService resourcePolicyService;

    @Autowired
    private CollectionConverter collectionConverter;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private BitstreamConverter bitstreamConverter;

    @Autowired
    private MetadataAuthorityService metadataAuthorityService;
    @Autowired
    private BitstreamFormatService bitstreamFormatService;
    @Autowired
    private MetadataFieldService metadataFieldService;

    @Autowired
    private PluginService pluginService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Autowired
    private Utils utils;

    private AuthorizationFeature downloadFeature;

    private Collection collectionA;
    private Item itemA;
    private Bitstream bitstreamA;
    private Bitstream bitstreamB;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        downloadFeature = authorizationFeatureService.find(DownloadFeature.NAME);

        String bitstreamContent = "Dummy content";

        Community communityA = CommunityBuilder.createCommunity(context).build();
        collectionA = CollectionBuilder.createCollection(context, communityA).withLogo("Blub").build();

        itemA = ItemBuilder.createItem(context, collectionA).build();

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstreamA = BitstreamBuilder.createBitstream(context, itemA, is)
                                         .withName("Bitstream")
                                         .withDescription("Description")
                                         .withMimeType("text/plain")
                                         .build();
            bitstreamB = BitstreamBuilder.createBitstream(context, itemA, is)
                                         .withName("Bitstream2")
                                         .withDescription("Description2")
                                         .withMimeType("text/plain")
                                         .build();
        }
        resourcePolicyService.removePolicies(context, bitstreamB, Constants.READ);


        context.restoreAuthSystemState();
    }


    @Test
    public void downloadOfCollectionAAsAdmin() throws Exception {
        CollectionRest collectionRest = collectionConverter.convert(collectionA, Projection.DEFAULT);
        String collectionUri = utils.linkToSingleResource(collectionRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", collectionUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void downloadOfItemAAsAdmin() throws Exception {
        ItemRest itemRest = itemConverter.convert(itemA, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", itemUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    @Test
    public void downloadOfBitstreamAAsAdmin() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamA, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        Authorization authorizationFeature = new Authorization(admin, downloadFeature, bitstreamRest);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                        .andExpect(jsonPath("$._embedded.authorizations", contains(
                                Matchers.is(AuthorizationMatcher.matchAuthorization(authorizationFeature)))));

    }

    @Test
    public void downloadOfBitstreamBAsAdmin() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamB, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        Authorization authorizationFeature = new Authorization(admin, downloadFeature, bitstreamRest);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                        .andExpect(jsonPath("$._embedded.authorizations", contains(
                                Matchers.is(AuthorizationMatcher.matchAuthorization(authorizationFeature)))));

    }


    // Tests for anonymous user
    @Test
    public void downloadOfCollectionAAsAnonymous() throws Exception {
        CollectionRest collectionRest = collectionConverter.convert(collectionA, Projection.DEFAULT);
        String collectionUri = utils.linkToSingleResource(collectionRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", collectionUri)
                                    .param("feature", downloadFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void downloadOfItemAAsAnonymous() throws Exception {
        ItemRest itemRest = itemConverter.convert(itemA, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();


        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", itemUri)
                                    .param("feature", downloadFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    @Test
    public void downloadOfBitstreamAAsAnonymous() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamA, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        Authorization authorizationFeature = new Authorization(null, downloadFeature, bitstreamRest);

        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", bitstreamUri)
                                    .param("feature", downloadFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                   .andExpect(jsonPath("$._embedded.authorizations", contains(
                           Matchers.is(AuthorizationMatcher.matchAuthorization(authorizationFeature)))));

    }

    @Test
    public void downloadOfBitstreamBAsAnonymous() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamB, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();


        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", bitstreamUri)
                                    .param("feature", downloadFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    // Test for Eperson
    @Test
    public void downloadOfCollectionAAsEperson() throws Exception {
        CollectionRest collectionRest = collectionConverter.convert(collectionA, Projection.DEFAULT);
        String collectionUri = utils.linkToSingleResource(collectionRest, "self").getHref();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", collectionUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void downloadOfItemAAsEperson() throws Exception {
        ItemRest itemRest = itemConverter.convert(itemA, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();

        String token = getAuthToken(eperson.getEmail(), password);


        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", itemUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void downloadOfBitstreamAAsEperson() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamA, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        Authorization authorizationFeature = new Authorization(eperson, downloadFeature, bitstreamRest);

        String token = getAuthToken(eperson.getEmail(), password);


        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                        .andExpect(jsonPath("$._embedded.authorizations", contains(
                                Matchers.is(AuthorizationMatcher.matchAuthorization(authorizationFeature)))));

    }

    @Test
    public void downloadOfBitstreamBAsEperson() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamB, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void downloadOfBitstreamWithCrisSecurity() throws Exception {

        //by default has no authority
        choiceAuthorityService.getChoiceAuthoritiesNames();
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                                         new String[] {
                                             "org.dspace.content.authority.OrcidAuthority = AuthorAuthority"
                                         });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        // this should be a publication collection but right now no control are enforced
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();
        // this should be a person collection
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Person").build();

        //2. A public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";
        EPerson authorEp = EPersonBuilder.createEPerson(context)
                                         .withEmail("author@example.com")
                                         .withPassword(password)
                                         .build();
        Item profile = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author")
                                  .withDspaceObjectOwner(authorEp)
                                  .build();
        // set our submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password)
                                          .build();
        context.setCurrentUser(submitter);
        Bitstream embargoedBit = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            // we need a publication to check our cris enhanced security
            Item publicItem1 =
                ItemBuilder.createItem(context, col1)
                           .withEntityType("Publication")
                           .withTitle("Public item 1")
                           .withIssueDate("2017-10-17")
                           .withAuthor("Just an author without profile")
                           .withAuthor("A profile not longer in the system",
                                       UUID.randomUUID().toString())
                           .withAuthor("An author with invalid authority",
                                       "this is not an uuid")
                           .withAuthor("Author",
                                       profile.getID().toString())
                           .build();

            embargoedBit = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod(Period.ofMonths(6))
                .build();
        }
        context.restoreAuthSystemState();

        BitstreamRest bitstreamRest = bitstreamConverter.convert(embargoedBit, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        //** WHEN **
        //anonymous try to download the bitstream
        getClient()
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", bitstreamUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());

        // another unrelated eperson should get forbidden
        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", bitstreamUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());

        Authorization authorizationFeature = new Authorization(submitter, downloadFeature, bitstreamRest);
        // the submitter should be able to download according to our custom cris policy
        getClient(getAuthToken(submitter.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", bitstreamUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                Matchers.is(AuthorizationMatcher.matchAuthorization(authorizationFeature)))));

        authorizationFeature = new Authorization(authorEp, downloadFeature, bitstreamRest);
        // the author should be able to download according to our custom cris policy
        getClient(getAuthToken(authorEp.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", bitstreamUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                .andExpect(jsonPath("$._embedded.authorizations", contains(
                        Matchers.is(AuthorizationMatcher.matchAuthorization(authorizationFeature)))));
    }

    @Test
    public void downloadOfBitstreamWithCrisSecurityDifferentBundles() throws Exception {
        //by default has no authority
        choiceAuthorityService.getChoiceAuthoritiesNames();
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                                         new String[] {
                                             "org.dspace.content.authority.OrcidAuthority = AuthorAuthority"
                                         });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Person").build();

        EPerson authorEp = EPersonBuilder.createEPerson(context)
                .withEmail("author@example.com")
                .withPassword(password)
                .build();
        Item profile = ItemBuilder.createItem(context, col2)
                .withTitle("Author")
                .withDspaceObjectOwner(authorEp)
                .build();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();
        context.setCurrentUser(submitter);

        // Create Publication with bitstreams in different bundles
        Item publicItem = ItemBuilder.createItem(context, col1)
                .withEntityType("Publication")
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Author", profile.getID().toString())
                .build();

        // Bitstream in ORIGINAL bundle (should be allowed)
        Bitstream originalBitstream = null;
        try (InputStream is = IOUtils.toInputStream("Original content", CharEncoding.UTF_8)) {
            originalBitstream = BitstreamBuilder
                .createBitstream(context, publicItem, is)
                .withName("Original File")
                .withMimeType("text/plain")
                .withEmbargoPeriod(Period.ofMonths(6))
                .build();
        }

        // Bitstream in LICENSE bundle (should be denied per configuration)
        Bitstream licenseBitstream = null;
        try (InputStream is = IOUtils.toInputStream("License text", CharEncoding.UTF_8)) {
            licenseBitstream = BitstreamBuilder
                .createBitstream(context, publicItem, is, "LICENSE")
                .withName("License File")
                .withMimeType("text/plain")
                .withEmbargoPeriod(Period.ofMonths(6))
                .build();
        }

        context.restoreAuthSystemState();

        BitstreamRest originalRest = bitstreamConverter.convert(originalBitstream, Projection.DEFAULT);
        BitstreamRest licenseRest = bitstreamConverter.convert(licenseBitstream, Projection.DEFAULT);
        String originalUri = utils.linkToSingleResource(originalRest, "self").getHref();
        String licenseUri = utils.linkToSingleResource(licenseRest, "self").getHref();

        //** WHEN/THEN **
        // Author can download from ORIGINAL bundle (configured as allowed)
        Authorization authOriginal = new Authorization(authorEp, downloadFeature, originalRest);
        getClient(getAuthToken(authorEp.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", originalUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(authOriginal)))));

        // Author CANNOT download from LICENSE bundle (not in allowed bundles list)
        getClient(getAuthToken(authorEp.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", licenseUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void downloadOfBitstreamWithCrisSecurityNonPublicationEntity() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Dataset Collection").build();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();
        context.setCurrentUser(submitter);

        // Create Dataset (not Publication - no CRIS policies configured)
        Item datasetItem = ItemBuilder.createItem(context, col1)
                .withEntityType("Dataset")
                .withTitle("Test Dataset")
                .withIssueDate("2017-10-17")
                .build();

        Bitstream datasetBitstream = null;
        try (InputStream is = IOUtils.toInputStream("Dataset content", CharEncoding.UTF_8)) {
            datasetBitstream = BitstreamBuilder
                .createBitstream(context, datasetItem, is)
                .withName("Dataset File")
                .withMimeType("text/plain")
                .withEmbargoPeriod(Period.ofMonths(6))
                .build();
        }

        context.restoreAuthSystemState();

        BitstreamRest bitstreamRest = bitstreamConverter.convert(datasetBitstream, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        //** WHEN/THEN **
        // Submitter CANNOT download Dataset bitstream (no CRIS policy configured for Dataset entity)
        // Falls back to standard ACL, which denies access due to embargo
        getClient(getAuthToken(submitter.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", bitstreamUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void downloadOfBitstreamWithCrisSecurityInvalidAuthorAuthority() throws Exception {
        //by default has no authority
        choiceAuthorityService.getChoiceAuthoritiesNames();
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                                         new String[] {
                                             "org.dspace.content.authority.OrcidAuthority = AuthorAuthority"
                                         });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();

        EPerson actualAuthor = EPersonBuilder.createEPerson(context)
                .withEmail("actualauthor@example.com")
                .withPassword(password)
                .build();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();
        context.setCurrentUser(submitter);

        // Create Publication with INVALID author authorities (not UUIDs)
        Item publicItem = ItemBuilder.createItem(context, col1)
                .withEntityType("Publication")
                .withTitle("Publication with invalid authorities")
                .withIssueDate("2017-10-17")
                .withAuthor("Invalid Authority", "not-a-uuid")
                .withAuthor("Missing Authority")  // No authority at all
                .withAuthor("Empty Authority", "")
                .build();

        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream("Content", CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem, is)
                .withName("Test File")
                .withMimeType("text/plain")
                .withEmbargoPeriod(Period.ofMonths(6))
                .build();
        }

        context.restoreAuthSystemState();

        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstream, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        //** WHEN/THEN **
        // actualAuthor CANNOT download (not listed as author in metadata)
        getClient(getAuthToken(actualAuthor.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", bitstreamUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());

        // Submitter CAN download (SUBMITTER policy applies)
        Authorization authFeature = new Authorization(submitter, downloadFeature, bitstreamRest);
        getClient(getAuthToken(submitter.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", bitstreamUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(authFeature)))));
    }

    @Test
    public void downloadOfBitstreamWithCrisSecurityMultipleBundles() throws Exception {
        //by default has no authority
        choiceAuthorityService.getChoiceAuthoritiesNames();
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                                         new String[] {
                                             "org.dspace.content.authority.OrcidAuthority = AuthorAuthority"
                                         });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Person").build();

        EPerson authorEp = EPersonBuilder.createEPerson(context)
                .withEmail("author@example.com")
                .withPassword(password)
                .build();
        Item profile = ItemBuilder.createItem(context, col2)
                .withTitle("Author")
                .withDspaceObjectOwner(authorEp)
                .build();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();
        context.setCurrentUser(submitter);

        Item publicItem = ItemBuilder.createItem(context, col1)
                .withEntityType("Publication")
                .withTitle("Publication with multiple bundles")
                .withIssueDate("2017-10-17")
                .withAuthor("Author", profile.getID().toString())
                .build();

        // Create bitstreams in different bundles
        Bitstream originalBit = null;
        try (InputStream is = IOUtils.toInputStream("Original", CharEncoding.UTF_8)) {
            originalBit = BitstreamBuilder
                .createBitstream(context, publicItem, is, "ORIGINAL")
                .withName("Original.txt")
                .withMimeType("text/plain")
                .withEmbargoPeriod(Period.ofMonths(6))
                .build();
        }

        Bitstream thumbnailBit = null;
        try (InputStream is = IOUtils.toInputStream("Thumbnail", CharEncoding.UTF_8)) {
            thumbnailBit = BitstreamBuilder
                .createBitstream(context, publicItem, is, "THUMBNAIL")
                .withName("Thumbnail.jpg")
                .withMimeType("image/jpeg")
                .withEmbargoPeriod(Period.ofMonths(6))
                .build();
        }

        Bitstream textBit = null;
        try (InputStream is = IOUtils.toInputStream("Text", CharEncoding.UTF_8)) {
            textBit = BitstreamBuilder
                .createBitstream(context, publicItem, is, "TEXT")
                .withName("Text.txt")
                .withMimeType("text/plain")
                .withEmbargoPeriod(Period.ofMonths(6))
                .build();
        }

        context.restoreAuthSystemState();

        BitstreamRest originalRest = bitstreamConverter.convert(originalBit, Projection.DEFAULT);
        BitstreamRest thumbnailRest = bitstreamConverter.convert(thumbnailBit, Projection.DEFAULT);
        BitstreamRest textRest = bitstreamConverter.convert(textBit, Projection.DEFAULT);

        String originalUri = utils.linkToSingleResource(originalRest, "self").getHref();
        String thumbnailUri = utils.linkToSingleResource(thumbnailRest, "self").getHref();
        String textUri = utils.linkToSingleResource(textRest, "self").getHref();

        //** WHEN/THEN **
        // Author can download from ORIGINAL bundle (in allowed list)
        Authorization authOriginal = new Authorization(authorEp, downloadFeature, originalRest);
        getClient(getAuthToken(authorEp.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", originalUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(authOriginal)))));

        // Author CANNOT download from THUMBNAIL bundle (not in allowed list)
        getClient(getAuthToken(authorEp.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", thumbnailUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());

        // Author CANNOT download from TEXT bundle (not in allowed list)
        getClient(getAuthToken(authorEp.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", textUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());

        // Submitter CAN download from ORIGINAL (in allowed list)
        Authorization authSubmitter = new Authorization(submitter, downloadFeature, originalRest);
        getClient(getAuthToken(submitter.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", originalUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(authSubmitter)))));

        // Submitter CANNOT download from THUMBNAIL (not in allowed list)
        getClient(getAuthToken(submitter.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", thumbnailUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void downloadOfBitstreamWithCrisSecurityMultipleAuthors() throws Exception {

        //by default has no authority
        choiceAuthorityService.getChoiceAuthoritiesNames();
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                                         new String[] {
                                             "org.dspace.content.authority.OrcidAuthority = EditorAuthority",
                                             "org.dspace.content.authority.OrcidAuthority = AuthorAuthority"
                                         });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.EditorAuthority.entityType", "Person");
        configurationService.setProperty("choices.plugin.dc.contributor.editor", "EditorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.editor", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.editor", "true");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Person").build();

        // Create multiple author profiles
        EPerson author1 = EPersonBuilder.createEPerson(context)
                .withEmail("author1@example.com")
                .withPassword(password)
                .build();
        Item profile1 = ItemBuilder.createItem(context, col2)
                .withTitle("Author One")
                .withDspaceObjectOwner(author1)
                .build();

        EPerson author2 = EPersonBuilder.createEPerson(context)
                .withEmail("author2@example.com")
                .withPassword(password)
                .build();
        Item profile2 = ItemBuilder.createItem(context, col2)
                .withTitle("Author Two")
                .withDspaceObjectOwner(author2)
                .build();

        EPerson editor = EPersonBuilder.createEPerson(context)
                .withEmail("editor@example.com")
                .withPassword(password)
                .build();
        Item editorProfile = ItemBuilder.createItem(context, col2)
                .withTitle("Editor")
                .withDspaceObjectOwner(editor)
                .build();

        EPerson nonContributor = EPersonBuilder.createEPerson(context)
                .withEmail("noncontributor@example.com")
                .withPassword(password)
                .build();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();
        context.setCurrentUser(submitter);

        // Create Publication with multiple authors and editor
        Item publicItem = ItemBuilder.createItem(context, col1)
                .withEntityType("Publication")
                .withTitle("Multi-author Publication")
                .withIssueDate("2017-10-17")
                .withAuthor("Author One", profile1.getID().toString())
                .withAuthor("Author Two", profile2.getID().toString())
                .withAuthor("Non-linked Author")  // No EPerson link
                .withEditor("Editor", editorProfile.getID().toString())
                .build();

        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream("Content", CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem, is)
                .withName("Publication File")
                .withMimeType("text/plain")
                .withEmbargoPeriod(Period.ofMonths(6))
                .build();
        }

        context.restoreAuthSystemState();

        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstream, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        //** WHEN/THEN **
        // Author1 CAN download (listed in dc.contributor.author)
        Authorization auth1 = new Authorization(author1, downloadFeature, bitstreamRest);
        getClient(getAuthToken(author1.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", bitstreamUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(auth1)))));

        // Author2 CAN download (listed in dc.contributor.author)
        Authorization auth2 = new Authorization(author2, downloadFeature, bitstreamRest);
        getClient(getAuthToken(author2.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", bitstreamUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(auth2)))));

        // Editor CAN download (listed in dc.contributor.editor)
        Authorization authEditor = new Authorization(editor, downloadFeature, bitstreamRest);
        getClient(getAuthToken(editor.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", bitstreamUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(authEditor)))));

        // NonContributor CANNOT download (not linked to item in any way)
        getClient(getAuthToken(nonContributor.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", bitstreamUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());

        // Submitter CAN download (SUBMITTER policy applies)
        Authorization authSubmitter = new Authorization(submitter, downloadFeature, bitstreamRest);
        getClient(getAuthToken(submitter.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", bitstreamUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(authSubmitter)))));
    }

    @Test
    public void downloadOfBitstreamWithCrisSecurityNoEmbargo() throws Exception {

        //by default has no authority
        choiceAuthorityService.getChoiceAuthoritiesNames();
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                                         new String[] {
                                             "org.dspace.content.authority.OrcidAuthority = AuthorAuthority"
                                         });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Person").build();

        EPerson authorEp = EPersonBuilder.createEPerson(context)
                .withEmail("author@example.com")
                .withPassword(password)
                .build();
        Item profile = ItemBuilder.createItem(context, col2)
                .withTitle("Author")
                .withDspaceObjectOwner(authorEp)
                .build();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();
        context.setCurrentUser(submitter);

        // Create Publication with PUBLIC bitstream (no embargo)
        Item publicItem = ItemBuilder.createItem(context, col1)
                .withEntityType("Publication")
                .withTitle("Public Publication")
                .withIssueDate("2017-10-17")
                .withAuthor("Author", profile.getID().toString())
                .build();

        Bitstream publicBitstream = null;
        try (InputStream is = IOUtils.toInputStream("Public content", CharEncoding.UTF_8)) {
            publicBitstream = BitstreamBuilder
                .createBitstream(context, publicItem, is)
                .withName("Public File")
                .withMimeType("text/plain")
                // No embargo - publicly accessible
                .build();
        }

        context.restoreAuthSystemState();

        BitstreamRest bitstreamRest = bitstreamConverter.convert(publicBitstream, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        //** WHEN/THEN **
        // Anonymous CAN download (standard ACL allows - no embargo)
        getClient()
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", bitstreamUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));

        // Unrelated eperson CAN download (standard ACL allows)
        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", bitstreamUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));

        // Author CAN download (both ACL and CRIS policy allow)
        Authorization authAuthor = new Authorization(authorEp, downloadFeature, bitstreamRest);
        getClient(getAuthToken(authorEp.getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                         .param("uri", bitstreamUri)
                         .param("feature", downloadFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(authAuthor)))));
    }

}
