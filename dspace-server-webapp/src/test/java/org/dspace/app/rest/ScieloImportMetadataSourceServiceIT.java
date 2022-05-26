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
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.scielo.service.ScieloImportMetadataSourceServiceImpl;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link ScieloImportMetadataSourceServiceImpl}
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class ScieloImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

    @Autowired
    private LiveImportClientImpl liveImportClientImpl;

    @Autowired
    private ScieloImportMetadataSourceServiceImpl scieloServiceImpl;

    @Test
    public void scieloImportMetadataGetRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream scieloResp = getClass().getResourceAsStream("scielo-test.txt")) {

            String scieloRipResp = IOUtils.toString(scieloResp, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(scieloRipResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords();
            Collection<ImportRecord> recordsImported = scieloServiceImpl.getRecords("test query", 0, 2);
            assertEquals(2, recordsImported.size());
            matchRecords(new ArrayList<ImportRecord>(recordsImported), collection2match);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void scieloImportMetadataGetRecordsCountTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream file = getClass().getResourceAsStream("scielo-test.txt")) {
            String scieloResp = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(scieloResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            int tot = scieloServiceImpl.getRecordsCount("test query");
            assertEquals(2, tot);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test(expected = MethodNotFoundException.class)
    public void scieloImportMetadataFindMatchingRecordsTest() throws Exception {
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
        scieloServiceImpl.findMatchingRecords(testItem);
    }

    @Test(expected = MethodNotFoundException.class)
    public void scieloImportMetadataGetRecordsCountByQueryTest() throws Exception {
        Query q = new Query();
        q.addParameter("query", "test query");
        scieloServiceImpl.getRecordsCount(q);
    }

    @Test
    public void scieloImportMetadataGetRecordsByIdTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream scieloResp = getClass().getResourceAsStream("scielo-single-record.txt")) {

            String scieloRipResp = IOUtils.toString(scieloResp, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(scieloRipResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords();
            collection2match.remove(1);
            ImportRecord record = scieloServiceImpl.getRecord("S0185-30582021000200231-mex");
            assertNotNull(record);
            Collection<ImportRecord> recordsImported = Arrays.asList(record);
            matchRecords(new ArrayList<ImportRecord>(recordsImported), collection2match);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    private ArrayList<ImportRecord> getRecords() {
        ArrayList<ImportRecord> records = new ArrayList<>();
        //define first record
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        MetadatumDTO ispartof = createMetadatumDTO("dc", "relation", "ispartof", "Nova tellus");
        MetadatumDTO date = createMetadatumDTO("dc", "date", "issued", "2021");
        MetadatumDTO citation = createMetadatumDTO("oaire", "citation", "issue", "2");
        MetadatumDTO doi = createMetadatumDTO("dc", "identifier", "doi", "10.19130/iifl.nt.2021.39.2.901");
        MetadatumDTO endPage = createMetadatumDTO("oaire", "citation", "endPage", "236");
        MetadatumDTO subject = createMetadatumDTO("dc", "subject", null, "Roma");
        MetadatumDTO subject2 = createMetadatumDTO("dc", "subject", null, "Historia");
        MetadatumDTO subject3 = createMetadatumDTO("dc", "subject", null, "ritos funerarios");
        MetadatumDTO subject4 = createMetadatumDTO("dc", "subject", null, "inframundo");
        MetadatumDTO subject5 = createMetadatumDTO("dc", "subject", null, "epitafios");
        MetadatumDTO author = createMetadatumDTO("dc", "contributor", "author", "Torres Marzo, Ricardo");
        MetadatumDTO title = createMetadatumDTO("dc", "title", null, "Requena Jiménez, Miguel, Los espacios"
                                       + " de la muerte en Roma, Madrid, Síntesis, 2021, 365 págs."
                                       + " más bibliografía en línea, ISBN 978-84-135759-6-4.");
        MetadatumDTO volume = createMetadatumDTO("oaire", "citation", "volume", "39");
        MetadatumDTO issn = createMetadatumDTO("dc", "identifier", "issn", "0185-3058");
        MetadatumDTO other = createMetadatumDTO("dc", "identifier", "other", "S0185-30582021000200231-mex");
        MetadatumDTO startPage = createMetadatumDTO("oaire", "citation", "startPage", "231");

        metadatums.add(ispartof);
        metadatums.add(date);
        metadatums.add(citation);
        metadatums.add(doi);
        metadatums.add(endPage);
        metadatums.add(subject);
        metadatums.add(subject2);
        metadatums.add(subject3);
        metadatums.add(subject4);
        metadatums.add(subject5);
        metadatums.add(author);
        metadatums.add(title);
        metadatums.add(volume);
        metadatums.add(issn);
        metadatums.add(other);
        metadatums.add(startPage);

        ImportRecord firstrRecord = new ImportRecord(metadatums);

        //define second record
        List<MetadatumDTO> metadatums2  = new ArrayList<MetadatumDTO>();
        MetadatumDTO ispartof2 = createMetadatumDTO("dc", "relation", "ispartof", "Revista de Derecho Privado");
        MetadatumDTO date2 = createMetadatumDTO("dc", "date", "issued", "2021");
        MetadatumDTO citation2 = createMetadatumDTO("oaire", "citation", "issue", "41");
        MetadatumDTO doi2 = createMetadatumDTO("dc", "identifier", "doi", "10.18601/01234366.n41.14");
        MetadatumDTO endPage2 = createMetadatumDTO("oaire", "citation", "endPage", "418");
        MetadatumDTO subject6 = createMetadatumDTO("dc", "subject", null, "sopravvenienza contrattuale");
        MetadatumDTO subject7 = createMetadatumDTO("dc", "subject", null, "covro");
        MetadatumDTO subject8 = createMetadatumDTO("dc", "subject", null, "buona fede in senso oggettivo");
        MetadatumDTO subject9 = createMetadatumDTO("dc", "subject", null, "obbligo di rinegoziare");
        MetadatumDTO subject10 = createMetadatumDTO("dc", "subject", null, "revisione del contratto");
        MetadatumDTO author2 = createMetadatumDTO("dc", "contributor", "author", "MAGRI, GEO");
        MetadatumDTO title2 = createMetadatumDTO("dc", "title", null,
                "Rinegoziazione e revisione del contratto. Tribunale di Roma, Sez. VI, 27 agosto 2020");
        MetadatumDTO issn2 = createMetadatumDTO("dc", "identifier", "issn", "0123-4366");
        MetadatumDTO other2 = createMetadatumDTO("dc", "identifier", "other", "S0123-43662021000200397-col");
        MetadatumDTO startPage2 = createMetadatumDTO("oaire", "citation", "startPage", "397");
        MetadatumDTO description = createMetadatumDTO("dc", "description", "abstract",
                "ABSTRACT: The Tribunal of Rome imposes an obligation to renegotiate long-term contracts,"
              + " the balance of which has been modified by the covro pandemic. The decision establishes a"
              + " general obligation for the parties to execute the contract in good faith and gives the judge"
              + " the possibility of a judicial review. This is a long-awaited decision in doctrine which complies"
              + " with the indications of the Supreme Court of Cassation expressed in its memorandum 56/2020.");

        metadatums2.add(ispartof2);
        metadatums2.add(date2);
        metadatums2.add(citation2);
        metadatums2.add(doi2);
        metadatums2.add(endPage2);
        metadatums2.add(subject6);
        metadatums2.add(subject7);
        metadatums2.add(subject8);
        metadatums2.add(subject9);
        metadatums2.add(subject10);
        metadatums2.add(author2);
        metadatums2.add(title2);
        metadatums2.add(issn2);
        metadatums2.add(other2);
        metadatums2.add(startPage2);
        metadatums2.add(description);

        ImportRecord secondRecord = new ImportRecord(metadatums2);
        records.add(firstrRecord);
        records.add(secondRecord);
        return records;
    }

}