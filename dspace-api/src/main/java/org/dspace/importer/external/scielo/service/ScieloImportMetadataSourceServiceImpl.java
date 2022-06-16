/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scielo.service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.el.MethodNotFoundException;
import javax.ws.rs.BadRequestException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.client.utils.URIBuilder;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.FileSourceException;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements a data source for querying Scielo
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class ScieloImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<Map<String,List<String>>>
        implements QuerySource {

    /**
     * This pattern is used when reading the Scielo response,
     * to check if the fields you are reading is in rid format
     */
    private static final String PATTERN = "^([A-Z][A-Z0-9])  - (.*)$";

    /**
     * This pattern is used to verify correct format of ScieloId
     */
    private static final String ID_PATTERN  = "^(.....)-(.*)-(...)$";

    private int timeout = 1000;

    private String url;

    @Autowired
    private LiveImportClient liveImportClient;

    @Override
    public void init() throws Exception {}

    @Override
    public String getImportSource() {
        return "scielo";
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
        List<ImportRecord> records = retry(new SearchByQueryCallable(query));
        return CollectionUtils.isEmpty(records) ? null : records.get(0);
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        List<ImportRecord> records = retry(new FindByIdCallable(id));
        return CollectionUtils.isEmpty(records) ? null : records.get(0);
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        return retry(new SearchNBByQueryCallable(query));
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for Scielo");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for Scielo");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for Scielo");
    }

    /**
     * This class is a Callable implementation to count the number of entries for an Scielo query
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
     */
    private class SearchNBByQueryCallable implements Callable<Integer> {

        private String query;

        private SearchNBByQueryCallable(String queryString) {
            this.query = queryString;
        }

        private SearchNBByQueryCallable(Query query) {
            this.query = query.getParameterAsClass("query", String.class);
        }

        @Override
        public Integer call() throws Exception {
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            URIBuilder uriBuilder = new URIBuilder(url + URLEncoder.encode(query, StandardCharsets.UTF_8));
            String resp = liveImportClient.executeHttpGetRequest(timeout, uriBuilder.toString(), params);
            Map<Integer, Map<String, List<String>>> records = getRecords(resp);
            return Objects.nonNull(records.size()) ? records.size() : 0;
        }
    }

    /**
     * This class is a Callable implementation to get an Scielo entry using ScieloID
     * The ScieloID to use can be passed through the constructor as a String
     * or as Query's map entry, with the key "id".
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
     */
    private class FindByIdCallable implements Callable<List<ImportRecord>> {

        private String id;

        private FindByIdCallable(String id) {
            this.id = id;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> results = new ArrayList<>();
            String scieloId = id.trim();
            Pattern risPattern = Pattern.compile(ID_PATTERN);
            Matcher risMatcher = risPattern.matcher(scieloId);
            if (risMatcher.matches()) {
                Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
                URIBuilder uriBuilder = new URIBuilder(url + URLEncoder.encode(scieloId, StandardCharsets.UTF_8));
                String resp = liveImportClient.executeHttpGetRequest(timeout, uriBuilder.toString(), params);
                Map<Integer, Map<String, List<String>>> records = getRecords(resp);
                if (Objects.nonNull(records) & !records.isEmpty()) {
                    results.add(transformSourceRecords(records.get(1)));
                }
            } else {
                throw new BadRequestException("id provided : " + scieloId + " is not an ScieloID");
            }
            return results;
        }
    }

    /**
     * This class is a Callable implementation to get Scielo entries based on query object.
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
            query.addParameter("start", start);
            query.addParameter("count", maxResult);
        }

        private SearchByQueryCallable(Query query) {
            this.query = query;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> results = new ArrayList<>();
            String q = query.getParameterAsClass("query", String.class);
            Integer count = query.getParameterAsClass("count", Integer.class);
            Integer start = query.getParameterAsClass("start", Integer.class);
            URIBuilder uriBuilder = new URIBuilder(url + URLEncoder.encode(q, StandardCharsets.UTF_8));
            uriBuilder.addParameter("start", start.toString());
            uriBuilder.addParameter("count", count.toString());
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            String resp = liveImportClient.executeHttpGetRequest(timeout, uriBuilder.toString(), params);
            Map<Integer, Map<String, List<String>>> records = getRecords(resp);
            for (int record : records.keySet()) {
                results.add(transformSourceRecords(records.get(record)));
            }
            return results;
        }
    }

    private Map<Integer, Map<String,List<String>>> getRecords(String resp) throws FileSourceException {
        Map<Integer, Map<String, List<String>>> records = new HashMap<Integer, Map<String,List<String>>>();
        BufferedReader reader;
        int countRecord = 0;
        try {
            reader = new BufferedReader(new StringReader(resp));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.equals("") || line.matches("^\\s*$")) {
                    continue;
                }
                line = line.replaceAll("\\uFEFF", "").trim();
                Pattern risPattern = Pattern.compile(PATTERN);
                Matcher risMatcher = risPattern.matcher(line);
                if (risMatcher.matches()) {
                    if (risMatcher.group(1).equals("TY") & risMatcher.group(2).equals("JOUR")) {
                        countRecord ++;
                        Map<String,List<String>> newMap = new HashMap<String, List<String>>();
                        records.put(countRecord, newMap);
                    } else {
                        Map<String, List<String>> tag2values = records.get(countRecord);
                        List<String> values = tag2values.get(risMatcher.group(1));
                        if (Objects.isNull(values)) {
                            List<String> newValues = new ArrayList<String>();
                            newValues.add(risMatcher.group(2));
                            tag2values.put(risMatcher.group(1), newValues);
                        } else {
                            values.add(risMatcher.group(2));
                            tag2values.put(risMatcher.group(1), values);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new FileSourceException("Cannot parse RIS file", e);
        }
        return records;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}