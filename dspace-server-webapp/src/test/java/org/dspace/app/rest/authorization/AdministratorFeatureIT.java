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

import org.dspace.app.rest.authorization.impl.AdministratorFeature;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
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
    private ConverterService converterService;
    @Autowired
    GroupService groupService;
    @Autowired
    AuthorizeService authService;
    @Autowired
    CommunityService communityService;

    private AuthorizationFeature administratorFeature;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        administratorFeature = authorizationFeatureService.find(AdministratorFeature.NAME);
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

        CommunityRest communityRestA = converterService.toRest(communityA, DefaultProjection.DEFAULT);
        CommunityRest SubCommunityOfArest = converterService.toRest(subCommunityOfA, DefaultProjection.DEFAULT);
        CollectionRest collectionRestOfSubComm = converterService.toRest(collectionOfSubComm,DefaultProjection.DEFAULT);

        // tokens
        String tokenAdminComA = getAuthToken(adminComA.getEmail(), password);
        String tokenAdminComB = getAuthToken(adminComB.getEmail(), password);

        // define authorization that we know must exists
        Authorization authAdminCommunityA = new Authorization(adminComA, administratorFeature, communityRestA);
        Authorization authAdminSubCommunityOfA = new Authorization(adminComA, administratorFeature,SubCommunityOfArest);
        Authorization authAdminBCommunityA = new Authorization(adminComB, administratorFeature, communityRestA);
        Authorization authAdminAColl = new Authorization(adminComA, administratorFeature, collectionRestOfSubComm);
        Authorization authAdminBColl = new Authorization(adminComB, administratorFeature, collectionRestOfSubComm);

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

        CollectionRest collectionRestA = converterService.toRest(collectionA, DefaultProjection.DEFAULT);
        CollectionRest collectionRestB = converterService.toRest(collectionB, DefaultProjection.DEFAULT);

        String tokenAdminColA = getAuthToken(adminColA.getEmail(), password);
        String tokenAdminColB = getAuthToken(adminColB.getEmail(), password);

        // define authorization that we know must exists
        Authorization authAdminCollectionA = new Authorization(adminColA, administratorFeature, collectionRestA);
        Authorization authAdminCollectionB = new Authorization(adminColB, administratorFeature, collectionRestB);
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
}
