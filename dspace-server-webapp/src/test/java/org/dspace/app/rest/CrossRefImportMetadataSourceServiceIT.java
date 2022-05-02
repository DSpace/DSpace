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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.importer.external.crossref.CrossRefImportMetadataSourceServiceImpl;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link CrossRefImportMetadataSourceServiceImpl}
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class CrossRefImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

    @Autowired
    private LiveImportClientImpl liveImportClientImpl;

    @Autowired
    private CrossRefImportMetadataSourceServiceImpl crossRefServiceImpl;

    @Test
    public void crossRefImportMetadataGetRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        String path = testProps.get("test.crossRef").toString();
        try (FileInputStream crossRefResp = new FileInputStream(path)) {

            String crossRefRespXmlResp = IOUtils.toString(crossRefResp, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(crossRefRespXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            Collection<ImportRecord> collection2match = getRecords();
            Collection<ImportRecord> recordsImported = crossRefServiceImpl.getRecords("test query", 0, 2);
            assertEquals(2, recordsImported.size());
            assertTrue(matchRecords(recordsImported, collection2match));
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void crossRefImportMetadataGetRecordsCountTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        String path = testProps.get("test.crossRef").toString();
        try (FileInputStream file = new FileInputStream(path)) {
            String crossRefRespXmlResp = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(crossRefRespXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            int tot = crossRefServiceImpl.getRecordsCount("test query");
            assertEquals(10, tot);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    private Collection<ImportRecord> getRecords() {
        Collection<ImportRecord> records = new LinkedList<ImportRecord>();
        //define first record
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        MetadatumDTO title = createMetadatumDTO("dc", "title", null,
                "State of Awareness of Freshers’ Groups Chortkiv State"
                + " Medical College of Prevention of Iodine Deficiency Diseases");
        MetadatumDTO type = createMetadatumDTO("dc", "type", null, "journal-article");
        MetadatumDTO date = createMetadatumDTO("dc", "date", "issued", "2016");
        MetadatumDTO ispartof = createMetadatumDTO("dc", "relation", "ispartof",
                                   "Ukraïnsʹkij žurnal medicini, bìologìï ta sportu");
        MetadatumDTO doi = createMetadatumDTO("dc", "identifier", "doi", "10.26693/jmbs01.02.184");
        MetadatumDTO issn = createMetadatumDTO("dc", "relation", "issn", "2415-3060");
        MetadatumDTO volume = createMetadatumDTO("oaire", "citation", "volume", "1");
        MetadatumDTO issue = createMetadatumDTO("oaire", "citation", "issue", "2");

        metadatums.add(title);
        metadatums.add(type);
        metadatums.add(date);
        metadatums.add(ispartof);
        metadatums.add(doi);
        metadatums.add(issn);
        metadatums.add(volume);
        metadatums.add(issue);

        ImportRecord firstrRecord = new ImportRecord(metadatums);

        //define second record
        List<MetadatumDTO> metadatums2  = new ArrayList<MetadatumDTO>();
        MetadatumDTO title2 = createMetadatumDTO("dc", "title", null,
                "Ischemic Heart Disease and Role of Nurse of Cardiology Department");
        MetadatumDTO type2 = createMetadatumDTO("dc", "type", null, "journal-article");
        MetadatumDTO date2 = createMetadatumDTO("dc", "date", "issued", "2016");
        MetadatumDTO ispartof2 = createMetadatumDTO("dc", "relation", "ispartof",
                                     "Ukraïnsʹkij žurnal medicini, bìologìï ta sportu");
        MetadatumDTO doi2 = createMetadatumDTO("dc", "identifier", "doi", "10.26693/jmbs01.02.105");
        MetadatumDTO issn2 = createMetadatumDTO("dc", "relation", "issn", "2415-3060");
        MetadatumDTO volume2 = createMetadatumDTO("oaire", "citation", "volume", "1");
        MetadatumDTO issue2 = createMetadatumDTO("oaire", "citation", "issue", "2");

        metadatums2.add(title2);
        metadatums2.add(type2);
        metadatums2.add(date2);
        metadatums2.add(ispartof2);
        metadatums2.add(doi2);
        metadatums2.add(issn2);
        metadatums2.add(volume2);
        metadatums2.add(issue2);

        ImportRecord secondRecord = new ImportRecord(metadatums2);
        records.add(firstrRecord);
        records.add(secondRecord);
        return records;
    }

}