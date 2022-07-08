/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.service.VersioningService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration Test to discover it.
 *
 * Before to run only tests:
 * mvn clean install
 *
 * To run single IT tests:
 * mvn test -DskipUnitTests=false -Dtest=[full.package.testClassName]#[testMethodName] -DfailIfNoTests=false
 *
 * ResultActions methods
 * https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/servlet/ResultActions.html
 */
public class AdvancedItemLevelVersioningIT extends AbstractControllerIntegrationTest {

    @Autowired
    private VersioningService versioningService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void createItemWithoutVersionSuffixTest() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("dspace.initialVersionSuffix", false);

        Community versionedCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Community for versioned items")
                                          .build();
        Collection versionedCollection = CollectionBuilder.createCollection(context, versionedCommunity)
                                          .withName("Collection of versioned items").build();

        List<Item> items = new ArrayList();
        // Hibernate 5.x's org.hibernate.dialect.H2Dialect sorts UUIDs as if they are Strings.
        // So, we must compare UUIDs as if they are strings.
        // In Hibernate 6, the H2Dialect has been updated with native UUID type support, at which point
        // we'd need to update the below comparator to compare them as java.util.UUID (which sorts based on RFC 4412).
        Comparator<Item> compareByUUID = Comparator.comparing(i -> i.getID().toString());

        Item publicItem1 = ItemBuilder.createItem(context, versionedCollection)
                                      .withTitle("Item without version suffix")
                                      .withIssueDate("2022-06-07")
                                      .withAuthor("Author, Austin").withAuthor("Author, Andre")
                                      .withSubject("On the fly test item without version suffix")
                                      .build();
        items.add(publicItem1);

        // sort items list by UUID (as Items will come back ordered by UUID)
        items.sort(compareByUUID);

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);


        System.out.println("createItemWithoutVersionSuffixTest!\n\n\n");

        getClient(token).perform(get("/api/core/items"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.items[0].handle", is("123456789/3")))
        ;

        context.restoreAuthSystemState();
    }

    @Test
    public void createVersionedItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("dspace.initialVersionSuffix", true);

        Community versionedCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Community for not - versioned items")
                                          .build();
        Collection versionedCollection = CollectionBuilder.createCollection(context, versionedCommunity)
                                          .withName("Collection of not - versioned items").build();

        List<Item> items = new ArrayList();
        // Hibernate 5.x's org.hibernate.dialect.H2Dialect sorts UUIDs as if they are Strings.
        // So, we must compare UUIDs as if they are strings.
        // In Hibernate 6, the H2Dialect has been updated with native UUID type support, at which point
        // we'd need to update the below comparator to compare them as java.util.UUID (which sorts based on RFC 4412).
        Comparator<Item> compareByUUID = Comparator.comparing(i -> i.getID().toString());

        Item publicItem1 = ItemBuilder.createItem(context, versionedCollection)
                                      .withTitle("Item with versioned suffix")
                                      .withIssueDate("2022-06-07")
                                      .withAuthor("Author, Austin").withAuthor("Author, Andre")
                                      .withSubject("On the fly test item with version suffix")
                                      .build();
        items.add(publicItem1);

        // sort items list by UUID (as Items will come back ordered by UUID)
        items.sort(compareByUUID);

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        System.out.println("createVersionedItemTest!\n\n\n");

        getClient(token).perform(get("/api/core/items"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.items[0].handle", is("123456789/6.1")))
        ;

        context.restoreAuthSystemState();
    }
}
