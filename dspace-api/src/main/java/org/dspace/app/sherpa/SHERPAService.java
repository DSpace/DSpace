/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.dspace.app.sherpa.v2.SHERPAResponse;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class SHERPAService {
    private CloseableHttpClient client = null;
    private int maxNumberOfTries;
    private long sleepBetweenTimeouts;
    private int timeout = 5000;

    /** log4j category */
    private static final Logger log = Logger.getLogger(SHERPAService.class);

    @Autowired
    ConfigurationService configurationService;

    public SHERPAService() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        // httpclient 4.3+ doesn't appear to have any sensible defaults any more.
        // Setting conservative defaults as not to hammer the SHERPA service too much.
        client = builder
                .disableAutomaticRetries()
                .setMaxConnTotal(5)
                .build();
    }

    public SHERPAResponse searchByJournalISSN(String query) {
        String endpoint = configurationService.getProperty("sherpa.romeo.url",
            "https://v2.sherpa.ac.uk/cgi/retrieve");
        String apiKey = configurationService.getProperty("sherpa.romeo.apikey");

        // API Key is *required* for v2 API calls
        if (null == apiKey) {
            log.error("SHERPA ROMeO API Key missing: please register for an API key and set sherpa.romeo.apikey");
            return new SHERPAResponse("SHERPA/RoMEO configuration invalid or missing");
        }

        HttpGet method = null;
        SHERPAResponse sherpaResponse = null;
        int numberOfTries = 0;

        while (numberOfTries < maxNumberOfTries && sherpaResponse == null) {
            numberOfTries++;

            if (log.isDebugEnabled()) {
                log.debug(String.format("Trying to contact SHERPA/RoMEO - attempt %d of %d; timeout is %d; sleep" +
                        "between timeouts is %d",
                        numberOfTries,
                        maxNumberOfTries,
                        timeout,
                        sleepBetweenTimeouts));
            }

            try {
                Thread.sleep(sleepBetweenTimeouts);

                URIBuilder uriBuilder = new URIBuilder(endpoint);

                uriBuilder.addParameter("item-type", "publication");
                uriBuilder.addParameter("filter", "[[\"issn\",\"equals\",\"" + query + "\"]]");
                uriBuilder.addParameter("format", "Json");
                if (StringUtils.isNotBlank(apiKey)) {
                    uriBuilder.addParameter("api-key", apiKey);
                }

                log.debug("Searching SHERPA endpoint with " + uriBuilder.toString());

                method = new HttpGet(uriBuilder.build());
                method.setConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(timeout)
                        .setConnectTimeout(timeout)
                        .setSocketTimeout(timeout)
                        .build());
                // Execute the method.

                HttpResponse response = client.execute(method);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != HttpStatus.SC_OK) {
                    sherpaResponse = new SHERPAResponse("SHERPA/RoMEO return not OK status: "
                            + statusCode);
                }

                HttpEntity responseBody = response.getEntity();

                if (null != responseBody) {
                    InputStream content = null;
                    try {
                        content = responseBody.getContent();
                        sherpaResponse = new SHERPAResponse(content, SHERPAResponse.SHERPAFormat.JSON);
                    } catch (IOException e) {
                        log.error("Encountered exception while contacting SHERPA/RoMEO: " + e.getMessage(), e);
                    } finally {
                        if (content != null) {
                            content.close();
                        }
                    }
                } else {
                    sherpaResponse = new SHERPAResponse("SHERPA/RoMEO returned no response");
                }
            } catch (Exception e) {
                log.error("Encountered exception while contacting SHERPA/RoMEO: " + e.getMessage(), e);
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
        }

        if (sherpaResponse == null) {
            sherpaResponse = new SHERPAResponse(
                    "Error processing the SHERPA/RoMEO answer");
        }

        return sherpaResponse;
    }

    public void setMaxNumberOfTries(int maxNumberOfTries) {
        this.maxNumberOfTries = maxNumberOfTries;
    }

    public void setSleepBetweenTimeouts(long sleepBetweenTimeouts) {
        this.sleepBetweenTimeouts = sleepBetweenTimeouts;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
