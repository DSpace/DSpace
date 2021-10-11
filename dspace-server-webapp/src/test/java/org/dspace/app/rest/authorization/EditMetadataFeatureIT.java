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

import org.dspace.app.rest.authorization.impl.EditMetadataFeature;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * Test for the EditMetadataFeature authorization feature.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class EditMetadataFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemConverter itemConverter;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    private Item itemA;
    private Group groupA;
    private EPerson user;
    private Community communityA;
    private Collection collectionA;
    private AuthorizationFeature canEditMetadataFeature;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        canEditMetadataFeature = authorizationFeatureService.find(EditMetadataFeature.NAME);

        user = EPersonBuilder.createEPerson(context)
                             .withEmail("userEmail@test.com")
                             .withPassword(password).build();

        communityA = CommunityBuilder.createCommunity(context)
                                     .withName("communityA").build();

        collectionA = CollectionBuilder.createCollection(context, communityA)
                                       .withName("collectionA").build();

        itemA = ItemBuilder.createItem(context, collectionA)
                           .withTitle("Item A")
                           .build();

        groupA = GroupBuilder.createGroup(context)
                             .withName("Group A")
                             .addMember(user)
                             .build();

        context.restoreAuthSystemState();

    }

    @Test
    public void checkCanEditMetadataFeatureTest() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("edit.metadata.allowed-group", groupA.getID());
        ResourcePolicyBuilder.createResourcePolicy(context)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .withUser(user)
                             .build();

        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenUser = getAuthToken(user.getEmail(), password);

        // define authorizations that we know must exists
        Authorization user2ItemA = new Authorization(user, canEditMetadataFeature, itemRestA);

        // define authorization that we know not exists
        Authorization admin2ItemA = new Authorization(eperson, canEditMetadataFeature, itemRestA);
        Authorization anonymous2ItemA = new Authorization(null, canEditMetadataFeature, itemRestA);
        Authorization eperson2ItemA = new Authorization(eperson, canEditMetadataFeature, itemRestA);

        getClient(tokenUser).perform(get("/api/authz/authorizations/" + user2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(user2ItemA))));

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + eperson2ItemA.getID()))
                               .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                             .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + anonymous2ItemA.getID()))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void checkCanEditMetadataFeatureWithDefaulGroupUnsetTest() throws Exception {
        context.turnOffAuthorisationSystem();

        ResourcePolicyBuilder.createResourcePolicy(context)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .withUser(user)
                             .build();

        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenUser = getAuthToken(user.getEmail(), password);

        // define authorizations that we know must exists
        Authorization user2ItemA = new Authorization(user, canEditMetadataFeature, itemRestA);

        // define authorization that we know not exists
        Authorization admin2ItemA = new Authorization(eperson, canEditMetadataFeature, itemRestA);
        Authorization anonymous2ItemA = new Authorization(null, canEditMetadataFeature, itemRestA);
        Authorization eperson2ItemA = new Authorization(eperson, canEditMetadataFeature, itemRestA);

        getClient(tokenUser).perform(get("/api/authz/authorizations/" + user2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(user2ItemA))));

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + eperson2ItemA.getID()))
                               .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                             .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + anonymous2ItemA.getID()))
                   .andExpect(status().isNotFound());
    }

}