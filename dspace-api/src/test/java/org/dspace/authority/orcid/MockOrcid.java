/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid;

import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.dspace.external.OrcidRestConnector;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Mock the ORCID source using a mock rest connector so that query will be
 * resolved against static file
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class MockOrcid extends Orcidv3SolrAuthorityImpl {

    OrcidRestConnector orcidRestConnector;

    @Override
    public void init() {
        initializeAccessToken();
        orcidRestConnector = Mockito.mock(OrcidRestConnector.class);
    }

    /**
     * Call this to set up mocking for any test classes that need it. We don't set it in init()
     * or other AbstractIntegrationTest implementations will complain of unnecessary Mockito stubbing
     */
    public void setupNoResultsSearch() {
        when(orcidRestConnector.get(ArgumentMatchers.startsWith("search?"), ArgumentMatchers.any()))
                .thenAnswer(new Answer<InputStream>() {
                    @Override
                    public InputStream answer(InvocationOnMock invocation) {
                        return this.getClass().getResourceAsStream("orcid-search-noresults.xml");
                    }
                });
    }
    /**
     * Call this to set up mocking for any test classes that need it. We don't set it in init()
     * or other AbstractIntegrationTest implementations will complain of unnecessary Mockito stubbing
     */
    public void setupSingleSearch() {
        when(orcidRestConnector.get(ArgumentMatchers.startsWith("search?q=Bollini"), ArgumentMatchers.any()))
                .thenAnswer(new Answer<InputStream>() {
                    @Override
                    public InputStream answer(InvocationOnMock invocation) {
                        return this.getClass().getResourceAsStream("orcid-search.xml");
                    }
                });
    }
    /**
     * Call this to set up mocking for any test classes that need it. We don't set it in init()
     * or other AbstractIntegrationTest implementations will complain of unnecessary Mockito stubbing
     */
    public void setupSearchWithResults() {
        when(orcidRestConnector.get(ArgumentMatchers.endsWith("/person"), ArgumentMatchers.any()))
                .thenAnswer(new Answer<InputStream>() {
                    @Override
                    public InputStream answer(InvocationOnMock invocation) {
                        return this.getClass().getResourceAsStream("orcid-person-record.xml");
                    }
                });

        setOrcidRestConnector(orcidRestConnector);
    }

    @Override
    public void initializeAccessToken() {
        if (getAccessToken() == null) {
            setAccessToken("mock-access-token");
        }
    }
}
