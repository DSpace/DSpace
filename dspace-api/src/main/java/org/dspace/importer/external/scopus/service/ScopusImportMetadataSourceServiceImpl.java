/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scopus.service;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import javax.el.MethodNotFoundException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
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
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.dspace.services.ConfigurationService;
import org.jaxen.JaxenException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements a data source for querying Scopus
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4Science dot it)
 *
 */

public class ScopusImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<OMElement>
    implements QuerySource {

    @Autowired
    private ConfigurationService configurationService;

    private int timeout = 1000;

    private static final Logger log = Logger.getLogger(ScopusImportMetadataSourceServiceImpl.class);

    int itemPerPage = 25;

    private static final String ENDPOINT_SEARCH_SCOPUS = "http://api.elsevier.com/content/search/scopus";

    /**
     * Initialize the class
     *
     * @throws Exception on generic exception
     */
    @Override
    public void init() throws Exception { }

    /**
     * The string that identifies this import implementation. Preferable a URI
     *
     * @return the identifying uri
     */
    @Override
    public String getImportSource() {
        return "scopus";
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        return retry(new SearchNBByQueryCallable(query));
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        return retry(new SearchNBByQueryCallable(query));
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start,
            int count) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query, count, start));
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query)
            throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query));
    }


    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByQueryCallable(query));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item)
            throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for Scopus");
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        List<ImportRecord> records = retry(new FindByIdCallable(id));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query)
            throws MetadataSourceException {
        return retry(new FindByQueryCallable(query));
    }

