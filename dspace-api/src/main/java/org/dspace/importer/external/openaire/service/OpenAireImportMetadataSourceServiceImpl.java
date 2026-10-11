/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.openaire.service;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.util.XMLUtils;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.dspace.services.ConfigurationService;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements a data source for querying OpenAIRE
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 */
public class OpenAireImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<Element>
        implements QuerySource {

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    private String baseAddress;

    /**
     * Timeout in milliseconds for connecting to, and reading from, the OpenAIRE API
     */
    private static final int TIMEOUT_MS = 10000;

    @Autowired
    private LiveImportClient liveImportClient;

    private String queryParam;

    @Override
    public String getImportSource() {
        return "openaire";
    }

    /**
     * The string that identifies this import implementation. Preferable a URI
     *
     * @return the identifying uri
     */
    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        return retry(new SearchByIdCallable(id));
    }

    /**
     * The string that identifies this import implementation. Preferable a URI
     *
     * @return the identifying uri
     */
    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        return retry(new SearchByIdCallable(query));
    }


    /**
     * Find the number of records matching a query;
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
     * Find the number of records matching a query;
     *
     * @param query a query object to base the search on.
     * @return the sum of the matching records over this import source
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query));
    }

    /**
     * Find the number of records matching a string query. Supports pagination
     *
     * @param query a query string to base the search on.
     * @param start offset to start at
     * @param count number of records to retrieve.
     * @return a set of records. Fully transformed.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query, start, count));
    }


    /**
     * Find records based on a object query.
     *
     * @param query a query object to base the search on.
     * @return a set of records. Fully transformed.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query));
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        throw new UnsupportedOperationException("This method is not implemented for OpenAIRE");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new UnsupportedOperationException("This method is not implemented for OpenAIRE");
    }

    /**
     * Set the baseAddress to this object
     *
     * @param baseAddress The String object that represents the baseAddress of this object
     */
    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
    }

    /**
     * Return the baseAddress set to this object
     *
     * @return The String object that represents the baseAddress of this object
     */
    public String getBaseAddress() {
        return baseAddress;
    }

    /**
     * Set the name of the query param, this correspond to the index used (title, author)
     * 
     * @param queryParam on which index make the query
     */
    public void setQueryParam(String queryParam) {
        this.queryParam = queryParam;
    }

    /**
     * Get the name of the query param for the rest call
     * 
     * @return the name of the query param, i.e. the index (title, author) to use
     */
    public String getQueryParam() {
        return queryParam;
    }
    /**
     * Initialize the class
     *
     * @throws Exception on generic exception
     */
    @Override
    public void init() throws Exception {
        if (baseAddress == null) {
            baseAddress = configurationService.getProperty("openaire.search.url",
                                                           "https://api.openaire.eu/search/publications");
        }
        if (queryParam == null) {
            queryParam = "title";
        }
    }

    /**
     * Perform a GET request to the OpenAIRE search API.
     *
     * @param queryParams the query parameters, added to the URL in iteration order
     * @return the response body, or an empty string if the request failed
     */
    private String executeQuery(Map<String, String> queryParams) {
        Map<String, Map<String, String>> params = new HashMap<>();
        params.put(LiveImportClientImpl.URI_PARAMETERS, queryParams);
        return liveImportClient.executeHttpGetRequest(TIMEOUT_MS, baseAddress, params);
    }

    public class SearchByIdCallable  implements Callable<ImportRecord> {

        String id = null;

        public SearchByIdCallable(String id) {
            this.id = id;
        }

        public SearchByIdCallable(Query query) {
            this.id = query.getParameterAsClass("id", String.class);
        }

        @Override
        public ImportRecord call() throws Exception {
            List<ImportRecord> results = new ArrayList<ImportRecord>();
            String responseString = executeQuery(Map.of("openairePublicationID", id));
            if (StringUtils.isNotBlank(responseString)) {
                List<Element> omElements = splitToRecords(responseString);
                if (omElements != null) {
                    for (Element record : omElements) {
                        results.add(filterMultipleTitles(transformSourceRecords(record)));
                    }
                }
                return results != null ? results.get(0) : null;
            } else {
                return null;
            }
        }
    }

    public class CountByQueryCallable implements Callable<Integer> {

        String q;

        public CountByQueryCallable(String query) {
            q = query;
        }

        public CountByQueryCallable(Query query) {
            q = query.getParameterAsClass("query", String.class);
        }

        @Override
        public Integer call() throws Exception {
            String responseString = executeQuery(Map.of(queryParam, q));
            if (StringUtils.isNotBlank(responseString)) {

                SAXBuilder saxBuilder = XMLUtils.getSAXBuilder();
                Document document = saxBuilder.build(new StringReader(responseString));
                Element root = document.getRootElement();

                XPathExpression<Element> xpath = XPathFactory.instance().compile("//header/total",
                    Filters.element(), null);

                Element totalItem = xpath.evaluateFirst(root);
                return totalItem != null ? Integer.parseInt(totalItem.getText()) : null;

            } else {
                return 0;
            }
        }
    }

    public class SearchByQueryCallable implements Callable<List<ImportRecord>> {

        String q;
        int page;
        int count;

        public SearchByQueryCallable(String query, int start, int count) {
            this.q = query;
            this.page = start / count;
            this.count = count;
        }

        public SearchByQueryCallable(Query query) {
            this.q = query.getParameterAsClass("query", String.class);
            this.page = query.getParameterAsClass("start", Integer.class) /
                query.getParameterAsClass("count", Integer.class);
            this.count = query.getParameterAsClass("count", Integer.class);
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            Map<String, String> queryParams = new LinkedHashMap<>();
            queryParams.put(queryParam, q);
            queryParams.put("page", String.valueOf(page + 1));
            queryParams.put("size", String.valueOf(count));
            List<ImportRecord> results = new ArrayList<ImportRecord>();
            String responseString = executeQuery(queryParams);
            if (StringUtils.isNotBlank(responseString)) {
                List<Element> omElements = splitToRecords(responseString);
                if (omElements != null) {
                    for (Element record : omElements) {
                        results.add(filterMultipleTitles(transformSourceRecords(record)));
                    }
                }
            }
            return results;
        }
    }

    /**
     * This method remove multiple titles occurrences
     * 
     * @param transformSourceRecords
     * @return ImportRecord with one or zero title
     */
    private ImportRecord filterMultipleTitles(ImportRecord transformSourceRecords) {
        List<MetadatumDTO> metadata = (List<MetadatumDTO>)transformSourceRecords.getValueList();
        ArrayList<MetadatumDTO> nextSourceRecord = new ArrayList<>();
        boolean found = false;
        for (MetadatumDTO dto : metadata) {
            if ("dc".equals(dto.getSchema()) && "title".equals(dto.getElement()) && dto.getQualifier() == null) {
                if (!found) {
                    nextSourceRecord.add(dto);
                    found = true;
                }
            } else {
                nextSourceRecord.add(dto);
            }
        }
        return new ImportRecord(nextSourceRecord);
    }

    private List<Element> splitToRecords(String recordsSrc) {

        try {
            SAXBuilder saxBuilder = XMLUtils.getSAXBuilder();
            Document document = saxBuilder.build(new StringReader(recordsSrc));
            Element root = document.getRootElement();

            List namespaces = Arrays.asList(
                Namespace.getNamespace("dri", "http://www.driver-repository.eu/namespace/dri"),
                Namespace.getNamespace("oaf", "http://namespace.openaire.eu/oaf"),
                Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance"));
            XPathExpression<Element> xpath = XPathFactory.instance().compile("//results/result",
                Filters.element(), null, namespaces);

            List<Element> recordsList = xpath.evaluate(root);
            return recordsList;
        } catch (JDOMException | IOException e) {
            return null;
        }
    }



}
