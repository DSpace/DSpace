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
import org.dspace.importer.external.datacite.DataCiteProjectImportMetadataSourceServiceImpl;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Integration tests for {@link DataCiteProjectImportMetadataSourceServiceImpl}
 * General tests for the datacite api are covered in the {@link DataCiteImportMetadataSourceServiceIT}
 * 
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 */
public class DataCiteProjectImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest  {

    @Autowired
    private LiveImportClientImpl liveImportClientImpl;

    // @Autowired
    private DataCiteProjectImportMetadataSourceServiceImpl dataCiteProjectServiceImpl;

    @Test
    public void dataCiteProjectImportMetadataGetRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ServiceManager serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();
        dataCiteProjectServiceImpl = serviceManager.getServiceByName("DataCiteProjectImportService",
                DataCiteProjectImportMetadataSourceServiceImpl.class);
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream dataCiteResp = getClass().getResourceAsStream("dataCiteProject-test.json")) {
            String dataCiteRespXmlResp = IOUtils.toString(dataCiteResp, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(dataCiteRespXmlResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ArrayList<ImportRecord> collection2match = getRecords();
            Collection<ImportRecord> recordsImported = dataCiteProjectServiceImpl.getRecords("10.60872/ror",
                    0, -1);
            assertEquals(1, recordsImported.size());
            matchRecords(new ArrayList<>(recordsImported), collection2match);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    private ArrayList<ImportRecord> getRecords() {
        ArrayList<ImportRecord> records = new ArrayList<>();
        //define first record
        List<MetadatumDTO> metadatums  = new ArrayList<>();
        MetadatumDTO title = createMetadatumDTO("dc", "title", null,
                "Affiliations and Identifiers for Research Organizations (ROR)");
        MetadatumDTO title1 = createMetadatumDTO("dc", "title", null,
            "Identifying Organizations");
        MetadatumDTO projectidentifier = createMetadatumDTO("dc", "identifier", null, "10.60872/ror");
        MetadatumDTO contributor1 = createMetadatumDTO("project", "investigator", null,
            "Ted Habermann");
        MetadatumDTO description1 = createMetadatumDTO("dc", "description", null,
            "The Research Organization Registry (ROR) is a community-led project launched in January 2019 to " +
                "develop an open, sustainable, usable, and unique identifier for every research organization in the " +
                "world. Metadata Game Changers worked with Dryad in the first large-scale adoption of RORs by a " +
                "repository. We connected to papers related to Dryad datasets, found affiliations from Crossref and " +
                "other sources, searched the early ROR for identifiers, and added them to the Dryad metadata. Since " +
                "that time, we have been involved in re-curating repositories to add RORs and other kinds of " +
                "identifiers.");
        MetadatumDTO subject1 = createMetadatumDTO("dc", "subject", null, "ROR");
        MetadatumDTO subject2 = createMetadatumDTO("dc", "subject", null,
            "Research Organizations");
        MetadatumDTO subject3 = createMetadatumDTO("dc", "subject", null, "Identifiers");
        MetadatumDTO subject4 = createMetadatumDTO("dc", "subject", null, "Affiliations");
        MetadatumDTO subject5 = createMetadatumDTO("dc", "subject", null, "Metadata");
        metadatums.add(title);
        metadatums.add(title1);
        metadatums.add(projectidentifier);
        metadatums.add(contributor1);
        metadatums.add(description1);
        metadatums.add(subject1);
        metadatums.add(subject2);
        metadatums.add(subject3);
        metadatums.add(subject4);
        metadatums.add(subject5);

        ImportRecord firstRecord = new ImportRecord(metadatums);

        records.add(firstRecord);
        return records;
    }
}
