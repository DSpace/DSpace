/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.authorization.impl.CanSubscribeFeature;
import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.converter.BundleConverter;
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.converter.EPersonConverter;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.suggestion.SolrSuggestionProvider;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.builder.SiteBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test of Profile Subscribe Dso Feature implementation.
 *
 * @author Alba Aliu (alba.aliu at 4science.it)
 */
public class CanSubscribeFeatureIT extends AbstractControllerIntegrationTest {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SolrSuggestionProvider.class);
    private Item collectionAProfile;
    private Item collectionBProfile;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemConverter itemConverter;
    @Autowired
    private CollectionConverter collectionConverter;
    @Autowired
    private SiteConverter siteConverter;
    @Autowired
    private Utils utils;
    @Autowired
    private SiteService siteService;
    @Autowired
    private CommunityConverter communityConverter;
    @Autowired
    private EPersonConverter ePersonConverter;
    @Autowired
    private BundleConverter bundleConverter;
    @Autowired
    private BitstreamConverter bitstreamConverter;
    private Collection collection;
    private Collection collectionA;
    private Community communityA;
    private AuthorizationFeature canSubscribeFeature;
    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;
    @Autowired
    private ResourcePolicyService resourcePolicyService;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").build();
        Community community1 = CommunityBuilder.createCommunity(context).withName("Community").build();
        collection = CollectionBuilder.createCollection(context, parentCommunity).withEntityType("Person")
                .withName("collection").build();
        communityA = CommunityBuilder.createCommunity(context)
                .withName("communityA")
                .withAdminGroup(admin).build();

        collectionA = CollectionBuilder.createCollection(context, communityA)
                .withName("Collection A")
                .withAdminGroup(admin).build();

        context.restoreAuthSystemState();
        canSubscribeFeature = authorizationFeatureService.find(CanSubscribeFeature.NAME);
    }

    @Test
    public void testCanSubscribeCommunity() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePersona = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("test@email.it")
                .build();
        String token = getAuthToken(ePersona.getEmail(), password);
        CommunityRest comRest = communityConverter.convert(parentCommunity, DefaultProjection.DEFAULT);
        String comUri = utils.linkToSingleResource(comRest, "self").getHref();
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                        .param("uri", comUri)
                        .param("feature", canSubscribeFeature.getName())
                        .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));

    }

    @Test
    public void testCanNotSubscribeItem() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePersonNotSubscribePermission = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("test@email.it")
                .build();
        // the user to be tested is not part of the group with read permission
        Group groupWithReadPermission = GroupBuilder.createGroup(context)
                .withName("Group A")
                .addMember(admin)
                .build();
        Item item = ItemBuilder.createItem(context, collectionA)
                .withTitle("Test item")
                .build();
        cleanUpPermissions(resourcePolicyService.find(context, item));
