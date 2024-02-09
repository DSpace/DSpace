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
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.pubmed.service.PubmedImportMetadataSourceServiceImpl;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link PubmedImportMetadataSourceServiceImpl}
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class PubmedImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

    @Autowired
    private PubmedImportMetadataSourceServiceImpl pubmedImportMetadataServiceImpl;

    @Autowired
    private LiveImportClientImpl liveImportClientImpl;

    @Test
    public void pubmedImportMetadataGetRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream fetchFile = getClass().getResourceAsStream("pubmedimport-fetch-test.xml");
                InputStream searchFile = getClass().getResourceAsStream("pubmedimport-search-test.xml")) {
            liveImportClientImpl.setHttpClient(httpClient);

            CloseableHttpResponse fetchResponse = mockResponse(
                    IOUtils.toString(fetchFile, Charset.defaultCharset()), 200, "OK");
            CloseableHttpResponse searchResponse = mockResponse(
                    IOUtils.toString(searchFile, Charset.defaultCharset()), 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(fetchResponse).thenReturn(searchResponse);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords();
            Collection<ImportRecord> recordsImported = pubmedImportMetadataServiceImpl.getRecords("test query", 0, 1);
            assertEquals(1, recordsImported.size());
            matchRecords(new ArrayList<ImportRecord>(recordsImported), collection2match);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void pubmedImportMetadataGetRecords2Test() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream fetchFile = getClass().getResourceAsStream("pubmedimport-fetch-test2.xml");
                InputStream searchFile = getClass().getResourceAsStream("pubmedimport-search-test2.xml")) {
            liveImportClientImpl.setHttpClient(httpClient);

            CloseableHttpResponse fetchResponse = mockResponse(
                    IOUtils.toString(fetchFile, Charset.defaultCharset()), 200, "OK");
            CloseableHttpResponse searchResponse = mockResponse(
                    IOUtils.toString(searchFile, Charset.defaultCharset()), 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(fetchResponse).thenReturn(searchResponse);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords2();
            Collection<ImportRecord> recordsImported = pubmedImportMetadataServiceImpl.getRecords("test query", 0, 1);
            assertEquals(1, recordsImported.size());
            matchRecords(new ArrayList<ImportRecord>(recordsImported), collection2match);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    private ArrayList<ImportRecord> getRecords() {
        ArrayList<ImportRecord> records = new ArrayList<>();
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        //define first record
        MetadatumDTO title = createMetadatumDTO("dc","title", null,
                "Teaching strategies of clinical reasoning in advanced nursing clinical practice: A scoping review.");
        MetadatumDTO description1 = createMetadatumDTO("dc", "description", "abstract", "To report and synthesize"
                + " the main strategies for teaching clinical reasoning described in the literature in the context of"
                + " advanced clinical practice and promote new areas of research to improve the pedagogical approach"
                + " to clinical reasoning in Advanced Practice Nursing.");
        MetadatumDTO description2 = createMetadatumDTO("dc", "description", "abstract", "Clinical reasoning and"
                + " clinical thinking are essential elements in the advanced nursing clinical practice decision-making"
                + " process. The quality improvement of care is related to the development of those skills."
                + " Therefore, it is crucial to optimize teaching strategies that can enhance the role of clinical"
                + " reasoning in advanced clinical practice.");
        MetadatumDTO description3 = createMetadatumDTO("dc", "description", "abstract", "A scoping review was"
                + " conducted using the framework developed by Arksey and O'Malley as a research strategy."
                + " Consistent with the nature of scoping reviews, a study protocol has been established.");
        MetadatumDTO description4 = createMetadatumDTO("dc", "description", "abstract", "The studies included and"
                + " analyzed in this scoping review cover from January 2016 to June 2022. Primary studies and secondary"
                + " revision studies, published in biomedical databases, were selected, including qualitative ones."
                + " Electronic databases used were: CINAHL, PubMed, Cochrane Library, Scopus, and OVID."
                + " Three authors independently evaluated the articles for titles, abstracts, and full text.");
        MetadatumDTO description5 = createMetadatumDTO("dc", "description", "abstract", "1433 articles were examined,"
                + " applying the eligibility and exclusion criteria 73 studies were assessed for eligibility,"
                + " and 27 were included in the scoping review. The results that emerged from the review were"
                + " interpreted and grouped into three macro strategies (simulations-based education, art and visual"
                + " thinking, and other learning approaches) and nineteen educational interventions.");
        MetadatumDTO description6 = createMetadatumDTO("dc", "description", "abstract", "Among the different"
                + " strategies, the simulations are the most used. Despite this, our scoping review reveals that is"
                + " necessary to use different teaching strategies to stimulate critical thinking, improve diagnostic"
                + " reasoning, refine clinical judgment, and strengthen decision-making. However, it is not possible to"
                + " demonstrate which methodology is more effective in obtaining the learning outcomes necessary to"
                + " acquire an adequate level of judgment and critical thinking. Therefore, it will be"
                + " necessary to relate teaching methodologies with the skills developed.");
        MetadatumDTO identifierOther = createMetadatumDTO("dc", "identifier", "other", "36708638");
        MetadatumDTO author1 = createMetadatumDTO("dc", "contributor", "author", "Giuffrida, Silvia");
        MetadatumDTO author2 = createMetadatumDTO("dc", "contributor", "author", "Silano, Verdiana");
        MetadatumDTO author3 = createMetadatumDTO("dc", "contributor", "author", "Ramacciati, Nicola");
        MetadatumDTO author4 = createMetadatumDTO("dc", "contributor", "author", "Prandi, Cesarina");
        MetadatumDTO author5 = createMetadatumDTO("dc", "contributor", "author", "Baldon, Alessia");
        MetadatumDTO author6 = createMetadatumDTO("dc", "contributor", "author", "Bianchi, Monica");
        MetadatumDTO date = createMetadatumDTO("dc", "date", "issued", "2023-02");
        MetadatumDTO language = createMetadatumDTO("dc", "language", "iso", "en");
        MetadatumDTO subject1 = createMetadatumDTO("dc", "subject", null, "Advanced practice nursing");
        MetadatumDTO subject2 = createMetadatumDTO("dc", "subject", null, "Clinical reasoning");
        MetadatumDTO subject3 = createMetadatumDTO("dc", "subject", null, "Critical thinking");
        MetadatumDTO subject4 = createMetadatumDTO("dc", "subject", null, "Educational strategies");
        MetadatumDTO subject5 = createMetadatumDTO("dc", "subject", null, "Nursing education");
        MetadatumDTO subject6 = createMetadatumDTO("dc", "subject", null, "Teaching methodology");

        metadatums.add(title);
        metadatums.add(description1);
        metadatums.add(description2);
        metadatums.add(description3);
        metadatums.add(description4);
        metadatums.add(description5);
        metadatums.add(description6);
        metadatums.add(identifierOther);
        metadatums.add(author1);
        metadatums.add(author2);
        metadatums.add(author3);
        metadatums.add(author4);
        metadatums.add(author5);
        metadatums.add(author6);
        metadatums.add(date);
        metadatums.add(language);
        metadatums.add(subject1);
        metadatums.add(subject2);
        metadatums.add(subject3);
        metadatums.add(subject4);
        metadatums.add(subject5);
        metadatums.add(subject6);
        ImportRecord record = new ImportRecord(metadatums);

        records.add(record);
        return records;
    }

    private ArrayList<ImportRecord> getRecords2() {
        ArrayList<ImportRecord> records = new ArrayList<>();
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        //define first record
        MetadatumDTO title = createMetadatumDTO("dc","title", null, "Searching NCBI Databases Using Entrez.");
        MetadatumDTO description = createMetadatumDTO("dc", "description", "abstract", "One of the most widely"
                + " used interfaces for the retrieval of information from biological databases is the NCBI Entrez"
                + " system. Entrez capitalizes on the fact that there are pre-existing, logical relationships between"
                + " the individual entries found in numerous public databases. The existence of such natural"
                + " connections, mostly biological in nature, argued for the development of a method through which"
                + " all the information about a particular biological entity could be found without having to"
                + " sequentially visit and query disparate databases. Two basic protocols describe simple, text-based"
                + " searches, illustrating the types of information that can be retrieved through the Entrez system."
                + " An alternate protocol builds upon the first basic protocol, using additional,"
                + " built-in features of the Entrez system, and providing alternative ways to issue the initial query."
                + " The support protocol reviews how to save frequently issued queries. Finally, Cn3D, a structure"
                + " visualization tool, is also discussed.");
        MetadatumDTO identifierOther = createMetadatumDTO("dc", "identifier", "other", "21975942");
        MetadatumDTO author1 = createMetadatumDTO("dc", "contributor", "author", "Gibney, Gretchen");
        MetadatumDTO author2 = createMetadatumDTO("dc", "contributor", "author", "Baxevanis, Andreas D");
        MetadatumDTO date = createMetadatumDTO("dc", "date", "issued", "2011-10");
        MetadatumDTO language = createMetadatumDTO("dc", "language", "iso", "en");

        metadatums.add(title);
        metadatums.add(description);
        metadatums.add(identifierOther);
        metadatums.add(author1);
        metadatums.add(author2);
        metadatums.add(date);
        metadatums.add(language);
        ImportRecord record = new ImportRecord(metadatums);

        records.add(record);
        return records;
    }

}