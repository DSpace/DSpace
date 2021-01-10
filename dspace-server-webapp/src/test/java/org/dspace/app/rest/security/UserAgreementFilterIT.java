/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test class that functionality of {@link UserAgreementFilter}
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UserAgreementFilterIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private EPersonService ePersonService;

    @Test
    public void tryToAccessToItemRestEndpoinWithUserAgreementFilterDisabledTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();

        Item privateItem = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2020-10-14")
                                      .withAuthor("Smith, Donald")
                                      .build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        getClient(tokenEperson).perform(get("/api/core/items/" + privateItem.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$", is(ItemMatcher
                                  .matchItemWithTitleAndDateIssued(privateItem, "Public item 1", "2020-10-14"))));
    }

    @Test
    public void tryToAccessToItemRestEndpoinWithoutAcceptedTermsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // enabled filter that require to accept terms
        configurationService.setProperty("user-agreement.filter-enabled", "true");

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Mykhaylo", "Boychuk")
                                      .withEmail("user.a@example.com")
                                      .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();

        Item privateItem = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2020-10-14")
                                      .withAuthor("Smith, Donald")
                                      .build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(userA.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        // user who has not accepted the terms the request must fail
        // for a 403 Forbidden if the url is not among the open ones
        getClient(tokenEperson).perform(get("/api/core/items/" + privateItem.getID()))
                               .andExpect(status().isForbidden());

        getClient(tokenAdmin).perform(get("/api/core/items/" + privateItem.getID()))
                             .andExpect(status().isForbidden());

        configurationService.setProperty("user-agreement.filter-enabled", "false");
    }

    @Test
    public void tryToAccessToItemRestEndpoinWithAcceptedTermsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // enabled filter that require to accept terms
        configurationService.setProperty("user-agreement.filter-enabled", "true");

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Stas", "Senyk")
                                      .withEmail("user.b@example.com")
                                      .withPassword(password).build();

        // accept terms
        ePersonService.addMetadata(context, userA, "dspace", "agreements", "end-user", "en", "true");

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();

        Item privateItem = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2020-10-14")
                                      .withAuthor("Smith, Donald")
                                      .build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(userA.getEmail(), password);

        getClient(tokenEperson).perform(get("/api/core/items/" + privateItem.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$", is(ItemMatcher
                                  .matchItemWithTitleAndDateIssued(privateItem, "Public item 1", "2020-10-14"))));

        configurationService.setProperty("user-agreement.filter-enabled", "false");
    }


    @Test
    public void userLoggedWithoutAcceptedTermsHasAccessToOpenedPathTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // enabled filter that require to accept terms
        configurationService.setProperty("user-agreement.filter-enabled", "true");

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Stas", "Senyk")
                                      .withEmail("user.b@example.com")
                                      .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();

        Item privateItem = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2020-10-14")
                                      .withAuthor("Smith, Donald")
                                      .build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(userA.getEmail(), password);

        getClient(tokenEperson).perform(get("/api/core/items/" + privateItem.getID()))
                               .andExpect(status().isForbidden());

        String[] oldOpenPathPatterns = configurationService.getArrayProperty("user-agreement.open-path-patterns");
        // add /api/core/items/ to the open-path
        configurationService.setProperty("user-agreement.open-path-patterns", "/api/core/items/**");

        getClient(tokenEperson).perform(get("/api/core/items/" + privateItem.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$", is(ItemMatcher
                                  .matchItemWithTitleAndDateIssued(privateItem, "Public item 1", "2020-10-14"))));

        configurationService.setProperty("user-agreement.filter-enabled", "false");
        resetOpenPathConfigurations(oldOpenPathPatterns);
    }

    @Test
    public void userNotLoggedHasAccessToOpenedPathTest() throws Exception {
        // enabled filter that require to accept terms
        configurationService.setProperty("user-agreement.filter-enabled", "true");

        getClient().perform(get("/api/authn/status"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.okay", is(true)))
                   .andExpect(jsonPath("$.authenticated", is(false)))
                   .andExpect(jsonPath("$.type", is("status")));

        configurationService.setProperty("user-agreement.filter-enabled", "false");
    }

    @Test
    public void tryToAccessToItemRestEndpointUserThatCanIgnoreTermsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // enabled filter that require to accept terms
        configurationService.setProperty("user-agreement.filter-enabled", "true");

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Mykhaylo", "Boychuk")
                                      .withEmail("user.a@example.com")
                                      .withPassword(password).build();

        // userA can ignore the terms
        ePersonService.addMetadata(context, userA, "dspace", "agreements", "ignore", "en", "true");

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();

        Item publicItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item 1")
                                     .withIssueDate("2020-10-14")
                                     .withAuthor("Smith, Donald")
                                     .build();

        context.restoreAuthSystemState();

        String tokenUserA = getAuthToken(userA.getEmail(), password);
        getClient(tokenUserA).perform(get("/api/core/items/" + publicItem.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", is(ItemMatcher
                                .matchItemWithTitleAndDateIssued(publicItem, "Public item 1", "2020-10-14"))));

        configurationService.setProperty("user-agreement.filter-enabled", "false");
    }

    private void resetOpenPathConfigurations(String[] values) {
        configurationService.getConfiguration().clearProperty("user-agreement.open-path-patterns");
        if (values != null) {
            for (String value : values) {
                configurationService.addPropertyValue("user-agreement.open-path-patterns", value);
            }
        }
    }
}