//        resourcePolicyService.delete(context, resourcePolicyService.find(context, item).get(0));
//        resourcePolicyService.find(context, item);
        setPermissions(item, groupWithReadPermission, Constants.READ);
        item.setSubmitter(eperson);
        context.restoreAuthSystemState();
        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);
        String token = getAuthToken(ePersonNotSubscribePermission.getEmail(), password);
        String comUri = utils.linkToSingleResource(itemRest, "self").getHref();
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                        .param("uri", comUri)
                        .param("feature", canSubscribeFeature.getName())
                        .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.page.totalElements", is(0)))
                .andExpect(jsonPath("$.page.totalPages", is(0)))
                .andExpect(jsonPath("$.page.number", is(0)))
                // default value of page size
                .andExpect(jsonPath("$.page.size", is(20)));
    }

    @Test
    public void testCanNotSubscribeCollection() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collectionWithReadPermission = CollectionBuilder.createCollection(context, communityA).withAdminGroup(admin).build();
        EPerson ePersonNotSubscribePermission = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("test@email.it")
                .build();
        // the user to be tested is not part of the group with read permission
        Group groupWithReadPermission = GroupBuilder.createGroup(context)
                .withName("Group A")
                .addMember(eperson)
                .build();
        cleanUpPermissions(resourcePolicyService.find(context, collectionWithReadPermission));
        setPermissions(collectionWithReadPermission, groupWithReadPermission, Constants.READ);
        context.restoreAuthSystemState();
        CollectionRest collectionRest = collectionConverter.convert(collectionWithReadPermission, Projection.DEFAULT);
        String token = getAuthToken(ePersonNotSubscribePermission.getEmail(), password);
        String comUri = utils.linkToSingleResource(collectionRest, "self").getHref();
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                        .param("uri", comUri)
                        .param("feature", canSubscribeFeature.getName())
                        .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.page.totalElements", is(0)))
                .andExpect(jsonPath("$.page.totalPages", is(0)))
                .andExpect(jsonPath("$.page.number", is(0)))
                // default value of page size
                .andExpect(jsonPath("$.page.size", is(20)));
    }

    @Test
    public void testCanNotSubscribeCommunity() throws Exception {
        context.turnOffAuthorisationSystem();
        Community communityWithReadPermissions = CommunityBuilder.createCommunity(context).build();
        EPerson ePersonNotSubscribePermission = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("test@email.it")
                .build();
        // the user to be tested is not part of the group with read permission
        Group groupWithReadPermission = GroupBuilder.createGroup(context)
                .withName("Group A")
                .addMember(admin)
                .addMember(eperson)
                .build();
        cleanUpPermissions(resourcePolicyService.find(context, communityWithReadPermissions));
        setPermissions(communityWithReadPermissions, groupWithReadPermission, Constants.READ);
        context.restoreAuthSystemState();
        CommunityRest communityRest = communityConverter.convert(communityWithReadPermissions, Projection.DEFAULT);
        String token = getAuthToken(ePersonNotSubscribePermission.getEmail(), password);
        String comUri = utils.linkToSingleResource(communityRest, "self").getHref();
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                        .param("uri", comUri)
                        .param("feature", canSubscribeFeature.getName())
                        .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.page.totalElements", is(0)))
                .andExpect(jsonPath("$.page.totalPages", is(0)))
                .andExpect(jsonPath("$.page.number", is(0)))
                // default value of page size
                .andExpect(jsonPath("$.page.size", is(20)));
    }

    @Test
    public void testCanSubscribeSite() throws Exception {
        context.turnOffAuthorisationSystem();
        Site site = SiteBuilder.createSite(context).build();
        EPerson ePersonNotSubscribePermission = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("test@email.it")
                .build();
        // the user to be tested is not part of the group with read permission
        Group groupWithReadPermission = GroupBuilder.createGroup(context)
                .withName("Group A")
                .addMember(admin)
                .addMember(eperson)
                .build();

        SiteRest siteRest = siteConverter.convert(site, Projection.DEFAULT);
        context.restoreAuthSystemState();
        String token = getAuthToken(ePersonNotSubscribePermission.getEmail(), password);
        String comUri = utils.linkToSingleResource(siteRest, "self").getHref();
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                        .param("uri", comUri)
                        .param("feature", canSubscribeFeature.getName())
                        .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));
    }

    @Test
    public void testCanNotSubscribeEPerson() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePersonNotSubscribePermission = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("test@email.it")
                .build();
        EPerson ePersonToBeChecked = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("alba.aliu@email.it")
                .build();
        // the user to be tested is not part of the group with read permission
        Group groupWithReadPermission = GroupBuilder.createGroup(context)
                .withName("Group A")
                .addMember(admin)
                .addMember(eperson)
                .build();
        cleanUpPermissions(resourcePolicyService.find(context, ePersonToBeChecked));
        setPermissions(ePersonToBeChecked, groupWithReadPermission, Constants.READ);
        context.restoreAuthSystemState();
        EPersonRest ePersonRest = ePersonConverter.convert(ePersonToBeChecked, Projection.DEFAULT);
        String token = getAuthToken(ePersonNotSubscribePermission.getEmail(), password);
        String comUri = utils.linkToSingleResource(ePersonRest, "self").getHref();
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                        .param("uri", comUri)
                        .param("feature", canSubscribeFeature.getName())
                        .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.page.totalElements", is(0)))
                .andExpect(jsonPath("$.page.totalPages", is(0)))
                .andExpect(jsonPath("$.page.number", is(0)))
                // default value of page size
                .andExpect(jsonPath("$.page.size", is(20)));
    }

    @Test
    public void testCanNotSubscribeBundle() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collectionA)
                .withTitle("Test item")
                .build();
        Bundle bundle = BundleBuilder.createBundle(context, item).withName("TestBundle").build();
        EPerson ePersonWithNoPermissions = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("alba.aliu@email.it")
                .build();
        // the user to be tested is not part of the group with read permission
        Group groupWithReadPermission = GroupBuilder.createGroup(context)
                .withName("Group A")
                .addMember(admin)
                .addMember(eperson)
                .build();
        cleanUpPermissions(resourcePolicyService.find(context, bundle));
        setPermissions(bundle, groupWithReadPermission, Constants.READ);
        context.restoreAuthSystemState();
        BundleRest bundleRest = bundleConverter.convert(bundle, Projection.DEFAULT);
        String token = getAuthToken(ePersonWithNoPermissions.getEmail(), password);
        String comUri = utils.linkToSingleResource(bundleRest, "self").getHref();
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                        .param("uri", comUri)
                        .param("feature", canSubscribeFeature.getName())
                        .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.page.totalElements", is(0)))
                .andExpect(jsonPath("$.page.totalPages", is(0)))
                .andExpect(jsonPath("$.page.number", is(0)))
                // default value of page size
                .andExpect(jsonPath("$.page.size", is(20)));
    }

    @Test
    public void testCanNotSubscribeBitstream() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collectionA)
                .withTitle("Test item")
                .build();
        String bitstreamContent = "testString";
        InputStream stream = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8);
        Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, stream).build();
        EPerson ePersonWithNoPermissions = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("alba.aliu@email.it")
                .build();
        // the user to be tested is not part of the group with read permission
        Group groupWithReadPermission = GroupBuilder.createGroup(context)
                .withName("Group A")
                .addMember(admin)
                .addMember(eperson)
                .build();
        cleanUpPermissions(resourcePolicyService.find(context, bitstream));
        setPermissions(bitstream, groupWithReadPermission, Constants.READ);
        context.restoreAuthSystemState();
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstream, Projection.DEFAULT);
        String token = getAuthToken(ePersonWithNoPermissions.getEmail(), password);
        String comUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                        .param("uri", comUri)
                        .param("feature", canSubscribeFeature.getName())
                        .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.page.totalElements", is(0)))
                .andExpect(jsonPath("$.page.totalPages", is(0)))
                .andExpect(jsonPath("$.page.number", is(0)))
                // default value of page size
                .andExpect(jsonPath("$.page.size", is(20)));
    }

    @Test
    public void testCanSubscribeBitstream() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collectionA)
                .withTitle("Test item")
                .build();
        String bitstreamContent = "testString";
        InputStream stream = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8);
        Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, stream).build();
        EPerson ePersonWithPermissions = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("alba.aliu@email.it")
                .build();
        // the user to be tested is  part of the group with read permission
        Group groupWithReadPermission = GroupBuilder.createGroup(context)
                .withName("Group A")
                .addMember(admin)
                .addMember(eperson)
                .addMember(ePersonWithPermissions)
                .build();
        cleanUpPermissions(resourcePolicyService.find(context, bitstream));
        setPermissions(bitstream, groupWithReadPermission, Constants.READ);
        context.restoreAuthSystemState();
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstream, Projection.DEFAULT);
        String token = getAuthToken(ePersonWithPermissions.getEmail(), password);
        String comUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                        .param("uri", comUri)
                        .param("feature", canSubscribeFeature.getName())
                        .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));

    }

    @Test
    public void testCanSubscribeBundle() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collectionA)
                .withTitle("Test item")
                .build();
        Bundle bundle = BundleBuilder.createBundle(context, item).withName("TestBundle").build();
        EPerson ePersonWithPermissions = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("alba.aliu@email.it")
                .build();
        // the user to be tested is not part of the group with read permission
        Group groupWithReadPermission = GroupBuilder.createGroup(context)
                .withName("Group A")
                .addMember(admin)
                .addMember(eperson)
                .addMember(ePersonWithPermissions)
                .build();
        cleanUpPermissions(resourcePolicyService.find(context, bundle));
        setPermissions(bundle, groupWithReadPermission, Constants.READ);
        context.restoreAuthSystemState();
        BundleRest bundleRest = bundleConverter.convert(bundle, Projection.DEFAULT);
        String token = getAuthToken(ePersonWithPermissions.getEmail(), password);
        String comUri = utils.linkToSingleResource(bundleRest, "self").getHref();
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                        .param("uri", comUri)
                        .param("feature", canSubscribeFeature.getName())
                        .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));
    }

    @Test
    public void testCanSubscribeEPerson() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePersonSubscribePermission = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("test@email.it")
                .build();
        EPerson ePersonToBeChecked = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("alba.aliu@email.it")
                .build();
        // the user to be tested is not part of the group with read permission
        Group groupWithReadPermission = GroupBuilder.createGroup(context)
                .withName("Group A")
                .addMember(admin)
                .addMember(eperson)
                .addMember(ePersonSubscribePermission)
                .build();
        cleanUpPermissions(resourcePolicyService.find(context, ePersonToBeChecked));
        setPermissions(ePersonToBeChecked, groupWithReadPermission, Constants.READ);
        context.restoreAuthSystemState();
        EPersonRest ePersonRest = ePersonConverter.convert(ePersonToBeChecked, Projection.DEFAULT);
        String token = getAuthToken(ePersonSubscribePermission.getEmail(), password);
        String comUri = utils.linkToSingleResource(ePersonRest, "self").getHref();
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                        .param("uri", comUri)
                        .param("feature", canSubscribeFeature.getName())
                        .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));
    }

    @Test
    public void testCanSubscribeCollection() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collectionWithReadPermission = CollectionBuilder.createCollection(context, communityA).withAdminGroup(admin).build();
        EPerson ePersonSubscribePermission = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("test@email.it")
                .build();
        context.restoreAuthSystemState();
        CollectionRest collectionRest = collectionConverter.convert(collectionWithReadPermission, Projection.DEFAULT);
        String token = getAuthToken(ePersonSubscribePermission.getEmail(), password);
        String comUri = utils.linkToSingleResource(collectionRest, "self").getHref();
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                        .param("uri", comUri)
                        .param("feature", canSubscribeFeature.getName())
                        .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));
    }

    @Test
    public void testCanSubscribeItem() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePersonSubscribePermission = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("test@email.it")
                .build();
        Item item = ItemBuilder.createItem(context, collectionA)
                .withTitle("Test item")
                .build();
        context.restoreAuthSystemState();
        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);
        String token = getAuthToken(ePersonSubscribePermission.getEmail(), password);
        String comUri = utils.linkToSingleResource(itemRest, "self").getHref();
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                        .param("uri", comUri)
                        .param("feature", canSubscribeFeature.getName())
                        .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));
    }

    private void setPermissions(DSpaceObject dSpaceObject, Group group, Integer permissions) {
        try {
            ResourcePolicyBuilder.createResourcePolicy(context)
                    .withDspaceObject(dSpaceObject)
                    .withAction(permissions)
                    .withGroup(group)
                    .build();
        } catch (SQLException | AuthorizeException sqlException) {
            log.error(sqlException.getMessage());
        }
    }

    private void cleanUpPermissions(List<ResourcePolicy> resourcePolicies) {
        try {
            for (ResourcePolicy resourcePolicy : resourcePolicies) {
                ResourcePolicyBuilder.delete(resourcePolicy.getID());
            }

        } catch (SQLException | SearchServiceException | IOException sqlException) {
            log.error(sqlException.getMessage());
        }
    }
}
