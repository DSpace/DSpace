/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.client;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.dspace.AbstractUnitTest;
import org.dspace.matomo.exception.MatomoClientException;
import org.dspace.matomo.model.MatomoRequestDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test for MatomoClientImplTest
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
@ExtendWith(MockitoExtension.class)
public class MatomoClientImplTest extends AbstractUnitTest {

    @Mock
    CloseableHttpClient httpClient;

    @Mock
    CloseableHttpResponse response;

    @Mock
    MatomoRequestBuilder builder;

    @Mock
    MatomoResponseReader reader;

    MatomoClientImpl matomoClient;

    @BeforeEach
    public void setUp() throws Exception {
        matomoClient = new MatomoClientImpl("testURL", "custom-token", builder, reader, httpClient);
    }

    @Test
    public void testNullRequest() {
        this.matomoClient.sendDetails((List<MatomoRequestDetails>) null);
        Mockito.verifyNoInteractions(httpClient);
        Mockito.verifyNoInteractions(builder);

        this.matomoClient.sendDetails();
        Mockito.verifyNoInteractions(httpClient);
        Mockito.verifyNoInteractions(builder);
    }

    @Test
    public void testEmptyRequest() {
        this.matomoClient.sendDetails(List.of());
        Mockito.verifyNoInteractions(httpClient);
        Mockito.verifyNoInteractions(builder);

        this.matomoClient.sendDetails();
        Mockito.verifyNoInteractions(httpClient);
        Mockito.verifyNoInteractions(builder);
    }


    @Test
    public void testSingleRequest() throws IOException {

        MatomoRequestDetails details =
            new MatomoRequestDetails()
                .addParameter("test1", "value1")
                .addParameter("test2", "value2")
                .addParameter("test3", "value3");

        String json =
            "{\"auth_token\": \"custom-token\", \"requests\": [\"?test1=value1&test2=value2&test3=value3\"]}";
        String jsonResponse =
            "{\"status\": \"success\", \"tracked\": 1, \"invalid\": 0, \"invalid_indices\": []}";
        Mockito.when(builder.buildJSON(Mockito.any())).thenReturn(json);
        Mockito.when(response.getCode()).thenReturn(200);
        Mockito.when(response.getEntity()).thenReturn(new StringEntity(jsonResponse));
        Mockito.when(reader.fromJSON(jsonResponse))
               .thenReturn(new MatomoResponse("success", 1, 0, null));
        Mockito.when(this.httpClient.execute(Mockito.any(HttpPost.class))).thenReturn(response);

        this.matomoClient.sendDetails(List.of(details));
        Mockito.verify(this.httpClient, Mockito.times(1)).execute(Mockito.any(HttpPost.class));

        this.matomoClient.sendDetails(details);
        Mockito.verify(this.httpClient, Mockito.times(2)).execute(Mockito.any(HttpPost.class));
    }

    @Test
    public void testFailSingleRequest() throws IOException {
        assertThrows(MatomoClientException.class, () -> {

            MatomoRequestDetails details =
                new MatomoRequestDetails()
                    .addParameter("test1", "value1")
                    .addParameter("test2", "value2")
                    .addParameter("test3", "value3");

            String json =
                "{'auth_token': 'custom-token', 'requests': ['?test1=value1&test2=value2&test3=value3']}";
            Mockito.when(builder.buildJSON(Mockito.any())).thenReturn(json);
            Mockito.when(response.getCode()).thenReturn(500);
            Mockito.when(this.httpClient.execute(Mockito.any(HttpPost.class))).thenReturn(response);

            this.matomoClient.sendDetails(List.of(details));
        });
    }


    @Test
    public void testExceptionRequest() throws IOException {
        assertThrows(MatomoClientException.class, () -> {

            MatomoRequestDetails details =
                new MatomoRequestDetails()
                    .addParameter("test1", "value1")
                    .addParameter("test2", "value2")
                    .addParameter("test3", "value3");

            String json =
                "{'auth_token': 'custom-token', 'requests': ['?test1=value1&test2=value2&test3=value3']}";
            Mockito.when(builder.buildJSON(Mockito.any())).thenReturn(json);
            Mockito.doThrow(IOException.class)
                .when(this.httpClient)
                .execute(Mockito.any(HttpPost.class));
            this.matomoClient.sendDetails(List.of(details));
        });
    }


}