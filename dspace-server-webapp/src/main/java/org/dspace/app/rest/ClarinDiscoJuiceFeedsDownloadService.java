/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.ws.rs.core.NoContentException;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The Service for downloading the DiscoFeeds and converting them to the String value.
 *
 * Functionality is copied from UFAL/CLARIN-DSPACE and wrapped to the service by
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Service
public class ClarinDiscoJuiceFeedsDownloadService implements InitializingBean {

    protected static Logger log = org.apache.logging.log4j.LogManager.getLogger(
            ClarinDiscoJuiceFeedsDownloadService.class);
    private static final String DISCOJUICE_URL = "https://static.discojuice.org/feeds/";

    /**
     * contains entityIDs of idps we wish to set the country to something different than discojuice feeds suggests
     **/
    private Set<String> rewriteCountries;
    protected static DatabaseReader locationService;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Instead of static {} method.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        String dbfile = configurationService.getProperty("usage-statistics.dbfile");
        DatabaseReader service = null;
        if (dbfile != null) {
            try {
                service = new DatabaseReader.Builder(
                        new BufferedInputStream(Files.newInputStream(Paths.get(dbfile)))).build();
            } catch (IOException e) {
                log.error("Unable to load GeoLite Database file (" + dbfile + ")! " +
                        "You may need to reinstall it. See the DSpace installation instructions for more details.", e);
            }
        } else {
            log.error("The required 'dbfile' configuration is missing in solr-statistics.cfg!");
        }
        locationService = service;

        rewriteCountries = new HashSet<String>();
        String[] propRewriteCountries = configurationService.getArrayProperty("discojuice.rewriteCountries");

        if (ArrayUtils.isEmpty(propRewriteCountries)) {
            throw new RuntimeException("The property `discojuice.rewriteCountries` is null!");
        }

