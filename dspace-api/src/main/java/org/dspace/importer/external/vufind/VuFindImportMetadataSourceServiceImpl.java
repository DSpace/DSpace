/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.vufind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.el.MethodNotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Implements a data source for querying VuFind
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class VuFindImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<String>
        implements QuerySource {

    private final static Logger log = LogManager.getLogger();

    private String url;
    private String urlSearch;

    private String fields;

    @Autowired
    private LiveImportClient liveImportClient;

    public VuFindImportMetadataSourceServiceImpl(String fields) {
        this.fields = fields;
    }

    @Override
    public String getImportSource() {
        return "VuFind";
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        String records = retry(new GetByVuFindIdCallable(id, fields));
        List<ImportRecord> importRecords = extractMetadataFromRecordList(records);
        return importRecords != null && !importRecords.isEmpty() ? importRecords.get(0) : null;
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
        String records = retry(new SearchByQueryCallable(query, count, start, fields));
        return extractMetadataFromRecordList(records);
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        String records = retry(new SearchByQueryCallable(query, fields));
        return extractMetadataFromRecordList(records);
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        String records = retry(new SearchByQueryCallable(query, fields));
        List<ImportRecord> importRecords = extractMetadataFromRecordList(records);
        return importRecords != null && !importRecords.isEmpty() ? importRecords.get(0) : null;
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        String records = retry(new FindMatchingRecordsCallable(query));
        return extractMetadataFromRecordList(records);
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for VuFind");
    }

    @Override
    public void init() throws Exception {}

    /**
     * This class is a Callable implementation to count the number of entries for an VuFind query.
     * This Callable use as query value to CrossRef the string queryString passed to constructor.
     * If the object will be construct through Query.class instance, the value of the Query's
     * map with the key "query" will be used.
     *
     * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
     */
    private class CountByQueryCallable implements Callable<Integer> {

        private Query query;

        public CountByQueryCallable(String queryString) {
            query = new Query();
            query.addParameter("query", queryString);
        }

        public CountByQueryCallable(Query query) {
            this.query = query;
        }

        @Override
        public Integer call() throws Exception {
            Integer start = 0;
            Integer count = 1;
            int page = start / count + 1;
            URIBuilder uriBuilder = new URIBuilder(urlSearch);
            uriBuilder.addParameter("type", "AllField");
            uriBuilder.addParameter("page", String.valueOf(page));
            uriBuilder.addParameter("limit", count.toString());
            uriBuilder.addParameter("prettyPrint", String.valueOf(true));
            uriBuilder.addParameter("lookfor", query.getParameterAsClass("query", String.class));
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            String responseString = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), params);
            JsonNode node = convertStringJsonToJsonNode(responseString);
            JsonNode resultCountNode = node.get("resultCount");
            return resultCountNode.intValue();
        }
    }

    /**
     * This class is a Callable implementation to get an VuFind entry using VuFind id
     * The id to use can be passed through the constructor as a String or as Query's map entry, with the key "id".
     *
     * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
     */
    private class GetByVuFindIdCallable implements Callable<String> {

        private String id;

        private String fields;

        public GetByVuFindIdCallable(String id, String fields) {
            this.id = id;
            if (fields != null && fields.length() > 0) {
                this.fields = fields;
            } else {
                this.fields = null;
            }
        }

        @Override
        public String call() throws Exception {
            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.addParameter("id", id);
            uriBuilder.addParameter("prettyPrint", "false");
            if (StringUtils.isNotBlank(fields)) {
                for (String field : fields.split(",")) {
                    uriBuilder.addParameter("field[]", field);
                }
            }
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            String response = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), params);
            return response;
        }
    }

    /**
     * This class is a Callable implementation to get VuFind entries based on query object.
     * This Callable use as query value the string queryString passed to constructor.
     * If the object will be construct through Query.class instance, a Query's map entry with key "query" will be used.
     * Pagination is supported too, using the value of the Query's map with keys "start" and "count".
     *
     * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
     */
    private class SearchByQueryCallable implements Callable<String> {

        private Query query;

        private String fields;

        public SearchByQueryCallable(String queryString, Integer maxResult, Integer start, String fields) {
            query = new Query();
            query.addParameter("query", queryString);
            query.addParameter("count", maxResult);
            query.addParameter("start", start);
            if (StringUtils.isNotBlank(fields)) {
                this.fields = fields;
            } else {
                this.fields = null;
            }
        }

        public SearchByQueryCallable(Query query, String fields) {
            this.query = query;
            if (StringUtils.isNotBlank(fields)) {
                this.fields = fields;
            } else {
                this.fields = null;
            }
        }

        @Override
        public String call() throws Exception {
            Integer start = query.getParameterAsClass("start", Integer.class);
            Integer count = query.getParameterAsClass("count", Integer.class);
            int page = count != 0 ? start / count : 0;
            URIBuilder uriBuilder = new URIBuilder(urlSearch);
            uriBuilder.addParameter("type", "AllField");
            //page looks 1 based (start = 0, count = 20 -> page = 0)
            uriBuilder.addParameter("page", String.valueOf(page + 1));
            uriBuilder.addParameter("limit", count.toString());
            uriBuilder.addParameter("prettyPrint", String.valueOf(true));
            uriBuilder.addParameter("lookfor", query.getParameterAsClass("query", String.class));
            if (StringUtils.isNotBlank(fields)) {
                for (String field : fields.split(",")) {
                    uriBuilder.addParameter("field[]", field);
                }
            }
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            return liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), params);
        }

    }

    /**
     * This class is a Callable implementation to search VuFind entries using author and title.
     *
     * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
     */
    public class FindMatchingRecordsCallable implements Callable<String> {

        private Query query;

        private String fields;

        public FindMatchingRecordsCallable(Query query) {
            this.query = query;
        }

        @Override
        public String call() throws Exception {
            String author = query.getParameterAsClass("author", String.class);
            String title = query.getParameterAsClass("title", String.class);
            Integer start = query.getParameterAsClass("start", Integer.class);
            Integer count = query.getParameterAsClass("count", Integer.class);
            int page = count != 0 ? start / count : 0;
            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.addParameter("type", "AllField");
            //pagination is 1 based (first page: start = 0, count = 20 -> page = 0 -> +1 = 1)
            uriBuilder.addParameter("page", String.valueOf(page ++));
            uriBuilder.addParameter("limit", count.toString());
            uriBuilder.addParameter("prettyPrint", "true");
            if (fields != null && !fields.isEmpty()) {
                for (String field : fields.split(",")) {
                    uriBuilder.addParameter("field[]", field);
                }
            }
            String filter = StringUtils.EMPTY;
            if (StringUtils.isNotBlank(author)) {
                filter = "author:" + author;
            }
            if (StringUtils.isNotBlank(title)) {
                if (StringUtils.isNotBlank(filter)) {
                    filter = filter + " AND title:" + title;
                } else {
                    filter = "title:" + title;
                }
            }
            uriBuilder.addParameter("lookfor", filter);
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            return liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), params);
        }

    }

    private JsonNode convertStringJsonToJsonNode(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode body = null;
        try {
            body = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Unable to process json response.", e);
        }
        return body;
    }

    private List<ImportRecord> extractMetadataFromRecordList(String records) {
        List<ImportRecord> recordsResult = new ArrayList<>();
        JsonNode jsonNode = convertStringJsonToJsonNode(records);
        JsonNode node = jsonNode.get("records");
        if (Objects.nonNull(node) && node.isArray()) {
            Iterator<JsonNode> nodes = node.iterator();
            while (nodes.hasNext()) {
                recordsResult.add(transformSourceRecords(nodes.next().toString()));
            }
        }
        return recordsResult;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrlSearch() {
        return urlSearch;
    }

    public void setUrlSearch(String urlSearch) {
        this.urlSearch = urlSearch;
    }

}