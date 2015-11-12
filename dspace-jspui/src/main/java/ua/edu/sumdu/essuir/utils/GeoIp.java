package ua.edu.sumdu.essuir.utils;


import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.statistics.util.SpiderDetector;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;


public class GeoIp  {

	private static Logger log = Logger.getLogger(GeoIp.class);
	
    private static final LookupService locationService;

    private static final boolean useProxies;

    static {
//    	log.info("solr.spidersfile:" + ConfigurationManager.getProperty("solr.spidersfile"));
//		log.info("solr.dbfile:" + ConfigurationManager.getProperty("solr.dbfile"));

        LookupService service = null;
        // Get the db file for the location
	String dbfile = ConfigurationManager.getProperty("usage-statistics", "dbfile");
	
       if (dbfile != null) {
            try {
                service = new LookupService(dbfile,
                        LookupService.GEOIP_STANDARD);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // System.out.println("NO SOLR DB FILE !");
        }
        locationService = service;
        
        if ("true".equals(ConfigurationManager.getProperty("useProxies")))
            useProxies = true;
        else
            useProxies = false;

        log.info("useProxies=" + useProxies);
    }

    public static String getCountryCode(HttpServletRequest request) {
        if (locationService == null)
            return null;

        boolean isSpiderBot = SpiderDetector.isSpider(request);

        try {
            if(isSpiderBot) {
                return null;
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
            Location location = locationService.getLocation(ip);
            
            if (location != null
                    && !("--".equals(location.countryCode)
                            && location.latitude == -180 && location.longitude == -180)) {
                return location.countryCode;
            }
            
        } catch (Exception e) {
        	log.error(e.getMessage(), e);
        }
        
        return "--";
    }

}
