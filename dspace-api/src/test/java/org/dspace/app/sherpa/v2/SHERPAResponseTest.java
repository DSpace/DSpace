/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.v2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.AbstractDSpaceTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Kim Shepherd
 */
public class SHERPAResponseTest extends AbstractDSpaceTest {

    public SHERPAResponseTest() {

    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test that the SHERPA API response (in this case saved as local JSON) is correctly
     * parsed and resulting objects have expected values
     * @throws IOException
     */
    @Test
    public void testParseJSON() throws IOException {

        InputStream content = null;
        try {
            // Get mock JSON - in this case, a known good result for The Lancet
            content = getClass().getResourceAsStream("thelancet.json");

            // Parse JSON input stream
            SHERPAResponse response = new SHERPAResponse(content, SHERPAResponse.SHERPAFormat.JSON);

            // Assert response is not error, or fail with message
            assertFalse("Response was flagged as 'isError'", response.isError());

            // Assert response has at least one journal result, or fail with message
            assertTrue("List of journals did not contain at least one parsed journal",
                CollectionUtils.isNotEmpty(response.getJournals()));

            // Assert response has a journal with title "The Lancet", or fail with message
            String expectedTitle = "The Lancet";
            assertTrue("Journal title did not match expected '" + expectedTitle + "' value",
                CollectionUtils.isNotEmpty(response.getJournals().get(0).getTitles())
                    && expectedTitle.equals(response.getJournals().get(0).getTitles().get(0)));

            // Assert response has expected publication (metadata) URI
            String expectedSystemMetadataUri = "http://v2.sherpa.ac.uk/id/publication/23803";
            assertTrue("Response metadata URI did not match expected '" + expectedSystemMetadataUri
                + "' value", expectedSystemMetadataUri.equals(response.getMetadata().getUri()));

            // Assert response has at least one policy
            assertTrue("Response did not contain at least one archiving policy",
                CollectionUtils.isNotEmpty(response.getJournals().get(0).getPolicies()));

            // Assert response has at least one permitted version
            assertTrue("Response did not contain at least one permitted version",
                CollectionUtils.isNotEmpty(response.getJournals().get(0).getPolicies().get(0).getPermittedVersions()));

            // Assert journal has at least one publisher
            assertTrue("Response did not contain at least one publisher",
                CollectionUtils.isNotEmpty(response.getJournals().get(0).getPublishers()));

            // Assert first publisher has name 'Elsevier'
            String expectedPublisherName = "Elsevier";
            assertTrue("Response did not contain expected publisher name '" + expectedPublisherName + "'",
                expectedPublisherName.equals(response.getJournals().get(0).getPublisher().getName()));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (content != null) {
                content.close();
            }
        }
    }
}
