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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import javax.el.MethodNotFoundException;

import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.scopus.service.LiveImportClient;
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

    private static final Logger log = Logger.getLogger(VuFindImportMetadataSourceServiceImpl.class);

    private static final String ENDPOINT_SEARCH = "https://vufind.org/advanced_demo/api/v1/search";
    private static final String ENDPOINT_RECORD = "https://vufind.org/advanced_demo/api/v1/record";

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
            URIBuilder uriBuilder = new URIBuilder(ENDPOINT_SEARCH);
            uriBuilder.addParameter("type", "AllField");
            uriBuilder.addParameter("page", String.valueOf(page));
            uriBuilder.addParameter("limit", count.toString());
            uriBuilder.addParameter("prettyPrint", String.valueOf(true));
            uriBuilder.addParameter("lookfor", query.getParameterAsClass("query", String.class));
            String responseString = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(),
                    new HashMap<String, String>());
            ReadContext ctx = JsonPath.parse(responseString);
            return ctx.read("$.resultCount");
        }
    }

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
            URIBuilder uriBuilder = new URIBuilder(ENDPOINT_RECORD);
            uriBuilder.addParameter("id", id);
            uriBuilder.addParameter("prettyPrint", "false");
            if (StringUtils.isNotBlank(fields)) {
                for (String field : fields.split(",")) {
                    uriBuilder.addParameter("field[]", field);
                }
            }
            String response = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(),
                    new HashMap<String, String>());
            return response;
        }
    }

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
            URIBuilder uriBuilder = new URIBuilder(ENDPOINT_SEARCH);
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
            return liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), new HashMap<String, String>());
        }

    }

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
            URIBuilder uriBuilder = new URIBuilder(ENDPOINT_RECORD);
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
            return liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), new HashMap<String, String>());
        }

    }

    private List<ImportRecord> extractMetadataFromRecordList(String records) {
        List<ImportRecord> recordsResult = new ArrayList<>();
        ReadContext ctx = JsonPath.parse(records);
        try {
            Object o = ctx.read("$.records[*]");
            if (o.getClass().isAssignableFrom(JSONArray.class)) {
                JSONArray array = (JSONArray)o;
                int size = array.size();
                for (int index = 0; index < size; index++) {
                    Gson gson = new Gson();
                    String innerJson = gson.toJson(array.get(index), LinkedHashMap.class);
                    recordsResult.add(transformSourceRecords(innerJson));
                }
            } else {
                recordsResult.add(transformSourceRecords(o.toString()));
            }
        } catch (Exception e) {
            log.error("Error reading data from VuFind " + e.getMessage(), e);
        }
        return recordsResult;
    }

}