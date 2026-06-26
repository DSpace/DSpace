/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.extraction.grobid;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.exception.FileMultipleOccurencesException;
import org.dspace.importer.external.exception.FileSourceException;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.FileSource;
import org.dspace.submit.extraction.grobid.client.ConsolidateHeaderEnum;
import org.dspace.submit.extraction.grobid.client.GrobidClient;
import org.dspace.submit.extraction.grobid.client.GrobidClientException;
import org.jdom2.Element;
import org.jdom2.input.DOMBuilder;
import org.w3c.dom.Document;

/**
 * Handle the retrieval and parsing of extracted metadata from a GROBID service
 * processed from a given PDF input stream
 * {@see grobid.cfg, spring-dspace-addon-import-services.xml and grobid-integration.xml}
 *
 * @author Kim Shepherd
 */
public class GrobidImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<Element>
        implements FileSource {

    private GrobidClient grobidClient;
    private List<String> supportedExtensions;

    @Override
    public String getImportSource() {
        return "GrobidMetadataSource";
    }

    @Override
    public List<String> getSupportedExtensions() {
        return this.supportedExtensions;
    }

    @Override
    public List<ImportRecord> getRecords(InputStream inputStream) throws FileSourceException {
        try {
            Optional<Document> teiDocument = grobidClient.retrieveHeaderDocument(inputStream,
                    ConsolidateHeaderEnum.CONSOLIDATE_AND_INJECT_METADATA);
            return teiDocument.map(document -> {
                // Convert W3C Document to JDOM for use with downstream MetadataContributors
                DOMBuilder domBuilder = new DOMBuilder();
                org.jdom2.Document jdomDoc = domBuilder.build(document);
                Element rootElement = jdomDoc.getRootElement();
                return List.of(transformSourceRecords(rootElement));
            }).orElse(Collections.emptyList());
        } catch (GrobidClientException e) {
            throw new FileSourceException(e.getMessage(), e);
        }
    }

    @Override
    public ImportRecord getRecord(InputStream inputStream) throws FileSourceException, FileMultipleOccurencesException {
        List<ImportRecord> records = getRecords(inputStream);
        if (records.size() > 1) {
            throw new FileMultipleOccurencesException("Only one GROBID TEI document should be returned");
        }
        if (records.isEmpty()) {
            return null;
        }
        return records.getFirst();
    }

    public void setSupportedExtensions(List<String> supportedExtensions) {
        this.supportedExtensions = supportedExtensions;
    }

    public GrobidClient getGrobidClient() {
        return grobidClient;
    }

    public void setGrobidClient(GrobidClient grobidClient) {
        this.grobidClient = grobidClient;
    }

    @Override
    public void init() throws Exception {

    }
}
