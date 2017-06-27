package uk.ac.edina.datashare.irusuk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Read apache log and log dataset downloads with irusuk
 */
public class ApacheLogReader {
    private Context context = null;
    private String handlePrefix = null;
    private String hostName = null;
    private String apacheLogDir = null;
    private static final Logger LOG = Logger.getLogger(ApacheLogReader.class);
    private static final String LOG_ENTRY_PATTERN =
            // 1:IP  2:logname 3:user 4:date time                 5:method 6:req 7:proto 8:respcode 9:size 10:cip 11:time 12:ref 13:user-agent
            "^(\\S+) (\\S+) (\\S+) \\[([\\w:/]+\\s[+\\-]\\d{4})\\] \"(\\S+) (\\S+) (\\S+)\" (\\d{3}) (\\S+) (\\S+) (\\d+) \"(\\S+)\" \"(.*)\"";
    private static final Pattern PATTERN = Pattern.compile(LOG_ENTRY_PATTERN);
    
    public ApacheLogReader(){
        try{
            this.context = new Context();
            this.handlePrefix = ConfigurationManager.getProperty("handle.prefix");
            this.hostName = ConfigurationManager.getProperty("dspace.hostname");
            this.apacheLogDir = ConfigurationManager.getProperty("apache.log.dir");
        }
        catch(SQLException ex){
            throw new RuntimeException(ex);
        }
    }
    
    private IDownloadStat parseLine(String line) {
        ApacheDownloadStat stat = null;
        Matcher m = PATTERN.matcher(line);
        if (!m.find()) {
            System.out.println("Cannot parse logline: " + line);
            LOG.warn("Cannot parse logline" + line);
        }
        else{
            String req = m.group(6);
            if(req.startsWith("/download")){
                DateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");
                try{
                    stat = new ApacheDownloadStat(
                            m.group(1),
                            dateFormat.parse(m.group(4)),
                            m.group(7).split("/")[0].toLowerCase(),
                            m.group(12),
                            m.group(13),
                            req);
                }
                catch(ParseException ex){
                    throw new RuntimeException(ex);
                }
            }
        }
        
        return stat;
    }

    private void processLog(){
        // get yesterdays log
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String logName = this.apacheLogDir + File.separator +
                "access_log." + dateFormat.format(cal.getTime());
        try (BufferedReader br = new BufferedReader(new FileReader(logName))) {
            String line;
            while ((line = br.readLine()) != null) {
                IDownloadStat stat = this.parseLine(line);
                if(stat != null){
                    new IRUSUKLog(this.context, stat);                    
                }
            }
        }
        catch(FileNotFoundException ex){
            throw new RuntimeException(ex);
        }
        catch(IOException ex){
            throw new RuntimeException(ex);
        }
        finally{
            try{
                this.context.complete();
            }
            catch(SQLException ex){}
        }
    }
        
    private class ApacheDownloadStat implements IDownloadStat{
        private String handle = null;
        private String ipAddress = "";        
        private String referrer = "";
        private String userAgent = null;
        private Date date = null;
        private URL url = null;
        
        public ApacheDownloadStat(
                String ipAddress,
                Date date,
                String protocol,
                String referrer,
                String userAgent,
                String request){
            if(ipAddress.length() > 1){
                this.ipAddress = ipAddress;
            }
            
            if(referrer.length() > 1){
                this.referrer = referrer;    
            }
            
            this.userAgent = userAgent;
            this.date = date;
            
            String handle = request.substring(request.lastIndexOf("_") + 1, request.indexOf(".zip"));
            this.handle = ApacheLogReader.this.handlePrefix + "/" + handle;
            
            try{
                this.url = new URL(protocol, ApacheLogReader.this.hostName, request);
            }
            catch(MalformedURLException ex){
                throw new RuntimeException(ex);
            }
        }
        
        @Override
        public Date getDate() {
            return this.date;
        }
        
        @Override
        public String getIPAddress() {
            return this.ipAddress;
        }
        
        @Override
        public String getOAIIdentifer() {
            return "oai:" + ApacheLogReader.this.hostName + ":" + this.handle; 
       }
        
        @Override
        public String getReferrer() {
            return this.referrer;
        }
        
        @Override
        public String getUserAgent() {
            return this.userAgent;
        }

        @Override
        public URL getUrl() {
            return this.url;
        } 
        
        @Override
        public String toString() {
            return "Date         : " + this.getDate() + "\n" +
                   "IP Address   : " + this.getIPAddress() + "\n" +
                   "OAIIdentifer : " + this.getOAIIdentifer() + "\n" +
                   "Referrer     : " + this.getReferrer() + "\n" +
                   "UserAgent    : " + this.getUserAgent() + "\n" +
                   "URL          : " + this.getUrl() + "\n";
        }
    }
    
    public static void main(String[] args){
        new ApacheLogReader().processLog();
    }
}
