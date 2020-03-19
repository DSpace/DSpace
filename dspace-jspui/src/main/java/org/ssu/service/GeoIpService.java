package org.ssu.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.statistics.util.SpiderDetector;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;

@Service
public class GeoIpService {
    private static Logger log = Logger.getLogger(GeoIpService.class);
    private static final String UNKNOWN_COUNTRY_CODE = "--";
    private static final DatabaseReader geoipLookup;;

    private static final boolean useProxies;

    static {
//    	log.info("solr.spidersfile:" + ConfigurationManager.getProperty("solr.spidersfile"));
//		log.info("solr.dbfile:" + ConfigurationManager.getProperty("solr.dbfile"));

        DatabaseReader service = null;
        // Get the db file for the location
        String dbfile = ConfigurationManager.getProperty("usage-statistics", "dbfile");

        if (dbfile != null) {
            try {
                File dbFile = new File(dbfile);
                service = new DatabaseReader.Builder(dbFile).build();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // System.out.println("NO SOLR DB FILE !");
        }
        geoipLookup = service;

        if ("true".equals(ConfigurationManager.getProperty("useProxies")))
            useProxies = true;
        else
            useProxies = false;

        log.info("useProxies=" + useProxies);
    }

    public static String getCountryCode(HttpServletRequest request) {
        if (geoipLookup == null)
            return UNKNOWN_COUNTRY_CODE;

        boolean isSpiderBot = SpiderDetector.isSpider(request);

        try {
            if(isSpiderBot) {
                return UNKNOWN_COUNTRY_CODE;
            }

            String ip = request.getRemoteAddr();

            if(useProxies && request.getHeader("X-Real-IP") != null) {
                /* This header is a comma delimited list */
                for(String xfip : request.getHeader("X-Real-IP").split(",")) {
                    /* proxy itself will sometime populate this header with the same value in
                        remote address. ordering in spec is vague, we'll just take the last
                        not equal to the proxy
                    */
                    if(!request.getHeader("X-Real-IP").contains(ip)) {
                        ip = xfip.trim();
                    }
                }
            }

            // Save the location information if valid, save the event without
            // location information if not valid

            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse cityResponse = geoipLookup.city(ipAddress);

            String countryCode = cityResponse.getCountry().getIsoCode();
            double longitude = cityResponse.getLocation().getLongitude();
            double latitude = cityResponse.getLocation().getLatitude();

            if (!(UNKNOWN_COUNTRY_CODE.equals(countryCode)
                    && latitude == -180 && longitude == -180)) {
                return Optional.ofNullable(countryCode).orElse(UNKNOWN_COUNTRY_CODE);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return UNKNOWN_COUNTRY_CODE;
    }
}
