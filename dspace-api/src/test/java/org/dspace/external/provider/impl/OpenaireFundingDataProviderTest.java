/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.dspace.AbstractDSpaceTest;
import org.dspace.external.factory.ExternalServiceFactory;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.external.service.ExternalDataService;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for OpenaireFundingDataProvider
 * 
 * @author pgraca
 *
 */
public class OpenaireFundingDataProviderTest extends AbstractDSpaceTest {

    ExternalDataService externalDataService;
    ExternalDataProvider openaireFundingDataProvider;

    /**
     * This method will be run before every test as per @Before. It will initialize
     * resources required for each individual unit test.
     *
     * Other methods can be annotated with @Before here or in subclasses but no
     * execution order is guaranteed
     */
    @Before
    public void init() {
        // Set up External Service Factory and set data providers
        externalDataService = ExternalServiceFactory.getInstance().getExternalDataService();
        openaireFundingDataProvider = externalDataService.getExternalDataProvider("openaireFunding");
    }

    @Test
    public void testNumberOfResultsWSingleKeyword() {
        assertNotNull("openaireFundingDataProvider is not null", openaireFundingDataProvider);
        assertEquals("openaireFunding.numberOfResults.query:mock", 77,
                openaireFundingDataProvider.getNumberOfResults("mock"));
    }

    @Test
    public void testNumberOfResultsWKeywords() {
        assertNotNull("openaireFundingDataProvider is not null", openaireFundingDataProvider);
        assertEquals("openaireFunding.numberOfResults.query:mock+test", 77,
                openaireFundingDataProvider.getNumberOfResults("mock+test"));
    }

    @Test
    public void testQueryResultsWSingleKeyword() {
        assertNotNull("openaireFundingDataProvider is not null", openaireFundingDataProvider);
        List<ExternalDataObject> results = openaireFundingDataProvider.searchExternalDataObjects("mock", 0, 10);
        assertEquals("openaireFunding.searchExternalDataObjects.size", 10, results.size());
    }

    @Test
    public void testQueryResultsWKeywords() {
        String value = "Mushroom Robo-Pic - Development of an autonomous robotic mushroom picking system";

        assertNotNull("openaireFundingDataProvider is not null", openaireFundingDataProvider);
        List<ExternalDataObject> results = openaireFundingDataProvider.searchExternalDataObjects("mock+test", 0, 10);
        assertEquals("openaireFunding.searchExternalDataObjects.size", 10, results.size());
        assertTrue("openaireFunding.searchExternalDataObjects.first.value", value.equals(results.get(0).getValue()));
    }

    @Test
    public void testGetDataObject() {
        String id = "aW5mbzpldS1yZXBvL2dyYW50QWdyZWVtZW50L0ZDVC81ODc2LVBQQ0RUSS8xMTAwNjIvUFQ=";
        String value = "Portuguese Wild Mushrooms: Chemical characterization and functional study"
                + " of antiproliferative and proapoptotic properties in cancer cell lines";

        assertNotNull("openaireFundingDataProvider is not null", openaireFundingDataProvider);

        Optional<ExternalDataObject> result = openaireFundingDataProvider.getExternalDataObject(id);

        assertTrue("openaireFunding.getExternalDataObject.exists", result.isPresent());
        assertTrue("openaireFunding.getExternalDataObject.value", value.equals(result.get().getValue()));
    }

    @Test
    public void testGetDataObjectWInvalidId() {
        String id = "WRONGID";

        assertNotNull("openaireFundingDataProvider is not null", openaireFundingDataProvider);

        Optional<ExternalDataObject> result = openaireFundingDataProvider.getExternalDataObject(id);

        assertTrue("openaireFunding.getExternalDataObject.notExists:WRONGID", result.isEmpty());
    }
}
