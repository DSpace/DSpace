/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.tools.ant.filters.StringInputStream;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.scopus.service.LiveImportClientImpl;
import org.dspace.importer.external.scopus.service.ScopusImportMetadataSourceServiceImpl;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link ScopusImportMetadataSourceServiceImpl}
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class ScopusImportMetadataSourceServiceIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ScopusImportMetadataSourceServiceImpl scopusServiceImpl;

    @Autowired
    private LiveImportClientImpl liveImportClientImpl;

    @Test
    public void scopusImportMetadataGetRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String originApiKey = scopusServiceImpl.getApiKey();
        if (StringUtils.isBlank(originApiKey)) {
            scopusServiceImpl.setApiKey("testApiKey");
        }
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        String path = testProps.get("test.scopus").toString();
        try (FileInputStream file = new FileInputStream(path)) {
            String xmlMetricsExample = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);

            CloseableHttpResponse response = mockResponse(xmlMetricsExample, 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            Collection<ImportRecord> collection2match = getRecords();
            Collection<ImportRecord> recordsImported = scopusServiceImpl.getRecords("test query", 0, 2);
            assertEquals(2, recordsImported.size());
            assertTrue(matchRecords(recordsImported, collection2match));
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
            scopusServiceImpl.setApiKey(originApiKey);
        }
    }

    @Test
    public void scopusImportMetadataGetRecordsCountTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String originApiKey = scopusServiceImpl.getApiKey();
        if (StringUtils.isBlank(originApiKey)) {
            scopusServiceImpl.setApiKey("testApiKey");
        }
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        String path = testProps.get("test.scopus").toString();
        try (FileInputStream file = new FileInputStream(path)) {
            String xmlMetricsExample = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);

            CloseableHttpResponse response = mockResponse(xmlMetricsExample, 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            int tot = scopusServiceImpl.getRecordsCount("test query");
            assertEquals(2, tot);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
            scopusServiceImpl.setApiKey(originApiKey);
        }
    }

    @Test
    public void scopusImportMetadataGetRecordsEmptyResponceTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String originApiKey = scopusServiceImpl.getApiKey();
        if (StringUtils.isBlank(originApiKey)) {
            scopusServiceImpl.setApiKey("testApiKey");
        }
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        String path = testProps.get("test.scopus-empty").toString();
        try (FileInputStream file = new FileInputStream(path)) {
            String xmlMetricsExample = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);

            CloseableHttpResponse response = mockResponse(xmlMetricsExample, 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            Collection<ImportRecord> recordsImported = scopusServiceImpl.getRecords("test query", 0, 20);
            ImportRecord  importedRecord = recordsImported.iterator().next();
            assertTrue(importedRecord.getValueList().isEmpty());
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
            scopusServiceImpl.setApiKey(originApiKey);
        }
    }

    private boolean matchRecords(Collection<ImportRecord> recordsImported, Collection<ImportRecord> records2match) {
        ImportRecord  firstImported = recordsImported.iterator().next();
        ImportRecord  secondImported = recordsImported.iterator().next();
        ImportRecord  first2match = recordsImported.iterator().next();
        ImportRecord  second2match = recordsImported.iterator().next();
        boolean checkFirstRecord = firstImported.getValueList().containsAll(first2match.getValueList());
        boolean checkSecondRecord = secondImported.getValueList().containsAll(second2match.getValueList());
        return checkFirstRecord && checkSecondRecord;
    }

    private Collection<ImportRecord> getRecords() {
        Collection<ImportRecord> records = new LinkedList<ImportRecord>();
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        //define first record
        MetadatumDTO title = createMetadatumDTO("dc","title", null,
                "Hardy potential versus lower order terms in Dirichlet problems: regularizing effects<sup>†</sup>");
        MetadatumDTO doi = createMetadatumDTO("dc", "identifier", "doi", "10.3934/mine.2023004");
        MetadatumDTO date = createMetadatumDTO("dc", "date", "issued", "2023-01-01");
        MetadatumDTO type = createMetadatumDTO("dc", "type", null, "Journal");
        MetadatumDTO citationVolume = createMetadatumDTO("oaire", "citation", "volume", "5");
        MetadatumDTO citationIssue = createMetadatumDTO("oaire", "citation", "issue", "1");
        MetadatumDTO scopusId = createMetadatumDTO("dc", "identifier", "scopus", "2-s2.0-85124241875");
        MetadatumDTO funding = createMetadatumDTO("dc", "relation", "funding", "Junta de Andalucía");
        MetadatumDTO grantno = createMetadatumDTO("dc", "relation", "grantno", "PGC2018-096422-B-I00");
        MetadatumDTO subject = createMetadatumDTO("dc", "subject", null,
                "Hardy potentials | Laplace equation | Summability of solutions");
        MetadatumDTO rights = createMetadatumDTO("dc", "rights", null, "open access");
        MetadatumDTO ispartof = createMetadatumDTO("dc", "relation", "ispartof",
                "Mathematics In Engineering");
        metadatums.add(title);
        metadatums.add(doi);
        metadatums.add(date);
        metadatums.add(type);
        metadatums.add(citationVolume);
        metadatums.add(citationIssue);
        metadatums.add(scopusId);
        metadatums.add(funding);
        metadatums.add(grantno);
        metadatums.add(subject);
        metadatums.add(rights);
        metadatums.add(ispartof);
        ImportRecord firstrRecord = new ImportRecord(metadatums);
        //define second record
        MetadatumDTO title2 = createMetadatumDTO("dc","title", null,
                "Large deviations for a binary collision model: energy evaporation<sup>†</sup>");
        MetadatumDTO doi2 = createMetadatumDTO("dc", "identifier", "doi", "10.3934/mine.2023001");
        MetadatumDTO date2 = createMetadatumDTO("dc", "date", "issued", "2023-01-01");
        MetadatumDTO type2 = createMetadatumDTO("dc", "type", null, "Journal");
        MetadatumDTO citationVolume2 = createMetadatumDTO("oaire", "citation", "volume", "5");
        MetadatumDTO citationIssue2 = createMetadatumDTO("oaire", "citation", "issue", "1");
        MetadatumDTO scopusId2 = createMetadatumDTO("dc", "identifier", "scopus", "2-s2.0-85124226483");
        MetadatumDTO grantno2 = createMetadatumDTO("dc", "relation", "grantno", "undefined");
        MetadatumDTO subject2 = createMetadatumDTO("dc", "subject", null,
        "Boltzmann equation | Discrete energy model | Kac model | Large deviations | Violation of energy conservation");
        MetadatumDTO rights2 = createMetadatumDTO("dc", "rights", null, "open access");
        MetadatumDTO ispartof2 = createMetadatumDTO("dc", "relation", "ispartof",
                "Mathematics In Engineering");
        metadatums.add(title2);
        metadatums.add(doi2);
        metadatums.add(date2);
        metadatums.add(type2);
        metadatums.add(citationVolume2);
        metadatums.add(citationIssue2);
        metadatums.add(scopusId2);
        metadatums.add(grantno2);
        metadatums.add(subject2);
        metadatums.add(rights2);
        metadatums.add(ispartof2);
        ImportRecord secondRecord = new ImportRecord(metadatums);
        records.add(firstrRecord);
        records.add(secondRecord);
        return records;
    }

    private MetadatumDTO createMetadatumDTO(String schema, String element, String qualifier, String value) {
        MetadatumDTO metadatumDTO = new MetadatumDTO();
        metadatumDTO.setSchema(schema);
        metadatumDTO.setElement(element);
        metadatumDTO.setQualifier(qualifier);
        metadatumDTO.setValue(value);
        return metadatumDTO;
    }

    private CloseableHttpResponse mockResponse(String xmlExample, int statusCode, String reason)
            throws UnsupportedEncodingException {
        BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
        basicHttpEntity.setChunked(true);
        basicHttpEntity.setContent(new StringInputStream(xmlExample));

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(statusLine(statusCode, reason));
        when(response.getEntity()).thenReturn(basicHttpEntity);
        return response;
    }

    private StatusLine statusLine(int statusCode, String reason) {
        return new StatusLine() {
            @Override
            public ProtocolVersion getProtocolVersion() {
                return new ProtocolVersion("http", 1, 1);
            }

            @Override
            public int getStatusCode() {
                return statusCode;
            }

            @Override
            public String getReasonPhrase() {
                return reason;
            }
        };
    }

}