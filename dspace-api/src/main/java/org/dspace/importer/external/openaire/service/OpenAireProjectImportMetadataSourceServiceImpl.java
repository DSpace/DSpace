/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.openaire.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.app.util.XMLUtils;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 * Implements a data source for querying OpenAIRE
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class OpenAireProjectImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<Element>
    implements QuerySource {

    private static final Logger log = LogManager.getLogger(OpenAireProjectImportMetadataSourceServiceImpl.class);
    private static final String ENDPOINT_SEARCH_OPENAIRE = "http://api.openaire.eu/search/projects";

    private int timeout = 1000;

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

    /**
     * Callable that searches OpenAIRE projects by name query.
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
            List<ImportRecord> results = new ArrayList<ImportRecord>();
            String queryString = query.getParameterAsClass("query", String.class);
            try {
                RequestConfig config = RequestConfig.custom()
                    .setConnectionRequestTimeout(timeout)
                    .build();
                try (CloseableHttpClient client =
                         DSpaceHttpClientFactory.getInstance().buildWithRequestConfig(config);
                     CloseableHttpResponse httpResponse =
                         client.execute(new HttpGet(
                             ENDPOINT_SEARCH_OPENAIRE + "?name=" + URLEncoder.encode(queryString)))) {
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK) {
                        throw new RuntimeException("WS call failed: " + statusCode);
                    }
                    InputStream is = httpResponse.getEntity().getContent();
                    String response = IOUtils.toString(is, Charsets.UTF_8);
                    List<Element> omElements = splitToRecords(response);
                    for (Element record : omElements) {
                        results.add(transformSourceRecords(record));
                    }
                }
            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
            }
            return results;
        }
    }

    /**
     * Callable that searches OpenAIRE projects by grant ID.
     */
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
            try {
                RequestConfig config = RequestConfig.custom()
                    .setConnectionRequestTimeout(timeout)
                    .build();
                try (CloseableHttpClient client =
                         DSpaceHttpClientFactory.getInstance().buildWithRequestConfig(config);
                     CloseableHttpResponse httpResponse =
                         client.execute(new HttpGet(
                             ENDPOINT_SEARCH_OPENAIRE + "?grantID=" + URLEncoder.encode(id)))) {
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK) {
                        throw new RuntimeException("WS call failed: " + statusCode);
                    }
                    InputStream is = httpResponse.getEntity().getContent();
                    String response = IOUtils.toString(is, Charsets.UTF_8);
                    List<Element> omElements = splitToRecords(response);
                    for (Element record : omElements) {
                        results.add(transformSourceRecords(record));
                    }
                }
            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
            }
            return results;
        }
    }

    /**
     * Parse an OpenAIRE XML response string and extract individual result elements.
     *
     * @param recordsSrc the raw XML response body
     * @return list of {@code result} elements, or an empty list on parse error
     */
    private List<Element> splitToRecords(String recordsSrc) {
        try {
            SAXBuilder saxBuilder = XMLUtils.getSAXBuilder();
            Document document = saxBuilder.build(new StringReader(recordsSrc));
            Element root = document.getRootElement();
            return root.getChildren("results").stream()
                .flatMap(results -> results.getChildren("result").stream())
                .collect(Collectors.toList());
        } catch (JDOMException | IOException e) {
            return new ArrayList<Element>();
        }
    }
}
