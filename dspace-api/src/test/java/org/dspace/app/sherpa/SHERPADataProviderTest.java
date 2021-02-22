/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.dspace.AbstractDSpaceTest;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.factory.ExternalServiceFactory;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.external.service.ExternalDataService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class SHERPADataProviderTest extends AbstractDSpaceTest {

    ExternalDataService externalDataService;
    ExternalDataProvider sherpaJournalProvider;
    ExternalDataProvider sherpaPublisherProvider;
    ExternalDataProvider sherpaJournalIssnProvider;

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        // Set up External Service Factory and set data providers
        externalDataService = ExternalServiceFactory.getInstance().getExternalDataService();
        sherpaJournalProvider = externalDataService.getExternalDataProvider("sherpaJournal");
        sherpaPublisherProvider = externalDataService.getExternalDataProvider("sherpaPublisher");
        sherpaJournalIssnProvider =
         externalDataService.getExternalDataProvider("sherpaJournalIssn");
    }

    @After
    public void tearDown() {
    }

    /**
     * Test searching the SHERPAv2JournalISSNProvider for an ISSN and inspect the returned data object
     * The provider is configured to use the Mock SHERPAService.
     */
    @Test
    public void testGetJournalISSNExternalObject() {
        // Get a response with a single valid journal, using the mock service which will return a response based on
        // thelancet.json stored response in test resources
        // We expect to see the following values set correctly:
        // dc.title =           The Lancet
        // dc.identifier.issn   0140-6736
        // getId()              0140-6736

        String validIssn = "0140-6736";
        String validName = "The Lancet";
        Optional<ExternalDataObject> externalDataObject = sherpaJournalIssnProvider.getExternalDataObject(validIssn);
        // If data object isn't here, throw a resource not found exception
        ExternalDataObject dataObject = externalDataObject.orElseThrow(
            () -> new ResourceNotFoundException("Couldn't find a data object for ISSN " + validIssn));

        // Instantiate some Strings that we'll set if we find the expected metadata
        String title = null;
        String identifier = null;
        for (MetadataValueDTO metadataValue : dataObject.getMetadata()) {
            if (metadataValue.getSchema().equalsIgnoreCase("dc") &&
            metadataValue.getElement().equalsIgnoreCase("title")) {
                title = metadataValue.getValue();
            } else if (metadataValue.getSchema().equalsIgnoreCase("dc")
                && metadataValue.getElement().equalsIgnoreCase("identifier")
                && metadataValue.getQualifier().equalsIgnoreCase("issn")) {
                identifier = metadataValue.getValue();
            }
        }

        // Does dc.title match the expected value?
        assertEquals("Title metadata must equal '" + validName + "' ", validName, title);

        // Does dc.identifier.uri match the expected value?
        assertEquals("Identifier ISSN must equal " + validIssn, validIssn, identifier);
    }

    /**
     * Test searching the SHERPAv2JournalISSNProvider for an ISSN and inspect the returned data object
     * The provider is configured to use the Mock SHERPAService.
     */
    @Test
    public void testSearchJournalISSNExternalObjects() {
        // Get a response with a single valid journal, using the mock service which will return a response based on
        // thelancet.json stored response in test resources
        // We expect to see the following values set correctly:
        // dc.title =           The Lancet
        // dc.identifier.issn   0140-6736
        // getId()              0140-6736

        String validIssn = "0140-6736";
        String validName = "The Lancet";
        List<ExternalDataObject> externalDataObjects =
            sherpaJournalIssnProvider.searchExternalDataObjects(validIssn, 0, 1);

        // Assert that the response is valid and not empty
        assertTrue("Couldn't find a data object for publication name " + validName,
            externalDataObjects != null && !externalDataObjects.isEmpty());

        // Get the first search result for inspection
        ExternalDataObject dataObject = externalDataObjects.get(0);

        // Assert that the data object itself is not null
        assertNotNull("External data object must not be null", dataObject);

        // Instantiate some Strings that we'll set if we find the expected metadata
        String title = null;
        String identifier = null;
        for (MetadataValueDTO metadataValue : dataObject.getMetadata()) {
            if (metadataValue.getSchema().equalsIgnoreCase("dc") &&
                metadataValue.getElement().equalsIgnoreCase("title")) {
                title = metadataValue.getValue();
            } else if (metadataValue.getSchema().equalsIgnoreCase("dc")
                && metadataValue.getElement().equalsIgnoreCase("identifier")
                && metadataValue.getQualifier().equalsIgnoreCase("issn")) {
                identifier = metadataValue.getValue();
            }
        }

        // Does dc.title match the expected value?
        assertEquals("Title metadata must equal '" + validName + "' ", validName, title);

        // Does dc.identifier.uri match the expected value?
        assertEquals("Identifier ISSN must equal " + validIssn, validIssn, identifier);
    }

    /**
     * Test searching the SHERPAv2JournalProvider for a journal and inspect the returned data object
     * The provider is configured to use the Mock SHERPAService.
     */
    @Test
    public void testGetJournalExternalObject() {
        // Get a response with a single valid journal, using the mock service which will return a response based on
        // thelancet.json stored response in test resources
        // We expect to see the following values set correctly:
        // dc.title =           The Lancet
        // dc.identifier.issn   0140-6736
        // getId()              0140-6736

        String validIssn = "0140-6736";
        String validName = "The Lancet";
        Optional<ExternalDataObject> externalDataObject = sherpaJournalProvider.getExternalDataObject(validName);
        // If data object isn't here, throw a resource not found exception
        ExternalDataObject dataObject = externalDataObject.orElseThrow(
            () -> new ResourceNotFoundException("Couldn't find a data object for publication name " + validName));

        // Instantiate some Strings that we'll set if we find the expected metadata
        String title = null;
        String identifier = null;
        for (MetadataValueDTO metadataValue : dataObject.getMetadata()) {
            if (metadataValue.getSchema().equalsIgnoreCase("dc") &&
                metadataValue.getElement().equalsIgnoreCase("title")) {
                title = metadataValue.getValue();
            } else if (metadataValue.getSchema().equalsIgnoreCase("dc")
                && metadataValue.getElement().equalsIgnoreCase("identifier")
                && metadataValue.getQualifier().equalsIgnoreCase("issn")) {
                identifier = metadataValue.getValue();
            }
        }

        // Does dc.title match the expected value?
        assertEquals("Title metadata must equal '" + validName + "' ", validName, title);

        // Does dc.identifier.uri match the expected value?
        assertEquals("Identifier ISSN must equal " + validIssn, validIssn, identifier);
    }

    /**
     * Test searching the SHERPAv2JournalProvider for a journal and inspect the returned data object
     * The provider is configured to use the Mock SHERPAService.
     */
    @Test
    public void testSearchJournalObjects() {
        // Get a response with a single valid journal, using the mock service which will return a response based on
        // thelancet.json stored response in test resources. We are searching here, using the search method but
        // will just return 1 result since that is what our test resource matches, and is sufficient for testing
        // We expect to see the following values set correctly:
        // dc.title =           The Lancet
        // dc.identifier.issn   0140-6736
        // getId()              0140-6736

        String validIssn = "0140-6736";
        String validName = "The Lancet";
        List<ExternalDataObject> externalDataObjects =
            sherpaJournalProvider.searchExternalDataObjects(validName, 0, 1);

        // Assert that the response is valid and not empty
        assertTrue("Couldn't find a data object for publication name " + validName,
            externalDataObjects != null && !externalDataObjects.isEmpty());

        // Get the first search result for inspection
        ExternalDataObject dataObject = externalDataObjects.get(0);

        // Assert that the data object itself is not null
        assertNotNull("External data object must not be null", dataObject);

        // Instantiate some Strings that we'll set if we find the expected metadata
        String title = null;
        String identifier = null;
        for (MetadataValueDTO metadataValue : dataObject.getMetadata()) {
            if (metadataValue.getSchema().equalsIgnoreCase("dc") &&
                metadataValue.getElement().equalsIgnoreCase("title")) {
                title = metadataValue.getValue();
            } else if (metadataValue.getSchema().equalsIgnoreCase("dc")
                && metadataValue.getElement().equalsIgnoreCase("identifier")
                && metadataValue.getQualifier().equalsIgnoreCase("issn")) {
                identifier = metadataValue.getValue();
            }
        }

        // Does dc.title match the expected value?
        assertEquals("Title metadata must equal '" + validName + "' ", validName, title);

        // Does dc.identifier.uri match the expected value?
        assertEquals("Identifier ISSN must equal " + validIssn, validIssn, identifier);
    }

    /**
     * Test searching the SHERPAv2JournalProvider for a journal and inspect the returned data object
     * The provider is configured to use the Mock SHERPAService.
     */
    @Test
    public void testGetPublisherExternalObject() {
        // Get a response with a single valid ISSN, using the mock service which will return a response based on
        // thelancet.json stored response in test resources
        // We expect to see the following values set correctly:
        // dc.title =                       Public Library of Science
        // dc.identifier.sherpaPublisher    112
        // dc.identifier.other              http://www.plos.org/

        // Set expected values
        String validName = "Public Library of Science";
        String validIdentifier = "112";
        String validUrl = "http://www.plos.org/";

        // Retrieve the dataobject(s) from the data provider
        Optional<ExternalDataObject> externalDataObject = sherpaPublisherProvider.getExternalDataObject(validName);
        // If data object isn't here, throw a resource not found exception
        ExternalDataObject dataObject = externalDataObject.orElseThrow(
            () -> new ResourceNotFoundException("Couldn't find a data object for publication name " + validName));

        // Instantiate some Strings that we'll set if we find the expected metadata
        String title = null;
        String identifier = null;
        String url = null;
        for (MetadataValueDTO metadataValue : dataObject.getMetadata()) {
            if (metadataValue.getSchema().equalsIgnoreCase("dc") &&
                metadataValue.getElement().equalsIgnoreCase("title")) {
                title = metadataValue.getValue();
            } else if (metadataValue.getSchema().equalsIgnoreCase("dc")
                && metadataValue.getElement().equalsIgnoreCase("identifier")
                && metadataValue.getQualifier().equalsIgnoreCase("sherpaPublisher")) {
                identifier = metadataValue.getValue();
            } else if (metadataValue.getSchema().equalsIgnoreCase("dc")
                && metadataValue.getElement().equalsIgnoreCase("identifier")
                && metadataValue.getQualifier().equalsIgnoreCase("other")) {
                url = metadataValue.getValue();
            }
        }

        // Does dc.title match the expected value?
        assertEquals("Title metadata must equal '" + validName + "' ", validName, title);

        // Does dc.identifier.sherpaPublisher match the expected value?
        assertEquals("Publisher ID must equal " + validIdentifier, validIdentifier, identifier);

        // Does dc.identifier.other match the expected value?
        assertEquals("Publisher URL must equal " + validUrl, validUrl, url);
    }

    /**
     * Test searching the SHERPAv2JournalProvider for a journal and inspect the returned data object
     * The provider is configured to use the Mock SHERPAService.
     */
    @Test
    public void testSearchPublisherExternalObjects() {
        // Get a response with a single valid ISSN, using the mock service which will return a response based on
        // thelancet.json stored response in test resources
        // We expect to see the following values set correctly:
        // dc.title =                       Public Library of Science
        // dc.identifier.sherpaPublisher    112
        // dc.identifier.other              http://www.plos.org/

        // Set expected values
        String validName = "Public Library of Science";
        String validIdentifier = "112";
        String validUrl = "http://www.plos.org/";

        // Retrieve the dataobject(s) from the data provider
        List<ExternalDataObject> externalDataObjects =
            sherpaPublisherProvider.searchExternalDataObjects(validName, 0, 1);

        // Assert that the response is valid and not empty
        assertTrue("Couldn't find a data object for publication name " + validName,
            externalDataObjects != null && !externalDataObjects.isEmpty());

        ExternalDataObject dataObject = externalDataObjects.get(0);

        // Assert that the data object itself is not null
        assertNotNull("External data object must not be null", dataObject);

        // Instantiate some Strings that we'll set if we find the expected metadata
        String title = null;
        String identifier = null;
        String url = null;
        for (MetadataValueDTO metadataValue : dataObject.getMetadata()) {
            if (metadataValue.getSchema().equalsIgnoreCase("dc") &&
                metadataValue.getElement().equalsIgnoreCase("title")) {
                title = metadataValue.getValue();
            } else if (metadataValue.getSchema().equalsIgnoreCase("dc")
                && metadataValue.getElement().equalsIgnoreCase("identifier")
                && metadataValue.getQualifier().equalsIgnoreCase("sherpaPublisher")) {
                identifier = metadataValue.getValue();
            } else if (metadataValue.getSchema().equalsIgnoreCase("dc")
                && metadataValue.getElement().equalsIgnoreCase("identifier")
                && metadataValue.getQualifier().equalsIgnoreCase("other")) {
                url = metadataValue.getValue();
            }
        }

        // Does dc.title match the expected value?
        assertEquals("Title metadata must equal '" + validName + "' ", validName, title);

        // Does dc.identifier.sherpaPublisher match the expected value?
        assertEquals("Publisher ID must equal " + validIdentifier, validIdentifier, identifier);

        // Does dc.identifier.other match the expected value?
        assertEquals("Publisher URL must equal " + validUrl, validUrl, url);
    }
}