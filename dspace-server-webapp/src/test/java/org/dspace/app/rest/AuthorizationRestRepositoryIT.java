/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.Serializable;
import java.util.UUID;

import org.dspace.app.rest.authorization.AlwaysFalseFeature;
import org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature;
import org.dspace.app.rest.authorization.AlwaysTrueFeature;
import org.dspace.app.rest.authorization.TrueForAdminsFeature;
import org.dspace.app.rest.authorization.TrueForLoggedUsersFeature;
import org.dspace.app.rest.authorization.TrueForTestUsersFeature;
import org.dspace.app.rest.authorize.Authorization;
import org.dspace.app.rest.authorize.AuthorizationFeature;
import org.dspace.app.rest.authorize.AuthorizationFeatureService;
import org.dspace.app.rest.authorize.AuthorizationRestUtil;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.dspace.discovery.FindableObject;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test suite for the Authorization endpoint
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class AuthorizationRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private AuthorizationRestUtil authorizationRestUtil;

    private SiteService siteService;

    private AuthorizationFeature alwaysTrue;

    private AuthorizationFeature alwaysFalse;

    private AuthorizationFeature alwaysException;

    private AuthorizationFeature trueForAdmins;

    private AuthorizationFeature trueForLoggedUsers;

    private AuthorizationFeature trueForTestUsers;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        siteService = ContentServiceFactory.getInstance().getSiteService();
        alwaysTrue = authorizationFeatureService.find(AlwaysTrueFeature.NAME);
        alwaysFalse = authorizationFeatureService.find(AlwaysFalseFeature.NAME);
        alwaysException = authorizationFeatureService.find(AlwaysThrowExceptionFeature.NAME);
        trueForAdmins = authorizationFeatureService.find(TrueForAdminsFeature.NAME);
        trueForLoggedUsers = authorizationFeatureService.find(TrueForLoggedUsersFeature.NAME);
        trueForTestUsers = authorizationFeatureService.find(TrueForTestUsersFeature.NAME);
    }

    @Test
    /**
     * This method is not implemented
     *
     * @throws Exception
     */
    public void findAllTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/authz/authorizations"))
                    .andExpect(status().isMethodNotAllowed());
        getClient().perform(get("/api/authz/authorizations"))
                    .andExpect(status().isMethodNotAllowed());
    }

    @Test
    /**
     * Verify that an user can access a specific authorization
     *
     * @throws Exception
     */
    public void findOneTest() throws Exception {
        Site site = siteService.findSite(context);

        // define three authorizations that we know must exists
        Authorization authAdminSite = new Authorization(admin, trueForAdmins, site);
        Authorization authNormalUserSite = new Authorization(eperson, trueForLoggedUsers, site);
        Authorization authAnonymousUserSite = new Authorization(null, alwaysTrue, site);

        // access the authorization for the admin user
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/authz/authorizations/" + authAdminSite.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$",
                            Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminSite))));

        // access the authorization for a normal user
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/authz/authorizations/" + authNormalUserSite.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$",
                            Matchers.is(AuthorizationMatcher.matchAuthorization(authNormalUserSite))));

        // access the authorization for a normal user as administrator
        getClient(adminToken).perform(get("/api/authz/authorizations/" + authNormalUserSite.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$",
                            Matchers.is(AuthorizationMatcher.matchAuthorization(authNormalUserSite))));

        // access the authorization for an anonymous user
        getClient().perform(get("/api/authz/authorizations/" + authAnonymousUserSite.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$",
                            Matchers.is(AuthorizationMatcher.matchAuthorization(authAnonymousUserSite))));
    }

    @Test
    /**
     * Verify that the unauthorized return code is used in the appropriate scenarios
     *
     * @throws Exception
     */
    public void findOneUnauthorizedTest() throws Exception {
        Site site = siteService.findSite(context);

        // define two authorizations that we know must exists
        Authorization authAdminSite = new Authorization(admin, alwaysTrue, site);
        Authorization authNormalUserSite = new Authorization(eperson, alwaysTrue, site);

        // try anonymous access to the authorization for the admin user
        getClient().perform(get("/api/authz/authorizations/" + authAdminSite.getID()))
                    .andExpect(status().isUnauthorized());

        // try anonymous access to the authorization for a normal user
        getClient().perform(get("/api/authz/authorizations/" + authNormalUserSite.getID()))
                    .andExpect(status().isUnauthorized());
    }

    @Test
    /**
     * Verify that the forbidden return code is used in the appropriate scenarios
     *
     * @throws Exception
     */
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Site site = siteService.findSite(context);
        EPerson testEPerson = EPersonBuilder.createEPerson(context)
                .withEmail("test-authorization@example.com")
                .withPassword(password).build();
        context.restoreAuthSystemState();

        // define three authorizations that we know must exists
        Authorization authAdminSite = new Authorization(admin, alwaysTrue, site);
        Authorization authNormalUserSite = new Authorization(eperson, alwaysTrue, site);
        Authorization authAnonymousUserSite = new Authorization(null, alwaysTrue, site);

        String testToken = getAuthToken(testEPerson.getEmail(), password);

        // try to access the authorization for the admin user with another user
        getClient(testToken).perform(get("/api/authz/authorizations/" + authAdminSite.getID()))
                    .andExpect(status().isForbidden());

        // try to access the authorization of a normal user with another user
        getClient(testToken).perform(get("/api/authz/authorizations/" + authNormalUserSite.getID()))
                    .andExpect(status().isForbidden());

        // try to access the authorization of the anonymous user with another user
        // FIXME we want that?
