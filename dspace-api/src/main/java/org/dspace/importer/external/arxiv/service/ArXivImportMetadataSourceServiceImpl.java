/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.arxiv.service;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import javax.el.MethodNotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * Implements a data source for querying ArXiv
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4Science dot it)
 *
 */
public class ArXivImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<Element>
    implements QuerySource {

    private WebTarget webTarget;
    private String baseAddress;

    /**
     * Find the number of records matching the query string in ArXiv. Supports pagination.
     *
     * @param query a query string to base the search on.
     * @param start offset to start at
     * @param count number of records to retrieve.
     * @return a set of records. Fully transformed.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query, count, start));
    }

    /**
     * Find records based on a object query and convert them to a list metadata mapped in ImportRecord.
     * The entry with the key "query" of the Query's map will be used as query string value.
     * 
     * @see org.dspace.importer.external.datamodel.Query
     * @see org.dspace.importer.external.datamodel.ImportRecord
     * @param query a query object to base the search on.
     * @return a set of records. Fully transformed.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query));
    }

    /**
     * Find the number of records matching the query string in ArXiv;
     *
     * @param query a query object to base the search on.
     * @return the sum of the matching records over this import source
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query));
    }


    /**
     * Find the number of records matching a query;
     * The entry with the key "query" of the Query's map will be used to get the query string.
     * 
     * @see org.dspace.importer.external.datamodel.Query
     * @param query a query string to base the search on.
     * @return the sum of the matching records over this import source
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query));
    }

    /**
     * Get a single record of metadata from the arxiv by ArXiv ID.
     *
     * @param id id of the record in ArXiv
     * @return the first matching record
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByIdCallable(id));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    /**
     * Get a single record from the ArXiv matching the query.
     * Field "query" will be used to get data from.
     * 
     * @see org.dspace.importer.external.datamodel.Query
     * @param query a query matching a single record
     * @return the first matching record
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByIdCallable(query));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    /**
     * Initialize the class
     *
     * @throws Exception on generic exception
     */
    @Override
    public void init() throws Exception {
        Client client = ClientBuilder.newClient();
        webTarget = client.target(baseAddress);
    }

    /**
     * The string that identifies this import implementation. Preferable a URI
     *
     * @return the identifying uri
     */
    @Override
    public String getImportSource() {
        return "arxiv";
    }

    /**
     * Expect this method will be not used and erased from the interface soon
     */
    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        // FIXME: we need this method?
        throw new MethodNotFoundException("This method is not implemented for ArXiv");
    }

    /**
     * Finds records based on query object.
     * Supports search by title and/or author
     *
     * @param query a query object to base the search on.
     * @return a collection of import records.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        return retry(new FindMatchingRecordCallable(query));
    }

    /**
     * This class is a Callable implementation to count the number of entries for an ArXiv
     * query.
     * This Callable use as query value to ArXiv the string queryString passed to constructor.
     * If the object will be construct through Query.class instance, the value of the Query's
     * map with the key "query" will be used.
     * 
     * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
     *
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
            String queryString = query.getParameterAsClass("query", String.class);
            Integer start = query.getParameterAsClass("start", Integer.class);
            Integer maxResult = query.getParameterAsClass("count", Integer.class);
            WebTarget local = webTarget.queryParam("search_query", queryString);
            if (maxResult != null) {
                local = local.queryParam("max_results", String.valueOf(maxResult));
            }
            if (start != null) {
                local = local.queryParam("start", String.valueOf(start));
            }
            Invocation.Builder invocationBuilder = local.request(MediaType.TEXT_PLAIN_TYPE);
            Response response = invocationBuilder.get();
            if (response.getStatus() == 200) {
                String responseString = response.readEntity(String.class);

                SAXBuilder saxBuilder = new SAXBuilder();
                Document document = saxBuilder.build(new StringReader(responseString));
                Element root = document.getRootElement();

                List namespaces = Arrays.asList(Namespace.getNamespace("opensearch",
                                                      "http://a9.com/-/spec/opensearch/1.1/"));
                XPathExpression<Element> xpath =
                    XPathFactory.instance().compile("opensearch:totalResults", Filters.element(), null, namespaces);

                Element count = xpath.evaluateFirst(root);
                try {
                    return Integer.parseInt(count.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    /**
     * This class is a Callable implementation to get ArXiv entries based on
     * query object.
     * This Callable use as query value the string queryString passed to constructor.
     * If the object will be construct through Query.class instance, a Query's map entry with key "query" will be used.
     * Pagination is supported too, using the value of the Query's map with keys "start" and "count".
     * 
     * @see org.dspace.importer.external.datamodel.Query
     * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
     *
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
            List<ImportRecord> results = new ArrayList<ImportRecord>();
            String queryString = query.getParameterAsClass("query", String.class);
            Integer start = query.getParameterAsClass("start", Integer.class);
            Integer maxResult = query.getParameterAsClass("count", Integer.class);
            WebTarget local = webTarget.queryParam("search_query", queryString);
            if (maxResult != null) {
                local = local.queryParam("max_results", String.valueOf(maxResult));
            }
            if (start != null) {
                local = local.queryParam("start", String.valueOf(start));
            }
            Invocation.Builder invocationBuilder = local.request(MediaType.TEXT_PLAIN_TYPE);
            Response response = invocationBuilder.get();
            if (response.getStatus() == 200) {
                String responseString = response.readEntity(String.class);
                List<Element> elements = splitToRecords(responseString);
                for (Element record : elements) {
                    results.add(transformSourceRecords(record));
                }
                return results;
            } else {
                return null;
            }
        }
    }

    /**
     * This class is a Callable implementation to get an ArXiv entry using ArXiv ID
     * The ID to use can be passed through the constructor as a String or as Query's map entry, with the key "id".
     *
     * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
     *
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
            List<ImportRecord> results = new ArrayList<ImportRecord>();
            String arxivid = query.getParameterAsClass("id", String.class);
            if (StringUtils.isNotBlank(arxivid)) {
                arxivid = arxivid.trim();
                if (arxivid.startsWith("http://arxiv.org/abs/")) {
                    arxivid = arxivid.substring("http://arxiv.org/abs/".length());
                } else if (arxivid.toLowerCase().startsWith("arxiv:")) {
                    arxivid = arxivid.substring("arxiv:".length());
                }
            }
            WebTarget local = webTarget.queryParam("id_list", arxivid);
            Invocation.Builder invocationBuilder = local.request(MediaType.TEXT_PLAIN_TYPE);
            Response response = invocationBuilder.get();
            if (response.getStatus() == 200) {
                String responseString = response.readEntity(String.class);
                List<Element> elements = splitToRecords(responseString);
                for (Element record : elements) {
                    results.add(transformSourceRecords(record));
                }
                return results;
            } else {
                return null;
            }
        }
    }

    /**
     * This class is a Callable implementation to search ArXiv entries
     * using author and title.
     * There are two field in the Query map to pass, with keys "title" and "author"
     * (at least one must be used).
     * 
     * @see org.dspace.importer.external.datamodel.Query
     * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
     *
     */
    private class FindMatchingRecordCallable implements Callable<List<ImportRecord>> {

        private Query query;

        private FindMatchingRecordCallable(Query q) {
            query = q;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            String queryString = getQuery(this.query);
            List<ImportRecord> results = new ArrayList<ImportRecord>();
            WebTarget local = webTarget.queryParam("search_query", queryString);
            Invocation.Builder invocationBuilder = local.request(MediaType.TEXT_PLAIN_TYPE);
            Response response = invocationBuilder.get();
            if (response.getStatus() == 200) {
                String responseString = response.readEntity(String.class);
                List<Element> elements = splitToRecords(responseString);
                for (Element record : elements) {
                    results.add(transformSourceRecords(record));
                }
                return results;
            } else {
                return null;
            }
        }

        private String getQuery(Query query) {
            String title = query.getParameterAsClass("title", String.class);
            String author = query.getParameterAsClass("author", String.class);
            StringBuffer queryString = new StringBuffer();
            if (StringUtils.isNotBlank(title)) {
                queryString.append("ti:\"").append(title).append("\"");
            }
            if (StringUtils.isNotBlank(author)) {
                // [FAU]
                if (queryString.length() > 0) {
                    queryString.append(" AND ");
                }
                queryString.append("au:\"").append(author).append("\"");
            }
            return queryString.toString();
        }
    }

    private List<Element> splitToRecords(String recordsSrc) {

        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new StringReader(recordsSrc));
            Element root = document.getRootElement();

            List namespaces = Arrays.asList(Namespace.getNamespace("ns",
                                                                   "http://www.w3.org/2005/Atom"));
            XPathExpression<Element> xpath =
                XPathFactory.instance().compile("ns:entry", Filters.element(), null, namespaces);

            List<Element> recordsList = xpath.evaluate(root);
            return recordsList;
        } catch (JDOMException | IOException e) {
            return null;
        }
    }

    /**
     * Return the baseAddress set to this object
     *
     * @return The String object that represents the baseAddress of this object
     */
    public String getBaseAddress() {
        return baseAddress;
    }

    /**
     * Set the baseAddress to this object
     *
     * @param baseAddress The String object that represents the baseAddress of this object
     */
    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
    }
}
