/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.epo.service.EpoImportMetadataSourceServiceImpl;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link EpoImportMetadataSourceServiceImpl}
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class EpoImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

    @Autowired
    private LiveImportClientImpl liveImportClient;

    @Autowired
    private EpoImportMetadataSourceServiceImpl epoServiceImpl;

    @Test
    public void epoImportMetadataGetRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        InputStream file2token = null;
        InputStream file = null;
        InputStream file2 = null;
        InputStream file3 = null;
        String originKey = epoServiceImpl.getConsumerKey();
        String originSecret = epoServiceImpl.getConsumerSecret();
        CloseableHttpClient originalHttpClient = liveImportClient.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try {
            file2token = getClass().getResourceAsStream("epo-token.json");
            file = getClass().getResourceAsStream("epo-resp.xml");
            file2 = getClass().getResourceAsStream("epo-first.xml");
            file3 = getClass().getResourceAsStream("epo-second.xml");

            String tokenResp = IOUtils.toString(file2token, Charset.defaultCharset());
            String epoResp = IOUtils.toString(file, Charset.defaultCharset());
            String epoResp2 = IOUtils.toString(file2, Charset.defaultCharset());
            String epoResp3 = IOUtils.toString(file3, Charset.defaultCharset());

            epoServiceImpl.setConsumerKey("test-key");
            epoServiceImpl.setConsumerSecret("test-secret");
            liveImportClient.setHttpClient(httpClient);

            CloseableHttpResponse responseWithToken = mockResponse(tokenResp, 200, "OK");
            CloseableHttpResponse response1 = mockResponse(epoResp, 200, "OK");
            CloseableHttpResponse response2 = mockResponse(epoResp2, 200, "OK");
            CloseableHttpResponse response3 = mockResponse(epoResp3, 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any()))
                           .thenReturn(responseWithToken, response1, response2, response3);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords();
            Collection<ImportRecord> recordsImported = epoServiceImpl.getRecords("test query", 0, 2);
            assertEquals(2, recordsImported.size());
            matchRecords(new ArrayList<ImportRecord>(recordsImported), collection2match);
        } finally {
            if (Objects.nonNull(file2token)) {
                file2token.close();
            }
            if (Objects.nonNull(file)) {
                file.close();
            }
            if (Objects.nonNull(file2)) {
                file2.close();
            }
            if (Objects.nonNull(file3)) {
                file3.close();
            }
            epoServiceImpl.setConsumerKey(originKey);
            epoServiceImpl.setConsumerSecret(originSecret);
            liveImportClient.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void epoImportMetadataGetRecordsCountTest() throws Exception {
        context.turnOffAuthorisationSystem();
        InputStream file = null;
        InputStream file2 = null;
        String originKey = epoServiceImpl.getConsumerKey();
        String originSecret = epoServiceImpl.getConsumerSecret();
        CloseableHttpClient originalHttpClient = liveImportClient.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try {
            file = getClass().getResourceAsStream("epo-token.json");
            file2 = getClass().getResourceAsStream("epo-resp.xml");
            String token = IOUtils.toString(file, Charset.defaultCharset());
            String epoResp = IOUtils.toString(file2, Charset.defaultCharset());

            epoServiceImpl.setConsumerKey("test-key");
            epoServiceImpl.setConsumerSecret("test-secret");
            liveImportClient.setHttpClient(httpClient);

            CloseableHttpResponse responseWithToken = mockResponse(token, 200, "OK");
            CloseableHttpResponse response1 = mockResponse(epoResp, 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(responseWithToken, response1);

            context.restoreAuthSystemState();
            int tot = epoServiceImpl.getRecordsCount("test query");
            assertEquals(10000, tot);
        } finally {
            if (Objects.nonNull(file)) {
                file.close();
            }
            if (Objects.nonNull(file2)) {
                file2.close();
            }
            epoServiceImpl.setConsumerKey(originKey);
            epoServiceImpl.setConsumerSecret(originSecret);
            liveImportClient.setHttpClient(originalHttpClient);
        }
    }

    private ArrayList<ImportRecord> getRecords() {
        ArrayList<ImportRecord> records = new ArrayList<>();
        //define first record
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        MetadatumDTO identifierOther = createMetadatumDTO("dc", "identifier", "other", "epodoc:ES2902749T");
        MetadatumDTO identifier = createMetadatumDTO("dc", "identifier", null, "18705153");
        MetadatumDTO date = createMetadatumDTO("dc", "date", "issued", "2022-01-29");
        MetadatumDTO dateSubmitted = createMetadatumDTO("dc", "date", "submitted", "2018-01-19");
        MetadatumDTO applicant = createMetadatumDTO("dc", "contributor", null, "PANKA BLOOD TEST GMBH");
        MetadatumDTO applicant2 = createMetadatumDTO("dc", "contributor", null, "Panka Blood Test GmbH");
        MetadatumDTO author = createMetadatumDTO("dc", "contributor", "author", "PANTEL KLAUS");
        MetadatumDTO author2 = createMetadatumDTO("dc", "contributor", "author", " BARTKOWIAK KAI");
        MetadatumDTO author3 = createMetadatumDTO("dc", "contributor", "author", "PANTEL, Klaus, ");
        MetadatumDTO author4 = createMetadatumDTO("dc", "contributor", "author", "BARTKOWIAK, Kai");
        MetadatumDTO title = createMetadatumDTO("dc", "title", null, "Método para el diagnóstico del cáncer de mama");
        MetadatumDTO subject = createMetadatumDTO("dc", "subject", null,
                                              "G01N  33/   574            A I                    ");

        metadatums.add(identifierOther);
        metadatums.add(identifier);
        metadatums.add(date);
        metadatums.add(dateSubmitted);
        metadatums.add(applicant);
        metadatums.add(applicant2);
        metadatums.add(author);
        metadatums.add(author2);
        metadatums.add(author3);
        metadatums.add(author4);
        metadatums.add(title);
        metadatums.add(subject);

        ImportRecord firstrRecord = new ImportRecord(metadatums);

        //define second record
        List<MetadatumDTO> metadatums2  = new ArrayList<MetadatumDTO>();
        MetadatumDTO identifierOther2 = createMetadatumDTO("dc", "identifier", "other", "epodoc:TW202202864");
        MetadatumDTO identifier2 = createMetadatumDTO("dc", "identifier", null, "109122801");
        MetadatumDTO date2 = createMetadatumDTO("dc", "date", "issued", "2022-01-16");
        MetadatumDTO dateSubmitted2 = createMetadatumDTO("dc", "date", "submitted", "2020-01-06");
        MetadatumDTO applicant3 = createMetadatumDTO("dc", "contributor", null, "ADVANTEST CORP [JP]");
        MetadatumDTO applicant4 = createMetadatumDTO("dc", "contributor", null, "ADVANTEST CORPORATION");
        MetadatumDTO author5 = createMetadatumDTO("dc", "contributor", "author", "POEPPE OLAF [DE]");
        MetadatumDTO author6 = createMetadatumDTO("dc", "contributor", "author", " HILLIGES KLAUS-DIETER [DE]");
        MetadatumDTO author7 = createMetadatumDTO("dc", "contributor", "author", " KRECH ALAN [US]");
        MetadatumDTO author8 = createMetadatumDTO("dc", "contributor", "author", "POEPPE, OLAF, ");
        MetadatumDTO author9 = createMetadatumDTO("dc", "contributor", "author", "HILLIGES, KLAUS-DIETER, ");
        MetadatumDTO author10 = createMetadatumDTO("dc", "contributor", "author", "KRECH, ALAN");
        MetadatumDTO title2 = createMetadatumDTO("dc", "title", null,
                "Automated test equipment for testing one or more devices under test, method for automated"
              + " testing of one or more devices under test, and computer program using a buffer memory");
        MetadatumDTO subject2 = createMetadatumDTO("dc", "subject", null,
                "G01R  31/   319            A I                    ");
        MetadatumDTO subject3 = createMetadatumDTO("dc", "subject", null,
                "G01R  31/  3193            A I                    ");
        metadatums2.add(identifierOther2);
        metadatums2.add(identifier2);
        metadatums2.add(date2);
        metadatums2.add(dateSubmitted2);
        metadatums2.add(applicant3);
        metadatums2.add(applicant4);
        metadatums2.add(author5);
        metadatums2.add(author6);
        metadatums2.add(author7);
        metadatums2.add(author8);
        metadatums2.add(author9);
        metadatums2.add(author10);
        metadatums2.add(title2);
        metadatums2.add(subject2);
        metadatums2.add(subject3);

        ImportRecord secondRecord = new ImportRecord(metadatums2);
        records.add(firstrRecord);
        records.add(secondRecord);
        return records;
    }

}