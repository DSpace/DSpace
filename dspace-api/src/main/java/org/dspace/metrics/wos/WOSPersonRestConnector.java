/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.wos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.Logger;
import org.dspace.metrics.scopus.CrisMetricDTO;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Connectors towards Web Of Science external service that collects
 * author-related metrics.
 *
 * Used {@link CloseableHttpClient} can be injected. i.e. for testing purposes.
 * Please note that {@link CloseableHttpClient} instance connection is eventually closed after performing operation.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class WOSPersonRestConnector {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(WOSPersonRestConnector.class);

    private String apiKey;
    private String wosUrl;
    private CloseableHttpClient httpClient;


    public CrisMetricDTO sendRequestToWOS(String orcidId)throws IOException {
        double total = 0;
        int record = 0;
        final int count = 100;
        int recordsFound = -1;
        JSONObject json = null;
        JSONArray records = null;
        boolean error = false;
        CrisMetricDTO metricDTO = new CrisMetricDTO();
        while (!error && (recordsFound == -1 || record < recordsFound)) {
            recordsFound = 0;
            try (CloseableHttpClient httpClient = Optional.ofNullable(this.httpClient)
                .orElseGet(HttpClients::createDefault)) {
                HttpGet httpGet = new HttpGet(wosUrl.concat("AI=(").concat(orcidId).concat(")&count=")
                    .concat(String.valueOf(count)).concat("&firstRecord=").concat(String.valueOf(record + 1)));
                httpGet.setHeader("Accept-Encoding", "gzip, deflate, br");
                httpGet.setHeader("Connection", "keep-alive");
                httpGet.setHeader("X-ApiKey", apiKey);
                httpGet.setHeader("Accept", "application/json");

                HttpResponse response = httpClient.execute(httpGet);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    return null;
                }
                InputStream inputStream = response.getEntity().getContent();
                try {
                    json = new JSONObject(IOUtils.toString(inputStream, Charset.defaultCharset()));
                    if (StringUtils
                        .isBlank(json.getJSONObject("Data").getJSONObject("Records").get("records").toString())) {
                        return null;
                    }
                    recordsFound = json.getJSONObject("QueryResult").getInt("RecordsFound");
                    records = json.getJSONObject("Data")
                        .getJSONObject("Records")
                        .getJSONObject("records")
                        .getJSONArray("REC");
                } catch (JSONException | IOException e) {
                    log.error(e.getMessage(), e);
                    error = true;
                } finally {
                    if (Objects.nonNull(inputStream)) {
                        inputStream.close();
                    }
                }
                record += records.length();
                total += sumMetricCounts(records);
                if (records.length() < count) {
                    // to be safe in the case the wos api would return less records than what initially reported
                    break;
                }
            }
        }
        metricDTO.setMetricCount(total);
        metricDTO.setMetricType(UpdateWOSPersonMetrics.WOS_PERSON_METRIC_TYPE);
        return metricDTO;
    }

    private int sumMetricCounts(JSONArray records) {
        int total = 0;
        if (Objects.nonNull(records)) {
            for (int i = 0; i < records.length(); i++) {
                Integer count = records.getJSONObject(i)
                                       .getJSONObject("dynamic_data")
                                       .getJSONObject("citation_related")
                                       .getJSONObject("tc_list")
                                       .getJSONObject("silo_tc")
                                       .getInt("local_count");
                if (Objects.nonNull(count)) {
                    total += count.intValue();
                }
            }
        }
        return total;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getWosUrl() {
        return wosUrl;
    }

    public void setWosUrl(String wosUrl) {
        this.wosUrl = wosUrl;
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * sets a custom {@link CloseableHttpClient} instance. Please make sure that
     * this instance is not closed.
     * @param httpClient
     */
    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

}