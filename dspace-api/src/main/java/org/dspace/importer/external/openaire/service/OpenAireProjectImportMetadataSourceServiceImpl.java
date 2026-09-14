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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
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
import org.dspace.services.ConfigurationService;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements a data source for querying OpenAIRE
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class OpenAireProjectImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<Element>
    implements QuerySource {

    private static final Logger log = LogManager.getLogger(OpenAireProjectImportMetadataSourceServiceImpl.class);

    private static final String DEFAULT_ENDPOINT_SEARCH_OPENAIRE = "https://api.openaire.eu/search/projects";

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    private String endpointSearchOpenAire;

    private int timeout = 1000;

    /**
     * Initialize the class
     *
     * @throws Exception on generic exception
     */
    @Override
    public void init() throws Exception {
        this.endpointSearchOpenAire = configurationService.getProperty("openaire.project.search.url",
                                                                       DEFAULT_ENDPOINT_SEARCH_OPENAIRE);
        this.timeout = configurationService.getIntProperty("openaire.project.timeout", timeout);
    }

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

    /**
     * Find the number of records matching a query.
     *
     * @param query a query string to base the search on.
     * @return the sum of the matching records over this import source
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query));
    }

    /**
     * Find the number of records matching a query.
     *
     * @param query a query object to base the search on.
     * @return the sum of the matching records over this import source
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query));
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query, count, start));
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        throw new UnsupportedOperationException("This method is not implemented for OpenAIRE projects");
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        throw new UnsupportedOperationException("This method is not implemented for OpenAIRE projects");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query));
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new UnsupportedOperationException("This method is not implemented for OpenAIRE projects");
    }

    /**
     * Build the {@link HttpGet} used to query the OpenAIRE projects search endpoint.
     *
     * @param queryString the search query (project name)
     * @param start       the offset of the first result to return (may be {@code null})
     * @param count       the maximum number of results to return (may be {@code null})
     * @return the configured {@link HttpGet}
     * @throws URISyntaxException if the endpoint URL cannot be parsed
     */
    private HttpGet buildSearchByNameRequest(String queryString, Integer start, Integer count)
        throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(endpointSearchOpenAire);
        uriBuilder.addParameter("name", queryString);
        // OpenAIRE search API paginates via "page" (1-based) and "size" parameters.
        if (count != null && count > 0) {
            int size = count;
            int offset = start != null && start > 0 ? start : 0;
            int page = (offset / size) + 1;
            uriBuilder.addParameter("page", String.valueOf(page));
            uriBuilder.addParameter("size", String.valueOf(size));
        }
        return new HttpGet(uriBuilder.build());
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
            Integer start = query.getParameterAsClass("start", Integer.class);
            Integer count = query.getParameterAsClass("count", Integer.class);
            try {
                RequestConfig config = RequestConfig.custom()
                    .setConnectionRequestTimeout(timeout)
                    .build();
                try (CloseableHttpClient client =
                         DSpaceHttpClientFactory.getInstance().buildWithRequestConfig(config);
                     CloseableHttpResponse httpResponse =
                         client.execute(buildSearchByNameRequest(queryString, start, count))) {
                    if (httpResponse.getStatusLine() == null) {
                        throw new IOException("WS call failed: no status line in the OpenAIRE response");
                    }
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK) {
                        throw new IOException("WS call failed: " + statusCode);
                    }
                    if (httpResponse.getEntity() == null) {
                        throw new IOException("WS call failed: empty entity in the OpenAIRE response");
                    }
                    InputStream is = httpResponse.getEntity().getContent();
                    String response = IOUtils.toString(is, Charsets.UTF_8);
                    List<Element> omElements = splitToRecords(response);
                    for (Element recordElement : omElements) {
                        results.add(transformSourceRecords(recordElement));
                    }
                }
            } catch (IOException | URISyntaxException e1) {
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
                URIBuilder uriBuilder = new URIBuilder(endpointSearchOpenAire);
                uriBuilder.addParameter("grantID", id);
                try (CloseableHttpClient client =
                         DSpaceHttpClientFactory.getInstance().buildWithRequestConfig(config);
                     CloseableHttpResponse httpResponse =
                         client.execute(new HttpGet(uriBuilder.build()))) {
                    if (httpResponse.getStatusLine() == null) {
                        throw new IOException("WS call failed: no status line in the OpenAIRE response");
                    }
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK) {
                        throw new IOException("WS call failed: " + statusCode);
                    }
                    if (httpResponse.getEntity() == null) {
                        throw new IOException("WS call failed: empty entity in the OpenAIRE response");
                    }
                    InputStream is = httpResponse.getEntity().getContent();
                    String response = IOUtils.toString(is, Charsets.UTF_8);
                    List<Element> omElements = splitToRecords(response);
                    for (Element recordElement : omElements) {
                        results.add(transformSourceRecords(recordElement));
                    }
                }
            } catch (IOException | URISyntaxException e1) {
                log.error(e1.getMessage(), e1);
            }
            return results;
        }
    }

    /**
     * Callable that counts the number of OpenAIRE projects matching a name query.
     */
    private class CountByQueryCallable implements Callable<Integer> {
        private String query;

        private CountByQueryCallable(String queryString) {
            this.query = queryString;
        }

        private CountByQueryCallable(Query query) {
            this.query = query.getParameterAsClass("query", String.class);
        }

        @Override
        public Integer call() throws Exception {
            try {
                RequestConfig config = RequestConfig.custom()
                    .setConnectionRequestTimeout(timeout)
                    .build();
                try (CloseableHttpClient client =
                         DSpaceHttpClientFactory.getInstance().buildWithRequestConfig(config);
                     CloseableHttpResponse httpResponse =
                         client.execute(buildSearchByNameRequest(query, null, null))) {
                    if (httpResponse.getStatusLine() == null) {
                        throw new IOException("WS call failed: no status line in the OpenAIRE response");
                    }
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK) {
                        throw new IOException("WS call failed: " + statusCode);
                    }
                    if (httpResponse.getEntity() == null) {
                        throw new IOException("WS call failed: empty entity in the OpenAIRE response");
                    }
                    InputStream is = httpResponse.getEntity().getContent();
                    String response = IOUtils.toString(is, Charsets.UTF_8);
                    return parseTotal(response);
                }
            } catch (IOException | URISyntaxException e1) {
                log.error(e1.getMessage(), e1);
            }
            return 0;
        }
    }

    /**
     * Parse the total number of results advertised in the OpenAIRE response header.
     *
     * @param recordsSrc the raw XML response body
     * @return the value of the {@code //header/total} element, or 0 if not present or on parse error
     */
    private int parseTotal(String recordsSrc) {
        try {
            SAXBuilder saxBuilder = XMLUtils.getSAXBuilder();
            Document document = saxBuilder.build(new StringReader(recordsSrc));
            Element root = document.getRootElement();
            Element header = root.getChild("header");
            if (header != null) {
                Element total = header.getChild("total");
                if (total != null && StringUtils.isNumeric(total.getTextTrim())) {
                    return Integer.parseInt(total.getTextTrim());
                }
            }
        } catch (JDOMException | IOException e) {
            log.error("Unable to parse OpenAIRE projects total from response", e);
        }
        return 0;
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
