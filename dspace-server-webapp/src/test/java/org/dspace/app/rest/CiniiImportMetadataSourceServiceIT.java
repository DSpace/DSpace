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
import org.dspace.importer.external.cinii.CiniiImportMetadataSourceServiceImpl;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link CiniiImportMetadataSourceServiceImpl}
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class CiniiImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

    @Autowired
    private LiveImportClientImpl liveImportClientImpl;

    @Autowired
    private CiniiImportMetadataSourceServiceImpl ciniiServiceImpl;

    @Test
    public void ciniiImportMetadataGetRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        InputStream ciniiRefResp = null;
        InputStream ciniiRefResp2 = null;
        InputStream ciniiRefResp3 = null;
        try {
            ciniiRefResp = getClass().getResourceAsStream("cinii-responce-ids.xml");
            ciniiRefResp2 = getClass().getResourceAsStream("cinii-first.xml");
            ciniiRefResp3 = getClass().getResourceAsStream("cinii-second.xml");

            String ciniiIdsXmlResp = IOUtils.toString(ciniiRefResp, Charset.defaultCharset());
            String ciniiFirstXmlResp = IOUtils.toString(ciniiRefResp2, Charset.defaultCharset());
            String ciniiSecondXmlResp = IOUtils.toString(ciniiRefResp3, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(ciniiIdsXmlResp, 200, "OK");
            CloseableHttpResponse response2 = mockResponse(ciniiFirstXmlResp, 200, "OK");
            CloseableHttpResponse response3 = mockResponse(ciniiSecondXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response, response2, response3);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords();
            Collection<ImportRecord> recordsImported = ciniiServiceImpl.getRecords("test query", 0, 2);
            assertEquals(2, recordsImported.size());
            matchRecords(new ArrayList<ImportRecord>(recordsImported), collection2match);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
            if (Objects.nonNull(ciniiRefResp)) {
                ciniiRefResp.close();
            }
            if (Objects.nonNull(ciniiRefResp2)) {
                ciniiRefResp2.close();
            }
            if (Objects.nonNull(ciniiRefResp3)) {
                ciniiRefResp3.close();
            }
        }
    }

    @Test
    public void ciniiImportMetadataGetRecordsCountTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream file = getClass().getResourceAsStream("cinii-responce-ids.xml")) {
            String ciniiXmlResp = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(ciniiXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            int tot = ciniiServiceImpl.getRecordsCount("test query");
            assertEquals(32, tot);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    private ArrayList<ImportRecord> getRecords() {
        ArrayList<ImportRecord> records = new ArrayList<>();
        //define first record
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        MetadatumDTO title = createMetadatumDTO("dc", "title", null,
                "Understanding the impact of mandatory accrual accounting on management practices:"
                + " Interpretation of Japanese local governments’ behavior");
        MetadatumDTO identifier = createMetadatumDTO("dc", "identifier", "other", "1010572092222310146");

        metadatums.add(title);
        metadatums.add(identifier);

        ImportRecord firstrRecord = new ImportRecord(metadatums);

        //define second record
        List<MetadatumDTO> metadatums2  = new ArrayList<MetadatumDTO>();
        MetadatumDTO title2 = createMetadatumDTO("dc", "title", null,
                "Band structures of passive films on titanium in simulated bioliquids determined"
                + " by photoelectrochemical response: principle governing the biocompatibility");
        MetadatumDTO language = createMetadatumDTO("dc", "language", "iso", "en");
        MetadatumDTO identifier2 = createMetadatumDTO("dc", "identifier", "other", "1050010687833449984");

        metadatums2.add(title2);
        metadatums2.add(language);
        metadatums2.add(identifier2);

        ImportRecord secondRecord = new ImportRecord(metadatums2);
        records.add(firstrRecord);
        records.add(secondRecord);
        return records;
    }

}