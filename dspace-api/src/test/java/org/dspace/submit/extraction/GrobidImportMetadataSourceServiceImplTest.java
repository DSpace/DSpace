/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.extraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.dspace.AbstractUnitTest;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.exception.FileMultipleOccurencesException;
import org.dspace.importer.external.exception.FileSourceException;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;
import org.dspace.importer.external.metadatamapping.contributor.SimpleMetadataContributor;
import org.dspace.importer.external.service.components.dto.PlainMetadataSourceDto;
import org.dspace.submit.extraction.grobid.TEI;
import org.dspace.submit.extraction.grobid.client.ConsolidateHeaderEnum;
import org.dspace.submit.extraction.grobid.client.GrobidClient;
import org.junit.Test;

/**
 * Unit tests for GrobidImportMetadataSourceServiceImpl.
 *
 * These tests mock the external GrobidClient and feed a TEI unmarshalled
 * from test resources: org/dspace/submit/extraction/grobid/grobid.xml.
 * The input PDF stream used is the companion resource grobid.pdf, but it is
 * not parsed by the test; the mocked client ignores it.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class GrobidImportMetadataSourceServiceImplTest extends AbstractUnitTest {

    private GrobidImportMetadataSourceServiceImpl newServiceWithMappingAndMock(GrobidClient grobidClient)
        throws Exception {
        GrobidImportMetadataSourceServiceImpl service = new GrobidImportMetadataSourceServiceImpl(grobidClient);

        Map<MetadataFieldConfig, MetadataContributor<PlainMetadataSourceDto>> mapping =
            new LinkedHashMap<>();

        // Replicate grobid-integration.xml minimal mapping needed for assertions
        mapping.put(new MetadataFieldConfig("dc.title"),
                    new SimpleMetadataContributor(new MetadataFieldConfig("dc.title"), "analytictitle"));
        mapping.put(new MetadataFieldConfig("dc.contributor.author"),
                    new SimpleMetadataContributor(new MetadataFieldConfig("dc.contributor.author"), "analyticname"));
        mapping.put(new MetadataFieldConfig("dc.description.abstract"),
                    new SimpleMetadataContributor(new MetadataFieldConfig("dc.description.abstract"),
                                                  "profiledescabstractparagraph"));
        mapping.put(new MetadataFieldConfig("dc.subject"),
                    new SimpleMetadataContributor(new MetadataFieldConfig("dc.subject"),
                                                  "profiledesckeywordsterm"));

        service.setMetadataFieldMap(mapping);

        return service;
    }

    private TEI loadTestTei() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream(
            "/org/dspace/submit/extraction/grobid/grobid.xml")) {
            assertNotNull("Test TEI XML resource should exist", is);
            JAXBContext jaxbContext = JAXBContext.newInstance(TEI.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (TEI) unmarshaller.unmarshal(is);
        }
    }

    private InputStream loadTestPdfStream() {
        InputStream pdf = this.getClass().getResourceAsStream(
            "/org/dspace/submit/extraction/grobid/grobid.pdf");
        assertNotNull("Test PDF resource should exist", pdf);
        return pdf;
    }

    @Test
    public void testGetImportSource() {
        GrobidImportMetadataSourceServiceImpl service =
            new GrobidImportMetadataSourceServiceImpl(mock(GrobidClient.class));
        assertEquals("GrobidMetadataSource", service.getImportSource());
    }

    @Test
    public void testSuccessfulExtractionAndMapping() throws Exception {
        TEI tei = loadTestTei();

        GrobidClient mockClient = mock(GrobidClient.class);
        when(mockClient.processHeaderDocument(any(InputStream.class), any(ConsolidateHeaderEnum.class)))
            .thenReturn(tei);

        GrobidImportMetadataSourceServiceImpl service = newServiceWithMappingAndMock(mockClient);

        try (InputStream pdf = loadTestPdfStream()) {
            ImportRecord record = service.getRecord(pdf);
            assertNotNull(record);

            // Collect values for quick contains checks
            List<String> dcTitleValues = valuesFor(record, "dc", "title");
            List<String> dcAuthorValues = valuesFor(record, "dc", "contributor", "author");
            List<String> dcAbstractValues = valuesFor(record, "dc", "description", "abstract");
            List<String> dcSubjectValues = valuesFor(record, "dc", "subject");

            assertTrue(dcTitleValues.contains(
                "Evaluation of Header Metadata Extraction Approaches and Tools for Scientific PDF Documents"));

            // Authors from grobid.xml
            assertTrue(dcAuthorValues.contains("Lipinski, Mario"));
            assertTrue(dcAuthorValues.contains("Yao, Kevin"));
            assertTrue(dcAuthorValues.contains("Breitinger, Corinna"));
            assertTrue(dcAuthorValues.contains("Beel, Joeran"));
            assertTrue(dcAuthorValues.contains("Gipp, Bela"));

            // Abstract starts with a known phrase; exact full text may vary in whitespace
            assertTrue(dcAbstractValues.stream().anyMatch(v -> v.startsWith("This paper evaluates the performance")));

            // Keywords include multiple terms
            assertTrue(dcSubjectValues.contains("Metadata Extraction"));
            assertTrue(dcSubjectValues.contains("Evaluation"));
            assertTrue(dcSubjectValues.contains("PDF"));
            assertTrue(dcSubjectValues.stream().anyMatch(v -> v.contains("Information Storage and Retrieval")));
        }
    }

    @Test(expected = FileSourceException.class)
    public void testClientFailureResultsInFileSourceException() throws Exception {
        GrobidClient mockClient = mock(GrobidClient.class);
        when(mockClient.processHeaderDocument(any(InputStream.class), any(ConsolidateHeaderEnum.class)))
            .thenThrow(new RuntimeException("boom"));

        GrobidImportMetadataSourceServiceImpl service = newServiceWithMappingAndMock(mockClient);
        try (InputStream pdf = loadTestPdfStream()) {
            // getRecord should translate internal null into FileSourceException
            service.getRecord(pdf);
        } catch (FileMultipleOccurencesException e) {
            throw new AssertionError("Did not expect multiple occurrences", e);
        }
    }

    private List<String> valuesFor(ImportRecord record, String schema, String element) {
        List<String> vals = new ArrayList<>();
        record.getValueList().stream()
              .filter(m -> schema.equals(m.getSchema()) && element.equals(m.getElement()) && m.getQualifier() == null)
              .forEach(m -> vals.add(m.getValue()));
        return vals;
    }

    private List<String> valuesFor(ImportRecord record, String schema, String element, String qualifier) {
        List<String> vals = new ArrayList<>();
        record.getValueList().stream()
              .filter(m -> schema.equals(m.getSchema()) && element.equals(m.getElement())
                  && qualifier.equals(m.getQualifier()))
              .forEach(m -> vals.add(m.getValue()));
        return vals;
    }
}
