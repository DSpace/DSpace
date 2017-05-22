/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scopus;

import java.io.*;
import java.util.*;
import java.util.Collection;
import java.util.concurrent.*;
import javax.ws.rs.core.*;
import org.apache.axiom.om.*;
import org.apache.axiom.om.xpath.*;
import org.apache.commons.lang.*;
import org.apache.cxf.jaxrs.client.*;
import org.apache.log4j.*;
import org.dspace.content.*;
import org.dspace.importer.external.datamodel.*;
import org.dspace.importer.external.exception.*;
import org.dspace.importer.external.metadatamapping.*;
import org.dspace.importer.external.scopus.wadl.*;
import org.dspace.importer.external.service.*;
import org.dspace.services.factory.*;
import org.springframework.beans.factory.annotation.*;

/**
 * @author lotte.hofstede at atmire.com
 */
public class ScopusImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<OMElement> {
    private static Logger log = Logger.getLogger(ScopusImportMetadataSourceServiceImpl.class);

    protected String baseAddress;
    protected String view;
    private String apiKey;

    private GenerateQueryForItem_Scopus generateQueryForItem = null;

    @Autowired
    public void setGenerateQueryForItem(GenerateQueryForItem_Scopus generateQueryForItem) {
        this.generateQueryForItem = generateQueryForItem;
    }

    @Override
    public int getNbRecords(String query) throws MetadataSourceException {
        return retry(new GetNbRecords(query));
    }

    @Override
    public int getNbRecords(Query query) throws MetadataSourceException {
        return retry(new GetNbRecords(query));
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        return retry(new GetRecords(query, start, count));
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        return retry(new GetRecords(query));
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        return retry(new GetRecord(id));
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        return retry(new GetRecord(query));
    }

    @Override
    public String getImportSource() {
        return getBaseAddress();
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        return null;
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query q) throws MetadataSourceException {
        return null;
    }

    @Override
    public void init() throws Exception {

    }


    private class GetNbRecords extends AbstractScopusCallable<Integer> {

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
            Response simple = getSearchResponse(query.getParameterAsClass("query", String.class), null, 0, 0);
            if (simple.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new MetadataSourceException(simple.getStatusInfo().getStatusCode() + " " + simple.getStatusInfo().getReasonPhrase());
            }

            String responseString = simple.readEntity(String.class);

            String count = getSingleElementValue(responseString, "opensearch:totalResults");