//        getClient(testToken).perform(get("/api/authz/authorizations/"
//            + authAnonymousUserSite.getID()))
//                    .andExpect(status().isForbidden());

        // check access as a test user to a not existing authorization for another
        // eperson (but existing for the test user)
        Authorization noTestAuthForNormalUserSite  = new Authorization(eperson, trueForTestUsers, site);
        getClient(testToken).perform(get("/api/authz/authorizations/" + noTestAuthForNormalUserSite.getID()))
                    .andExpect(status().isForbidden());
    }

    @Test
    /**
     * Verify that the not found return code is used in the appropriate scenarios
     *
     * @throws Exception
     */
    public void findOneNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Site site = siteService.findSite(context);
        context.restoreAuthSystemState();

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);

        // define three authorizations that we know will be no granted
        Authorization authAdminSite = new Authorization(admin, alwaysFalse, site);
        Authorization authNormalUserSite = new Authorization(eperson, alwaysFalse, site);
        Authorization authAnonymousUserSite = new Authorization(null, alwaysFalse, site);

        getClient(adminToken).perform(get("/api/authz/authorizations/" + authAdminSite.getID()))
                    .andExpect(status().isNotFound());

        getClient(epersonToken).perform(get("/api/authz/authorizations/" + authNormalUserSite.getID()))
                    .andExpect(status().isNotFound());
        // also the admin cannot retrieve a not existing authorization for the normal user
        getClient(epersonToken).perform(get("/api/authz/authorizations/" + authNormalUserSite.getID()))
                .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + authAnonymousUserSite.getID()))
                    .andExpect(status().isNotFound());
        // also the admin cannot retrieve a not existing authorization for the anonymous user
        getClient(adminToken).perform(get("/api/authz/authorizations/" + authAnonymousUserSite.getID()))
                    .andExpect(status().isNotFound());

        // build a couple of IDs that look good but are related to not existing authorizations
        // the trueForAdmins feature is not defined for eperson
        String authInvalidType = getAuthorizationID(admin, trueForAdmins, eperson);
        getClient(adminToken).perform(get("/api/authz/authorizations/" + authInvalidType))
                    .andExpect(status().isNotFound());

        // the specified item doesn't exist
        String authNotExistingObject = getAuthorizationID(admin, alwaysTrue, Constants.ITEM, UUID.randomUUID());
        getClient(adminToken).perform(get("/api/authz/authorizations/" + authNotExistingObject))
                    .andExpect(status().isNotFound());

        // the specified eperson doesn't exist
        String authNotExistingEPerson = getAuthorizationID(UUID.randomUUID(), alwaysTrue, site);
        getClient(adminToken).perform(get("/api/authz/authorizations/" + authNotExistingEPerson))
                    .andExpect(status().isNotFound());

        // the specified feature doesn't exist
        String authNotExistingFeature = getAuthorizationID(admin, "notexistingfeature", site);
        getClient(adminToken).perform(get("/api/authz/authorizations/" + authNotExistingFeature))
                    .andExpect(status().isNotFound());

        // check access as admin to a not existing authorization for another eperson (but existing for the admin)
        Authorization noAdminAuthForNormalUserSite  = new Authorization(eperson, trueForAdmins, site);
        getClient(adminToken).perform(get("/api/authz/authorizations/" + noAdminAuthForNormalUserSite.getID()))
                    .andExpect(status().isNotFound());

        // check a couple of completely wrong IDs
        String notValidID = "notvalidID";
        getClient(adminToken).perform(get("/api/authz/authorizations/" + notValidID))
                    .andExpect(status().isNotFound());

        String notValidIDWithWrongEpersonPart = getAuthorizationID("1", alwaysTrue.getName(),
                String.valueOf(site.getType()), site.getID().toString());
        // use the admin token otherwise it would result in a forbidden (attempt to access authorization of other users)
        getClient(adminToken).perform(get("/api/authz/authorizations/" + notValidIDWithWrongEpersonPart))
                    .andExpect(status().isNotFound());

        String notValidIDWithWrongObjectTypePart = getAuthorizationID(eperson.getID().toString(), alwaysTrue.getName(),
                "SITE", site.getID().toString());
        getClient(epersonToken).perform(get("/api/authz/authorizations/" + notValidIDWithWrongObjectTypePart))
                    .andExpect(status().isNotFound());

        String notValidIDWithUnknownObjectTypePart =
                getAuthorizationID(eperson.getID().toString(), alwaysTrue.getName(),
                        String.valueOf(Integer.MAX_VALUE), "1");
        getClient(epersonToken).perform(get("/api/authz/authorizations/" + notValidIDWithUnknownObjectTypePart))
                    .andExpect(status().isNotFound());

    }

    @Test
    /**
     * Verify that an exception in the feature check will be reported back
     *
     * @throws Exception
     */
    public void findOneInternalServerErrorTest() throws Exception {
        Site site = siteService.findSite(context);

        // define two authorizations that we know will throw exceptions
        Authorization authAdminSite = new Authorization(admin, alwaysException, site);
        Authorization authNormalUserSite = new Authorization(eperson, alwaysException, site);

        String adminToken = getAuthToken(admin.getEmail(), password);
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(adminToken).perform(get("/api/authz/authorizations/" + authAdminSite.getID()))
                    .andExpect(status().isInternalServerError());

        getClient(epersonToken).perform(get("/api/authz/authorizations/" + authNormalUserSite.getID()))
                    .andExpect(status().isInternalServerError());
    }

    // utility methods to build authorization ID without having an authorization object
    private String getAuthorizationID(EPerson eperson, AuthorizationFeature feature, FindableObject obj) {
        return getAuthorizationID(eperson != null ? eperson.getID().toString() : null, feature.getName(),
                String.valueOf(obj.getType()), obj.getID());
    }

    private String getAuthorizationID(UUID epersonUuid, AuthorizationFeature feature, FindableObject obj) {
        return getAuthorizationID(epersonUuid != null ? epersonUuid.toString() : null, feature.getName(),
                String.valueOf(obj.getType()), obj.getID());
    }

    private String getAuthorizationID(EPerson eperson, String featureName, FindableObject obj) {
        return getAuthorizationID(eperson != null ? eperson.getID().toString() : null, featureName,
                String.valueOf(obj.getType()), obj.getID());
    }

    private String getAuthorizationID(EPerson eperson, AuthorizationFeature feature, int objType, Serializable objID) {
        return getAuthorizationID(eperson != null ? eperson.getID().toString() : null, feature.getName(),
                String.valueOf(objType), objID);
    }

    private String getAuthorizationID(String epersonUuid, String featureName, String type, Serializable id) {
        return (epersonUuid != null ? epersonUuid + "_" : "") + featureName + "_" + type + "_"
                + id.toString();
    }

}
