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
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
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

    //@Autowired
    private DataCiteImportMetadataSourceServiceImpl dataCiteServiceImpl;

    @Before
    public void setup() throws Exception {
        ServiceManager serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();
        dataCiteServiceImpl = serviceManager.getServiceByName("DataCiteImportService",
            DataCiteImportMetadataSourceServiceImpl.class);
    }

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
    public void dataCiteImportMetadataGetRecordsTest2() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream dataCiteResp = getClass().getResourceAsStream("dataCite-test1.json")) {
            String dataCiteRespXmlResp = IOUtils.toString(dataCiteResp, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(dataCiteRespXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords2();
            Collection<ImportRecord> recordsImported = dataCiteServiceImpl.getRecords("10.5446/57552",
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

    @Test(expected = UnsupportedOperationException.class)
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
        MetadatumDTO dateissued = createMetadatumDTO("dc", "date", "issued", "2022");
        MetadatumDTO publisher = createMetadatumDTO("dc", "publisher", null, "arXiv");
        MetadatumDTO type = createMetadatumDTO("dc", "type", null, "Working Paper");
        MetadatumDTO abstrct = createMetadatumDTO("dc", "description", "abstract", "A proof is one of the most" +
            " important concepts of mathematics. However, there is a striking difference between how a proof " +
            "is defined in theory and how it " +
            "is used in practice. This puts the unique status of mathematics as exact science into peril. " +
            "Now may be the time to reconcile theory and practice, i.e. precision and intuition, through the" +
            " advent of computer proof assistants. For the most time this has been a topic for experts in specialized" +
            " communities. However, mathematical proofs have become increasingly sophisticated, stretching the" +
            " boundaries of what is humanly comprehensible, so that leading mathematicians have asked for formal" +
            " verification of their proofs. At the same time, major theorems in mathematics have recently been" +
            " computer-verified by people from outside of these communities, even by beginning students. This" +
            " article investigates the gap between the different definitions of a proof and possibilities to" +
            " build bridges. It is written as a polemic or a collage by different members of the communities in" +
            " mathematics and computer science at different stages of their careers, challenging well-known" +
            " preconceptions and exploring new perspectives.");
        MetadatumDTO description = createMetadatumDTO("dc", "description", null, "17 pages, 1 figure");
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
        metadatums.add(dateissued);
        metadatums.add(publisher);
        metadatums.add(type);
        metadatums.add(abstrct);
        metadatums.add(description);

        ImportRecord firstRecord = new ImportRecord(metadatums);

        records.add(firstRecord);
        return records;
    }

    private ArrayList<ImportRecord> getRecords2() {
        ArrayList<ImportRecord> records = new ArrayList<>();
        //define first record
        List<MetadatumDTO> metadatums  = new ArrayList<>();
        MetadatumDTO title = createMetadatumDTO("dc", "title", null,
            "Der Professorinnen- und Professorenkatalog der Universität Bamberg");
        MetadatumDTO doi = createMetadatumDTO("dc", "identifier", "doi", "10.5446/57552");
        MetadatumDTO author1 = createMetadatumDTO("dc", "contributor", "author", "Prussat, Margrit");
        MetadatumDTO author2 = createMetadatumDTO("dc", "contributor", "author", "Gantner, Florian");
        MetadatumDTO dateissued = createMetadatumDTO("dc", "date", "issued", "2022");
        MetadatumDTO publisher = createMetadatumDTO("dc", "publisher", null, "The Library Code GmbH");
        MetadatumDTO type = createMetadatumDTO("dc", "type", null, "Video");
        MetadatumDTO language =  createMetadatumDTO("dc", "language", "iso", "de");
        MetadatumDTO abstrct = createMetadatumDTO("dc", "description", "abstract", "Dr. Margrit Prussat" +
            " und Florian Gantner stellen auf dem DSpace Anwendertreffen 2021 den mit DSpace betriebenen" +
            " Professorinnen- und Professorenkatalog der Universität Bamberg vor.");
        metadatums.add(title);
        metadatums.add(doi);
        metadatums.add(author1);
        metadatums.add(author2);
        metadatums.add(dateissued);
        metadatums.add(publisher);
        metadatums.add(type);
        metadatums.add(abstrct);
        metadatums.add(language);

        ImportRecord firstRecord = new ImportRecord(metadatums);

        records.add(firstRecord);
        return records;
    }

    @Test
    public void dataCiteImportMetadataNoResultsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream dataciteResp = getClass().getResourceAsStream("dataCite-noResults.json")) {
            String dataciteTextResp = IOUtils.toString(dataciteResp, Charset.defaultCharset());
            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(dataciteTextResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);
            context.restoreAuthSystemState();
            int tot = dataCiteServiceImpl.getRecordsCount("nocontent");
            assertEquals(0, tot);
            Collection<ImportRecord> importRecords  = dataCiteServiceImpl.getRecords("nocontent", 0 , -1);
            assertEquals(0, importRecords.size());
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }
}
