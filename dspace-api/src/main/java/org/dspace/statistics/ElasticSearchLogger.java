/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;


import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.statistics.util.DnsLookup;
import org.dspace.statistics.util.LocationUtils;
import org.dspace.statistics.util.SpiderDetector;
import org.elasticsearch.action.ActionFuture;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ElasticSearchLogger {

    private static Logger log = Logger.getLogger(ElasticSearchLogger.class);

    private static boolean useProxies;

    public static final String DATE_FORMAT_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String DATE_FORMAT_DCDATE = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static LookupService locationService;

    public static String clusterName = "dspacestatslogging";
    public static String indexName = "dspaceindex";
    public static String indexType = "stats";
    public static String address = "127.0.0.1";
    public static int port = 9300;

    private static Client client;

    public static enum ClientType {
        NODE, LOCAL, TRANSPORT
    }


    public ElasticSearchLogger() {
        // nobody should be instantiating this...
    }

    public ElasticSearchLogger(boolean doInitialize) {
        initializeElasticSearch();
    }

    public static ElasticSearchLogger getInstance() {
        return ElasticSearchLoggerSingletonHolder.instance;
    }

    // Singleton Pattern of "Initialization on demand holder idiom"
    private static class ElasticSearchLoggerSingletonHolder {
        public static final ElasticSearchLogger instance = new ElasticSearchLogger(true);
    }

    public void initializeElasticSearch() {
        log.info("DSpace ElasticSearchLogger Initializing");
        try {
        LookupService service = null;
        // Get the db file for the location
        String dbfile = ConfigurationManager.getProperty("usage-statistics", "dbfile");
        if (dbfile != null) {
            try {
                service = new LookupService(dbfile, LookupService.GEOIP_STANDARD);
            } catch (FileNotFoundException fe) {
                log.error("The GeoLite Database file is missing (" + dbfile + ")! Usage Statistics cannot generate location based reports! Please see the DSpace installation instructions for instructions to install this file.", fe);
            } catch (IOException e) {
                log.error("Unable to load GeoLite Database file (" + dbfile + ")! You may need to reinstall it. See the DSpace installation instructions for more details.", e);
            }
        } else {
            log.error("The required 'dbfile' configuration is missing in usage-statistics.cfg!");
        }
        locationService = service;

        if ("true".equals(ConfigurationManager.getProperty("useProxies"))) {
            useProxies = true;
        } else {
            useProxies = false;
        }

        log.info("useProxies=" + useProxies);
        
        // Configurable values for all elasticsearch connection constants
        clusterName = getConfigurationStringWithFallBack("elastic-search-statistics", "clusterName", clusterName);
        indexName   = getConfigurationStringWithFallBack("elastic-search-statistics", "indexName", indexName);
        indexType   = getConfigurationStringWithFallBack("elastic-search-statistics", "indexType", indexType);
        address     = getConfigurationStringWithFallBack("elastic-search-statistics", "address", address);
        port        = ConfigurationManager.getIntProperty("elastic-search-statistics", "port", port);

        //Initialize the connection to Elastic Search, and ensure our index is available.
        client = getClient();

        IndicesExistsRequest indicesExistsRequest = new IndicesExistsRequest();
        indicesExistsRequest.indices(new String[] {indexName});

        ActionFuture<IndicesExistsResponse> actionFutureIndicesExist = client.admin().indices().exists(indicesExistsRequest);        
        log.info("DS ES Checking if index exists");
        if(! actionFutureIndicesExist.actionGet().isExists() ) {
            //If elastic search index exists, then we are good to go, otherwise, we need to create that index. Should only need to happen once ever.
            log.info("DS ES index didn't exist, we need to create it.");

            Settings settings = ImmutableSettings.settingsBuilder()
                    .put("number_of_replicas", 1)
                    .put("number_of_shards", 5)
                    .put("cluster.name", clusterName)
                    .build();
            
            String stringMappingJSON = "{\""+indexType+"\" : { \"properties\" : {\n" +
                    "   \"userAgent\":{\n" +
                    "      \"type\":\"string\"\n" +
                    "   },\n" +
                    "   \"countryCode\":{\n" +
                    "      \"type\":\"string\",\n" +
                    "      \"index\":\"not_analyzed\",\n" +
                    "      \"omit_norms\":true\n" +
                    "   },\n" +
                    "   \"dns\":{\n" +
                    "      \"type\":\"multi_field\",\n" +
                    "      \"fields\": {\n" +
                    "        \"dns\": {\"type\":\"string\",\"index\":\"analyzed\"},\n" +
                    "        \"untouched\":{\"type\":\"string\",\"index\":\"not_analyzed\"}\n" +
                    "      }\n" +
                    "   },\n" +
                    "   \"isBot\":{\n" +
                    "      \"type\":\"boolean\"\n" +
                    "   },\n" +
                    "   \"owningColl\":{\n" +
                    "      \"type\":\"integer\",\n" +
                    "      \"index\":\"not_analyzed\"\n" +
                    "   },\n" +
                    "   \"type\":{\n" +
                    "      \"type\":\"string\",\n" +
                    "      \"index\":\"not_analyzed\",\n" +
                    "      \"omit_norms\":true\n" +
                    "   },\n" +
                    "   \"owningComm\":{\n" +
                    "      \"type\":\"integer\",\n" +
                    "      \"index\":\"not_analyzed\"\n" +
                    "   },\n" +
                    "   \"city\":{\n" +
                    "      \"type\":\"multi_field\",\n" +
                    "      \"fields\": {\n" +
                    "        \"city\": {\"type\":\"string\",\"index\":\"analyzed\"},\n" +
                    "        \"untouched\":{\"type\":\"string\",\"index\":\"not_analyzed\"}\n" +
                    "      }\n" +
                    "   },\n" +
                    "   \"country\":{\n" +
                    "      \"type\":\"multi_field\",\n" +
                    "      \"fields\": {\n" +
                    "         \"country\": {\"type\":\"string\",\"index\":\"analyzed\"},\n" +
                    "         \"untouched\":{\"type\":\"string\",\"index\":\"not_analyzed\"}\n" +
                    "       }\n" +
                    "   },\n" +
                    "   \"ip\":{\n" +
                    "      \"type\":\"multi_field\",\n" +
                    "       \"fields\": {\n" +
                    "         \"ip\": {\"type\":\"string\",\"index\":\"analyzed\"},\n" +
                    "         \"untouched\":{\"type\":\"string\",\"index\":\"not_analyzed\"}\n" +
                    "       }\n" +
                    "   },\n" +
                    "   \"id\":{\n" +
                    "      \"type\":\"integer\",\n" +
                    "      \"index\":\"not_analyzed\"\n" +
                    "   },\n" +
                    "   \"time\":{\n" +
                    "      \"type\":\"date\"\n" +
                    "   },\n" +
                    "   \"owningItem\":{\n" +
                    "      \"type\":\"string\",\n" +
                    "      \"index\":\"not_analyzed\"\n" +
                    "   },\n" +
                    "   \"continent\":{\n" +
                    "      \"type\":\"string\",\n" +
                    "      \"index\":\"not_analyzed\"\n" +
                    "   },\n" +
                    "   \"geo\":{\n" +
                    "      \"type\":\"geo_point\"\n" +
                    "   },\n" +
                    "   \"bundleName\":{\n" +
                    "      \"type\":\"string\",\n" +
                    "      \"index\":\"not_analyzed\"\n" +
                    "   },\n" +
                    "   \"epersonid\":{\n" +
                    "      \"type\":\"string\",\n" +
                    "      \"index\":\"not_analyzed\"\n" +
                    "   }\n" +
                    "} } }";

            client.prepareIndex(indexName, indexType, "1")
                    .setSource(XContentFactory.jsonBuilder()
                                .startObject()
                                    .field("user", "kimchy")
                                    .field("postDate", new Date())
                                    .field("message", "trying out Elastic Search")
                                .endObject()
                              )
                    .execute()
                    .actionGet();

            log.info("Create INDEX ["+indexName+"]/["+indexType+"]");

            // Wait for create to be finished.
            client.admin().indices().prepareRefresh(indexName).execute().actionGet();

            //Put the schema/mapping
            log.info("Put Mapping for ["+indexName+"]/["+indexType+"]="+stringMappingJSON);
            PutMappingRequestBuilder putMappingRequestBuilder = client.admin().indices().preparePutMapping(indexName).setType(indexType);
            putMappingRequestBuilder.setSource(stringMappingJSON);
            PutMappingResponse response = putMappingRequestBuilder.execute().actionGet();

            if(!response.isAcknowledged()) {
                log.info("Could not define mapping for type ["+indexName+"]/["+indexType+"]");
            } else {
                log.info("Successfully put mapping for ["+indexName+"]/["+indexType+"]");
            }

            log.info("DS ES index didn't exist, but we created it.");
        } else {
            log.info("DS ES index already exists");
        }

        log.info("DSpace ElasticSearchLogger Initialized Successfully (I suppose)");

        } catch (Exception e) {
            log.info("Elastic Search crashed during init. " + e.getMessage());
        }
    }

    public void post(DSpaceObject dspaceObject, HttpServletRequest request, EPerson currentUser) {
        //log.info("DS-ES post for type:"+dspaceObject.getType() + " -- " + dspaceObject.getName());

        client = ElasticSearchLogger.getInstance().getClient();

        boolean isSpiderBot = SpiderDetector.isSpider(request);

        try {
            if (isSpiderBot &&
                    !ConfigurationManager.getBooleanProperty("usage-statistics", "logBots", true)) {
                return;
            }


            // Save our basic info that we already have

            String ip = request.getRemoteAddr();

            if (isUseProxies() && request.getHeader("X-Forwarded-For") != null) {
                /* This header is a comma delimited list */
                for (String xfip : request.getHeader("X-Forwarded-For").split(",")) {
                    /* proxy itself will sometime populate this header with the same value in
                        remote address. ordering in spec is vague, we'll just take the last
                        not equal to the proxy
                    */
                    if (!request.getHeader("X-Forwarded-For").contains(ip)) {
                        ip = xfip.trim();
                    }
                }
            }

            XContentBuilder docBuilder = null;


            docBuilder = XContentFactory.jsonBuilder().startObject();


            docBuilder.field("ip", ip);

            docBuilder.field("id", dspaceObject.getID());

            // The numerical constant that represents the DSpaceObject TYPE. i.e. 0=bitstream, 2=item, ...
            docBuilder.field("typeIndex", dspaceObject.getType());

            // The text that represent the DSpaceObject TYPE. i.e. BITSTREAM, ITEM, COLLECTION, COMMUNITY
            docBuilder.field("type", Constants.typeText[dspaceObject.getType()]);

            // Save the current time
            docBuilder.field("time", DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
            if (currentUser != null) {
                docBuilder.field("epersonid", currentUser.getID());
            }

            try {
                String dns = DnsLookup.reverseDns(ip);
                docBuilder.field("dns", dns.toLowerCase());
            } catch (Exception e) {
                log.error("Failed DNS Lookup for IP:" + ip);
                log.debug(e.getMessage(), e);
            }

            // Save the location information if valid, save the event without
            // location information if not valid
            Location location = locationService.getLocation(ip);
            if (location != null
                    && !("--".equals(location.countryCode)
                    && location.latitude == -180 && location.longitude == -180)) {
                try {
                    docBuilder.field("continent", LocationUtils
                            .getContinentCode(location.countryCode));
                } catch (Exception e) {
                    System.out
                            .println("COUNTRY ERROR: " + location.countryCode);
                }
                docBuilder.field("countryCode", location.countryCode);
                docBuilder.field("city", location.city);
                docBuilder.field("latitude", location.latitude);
                docBuilder.field("longitude", location.longitude);
                docBuilder.field("isBot", isSpiderBot);

                if (request.getHeader("User-Agent") != null) {
                    docBuilder.field("userAgent", request.getHeader("User-Agent"));
                }
            }

            if (dspaceObject instanceof Bitstream) {
                Bitstream bit = (Bitstream) dspaceObject;
                Bundle[] bundles = bit.getBundles();
                docBuilder.field("bundleName").startArray();
                for (Bundle bundle : bundles) {
                    docBuilder.value(bundle.getName());
                }
                docBuilder.endArray();
            }

            storeParents(docBuilder, getParents(dspaceObject));

            docBuilder.endObject();

            if (docBuilder != null) {
                IndexRequestBuilder irb = client.prepareIndex(indexName, indexType)
                        .setSource(docBuilder);
                //log.info("Executing document insert into index");
                if(client == null) {
                    log.error("Hey, client is null");
                }
                irb.execute().actionGet();
            }

        } catch (RuntimeException re) {
            log.error("RunTimer in ESL:\n" + ExceptionUtils.getStackTrace(re));
            throw re;
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            client.close();
        }
    }

    public void post(DSpaceObject dspaceObject, String ip, String userAgent, String xforwarderfor, EPerson currentUser) {
        //log.info("DS-ES post for type:"+dspaceObject.getType() + " -- " + dspaceObject.getName());

        client = ElasticSearchLogger.getInstance().getClient();

        boolean isSpiderBot = SpiderDetector.isSpider(ip);

        try {
            if (isSpiderBot &&
                    !ConfigurationManager.getBooleanProperty("usage-statistics", "logBots", true)) {
                return;
            }


            // Save our basic info that we already have

            if (isUseProxies() && xforwarderfor != null) {
                /* This header is a comma delimited list */
                for (String xfip : xforwarderfor.split(",")) {
                    /* proxy itself will sometime populate this header with the same value in
                        remote address. ordering in spec is vague, we'll just take the last
                        not equal to the proxy
                    */
                    if (!xforwarderfor.contains(ip)) {
                        ip = xfip.trim();
                    }
                }
            }

            XContentBuilder docBuilder = null;


            docBuilder = XContentFactory.jsonBuilder().startObject();


            docBuilder.field("ip", ip);

            docBuilder.field("id", dspaceObject.getID());

            // The numerical constant that represents the DSpaceObject TYPE. i.e. 0=bitstream, 2=item, ...
            docBuilder.field("typeIndex", dspaceObject.getType());

            // The text that represent the DSpaceObject TYPE. i.e. BITSTREAM, ITEM, COLLECTION, COMMUNITY
            docBuilder.field("type", Constants.typeText[dspaceObject.getType()]);

            // Save the current time
            docBuilder.field("time", DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
            if (currentUser != null) {
                docBuilder.field("epersonid", currentUser.getID());
            }

            try {
                String dns = DnsLookup.reverseDns(ip);
                docBuilder.field("dns", dns.toLowerCase());
            } catch (Exception e) {
                log.error("Failed DNS Lookup for IP:" + ip);
                log.debug(e.getMessage(), e);
            }

            // Save the location information if valid, save the event without
            // location information if not valid
            Location location = locationService.getLocation(ip);
            if (location != null
                    && !("--".equals(location.countryCode)
                    && location.latitude == -180 && location.longitude == -180)) {
                try {
                    docBuilder.field("continent", LocationUtils
                            .getContinentCode(location.countryCode));
                } catch (Exception e) {
                    System.out
                            .println("COUNTRY ERROR: " + location.countryCode);
                }
                docBuilder.field("countryCode", location.countryCode);
                docBuilder.field("city", location.city);
                docBuilder.field("latitude", location.latitude);
                docBuilder.field("longitude", location.longitude);
                docBuilder.field("isBot", isSpiderBot);

                if (userAgent != null) {
                    docBuilder.field("userAgent", userAgent);
                }
            }

            if (dspaceObject instanceof Bitstream) {
                Bitstream bit = (Bitstream) dspaceObject;
                Bundle[] bundles = bit.getBundles();
                docBuilder.field("bundleName").startArray();
                for (Bundle bundle : bundles) {
                    docBuilder.value(bundle.getName());
                }
                docBuilder.endArray();
            }

            storeParents(docBuilder, getParents(dspaceObject));

            docBuilder.endObject();

            if (docBuilder != null) {
                IndexRequestBuilder irb = client.prepareIndex(indexName, indexType)
                        .setSource(docBuilder);
                //log.info("Executing document insert into index");
                if(client == null) {
                    log.error("Hey, client is null");
                }
                irb.execute().actionGet();
            }

        } catch (RuntimeException re) {
            log.error("RunTimer in ESL:\n" + ExceptionUtils.getStackTrace(re));
            throw re;
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            client.close();
        }
    }

    
    public static String getClusterName() {
        return clusterName;
    }

    public static void setClusterName(String clusterName) {
        ElasticSearchLogger.clusterName = clusterName;
    }

    public static String getIndexName() {
        return indexName;
    }

    public static void setIndexName(String indexName) {
        ElasticSearchLogger.indexName = indexName;
    }

    public static String getIndexType() {
        return indexType;
    }

    public static void setIndexType(String indexType) {
        ElasticSearchLogger.indexType = indexType;
    }

    public static String getAddress() {
        return address;
    }

    public static void setAddress(String address) {
        ElasticSearchLogger.address = address;
    }

    public static int getPort() {
        return port;
    }

    public static void setPort(int port) {
        ElasticSearchLogger.port = port;
    }

    public void buildParents(DSpaceObject dso, HashMap<String, ArrayList<Integer>> parents)
            throws SQLException {
        if (dso instanceof Community) {
            Community comm = (Community) dso;
            while (comm != null && comm.getParentCommunity() != null) {
                comm = comm.getParentCommunity();
                parents.get("owningComm").add(comm.getID());
            }
        } else if (dso instanceof Collection) {
            Collection coll = (Collection) dso;
            for (Community community : coll.getCommunities()) {
                parents.get("owningComm").add(community.getID());
                buildParents(community, parents);
            }
        } else if (dso instanceof Item) {
            Item item = (Item) dso;
            for (Collection collection : item.getCollections()) {
                parents.get("owningColl").add(collection.getID());
                buildParents(collection, parents);
            }
        } else if (dso instanceof Bitstream) {
            Bitstream bitstream = (Bitstream) dso;

            for (Bundle bundle : bitstream.getBundles()) {
                for (Item item : bundle.getItems()) {

                    parents.get("owningItem").add(item.getID());
                    buildParents(item, parents);
                }
            }
        }

    }

    public HashMap<String, ArrayList<Integer>> getParents(DSpaceObject dso)
            throws SQLException {
        HashMap<String, ArrayList<Integer>> parents = new HashMap<String, ArrayList<Integer>>();
        parents.put("owningComm", new ArrayList<Integer>());
        parents.put("owningColl", new ArrayList<Integer>());
        parents.put("owningItem", new ArrayList<Integer>());

        buildParents(dso, parents);
        return parents;
    }

    public void storeParents(XContentBuilder docBuilder, HashMap<String, ArrayList<Integer>> parents) throws IOException {

        Iterator it = parents.keySet().iterator();
        while (it.hasNext()) {

            String key = (String) it.next();

            ArrayList<Integer> ids = parents.get(key);

            if (ids.size() > 0) {
                docBuilder.field(key).startArray();
                for (Integer i : ids) {
                    docBuilder.value(i);
                }
                docBuilder.endArray();
            }
        }
    }


    public boolean isUseProxies() {
        return useProxies;
    }

    // Transport Client will talk to server on 9300
    public void createTransportClient() {
        // Configurable values for all elasticsearch connection constants
        // Can't guarantee that these values are already loaded, since this can be called by a different JVM
        clusterName = getConfigurationStringWithFallBack("elastic-search-statistics", "clusterName", clusterName);
        indexName   = getConfigurationStringWithFallBack("elastic-search-statistics", "indexName", indexName);
        indexType   = getConfigurationStringWithFallBack("elastic-search-statistics", "indexType", indexType);
        address     = getConfigurationStringWithFallBack("elastic-search-statistics", "address", address);
        port        = ConfigurationManager.getIntProperty("elastic-search-statistics", "port", port);

        log.info("Creating TransportClient to [Address:" + address + "] [Port:" + port + "] [cluster.name:" + clusterName + "]");

        Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build();
        client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(address, port));
    }
    
    public Client getClient() {
        //Get an available client, otherwise new default is NODE.
        return getClient(ClientType.NODE);
    }

    // Get the already available client, otherwise we will create a new client.
    // TODO Allow for config to determine which architecture / topology to use.
    //   - Local Node, store Data
    //   - Node Client, must discover a master within ES cluster
    //   - Transport Client, specify IP address of server running ES.
    public Client getClient(ClientType clientType) {
        if(client == null) {
            log.error("getClient reports null client");

            if(clientType == ClientType.TRANSPORT) {
                createTransportClient();
            } else {
                createNodeClient(clientType);
            }
        }

        return client;
    }

    // Node Client will discover other ES nodes running in local JVM
    public Client createNodeClient(ClientType clientType) {
        String dspaceDir = ConfigurationManager.getProperty("dspace.dir");
        Settings settings = ImmutableSettings.settingsBuilder().put("path.data", dspaceDir + "/elasticsearch/").build();
        
        NodeBuilder nodeBuilder = NodeBuilder.nodeBuilder().clusterName(clusterName).data(true).settings(settings);

        if(clientType == ClientType.LOCAL) {
            log.info("Create a Local Node.");
            nodeBuilder = nodeBuilder.local(true);
        } else if(clientType == ClientType.NODE) {
            log.info("Create a nodeClient, allows transport clients to connect");
            nodeBuilder = nodeBuilder.local(false);
        }

        Node node = nodeBuilder.node();
        log.info("Got node");
        client = node.client();
        log.info("Created new node client");
        return client;
    }
    
    public String getConfigurationStringWithFallBack(String module, String configurationKey, String defaultFallbackValue) {
        String configDrivenValue = ConfigurationManager.getProperty(module, configurationKey);
        if(configDrivenValue == null || configDrivenValue.trim().equalsIgnoreCase("")) {
            return defaultFallbackValue;
        } else {
            return configDrivenValue;
        }
    }

}
