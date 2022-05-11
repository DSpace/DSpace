/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ads;

import static org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl.HEADER_PARAMETERS;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.el.MethodNotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Implements a data source for querying ADS
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class ADSImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<String>
        implements QuerySource {

    private final static Logger log = LogManager.getLogger();

    private String url;
    private String resultFieldList;

    private String apiKey;
    private int timeout = 1000;

    @Autowired
    private LiveImportClient liveImportClient;

    @Override
    public String getImportSource() {
        return "ads";
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
        return retry(new SearchByQueryCallable(query, count, start));
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
        return retry(new FindMatchingRecordCallable(query));
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for CrossRef");
    }

    @Override
    public void init() throws Exception {}

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * This class is a Callable implementation to get ADS entries based on query object.
     * This Callable use as query value the string queryString passed to constructor.
     * If the object will be construct through Query.class instance, a Query's map entry with key "query" will be used.
     * Pagination is supported too, using the value of the Query's map with keys "start" and "count".
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
     */
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
            return search(query.getParameterAsClass("query", String.class),
                query.getParameterAsClass("start", Integer.class),
                query.getParameterAsClass("count", Integer.class),
                getApiKey());
        }
    }

    /**
     * This class is a Callable implementation to get an ADS entry using bibcode
     * The bibcode to use can be passed through the constructor as a String or as Query's map entry, with the key "id".
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
            String queryString = "bibcode:" + query.getParameterAsClass("id", String.class);
            return search(queryString, 0 , 1, getApiKey());
        }
    }

    /**
     * This class is a Callable implementation to search ADS entries
     * using author and title and year.
     * Pagination is supported too, using the value of the Query's map with keys "start" and "count".
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
     */
    private class FindMatchingRecordCallable implements Callable<List<ImportRecord>> {

        private Query query;

        private FindMatchingRecordCallable(Query q) {
            query = q;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            Integer count = query.getParameterAsClass("count", Integer.class);
            Integer start = query.getParameterAsClass("start", Integer.class);
            String author = query.getParameterAsClass("author", String.class);
            String title = query.getParameterAsClass("title", String.class);
            Integer year = query.getParameterAsClass("year", Integer.class);
            return search(title, author, year, start, count, getApiKey());
        }

    }

    /**
     * This class is a Callable implementation to count the number of entries for an ADS query.
     * This Callable use as query value to ADS the string queryString passed to constructor.
     * If the object will be construct through Query.class instance, the value of the Query's
     * map with the key "query" will be used.
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
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
            return count(query.getParameterAsClass("query", String.class), getApiKey());
        }
    }

    private List<ImportRecord> search(String title, String author, int year,  int start, int count, String token) {
        String query = "";
        if (StringUtils.isNotBlank(title)) {
            query += "title:" + title;
        }
        if (StringUtils.isNotBlank(author)) {
            String splitRegex = "(\\s*,\\s+|\\s*;\\s+|\\s*;+|\\s*,+|\\s+)";
            String[] authors = author.split(splitRegex);
            // [FAU]
            if (StringUtils.isNotBlank(query)) {
                query = "author:";
            } else {
                query += "&fq=author:";
            }
            int x = 0;
            for (String auth : authors) {
                x++;
                query += auth;
                if (x < authors.length) {
                    query += " AND ";
                }
            }
        }
        if (year != -1) {
            // [DP]
            if (StringUtils.isNotBlank(query)) {
                query = "year:";
            } else {
                query += "&fq=year:";
            }
            query += year;
        }
        return search(query.toString(), start, count, token);
    }

    public Integer count(String query, String token) {
        try {
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            Map<String, String> headerParameters = new HashMap<String, String>();
            headerParameters.put("Authorization", "Bearer " + token);
            params.put(HEADER_PARAMETERS, headerParameters);

            URIBuilder uriBuilder = new URIBuilder(this.url);
            uriBuilder.addParameter("q", query);
            uriBuilder.addParameter("rows", "1");
            uriBuilder.addParameter("start", "0");
            uriBuilder.addParameter("fl", this.resultFieldList);

            String resp = liveImportClient.executeHttpGetRequest(timeout, uriBuilder.toString(), params);
            JsonNode jsonNode = convertStringJsonToJsonNode(resp);
            return jsonNode.at("/response/numFound").asInt();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<ImportRecord> search(String query, Integer start, Integer count, String token) {
        List<ImportRecord> adsResults = new ArrayList<>();
        try {
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            Map<String, String> headerParameters = new HashMap<String, String>();
            headerParameters.put("Authorization", "Bearer " + token);
            params.put(HEADER_PARAMETERS, headerParameters);

            URIBuilder uriBuilder = new URIBuilder(this.url);
            uriBuilder.addParameter("q", query);
            uriBuilder.addParameter("rows", count.toString());
            uriBuilder.addParameter("start", start.toString());
            uriBuilder.addParameter("fl", this.resultFieldList);

            String resp = liveImportClient.executeHttpGetRequest(timeout, uriBuilder.toString(), params);

            JsonNode jsonNode = convertStringJsonToJsonNode(resp);
            JsonNode docs = jsonNode.at("/response/docs");
            if (docs.isArray()) {
                Iterator<JsonNode> nodes = docs.elements();
                while (nodes.hasNext()) {
                    JsonNode node = nodes.next();
                    adsResults.add(transformSourceRecords(node.toString()));
                }
            } else {
                adsResults.add(transformSourceRecords(docs.toString()));
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return adsResults;
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

    public void setResultFieldList(String resultFieldList) {
        this.resultFieldList = resultFieldList;
    }

}