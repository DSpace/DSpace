/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.builder.CollectionBuilder.createCollection;
import static org.dspace.app.rest.builder.CommunityBuilder.createCommunity;
import static org.dspace.app.rest.builder.ItemBuilder.createItem;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration test to test the /api/discover/sitemaps/{name} endpoint, see {@link SitemapRestController}
 *
 * @author Maria Verdonck (Atmire) on 08/07/2020
 */
public class SitemapRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConfigurationService configurationService;

    private final static String SITEMAPS_ENDPOINT = "/api/" + RestModel.DISCOVER + "/sitemaps";

    private Item item1;
    private Item item2;

    @Before
    @Override
    public void setUp() throws Exception {

        super.setUp();

        context.turnOffAuthorisationSystem();

        Community community = createCommunity(context).build();
        Collection collection = createCollection(context, community).build();
        this.item1 = createItem(context, collection)
            .withTitle("Test 1")
            .withIssueDate("2010-10-17")
            .build();
        this.item2 = createItem(context, collection)
            .withTitle("Test 2")
            .withIssueDate("2015-8-3")
            .build();

        runDSpaceScript("generate-sitemaps");

        context.restoreAuthSystemState();
    }

    @Test
    public void testSitemap_notValidSiteMapFile() throws Exception {
        //** WHEN **
        //We attempt to retrieve a non valid sitemap file
        getClient().perform(get(SITEMAPS_ENDPOINT + "/notValidSiteMapFile"))
                   //** THEN **
                   .andExpect(status().isNotFound());
    }

    @Test
    public void testSitemap_sitemapIndexHtml() throws Exception {
        //** WHEN **
        //We retrieve sitemap_index.html
        MvcResult result = getClient().perform(get(SITEMAPS_ENDPOINT + "/sitemap_index.html"))
                                      //** THEN **
                                      .andExpect(status().isOk())
                                      //We expect the content type to match
                                      .andExpect(content().contentType("text/html"))
                                      .andReturn();

        String response = result.getResponse().getContentAsString();
        // contains a link to /api/discover/sitemaps/sitemap0.html
        assertTrue(response.contains(SITEMAPS_ENDPOINT + "/sitemap0.html"));
    }

    @Test
    public void testSitemap_sitemap0Html() throws Exception {
        //** WHEN **
        //We retrieve sitemap0.html
        MvcResult result = getClient().perform(get(SITEMAPS_ENDPOINT + "/sitemap0.html"))
                                      //** THEN **
                                      .andExpect(status().isOk())
                                      //We expect the content type to match
                                      .andExpect(content().contentType("text/html"))
                                      .andReturn();

        String response = result.getResponse().getContentAsString();
        // contains a link to items: [dspace.ui.url]/items/<uuid>
        assertTrue(response.contains(configurationService.getProperty("dspace.ui.url") + "/items/" + item1.getID()));
        assertTrue(response.contains(configurationService.getProperty("dspace.ui.url") + "/items/" + item2.getID()));
    }

    @Test
    public void testSitemap_sitemapIndexXml() throws Exception {
        //** WHEN **
        //We retrieve sitemap_index.xml
        MvcResult result = getClient().perform(get(SITEMAPS_ENDPOINT + "/sitemap_index.xml"))
                                      //** THEN **
                                      .andExpect(status().isOk())
                                      //We expect the content type to match
                                      .andExpect(content().contentType("application/xml"))
                                      .andReturn();

        String response = result.getResponse().getContentAsString();
        // contains a link to /api/discover/sitemaps/sitemap0.html
        assertTrue(response.contains(SITEMAPS_ENDPOINT + "/sitemap0.xml"));
    }

    @Test
    public void testSitemap_sitemap0Xml() throws Exception {
        //** WHEN **
        //We retrieve sitemap0.html
        MvcResult result = getClient().perform(get(SITEMAPS_ENDPOINT + "/sitemap0.xml"))
                                      //** THEN **
                                      .andExpect(status().isOk())
                                      //We expect the content type to match
                                      .andExpect(content().contentType("application/xml"))
                                      .andReturn();

        String response = result.getResponse().getContentAsString();
        // contains a link to items: [dspace.ui.url]/items/<uuid>
        assertTrue(response.contains(configurationService.getProperty("dspace.ui.url") + "/items/" + item1.getID()));
        assertTrue(response.contains(configurationService.getProperty("dspace.ui.url") + "/items/" + item2.getID()));
    }
}
