package org.dspace.app.sherpa.v2;

import static org.junit.Assert.assertEquals;
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

public class SHERPAPublisherResponseTest extends AbstractDSpaceTest {

    public SHERPAPublisherResponseTest() {

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
            // Get mock JSON - in this case, a known good result for PLOS
            content = getClass().getResourceAsStream("plos.json");

            // Parse JSON input stream
            SHERPAPublisherResponse response = new SHERPAPublisherResponse(content,
                SHERPAPublisherResponse.SHERPAFormat.JSON);

            // Assert response is not error, or fail with message
            assertFalse("Response was flagged as 'isError'", response.isError());

            // Assert response has at least one publisher result, or fail with message
            assertTrue("List of publishers did not contain at least one parsed publisher",
                CollectionUtils.isNotEmpty(response.getPublishers()));

            // Assert response has a publisher with name "Public Library of Science", or fail with message
            String expectedName = "Public Library of Science";
            assertEquals("Publisher name did not match expected '" + expectedName + "' value",
                expectedName, response.getPublishers().get(0).getName());

            // Assert response has expected publisher URL
            String expectedUrl = "http://www.plos.org/";
            assertEquals("Response metadata URI did not match expected '" + expectedUrl
                + "' value", expectedUrl, response.getPublishers().get(0).getUri());

            // Assert response has at expected publisher ID
            String expectedId = "112";
            assertEquals("Response publisher ID did not match expected ID " + expectedId,
                expectedId, response.getPublishers().get(0).getIdentifier());

        } catch(IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (content != null) {
                content.close();
            }
        }
    }
}
