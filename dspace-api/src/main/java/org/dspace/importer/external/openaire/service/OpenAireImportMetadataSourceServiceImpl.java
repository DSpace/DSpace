/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.openaire.service;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import javax.el.MethodNotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.jaxen.JaxenException;

/**
 * Implements a data source for querying OpenAIRE
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 */
public class OpenAireImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<OMElement>
        implements QuerySource {

    private String baseAddress;

    private WebTarget webTarget;

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
     * Initialize the class
     *
     * @throws Exception on generic exception
     */
    @Override
    public void init() throws Exception {
        Client client = ClientBuilder.newClient();
        if (baseAddress == null) {
            baseAddress = "http://api.openaire.eu/search/publications";
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
                List<OMElement> omElements = splitToRecords(responseString);
                if (omElements != null) {
                    for (OMElement record : omElements) {
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

        String author;

        public CountByQueryCallable(String query) {
            author = query;
        }

        public CountByQueryCallable(Query query) {
            author = query.getParameterAsClass("query", String.class);
        }

        @Override
        public Integer call() throws Exception {
            WebTarget localTarget = webTarget.queryParam("author", author);
            Invocation.Builder invocationBuilder = localTarget.request();
            Response response = invocationBuilder.get();
            if (response.getStatus() == 200) {
                String responseString = response.readEntity(String.class);
                OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(responseString));
                OMElement element = records.getDocumentElement();
                AXIOMXPath xpath = null;
                try {
                    xpath = new AXIOMXPath("/response/header/total");
                    OMElement totalItem = (OMElement) xpath.selectSingleNode(element);
                    return totalItem != null ? Integer.parseInt(totalItem.getText()) : null;
                } catch (JaxenException e) {
                    return 0;
                }
            } else {
                return 0;
            }
        }
    }

    public class SearchByQueryCallable implements Callable<List<ImportRecord>> {

        String author;
        int page;
        int count;

        public SearchByQueryCallable(String query, int start, int count) {
            this.author = query;
            this.page = start / count;
            this.count = count;
        }

        public SearchByQueryCallable(Query query) {
            this.author = query.getParameterAsClass("query", String.class);
            this.page = query.getParameterAsClass("start", Integer.class) /
                query.getParameterAsClass("count", Integer.class);
            this.count = query.getParameterAsClass("count", Integer.class);
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            WebTarget localTarget = webTarget.queryParam("author", author);
            localTarget = localTarget.queryParam("page", page + 1);
            localTarget = localTarget.queryParam("size", count);
            List<ImportRecord> results = new ArrayList<ImportRecord>();
            Invocation.Builder invocationBuilder = localTarget.request();
            Response response = invocationBuilder.get();
            if (response.getStatus() == 200) {
                String responseString = response.readEntity(String.class);
                List<OMElement> omElements = splitToRecords(responseString);
                if (omElements != null) {
                    for (OMElement record : omElements) {
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

    private List<OMElement> splitToRecords(String recordsSrc) {
        OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(recordsSrc));
        OMElement element = records.getDocumentElement();
        AXIOMXPath xpath = null;
        try {
            xpath = new AXIOMXPath("/response/results/result");
            xpath.addNamespace("dri", "http://www.driver-repository.eu/namespace/dri");
            xpath.addNamespace("oaf", "http://namespace.openaire.eu/oaf");
            xpath.addNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            List<OMElement> recordsList = xpath.selectNodes(element);
            return recordsList;
        } catch (JaxenException e) {
            return null;
        }
    }



}
