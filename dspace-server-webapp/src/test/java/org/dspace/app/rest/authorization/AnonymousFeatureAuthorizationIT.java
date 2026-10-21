/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Anonymous feature checks must preserve configuration and resource policy decisions.
 */
public class AnonymousFeatureAuthorizationIT extends AbstractControllerIntegrationTest {
    private static final String EXCEPTION_FEATURE_DISABLED =
        "org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff";

    private Object previousExceptionFeatureSetting;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private SiteService siteService;

    @Autowired
    private GroupService groupService;

    @Before
    public void disableExceptionFeature() {
        previousExceptionFeatureSetting = configurationService.getProperty(EXCEPTION_FEATURE_DISABLED);
        configurationService.setProperty(EXCEPTION_FEATURE_DISABLED, true);
    }

    @After
    public void restoreExceptionFeature() {
        configurationService.setProperty(EXCEPTION_FEATURE_DISABLED, previousExceptionFeatureSetting);
    }

    @Test
    public void anonymousConfigurationFeaturesFollowConfiguration() throws Exception {
        String siteUri = configurationService.getProperty("dspace.server.url")
            + "/api/core/sites/" + siteService.findSite(context).getID();
        String[][] features = {{"canSeeQA", "qaevents.enabled"}, {"coarNotifyEnabled", "ldn.enabled"}};
        for (String[] feature : features) {
            Object previous = configurationService.getProperty(feature[1]);
            try {
                for (boolean enabled : new boolean[] {true, false}) {
                    configurationService.setProperty(feature[1], enabled);
                    getClient().perform(get("/api/authz/authorizations/search/object")
                        .param("uri", siteUri)
                        .param("feature", feature[0]))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(enabled ? 1 : 0)));
                    getClient().perform(get("/api/authz/authorizations/search/object")
                        .param("uri", siteUri).param("size", "100").param("embed", "feature"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.authorizations[*]._embedded.feature.id",
                            enabled ? hasItem(feature[0]) : not(hasItem(feature[0]))));
                }
            } finally {
                configurationService.setProperty(feature[1], previous);
            }
        }
    }

    @Test
    public void anonymousWritePoliciesRemainEffective() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("Community").build();
        Collection collection = CollectionBuilder.createCollection(context, community).withName("Collection").build();
        Item item = ItemBuilder.createItem(context, collection).withTitle("Anonymous policy item").build();
        Group anonymous = groupService.findByName(context, Group.ANONYMOUS);
        ResourcePolicyBuilder.createResourcePolicy(context, null, anonymous)
            .withDspaceObject(item).withAction(Constants.WRITE).build();
        context.restoreAuthSystemState();

        String itemUri = configurationService.getProperty("dspace.server.url") + "/api/core/items/" + item.getID();
        for (String feature : new String[] {"canEditItem", "canEditMetadata", "canManageRelationships"}) {
            getClient().perform(get("/api/authz/authorizations/search/object")
                .param("uri", itemUri).param("feature", feature))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", is(1)));
        }
        getClient().perform(get("/api/authz/authorizations/search/object")
            .param("uri", itemUri).param("feature", "canCreateVersion"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", is(0)));
        getClient().perform(get("/api/authz/authorizations/search/object")
            .param("uri", itemUri).param("size", "100").param("embed", "feature"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[*]._embedded.feature.id", hasItem("canEditItem")))
            .andExpect(jsonPath("$._embedded.authorizations[*]._embedded.feature.id", hasItem("canEditMetadata")))
            .andExpect(jsonPath("$._embedded.authorizations[*]._embedded.feature.id",
                hasItem("canManageRelationships")))
            .andExpect(jsonPath("$._embedded.authorizations[*]._embedded.feature.id",
                not(hasItem("canCreateVersion"))));
    }
}
