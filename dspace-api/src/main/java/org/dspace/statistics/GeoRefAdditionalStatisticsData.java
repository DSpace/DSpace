/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.statistics.StatisticsMetadataGenerator;
import org.dspace.statistics.util.LocationUtils;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

public class GeoRefAdditionalStatisticsData implements
        StatisticsMetadataGenerator
{
    private static final LookupService locationService;
    
    private static Logger log = Logger
            .getLogger(GeoRefAdditionalStatisticsData.class);

    static {
        LookupService service = null;
        // Get the db file for the location
        String dbfile = ConfigurationManager.getProperty("solr.dbfile");
        if (dbfile != null)
        {
            try
            {
                service = new LookupService(dbfile,
                        LookupService.GEOIP_STANDARD);
            }
            catch (IOException e)
            {
                log.error(e.getMessage(), e);
            }
        }
        else
        {
            log.error("solr.dbfile: " + dbfile + " not found!");
        }
        locationService = service;
    }

    @Override
    public void addMetadata(SolrInputDocument doc1, HttpServletRequest request, DSpaceObject dspaceObject)
    {
        String ip = (String) doc1.getFieldValue("ip");
        // Save the location information if valid, save the event without
        // location information if not valid
        if (ConfigurationManager.getBooleanProperty(
                "solr.statistics.randomize-localhost", false)
                && ip.equals("127.0.0.1"))
        {
            ip ="";
            for (int j = 0; j < 4; j++) {
                ip += getRandomNumberInRange(0, 254);
                if (j != 3)
                    ip += ".";
            }
            String dns = (String) doc1.getFieldValue("dns");
            if (StringUtils.isEmpty(dns))
            {
                doc1.addField("dns",
                        new String[] { "cineca.it", "dspace.org", "hub.hku.hk",
                                "mit.edu" }[(int) getRandomNumberInRange(0, 3)]);
            }
        }
        
        String dnsValue = (String) doc1.getFieldValue("dns");
        if (dnsValue != null && dnsValue != doc1.getFieldValue("ip"))
        {
            
            int lastIndexOf = dnsValue.lastIndexOf(".");
            if (lastIndexOf != -1)
            {
                doc1.addField("topdomain", dnsValue.substring(lastIndexOf));
            }
        }
        
        Location location = locationService.getLocation(ip);
        if (location != null
                && !("--".equals(location.countryCode)
                        && location.latitude == -180 && location.longitude == -180))
        {
            doc1.addField("countryCode", location.countryCode);
            doc1.addField("city", location.city);
            doc1.addField("latitude", location.latitude);
            doc1.addField("longitude", location.longitude);
            doc1.addField("location", location.latitude + ","
                    + location.longitude);
        	try {
        		String continent = LocationUtils.getContinentCode(location.countryCode);
        		doc1.addField("continent", continent);
	    	} catch (Exception e) {
	    		// We could get an error if our country == Europa this doesn't
	    		// matter for generating statistics so ignore it
	    		log.error("COUNTRY ERROR: " + location.countryCode);
	    	}
        }
    }
    
    private static long getRandomNumberInRange(long min, long max) {
        return min + (long) (Math.random() * ((max - min) + 1));
    }
}
