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
import jakarta.el.MethodNotFoundException;
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
import org.springframework.beans.factory.annotation.Autowired;


/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class OpenAlexImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<String>
    implements OpenAlexImportMetadataSourceService {

    private final static Logger log = LogManager.getLogger();
    private final int timeout = 1000;
    private String url;

    @Autowired
    private LiveImportClient liveImportClient;

    @Override
    public String getImportSource() {
        return "openalex";
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        if (id == null) {
            throw new MetadataSourceException("ID cannot be null");
        }
        List<ImportRecord> records = retry(new SearchByIdCallable(id));
        return CollectionUtils.isEmpty(records) ? null : records.get(0);
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        if (query == null) {
            throw new MetadataSourceException("Query cannot be null");
        }
        return retry(new CountByQueryCallable(query));
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        if (query == null) {
            throw new MetadataSourceException("Query cannot be null");
        }
        return retry(new CountByQueryCallable(query));
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        if (query == null) {
            throw new MetadataSourceException("Query cannot be null");
        }
        return retry(new SearchByQueryCallable(query, start, count));
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for OpenAlex");
    }

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
        throw new MethodNotFoundException("This method is not implemented for OpenAlex");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for OpenAlex");
    }

    @Override
    public void init() throws Exception {
        if (liveImportClient == null) {
            throw new IllegalStateException("LiveImportClient not properly initialized");
        }
        if (StringUtils.isBlank(url)) {
            throw new IllegalStateException("URL not properly configured");
        }
    }

    public Integer count(String query) throws MetadataSourceException {
        if (query == null) {
            throw new MetadataSourceException("Query cannot be null");
        }
        Map<String, Map<String, String>> params = new HashMap<>();
        Map<String, String> uriParams = new HashMap<>();
        params.put(LiveImportClientImpl.URI_PARAMETERS, uriParams);
        try {
            uriParams.put("search", query);
            String resp = liveImportClient.executeHttpGetRequest(timeout, this.url, params);
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

    private List<ImportRecord> searchById(String id) {
        List<ImportRecord> results = new ArrayList<>();
        try {
            String resp = liveImportClient.executeHttpGetRequest(timeout, this.url + "/" + id, new HashMap<>());
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

        try {
            uriParams.put("search", query);
            if (page != null) {
                uriParams.put("page", String.valueOf(page + 1));
            }
            if (pageSize != null) {
                uriParams.put("per_page", String.valueOf(pageSize));
            }

            String resp = liveImportClient.executeHttpGetRequest(timeout, this.url, params);
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