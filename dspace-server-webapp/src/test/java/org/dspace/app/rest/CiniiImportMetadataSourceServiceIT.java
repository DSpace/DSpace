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
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.importer.external.cinii.CiniiImportMetadataSourceServiceImpl;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link CiniiImportMetadataSourceServiceImpl}
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class CiniiImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

    @Autowired
    private LiveImportClientImpl liveImportClientImpl;

    @Autowired
    private CiniiImportMetadataSourceServiceImpl ciniiServiceImpl;

    @Test
    public void ciniiImportMetadataGetRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        InputStream ciniiRefResp = null;
        InputStream ciniiRefResp2 = null;
        InputStream ciniiRefResp3 = null;
        try {
            ciniiRefResp = getClass().getResourceAsStream("cinii-responce-ids.xml");
            ciniiRefResp2 = getClass().getResourceAsStream("cinii-first.xml");
            ciniiRefResp3 = getClass().getResourceAsStream("cinii-second.xml");

            String ciniiIdsXmlResp = IOUtils.toString(ciniiRefResp, Charset.defaultCharset());
            String ciniiFirstXmlResp = IOUtils.toString(ciniiRefResp2, Charset.defaultCharset());
            String ciniiSecondXmlResp = IOUtils.toString(ciniiRefResp3, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(ciniiIdsXmlResp, 200, "OK");
            CloseableHttpResponse response2 = mockResponse(ciniiFirstXmlResp, 200, "OK");
            CloseableHttpResponse response3 = mockResponse(ciniiSecondXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response, response2, response3);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords();
            Collection<ImportRecord> recordsImported = ciniiServiceImpl.getRecords("test query", 0, 2);
            assertEquals(2, recordsImported.size());
            matchRecords(new ArrayList<ImportRecord>(recordsImported), collection2match);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
            if (Objects.nonNull(ciniiRefResp)) {
                ciniiRefResp.close();
            }
            if (Objects.nonNull(ciniiRefResp2)) {
                ciniiRefResp2.close();
            }
            if (Objects.nonNull(ciniiRefResp3)) {
                ciniiRefResp3.close();
            }
        }
    }

    @Test
    public void ciniiImportMetadataGetRecordsCountTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream file = getClass().getResourceAsStream("cinii-responce-ids.xml")) {
            String ciniiXmlResp = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(ciniiXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            int tot = ciniiServiceImpl.getRecordsCount("test query");
            assertEquals(32, tot);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    private ArrayList<ImportRecord> getRecords() {
        ArrayList<ImportRecord> records = new ArrayList<>();
        //define first record
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        MetadatumDTO title = createMetadatumDTO("dc", "title", null,
                "A Review of the Chinese Government Support and Sustainability"
                + " Assessment for Ecovillage Development with a Global Perspective");
        MetadatumDTO source = createMetadatumDTO("dc", "source", null,
                "International Review for Spatial Planning and Sustainable Development");
        MetadatumDTO date = createMetadatumDTO("dc", "date", "issued", "2022");
        MetadatumDTO language = createMetadatumDTO("dc", "language", "iso", "ENG");
        MetadatumDTO identifier = createMetadatumDTO("dc", "identifier", "other", "130008141851");
        MetadatumDTO description = createMetadatumDTO("dc", "description", "abstract",
                "<p>Having achieved substantial progress in urban development over the past three decades,"
                + " the Chinese government has turned to ecovillage development as one of the more effective"
                + " ways to solve increasingly serious rural issues, such as poverty, rural hollowing,"
                + " a deteriorating natural environment, and farmland abandonment. However, in spite of"
                + " various promotional policies and substantial financial investment, there are very few"
                + " studies assessing the impact of governmental support on ecovillage development."
                + " This paper presents a study applying both qualitative research and quantitative analysis"
                + " to compare the effects of the support, especially in funding and policies, on their development."
                + " A comparison was made of three cases, one in China and two elsewhere. To provide a common basis"
                + " for comparison, three quantification based assessments were examined with a view to applying"
                + " them in this study.<b> </b>These were the Evaluation for Construction of Beautiful Village"
                + " (ECBV) from China, and the BREEAM Community and LEED-ND, two well-established international"
                + " examples. The following analyses using the three methods reveal the strengths and weaknesses"
                + " of the Chinese government support in ecovillage development, and limitations of the"
                + " quantification-based assessment methods. Proposals are made for improving the nature of"
                + " government support and the use of the ECBV. These research outcomes can help formulate"
                + " the rural development policies in the critical time of socio-economic transition in China,"
                + " and the research process could be a reference to review quantification based assessment"
                + " methods in other developing countries with similar levels of development. </p>");

        metadatums.add(title);
        metadatums.add(source);
        metadatums.add(date);
        metadatums.add(language);
        metadatums.add(description);
        metadatums.add(identifier);

        ImportRecord firstrRecord = new ImportRecord(metadatums);

        //define second record
        List<MetadatumDTO> metadatums2  = new ArrayList<MetadatumDTO>();
        MetadatumDTO title2 = createMetadatumDTO("dc", "title", null,
                "Surface Electronic States and Inclining Surfaces in MoTe2 Probed by Photoemission Spectromicroscopy");
        MetadatumDTO source2 = createMetadatumDTO("dc", "source", null,
                "Journal of the Physical Society of Japan");
        MetadatumDTO date2 = createMetadatumDTO("dc", "date", "issued", "2021-08-15");
        MetadatumDTO issn = createMetadatumDTO("dc", "identifier", "issn", "0031-9015");
        MetadatumDTO identifier2 = createMetadatumDTO("dc", "identifier", "other", "210000159181");

        metadatums2.add(title2);
        metadatums2.add(issn);
        metadatums2.add(source2);
        metadatums2.add(date2);
        metadatums2.add(identifier2);

        ImportRecord secondRecord = new ImportRecord(metadatums2);
        records.add(firstrRecord);
        records.add(secondRecord);
        return records;
    }

}