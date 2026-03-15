/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.exception.OrcidClientException;
import org.dspace.orcid.model.OrcidTokenResponseDTO;
import org.mockito.Mockito;
import org.orcid.jaxb.model.v3.release.record.Person;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedSearch;

/**
 * Mock the ORCID source using a mock OrcidClient so that queries will be
 * resolved against static files.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class MockOrcid extends Orcidv3SolrAuthorityImpl {

    private OrcidClient mockOrcidClient;

    @Override
    public void init() {
        mockOrcidClient = Mockito.mock(OrcidClient.class);
        // Mock getReadPublicAccessToken to return a mock token
        OrcidTokenResponseDTO mockToken = new OrcidTokenResponseDTO();
        mockToken.setAccessToken("mock-access-token");
        when(mockOrcidClient.getReadPublicAccessToken()).thenReturn(mockToken);
        // Set via field injection workaround
        this.orcidClient = mockOrcidClient;
    }

    /**
     * Call this to set up mocking for any test classes that need it. We don't set it in init()
     * or other AbstractIntegrationTest implementations will complain of unnecessary Mockito stubbing.
     *
     * @throws OrcidClientException if mock setup fails
     */
    public void setupNoResultsSearch() throws OrcidClientException {
        ExpandedSearch emptySearch = new ExpandedSearch();
        when(mockOrcidClient.expandedSearch(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(emptySearch);
        when(mockOrcidClient.expandedSearch(anyString(), anyInt(), anyInt()))
                .thenReturn(emptySearch);
    }

    /**
     * Call this to set up mocking for any test classes that need it. We don't set it in init()
     * or other AbstractIntegrationTest implementations will complain of unnecessary Mockito stubbing.
     *
     * @throws OrcidClientException if mock setup fails
     */
    public void setupSingleSearch() throws OrcidClientException {
        // For single search, return empty results (the original mock loaded orcid-search.xml
        // which contained a single result, but the actual person data is loaded separately)
        ExpandedSearch emptySearch = new ExpandedSearch();
        when(mockOrcidClient.expandedSearch(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(emptySearch);
        when(mockOrcidClient.expandedSearch(anyString(), anyInt(), anyInt()))
                .thenReturn(emptySearch);
    }

    /**
     * Call this to set up mocking for any test classes that need it. We don't set it in init()
     * or other AbstractIntegrationTest implementations will complain of unnecessary Mockito stubbing.
     *
     * @throws OrcidClientException if mock setup fails
     */
    public void setupSearchWithResults() throws OrcidClientException {
        Person person = loadPersonFromXml();
        when(mockOrcidClient.getPerson(anyString(), anyString())).thenReturn(person);
        when(mockOrcidClient.getPerson(anyString())).thenReturn(person);
    }

    /**
     * Load a Person object from the test XML resource.
     *
     * @return the Person object
     */
    private Person loadPersonFromXml() {
        try (InputStream is = this.getClass().getResourceAsStream("orcid-person.xml")) {
            JAXBContext jaxbContext = JAXBContext.newInstance(Person.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (Person) unmarshaller.unmarshal(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mock ORCID person XML", e);
        }
    }
}
