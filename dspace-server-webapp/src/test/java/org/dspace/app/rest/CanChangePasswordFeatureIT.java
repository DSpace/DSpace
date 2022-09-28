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

import org.dspace.app.rest.authorization.Authorization;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureService;
import org.dspace.app.rest.authorization.impl.CanChangePasswordFeature;
import org.dspace.app.rest.converter.EPersonConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the Can Change Password Feature.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 *
 */
public class CanChangePasswordFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private EPersonConverter ePersonConverter;

    private AuthorizationFeature canChangePasswordFeature;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        canChangePasswordFeature = authorizationFeatureService.find(CanChangePasswordFeature.NAME);
    }

    @Test
    public void testCanChangePasswordFeatureWithAdmin() throws Exception {
        EPersonRest adminRest = ePersonConverter.convert(admin, DefaultProjection.DEFAULT);
        Authorization authorization = new Authorization(admin, canChangePasswordFeature, adminRest);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authorization.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(
                                 AuthorizationMatcher.matchAuthorization(authorization))));
    }

    @Test
    public void testCanChangePasswordFeatureWithNotAdmin() throws Exception {
        EPersonRest ePersonRest = ePersonConverter.convert(eperson, DefaultProjection.DEFAULT);
        Authorization authorization = new Authorization(eperson, canChangePasswordFeature, ePersonRest);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/authz/authorizations/" + authorization.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(
                                 AuthorizationMatcher.matchAuthorization(authorization))));
    }

    @Test
    public void testCanChangePasswordFeatureIfAdminImpersonatingAnotherUser() throws Exception {
        EPersonRest ePersonRest = ePersonConverter.convert(eperson, DefaultProjection.DEFAULT);
        Authorization authorization = new Authorization(eperson, canChangePasswordFeature, ePersonRest);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authorization.getID()))
                               .andExpect(status().isNotFound());
    }

}
