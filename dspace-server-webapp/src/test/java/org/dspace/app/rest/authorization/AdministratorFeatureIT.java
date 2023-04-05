/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;

import org.dspace.app.rest.authorization.impl.AdministratorOfFeature;
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.SiteService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.GroupService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test suite for the administrator feature
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 *
 */
public class AdministratorFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;
    @Autowired
    GroupService groupService;
    @Autowired
    AuthorizeService authService;
    @Autowired
    CommunityService communityService;
    @Autowired
    private ItemConverter itemConverter;
    @Autowired
    private CommunityConverter communityConverter;
    @Autowired
    private CollectionConverter collectionConverter;
    @Autowired
    private SiteConverter siteConverter;

    private SiteService siteService;

    private EPerson adminComA;
    private EPerson adminComB;
    private EPerson adminColA;
    private EPerson adminColB;
    private EPerson adminItemA;
    private EPerson adminItemB;

    private Community communityA;
    private Community subCommunityOfA;
    private Community communityB;
    private Collection collectionA;
    private Collection collectionB;
    private Item itemInCollectionA;
    private Item itemInCollectionB;


    /** 
     * this hold a reference to the test feature {@link AdministratorOfFeature}
     */
    private AuthorizationFeature administratorFeature;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        siteService = ContentServiceFactory.getInstance().getSiteService();
        administratorFeature = authorizationFeatureService.find(AdministratorOfFeature.NAME);
        initAdminsAndObjects();
    }

    private void initAdminsAndObjects() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();


        adminComA = EPersonBuilder.createEPerson(context)
                                  .withEmail("adminComA@example.com")
                                  .withPassword(password)
                                  .build();

        adminComB = EPersonBuilder.createEPerson(context)
                                  .withEmail("adminComB@example.com")
                                  .withPassword(password)
                                  .build();

        adminColA = EPersonBuilder.createEPerson(context)
                                  .withEmail("adminColA@example.com")
                                  .withPassword(password)
                                  .build();

        adminColB = EPersonBuilder.createEPerson(context)
                                  .withEmail("adminColB@example.com")
                                  .withPassword(password)
                                  .build();

        adminItemA = EPersonBuilder.createEPerson(context)
                                   .withEmail("adminItemA@example.com")
                                   .withPassword(password)
                                   .build();

        adminItemB = EPersonBuilder.createEPerson(context)
                                   .withEmail("adminItemB@example.com")
                                   .withPassword(password)
                                   .build();

        communityA = CommunityBuilder.createCommunity(context)
                                     .withName("Community A")
                                     .withAdminGroup(adminComA)
                                     .build();

        subCommunityOfA = CommunityBuilder.createSubCommunity(context, communityA)
                                          .withName("Sub Community of CommunityA")
                                          .build();

        communityB = CommunityBuilder.createCommunity(context)
                                     .withName("Community B")
                                     .withAdminGroup(adminComB)
                                     .build();

        collectionA = CollectionBuilder.createCollection(context, subCommunityOfA)
                                       .withName("Collection A")
                                       .withAdminGroup(adminColA)
                                       .build();

        collectionB = CollectionBuilder.createCollection(context, communityB)
                                       .withName("Collection B")
                                       .withAdminGroup(adminColB)
                                       .build();

        itemInCollectionA = ItemBuilder.createItem(context, collectionA)
                                       .withTitle("Item in Collection A")
                                       .withAdminUser(adminItemA)
                                       .build();

        itemInCollectionB = ItemBuilder.createItem(context, collectionB)
                                       .withTitle("Item in Collection B")
                                       .withAdminUser(adminItemB)
                                       .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void communityWithAdministratorFeatureTest() throws Exception {
        CommunityRest communityRestA = communityConverter.convert(communityA, DefaultProjection.DEFAULT);
        CommunityRest communityRestB = communityConverter.convert(communityB, DefaultProjection.DEFAULT);
        CommunityRest SubCommunityOfARest = communityConverter.convert(subCommunityOfA, DefaultProjection.DEFAULT);

        // tokens
        String tokenAdminComA = getAuthToken(adminComA.getEmail(), password);
        String tokenAdminComB = getAuthToken(adminComB.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        // define authorizations that we know must exists
        Authorization authAdminSiteComA = new Authorization(admin, administratorFeature, communityRestA);
        Authorization authAdminComAComA = new Authorization(adminComA, administratorFeature, communityRestA);
        Authorization authAdminComASubComA = new Authorization(adminComA, administratorFeature, SubCommunityOfARest);
        Authorization authAdminComBComB = new Authorization(adminComB, administratorFeature, communityRestB);


        // define authorizations that we know not exists
        Authorization authAdminComBComA = new Authorization(adminComB, administratorFeature, communityRestA);
        Authorization authAdminComBSubComA = new Authorization(adminComB, administratorFeature, SubCommunityOfARest);
        Authorization authAdminColAComA = new Authorization(adminColA, administratorFeature, communityRestA);
        Authorization authAdminItemAComA = new Authorization(adminItemA, administratorFeature, communityRestA);
        Authorization authEPersonComA = new Authorization(eperson, administratorFeature, communityRestA);
        Authorization authAnonymousComA = new Authorization(null, administratorFeature, communityRestA);



        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminSiteComA.getID()))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminSiteComA))));

        getClient(tokenAdminComA).perform(get("/api/authz/authorizations/" + authAdminComAComA.getID()))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher
                                           .matchAuthorization(authAdminComAComA))));

        getClient(tokenAdminComA).perform(get("/api/authz/authorizations/" + authAdminComASubComA.getID()))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminComASubComA))));

        getClient(tokenAdminComB).perform(get("/api/authz/authorizations/" + authAdminComBComB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminComBComB))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminComBComA.getID()))
                  .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminComBSubComA.getID()))
                  .andExpect(status().isNotFound());
        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminColAComA.getID()))
                                 .andExpect(status().isNotFound());
        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminItemAComA.getID()))
                                 .andExpect(status().isNotFound());
        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authEPersonComA.getID()))
                                 .andExpect(status().isNotFound());
        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAnonymousComA.getID()))
                                 .andExpect(status().isNotFound());


    }

    @Test
    public void collectionWithAdministratorFeatureTest() throws Exception {
        CollectionRest collectionRestA = collectionConverter.convert(collectionA, DefaultProjection.DEFAULT);
        CollectionRest collectionRestB = collectionConverter.convert(collectionB, DefaultProjection.DEFAULT);

        String tokenAdminColA = getAuthToken(adminColA.getEmail(), password);
        String tokenAdminColB = getAuthToken(adminColB.getEmail(), password);
        String tokenAdminComA = getAuthToken(adminComA.getEmail(), password);
        String tokenAdminComB = getAuthToken(adminComB.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        // define authorizations that we know must exists

        Authorization authAdminSiteColA = new Authorization(admin, administratorFeature, collectionRestA);
        Authorization authAdminComAColA = new Authorization(adminComA, administratorFeature, collectionRestA);
        Authorization authAdminColAColA = new Authorization(adminColA, administratorFeature, collectionRestA);

        Authorization authAdminSiteColB = new Authorization(admin, administratorFeature, collectionRestB);
        Authorization authAdminComBColB = new Authorization(adminComB, administratorFeature, collectionRestB);
        Authorization authAdminColBColB = new Authorization(adminColB, administratorFeature, collectionRestB);

        // define authorization that we know not exists
        Authorization authAdminColBColA = new Authorization(adminColB, administratorFeature, collectionRestA);
        Authorization authAdminComBColA = new Authorization(adminComB, administratorFeature, collectionRestA);
        Authorization authAdminItemAColA = new Authorization(adminItemA, administratorFeature, collectionRestA);
        Authorization authEPersonColA = new Authorization(eperson, administratorFeature, collectionRestA);
        Authorization authAnonymousColA = new Authorization(null, administratorFeature, collectionRestA);



        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminSiteColA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(
                                     AuthorizationMatcher.matchAuthorization(authAdminSiteColA))));

        getClient(tokenAdminComA).perform(get("/api/authz/authorizations/" + authAdminComAColA.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.is(
                                         AuthorizationMatcher.matchAuthorization(authAdminComAColA))));

        getClient(tokenAdminColA).perform(get("/api/authz/authorizations/" + authAdminColAColA.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.is(
                                         AuthorizationMatcher.matchAuthorization(authAdminColAColA))));


        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminSiteColB.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(
                                     AuthorizationMatcher.matchAuthorization(authAdminSiteColB))));

        getClient(tokenAdminComB).perform(get("/api/authz/authorizations/" + authAdminComBColB.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.is(
                                         AuthorizationMatcher.matchAuthorization(authAdminComBColB))));

        getClient(tokenAdminColB).perform(get("/api/authz/authorizations/" + authAdminColBColB.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.is(
                                         AuthorizationMatcher.matchAuthorization(authAdminColBColB))));


        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminColBColA.getID()))
                             .andExpect(status().isNotFound());
        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminComBColA.getID()))
                             .andExpect(status().isNotFound());
        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminItemAColA.getID()))
                             .andExpect(status().isNotFound());
        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authEPersonColA.getID()))
                             .andExpect(status().isNotFound());
        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAnonymousColA.getID()))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void siteWithAdministratorFeatureTest() throws Exception {

        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, DefaultProjection.DEFAULT);

        // tokens
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        // define authorizations of Admin that we know must exists
        Authorization authAdminSite = new Authorization(admin, administratorFeature, siteRest);

        // define authorizations of EPerson that we know not exists
        Authorization authAdminComASite = new Authorization(adminComA, administratorFeature, siteRest);
        Authorization authAdminColASite = new Authorization(adminColA, administratorFeature, siteRest);
        Authorization authAdminItemASite = new Authorization(adminItemA, administratorFeature, siteRest);
        Authorization authEPersonSite = new Authorization(eperson, administratorFeature, siteRest);
        Authorization authAnonymousSite = new Authorization(null, administratorFeature, siteRest);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminSite.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminSite))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authEPersonSite.getID()))
                    .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminComASite.getID()))
                    .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminColASite.getID()))
                    .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminItemASite.getID()))
                   .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authEPersonSite.getID()))
                   .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAnonymousSite.getID()))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void itemWithAdministratorFeatureTest() throws Exception {

        ItemRest itemRestA = itemConverter.convert(itemInCollectionA, DefaultProjection.DEFAULT);
        ItemRest itemRestB = itemConverter.convert(itemInCollectionB, DefaultProjection.DEFAULT);

        String tokenAdminItemA = getAuthToken(adminItemA.getEmail(), password);
        String tokenAdminItemB = getAuthToken(adminItemB.getEmail(), password);
        String tokenAdminColA = getAuthToken(adminColA.getEmail(), password);
        String tokenAdminColB = getAuthToken(adminColB.getEmail(), password);
        String tokenAdminComA = getAuthToken(adminComA.getEmail(), password);
        String tokenAdminComB = getAuthToken(adminComB.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        // define authorizations that we know must exists

        Authorization authAdminSiteItemA = new Authorization(admin, administratorFeature, itemRestA);
        Authorization authAdminComAItemA = new Authorization(adminComA, administratorFeature, itemRestA);
        Authorization authAdminColAItemA = new Authorization(adminColA, administratorFeature, itemRestA);
        Authorization authAdminItemAItemA = new Authorization(adminItemA, administratorFeature, itemRestA);

        Authorization authAdminSiteItemB = new Authorization(admin, administratorFeature, itemRestB);
        Authorization authAdminComBItemB = new Authorization(adminComB, administratorFeature, itemRestB);
        Authorization authAdminColBItemB = new Authorization(adminColB, administratorFeature, itemRestB);
        Authorization authAdminItemBItemB = new Authorization(adminItemB, administratorFeature, itemRestB);


        // define authorization that we know not exists
        Authorization authAdminComBItemA = new Authorization(adminComB, administratorFeature, itemRestA);
        Authorization authAdminColBItemA = new Authorization(adminColB, administratorFeature, itemRestA);
        Authorization authAdminItemBItemA = new Authorization(adminItemB, administratorFeature, itemRestA);
        Authorization authEPersonItemA = new Authorization(eperson, administratorFeature, itemRestA);
        Authorization authAnonymousItemA = new Authorization(null, administratorFeature, itemRestA);



        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminSiteItemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(
                                     AuthorizationMatcher.matchAuthorization(authAdminSiteItemA))));

        getClient(tokenAdminComA).perform(get("/api/authz/authorizations/" + authAdminComAItemA.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.is(
                                         AuthorizationMatcher.matchAuthorization(authAdminComAItemA))));

        getClient(tokenAdminColA).perform(get("/api/authz/authorizations/" + authAdminColAItemA.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.is(
                                         AuthorizationMatcher.matchAuthorization(authAdminColAItemA))));

        getClient(tokenAdminItemA).perform(get("/api/authz/authorizations/" + authAdminItemAItemA.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.is(
                                         AuthorizationMatcher.matchAuthorization(authAdminItemAItemA))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminSiteItemB.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(
                                     AuthorizationMatcher.matchAuthorization(authAdminSiteItemB))));

        getClient(tokenAdminComB).perform(get("/api/authz/authorizations/" + authAdminComBItemB.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.is(
                                         AuthorizationMatcher.matchAuthorization(authAdminComBItemB))));

        getClient(tokenAdminColB).perform(get("/api/authz/authorizations/" + authAdminColBItemB.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.is(
                                         AuthorizationMatcher.matchAuthorization(authAdminColBItemB))));

        getClient(tokenAdminItemB).perform(get("/api/authz/authorizations/" + authAdminItemBItemB.getID()))
                                  .andExpect(status().isOk())
                                  .andExpect(jsonPath("$", Matchers.is(
                                          AuthorizationMatcher.matchAuthorization(authAdminItemBItemB))));



        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminComBItemA.getID()))
                             .andExpect(status().isNotFound());
        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminColBItemA.getID()))
                             .andExpect(status().isNotFound());
        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminItemBItemA.getID()))
                             .andExpect(status().isNotFound());
        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authEPersonItemA.getID()))
                             .andExpect(status().isNotFound());
        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAnonymousItemA.getID()))
                             .andExpect(status().isNotFound());
    }
}
