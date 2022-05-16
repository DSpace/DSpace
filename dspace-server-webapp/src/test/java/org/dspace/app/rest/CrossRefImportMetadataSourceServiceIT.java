/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.el.MethodNotFoundException;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Item;
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
        try (InputStream crossRefResp = getClass().getResourceAsStream("crossRef-test.json")) {

            String crossRefRespXmlResp = IOUtils.toString(crossRefResp, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(crossRefRespXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords();
            Collection<ImportRecord> recordsImported = crossRefServiceImpl.getRecords("test query", 0, 2);
            assertEquals(2, recordsImported.size());
            matchRecords(new ArrayList<ImportRecord>(recordsImported), collection2match);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void crossRefImportMetadataGetRecordsCountTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream crossRefResp = getClass().getResourceAsStream("crossRef-test.json")) {
            String crossRefRespXmlResp = IOUtils.toString(crossRefResp, Charset.defaultCharset());

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

    @Test
    public void crossRefImportMetadataGetRecordByIdTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream crossRefResp = getClass().getResourceAsStream("crossRef-by-id.json")) {

            String crossRefRespXmlResp = IOUtils.toString(crossRefResp, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(crossRefRespXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords();
            collection2match.remove(1);
            ImportRecord recordImported = crossRefServiceImpl.getRecord("10.26693/jmbs01.02.184");
            assertNotNull(recordImported);
            Collection<ImportRecord> recordsImported = Arrays.asList(recordImported);
            matchRecords(new ArrayList<ImportRecord>(recordsImported), collection2match);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test(expected = MethodNotFoundException.class)
    public void crossRefImportMetadataFindMatchingRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        org.dspace.content.Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                                              .withName("Collection 1")
                                                              .build();

        Item testItem = ItemBuilder.createItem(context, col1)
                                   .withTitle("test item")
                                   .withIssueDate("2021")
                                   .build();

        context.restoreAuthSystemState();
        crossRefServiceImpl.findMatchingRecords(testItem);
    }

    private ArrayList<ImportRecord> getRecords() {
        ArrayList<ImportRecord> records = new ArrayList<>();
        //define first record
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        MetadatumDTO title = createMetadatumDTO("dc", "title", null,
                "State of Awareness of Freshers’ Groups Chortkiv State"
                + " Medical College of Prevention of Iodine Deficiency Diseases");
        MetadatumDTO author = createMetadatumDTO("dc", "contributor", "author", "L.V. Senyuk");
        MetadatumDTO type = createMetadatumDTO("dc", "type", null, "journal-article");
        MetadatumDTO date = createMetadatumDTO("dc", "date", "issued", "2016");
        MetadatumDTO ispartof = createMetadatumDTO("dc", "relation", "ispartof",
                                   "Ukraïnsʹkij žurnal medicini, bìologìï ta sportu");
        MetadatumDTO doi = createMetadatumDTO("dc", "identifier", "doi", "10.26693/jmbs01.02.184");
        MetadatumDTO issn = createMetadatumDTO("dc", "identifier", "issn", "2415-3060");
        MetadatumDTO volume = createMetadatumDTO("oaire", "citation", "volume", "1");
        MetadatumDTO issue = createMetadatumDTO("oaire", "citation", "issue", "2");

        metadatums.add(title);
        metadatums.add(author);
        metadatums.add(date);
        metadatums.add(type);
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
        MetadatumDTO author2 = createMetadatumDTO("dc", "contributor", "author", "K. І. Kozak");
        MetadatumDTO type2 = createMetadatumDTO("dc", "type", null, "journal-article");
        MetadatumDTO date2 = createMetadatumDTO("dc", "date", "issued", "2016");
        MetadatumDTO ispartof2 = createMetadatumDTO("dc", "relation", "ispartof",
                                     "Ukraïnsʹkij žurnal medicini, bìologìï ta sportu");
        MetadatumDTO doi2 = createMetadatumDTO("dc", "identifier", "doi", "10.26693/jmbs01.02.105");
        MetadatumDTO issn2 = createMetadatumDTO("dc", "identifier", "issn", "2415-3060");
        MetadatumDTO volume2 = createMetadatumDTO("oaire", "citation", "volume", "1");
        MetadatumDTO issue2 = createMetadatumDTO("oaire", "citation", "issue", "2");

        metadatums2.add(title2);
        metadatums2.add(author2);
        metadatums2.add(date2);
        metadatums2.add(type2);
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