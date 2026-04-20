/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.openalex.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.DoiCheck;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OpenAlexImportMetadataSourceService} that provides
 * metadata import functionality from OpenAlex into DSpace.
 *
 * This class interacts with the OpenAlex API to fetch metadata records based on
 * queries or identifiers. It uses {@link LiveImportClient} to perform HTTP requests
 * and processes responses to extract metadata.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class OpenAlexImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<String>
    implements OpenAlexImportMetadataSourceService {

    private final static Logger log = LogManager.getLogger();
    private static final String URL_FILTER_AUTHORSHIPS_AUTHOR_ID = "?filter=authorships.author.id:";
    private static final String PARAM_FILTER_AUTHORSHIPS_AUTHOR_ID = "authorships.author.id:";
    private static final String URL_FILTER_DOI = "?filter=doi:";
    private static final String PARAM_FILTER_DOI = "doi:";
    private final int timeout = 1000;
    private String url;

    @Autowired
    private LiveImportClient liveImportClient;

    /**
     * Returns the source name of the metadata provider.
     *
     * @return The string "openalex".
     */
    @Override
    public String getImportSource() {
        return "openalex";
    }

    /**
     * Retrieves a metadata record by its OpenAlex identifier.
     *
     * @param id The identifier of the record to fetch.
     * @return The corresponding {@link ImportRecord} or null if not found.
     * @throws MetadataSourceException If the ID is null or an error occurs.
     */
    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        if (id == null) {
            throw new MetadataSourceException("ID cannot be null");
        }
        List<ImportRecord> records = retry(new SearchByIdCallable(id));
        return CollectionUtils.isEmpty(records) ? null : records.get(0);
    }

    /**
     * Counts the number of metadata records matching a query.
     *
     * @param query a query string to base the search on.
     * @return The number of matching records.
     * @throws MetadataSourceException If the query is null or an error occurs.
     */
    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        if (query == null) {
            throw new MetadataSourceException("Query cannot be null");
        }
        return retry(new CountByQueryCallable(query));
    }

    /**
     * Counts the number of metadata records matching a query.
     *
     * @param query a query object to base the search on.
     * @return The number of matching records.
     * @throws MetadataSourceException If the query is null or an error occurs.
     */
    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        if (query == null) {
            throw new MetadataSourceException("Query cannot be null");
        }
        return retry(new CountByQueryCallable(query));
    }

    /**
     * Retrieves a collection of metadata records based on a query with pagination.
     *
     * @param query a query string to base the search on.
     * @param start The starting index for pagination.
     * @param count The maximum number of records to return.
     * @return A collection of matching {@link ImportRecord}.
     * @throws MetadataSourceException If the query is null or an error occurs.
     */
    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        if (query == null) {
            throw new MetadataSourceException("Query cannot be null");
        }
        return retry(new SearchByQueryCallable(query, start, count));
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        throw new UnsupportedOperationException("This method is not implemented for OpenAlex");
    }

    /**
     * Retrieves a metadata record by its identifier.
     *
     * @param query a query object containing the identifier of the record to fetch.
     * @return The corresponding {@link ImportRecord} or null if not found.
     * @throws MetadataSourceException If the query is null or an error occurs.
     */
    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        if (query == null) {
            throw new MetadataSourceException("Query cannot be null");
        }
        List<ImportRecord> records = retry(new SearchByIdCallable(query));
        return CollectionUtils.isEmpty(records) ? null : records.get(0);
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        throw new UnsupportedOperationException("This method is not implemented for OpenAlex");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new UnsupportedOperationException("This method is not implemented for OpenAlex");
    }

    /**
     * Initializes the service by validating necessary configurations.
     *
     * @throws Exception If required properties are not properly set.
     */
    @Override
    public void init() throws Exception {
        if (liveImportClient == null) {
            throw new IllegalStateException("LiveImportClient not properly initialized");
        }
        if (StringUtils.isBlank(url)) {
            throw new IllegalStateException("URL not properly configured");
        }
    }

    private Integer count(String query) throws MetadataSourceException {
        if (query == null) {
            throw new MetadataSourceException("Query cannot be null");
        }
        Map<String, Map<String, String>> params = new HashMap<>();
        Map<String, String> uriParams = new HashMap<>();
        params.put(LiveImportClientImpl.URI_PARAMETERS, uriParams);
        String queryUrl = url;
        try {
            if (queryUrl.contains(URL_FILTER_AUTHORSHIPS_AUTHOR_ID)) {
                queryUrl = queryUrl.replace(URL_FILTER_AUTHORSHIPS_AUTHOR_ID, "");
                uriParams.put("filter", PARAM_FILTER_AUTHORSHIPS_AUTHOR_ID + query);
            } else if (queryUrl.contains(URL_FILTER_DOI)) {
                queryUrl = queryUrl.replace(URL_FILTER_DOI, "");
                if (DoiCheck.isDoi(query)) {
                    uriParams.put("filter", PARAM_FILTER_DOI + DoiCheck.purgeDoiValue(query));
                }
            } else if (queryUrl.contains("sources")) {
                uriParams.put("filter", "type:journal,default.search:" + query);
            } else {
                uriParams.put("search", query);
            }
            String resp = liveImportClient.executeHttpGetRequest(timeout, queryUrl, params);
            if (StringUtils.isEmpty(resp)) {
                log.error("Got an empty response from LiveImportClient for query: {}", query);
                return 0;
            }
            JsonNode jsonNode = convertStringJsonToJsonNode(resp);
            if (jsonNode != null && jsonNode.hasNonNull("meta")
                && jsonNode.at("/meta/count").isNumber()) {
                return jsonNode.at("/meta/count").asInt();
            }
        } catch (Exception e) {
            log.error("Error executing count query", e);
        }
        return 0;
    }

    /**
     * Searches for a record by its ID.
     *
     * @param id The identifier of the record.
     * @return A list containing the matching {@link ImportRecord}, or empty if none found.
     */
    private List<ImportRecord> searchById(String id) {
        List<ImportRecord> results = new ArrayList<>();
        try {
            String queryUrl = url;
            if (queryUrl.contains(URL_FILTER_AUTHORSHIPS_AUTHOR_ID)) {
                queryUrl = queryUrl.replace(URL_FILTER_AUTHORSHIPS_AUTHOR_ID, "");
            } else if (queryUrl.contains(URL_FILTER_DOI)) {
                queryUrl = queryUrl.replace(URL_FILTER_DOI, "");
            }
            String resp = liveImportClient.executeHttpGetRequest(timeout, queryUrl + "/" + id, new HashMap<>());
            if (StringUtils.isEmpty(resp)) {
                return results;
            }
            JsonNode jsonNode = convertStringJsonToJsonNode(resp);
            if (jsonNode != null) {
                ImportRecord record = transformSourceRecords(jsonNode.toString());
                if (record != null) {
                    results.add(record);
                }
            }
        } catch (Exception e) {
            log.error("Error searching by ID: {}", id, e);
        }
        return results;
    }

    private List<ImportRecord> search(String query, Integer page, Integer pageSize) {
        List<ImportRecord> results = new ArrayList<>();
        Map<String, Map<String, String>> params = new HashMap<>();
        Map<String, String> uriParams = new HashMap<>();
        params.put(LiveImportClientImpl.URI_PARAMETERS, uriParams);
        String queryUrl = url;
        try {
            if (queryUrl.contains(URL_FILTER_AUTHORSHIPS_AUTHOR_ID)) {
                queryUrl = queryUrl.replace(URL_FILTER_AUTHORSHIPS_AUTHOR_ID, "");
                uriParams.put("filter", PARAM_FILTER_AUTHORSHIPS_AUTHOR_ID + query);
            } else if (queryUrl.contains(URL_FILTER_DOI)) {
                queryUrl = queryUrl.replace(URL_FILTER_DOI, "");
                if (DoiCheck.isDoi(query)) {
                    uriParams.put("filter", PARAM_FILTER_DOI + DoiCheck.purgeDoiValue(query));
                }
            } else if (queryUrl.contains("sources")) {
                uriParams.put("filter", "type:journal,default.search:" + query);
            } else {
                uriParams.put("search", query);
            }
            if (page != null) {
                uriParams.put("page", String.valueOf(page + 1));
            }
            if (pageSize != null) {
                uriParams.put("per_page", String.valueOf(pageSize));
            }

            String resp = liveImportClient.executeHttpGetRequest(timeout, queryUrl, params);
            if (StringUtils.isEmpty(resp)) {
                return results;
            }

            JsonNode jsonNode = convertStringJsonToJsonNode(resp);
            if (jsonNode != null) {
                JsonNode docs = jsonNode.at("/results");
                if (docs != null && docs.isArray()) {
                    for (JsonNode node : docs) {
                        if (node != null) {
                            ImportRecord record = transformSourceRecords(node.toString());
                            if (record != null) {
                                results.add(record);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error executing search query", e);
        }
        return results;
    }


    private JsonNode convertStringJsonToJsonNode(String json) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        try {
            return new ObjectMapper().readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Unable to process JSON response", e);
            return null;
        }
    }

    /**
     * Sets the base URL for OpenAlex API requests.
     *
     * @param url The OpenAlex API base URL.
     */
    public void setUrl(String url) {
        this.url = StringUtils.trimToNull(url);
    }

    private class SearchByQueryCallable implements Callable<List<ImportRecord>> {
        private final Query query;

        private SearchByQueryCallable(String queryString, int start, int count) {
            query = new Query();
            query.addParameter("query", queryString);
            query.addParameter("page", start / count);
            query.addParameter("count", count);
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            String queryString = query.getParameterAsClass("query", String.class);
            if (queryString == null) {
                throw new MetadataSourceException("Query cannot be null");
            }
            return search(queryString,
                          query.getParameterAsClass("page", Integer.class),
                          query.getParameterAsClass("count", Integer.class));
        }
    }

    private class SearchByIdCallable implements Callable<List<ImportRecord>> {
        private final Query query;

        private SearchByIdCallable(String id) {
            this.query = new Query();
            query.addParameter("id", id);
        }

        private SearchByIdCallable(Query query) {
            this.query = query;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            String id = query.getParameterAsClass("id", String.class);
            if (id == null) {
                throw new MetadataSourceException("Id cannot be null");
            }
            return searchById(id);
        }
    }

    private class CountByQueryCallable implements Callable<Integer> {
        private final Query query;

        private CountByQueryCallable(String queryString) {
            query = new Query();
            query.addParameter("query", queryString);
        }

        private CountByQueryCallable(Query query) {
            this.query = query;
        }

        @Override
        public Integer call() throws Exception {
            String queryString = query.getParameterAsClass("query", String.class);
            if (queryString == null) {
                throw new MetadataSourceException("Query cannot be null");
            }
            return count(queryString);
        }
    }
}