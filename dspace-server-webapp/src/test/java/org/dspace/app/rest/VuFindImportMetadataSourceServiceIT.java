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
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.vufind.VuFindImportMetadataSourceServiceImpl;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link VuFindImportMetadataSourceServiceImpl}
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class VuFindImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

    @Autowired
    private LiveImportClientImpl liveImportClientImpl;

    @Autowired
    private VuFindImportMetadataSourceServiceImpl vuFindService;

    @Test
    public void vuFindImportMetadataGetRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream vuFindRespIS = getClass().getResourceAsStream("vuFind-generic.json")) {

            String vuFindResp = IOUtils.toString(vuFindRespIS, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(vuFindResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords();
            Collection<ImportRecord> recordsImported = vuFindService.getRecords("test query", 0, 2);
            assertEquals(2, recordsImported.size());
            matchRecords(new ArrayList<>(recordsImported), collection2match);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void vuFindImportMetadataGetRecordsCountTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream vuFindRespIS = getClass().getResourceAsStream("vuFind-generic.json")) {
            String vuFindResp = IOUtils.toString(vuFindRespIS, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(vuFindResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            int tot = vuFindService.getRecordsCount("test query");
            assertEquals(1994, tot);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void vuFindImportMetadataGetRecordByIdTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream vuFindByIdResp = getClass().getResourceAsStream("vuFind-by-id.json")) {

            String vuFindResp = IOUtils.toString(vuFindByIdResp, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(vuFindResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords();
            collection2match.remove(1);
            ImportRecord recordImported = vuFindService.getRecord("653510");
            assertNotNull(recordImported);
            Collection<ImportRecord> recordsImported = Arrays.asList(recordImported);
            matchRecords(new ArrayList<>(recordsImported), collection2match);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test(expected = MethodNotFoundException.class)
    public void vuFindImportMetadataFindMatchingRecordsTest() throws Exception {
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
        vuFindService.findMatchingRecords(testItem);
    }

    private ArrayList<ImportRecord> getRecords() {
        ArrayList<ImportRecord> records = new ArrayList<>();
        //define first record
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        MetadatumDTO identifierOther = createMetadatumDTO("dc", "identifier", "other", "653510");
        MetadatumDTO language = createMetadatumDTO("dc", "language", "iso", "Italian");
        MetadatumDTO title = createMetadatumDTO("dc", "title", null,
                "La pianta marmorea di Roma antica: Forma urbis Romae /");
        MetadatumDTO subject = createMetadatumDTO("dc", "subject", null, "Rome (Italy)");
        MetadatumDTO subject2 = createMetadatumDTO("dc", "subject", null, "Maps");
        MetadatumDTO subject3 = createMetadatumDTO("dc", "subject", null, "Early works to 1800.");
        MetadatumDTO subject4 = createMetadatumDTO("dc", "subject", null, "Rome (Italy)");
        MetadatumDTO subject5 = createMetadatumDTO("dc", "subject", null, "Antiquities");
        MetadatumDTO subject6 = createMetadatumDTO("dc", "subject", null, "Maps.");
        MetadatumDTO identifier = createMetadatumDTO("dc", "identifier", null,
                                                     "http://hdl.handle.net/20.500.12390/231");
        metadatums.add(identifierOther);
        metadatums.add(language);
        metadatums.add(title);
        metadatums.add(identifier);
        metadatums.add(subject);
        metadatums.add(subject2);
        metadatums.add(subject3);
        metadatums.add(subject4);
        metadatums.add(subject5);
        metadatums.add(subject6);

        ImportRecord firstrRecord = new ImportRecord(metadatums);

        //define second record
        List<MetadatumDTO> metadatums2  = new ArrayList<MetadatumDTO>();
        MetadatumDTO identifierOther2 = createMetadatumDTO("dc", "identifier", "other", "1665326");
        MetadatumDTO language2 = createMetadatumDTO("dc", "language", "iso", "English");
        MetadatumDTO title2 = createMetadatumDTO("dc", "title", null,
                "Expert frames : scientific and policy practices of Roma classification /");
        MetadatumDTO subject7 = createMetadatumDTO("dc", "subject", null, "Public opinion");
        MetadatumDTO subject8 = createMetadatumDTO("dc", "subject", null, "Europe.");
        MetadatumDTO subject9 = createMetadatumDTO("dc", "subject", null, "Stereotypes (Social psychology)");
        MetadatumDTO subject10 = createMetadatumDTO("dc", "subject", null, "Romanies");
        MetadatumDTO subject11 = createMetadatumDTO("dc", "subject", null, "Public opinion.");
        MetadatumDTO identifier2 = createMetadatumDTO("dc", "identifier", null,
                "http://ezproxy.villanova.edu/login?URL=http://www.jstor.org/stable/10.7829/j.ctt1ggjj08");
        metadatums2.add(identifierOther2);
        metadatums2.add(language2);
        metadatums2.add(title2);
        metadatums2.add(identifier2);
        metadatums2.add(subject7);
        metadatums2.add(subject8);
        metadatums2.add(subject9);
        metadatums2.add(subject10);
        metadatums2.add(subject11);

        ImportRecord secondRecord = new ImportRecord(metadatums2);
        records.add(firstrRecord);
        records.add(secondRecord);
        return records;
    }

}