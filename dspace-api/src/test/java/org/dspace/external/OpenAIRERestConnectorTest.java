/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import eu.openaire.jaxb.model.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;


public class OpenAIRERestConnectorTest {

    @Test
    public void searchProjectByKeywords() {
        try (InputStream is = this.getClass().getResourceAsStream("openaire-projects.xml");
               MockWebServer mockServer = new MockWebServer()) {
            String projects = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("( mushroom)", "( DEADBEEF)");
            mockServer.enqueue(new MockResponse().setResponseCode(200).setBody(projects));

            // setup mocks so we don't have to set whole DSpace kernel etc.
            // still, the idea is to test how the get method behaves
            CloseableHttpClient httpClient = spy(HttpClientBuilder.create().build());
            doReturn(httpClient.execute(new HttpGet(mockServer.url("").toString())))
                    .when(httpClient).execute(Mockito.any());

            DSpaceHttpClientFactory mock = Mockito.mock(DSpaceHttpClientFactory.class);
            when(mock.build()).thenReturn(httpClient);

            try (MockedStatic<DSpaceHttpClientFactory> mockedFactory =
                         Mockito.mockStatic(DSpaceHttpClientFactory.class)) {
                mockedFactory.when(DSpaceHttpClientFactory::getInstance).thenReturn(mock);
                OpenAIRERestConnector connector = new OpenAIRERestConnector(mockServer.url("").toString());
                Response response = connector.searchProjectByKeywords(0, 10, "keyword");
                // Basically check it doesn't throw UnmarshallerException and that we are getting our mocked response
                assertTrue("Expected the query to contain the replaced keyword",
                        response.getHeader().getQuery().contains("DEADBEEF"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}