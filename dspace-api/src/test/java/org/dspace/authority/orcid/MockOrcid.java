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

    @Override
    public void init() {
        OrcidRestConnector orcidRestConnector = Mockito.mock(OrcidRestConnector.class);
        when(orcidRestConnector.get(ArgumentMatchers.startsWith("search?"), ArgumentMatchers.any()))
        .thenAnswer(new Answer<InputStream>() {
                @Override
                public InputStream answer(InvocationOnMock invocation) {
                    return this.getClass().getResourceAsStream("orcid-search-noresults.xml");
                }
            });
        when(orcidRestConnector.get(ArgumentMatchers.startsWith("search?q=Bollini"), ArgumentMatchers.any()))
            .thenAnswer(new Answer<InputStream>() {
                    @Override
                    public InputStream answer(InvocationOnMock invocation) {
                        return this.getClass().getResourceAsStream("orcid-search.xml");
                    }
                });
        when(orcidRestConnector.get(ArgumentMatchers.endsWith("/person"), ArgumentMatchers.any()))
            .thenAnswer(new Answer<InputStream>() {
                    @Override
                    public InputStream answer(InvocationOnMock invocation) {
                        return this.getClass().getResourceAsStream("orcid-person-record.xml");
                    }
                });

        setOrcidRestConnector(orcidRestConnector);
    }

}
