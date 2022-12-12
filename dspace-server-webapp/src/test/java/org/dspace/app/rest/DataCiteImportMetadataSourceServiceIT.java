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
import javax.el.MethodNotFoundException;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Item;
import org.dspace.importer.external.datacite.DataCiteImportMetadataSourceServiceImpl;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Integration tests for {@link DataCiteImportMetadataSourceServiceImpl}
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class DataCiteImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

    @Autowired
    private LiveImportClientImpl liveImportClientImpl;

    @Autowired
    private DataCiteImportMetadataSourceServiceImpl dataCiteServiceImpl;

    @Test
    public void dataCiteImportMetadataGetRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream dataCiteResp = getClass().getResourceAsStream("dataCite-test.json")) {
            String dataCiteRespXmlResp = IOUtils.toString(dataCiteResp, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(dataCiteRespXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords();
            Collection<ImportRecord> recordsImported = dataCiteServiceImpl.getRecords("10.48550/arxiv.2207.04779",
                    0, -1);
            assertEquals(1, recordsImported.size());
            matchRecords(new ArrayList<>(recordsImported), collection2match);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void dataCiteImportMetadataGetRecordsCountTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream dataciteResp = getClass().getResourceAsStream("dataCite-test.json")) {
            String dataciteTextResp = IOUtils.toString(dataciteResp, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(dataciteTextResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            int tot = dataCiteServiceImpl.getRecordsCount("10.48550/arxiv.2207.04779");
            assertEquals(1, tot);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test(expected = MethodNotFoundException.class)
    public void dataCiteImportMetadataFindMatchingRecordsTest() throws Exception {
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
        dataCiteServiceImpl.findMatchingRecords(testItem);
    }

    private ArrayList<ImportRecord> getRecords() {
        ArrayList<ImportRecord> records = new ArrayList<>();
        //define first record
        List<MetadatumDTO> metadatums  = new ArrayList<>();
        MetadatumDTO title = createMetadatumDTO("dc", "title", null,
                "Mathematical Proof Between Generations");
        MetadatumDTO doi = createMetadatumDTO("dc", "identifier", "doi", "10.48550/arxiv.2207.04779");
        MetadatumDTO author1 = createMetadatumDTO("dc", "contributor", "author", "Bayer, Jonas");
        MetadatumDTO author2 = createMetadatumDTO("dc", "contributor", "author", "Benzmüller, Christoph");
        MetadatumDTO author3 = createMetadatumDTO("dc", "contributor", "author", "Buzzard, Kevin");
        MetadatumDTO author4 = createMetadatumDTO("dc", "contributor", "author", "David, Marco");
        MetadatumDTO author5 = createMetadatumDTO("dc", "contributor", "author", "Lamport, Leslie");
        MetadatumDTO author6 = createMetadatumDTO("dc", "contributor", "author", "Matiyasevich, Yuri");
        MetadatumDTO author7 = createMetadatumDTO("dc", "contributor", "author", "Paulson, Lawrence");
        MetadatumDTO author8 = createMetadatumDTO("dc", "contributor", "author", "Schleicher, Dierk");
        MetadatumDTO author9 = createMetadatumDTO("dc", "contributor", "author", "Stock, Benedikt");
        MetadatumDTO author10 = createMetadatumDTO("dc", "contributor", "author", "Zelmanov, Efim");
        metadatums.add(title);
        metadatums.add(doi);
        metadatums.add(author1);
        metadatums.add(author2);
        metadatums.add(author3);
        metadatums.add(author4);
        metadatums.add(author5);
        metadatums.add(author6);
        metadatums.add(author7);
        metadatums.add(author8);
        metadatums.add(author9);
        metadatums.add(author10);

        ImportRecord firstRecord = new ImportRecord(metadatums);

        records.add(firstRecord);
        return records;
    }

}
