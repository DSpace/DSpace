/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.doi.crossref;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CrossRefClientTest {

    private MockWebServer server;
    private CrossRefClient client;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        var baseClient = TestClient.baseHttpClientForTest();

        client = new CrossRefClient(
                "http",
                server.getHostName(),
                server.getPort(),
                "/deposit",
                "user",
                "pass",
                baseClient);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void sendDepositRequest_success_returnsDoiResponseAndSendsMultipart() throws Exception {
        var metadata = "metaData";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?><doi_batch_diagnostic />"));

        var response = client.sendDepositRequest(metadata);

        assertThat(response).isNotNull();
        assertThat(response.statusCode()).isEqualTo(200);

        var recorded = server.takeRequest();

        // check https://www.crossref.org/documentation/register-maintain-records/direct-deposit-xml/https-post/ on details of the crossref API

        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/deposit");

        var body = recorded.getBody().readUtf8();
        assertThat(body).contains("Content-Disposition: form-data; name=\"mdFile\"; filename=\"requestData.xml\"");
        assertThat(body).contains(metadata);

        var contentType = recorded.getHeader("Content-Type");
        assertThat(contentType).contains("multipart/form-data");
    }

    @Test
    public void sendDepositRequest_unauthorized_throwsDOIIdentifierExceptionWithAuthCode() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?><doi_batch_diagnostic />"));

        assertThatExceptionOfType(DOIIdentifierException.class)
                .isThrownBy(() -> client.sendDepositRequest("metadata"))
                .matches(ex -> ex.getCode() == DOIIdentifierException.AUTHENTICATION_ERROR);

    }

    @Test
    public void sendDepositRequest_200withFailureMessage_throwsDOIIdentifierExceptionWithBadRequest() throws Exception {
        server.enqueue(new MockResponse()
                       .setResponseCode(200)
                       .setBody("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <doi_batch_diagnostic status="completed" sp="cskAir.local">
                                    <submission_id>1407395745</submission_id>
                                    <batch_id>test201707181318</batch_id>
                                    <record_diagnostic status="Failure" msg_id="4">
                                       <doi>10.5555/doitestwithcomponent_2</doi>
                                       <msg>Record not processed (etc.)</msg>
                                    </record_diagnostic>
                                    <batch_data>
                                       <record_count>1</record_count>
                                       <success_count>0</success_count>
                                       <warning_count>0</warning_count>
                                       <failure_count>1</failure_count>
                                    </batch_data>
                                 </doi_batch_diagnostic>
                                """));

        assertThatExceptionOfType(DOIIdentifierException.class)
                .isThrownBy(() -> client.sendDepositRequest("metadata"))
                .matches(ex -> ex.getCode() == DOIIdentifierException.BAD_REQUEST);
    }
}
