/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.pubmed.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import com.google.common.io.CharStreams;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.FileMultipleOccurencesException;
import org.dspace.importer.external.exception.FileSourceException;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.FileSource;
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
 * Implements a data source for querying PubMed Central
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 */
public class PubmedImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<Element>
    implements QuerySource, FileSource {

    private String urlFetch;
    private String urlSearch;

    private int attempt = 3;

    private List<String> supportedExtensions;

    @Autowired
    private LiveImportClient liveImportClient;

    /**
     * Set the file extensions supported by this metadata service
     * 
     * @param supportedExtensions the file extensions (xml,txt,...) supported by this service
     */
    public void setSupportedExtensions(List<String> supportedExtensions) {
        this.supportedExtensions = supportedExtensions;
    }

    @Override
    public List<String> getSupportedExtensions() {
        return supportedExtensions;
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
        return retry(new GetNbRecords(query));
    }

    /**
     * Find the number of records matching a query;
     *
     * @param query a query object to base the search on.
     * @return the sum of the matching records over this import source
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        return retry(new GetNbRecords(query));
    }

    /**
     * Find the number of records matching a string query. Supports pagination
     *
     * @param query a query string to base the search on.
     * @param start offset to start at
     * @param count number of records to retrieve.
     * @return a set of records. Fully transformed.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        return retry(new GetRecords(query, start, count));
    }

    /**
     * Find records based on a object query.
     *
     * @param query a query object to base the search on.
     * @return a set of records. Fully transformed.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        return retry(new GetRecords(query));
    }

    /**
     * Get a single record from the source.
     *
     * @param id identifier for the record
     * @return the first matching record
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        return retry(new GetRecord(id));
    }

    /**
     * Get a single record from the source.
     *
     * @param query a query matching a single record
     * @return the first matching record
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        return retry(new GetRecord(query));
    }

    /**
     * The string that identifies this import implementation. Preferable a URI
     *
     * @return the identifying uri
     */
    @Override
    public String getImportSource() {
        return "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
    }

    /**
     * Finds records based on an item
     *
     * @param item an item to base the search on
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        return retry(new FindMatchingRecords(item));
    }

    /**
     * Finds records based on query object.
     * Delegates to one or more MetadataSource implementations based on the uri.  Results will be aggregated.
     *
     * @param query a query object to base the search on.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        return retry(new FindMatchingRecords(query));
    }

    /**
     * Initialize the class
     *
     * @throws Exception on generic exception
     */
    @Override
    public void init() throws Exception {}

    private class GetNbRecords implements Callable<Integer> {

        private GetNbRecords(String queryString) {
            query = new Query();
            query.addParameter("query", queryString);
        }

        private Query query;

        public GetNbRecords(Query query) {
            this.query = query;
        }

        @Override
        public Integer call() throws Exception {
            URIBuilder uriBuilder = new URIBuilder(urlSearch);
            uriBuilder.addParameter("db", "pubmed");
            uriBuilder.addParameter("term", query.getParameterAsClass("query", String.class));
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            String response = StringUtils.EMPTY;
            int countAttempt = 0;
            while (StringUtils.isBlank(response) && countAttempt <= attempt) {
                countAttempt++;
                response = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), params);
            }

            if (StringUtils.isBlank(response)) {
                throw new RuntimeException("After " + attempt
                        + " attempts to contact the PubMed service, a correct answer could not be received."
                        + " The request was made with this URL:" + uriBuilder.toString());
            }

            return Integer.parseInt(getSingleElementValue(response, "Count"));
        }
    }

    private String getSingleElementValue(String src, String elementName) {
        String value = null;

        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new StringReader(src));
            Element root = document.getRootElement();

            XPathExpression<Element> xpath =
                XPathFactory.instance().compile("//" + elementName, Filters.element());

            Element record = xpath.evaluateFirst(root);
            if (record != null) {
                value = record.getText();
            }
        } catch (JDOMException | IOException e) {
            value = null;
        }
        return value;
    }

    private class GetRecords implements Callable<Collection<ImportRecord>> {

        private Query query;

        private GetRecords(String queryString, int start, int count) {
            query = new Query();
            query.addParameter("query", queryString);
            query.addParameter("start", start);
            query.addParameter("count", count);
        }

        private GetRecords(Query q) {
            this.query = q;
        }

        @Override
        public Collection<ImportRecord> call() throws Exception {
            String queryString = query.getParameterAsClass("query", String.class);
            Integer start = query.getParameterAsClass("start", Integer.class);
            Integer count = query.getParameterAsClass("count", Integer.class);

            if (Objects.isNull(count) || count < 0) {
                count = 10;
            }

            if (Objects.isNull(start) || start < 0) {
                start = 0;
            }

            List<ImportRecord> records = new LinkedList<ImportRecord>();

            URIBuilder uriBuilder = new URIBuilder(urlSearch);
            uriBuilder.addParameter("db", "pubmed");
            uriBuilder.addParameter("retstart", start.toString());
            uriBuilder.addParameter("retmax", count.toString());
            uriBuilder.addParameter("usehistory", "y");
            uriBuilder.addParameter("term", queryString);
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            String response = StringUtils.EMPTY;
            int countAttempt = 0;
            while (StringUtils.isBlank(response) && countAttempt <= attempt) {
                countAttempt++;
                response = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), params);
            }

            if (StringUtils.isBlank(response)) {
                throw new RuntimeException("After " + attempt
                        + " attempts to contact the PubMed service, a correct answer could not be received."
                        + " The request was made with this URL:" + uriBuilder.toString());
            }

            String queryKey = getSingleElementValue(response, "QueryKey");
            String webEnv = getSingleElementValue(response, "WebEnv");

            URIBuilder uriBuilder2 = new URIBuilder(urlFetch);
            uriBuilder2.addParameter("db", "pubmed");
            uriBuilder2.addParameter("retstart", start.toString());
            uriBuilder2.addParameter("retmax", count.toString());
            uriBuilder2.addParameter("WebEnv", webEnv);
            uriBuilder2.addParameter("query_key", queryKey);
            uriBuilder2.addParameter("retmode", "xml");
            Map<String, Map<String, String>> params2 = new HashMap<String, Map<String,String>>();
            String response2 = StringUtils.EMPTY;
            countAttempt = 0;
            while (StringUtils.isBlank(response2) && countAttempt <= attempt) {
                countAttempt++;
                response2 = liveImportClient.executeHttpGetRequest(1000, uriBuilder2.toString(), params2);
            }

            if (StringUtils.isBlank(response2)) {
                throw new RuntimeException("After " + attempt
                        + " attempts to contact the PubMed service, a correct answer could not be received."
                        + " The request was made with this URL:" + uriBuilder2.toString());
            }

            List<Element> elements = splitToRecords(response2);

            for (Element record : elements) {
                records.add(transformSourceRecords(record));
            }

            return records;
        }
    }

    private List<Element> splitToRecords(String recordsSrc) {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new StringReader(recordsSrc));
            Element root = document.getRootElement();

            XPathExpression<Element> xpath =
                XPathFactory.instance().compile("//PubmedArticle", Filters.element());

            List<Element> recordsList = xpath.evaluate(root);
            return recordsList;
        } catch (JDOMException | IOException e) {
            return null;
        }
    }

    private class GetRecord implements Callable<ImportRecord> {

        private Query query;

        private GetRecord(String id) {
            query = new Query();
            query.addParameter("id", id);
        }

        public GetRecord(Query q) {
            query = q;
        }

        @Override
        public ImportRecord call() throws Exception {

            URIBuilder uriBuilder = new URIBuilder(urlFetch);
            uriBuilder.addParameter("db", "pubmed");
            uriBuilder.addParameter("retmode", "xml");
            uriBuilder.addParameter("id", query.getParameterAsClass("id", String.class));

            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            String response = StringUtils.EMPTY;
            int countAttempt = 0;
            while (StringUtils.isBlank(response) && countAttempt <= attempt) {
                countAttempt++;
                response = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), params);
            }

            if (StringUtils.isBlank(response)) {
                throw new RuntimeException("After " + attempt
                        + " attempts to contact the PubMed service, a correct answer could not be received."
                        + " The request was made with this URL:" + uriBuilder.toString());
            }

            List<Element> elements = splitToRecords(response);

            return elements.isEmpty() ? null : transformSourceRecords(elements.get(0));
        }
    }

    private class FindMatchingRecords implements Callable<Collection<ImportRecord>> {

        private Query query;

        private FindMatchingRecords(Item item) throws MetadataSourceException {
            query = getGenerateQueryForItem().generateQueryForItem(item);
        }

        public FindMatchingRecords(Query q) {
            query = q;
        }

        @Override
        public Collection<ImportRecord> call() throws Exception {

            URIBuilder uriBuilder = new URIBuilder(urlSearch);
            uriBuilder.addParameter("db", "pubmed");
            uriBuilder.addParameter("usehistory", "y");
            uriBuilder.addParameter("term", query.getParameterAsClass("term", String.class));
            uriBuilder.addParameter("field", query.getParameterAsClass("field", String.class));

            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            String response = StringUtils.EMPTY;
            int countAttempt = 0;
            while (StringUtils.isBlank(response) && countAttempt <= attempt) {
                countAttempt++;
                response = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), params);
            }

            if (StringUtils.isBlank(response)) {
                throw new RuntimeException("After " + attempt
                        + " attempts to contact the PubMed service, a correct answer could not be received."
                        + " The request was made with this URL:" + uriBuilder.toString());
            }

            String webEnv = getSingleElementValue(response, "WebEnv");
            String queryKey = getSingleElementValue(response, "QueryKey");

            URIBuilder uriBuilder2 = new URIBuilder(urlFetch);
            uriBuilder2.addParameter("db", "pubmed");
            uriBuilder2.addParameter("retmode", "xml");
            uriBuilder2.addParameter("WebEnv", webEnv);
            uriBuilder2.addParameter("query_key", queryKey);

            Map<String, Map<String, String>> params2 = new HashMap<String, Map<String,String>>();
            String response2 = StringUtils.EMPTY;
            countAttempt = 0;
            while (StringUtils.isBlank(response2) && countAttempt <= attempt) {
                countAttempt++;
                response2 = liveImportClient.executeHttpGetRequest(1000, uriBuilder2.toString(), params2);
            }

            if (StringUtils.isBlank(response2)) {
                throw new RuntimeException("After " + attempt
                        + " attempts to contact the PubMed service, a correct answer could not be received."
                        + " The request was made with this URL:" + uriBuilder2.toString());
            }

            return parseXMLString(response2);
        }
    }

    @Override
    public List<ImportRecord> getRecords(InputStream inputStream) throws FileSourceException {
        try (Reader reader = new InputStreamReader(inputStream, "UTF-8")) {
            String xml = CharStreams.toString(reader);
            return parseXMLString(xml);
        } catch (IOException e) {
            throw new FileSourceException ("Cannot read XML from InputStream", e);
        }
    }

    @Override
    public ImportRecord getRecord(InputStream inputStream) throws FileSourceException, FileMultipleOccurencesException {
        List<ImportRecord> importRecord = getRecords(inputStream);
        if (importRecord == null || importRecord.isEmpty()) {
            throw new FileSourceException("Cannot find (valid) record in File");
        } else if (importRecord.size() > 1) {
            throw new FileMultipleOccurencesException("File contains more than one entry");
        } else {
            return importRecord.get(0);
        }
    }

    private List<ImportRecord> parseXMLString(String xml) {
        List<ImportRecord> records = new LinkedList<ImportRecord>();
        List<Element> elements = splitToRecords(xml);
        for (Element record : elements) {
            records.add(transformSourceRecords(record));
        }
        return records;
    }

    public String getUrlFetch() {
        return urlFetch;
    }

    public void setUrlFetch(String urlFetch) {
        this.urlFetch = urlFetch;
    }

    public String getUrlSearch() {
        return urlSearch;
    }

    public void setUrlSearch(String urlSearch) {
        this.urlSearch = urlSearch;
    }

}