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

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.scopus.service.LiveImportClientImpl;
import org.dspace.importer.external.wos.service.WOSImportMetadataSourceServiceImpl;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link WOSImportMetadataSourceServiceImpl}
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class WOSImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

    @Autowired
    private WOSImportMetadataSourceServiceImpl wosImportMetadataService;

    @Autowired
    private LiveImportClientImpl liveImportClientImpl;

    @Test
    public void wosImportMetadataGetRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String originApiKey = wosImportMetadataService.getApiKey();
        if (StringUtils.isBlank(originApiKey)) {
            wosImportMetadataService.setApiKey("testApiKey");
        }
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        String path2file = testProps.get("test.wos").toString();
        try (FileInputStream file = new FileInputStream(path2file)) {
            String wosXmlResp = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);

            CloseableHttpResponse response = mockResponse(wosXmlResp, 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            Collection<ImportRecord> recordsImported = wosImportMetadataService.getRecords("test query", 0, 2);
            Collection<ImportRecord> collection2match = getRecords();
            assertEquals(2, recordsImported.size());
            assertTrue(matchRecords(recordsImported, collection2match));
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
            wosImportMetadataService.setApiKey(originApiKey);
        }
    }

    @Test
    public void wosImportMetadataGetRecordsCountTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String originApiKey = wosImportMetadataService.getApiKey();
        if (StringUtils.isBlank(originApiKey)) {
            wosImportMetadataService.setApiKey("testApiKey");
        }
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        String path = testProps.get("test.wos").toString();
        try (FileInputStream file = new FileInputStream(path)) {
            String wosXmlResp = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(wosXmlResp, 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            int tot = wosImportMetadataService.getRecordsCount("test query");
            assertEquals(2, tot);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
            wosImportMetadataService.setApiKey(originApiKey);
        }
    }

    private Collection<ImportRecord> getRecords() {
        Collection<ImportRecord> records = new LinkedList<ImportRecord>();
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        //define first record
        MetadatumDTO edition = createMetadatumDTO("oaire","citation", "edition", "WOS.ISSHP");
        MetadatumDTO date = createMetadatumDTO("dc", "date", "issued", "2019");
        MetadatumDTO startPage = createMetadatumDTO("oaire","citation", "startPage", "225");
        MetadatumDTO endPage = createMetadatumDTO("oaire","citation", "endPage", "234");
        MetadatumDTO type = createMetadatumDTO("dc", "type", null, "Book in series");
        MetadatumDTO issn = createMetadatumDTO("dc", "relation", "issn", "2340-1117");
        MetadatumDTO isbn = createMetadatumDTO("dc", "identifier", "isbn", "978-84-09-12031-4");
        MetadatumDTO iso = createMetadatumDTO("dc", "language", "iso", "1");
        MetadatumDTO author = createMetadatumDTO("dc", "contributor", "author", "Bollini, Letizia");
        MetadatumDTO author2 = createMetadatumDTO("dc", "contributor", "author", "Chova, LG");
        MetadatumDTO author3 = createMetadatumDTO("dc", "contributor", "author", "Martinez, AL");
        MetadatumDTO author4 = createMetadatumDTO("dc", "contributor", "author", "Torres, IC");
        MetadatumDTO isi = createMetadatumDTO("dc", "identifier", "isi", "WOS:000551093100034");
        metadatums.add(edition);
        metadatums.add(date);
        metadatums.add(startPage);
        metadatums.add(endPage);
        metadatums.add(type);
        metadatums.add(issn);
        metadatums.add(isbn);
        metadatums.add(iso);
        metadatums.add(author);
        metadatums.add(author2);
        metadatums.add(author3);
        metadatums.add(author4);
        metadatums.add(isi);
        ImportRecord firstrRecord = new ImportRecord(metadatums);

        //define second record
        List<MetadatumDTO> metadatums2  = new ArrayList<MetadatumDTO>();
        MetadatumDTO edition2 = createMetadatumDTO("oaire","citation", "edition", "WOS.ISSHP");
        MetadatumDTO date2 = createMetadatumDTO("dc", "date", "issued", "2019");
        MetadatumDTO startPage2 = createMetadatumDTO("oaire","citation", "startPage", "224");
        MetadatumDTO endPage2 = createMetadatumDTO("oaire","citation", "endPage", "224");
        MetadatumDTO type2 = createMetadatumDTO("dc", "type", null, "Book in series");
        MetadatumDTO issn2 = createMetadatumDTO("dc", "relation", "issn", "2340-1117");
        MetadatumDTO isbn2 = createMetadatumDTO("dc", "identifier", "isbn", "978-84-09-12031-4");
        MetadatumDTO iso2 = createMetadatumDTO("dc", "language", "iso", "1");
        MetadatumDTO author5 = createMetadatumDTO("dc", "contributor", "author", "Bollini, Letizia");
        MetadatumDTO author6 = createMetadatumDTO("dc", "contributor", "author", "Chova, LG");
        MetadatumDTO author7 = createMetadatumDTO("dc", "contributor", "author", "Martinez, AL");
        MetadatumDTO author8 = createMetadatumDTO("dc", "contributor", "author", "Torres, IC");
        MetadatumDTO isi2 = createMetadatumDTO("dc", "identifier", "isi", "WOS:000551093100033");
        metadatums2.add(edition2);
        metadatums2.add(date2);
        metadatums2.add(startPage2);
        metadatums2.add(endPage2);
        metadatums2.add(type2);
        metadatums2.add(issn2);
        metadatums2.add(isbn2);
        metadatums2.add(iso2);
        metadatums2.add(author5);
        metadatums2.add(author6);
        metadatums2.add(author7);
        metadatums2.add(author8);
        metadatums2.add(isi2);
        ImportRecord secondRecord = new ImportRecord(metadatums2);

        records.add(firstrRecord);
        records.add(secondRecord);
        return records;
    }

}