/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

public class DiscoJuiceFeeds extends AbstractAction {
    /** log4j logger. */
    private static Logger log = Logger.getLogger(DiscoJuiceFeeds.class);
    private static final String discojuiceURL = "https://static.discojuice.org/feeds/";
    private static final LookupService locationService;
    /** contains entityIDs of idps we wish to set the country to something different than discojuice feeds suggests **/
    private static final Set<String> rewriteCountries;
    static {
        String dbfile = ConfigurationManager.getProperty("usage-statistics", "dbfile");
        LookupService service = null;
        if (dbfile != null)
        {
            try
            {
                service = new LookupService(dbfile,
                        LookupService.GEOIP_STANDARD);
            }
            catch (FileNotFoundException fe)
            {
                log.error("The GeoLite Database file is missing (" + dbfile + ")! Solr Statistics cannot generate location based reports! Please see the DSpace installation instructions for instructions to install this file.", fe);
            }
            catch (IOException e)
            {
                log.error("Unable to load GeoLite Database file (" + dbfile + ")! You may need to reinstall it. See the DSpace installation instructions for more details.", e);
            }
        }
        else
        {
            log.error("The required 'dbfile' configuration is missing in solr-statistics.cfg!");
        }
        locationService = service;
        
        rewriteCountries = new HashSet<String>();
        String propRewriteCountries = ConfigurationManager.getProperty("discojuice", "rewriteCountries");
        for(String country : propRewriteCountries.split(",")){
        	country = country.trim();
        	rewriteCountries.add(country);
        }
    }

    public Map act(Redirector redirector, SourceResolver resolver,
            Map objectModel, String source, Parameters parameters)
            throws Exception {

        File feedsFile = feedsFile();
        refreshFeedsFile(feedsFile);

        Map<String, String> map = new HashMap<String, String>();
        map.put("feedsFile", feedsFile.getAbsolutePath());

        return map;
    }

    public static File feedsFile(){
        String dspace_dir = ConfigurationManager.getProperty("dspace.dir");
        File feedsFile = new File(dspace_dir + "/webapps/xmlui/discojuice/feeds/feeds.jsonp");
        return feedsFile;
    }

    private void refreshFeedsFile(File feedsFile) throws Exception{
        String feedsConfig = ConfigurationManager.getProperty("discojuice", "feeds");
        String shibbolethDiscoFeedUrl = ConfigurationManager.getProperty("lr","lr.shibboleth.discofeed.url");

        if(feedsFile.exists()){
            Long refreshInterval = new Long(ConfigurationManager.getIntProperty("discojuice", "refresh")*3600000); //hours to millis
            Date refreshWhen = new Date(feedsFile.lastModified()+refreshInterval);
            Date now = new Date();
            if(refreshWhen.before(now)){
                try{
                    writeFeedsFile(feedsFile,feedsConfig,shibbolethDiscoFeedUrl);
                }catch(Exception e){
                    log.error("The feeds file was not refreshed."); //But we can use the old one
                }
            }
        }else{
            File parent = feedsFile.getParentFile();
            if(parent != null && !parent.exists()){
                parent.mkdirs();
            }
            writeFeedsFile(feedsFile,feedsConfig,shibbolethDiscoFeedUrl);
        }
    }

