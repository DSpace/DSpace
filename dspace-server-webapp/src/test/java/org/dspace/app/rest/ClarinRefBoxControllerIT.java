/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Test;

/**
 * The Integration Test class for the ClarinRefBoxController.
 */
public class ClarinRefBoxControllerIT extends AbstractControllerIntegrationTest {

    // FS = featuredService
    private Item itemWithFS;
    private Item item;
    private Collection collection;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("test").build();
        collection = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        item = ItemBuilder.createItem(context, collection)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Test author 2")
                .withSubject("TestingForMore 2")
                .build();
        itemWithFS = ItemBuilder.createItem(context, collection)
                .withTitle("Public item 1")
                .withIssueDate("2016-02-13")
                .withAuthor("Test author")
                .withSubject("TestingForMore")
                .withMetadata("local","featuredService","kontext", "Slovak|URLSlovak")
                .withMetadata("local","featuredService","kontext", "Czech|URLCzech")
                .withMetadata("local","featuredService","pmltq", "Arabic|URLArabic")
                .build();
        context.restoreAuthSystemState();
    }

    @Test
    public void returnFeaturedServiceWithoutLinks() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/refbox/services?id=" + item.getID()))
                .andExpect(status().isOk());
    }

    @Test
    public void returnFeaturedServiceWithLinks() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/refbox/services?id=" + itemWithFS.getID()))
                .andExpect(status().isOk());
    }
}
