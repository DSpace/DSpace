package uk.ac.edina.datashare.irusuk;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.content.DCDate;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import com.atmire.statistics.export.ExportUsageEventListener;

/**
 * Logs a datashare zip download with IRUS UK.
 */
public class IRUSUKLog {
    private static final Logger LOG = Logger.getLogger(IRUSUKLog.class);
    private StringBuffer data;

    /**
     * Log a download with IRUS UK.
     * @param context
     * @param stat
     */
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
                if(ConfigurationManager.getProperty("dspace.hostname") == "datashare.is.ed.ac.uk"){
                    LOG.info("send: " + this.urlString);
                    ExportUsageEventListener.processUrl(this.context, this.urlString);
                }
                else{
                    LOG.info("not sent: " + this.urlString);
                }
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