            try {
                return Integer.parseInt(count);
            } catch (NumberFormatException e) {
                log.error("ScopusImportMetadataSourceServiceImpl: failed to parse number of results, server response: " + responseString);
                return 0;
            }
        }
    }

    private class GetRecords extends AbstractScopusCallable<Collection<ImportRecord>> {

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
            List<ImportRecord> records = new ArrayList<ImportRecord>();

            Response simple = getSearchResponse(query.getParameterAsClass("query", String.class), null,
                    query.getParameterAsClass("start", Integer.class),
                    query.getParameterAsClass("count", Integer.class));
            if (simple.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new MetadataSourceException(simple.getStatusInfo().getStatusCode() + " " + simple.getStatusInfo().getReasonPhrase());
            }

            List<OMElement> omElements = splitToRecords(simple.readEntity(String.class));

            for (OMElement record : omElements) {
                records.add(new ImportRecord(new LinkedList<>(getMetadataFieldMapping().resultToDCValueMapping(record))));
            }

            return records;
        }
    }

    private class GetRecord extends AbstractScopusCallable<ImportRecord> {

        private Query query;

        private GetRecord(String queryString) {
            query = new Query();
            query.addParameter("id", queryString);
        }

        public GetRecord(Query q) {
            query = q;
        }


        @Override
        public ImportRecord call() throws Exception {
            ImportRecord record = null;
            Response simple = getSearchResponse(query.getParameterAsClass("id", String.class), null);
            if (simple.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new MetadataSourceException(simple.getStatusInfo().getStatusCode() + " " + simple.getStatusInfo().getReasonPhrase());
            }

            OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(simple.readEntity(String.class)));
            OMElement element = (OMElement) records.getDocumentElement().getChildrenWithLocalName("entry").next();
            if (element != null) {
                element.declareNamespace("http://www.w3.org/2005/Atom", "a");
                element.declareNamespace("http://purl.org/dc/elements/1.1/", "dc");
                record = new ImportRecord(new LinkedList<>(getMetadataFieldMapping().resultToDCValueMapping(element)));
            }

            return record;
        }
    }

    private class FindMatchingRecords extends AbstractScopusCallable<Collection<ImportRecord>> {

        private Item item;

        private FindMatchingRecords(Item item) {
            this.item = item;
        }

        public FindMatchingRecords(Query q) {
            item = q.getParameterAsClass("item", Item.class);
        }

        @Override
        public Collection<ImportRecord> call() throws Exception {

            String query = generateQueryForItem.generateQueryForItem(item);
            List<ImportRecord> records = performCall(query);

            if (records.size() == 0) {
                String fallbackQuery = generateQueryForItem.generateFallbackQueryForItem(item);
                records = performCall(fallbackQuery);
            }

            return records;
        }

        public List<ImportRecord> performCall(String query) throws Exception {
            List<ImportRecord> records = new LinkedList<ImportRecord>();
            if (query == null) {
                return records;
            }
            Response simple = getSearchResponse(query, "identifier");
            if (simple.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new MetadataSourceException(simple.getStatusInfo().getStatusCode() + " " + simple.getStatusInfo().getReasonPhrase());
            }
            OMElement element = getDocumentElement(simple);

            AXIOMXPath xpath = new AXIOMXPath("/a:search-results/a:entry/dc:identifier");
            xpath.addNamespace("a", "http://www.w3.org/2005/Atom");
            xpath.addNamespace("dc", "http://purl.org/dc/elements/1.1/");
            List<OMElement> recordsList = xpath.selectNodes(element);

            for (OMElement omElement : recordsList) {
                String text = omElement.getText();
                String scopus_id = "SCOPUS_ID:";
                if (text.startsWith(scopus_id)) {
                    String id = text.substring(scopus_id.length());
                    MetadatumDTO dcValue = new MetadatumDTO();
                    dcValue.setSchema("scopus");
                    dcValue.setElement("id");
                    dcValue.setValue(id);
                    ImportRecord record = new ImportRecord(Collections.singletonList(dcValue));
                    records.add(record);
                }
            }

            if (records.size() == 0) {
                String fallbackQuery = generateQueryForItem.generateFallbackQueryForItem(item);
            }

            return records;
        }
    }

    private abstract class AbstractScopusCallable<T> implements Callable<T> {


        protected Response getSearchResponse(String query, String fields) {
            IndexScopusResource scopusResource = JAXRSClientFactory.create(getBaseAddress(), IndexScopusResource.class);
            //      http://api.elsevier.com/content/search/index:SCOPUS?query=DOI(10.1007/s10439-010-0201-5)&field=citedby-count&apiKey=7f8c024a802ae228658bb08c974dbefb
            return scopusResource.simple("application/xml", null, null, null, null, null, null, getApiKey(), null, null, query, getView(), fields,
                    null, null, null, null, null, null, null, null);
        }

        protected Response getSearchResponse(String query, String fields, int start, int count) {
            IndexScopusResource scopusResource = JAXRSClientFactory.create(getBaseAddress(), IndexScopusResource.class);
            //      http://api.elsevier.com/content/search/index:SCOPUS?query=DOI(10.1007/s10439-010-0201-5)&field=citedby-count&apiKey=7f8c024a802ae228658bb08c974dbefb
            return scopusResource.simple("application/xml", null, null, null, null, null, null, getApiKey(), null, null, query, getView(), fields,
                    null, Integer.toString(start), Integer.toString(count), null, null, null, null, null);
        }

        protected OMElement getDocumentElement(Response simple) {
            InputStream inputStream = simple.readEntity(InputStream.class);
            OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(inputStream);
            return records.getDocumentElement();
        }
    }


    public String getBaseAddress() {
        if(baseAddress == null){
            baseAddress = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("external-sources.scopus.url");
        }

        return baseAddress;
    }

    public String getView() {
        if(view == null){
            view = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("external-sources.scopus.view");
        }

        return view;
    }

    public String getApiKey() {
        if(StringUtils.isBlank(apiKey)){
            apiKey = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("external-sources.elsevier.key");
        }

        return apiKey;
    }
}
