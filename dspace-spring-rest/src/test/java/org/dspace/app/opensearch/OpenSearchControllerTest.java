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

import org.junit.Ignore;
import org.junit.Test;

/**
 * Integration test to test the /opensearch endpoint
 * (Class has to start or end with IT to be picked up by the failsafe plugin)
 *
 * @author Oliver Goldschmidt (o dot goldschmidt at tuhh dot de)
 */
public class OpenSearchControllerTest extends AbstractControllerIntegrationTest {

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

    // HTML is an open issue in OpenSearch, so skip this test at the moment
    @Test
    @Ignore
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
                   .andExpect(xpath("OpenSearchDescription/ShortName").string("DSpace"))
                   .andExpect(xpath("OpenSearchDescription/LongName").string("DSpace at My University"))
                   .andExpect(xpath("OpenSearchDescription/Description")
                       .string("DSpace at My University DSpace repository")
                   )
        ;
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
                <Url type="text/html" template="http://localhost:8080/simple-search?query={searchTerms}" />
                <Url type="application/atom+xml; charset=UTF-8" template="http://localhost:8080/open-search/?query={searchTerms}&amp;start={startIndex?}&amp;rpp={count?}&amp;format=atom" />
                <Url type="application/rss+xml; charset=UTF-8" template="http://localhost:8080/open-search/?query={searchTerms}&amp;start={startIndex?}&amp;rpp={count?}&amp;format=rss" />
            </OpenSearchDescription>
        */
    }
}