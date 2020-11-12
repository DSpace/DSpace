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

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.authorization.impl.RequestCopyFeature;
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
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RequestCopyFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    ResourcePolicyService resourcePolicyService;

    @Autowired
    private CollectionConverter collectionConverter;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private BitstreamConverter bitstreamConverter;

    @Autowired
    private ConfigurationService configurationService;


    @Autowired
    private Utils utils;


    private AuthorizationFeature requestCopyFeature;

    private Collection collectionA;
    private Item itemA;
    private Bitstream bitstreamA;
    private Bitstream bitstreamB;

    private Item itemInWorkSpace;
    private Bitstream bitstreamFromWorkSpaceItem;

    private Bitstream bitstreamFromCollection;


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        configurationService.setProperty("request.item.type", "all");

        context.turnOffAuthorisationSystem();
        requestCopyFeature = authorizationFeatureService.find(RequestCopyFeature.NAME);

        String bitstreamContent = "Dummy content";

        Community communityA = CommunityBuilder.createCommunity(context).build();
        collectionA = CollectionBuilder.createCollection(context, communityA).withLogo("Blub").build();
        bitstreamFromCollection = collectionA.getLogo();

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
            WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collectionA)
                                                              .withFulltext("Test", "source", is)
                                                              .build();
            itemInWorkSpace = workspaceItem.getItem();
            bitstreamFromWorkSpaceItem = itemInWorkSpace.getBundles("ORIGINAL").get(0).getBitstreams().get(0);
        }
        resourcePolicyService.removePolicies(context, bitstreamB, Constants.READ);


        context.restoreAuthSystemState();
    }


    @Test
    public void requestCopyOnCollectionAAsAdmin() throws Exception {
        CollectionRest collectionRest = collectionConverter.convert(collectionA, Projection.DEFAULT);
        String collectionUri = utils.linkToSingleResource(collectionRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", collectionUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void requestCopyOnItemAAsAdmin() throws Exception {
        ItemRest itemRest = itemConverter.convert(itemA, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", itemUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    @Test
    public void requestCopyOnItemInWorkSpaceAsAdmin() throws Exception {
        ItemRest itemRest = itemConverter.convert(itemInWorkSpace, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", itemUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    @Test
    public void requestCopyOnBitstreamAAsAdmin() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamA, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    @Test
    public void requestCopyOnBitstreamBAsAdmin() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamB, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    @Test
    public void requestCopyOnBitstreamFromWorkSpaceItemAsAdmin() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamFromWorkSpaceItem, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    @Test
    public void requestCopyOnBitstreamFromCollectionAsAdmin() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamFromCollection, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }


    // Tests for anonymous user
    @Test
    public void requestCopyOnCollectionAAsAnonymous() throws Exception {
        CollectionRest collectionRest = collectionConverter.convert(collectionA, Projection.DEFAULT);
        String collectionUri = utils.linkToSingleResource(collectionRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", collectionUri)
                                    .param("feature", requestCopyFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void requestCopyOnItemAAsAnonymous() throws Exception {
        ItemRest itemRest = itemConverter.convert(itemA, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();
        Authorization authorizationFeature = new Authorization(null, requestCopyFeature, itemRest);


        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", itemUri)
                                    .param("feature", requestCopyFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                   .andExpect(jsonPath("$._embedded.authorizations", contains(
                           Matchers.is(AuthorizationMatcher.matchAuthorization(authorizationFeature)))));
    }

    @Test
    public void requestCopyOnItemInWorkSpaceAsAnonymous() throws Exception {
        ItemRest itemRest = itemConverter.convert(itemInWorkSpace, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", itemUri)
                                    .param("feature", requestCopyFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    @Test
    public void requestCopyOnBitstreamAAsAnonymous() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamA, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", bitstreamUri)
                                    .param("feature", requestCopyFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    @Test
    public void requestCopyOnBitstreamBAsAnonymous() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamB, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();
        Authorization authorizationFeature = new Authorization(null, requestCopyFeature, bitstreamRest);


        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", bitstreamUri)
                                    .param("feature", requestCopyFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                   .andExpect(jsonPath("$._embedded.authorizations", contains(
                           Matchers.is(AuthorizationMatcher.matchAuthorization(authorizationFeature))))
                   );
    }

    @Test
    public void requestCopyOnBitstreamFromWorkSpaceItemAsAnonymous() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamFromWorkSpaceItem, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", bitstreamUri)
                                    .param("feature", requestCopyFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    @Test
    public void requestCopyOnBitstreamFromCollectionAsAnonymous() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamFromCollection, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", bitstreamUri)
                                    .param("feature", requestCopyFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    // Test for Eperson
    @Test
    public void requestCopyOnCollectionAAsEperson() throws Exception {
        CollectionRest collectionRest = collectionConverter.convert(collectionA, Projection.DEFAULT);
        String collectionUri = utils.linkToSingleResource(collectionRest, "self").getHref();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", collectionUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void requestCopyOnItemAAsEperson() throws Exception {
        ItemRest itemRest = itemConverter.convert(itemA, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();
        Authorization authorizationFeature = new Authorization(eperson, requestCopyFeature, itemRest);

        String token = getAuthToken(eperson.getEmail(), password);


        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", itemUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                        .andExpect(jsonPath("$._embedded.authorizations", contains(
                                Matchers.is(AuthorizationMatcher.matchAuthorization(authorizationFeature)))));
    }

    @Test
    public void requestCopyOnItemInWorkSpaceAsEperson() throws Exception {
        ItemRest itemRest = itemConverter.convert(itemInWorkSpace, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();

        String token = getAuthToken(eperson.getEmail(), password);


        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", itemUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    @Test
    public void requestCopyOnBitstreamAAsEperson() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamA, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        String token = getAuthToken(eperson.getEmail(), password);


        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    @Test
    public void requestCopyOnBitstreamBAsEperson() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamB, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();
        Authorization authorizationFeature = new Authorization(eperson, requestCopyFeature, bitstreamRest);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                        .andExpect(jsonPath("$._embedded.authorizations", contains(
                                Matchers.is(AuthorizationMatcher.matchAuthorization(authorizationFeature))))
                        );
    }

    @Test
    public void requestCopyOnBitstreamFromWorkSpaceItemAsEperson() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamFromWorkSpaceItem, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    @Test
    public void requestCopyOnBitstreamFromCollectionAsEperson() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamFromCollection, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    public void requestACopyItemTypeLoggedAsAnonymous() throws Exception {
        configurationService.setProperty("request.item.type", "logged");

        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamB, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", bitstreamUri)
                                    .param("feature", requestCopyFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void requestACopyItemTypeLoggedAsEperson() throws Exception {
        configurationService.setProperty("request.item.type", "logged");

        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamB, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();
        Authorization authorizationFeature = new Authorization(eperson, requestCopyFeature, bitstreamRest);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                        .andExpect(jsonPath("$._embedded.authorizations", contains(
                                Matchers.is(AuthorizationMatcher.matchAuthorization(authorizationFeature))))
                        );
    }

    public void requestACopyItemTypeEmptyAsAnonymous() throws Exception {
        configurationService.setProperty("request.item.type", "");

        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamB, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", bitstreamUri)
                                    .param("feature", requestCopyFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    public void requestACopyItemTypeEmptyAsEperson() throws Exception {
        configurationService.setProperty("request.item.type", "");

        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamB, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    public void requestACopyItemTypeBogusValueAsAnonymous() throws Exception {
        configurationService.setProperty("request.item.type", "invalid value");

        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamB, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", bitstreamUri)
                                    .param("feature", requestCopyFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    public void requestACopyItemTypeBogusValueAsEperson() throws Exception {
        configurationService.setProperty("request.item.type", "invalid value");

        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamB, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", requestCopyFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }
}
