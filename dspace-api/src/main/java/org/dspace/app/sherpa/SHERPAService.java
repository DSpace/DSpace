/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa;

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
import org.dspace.core.ConfigurationManager;

public class SHERPAService
{
    private CloseableHttpClient client = null;

    private int maxNumberOfTries;
    private long sleepBetweenTimeouts;
    private int timeout = 5000;

    /** log4j category */
    private static final Logger log = Logger.getLogger(SHERPAService.class);

    public SHERPAService() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        // httpclient 4.3+ doesn't appear to have any sensible defaults any more. Setting conservative defaults as not to hammer the SHERPA service too much.
        client = builder
                .disableAutomaticRetries()
                .setMaxConnTotal(5)
                .build();
    }


    public SHERPAResponse searchByJournalISSN(String query)
    {
        String endpoint = ConfigurationManager.getProperty("sherpa.romeo.url");
        String apiKey = ConfigurationManager.getProperty("sherpa.romeo.apikey");

        HttpGet method = null;
        SHERPAResponse sherpaResponse = null;
        int numberOfTries = 0;

        while(numberOfTries<maxNumberOfTries && sherpaResponse==null) {
            numberOfTries++;

            if (log.isDebugEnabled())
            {
                log.debug(String.format("Trying to contact SHERPA/RoMEO - attempt %d of %d; timeout is %d; sleep between timeouts is %d",
                        numberOfTries,
                        maxNumberOfTries,
                        timeout,
                        sleepBetweenTimeouts));
            }

            try {
                Thread.sleep(sleepBetweenTimeouts);

                URIBuilder uriBuilder = new URIBuilder(endpoint);
                uriBuilder.addParameter("issn", query);
                uriBuilder.addParameter("versions", "all");
                if (StringUtils.isNotBlank(apiKey))
                    uriBuilder.addParameter("ak", apiKey);

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

                if (null != responseBody)
                    sherpaResponse = new SHERPAResponse(responseBody.getContent());
                else
                    sherpaResponse = new SHERPAResponse("SHERPA/RoMEO returned no response");
            } catch (Exception e) {
                log.warn("Encountered exception while contacting SHERPA/RoMEO: " + e.getMessage(), e);
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
        }

        if(sherpaResponse==null){
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
