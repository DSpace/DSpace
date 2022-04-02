/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.crossref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.el.MethodNotFoundException;

import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.scopus.service.LiveImportClient;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.DoiCheck;
import org.dspace.importer.external.service.components.QuerySource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements a data source for querying CrossRef
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class CrossRefImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<String>
        implements QuerySource {

    private static final String ENDPOINT_WORKS = "https://api.crossref.org/works";

    @Autowired
    private LiveImportClient liveImportClient;

    @Override
    public String getImportSource() {
        return "crossref";
    }

    @Override
    public void init() throws Exception {}

    @Override
    public ImportRecord getRecord(String recordId) throws MetadataSourceException {
        List<ImportRecord> records = null;
        String id = getID(recordId);
        if (StringUtils.isNotBlank(id)) {
            records = retry(new SearchByIdCallable(id));
        } else {
            records = retry(new SearchByIdCallable(recordId));
        }
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        String id = getID(query);
        if (StringUtils.isNotBlank(id)) {
            return retry(new DoiCheckCallable(id));
        }
        return retry(new CountByQueryCallable(query));
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        String id = getID(query.toString());
        if (StringUtils.isNotBlank(id)) {
            return retry(new DoiCheckCallable(id));
        }
        return retry(new CountByQueryCallable(query));
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        String id = getID(query.toString());
        if (StringUtils.isNotBlank(id)) {
            return retry(new SearchByIdCallable(id));
        }
        return retry(new SearchByQueryCallable(query, count, start));
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        String id = getID(query.toString());
        if (StringUtils.isNotBlank(id)) {
            return retry(new SearchByIdCallable(id));
        }
        return retry(new SearchByQueryCallable(query));
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        List<ImportRecord> records = null;
        String id = getID(query.toString());
        if (StringUtils.isNotBlank(id)) {
            records = retry(new SearchByIdCallable(id));
        } else {
            records = retry(new SearchByIdCallable(query));
        }
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        String id = getID(query.toString());
        if (StringUtils.isNotBlank(id)) {
            return retry(new SearchByIdCallable(id));
        }
        return retry(new FindMatchingRecordCallable(query));
    }


    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for CrossRef");
    }

    public String getID(String query) {
        if (DoiCheck.isDoi(query)) {
            return "filter=doi:" + query;
        }
        return StringUtils.EMPTY;
    }

    private class SearchByQueryCallable implements Callable<List<ImportRecord>> {

        private Query query;

        private SearchByQueryCallable(String queryString, Integer maxResult, Integer start) {
            query = new Query();
            query.addParameter("query", queryString);
            query.addParameter("count", maxResult);
            query.addParameter("start", start);
        }

        private SearchByQueryCallable(Query query) {
            this.query = query;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> results = new ArrayList<>();
            Integer count = query.getParameterAsClass("count", Integer.class);
            Integer start = query.getParameterAsClass("start", Integer.class);

            URIBuilder uriBuilder = new URIBuilder(ENDPOINT_WORKS);
            uriBuilder.addParameter("query", query.getParameterAsClass("query", String.class));
            if (Objects.nonNull(count)) {
                uriBuilder.addParameter("rows", count.toString());
            }
            if (Objects.nonNull(start)) {
                uriBuilder.addParameter("offset", start.toString());
            }

            String response = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(),
                    new HashMap<String, String>());
            ReadContext ctx = JsonPath.parse(response);
            Object o = ctx.read("$.message.items");
            if (o.getClass().isAssignableFrom(JSONArray.class)) {
                JSONArray array = (JSONArray) o;
                int size = array.size();
                for (int index = 0; index < size; index++) {
                    Gson gson = new Gson();
                    String innerJson = gson.toJson(array.get(index), LinkedHashMap.class);
                    results.add(transformSourceRecords(innerJson));
                }
            } else {
                results.add(transformSourceRecords(o.toString()));
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
            List<ImportRecord> results = new ArrayList<>();
            URIBuilder uriBuilder = new URIBuilder(
                    ENDPOINT_WORKS + "/" + query.getParameterAsClass("id", String.class));
            String responseString = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(),
                    new HashMap<String, String>());
            ReadContext ctx = JsonPath.parse(responseString);
            Object o = ctx.read("$.message");
            if (o.getClass().isAssignableFrom(JSONArray.class)) {
                JSONArray array = (JSONArray) o;
                int size = array.size();
                for (int index = 0; index < size; index++) {
                    Gson gson = new Gson();
                    String innerJson = gson.toJson(array.get(index), LinkedHashMap.class);
                    results.add(transformSourceRecords(innerJson));
                }
            } else {
                Gson gson = new Gson();
                results.add(transformSourceRecords(gson.toJson(o, Object.class)));
            }
            return results;
        }
    }

    private class FindMatchingRecordCallable implements Callable<List<ImportRecord>> {

        private Query query;

        private FindMatchingRecordCallable(Query q) {
            query = q;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            String queryValue = query.getParameterAsClass("query", String.class);
            Integer count = query.getParameterAsClass("count", Integer.class);
            Integer start = query.getParameterAsClass("start", Integer.class);
            String author = query.getParameterAsClass("author", String.class);
            String title = query.getParameterAsClass("title", String.class);
            String bibliographics = query.getParameterAsClass("bibliographics", String.class);
            List<ImportRecord> results = new ArrayList<>();
            URIBuilder uriBuilder = new URIBuilder(ENDPOINT_WORKS);
            if (Objects.nonNull(queryValue)) {
                uriBuilder.addParameter("query", queryValue);
            }
            if (Objects.nonNull(count)) {
                uriBuilder.addParameter("rows", count.toString());
            }
            if (Objects.nonNull(start)) {
                uriBuilder.addParameter("offset", start.toString());
            }
            if (Objects.nonNull(author)) {
                uriBuilder.addParameter("query.author", author);
            }
            if (Objects.nonNull(title )) {
                uriBuilder.addParameter("query.container-title", title);
            }
            if (Objects.nonNull(bibliographics)) {
                uriBuilder.addParameter("query.bibliographic", bibliographics);
            }

            String resp = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(),
                    new HashMap<String, String>());
            ReadContext ctx = JsonPath.parse(resp);
            Object o = ctx.read("$.message.items[*]");
            if (o.getClass().isAssignableFrom(JSONArray.class)) {
                JSONArray array = (JSONArray) o;
                int size = array.size();
                for (int index = 0; index < size; index++) {
                    Gson gson = new Gson();
                    String innerJson = gson.toJson(array.get(index), LinkedHashMap.class);
                    results.add(transformSourceRecords(innerJson));
                }
            } else {
                results.add(transformSourceRecords(o.toString()));
            }
            return results;
        }

    }

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
            URIBuilder uriBuilder = new URIBuilder(ENDPOINT_WORKS);
            uriBuilder.addParameter("query", query.getParameterAsClass("query", String.class));
            String responseString = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(),
                    new HashMap<String, String>());
            ReadContext ctx = JsonPath.parse(responseString);
            return ctx.read("$.message.total-results");
        }
    }

    private class DoiCheckCallable implements Callable<Integer> {

        private final Query query;

        private DoiCheckCallable(final String id) {
            final Query query = new Query();
            query.addParameter("id", id);
            this.query = query;
        }

        private DoiCheckCallable(final Query query) {
            this.query = query;
        }

        @Override
        public Integer call() throws Exception {
            URIBuilder uriBuilder = new URIBuilder(
                    ENDPOINT_WORKS + "/" + query.getParameterAsClass("id", String.class));
            String responseString = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(),
                    new HashMap<String, String>());
            ReadContext ctx = JsonPath.parse(responseString);
            return StringUtils.equals(ctx.read("$.status"), "ok") ? 1 : 0;
        }
    }

}