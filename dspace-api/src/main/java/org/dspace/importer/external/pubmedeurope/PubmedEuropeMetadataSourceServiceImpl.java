/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.pubmedeurope;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.el.MethodNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.client.ClientProtocolException;
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
import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

/**
 * Implements a data source for querying PubMed Europe
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class PubmedEuropeMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<Element>
        implements QuerySource {

    private final static Logger log = LogManager.getLogger();

    private String url;

    @Autowired
    private LiveImportClient liveImportClient;

    @Override
    public String getImportSource() {
        return "pubmedeu";
    }

    /**
     * Get a single record from the PubMed Europe.
     *
     * @param id                         Identifier for the record
     * @return                           The first matching record
     * @throws MetadataSourceException   If the underlying methods throw any exception.
     */
    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByIdCallable(id));
        return CollectionUtils.isEmpty(records) ? null : records.get(0);
    }

    /**
     * Find the number of records matching a query;
     *
     * @param query a query string to base the search on.
     * @return the sum of the matching records over this import source
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query));
    }

    /**
     * Find the number of records matching a query;
     *
     * @param query                      A query string to base the search on.
     * @return                           The sum of the matching records over this import source
     * @throws MetadataSourceException   If the underlying methods throw any exception.
     */
    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query));
    }

    /**
     * Find records matching a string query.
     *
     * @param query                      A query string to base the search on.
     * @param start                      Offset to start at
     * @param count                      Number of records to retrieve.
     * @return                           A set of records. Fully transformed.
     * @throws MetadataSourceException   If the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query, count, start));
    }

    /**
     * Find records based on a object query.
     *
     * @param query                     A query object to base the search on.
     * @return                          A set of records. Fully transformed.
     * @throws MetadataSourceException  If the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query));
    }

    /**
     * Get a single record from the PubMed Europe.
     *
     * @param query                       A query matching a single record
     * @return                            The first matching record
     * @throws MetadataSourceException    If the underlying methods throw any exception.
     */
    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByIdCallable(query));
        return CollectionUtils.isEmpty(records) ? null : records.get(0);
    }

    /**
     * Finds records based on query object.
     *
     * @param query                        A query object to base the search on.
     * @return                             A collection of import records.
     * @throws MetadataSourceException     If the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        return retry(new FindMatchingRecordCallable(query));
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for PubMed Europe");
    }

    @Override
    public void init() throws Exception {}

    public List<ImportRecord> getByPubmedEuropeID(String pubmedID, Integer start, Integer size)
            throws IOException, HttpException {
        String query = "(EXT_ID:" + pubmedID + ")";
        return search(query, size < 1 ? 1 : size, start);
    }

    /**
     * This class is a Callable implementation to get PubMed Europe entries based on
     * query object.
     * 
     * This Callable use as query value the string queryString passed to constructor.
     * If the object will be construct through Query.class instance, a Query's map entry with key "query" will be used.
     * Pagination is supported too, using the value of the Query's map with keys "start" and "count".
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
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
            Integer count = query.getParameterAsClass("count", Integer.class);
            Integer start = query.getParameterAsClass("start", Integer.class);
            String queryString = query.getParameterAsClass("query", String.class);
            return search(queryString, count, start);

        }
    }

    /**
     * This class is a Callable implementation to get an PubMed Europe entry using PubMed Europe ID
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
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
            return getByPubmedEuropeID(query.getParameterAsClass("id", String.class), 1 ,0);
        }
    }

    /**
     * This class is a Callable implementation to search PubMed Europe entries
     * using author, title and year.
     * Pagination is supported too, using the value of the Query's map with keys "start" and "count".
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
     */
    public class FindMatchingRecordCallable implements Callable<List<ImportRecord>> {

        private Query query;

        private FindMatchingRecordCallable(Query q) {
            query = q;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            String title = query.getParameterAsClass("title", String.class);
            String author = query.getParameterAsClass("author", String.class);
            Integer year = query.getParameterAsClass("year", Integer.class);
            Integer maxResult = query.getParameterAsClass("maxResult", Integer.class);
            Integer start = query.getParameterAsClass("start", Integer.class);
            return search(title, author, year, maxResult, start);
        }

    }

    /**
     * This class is a Callable implementation to count the number
     * of entries for an PubMed Europe query.
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
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
            try {
                return count(query.getParameterAsClass("query", String.class));
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }
    }

    /**
     * Returns the total number of PubMed Europe publications returned by a specific query
     * 
     * @param query                      A keyword or combination of keywords to be searched
     * @throws URISyntaxException        If URI syntax error
     * @throws ClientProtocolException   The client protocol exception
     * @throws IOException               If IO error
     * @throws JaxenException            If Xpath evaluation failed
     */
    public Integer count(String query) throws URISyntaxException, ClientProtocolException, IOException, JaxenException {
        try {
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            String response = liveImportClient.executeHttpGetRequest(1000, buildURI(1, query), params);

            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new StringReader(response));
            Element root = document.getRootElement();
            Element element = root.getChild("hitCount");
            return Integer.parseInt(element.getValue());
        } catch (JDOMException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public List<ImportRecord> search(String title, String author, int year, int count, int start)
            throws IOException {
        StringBuffer query = new StringBuffer();
        query.append("(");
        if (StringUtils.isNotBlank(title)) {
            query.append("TITLE:").append(title);
            query.append(")");
        }
        if (StringUtils.isNotBlank(author)) {
            // Search for a surname and (optionally) initial(s) in publication author lists
            // AUTH:einstein, AUTH:”Smith AB”
            String splitRegex = "(\\s*,\\s+|\\s*;\\s+|\\s*;+|\\s*,+|\\s+)";
            String[] authors = author.split(splitRegex);
            if (query.length() > 0) {
                query.append(" AND ");
            }
            query.append("(");
            int countAuthors = 0;
            for (String auth : authors) {
                countAuthors++;
                query.append("AUTH:\"").append(auth).append("\"");
                if (countAuthors < authors.length) {
                    query.append(" AND ");
                }
            }
            query.append(")");
        }
        if (year != -1) {
            if (query.length() > 0) {
                query.append(" AND ");
            }
            query.append("( PUB_YEAR:").append(year).append(")");
        }
        query.append(")");
        return search(query.toString(), count, start);
    }

    /**
     * Returns a list of PubMed Europe publication records
     * 
     * @param query           A keyword or combination of keywords to be searched
     * @param size           The number of search results per page
     * @param start           Start number for the acquired search result list
     * @throws IOException    If IO error
     */
    public List<ImportRecord> search(String query, Integer size, Integer start) throws IOException {
        List<ImportRecord> results = new ArrayList<>();
        try {
            URIBuilder uriBuilder = new URIBuilder(this.url);
            uriBuilder.addParameter("format", "xml");
            uriBuilder.addParameter("resulttype", "core");
            uriBuilder.addParameter("pageSize", String.valueOf(size));
            uriBuilder.addParameter("query", query);
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            boolean lastPage = false;
            int skipped = 0;
            while (!lastPage || results.size() < size) {
                String response = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), params);
                String cursorMark = StringUtils.EMPTY;
                if (StringUtils.isNotBlank(response)) {
                    SAXBuilder saxBuilder = new SAXBuilder();
                    Document document = saxBuilder.build(new StringReader(response));
                    XPathFactory xpfac = XPathFactory.instance();
                    XPathExpression<Element> xPath = xpfac.compile("//responseWrapper/resultList/result",
                            Filters.element());
                    List<Element> records = xPath.evaluate(document);
                    if (records.size() > 0) {
                        for (Element item : records) {
                            if (start > skipped) {
                                skipped++;
                            } else {
                                results.add(transformSourceRecords(item));
                            }
                        }
                    } else {
                        lastPage = true;
                        break;
                    }
                    Element root = document.getRootElement();
                    Element nextCursorMark = root.getChild("nextCursorMark");
                    cursorMark = Objects.nonNull(nextCursorMark) ? nextCursorMark.getValue() : StringUtils.EMPTY;
                }
                if (StringUtils.isNotBlank(cursorMark)) {
                    uriBuilder.setParameter("cursorMar", cursorMark);
                } else {
                    lastPage = true;
                }
            }
        } catch (URISyntaxException | JDOMException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        return results;
    }

    private String buildURI(Integer pageSize, String query) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(this.url);
        uriBuilder.addParameter("format", "xml");
        uriBuilder.addParameter("resulttype", "core");
        uriBuilder.addParameter("pageSize", String.valueOf(pageSize));
        uriBuilder.addParameter("query", query);
        return uriBuilder.toString();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}