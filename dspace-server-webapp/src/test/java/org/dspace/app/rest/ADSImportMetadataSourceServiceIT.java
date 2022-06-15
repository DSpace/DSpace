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
import org.dspace.importer.external.ads.ADSImportMetadataSourceServiceImpl;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link ADSImportMetadataSourceServiceImpl}
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class ADSImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

    @Autowired
    private LiveImportClientImpl liveImportClient;

    @Autowired
    private ADSImportMetadataSourceServiceImpl adsServiceImpl;

    @Test
    public void adsImportMetadataGetRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClient.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream file = getClass().getResourceAsStream("ads-ex.json")) {

            String adsJsonResp = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClient.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(adsJsonResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords();
            Collection<ImportRecord> recordsImported = adsServiceImpl.getRecords("test query", 0, 2);
            assertEquals(2, recordsImported.size());
            matchRecords(new ArrayList<ImportRecord>(recordsImported), collection2match);
        } finally {
            liveImportClient.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void adsImportMetadataGetRecordsCountTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClient.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream file = getClass().getResourceAsStream("ads-ex.json")) {
            String adsResp = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClient.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(adsResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            int tot = adsServiceImpl.getRecordsCount("test query");
            assertEquals(9383, tot);
        } finally {
            liveImportClient.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void adsImportMetadataGetRecordsCountByQueryTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClient.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream file = getClass().getResourceAsStream("ads-ex.json")) {
            String adsResp = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClient.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(adsResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            Query q = new Query();
            q.addParameter("query", "test");
            int tot = adsServiceImpl.getRecordsCount(q);
            assertEquals(9383, tot);
        } finally {
            liveImportClient.setHttpClient(originalHttpClient);
        }
    }

    @Test(expected = MethodNotFoundException.class)
    public void adsImportMetadataFindMatchingRecordsTest() throws Exception {
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
        adsServiceImpl.findMatchingRecords(testItem);
    }

    @Test
    public void adsImportMetadataGetRecordByIdTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClient.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream file = getClass().getResourceAsStream("ads-single-obj.json")) {

            String adsJsonResp = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClient.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(adsJsonResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords();
            collection2match.remove(1);
            ImportRecord recordImported = adsServiceImpl.getRecord("2017PhRvL.119p1101A");
            assertNotNull(recordImported);
            matchRecords(new ArrayList<ImportRecord>(Arrays.asList(recordImported)), collection2match);
        } finally {
            liveImportClient.setHttpClient(originalHttpClient);
        }
    }

    private ArrayList<ImportRecord> getRecords() {
        ArrayList<ImportRecord> records = new ArrayList<>();
        //define first record
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        MetadatumDTO author = createMetadatumDTO("dc", "contributor", "author", "Abbott, B. P.");
        MetadatumDTO author2 = createMetadatumDTO("dc", "contributor", "author", "Babak, S.");
        MetadatumDTO author3 = createMetadatumDTO("dc", "contributor", "author", "Di Fiore, L.");
        MetadatumDTO author4 = createMetadatumDTO("dc", "contributor", "author", "Virgo Collaboration");
        MetadatumDTO doi = createMetadatumDTO("dc", "identifier", "doi", "10.1103/PhysRevLett.116.061102");
        MetadatumDTO type = createMetadatumDTO("dc", "type", null, "article");
        MetadatumDTO adsbibcode = createMetadatumDTO("dc", "identifier", "other", "2016PhRvL.116f1102A");
        MetadatumDTO date = createMetadatumDTO("dc", "date", "issued", "2016");
        MetadatumDTO subject = createMetadatumDTO("dc", "subject", null, "General Relativity and Quantum Cosmology");
        MetadatumDTO subject2 = createMetadatumDTO("dc", "subject", null,
                "Astrophysics - High Energy Astrophysical Phenomena");
        MetadatumDTO source = createMetadatumDTO("dc", "source", null, "Physical Review Letters");
        MetadatumDTO title = createMetadatumDTO("dc", "title", null,
                "Observation of Gravitational Waves from a Binary Black Hole Merger");
        MetadatumDTO description = createMetadatumDTO("dc", "description", "abstract",
                "On September 14, 2015 at 09:50:45 UTC the two detectors of the Laser"
              + " Interferometer Gravitational-Wave Observatory simultaneously observed"
              + " a transient gravitational-wave signal. The signal sweeps upwards in frequency"
              + " from 35 to 250 Hz with a peak gravitational-wave strain of 1.0 ×10<SUP>-21</SUP>."
              + " It matches the waveform predicted by general relativity for the inspiral and merger"
              + " of a pair of black holes and the ringdown of the resulting single black hole."
              + " The signal was observed with a matched-filter signal-to-noise ratio of 24 and a false"
              + " alarm rate estimated to be less than 1 event per 203 000 years, equivalent to a significance"
              + " greater than 5.1 σ . The source lies at a luminosity distance of 41 0<SUB>-180</SUB><SUP>+160</SUP>"
              + " Mpc corresponding to a redshift z =0.0 9<SUB>-0.04</SUB><SUP>+0.03</SUP> ."
              + " In the source frame, the initial black hole masses are 3 6<SUB>-4</SUB><SUP>+5</SUP>M<SUB>⊙</SUB>"
              + " and 2 9<SUB>-4</SUB><SUP>+4</SUP>M<SUB>⊙</SUB> , and the final black hole mass is"
              + " 6 2<SUB>-4</SUB><SUP>+4</SUP>M<SUB>⊙</SUB> , with 3. 0<SUB>-0.5</SUB><SUP>+0.5</SUP>M<SUB>⊙</SUB>"
              + " c<SUP>2</SUP> radiated in gravitational waves. All uncertainties define 90% credible intervals."
              + " These observations demonstrate the existence of binary stellar-mass black hole systems."
              + " This is the first direct detection of gravitational waves and the first observation of a binary"
              + " black hole merger.");

        metadatums.add(description);
        metadatums.add(author);
        metadatums.add(author2);
        metadatums.add(author3);
        metadatums.add(author4);
        metadatums.add(doi);
        metadatums.add(type);
        metadatums.add(adsbibcode);
        metadatums.add(date);
        metadatums.add(subject);
        metadatums.add(subject2);
        metadatums.add(source);
        metadatums.add(title);

        ImportRecord firstrRecord = new ImportRecord(metadatums);

        //define second record
        List<MetadatumDTO> metadatums2  = new ArrayList<MetadatumDTO>();
        MetadatumDTO author5 = createMetadatumDTO("dc", "contributor", "author", "Abbott, B. P.");
        MetadatumDTO author6 = createMetadatumDTO("dc", "contributor", "author", "Babak, S.");
        MetadatumDTO author7 = createMetadatumDTO("dc", "contributor", "author", "Vorvick, C.");
        MetadatumDTO author8 = createMetadatumDTO("dc", "contributor", "author", "Wade, M.");
        MetadatumDTO doi2 = createMetadatumDTO("dc", "identifier", "doi", "10.1103/PhysRevLett.119.161101");
        MetadatumDTO type2 = createMetadatumDTO("dc", "type", null, "article");
        MetadatumDTO adsbibcode2 = createMetadatumDTO("dc", "identifier", "other", "2017PhRvL.119p1101A");
        MetadatumDTO date2 = createMetadatumDTO("dc", "date", "issued", "2017");
        MetadatumDTO subject3 = createMetadatumDTO("dc", "subject", null, "General Relativity and Quantum Cosmology");
        MetadatumDTO subject4 = createMetadatumDTO("dc", "subject", null,
                "Astrophysics - High Energy Astrophysical Phenomena");
        MetadatumDTO source2 = createMetadatumDTO("dc", "source", null, "Physical Review Letters");
        MetadatumDTO title2 = createMetadatumDTO("dc", "title", null,
                "GW170817: Observation of Gravitational Waves from a Binary Neutron Star Inspiral");
        MetadatumDTO description2 = createMetadatumDTO("dc", "description", "abstract",
                "On August 17, 2017 at 12∶41:04 UTC the Advanced LIGO and Advanced Virgo"
              + " gravitational-wave detectors made their first observation of a binary neutron star inspiral."
              + " The signal, GW170817, was detected with a combined signal-to-noise ratio of 32.4 and a"
              + " false-alarm-rate estimate of less than one per 8.0 ×10<SUP>4</SUP> years ."
              + " We infer the component masses of the binary to be between 0.86 and 2.26 M<SUB>⊙</SUB> ,"
              + " in agreement with masses of known neutron stars. Restricting the component spins to the"
              + " range inferred in binary neutron stars, we find the component masses to be in the"
              + " range 1.17 - 1.60 M<SUB>⊙</SUB> , with the total mass of the system 2.7"
              + " 4<SUB>-0.01</SUB><SUP>+0.04</SUP>M<SUB>⊙</SUB> . The source was localized within a sky region"
              + " of 28 deg<SUP>2</SUP> (90% probability) and had a luminosity distance of 4"
              + " 0<SUB>-14</SUB><SUP>+8</SUP> Mpc , the closest and most precisely localized"
              + " gravitational-wave signal yet. The association with the γ -ray burst GRB 170817A,"
              + " detected by Fermi-GBM 1.7 s after the coalescence, corroborates the hypothesis of a neutron"
              + " star merger and provides the first direct evidence of a link between these mergers and"
              + " short γ -ray bursts. Subsequent identification of transient counterparts across the"
              + " electromagnetic spectrum in the same location further supports the interpretation of"
              + " this event as a neutron star merger. This unprecedented joint gravitational and"
              + " electromagnetic observation provides insight into astrophysics, dense matter,"
              + " gravitation, and cosmology.");

        metadatums2.add(description2);
        metadatums2.add(author5);
        metadatums2.add(author6);
        metadatums2.add(author7);
        metadatums2.add(author8);
        metadatums2.add(doi2);
        metadatums2.add(type2);
        metadatums2.add(adsbibcode2);
        metadatums2.add(date2);
        metadatums2.add(subject3);
        metadatums2.add(subject4);
        metadatums2.add(source2);
        metadatums2.add(title2);

        ImportRecord secondRecord = new ImportRecord(metadatums2);
        records.add(firstrRecord);
        records.add(secondRecord);
        return records;
    }

}