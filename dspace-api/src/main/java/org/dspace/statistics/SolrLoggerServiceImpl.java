/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.luke.FieldFlag;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.util.NamedList;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.service.ClientInfoService;
import org.dspace.services.ConfigurationService;
import org.dspace.statistics.service.SolrLoggerService;
import org.dspace.statistics.util.LocationUtils;
import org.dspace.statistics.util.SpiderDetector;
import org.dspace.usage.UsageWorkflowEvent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Static holder for a HttpSolrClient connection pool to issue
 * usage logging events to Solr from DSpace libraries, and some static query
 * composers.
 *
 * @author ben at atmire.com
 * @author kevinvandevelde at atmire.com
 * @author mdiggory at atmire.com
 */
public class SolrLoggerServiceImpl implements SolrLoggerService, InitializingBean {

    private static final Logger log = LogManager.getLogger();

    private static final String MULTIPLE_VALUES_SPLITTER = "|";
    protected SolrClient solr;

    public static final String DATE_FORMAT_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String DATE_FORMAT_DCDATE = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    protected DatabaseReader locationService;

    protected boolean useProxies;

    private static final List<String> statisticYearCores = new ArrayList<>();
    private static boolean statisticYearCoresInit = false;

    private static final String IP_V4_REGEX = "^((?:\\d{1,3}\\.){3})\\d{1,3}$";
    private static final String IP_V6_REGEX = "^(.*):.*:.*$";

    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected ContentServiceFactory contentServiceFactory;
    @Autowired(required = true)
    private ConfigurationService configurationService;
    @Autowired(required = true)
    private ClientInfoService clientInfoService;
    @Autowired
    private SolrStatisticsCore solrStatisticsCore;
    @Autowired
    private GeoIpService geoIpService;

    /** URL to the current-year statistics core.  Prior-year shards will have a year suffixed. */
    private String statisticsCoreURL;

    /** Name of the current-year statistics core.  Prior-year shards will have a year suffixed. */
    private String statisticsCoreBase;

    public static enum StatisticsType {
        VIEW("view"),
        SEARCH("search"),
        SEARCH_RESULT("search_result"),
        WORKFLOW("workflow");

        private final String text;

        StatisticsType(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }
    }

    protected SolrLoggerServiceImpl() {

    }


    @Override
    public void afterPropertiesSet() throws Exception {
        solr = solrStatisticsCore.getSolr();

        // Read in the file so we don't have to do it all the time
        //spiderIps = SpiderDetector.getSpiderIpAddresses();

        DatabaseReader service = null;
        try {
            service = geoIpService.getDatabaseReader();
        } catch (IllegalStateException ex) {
            log.error(ex);
        }
        locationService = service;
    }

    @Override
    public void post(DSpaceObject dspaceObject, HttpServletRequest request,
                     EPerson currentUser) {
        postView(dspaceObject, request, currentUser);
    }

    @Override
    public void postView(DSpaceObject dspaceObject, HttpServletRequest request,
                         EPerson currentUser) {
        if (solr == null || locationService == null) {
            return;
        }
        initSolrYearCores();


        try {
            SolrInputDocument doc1 = getCommonSolrDoc(dspaceObject, request, currentUser);
            if (doc1 == null) {
                return;
            }
            if (dspaceObject instanceof Bitstream) {
                Bitstream bit = (Bitstream) dspaceObject;
                List<Bundle> bundles = bit.getBundles();
                for (Bundle bundle : bundles) {
                    doc1.addField("bundleName", bundle.getName());
                }
            }

            doc1.addField("statistics_type", StatisticsType.VIEW.text());


            solr.add(doc1);
            // commits are executed automatically using the solr autocommit
            boolean useAutoCommit = configurationService.getBooleanProperty("solr-statistics.autoCommit", true);
            if (!useAutoCommit) {
                solr.commit(false, false);
            }

        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            String email = null == currentUser ? "[anonymous]" : currentUser.getEmail();
            log.error("Error saving VIEW event to Solr for DSpaceObject {} by EPerson {}",
                      dspaceObject.getID(), email, e);
        }
    }

    @Override
    public void postView(DSpaceObject dspaceObject,
                         String ip, String userAgent, String xforwardedfor, EPerson currentUser) {
        if (solr == null || locationService == null) {
            return;
        }
        initSolrYearCores();

        try {
            SolrInputDocument doc1 = getCommonSolrDoc(dspaceObject, ip, userAgent, xforwardedfor,
                                                      currentUser);
            if (doc1 == null) {
                return;
            }
            if (dspaceObject instanceof Bitstream) {
                Bitstream bit = (Bitstream) dspaceObject;
                List<Bundle> bundles = bit.getBundles();
                for (Bundle bundle : bundles) {
                    doc1.addField("bundleName", bundle.getName());
                }
            }

            doc1.addField("statistics_type", StatisticsType.VIEW.text());

            solr.add(doc1);
            // commits are executed automatically using the solr autocommit
            boolean useAutoCommit = configurationService.getBooleanProperty("solr-statistics.autoCommit", true);
            if (!useAutoCommit) {
                solr.commit(false, false);
            }

        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            log.error("Error saving VIEW event to Solr for DSpaceObject {} by EPerson {}",
                      dspaceObject.getID(), currentUser.getEmail(), e);
        }
    }