    private void writeFeedsFile(File feedsFile,String feedsConfig, String url) throws Exception{

        JSONParser parser = new JSONParser();

        Map<String,JSONObject> shibDiscoEntities = new HashMap<String,JSONObject>();
        URL shibDiscoFeedUrl = new URL(url);
        //Obtain shibboleths discofeed
        try{
            URLConnection conn = shibDiscoFeedUrl.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            //Caution does not follow redirects
            Object obj = parser.parse(new InputStreamReader(conn.getInputStream()));
            JSONArray entityArray = (JSONArray)obj;
            Iterator<JSONObject> i = entityArray.iterator();
            while(i.hasNext()){
                JSONObject entity = i.next();
                shibDiscoEntities.put((String)entity.get("entityID"), entity);
            }
        }catch(Exception e){
            log.error("Failed to obtain/parse "+shibDiscoFeedUrl.toString() + "\nCheck timeouts, redirects, shibboleth config.\n" + e);
            throw e; //Don't continue
        }

        //String[] feeds = {"edugain", "cesnet"};
        String[] feeds = feedsConfig.split(",");

        JSONArray filteredEntities = new JSONArray();

        List<String> seenIDs = new ArrayList<String>();

        try{
            for(String feed:feeds){
				URLConnection conn = new URL(discojuiceURL+feed.trim()).openConnection();
				conn.setConnectTimeout(5000);
				conn.setReadTimeout(10000);
				Object obj = parser.parse(new InputStreamReader(conn.getInputStream()));
				JSONArray entityArray = (JSONArray)obj;
				Iterator<JSONObject> i = entityArray.iterator();
				while(i.hasNext()){
					JSONObject entity = i.next();
					String entityID = (String)entity.get("entityID");
					if(shibDiscoEntities.containsKey(entityID)){
						if(rewriteCountries.contains(entityID)){
							String old_country = (String)entity.remove("country");
							String new_country = guessCountry(shibDiscoEntities.get(entityID));
							entity.put("country", new_country);
							log.info(String.format("For %s changed country from %s to %s", entityID, old_country, new_country));
						}
						filteredEntities.add(entity);
						seenIDs.add(entityID);
					}
				}
            }
        }catch(SocketTimeoutException ste){
            log.error(ste);
        }catch(ParseException pe){
            log.error(pe);
        }catch(IOException e){
            log.error(e);
        }

        //log missed shibboleth entries
        StringBuilder fromShib = new StringBuilder();
        for(String entityID:shibDiscoEntities.keySet()){
            if(!seenIDs.contains(entityID)){
                JSONObject entity = shibDiscoEntities.get(entityID);
                if(entity != null){
                    String country = guessCountry(entity);
                    entity.put("country", country);
                    filteredEntities.add(entity);
                    fromShib.append(entityID);
                    fromShib.append('\n');
                }
            }
        }

        if(fromShib.length()>0){
            log.info("The following entities were added from shibboleth disco feed.\n" + fromShib.toString());
        }

        FileWriter fw =  new FileWriter(feedsFile);
        fw.write("dj_md_1(");
        fw.write(filteredEntities.toJSONString());
        fw.write(")");
        fw.flush();
        fw.close();
        //System.out.println(filteredEntities.toJSONString());
    }

    private String guessCountry(JSONObject entity){
    	if(locationService != null && entity.containsKey("InformationURLs")){
    		JSONArray informationURLs = (JSONArray)entity.get("InformationURLs");
    		if(informationURLs.size() > 0){
    			String informationURL = (String)((JSONObject)informationURLs.get(0)).get("value");
    			try{
    				Location location = locationService.getLocation(java.net.InetAddress.getByName(new URL(informationURL).getHost()));
    				if(location != null && location.countryCode != null){
    					return location.countryCode;
    				}else{
    					log.info("Country or location is null for " + informationURL);
    				}
    			}catch(MalformedURLException e){
    				
    			}catch(java.net.UnknownHostException e){
    				
    			}
    		}
    	}
    	String entityID = (String)entity.get("entityID");
        //entityID not necessarily an URL
        try{
            URL url = new URL(entityID);
            String host = url.getHost();
            String topLevel = host.substring(host.lastIndexOf('.')+1);
            if(topLevel.length() == 2 && !topLevel.equalsIgnoreCase("eu")){
                //assume country code
                return topLevel.toUpperCase();
            }
        }catch(MalformedURLException e){

        }
        return "_all_"; //by default add "_all_", better search in dj
    }

    //For testing
    public static void main(String[] args) throws Exception{
        DiscoJuiceFeeds djf = new DiscoJuiceFeeds();
        djf.writeFeedsFile(new File("/tmp/feeds.jsonp"),
            "edugain, dfn, cesnet, surfnet2, haka, kalmar",
            "lindat.mff.cuni.cz");
    }

}
