/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.authorization.impl.CanReplaceBitstreamFeature;
import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the canReplaceBitstream authorization feature.
 *
 * @author Jens Vannerum (jens dot vannerum at atmire dot com)
 */
public class CanReplaceBitstreamFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private BitstreamConverter bitstreamConverter;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private Utils utils;

    private AuthorizationFeature canReplaceBitstreamFeature;

    private Collection collection;
    private Item item;
    private Bitstream bitstream;
    private EPerson userWithWrite;
    private EPerson userWithoutWrite;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        canReplaceBitstreamFeature = authorizationFeatureService.find(CanReplaceBitstreamFeature.NAME);

        Community community = CommunityBuilder.createCommunity(context).build();
        collection = CollectionBuilder.createCollection(context, community).build();
        item = ItemBuilder.createItem(context, collection).build();

        String bitstreamContent = "Test bitstream content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, item, is)
                                       .withName("TestBitstream")
                                       .withDescription("Test Description")
                                       .withMimeType("text/plain")
                                       .build();
        }

        userWithWrite = EPersonBuilder.createEPerson(context)
                                      .withEmail("userWithWrite@test.com")
                                      .withPassword(password)
                                      .build();

        userWithoutWrite = EPersonBuilder.createEPerson(context)
                                        .withEmail("userWithoutWrite@test.com")
                                        .withPassword(password)
                                        .build();

        ResourcePolicyBuilder.createResourcePolicy(context)
                            .withDspaceObject(bitstream)
                            .withAction(Constants.WRITE)
                            .withUser(userWithWrite)
                            .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testCanReplaceBitstreamWithConfigurationEnabledAndWritePermission() throws Exception {
        configurationService.setProperty("replace-bitstream.enabled", true);

        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstream, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        Authorization authorization = new Authorization(userWithWrite, canReplaceBitstreamFeature, bitstreamRest);

        String token = getAuthToken(userWithWrite.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                .param("uri", bitstreamUri)
                                .param("feature", canReplaceBitstreamFeature.getName()))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.page.totalElements", is(1)))
                       .andExpect(jsonPath("$._embedded.authorizations[0]",
                                          AuthorizationMatcher.matchAuthorization(authorization)));
    }

    @Test
    public void testCanReplaceBitstreamWithConfigurationEnabledButWithoutWritePermission() throws Exception {
        configurationService.setProperty("replace-bitstream.enabled", true);

        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstream, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        String token = getAuthToken(userWithoutWrite.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                .param("uri", bitstreamUri)
                                .param("feature", canReplaceBitstreamFeature.getName()))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.page.totalElements", is(0)))
                       .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void testCanReplaceBitstreamWithConfigurationDisabled() throws Exception {
        configurationService.setProperty("replace-bitstream.enabled", false);

        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstream, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        String token = getAuthToken(userWithWrite.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                .param("uri", bitstreamUri)
                                .param("feature", canReplaceBitstreamFeature.getName()))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.page.totalElements", is(0)))
                       .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void testCanReplaceBitstreamAsAdminWithConfigurationEnabled() throws Exception {
        configurationService.setProperty("replace-bitstream.enabled", true);

        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstream, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        Authorization authorization = new Authorization(admin, canReplaceBitstreamFeature, bitstreamRest);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                .param("uri", bitstreamUri)
                                .param("feature", canReplaceBitstreamFeature.getName()))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.page.totalElements", is(1)))
                       .andExpect(jsonPath("$._embedded.authorizations[0]",
                                          AuthorizationMatcher.matchAuthorization(authorization)));
    }

    @Test
    public void testCanReplaceBitstreamAsAnonymousUser() throws Exception {
        configurationService.setProperty("replace-bitstream.enabled", true);

        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstream, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                           .param("uri", bitstreamUri)
                           .param("feature", canReplaceBitstreamFeature.getName()))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.page.totalElements", is(0)))
                  .andExpect(jsonPath("$._embedded").doesNotExist());
    }
}
