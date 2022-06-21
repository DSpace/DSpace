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

import org.dspace.app.rest.authorization.impl.CanEditVersionFeature;
import org.dspace.app.rest.converter.VersionConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.VersionBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.Version;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canEditVersion authorization feature.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CanEditVersionFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private VersionConverter versionConverter;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;


    private AuthorizationFeature canEditVersionFeature;

    final String feature = "canEditVersion";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        canEditVersionFeature = authorizationFeatureService.find(CanEditVersionFeature.NAME);

        context.restoreAuthSystemState();
    }

    @Test
    public void canEditVersionsFeatureTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("Parent Community")
                                                  .build();

        Collection col1 = CollectionBuilder.createCollection(context, rootCommunity)
                                           .withName("Collection 1")
                                           .withSubmitterGroup(eperson)
                                           .build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item")
                               .withIssueDate("2021-04-19")
                               .withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        Version version = VersionBuilder.createVersion(context, item, "My test summary").build();

        context.restoreAuthSystemState();

        VersionRest versionRest = versionConverter.convert(version, DefaultProjection.DEFAULT);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEPerson = getAuthToken(eperson.getEmail(), password);

        // define authorizations that we know must exists
        Authorization admin2Version = new Authorization(admin, canEditVersionFeature, versionRest);

        // define authorization that we know not exists
        Authorization eperson2Version = new Authorization(eperson, canEditVersionFeature, versionRest);
        Authorization anonymous2Version = new Authorization(null, canEditVersionFeature, versionRest);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2Version.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$",
                                        Matchers.is(AuthorizationMatcher.matchAuthorization(admin2Version))));

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + eperson2Version.getID()))
                               .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + anonymous2Version.getID()))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void canEditVersionsFeatureByColAndComAdminsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson adminComA = EPersonBuilder.createEPerson(context)
                                          .withEmail("testComAdminA@test.com")
                                          .withPassword(password)
                                          .build();

        EPerson adminComB = EPersonBuilder.createEPerson(context)
                                          .withEmail("testComBdminA@test.com")
                                          .withPassword(password)
                                          .build();

        EPerson adminCol1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("testCol1Admin@test.com")
                                          .withPassword(password)
                                          .build();

        EPerson adminCol2 = EPersonBuilder.createEPerson(context)
                                          .withEmail("testCol2Admin@test.com")
                                          .withPassword(password)
                                          .build();

        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("Parent Community")
                                                  .build();

        Community subCommunityA = CommunityBuilder.createSubCommunity(context, rootCommunity)
                                                  .withName("Sub Community A")
                                                  .withAdminGroup(adminComA)
                                                  .build();

        CommunityBuilder.createSubCommunity(context, rootCommunity)
                        .withName("Sub Community B")
                        .withAdminGroup(adminComB)
                        .build();

        Collection col1 = CollectionBuilder.createCollection(context, subCommunityA)
                                           .withName("Collection 1")
                                           .withSubmitterGroup(eperson)
                                           .withAdminGroup(adminCol1)
                                           .build();

        CollectionBuilder.createCollection(context, subCommunityA)
                         .withName("Collection 2")
                         .withAdminGroup(adminCol2)
                         .build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item")
                               .withIssueDate("2021-04-19")
                               .withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        Version version = VersionBuilder.createVersion(context, item, "My test summary").build();

        context.restoreAuthSystemState();

        VersionRest versionRest = versionConverter.convert(version, DefaultProjection.DEFAULT);

        String tokenAdminComA = getAuthToken(adminComA.getEmail(), password);
        String tokenAdminComB = getAuthToken(adminComB.getEmail(), password);
        String tokenAdminCol1 = getAuthToken(adminCol1.getEmail(), password);
        String tokenAdminCol2 = getAuthToken(adminCol2.getEmail(), password);

        // define authorizations that we know must exists
        Authorization adminOfComAToVersion = new Authorization(adminComA, canEditVersionFeature, versionRest);
        Authorization adminOfCol1ToVersion = new Authorization(adminCol1, canEditVersionFeature, versionRest);

        // define authorization that we know not exists
        Authorization adminOfComBToVersion = new Authorization(adminComB, canEditVersionFeature, versionRest);
        Authorization adminOfCol2ToVersion = new Authorization(adminCol2, canEditVersionFeature, versionRest);

        getClient(tokenAdminComA).perform(get("/api/authz/authorizations/" + adminOfComAToVersion.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.is(
                                            AuthorizationMatcher.matchAuthorization(adminOfComAToVersion))));

        getClient(tokenAdminCol1).perform(get("/api/authz/authorizations/" + adminOfCol1ToVersion.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.is(
                                            AuthorizationMatcher.matchAuthorization(adminOfCol1ToVersion))));

        getClient(tokenAdminComB).perform(get("/api/authz/authorizations/" + adminOfComBToVersion.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenAdminCol2).perform(get("/api/authz/authorizations/" + adminOfCol2ToVersion.getID()))
                                 .andExpect(status().isNotFound());
    }

    @Test
    public void canEditVersionsFeatureByColAndComAdminsTest2() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson adminComA = EPersonBuilder.createEPerson(context)
                                          .withEmail("testComAdminA@test.com")
                                          .withPassword(password)
                                          .build();

        EPerson adminCol1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("testCol1Admin@test.com")
                                          .withPassword(password)
                                          .build();


        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("Parent Community")
                                                  .build();

        Community subCommunityA = CommunityBuilder.createSubCommunity(context, rootCommunity)
                                                  .withName("Sub Community A")
                                                  .withAdminGroup(adminComA)
                                                  .build();

        Collection col1 = CollectionBuilder.createCollection(context, subCommunityA)
                                           .withName("Collection 1")
                                           .withEntityType("Publication")
                                           .withSubmitterGroup(eperson)
                                           .withAdminGroup(adminCol1)
                                           .build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item")
                               .withIssueDate("2021-04-19")
                               .withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        Version version = VersionBuilder.createVersion(context, item, "My test summary").build();

        context.restoreAuthSystemState();

        VersionRest versionRest = versionConverter.convert(version, DefaultProjection.DEFAULT);

        String tokenAdminComA = getAuthToken(adminComA.getEmail(), password);
        String tokenAdminCol1 = getAuthToken(adminCol1.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEPErson = getAuthToken(eperson.getEmail(), password);

        // define authorizations that we know must exists
        Authorization adminOfComAToVersion = new Authorization(adminComA, canEditVersionFeature, versionRest);
        Authorization adminOfCol1ToVersion = new Authorization(adminCol1, canEditVersionFeature, versionRest);
        Authorization adminToVersion = new Authorization(admin, canEditVersionFeature, versionRest);

        // define authorization that we know not exists
        Authorization epersonToVersion = new Authorization(eperson, canEditVersionFeature, versionRest);
        Authorization anonymousToVersion = new Authorization(null, canEditVersionFeature, versionRest);

        getClient(tokenAdminComA).perform(get("/api/authz/authorizations/" + adminOfComAToVersion.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.is(
                                            AuthorizationMatcher.matchAuthorization(adminOfComAToVersion))));

        getClient(tokenAdminCol1).perform(get("/api/authz/authorizations/" + adminOfCol1ToVersion.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.is(
                                            AuthorizationMatcher.matchAuthorization(adminOfCol1ToVersion))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + adminToVersion.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(
                                        AuthorizationMatcher.matchAuthorization(adminToVersion))));

        getClient(tokenEPErson).perform(get("/api/authz/authorizations/" + epersonToVersion.getID()))
                               .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + anonymousToVersion.getID()))
                   .andExpect(status().isNotFound());
    }

}
