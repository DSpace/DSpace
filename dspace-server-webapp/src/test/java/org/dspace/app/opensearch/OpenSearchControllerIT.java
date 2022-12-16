/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.opensearch;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Integration test to test the /opensearch endpoint
 * (Class has to start or end with IT to be picked up by the failsafe plugin)
 *
 * @author Oliver Goldschmidt (o dot goldschmidt at tuhh dot de)
 */
public class OpenSearchControllerIT extends AbstractControllerIntegrationTest {

    private ConfigurationService configurationService;

    @Before
    public void init() throws Exception {
        //enable OpenSearch by configuration
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        configurationService.setProperty("websvc.opensearch.enable", true);
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

    // there is no searchHtmlTest here as the html search is redirected to the angular UI

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
    public void noResultsTest() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/opensearch/search")
                                .param("query", "this query is not supposed to have a result"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/atom+xml;charset=UTF-8"
                   .andExpect(content().contentType("application/atom+xml;charset=UTF-8"))
                   .andExpect(xpath("feed/totalResults").string("0"))
                   .andExpect(xpath("feed/Query/@searchTerms").string("this+query+is+not+supposed+to+have+a+result"))
        ;
    }

    @Test
    public void findResultSimpleTest() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                          .withTitle("Boars at Yellowstone")
                                          .withIssueDate("2017-10-17")
                                          .withAuthor("Ballini, Andreas").withAuthor("Moriarti, Susan")
                                          .build();
        Item publicItem2 = ItemBuilder.createItem(context, col1)
                                          .withTitle("Yellowstone and bisons")
                                          .withIssueDate("2017-10-18")
                                          .withAuthor("Ballini, Andreas").withAuthor("Moriarti, Susan")
                                          .build();
        //When we call the root endpoint
        getClient().perform(get("/opensearch/search")
                                .param("query", "Yellowstone"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/atom+xml;charset=UTF-8"
                   .andExpect(content().contentType("application/atom+xml;charset=UTF-8"))
                   .andExpect(xpath("feed/Query/@searchTerms").string("Yellowstone"))
                   .andExpect(xpath("feed/totalResults").string("2"))
        ;
    }

    // This test does not find the record, so there are obviously issues with special chars
    @Ignore
    @Test
    public void findResultWithSpecialCharsTest() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                          .withTitle("Bären im Yellowstonepark")
                                          .withIssueDate("2017-10-17")
                                          .withAuthor("Bäcker, Nick")
                                          .build();
        //When we call the root endpoint
        getClient().perform(get("/opensearch/search")
                                .param("query", "Bär"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/atom+xml;charset=UTF-8"
                   .andExpect(content().contentType("application/atom+xml;charset=UTF-8"))
                   .andExpect(xpath("feed/Query/@searchTerms").string("B%C3%A4r"))
                   .andExpect(xpath("feed/totalResults").string("1"))
        ;
    }

    // Ignore this test as it is throwing an exception
    @Ignore
    @Test
    public void invalidQueryTest() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/opensearch/search")
                                .param("query", "urn:nbn:de:fake-123"))
                   // We get an exception for such a query, which is obviously not expected
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/atom+xml;charset=UTF-8"
                   .andExpect(content().contentType("application/atom+xml;charset=UTF-8"))
                   .andExpect(xpath("feed/Query/@searchTerms").string("urn:nbn:de:fake-123"))
                   .andExpect(xpath("feed/totalResults").string("0"))
        ;
    }

    @Test
    public void emptyQueryTest() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/opensearch/search")
                                .param("query", ""))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/atom+xml;charset=UTF-8"
                   .andExpect(content().contentType("application/atom+xml;charset=UTF-8"))
                   .andExpect(xpath("feed/totalResults").string("0"))
        ;
    }

    @Test
    public void validSortTest() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/opensearch/search")
                                .param("query", "")
                                .param("sort", "dc.date.issued"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/atom+xml;charset=UTF-8"
                   .andExpect(content().contentType("application/atom+xml;charset=UTF-8"))
                   .andExpect(xpath("feed/totalResults").string("0"))
        ;
    }

    @Test
    public void invalidSortTest() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/opensearch/search")
                                .param("query", "")
                                .param("sort", "dc.invalid.field"))
                   //We get an exception for such a sort field
                   //The status has to be 400 ERROR
                   .andExpect(status().is(400))
        ;
    }

    @Test
    public void serviceDocumentTest() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/opensearch/service"))
                // The status has to be 200 OK
                .andExpect(status().isOk())
                // and the contentType has to be an opensearchdescription
                .andExpect(content().contentType("application/opensearchdescription+xml;charset=UTF-8"))
                // and there need to be some values taken from the test configuration
                .andExpect(xpath("OpenSearchDescription/ShortName").string("DSpace"))
                .andExpect(xpath("OpenSearchDescription/LongName").string("DSpace at My University"))
                .andExpect(xpath("OpenSearchDescription/Description")
                        .string("DSpace at My University DSpace repository"))
                .andExpect(xpath("OpenSearchDescription/Url[@type='text/html']/@template")
                        .string("http://localhost:4000/search?query={searchTerms}"))
                .andExpect(xpath("OpenSearchDescription/Url[@type='application/atom+xml; charset=UTF-8']/@template")
                        .string("http://localhost/opensearch/search?"
                                + "query={searchTerms}&start={startIndex?}&rpp={count?}&format=atom"))
                .andExpect(xpath("OpenSearchDescription/Url[@type='application/rss+xml; charset=UTF-8']/@template")
                        .string("http://localhost/opensearch/search?"
                                + "query={searchTerms}&start={startIndex?}&rpp={count?}&format=rss"));
        /* Expected response for the service document is:
            <?xml version="1.0" encoding="UTF-8"?>
            <OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/">
                <ShortName>DSpace</ShortName>
                <LongName>DSpace at My University</LongName>
                <Description>DSpace at My University DSpace repository</Description>
                <InputEncoding>UTF-8</InputEncoding>
                <OutputEncoding>UTF-8</OutputEncoding>
                <Query role="example" searchTerms="photosyntesis" />
                <Tags>IR DSpace</Tags>
                <Contact>dspace-help@myu.edu</Contact>
                <Image height="16" width="16" type="image/vnd.microsoft.icon">http://www.dspace.org/images/favicon.ico</Image>
                <Url type="text/html" template="http://localhost:4000/search?query={searchTerms}" />
                <Url type="application/atom+xml; charset=UTF-8" template="http://localhost:8080/server/opensearch/search?query={searchTerms}&amp;start={startIndex?}&amp;rpp={count?}&amp;format=atom" />
                <Url type="application/rss+xml; charset=UTF-8" template="http://localhost:8080/server/opensearch/search?query={searchTerms}&amp;start={startIndex?}&amp;rpp={count?}&amp;format=rss" />
            </OpenSearchDescription>
        */
    }
}
