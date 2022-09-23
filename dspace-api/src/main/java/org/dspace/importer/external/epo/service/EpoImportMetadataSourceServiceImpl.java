/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.epo.service;

import static org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl.HEADER_PARAMETERS;

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
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.impl.dv.util.Base64;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.contributor.EpoIdMetadataContributor.EpoDocumentId;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.jaxen.JaxenException;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements a data source for querying EPO
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4Science dot it)
 */
public class EpoImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<Element>
        implements QuerySource {

    private final static Logger log = LogManager.getLogger();

    private String url;
    private String authUrl;
    private String searchUrl;

    private String consumerKey;
    private String consumerSecret;

    private MetadataFieldConfig dateFiled;
    private MetadataFieldConfig applicationNumber;

    public static final String APP_NO_DATE_SEPARATOR = "$$$";
    private static final String APP_NO_DATE_SEPARATOR_REGEX = "\\$\\$\\$";

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
        return "epo";
    }

    /**
     * Set the customer epo key
     * @param consumerKey the customer consumer key
     */
    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    /**
     * Set the costumer epo secret
     * @param consumerSecret the customer epo secret
     */
    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setDateFiled(MetadataFieldConfig dateFiled) {
        this.dateFiled = dateFiled;
    }

    public MetadataFieldConfig getDateFiled() {
        return dateFiled;
    }

    public void setApplicationNumber(MetadataFieldConfig applicationNumber) {
        this.applicationNumber = applicationNumber;
    }

    public MetadataFieldConfig getApplicationNumber() {
        return applicationNumber;
    }

    /***
     * Log to EPO, bearer is valid for 20 minutes
     * 
     * @param consumerKey       The consumer Key
     * @param consumerSecretKey The consumer secret key
     * @return
     * @throws IOException
     * @throws HttpException
     */
    protected String login() throws IOException, HttpException {
        Map<String, Map<String, String>> params = getLoginParams();
        String entity = "grant_type=client_credentials";
        String json = liveImportClient.executeHttpPostRequest(this.authUrl, params, entity);
        ObjectMapper mapper = new ObjectMapper(new JsonFactory());
        JsonNode rootNode = mapper.readTree(json);
        JsonNode accessTokenNode = rootNode.get("access_token");
        return accessTokenNode.asText();
    }

    private Map<String, Map<String, String>> getLoginParams() {
        Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
        Map<String, String> headerParams = getLoginHeaderParams();
        params.put(HEADER_PARAMETERS, headerParams);
        return params;
    }

    private Map<String, String> getLoginHeaderParams() {
        Map<String, String> params = new HashMap<String, String>();
        String authString = consumerKey + ":" + consumerSecret;
        params.put("Authorization", "Basic " + Base64.encode(authString.getBytes()));
        params.put("Content-type", "application/x-www-form-urlencoded");
        return params;
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        if (StringUtils.isNotBlank(consumerKey) && StringUtils.isNotBlank(consumerSecret)) {
            try {
                String bearer = login();
                return retry(new CountRecordsCallable(query, bearer));
            } catch (IOException | HttpException e) {
                log.warn(e.getMessage());
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return 0;

    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        if (StringUtils.isNotBlank(consumerKey) && StringUtils.isNotBlank(consumerSecret)) {
            try {
                String bearer = login();
                return retry(new CountRecordsCallable(query, bearer));
            } catch (IOException | HttpException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start,
            int count) throws MetadataSourceException {
        if (StringUtils.isNotBlank(consumerKey) && StringUtils.isNotBlank(consumerSecret)) {
            try {
                String bearer = login();
                return retry(new SearchByQueryCallable(query, bearer, start, count));
            } catch (IOException | HttpException e) {
                log.warn(e.getMessage());
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return new ArrayList<ImportRecord>();
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query)
            throws MetadataSourceException {
        if (StringUtils.isNotBlank(consumerKey) && StringUtils.isNotBlank(consumerSecret)) {
            try {
                String bearer = login();
                return retry(new SearchByQueryCallable(query, bearer));
            } catch (IOException | HttpException e) {
                log.warn(e.getMessage());
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return new ArrayList<ImportRecord>();
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        if (StringUtils.isNotBlank(consumerKey) && StringUtils.isNotBlank(consumerSecret)) {
            try {
                String bearer = login();
                List<ImportRecord> list = retry(new SearchByIdCallable(id, bearer));
                return CollectionUtils.isNotEmpty(list) ? list.get(0) : null;
            } catch (IOException | HttpException e) {
                log.warn(e.getMessage());
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        return null;
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item)
            throws MetadataSourceException {
        return null;
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query)
            throws MetadataSourceException {
        return null;
    }

    /**
     * This class is a Callable implementation to count the number of entries for an EPO query.
     * This Callable use as query value to EPO the string queryString passed to constructor.
     * If the object will be construct through Query.class instance, the value of the Query's
     * map with the key "query" will be used.
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
     */
    private class CountRecordsCallable implements Callable<Integer> {

        private String bearer;
        private String query;

        private CountRecordsCallable(Query query, String bearer) {
            this.query = query.getParameterAsClass("query", String.class);
            this.bearer = bearer;
        }

        private CountRecordsCallable(String query, String bearer) {
            this.query = query;
            this.bearer = bearer;
        }

        public Integer call() throws Exception {
            return countDocument(bearer, query);
        }
    }

    /**
     * This class is a Callable implementation to get an EPO entry using epodocID (epodoc:AB1234567T)
     * The epodocID to use can be passed through the constructor as a String or as Query's map entry, with the key "id".
     *
     * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
     */
    private class SearchByIdCallable implements Callable<List<ImportRecord>> {

        private String id;
        private String bearer;

        private SearchByIdCallable(String id, String bearer) {
            this.id = id;
            this.bearer = bearer;
        }

        public List<ImportRecord> call() throws Exception {
            int positionToSplit = id.indexOf(":");
            String docType = EpoDocumentId.EPODOC;
            String idS = id;
            if (positionToSplit != -1) {
                docType = id.substring(0, positionToSplit);
                idS = id.substring(positionToSplit + 1, id.length());
            } else if (id.contains(APP_NO_DATE_SEPARATOR)) {
                 // special case the id is the combination of the applicationnumber and date filed
                String query = "applicationnumber=" + id.split(APP_NO_DATE_SEPARATOR_REGEX)[0];
                SearchByQueryCallable search = new SearchByQueryCallable(query, bearer, 0, 10);
                List<ImportRecord> records = search.call().stream()
                        .filter(r -> r.getValue(dateFiled.getSchema(), dateFiled.getElement(),
                                    dateFiled.getQualifier())
                                .stream()
                                .anyMatch(m -> StringUtils.equals(m.getValue(),
                                        id.split(APP_NO_DATE_SEPARATOR_REGEX)[1])
                        ))
                        .limit(1).collect(Collectors.toList());
                return records;
            }
            List<ImportRecord> records = searchDocument(bearer, idS, docType);
            if (records.size() > 1) {
                log.warn("More record are returned with epocID " + id);
            }
            return records;
        }
    }

    /**
     * This class is a Callable implementation to get EPO entries based on query object.
     * This Callable use as query value the string queryString passed to constructor.
     * If the object will be construct through Query.class instance, a Query's map entry with key "query" will be used.
     * Pagination is supported too, using the value of the Query's map with keys "start" and "count".
     * 
     * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
     */
    private class SearchByQueryCallable implements Callable<List<ImportRecord>> {

        private Query query;
        private Integer start;
        private Integer count;
        private String bearer;

        private SearchByQueryCallable(Query query, String bearer) {
            this.query = query;
            this.bearer = bearer;
        }

        public SearchByQueryCallable(String queryValue, String bearer, int start, int count) {
            this.query = new Query();
            query.addParameter("query", queryValue);
            this.start = query.getParameterAsClass("start", Integer.class) != null ?
                query.getParameterAsClass("start", Integer.class) : 0;
            this.count = query.getParameterAsClass("count", Integer.class) != null ?
                query.getParameterAsClass("count", Integer.class) : 20;
            this.bearer = bearer;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> records = new ArrayList<ImportRecord>();
            String queryString = query.getParameterAsClass("query", String.class);
            if (StringUtils.isNotBlank(consumerKey) && StringUtils.isNotBlank(consumerSecret)) {
                if (StringUtils.isNotBlank(queryString) && StringUtils.isNotBlank(bearer)) {
                    List<EpoDocumentId> epoDocIds = searchDocumentIds(bearer, queryString, start + 1, count);
                    for (EpoDocumentId epoDocId : epoDocIds) {
                        List<ImportRecord> recordfounds = searchDocument(bearer, epoDocId);
                        if (recordfounds.size() > 1) {
                            log.warn("More record are returned with epocID " + epoDocId.toString());
                        }
                        records.addAll(recordfounds);
                    }
                }

            }
            return records;
        }
    }

    private Integer countDocument(String bearer, String query) {
        if (StringUtils.isBlank(bearer)) {
            return null;
        }
        try {
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            Map<String, String> headerParameters = new HashMap<String, String>();
            headerParameters.put("Authorization", "Bearer " + bearer);
            headerParameters.put("X-OPS-Range", "1-1");
            params.put(HEADER_PARAMETERS, headerParameters);

            URIBuilder uriBuilder = new URIBuilder(this.searchUrl);
            uriBuilder.addParameter("q", query);

            String response = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), params);

            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new StringReader(response));
            Element root = document.getRootElement();

            List<Namespace> namespaces = Arrays.asList(
                 Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink"),
                 Namespace.getNamespace("ops", "http://ops.epo.org"),
                 Namespace.getNamespace("ns", "http://www.epo.org/exchange"));

            String totalRes = getElement(root, namespaces, "//ops:biblio-search/@total-result-count");
            return Integer.parseInt(totalRes);
        } catch (JDOMException | IOException | URISyntaxException | JaxenException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private List<EpoDocumentId> searchDocumentIds(String bearer, String query, int start, int count) {
        List<EpoDocumentId> results = new ArrayList<EpoDocumentId>();
        int end = start + count;
        if (StringUtils.isBlank(bearer)) {
            return results;
        }
        try {
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            Map<String, String> headerParameters = new HashMap<String, String>();
            headerParameters.put("Authorization", "Bearer " + bearer);
            if (start >= 1 && end > start) {
                headerParameters.put("X-OPS-Range", start + "-" + end);
            }
            params.put(HEADER_PARAMETERS, headerParameters);

            URIBuilder uriBuilder = new URIBuilder(this.searchUrl);
            uriBuilder.addParameter("q", query);

            String response = liveImportClient.executeHttpGetRequest(1000, uriBuilder.toString(), params);

            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new StringReader(response));
            Element root = document.getRootElement();

            List<Namespace> namespaces = Arrays.asList(
                 Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink"),
                 Namespace.getNamespace("ops", "http://ops.epo.org"),
                 Namespace.getNamespace("ns", "http://www.epo.org/exchange"));
            XPathExpression<Element> xpath = XPathFactory.instance()
                    .compile("//ns:document-id", Filters.element(), null, namespaces);

            List<Element> documentIds = xpath.evaluate(root);
            for (Element documentId : documentIds) {
                results.add(new EpoDocumentId(documentId, namespaces));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return results;
    }

    private List<ImportRecord> searchDocument(String bearer, EpoDocumentId id) {
        return searchDocument(bearer, id.getId(), id.getDocumentIdType());
    }

    private List<ImportRecord> searchDocument(String bearer, String id, String docType) {
        List<ImportRecord> results = new ArrayList<ImportRecord>();
        if (StringUtils.isBlank(bearer)) {
            return results;
        }
        try {
            Map<String, Map<String, String>> params = new HashMap<String, Map<String,String>>();
            Map<String, String> headerParameters = new HashMap<String, String>();
            headerParameters.put("Authorization", "Bearer " + bearer);
            params.put(HEADER_PARAMETERS, headerParameters);

            String url = this.url.replace("$(doctype)", docType).replace("$(id)", id);

            String response = liveImportClient.executeHttpGetRequest(1000, url, params);
            List<Element> elements = splitToRecords(response);
            for (Element element : elements) {
                results.add(transformSourceRecords(element));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return results;
    }

    private List<Element> splitToRecords(String recordsSrc) {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new StringReader(recordsSrc));
            Element root = document.getRootElement();
            List<Namespace> namespaces = Arrays.asList(Namespace.getNamespace("ns", "http://www.epo.org/exchange"));
            XPathExpression<Element> xpath = XPathFactory.instance().compile("//ns:exchange-document",
                    Filters.element(), null, namespaces);

            List<Element> recordsList = xpath.evaluate(root);
            return recordsList;
        } catch (JDOMException | IOException e) {
            log.error(e.getMessage(), e);
            return new LinkedList<Element>();
        }
    }

    private String getElement(Element document, List<Namespace> namespaces, String path) throws JaxenException {
        XPathExpression<Object> xpath = XPathFactory.instance().compile(path, Filters.fpassthrough(), null, namespaces);
        List<Object> nodes = xpath.evaluate(document);
        //exactly one element expected for any field
        if (CollectionUtils.isEmpty(nodes)) {
            return StringUtils.EMPTY;
        } else {
            return getValue(nodes.get(0));
        }
    }

    private String getValue(Object el) {
        if (el instanceof Element) {
            return ((Element) el).getText();
        } else if (el instanceof Attribute) {
            return ((Attribute) el).getValue();
        } else if (el instanceof String) {
            return (String)el;
        } else if (el instanceof Text) {
            return ((Text) el).getText();
        } else {
            log.error("node of type: " + el.getClass());
            return "";
        }
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public void setSearchUrl(String searchUrl) {
        this.searchUrl = searchUrl;
    }

}
