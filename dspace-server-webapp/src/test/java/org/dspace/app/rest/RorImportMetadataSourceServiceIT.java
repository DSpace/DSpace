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

        //ror-records.json is the result of a GET request to https://api.ror.org/v2/organizations at 16/07/2025.
        try (InputStream file = getClass().getResourceAsStream("ror-records.json")) {

            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClient.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(jsonResponse, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            Collection<ImportRecord> recordsImported = rorServiceImpl.getRecords("test query", 0, 2);
            assertThat(recordsImported, hasSize(20));

            ImportRecord record = recordsImported.iterator().next();

            assertThat(record.getValueList(), hasSize(9));

            assertThat(record.getSingleValue("organization.legalName"),
                    is("University American College Skopje"));
            assertThat(record.getSingleValue("organization.identifier.ror"), is("https://ror.org/05hknds03"));
            assertThat(record.getSingleValue("organization.alternateName"), is("UACS"));
            assertThat(record.getSingleValue("organization.url"), is("https://uacs.edu.mk"));
            assertThat(record.getSingleValue("dc.type"), is("education"));
            assertThat(record.getSingleValue("organization.address.addressCountry"), is("MK"));
            assertThat(record.getSingleValue("organization.address.addressLocality"), is("Skopje"));
            assertThat(record.getSingleValue("organization.foundingDate"), is("2005"));
            assertThat(record.getSingleValue("organization.identifier.isni"), is("0000 0004 0446 4427"));


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
            assertThat(count, equalTo(115409));
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

            System.out.println("file = " + file.toString());

            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            liveImportClient.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(jsonResponse, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ImportRecord record = rorServiceImpl.getRecord("https://ror.org/02437s643");

            assertThat(record.getValueList(), hasSize(10));
            assertThat(record.getSingleValue("organization.legalName"),
                    is("University of Illinois Chicago, Rockford campus"));
            assertThat(record.getSingleValue("organization.identifier.ror"), is("https://ror.org/02437s643"));
            assertThat(record.getSingleValue("organization.alternateName"), is("UICOMR"));
            assertThat(record.getSingleValue("organization.url"), is("https://www.uillinois.edu"));
            assertThat(record.getSingleValue("dc.type"), is("education"));
            assertThat(record.getSingleValue("organization.address.addressCountry"), is("US"));
            assertThat(record.getSingleValue("organization.address.addressLocality"), is("Rockford"));
            assertThat(record.getSingleValue("organization.foundingDate"), is("1972"));
            assertThat(record.getSingleValue("organization.identifier.isni"), is("0000 0000 9018 7542"));
            assertThat(record.getSingleValue("organization.parentOrganization"), is("University of Illinois Chicago"));
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
            assertEquals(115409, tot);
        } finally {
            liveImportClient.setHttpClient(originalHttpClient);
        }
    }

    private Matcher<Optional<String>> is(String value) {
        return matches(optionalValue -> optionalValue.isPresent() && optionalValue.get().equals(value));
    }
}
