/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.impl.EPersonForgotPasswordFeature;
import org.dspace.app.rest.converter.EPersonConverter;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.EPersonBuilder;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class EPersonForgotPasswordFeatureIT extends AbstractControllerIntegrationTest {
    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private SiteService siteService;

    @Autowired
    private SiteConverter siteConverter;


    @Autowired
    private EPersonConverter personConverter;

    @Autowired
    private Utils utils;

    private AuthorizationFeature epersonForgotPasswordFeature;

    public static final String[] SHIB_ONLY = {"org.dspace.authenticate.ShibAuthentication"};


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        epersonForgotPasswordFeature = authorizationFeatureService.find(EPersonForgotPasswordFeature.NAME);
    }

    @Test
    public void userForgotPasswordSuccessTest() throws Exception {

        context.turnOffAuthorisationSystem();
        EPerson epersonPassLogin = EPersonBuilder.createEPerson(context)
                                              .withNameInMetadata("Vincenzo", "Mecca")
                                              .withCanLogin(true)
                                              .withPassword("Strong-Password")
                                              .withEmail("vincenzo.mecca@4science.it")
                                              .build();
        context.restoreAuthSystemState();

        configurationService.setProperty("user.forgot-password", true);
        EPersonRest personRest = personConverter.convert(epersonPassLogin, Projection.DEFAULT);
        String personUri = utils.linkToSingleResource(personRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                                .param("uri", personUri)
                                .param("feature", epersonForgotPasswordFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));
    }

    @Test
    public void userForgotPasswordFeatureUnauthorizedTest() throws Exception {

        Site site = siteService.findSite(context);
        SiteRest SiteRest = siteConverter.convert(site, Projection.DEFAULT);
        String siteUri = utils.linkToSingleResource(SiteRest, "self").getHref();

        configurationService.setProperty("user.forgot-password", false);

        getClient().perform(get("/api/authz/authorizations/search/object")
                                .param("uri", siteUri)
                                .param("feature", epersonForgotPasswordFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)));
    }


    @Test
    public void userForgotPasswordNoLoginTest() throws Exception {

        context.turnOffAuthorisationSystem();
        EPerson noLoginPerson = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("User", "NoLogin")
                                      .withCanLogin(false)
                                      .withPassword("Strong-Password")
                                      .build();
        context.restoreAuthSystemState();

        EPersonRest personRest = personConverter.convert(noLoginPerson, Projection.DEFAULT);
        String personUri = utils.linkToSingleResource(personRest, "self").getHref();
        configurationService.setProperty("user.forgot-password", true);
        getClient().perform(get("/api/authz/authorizations/search/object")
                                .param("uri", personUri)
                                .param("feature", epersonForgotPasswordFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void userForgotPasswordUnauthorizedNoPasswordAuthMethodTest() throws Exception {
        //Enable Shibboleth and password login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        EPersonRest personRest = personConverter.convert(eperson, Projection.DEFAULT);
        String personUri = utils.linkToSingleResource(personRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                                .param("uri", personUri)
                                .param("feature", epersonForgotPasswordFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)));
    }
}
