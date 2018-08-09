/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;

import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.statistics.service.ElasticSearchLoggerService;
import org.dspace.statistics.util.DnsLookup;
import org.dspace.statistics.util.LocationUtils;
import org.dspace.statistics.util.SpiderDetector;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import org.springframework.beans.factory.InitializingBean;

/*
 * @deprecated  As of DSpace 6.0, ElasticSearch statistics are replaced by Solr statistics
 * @see org.dspace.statistics.SolrLoggerServiceImpl#SolrLoggerServiceImpl
 */
public class ElasticSearchLoggerServiceImpl implements ElasticSearchLoggerService, InitializingBean {

    private static Logger log = Logger.getLogger(ElasticSearchLoggerServiceImpl.class);

    protected boolean useProxies;

    public static final String DATE_FORMAT_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String DATE_FORMAT_DCDATE = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    protected static DatabaseReader locationService;

    protected String clusterName = "dspacestatslogging";
    protected String indexName = "dspaceindex";
    protected String indexType = "stats";
    protected String address = "127.0.0.1";
    protected int port = 9300;

    protected Client client;

    protected ElasticSearchLoggerServiceImpl() {
        // nobody should be instantiating this...
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("DSpace ElasticSearchLogger Initializing");
        try {
        DatabaseReader service = null;
        // Get the db file for the location
        String dbPath = ConfigurationManager.getProperty("usage-statistics.dbfile");
        if (dbPath != null) {
            try {
                File dbFile = new File(dbPath);
                service = new DatabaseReader.Builder(dbFile).build();
            } catch (FileNotFoundException fe) {
                log.error(
                        "The GeoLite Database file is missing (" + dbPath + ")! Usage Statistics cannot generate location"
                            + " based reports! Please see the DSpace installation instructions for instructions to"
                            + " install this file.",
                        fe);
            } catch (IOException e) {
                log.error(
                        "Unable to load GeoLite Database file (" + dbPath + ")! You may need to reinstall it. See the"
                            + " DSpace installation instructions for more details.",
                        e);
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
            boolean hasIndex = false;
            try {
                log.info("Checking Elastic Search cluster health...");
                ClusterHealthResponse healthResponse = client.admin().cluster().prepareHealth(indexName).setWaitForYellowStatus()
                        .setTimeout(TimeValue.timeValueSeconds(30)).execute()
                        .actionGet();

                if (healthResponse.isTimedOut() || healthResponse.getStatus() == ClusterHealthStatus.RED) {
                    throw new IllegalStateException("cluster not ready due to health: " + healthResponse.toString());
                }

                log.info("DS ES Checking if index exists");
                hasIndex = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
            } catch (Exception e) {
                log.error("Exception during health check, likely have to create index and put mapping still. Exception:" + e.getMessage());
                hasIndex = false;
            }

        if(! hasIndex) {
            //If elastic search index exists, then we are good to go, otherwise, we need to create that index. Should only need to happen once ever.
            log.info("DS ES index didn't exist, we need to create it.");

            String mappingPath = ElasticSearchLoggerServiceImpl.class.getPackage().getName().replaceAll("\\.", "/") ;
            URL url = Resources.getResource(mappingPath + "/elasticsearch-statistics-mapping.json");
            String stringMappingJSON = Resources.toString(url, Charsets.UTF_8);
            stringMappingJSON = stringMappingJSON.replace("stats", indexType);

            //Necessary to post, in order for index/type to be created.
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
            log.error("Elastic Search crashed during init. " + e.getMessage(), e);
        }
    }

    @Override
    public void post(DSpaceObject dspaceObject, HttpServletRequest request, EPerson currentUser) {
        //log.info("DS-ES post for type:"+dspaceObject.getType() + " -- " + dspaceObject.getName());

        client = getClient();

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
                log.info("Failed DNS Lookup for IP:" + ip);
                log.debug(e.getMessage(), e);
            }

            // Save the location information if valid, save the event without
            // location information if not valid
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse location = locationService.city(ipAddress);
            String countryCode = location.getCountry().getIsoCode();
            double latitude = location.getLocation().getLatitude();
            double longitude = location.getLocation().getLongitude();
            if (!("--".equals(countryCode)
                    && latitude == -180 && longitude == -180)) {
                try {
                    docBuilder.field("continent", LocationUtils
                            .getContinentCode(countryCode));
                } catch (IOException e) {
                    System.out
                            .println("COUNTRY ERROR: " + countryCode);
                }
                docBuilder.field("countryCode", countryCode);
                docBuilder.field("city", location.getCity().getName());
                docBuilder.field("latitude", latitude);
                docBuilder.field("longitude", longitude);
                docBuilder.field("isBot", isSpiderBot);

                if (request.getHeader("User-Agent") != null) {
                    docBuilder.field("userAgent", request.getHeader("User-Agent"));
                }
            }

            if (dspaceObject instanceof Bitstream) {
                Bitstream bit = (Bitstream) dspaceObject;
                List<Bundle> bundles = bit.getBundles();
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

    @Override
    public void post(DSpaceObject dspaceObject, String ip, String userAgent, String xforwardedfor, EPerson currentUser) {
        //log.info("DS-ES post for type:"+dspaceObject.getType() + " -- " + dspaceObject.getName());

        client = getClient();

        boolean isSpiderBot = SpiderDetector.isSpider(ip);

        try {
            if (isSpiderBot &&
                    !ConfigurationManager.getBooleanProperty("usage-statistics", "logBots", true)) {
                return;
            }


            // Save our basic info that we already have

            if (isUseProxies() && xforwardedfor != null) {
                /* This header is a comma delimited list */
                for (String xfip : xforwardedfor.split(",")) {
                    /* proxy itself will sometime populate this header with the same value in
                        remote address. ordering in spec is vague, we'll just take the last
                        not equal to the proxy
                    */
                    if (!xforwardedfor.contains(ip)) {
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
                log.info("Failed DNS Lookup for IP:" + ip);
                log.debug(e.getMessage(), e);
            }

            // Save the location information if valid, save the event without
            // location information if not valid.
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse location = locationService.city(ipAddress);
            String countryCode = location.getCountry().getIsoCode();
            double latitude = location.getLocation().getLatitude();
            double longitude = location.getLocation().getLongitude();
            if (!("--".equals(countryCode)
                    && latitude == -180 && longitude == -180)) {
                try {
                    docBuilder.field("continent", LocationUtils
                            .getContinentCode(countryCode));
                } catch (IOException e) {
                    System.out
                            .println("COUNTRY ERROR: " + countryCode);
                }
                docBuilder.field("countryCode", countryCode);
                docBuilder.field("city", location.getCity().getName());
                docBuilder.field("latitude", latitude);
                docBuilder.field("longitude", longitude);
                docBuilder.field("isBot", isSpiderBot);

                if (userAgent != null) {
                    docBuilder.field("userAgent", userAgent);
                }
            }

            if (dspaceObject instanceof Bitstream) {
                Bitstream bit = (Bitstream) dspaceObject;
                List<Bundle> bundles = bit.getBundles();
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

    
    @Override
    public String getClusterName() {
        return clusterName;
    }

    @Override
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    @Override
    public String getIndexName() {
        return indexName;
    }

    @Override
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    @Override
    public String getIndexType() {
        return indexType;
    }

    @Override
    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void buildParents(DSpaceObject dso, HashMap<String, ArrayList<String>> parents)
            throws SQLException {
        if (dso instanceof Community) {
            Community comm = (Community) dso;
            while (comm != null && CollectionUtils.isNotEmpty(comm.getParentCommunities())) {
                comm = comm.getParentCommunities().get(0);
                parents.get("owningComm").add(comm.getID().toString());
            }
        } else if (dso instanceof Collection) {
            Collection coll = (Collection) dso;
            for (Community community : coll.getCommunities()) {
                parents.get("owningComm").add(community.getID().toString());
                buildParents(community, parents);
            }
        } else if (dso instanceof Item) {
            Item item = (Item) dso;
            for (Collection collection : item.getCollections()) {
                parents.get("owningColl").add(collection.getID().toString());
                buildParents(collection, parents);
            }
        } else if (dso instanceof Bitstream) {
            Bitstream bitstream = (Bitstream) dso;

            for (Bundle bundle : bitstream.getBundles()) {
                for (Item item : bundle.getItems()) {

                    parents.get("owningItem").add(item.getID().toString());
                    buildParents(item, parents);
                }
            }
        }

    }

    @Override
    public HashMap<String, ArrayList<String>> getParents(DSpaceObject dso)
            throws SQLException {
        HashMap<String, ArrayList<String>> parents = new HashMap<>();
        parents.put("owningComm", new ArrayList<String>());
        parents.put("owningColl", new ArrayList<String>());
        parents.put("owningItem", new ArrayList<String>());

        buildParents(dso, parents);
        return parents;
    }

    @Override
    public void storeParents(XContentBuilder docBuilder, HashMap<String, ArrayList<String>> parents) throws IOException {

        Iterator it = parents.keySet().iterator();
        while (it.hasNext()) {

            String key = (String) it.next();

            ArrayList<String> ids = parents.get(key);

            if (ids.size() > 0) {
                docBuilder.field(key).startArray();
                for (String i : ids) {
                    docBuilder.value(i);
                }
                docBuilder.endArray();
            }
        }
    }


    @Override
    public boolean isUseProxies() {
        return useProxies;
    }

    // Transport Client will talk to server on 9300
    @Override
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
    
    @Override
    public Client getClient() {
        //Get an available client, otherwise new default is NODE.
        return getClient(ClientType.NODE);
    }

    // Get the already available client, otherwise we will create a new client.
    // TODO Allow for config to determine which architecture / topology to use.
    //   - Local Node, store Data
    //   - Node Client, must discover a master within ES cluster
    //   - Transport Client, specify IP address of server running ES.
    @Override
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

    @Override
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
    
    @Override
    public String getConfigurationStringWithFallBack(String module, String configurationKey, String defaultFallbackValue) {
        String configDrivenValue = ConfigurationManager.getProperty(module, configurationKey);
        if(configDrivenValue == null || configDrivenValue.trim().equalsIgnoreCase("")) {
            return defaultFallbackValue;
        } else {
            return configDrivenValue;
        }
    }

}
