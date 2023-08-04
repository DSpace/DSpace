/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.wos.service;

import static org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl.HEADER_PARAMETERS;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.el.MethodNotFoundException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements a data source for querying Web of Science.
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class WOSImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<Element>
        implements QuerySource {

    private final static Logger log = LogManager.getLogger();

    private static final String AI_PATTERN  = "^AI=(.*)";
    private static final Pattern ISI_PATTERN = Pattern.compile("^\\d{15}$");

    private int timeout = 1000;

    private String url;
    private String urlSearch;
    private String apiKey;

    @Autowired
    private LiveImportClient liveImportClient;

    @Override
    public void init() throws Exception {}

    /**
     * The string that identifies this import implementation. Preferable a URI
     *
     * @return the identifying uri
     */
    @Override
    public String getImportSource() {
        return "wos";
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
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        List<ImportRecord> records = retry(new FindByIdCallable(id));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        return retry(new SearchNBByQueryCallable(query));
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for WOS");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for WOS");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for WOS");
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
                String queryString = URLEncoder.encode(checkQuery(query), StandardCharsets.UTF_8);
                String url = urlSearch + queryString + "&count=1&firstRecord=1";
                Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
                params.put(HEADER_PARAMETERS, getRequestParameters());
                String response = liveImportClient.executeHttpGetRequest(timeout, url, params);

                SAXBuilder saxBuilder = new SAXBuilder();
                Document document = saxBuilder.build(new StringReader(response));
                Element root = document.getRootElement();
                XPathExpression<Element> xpath = XPathFactory.instance().compile("//*[@name=\"RecordsFound\"]",
                                                 Filters.element(), null);
                Element tot = xpath.evaluateFirst(root);
                return Integer.valueOf(tot.getValue());
            }
            return null;
        }
    }

    /**
     * This class is a Callable implementation to get a Web of Science entry using Doi
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
     */
    private class FindByIdCallable implements Callable<List<ImportRecord>> {

        private String doi;

        private FindByIdCallable(String doi) {
            this.doi = URLEncoder.encode(doi, StandardCharsets.UTF_8);
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> results = new ArrayList<>();
            if (StringUtils.isNotBlank(apiKey)) {
                String urlString = url + this.doi + "?databaseId=WOS&lang=en&count=10&firstRecord=1";
                Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
                params.put(HEADER_PARAMETERS, getRequestParameters());
                String response = liveImportClient.executeHttpGetRequest(timeout, urlString, params);

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
            String queryString = checkQuery(query.getParameterAsClass("query", String.class));
            Integer start = query.getParameterAsClass("start", Integer.class);
            Integer count = query.getParameterAsClass("count", Integer.class);
            if (StringUtils.isNotBlank(apiKey)) {
                Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
                params.put(HEADER_PARAMETERS, getRequestParameters());
                String url = urlSearch + URLEncoder.encode(queryString, StandardCharsets.UTF_8)
                                                 + "&count=" + count + "&firstRecord=" + (start + 1);
                String response = liveImportClient.executeHttpGetRequest(timeout, url, params);

                List<Element> omElements = splitToRecords(response);
                for (Element el : omElements) {
                    results.add(transformSourceRecords(el));
                }
            }
            return results;
        }

    }

    private Map<String, String> getRequestParameters() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("Accept", "application/xml");
        params.put("X-ApiKey", this.apiKey);
        return params;
    }

    /**
     * This method check if the query contain
     * "AI=(...)" Author Identifier or a DOI "DO=(query)"
     * or Accession Number "UT=(query)".
     * Otherwise the value is placed in TS=(query) tag
     * that searches for topic terms in the following fields within a document:
     * Title, Abstract, Author keywords, Keywords Plus
     * 
     * @param query
     */
    private String checkQuery(String query) {
        Pattern risPattern = Pattern.compile(AI_PATTERN);
        Matcher risMatcher = risPattern.matcher(query.trim());
        if (risMatcher.matches()) {
            return query;
        }
        if (DoiCheck.isDoi(query)) {
            // FIXME: workaround to be removed once fixed by the community the double post of query param
            if (query.startsWith(",")) {
                query = query.substring(1);
            }
            return "DO=(" + query + ")";
        } else if (isIsi(query)) {
            return "UT=(" + query + ")";
        }
        StringBuilder queryBuilder =  new StringBuilder("TS=(");
        queryBuilder.append(query).append(")");
        return queryBuilder.toString();
    }

    private boolean isIsi(String query) {
        if (query.startsWith("WOS:")) {
            return true;
        }
        Matcher matcher = ISI_PATTERN.matcher(query.trim());
        return matcher.matches();
    }

    private List<Element> splitToRecords(String recordsSrc) {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new StringReader(recordsSrc));
            Element root = document.getRootElement();
            String cData = XPathFactory.instance().compile("//*[@name=\"Records\"]",
                           Filters.element(), null).evaluate(root).get(0).getValue().trim();
            Document intDocument = saxBuilder.build(new StringReader(cData));
            XPathExpression<Element> xPath = XPathFactory.instance().compile("*", Filters.element(), null);
            List<Element> records = xPath.evaluate(intDocument.getRootElement());
            if (CollectionUtils.isNotEmpty(records)) {
                return records;
            }
        } catch (JDOMException | IOException e) {
            log.error(e.getMessage());
            return new ArrayList<Element>();
        }
        return new ArrayList<Element>();
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

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

}