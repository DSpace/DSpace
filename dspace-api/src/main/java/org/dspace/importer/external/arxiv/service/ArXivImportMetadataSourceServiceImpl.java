/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.arxiv.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.jaxen.JaxenException;

public class ArXivImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<OMElement> {
    private int timeout = 1000;

    /**
     * How long to wait for a connection to be established.
     *
     * @param timeout milliseconds
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
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
        return null;
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

            HttpGet method = null;
            try {
                HttpClient client = new DefaultHttpClient();
                HttpParams params = client.getParams();
                params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);

                try {
                    URIBuilder uriBuilder = new URIBuilder("http://export.arxiv.org/api/query");
                    uriBuilder.addParameter("search_query", queryString);
                    if (maxResult != null) {
                        uriBuilder.addParameter("max_results", String.valueOf(maxResult));
                    }
                    if (start != null) {
                        uriBuilder.addParameter("start", String.valueOf(start));
                    }
                    method = new HttpGet(uriBuilder.build());
                } catch (URISyntaxException ex) {
                    throw new HttpException(ex.getMessage());
                }

                // Execute the method.
                HttpResponse response = client.execute(method);
                StatusLine responseStatus = response.getStatusLine();
                int statusCode = responseStatus.getStatusCode();

                if (statusCode != HttpStatus.SC_OK) {
                    if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                        throw new RuntimeException("arXiv query is not valid");
                    } else {
                        throw new RuntimeException("Http call failed: "
                                                       + responseStatus);
                    }
                }

                try {
                    InputStreamReader isReader = new InputStreamReader(response.getEntity().getContent());
                    BufferedReader reader = new BufferedReader(isReader);
                    StringBuilder sb = new StringBuilder();
                    String str;
                    while ((str = reader.readLine()) != null) {
                        sb.append(str);
                    }
                    System.out.println("XML: " + sb.toString());
                    List<OMElement> omElements = splitToRecords(sb.toString());
                    for (OMElement record : omElements) {
                        results.add(transformSourceRecords(record));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(
                         "ArXiv identifier is not valid or not exist");
                }
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
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
            HttpGet method = null;
            try {
                HttpClient client = new DefaultHttpClient();
                HttpParams params = client.getParams();
                params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
                try {
                    URIBuilder uriBuilder = new URIBuilder("http://export.arxiv.org/api/query");
                    if (StringUtils.isNotBlank(arxivid)) {
                        arxivid = arxivid.trim();
                        if (arxivid.startsWith("http://arxiv.org/abs/")) {
                            arxivid = arxivid.substring("http://arxiv.org/abs/".length());
                        } else if (arxivid.toLowerCase().startsWith("arxiv:")) {
                            arxivid = arxivid.substring("arxiv:".length());
                        }
                        uriBuilder.addParameter("id_list", arxivid);
                        method = new HttpGet(uriBuilder.build());
                    }
                } catch (URISyntaxException ex) {
                    throw new HttpException(ex.getMessage());
                }

                // Execute the method.
                HttpResponse response = client.execute(method);
                StatusLine responseStatus = response.getStatusLine();
                int statusCode = responseStatus.getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                        throw new RuntimeException("arXiv query is not valid");
                    } else {
                        throw new RuntimeException("Http call failed: "
                                                     + responseStatus);
                    }
                }
                try {
                    InputStreamReader isReader = new InputStreamReader(response.getEntity().getContent());
                    BufferedReader reader = new BufferedReader(isReader);
                    StringBuffer sb = new StringBuffer();
                    String str;
                    while ((str = reader.readLine()) != null) {
                        sb.append(str);
                    }
                    List<OMElement> omElements = splitToRecords(sb.toString());
                    for (OMElement record : omElements) {
                        results.add(transformSourceRecords(record));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(
                       "ArXiv identifier is not valid or not exist");
                }
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
            return results;
        }
    }

    private class FindMatchingRecordCallable implements Callable<List<ImportRecord>> {
        private Query query;

        private FindMatchingRecordCallable(Item item) throws MetadataSourceException {
            query = getGenerateQueryForItem().generateQueryForItem(item);
        }

        public FindMatchingRecordCallable(Query q) {
            query = q;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            return null;
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
