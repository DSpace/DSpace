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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
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
        try (InputStream file = getClass().getResourceAsStream("wos-responce.xml")) {
            String wosXmlResp = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);

            CloseableHttpResponse response = mockResponse(wosXmlResp, 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            Collection<ImportRecord> recordsImported = wosImportMetadataService.getRecords("test query", 0, 2);
            ArrayList<ImportRecord> collection2match = getRecords();
            assertEquals(2, recordsImported.size());
            matchRecords(new ArrayList<ImportRecord>(recordsImported), collection2match);
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
        try (InputStream file = getClass().getResourceAsStream("wos-responce.xml")) {
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

    private ArrayList<ImportRecord> getRecords() {
        ArrayList<ImportRecord> records = new ArrayList<>();
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        //define first record
        MetadatumDTO edition = createMetadatumDTO("oaire","citation", "edition", "WOS.ISSHP");
        MetadatumDTO date = createMetadatumDTO("dc", "date", "issued", "2019");
        MetadatumDTO startPage = createMetadatumDTO("oaire","citation", "startPage", "225");
        MetadatumDTO endPage = createMetadatumDTO("oaire","citation", "endPage", "234");
        MetadatumDTO type = createMetadatumDTO("dc", "type", null, "Book in series");
        MetadatumDTO ispartof = createMetadatumDTO("dc", "relation", "ispartof",
                "EDULEARN19: 11TH INTERNATIONAL CONFERENCE ON EDUCATION AND NEW LEARNING TECHNOLOGIES");
        MetadatumDTO ispartofseries = createMetadatumDTO("dc", "relation", "ispartofseries",
                "EDULEARN19: 11TH INTERNATIONAL CONFERENCE ON EDUCATION AND NEW LEARNING TECHNOLOGIES");
        MetadatumDTO title = createMetadatumDTO("dc", "title", null,
                "MENTORING IN EDUCATION. FEMALE ROLE MODELS IN ITALIAN DESIGN ACADEMIC CULTURE");
        MetadatumDTO issn = createMetadatumDTO("dc", "identifier", "issn", "2340-1117");
        MetadatumDTO isbn = createMetadatumDTO("dc", "identifier", "isbn", "978-84-09-12031-4");
        MetadatumDTO description = createMetadatumDTO("dc", "description", "abstract",
                                         "It is widely recognized that mentors and tutors are");
        MetadatumDTO iso = createMetadatumDTO("dc", "language", "iso", "1");
        MetadatumDTO author = createMetadatumDTO("dc", "contributor", "author", "Bollini, Letizia");
        MetadatumDTO author2 = createMetadatumDTO("dc", "contributor", "author", "Chova, LG");
        MetadatumDTO author3 = createMetadatumDTO("dc", "contributor", "author", "Martinez, AL");
        MetadatumDTO author4 = createMetadatumDTO("dc", "contributor", "author", "Torres, IC");
        MetadatumDTO subject = createMetadatumDTO("dc", "subject", null, "VISUAL DESIGN");
        MetadatumDTO subject2 = createMetadatumDTO("dc", "subject", null, "EXPERIENCE");
        MetadatumDTO subject3 = createMetadatumDTO("dc", "subject", null, "Design education");
        MetadatumDTO subject4 = createMetadatumDTO("dc", "subject", null, "female role models");
        MetadatumDTO subject5 = createMetadatumDTO("dc", "subject", null, "mentoring in education");
        MetadatumDTO subject6 = createMetadatumDTO("dc", "subject", null, "Social Sciences");
        MetadatumDTO editor = createMetadatumDTO("dc", "contributor", "editor", "Chova, LG");
        MetadatumDTO editor2 = createMetadatumDTO("dc", "contributor", "editor", "Martinez, AL");
        MetadatumDTO editor3 = createMetadatumDTO("dc", "contributor", "editor", "Torres, IC");
        MetadatumDTO isi = createMetadatumDTO("dc", "identifier", "other", "WOS:000551093100034");
        metadatums.add(edition);
        metadatums.add(date);
        metadatums.add(startPage);
        metadatums.add(endPage);
        metadatums.add(type);
        metadatums.add(ispartof);
        metadatums.add(ispartofseries);
        metadatums.add(title);
        metadatums.add(issn);
        metadatums.add(isbn);
        metadatums.add(description);
        metadatums.add(iso);
        metadatums.add(author);
        metadatums.add(author2);
        metadatums.add(author3);
        metadatums.add(author4);
        metadatums.add(subject);
        metadatums.add(subject2);
        metadatums.add(subject3);
        metadatums.add(subject4);
        metadatums.add(subject5);
        metadatums.add(subject6);
        metadatums.add(editor);
        metadatums.add(editor2);
        metadatums.add(editor3);
        metadatums.add(isi);
        ImportRecord firstrRecord = new ImportRecord(metadatums);

        //define second record
        List<MetadatumDTO> metadatums2  = new ArrayList<MetadatumDTO>();
        MetadatumDTO edition2 = createMetadatumDTO("oaire","citation", "edition", "WOS.ISSHP");
        MetadatumDTO date2 = createMetadatumDTO("dc", "date", "issued", "2019");
        MetadatumDTO startPage2 = createMetadatumDTO("oaire","citation", "startPage", "224");
        MetadatumDTO endPage2 = createMetadatumDTO("oaire","citation", "endPage", "224");
        MetadatumDTO type2 = createMetadatumDTO("dc", "type", null, "Book in series");
        MetadatumDTO ispartof2 = createMetadatumDTO("dc", "relation", "ispartof",
                "EDULEARN19: 11TH INTERNATIONAL CONFERENCE ON EDUCATION AND NEW LEARNING TECHNOLOGIES");
        MetadatumDTO ispartofseries2 = createMetadatumDTO("dc", "relation", "ispartofseries",
                "EDULEARN19: 11TH INTERNATIONAL CONFERENCE ON EDUCATION AND NEW LEARNING TECHNOLOGIES");
        MetadatumDTO title2 = createMetadatumDTO("dc", "title", null,
        "DYSLEXIA AND TYPOGRAPHY: HOW TO IMPROVE EDUCATIONAL PUBLISHING FOR INCLUSIVE DESIGN - A CRITICAL REVIEW");
        MetadatumDTO issn2 = createMetadatumDTO("dc", "identifier", "issn", "2340-1117");
        MetadatumDTO isbn2 = createMetadatumDTO("dc", "identifier", "isbn", "978-84-09-12031-4");
        MetadatumDTO iso2 = createMetadatumDTO("dc", "language", "iso", "1");
        MetadatumDTO author5 = createMetadatumDTO("dc", "contributor", "author", "Bollini, L.");
        MetadatumDTO author6 = createMetadatumDTO("dc", "contributor", "author", "Chova, LG");
        MetadatumDTO author7 = createMetadatumDTO("dc", "contributor", "author", "Martinez, AL");
        MetadatumDTO author8 = createMetadatumDTO("dc", "contributor", "author", "Torres, IC");
        MetadatumDTO subject7 = createMetadatumDTO("dc", "subject", null, "Dyslexia and Typography");
        MetadatumDTO subject8 = createMetadatumDTO("dc", "subject", null, "Font design");
        MetadatumDTO subject9 = createMetadatumDTO("dc", "subject", null, "educational publishing");
        MetadatumDTO subject10 = createMetadatumDTO("dc", "subject", null, "Design for all");
        MetadatumDTO subject11 = createMetadatumDTO("dc", "subject", null, "Universal design");
        MetadatumDTO subject12 = createMetadatumDTO("dc", "subject", null, "Social Sciences");
        MetadatumDTO isi2 = createMetadatumDTO("dc", "identifier", "other", "WOS:000551093100033");
        metadatums2.add(edition2);
        metadatums2.add(date2);
        metadatums2.add(startPage2);
        metadatums2.add(endPage2);
        metadatums2.add(type2);
        metadatums2.add(ispartof2);
        metadatums2.add(ispartofseries2);
        metadatums2.add(title2);
        metadatums2.add(issn2);
        metadatums2.add(isbn2);
        metadatums2.add(iso2);
        metadatums2.add(author5);
        metadatums2.add(author6);
        metadatums2.add(author7);
        metadatums2.add(author8);
        metadatums2.add(subject7);
        metadatums2.add(subject8);
        metadatums2.add(subject9);
        metadatums2.add(subject10);
        metadatums2.add(subject11);
        metadatums2.add(subject12);
        metadatums2.add(editor);
        metadatums2.add(editor2);
        metadatums2.add(editor3);
        metadatums2.add(isi2);
        ImportRecord secondRecord = new ImportRecord(metadatums2);

        records.add(firstrRecord);
        records.add(secondRecord);
        return records;
    }

}