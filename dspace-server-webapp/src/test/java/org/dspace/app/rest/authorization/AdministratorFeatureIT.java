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

import org.dspace.app.rest.authorization.impl.AdministratorOfFeature;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
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
    private CommunityConverter communityConverter;
    @Autowired
    private CollectionConverter collectionConverter;
    @Autowired
    private SiteConverter siteConverter;

    private SiteService siteService;

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
    }

    @Test
    public void communityWithAdministratorFeatureTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson adminComA = EPersonBuilder.createEPerson(context)
                           .withEmail("adminComA@example.com")
                           .withPassword(password)
                           .build();

        EPerson adminComB = EPersonBuilder.createEPerson(context)
                           .withEmail("adminComB@example.com")
                           .withPassword(password)
                           .build();

        Community communityA = CommunityBuilder.createCommunity(context)
                              .withName("Community A")
                              .withAdminGroup(adminComA)
                              .build();

        Community subCommunityOfA = CommunityBuilder.createSubCommunity(context, communityA)
                                   .withName("Sub Community of CommunityA")
                                   .build();

        Collection collectionOfSubComm = CollectionBuilder.createCollection(context, subCommunityOfA)
                                        .withName("Collection of subCommunity")
                                        .build();

        Community communityB = CommunityBuilder.createCommunity(context)
                              .withName("Community B")
                              .withAdminGroup(adminComB)
                              .build();

        context.restoreAuthSystemState();

        CommunityRest communityRestA = communityConverter.convert(communityA, DefaultProjection.DEFAULT);
        CommunityRest SubCommunityOfArest = communityConverter.convert(subCommunityOfA, DefaultProjection.DEFAULT);
        CollectionRest collectionRestOfSubComm = collectionConverter.convert(collectionOfSubComm,
                                                                             DefaultProjection.DEFAULT);

        // tokens
        String tokenAdminComA = getAuthToken(adminComA.getEmail(), password);
        String tokenAdminComB = getAuthToken(adminComB.getEmail(), password);

        // define authorizations that we know must exists
        Authorization authAdminCommunityA = new Authorization(adminComA, administratorFeature, communityRestA);
        Authorization authAdminSubCommunityOfA = new Authorization(adminComA, administratorFeature,SubCommunityOfArest);
        Authorization authAdminAColl = new Authorization(adminComA, administratorFeature, collectionRestOfSubComm);

        // define authorizations that we know not exists
        Authorization authAdminBColl = new Authorization(adminComB, administratorFeature, collectionRestOfSubComm);
        Authorization authAdminBCommunityA = new Authorization(adminComB, administratorFeature, communityRestA);

        getClient(tokenAdminComA).perform(get("/api/authz/authorizations/" + authAdminCommunityA.getID()))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminCommunityA))));

        getClient(tokenAdminComA).perform(get("/api/authz/authorizations/" + authAdminSubCommunityOfA.getID()))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher
                                           .matchAuthorization(authAdminSubCommunityOfA))));

        getClient(tokenAdminComA).perform(get("/api/authz/authorizations/" + authAdminAColl.getID()))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminAColl))));

        getClient(tokenAdminComB).perform(get("/api/authz/authorizations/" + authAdminBCommunityA.getID()))
                  .andExpect(status().isNotFound());

        getClient(tokenAdminComB).perform(get("/api/authz/authorizations/" + authAdminBColl.getID()))
                  .andExpect(status().isNotFound());
    }

    @Test
    public void collectionWithAdministratorFeatureTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson adminColA = EPersonBuilder.createEPerson(context)
                           .withEmail("adminColA@example.com")
                           .withPassword(password)
                           .build();

        EPerson adminColB = EPersonBuilder.createEPerson(context)
                           .withEmail("adminColB@example.com")
                           .withPassword(password)
                           .build();

        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                   .withName("Parent Community")
                                   .build();

        Collection collectionA = CollectionBuilder.createCollection(context, parentCommunity)
                                .withName("Collection A")
                                .withAdminGroup(adminColA)
                                .build();

        Collection collectionB = CollectionBuilder.createCollection(context, parentCommunity)
                                .withName("Collection B")
                                .withAdminGroup(adminColB)
                                .build();

        context.restoreAuthSystemState();

        CollectionRest collectionRestA = collectionConverter.convert(collectionA, DefaultProjection.DEFAULT);
        CollectionRest collectionRestB = collectionConverter.convert(collectionB, DefaultProjection.DEFAULT);

        String tokenAdminColA = getAuthToken(adminColA.getEmail(), password);
        String tokenAdminColB = getAuthToken(adminColB.getEmail(), password);

        // define authorizations that we know must exists
        Authorization authAdminCollectionA = new Authorization(adminColA, administratorFeature, collectionRestA);
        Authorization authAdminCollectionB = new Authorization(adminColB, administratorFeature, collectionRestB);

        // define authorization that we know not exists
        Authorization authAdminBcollectionA = new Authorization(adminColB, administratorFeature, collectionRestA);

        getClient(tokenAdminColA).perform(get("/api/authz/authorizations/" + authAdminCollectionA.getID()))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminCollectionA))));

        getClient(tokenAdminColB).perform(get("/api/authz/authorizations/" + authAdminCollectionB.getID()))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminCollectionB))));

        getClient(tokenAdminColB).perform(get("/api/authz/authorizations/" + authAdminBcollectionA.getID()))
                  .andExpect(status().isNotFound());
    }

    @Test
    public void siteWithAdministratorFeatureTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                   .withName("Test Parent Community")
                                   .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                               .withName("Test Collection")
                               .build();

        context.restoreAuthSystemState();

        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, DefaultProjection.DEFAULT);
        CommunityRest communityRest = communityConverter.convert(parentCommunity, DefaultProjection.DEFAULT);
        CollectionRest collectionRest = collectionConverter.convert(collection, DefaultProjection.DEFAULT);

        // tokens
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);


        // define authorizations of Admin that we know must exists
        Authorization authAdminSite = new Authorization(admin, administratorFeature, siteRest);
        Authorization authAdminCommunity = new Authorization(admin, administratorFeature, communityRest);
        Authorization authAdminCollection = new Authorization(admin, administratorFeature, collectionRest);

        // define authorizations of EPerson that we know not exists
        Authorization authEPersonSite = new Authorization(eperson, administratorFeature, siteRest);
        Authorization authEpersonCommunity = new Authorization(eperson, administratorFeature, communityRest);
        Authorization authEpersonCollection = new Authorization(eperson, administratorFeature, collectionRest);

        // define authorizations of Anonymous that we know not exists
        Authorization authAnonymousSite = new Authorization(null, administratorFeature, siteRest);
        Authorization authAnonymousCommunity = new Authorization(null, administratorFeature, communityRest);
        Authorization authAnonymousCollection = new Authorization(null, administratorFeature, collectionRest);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminSite.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminSite))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminCommunity.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminCommunity))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminCollection.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminCollection))));

        getClient(tokenEperson).perform(get("/api/authz/authorizations/" + authEPersonSite.getID()))
                    .andExpect(status().isNotFound());

        getClient(tokenEperson).perform(get("/api/authz/authorizations/" + authEpersonCommunity.getID()))
                    .andExpect(status().isNotFound());

        getClient(tokenEperson).perform(get("/api/authz/authorizations/" + authEpersonCollection.getID()))
                    .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + authAnonymousSite.getID()))
                   .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + authAnonymousCommunity.getID()))
                   .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + authAnonymousCollection.getID()))
                   .andExpect(status().isNotFound());
    }
}