/**
 * 
 * This class implements a callable to get the numbers of result
 * @author pasquale
 *
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
            List<ImportRecord> results = new ArrayList<>();
            String proxyHost = configurationService.getProperty("http.proxy.host");
            String proxyPort = configurationService.getProperty("http.proxy.port");
            String apiKey = configurationService.getProperty("submission.lookup.scopus.apikey");
            if (apiKey != null && !apiKey.equals("")) {
                HttpGet method = null;
                try {
                    HttpClientBuilder hcBuilder = HttpClients.custom();
                    Builder requestConfigBuilder = RequestConfig.custom();
                    requestConfigBuilder.setConnectionRequestTimeout(timeout);

                    if (StringUtils.isNotBlank(proxyHost)
                        && StringUtils.isNotBlank(proxyPort)) {
                        HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort), "http");
                        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
                        hcBuilder.setRoutePlanner(routePlanner);
                    }

                    HttpClient client = hcBuilder.build();
                    // open session
                    method = new HttpGet(
                        ENDPOINT_SEARCH_SCOPUS + "?httpAccept=application/xml&apiKey=" + apiKey + query);
                    method.setConfig(requestConfigBuilder.build());
                        // Execute the method.
                    HttpResponse httpResponse = client.execute(method);
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK) {
                        throw new RuntimeException("WS call failed: "
                                                           + statusCode);
                    }
                    InputStream is = httpResponse.getEntity().getContent();
                    String response = IOUtils.toString(is, Charsets.UTF_8);
                    OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(response));
                    OMElement element = records.getDocumentElement();
                    AXIOMXPath xpath = null;
                    try {
                        xpath = new AXIOMXPath("opensearch:totalResults");
                        xpath.addNamespace("opensearch", "http://a9.com/-/spec/opensearch/1.1/");
                        OMElement count = (OMElement) xpath.selectSingleNode(element);
                        return Integer.parseInt(count.getText());
                    } catch (JaxenException e) {
                        return null;
                    }
                } catch (Exception e1) {
                    log.error(e1.getMessage(), e1);
                } finally {
                    if (method != null) {
                        method.releaseConnection();
                    }
                }
            }
            return null;
        }
    }


    private class FindByIdCallable implements Callable<List<ImportRecord>> {

        private String doi;

        private FindByIdCallable(String doi) {
            this.doi = doi;
        }


        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> results = new ArrayList<>();
            String queryString = "DOI(" + doi.replace("!", "/") + ")";
            String proxyHost = configurationService.getProperty("http.proxy.host");
            String proxyPort = configurationService.getProperty("http.proxy.port");
            String apiKey = configurationService.getProperty("submission.lookup.scopus.apikey");
            if (apiKey != null && !apiKey.equals("")) {
                HttpGet method = null;
                try {
                    HttpClientBuilder hcBuilder = HttpClients.custom();
                    Builder requestConfigBuilder = RequestConfig.custom();
                    requestConfigBuilder.setConnectionRequestTimeout(timeout);

                    if (StringUtils.isNotBlank(proxyHost)
                        && StringUtils.isNotBlank(proxyPort)) {
                        HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort), "http");
                        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
                        hcBuilder.setRoutePlanner(routePlanner);
                    }

                    HttpClient client = hcBuilder.build();
                    // open session
                    method = new HttpGet(
                        ENDPOINT_SEARCH_SCOPUS + "?httpAccept=application/xml&apiKey=" + apiKey +
                            "&view=COMPLETE&query=" + URLEncoder
                            .encode(queryString));
                    method.setConfig(requestConfigBuilder.build());
                        // Execute the method.
                    HttpResponse httpResponse = client.execute(method);
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK) {
                        throw new RuntimeException("WS call failed: "
                                                           + statusCode);
                    }
                    InputStream is = httpResponse.getEntity().getContent();
                    String response = IOUtils.toString(is, Charsets.UTF_8);
                    List<OMElement> omElements = splitToRecords(response);
                    for (OMElement record : omElements) {
                        results.add(transformSourceRecords(record));
                    }
                } catch (Exception e1) {
                    log.error(e1.getMessage(), e1);
                } finally {
                    if (method != null) {
                        method.releaseConnection();
                    }
                }
            }
            return results;
        }
    }


/**
 * 
 * This class implements a callable to get the items based on query parameters
 * @author pasquale
 *
 */
    private class FindByQueryCallable implements Callable<List<ImportRecord>> {

        private String title;
        private String author;
        private Integer year;
        private Integer start;
        private Integer count;

        private FindByQueryCallable(Query query) {
            this.title = query.getParameterAsClass("title", String.class);
            this.year = query.getParameterAsClass("year", Integer.class);
            this.author = query.getParameterAsClass("author", String.class);
            this.start = query.getParameterAsClass("start", Integer.class) != null ?
                query.getParameterAsClass("start", Integer.class) : 0;
            this.count = query.getParameterAsClass("count", Integer.class) != null ?
                query.getParameterAsClass("count", Integer.class) : 20;
        }


        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> results = new ArrayList<>();
            String queryString = "";
            StringBuffer query = new StringBuffer();
            if (StringUtils.isNotBlank(title)) {
                query.append("title(").append(title).append("");
            }
            if (StringUtils.isNotBlank(author)) {
                // [FAU]
                if (query.length() > 0) {
                    query.append(" AND ");
                }
                query.append("AUTH(").append(author).append(")");
            }
            if (year != -1) {
                // [DP]
                if (query.length() > 0) {
                    query.append(" AND ");
                }
                query.append("PUBYEAR IS ").append(year);
            }
            queryString = query.toString();

            String proxyHost = configurationService.getProperty("http.proxy.host");
            String proxyPort = configurationService.getProperty("http.proxy.port");
            String apiKey = configurationService.getProperty("submission.lookup.scopus.apikey");

            if (apiKey != null && !apiKey.equals("")) {
                HttpGet method = null;
                try {
                    HttpClientBuilder hcBuilder = HttpClients.custom();
                    Builder requestConfigBuilder = RequestConfig.custom();
                    requestConfigBuilder.setConnectionRequestTimeout(timeout);

                    if (StringUtils.isNotBlank(proxyHost)
                        && StringUtils.isNotBlank(proxyPort)) {
                        HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort), "http");
                        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
                        hcBuilder.setRoutePlanner(routePlanner);
                    }

                    HttpClient client = hcBuilder.build();
                    // open session
                    method = new HttpGet(
                        ENDPOINT_SEARCH_SCOPUS + "?httpAccept=application/xml&apiKey=" + apiKey +
                            "&view=COMPLETE&start=" + start + "&count=" + count + "&query=" + URLEncoder
                            .encode(queryString));
                    method.setConfig(requestConfigBuilder.build());
                        // Execute the method.
                    HttpResponse httpResponse = client.execute(method);
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK) {
                        throw new RuntimeException("WS call failed: "
                                                           + statusCode);
                    }
                    InputStream is = httpResponse.getEntity().getContent();
                    String response = IOUtils.toString(is, Charsets.UTF_8);
                    List<OMElement> omElements = splitToRecords(response);
                    for (OMElement record : omElements) {
                        results.add(transformSourceRecords(record));
                    }
                } catch (Exception e1) {
                    log.error(e1.getMessage(), e1);
                } finally {
                    if (method != null) {
                        method.releaseConnection();
                    }
                }
            }
            return results;
        }
    }

    /**
     * That's ok, just a separator
     * @author pasquale
     *
     */
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
            String queryString = query.getParameterAsClass("query", String.class);
            Integer start = query.getParameterAsClass("start", Integer.class);
            Integer count = query.getParameterAsClass("count", Integer.class);
            String proxyHost = configurationService.getProperty("http.proxy.host");
            String proxyPort = configurationService.getProperty("http.proxy.port");
            String apiKey = configurationService.getProperty("submission.lookup.scopus.apikey");

            if (apiKey != null && !apiKey.equals("")) {
                HttpGet method = null;
                try {
                    HttpClientBuilder hcBuilder = HttpClients.custom();
                    Builder requestConfigBuilder = RequestConfig.custom();
                    requestConfigBuilder.setConnectionRequestTimeout(timeout);

                    if (StringUtils.isNotBlank(proxyHost)
                        && StringUtils.isNotBlank(proxyPort)) {
                        HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort), "http");
                        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
                        hcBuilder.setRoutePlanner(routePlanner);
                    }

                    HttpClient client = hcBuilder.build();
                    // open session
                    method = new HttpGet(
                        ENDPOINT_SEARCH_SCOPUS + "?httpAccept=application/xml&apiKey=" + apiKey +
                            "&start=" + (start != null ? start : 0) + "&count=" + (count != null ? count : 20) +
                            "&query=" + URLEncoder.encode(queryString));
                    method.setConfig(requestConfigBuilder.build());
                        // Execute the method.
                    HttpResponse httpResponse = client.execute(method);
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK) {
                        throw new RuntimeException("WS call failed: "
                                                           + statusCode);
                    }
                    InputStream is = httpResponse.getEntity().getContent();
                    String response = IOUtils.toString(is, Charsets.UTF_8);
                    List<OMElement> omElements = splitToRecords(response);
                    for (OMElement record : omElements) {
                        results.add(transformSourceRecords(record));
                    }
                } catch (Exception e1) {
                    log.error(e1.getMessage(), e1);
                } finally {
                    if (method != null) {
                        method.releaseConnection();
                    }
                }
            }
            return results;
        }
    }


    private List<OMElement> splitToRecords(String recordsSrc) {
        OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(recordsSrc));
        OMElement element = records.getDocumentElement();
        AXIOMXPath xpath = null;
        try {
            xpath = new AXIOMXPath("ns:entry");
            xpath.addNamespace("ns", "http://www.w3.org/2005/Atom");
            List<OMElement> recordsList = xpath.selectNodes(element);
            return recordsList;
        } catch (JaxenException e) {
            return null;
        }
    }

}
