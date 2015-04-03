/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.dspace.app.statistics.LogAnalyser;
import org.dspace.checker.BitstreamInfo;
import org.dspace.checker.BitstreamInfoDAO;
import org.dspace.checker.CheckerCommand;
import org.dspace.checker.ChecksumCheckResults;
import org.dspace.checker.ChecksumResultsCollector;
import org.dspace.checker.SimpleDispatcher;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.curate.Curator;
import org.dspace.embargo.EmbargoManager;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;

import com.ibm.icu.text.SimpleDateFormat;

import cz.cuni.mff.ufal.checks.ImportantLogs;
import cz.cuni.mff.ufal.checks.ShibUserLogins;
import cz.cuni.mff.ufal.dspace.IOUtils;
import cz.cuni.mff.ufal.dspace.PIDService;
import cz.cuni.mff.ufal.dspace.handle.PIDLogMiner;
import cz.cuni.mff.ufal.dspace.handle.PIDLogStatistics;
import cz.cuni.mff.ufal.dspace.handle.PIDLogStatisticsEntry;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

/**
 * 
 * @author jmš
 */
@SuppressWarnings("deprecation")
public class Report {

    /** log4j logger. */
    private static Logger log = Logger.getLogger(Report.class);
    public static final String EMAIL_PATH = "config/emails/ufal_statistics";
    
    // generate info (logs) from before 7 days
    private static final int FROM_LAST_DAYS = -7;
    
    // vars
    // 
    
    private LinkedHashMap<String,String> reports_output_ = null;
    private GregorianCalendar start_date_ = null;
    private GregorianCalendar end_date_ = null;
    private long start_ = -1L;
    private LinkedHashMap<String, simple_report> reports_;
    
    // ctor
    //
    
