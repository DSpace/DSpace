/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scielo.service;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.el.MethodNotFoundException;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.FileSourceException;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements a data source for querying Scielo
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class ScieloImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<Map<String,List<String>>>
                                                   implements QuerySource {

    private static final Logger log = Logger.getLogger(ScieloImportMetadataSourceServiceImpl.class);

    private static final String ENDPOINT_SEARCH_SCIELO = "https://search.scielo.org/?output=ris&q=";

    private static final String PATTERN = "^([A-Z][A-Z0-9])  - (.*)$";

    private static final String ID_PATTERN  = "^(.....)-(.*)-(...)$";

    private int timeout = 1000;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public void init() throws Exception {}

    @Override
    public String getImportSource() {
        return "scielo";
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query, count, start));
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query));
    }


    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByQueryCallable(query));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        List<ImportRecord> records = retry(new FindByIdCallable(id));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        return retry(new SearchNBByQueryCallable(query));
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for WOS");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for WOS");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for WOS");
    }

    /**
     * This class implements a callable to get the numbers of result
     * 
     * @author Boychuk Mykhaylo
     */
    private class SearchNBByQueryCallable implements Callable<Integer> {

        private String query;

        private SearchNBByQueryCallable(String queryString) {
            this.query = queryString;
        }

        private SearchNBByQueryCallable(Query query) {
            this.query = query.getParameterAsClass("query", String.class);
        }

        @Override
        public Integer call() throws Exception {
            String proxyHost = configurationService.getProperty("http.proxy.host");
            String proxyPort = configurationService.getProperty("http.proxy.port");
            HttpGet method = null;
            try {
                HttpClientBuilder hcBuilder = HttpClients.custom();
                Builder requestConfigBuilder = RequestConfig.custom();
                requestConfigBuilder.setConnectionRequestTimeout(timeout);

                if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort)) {
                    HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort), "http");
                    DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
                    hcBuilder.setRoutePlanner(routePlanner);
                }
                HttpClient client = hcBuilder.build();
                method = new HttpGet(ENDPOINT_SEARCH_SCIELO + URLEncoder.encode(query, StandardCharsets.UTF_8));
                method.setConfig(requestConfigBuilder.build());
                HttpResponse httpResponse = client.execute(method);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    throw new RuntimeException("WS call failed: " + statusCode);
                }
                InputStream is = httpResponse.getEntity().getContent();
                Map<Integer, Map<String,List<String>>> records = getRecords(is);
                if (Objects.nonNull(records.size())) {
                    return records.size();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
            return 0;
        }
    }

    private class FindByIdCallable implements Callable<List<ImportRecord>> {

        private String id;

        private FindByIdCallable(String id) {
            this.id = id;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> results = new ArrayList<>();
            String scieloId = id.trim();
            Pattern risPattern = Pattern.compile(ID_PATTERN);
            Matcher risMatcher = risPattern.matcher(scieloId);
            if (risMatcher.matches()) {
                String proxyHost = configurationService.getProperty("http.proxy.host");
                String proxyPort = configurationService.getProperty("http.proxy.port");
                HttpGet method = null;
                try {
                    HttpClientBuilder hcBuilder = HttpClients.custom();
                    Builder requestConfigBuilder = RequestConfig.custom();
                    requestConfigBuilder.setConnectionRequestTimeout(timeout);
                    if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort)) {
                        HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort), "http");
                        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
                        hcBuilder.setRoutePlanner(routePlanner);
                    }
                    HttpClient client = hcBuilder.build();
                    method = new HttpGet(ENDPOINT_SEARCH_SCIELO + URLEncoder.encode(scieloId, StandardCharsets.UTF_8));
                    method.setConfig(requestConfigBuilder.build());
                    HttpResponse httpResponse = client.execute(method);
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK) {
                        throw new RuntimeException("WS call failed: " + statusCode);
                    }
                    InputStream is = httpResponse.getEntity().getContent();
                    Map<Integer, Map<String, List<String>>> records = getRecords(is);
                    if (Objects.nonNull(records) & !records.isEmpty()) {
                        results.add(transformSourceRecords(records.get(1)));
                    }
                } catch (Exception e1) {
                    log.error(e1.getMessage(), e1);
                } finally {
                    if (method != null) {
                        method.releaseConnection();
                    }
                }
            } else {
                throw new BadRequestException("id provided : " + scieloId + " is not an ScieloID");
            }
            return results;
        }
    }

    private class SearchByQueryCallable implements Callable<List<ImportRecord>> {
        private Query query;


        private SearchByQueryCallable(String queryString, Integer maxResult, Integer start) {
            query = new Query();
            query.addParameter("query", queryString);
            query.addParameter("start", start);
            query.addParameter("count", maxResult);
        }

        private SearchByQueryCallable(Query query) {
            this.query = query;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> results = new ArrayList<>();
            String q = query.getParameterAsClass("query", String.class);
            String proxyHost = configurationService.getProperty("http.proxy.host");
            String proxyPort = configurationService.getProperty("http.proxy.port");
            HttpGet method = null;
            try {
                HttpClientBuilder hcBuilder = HttpClients.custom();
                Builder requestConfigBuilder = RequestConfig.custom();
                requestConfigBuilder.setConnectionRequestTimeout(timeout);
                if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort)) {
                    HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort), "http");
                    DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
                    hcBuilder.setRoutePlanner(routePlanner);
                }
                HttpClient client = hcBuilder.build();
                method = new HttpGet(ENDPOINT_SEARCH_SCIELO + URLEncoder.encode(q, StandardCharsets.UTF_8));
                method.setConfig(requestConfigBuilder.build());
                HttpResponse httpResponse = client.execute(method);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    throw new RuntimeException("WS call failed: " + statusCode);
                }
                InputStream is = httpResponse.getEntity().getContent();
                Map<Integer, Map<String,List<String>>> records = getRecords(is);
                for (int record : records.keySet()) {
                    results.add(transformSourceRecords(records.get(record)));
                }
            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
            }
            return results;
        }
    }

    private Map<Integer, Map<String,List<String>>> getRecords(InputStream inputStream) throws FileSourceException {
        Map<Integer, Map<String, List<String>>> records = new HashMap<Integer, Map<String,List<String>>>();
        BufferedReader reader;
        int countRecord = 0;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.equals("") || line.matches("^\\s*$")) {
                    continue;
                }
                line = line.replaceAll("\\uFEFF", "").trim();
                Pattern risPattern = Pattern.compile(PATTERN);
                Matcher risMatcher = risPattern.matcher(line);
                if (risMatcher.matches()) {
                    if (risMatcher.group(1).equals("TY") & risMatcher.group(2).equals("JOUR")) {
                        countRecord ++;
                        Map<String,List<String>> newMap = new HashMap<String, List<String>>();
                        records.put(countRecord, newMap);
                    } else {
                        Map<String, List<String>> tag2values = records.get(countRecord);
                        List<String> values = tag2values.get(risMatcher.group(1));
                        if (Objects.isNull(values)) {
                            List<String> newValues = new ArrayList<String>();
                            newValues.add(risMatcher.group(2));
                            tag2values.put(risMatcher.group(1), newValues);
                        } else {
                            values.add(risMatcher.group(2));
                            tag2values.put(risMatcher.group(1), values);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new FileSourceException("Cannot parse RIS file", e);
        }
        return records;
    }

}