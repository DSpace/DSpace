/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.arxiv.service;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.jaxen.JaxenException;

public class ArXivImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<OMElement> {

    private WebTarget webTarget;

    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query, count, start));
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query));
    }

    @Override
    public int getNbRecords(String query) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByQueryCallable(query, null, null));
        return records != null ? records.size() : 0;
    }

    @Override
    public int getNbRecords(Query query) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByQueryCallable(query));
        return records != null ? records.size() : 0;
    }


    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByIdCallable(id));
        if (records != null && records.size() > 1) {
            throw new MetadataSourceException("More than one result found");
        }
        return records == null ? null : records.get(0);
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        List<ImportRecord> records = retry(new SearchByIdCallable(query));
        if (records != null && records.size() > 1) {
            throw new MetadataSourceException("More than one result found");
        }
        return records == null ? null : records.get(0);
    }


    @Override
    public void init() throws Exception {
        Client client = ClientBuilder.newClient();
        webTarget = client.target("http://export.arxiv.org/api/query");
    }






    @Override
    public String getImportSource() {
        return "arxiv";
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new RuntimeException();
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        return retry(new FindMatchingRecordCallable(query));
    }

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
            String responseString = response.readEntity(String.class);
            List<OMElement> omElements = splitToRecords(responseString);
            for (OMElement record : omElements) {
                results.add(transformSourceRecords(record));
            }
            return results;
        }
    }

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
            String responseString = response.readEntity(String.class);
            List<OMElement> omElements = splitToRecords(responseString);
            for (OMElement record : omElements) {
                results.add(transformSourceRecords(record));
            }
            return results;
        }
    }

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
            String responseString = response.readEntity(String.class);
            List<OMElement> omElements = splitToRecords(responseString);
            for (OMElement record : omElements) {
                results.add(transformSourceRecords(record));
            }
            return results;
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

    private static List<OMElement> splitToRecords(String recordsSrc) {
        OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(recordsSrc));
        OMElement element = records.getDocumentElement();
        AXIOMXPath xpath = null;
        try {
            xpath = new AXIOMXPath("ns:entry");
            xpath.addNamespace("ns", "http://www.w3.org/2005/Atom");
            List<OMElement> recordsList = xpath.selectNodes(element);
            return recordsList;
        } catch (JaxenException e) {
            return null;
        }
    }

}
