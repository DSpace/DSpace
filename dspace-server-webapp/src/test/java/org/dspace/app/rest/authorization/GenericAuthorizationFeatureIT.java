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
        ResourcePolicyBuilder.createResourcePolicy(context, null, item1AdminGroup)
            .withDspaceObject(item1)
            .withAction(Constants.ADMIN)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, communityAWriter, null)
            .withDspaceObject(communityA)
            .withAction(Constants.WRITE)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, collectionXWriter, null)
            .withDspaceObject(collectionX)
            .withAction(Constants.WRITE)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, item1Writer, null)
            .withDspaceObject(item1)
            .withAction(Constants.WRITE)
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

        configurationService.setProperty(
            "org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", "true");
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
        final String feature = "canMove";

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
        ResourcePolicy removePermission = ResourcePolicyBuilder.createResourcePolicy(context, item1Writer, null)
            .withDspaceObject(collectionX)
            .withAction(Constants.REMOVE)
            .build();
        context.restoreAuthSystemState();

        // verify item 1 write has this feature on item 1
        getClient(item1WriterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
                + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canMove')]")
                .exists());
    }

    @Test
    public void testCanMoveWriter() throws Exception {
        testWriteUsersHavePermissionsItem("canMove", false);

        // grant item 1 write REMOVE permissions on the item’s owning collection
        context.turnOffAuthorisationSystem();
        ResourcePolicy removePermission = ResourcePolicyBuilder.createResourcePolicy(context, item1Writer, null)
            .withDspaceObject(collectionX)
            .withAction(Constants.REMOVE)
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

    @Test
    public void testCanDeleteAdmin() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        String communityAAdminToken = getAuthToken(communityAAdmin.getEmail(), password);
        String collectionXAdminToken = getAuthToken(collectionXAdmin.getEmail(), password);
        String item1AdminToken = getAuthToken(item1Admin.getEmail(), password);
        String siteId = ContentServiceFactory.getInstance().getSiteService().findSite(context).getID().toString();
        final String feature = "canDelete";

        // Verify the general admin doesn’t have this feature on the site
        getClient(adminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
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
        getClient(communityAAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/communities/" + communityA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin has this feature on community AA
        getClient(communityAAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/communities/" + communityAA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Create a community AA admin and verify the community AA admin doesn’t have this feature on community AA
        context.turnOffAuthorisationSystem();
        EPerson communityAAAdmin = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("communityAAAdmin@my.edu")
            .withPassword(password)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, communityAAAdmin, null)
            .withDspaceObject(communityAA)
            .withAction(Constants.ADMIN)
            .build();
        context.restoreAuthSystemState();
        String communityAAAdminToken = getAuthToken(communityAAAdmin.getEmail(), password);
        getClient(communityAAAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/communities/" + communityAA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify collection X admin doesn’t have this feature on community A
        getClient(collectionXAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/communities/" + communityA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify community A admin doesn’t have this feature on community B
        getClient(communityAAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/communities/" + communityB.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify the general admin has this feature on collection X
        getClient(adminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/collections/" + collectionX.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin has this feature on collection X
        getClient(communityAAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/collections/" + collectionX.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify collection X admin doesn’t have this feature on collection X
        getClient(collectionXAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/collections/" + collectionX.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify item 1 admin doesn’t have this feature on collection X
        getClient(item1AdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/collections/" + collectionX.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify collection X admin doesn’t have this feature on collection Y
        getClient(collectionXAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/collections/" + collectionY.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify the general admin has this feature on item 1
        getClient(adminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin has this feature on item 1
        getClient(communityAAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify collection X admin has this feature on item 1
        getClient(collectionXAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin doesn’t have this feature on item 1
        getClient(item1AdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify item 1 admin doesn’t have this feature on item 2
        getClient(item1AdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item2.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify the general admin has this feature on the bundle in item 1
        getClient(adminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin has this feature on the bundle in item 1
        getClient(communityAAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify collection X admin has this feature on the bundle in item 1
        getClient(collectionXAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin has this feature on the bundle in item 1
        getClient(item1AdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin doesn’t have this feature on the bundle in item 2
        getClient(item1AdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle2.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify the general admin has this feature on the bitstream in item 1
        getClient(adminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bitstreams/" + bitstream1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin has this feature on the bitstream in item 1
        getClient(communityAAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bitstreams/" + bitstream1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify collection X admin has this feature on the bitstream in item 1
        getClient(collectionXAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bitstreams/" + bitstream1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin has this feature on the bitstream in item 1
        getClient(item1AdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bitstreams/" + bitstream1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin doesn’t have this feature on the bitstream in item 2
        getClient(item1AdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bitstreams/" + bitstream2.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());
    }

    @Test
    public void testCanDeleteAdminParent() throws Exception {
        String collectionXAdminToken = getAuthToken(collectionXAdmin.getEmail(), password);
        String item1AdminToken = getAuthToken(item1Admin.getEmail(), password);
        final String feature = "canDelete";

        // Create a community AA admin, grant REMOVE permissions on community A to this user
        context.turnOffAuthorisationSystem();
        EPerson communityAAAdmin = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("communityAAAdmin@my.edu")
            .withPassword(password)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, communityAAAdmin, null)
            .withDspaceObject(communityA)
            .withAction(Constants.REMOVE)
            .build();
        context.restoreAuthSystemState();
        String communityAAAdminToken = getAuthToken(communityAAAdmin.getEmail(), password);
        //verify the community AA admin has this feature on community AA
        getClient(communityAAAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/communities/" + communityAA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Grant REMOVE permissions on community AA for collection X admin
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, collectionXAdmin, null)
            .withDspaceObject(communityAA)
            .withAction(Constants.REMOVE)
            .build();
        context.restoreAuthSystemState();
        // verify collection X admin has this feature on collection X
        getClient(collectionXAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/collections/" + collectionX.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Grant REMOVE permissions on collection X for item 1 admin
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, item1Admin, null)
            .withDspaceObject(collectionX)
            .withAction(Constants.REMOVE)
            .build();
        context.restoreAuthSystemState();
        // verify item 1 admin has this feature on item 1
        getClient(item1AdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());
    }

    @Test
    public void testCanDeleteWriter() throws Exception {
        testWriteUsersHavePermissionsAllDso("canManagePolicies", false);
    }

    @Test
    public void testCanDeleteMinimalPermissions() throws Exception {
        final String feature = "canDelete";

        // Create a new user, grant DELETE permissions on community A to this user
        context.turnOffAuthorisationSystem();
        EPerson communityADeleter = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("communityADeleter@my.edu")
            .withPassword(password)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, communityADeleter, null)
            .withDspaceObject(communityA)
            .withAction(Constants.DELETE)
            .build();
        context.restoreAuthSystemState();
        String communityADeleterToken = getAuthToken(communityADeleter.getEmail(), password);
        // Verify the user has this feature on community A
        getClient(communityADeleterToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/communities/" + communityA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());
        // Verify this user doesn’t have this feature on community AA
        getClient(communityADeleterToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/communities/" + communityAA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());


        // Create a new user, grant REMOVE permissions on community A to this user
        context.turnOffAuthorisationSystem();
        EPerson communityARemover = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("communityARemover@my.edu")
            .withPassword(password)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, communityARemover, null)
            .withDspaceObject(communityA)
            .withAction(Constants.REMOVE)
            .build();
        context.restoreAuthSystemState();
        String communityARemoverToken = getAuthToken(communityARemover.getEmail(), password);
        // Verify the user has this feature on community AA
        getClient(communityARemoverToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/communities/" + communityAA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());
        // Verify this user doesn’t have this feature on community A
        getClient(communityARemoverToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/communities/" + communityA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());
        // Verify this user doesn’t have this feature on collection X
        getClient(communityARemoverToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/collections/" + collectionX.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Create a new user, grant REMOVE permissions on community AA to this user
        context.turnOffAuthorisationSystem();
        EPerson communityAARemover = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("communityAARemover@my.edu")
            .withPassword(password)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, communityAARemover, null)
            .withDspaceObject(communityAA)
            .withAction(Constants.REMOVE)
            .build();
        context.restoreAuthSystemState();
        String communityAARemoverToken = getAuthToken(communityAARemover.getEmail(), password);
        // Verify the user has this feature on collection X
        getClient(communityAARemoverToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/collections/" + collectionX.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());
        // Verify this user doesn’t have this feature on community AA
        getClient(communityAARemoverToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/communities/" + communityAA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());
        // Verify this user doesn’t have this feature on item 1
        getClient(communityAARemoverToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Create a new user, grant REMOVE permissions on collection X to this user
        context.turnOffAuthorisationSystem();
        EPerson collectionXRemover = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("communityXRemover@my.edu")
            .withPassword(password)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, collectionXRemover, null)
            .withDspaceObject(collectionX)
            .withAction(Constants.REMOVE)
            .build();
        context.restoreAuthSystemState();
        String collectionXRemoverToken = getAuthToken(collectionXRemover.getEmail(), password);
        // Verify the user doesn’t have this feature on item 1
        getClient(collectionXRemoverToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Create a new user, grant DELETE permissions on item 1 to this user
        context.turnOffAuthorisationSystem();
        EPerson item1Deleter = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("item1Deleter@my.edu")
            .withPassword(password)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, item1Deleter, null)
            .withDspaceObject(item1)
            .withAction(Constants.DELETE)
            .build();
        context.restoreAuthSystemState();
        String item1DeleterToken = getAuthToken(item1Deleter.getEmail(), password);
        // Verify the user doesn’t have this feature on item 1
        getClient(item1DeleterToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Create a new user, grant REMOVE permissions on collection X and DELETE permissions on item 1 to this user
        context.turnOffAuthorisationSystem();
        EPerson collectionXRemoverItem1Deleter = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("collectionXDeleter@my.edu")
            .withPassword(password)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, collectionXRemoverItem1Deleter, null)
            .withDspaceObject(collectionX)
            .withAction(Constants.REMOVE)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, collectionXRemoverItem1Deleter, null)
            .withDspaceObject(item1)
            .withAction(Constants.DELETE)
            .build();
        context.restoreAuthSystemState();
        String collectionXRemoverItem1DeleterToken = getAuthToken(collectionXRemoverItem1Deleter.getEmail(), password);
        // Verify the user has this feature on item 1
        getClient(collectionXRemoverItem1DeleterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());
        // Verify this user doesn’t have this feature on collection X
        getClient(collectionXRemoverItem1DeleterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/collections/" + collectionX.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());
        // Verify this user doesn’t have this feature on the bundle in item 1
        getClient(collectionXRemoverItem1DeleterToken).perform(
            get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Create a new user, grant REMOVE permissions on item 1 to this user
        context.turnOffAuthorisationSystem();
        EPerson item1Remover = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("item1Remover@my.edu")
            .withPassword(password)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, item1Remover, null)
            .withDspaceObject(item1)
            .withAction(Constants.REMOVE)
            .build();
        context.restoreAuthSystemState();
        String item1RemoverToken = getAuthToken(item1Remover.getEmail(), password);
        // Verify the user has this feature on the bundle in item 1
        getClient(item1RemoverToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());
        // Verify this user doesn’t have this feature on item 1
        getClient(item1RemoverToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());
        // Verify this user doesn’t have this feature on the bitstream in item 1
        getClient(item1RemoverToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bitstreams/" + bitstream1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Create a new user, grant REMOVE permissions on the bundle in item 1 to this user
        context.turnOffAuthorisationSystem();
        EPerson bundle1Remover = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("bundle1Remover@my.edu")
            .withPassword(password)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, bundle1Remover, null)
            .withDspaceObject(bundle1)
            .withAction(Constants.REMOVE)
            .build();
        context.restoreAuthSystemState();
        String bundle1RemoverToken = getAuthToken(bundle1Remover.getEmail(), password);
        // Verify the user doesn’t have this feature on the bitstream in item 1
        getClient(bundle1RemoverToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bitstreams/" + bitstream1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Create a new user, grant REMOVE permissions on the bundle in item 1
        // and REMOVE permissions on item 1 to this user
        context.turnOffAuthorisationSystem();
        EPerson bundle1item1Remover = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("bundle1item1Remover@my.edu")
            .withPassword(password)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, bundle1item1Remover, null)
            .withDspaceObject(bundle1)
            .withAction(Constants.REMOVE)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, bundle1item1Remover, null)
            .withDspaceObject(item1)
            .withAction(Constants.REMOVE)
            .build();
        context.restoreAuthSystemState();
        String bundle1item1RemoverToken = getAuthToken(bundle1item1Remover.getEmail(), password);
        // Verify the user has this feature on the bitstream in item 1
        getClient(bundle1item1RemoverToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bitstreams/" + bitstream1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());
    }

    @Test
    public void testCanReorderBitstreamsAdmin() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        String communityAAdminToken = getAuthToken(communityAAdmin.getEmail(), password);
        String collectionXAdminToken = getAuthToken(collectionXAdmin.getEmail(), password);
        String item1AdminToken = getAuthToken(item1Admin.getEmail(), password);
        final String feature = "canReorderBitstreams";

        // Verify the general admin has this feature on the bundle in item 1
        getClient(adminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin has this feature on the bundle in item 1
        getClient(communityAAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify collection X admin has this feature on the bundle in item 1
        getClient(collectionXAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin has this feature on the bundle in item 1
        getClient(item1AdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin doesn’t have this feature on the bundle in item 2
        getClient(communityAAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle2.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());
    }

    @Test
    public void testCanReorderBitstreamsWriter() throws Exception {
        String communityAWriterToken = getAuthToken(communityAWriter.getEmail(), password);
        String collectionXWriterToken = getAuthToken(collectionXWriter.getEmail(), password);
        String item1WriterToken = getAuthToken(item1Writer.getEmail(), password);
        final String feature = "canReorderBitstreams";

        // Verify community A write doesn’t have this feature on the bundle in item 1
        getClient(communityAWriterToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());
        // Verify collection X write doesn’t have this feature on the bundle in item 1
        getClient(collectionXWriterToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());
        // Verify item 1 write doesn’t have this feature on the bundle in item 1
        getClient(item1WriterToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Create a new user, grant WRITE permissions on the bundle in item 1 to this user
        // Verify the user has this feature on the bundle in item 1
        getClient(communityAWriterToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());
    }

    @Test
    public void testCanCreateBitstreamAdmin() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        String communityAAdminToken = getAuthToken(communityAAdmin.getEmail(), password);
        String collectionXAdminToken = getAuthToken(collectionXAdmin.getEmail(), password);
        String item1AdminToken = getAuthToken(item1Admin.getEmail(), password);
        final String feature = "canCreateBitstream";

        // Verify the general admin has this feature on the bundle in item 1
        getClient(adminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin has this feature on the bundle in item 1
        getClient(communityAAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify collection X admin has this feature on the bundle in item 1
        getClient(collectionXAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify item 1 admin has this feature on the bundle in item 1
        getClient(item1AdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());

        // Verify community A admin doesn’t have this feature on the bundle in item 2
        getClient(communityAAdminToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle2.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());
    }

    @Test
    public void testCanCreateBitstreamWriter() throws Exception {
        String communityAWriterToken = getAuthToken(communityAWriter.getEmail(), password);
        String collectionXWriterToken = getAuthToken(collectionXWriter.getEmail(), password);
        String item1WriterToken = getAuthToken(item1Writer.getEmail(), password);
        final String feature = "canCreateBitstream";

        // Verify community A write doesn’t have this feature on the bundle in item 1
        getClient(communityAWriterToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify collection X write doesn’t have this feature on the bundle in item 1
        getClient(collectionXWriterToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify item 1 write doesn’t have this feature on the bundle in item 1
        getClient(item1WriterToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Create a new user, grant WRITE permissions on the bundle in item 1 to this user
        context.turnOffAuthorisationSystem();
        EPerson bundle1Writer = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("bundle1Writer@my.edu")
            .withPassword(password)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, bundle1Writer, null)
            .withDspaceObject(bundle1)
            .withAction(Constants.WRITE)
            .build();
        context.restoreAuthSystemState();
        String bundle1WriterToken = getAuthToken(bundle1Writer.getEmail(), password);
        // Verify the user doesn’t have this feature on the bundle in item 1
        getClient(bundle1WriterToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Create a new user, grant ADD permissions on the bundle in item 1 to this user
        context.turnOffAuthorisationSystem();
        EPerson bundle1Adder = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("bundle1Adder@my.edu")
            .withPassword(password)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, bundle1Adder, null)
            .withDspaceObject(bundle1)
            .withAction(Constants.ADD)
            .build();
        context.restoreAuthSystemState();
        String bundle1AdderToken = getAuthToken(bundle1Adder.getEmail(), password);
        // Verify the user doesn’t have this feature on the bundle in item 1
        getClient(bundle1AdderToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Create a new user, grant ADD and WRITE permissions on the bundle in item 1
        // and ADD and WRITE permission on the item to this user
        context.turnOffAuthorisationSystem();
        EPerson bundle1WriterAdder = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("bundle1WriterAdder@my.edu")
            .withPassword(password)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, bundle1WriterAdder, null)
            .withDspaceObject(bundle1)
            .withAction(Constants.ADD)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, bundle1WriterAdder, null)
            .withDspaceObject(bundle1)
            .withAction(Constants.WRITE)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, bundle1WriterAdder, null)
            .withDspaceObject(item1)
            .withAction(Constants.ADD)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, bundle1WriterAdder, null)
            .withDspaceObject(item1)
            .withAction(Constants.WRITE)
            .build();
        context.restoreAuthSystemState();
        String bundle1WriterAdderToken = getAuthToken(bundle1WriterAdder.getEmail(), password);
        // Verify the user has this feature on the bundle in item 1
        getClient(bundle1WriterAdderToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());
    }

    @Test
    public void testCanCreateBundleAdmin() throws Exception {
        testAdminsHavePermissionsItem("canCreateBundle");
    }

    @Test
    public void testCanCreateBundleWriter() throws Exception {
        String communityAWriterToken = getAuthToken(communityAWriter.getEmail(), password);
        String collectionXWriterToken = getAuthToken(collectionXWriter.getEmail(), password);
        String item1WriterToken = getAuthToken(item1Writer.getEmail(), password);
        final String feature = "canCreateBundle";

        // Verify community A write doesn’t have this feature on item 1
        getClient(communityAWriterToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify collection X write doesn’t have this feature on item 1
        getClient(collectionXWriterToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Verify item 1 write doesn’t have this feature on item 1
        getClient(item1WriterToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").doesNotExist());

        // Create a new user, grant ADD and WRITE permissions on item 1 to this user
        context.turnOffAuthorisationSystem();
        EPerson item1AdderWriter = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("item1AdderWriter@my.edu")
            .withPassword(password)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, item1AdderWriter, null)
            .withDspaceObject(item1)
            .withAction(Constants.ADD)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, item1AdderWriter, null)
            .withDspaceObject(item1)
            .withAction(Constants.WRITE)
            .build();
        context.restoreAuthSystemState();
        String item1AdderWriterToken = getAuthToken(item1AdderWriter.getEmail(), password);
        // Verify the user has this feature on item 1
        getClient(item1AdderWriterToken).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/items/" + item1.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='"
                + feature + "')]").exists());
    }
}
