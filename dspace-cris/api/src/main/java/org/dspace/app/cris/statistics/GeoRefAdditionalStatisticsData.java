/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.statistics.util.StatsConfig;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.StatisticsMetadataGenerator;
import org.dspace.utils.DSpace;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

public class GeoRefAdditionalStatisticsData implements
        StatisticsMetadataGenerator
{
    private LookupService locationService;

    private static Logger log = Logger
            .getLogger(GeoRefAdditionalStatisticsData.class);

    private Properties countries2Continent = null;

    public Properties getCountries2Continent()
    {

        if (countries2Continent == null)
        {
            countries2Continent = new Properties();
            FileInputStream fcc = null;
            // FileInputStream fcn = null;
            try
            {
                fcc = new FileInputStream(ConfigurationManager
                        .getProperty("dspace.dir")
                        + "/config/countries2continent.properties");
                countries2Continent.load(fcc);
            }
            catch (Exception notfound)
            {
                throw new IllegalArgumentException(
                        "Failed to load configuration file for GeoRefAdditionalStatisticsData",
                        notfound);
            }
            finally
            {
                if (fcc != null)
                {
                    try
                    {
                        fcc.close();
                    }
                    catch (IOException ioe)
                    {
                        log.error(ioe.getMessage(), ioe);
                    }
                }
            }
        }
        return countries2Continent;
    }

    @Override
    public void addMetadata(SolrInputDocument doc1, HttpServletRequest request,
            DSpaceObject dspaceObject)
    {
        String ip = (String) doc1.getFieldValue("ip");
        if (ip == null)
            return;
        // Save the location information if valid, save the event without
        // location information if not valid
        if (ConfigurationManager.getBooleanProperty(
                SolrLogger.CFG_USAGE_MODULE, "randomize-localhost", false)
                && ip.equals("127.0.0.1"))
        {
            ip = "";
            for (int j = 0; j < 4; j++)
            {
                ip += getRandomNumberInRange(0, 254);
                if (j != 3)
                    ip += ".";
            }
            String dns = (String) doc1.getFieldValue("dns");
            if (StringUtils.isEmpty(dns))
            {
                doc1.addField("dns",
                        new String[] { "cilea.it", "enel.com", "hub.hku.hk",
                                "mit.edu" }[(int) getRandomNumberInRange(0, 3)]);
            }
        }

        String dnsValue = (String) doc1.getFieldValue("dns");
        if (StringUtils.isNotBlank(dnsValue )){
        	int firstIndexof =  dnsValue.indexOf(".");
        	if(firstIndexof != -1){
        		doc1.addField("domaindns", dnsValue.substring(firstIndexof));
        	}
        	
        	if( dnsValue != doc1.getFieldValue("ip")){
        		int lastIndexOf = dnsValue.lastIndexOf(".");
        		if (lastIndexOf != -1)
        		{
        			doc1.addField("topdomain", dnsValue.substring(lastIndexOf));
        		}
       		}
        }

        Location location = getLocationService().getLocation(ip);
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
            if (location.countryCode != null)
            {
                String continentCode = getCountries2Continent()
                        .getProperty(location.countryCode);
                if (continentCode == null)
                {
                    continentCode = getCountries2Continent().getProperty("default");
                }
                if (continentCode != null)
                {
                    doc1.addField("continent", continentCode);
                }
            }
        }
    }

    private static long getRandomNumberInRange(long min, long max)
    {
        return min + (long) (Math.random() * ((max - min) + 1));
    }

    public LookupService getLocationService()
    {

        if (locationService == null)
        {
            LookupService service = null;
            // Get the db file for the location
            String dbfile = ConfigurationManager.getProperty(
                    SolrLogger.CFG_USAGE_MODULE, "dbfile");
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
        return locationService;
    }
}
