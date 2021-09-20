/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.openaire.service;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

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
 * Implements a data source for querying OpenAIRE
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class OpenAireProjectImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<OMElement>
    implements QuerySource {

    private static final Logger log = Logger.getLogger(OpenAireProjectImportMetadataSourceServiceImpl.class);
    private static final String ENDPOINT_SEARCH_OPENAIRE = "http://api.openaire.eu/search/projects";

    private int timeout = 1000;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Initialize the class
     *
     * @throws Exception on generic exception
     */
    @Override
    public void init() throws Exception {}

    /**
     * The string that identifies this import implementation. Preferable a URI
     *
     * @return the identifying uri
     */
    @Override
    public String getImportSource() {
        return "openaire";
    }

    /**
     * Get a single record from the source by id
     *
     * @param id   id of the record in OpenAIRE
     * 
     * @return the first matching record
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByIdCallable(id));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query, count, start));
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query));
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        // TODO Auto-generated method stub
        return null;
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
            List<ImportRecord> results = new ArrayList<ImportRecord>();
            String queryString = query.getParameterAsClass("query", String.class);
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
                // open session
                method = new HttpGet(ENDPOINT_SEARCH_OPENAIRE + "?name=" + URLEncoder.encode(queryString));
                method.setConfig(requestConfigBuilder.build());
                // Execute the method.
                HttpResponse httpResponse = client.execute(method);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    throw new RuntimeException("WS call failed: " + statusCode);
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
            return results;
        }
    }

    private class SearchByIdCallable implements Callable<List<ImportRecord>> {
        private Query query;

        private SearchByIdCallable(Query query) {
            this.query = query;
        }

        private SearchByIdCallable(String id) {
            this.query = new Query();
            query.addParameter("id", id);
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> results = new ArrayList<ImportRecord>();
            String id = query.getParameterAsClass("id", String.class);
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
                // open session
                method = new HttpGet(ENDPOINT_SEARCH_OPENAIRE + "?grantID=" + URLEncoder.encode(id));
                method.setConfig(requestConfigBuilder.build());
                // Execute the method.
                HttpResponse httpResponse = client.execute(method);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    throw new RuntimeException("WS call failed: " + statusCode);
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
            return results;
        }
    }

    private List<OMElement> splitToRecords(String recordsSrc) {
        OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(recordsSrc));
        OMElement element = records.getDocumentElement();
        AXIOMXPath xpath = null;
        try {
            xpath = new AXIOMXPath("/response/results/result");
            List<OMElement> recordsList = xpath.selectNodes(element);
            return recordsList;
        } catch (JaxenException e) {
            return null;
        }
    }
}
