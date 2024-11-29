/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ror.service;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.el.MethodNotFoundException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements a {@code AbstractImportMetadataSourceService} for querying ROR services.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class RorImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<String>
    implements QuerySource {

    private final static Logger log = LogManager.getLogger();
    protected static final String ROR_IDENTIFIER_PREFIX = "https://ror.org/";

    private String url;

    private int timeout = 1000;

    @Autowired
    private LiveImportClient liveImportClient;

    @Override
    public String getImportSource() {
        return "ror";
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByIdCallable(id));
        return CollectionUtils.isEmpty(records) ? null : records.get(0);
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query));
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query));
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query));
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query));
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByIdCallable(query));
        return CollectionUtils.isEmpty(records) ? null : records.get(0);
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for ROR");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for ROR");
    }

    @Override
    public void init() throws Exception {
    }

    /**
     * This class is a Callable implementation to get ROR entries based on query
     * object. This Callable use as query value the string queryString passed to
     * constructor. If the object will be construct through Query.class instance, a
     * Query's map entry with key "query" will be used. Pagination is supported too,
     * using the value of the Query's map with keys "start" and "count".
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
     */
    private class SearchByQueryCallable implements Callable<List<ImportRecord>> {

        private Query query;

        private SearchByQueryCallable(String queryString) {
            query = new Query();
            query.addParameter("query", queryString);
        }

        private SearchByQueryCallable(Query query) {
            this.query = query;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            return search(query.getParameterAsClass("query", String.class));
        }
    }

    /**
     * This class is a Callable implementation to get an ROR entry using bibcode The
     * bibcode to use can be passed through the constructor as a String or as
     * Query's map entry, with the key "id".
     *
     * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
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
            return searchById(query.getParameterAsClass("id", String.class));
        }
    }

    /**
     * This class is a Callable implementation to count the number of entries for a
     * ROR query. This Callable uses as query value to ROR the string queryString
     * passed to constructor. If the object will be construct through {@code Query}
     * instance, the value of the Query's map with the key "query" will be used.
     * 
     * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
     */
    private class CountByQueryCallable implements Callable<Integer> {
        private Query query;

        private CountByQueryCallable(String queryString) {
            query = new Query();
            query.addParameter("query", queryString);
        }

        private CountByQueryCallable(Query query) {
            this.query = query;
        }

        @Override
        public Integer call() throws Exception {
            return count(query.getParameterAsClass("query", String.class));
        }
    }

    /**
     * Counts the number of results for the given query.
     *
     * @param  query   the query string to count results for
     * @return        the number of results for the given query
     */
    public Integer count(String query) {
        try {
            Map<String, Map<String, String>> params = new HashMap<String, Map<String, String>>();

            URIBuilder uriBuilder = new URIBuilder(this.url);
            uriBuilder.addParameter("query", query);

            String resp = liveImportClient.executeHttpGetRequest(timeout, uriBuilder.toString(), params);
            if (StringUtils.isEmpty(resp)) {
                return 0;
            }
            JsonNode jsonNode = convertStringJsonToJsonNode(resp);
            return jsonNode.at("/number_of_results").asInt();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private List<ImportRecord> searchById(String id) {

        List<ImportRecord> importResults = new ArrayList<>();

        id = StringUtils.removeStart(id, ROR_IDENTIFIER_PREFIX);

        try {
            Map<String, Map<String, String>> params = new HashMap<String, Map<String, String>>();

            URIBuilder uriBuilder = new URIBuilder(this.url + "/" + id);

            String resp = liveImportClient.executeHttpGetRequest(timeout, uriBuilder.toString(), params);
            if (StringUtils.isEmpty(resp)) {
                return importResults;
            }

            JsonNode jsonNode = convertStringJsonToJsonNode(resp);
            importResults.add(transformSourceRecords(jsonNode.toString()));

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return importResults;
    }

    private List<ImportRecord> search(String query) {
        List<ImportRecord> importResults = new ArrayList<>();
        try {
            Map<String, Map<String, String>> params = new HashMap<String, Map<String, String>>();

            URIBuilder uriBuilder = new URIBuilder(this.url);
            uriBuilder.addParameter("query", query);

            String resp = liveImportClient.executeHttpGetRequest(timeout, uriBuilder.toString(), params);
            if (StringUtils.isEmpty(resp)) {
                return importResults;
            }

            JsonNode jsonNode = convertStringJsonToJsonNode(resp);
            JsonNode docs = jsonNode.at("/items");
            if (docs.isArray()) {
                Iterator<JsonNode> nodes = docs.elements();
                while (nodes.hasNext()) {
                    JsonNode node = nodes.next();
                    importResults.add(transformSourceRecords(node.toString()));
                }
            } else {
                importResults.add(transformSourceRecords(docs.toString()));
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return importResults;
    }

    private JsonNode convertStringJsonToJsonNode(String json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Unable to process json response.", e);
        }
        return null;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
