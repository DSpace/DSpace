/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.openaire.service;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import jakarta.el.MethodNotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.dspace.services.ConfigurationService;
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
 * Implements a data source for querying OpenAIRE
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 */
public class OpenAireImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<Element>
        implements QuerySource {

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    private String baseAddress;

    private WebTarget webTarget;

    private String queryParam;

    @Override
    public String getImportSource() {
        return "openaire";
    }

    /**
     * The string that identifies this import implementation. Preferable a URI
     *
     * @return the identifying uri
     */
    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        return retry(new SearchByIdCallable(id));
    }

    /**
     * The string that identifies this import implementation. Preferable a URI
     *
     * @return the identifying uri
     */
    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        return retry(new SearchByIdCallable(query));
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
     * @param query a query object to base the search on.
     * @return the sum of the matching records over this import source
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query));
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
        return retry(new SearchByQueryCallable(query, start, count));
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
        return retry(new SearchByQueryCallable(query));
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for OpenAIRE");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for OpenAIRE");
    }

    /**
     * Set the baseAddress to this object
     *
     * @param baseAddress The String object that represents the baseAddress of this object
     */
    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
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
     * Set the name of the query param, this correspond to the index used (title, author)
     * 
     * @param queryParam on which index make the query
     */
    public void setQueryParam(String queryParam) {
        this.queryParam = queryParam;
    }

    /**
     * Get the name of the query param for the rest call
     * 
     * @return the name of the query param, i.e. the index (title, author) to use
     */
    public String getQueryParam() {
        return queryParam;
    }
    /**
     * Initialize the class
     *
     * @throws Exception on generic exception
     */
    @Override
    public void init() throws Exception {
        Client client = ClientBuilder.newClient();
        if (baseAddress == null) {
            baseAddress = configurationService.getProperty("openaire.search.url",
                                                           "https://api.openaire.eu/search/publications");
        }
        if (queryParam == null) {
            queryParam = "title";
        }
        webTarget = client.target(baseAddress);
    }

    public class SearchByIdCallable  implements Callable<ImportRecord> {

        String id = null;

        public SearchByIdCallable(String id) {
            this.id = id;
        }

        public SearchByIdCallable(Query query) {
            this.id = query.getParameterAsClass("id", String.class);
        }

        @Override
        public ImportRecord call() throws Exception {
            List<ImportRecord> results = new ArrayList<ImportRecord>();
            WebTarget localTarget = webTarget.queryParam("openairePublicationID", id);
            Invocation.Builder invocationBuilder = localTarget.request();
            Response response = invocationBuilder.get();
            if (response.getStatus() == 200) {
                String responseString = response.readEntity(String.class);
                List<Element> omElements = splitToRecords(responseString);
                if (omElements != null) {
                    for (Element record : omElements) {
                        results.add(filterMultipleTitles(transformSourceRecords(record)));
                    }
                }
                return results != null ? results.get(0) : null;
            } else {
                return null;
            }
        }
    }

    public class CountByQueryCallable implements Callable<Integer> {

        String q;

        public CountByQueryCallable(String query) {
            q = query;
        }

        public CountByQueryCallable(Query query) {
            q = query.getParameterAsClass("query", String.class);
        }

        @Override
        public Integer call() throws Exception {
            WebTarget localTarget = webTarget.queryParam(queryParam, q);
            Invocation.Builder invocationBuilder = localTarget.request();
            Response response = invocationBuilder.get();
            if (response.getStatus() == 200) {
                String responseString = response.readEntity(String.class);

                SAXBuilder saxBuilder = new SAXBuilder();
                Document document = saxBuilder.build(new StringReader(responseString));
                Element root = document.getRootElement();

                XPathExpression<Element> xpath = XPathFactory.instance().compile("//header/total",
                    Filters.element(), null);

                Element totalItem = xpath.evaluateFirst(root);
                return totalItem != null ? Integer.parseInt(totalItem.getText()) : null;

            } else {
                return 0;
            }
        }
    }

    public class SearchByQueryCallable implements Callable<List<ImportRecord>> {

        String q;
        int page;
        int count;

        public SearchByQueryCallable(String query, int start, int count) {
            this.q = query;
            this.page = start / count;
            this.count = count;
        }

        public SearchByQueryCallable(Query query) {
            this.q = query.getParameterAsClass("query", String.class);
            this.page = query.getParameterAsClass("start", Integer.class) /
                query.getParameterAsClass("count", Integer.class);
            this.count = query.getParameterAsClass("count", Integer.class);
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            WebTarget localTarget = webTarget.queryParam(queryParam, q);
            localTarget = localTarget.queryParam("page", page + 1);
            localTarget = localTarget.queryParam("size", count);
            List<ImportRecord> results = new ArrayList<ImportRecord>();
            Invocation.Builder invocationBuilder = localTarget.request();
            Response response = invocationBuilder.get();
            if (response.getStatus() == 200) {
                String responseString = response.readEntity(String.class);
                List<Element> omElements = splitToRecords(responseString);
                if (omElements != null) {
                    for (Element record : omElements) {
                        results.add(filterMultipleTitles(transformSourceRecords(record)));
                    }
                }
            }
            return results;
        }
    }

    /**
     * This method remove multiple titles occurrences
     * 
     * @param transformSourceRecords
     * @return ImportRecord with one or zero title
     */
    private ImportRecord filterMultipleTitles(ImportRecord transformSourceRecords) {
        List<MetadatumDTO> metadata = (List<MetadatumDTO>)transformSourceRecords.getValueList();
        ArrayList<MetadatumDTO> nextSourceRecord = new ArrayList<>();
        boolean found = false;
        for (MetadatumDTO dto : metadata) {
            if ("dc".equals(dto.getSchema()) && "title".equals(dto.getElement()) && dto.getQualifier() == null) {
                if (!found) {
                    nextSourceRecord.add(dto);
                    found = true;
                }
            } else {
                nextSourceRecord.add(dto);
            }
        }
        return new ImportRecord(nextSourceRecord);
    }

    private List<Element> splitToRecords(String recordsSrc) {

        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new StringReader(recordsSrc));
            Element root = document.getRootElement();

            List namespaces = Arrays.asList(
                Namespace.getNamespace("dri", "http://www.driver-repository.eu/namespace/dri"),
                Namespace.getNamespace("oaf", "http://namespace.openaire.eu/oaf"),
                Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance"));
            XPathExpression<Element> xpath = XPathFactory.instance().compile("//results/result",
                Filters.element(), null, namespaces);

            List<Element> recordsList = xpath.evaluate(root);
            return recordsList;
        } catch (JDOMException | IOException e) {
            return null;
        }
    }



}