    /**
     * Returns a solr input document containing common information about the statistics
     * regardless if we are logging a search or a view of a DSpace object
     *
     * @param dspaceObject the object used.
     * @param request      the current request context.
     * @param currentUser  the current session's user.
     * @return a solr input document
     * @throws SQLException in case of a database exception
     */
    protected SolrInputDocument getCommonSolrDoc(DSpaceObject dspaceObject, HttpServletRequest request,
                                                 EPerson currentUser) throws SQLException {
        boolean isSpiderBot = request != null && SpiderDetector.isSpider(request);
        if (isSpiderBot &&
            !configurationService.getBooleanProperty("usage-statistics.logBots", true)) {
            return null;
        }

        SolrInputDocument doc1 = new SolrInputDocument();
        // Save our basic info that we already have

        if (request != null) {
            String ip = clientInfoService.getClientIp(request);
            if (configurationService.getBooleanProperty("anonymize_statistics.anonymize_on_log", false)) {
                try {
                    doc1.addField("ip", anonymizeIp(ip));
                } catch (UnknownHostException e) {
                    log.warn(e.getMessage(), e);
                }
            } else {
                doc1.addField("ip", ip);
            }

            //Also store the referrer
            if (request.getHeader("referer") != null) {
                doc1.addField("referrer", request.getHeader("referer"));
            }

            InetAddress ipAddress = null;
            try {
                String dns;
                if (!configurationService.getBooleanProperty("anonymize_statistics.anonymize_on_log", false)) {
                    ipAddress = InetAddress.getByName(ip);
                    dns = ipAddress.getHostName();
                } else {
                    dns = configurationService.getProperty("anonymize_statistics.dns_mask", "anonymized");
                }
                doc1.addField("dns", dns.toLowerCase(Locale.ROOT));
            } catch (UnknownHostException e) {
                log.info("Failed DNS Lookup for IP:  {}", ip);
                log.debug(e.getMessage(), e);
            }
            if (request.getHeader("User-Agent") != null) {
                doc1.addField("userAgent", request.getHeader("User-Agent"));
            }
            doc1.addField("isBot", isSpiderBot);
            // Save the location information if valid, save the event without
            // location information if not valid
            if (locationService != null && ipAddress != null) {
                try {
                    CityResponse location = locationService.city(ipAddress);
                    String countryCode = location.getCountry().getIsoCode();
                    double latitude = location.getLocation().getLatitude();
                    double longitude = location.getLocation().getLongitude();
                    if (!(
                            "--".equals(countryCode)
                            && latitude == -180
                            && longitude == -180)
                    ) {
                        try {
                            doc1.addField("continent", LocationUtils
                                .getContinentCode(countryCode));
                        } catch (Exception e) {
                            log.warn("Failed to load country/continent table: {}", countryCode);
                        }
                        doc1.addField("countryCode", countryCode);
                        doc1.addField("city", location.getCity().getName());
                        doc1.addField("latitude", latitude);
                        doc1.addField("longitude", longitude);
                    }
                } catch (IOException e) {
                    log.warn("GeoIP lookup failed.", e);
                } catch (GeoIp2Exception e) {
                    log.info("Unable to get location of request: {}", e.getMessage());
                }
            }
        }

        if (dspaceObject != null) {
            doc1.addField("id", dspaceObject.getID().toString());
            doc1.addField("type", dspaceObject.getType());
            storeParents(doc1, dspaceObject);
        }
        // Save the current time
        doc1.addField("time", DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
        if (currentUser != null) {
            doc1.addField("epersonid", currentUser.getID().toString());
        }

        return doc1;
    }

    protected SolrInputDocument getCommonSolrDoc(DSpaceObject dspaceObject, String ip, String userAgent,
                                                 String xforwardedfor, EPerson currentUser) throws SQLException {
        boolean isSpiderBot = SpiderDetector.isSpider(ip);
        if (isSpiderBot &&
            !configurationService.getBooleanProperty("usage-statistics.logBots", true)) {
            return null;
        }

        SolrInputDocument doc1 = new SolrInputDocument();
        // Save our basic info that we already have

        ip = clientInfoService.getClientIp(ip, xforwardedfor);
        if (configurationService.getBooleanProperty("anonymize_statistics.anonymize_on_log", false)) {
            try {
                doc1.addField("ip", anonymizeIp(ip));
            } catch (UnknownHostException e) {
                log.warn(e.getMessage(), e);
            }
        } else {
            doc1.addField("ip", ip);
        }

        InetAddress ipAddress = null;
        try {
            String dns;
            if (!configurationService.getBooleanProperty("anonymize_statistics.anonymize_on_log", false)) {
                ipAddress = InetAddress.getByName(ip);
                dns = ipAddress.getHostName();
            } else {
                dns = configurationService.getProperty("anonymize_statistics.dns_mask", "anonymized");
            }
            doc1.addField("dns", dns.toLowerCase(Locale.ROOT));
        } catch (UnknownHostException e) {
            log.info("Failed DNS Lookup for IP:  {}", ip);
            log.debug(e.getMessage(), e);
        }
        if (userAgent != null) {
            doc1.addField("userAgent", userAgent);
        }
        doc1.addField("isBot", isSpiderBot);
        // Save the location information if valid, save the event without
        // location information if not valid
        if (locationService != null) {
            try {
                CityResponse location = locationService.city(ipAddress);
                String countryCode = location.getCountry().getIsoCode();
                double latitude = location.getLocation().getLatitude();
                double longitude = location.getLocation().getLongitude();
                if (!(
                        "--".equals(countryCode)
                                && latitude == -180
                                && longitude == -180)
                ) {
                    try {
                        doc1.addField("continent", LocationUtils
                                .getContinentCode(countryCode));
                    } catch (Exception e) {
                        System.out
                                .println("COUNTRY ERROR: " + countryCode);
                    }
                    doc1.addField("countryCode", countryCode);
                    doc1.addField("city", location.getCity().getName());
                    doc1.addField("latitude", latitude);
                    doc1.addField("longitude", longitude);
                }
            } catch (IOException e) {
                log.warn("GeoIP lookup failed.", e);
            } catch (GeoIp2Exception e) {
                log.info("Unable to get location of request: {}", e.getMessage());
            }
        }

        if (dspaceObject != null) {
            doc1.addField("id", dspaceObject.getID().toString());
            doc1.addField("type", dspaceObject.getType());
            storeParents(doc1, dspaceObject);
        }
        // Save the current time
        doc1.addField("time", DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
        if (currentUser != null) {
            doc1.addField("epersonid", currentUser.getID().toString());
        }

        return doc1;
    }


    @Override
    public void postSearch(DSpaceObject resultObject, HttpServletRequest request, EPerson currentUser,
                           List<String> queries, int rpp, String sortBy, String order, int page, DSpaceObject scope) {
        try {
            SolrInputDocument solrDoc = getCommonSolrDoc(resultObject, request, currentUser);
            if (solrDoc == null) {
                return;
            }
            initSolrYearCores();

            for (String query : queries) {
                solrDoc.addField("query", query);
            }

            if (resultObject != null) {
                //We have a search result
                solrDoc.addField("statistics_type", StatisticsType.SEARCH_RESULT.text());
            } else {
                solrDoc.addField("statistics_type", StatisticsType.SEARCH.text());
            }
            //Store the scope
            if (scope != null) {
                solrDoc.addField("scopeId", scope.getID().toString());
                solrDoc.addField("scopeType", scope.getType());
            }

            if (rpp != -1) {
                solrDoc.addField("rpp", rpp);
            }

            if (sortBy != null) {
                solrDoc.addField("sortBy", sortBy);
                if (order != null) {
                    solrDoc.addField("sortOrder", order);
                }
            }

            if (page != -1) {
                solrDoc.addField("page", page);
            }

            solr.add(solrDoc);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            log.error("Error saving SEARCH event to Solr by EPerson {}",
                      currentUser.getEmail(), e);
        }
    }

    @Override
    public void postWorkflow(UsageWorkflowEvent usageWorkflowEvent) throws SQLException {
        initSolrYearCores();
        try {
            SolrInputDocument solrDoc = getCommonSolrDoc(usageWorkflowEvent.getObject(), null, null);

            //Log the current collection & the scope !
            solrDoc.addField("owningColl", usageWorkflowEvent.getScope().getID().toString());
            storeParents(solrDoc, usageWorkflowEvent.getScope());

            if (usageWorkflowEvent.getWorkflowStep() != null) {
                solrDoc.addField("workflowStep", usageWorkflowEvent.getWorkflowStep());
            }
            if (usageWorkflowEvent.getOldState() != null) {
                solrDoc.addField("previousWorkflowStep", usageWorkflowEvent.getOldState());
            }
            if (usageWorkflowEvent.getGroupOwners() != null) {
                for (int i = 0; i < usageWorkflowEvent.getGroupOwners().length; i++) {
                    Group group = usageWorkflowEvent.getGroupOwners()[i];
                    solrDoc.addField("owner", "g" + group.getID().toString());
                }
            }
            if (usageWorkflowEvent.getEpersonOwners() != null) {
                for (int i = 0; i < usageWorkflowEvent.getEpersonOwners().length; i++) {
                    EPerson ePerson = usageWorkflowEvent.getEpersonOwners()[i];
                    solrDoc.addField("owner", "e" + ePerson.getID().toString());
                }
            }

            solrDoc.addField("workflowItemId", usageWorkflowEvent.getWorkflowItem().getID().toString());

            EPerson submitter = ((Item) usageWorkflowEvent.getObject()).getSubmitter();
            if (submitter != null) {
                solrDoc.addField("submitter", submitter.getID().toString());
            }
            solrDoc.addField("statistics_type", StatisticsType.WORKFLOW.text());
            if (usageWorkflowEvent.getActor() != null) {
                solrDoc.addField("actor", usageWorkflowEvent.getActor().getID().toString());
            }

            solr.add(solrDoc);
        } catch (Exception e) {
            //Log the exception, no need to send it through, the workflow shouldn't crash because of this !
            log.error("Error saving WORKFLOW event to Solr", e);
        }

    }

    @Override
    public void storeParents(SolrInputDocument doc1, DSpaceObject dso)
        throws SQLException {
        if (dso instanceof Community) {
            Community comm = (Community) dso;
            List<Community> parentCommunities = comm.getParentCommunities();
            for (Community parent : parentCommunities) {
                doc1.addField("owningComm", parent.getID().toString());
                storeParents(doc1, parent);
            }
        } else if (dso instanceof Collection) {
            Collection coll = (Collection) dso;
            List<Community> communities = coll.getCommunities();
            for (Community community : communities) {
                doc1.addField("owningComm", community.getID().toString());
                storeParents(doc1, community);
            }
        } else if (dso instanceof Item) {
            Item item = (Item) dso;
            List<Collection> collections = item.getCollections();
            for (Collection collection : collections) {
                doc1.addField("owningColl", collection.getID().toString());
                storeParents(doc1, collection);
            }
        } else if (dso instanceof Bitstream) {
            Bitstream bitstream = (Bitstream) dso;
            List<Bundle> bundles = bitstream.getBundles();
            for (Bundle bundle : bundles) {
                List<Item> items = bundle.getItems();
                for (Item item : items) {
                    doc1.addField("owningItem", item.getID().toString());
                    storeParents(doc1, item);
                }
            }
        }
    }

    @Override
    public boolean isUseProxies() {
        return clientInfoService.isUseProxiesEnabled();
    }

    @Override
    public void removeIndex(String query) throws IOException,
        SolrServerException {
        solr.deleteByQuery(query);
        solr.commit();
    }

    @Override
    public Map<String, List<String>> queryField(String query,
                                                List oldFieldVals, String field)
            throws IOException {
        Map<String, List<String>> currentValsStored = new HashMap<>();
        try {
            // Get one document (since all the metadata for all the values
            // should be the same just get the first one we find
            Map<String, String> params = new HashMap<>();
            params.put("q", query);
            params.put("rows", "1");
            MapSolrParams solrParams = new MapSolrParams(params);
            QueryResponse response = solr.query(solrParams);
            // Make sure we at least got a document
            if (response.getResults().getNumFound() == 0) {
                return currentValsStored;
            }
        } catch (SolrServerException e) {
            e.printStackTrace();
        }
        return currentValsStored;
    }


    public class ResultProcessor {

        private SolrInputDocument toSolrInputDocument(SolrDocument d) {
            SolrInputDocument doc = new SolrInputDocument();

            for (String name : d.getFieldNames()) {
                doc.addField(name, d.getFieldValue(name));
            }

            return doc;
        }

        public void execute(String query) throws SolrServerException, IOException {
            Map<String, String> params = new HashMap<>();
            params.put("q", query);
            params.put("rows", "10");
            if (0 < statisticYearCores.size()) {
                params.put(ShardParams.SHARDS, StringUtils.join(statisticYearCores.iterator(), ','));
            }
            MapSolrParams solrParams = new MapSolrParams(params);
            QueryResponse response = solr.query(solrParams);

            SolrDocumentList results = response.getResults();
            long numbFound = results.getNumFound();

            // process the first batch
            for (SolrDocument result : results) {
                process(toSolrInputDocument(result));
            }

            // Run over the rest
            for (int i = 10; i < numbFound; i += 10) {
                params.put("start", String.valueOf(i));
                solrParams = new MapSolrParams(params);
                response = solr.query(solrParams);
                results = response.getResults();
                for (SolrDocument result : results) {
                    process(toSolrInputDocument(result));
                }
            }

        }

        public void commit() throws IOException, SolrServerException {
            solr.commit();
        }

        /**
         * Override to manage pages of documents
         *
         * @param docs a list of Solr documents
         * @throws IOException         A general class of exceptions produced by failed or interrupted I/O operations.
         * @throws SolrServerException Exception from the Solr server to the solrj Java client.
         */
        public void process(List<SolrInputDocument> docs) throws IOException, SolrServerException {
            for (SolrInputDocument doc : docs) {
                process(doc);
            }
        }

        /**
         * Override to manage individual documents
         *
         * @param doc Solr document
         * @throws IOException         A general class of exceptions produced by failed or interrupted I/O operations.
         * @throws SolrServerException Exception from the Solr server to the solrj Java client.
         */
        public void process(SolrInputDocument doc) throws IOException, SolrServerException {


        }
    }


    @Override
    public void markRobotsByIP() {
        for (String ip : SpiderDetector.getSpiderIpAddresses()) {

            try {

                /* Result Process to alter record to be identified as a bot */
                ResultProcessor processor = new ResultProcessor() {
                    @Override
                    public void process(SolrInputDocument doc) throws IOException, SolrServerException {
                        doc.removeField("isBot");
                        doc.addField("isBot", true);
                        solr.add(doc);
                        log.info("Marked " + doc.getFieldValue("ip") + " as bot");
                    }
                };

                /* query for ip, exclude results previously set as bots. */
                processor.execute("ip:" + ip + "* AND -isBot:true");

                solr.commit();

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }


        }

    }

    @Override
    public void markRobotByUserAgent(String agent) {
        try {

            /* Result Process to alter record to be identified as a bot */
            ResultProcessor processor = new ResultProcessor() {
                @Override
                public void process(SolrInputDocument doc) throws IOException, SolrServerException {
                    doc.removeField("isBot");
                    doc.addField("isBot", true);
                    solr.add(doc);
                }
            };

            /* query for ip, exclude results previously set as bots. */
            processor.execute("userAgent:" + agent + " AND -isBot:true");

            solr.commit();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void deleteRobotsByIsBotFlag() {
        try {
            solr.deleteByQuery("isBot:true");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void deleteIP(String ip) {
        try {
            solr.deleteByQuery("ip:" + ip + "*");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void deleteRobotsByIP() {
        for (String ip : SpiderDetector.getSpiderIpAddresses()) {
            deleteIP(ip);
        }
    }

    @Override
    public void update(String query, String action,
                       List<String> fieldNames, List<List<Object>> fieldValuesList)
        throws SolrServerException, IOException {
        update(query, action, fieldNames, fieldValuesList, true);
    }

    @Override
    public void update(String query, String action,
                       List<String> fieldNames, List<List<Object>> fieldValuesList, boolean commit)
            throws SolrServerException, IOException {

        // Since there is NO update
        // We need to get our documents
        // QueryResponse queryResponse = solr.query()//query(query, null, -1,
        // null, null, null);

        List<SolrInputDocument> docsToUpdate = new ArrayList<>();

        ResultProcessor processor = new ResultProcessor() {
            @Override
            public void process(SolrInputDocument document) {
                docsToUpdate.add(document);
            }
        };

        processor.execute(query);

        // Add the new (updated onces
        for (int i = 0; i < docsToUpdate.size(); i++) {
            SolrInputDocument solrDocument = docsToUpdate.get(i);

            // Delete the document from the solr client
            solr.deleteByQuery("uid:" + solrDocument.getFieldValue("uid"));

            // Now loop over our fieldname actions
            for (int j = 0; j < fieldNames.size(); j++) {
                String fieldName = fieldNames.get(j);
                List<Object> fieldValues = fieldValuesList.get(j);

                if (action.equals("addOne") || action.equals("replace")) {
                    if (action.equals("replace")) {
                        solrDocument.removeField(fieldName);
                    }

                    for (Object fieldValue : fieldValues) {
                        solrDocument.addField(fieldName, fieldValue);
                    }
                } else if (action.equals("remOne")) {
                    // Remove the field
                    java.util.Collection<Object> values = solrDocument
                        .getFieldValues(fieldName);
                    solrDocument.removeField(fieldName);
                    for (Object value : values) {
                        // Keep all the values besides the one we need to remove
                        if (!fieldValues.contains((value))) {
                            solrDocument.addField(fieldName, value);
                        }
                    }
                }
            }

            // see https://stackoverflow.com/questions/26941260/normalizing-solr-records-for-sharding-version-issues
            solrDocument.removeField("_version_");

            solr.add(solrDocument);

            if (commit) {
                commit();
            }
        }
        // System.out.println("SolrLogger.update(\""+query+"\"):"+(new
        // Date().getTime() - start)+"ms,"+numbFound+"records");
    }

    @Override
    public void query(String query, int max, int facetMinCount)
            throws SolrServerException, IOException {
        query(query, null, null, 0, max, null, null, null, null, null, false, facetMinCount);
    }

    @Override
    public ObjectCount[] queryFacetField(String query,
                                         String filterQuery, String facetField, int max, boolean showTotal,
                                         List<String> facetQueries, int facetMinCount)
            throws SolrServerException, IOException {
        QueryResponse queryResponse = query(query, filterQuery, facetField,
                                            0, max, null, null, null, facetQueries, null, false, facetMinCount);
        if (queryResponse == null) {
            return new ObjectCount[0];
        }

        FacetField field = queryResponse.getFacetField(facetField);
        // At least make sure we have one value
        if (0 < field.getValueCount()) {
            // Create an array for our result
            ObjectCount[] result = new ObjectCount[field.getValueCount()
                + (showTotal ? 1 : 0)];
            // Run over our results & store them
            for (int i = 0; i < field.getValues().size(); i++) {
                FacetField.Count fieldCount = field.getValues().get(i);
                result[i] = new ObjectCount();
                result[i].setCount(fieldCount.getCount());
                result[i].setValue(fieldCount.getName());
            }
            if (showTotal) {
                result[result.length - 1] = new ObjectCount();
                result[result.length - 1].setCount(queryResponse.getResults()
                                                                .getNumFound());
                result[result.length - 1].setValue("total");
            }
            return result;
        } else {
            // Return an empty array cause we got no data
            return new ObjectCount[0];
        }
    }

    @Override
    public ObjectCount[] queryFacetDate(String query,
                                        String filterQuery, int max, String dateType, String dateStart,
                                        String dateEnd, boolean showTotal, Context context, int facetMinCount)
            throws SolrServerException, IOException {
        QueryResponse queryResponse = query(query, filterQuery, null, 0, max,
                                            dateType, dateStart, dateEnd, null, null, false, facetMinCount);
        if (queryResponse == null) {
            return new ObjectCount[0];
        }

        List<RangeFacet> rangeFacets = queryResponse.getFacetRanges();
        for (RangeFacet rangeFacet: rangeFacets) {
            if (rangeFacet.getName().equalsIgnoreCase("time")) {
                RangeFacet timeFacet = rangeFacet;
                // Create an array for our result
                ObjectCount[] result = new ObjectCount[timeFacet.getCounts().size()
                                                       + (showTotal ? 1 : 0)];
                // Run over our datefacet & store all the values
                for (int i = 0; i < timeFacet.getCounts().size(); i++) {
                    RangeFacet.Count dateCount = (RangeFacet.Count) timeFacet.getCounts().get(i);
                    result[i] = new ObjectCount();
                    result[i].setCount(dateCount.getCount());
                    result[i].setValue(getDateView(dateCount.getValue(), dateType, context));
                }
                if (showTotal) {
                    result[result.length - 1] = new ObjectCount();
                    result[result.length - 1].setCount(queryResponse.getResults()
                                                                    .getNumFound());
                    // TODO: Make sure that this total is gotten out of the msgs.xml
                    result[result.length - 1].setValue("total");
                }
                return result;
            }
        }
        return new ObjectCount[0];
    }

    @Override
    public Map<String, Integer> queryFacetQuery(String query, String filterQuery, List<String> facetQueries,
                                                int facetMinCount)
        throws SolrServerException, IOException {
        QueryResponse response = query(query, filterQuery, null, 0, 1, null, null,
                                       null, facetQueries, null, false, facetMinCount);
        return response.getFacetQuery();
    }

    @Override
    public ObjectCount queryTotal(String query, String filterQuery, int facetMinCount)
        throws SolrServerException, IOException {
        QueryResponse queryResponse = query(query, filterQuery, null, 0, -1, null,
                                            null, null, null, null, false, facetMinCount);
        ObjectCount objCount = new ObjectCount();
        objCount.setCount(queryResponse.getResults().getNumFound());

        return objCount;
    }

    protected String getDateView(String name, String type, Context context) {
        if (name != null && name.matches("^[0-9]{4}\\-[0-9]{2}.*")) {
            /*
             * if ("YEAR".equalsIgnoreCase(type)) return name.substring(0, 4);
             * else if ("MONTH".equalsIgnoreCase(type)) return name.substring(0,
             * 7); else if ("DAY".equalsIgnoreCase(type)) return
             * name.substring(0, 10); else if ("HOUR".equalsIgnoreCase(type))
             * return name.substring(11, 13);
             */
            // Get our date
            Date date = null;
            try {
                SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT_8601, context.getCurrentLocale());
                date = format.parse(name);
            } catch (ParseException e) {
                try {
                    // We should use the dcdate (the dcdate is used when
                    // generating random data)
                    SimpleDateFormat format = new SimpleDateFormat(
                        DATE_FORMAT_DCDATE, context.getCurrentLocale());
                    date = format.parse(name);
                } catch (ParseException e1) {
                    e1.printStackTrace();
                }
                // e.printStackTrace();
            }
            String dateformatString = "dd-MM-yyyy";
            if ("DAY".equals(type)) {
                dateformatString = "dd-MM-yyyy";
            } else if ("MONTH".equals(type)) {
                dateformatString = "MMMM yyyy";

            } else if ("YEAR".equals(type)) {
                dateformatString = "yyyy";
            }
            SimpleDateFormat simpleFormat = new SimpleDateFormat(
                dateformatString, context.getCurrentLocale());
            if (date != null) {
                name = simpleFormat.format(date);
            }

        }
        return name;
    }

    @Override
    public QueryResponse query(String query, String filterQuery, String facetField, int rows, int max, String dateType,
                               String dateStart, String dateEnd, List<String> facetQueries, String sort,
                               boolean ascending, int facetMinCount)
            throws SolrServerException, IOException {

        return query(query, filterQuery, facetField, rows, max, dateType, dateStart, dateEnd, facetQueries, sort,
                ascending, facetMinCount, true);
    }

    @Override
    public QueryResponse query(String query, String filterQuery, String facetField, int rows, int max, String dateType,
                               String dateStart, String dateEnd, List<String> facetQueries, String sort,
                               boolean ascending, int facetMinCount, boolean defaultFilterQueries)
            throws SolrServerException, IOException {

        if (solr == null) {
            return null;
        }

        // System.out.println("QUERY");
        SolrQuery solrQuery = new SolrQuery().setRows(rows).setQuery(query)
                                             .setFacetMinCount(facetMinCount);
        addAdditionalSolrYearCores(solrQuery);

        // Set the date facet if present
        if (dateType != null) {
            solrQuery.setParam("facet.range", "time")
                .
                // EXAMPLE: NOW/MONTH+1MONTH
                    setParam("f.time.facet.range.end",
                             "NOW/" + dateType + dateEnd + dateType).setParam(
                "f.time.facet.range.gap", "+1" + dateType)
                .
                // EXAMPLE: NOW/MONTH-" + nbMonths + "MONTHS
                    setParam("f.time.facet.range.start",
                             "NOW/" + dateType + dateStart + dateType + "S")
                .setFacet(true);
        }
        if (facetQueries != null) {
            for (int i = 0; i < facetQueries.size(); i++) {
                String facetQuery = facetQueries.get(i);
                solrQuery.addFacetQuery(facetQuery);
            }
            if (0 < facetQueries.size()) {
                solrQuery.setFacet(true);
            }
        }

        if (facetField != null) {
            solrQuery.addFacetField(facetField);
        }

        // Set the top x of if present
        if (max != -1) {
            solrQuery.setFacetLimit(max);
        }

        // A filter is used instead of a regular query to improve
        // performance and ensure the search result ordering will
        // not be influenced

        // Choose to filter by the Legacy spider IP list (may get too long to properly filter all IP's
        if (defaultFilterQueries && configurationService.getBooleanProperty(
                "solr-statistics.query.filter.spiderIp", false)) {
            solrQuery.addFilterQuery(getIgnoreSpiderIPs());
        }

        // Choose to filter by isBot field, may be overriden in future
        // to allow views on stats based on bots.
        if (defaultFilterQueries && configurationService.getBooleanProperty(
                "solr-statistics.query.filter.isBot", true)) {
            solrQuery.addFilterQuery("-isBot:true");
        }

        if (sort != null) {
            solrQuery.addSort(sort, (ascending ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc));
        }

        String[] bundles = configurationService.getArrayProperty("solr-statistics.query.filter.bundles");
        if (defaultFilterQueries && bundles != null && bundles.length > 0) {

            /**
             * The code below creates a query that will allow only records which do not have a bundlename
             * (items, collections, ...) or bitstreams that have a configured bundle name
             */
            StringBuilder bundleQuery = new StringBuilder();
            //Also add the possibility that if no bundle name is there these results will also be returned !
            bundleQuery.append("-(bundleName:[* TO *]");
            for (int i = 0; i < bundles.length; i++) {
                String bundle = bundles[i].trim();
                bundleQuery.append("-bundleName:").append(bundle);
                if (i != bundles.length - 1) {
                    bundleQuery.append(" AND ");
                }
            }
            bundleQuery.append(")");


            solrQuery.addFilterQuery(bundleQuery.toString());
        }

        if (filterQuery != null) {
            solrQuery.addFilterQuery(filterQuery);
        }

        QueryResponse response;
        try {
            // solr.set
            response = solr.query(solrQuery);
        } catch (SolrServerException | IOException e) {
            log.error("Error searching Solr usage events using query {}", query, e);
            throw e;
        }
        return response;
    }


    /**
     * String of IP and Ranges in IPTable as a Solr Query
     */
    protected String filterQuery = null;

    @Override
    public String getIgnoreSpiderIPs() {
        if (filterQuery == null) {
            StringBuilder query = new StringBuilder();
            boolean first = true;
            for (String ip : SpiderDetector.getSpiderIpAddresses()) {
                if (first) {
                    query.append(" AND ");
                    first = false;
                }

                query.append(" NOT(ip: ").append(ip).append(")");
            }
            filterQuery = query.toString();
        }

        return filterQuery;

    }

    @Override
    public void optimizeSOLR() {
        try {
            long start = System.currentTimeMillis();
            System.out.println("SOLR Optimize -- Process Started:" + start);
            solr.optimize();
            long finish = System.currentTimeMillis();
            System.out.println("SOLR Optimize -- Process Finished:" + finish);
            System.out.println("SOLR Optimize -- Total time taken:" + (finish - start) + " (ms).");
        } catch (SolrServerException sse) {
            System.err.println(sse.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    @Override
    public void shardSolrIndex() throws IOException, SolrServerException {
        if (!(solr instanceof HttpSolrClient)) {
            return;
        }

        /*
        Start by faceting by year so we can include each year in a separate core !
         */
        SolrQuery yearRangeQuery = new SolrQuery();
        yearRangeQuery.setQuery("*:*");
        yearRangeQuery.setRows(0);
        yearRangeQuery.setFacet(true);
        yearRangeQuery.add(FacetParams.FACET_RANGE, "time");
        //We go back to 2000 the year 2000, this is a bit overkill but this way we ensure we have everything
        //The alternative would be to sort but that isn't recommended since it would be a very costly query !
        yearRangeQuery.add(FacetParams.FACET_RANGE_START,
                           "NOW/YEAR-" + (Calendar.getInstance().get(Calendar.YEAR) - 2000) + "YEARS");
        //Add the +0year to ensure that we DO NOT include the current year
        yearRangeQuery.add(FacetParams.FACET_RANGE_END, "NOW/YEAR+0YEARS");
        yearRangeQuery.add(FacetParams.FACET_RANGE_GAP, "+1YEAR");
        yearRangeQuery.add(FacetParams.FACET_MINCOUNT, String.valueOf(1));

        //Create a temp directory to store our files in !
        File tempDirectory = new File(
            configurationService.getProperty("dspace.dir") + File.separator + "temp" + File.separator);
        tempDirectory.mkdirs();


        QueryResponse queryResponse = solr.query(yearRangeQuery);
        //We only have one range query !
        List<RangeFacet.Count> yearResults = queryResponse.getFacetRanges().get(0).getCounts();
        for (RangeFacet.Count count : yearResults) {
            long totalRecords = count.getCount();

            //Create a range query from this !
            //We start with out current year
            DCDate dcStart = new DCDate(count.getValue());
            Calendar endDate = Calendar.getInstance();
            //Advance one year for the start of the next one !
            endDate.setTime(dcStart.toDate());
            endDate.add(Calendar.YEAR, 1);
            DCDate dcEndDate = new DCDate(endDate.getTime());


            StringBuilder filterQuery = new StringBuilder();
            filterQuery.append("time:([");
            filterQuery.append(ClientUtils.escapeQueryChars(dcStart.toString()));
            filterQuery.append(" TO ");
            filterQuery.append(ClientUtils.escapeQueryChars(dcEndDate.toString()));
            filterQuery.append("]");
            //The next part of the filter query excludes the content from midnight of the next year !
            filterQuery.append(" NOT ").append(ClientUtils.escapeQueryChars(dcEndDate.toString()));
            filterQuery.append(")");


            Map<String, String> yearQueryParams = new HashMap<>();
            yearQueryParams.put(CommonParams.Q, "*:*");
            yearQueryParams.put(CommonParams.ROWS, String.valueOf(10000));
            yearQueryParams.put(CommonParams.FQ, filterQuery.toString());
            yearQueryParams.put(CommonParams.WT, "csv");

            //Tell SOLR how to escape and separate the values of multi-valued fields
            yearQueryParams.put("csv.escape", "\\");
            yearQueryParams.put("csv.mv.separator", MULTIPLE_VALUES_SPLITTER);

            //Start by creating a new core
            String coreName = statisticsCoreBase + "-" + dcStart.getYearUTC();
            HttpSolrClient statisticsYearServer = createCore((HttpSolrClient) solr, coreName);

            System.out.println("Moving: " + totalRecords + " into core " + coreName);
            log.info("Moving: " + totalRecords + " records into core " + coreName);

            List<File> filesToUpload = new ArrayList<>();
            for (int i = 0; i < totalRecords; i += 10000) {
                String solrRequestUrl = ((HttpSolrClient) solr).getBaseURL() + "/select";
                solrRequestUrl = generateURL(solrRequestUrl, yearQueryParams);

                HttpGet get = new HttpGet(solrRequestUrl);
                InputStream csvInputstream;
                File csvFile = new File(tempDirectory.getPath()
                        + File.separatorChar
                        + "temp."
                        + dcStart.getYearUTC()
                        + "."
                        + i
                        + ".csv");
                try ( CloseableHttpClient hc = HttpClientBuilder.create().build(); ) {
                    HttpResponse response = hc.execute(get);
                    csvInputstream = response.getEntity().getContent();
                    //Write the csv ouput to a file !
                    FileUtils.copyInputStreamToFile(csvInputstream, csvFile);
                }
                filesToUpload.add(csvFile);

                //Add 10000 & start over again
                yearQueryParams.put(CommonParams.START, String.valueOf((i + 10000)));
            }

            Set<String> multivaluedFields = getMultivaluedFieldNames();

            for (File tempCsv : filesToUpload) {
                //Upload the data in the csv files to our new solr core
                ContentStreamUpdateRequest contentStreamUpdateRequest = new ContentStreamUpdateRequest("/update");
                contentStreamUpdateRequest.setParam("stream.contentType", "text/csv;charset=utf-8");
                contentStreamUpdateRequest.setParam("escape", "\\");
                contentStreamUpdateRequest.setParam("skip", "_version_");
                contentStreamUpdateRequest.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
                contentStreamUpdateRequest.addFile(tempCsv, "text/csv;charset=utf-8");

                //Add parsing directives for the multivalued fields so that they are stored as separate values
                // instead of one value
                for (String multivaluedField : multivaluedFields) {
                    contentStreamUpdateRequest.setParam("f." + multivaluedField + ".split", Boolean.TRUE.toString());
                    contentStreamUpdateRequest
                        .setParam("f." + multivaluedField + ".separator", MULTIPLE_VALUES_SPLITTER);
                }

                statisticsYearServer.request(contentStreamUpdateRequest);
            }

            statisticsYearServer.commit(true, true);


            //Delete contents of this year from our year query !
            solr.deleteByQuery(filterQuery.toString());
            solr.commit(true, true);

            log.info("Moved {} records into core: {}", totalRecords, coreName);
        }

        FileUtils.deleteDirectory(tempDirectory);
    }

    protected HttpSolrClient createCore(HttpSolrClient solr, String coreName)
            throws IOException, SolrServerException {
        String baseSolrUrl = solr.getBaseURL().replace(statisticsCoreBase, ""); // Has trailing slash

        //DS-3458: Test to see if a solr core already exists.  If it exists,
        // return a connection to that core.  Otherwise create a new core and
        // return a connection to it.
        HttpSolrClient returnServer = new HttpSolrClient.Builder(baseSolrUrl + coreName).build();
        try {
            SolrPingResponse ping = returnServer.ping();
            log.debug("Ping of Solr Core {} returned with Status {}",
                    coreName, ping.getStatus());
            return returnServer;
        } catch (IOException | RemoteSolrException | SolrServerException e) {
            log.debug("Ping of Solr Core {} failed with {}.  New Core Will be Created",
                    coreName, e.getClass().getName());
        }

        //Unfortunately, this class is documented as "experimental and subject to change" on the Lucene website.
        //http://lucene.apache.org/solr/4_4_0/solr-solrj/org/apache/solr/client/solrj/request/CoreAdminRequest.html
        CoreAdminRequest.Create create = new CoreAdminRequest.Create();
        create.setCoreName(coreName);
        String configSetName = configurationService
                .getProperty("solr-statistics.configset", "statistics");
        create.setConfigSet(configSetName);
        create.setInstanceDir(coreName);

        HttpSolrClient solrServer = new HttpSolrClient.Builder(baseSolrUrl).build();
        create.process(solrServer);
        log.info("Created core with name: {} from configset {}", coreName, configSetName);
        return returnServer;
    }

    /**
     * Retrieves a list of all the multi valued fields in the solr core.
     *
     * @return all fields tagged as multivalued
     * @throws SolrServerException When getting the schema information from the SOLR core fails
     * @throws IOException         When connection to the SOLR server fails
     */
    public Set<String> getMultivaluedFieldNames() throws SolrServerException, IOException {
        Set<String> multivaluedFields = new HashSet<>();
        LukeRequest lukeRequest = new LukeRequest();
        lukeRequest.setShowSchema(true);
        LukeResponse process = lukeRequest.process(solr);
        Map<String, LukeResponse.FieldInfo> fields = process.getFieldInfo();
        for (String fieldName : fields.keySet()) {
            LukeResponse.FieldInfo fieldInfo = fields.get(fieldName);
            EnumSet<FieldFlag> flags = fieldInfo.getFlags();
            for (FieldFlag fieldFlag : flags) {
                if (fieldFlag.getAbbreviation() == FieldFlag.MULTI_VALUED.getAbbreviation()) {
                    multivaluedFields.add(fieldName);
                }
            }
        }
        return multivaluedFields;
    }


    @Override
    public void reindexBitstreamHits(boolean removeDeletedBitstreams) throws Exception {
        if (!(solr instanceof HttpSolrClient)) {
            return;
        }

        Context context = new Context();

        try {
            //First of all retrieve the total number of records to be updated
            SolrQuery query = new SolrQuery();
            query.setQuery("*:*");
            query.addFilterQuery("type:" + Constants.BITSTREAM);
            //Only retrieve records which do not have a bundle name
            query.addFilterQuery("-bundleName:[* TO *]");
            query.setRows(0);
            addAdditionalSolrYearCores(query);
            long totalRecords = solr.query(query).getResults().getNumFound();

            File tempDirectory = new File(
                configurationService.getProperty("dspace.dir") + File.separator + "temp" + File.separator);
            tempDirectory.mkdirs();
            List<File> tempCsvFiles = new ArrayList<>();
            for (int i = 0; i < totalRecords; i += 10000) {
                Map<String, String> params = new HashMap<>();
                params.put(CommonParams.Q, "*:*");
                params.put(CommonParams.FQ, "-bundleName:[* TO *] AND type:" + Constants.BITSTREAM);
                params.put(CommonParams.WT, "csv");
                params.put(CommonParams.ROWS, String.valueOf(10000));
                params.put(CommonParams.START, String.valueOf(i));

                String solrRequestUrl = ((HttpSolrClient) solr).getBaseURL() + "/select";
                solrRequestUrl = generateURL(solrRequestUrl, params);

                HttpGet get = new HttpGet(solrRequestUrl);
                List<String[]> rows;
                try ( CloseableHttpClient hc = HttpClientBuilder.create().build(); ) {
                    HttpResponse response = hc.execute(get);
                    InputStream csvOutput = response.getEntity().getContent();
                    Reader csvReader = new InputStreamReader(csvOutput);
                    rows = new CSVReader(csvReader).readAll();
                }
                String[][] csvParsed = rows.toArray(new String[rows.size()][]);
                String[] header = csvParsed[0];
                //Attempt to find the bitstream id index !
                int idIndex = 0;
                for (int j = 0; j < header.length; j++) {
                    if (header[j].equals("id")) {
                        idIndex = j;
                    }
                }

                File tempCsv = new File(tempDirectory.getPath() + File.separatorChar + "temp." + i + ".csv");
                tempCsvFiles.add(tempCsv);
                CSVWriter csvp = new CSVWriter(new FileWriter(tempCsv));
                //csvp.setAlwaysQuote(false);

                //Write the header !
                csvp.writeNext((String[]) ArrayUtils.add(header, "bundleName"));
                Map<String, String> bitBundleCache = new HashMap<>();
                //Loop over each line (skip the headers though)!
                for (int j = 1; j < csvParsed.length; j++) {
                    String[] csvLine = csvParsed[j];
                    //Write the default line !
                    String bitstreamId = csvLine[idIndex];
                    //Attempt to retrieve our bundle name from the cache !
                    String bundleName = bitBundleCache.get(bitstreamId);
                    if (bundleName == null) {
                        //Nothing found retrieve the bitstream
                        Bitstream bitstream = bitstreamService.findByIdOrLegacyId(context, bitstreamId);
                        //Attempt to retrieve our bitstream !
                        if (bitstream != null) {
                            List<Bundle> bundles = bitstream.getBundles();
                            if (bundles != null && 0 < bundles.size()) {
                                Bundle bundle = bundles.get(0);
                                bundleName = bundle.getName();
                            } else {
                                //No bundle found, we are either a collection or a community logo, check for it !
                                DSpaceObject parentObject = bitstreamService.getParentObject(context, bitstream);
                                if (parentObject instanceof Collection) {
                                    bundleName = "LOGO-COLLECTION";
                                } else if (parentObject instanceof Community) {
                                    bundleName = "LOGO-COMMUNITY";
                                }

                            }
                            //Cache the bundle name
                            bitBundleCache.put(bitstream.getID().toString(), bundleName);
                            //Remove the bitstream from cache
                        }
                        //Check if we don't have a bundlename
                        //If we don't have one & we do not need to delete the deleted bitstreams ensure that a
                        // BITSTREAM_DELETED bundle name is given !
                        if (bundleName == null && !removeDeletedBitstreams) {
                            bundleName = "BITSTREAM_DELETED";
                        }
                    }
                    csvp.writeNext((String[]) ArrayUtils.add(csvLine, bundleName));
                }

                //Loop over our parsed csv
                csvp.flush();
                csvp.close();
            }

            //Add all the separate csv files
            for (File tempCsv : tempCsvFiles) {
                ContentStreamUpdateRequest contentStreamUpdateRequest = new ContentStreamUpdateRequest("/update");
                contentStreamUpdateRequest.setParam("stream.contentType", "text/csv;charset=utf-8");
                contentStreamUpdateRequest.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
                contentStreamUpdateRequest.addFile(tempCsv, "text/csv;charset=utf-8");

                solr.request(contentStreamUpdateRequest);
            }

            //Now that all our new bitstream stats are in place, delete all the old ones !
            solr.deleteByQuery("-bundleName:[* TO *] AND type:" + Constants.BITSTREAM);
            //Commit everything to wrap up
            solr.commit(true, true);
            //Clean up our directory !
            FileUtils.deleteDirectory(tempDirectory);
        } catch (Exception e) {
            log.error("Error while updating the bitstream statistics", e);
            throw e;
        } finally {
            context.abort();
        }
    }


    @Override
    public void exportHits() throws Exception {
        Context context = new Context();

        File tempDirectory = new File(
            configurationService.getProperty("dspace.dir") + File.separator + "temp" + File.separator);
        tempDirectory.mkdirs();

        try {
            //First of all retrieve the total number of records to be updated
            SolrQuery query = new SolrQuery();
            query.setQuery("*:*");

            ModifiableSolrParams solrParams = new ModifiableSolrParams();
            solrParams.set(CommonParams.Q, "statistics_type:view OR (*:* AND -statistics_type:*)");
            solrParams.set(CommonParams.WT, "javabin");
            solrParams.set(CommonParams.ROWS, String.valueOf(10000));

            addAdditionalSolrYearCores(query);
            long totalRecords = solr.query(query).getResults().getNumFound();
            System.out.println("There are " + totalRecords + " usage events in SOLR for download/view.");

            for (int i = 0; i < totalRecords; i += 10000) {
                solrParams.set(CommonParams.START, String.valueOf(i));
                QueryResponse queryResponse = solr.query(solrParams);
                SolrDocumentList docs = queryResponse.getResults();

                File exportOutput = new File(tempDirectory.getPath() + File.separatorChar + "usagestats_" + i + ".csv");
                exportOutput.delete();

                //export docs
                addDocumentsToFile(context, docs, exportOutput);
                System.out.println(
                    "Export hits [" + i + " - " + String.valueOf(i + 9999) + "] to " + exportOutput.getCanonicalPath());
            }
        } catch (Exception e) {
            log.error("Error while exporting SOLR data", e);
            throw e;
        } finally {
            context.abort();
        }
    }

    @Override
    public void commit() throws IOException, SolrServerException {
        solr.commit();
    }

    protected void addDocumentsToFile(Context context, SolrDocumentList docs, File exportOutput)
        throws SQLException, ParseException, IOException {
        for (SolrDocument doc : docs) {
            String ip = doc.get("ip").toString();
            if (ip.equals("::1")) {
                ip = "127.0.0.1";
            }

            String id = doc.get("id").toString();
            String type = doc.get("type").toString();
            String time = doc.get("time").toString();

            //20140527162409835,view_bitstream,1292,2014-05-27T16:24:09,anonymous,127.0.0.1
            DSpaceObjectLegacySupportService dsoService = contentServiceFactory
                .getDSpaceLegacyObjectService(Integer.parseInt(type));
            DSpaceObject dso = dsoService.findByIdOrLegacyId(context, id);
            if (dso == null) {
                log.debug("Document no longer exists in DB. type:" + type + " id:" + id);
                continue;
            }

            //InputFormat: Mon May 19 07:21:27 EDT 2014
            DateFormat inputDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
            Date solrDate = inputDateFormat.parse(time);

            //OutputFormat: 2014-05-27T16:24:09
            DateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

            String out = time + "," + "view_" + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso)
                                                                     .toLowerCase() + "," + id + "," + outputDateFormat
                .format(solrDate) + ",anonymous," + ip + "\n";
            FileUtils.writeStringToFile(exportOutput, out, StandardCharsets.UTF_8, true);
        }
    }

    protected String generateURL(String baseURL, Map<String, String> parameters) throws UnsupportedEncodingException {
        boolean first = true;
        StringBuilder result = new StringBuilder(baseURL);
        for (String key : parameters.keySet()) {
            if (first) {
                result.append("?");
                first = false;
            } else {
                result.append("&");
            }

            result.append(key).append("=").append(URLEncoder.encode(parameters.get(key), "UTF-8"));
        }

        return result.toString();
    }

    protected void addAdditionalSolrYearCores(SolrQuery solrQuery) {
        //Only add if needed
        initSolrYearCores();
        if (0 < statisticYearCores.size()) {
            //The shards are a comma separated list of the urls to the cores
            solrQuery.add(ShardParams.SHARDS, StringUtils.join(statisticYearCores.iterator(), ","));
        }

    }

    /*
     * The statistics shards should not be initialized until all tomcat webapps
     * are fully initialized.  DS-3457 uncovered an issue in DSpace 6x in which
     * this code triggered Tomcat to hang when statistics shards are present.
     * This code is synchonized in the event that 2 threads trigger the
     * initialization at the same time.
     */
    protected synchronized void initSolrYearCores() {
        if (statisticYearCoresInit || !(solr instanceof HttpSolrClient) || !configurationService.getBooleanProperty(
            "usage-statistics.shardedByYear", false)) {
            return;
        }

        //Base url should like : http://localhost:{port.number}/solr
        String baseSolrUrl = ((HttpSolrClient) solr).getBaseURL().replace(statisticsCoreBase, "");

        try (HttpSolrClient enumClient = new HttpSolrClient.Builder(baseSolrUrl).build();) {
            //Attempt to retrieve all the statistic year cores
            CoreAdminRequest coresRequest = new CoreAdminRequest();
            coresRequest.setAction(CoreAdminAction.STATUS);
            CoreAdminResponse coresResponse = coresRequest.process(enumClient);
            NamedList<Object> response = coresResponse.getResponse();
            NamedList<Object> coreStatuses = (NamedList<Object>) response.get("status");
            List<String> statCoreNames = new ArrayList<>(coreStatuses.size());
            for (Map.Entry<String, Object> coreStatus : coreStatuses) {
                String coreName = coreStatus.getKey();
                if (coreName.startsWith(statisticsCoreBase)) {
                    statCoreNames.add(coreName);
                }
            }

            for (String statCoreName : statCoreNames) {
                log.info("Loading core with name: " + statCoreName);

                createCore((HttpSolrClient) solr, statCoreName);
                //Add it to our cores list so we can query it !
                statisticYearCores
                    .add(baseSolrUrl.replace("http://", "").replace("https://", "") + statCoreName);
            }
            //Also add the core containing the current year !
            statisticYearCores.add(((HttpSolrClient) solr)
                    .getBaseURL()
                    .replace("http://", "")
                    .replace("https://", ""));
        } catch (IOException | SolrServerException e) {
            log.error(e.getMessage(), e);
        }
        statisticYearCoresInit = true;
    }

    public Object anonymizeIp(String ip) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(ip);
        if (address instanceof Inet4Address) {
            return ip.replaceFirst(IP_V4_REGEX, "$1" + configurationService.getProperty(
                    "anonymize_statistics.ip_v4_mask", "255"));
        } else if (address instanceof Inet6Address) {
            return ip.replaceFirst(IP_V6_REGEX, "$1:" + configurationService.getProperty(
                    "anonymize_statistics.ip_v6_mask", "FFFF:FFFF"));
        }

        throw new UnknownHostException("unknown ip format");
    }
}
