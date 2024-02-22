/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.matcher.LambdaMatcher.matches;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.ror.service.RorImportMetadataSourceServiceImpl;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public class RorImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

    @Autowired
    private LiveImportClientImpl liveImportClient;

    @Autowired
    private RorImportMetadataSourceServiceImpl rorServiceImpl;

    @Test
    public void tesGetRecords() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClient.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream file = getClass().getResourceAsStream("ror-records.json")) {

            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClient.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(jsonResponse, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            Collection<ImportRecord> recordsImported = rorServiceImpl.getRecords("test query", 0, 2);
            assertThat(recordsImported, hasSize(20));

            ImportRecord record = recordsImported.iterator().next();

            assertThat(record.getValueList(), hasSize(11));

            assertThat(
                record.getSingleValue("organization.legalName"),
                is("The University of Texas")
            );
            assertThat(record.getSingleValue("organization.identifier.ror"), is("https://ror.org/02f6dcw23"));
            assertThat(record.getSingleValue("organization.alternateName"), is("UTHSCSA"));
            assertThat(record.getSingleValue("organization.url"), is("http://www.uthscsa.edu/"));
            assertThat(record.getSingleValue("dc.type"), is("Education"));
            assertThat(record.getSingleValue("organization.address.addressCountry"), is("US"));
            assertThat(record.getSingleValue("organization.foundingDate"), is("1959"));
            assertThat(record.getValue("organization", "identifier", "crossrefid"), hasSize(2));
            assertThat(record.getSingleValue("organization.identifier.isni"), is("0000 0001 0629 5880"));
            assertThat(record.getSingleValue("organization.parentOrganization"), is("The University of Texas System"));

        } finally {
            liveImportClient.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void tesCount() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClient.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream file = getClass().getResourceAsStream("ror-records.json")) {

            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClient.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(jsonResponse, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            Integer count = rorServiceImpl.count("test");
            assertThat(count, equalTo(200));
        } finally {
            liveImportClient.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void tesGetRecord() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClient.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream file = getClass().getResourceAsStream("ror-record.json")) {

            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClient.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(jsonResponse, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ImportRecord record = rorServiceImpl.getRecord("https://ror.org/01sps7q28");
            assertThat(record.getValueList(), hasSize(9));
            assertThat(
                record.getSingleValue("organization.legalName"),
                is("The University of Texas Health Science Center at Tyler")
            );
            assertThat(record.getSingleValue("organization.identifier.ror"), is("https://ror.org/01sps7q28"));
            assertThat(record.getSingleValue("organization.alternateName"), is("UTHSCT"));
            assertThat(record.getSingleValue("organization.url"),
                is("https://www.utsystem.edu/institutions/university-texas-health-science-center-tyler"));
            assertThat(record.getSingleValue("dc.type"), is("Healthcare"));
            assertThat(record.getSingleValue("organization.address.addressCountry"), is("US"));
            assertThat(record.getSingleValue("organization.foundingDate"), is("1947"));
            assertThat(record.getSingleValue("organization.identifier.isni"), is("0000 0000 9704 5790"));
            assertThat(record.getSingleValue("organization.parentOrganization"), is("The University of Texas System"));

        } finally {
            liveImportClient.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void tesGetRecordsCount() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClient.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        try (InputStream rorResponse = getClass().getResourceAsStream("ror-records.json")) {
            String rorJsonResponse = IOUtils.toString(rorResponse, Charset.defaultCharset());

            liveImportClient.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(rorJsonResponse, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            int tot = rorServiceImpl.getRecordsCount("test query");
            assertEquals(200, tot);
        } finally {
            liveImportClient.setHttpClient(originalHttpClient);
        }
    }

    private Matcher<Optional<String>> is(String value) {
        return matches(optionalValue -> optionalValue.isPresent() && optionalValue.get().equals(value));
    }
}
