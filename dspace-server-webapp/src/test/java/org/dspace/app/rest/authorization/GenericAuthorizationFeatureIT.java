package org.dspace.app.rest.authorization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the following authorization features:
 *     canManagePolicies
 *     canEditMetadata
 *     canMove
 *     canMakePrivate
 *     canMakeDiscoverable
 *     canDelete
 *     canReorderBitstreams
 *     canCreateBitstream
 *     canCreateBundle
 */
public class GenericAuthorizationFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConfigurationService configurationService;

    private Community communityA;
    private Community communityAA;
    private Community communityB;
    private Community communityBB;
    private Collection collectionX;
    private Collection collectionY;
    private Item item1;
    private Item item2;
    private Bundle bundle1;
    private Bundle bundle2;
    private Bitstream bitstream1;
    private Bitstream bitstream2;

    private Group item1AdminGroup;

    private EPerson communityAAdmin;
    private EPerson collectionXAdmin;
    private EPerson item1Admin;
    private EPerson communityAWriter;
    private EPerson collectionXWriter;
    private EPerson item1Writer;

    boolean originalAlwaysThrowException;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        communityAAdmin = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("communityAAdmin@my.edu")
            .withPassword(password)
            .build();
        collectionXAdmin = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("collectionXAdmin@my.edu")
            .withPassword(password)
            .build();
        item1Admin = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("item1Admin@my.edu")
            .withPassword(password)
            .build();
        communityAWriter = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("communityAWriter@my.edu")
            .withPassword(password)
            .build();
        collectionXWriter = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("collectionXWriter@my.edu")
            .withPassword(password)
            .build();
        item1Writer = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("item1Writer@my.edu")
            .withPassword(password)
            .build();

        communityA = CommunityBuilder.createCommunity(context)
            .withName("communityA")
            .withAdminGroup(communityAAdmin)
            .build();
        communityAA = CommunityBuilder.createCommunity(context)
            .withName("communityAA")
            .addParentCommunity(context, communityA)
            .build();
        collectionX = CollectionBuilder.createCollection(context, communityAA)
            .withName("collectionX")
            .withAdminGroup(collectionXAdmin)
            .build();
        item1 = ItemBuilder.createItem(context, collectionX)
            .withTitle("item1")
            .withIssueDate("2020-07-08")
            .withAuthor("Smith, Donald").withAuthor("Doe, John")
            .withSubject("item1Entry")
            .build();
        bundle1 = BundleBuilder.createBundle(context, item1)
            .withName("bundle1")
            .build();
        try (InputStream is = IOUtils.toInputStream("randomContent", CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, bundle1, is)
                .withName("bitstream1")
                .withMimeType("text/plain")
                .build();
        }

        item1AdminGroup = GroupBuilder.createGroup(context)
            .withName("item1AdminGroup")
            .addMember(item1Admin)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(item1)
            .withAction(Constants.ADMIN)
            .withGroup(item1AdminGroup)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(communityA)
            .withAction(Constants.WRITE)
            .withUser(communityAWriter)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collectionX)
            .withAction(Constants.WRITE)
            .withUser(collectionXWriter)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(item1)
            .withAction(Constants.WRITE)
            .withUser(item1Writer)
            .build();

        communityB = CommunityBuilder.createCommunity(context)
            .withName("communityB")
            .build();
        communityBB = CommunityBuilder.createCommunity(context)
            .withName("communityBB")
            .addParentCommunity(context, communityB)
            .build();
        collectionY = CollectionBuilder.createCollection(context, communityBB)
            .withName("collectionY")
            .build();
        item2 = ItemBuilder.createItem(context, collectionY)
            .withTitle("item2")
            .withIssueDate("2020-07-08")
            .withAuthor("Smith, Donald").withAuthor("Doe, John")
            .withSubject("item2Entry")
            .build();
        bundle2 = BundleBuilder.createBundle(context, item2)
            .withName("bundle2")
            .build();
        try (InputStream is = IOUtils.toInputStream("randomContent", CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder.createBitstream(context, bundle2, is)
                .withName("bitstream2")
                .withMimeType("text/plain")
                .build();
        }

        context.restoreAuthSystemState();

        originalAlwaysThrowException = configurationService.getBooleanProperty(
            "org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", false);
        configurationService.setProperty(
            "org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", "true");
    }

    @Override
    @After
    public void destroy() throws Exception {
        configurationService.setProperty(
            "org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", originalAlwaysThrowException);
        super.destroy();
    }

    private void testAdminsHavePermissionsAllDso(String feature) throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        String communityAAdminToken = getAuthToken(communityAAdmin.getEmail(), password);
        String collectionXAdminToken = getAuthToken(collectionXAdmin.getEmail(), password);
        String item1AdminToken = getAuthToken(item1Admin.getEmail(), password);
        String siteId = ContentServiceFactory.getInstance().getSiteService().findSite(context).getID().toString();

        // Verify the general admin has this feature on the site
        getClient(adminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin doesn’t have this feature on the site
        getClient(communityAAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/sites/" + siteId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify the general admin has this feature on community A
        getClient(adminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/communities/" + communityA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin has this feature on community A
        getClient(communityAAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/communities/" + communityA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin has this feature on community AA
        getClient(communityAAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/communities/" + communityAA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify collection X admin doesn’t have this feature on community A
        getClient(collectionXAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/communities/" + communityA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify community A admin doesn’t have this feature on community B
        getClient(communityAAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/communities/" + communityB.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify the general admin has this feature on collection X
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/collections/" + collectionX.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin has this feature on collection X
        getClient(communityAAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/collections/" + collectionX.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify collection X admin has this feature on collection X
        getClient(collectionXAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/collections/" + collectionX.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin doesn’t have this feature on collection X
        getClient(item1AdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/collections/" + collectionX.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify collection X admin doesn’t have this feature on collection Y
        getClient(collectionXAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/collections/" + collectionY.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify the general admin has this feature on item 1
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin has this feature on item 1
        getClient(communityAAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify collection X admin has this feature on item 1
        getClient(collectionXAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin has this feature on item 1
        getClient(item1AdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin doesn’t have this feature on item 2
        getClient(item1AdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item2.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify the general admin has this feature on the bundle in item 1
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin has this feature on the bundle in item 1
        getClient(communityAAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify collection X admin has this feature on the bundle in item 1
        getClient(collectionXAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin has this feature on the bundle in item 1
        getClient(item1AdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin doesn’t have this feature on the bundle in item 2
        getClient(item1AdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/bundles/" + bundle2.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify the general admin has this feature on the bitstream in item 1
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/bitstreams/" + bitstream1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin has this feature on the bitstream in item 1
        getClient(communityAAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/bitstreams/" + bitstream1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify collection X admin has this feature on the bitstream in item 1
        getClient(collectionXAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/bitstreams/" + bitstream1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin has this feature on the bitstream in item 1
        getClient(item1AdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/bitstreams/" + bitstream1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin doesn’t have this feature on the bitstream in item 2
        getClient(item1AdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/bitstreams/" + bitstream2.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());
    }

    private void testAdminsHavePermissionsItem(String feature) throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        String communityAAdminToken = getAuthToken(communityAAdmin.getEmail(), password);
        String collectionXAdminToken = getAuthToken(collectionXAdmin.getEmail(), password);
        String item1AdminToken = getAuthToken(item1Admin.getEmail(), password);

        // Verify the general admin has this feature on item 1
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin has this feature on item 1
        getClient(communityAAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify collection X admin has this feature on item 1
        getClient(collectionXAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin has this feature on item 1
        getClient(item1AdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin doesn’t have this feature on item 2
        getClient(communityAAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item2.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

    }

    private void testWriteUsersHavePermissionsAllDso(String feature, boolean hasDSOAccess) throws Exception {
        String communityAWriterToken = getAuthToken(communityAWriter.getEmail(), password);
        String collectionXWriterToken = getAuthToken(collectionXWriter.getEmail(), password);
        String item1WriterToken = getAuthToken(item1Writer.getEmail(), password);

        // Verify community A write has this feature on community A if the boolean parameter is true
        // (or doesn’t have access otherwise)
        if (hasDSOAccess) {
            getClient(communityAWriterToken).perform(
                get("/api/authz/authorizations/search/object?embed=feature&uri="
                    + "http://localhost/api/core/communities/" + communityA.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                    + feature + "')]").exists());
        } else {
            getClient(communityAWriterToken).perform(
                get("/api/authz/authorizations/search/object?embed=feature&uri="
                    + "http://localhost/api/core/communities/" + communityA.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                    + feature + "')]").doesNotExist());
        }

        // Verify community A write doesn’t have this feature on community AA
        getClient(communityAWriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/communities/" + communityAA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify community A write doesn’t have this feature on collection X
        getClient(communityAWriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/collections/" + collectionX.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify community A write doesn’t have this feature on item 1
        getClient(communityAWriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify community A write doesn’t have this feature on the bundle in item 1
        getClient(communityAWriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify community A write doesn’t have this feature on the bitstream in item 1
        getClient(communityAWriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify collection X write doesn’t have this feature on community A
        getClient(collectionXWriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/communities/" + communityA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify collection X write doesn’t have this feature on community AA
        getClient(collectionXWriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/communities/" + communityAA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify collection X write has this feature on collection X if the boolean parameter is true
        // (or doesn’t have access otherwise)
        if (hasDSOAccess) {
            getClient(collectionXWriterToken).perform(
                get("/api/authz/authorizations/search/object?embed=feature&uri="
                    + "http://localhost/api/core/collections/" + collectionX.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                    + feature + "')]").exists());
        } else {
            getClient(collectionXWriterToken).perform(
                get("/api/authz/authorizations/search/object?embed=feature&uri="
                    + "http://localhost/api/core/collections/" + collectionX.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                    + feature + "')]").doesNotExist());
        }

        // Verify collection X write doesn’t have this feature on item 1
        getClient(collectionXWriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify collection X write doesn’t have this feature on the bundle in item 1
        getClient(collectionXWriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify collection X write doesn’t have this feature on the bitstream in item 1
        getClient(collectionXWriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/bitstreams/" + bitstream1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify item 1 write doesn’t have this feature on community A
        getClient(item1WriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/communities/" + communityA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify item 1 write doesn’t have this feature on community AA
        getClient(item1WriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/communities/" + communityAA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify item 1 write doesn’t have this feature on collection X
        getClient(item1WriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/collections/" + collectionX.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify item 1 write has this feature on item 1 if the boolean parameter is true
        // (or doesn’t have access otherwise)
        if (hasDSOAccess) {
            getClient(item1WriterToken).perform(
                get("/api/authz/authorizations/search/object?embed=feature&uri="
                    + "http://localhost/api/core/items/" + item1.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                    + feature + "')]").exists());
        } else {
            getClient(item1WriterToken).perform(
                get("/api/authz/authorizations/search/object?embed=feature&uri="
                    + "http://localhost/api/core/items/" + item1.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                    + feature + "')]").doesNotExist());
        }

        // Verify item 1 write doesn’t have this feature on the bundle in item 1
        getClient(item1WriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify item 1 write doesn’t have this feature on the bitstream in item 1
        getClient(item1WriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/bitstreams/" + bitstream1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify community A write doesn’t have this feature on community B
        getClient(communityAWriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/communities/" + communityB.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify collection X write doesn’t have this feature on collection Y
        getClient(collectionXWriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/collections/" + collectionY.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify item 1 write doesn’t have this feature on item 2
        getClient(item1WriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item2.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());
    }

    private void testWriteUsersHavePermissionsItem(String feature, boolean hasDSOAccess) throws Exception {
        String communityAWriterToken = getAuthToken(communityAWriter.getEmail(), password);
        String collectionXWriterToken = getAuthToken(collectionXWriter.getEmail(), password);
        String item1WriterToken = getAuthToken(item1Writer.getEmail(), password);

        // Verify community A write doesn’t have this feature on item 1
        getClient(communityAWriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify collection X write doesn’t have this feature on item 1
        getClient(collectionXWriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify item 1 write has this feature on item 1 if the boolean parameter is true
        // (or doesn’t have access otherwise)
        if (hasDSOAccess) {
            getClient(item1WriterToken).perform(
                get("/api/authz/authorizations/search/object?embed=feature&uri="
                    + "http://localhost/api/core/items/" + item1.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                    + feature + "')]").exists());
        } else {
            getClient(item1WriterToken).perform(
                get("/api/authz/authorizations/search/object?embed=feature&uri="
                    + "http://localhost/api/core/items/" + item1.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                    + feature + "')]").doesNotExist());
        }

        // Verify item 1 write doesn’t have this feature on item 2
        getClient(item1WriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item2.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

    }

    @Test
    public void testCanManagePoliciesAdmin() throws Exception {
        testWriteUsersHavePermissionsAllDso("canManagePolicies", false);
    }

    @Test
    public void testCanManagePoliciesWriter() throws Exception {
        testWriteUsersHavePermissionsAllDso("canManagePolicies", false);
    }


    @Test
    public void testCanEditMetadataAdmin() throws Exception {
        testAdminsHavePermissionsAllDso("canEditMetadata");
    }

    @Test
    public void testCanEditMetadataWriter() throws Exception {
        testWriteUsersHavePermissionsAllDso("canEditMetadata", true);
    }

    @Test
    public void testCanMoveAdmin() throws Exception {
        String item1WriterToken = getAuthToken(item1Writer.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);
        String communityAAdminToken = getAuthToken(communityAAdmin.getEmail(), password);
        String collectionXAdminToken = getAuthToken(collectionXAdmin.getEmail(), password);
        String item1AdminToken = getAuthToken(item1Admin.getEmail(), password);
        String feature = "canMove";

        // Verify the general admin has this feature on item 1
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin has this feature on item 1
        getClient(communityAAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify collection X admin has this feature on item 1
        getClient(collectionXAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin doesn’t have this feature on item 1
        getClient(item1AdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify community A admin doesn’t have this feature on item 2
        getClient(communityAAdminToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item2.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());


        // grant item 1 admin REMOVE permissions on the item’s owning collection
        // verify item 1 admin has this feature on item 1
        context.turnOffAuthorisationSystem();
        ResourcePolicy removePermission = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collectionX)
            .withAction(Constants.REMOVE)
            .withUser(item1Writer)
            .build();
        context.restoreAuthSystemState();

        // verify item 1 write has this feature on item 1
        getClient(item1WriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canMove')]")
                .exists());

        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.delete(removePermission.getID());
        context.restoreAuthSystemState();
    }

    @Test
    public void testCanMoveWriter() throws Exception {
        testWriteUsersHavePermissionsItem("canMove", false);

        // grant item 1 write REMOVE permissions on the item’s owning collection
        context.turnOffAuthorisationSystem();
        ResourcePolicy removePermission = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collectionX)
            .withAction(Constants.REMOVE)
            .withUser(item1Writer)
            .build();
        context.restoreAuthSystemState();

        String item1WriterToken = getAuthToken(item1Writer.getEmail(), password);
        // verify item 1 write has this feature on item 1
        getClient(item1WriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canMove')]")
                .exists());

        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.delete(removePermission.getID());
        context.restoreAuthSystemState();
    }

    @Test
    public void testCanMakePrivateAdmin() throws Exception {
        testAdminsHavePermissionsItem("canMakePrivate");
    }

    @Test
    public void testCanMakePrivateWriter() throws Exception {
        testWriteUsersHavePermissionsItem("canMakePrivate", true);
    }

    @Test
    public void testCanMakeDiscoverableAdmin() throws Exception {
        testAdminsHavePermissionsItem("canMakeDiscoverable");
    }

    @Test
    public void testCanMakeDiscoverableWriter() throws Exception {
        testWriteUsersHavePermissionsItem("canMakeDiscoverable", true);
    }
}