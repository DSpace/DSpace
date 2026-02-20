/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.doi.crossref;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Test;

/**
 * Manual test that connects to the actual Crossref API.
 * Require local.properties with crossref credentials to be setup.
 */
public class CrossRefClientManual {

    private CrossRefClient client;

    @Before
    public void setUp() throws IOException {

        var baseClient = TestClient.baseHttpClientForTest();

        var credentials = credentials();

        this.client = new CrossRefClient(
                "https",
                "test.crossref.org",
                null,
                "/v2/deposits",
                credentials.username(),
                credentials.secret(),
                baseClient);
    }

    @Test
    public void sendDepositRequest_success_returnsDoiResponseAndSendsMultipart() throws Exception {

        Configurator.setLevel("org.apache.http.wire", Level.DEBUG);

        // language=XML
        var metadata = """
                <?xml version="1.0" encoding="UTF-8"?>
                            <doi_batch version="4.4.2" xmlns="http://www.crossref.org/schema/4.4.2"
                                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                xsi:schemaLocation="http://www.crossref.org/schema/4.4.2 http://www.crossref.org/schema/deposit/crossref4.4.2.xsd">
                              <head>
                                <doi_batch_id>example-batch-001</doi_batch_id>
                                <timestamp>20251014</timestamp>
                                <depositor>
                                  <depositor_name>Example Publisher</depositor_name>
                                  <email_address>contact@example.org</email_address>
                                </depositor>
                                <registrant>Example Publisher</registrant>
                              </head>
                              <body>
                                <journal>
                                  <journal_metadata language="en">
                                    <full_title>Example Journal</full_title>
                                    <abbrev_title>Ex J</abbrev_title>
                                    <issn media_type="electronic">1234-5678</issn>
                                  </journal_metadata>
                                  <journal_issue>
                                    <publication_date media_type="online">
                                      <year>2025</year>
                                    </publication_date>
                                    <journal_volume>
                                      <volume>10</volume>
                                    </journal_volume>
                                    <issue>1</issue>
                                  </journal_issue>
                                  <journal_article publication_type="full_text">
                                    <titles>
                                      <title>Example Article Title</title>
                                    </titles>
                                    <contributors>
                                      <person_name sequence="first" contributor_role="author">
                                        <given_name>Jane</given_name>
                                        <surname>Doe</surname>
                                      </person_name>
                                    </contributors>
                                    <publication_date media_type="online">
                                      <year>2025</year>
                                    </publication_date>
                                    <doi_data>
                                      <doi>10.9876/example.001</doi>
                                      <resource>https://example.org/articles/001</resource>
                                    </doi_data>
                                  </journal_article>
                                </journal>
                              </body>
                            </doi_batch>
                """;

        var response = client.sendDepositRequest(metadata);

        assertThat(response).isNotNull();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.content()).contains("Your batch submission was successfully received.");
    }

    private static CrossRefTestCredentials credentials() {
        var file = new File("../local.properties").getAbsoluteFile();
        assertThat(file).describedAs("please create a file local.properties " +
                                     "with the crossref credentials in the project root!").exists();

        try (var fis = new FileInputStream(file)) {
            var props = new Properties();
            props.load(fis);

            return new CrossRefTestCredentials(
                    Objects.requireNonNull(props.getProperty("crossref.username"),
                            "crossref.username is required"),
                    Objects.requireNonNull(props.getProperty("crossref.secret"),
                            "crossref.secret is required"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    record CrossRefTestCredentials(String username, String secret) {}
}