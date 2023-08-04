/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scopus.service;

import static org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl.URI_PARAMETERS;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.el.MethodNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.DoiCheck;
import org.dspace.importer.external.service.components.QuerySource;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements a data source for querying Scopus
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science dot com)
 */
public class ScopusImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<Element>
        implements QuerySource {

    private int timeout = 1000;

    int itemPerPage = 25;

    private String url;
    private String apiKey;
    private String instKey;
    private String viewMode;

    @Autowired
    private LiveImportClient liveImportClient;

    public LiveImportClient getLiveImportClient() {
        return liveImportClient;
    }

    public void setLiveImportClient(LiveImportClient liveImportClient) {
        this.liveImportClient = liveImportClient;
    }

    @Override
    public void init() throws Exception {}

    /**
     * The string that identifies this import implementation. Preferable a URI
     *
     * @return the identifying uri
     */
    @Override
    public String getImportSource() {
        return "scopus";
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        if (isEID(query)) {
            return retry(new FindByIdCallable(query)).size();
        }
        if (DoiCheck.isDoi(query)) {
            query = DoiCheck.purgeDoiValue(query);
        }
        return retry(new SearchNBByQueryCallable(query));
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        if (isEID(query.toString())) {
            return retry(new FindByIdCallable(query.toString())).size();
        }
        if (DoiCheck.isDoi(query.toString())) {
            query.addParameter("query", DoiCheck.purgeDoiValue(query.toString()));
        }
        return retry(new SearchNBByQueryCallable(query));
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start,
            int count) throws MetadataSourceException {
        if (isEID(query)) {
            return retry(new FindByIdCallable(query));
        }
        if (DoiCheck.isDoi(query)) {
            query = DoiCheck.purgeDoiValue(query);
        }
        return retry(new SearchByQueryCallable(query, count, start));
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query)
            throws MetadataSourceException {
        if (isEID(query.toString())) {
            return retry(new FindByIdCallable(query.toString()));
        }
        if (DoiCheck.isDoi(query.toString())) {
            query.addParameter("query", DoiCheck.purgeDoiValue(query.toString()));
        }
        return retry(new SearchByQueryCallable(query));
    }


    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        List<ImportRecord> records = null;
        if (DoiCheck.isDoi(query.toString())) {
            query.addParameter("query", DoiCheck.purgeDoiValue(query.toString()));
        }
        if (isEID(query.toString())) {
            records = retry(new FindByIdCallable(query.toString()));
        } else {
            records = retry(new SearchByQueryCallable(query));
        }
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item)
            throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for Scopus");
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        List<ImportRecord> records = retry(new FindByIdCallable(id));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query)
            throws MetadataSourceException {
        if (isEID(query.toString())) {
            return retry(new FindByIdCallable(query.toString()));
        }
        if (DoiCheck.isDoi(query.toString())) {
            query.addParameter("query", DoiCheck.purgeDoiValue(query.toString()));
        }
        return retry(new FindByQueryCallable(query));
    }

    private boolean isEID(String query) {
        Pattern pattern = Pattern.compile("2-s2\\.0-\\d+");
        Matcher match = pattern.matcher(query);
        if (match.matches()) {
            return true;
        }
        return false;
    }

    /**
     * This class implements a callable to get the numbers of result
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
            if (StringUtils.isNotBlank(apiKey)) {
                // Execute the request.
                Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
                Map<String, String> requestParams = getRequestParameters(query, null, null, null);
                params.put(URI_PARAMETERS, requestParams);
                String response = liveImportClient.executeHttpGetRequest(timeout, url, params);

                SAXBuilder saxBuilder = new SAXBuilder();
                Document document = saxBuilder.build(new StringReader(response));
                Element root = document.getRootElement();

                List<Namespace> namespaces = Arrays.asList(
                     Namespace.getNamespace("opensearch", "http://a9.com/-/spec/opensearch/1.1/"));
                XPathExpression<Element> xpath = XPathFactory.instance()
                          .compile("opensearch:totalResults", Filters.element(), null, namespaces);

                Element count = xpath.evaluateFirst(root);
                try {
                    return Integer.parseInt(count.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }
    }

    /**
     * This class is a Callable implementation to get a Scopus entry using EID
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
     */
    private class FindByIdCallable implements Callable<List<ImportRecord>> {

        private String eid;

        private FindByIdCallable(String eid) {
            this.eid = eid;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> results = new ArrayList<>();
            String queryString = "EID(" + eid.replace("!", "/") + ")";
            if (StringUtils.isNotBlank(apiKey)) {
                Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
                Map<String, String> requestParams = getRequestParameters(queryString, viewMode, null, null);
                params.put(URI_PARAMETERS, requestParams);
                String response = liveImportClient.executeHttpGetRequest(timeout, url, params);
                List<Element> elements = splitToRecords(response);
                for (Element record : elements) {
                    results.add(transformSourceRecords(record));
                }
            }
            return results;
        }
    }

    /**
     * This class implements a callable to get the items based on query parameters
     */
    private class FindByQueryCallable implements Callable<List<ImportRecord>> {

        private String title;
        private String author;
        private Integer year;
        private Integer start;
        private Integer count;

        private FindByQueryCallable(Query query) {
            this.title = query.getParameterAsClass("title", String.class);
            this.year = query.getParameterAsClass("year", Integer.class);
            this.author = query.getParameterAsClass("author", String.class);
            this.start = query.getParameterAsClass("start", Integer.class) != null ?
                query.getParameterAsClass("start", Integer.class) : 0;
            this.count = query.getParameterAsClass("count", Integer.class) != null ?
                query.getParameterAsClass("count", Integer.class) : 20;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> results = new ArrayList<>();
            String queryString = "";
            StringBuffer query = new StringBuffer();
            if (StringUtils.isNotBlank(title)) {
                query.append("title(").append(title).append("");
            }
            if (StringUtils.isNotBlank(author)) {
                // [FAU]
                if (query.length() > 0) {
                    query.append(" AND ");
                }
                query.append("AUTH(").append(author).append(")");
            }
            if (year != -1) {
                // [DP]
                if (query.length() > 0) {
                    query.append(" AND ");
                }
                query.append("PUBYEAR IS ").append(year);
            }
            queryString = query.toString();

            if (apiKey != null && !apiKey.equals("")) {
                Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
                Map<String, String> requestParams = getRequestParameters(queryString, viewMode, start, count);
                params.put(URI_PARAMETERS, requestParams);
                String response = liveImportClient.executeHttpGetRequest(timeout, url, params);
                List<Element> elements = splitToRecords(response);
                for (Element record : elements) {
                    results.add(transformSourceRecords(record));
                }
            }
            return results;
        }
    }

    /**
     * Find records matching a string query.
     *
     * @param query    A query string to base the search on.
     * @param start    Offset to start at
     * @param count    Number of records to retrieve.
     * @return         A set of records. Fully transformed.
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
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
            String queryString = query.getParameterAsClass("query", String.class);
            Integer start = query.getParameterAsClass("start", Integer.class);
            Integer count = query.getParameterAsClass("count", Integer.class);
            if (StringUtils.isNotBlank(apiKey)) {
                Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
                Map<String, String> requestParams = getRequestParameters(queryString, viewMode, start, count);
                params.put(URI_PARAMETERS, requestParams);
                String response = liveImportClient.executeHttpGetRequest(timeout, url, params);
                List<Element> elements = splitToRecords(response);
                for (Element record : elements) {
                    results.add(transformSourceRecords(record));
                }
            }
            return results;
        }
    }

    private Map<String, String> getRequestParameters(String query, String viewMode, Integer start, Integer count) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("httpAccept", "application/xml");
        params.put("apiKey", apiKey);
        params.put("query", query);

        if (StringUtils.isNotBlank(instKey)) {
            params.put("insttoken", instKey);
        }
        if (StringUtils.isNotBlank(viewMode)) {
            params.put("view", viewMode);
        }

        params.put("start", (Objects.nonNull(start) ? start + "" : "0"));
        params.put("count", (Objects.nonNull(count) ? count + "" : "20"));
        return params;
    }

    private List<Element> splitToRecords(String recordsSrc) {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new StringReader(recordsSrc));
            Element root = document.getRootElement();
            List<Element> records = root.getChildren("entry",Namespace.getNamespace("http://www.w3.org/2005/Atom"));
            return records;
        } catch (JDOMException | IOException e) {
            return new ArrayList<Element>();
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getViewMode() {
        return viewMode;
    }

    public void setViewMode(String viewMode) {
        this.viewMode = viewMode;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getInstKey() {
        return instKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setInstKey(String instKey) {
        this.instKey = instKey;
    }

}