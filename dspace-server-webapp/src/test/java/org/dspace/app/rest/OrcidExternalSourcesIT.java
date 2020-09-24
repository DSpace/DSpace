/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amazonaws.util.StringInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.external.OrcidRestConnector;
import org.dspace.external.provider.impl.OrcidV3AuthorDataProvider;
import org.dspace.external.provider.orcid.xml.XMLtoBio;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.orcid.jaxb.model.record_v3.NameType;
import org.orcid.jaxb.model.record_v3.NameType.FamilyName;
import org.orcid.jaxb.model.record_v3.Person;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This test suite includes end to end test to verify the integration with ORCID as an External Source.
 * The test run only if the orcid.clientid property is configured.
 * 
 * @author Mykhaylo Boychuk (4Science.it)
 *
 */
public class OrcidExternalSourcesIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    private OrcidRestConnector orcidRestConnector;

    @Autowired
    private OrcidV3AuthorDataProvider orcidV3AuthorDataProvider;

    public void onlyRunIfConfigExists() {
        if (StringUtils.isBlank(configurationService.getProperty("orcid.clientid"))) {
            Assume.assumeNoException(new IllegalStateException("Missing ORCID credentials"));
        }
    }

    @Test
    public void findOneExternalSourcesExistingSources() throws Exception {
        onlyRunIfConfigExists();
        getClient().perform(get("/api/integration/externalsources/orcid"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.id", is("orcid")),
                           hasJsonPath("$.name", is("orcid")),
                           hasJsonPath("$.hierarchical", is(false)),
                           hasJsonPath("$.type", is("externalsource"))
                   )));
    }

    @Test
    public void findOneExternalSourcesExistingSourcesWithentryValueTest() throws Exception {
        onlyRunIfConfigExists();
        String entry = "0000-0002-9029-1854";
        getClient().perform(get("/api/integration/externalsources/orcid/entryValues/" + entry))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.id", is(entry)),
                           hasJsonPath("$.display", is("Bollini, Andrea")),
                           hasJsonPath("$.value", is("Bollini, Andrea")),
                           hasJsonPath("$.externalSource", is("orcid")),
                           hasJsonPath("$.type", is("externalSourceEntry"))
                   )))
                   .andExpect(jsonPath("$.metadata['dc.identifier.uri'][0].value",is("https://orcid.org/" + entry)))
                   .andExpect(jsonPath("$.metadata['person.familyName'][0].value",is("Bollini")))
                   .andExpect(jsonPath("$.metadata['person.givenName'][0].value",is("Andrea")))
                   .andExpect(jsonPath("$.metadata['person.identifier.orcid'][0].value",is(entry)));
    }

    @Test
    public void findOneExternalSourceEntriesApplicableQuery() throws Exception {
        onlyRunIfConfigExists();
        String q = "orcid:0000-0002-9029-1854";
        getClient().perform(get("/api/integration/externalsources/orcid/entries")
                   .param("query", q))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalSourceEntries[0]", Matchers.allOf(
                           hasJsonPath("$.id", is("0000-0002-9029-1854")),
                           hasJsonPath("$.display", is("Bollini, Andrea")),
                           hasJsonPath("$.value", is("Bollini, Andrea")),
                           hasJsonPath("$.externalSource", is("orcid")),
                           hasJsonPath("$.type", is("externalSourceEntry"))
                   )))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.uri'][0].value",
                                   is("https://orcid.org/0000-0002-9029-1854")))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.familyName'][0].value",
                                   is("Bollini")))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.givenName'][0].value",
                                   is("Andrea")))
               .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.identifier.orcid'][0].value",
                                   is("0000-0002-9029-1854")));
    }

    @Test
    public void findOneExternalSourceEntriesApplicableQueryFamilyNameAndGivenNamesTest() throws Exception {
        onlyRunIfConfigExists();
        String q = "family-name:bollini AND given-names:andrea";
        getClient().perform(get("/api/integration/externalsources/orcid/entries")
                   .param("query", q))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalSourceEntries[0]", Matchers.allOf(
                           hasJsonPath("$.id", is("0000-0002-9029-1854")),
                           hasJsonPath("$.display", is("Bollini, Andrea")),
                           hasJsonPath("$.value", is("Bollini, Andrea")),
                           hasJsonPath("$.externalSource", is("orcid")),
                           hasJsonPath("$.type", is("externalSourceEntry"))
                   )))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.uri'][0].value",
                                   is("https://orcid.org/0000-0002-9029-1854")))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.familyName'][0].value",
                                   is("Bollini")))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.givenName'][0].value",
                                   is("Andrea")))
               .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.identifier.orcid'][0].value",
                                   is("0000-0002-9029-1854")));
    }

    @Test
    public void findOneExternalSourcesMockitoTest() throws Exception {

        HttpClient httpClient = Mockito.mock(HttpClient.class);
        XMLtoBio converter = Mockito.mock(XMLtoBio.class);
        orcidV3AuthorDataProvider.setConverter(converter);
        orcidRestConnector.setHttpClient(httpClient);

        BasicHttpResponse basicHttpResponse = new BasicHttpResponse(new ProtocolVersion("http", 1, 1), 200, "OK");
        basicHttpResponse.setEntity(new BasicHttpEntity());
        StringInputStream is = new StringInputStream("HttpResponseProxy{HTTP/1.1 200 OK "
                + "[Server: nginx/1.16.1, Cache-Control: no-cache, no-store, max-age=0, must-revalidate,"
                + " Content-Type: application/vnd.orcid+xml; qs=5;charset=UTF-8,"
                + " Expires: 0, Pragma: no-cache, X-XSS-Protection: 1; mode=block, Transfer-Encoding: chunked,"
                + " Connection: keep-alive, path=/, X-Frame-Options: DENY]}");
        BasicHttpEntity bhe = new BasicHttpEntity();
        basicHttpResponse.setEntity(bhe);
        bhe.setChunked(true);
        bhe.setContent(is);

        String entry = "0000-0002-9029-1854";

        NameType name = new NameType();
        name.setFamilyName(new FamilyName("Bollini, Andrea"));

        Person person = new Person();
        person.setName(name);
        name.setPath(entry);

        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(basicHttpResponse);
        when(converter.convertSinglePerson(is)).thenReturn(person);

        getClient().perform(get("/api/integration/externalsources/orcid/entryValues/" + entry))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.id", is(entry)),
                           hasJsonPath("$.display", is("Bollini, Andrea")),
                           hasJsonPath("$.value", is("Bollini, Andrea")),
                           hasJsonPath("$.externalSource", is("orcid")),
                           hasJsonPath("$.type", is("externalSourceEntry"))
                           )));
    }
}