    public Report() {
        reports_output_ = new LinkedHashMap<String,String>();
        
        GregorianCalendar calendar = new GregorianCalendar();
        end_date_ = new GregorianCalendar( 
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        start_date_ = (GregorianCalendar)end_date_.clone();
        // get info from the last 7 days
        start_date_.add(Calendar.DAY_OF_MONTH, FROM_LAST_DAYS);
        start_ = System.currentTimeMillis();
        //
        init_reports();
    }
    
    private void init_reports() {
        reports_ = new LinkedHashMap<String, simple_report>();
        reports_.put( "Date", new report_date_info(start_date_, end_date_) );
        reports_.put( "Interesting url info", new report_interesting_urls(end_date_) );
        reports_.put( "Item count", new report_item_count() );
        reports_.put( "PID service info", new report_pid_service() );
        reports_.put( "Item rights info", new report_item_rights_info() );
        reports_.put( "Embargoed item", new report_item_embargo() );
        reports_.put( "Item(s) info", new report_item_info() );
        reports_.put( "User info", new report_user_count() );
        reports_.put( "Server info", new report_server_info() );
        reports_.put( "Log info", new report_log_info(start_date_, end_date_) );
        reports_.put( "Checksum checker", new report_checksum() );
        reports_.put( "Curation tasks", new report_curator() );
        reports_.put( "Discojuice feeds", new report_discojuice_info() );
        reports_.put( "VLO harvester", new report_vlo_info() );
        reports_.put( "OAI-PMH validation", new report_oaipmh_validator() );
        reports_.put( "Server resources info", new report_vm_bean_counters() );
        reports_.put( "Assetstore verify file validity", new report_assetstore_validity() );
        reports_.put( "Handle(s) info", new report_handle_info() );
        reports_.put( "Handle resolution statistics", new report_handle_resolution_statistics(start_date_, end_date_) );
        reports_.put( "Shibboleth curation tasks", new report_shibd_curation() );
        reports_.put( "Logs curation tasks", new report_logs_curation(start_date_, end_date_) );
    }

    // helpers
    //
    public void generate() {
        generate(null);
    }
    
    public void generate(List<Integer> to_perform) 
    {
        String url = ConfigurationManager.getProperty("dspace.url");
        add_report("Url", url);
        
        int pos = -1;
        for ( Entry<String, simple_report> report : reports_.entrySet() ) 
        {
            ++pos;
            if ( to_perform != null && !to_perform.contains(pos) ) {
                    continue;
            }
            System.err.println( String.format("#%d. Processing [%s] at [%s]", 
                    pos, report.getKey(),  
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
                );

            // do the stuff
            String output = "";
            try {
                output = report.getValue().process();
            }catch(Exception e) {
                output = "exception occurred when processing report - " + ExceptionUtils.getStackTrace(e);
            }
            add_report(report.getKey(), output);
        }
    }
    
    //
    //
    public String toString() {
        String ret = "";
        for ( Entry<String,String> i : reports_output_.entrySet() ) {
            ret += String.format( "\n#### %s\n%s\n\n" +
                    "###############################\n", 
                    i.getKey(), i.getValue());
        }
        return ret;
    }
    
    //
    //
    void add_report( String name, String report )
    {
      long end = System.currentTimeMillis();
      name += String.format(" [took: %ds] [# lines: %d]", 
              (end - start_) / 1000,
              new StringTokenizer(report, "\r\n").countTokens() );
      reports_output_.put( name, report.replaceAll( "\\s+$", "") );
      start_ = System.currentTimeMillis();
    }
    
    
   
    // main
    //
    
    public static void main(String[] args)
    {
        System.out.println("Starting UFAL report generation...");
        //test();
        /**/
        // set up command line parser
        CommandLineParser parser = new PosixParser();
        CommandLine cmdline = null;
        // create an options object and populate it
        Options options = new Options();
        options.addOption("e", "email", true,
                        "Send report to this email address.");
        options.addOption("s", "specific", true,
                        "Perform only specific reports (use index starting from 0).");

        try {
            cmdline = parser.parse(options, args);
        }
        catch (ParseException e)
        {
            System.err.println("Invalid cmd line " + e.toString() );
            log.fatal(e);
            System.exit(1);
        }
        
        // try loading dspace
        DSpaceApi.load_dspace();
        String dspace_dir = ConfigurationManager.getProperty("dspace.dir");

        try
        {
            //
            List<Integer> to_perform = null;
            if ( cmdline.getOptionValues('s') != null ) {
                to_perform = new ArrayList<Integer>();
                for (String s :  cmdline.getOptionValues('s')) {
                    to_perform.add( Integer.valueOf(s));
                }
            }

            Report r = new Report();
            r.generate(to_perform);
            System.out.println("Reports generated...");

            // send/write the report
            //
            if ( cmdline.hasOption('e') )
            {
                String to = cmdline.getOptionValue('e');
                if ( !to.contains("@") ) {
                    to = ConfigurationManager.getProperty("info.recipient");
                }
                try {
                    String email_path = dspace_dir.endsWith("/") ? dspace_dir : dspace_dir + "/";
                    email_path += Report.EMAIL_PATH;
                    System.out.println(String.format("Looking for email template at [%s]", email_path));
                    Email email = Email.getEmail(email_path);
                    email.addRecipient(to);
                    email.addArgument( r.toString() );
                    email.send();
                }catch (Exception me) {
                    System.err.println("\nError sending email:");
                    System.err.println(" - Error: " + me);
                    System.exit(1);
                }
            }

          // write it
          //
            System.out.print( r.toString() );

        }catch (Exception e) {
            log.fatal(e);
            e.printStackTrace();
        }finally {
        }/**/
        
    } // main
    
    /*
    //Log testing...
    public static void test(){
    	GregorianCalendar calendar = new GregorianCalendar();
        GregorianCalendar konec = new GregorianCalendar( 
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        GregorianCalendar zacatek = (GregorianCalendar)konec.clone();
        zacatek.add(Calendar.DAY_OF_MONTH, -31);
    	report_logs_curation rlc = new report_logs_curation(zacatek,konec,"/tmp/log");
    	String result = rlc.process();
    	System.out.println(result);
    }*/
    
    
} // class


//================================
//
//

/**
 * Return report in a string.
 */
interface simple_report {
    public String process();
}


//================================
// Dates
//

class report_date_info implements simple_report
{
    final GregorianCalendar start_date;
    final GregorianCalendar end_date;
    
    public report_date_info(
            GregorianCalendar start_date,GregorianCalendar end_date) 
    {
        this.start_date = start_date; 
        this.end_date = end_date;
    }

    @Override
    public String process() {
        return String.format("Generated: %s\n From - To: %s - %s",
                new Date().toString(),
                new SimpleDateFormat("MM/dd/yyyy").format(start_date.getTime()),
                new SimpleDateFormat("MM/dd/yyyy").format(end_date.getTime())
                );
    }
    
}


//================================
// Interesting urls
//

class report_interesting_urls implements simple_report
{
    final GregorianCalendar end_date;
    
    public report_interesting_urls(GregorianCalendar end_date) {
        this.end_date = end_date;
    }
    
    @Override
    public String process() 
    {
        String url = ConfigurationManager.getProperty("dspace.url");
        String ret = "";
        
        ret += String.format( "Statistics: %s/statistics?date=%s\n",
                url,
                new SimpleDateFormat("yyyy-M").format(end_date.getTime()));
        // http://localhost:8080/xmlui/statistics-google
        ret += String.format( "  Project info: http://svn.ms.mff.cuni.cz/redmine/projects/dspace-modifications");
        ret += String.format( " QA monitoring: http://ufal-point-dev.ms.mff.cuni.cz/nagios3/");
        ret += String.format( "    QA testing: http://ufal-point-dev.ms.mff.cuni.cz:8083/");
        ret += String.format( "      GA stats: %s/statistics-google\n", url);
        ret += String.format( "       CP info: %s/admin/panel?java\n", url);
        ret += String.format( "       CP conf: %s/admin/panel?dspace\n", url);
        ret += String.format( "Clarin harvest: %s", ConfigurationManager.getProperty("lr", "lr.harvester.info.url"));
        ret += String.format( "    Clarin VLO: http://www.clarin.eu/vlo");
        ret += String.format( " Clarin centers: https://centerregistry-clarin.esc.rzg.mpg.de/centers/");
        ret += "   INL harvest: https://portal.clarin.inl.nl/imdiportal/BC?virtpath=/TST-LRs/External%20Resources/OLAC%20Metadata%20Providers/Providers/Charles_University_Prague";
        return ret;
    }
    
}


//================================
// VLO
//

class report_vlo_info implements simple_report
{

    @Override
    public String process() 
    {
        final HashMap<String, String> info = new HashMap<String, String>();
        final List<String> errors = new ArrayList<String>(); 
        try{
        String harvesterInfoUrl = ConfigurationManager.getProperty("lr", "lr.harvester.info.url");
        if(harvesterInfoUrl == null || harvesterInfoUrl.trim().length() == 0){
        	return "PLEASE configure lr.harvester.info.url";
        }
        final String harvesterInfoAnchorName = ConfigurationManager.getProperty("lr", "lr.harvester.info.anchorName");
        if(harvesterInfoAnchorName == null || harvesterInfoAnchorName.trim().length() == 0){
        	return "PLEASE configure lr.harvester.info.anchorName";
        }
        //Try to download the page
        StringWriter writer = new StringWriter();
        org.apache.commons.io.IOUtils.copy(new URL(harvesterInfoUrl.trim()).openStream(), writer);
        String page = writer.toString();
        //String page = org.apache.commons.io.FileUtils.readFileToString(new File("/tmp/lindat.html"));
        //end download
        
            Reader reader = new StringReader(page);
            HTMLEditorKit.Parser parser = new ParserDelegator();
            parser.parse(reader, new HTMLEditorKit.ParserCallback(){                 
                boolean contentDiv = false;
                boolean content_a = false;
                boolean content_p = false;
                String p_text;
                boolean content_p_strong = false;
                boolean content_table_tr = false;
                java.util.LinkedList<String> row = new java.util.LinkedList<String>();
                boolean content_table_td = false;
                String title;
                public void handleStartTag(Tag tag, MutableAttributeSet attrSet, int pos) {
                    if(tag.toString().equals(Tag.DIV.toString())){
                        if(attrSet.containsAttribute(HTML.Attribute.ID, "content")){
                            contentDiv = true;
                        }
                    }
                    else if(contentDiv && tag.toString().equals(Tag.A.toString())){
                        if(attrSet.containsAttribute(HTML.Attribute.NAME, harvesterInfoAnchorName.trim())){
                            content_a = true;
                        }
                    }
                    else if(contentDiv && tag.toString().equals(Tag.P.toString())){
                        content_p = true;
                    }
                    else if(content_p && tag.toString().equals(Tag.STRONG.toString())){
                        content_p_strong = true;
                    }
                    else if(contentDiv && tag.toString().equals(Tag.TR.toString())){
                        content_table_tr = true;
                    }
                    else if(content_table_tr && tag.toString().equals(Tag.TD.toString())){                      
                        Object titleAtr = attrSet.getAttribute(HTML.Attribute.TITLE);
                        if(titleAtr != null){
                            title = titleAtr.toString();
                        }
                        content_table_td = true;
                    }
                }
         
                public void handleText(char[] data, int pos) {
                    String text = new String(data);
                    if(content_a){
                        //ret.append(data).append("\n");
                        info.put("vlo_records", text.replaceAll(".*\\((\\d+).*", "$1")); 
                    }
                    if(content_p && p_text == null){
                        p_text = text;
                    }
                    if(content_p_strong && p_text.contains("records were updated")){
                        info.put("updated", text);
                    }
                    if(content_table_td){
                        row.push(text);
                        if(row.size() == 1 && title == null){
                            title = text;
                        }
                    }
                }
         
                /*public void handleEndOfLineString(String data) {
                    // This is invoked after the stream has been parsed, but before flush. 
                    // eol will be one of \n, \r or \r\n, which ever is 
                    // encountered the most in parsing the stream.
                    System.out.println("End of Line String => " + data);
                }*/
         
                public void handleEndTag(Tag tag, int pos) {
                    if(tag.toString().equals(Tag.DIV.toString()) && contentDiv){
                            contentDiv = false;
                    }
                    else if(contentDiv && tag.toString().equals(Tag.A.toString()) && content_a){
                            content_a = false;
                    }
                    else if(contentDiv && tag.toString().equals(Tag.P.toString())){
                        content_p = false;
                        p_text = null;
                    }
                    else if(content_p && tag.toString().equals(Tag.STRONG.toString())){
                        content_p_strong = false;
                    }
                    else if(contentDiv && tag.toString().equals(Tag.TR.toString())){
                        content_table_tr = false;
                        //find errors in row
                        for(String td_text : row){
                            if(td_text.toLowerCase().contains("error")){
                                errors.add(title);
                            }
                        }
                        row = new java.util.LinkedList<String>();
                        title = null;
                    }
                    else if(content_table_tr && tag.toString().equals(Tag.TD.toString())){
                        content_table_td = false;
                    }
                }
         
                public void handleError(String err, int pos) {
                    //System.out.println("Error => " + err);
                }
            }, true);
            reader.close();
            info.put("total_records", db.get_items_totalCount()+"");
        }catch(Exception e){
            e.printStackTrace();
        }
        String ret = String.format("Number of records in vlo is %s (we have %s items in our repository).\nThe records were harvested at %s.\nIt contains %s errors.\n" ,info.get("vlo_records"), info.get("total_records"), info.get("updated"),errors.size());     
        if(errors.size() > 0){
            ret += "Erroneous ids:\n";
            for(String title : errors){
                ret += title +"\n";
            }
        }
        return ret;
    }
    
}


//================================
// Item rights
//

class report_item_rights_info implements simple_report
{

    @Override
    public String process() {
        //
        Map<String,String> info = db.item_rights_info();
        String pub_info = "";
        for ( Map.Entry<String, String> e : info.entrySet() ) {
            pub_info += String.format("%s: %s\n", e.getKey(), e.getValue());
        }
        return pub_info;
    }
}


//================================
//Server info
//

class report_item_embargo implements simple_report
{
     @Override
     public String process() 
     {
         String ret = "";
         Context context = null;
         try {
            context = new Context();
            ItemIterator item_iter = null;
            try {
                item_iter = EmbargoManager.getEmbargoedItems(context);
            } catch (IllegalArgumentException e) {
            	ret +=" No emargoed items found - " + e.getMessage();
            } catch (Exception e) {
            	ret += e.toString();
            }
             
            while ( item_iter != null && item_iter.hasNext() )
            {
                Item item = item_iter.next();
                String handle = item.getHandle();
                DCDate date = null;
                try {
                    date = EmbargoManager.getEmbargoTermsAsDate(context, item);
                } catch (Exception e) {
                }
                ret += String.format( "%s embargoed till [%s]\n",
                        handle,
                        date != null ? date.toString() : "null"
                       );
            }
            context.complete();
         } catch (SQLException e) {
        	 try {
        		 context.abort();
        	 }catch (Exception e1) {
        	 }
         }
         
         return ret;
     }
}
     
//================================
// Server info
//

class report_server_info implements simple_report
{
    @Override
    public String process() 
    {
        String ret = "";
        ret += String.format( " Dspace built: %s\n", Info.get_ufal_build_time() );
        ret += String.format( "Server uptime: %s\n", Info.get_proc_uptime() );
        
        for ( String[] ss : new String[][] { 
                new String[] {ConfigurationManager.getProperty("assetstore.dir"),
                        "  Assetstore size",},
                        new String[] {ConfigurationManager.getProperty("search.dir"),
                        "  Search dir size",},
                        new String[] {ConfigurationManager.getProperty("log.dir"),
                        "    Log  dir size",},
            })
        {
            long dir_size = IOUtils.dir_size(new File(ss[0]));
            ret += String.format( "%s: %.3f MB\n", ss[1], 
                    dir_size / (1024. * 1024.) );
        }
        return ret;
    }
}


//================================
// User count
//

class report_user_count implements simple_report
{
    @Override
    public String process() 
    {
        String ret = "";
        Map<String,Integer> info = new HashMap<String,Integer>();
        try {
            Context context = new Context();
            EPerson[] epersons = EPerson.findAll(context, EPerson.LASTNAME);
            info.put( "Count", epersons.length );
            info.put( "Can log in (password)", 0 );
            info.put( "Have email", 0 );
            info.put( "Have 1st name", 0 );
            info.put( "Have 2nd name", 0 );
            info.put( "Have lang", 0 );
            info.put( "Have netid", 0 );
            info.put( "Self registered", 0 );

            for ( EPerson e : epersons ) 
            {
                if ( e.getEmail() != null && e.getEmail().length() > 0 )
                    info.put( "Have email", info.get("Have email") + 1);
                if ( e.canLogIn() )
                    info.put( "Can log in (password)", info.get("Can log in (password)") + 1);
                if ( e.getFirstName() != null && e.getFirstName().length() > 0 )
                    info.put( "Have 1st name", info.get("Have 1st name") + 1);
                if ( e.getLastName() != null && e.getLastName().length() > 0 )
                    info.put( "Have 2nd name", info.get("Have 2nd name") + 1);
                if ( e.getLanguage() != null && e.getLanguage().length() > 0 )
                    info.put( "Have lang", info.get("Have lang") + 1);
                if ( e.getNetid() != null && e.getNetid().length() > 0 )
                    info.put( "Have netid", info.get("Have netid") + 1);
                if ( e.getNetid() != null && e.getNetid().length() > 0 )
                    info.put( "Self registered", info.get("Self registered") + 1);
            }
            context.complete();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        ret += String.format("          Users: %d\n", info.get("Count"));
        ret += String.format("     Have email: %d\n", info.get("Have email"));
        for ( Map.Entry<String, Integer> e : info.entrySet() )
        {
            if ( !e.getKey().equals("Count") &&
                    !e.getKey().equals("Have email"))
            {
                ret += String.format( "%15s: %s\n", 
                        e.getKey(), 
                        String.valueOf(e.getValue()));
            }
            
        }
        
        // taken from dspace-info.pl
        try {
            // empty group
            ret += db.get_empty_groups();
            ret += db.get_subscribers();

        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        
        return ret;
    }
}


//================================
// Item count
//

class report_item_count implements simple_report
{
    @Override
    public String process() 
    {
        String ret = "";
        int tot_cnt = 0;
        try {
            for ( Map.Entry<String, Integer> name_count: db.init_communitiesList() ) 
            {
                ret += String.format( "Collection [%s]: %d\n", 
                        name_count.getKey(), name_count.getValue() );
                tot_cnt += name_count.getValue();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        try {
            ret += "\nCollection sizes:\n";
            ret += db.get_collection_sizes();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        ret += String.format( "\nPublished items (archived, not withdrawn): %d\n", tot_cnt );
        try {
            ret += String.format( "Withdrawn items: %d\n", 
                    db.items_withdrawn() );
            ret += String.format( "Not published items (in workspace or workflow mode): %d\n", 
                    db.items_not_archived() );
            
            for(TableRow row : db.workspaceitems()) {
                ret += String.format( "\tIn Stage %s: %s\n", 
                        row.getIntColumn("stage_reached"), row.getLongColumn("cnt") );
            }
            
            ret += String.format( "\n\tWaiting for approval (workflow items): %d\n", 
                    db.workflowitems() );
            
        } catch (SQLException e) {
            ret += " Database problem - exception " + e.toString();
        }
        
        return ret;
    }
}


//================================
// Item info
//

class report_item_info implements simple_report
{
    @Override
    public String process() 
    {
        String report_sizes = "";
        try {
            report_sizes = db.get_object_sizes();
        }catch( SQLException e) {
            report_sizes = "sql exception";
        }
        return report_sizes;
    }
}


//================================
// Log info
//

class report_log_info implements simple_report
{
    final GregorianCalendar start_date; 
    final GregorianCalendar end_date;
    
    public report_log_info(
            GregorianCalendar start_date, GregorianCalendar end_date) 
    {
        this.start_date = start_date;
        this.end_date = end_date;
    }
    
    static public String null2ZeroString(String test) {
        return test==null ? "0" : test;
    }
    
    @Override
    public String process()
    {
        String ret = "";

        Date myStartDate = start_date.getTime();
        Date myEndDate = end_date.getTime();
        
        try {
            Context c = new Context();
            String report = LogAnalyser.processLogs(
                c, 
                null, 
                null, 
                null, 
                "/dev/null", 
                myStartDate, 
                myEndDate, 
                false);
            Map<String,String> map = new HashMap<String,String>();
            for (String key : new String[] { "exceptions", 
                                             "warnings",
                                            } )
                map.put(key, "unknown");
            
            // the function above is really crazy
            for (String line : report.split("\\r?\\n") )
            {
                String[] parts = line.split("=");
                if ( parts.length == 2 )
                    map.put( parts[0], parts[1] );
            }
            
            // find the interesting information
            ret += String.format( "      Exceptions: %s\n", null2ZeroString(map.get("exceptions")) );
            ret += String.format( "        Warnings: %s\n", null2ZeroString(map.get("warnings")) );
            ret += String.format( " Archive browsed: %s\n", null2ZeroString(map.get("action.browse")) );
            ret += String.format( "Archive searched: %s\n", null2ZeroString(map.get("action.search")) );
            ret += String.format( "       Logged in: %s\n", null2ZeroString(map.get("action.login")) );
            ret += String.format( "    Failed login: %s\n", null2ZeroString(map.get("action.failed_login")) );
            ret += String.format( "   OAI requestes: %s\n", null2ZeroString(map.get("action.oai_request")) );
            ret += String.format( "Items added since [%s] (db): %s\n",
                    new SimpleDateFormat("MM/dd/yyyy").format(start_date.getTime()),
                    LogAnalyser.getNumItems( c ) );
            c.complete();
            
            
        } catch (Exception e) {
            //e.printStackTrace();
        }
        
        return ret;
    }
    
}


//================================
// md5
//

class report_checksum implements simple_report
{

    @Override
    public String process() 
    {
        String ret = "No md5 checks made!";
        CheckerCommand checker = new CheckerCommand();
        Date process_start = Calendar.getInstance().getTime();
        checker.setProcessStartDate(process_start);
        checker.setDispatcher(
                //new LimitedCountDispatcher(new SimpleDispatcher(new BitstreamInfoDAO(), null, false), 1)
                // loop through all files
                new SimpleDispatcher(new BitstreamInfoDAO(), process_start, false)
                );
        
        md5_collector collector = new md5_collector();
        checker.setCollector(collector);
        checker.setReportVerbose(true);
        Context context = null;
		try {
			context = new Context();
            checker.process(context);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			if(context != null){
                context.abort();
			}
		}
        

        if ( collector.arr.size() > 0 )
        {
            ret = String.format("Checksum performed on [%d] items:\n", 
                    collector.arr.size());
            int ok_items = 0;
            for ( BitstreamInfo bi : collector.arr )
            {
                if ( !ChecksumCheckResults.CHECKSUM_MATCH.equals(
                        bi.getChecksumCheckResult()) ) 
                {
                    ret += String.format( "md5 checksum FAILED (%s): %s id: %s bitstream-id: %s\n was: %s\n  is: %s\n",
                            bi.getChecksumCheckResult(),
                            bi.getName(),
                            bi.getInternalId(),
                            bi.getBitstreamId(),
                            bi.getStoredChecksum(),
                            bi.getCalculatedChecksum()
                            );
                }else {
                    ok_items++;
                }
            }
            
            ret += String.format( "checksum OK for [%d] items\n", ok_items );
        }
        return ret;     
    }
}


//================================
// Shibd curation
//

class report_shibd_curation implements simple_report
{

    @Override
    public String process() 
    {
        StringBuilder ret = new StringBuilder();
        final String input_dir = ConfigurationManager.getProperty("lr","lr.shibboleth.log.path");
        final String default_log = ConfigurationManager.getProperty("lr","lr.shibboleth.log.defaultName");
        
        String input_file = new File(input_dir, default_log).toString();
        
        ret.append(String.format("Parsing %s:\n",input_file ));
        BufferedReader safe_reader = null;
        try {
            safe_reader = IOUtils.safe_reader( input_file );
            // output warnings
            ShibUserLogins user_logins = new ShibUserLogins( safe_reader );
            if ( 0 < user_logins.warnings().size() ) {
                for ( String warning : user_logins.warnings() ) {
                    ret.append(warning + "\n");
                }
            }else {
                ret.append("No shibboleth warnings have been found.\n");
            }
        } catch ( Exception e ) {
            ret.append(String.format("File: [%s] Warning: [%s]", 
                    default_log, e.toString() )); 
            //return ret.toString();
        }
                
        // > WARN from shibd_warn in the last week
        ret.append("\nParsing shibd_warn.*:\n");
        File dir = new File(input_dir);
            String[] files = dir.list(new java.io.FilenameFilter(){
                    @Override
                    public boolean accept(File dir, String name){
                        return name.contains("shibd_warn");
                    }
                    });
        Long nowMillis = System.currentTimeMillis();
        String weekAgo = new SimpleDateFormat("yyyy-MM-dd").format(new Date(nowMillis-604800000));
        String[] cmd = new String[]{"awk", "-v", "from="+weekAgo, "BEGIN{FS=\" \"} {if($1>=from) print $0}"};
        String[] cmdWithFiles = new String[cmd.length + files.length];
        System.arraycopy(cmd,0,cmdWithFiles,0,cmd.length);
        System.arraycopy(files,0,cmdWithFiles,cmd.length,files.length);
        try{
            Process child = Runtime.getRuntime().exec(cmdWithFiles, null, dir);
            BufferedReader[] outputs = new BufferedReader[]{ new BufferedReader(new InputStreamReader(child.getInputStream())),new BufferedReader(new InputStreamReader(child.getErrorStream()))};
            for(BufferedReader out:outputs){
                String s = null;
                while((s = out.readLine()) != null){
                    ret.append(s);
                    ret.append("\n");
                }
            }
        }catch(java.io.IOException e){
            ret.append(e);
        }
        return ret.toString();      
    }
}


//================================
// Logs curation
//

class report_logs_curation implements simple_report
{
    final static private int MAX_LINES = 10;

    final GregorianCalendar start_date; 
    final GregorianCalendar end_date;
    final String log_dir;
    
    public report_logs_curation(
            GregorianCalendar start_date, GregorianCalendar end_date) 
    {
        this(start_date,end_date,ConfigurationManager.getProperty("log.dir"));
    }
    
    public report_logs_curation(GregorianCalendar start_date, GregorianCalendar end_date, String log_dir){
        this.start_date = start_date;
        this.end_date = end_date;
    	this.log_dir = log_dir;
    }

    @Override
    public String process() 
    {
        StringBuffer ret = new StringBuffer();
        
        List<String> dates = new ArrayList<String>();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime( start_date.getTime() );

        while (calendar.getTime().before(end_date.getTime()))
        {
             Date resultado = calendar.getTime();
             dates.add(new SimpleDateFormat("yyyy-MM-dd").format(resultado));
             calendar.add(Calendar.DATE, 1);
        }
        
        for ( String date_str : dates.toArray(new String[dates.size()]) )
        {
            String[] log_names = IOUtils.list_files( log_dir, date_str );
            for ( int i = 0; i < log_names.length; ++i )
            {
                System.err.println( String.format(
                    "    #%d. Parsing [%s] log file at [%s]", 
                    i, log_names[i], 
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())
                ));
                String input_file = new File(log_dir, log_names[i]).toString(); 
        
                BufferedReader safe_reader = null;
                try {
                    safe_reader = IOUtils.safe_reader( input_file );
                } catch ( EOFException e ) {
                    continue;
                } catch ( Exception e ) {
                    ret.append(String.format("Exception [%s] with %s\n", 
                            e.toString(), input_file));
                    continue;
                }       
                // output warnings
                ImportantLogs logs = new ImportantLogs( 
                        safe_reader, IOUtils.get_date_from_log_file(input_file) );
                boolean problem_found = (logs._lines.size() != 0 
                        || 0 < logs.warnings().size() );
                // output info
                if ( problem_found ) 
                {
                    ret.append(String.format("File: [%s] Warnings/Errors: [%d/%d]\n\n", 
                            log_names[i], 
                            logs.warnings().size(),
                            logs._lines.size() ));
                    for ( int j = 0; j < logs._lines.size(); ++ j ) 
                    {
                        if ( j > MAX_LINES ) {
                            ret.append( String.format("****\n... truncated [%d] lines...\n************\n\n", 
                                            logs._lines.size() - j ) );
                            break;
                        }
                        String l = logs._lines.get(j); 
                        ret.append(String.format("\t%s\n", l ));
                    }
                }
            }
        }
        return ret.toString();     
    }
}


//================================
// Curator
//


class report_curator implements simple_report
{

    @Override
    public String process() 
    {
        String ret = "";
        //These are considered successful
        //int[] goodStates = {Curator.CURATE_SUCCESS, Curator.CURATE_SKIP};                   
        // all curation
        for (String task_name : new String[] {
            "profileformats",
            "requiredmetadata",
            "fastchecklinks",
            "checkhandles",
            "checkmetadata"
        } )
        {
            try {
                ret += String.format("=================\n\nTask %s", task_name);
                Curator curator = new Curator();
                curator.addTask(task_name);
                //curator.setReporter("-");
                curator.setInvoked(Curator.Invoked.INTERACTIVE);
                try
                {
                    Context c = new Context();
                    // Curate this object & return result
                    long startTime = System.currentTimeMillis();
                    curator.curate( c, Site.getSiteHandle() );
                    ArrayList<String> results = curator.getOverallResult(task_name);
                    long endTime = System.currentTimeMillis();
                    ret += String.format(" Took: %ds Returned: %d\n\n", 
                                    (endTime - startTime)/1000, curator.getOverallStatus(task_name) );
                    // do for failed or for profile...
                    // checklinks have strange output - show only the bad ones
                    if ( task_name.equals("fastchecklinks") || task_name.equals("checkhandles") ) {
                        ret += output_checklinks(results);
                    }else if (task_name.equals("requiredmetadata")) {
                        ret += output_requiredmetadata(results);
                    }else {
                        for(String str : results) {
                            ret += str + "\n";
                        }
                    }
                    c.complete();
                }catch (Exception e) {
                    ret += String.format("  exception occurred: %s", e.toString());
                }
            }catch (Exception e) {
                ret += String.format(" exception occurred: %s", e.toString());
            }
        }
        return ret;
    }
    
    private static String output_checklinks(ArrayList<String> results) {
        String ret = "";
        String last_item = null;
        for (String strs : results) {
            for ( String str: strs.split("\n") ) {
                if ( str.trim().endsWith("- OK") ) {
                    continue;
                }else if (str.trim().startsWith("Item:")) {
                    last_item = str;
                }else {
                    if ( last_item != null ) {
                        ret += last_item + "\n";
                        last_item = null;
                    }
                    ret += str + "\n";
                }
            }
        }
        return ret;
    }

    private static String output_requiredmetadata(ArrayList<String> results) {
        String ret = "";
        for (String strs : results) {
            for ( String str: strs.split("\n") ) {
                if ( str.trim().endsWith("has all required fields") ||
                         str.trim().endsWith("This task runs only on ITEMs.") 
                                ) {
                    continue;
                }
                ret += str + "\n";
            }
        }
        return ret;
    }

}


//================================
// OA-PMH
//

class report_oaipmh_validator implements simple_report
{

    @Override
    public String process() 
    {
        String ret ="";
        String dspace_dir = ConfigurationManager.getProperty("dspace.dir");
        String dspace_url = ConfigurationManager.getProperty("dspace.baseUrl");
        String oaiurl = dspace_url + "/oai/request";
        ret += String.format("Trying [%s]\n", oaiurl);
        ret += IOUtils.run( new File(dspace_dir+"/bin/"), 
                new String[] {"python", "./validators/oai_pmh/validate.py", oaiurl} );
        return ret;
    }
}


//================================
// VM
//

class report_vm_bean_counters implements simple_report
{

    @Override
    public String process() 
    {
        String ret = IOUtils.run( new File(ConfigurationManager.getProperty("dspace.dir")), 
                new String[] {"sudo", "cat", "/proc/user_beancounters"} );
        return ret;
    }
}


//================================
// Assetstore validity
//

class report_assetstore_validity implements simple_report
{

    @Override
    public String process() 
    {
        String ret ="";
        String dspace_dir = ConfigurationManager.getProperty("dspace.dir");
        String assetstore_dir = dspace_dir + "/assetstore";
        ret += IOUtils.run( new File(dspace_dir+"/bin/validators/assetstore/"), 
                new String[] {"python", "main.py", "--dir=" + assetstore_dir} );
        return ret;
    }
}


//================================
// PID
//

class report_pid_service implements simple_report
{

    @Override
    public String process() 
    {
        String ret = "";
        try {
            String whoami = PIDService.who_am_i("encoding=xml");
            ret += String.format("Who am I\n\t%s\n", whoami.replaceAll("\n", "\n\t"));
            String test_pid = ConfigurationManager.getProperty("lr", "lr.pid.service.testPid");
            ret += "Testing PID server\n\t";
            if ( test_pid != null ) {
                ret += PIDService.test_pid(test_pid);
            }else {
                ret += "Testing PID server not done! Test pid not in dspace.cfg!";
            }
        }catch( org.apache.commons.lang.NotImplementedException e ) {
            ret += String.format("Testing PID server - method who_am_i not implemented");
        }catch( Exception e ) {
            ret += String.format("Testing PID server failed - exception occurred: %s", e.toString());
            e.printStackTrace();
        }
        return ret;
    }
}


//================================
//
//

class report_discojuice_info implements simple_report
{

    @Override
    public String process() 
    {
        String ret = "";
        try{
            String feedsConfig = ConfigurationManager.getProperty("discojuice", "feeds");
            ret += String.format("Using these static discojuice feeds [%s] as source.\n",feedsConfig);
            //Try to download our feeds file, so the proper action is triggered
            StringWriter writer = new StringWriter();
            org.apache.commons.io.IOUtils.copy(new URL(
                ConfigurationManager.getProperty("dspace.url")+"/discojuice/feeds").openStream(), writer);
            String jsonp = writer.toString();
            //end download
            String json = jsonp.substring(jsonp.indexOf("(") + 1, jsonp.lastIndexOf(")")); //strip the dj_md_1()
            Set<String> entities = new HashSet<String>();
            JSONParser parser = new JSONParser();
            JSONArray entityArray = (JSONArray)parser.parse(json);
            Iterator<JSONObject> i = entityArray.iterator();        
            int counter = 0;
            while(i.hasNext()){
            	counter++;
                JSONObject entity = i.next();
                String entityID = (String)entity.get("entityID");
                entities.add(entityID);
            }
            int idCount = entities.size();
            ret += String.format("Our feeds file contains %d entities out of which %d are unique.\n" +
                "This number should be around 1200?? (20.11.2014).",
                counter, idCount);
        } catch (Exception e){
            e.printStackTrace();
        }
        
        return ret;
    }
}

/**
 * This class provides basic report about handles in current DSpace instance
 * 
 * @author Michal Josífko
 *
 */
class report_handle_info implements simple_report
{
    @Override
    public String process() 
    {
        String ret = "";
        
        try
        {
            long total_count = db.get_handles_total_count();
            ret += "\n";
            ret += String.format("Total count: %d\n", total_count);
            List<TableRow> invalid_handles = db.get_handles_invalid_handles();
            ret += String.format("Invalid handles count: %d\n", invalid_handles.size());
            if(invalid_handles.size() > 0)
            {
                ret += "\n";
                ret += "Invalid handles:\n";
                ret += "----------------\n";
                ret += String.format("%-6s\t%-32s\t%-10s\t%-10s\t%s\n","Handle ID","Handle","Res. type ID","Resource ID","URL");
                for(TableRow row : invalid_handles)
                {
                    int handle_id = row.getIntColumn("handle_id");                    
                    String handle = row.getStringColumn("handle");
                    
                    Integer resource_type_id = row.getIntColumn("resource_type_id");
                    if(resource_type_id < 0) resource_type_id = null;                    
                    
                    Integer resource_id = row.getIntColumn("resource_id");
                    if(resource_id < 0) resource_id = null;
                    
                    String url = row.getStringColumn("url");
                    ret += String.format("%-10d\t%-32s\t%-10d\t%-10d\t%s\n",handle_id,handle,resource_type_id,resource_id,url);
                }
            }
        }
        catch(SQLException e) 
        {
            ret += " Database problem - exception " + e.toString();
        }
        
        return ret;
    }
}

/**
 * This class provides basic statistics about handle resolution gathered from HandlePlugin logs 
 * 
 * @author Michal Josífko
 *
 */
class report_handle_resolution_statistics implements simple_report
{
    private Calendar startDate;    
    private Calendar endDate;
    private int topN; 
    
    public report_handle_resolution_statistics(Calendar startDate, Calendar endDate )
    {
        this(startDate, endDate, 10);        
    }
    
    public report_handle_resolution_statistics(Calendar startDate, Calendar endDate, int topN )
    {
        this.startDate = startDate;
        this.endDate = endDate;
        this.topN = topN;
    }
    
    @Override
    public String process() 
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        Calendar realEndDate = (Calendar)endDate.clone();
        realEndDate.add(Calendar.DATE, -1);
        
        StringBuffer buf = new StringBuffer();
        buf.append("============================================================\n");
        buf.append(String.format("PID resolution statistics\n"));
        buf.append("============================================================\n");
        buf.append("\n\n");
        PIDLogMiner logMiner = new PIDLogMiner();
        PIDLogStatistics statistics = logMiner.computeStatistics(startDate.getTime(), realEndDate.getTime());
        Map<String, List<PIDLogStatisticsEntry>> topNEntries= statistics.getTopN(topN);        
        String eventsToDisplay[] = { PIDLogMiner.FAILURE_EVENT, PIDLogMiner.REQUEST_EVENT, PIDLogMiner.SUCCESS_EVENT, PIDLogMiner.UNKNOWN_EVENT };
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        for(String event: eventsToDisplay)
        {
            if(topNEntries.containsKey(event)) 
            {
                buf.append(String.format("Top %d events of type %s between %s and %s\n", topN, event, 
                        dateFormat.format(startDate.getTime()), 
                        dateFormat.format(realEndDate.getTime())));
                buf.append(String.format("---------------------------------------------------------------\n", topN, event));
                buf.append(String.format("%-10s%-40s%-25s%-25s\n", "Count", "PID", "First occurence", "Last occurence"));
                buf.append(String.format("--------------------------------------------------------------------------------------------------\n", topN, event));
                for (PIDLogStatisticsEntry entry : topNEntries.get(event))
                {                    
                    buf.append(String.format("%-10d%-40s%-25s%-25s\n", entry.getCount(), entry.getPID(), 
                            dateTimeFormat.format(entry.getFirstOccurence()), 
                            dateTimeFormat.format(entry.getLastOccurence())));                
                }
                buf.append("\n");
            }
        }
        return buf.toString();
    }
}



//================================
//
//

class md5_collector implements ChecksumResultsCollector
{
    public List<BitstreamInfo> arr = new ArrayList<BitstreamInfo>();
    public void collect(BitstreamInfo info)
    {
        arr.add(info);
    }

}


//================================
// DSpace db helpers
//

class db {
    
    //
    //

    private static java.util.List<java.util.Map.Entry<String,Integer>> _communitiesList = null;
    static int get_items_totalCount() throws SQLException{
        if(_communitiesList == null){
            init_communitiesList();
        }
        int total = 0;
        for(java.util.Map.Entry<String, Integer> name_count : _communitiesList){
            total += name_count.getValue();
        }
        return total;
    }
    static List<Map.Entry<String,Integer>> init_communitiesList() throws SQLException{
        _communitiesList = new java.util.ArrayList<Map.Entry<String,Integer>>();
        Context context = new Context();
        Community[] top_communities = Community.findAllTop(context);
        for ( Community c : top_communities ) 
        {
            _communitiesList.add(new java.util.AbstractMap.SimpleEntry<String, Integer>(c.getName(), new Integer(c.countItems())));
        }
        context.complete();
        return _communitiesList;
    }
    
    static String get_empty_groups() throws SQLException 
    {
        String ret = "";
        Context c = new Context();
        TableRowIterator irows = DatabaseManager.query(c, 
                "SELECT eperson_group_id, name from epersongroup "
                + "WHERE eperson_group_id NOT IN (SELECT eperson_group_id FROM epersongroup2eperson)");
        List<TableRow> rows = irows.toList();
        ret += String.format("   Empty groups: %d (", rows.size());
        for ( TableRow row : rows ) {
            ret+= row.getStringColumn("name") + ", ";
        }
        ret += ")\n";
        c.complete();
        return ret;
    }
    

    static String get_subscribers() throws SQLException {
        String ret = "";
        List<TableRow> rows = db.subscribers();
        ret += String.format("    Subscribers: %d (", rows.size());
        for ( TableRow row : rows ) {
            ret+= row.getIntColumn("eperson_id") + ", ";
        }
        ret += ")\n";
        
        rows = db.subscribed_collections();
        ret += String.format("Subscr. collec.: %d (", rows.size());
        for ( TableRow row : rows ) {
            ret+= row.getIntColumn("collection_id") + ", ";
        }
        ret += ")\n";
        return ret;
    }

    
    static String get_collection_sizes() throws SQLException {
        String ret = "";
    List<TableRow> rows = db.sql( 
                "SELECT c1.name, SUM(bs.size_bytes) FROM " +
                "collection c1, collection2item c2i1, item2bundle i2b1, " +
                "bundle2bitstream b2b1, bitstream bs " +
            "WHERE " +
                "c1.collection_id=c2i1.collection_id AND " +
                "c2i1.item_id=i2b1.item_id AND " +
                "i2b1.bundle_id=b2b1.bundle_id AND " +
                "b2b1.bitstream_id=bs.bitstream_id " +
            "GROUP BY c1.name");
        double total_size = 0;
        for ( TableRow row : rows ) {
            double size = row.getLongColumn("sum") / (1024. * 1024.);
            total_size += size;
            ret += String.format( "\t%s:  %.3f MB\n", 
                    row.getStringColumn("name"), 
                    size );
        }
        ret += String.format( "Total size: %.3f MB\n", 
                total_size );
        
        ret += String.format( "Resource without policy: %d\n", 
                db.bitstreams_without_policy() );

        ret += String.format( "     Deleted bitstreams: %d\n", 
                db.bitstreams_deleted() );

            
        rows = db.bitstream_orphans();
        String list_str = "";
        for ( TableRow row : rows ) {
            list_str += String.format( "%d, ", row.getIntColumn("bitstream_id") );
        }
        ret += String.format( "      Orphan bitstreams: %d [%s]\n", 
                rows.size(), list_str );

        return ret;
    }
        
    public static String get_object_sizes() throws SQLException 
    {
        String ret = "";
        Context c = new Context();
        
        for (String tb : new String [] {
                "bitstream",
                "bundle",
                "collection",
                "community",
                "dcvalue",
                "eperson",
                "item",
                "handle",
                "epersongroup",
                "workflowitem",
                "workspaceitem",
        } ) 
        {
            TableRowIterator irows = DatabaseManager.query(c, 
                    "SELECT COUNT(*) from " + tb );
            List<TableRow> rows = irows.toList();
            ret += String.format("Count %s: %s\n", 
                    tb, 
                    String.valueOf(rows.get(0).getLongColumn("count")));
        }
        
        c.complete();
        return ret;
    }
    
    @SuppressWarnings("deprecation")
    public static Map<String,String> item_rights_info()
    {
        Map<String,String> ret = new HashMap<String,String>();
        Map<String,Integer> info = new HashMap<String,Integer>();
        try {
            Context context = new Context();
            ItemIterator it = Item.findAll(context);
            while( it.hasNext() )
            {
                Item i = it.next();
                Metadatum[] labels = i.getMetadata("dc", "rights", "label", Item.ANY);
                String pub_dc_value = "";
                
                if ( labels.length > 0 ) {
                    for ( Metadatum dc : labels ) {
                        if (pub_dc_value.length() == 0) {
                            pub_dc_value = dc.value;
                        }else {
                            pub_dc_value = pub_dc_value + " " + dc.value;
                        }
                    }
                }else {
                    pub_dc_value = "no licence";
                }
                
                if ( !info.containsKey(pub_dc_value) ) {
                    info.put(pub_dc_value, 0);
                }
                info.put( pub_dc_value, info.get(pub_dc_value) + 1);
            }
            context.complete();
            
            for ( Map.Entry<String, Integer> e : info.entrySet() ) 
            {
                ret.put( e.getKey(), String.valueOf( e.getValue() ));
                
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ret;     
    }
    
    //
    //
    
    static List<TableRow> sql(String sql) throws SQLException
    {
        Context c = new Context();
        List<TableRow> ret = DatabaseManager.query(c, sql).toList();
        c.complete();
        return ret;
    }

    static int bitstreams_deleted() throws SQLException
    {
        return sql("SELECT * from bitstream where deleted=true").size();
    }
    
    static List<TableRow> bitstream_orphans() throws SQLException
    {
        return sql("SELECT bitstream_id FROM bitstream WHERE deleted<>true AND bitstream_id "
                + "NOT IN (" +
                 "select bitstream_id from bundle2bitstream " +
                 "UNION select logo_bitstream_id from community WHERE logo_bitstream_id is not NULL " +
                 "UNION select primary_bitstream_id from bundle WHERE primary_bitstream_id is not NULL order by bitstream_id " +
                         ")");
        
    }
    
    static int bitstreams_without_policy() throws SQLException
    {
        return sql(
        "SELECT bitstream_id FROM bitstream WHERE deleted<>true AND bitstream_id NOT IN (SELECT "
            + "resource_id FROM resourcepolicy WHERE resource_type_id=0)").size();
    }
    
    static int items_withdrawn() throws SQLException
    {
        return sql("select * from item where withdrawn=true").size();
    }
    
    static int items_not_archived() throws SQLException
    {
        return sql("select * from item where in_archive=false and withdrawn=false").size();
    }
    
    
    static List<TableRow> subscribers() throws SQLException
    {
        return sql("SELECT DISTINCT ON (eperson_id) eperson_id FROM subscription");
    }
    
    static List<TableRow> subscribed_collections() throws SQLException
    {
        return sql("SELECT DISTINCT ON (collection_id) collection_id FROM subscription");
    }
    
    static List<TableRow> workspaceitems() throws SQLException
    {
        return sql("SELECT stage_reached, count(1) as cnt FROM workspaceitem GROUP BY stage_reached ORDER BY stage_reached;");
    }            

    static int workflowitems() throws SQLException
    {
        return sql("SELECT * FROM workflowitem;").size();
    }
    
    static long get_handles_total_count() throws SQLException
    {        
        List<TableRow> rows = sql("SELECT count(1) as cnt FROM handle");     
        return rows.get(0).getLongColumn("cnt");
    }

    static List<TableRow> get_handles_invalid_handles() throws SQLException
    {
        List<TableRow> rows = sql("SELECT * FROM handle "
                + " WHERE NOT ("
                + "     (handle IS NOT NULL AND resource_type_id IS NOT NULL AND resource_id IS NOT NULL)"
                + " OR "
                + "     (handle IS NOT NULL AND url IS NOT NULL)"
                + " ) ");
        return rows;
    }


} // db
