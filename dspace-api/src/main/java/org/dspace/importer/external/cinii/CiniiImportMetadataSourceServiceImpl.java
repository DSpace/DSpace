/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.cinii;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.el.MethodNotFoundException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.dspace.services.ConfigurationService;
import org.jdom2.Attribute;
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
 * Implements a data source for querying Cinii
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class CiniiImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<Element>
        implements QuerySource {

    private final static Logger log = LogManager.getLogger();

    private String url;
    private String urlSearch;

    @Autowired
    private LiveImportClient liveImportClient;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public String getImportSource() {
        return "cinii";
    }

    @Override
    public void init() throws Exception {}

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByIdCallable(id));
        return CollectionUtils.isNotEmpty(records) ? records.get(0) : null;
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
        return CollectionUtils.isNotEmpty(records) ? records.get(0) : null;
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        return retry(new FindMatchingRecordCallable(query));
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for Cinii");
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

    /**
     * This class is a Callable implementation to get CiNii entries based on
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
            List<ImportRecord> records = new LinkedList<ImportRecord>();
            Integer count = query.getParameterAsClass("count", Integer.class);
            Integer start = query.getParameterAsClass("start", Integer.class);
            String queryString = query.getParameterAsClass("query", String.class);
            String appId = configurationService.getProperty("cinii.appid");
            List<String> ids = getCiniiIds(appId, count, null, null, null, start, queryString);
            if (CollectionUtils.isNotEmpty(ids)) {
                for (String id : ids) {
                    List<ImportRecord> tmp = search(id, appId);
                    if (CollectionUtils.isNotEmpty(tmp)) {
                        tmp.forEach(x -> x.addValue(createIdentifier(id)));
                    }
                    records.addAll(tmp);
                }
            }
            return records;
        }
    }

    /**
     * This class is a Callable implementation to get an CiNii entry using CiNii ID
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
            String appId = configurationService.getProperty("cinii.appid");
            String id = query.getParameterAsClass("id", String.class);
            List<ImportRecord> importRecord = search(id, appId);
            if (CollectionUtils.isNotEmpty(importRecord)) {
                importRecord.forEach(x -> x.addValue(createIdentifier(id)));
            }
            return importRecord;
        }
    }

    /**
     * This class is a Callable implementation to search CiNii entries
     * using author, title and year.
     * Pagination is supported too, using the value of the Query's map with keys "start" and "count".
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
     */
    private class FindMatchingRecordCallable implements Callable<List<ImportRecord>> {

        private Query query;

        private FindMatchingRecordCallable(Query q) {
            query = q;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> records = new LinkedList<ImportRecord>();
            String title = query.getParameterAsClass("title", String.class);
            String author = query.getParameterAsClass("author", String.class);
            Integer year = query.getParameterAsClass("year", Integer.class);
            Integer maxResult = query.getParameterAsClass("maxResult", Integer.class);
            Integer start = query.getParameterAsClass("start", Integer.class);
            String appId = configurationService.getProperty("cinii.appid");
            List<String> ids = getCiniiIds(appId, maxResult, author, title, year, start, null);
            if (CollectionUtils.isNotEmpty(ids)) {
                for (String id : ids) {
                    List<ImportRecord> importRecords = search(id, appId);
                    if (CollectionUtils.isNotEmpty(importRecords)) {
                        importRecords.forEach(x -> x.addValue(createIdentifier(id)));
                    }
                    records.addAll(importRecords);
                }
            }
            return records;
        }

    }

    /**
     * This class is a Callable implementation to count the number
     * of entries for an CiNii query.
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
            String appId = configurationService.getProperty("cinii.appid");
            String queryString = query.getParameterAsClass("query", String.class);
            return countCiniiElement(appId, null, null, null, null, null, queryString);
        }
    }

    /**
     * Get metadata by searching CiNii RDF API with CiNii NAID
     *
     * @param id    CiNii NAID to search by
     * @param appId registered application identifier for the API
     * @return record metadata
     * @throws IOException   A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws HttpException Represents a XML/HTTP fault and provides access to the HTTP status code.
     */
    protected List<ImportRecord> search(String id, String appId)
        throws IOException, HttpException {
        try {
            List<ImportRecord> records = new LinkedList<ImportRecord>();
            URIBuilder uriBuilder = new URIBuilder(this.url + id + ".rdf?appid=" + appId);
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            String response = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), params);
            List<Element> elements = splitToRecords(response);
            for (Element record : elements) {
                records.add(transformSourceRecords(record));
            }
            return records;
        } catch (URISyntaxException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private List<Element> splitToRecords(String recordsSrc) {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new StringReader(recordsSrc));
            Element root = document.getRootElement();
            return root.getChildren();
        } catch (JDOMException | IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns a list of uri links (for example:https://cir.nii.ac.jp/crid/123456789)
     * to the searched CiNii articles
     * 
     * @param appId       Application ID
     * @param maxResult   The number of search results per page
     * @param author      Author name
     * @param title       Article name
     * @param year        Year of publication
     * @param start       Start number for the acquired search result list
     * @param query       Keyword to be searched
     */
    private List<String> getCiniiIds(String appId, Integer maxResult, String author, String title,
        Integer year, Integer start, String query) {
        try {
            List<String> ids = new ArrayList<>();
            URIBuilder uriBuilder = new URIBuilder(this.urlSearch);
            uriBuilder.addParameter("format", "rss");
            if (StringUtils.isNotBlank(appId)) {
                uriBuilder.addParameter("appid", appId);
            }
            if (Objects.nonNull(maxResult) && maxResult != 0) {
                uriBuilder.addParameter("count", maxResult.toString());
            }
            if (Objects.nonNull(start)) {
                uriBuilder.addParameter("start", start.toString());
            }
            if (StringUtils.isNotBlank(title)) {
                uriBuilder.addParameter("title", title);
            }
            if (StringUtils.isNotBlank(author)) {
                uriBuilder.addParameter("author", author);
            }
            if (StringUtils.isNotBlank(query)) {
                uriBuilder.addParameter("q", query);
            }
            if (Objects.nonNull(year) && year != -1 && year != 0) {
                uriBuilder.addParameter("year_from", String.valueOf(year));
                uriBuilder.addParameter("year_to", String.valueOf(year));
            }
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            String response = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), params);
            int url_len = this.url.length() - 1;
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new StringReader(response));
            Element root = document.getRootElement();
            List<Namespace> namespaces = Arrays.asList(
                 Namespace.getNamespace("ns", "http://purl.org/rss/1.0/"),
                 Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"));
            XPathExpression<Attribute> xpath = XPathFactory.instance().compile("//ns:item/@rdf:about",
                    Filters.attribute(), null, namespaces);
            List<Attribute> recordsList = xpath.evaluate(root);
            for (Attribute item : recordsList) {
                String value = item.getValue();
                if (value.length() > url_len) {
                    ids.add(value.substring(url_len + 1));
                }
            }
            return ids;
        } catch (JDOMException | IOException | URISyntaxException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns the total number of CiNii articles returned by a specific query
     * 
     * @param appId       Application ID
     * @param maxResult   The number of search results per page
     * @param author      Author name
     * @param title       Article name
     * @param year        Year of publication
     * @param start       Start number for the acquired search result list
     * @param query       Keyword to be searched
     */
    private Integer countCiniiElement(String appId, Integer maxResult, String author, String title,
            Integer year, Integer start, String query) {
        try {
            URIBuilder uriBuilder = new URIBuilder(this.urlSearch);
            uriBuilder.addParameter("format", "rss");
            uriBuilder.addParameter("appid", appId);
            if (Objects.nonNull(maxResult) && maxResult != 0) {
                uriBuilder.addParameter("count", maxResult.toString());
            }
            if (Objects.nonNull(start)) {
                uriBuilder.addParameter("start", start.toString());
            }
            if (StringUtils.isNotBlank(title)) {
                uriBuilder.addParameter("title", title);
            }
            if (StringUtils.isNotBlank(author)) {
                uriBuilder.addParameter("author", author);
            }
            if (StringUtils.isNotBlank(query)) {
                uriBuilder.addParameter("q", query);
            }
            if (Objects.nonNull(year) && year != -1 && year != 0) {
                uriBuilder.addParameter("year_from", String.valueOf(year));
                uriBuilder.addParameter("year_to", String.valueOf(year));
            }

            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            String response = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), params);

            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new StringReader(response));
            Element root = document.getRootElement();
            List<Namespace> namespaces = Arrays
                    .asList(Namespace.getNamespace("opensearch", "http://a9.com/-/spec/opensearch/1.1/"));
            XPathExpression<Element> xpath = XPathFactory.instance().compile("//opensearch:totalResults",
                    Filters.element(), null, namespaces);
            List<Element> nodes = xpath.evaluate(root);
            if (nodes != null && !nodes.isEmpty()) {
                return Integer.parseInt(((Element) nodes.get(0)).getText());
            }
            return 0;
        } catch (JDOMException | IOException | URISyntaxException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private MetadatumDTO createIdentifier(String id) {
        MetadatumDTO metadatumDTO = new MetadatumDTO();
        metadatumDTO.setSchema("dc");
        metadatumDTO.setElement("identifier");
        metadatumDTO.setQualifier("other");
        metadatumDTO.setValue(id);
        return metadatumDTO;
    }

}