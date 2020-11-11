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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.el.MethodNotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import net.minidev.json.JSONArray;
import org.apache.http.client.methods.HttpGet;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;


/**
 * Implements a data source for querying CrossRef
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public class CrossRefImportMetadataSourceServiceImpl
    extends AbstractImportMetadataSourceService<String> implements QuerySource {

    private WebTarget webTarget;

    @Override
    public String getImportSource() {
        return "crossref";
    }

    @Override
    public void init() throws Exception {
        Client client = ClientBuilder.newClient();
        webTarget = client.target("https://api.crossref.org/works");
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByIdCallable(id));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        //TODO if a doi check if exists
        if (new CrossRefDoiCheck(webTarget).isDoi(query)) {
            return retry(new DoiCheckCallable(query));
        }
        return retry(new CountByQueryCallable(query));
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        //TODO if a doi check if exists (making a head http request to the https://api.crossref.org/works/<id>)

        final boolean isDoi = doi(query);

        if (isDoi) {
            return retry(new DoiCheckCallable(query));
        }
        return retry(new CountByQueryCallable(query));
    }


    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        //TODO if a doi call SearchByIdCallable
        if (new CrossRefDoiCheck(webTarget).isDoi(query)) {
            return retry(new SearchByIdCallable(query));
        }
        return retry(new SearchByQueryCallable(query, count, start));
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        //TODO if a doi call SearchByIdCallable
        final boolean isDoi = doi(query);
        if (isDoi) {
            return retry(new SearchByIdCallable(query));
        }
        return retry(new SearchByQueryCallable(query));
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByIdCallable(query));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        //TODO if a doi call SearchByIdCallable
        final Entry entry = (Entry) query.getParameters().entrySet().iterator().next();
        if (new CrossRefDoiCheck(webTarget).isDoi((String) entry.getValue())) {
            return retry(new FindMatchingRecordCallable(query));
        }
        return Collections.emptyList();
    }


    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for CrossRef");
    }

    private boolean doi(final Query query) {
        return Optional.ofNullable(query.getParameter("id"))
                       .filter(c -> !c.isEmpty())
                       .map(c -> c.iterator().next())
                       .map(o -> (String) o)
                       .filter(value -> new CrossRefDoiCheck(webTarget).isDoi(value))
                       .isPresent();
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
            HttpGet method = null;
            try {
                Integer count = query.getParameterAsClass("count", Integer.class);
                Integer start = query.getParameterAsClass("start", Integer.class);
                WebTarget local = webTarget.queryParam("query", query.getParameterAsClass("query", String.class));
                if (count != null) {
                    local = local.queryParam("rows", count);
                }
                if (start != null) {
                    local = local.queryParam("offset", start);
                }
                Invocation.Builder invocationBuilder = local.request();
                Response response = invocationBuilder.get();
                if (response.getStatus() != 200) {
                    return null;
                }
                String responseString = response.readEntity(String.class);
                ReadContext ctx = JsonPath.parse(responseString);
                Object o = ctx.read("$.message.items[*]");
                if (o.getClass().isAssignableFrom(JSONArray.class)) {
                    JSONArray array = (JSONArray)o;
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
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
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
            HttpGet method = null;
            try {
                WebTarget local = webTarget.path(query.getParameterAsClass("id", String.class));
                Invocation.Builder invocationBuilder = local.request();
                Response response = invocationBuilder.get();
                if (response.getStatus() != 200) {
                    return null;
                }
                String responseString = response.readEntity(String.class);
                ReadContext ctx = JsonPath.parse(responseString);
                Object o = ctx.read("$.message");
                if (o.getClass().isAssignableFrom(JSONArray.class)) {
                    JSONArray array = (JSONArray)o;
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
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
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
            HttpGet method = null;
            try {
                WebTarget local = webTarget;
                if (queryValue != null) {
                    local = local.queryParam("query", queryValue);
                }
                if (count != null) {
                    local = local.queryParam("rows", count);
                }
                if (start != null) {
                    local = local.queryParam("offset", start);
                }
                if (author != null) {
                    local = local.queryParam("query.author", author);
                }
                if (title != null) {
                    local = local.queryParam("query.container-title", title);
                }
                if (bibliographics != null) {
                    local = local.queryParam("query.bibliographic", bibliographics);
                }
                Invocation.Builder invocationBuilder = local.request();
                Response response = invocationBuilder.get();
                if (response.getStatus() != 200) {
                    return null;
                }
                String responseString = response.readEntity(String.class);
                ReadContext ctx = JsonPath.parse(responseString);
                Object o = ctx.read("$.message.items[*]");
                if (o.getClass().isAssignableFrom(JSONArray.class)) {
                    JSONArray array = (JSONArray)o;
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
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
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
            HttpGet method = null;
            try {
                WebTarget local = webTarget.queryParam("query", query.getParameterAsClass("query", String.class));
                Invocation.Builder invocationBuilder = local.request();
                Response response = invocationBuilder.get();
                if (response.getStatus() != 200) {
                    return null;
                }
                String responseString = response.readEntity(String.class);
                ReadContext ctx = JsonPath.parse(responseString);
                return ctx.read("$.message.total-results");
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
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
            WebTarget local = webTarget.path(query.getParameterAsClass("id", String.class));
            Invocation.Builder invocationBuilder = local.request();
            Response response = invocationBuilder.head();
            return response.getStatus() == 200 ? 1 : 0;
        }
    }

}
