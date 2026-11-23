/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ror.service;

import static org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl.HEADER_PARAMETERS;

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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements a {@code AbstractImportMetadataSourceService} for querying ROR services.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class RorImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<String>
    implements RorImportMetadataSourceService {

    private final static Logger log = LogManager.getLogger();
    protected static final String ROR_IDENTIFIER_PREFIX = "https://ror.org/";
    protected static final String ROR_CLIENT_ID_HEADER = "Client-Id";
    protected static final String ROR_CLIENT_ID_PROP = "ror.client-id";

    // The ROR v2 API returns a fixed number of results per page (controlled by the "page" parameter)
    protected static final int ROR_PAGE_SIZE = 20;

    private String url;

    private int timeout = 5000;

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
        return retry(new SearchByQueryCallable(query, start, count));
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
        throw new UnsupportedOperationException("This method is not implemented for ROR");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new UnsupportedOperationException("This method is not implemented for ROR");
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

        private SearchByQueryCallable(String queryString, int start, int count) {
            query = new Query();
            query.addParameter("query", queryString);
            query.addParameter("start", start);
            query.addParameter("count", count);
        }

        private SearchByQueryCallable(Query query) {
            this.query = query;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            String queryString = query.getParameterAsClass("query", String.class);
            Integer start = query.getParameterAsClass("start", Integer.class);
            Integer count = query.getParameterAsClass("count", Integer.class);
            return search(queryString, start, count);
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
            Map<String, Map<String, String>> params = getBaseParams();

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

        id = Strings.CS.removeStart(id, ROR_IDENTIFIER_PREFIX);

        try {
            Map<String, Map<String, String>> params = getBaseParams();

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
        return search(query, null, null);
    }

    /**
     * Search the ROR API for the given query, honouring the requested pagination window.
     *
     * <p>The ROR v2 API is page-based with a fixed page size of {@value #ROR_PAGE_SIZE} results per
     * page (controlled by the {@code page} query parameter, 1-based). DSpace, however, requests an
     * offset-based window defined by {@code start} (0-based offset) and {@code count} (page size).
     * To bridge the two, this method fetches every ROR page that overlaps the requested window and
     * then slices the aggregated records down to exactly {@code [start, start + count)}.</p>
     *
     * <p>When {@code start}/{@code count} are {@code null} or {@code count <= 0}, the first ROR page
     * is returned unsliced (legacy behaviour).</p>
     *
     * @param query the query string
     * @param start the 0-based offset of the first record to return, or {@code null}
     * @param count the maximum number of records to return, or {@code null}
     * @return the windowed list of import records
     */
    private List<ImportRecord> search(String query, Integer start, Integer count) {
        List<ImportRecord> importResults = new ArrayList<>();

        int offset = start != null && start > 0 ? start : 0;
        int limit = count != null ? count : 0;

        // Without a positive limit we cannot bound the window: fetch the first page as-is
        if (limit <= 0) {
            collectPage(query, 1, importResults);
            return importResults;
        }

        // ROR pages are 1-based and fixed-size; compute the page range covering [offset, offset+limit)
        int firstPage = (offset / ROR_PAGE_SIZE) + 1;
        int lastPage = ((offset + limit - 1) / ROR_PAGE_SIZE) + 1;

        List<ImportRecord> aggregated = new ArrayList<>();
        for (int page = firstPage; page <= lastPage; page++) {
            int before = aggregated.size();
            collectPage(query, page, aggregated);
            // Stop early if the page came back short (or empty): there are no further results
            if (aggregated.size() - before < ROR_PAGE_SIZE) {
                break;
            }
        }

        // Slice the aggregated pages down to the exact requested window
        int fromIndex = Math.min(offset - ((firstPage - 1) * ROR_PAGE_SIZE), aggregated.size());
        int toIndex = Math.min(fromIndex + limit, aggregated.size());
        importResults.addAll(aggregated.subList(fromIndex, toIndex));

        return importResults;
    }

    /**
     * Fetch a single ROR result page and append its parsed records to the given list.
     *
     * @param query   the query string
     * @param page    the 1-based ROR page number to fetch
     * @param results the list to append parsed records to
     */
    private void collectPage(String query, int page, List<ImportRecord> results) {
        try {
            Map<String, Map<String, String>> params = getBaseParams();

            URIBuilder uriBuilder = new URIBuilder(this.url);
            uriBuilder.addParameter("query", query);
            if (page > 1) {
                uriBuilder.addParameter("page", String.valueOf(page));
            }

            String resp = liveImportClient.executeHttpGetRequest(timeout, uriBuilder.toString(), params);
            if (StringUtils.isEmpty(resp)) {
                return;
            }

            JsonNode jsonNode = convertStringJsonToJsonNode(resp);
            JsonNode docs = jsonNode.at("/items");
            if (docs.isArray()) {
                Iterator<JsonNode> nodes = docs.elements();
                while (nodes.hasNext()) {
                    JsonNode node = nodes.next();
                    results.add(transformSourceRecords(node.toString()));
                }
            } else {
                results.add(transformSourceRecords(docs.toString()));
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    protected Map<String, Map<String, String>> getBaseParams() {
        Map<String, Map<String, String>> params = new HashMap<>();
        String rorClientId =
            DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(ROR_CLIENT_ID_PROP);
        if (StringUtils.isNotEmpty(rorClientId)) {
            params.put(HEADER_PARAMETERS, Map.of(ROR_CLIENT_ID_HEADER, rorClientId));
        }
        return params;
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
