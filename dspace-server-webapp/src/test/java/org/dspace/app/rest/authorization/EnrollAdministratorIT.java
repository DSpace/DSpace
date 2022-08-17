/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.impl.AdministratorOfFeature;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.EPersonBuilder;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.SiteService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * test that the enroll process for a new administrator, i.e. create an user and
 * add user to the administrators group, will result in the Administrator Feature(s) to be available to such user.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 *
 */
public class EnrollAdministratorIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;
    @Autowired
    GroupService groupService;
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
    public void addUserToAdminGroupOnSiteTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
                          .withNameInMetadata("Jhon", "Brown")
                          .withEmail("johnbrown@example.com")
                          .withPassword(password)
                          .build();

        context.restoreAuthSystemState();

        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, DefaultProjection.DEFAULT);

        Group adminGroup = groupService.findByName(context, Group.ADMIN);

        // tokens
        String tokenEperson1 = getAuthToken(eperson1.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        // add eperson1 to the admin group
        getClient(tokenAdmin).perform(post("/api/eperson/groups/" + adminGroup.getID() + "/epersons")
                             .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                             .content(REST_SERVER_URL + "eperson/groups/" + eperson1.getID()))
                             .andExpect(status().isNoContent());

        // define authorization that we know must exists
        Authorization authAdminSite = new Authorization(eperson1, administratorFeature, siteRest);

        // access the authorization for the eperson1 user
        getClient(tokenEperson1).perform(get("/api/authz/authorizations/" + authAdminSite.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminSite))));
    }
}