        for (String country : propRewriteCountries) {
            country = country.trim();
            rewriteCountries.add(country);
        }
    }

    public String createFeedsContent() {
        log.info("Going to create feeds content.");
        String[] feedsConfig = configurationService.getArrayProperty("discojuice.feeds");
        String shibbolethDiscoFeedUrl = configurationService.getProperty("shibboleth.discofeed.url");

        if (StringUtils.isEmpty(shibbolethDiscoFeedUrl)) {
            throw new RuntimeException("Cannot load the property `shibboleth.discofeed.url` from the configuration " +
                    "file, maybe it is not set in the configuration file");
        }

        if (ArrayUtils.isEmpty(feedsConfig)) {
            throw new RuntimeException("Cannot load the property `discojuice.feeds` from the configuration " +
                    "file, maybe it is not set in the configuration file");
        }

        String old_value = System.getProperty("jsse.enableSNIExtension");
        System.setProperty("jsse.enableSNIExtension", "false");

        final Map<String, JSONObject> shibDiscoEntities = toMap(shrink(
                ClarinDiscoJuiceFeedsDownloadService.downloadJSON(shibbolethDiscoFeedUrl)));

        //true is the default http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html
        old_value = (old_value == null) ? "true" : old_value;
        System.setProperty("jsse.enableSNIExtension", old_value);

        String feedsContent = "";
        Set<String> processedEntities = new HashSet<>();
        //loop through disco cdn feeds
        for (String feed : feedsConfig) {
            Map<String, JSONObject> feedMap = toMap(
                    ClarinDiscoJuiceFeedsDownloadService.downloadJSON(DISCOJUICE_URL + feed.trim()));
            //loop through entities in one feed
            for (Map.Entry<String, JSONObject> entry: feedMap.entrySet()) {
                String entityID = entry.getKey();
                JSONObject cdnEntity = entry.getValue();
                //keep only entities from shibboleth, add only once, but copy geo, icon, country
                if (shibDiscoEntities.containsKey(entityID) && !processedEntities.contains(entityID)) {
                    JSONObject geo = (JSONObject) cdnEntity.get("geo");
                    String icon = (String) cdnEntity.get("icon");
                    String country = (String) cdnEntity.get("country");
                    JSONObject shibEntity = shibDiscoEntities.get(entityID);
                    if (geo != null) {
                        shibEntity.put("geo", geo);
                    }
                    if (icon != null) {
                        shibEntity.put("icon", icon);
                    }
                    if (country != null) {
                        shibEntity.put("country", country);
                    }
                    processedEntities.add(entityID);
                }
            }
        }

        //loop through shib entities, we show these...
        for (JSONObject shibEntity : shibDiscoEntities.values()) {
            //rewrite or guess countries
            if (rewriteCountries.contains(shibEntity.get("entityID")) || isBlank((String)shibEntity.get("country"))) {
                String old_country = (String)shibEntity.remove("country");
                String new_country = guessCountry(shibEntity);
                shibEntity.put("country", new_country);
                log.debug(String.format("For %s changed country from %s to %s", shibEntity.get("entityID"),
                        old_country, new_country));
            }
        }

        if (shibDiscoEntities.isEmpty()) {
            return null;
        } else {
            JSONArray ret = new JSONArray();
            ret.addAll(shibDiscoEntities.values());
            return ret.toJSONString();
        }
    }

    private static Map<String, JSONObject> toMap(JSONArray jsonArray) {
        Map<String, JSONObject> map = new HashMap<>();
        for (Object entityO : jsonArray) {
            JSONObject entity = (JSONObject) entityO;
            String entityID = (String) entity.get("entityID");
            if (!map.containsKey(entityID)) {
                map.put(entityID, entity);
            }
        }
        return map;
    }

    private static JSONArray shrink(JSONArray jsonArray) {
        for (Object entityO : jsonArray) {
            JSONObject entity = (JSONObject) entityO;
            // if there are DisplayNames only the first one will be used in title copy the rest
            // to keywords
            // copy any value in Keywords and Description to keywords
            for (String key: new String[]{"DisplayNames", "Keywords", "Descriptions"}) {
                if (entity.containsKey(key)) {
                    JSONArray keyObjects = (JSONArray) entity.get(key);
                    List<String> values = getValues(keyObjects);
                    if (!values.isEmpty()) {
                        if ("DisplayNames".equals(key)) {
                            entity.put("title", values.remove(0));
                            if (values.isEmpty()) {
                                continue;
                            }
                        }
                        if (entity.containsKey("keywords")) {
                            values.addAll((List<String>) entity.get("keywords"));
                        }
                        entity.put("keywords", values);
                    }
                }
            }

            // Logos (in contrast to icon) are currently unused by the fronted; they just eat bandwidth
            // The same for InformationURLs, Descriptions, PrivacyStatementURLs
            // Can be configured
            String[] toRemove = new DSpace().getConfigurationService().getPropertyAsType("discojuice" +
                    ".remove_from_shib_feed_object", new String[]
                    {
                        "Logos", "InformationURLs", "Descriptions", "PrivacyStatementURLs", "DisplayNames",
                        "Keywords"
                    });
            for (String key : toRemove) {
                entity.remove(key);
            }
        }
        return jsonArray;
    }

    private static List<String> getValues(JSONArray array) {
        ArrayList<String> res = new ArrayList<>(array.size());
        for (Object obj : array) {
            JSONObject jObj = (JSONObject) obj;
            if (jObj.containsKey("value")) {
                res.add((String)jObj.get("value"));
            }
        }
        return res;
    }

    /**
     * Open Connection for the test file or URL defined in the cfg.
     */
    private static URLConnection openURLConnection(String url) throws IOException {
        // If is not test.
        if (!StringUtils.startsWith(url,"TEST:")) {
            return new URL(url).openConnection();
        }

        URL testFileURL = null;
        // For test load the DiscoFeed response from the test file. The property must start with `TEST:` prefix.
        testFileURL = ClarinDiscoJuiceFeedsDownloadService.class.getResource("discofeedResponse.json");
        if (Objects.isNull(testFileURL)) {
            throw new NoContentException("Cannot open the test `discofeedResponse.json` file.");
        }
        return testFileURL.openConnection();
    }

    private static JSONArray downloadJSON(String url) {


        JSONParser parser = new JSONParser();
        try {
            URLConnection conn = openURLConnection(url);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            //Caution does not follow redirects, and even if you set it to http->https is not possible
            Object obj = parser.parse(new InputStreamReader(conn.getInputStream()));
            return (JSONArray) obj;
        } catch (IOException | ParseException e) {
            log.error("Failed to obtain/parse " + url + "\nCheck timeouts, redirects, shibboleth config.\n" + e);
        }
        return new JSONArray();
    }

    private static String guessCountry(JSONObject entity) {
        if (locationService != null && entity.containsKey("InformationURLs")) {
            JSONArray informationURLs = (JSONArray)entity.get("InformationURLs");
            if (informationURLs.size() > 0) {
                String informationURL = (String) ((JSONObject)informationURLs.get(0)).get("value");
                try {
                    CityResponse cityResponse = locationService.city(
                            InetAddress.getByName(new URL(informationURL).getHost()));
                    if (cityResponse != null && cityResponse.getCountry() != null &&
                            isNotBlank(cityResponse.getCountry().getIsoCode())) {
                        String code = cityResponse.getCountry().getIsoCode();
                        log.debug("Found code " + code + " for " + informationURL);
                        return code;
                    } else {
                        log.debug("Country or location is null for " + informationURL);
                    }
                } catch (IOException | GeoIp2Exception e) {
                    log.debug(e);
                }
            }
        }
        String entityID = (String) entity.get("entityID");
        // entityID not necessarily an URL
        try {
            URL url = new URL(entityID);
            String host = url.getHost();
            String topLevel = host.substring(host.lastIndexOf('.') + 1);
            if (topLevel.length() == 2 && !topLevel.equalsIgnoreCase("eu")) {
                // assume country code
                return topLevel.toUpperCase();
            }
        } catch (MalformedURLException e) {
            log.debug("ERROR: " + e.getMessage());
        }
        return "_all_"; // by default add "_all_", better search in dj
    }

}
