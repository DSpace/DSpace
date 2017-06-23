package uk.ac.edina.datashare.irusuk;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Date;

import org.apache.cocoon.environment.Request;
import org.dspace.app.util.Util;
import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import com.atmire.statistics.export.ExportUsageEventListener;

/**
 * Logs a datashare zip download with IRUS UK.
 */
public class IRUSUKLog {
    //private Logger LOG = Logger.getLogger(IRISUKLog.class);
    private StringBuffer data;
    
    /**
     * @param context DSpace context.
     * @param request Cocoon request.
     * @param item DSpace item.
     * @param fileName Zip file name.
     */
	public IRUSUKLog(Context context, Request request, DSpaceObject item, String fileName){ 
	    this.data = new StringBuffer();
	    this.addField("url_ver", this.getTrackerURlVersion(), true);
	    this.addField("req_id", this.getIpAddress(request));
	    this.addField("req_dat", this.getUserAgent(request)); 
	    this.addField("rft.artnum", this.getOAIIdentifer(item)); 
	    this.addField("rfr_dat", this.getReferrer(request));
	    this.addField("rfr_id", this.getSource());
	    this.addField("url_tim", this.getDate());
	    this.addField("svc_dat", this.getUrl(item, fileName));
	    
	    String url = ConfigurationManager.getProperty("stats", "tracker.baseurl") + "?" + this.data.toString();
	    this.send(context, url);
	}
	
	public IRUSUKLog(Context context, IDownloadStat stat){
	    this.data = new StringBuffer();
        this.addField("url_ver", this.getTrackerURlVersion(), true);
        this.addField("req_id", stat.getIPAddress());
        this.addField("req_dat", stat.getUserAgent()); 
        this.addField("rft.artnum", stat.getOAIIdentifer()); 
        this.addField("rfr_dat", stat.getReferrer());
        this.addField("rfr_id", this.getSource());
        this.addField("url_tim", new DCDate(stat.getDate()).toString());
        this.addField("svc_dat", stat.getUrl().toString());
        
        String url = ConfigurationManager.getProperty("stats", "tracker.baseurl") + "?" + this.data.toString();
        this.send(context, url);
	}
	
	/**
	 * Add a field to log.
	 * @param field Log field name.
	 * @param value Log field value.
	 */
	private void addField(String field, String value){
	    this.addField(field, value, false);
	}
	
    /**
     * Add a field to log.
     * @param field Log field name.
     * @param value Log field value.
     * @boolean first Is this field the first to be added?
     */
    private void addField(String field, String value, boolean first){
        try{
            if(!first){
                this.data.append("&");
            }
            
            this.data.append(URLEncoder.encode(field, "UTF-8")).append("=").append(URLEncoder.encode(value, "UTF-8"));
        }
        catch(UnsupportedEncodingException ex){
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * @return Full data string sent to IRIS.
     */
    public String getData(){
    	return this.data.toString();
    }
    
    /**
     * @return Current date.
     */
    private String getDate(){
        return new DCDate(new Date()).toString();
    }
      
    /**
     * @param request Cocoon request.
     * @return User's ip address.
     */
    private String getIpAddress(Request request){
        return Util.getIPAddress(request);
    }
    
    /**
     * @param item DSpace item.
     * @return OAI identifier/URI.
     */
    private String getOAIIdentifer(DSpaceObject item){
        return "oai:" + ConfigurationManager.getProperty("dspace.hostname") + ":" + item.getHandle();
    }
    
    /**
     * @param request Coccon request.
     * @return Page referer.
     */
    private String getReferrer(Request request){
        String referer = request.getHeader("referer");
        if(referer == null){
            referer = "";
        }
        
        return referer;
    }
    
    /**
     * @return The source of the log.
     */
    private String getSource(){
        return ConfigurationManager.getProperty("dspace.hostname");
    }
    
    /**
     * @return Tracker Version url.
     */
    private String getTrackerURlVersion(){
        return ConfigurationManager.getProperty("stats", "tracker.urlversion");
    }
    
    /**
     * @return The url of the downloaded item.
     */
    private String getUrl(DSpaceObject item, String fileName){
        return ConfigurationManager.getProperty("dspace.url") + "/download/" + item.getHandle() + "/" + fileName;
    }

    /**
     * @return The user agent.
     */
	private String getUserAgent(Request request){
	    return request.getHeader("USER-AGENT");
	}
	
    /**
     * @return Send log in new thread.
     */
	private void send(Context context, String urlString){
	    // send log in new thread
	    SendThread t = new SendThread(context, urlString);
	    t.start();
	}
	
	/**
	 * Class sends log to IRUS UK in a new thread.
	 */
	private class SendThread extends Thread {
	    Context context;
	    String urlString;
	    
        SendThread(Context context, String urlString) {
            this.context = context;
            this.urlString = urlString;
        }

        public void run() {
            try{
            	ExportUsageEventListener.processUrl(this.context, this.urlString);
            }
            catch(IOException ex){
                throw new RuntimeException(ex);
            }
            catch(SQLException ex){
                throw new RuntimeException(ex);
            }
        }
    }
}
