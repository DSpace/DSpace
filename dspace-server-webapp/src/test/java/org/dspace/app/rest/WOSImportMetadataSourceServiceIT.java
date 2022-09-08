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
            Collection<ImportRecord> recordsImported = wosImportMetadataService.getRecords("science", 0, 2);
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
            int tot = wosImportMetadataService.getRecordsCount("science");
            assertEquals(1853785, tot);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
            wosImportMetadataService.setApiKey(originApiKey);
        }
    }

    private ArrayList<ImportRecord> getRecords() {
        ArrayList<ImportRecord> records = new ArrayList<>();
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        //define first record
        MetadatumDTO edition = createMetadatumDTO("oaire","citation", "edition", "WOS.SSCI");
        MetadatumDTO date = createMetadatumDTO("dc", "date", "issued", "2022");
        MetadatumDTO type = createMetadatumDTO("dc", "type", null, "Journal");
        MetadatumDTO ispartof = createMetadatumDTO("dc", "relation", "ispartof",
                                   "ETR&D-EDUCATIONAL TECHNOLOGY RESEARCH AND DEVELOPMENT");
        MetadatumDTO ispartofseries = createMetadatumDTO("dc", "relation", "ispartofseries",
                                   "ETR&D-EDUCATIONAL TECHNOLOGY RESEARCH AND DEVELOPMENT");
        MetadatumDTO title = createMetadatumDTO("dc", "title", null,
                "Preservice science teachers coding science simulations:"
                + " epistemological understanding, coding skills, and lesson design");
        MetadatumDTO issn = createMetadatumDTO("dc", "identifier", "issn", "1042-1629");
        MetadatumDTO doi = createMetadatumDTO("dc", "identifier", null, "10.1007/s11423-022-10119-7");
        MetadatumDTO description = createMetadatumDTO("dc", "description", "abstract",
                "National and state science learning standards urge K-12 educators to offer authentic Science,"
                + " Technology, Engineering, and Mathematics learning experiences.");
        MetadatumDTO iso = createMetadatumDTO("dc", "language", "iso", "1");
        MetadatumDTO author = createMetadatumDTO("dc", "contributor", "author", "Vasconcelos, Lucas");
        MetadatumDTO author2 = createMetadatumDTO("dc", "contributor", "author", "Kim, ChanMin");
        MetadatumDTO subject = createMetadatumDTO("dc", "subject", null, "MODEL-BASED INQUIRY");
        MetadatumDTO subject2 = createMetadatumDTO("dc", "subject", null, "COMPUTATIONAL THINKING");
        MetadatumDTO subject3 = createMetadatumDTO("dc", "subject", null, "ELEMENTARY TEACHERS");
        MetadatumDTO subject4 = createMetadatumDTO("dc", "subject", null, "STIMULATED-RECALL");
        MetadatumDTO subject5 = createMetadatumDTO("dc", "subject", null, "STUDENTS");
        MetadatumDTO subject6 = createMetadatumDTO("dc", "subject", null, "TECHNOLOGY");
        MetadatumDTO subject7 = createMetadatumDTO("dc", "subject", null, "KNOWLEDGE");
        MetadatumDTO subject8 = createMetadatumDTO("dc", "subject", null, "K-12");
        MetadatumDTO subject9 = createMetadatumDTO("dc", "subject", null, "CONCEPTIONS");
        MetadatumDTO subject10 = createMetadatumDTO("dc", "subject", null, "VIEWS");
        MetadatumDTO subject11 = createMetadatumDTO("dc", "subject", null, "Scientific models");
        MetadatumDTO subject12 = createMetadatumDTO("dc", "subject", null, "Scientific modeling");
        MetadatumDTO subject13 = createMetadatumDTO("dc", "subject", null, "Block-based coding");
        MetadatumDTO subject14 = createMetadatumDTO("dc", "subject", null, "Epistemological understanding");
        MetadatumDTO subject15 = createMetadatumDTO("dc", "subject", null, "Coding concepts");
        MetadatumDTO subject16 = createMetadatumDTO("dc", "subject", null, "Lesson design");
        MetadatumDTO subject17 = createMetadatumDTO("dc", "subject", null, "Social Sciences");
        MetadatumDTO other = createMetadatumDTO("dc", "identifier", "other", "WOS:000805105200003");
        metadatums.add(edition);
        metadatums.add(date);
        metadatums.add(type);
        metadatums.add(ispartof);
        metadatums.add(ispartofseries);
        metadatums.add(title);
        metadatums.add(issn);
        metadatums.add(doi);
        metadatums.add(description);
        metadatums.add(iso);
        metadatums.add(author);
        metadatums.add(author2);
        metadatums.add(subject);
        metadatums.add(subject2);
        metadatums.add(subject3);
        metadatums.add(subject4);
        metadatums.add(subject5);
        metadatums.add(subject6);
        metadatums.add(subject7);
        metadatums.add(subject8);
        metadatums.add(subject9);
        metadatums.add(subject10);
        metadatums.add(subject11);
        metadatums.add(subject12);
        metadatums.add(subject13);
        metadatums.add(subject14);
        metadatums.add(subject15);
        metadatums.add(subject16);
        metadatums.add(subject17);
        metadatums.add(other);
        ImportRecord firstrRecord = new ImportRecord(metadatums);

        //define second record
        List<MetadatumDTO> metadatums2  = new ArrayList<MetadatumDTO>();
        MetadatumDTO edition2 = createMetadatumDTO("oaire","citation", "edition", "WOS.SCI");
        MetadatumDTO edition3 = createMetadatumDTO("oaire","citation", "edition", "WOS.SSCI");
        MetadatumDTO date2 = createMetadatumDTO("dc", "date", "issued", "2022");
        MetadatumDTO type2 = createMetadatumDTO("dc", "type", null, "Journal");
        MetadatumDTO ispartof2 = createMetadatumDTO("dc", "relation", "ispartof", "NATURE HUMAN BEHAVIOUR");
        MetadatumDTO ispartofseries2 = createMetadatumDTO("dc", "relation", "ispartofseries", "NATURE HUMAN BEHAVIOUR");

        MetadatumDTO title2 = createMetadatumDTO("dc", "title", null,
        "The latent structure of global scientific development");

        MetadatumDTO issn2 = createMetadatumDTO("dc", "identifier", "issn", "2397-3374");
        MetadatumDTO doi2 = createMetadatumDTO("dc", "identifier", null, "10.1038/s41562-022-01367-x");
        MetadatumDTO description2 = createMetadatumDTO("dc", "description", "abstract",
                "Science is essential to innovation and economic prosperity."
                + "By examining the scientific output of each country,"
                + " Ahn et al. reveal a three-cluster structure of global science.");
        MetadatumDTO iso2 = createMetadatumDTO("dc", "language", "iso", "1");
        MetadatumDTO author3 = createMetadatumDTO("dc", "contributor", "author", "Miao, Lili");
        MetadatumDTO author4 = createMetadatumDTO("dc", "contributor", "author", "Murray, Dakota");
        MetadatumDTO author5 = createMetadatumDTO("dc", "contributor", "author", "Jung, Woo-Sung");
        MetadatumDTO author6 = createMetadatumDTO("dc", "contributor", "author", "Lariviere, Vincent");
        MetadatumDTO author7 = createMetadatumDTO("dc", "contributor", "author", "Sugimoto, Cassidy R.");
        MetadatumDTO author8 = createMetadatumDTO("dc", "contributor", "author", "Ahn, Yong-Yeol");
        MetadatumDTO subject18 = createMetadatumDTO("dc", "subject", null, "RESEARCH OUTPUT");
        MetadatumDTO subject19 = createMetadatumDTO("dc", "subject", null, "ECONOMIC-GROWTH");
        MetadatumDTO subject20 = createMetadatumDTO("dc", "subject", null, "SCIENCE");
        MetadatumDTO subject21 = createMetadatumDTO("dc", "subject", null, "KNOWLEDGE");
        MetadatumDTO subject22 = createMetadatumDTO("dc", "subject", null, "CHINA");
        MetadatumDTO subject23 = createMetadatumDTO("dc", "subject", null, "CAUSALITY");
        MetadatumDTO subject24 = createMetadatumDTO("dc", "subject", null, "BACKBONE");
        MetadatumDTO subject25 = createMetadatumDTO("dc", "subject", null, "SPREAD");
        MetadatumDTO subject26 = createMetadatumDTO("dc", "subject", null, "Social Sciences");
        MetadatumDTO subject27 = createMetadatumDTO("dc", "subject", null, "Science & Technology");
        MetadatumDTO subject28 = createMetadatumDTO("dc", "subject", null, "Life Sciences & Biomedicine");
        MetadatumDTO other2 = createMetadatumDTO("dc", "identifier", "other", "WOS:000805100600001");
        MetadatumDTO rid = createMetadatumDTO("person", "identifier", "rid", "C-6334-2011");
        MetadatumDTO rid2 = createMetadatumDTO("person", "identifier", "rid", "B-1251-2008");
        metadatums2.add(edition2);
        metadatums2.add(edition3);
        metadatums2.add(date2);
        metadatums2.add(type2);
        metadatums2.add(ispartof2);
        metadatums2.add(ispartofseries2);
        metadatums2.add(title2);
        metadatums2.add(issn2);
        metadatums2.add(doi2);
        metadatums2.add(description2);
        metadatums2.add(iso2);
        metadatums2.add(author3);
        metadatums2.add(author4);
        metadatums2.add(author5);
        metadatums2.add(author6);
        metadatums2.add(author7);
        metadatums2.add(author8);
        metadatums2.add(subject18);
        metadatums2.add(subject19);
        metadatums2.add(subject20);
        metadatums2.add(subject21);
        metadatums2.add(subject22);
        metadatums2.add(subject23);
        metadatums2.add(subject24);
        metadatums2.add(subject25);
        metadatums2.add(subject26);
        metadatums2.add(subject27);
        metadatums2.add(subject28);
        metadatums2.add(other2);
        metadatums2.add(rid);
        metadatums2.add(rid2);
        ImportRecord secondRecord = new ImportRecord(metadatums2);

        records.add(firstrRecord);
        records.add(secondRecord);
        return records;
    }

}