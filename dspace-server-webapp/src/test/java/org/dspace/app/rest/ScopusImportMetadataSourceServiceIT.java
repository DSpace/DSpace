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
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
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
public class ScopusImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

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
        try (InputStream file = getClass().getResourceAsStream("scopus-ex.xml")) {
            String scopusXmlResp = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(scopusXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords();
            Collection<ImportRecord> recordsImported = scopusServiceImpl.getRecords("roma", 0, 2);
            assertEquals(2, recordsImported.size());
            matchRecords(new ArrayList<ImportRecord>(recordsImported), collection2match);
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
        try (InputStream file = getClass().getResourceAsStream("scopus-ex.xml")) {
            String scopusXmlResp = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(scopusXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            int tot = scopusServiceImpl.getRecordsCount("roma");
            assertEquals(2, tot);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
            scopusServiceImpl.setApiKey(originApiKey);
        }
    }

    @Test
    public void scopusImportMetadataGetRecordsEmptyResponseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String originApiKey = scopusServiceImpl.getApiKey();
        if (StringUtils.isBlank(originApiKey)) {
            scopusServiceImpl.setApiKey("testApiKey");
        }
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream file = getClass().getResourceAsStream("scopus-empty-resp.xml")) {
            String scopusXmlResp = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(scopusXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            Collection<ImportRecord> recordsImported = scopusServiceImpl.getRecords("roma", 0, 20);
            assertTrue(recordsImported.isEmpty());
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
            scopusServiceImpl.setApiKey(originApiKey);
        }
    }

    @Test
    public void scopusImportMetadataGetRecordTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String originApiKey = scopusServiceImpl.getApiKey();
        if (StringUtils.isBlank(originApiKey)) {
            scopusServiceImpl.setApiKey("testApiKey");
        }
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream file = getClass().getResourceAsStream("scopus-ex.xml")) {
            String scopusXmlResp = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(scopusXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();

            ImportRecord importRecord = scopusServiceImpl.getRecord("2-s2.0-85124241875");
            matchRecord(importRecord, getRecords().get(0));
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
            scopusServiceImpl.setApiKey(originApiKey);
        }
    }

    @Test
    public void scopusImportAuthorMetadataWhenAuthorsElementsMissedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String originApiKey = scopusServiceImpl.getApiKey();
        if (StringUtils.isBlank(originApiKey)) {
            scopusServiceImpl.setApiKey("testApiKey");
        }
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try {
            String scopusXmlResp = getResponseWithCreatorAndWithoutAuthorsElement();

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(scopusXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();

            MetadatumDTO createdMetadata = createMetadatumDTO("dc", "contributor", "author", "Sforza L.");
            ImportRecord importRecord = scopusServiceImpl.getRecord("2-s2.0-85128066737");

            assertEquals(importRecord.getValue("dc", "contributor", "author").size(), 1);
            matchMetadataValue(new ArrayList<>(importRecord.getValue("dc", "contributor", "author")),
                List.of(createdMetadata));
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
            scopusServiceImpl.setApiKey(originApiKey);
        }
    }

    private ArrayList<ImportRecord> getRecords() {
        ArrayList<ImportRecord> records = new ArrayList<>();
        //define first record
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        MetadatumDTO doi = createMetadatumDTO("dc", "identifier", null, "10.3934/mine.2023004");
        MetadatumDTO title = createMetadatumDTO("dc","title", null,
                "Hardy potential versus lower order terms in Dirichlet problems: regularizing effects<sup>†</sup>");
        MetadatumDTO type = createMetadatumDTO("dc", "type", null, "Resource Types::text::journal::journal article");
        MetadatumDTO date = createMetadatumDTO("dc", "date", "issued", "2023-01-01");
        MetadatumDTO scopusId = createMetadatumDTO("dc", "identifier", "scopus", "2-s2.0-85124241875");
        MetadatumDTO citationVolume = createMetadatumDTO("oaire", "citation", "volume", "5");
        MetadatumDTO citationIssue = createMetadatumDTO("oaire", "citation", "issue", "1");
        MetadatumDTO subject = createMetadatumDTO("dc", "subject", null,
                                                  "Hardy potentials | Laplace equation | Summability of solutions");
        MetadatumDTO author = createMetadatumDTO("dc", "contributor", "author", "Arcoya, David");
        MetadatumDTO scopusAuthorId = createMetadatumDTO("person", "identifier", "scopus-author-id", "6602330574");
        MetadatumDTO orgunit = createMetadatumDTO("person", "affiliation", "name", "Universidad de Granada");
        MetadatumDTO author2 = createMetadatumDTO("dc", "contributor", "author", "Boccardo, Lucio");
        MetadatumDTO scopusAuthorId2 = createMetadatumDTO("person", "identifier", "scopus-author-id", "7003612261");
        MetadatumDTO orgunit2 = createMetadatumDTO("person", "affiliation", "name","Sapienza Università di Roma");
        MetadatumDTO author3 = createMetadatumDTO("dc", "contributor", "author", "Orsina, Luigi");
        MetadatumDTO scopusAuthorId3 = createMetadatumDTO("person", "identifier", "scopus-author-id", "6602595438");
        MetadatumDTO orgunit3 = createMetadatumDTO("person", "affiliation", "name","Sapienza Università di Roma");
        MetadatumDTO rights = createMetadatumDTO("dc", "rights", null, "true");
        MetadatumDTO ispartof = createMetadatumDTO("dc", "relation", "ispartof", "Mathematics In Engineering");
        MetadatumDTO ispartofseries = createMetadatumDTO("dc","relation","ispartofseries","Mathematics In Engineering");

        metadatums.add(doi);
        metadatums.add(title);
        metadatums.add(date);
        metadatums.add(scopusId);
        metadatums.add(citationVolume);
        metadatums.add(citationIssue);
        metadatums.add(subject);
        metadatums.add(rights);
        metadatums.add(ispartof);
        metadatums.add(ispartofseries);
        metadatums.add(author);
        metadatums.add(scopusAuthorId);
        metadatums.add(orgunit);
        metadatums.add(author2);
        metadatums.add(scopusAuthorId2);
        metadatums.add(orgunit2);
        metadatums.add(author3);
        metadatums.add(scopusAuthorId3);
        metadatums.add(orgunit3);
        metadatums.add(type);
        ImportRecord firstrRecord = new ImportRecord(metadatums);

        //define second record
        List<MetadatumDTO> metadatums2  = new ArrayList<MetadatumDTO>();
        MetadatumDTO doi2 = createMetadatumDTO("dc", "identifier", null, "10.3934/mine.2023001");
        MetadatumDTO title2 = createMetadatumDTO("dc","title", null,
                "Large deviations for a binary collision model: energy evaporation<sup>†</sup>");
        MetadatumDTO date2 = createMetadatumDTO("dc", "date", "issued", "2023-01-01");
        MetadatumDTO scopusId2 = createMetadatumDTO("dc", "identifier", "scopus", "2-s2.0-85124226483");
        MetadatumDTO type2 = createMetadatumDTO("dc", "type", null, "Resource Types::text::journal::journal article");
        MetadatumDTO citationVolume2 = createMetadatumDTO("oaire", "citation", "volume", "5");
        MetadatumDTO citationIssue2 = createMetadatumDTO("oaire", "citation", "issue", "1");

        MetadatumDTO subject2 = createMetadatumDTO("dc", "subject", null,
        "Boltzmann equation | Discrete energy model | Kac model | Large deviations | Violation of energy conservation");

        MetadatumDTO author4 = createMetadatumDTO("dc", "contributor", "author", "Basile, Giada");
        MetadatumDTO scopusAuthorId4 = createMetadatumDTO("person", "identifier", "scopus-author-id", "55613229065");
        MetadatumDTO orgunit4 = createMetadatumDTO("person", "affiliation", "name","Sapienza Università di Roma");
        MetadatumDTO author5 = createMetadatumDTO("dc", "contributor", "author", "Benedetto, Dario");
        MetadatumDTO scopusAuthorId5 = createMetadatumDTO("person", "identifier", "scopus-author-id", "55893665100");
        MetadatumDTO orgunit5 = createMetadatumDTO("person", "affiliation", "name","Sapienza Università di Roma");
        MetadatumDTO author6 = createMetadatumDTO("dc", "contributor", "author", "Caglioti, Emanuele");
        MetadatumDTO scopusAuthorId6 = createMetadatumDTO("person", "identifier", "scopus-author-id", "7004588675");
        MetadatumDTO orgunit6 = createMetadatumDTO("person", "affiliation", "name","Sapienza Università di Roma");
        MetadatumDTO author7 = createMetadatumDTO("dc", "contributor", "author", "Bertini, Lorenzo");
        MetadatumDTO scopusAuthorId7 = createMetadatumDTO("person", "identifier", "scopus-author-id", "7005555198");
        MetadatumDTO orgunit7 = createMetadatumDTO("person", "affiliation", "name","Sapienza Università di Roma");
        MetadatumDTO rights2 = createMetadatumDTO("dc", "rights", null, "true");
        MetadatumDTO ispartof2 = createMetadatumDTO("dc", "relation", "ispartof", "Mathematics In Engineering");
        MetadatumDTO ispartofseries2 = createMetadatumDTO("dc", "relation", "ispartofseries",
                                                          "Mathematics In Engineering");
        metadatums2.add(doi2);
        metadatums2.add(title2);
        metadatums2.add(date2);
        metadatums2.add(scopusId2);
        metadatums2.add(citationVolume2);
        metadatums2.add(citationIssue2);
        metadatums2.add(subject2);
        metadatums2.add(rights2);
        metadatums2.add(ispartof2);
        metadatums2.add(ispartofseries2);
        metadatums2.add(author4);
        metadatums2.add(scopusAuthorId4);
        metadatums2.add(orgunit4);
        metadatums2.add(author5);
        metadatums2.add(scopusAuthorId5);
        metadatums2.add(orgunit5);
        metadatums2.add(author6);
        metadatums2.add(scopusAuthorId6);
        metadatums2.add(orgunit6);
        metadatums2.add(author7);
        metadatums2.add(scopusAuthorId7);
        metadatums2.add(orgunit7);
        metadatums2.add(type2);
        ImportRecord secondRecord = new ImportRecord(metadatums2);
        records.add(firstrRecord);
        records.add(secondRecord);
        return records;
    }

    private String getResponseWithCreatorAndWithoutAuthorsElement() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<search-results " +
            "xmlns=\"http://www.w3.org/2005/Atom\" " +
            "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
            "xmlns:opensearch=\"http://a9.com/-/spec/opensearch/1.1/\" " +
            "xmlns:prism=\"http://prismstandard.org/namespaces/basic/2.0/\" " +
            "xmlns:atom=\"http://www.w3.org/2005/Atom\">\n" +
            "    <opensearch:totalResults>1</opensearch:totalResults>\n" +
            "    <opensearch:startIndex>0</opensearch:startIndex>\n" +
            "    <opensearch:itemsPerPage>1</opensearch:itemsPerPage>\n" +
            "    <opensearch:Query role=\"request\" searchTerms=\"EID(2-s2.0-85128066737)\" startPage=\"0\"/>\n" +
            "    <link ref=\"self\" href=\"https://api.elsevier.com/content/search/scopus?start=0&amp;count=25&amp;" +
            "query=EID%282-s2.0-85128066737%29\" type=\"application/xml\"/>\n" +
            "    <link ref=\"first\" href=\"https://api.elsevier.com/content/search/scopus?start=0&amp;count=25&amp;" +
            "query=EID%282-s2.0-85128066737%29\" type=\"application/xml\"/>\n" +
            "<entry>\n" +
            "<dc:creator>Sforza L.</dc:creator>\n" +
            "</entry>\n" +
            "</search-results>";
    }

}
