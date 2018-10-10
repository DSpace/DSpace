/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.opensearch;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.mock.web.MockHttpServletResponse;

/*
import org.dspace.app.rest.OpenSearchController;
import org.dspace.services.ConfigurationService;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.BrowseEntryResourceMatcher;
import org.dspace.app.rest.matcher.BrowseIndexMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.util.service.OpenSearchService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.Group;
*/
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;
import org.junit.Before;
import org.springframework.test.web.servlet.MockMvc;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.XpathResultMatchers;

/**
 * Integration test to test the /opensearch endpoint
 * (Class has to start or end with IT to be picked up by the failsafe plugin)
 *
 * @author Oliver Goldschmidt (o dot goldschmidt at tuhh dot de)
 */
public class OpenSearchControllerTest extends AbstractControllerIntegrationTest {

    // configuration is taken from dspace-api/src/test/data/dspaceFolder/config/local.cfg
    private ConfigurationService configurationService;

    @Before
    public void init() throws Exception {
        // override the configuration settings here if other settings are required for test
        // configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        // configurationService.setProperty("websvc.opensearch.enable", true);
        /*
        System.out.println("Testing OpenSearch");
        MockHttpServletResponse resp2 = getClient().perform(get("/opensearch/search")
            .param("query", "cats"))
            .andReturn().getResponse();
        System.out.println("Response from Test 2: "+resp2.getContentAsString());
        */
    }

    @Test
    public void searchAtomTest() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/opensearch/search")
                                .param("query", "cats"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/atom+xml;charset=UTF-8"
                   .andExpect(content().contentType("application/atom+xml;charset=UTF-8"))
        ;
    }

    /* HTML is an open issue in OpenSearch, so skip this test at the moment
    @Test
    public void searchHtmlTest() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/opensearch/search")
                                .param("query", "cats")
                                .param("format", "html"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/atom+xml;charset=UTF-8"
                   .andExpect(content().contentType("text/html;charset=UTF-8"))
        ;
    }
    */

    @Test
    public void searchRssTest() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/opensearch/search")
                                .param("query", "cats")
                                .param("format", "rss"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/rss+xml;charset=UTF-8"
                   .andExpect(content().contentType("application/rss+xml;charset=UTF-8"))
        ;
    }

    @Test
    public void serviceDocumentTest() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/opensearch/service"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   // and the contentType has to be an opensearchdescription
                   .andExpect(content().contentType("application/opensearchdescription+xml;charset=UTF-8"))
                   // and there need to be some values taken from the test configuration
                   .andExpect(xpath("OpenSearchDescription/ShortName").string("DS7 OpenSearch"))
                   .andExpect(xpath("OpenSearchDescription/LongName").string("DSpace 7 OpenSearch Service"))
                   .andExpect(xpath("OpenSearchDescription/Description").string("OpenSearch Service for DSpace 7"))
                   .andExpect(xpath("OpenSearchDescription/ShortName").string("DS7 OpenSearch"))
        ;
        /* Expected response for the service document is:
            <?xml version="1.0" encoding="UTF-8"?>
            <OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/">
                <ShortName>DS7 OpenSearch</ShortName>
                <LongName>DSpace 7 OpenSearch Service</LongName>
                <Description>OpenSearch Service for DSpace 7</Description>
                <InputEncoding>UTF-8</InputEncoding>
                <OutputEncoding>UTF-8</OutputEncoding>
                <Query role="example" searchTerms="cats" />
                <Tags>IR DSpace</Tags>
                <Contact>dspace-help@myu.edu</Contact>
                <Image height="16" width="16" type="image/vnd.microsoft.icon">http://www.dspace.org/images/favicon.ico</Image>
                <Url type="text/html" template="http://localhost:8080/simple-search?query={searchTerms}" />
                <Url type="application/atom+xml; charset=UTF-8" template="http://localhost:8080/open-search/?query={searchTerms}&amp;start={startIndex?}&amp;rpp={count?}&amp;format=atom" />
                <Url type="application/rss+xml; charset=UTF-8" template="http://localhost:8080/open-search/?query={searchTerms}&amp;start={startIndex?}&amp;rpp={count?}&amp;format=rss" />
            </OpenSearchDescription>
        */
    }
}